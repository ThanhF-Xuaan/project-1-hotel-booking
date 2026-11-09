package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HolidayCalendarResponse {

    @Schema(description = "ID ngày lễ trong lịch", example = "1")
    Integer id;

    @Schema(description = "Tên ngày lễ", example = "Tết Dương Lịch 2026")
    String name;

    @Schema(description = "Ngày diễn ra", example = "2026-01-01")
    LocalDate date;

    @Schema(description = "Mô tả chi tiết")
    String description;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời điểm tạo")
    OffsetDateTime createdAt;
}

