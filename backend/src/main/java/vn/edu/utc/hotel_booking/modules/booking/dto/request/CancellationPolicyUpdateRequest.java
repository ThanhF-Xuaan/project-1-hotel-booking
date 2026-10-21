package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.dto.policy.CancellationTierDto;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationFeeType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancellationPolicyUpdateRequest {

    @NotBlank(message = "Tên chính sách không được để trống")
    @Size(max = 150, message = "Tên chính sách tối đa 150 ký tự")
    String name;

    @NotNull(message = "Loại booking không được để trống")
    BookingType bookingType;

    String description;

    @NotNull(message = "Số ngày hủy miễn phí không được để trống")
    @Min(value = 0, message = "Số ngày hủy miễn phí phải >= 0")
    Integer freeCancellationBeforeDays;

    @NotNull(message = "Loại phí hủy không được để trống")
    CancellationFeeType cancellationFeeType;

    @DecimalMin(value = "0.00", message = "Tỷ lệ phạt mặc định phải >= 0.00")
    @DecimalMax(value = "100.00", message = "Tỷ lệ phạt mặc định phải <= 100.00")
    BigDecimal defaultPenaltyPercent;

    @Builder.Default
    List<CancellationTierDto> tiers = new ArrayList<>();

    Boolean isActive;
}
