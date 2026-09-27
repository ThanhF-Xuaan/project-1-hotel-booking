package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingChargeType;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingGuestType;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreateRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    Short hotelId;

    @NotNull(message = "ID khách hàng không được để trống")
    Long guestId;

    Long companyId;

    @Builder.Default
    BookingType bookingType = BookingType.FIT;

    @Builder.Default
    BigDecimal serviceFeeRate = BigDecimal.ZERO;

    @NotEmpty(message = "Danh sách phòng đặt không được để trống")
    @Valid
    @Builder.Default
    List<RoomItemRequest> rooms = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomItemRequest {
        @NotNull(message = "Loại phòng không được để trống")
        Integer hotelRoomTypeId;

        @NotNull(message = "Ngày nhận phòng không được để trống")
        LocalDate checkInDate;

        @NotNull(message = "Ngày trả phòng không được để trống")
        LocalDate checkOutDate;

        @Builder.Default
        Short quantity = 1;

        @Builder.Default
        Short adultCount = 1;

        @Builder.Default
        Short childCount = 0;

        @Builder.Default
        Short infantCount = 0;

        // Cho trường hợp Front desk walk-in chọn trực tiếp phòng vật lý
        Integer roomInstanceId;

        @Builder.Default
        List<GuestItemRequest> guests = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GuestItemRequest {
        String firstName;
        String lastName;
        String fullName;
        LocalDate birthDate;

        @Builder.Default
        BookingGuestType guestType = BookingGuestType.ADULT;

        String identityType;
        String identityNumber;
    }
}
