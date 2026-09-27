package vn.edu.utc.hotel_booking.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseSearchDto {

    @Schema(description = "Số trang (bắt đầu từ 0)", example = "0")
    @Min(value = 0, message = "Số trang không được âm")
    @Builder.Default
    Integer page = 0;

    @Schema(description = "Kích thước trang (số bản ghi / trang)", example = "10")
    @Min(value = 1, message = "Kích thước trang tối thiểu là 1")
    @Max(value = 100, message = "Kích thước trang tối đa là 100")
    @Builder.Default
    Integer pageSize = 10;

    @Schema(description = "Trường sắp xếp", example = "createdAt")
    @Builder.Default
    String sortBy = "createdAt";

    @Schema(description = "Hướng sắp xếp: ASC hoặc DESC", example = "DESC")
    @Builder.Default
    String sortDirection = "DESC";
}
