package vn.edu.utc.hotel_booking.modules.notification.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.notification.dto.ServiceItemEmailDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    List<ServiceItemEmailDto> services = new ArrayList<>();

    @Builder.Default
    List<ServiceItemEmailDto> surcharges = new ArrayList<>();

    // Các trường dữ liệu thanh toán bổ sung cho All-in-One OTA
    Boolean isPaid;
    String paymentMethod;
    String transactionReference;
    String invoiceNumber;
    OffsetDateTime paidAt;
}
