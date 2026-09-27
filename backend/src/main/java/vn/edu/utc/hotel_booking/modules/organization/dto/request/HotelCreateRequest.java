package vn.edu.utc.hotel_booking.modules.organization.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelCreateRequest {

    @Schema(description = "ID khu vực trực thuộc", example = "1")
    @NotNull(message = "Khu vực trực thuộc không được để trống")
    Short regionId;

    @Schema(description = "Tên khách sạn cơ sở", example = "Viettel Luxury Hà Nội")
    @NotBlank(message = "Tên khách sạn không được để trống")
    @Size(max = 255, message = "Tên khách sạn tối đa 255 ký tự")
    String name;

    @Schema(description = "Địa chỉ chi tiết", example = "Tòa nhà Viettel, Nam Từ Liêm, Hà Nội")
    @NotBlank(message = "Địa chỉ khách sạn không được để trống")
    String address;

    @Schema(description = "Số điện thoại liên hệ", example = "02466668888")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    String phone;

    @Schema(description = "Giờ nhận phòng tiêu chuẩn (HH:mm:ss)", example = "14:00:00")
    @Builder.Default
    LocalTime checkInTime = LocalTime.of(14, 0);

    @Schema(description = "Giờ trả phòng tiêu chuẩn (HH:mm:ss)", example = "12:00:00")
    @Builder.Default
    LocalTime checkOutTime = LocalTime.of(12, 0);

    @Schema(description = "Phí dịch vụ khách sạn (%)", example = "5.00")
    @DecimalMin(value = "0.0", message = "Phí dịch vụ tối thiểu là 0%")
    @DecimalMax(value = "100.0", message = "Phí dịch vụ tối đa là 100%")
    @Builder.Default
    BigDecimal serviceFeePercent = BigDecimal.ZERO;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";
}
