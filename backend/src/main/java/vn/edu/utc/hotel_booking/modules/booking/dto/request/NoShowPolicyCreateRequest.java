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
public class NoShowPolicyCreateRequest {

    @NotBlank(message = "Mã chính sách No-Show không được để trống")
    @Size(max = 50, message = "Mã chính sách tối đa 50 ký tự")
    String policyCode;

    @NotBlank(message = "Tên chính sách không được để trống")
    @Size(max = 150, message = "Tên chính sách tối đa 150 ký tự")
    String name;

    @NotNull(message = "Loại booking không được để trống")
    @Builder.Default
    BookingType bookingType = BookingType.ALL;

    String description;

    @NotNull(message = "Giờ cắt phòng không được để trống")
    @JsonFormat(pattern = "HH:mm:ss")
    @Builder.Default
    LocalTime releaseRoomTime = LocalTime.of(23, 59, 0);

    @NotNull(message = "Loại phạt No-Show không được để trống")
    @Builder.Default
    NoShowPenaltyType penaltyType = NoShowPenaltyType.PERCENT_FIRST_NIGHT_RATE;

    @NotNull(message = "Giá trị phạt không được để trống")
    @Positive(message = "Giá trị phạt phải lớn hơn 0")
    @Builder.Default
    BigDecimal penaltyValue = new BigDecimal("100.00");

    @Builder.Default
    Boolean isActive = true;
}
