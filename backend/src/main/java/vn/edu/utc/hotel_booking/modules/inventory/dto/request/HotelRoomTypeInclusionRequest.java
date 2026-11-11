package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.inventory.entity.InclusionFrequency;
import vn.edu.utc.hotel_booking.modules.inventory.entity.InclusionTargetType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeInclusionRequest {

    Integer menuId;
    Integer masterItemId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    @Builder.Default
    Integer quantity = 1;

    @NotNull(message = "Tần suất áp dụng không được để trống")
    @Builder.Default
    InclusionFrequency frequency = InclusionFrequency.PER_NIGHT;

    @NotNull(message = "Đối tượng áp dụng không được để trống")
    @Builder.Default
    InclusionTargetType targetType = InclusionTargetType.PER_ROOM;

    String customDisplayName;
    String description;
}

