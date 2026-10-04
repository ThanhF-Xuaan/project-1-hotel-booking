package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

import java.time.OffsetDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StayGuestSearchDto extends BaseSearchDto {

    @Schema(description = "ID khách sạn cơ sở", example = "1")
    Short hotelId;

    @Schema(description = "ID đơn đặt phòng", example = "100")
    Long bookingId;

    @Schema(description = "Số phòng", example = "P101")
    String roomNumber;

    @Schema(description = "Số CCCD hoặc Hộ chiếu", example = "001200001234")
    String documentNumber;

    @Schema(description = "Từ khóa tìm kiếm (họ tên, số giấy tờ)", example = "NGUYỄN VĂN A")
    String keyword;

    @Schema(description = "Thời gian check-in từ")
    OffsetDateTime fromDate;

    @Schema(description = "Thời gian check-in đến")
    OffsetDateTime toDate;
}
