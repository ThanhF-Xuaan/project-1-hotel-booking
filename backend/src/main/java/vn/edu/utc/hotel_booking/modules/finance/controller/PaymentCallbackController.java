package vn.edu.utc.hotel_booking.modules.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CreatePaymentRequest;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.PaymentUrlResponse;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.PaymentService;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Webhook PUBLIC (permitAll qua /api/v1/public/** trong SecurityConfig — không JWT).
 *
 * VNPay/MoMo gọi về để báo kết quả — bắt buộc:
 *  - Verify chữ ký trước (sai → HTTP 400 + log payload);
 *  - Idempotent: xử lý lại vẫn trả 200 cho gateway (không update lần 2);
 *  - Tự ghi transactionReference từ webhook (vnp_TransactionNo / MoMo orderId).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public/payments")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment Callback Controller", description = "Webhook công khai từ VNPay/MoMo (không JWT)")
public class PaymentCallbackController {

    PaymentService paymentService;
    PaymentRepository paymentRepository;

    /** FE result page (redirect cuối cùng sau khi BE verify xong) */
    String feResultUrl;

    // Constructor tường minh với @Value — @RequiredArgsConstructor sẽ kéo field String
    // vào constructor parameter nên Spring không inject được cấu hình từ application.yaml
    public PaymentCallbackController(
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            @Value("${payment.gateway.vnpay.fe-result-url}") String feResultUrl) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.feResultUrl = feResultUrl;
    }

    @GetMapping({"/vnpay/ipn", "/vnpay-ipn"})
    @Operation(summary = "VNPay IPN — server-to-server, verify chữ ký + idempotent")
    public ResponseEntity<Map<String, String>> vnPayIpn(@RequestParam Map<String, String> params) {
        try {
            paymentService.handleGatewayCallback(PaymentMethod.VNPAY, params);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.PAYMENT_CALLBACK_DUPLICATED) {
                // Idempotent — đã xử lý rồi: xác nhận lại 200 để gateway ngừng retry
                log.info("VNPay IPN trùng lặp (idempotent) txnRef={}", params.get("vnp_TxnRef"));
                return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Already Confirmed"));
            }
            if (e.getErrorCode() == ErrorCode.PAYMENT_SIGNATURE_INVALID) {
                // Chữ ký sai → HTTP 400 (payload đã được Service log.warn)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("RspCode", "97", "Message", "Invalid Signature"));
            }
            throw e;
        } catch (Throwable t) {
            log.error("VNPay IPN gặp lỗi nghiêm trọng: txnRef={} - {}", params.get("vnp_TxnRef"), t.getMessage(), t);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("RspCode", "99", "Message", "Unknown Error"));
        }
    }

    @GetMapping({"/vnpay/return", "/vnpay-return"})
    @Operation(summary = "VNPay redirect trình duyệt — verify rồi 302 về trang kết quả FE")
    public ResponseEntity<Void> vnPayReturn(@RequestParam Map<String, String> params) {
        boolean paid;
        // Khi xử lý mới tại đây (chưa qua IPN) → BE trả kèm id để FE poll trạng thái
        String paymentIdPart = "";
        String bookingIdPart = "";
        try {
            PaymentResponse response = paymentService.handleGatewayCallback(PaymentMethod.VNPAY, params);
            paid = response.getStatus() == PaymentStatus.SUCCESS;
            paymentIdPart = "&paymentId=" + response.getId();
            bookingIdPart = "&bookingId=" + response.getBookingId();
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.PAYMENT_CALLBACK_DUPLICATED) {
                // Đã xử lý qua IPN rồi (idempotent) — dùng chính kết quả của gateway để hiển thị
                paid = "00".equals(params.get("vnp_ResponseCode"));
            } else {
                // Chữ ký sai / lỗi khác → trang kết quả FE hiển thị thất bại + cho thử lại
                log.warn("VNPay return lỗi: {} — txnRef={}", e.getErrorCode(), params.get("vnp_TxnRef"));
                paid = false;
            }
        } catch (Throwable t) {
            log.error("VNPay return gặp lỗi không mong muốn khi xử lý callback: txnRef={} - {}",
                    params.get("vnp_TxnRef"), t.getMessage(), t);
            paid = false;
        }
        String status = paid ? "PAID" : "FAILED";
        String txnRef = params.get("vnp_TxnRef") != null ? params.get("vnp_TxnRef") : "";

        if ((paymentIdPart.isBlank() || bookingIdPart.isBlank()) && !txnRef.isBlank() && paymentRepository != null) {
            try {
                var optPayment = paymentRepository.findByGatewayTxnId(txnRef);
                if (optPayment.isPresent()) {
                    var p = optPayment.get();
                    if (paymentIdPart.isBlank()) {
                        paymentIdPart = "&paymentId=" + p.getId();
                    }
                    if (bookingIdPart.isBlank()) {
                        try {
                            if (p.getBooking() != null && p.getBooking().getId() != null) {
                                bookingIdPart = "&bookingId=" + p.getBooking().getId();
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable t) {
                log.warn("Không thể tra cứu bổ sung Payment ID / Booking ID cho txnRef {}: {}", txnRef, t.getMessage());
            }
        }

        StringBuilder queryParams = new StringBuilder();
        queryParams.append("?status=").append(status);
        queryParams.append("&txnRef=").append(java.net.URLEncoder.encode(txnRef, java.nio.charset.StandardCharsets.UTF_8));
        queryParams.append(paymentIdPart).append(bookingIdPart);

        // Chuyển tiếp các thông tin đối chiếu kỹ thuật theo chuẩn Docs VNPay
        String[] forwardFields = {"vnp_ResponseCode", "vnp_TransactionNo", "vnp_BankCode",
                "vnp_CardType", "vnp_PayDate", "vnp_BankTranNo", "vnp_OrderInfo", "vnp_TransactionStatus", "vnp_Amount"};
        for (String field : forwardFields) {
            String val = params.get(field);
            if (val != null && !val.isBlank()) {
                queryParams.append("&").append(field).append("=")
                        .append(java.net.URLEncoder.encode(val, java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        String separator = feResultUrl.contains("?") ? "&" : "?";
        String queryString = queryParams.length() > 0 && queryParams.charAt(0) == '?'
                ? queryParams.substring(1)
                : queryParams.toString();
        URI redirect = URI.create(feResultUrl + separator + queryString);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirect).build();
    }

    @PostMapping("/momo/callback")
    @Operation(summary = "MoMo callback — verify signature + idempotent (luôn 200 khi đã xử lý)")
    public ResponseEntity<Map<String, Object>> moMoCallback(@RequestBody Map<String, Object> body) {
        Map<String, String> params = body.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() == null ? "" : String.valueOf(e.getValue())));
        try {
            paymentService.handleGatewayCallback(PaymentMethod.MOMO, params);
            return ResponseEntity.ok(Map.of("statusCode", 0));
        } catch (AppException e) {
            if (e.getErrorCode() == ErrorCode.PAYMENT_CALLBACK_DUPLICATED) {
                // Idempotent — đã xử lý → vẫn 200 cho MoMo (đúng contract)
                log.info("MoMo callback trùng lặp (idempotent) orderId={}", params.get("orderId"));
                return ResponseEntity.ok(Map.of("statusCode", 0));
            }
            if (e.getErrorCode() == ErrorCode.PAYMENT_SIGNATURE_INVALID) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("statusCode", 1, "message", "Invalid Signature"));
            }
            throw e;
        }
    }

    @PostMapping("/vnpay/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo phiên thanh toán VNPay công khai cho khách đặt phòng online")
    public ApiResponse<PaymentUrlResponse> createPublicVnPayPayment(
            @Valid @RequestBody CreatePaymentRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success("Tạo phiên thanh toán VNPay thành công",
                paymentService.createGatewayPayment(request, resolveClientIp(httpRequest)));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Tra cứu trạng thái thanh toán công khai cho khách vãng lai")
    public ApiResponse<PaymentResponse> getPublicPaymentStatus(@PathVariable Long id) {
        return ApiResponse.success(paymentService.getById(id));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            ip = forwarded.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        if (ip == null || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
