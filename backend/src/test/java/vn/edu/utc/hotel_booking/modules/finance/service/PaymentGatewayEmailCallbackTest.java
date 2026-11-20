package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;
import vn.edu.utc.hotel_booking.modules.finance.entity.Payment;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentMethod;
import vn.edu.utc.hotel_booking.modules.finance.entity.PaymentStatus;
import vn.edu.utc.hotel_booking.modules.notification.event.BookingConfirmationEmailEvent;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentGatewayEmailCallbackTest {

    @Test
    @DisplayName("Khi VNPay thanh toán SUCCESS, phát 1 sự kiện BookingConfirmationEmailEvent All-in-One")
    void shouldPublishSingleAllInOneEmailWhenPaymentSuccess() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        BookingGuest guest = BookingGuest.builder()
                .fullName("Nguyễn Văn A")
                .email("nva@gmail.com")
                .phone("0987654321")
                .build();

        Booking booking = Booking.builder()
                .id(100L)
                .bookingNumber("BK100")
                .guest(guest)
                .totalAmount(new BigDecimal("1500000.00"))
                .build();

        Payment payment = Payment.builder()
                .id(50L)
                .booking(booking)
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.SUCCESS)
                .transactionReference("15738002")
                .paidAt(OffsetDateTime.now())
                .totalAmount(new BigDecimal("1500000.00"))
                .build();

        BookingConfirmationEmailEvent expectedEvent = BookingConfirmationEmailEvent.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingNumber())
                .recipientEmail(guest.getEmail())
                .customerName(guest.getFullName())
                .totalAmount(booking.getTotalAmount())
                .isPaid(true)
                .paymentMethod(payment.getPaymentMethod().name())
                .transactionReference(payment.getTransactionReference())
                .paidAt(payment.getPaidAt())
                .build();

        eventPublisher.publishEvent(expectedEvent);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        Object published = captor.getValue();
        assertThat(published).isInstanceOf(BookingConfirmationEmailEvent.class);
        BookingConfirmationEmailEvent event = (BookingConfirmationEmailEvent) published;
        assertThat(event.getIsPaid()).isTrue();
        assertThat(event.getPaymentMethod()).isEqualTo("VNPAY");
        assertThat(event.getTransactionReference()).isEqualTo("15738002");
    }
}
