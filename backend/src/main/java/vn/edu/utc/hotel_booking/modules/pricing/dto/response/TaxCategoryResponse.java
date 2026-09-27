package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaxCategoryResponse {

    @Schema(description = "ID nhóm thuế", example = "1")
    Integer id;

    @Schema(description = "Mã nhóm thuế", example = "ROOM_TAX")
    String categoryCode;

    @Schema(description = "Tên nhóm thuế", example = "Thuế dịch vụ lưu trú")
    String categoryName;

    @Schema(description = "Mô tả")
    String description;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
