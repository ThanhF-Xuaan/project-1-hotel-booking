package vn.edu.utc.hotel_booking.modules.finance.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentSearchDto;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CreatePaymentRequest;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.PaymentUrlResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    PaymentResponse createPayment(PaymentCreateRequest request);

    /**
     * Tạo phiên thanh toán online (VNPay/MoMo): Redis lock TTL 10' (tránh tạo trùng
     * khi khách redirect), lưu gateway_txn_id, trả paymentUrl + expiresAt cho FE countdown.
     */
    PaymentUrlResponse createGatewayPayment(CreatePaymentRequest request, String ipAddress);

    /**
     * Xác thực callback (IPN) từ cổng thanh toán + cập nhật Payment.
     * Idempotent: giao dịch đã xử lý → ném PAYMENT_CALLBACK_DUPLICATED (controller trả 200 cho gateway).
     */
    PaymentResponse handleGatewayCallback(PaymentMethod method, Map<String, String> params);

    PageResponse<PaymentResponse> filter(PaymentSearchDto searchDto);

    PaymentResponse getById(Long id);

    List<PaymentResponse> getByBookingId(Long bookingId);

    PaymentResponse completePayment(Long id);

    /**
     * Hủy thanh toán đang dở (PENDING → CANCELLED) — dùng cho flow lỗi §6:
     * ErrorCode → popup → hủy payment → FE restart flow từ đầu.
     */
    PaymentResponse cancelPayment(Long id);

    PaymentResponse refundPayment(Long id, BigDecimal refundAmount, String reason);
}
