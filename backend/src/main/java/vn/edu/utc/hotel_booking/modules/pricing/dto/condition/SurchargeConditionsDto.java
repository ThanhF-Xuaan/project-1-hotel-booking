package vn.edu.utc.hotel_booking.modules.pricing.dto.condition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SurchargeConditionsDto {

    @JsonProperty("standard_time")
    @Schema(description = "Giờ nhận/trả phòng quy chuẩn của cơ sở (do server tự động gán từ Hotel)", example = "14:00")
    String standardTime;

    @Valid
    @JsonProperty("tiers")
    @Builder.Default
    List<SurchargeTierDto> tiers = new ArrayList<>();
}
