package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailableHotelResponse {

    @Schema(description = "ID khách sạn", example = "1")
    Integer hotelId;

    @Schema(description = "Tên khách sạn", example = "Viettel Luxury Hà Nội")
    String hotelName;

    @Schema(description = "Địa chỉ khách sạn", example = "123 Trần Duy Hưng, Cầu Giấy, Hà Nội")
    String address;

    @Schema(description = "Số điện thoại liên hệ", example = "02466668888")
    String phone;

    @Schema(description = "Tên khu vực", example = "Hà Nội")
    String regionName;

    @Schema(description = "Số lượng hạng phòng còn trống", example = "3")
    Integer availableRoomTypesCount;

    @Schema(description = "Giá phòng một đêm thấp nhất (đã bao gồm phí dịch vụ 5% và VAT)", example = "1500000.00")
    BigDecimal minPricePerNight;

    @Schema(description = "Đơn vị tiền tệ", example = "VND")
    @Builder.Default
    String currency = "VND";
}
