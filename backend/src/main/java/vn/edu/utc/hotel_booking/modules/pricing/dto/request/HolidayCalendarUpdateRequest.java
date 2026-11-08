package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HolidayCalendarUpdateRequest {

    @NotBlank(message = "Tên ngày lễ không được để trống")
    @Size(max = 150, message = "Tên ngày lễ tối đa 150 ký tự")
    @Schema(description = "Tên ngày lễ, sự kiện", example = "Tết Dương Lịch 2026")
    String name;

    @NotNull(message = "Ngày lễ không được để trống")
    @Schema(description = "Ngày diễn ra", example = "2026-01-01")
    LocalDate date;

    @Schema(description = "Mô tả chi tiết ngày lễ")
    String description;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái chỉ nhận ACTIVE hoặc INACTIVE")
    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}

