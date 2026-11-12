package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherValidationResponse {

    @Schema(description = "Trạng thái hợp lệ của voucher", example = "true")
    Boolean valid;

    @Schema(description = "Thông báo kết quả xác thực", example = "Áp dụng mã voucher thành công")
    String message;

    @Schema(description = "Mã voucher", example = "SUMMER2026")
    String voucherCode;

    @Schema(description = "Tiêu đề voucher", example = "Ưu đãi mùa hè 2026")
    String title;

    @Schema(description = "Mô tả chi tiết", example = "Giảm 15% cho mọi hạng phòng")
    String description;

    @Schema(description = "Loại giảm giá (PERCENT hoặc FIXED)", example = "PERCENT")
    String discountType;

    @Schema(description = "Giá trị cấu hình của voucher", example = "15.00")
    BigDecimal discountValue;

    @Schema(description = "Số tiền được giảm thực tế", example = "180000.00")
    BigDecimal discountAmount;

    @Schema(description = "Số tiền phải trả sau khi giảm", example = "1020000.00")
    BigDecimal finalAmount;

    @Schema(description = "Phạm vi áp dụng (ROOM_ONLY, TOTAL_BOOKING, SERVICE_ONLY)", example = "ROOM_ONLY")
    String applyScope;
}

