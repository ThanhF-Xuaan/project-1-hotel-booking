package vn.edu.utc.hotel_booking.modules.organization.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelResponse {

    @Schema(description = "ID khách sạn cơ sở", example = "1")
    Short id;

    @Schema(description = "ID khu vực trực thuộc", example = "1")
    Short regionId;

    @Schema(description = "Tên khu vực trực thuộc", example = "Miền Bắc")
    String regionName;

    @Schema(description = "Mã khu vực trực thuộc", example = "NORTH")
    String regionCode;

    @Schema(description = "Tên khách sạn cơ sở", example = "Viettel Luxury Hà Nội")
    String name;

    @Schema(description = "Địa chỉ chi tiết", example = "Tòa nhà Viettel, Nam Từ Liêm, Hà Nội")
    String address;

    @Schema(description = "Số điện thoại", example = "02466668888")
    String phone;

    @Schema(description = "Giờ nhận phòng tiêu chuẩn", example = "14:00:00")
    LocalTime checkInTime;

    @Schema(description = "Giờ trả phòng tiêu chuẩn", example = "12:00:00")
    LocalTime checkOutTime;

    @Schema(description = "Phí dịch vụ khách sạn (%)", example = "5.00")
    BigDecimal serviceFeePercent;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    OffsetDateTime updatedAt;
}
