package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPenaltyType;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoShowPolicyUpdateRequest {

    @NotBlank(message = "Tên chính sách không được để trống")
    @Size(max = 150, message = "Tên chính sách tối đa 150 ký tự")
    String name;

    @NotNull(message = "Loại booking không được để trống")
    BookingType bookingType;

    String description;

    @NotNull(message = "Giờ cắt phòng không được để trống")
    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime releaseRoomTime;

    @NotNull(message = "Loại phạt No-Show không được để trống")
    NoShowPenaltyType penaltyType;

    @NotNull(message = "Giá trị phạt không được để trống")
    @Positive(message = "Giá trị phạt phải lớn hơn 0")
    BigDecimal penaltyValue;

    Boolean isActive;
}
