package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {

    Long id;
    Short hotelId;
    String hotelName;
    Long guestId;
    String guestName;
    String guestPhone;
    Long companyId;
    String companyName;
    BookingType bookingType;
    String bookingNumber;
    BigDecimal subtotalAmount;
    BigDecimal serviceFeeRate;
    BigDecimal serviceFeeAmount;
    BigDecimal totalVatAmount;
    BigDecimal totalAmount;
    BookingStatus status;
    OffsetDateTime issuedAt;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    @Builder.Default
    List<BookingDetailResponse> bookingDetails = new ArrayList<>();
}
