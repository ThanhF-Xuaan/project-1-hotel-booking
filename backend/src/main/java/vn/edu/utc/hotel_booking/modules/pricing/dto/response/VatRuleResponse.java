package vn.edu.utc.hotel_booking.modules.pricing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VatRuleResponse {

    @Schema(description = "ID quy tắc thuế VAT", example = "1")
    Integer id;

    @Schema(description = "ID nhóm thuế", example = "1")
    Integer taxCategoryId;

    @Schema(description = "Mã quy tắc thuế VAT", example = "VAT_ROOM_10")
    String vatCode;

    @Schema(description = "Tên quy tắc thuế VAT", example = "Thuế giá trị gia tăng dịch vụ phòng 10%")
    String vatName;

    @Schema(description = "Tỷ lệ thuế VAT (%)", example = "10.00")
    BigDecimal vatPercent;

    @Schema(description = "Ngày bắt đầu áp dụng", example = "2026-01-01")
    LocalDate startDate;

    @Schema(description = "Ngày kết thúc áp dụng")
    LocalDate endDate;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
