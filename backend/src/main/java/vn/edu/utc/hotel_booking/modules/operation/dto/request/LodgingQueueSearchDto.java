package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;

import java.time.OffsetDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LodgingQueueSearchDto extends BaseSearchDto {

    @Schema(description = "ID khách sạn cơ sở", example = "1")
    Short hotelId;

    @Schema(description = "Trạng thái hàng chờ (PENDING, EXPORTED, ERROR, CANCELLED)", example = "PENDING")
    LodgingQueueStatus status;

    @Schema(description = "Thời gian từ")
    OffsetDateTime fromDate;

    @Schema(description = "Thời gian đến")
    OffsetDateTime toDate;
}
