package vn.edu.utc.hotel_booking.modules.notification.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingConfirmationEmailEvent {

    Long bookingId;
    String bookingCode;
    String recipientEmail;
    String customerName;
    String hotelName;
    String hotelAddress;
    String hotelPhone;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    String roomTypeName;
    Integer totalRooms;
    Integer totalGuests;
    BigDecimal subtotalAmount;
    BigDecimal serviceFeeAmount;
    BigDecimal vatAmount;
    BigDecimal totalAmount;
    String currency;
    String portalUrl;
}
