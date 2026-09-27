package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeSearchDto extends BaseSearchDto {

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "ID loại phòng", example = "1")
    Short roomTypeId;

    @Schema(description = "Số khách cần tìm (người lớn)", example = "2")
    Short adults;

    @Schema(description = "Số trẻ em cần tìm", example = "1")
    Short children;

    @Schema(description = "Giá tối thiểu", example = "500000")
    BigDecimal minPrice;

    @Schema(description = "Giá tối đa", example = "2000000")
    BigDecimal maxPrice;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;
}
