package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.gateway.PaymentGateway;
import vn.edu.utc.hotel_booking.modules.finance.gateway.VnPayGateway;
import vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.TransactionRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.PaymentServiceImpl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Giai đoạn C5 — flow callback VNPay qua Service THẬT với VnPayGateway THẬT:
 * verify chữ ký (đúng/sai secret, thiếu param) + idempotency + ghi reference tự động.
 */
@ExtendWith(MockitoExtension.class)
class VnPayCallbackTest {

    @Mock PaymentRepository paymentRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock BookingRepository bookingRepository;
    @Mock FinanceMapper financeMapper;
    @Mock StringRedisTemplate redisTemplate;

    PaymentServiceImpl paymentService;
    VnPayGateway vnPayGateway;

    Booking booking;
    Payment pendingPayment;

    private static final String TXN_REF = "VNP20261001120000-1234";
    private static final String SECRET = "callback-test-secret";

    @BeforeEach
    void setUp() {
        vnPayGateway = new VnPayGateway(
                "TESTTMN01", SECRET,
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/result");
        List<PaymentGateway> gateways = List.of(vnPayGateway);
        paymentService = new PaymentServiceImpl(
                paymentRepository, transactionRepository, bookingRepository,
                financeMapper, redisTemplate, gateways);

        booking = Booking.builder().id(1L).bookingNumber("BK99999")
                .totalAmount(BigDecimal.valueOf(1500000)).build();
        pendingPayment = Payment.builder()
                .id(10L)
                .booking(booking)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.VNPAY)
                .gatewayTxnId(TXN_REF)
                .status(PaymentStatus.PENDING)
                .build();
    }

    /** Ký callback như VNPay IPN (sign trên chuỗi đã encode) */
    private Map<String, String> signedCallback(Map<String, String> data) {
        Map<String, String> params = new LinkedHashMap<>(data);
        params.put("vnp_SecureHash", vnPayGateway.sign(vnPayGateway.buildQueryString(data)));
        params.put("vnp_SecureHashType", "SHA512");
        return params;
    }

    private Map<String, String> successData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("vnp_TxnRef", TXN_REF);
        data.put("vnp_Amount", "150000000"); // 1.500.000 × 100
        data.put("vnp_ResponseCode", "00");
        data.put("vnp_TransactionStatus", "00");
        data.put("vnp_TransactionNo", "TM1263456789");
        return data;
    }

    @Test
    @DisplayName("IPN chữ ký đúng + thành công → Payment SUCCESS, TỰ GHI reference (vnp_TransactionNo), tạo Transaction, giải phóng lock")
    void callback_ThanhCong_GhiReferenceTuDong() {
        when(paymentRepository.findByGatewayTxnId(TXN_REF)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder()
                .id(10L).status(PaymentStatus.SUCCESS).build());

        PaymentResponse response = paymentService.handleGatewayCallback(
                PaymentMethod.VNPAY, signedCallback(successData()));

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(pendingPayment.getPaidAt()).isNotNull();
        // C3: reference do WEBHOOK tự ghi — không phải FE nhập tay
        assertThat(pendingPayment.getTransactionReference()).isEqualTo("TM1263456789");
        assertThat(pendingPayment.getRawCallbackPayload()).contains("vnp_TxnRef");
        assertThat(pendingPayment.getGatewayResponseCode()).isEqualTo("00");

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getReferenceCode()).isEqualTo("TM1263456789");

        verify(redisTemplate).delete("payment:lock:booking:1");
    }

    @Test
    @DisplayName("Idempotency: IPN gọi LẦN 2 khi payment đã SUCCESS → PAYMENT_CALLBACK_DUPLICATED (8005), không update lần 2")
    void callback_Idempotent_GoiLan2_Loi8005() {
        pendingPayment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findByGatewayTxnId(TXN_REF)).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> paymentService.handleGatewayCallback(
                PaymentMethod.VNPAY, signedCallback(successData())))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_CALLBACK_DUPLICATED));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Chữ ký SAI secret → PAYMENT_SIGNATURE_INVALID (8004), KHÔNG đụng DB")
    void callback_SaiChuKy_Loi8004_KhongChamDB() {
        VnPayGateway gatewayKhacSecret = new VnPayGateway(
                "TESTTMN01", "different-secret",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/result");
        Map<String, String> params = new LinkedHashMap<>(successData());
        params.put("vnp_SecureHash", gatewayKhacSecret.sign(
                gatewayKhacSecret.buildQueryString(successData())));
        params.put("vnp_SecureHashType", "SHA512");

        assertThatThrownBy(() -> paymentService.handleGatewayCallback(PaymentMethod.VNPAY, params))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_SIGNATURE_INVALID));

        verify(paymentRepository, never()).findByGatewayTxnId(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Thiếu param bắt buộc (vnp_TxnRef) dù ký đúng → PAYMENT_SIGNATURE_INVALID (8004)")
    void callback_ThieuTxnRef_Loi8004() {
        Map<String, String> data = successData();
        data.remove("vnp_TxnRef");

        assertThatThrownBy(() -> paymentService.handleGatewayCallback(
                PaymentMethod.VNPAY, signedCallback(data)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_SIGNATURE_INVALID));
    }

    @Test
    @DisplayName("Số tiền callback ≠ số tiền thanh toán → INVALID_PAYMENT_AMOUNT (8002)")
    void callback_LechTien_Loi8002() {
        when(paymentRepository.findByGatewayTxnId(TXN_REF)).thenReturn(Optional.of(pendingPayment));
        Map<String, String> data = successData();
        data.put("vnp_Amount", "99900000"); // 999.000 ≠ 1.500.000 — ký hợp lệ

        assertThatThrownBy(() -> paymentService.handleGatewayCallback(
                PaymentMethod.VNPAY, signedCallback(data)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_PAYMENT_AMOUNT));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Gateway báo thất bại (responseCode=24) → Payment FAILED, có payload, KHÔNG tạo Transaction, vẫn giải phóng lock")
    void callback_GatewayThatBai_ThanhToanFAILED() {
        when(paymentRepository.findByGatewayTxnId(TXN_REF)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder()
                .id(10L).status(PaymentStatus.FAILED).build());
        Map<String, String> data = successData();
        data.put("vnp_ResponseCode", "24");

        PaymentResponse response = paymentService.handleGatewayCallback(
                PaymentMethod.VNPAY, signedCallback(data));

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(pendingPayment.getRawCallbackPayload()).contains("vnp_TxnRef");
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(redisTemplate).delete("payment:lock:booking:1");
    }

    @Test
    @DisplayName("Không tìm thấy Payment theo txnRef → PAYMENT_NOT_FOUND (8001)")
    void callback_KhongTimThayPayment_Loi8001() {
        when(paymentRepository.findByGatewayTxnId(TXN_REF)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.handleGatewayCallback(
                PaymentMethod.VNPAY, signedCallback(successData())))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }
}
