package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscountRuleSearchDto {

    @Schema(description = "Số trang (bắt đầu từ 0)", example = "0")
    @Builder.Default
    Integer page = 0;

    @Schema(description = "Số bản ghi mỗi trang", example = "10")
    @Builder.Default
    Integer pageSize = 10;

    @Schema(description = "ID khách sạn lọc theo cơ sở", example = "1")
    Short hotelId;

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "ID chiến dịch khuyến mại", example = "1")
    Integer campaignId;

    @Schema(description = "Mã loại giảm giá (SPECIAL_CAMPAIGN, LONG_STAY...)", example = "SPECIAL_CAMPAIGN")
    String ruleTypeCode;

    @Schema(description = "Trạng thái quy tắc (ACTIVE, INACTIVE)", example = "ACTIVE")
    String status;

    @Schema(description = "Ngày cần kiểm tra hiệu lực", example = "2026-06-15")
    LocalDate activeOnDate;

    @Schema(description = "Cột sắp xếp", example = "id")
    @Builder.Default
    String sortBy = "id";

    @Schema(description = "Chiều sắp xếp (ASC, DESC)", example = "DESC")
    @Builder.Default
    String sortDirection = "DESC";
}
