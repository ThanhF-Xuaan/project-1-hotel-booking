package vn.edu.utc.hotel_booking.modules.organization.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelSearchDto extends BaseSearchDto {

    @Schema(description = "Lọc theo khu vực/vùng", example = "1")
    Short regionId;

    @Schema(description = "Từ khóa tìm kiếm (tên khách sạn, địa chỉ hoặc SĐT)", example = "Luxury")
    String keyword;

    @Schema(description = "Lọc theo trạng thái (ACTIVE / INACTIVE)", example = "ACTIVE")
    String status;
}
