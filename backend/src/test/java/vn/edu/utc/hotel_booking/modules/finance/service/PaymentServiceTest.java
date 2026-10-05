package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.gateway.PaymentGateway;
import vn.edu.utc.hotel_booking.modules.finance.gateway.VnPayGateway;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.CreatePaymentRequest;
import vn.edu.utc.hotel_booking.modules.finance.gateway.dto.PaymentUrlResponse;
import vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.TransactionRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.PaymentServiceImpl;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock BookingRepository bookingRepository;
    @Mock FinanceMapper financeMapper;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock List<PaymentGateway> paymentGateways;

    @InjectMocks
    PaymentServiceImpl paymentService;

    private Booking testBooking;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testBooking = Booking.builder()
                .id(1L)
                .bookingNumber("BK99999")
                .totalAmount(BigDecimal.valueOf(1500000))
                .build();

        testPayment = Payment.builder()
                .id(10L)
                .booking(testBooking)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.CASH)
                .status(PaymentStatus.SUCCESS)
                .build();
    }

    @Test
    @DisplayName("Thanh toán tiền mặt thành công tạo Payment SUCCESS và ghi Transaction PAYMENT")
    void createPayment_Cash_ImmediateSuccess() {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(1L)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.CASH)
                .paymentPurpose(PaymentPurpose.FULL_PAYMENT)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder()
                .id(10L)
                .bookingId(1L)
                .status(PaymentStatus.SUCCESS)
                .totalAmount(BigDecimal.valueOf(1500000))
                .build());

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Hoàn tiền thành công cho giao dịch thanh toán đã thành công")
    void refundPayment_Success() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder()
                .id(10L)
                .status(PaymentStatus.REFUNDED)
                .build());

        PaymentResponse response = paymentService.refundPayment(10L, BigDecimal.valueOf(500000), "Khách trả phòng sớm");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Hoàn tiền vượt quá tổng tiền thanh toán sẽ báo lỗi")
    void refundPayment_AmountExceeds_ThrowsException() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(testPayment));

        assertThatThrownBy(() -> paymentService.refundPayment(10L, BigDecimal.valueOf(2000000), "Lỗi tính"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_PAYMENT_AMOUNT));
    }

    // ==================== GIAI ĐOẠN B — validate reference theo method ====================

    @Test
    @DisplayName("B1 happy: Bank transfer có mã giao dịch → PENDING, giữ nguyên mã, không tạo Transaction")
    void createPayment_Bank_WithReference_Pending() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(10L).build());

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(1L)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .transactionReference("  FT260930001  ")
                .build();

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response).isNotNull();
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(paymentCaptor.getValue().getTransactionReference()).isEqualTo("FT260930001"); // đã trim
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("B1 validation: Bank transfer thiếu mã giao dịch → PAYMENT_REFERENCE_REQUIRED (8007)")
    void createPayment_Bank_MissingReference_Throws() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(1L)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.PAYMENT_REFERENCE_REQUIRED));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("B1 validation: approval code chỉ toàn khoảng trắng vẫn bị coi là thiếu (8007)")
    void createPayment_Card_BlankReference_Throws() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(1L)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionReference("   ")
                .build();

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.PAYMENT_REFERENCE_REQUIRED));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("B1 edge: FE gửi tay reference cho VNPAY → bỏ qua (để webhook tự ghi), status PENDING")
    void createPayment_Vnpay_HandEnteredReference_Ignored() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(10L).build());

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(1L)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionReference("FE-NGAY-TAY-123")
                .build();

        paymentService.createPayment(request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getTransactionReference()).isNull();
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("B2: CREDIT_CARD nhập approval code → SUCCESS ngay, Transaction lấy đúng approval code")
    void createPayment_CreditCard_ApprovalCode_ImmediateSuccess() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(10L).build());

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(1L)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionReference("AP778899")
                .build();

        paymentService.createPayment(request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paymentCaptor.getValue().getPaidAt()).isNotNull();

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getReferenceCode()).isEqualTo("AP778899");
    }

    @Test
    @DisplayName("B3: CASH có số phiếu thu → Transaction.referenceCode = số phiếu thu (mã thật)")
    void createPayment_Cash_WithReceipt_UsesReceiptAsReferenceCode() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(10L).build());

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(1L)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.CASH)
                .transactionReference("PT-001")
                .build();

        paymentService.createPayment(request);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getReferenceCode()).isEqualTo("PT-001");
    }

    @Test
    @DisplayName("B3: CASH không có phiếu thu → fallback mã hệ thống TX-<timestamp>")
    void createPayment_Cash_WithoutReference_FallsBackTxCode() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(10L).build());

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .bookingId(1L)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        paymentService.createPayment(request);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getReferenceCode()).startsWith("TX-");
    }

    @Test
    @DisplayName("B3: completePayment cho bank transfer → Transaction dùng mã giao dịch NH thật (FT26...)")
    void completePayment_PendingBank_UsesRealReferenceCode() {
        Payment pendingBankPayment = Payment.builder()
                .id(11L)
                .booking(testBooking)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .transactionReference("FT261234567")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(11L)).thenReturn(Optional.of(pendingBankPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(11L).build());

        paymentService.completePayment(11L);

        assertThat(pendingBankPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getReferenceCode()).isEqualTo("FT261234567");
    }

    // ==================== GIAI ĐOẠN C — createGatewayPayment (Redis lock TTL 10') ====================

    @Test
    @DisplayName("C2 happy: tạo phiên VNPay — lock Redis TTL 10', lưu gatewayTxnId, trả paymentUrl + expiresAt")
    void createGatewayPayment_VnPay_HappyPath() {
        VnPayGateway realGateway = new VnPayGateway(
                "TESTTMN01", "test-secret",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/result");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(paymentGateways.stream()).thenReturn(Stream.of(realGateway));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .bookingId(1L)
                .method(PaymentMethod.VNPAY)
                .build();

        PaymentUrlResponse response = paymentService.createGatewayPayment(request, "127.0.0.1");

        assertThat(response.getTxnRef()).startsWith("VNP");
        assertThat(response.getPaymentUrl()).contains("vnp_TxnRef=", response.getTxnRef());
        assertThat(response.getExpiresAt()).isAfter(OffsetDateTime.now().plusMinutes(9)); // TTL 10'

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(paymentCaptor.getValue().getGatewayTxnId()).isEqualTo(response.getTxnRef());
        // Khoá xong thì chỉ khi callback mới mở — không tạo Transaction ngay
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("C2 lock: phiên thanh toán đang diễn ra (lock Redis còn hạn) → PAYMENT_CALLBACK_DUPLICATED (8005)")
    void createGatewayPayment_LockDangGiu_Loi8005() {
        VnPayGateway realGateway = new VnPayGateway(
                "TESTTMN01", "test-secret",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/result");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(paymentGateways.stream()).thenReturn(Stream.of(realGateway));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .bookingId(1L)
                .method(PaymentMethod.VNPAY)
                .build();

        assertThatThrownBy(() -> paymentService.createGatewayPayment(request, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_CALLBACK_DUPLICATED));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("C2 validate: gửi CASH vào endpoint gateway → INVALID_REQUEST_DATA (1002), không đụng booking")
    void createGatewayPayment_MethodKhongPhaiOnline_Loi1002() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .bookingId(1L)
                .method(PaymentMethod.CASH)
                .build();

        assertThatThrownBy(() -> paymentService.createGatewayPayment(request, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST_DATA));

        verify(bookingRepository, never()).findById(any());
    }

    // ==================== §6.3 — cancelPayment (PENDING → CANCELLED) cho flow lỗi FE ====================

    @Test
    @DisplayName("§6.3: cancel thanh toán PENDING → CANCELLED + giải phóng lock Redis")
    void cancelPayment_Pending_ThanhCong() {
        Payment pendingPayment = Payment.builder()
                .id(12L)
                .booking(testBooking)
                .totalAmount(BigDecimal.valueOf(1500000))
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(12L)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(financeMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder()
                .id(12L).status(PaymentStatus.CANCELLED).build());

        PaymentResponse response = paymentService.cancelPayment(12L);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(redisTemplate).delete("payment:lock:booking:1");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("§6.3: cancel thanh toán đã SUCCESS → PAYMENT_NOT_PENDING (8006)")
    void cancelPayment_DaThanhCong_Loi8006() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(testPayment)); // status SUCCESS

        assertThatThrownBy(() -> paymentService.cancelPayment(10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_NOT_PENDING));

        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
