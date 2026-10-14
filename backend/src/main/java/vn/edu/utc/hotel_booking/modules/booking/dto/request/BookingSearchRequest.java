package vn.edu.utc.hotel_booking.modules.booking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.common.dto.BaseSearchDto;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingSearchRequest extends BaseSearchDto {

    @Schema(description = "Loại điểm đến chọn từ Dropdown (REGION hoặc HOTEL)", example = "REGION")
    DestinationType destinationType;

    @Schema(description = "ID của điểm đến được chọn từ Dropdown (ID khu vực hoặc ID khách sạn)", example = "1")
    Integer destinationId;

    @Schema(description = "ID khu vực (dùng khi tìm kiếm theo khu vực, tương thích ngược)", example = "1")
    Short regionId;

    @Schema(description = "ID khách sạn (dùng khi tìm kiếm theo khách sạn cụ thể, tương thích ngược)", example = "1")
    Integer hotelId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    @Schema(description = "Ngày nhận phòng", example = "2026-10-15")
    LocalDate checkInDate;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Schema(description = "Ngày trả phòng", example = "2026-10-17")
    LocalDate checkOutDate;

    @Min(value = 1, message = "Số lượng phòng cần tìm tối thiểu là 1")
    @Schema(description = "Số lượng phòng cần tìm", example = "1")
    Integer roomsCount = 1;

    @Schema(description = "Mã voucher giảm giá (nếu có)", example = "SUMMER2026")
    String voucherCode;

    /**
     * Tự động chuẩn hóa giữa cặp (destinationType, destinationId) và cặp (hotelId, regionId)
     */
    public void normalize() {
        if (destinationType != null && destinationId != null) {
            if (destinationType == DestinationType.HOTEL) {
                this.hotelId = destinationId;
            } else if (destinationType == DestinationType.REGION) {
                this.regionId = destinationId.shortValue();
            }
        } else if (hotelId != null) {
            this.destinationType = DestinationType.HOTEL;
            this.destinationId = hotelId;
        } else if (regionId != null) {
            this.destinationType = DestinationType.REGION;
            this.destinationId = regionId.intValue();
        }
    }
}
