package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherSearchDto {

    @Schema(description = "Số trang (bắt đầu từ 0)", example = "0")
    @Builder.Default
    Integer page = 0;

    @Schema(description = "Số bản ghi mỗi trang", example = "10")
    @Builder.Default
    Integer pageSize = 10;

    @Schema(description = "Từ khóa tìm kiếm (mã voucher hoặc tiêu đề)", example = "SUMMER")
    String keyword;

    @Schema(description = "ID khách sạn lọc theo cơ sở (null = toàn chuỗi)", example = "1")
    Short hotelId;

    @Schema(description = "Loại giảm giá (PERCENT hoặc FIXED)", example = "PERCENT")
    String discountType;

    @Schema(description = "Phạm vi áp dụng (ROOM_ONLY, TOTAL_BOOKING, SERVICE_ONLY)", example = "ROOM_ONLY")
    String applyScope;

    @Schema(description = "Trạng thái voucher (ACTIVE, INACTIVE, EXPIRED)", example = "ACTIVE")
    String status;

    @Schema(description = "Thời điểm kiểm tra hiệu lực", example = "2026-06-15T00:00:00+07:00")
    OffsetDateTime activeAt;

    @Schema(description = "Cột sắp xếp", example = "id")
    @Builder.Default
    String sortBy = "id";

    @Schema(description = "Chiều sắp xếp (ASC, DESC)", example = "DESC")
    @Builder.Default
    String sortDirection = "DESC";
}

