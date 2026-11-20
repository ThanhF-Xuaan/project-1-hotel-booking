package vn.edu.utc.hotel_booking.modules.notification.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BookingConfirmationEmailEventTest {

    @Test
    @DisplayName("Kiểm tra builder khởi tạo đầy đủ trường đặt phòng và biên lai thanh toán All-in-One")
    void shouldBuildCompleteConfirmationEventWithPaymentInfo() {
        OffsetDateTime now = OffsetDateTime.now();
        BookingConfirmationEmailEvent event = BookingConfirmationEmailEvent.builder()
                .bookingId(101L)
                .bookingCode("BK1795156347565907")
                .recipientEmail("guest@smartstay.space")
                .customerName("Nguyễn Văn A")
                .hotelName("SmartStay Hà Nội Grand")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .roomTypeName("Deluxe King City View")
                .totalRooms(1)
                .totalGuests(2)
                .totalAmount(new BigDecimal("2821600.00"))
                .currency("VND")
                .portalUrl("https://smartstay.space/my-bookings")
                .isPaid(true)
                .paymentMethod("VNPAY")
                .transactionReference("15738002")
                .invoiceNumber("INV1795156347565907")
                .paidAt(now)
                .build();

        assertThat(event.getBookingCode()).isEqualTo("BK1795156347565907");
        assertThat(event.getIsPaid()).isTrue();
        assertThat(event.getPaymentMethod()).isEqualTo("VNPAY");
        assertThat(event.getTransactionReference()).isEqualTo("15738002");
        assertThat(event.getInvoiceNumber()).isEqualTo("INV1795156347565907");
        assertThat(event.getPaidAt()).isEqualTo(now);
        assertThat(event.getPortalUrl()).isEqualTo("https://smartstay.space/my-bookings");
    }
}

