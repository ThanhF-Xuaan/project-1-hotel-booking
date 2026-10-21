package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingChargeType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SurchargeProposalResponse {

    @Schema(description = "Có áp dụng phụ thu theo mốc giờ hay không")
    boolean applicable;

    @Schema(description = "Loại phụ thu đề xuất")
    BookingChargeType chargeType;

    @Schema(description = "Giờ quy định chuẩn của khách sạn (VD: 14:00 đối với check-in, 12:00 đối với check-out)")
    String standardTime;

    @Schema(description = "Mốc thời gian thực tế xem xét")
    OffsetDateTime actualTime;

    @Schema(description = "Chênh lệch thời gian tính bằng phút (âm: sớm hơn, dương: muộn hơn)")
    Long timeDifferenceMinutes;

    @Schema(description = "Tên quy tắc chính sách áp dụng")
    String policyRule;

    @Schema(description = "Đơn giá phòng cơ sở dùng để tính phụ thu (1 đêm)")
    BigDecimal baseRoomRate;

    @Schema(description = "Tỷ lệ phụ thu đề xuất (%)")
    BigDecimal suggestedPercentage;

    @Schema(description = "Số tiền phụ thu đề xuất (VNĐ)")
    BigDecimal suggestedAmount;

    @Schema(description = "Tên khoản mục phụ thu đề xuất")
    String suggestedItemName;

    @Schema(description = "Phí dịch vụ mặc định (%)")
    BigDecimal defaultServiceFeeRate;

    @Schema(description = "Thuế VAT mặc định (%)")
    BigDecimal defaultVatRate;
}
