package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherValidateRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Schema(description = "Mã voucher cần kiểm tra", example = "SUMMER2026")
    String voucherCode;

    @Schema(description = "ID khách sạn cơ sở", example = "1")
    Short hotelId;

    @NotNull(message = "Số tiền đơn hàng không được để trống")
    @DecimalMin(value = "0.0", message = "Số tiền đơn hàng không được âm")
    @Schema(description = "Số tiền đơn hàng / tiền phòng trước chiết khấu", example = "1200000.00")
    BigDecimal orderAmount;

    @Schema(description = "Phạm vi áp dụng (ROOM_ONLY, TOTAL_BOOKING, SERVICE_ONLY)", example = "ROOM_ONLY")
    @Builder.Default
    String applyScope = "ROOM_ONLY";

    @Schema(description = "ID khách lưu trú (nếu có để kiểm tra max_usage_per_user)", example = "10")
    Long bookingGuestId;
}

