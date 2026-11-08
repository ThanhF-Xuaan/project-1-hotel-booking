package vn.edu.utc.hotel_booking.modules.pricing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelAgePolicySearchDto extends BaseSearchDto {

    @Schema(description = "ID khách sạn (để trống để lấy tất cả, truyền null hoặc 0 để lọc toàn chuỗi)", example = "1")
    Short hotelId;

    @Schema(description = "Loại khách (INFANT, CHILD, ADULT)", example = "CHILD")
    String guestType;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Chỉ lọc chính sách toàn chuỗi (hotel_id IS NULL)", example = "false")
    Boolean chainOnly;
}

