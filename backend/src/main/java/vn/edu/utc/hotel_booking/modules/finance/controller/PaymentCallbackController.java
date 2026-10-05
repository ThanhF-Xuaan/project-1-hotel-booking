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
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;
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

    /** FE result page (redirect cuối cùng sau khi BE verify xong) */
    String feResultUrl;

    // Constructor tường minh với @Value — @RequiredArgsConstructor sẽ kéo field String
    // vào constructor parameter nên Spring không inject được cấu hình từ application.yaml
    public PaymentCallbackController(
            PaymentService paymentService,
            @Value("${payment.gateway.vnpay.fe-result-url}") String feResultUrl) {
        this.paymentService = paymentService;
        this.feResultUrl = feResultUrl;
    }

    @GetMapping("/vnpay/ipn")
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
        }
    }

    @GetMapping("/vnpay/return")
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
        }
        String status = paid ? "PAID" : "FAILED";
        String txnRef = params.get("vnp_TxnRef") != null ? params.get("vnp_TxnRef") : "";
        URI redirect = URI.create(feResultUrl + "?status=" + status
                + "&txnRef=" + txnRef + paymentIdPart + bookingIdPart);
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
}
