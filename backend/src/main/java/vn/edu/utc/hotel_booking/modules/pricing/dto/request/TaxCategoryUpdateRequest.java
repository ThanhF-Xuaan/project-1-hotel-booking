package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaxCategoryUpdateRequest {

    @NotBlank(message = "Tên nhóm thuế không được để trống")
    @Size(max = 150, message = "Tên nhóm thuế tối đa 150 ký tự")
    @Schema(description = "Tên nhóm thuế", example = "Thuế dịch vụ lưu trú")
    String categoryName;

    @Schema(description = "Mô tả chi tiết nhóm thuế")
    String description;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái chỉ nhận ACTIVE hoặc INACTIVE")
    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}

