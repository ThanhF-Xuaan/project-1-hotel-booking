package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricingRuleResponse {

    @Schema(description = "ID quy tắc giá", example = "1")
    Integer id;

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Tên loại phòng", example = "Phòng Deluxe Hướng Biển")
    String roomTypeName;

    @Schema(description = "ID ngày lễ tết", example = "1")
    Integer holidayCalendarId;

    @Schema(description = "Mã loại quy tắc", example = "SEASONAL")
    String ruleTypeCode;

    @Schema(description = "Tên loại quy tắc", example = "Giá mùa cao điểm")
    String ruleTypeName;

    @Schema(description = "Loại điều chỉnh (PERCENT, FIXED)", example = "PERCENT")
    String adjustmentType;

    @Schema(description = "Giá trị điều chỉnh", example = "15.00")
    BigDecimal adjustmentValue;

    @Schema(description = "Ngày bắt đầu áp dụng", example = "2026-06-01")
    LocalDate startDate;

    @Schema(description = "Ngày kết thúc áp dụng", example = "2026-08-31")
    LocalDate endDate;

    @Schema(description = "Trạng thái quy tắc", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
