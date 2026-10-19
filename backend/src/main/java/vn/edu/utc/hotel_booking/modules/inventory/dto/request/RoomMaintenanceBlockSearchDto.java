package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomMaintenanceBlockSearchDto extends BaseSearchDto {

    @Schema(description = "ID phòng vật lý", example = "1")
    Integer roomInstanceId;

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "Loại block (OOO/OOS)", example = "OOO")
    String blockType;

    @Schema(description = "Trạng thái (ACTIVE, COMPLETED, CANCELLED)", example = "ACTIVE")
    String status;

    @Schema(description = "Từ ngày", example = "2026-10-01")
    LocalDate fromDate;

    @Schema(description = "Đến ngày", example = "2026-10-31")
    LocalDate toDate;
}
