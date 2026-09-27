package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.PaymentResponse;
import vn.edu.utc.hotel_booking.modules.finance.entity.*;
import vn.edu.utc.hotel_booking.modules.finance.mapper.FinanceMapper;
import vn.edu.utc.hotel_booking.modules.finance.repository.PaymentRepository;
import vn.edu.utc.hotel_booking.modules.finance.repository.TransactionRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.PaymentServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

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
}
