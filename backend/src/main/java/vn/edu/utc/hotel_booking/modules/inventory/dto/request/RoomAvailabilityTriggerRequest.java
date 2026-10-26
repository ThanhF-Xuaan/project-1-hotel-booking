package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Yêu cầu kích hoạt thủ công sinh tồn kho buồng phòng")
public class RoomAvailabilityTriggerRequest {

    @Schema(description = "Ngày bắt đầu sinh tồn kho (để trống sẽ mặc định là ngày hôm nay)", example = "2026-11-01")
    LocalDate startDate;

    @Min(value = 1, message = "Số ngày mở bán tối thiểu là 1 ngày")
    @Max(value = 730, message = "Số ngày mở bán tối đa là 730 ngày (2 năm)")
    @Builder.Default
    @Schema(description = "Số ngày mở bán cần sinh (mặc định 365 ngày, tối đa 730 ngày)", example = "365")
    Integer horizonDays = 365;
}
