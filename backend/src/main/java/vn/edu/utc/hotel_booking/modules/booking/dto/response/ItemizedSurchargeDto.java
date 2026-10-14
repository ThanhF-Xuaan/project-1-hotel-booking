package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemizedSurchargeDto {

    @Schema(description = "Số người lớn vượt tiêu chuẩn", example = "1")
    Short extraAdultsCount;

    @Schema(description = "Tổng phụ thu người lớn toàn kỳ", example = "400000.00")
    BigDecimal extraAdultsSurchargeTotal;

    @Schema(description = "Số trẻ em vượt tiêu chuẩn", example = "1")
    Short extraChildrenCount;

    @Schema(description = "Tổng phụ thu trẻ em toàn kỳ", example = "200000.00")
    BigDecimal extraChildrenSurchargeTotal;

    @Schema(description = "Số trẻ sơ sinh", example = "1")
    Short infantsCount;

    @Schema(description = "Tổng phụ thu trẻ sơ sinh (miễn phí)", example = "0.00")
    BigDecimal infantsSurchargeTotal;

    @Schema(description = "Số giường phụ yêu cầu", example = "1")
    Short extraBedsCount;

    @Schema(description = "Tổng phụ thu giường phụ toàn kỳ", example = "600000.00")
    BigDecimal extraBedsSurchargeTotal;

    @Schema(description = "Tổng các khoản phụ thu toàn kỳ", example = "1200000.00")
    BigDecimal totalSurchargeAmount;

    @Schema(description = "Ghi chú giải thích phụ thu cho khách hàng")
    List<String> surchargeNotes;
}
