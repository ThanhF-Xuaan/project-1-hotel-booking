package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscountTierDto {

    @NotNull(message = "Số đêm tối thiểu không được để trống")
    @Positive(message = "Số đêm tối thiểu phải lớn hơn hoặc bằng 1")
    @JsonProperty("minNights")
    @JsonAlias({"min_nights", "minNights"})
    @Schema(description = "Số đêm lưu trú tối thiểu", example = "3")
    Integer minNights;

    @JsonProperty("maxNights")
    @JsonAlias({"max_nights", "maxNights"})
    @Schema(description = "Số đêm lưu trú tối đa (Để trống nếu không giới hạn / từ X đêm trở lên)", example = "6")
    Integer maxNights;

    @JsonProperty("discountType")
    @JsonAlias({"discount_type", "discountType"})
    @Builder.Default
    @Schema(description = "Loại chiết khấu: PERCENT hoặc FIXED", example = "PERCENT")
    String discountType = "PERCENT";

    @NotNull(message = "Giá trị chiết khấu không được để trống")
    @Positive(message = "Giá trị chiết khấu phải lớn hơn 0")
    @JsonProperty("discountValue")
    @JsonAlias({"discount_value", "discountValue"})
    @Schema(description = "Giá trị chiết khấu", example = "10.00")
    BigDecimal discountValue;
}
