package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherResponse {

    @Schema(description = "ID voucher", example = "1")
    Integer id;

    @Schema(description = "ID khách sạn áp dụng (null = Toàn chuỗi)", example = "1")
    Short hotelId;

    @Schema(description = "Tên khách sạn áp dụng", example = "Viettel Luxury Hà Nội")
    String hotelName;

    @Schema(description = "Mã voucher", example = "SUMMER2026")
    String voucherCode;

    @Schema(description = "Tiêu đề voucher", example = "Ưu đãi mùa hè 2026")
    String title;

    @Schema(description = "Mô tả chi tiết", example = "Giảm 15% cho mọi hạng phòng")
    String description;

    @Schema(description = "Loại giảm giá (PERCENT hoặc FIXED)", example = "PERCENT")
    String discountType;

    @Schema(description = "Giá trị giảm giá", example = "15.00")
    BigDecimal discountValue;

    @Schema(description = "Phạm vi áp dụng (ROOM_ONLY, TOTAL_BOOKING, SERVICE_ONLY)", example = "ROOM_ONLY")
    String applyScope;

    @Schema(description = "Giá trị đơn hàng tối thiểu", example = "500000.00")
    BigDecimal minOrderAmount;

    @Schema(description = "Mức giảm tối đa", example = "200000.00")
    BigDecimal maxDiscountAmount;

    @Schema(description = "Tổng số lượng phát hành", example = "100")
    Integer totalQuantity;

    @Schema(description = "Số lượng đã sử dụng", example = "45")
    Integer usedQuantity;

    @Schema(description = "Số lượng còn lại", example = "55")
    Integer remainingQuantity;

    @Schema(description = "Số lượt dùng tối đa mỗi khách", example = "1")
    Short maxUsagePerUser;

    @Schema(description = "Thời gian bắt đầu hiệu lực", example = "2026-06-01T00:00:00+07:00")
    OffsetDateTime validFrom;

    @Schema(description = "Thời gian kết thúc hiệu lực", example = "2026-08-31T23:59:59+07:00")
    OffsetDateTime validTo;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời điểm tạo", example = "2026-06-01T10:00:00+07:00")
    OffsetDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật", example = "2026-06-01T10:00:00+07:00")
    OffsetDateTime updatedAt;
}

