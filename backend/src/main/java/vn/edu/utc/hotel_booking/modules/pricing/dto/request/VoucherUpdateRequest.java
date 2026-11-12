package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherUpdateRequest {

    @Schema(description = "ID khách sạn áp dụng (null = Toàn chuỗi áp dụng)", example = "1")
    Short hotelId;

    @NotBlank(message = "Tiêu đề voucher không được để trống")
    @Size(max = 150, message = "Tiêu đề voucher tối đa 150 ký tự")
    @Schema(description = "Tiêu đề voucher", example = "Ưu đãi mùa hè 2026 - Cập nhật")
    String title;

    @Schema(description = "Mô tả chi tiết chương trình ưu đãi", example = "Giảm 15% cho mọi hạng phòng")
    String description;

    @NotBlank(message = "Loại giảm giá không được để trống (PERCENT hoặc FIXED)")
    @Pattern(regexp = "^(PERCENT|FIXED)$", message = "Loại giảm giá phải là PERCENT hoặc FIXED")
    @Schema(description = "Loại giảm giá: PERCENT hoặc FIXED", example = "PERCENT")
    String discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @Positive(message = "Giá trị giảm giá phải lớn hơn 0")
    @Schema(description = "Giá trị giảm giá (% hoặc số tiền)", example = "15.00")
    BigDecimal discountValue;

    @NotBlank(message = "Phạm vi áp dụng không được để trống")
    @Pattern(regexp = "^(ROOM_ONLY|TOTAL_BOOKING|SERVICE_ONLY)$", message = "Phạm vi áp dụng phải là ROOM_ONLY, TOTAL_BOOKING hoặc SERVICE_ONLY")
    @Schema(description = "Phạm vi áp dụng", example = "ROOM_ONLY")
    String applyScope;

    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng tối thiểu không được âm")
    @Schema(description = "Giá trị đơn hàng tối thiểu để áp dụng", example = "500000.00")
    BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0", message = "Mức giảm tối đa không được âm")
    @Schema(description = "Mức giảm tối đa nếu discount_type là PERCENT", example = "200000.00")
    BigDecimal maxDiscountAmount;

    @NotNull(message = "Tổng số lượng phát hành không được để trống")
    @Min(value = 1, message = "Tổng số lượng phát hành tối thiểu là 1")
    @Schema(description = "Tổng số lượng voucher phát hành", example = "150")
    Integer totalQuantity;

    @NotNull(message = "Số lượt dùng tối đa mỗi khách không được để trống")
    @Min(value = 1, message = "Số lượt dùng tối đa mỗi khách tối thiểu là 1")
    @Schema(description = "Số lượt dùng tối đa mỗi khách", example = "2")
    Short maxUsagePerUser;

    @NotNull(message = "Thời gian bắt đầu hiệu lực không được để trống")
    @Schema(description = "Thời gian bắt đầu hiệu lực", example = "2026-06-01T00:00:00+07:00")
    OffsetDateTime validFrom;

    @NotNull(message = "Thời gian kết thúc hiệu lực không được để trống")
    @Schema(description = "Thời gian kết thúc hiệu lực", example = "2026-08-31T23:59:59+07:00")
    OffsetDateTime validTo;

    @NotBlank(message = "Trạng thái voucher không được để trống")
    @Pattern(regexp = "^(ACTIVE|INACTIVE|EXPIRED)$", message = "Trạng thái phải là ACTIVE, INACTIVE hoặc EXPIRED")
    @Schema(description = "Trạng thái voucher", example = "ACTIVE")
    String status;
}

