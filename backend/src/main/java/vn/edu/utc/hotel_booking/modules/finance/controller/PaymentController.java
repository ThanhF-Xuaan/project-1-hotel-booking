package vn.edu.utc.hotel_booking.modules.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentSearchDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CreatePaymentRequest;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.PaymentUrlResponse;
import vn.edu.utc.hotel_booking.modules.finance.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment Controller", description = "Quản lý thanh toán, giao dịch cọc và hoàn tiền")
public class PaymentController {

    PaymentService paymentService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Tạo mới giao dịch thanh toán")
    public ApiResponse<PaymentResponse> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        return ApiResponse.success("Tạo thanh toán thành công", paymentService.createPayment(request));
    }

    @PostMapping("/filter")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Tìm kiếm và phân trang lịch sử thanh toán")
    public ApiResponse<PageResponse<PaymentResponse>> filter(@RequestBody PaymentSearchDto searchDto) {
        return ApiResponse.success(paymentService.filter(searchDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Lấy chi tiết thanh toán theo ID")
    public ApiResponse<PaymentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(paymentService.getById(id));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('CHAIN_EXECUTIVE', 'CHAIN_ADMIN', 'REGION_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Lấy danh sách các khoản thanh toán của đơn đặt phòng")
    public ApiResponse<List<PaymentResponse>> getByBookingId(@PathVariable Long bookingId) {
        return ApiResponse.success(paymentService.getByBookingId(bookingId));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'FINANCE')")
    @Operation(summary = "Xác nhận hoàn tất thanh toán thành công")
    public ApiResponse<PaymentResponse> completePayment(@PathVariable Long id) {
        return ApiResponse.success("Xác nhận thanh toán thành công", paymentService.completePayment(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")
    @Operation(summary = "Hủy thanh toán đang chờ (PENDING → CANCELLED) — dùng cho flow lỗi: popup → hủy → làm lại")
    public ApiResponse<PaymentResponse> cancelPayment(@PathVariable Long id) {
        return ApiResponse.success("Hủy thanh toán thành công", paymentService.cancelPayment(id));
    }

    @PostMapping("/vnpay/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST', 'ROLE_CUSTOMER')")
    @Operation(summary = "Tạo phiên thanh toán VNPay — trả paymentUrl + txnRef + expiresAt (TTL 10')")
    public ApiResponse<PaymentUrlResponse> createVnPayPayment(
            @Valid @RequestBody CreatePaymentRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success("Tạo phiên thanh toán VNPay thành công",
                paymentService.createGatewayPayment(request, resolveClientIp(httpRequest)));
    }

    @PostMapping("/momo/create")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST', 'ROLE_CUSTOMER')")
    @Operation(summary = "Tạo phiên thanh toán MoMo — trả paymentUrl + txnRef + expiresAt (TTL 10')")
    public ApiResponse<PaymentUrlResponse> createMoMoPayment(
            @Valid @RequestBody CreatePaymentRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success("Tạo phiên thanh toán MoMo thành công",
                paymentService.createGatewayPayment(request, resolveClientIp(httpRequest)));
    }

    /** Lấy IP thật của khách (qua proxy/load-balancer nếu có) — VNPay yêu cầu vnp_IpAddr */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'FINANCE')")
    @Operation(summary = "Hoàn trả tiền thanh toán (Refund)")
    public ApiResponse<PaymentResponse> refundPayment(
            @PathVariable Long id,
            @RequestParam BigDecimal refundAmount,
            @RequestParam(required = false) String reason
    ) {
        return ApiResponse.success("Hoàn tiền thành công", paymentService.refundPayment(id, refundAmount, reason));
    }
}
