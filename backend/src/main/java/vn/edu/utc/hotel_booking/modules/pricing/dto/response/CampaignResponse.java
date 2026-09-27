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
public class CampaignResponse {

    @Schema(description = "ID chiến dịch", example = "1")
    Integer id;

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "Tên khách sạn", example = "Grand Hotel Da Nang")
    String hotelName;

    @Schema(description = "Tên chiến dịch", example = "Chào hè sôi động 2026")
    String name;

    @Schema(description = "Mô tả chiến dịch")
    String description;

    @Schema(description = "Ngày bắt đầu", example = "2026-06-01")
    LocalDate startDate;

    @Schema(description = "Ngày kết thúc", example = "2026-08-31")
    LocalDate endDate;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
