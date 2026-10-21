package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalkInQuoteRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    Short hotelId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    LocalDate checkOutDate;

    LocalTime actualCheckInTime;

    @NotEmpty(message = "Danh sách phòng không được để trống")
    @Size(max = 5, message = "Đơn đặt phòng walk-in tối đa 5 phòng")
    @Valid
    @Builder.Default
    List<WalkInQuoteRoomItemRequest> rooms = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WalkInQuoteRoomItemRequest {
        @NotNull(message = "Loại phòng không được để trống")
        Integer hotelRoomTypeId;

        @Builder.Default
        Short adultCount = 1;

        @Builder.Default
        Short childCount = 0;

        @Builder.Default
        Short infantCount = 0;

        @Builder.Default
        Short extraBeds = 0;
    }
}
