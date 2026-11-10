package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscountConditionDto {

    @JsonProperty("minNights")
    @JsonAlias({"min_nights", "minNights"})
    @Schema(description = "Số đêm tối thiểu đơn lẻ (nếu không dùng mảng tiers)", example = "3")
    Integer minNights;

    @JsonProperty("maxNights")
    @JsonAlias({"max_nights", "maxNights"})
    @Schema(description = "Số đêm tối đa đơn lẻ (nếu không dùng mảng tiers)", example = "7")
    Integer maxNights;

    @JsonProperty("minAdvanceDays")
    @JsonAlias({"min_advance_days", "minAdvanceDays"})
    @Schema(description = "Số ngày đặt trước tối thiểu (dành cho EARLY_BIRD)", example = "30")
    Integer minAdvanceDays;

    @JsonProperty("maxAdvanceDays")
    @JsonAlias({"max_advance_days", "maxAdvanceDays"})
    @Schema(description = "Số ngày đặt trước tối đa (dành cho LAST_MINUTE)", example = "1")
    Integer maxAdvanceDays;

    @JsonProperty("tiers")
    @Builder.Default
    @Schema(description = "Danh sách các bậc phân tầng giảm giá theo số đêm lưu trú")
    List<DiscountTierDto> tiers = new ArrayList<>();
}
