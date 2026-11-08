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
public class TaxCategoryCreateRequest {

    @NotBlank(message = "Mã nhóm thuế không được để trống")
    @Size(min = 3, max = 50, message = "Mã nhóm thuế từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã nhóm thuế chỉ gồm chữ hoa, chữ số và dấu gạch dưới")
    @Schema(description = "Mã nhóm thuế", example = "ROOM_TAX")
    String categoryCode;

    @NotBlank(message = "Tên nhóm thuế không được để trống")
    @Size(max = 150, message = "Tên nhóm thuế tối đa 150 ký tự")
    @Schema(description = "Tên nhóm thuế", example = "Thuế dịch vụ lưu trú")
    String categoryName;

    @Schema(description = "Mô tả chi tiết nhóm thuế")
    String description;
}
