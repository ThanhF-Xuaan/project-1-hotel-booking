package vn.edu.utc.hotel_booking.modules.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeCreateRequest {

    @NotNull(message = "ID khách sạn không được để trống")
    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @NotNull(message = "ID loại phòng không được để trống")
    @Schema(description = "ID loại phòng danh mục", example = "1")
    Short roomTypeId;

    @NotNull(message = "ID nhóm thuế không được để trống")
    @Schema(description = "ID nhóm thuế", example = "1")
    Integer taxCategoryId;

    @Min(value = 1, message = "Số người lớn tiêu chuẩn tối thiểu là 1")
    @Schema(description = "Số người lớn tiêu chuẩn", example = "2")
    @Builder.Default
    Short standardAdults = 2;

    @Min(value = 0, message = "Số trẻ em tiêu chuẩn tối thiểu là 0")
    @Schema(description = "Số trẻ em tiêu chuẩn", example = "0")
    @Builder.Default
    Short standardChildren = 0;

    @Min(value = 1, message = "Số người lớn tối đa tối thiểu là 1")
    @Schema(description = "Số người lớn tối đa", example = "2")
    @Builder.Default
    Short maxAdults = 2;

    @Min(value = 0, message = "Số trẻ em tối đa tối thiểu là 0")
    @Schema(description = "Số trẻ em tối đa", example = "1")
    @Builder.Default
    Short maxChildren = 1;

    @Min(value = 0, message = "Số trẻ sơ sinh tối đa tối thiểu là 0")
    @Schema(description = "Số trẻ sơ sinh tối đa", example = "1")
    @Builder.Default
    Short maxInfants = 1;

    @Min(value = 1, message = "Tổng số khách tối đa tối thiểu là 1")
    @Schema(description = "Tổng số khách tối đa", example = "3")
    @Builder.Default
    Short maxTotalGuests = 3;

    @Min(value = 1, message = "Số giường tối đa tối thiểu là 1")
    @Schema(description = "Số giường tối đa", example = "1")
    @Builder.Default
    Short maxBeds = 1;

    @Min(value = 0, message = "Số giường phụ tối đa tối thiểu là 0")
    @Schema(description = "Số giường phụ cho phép", example = "1")
    @Builder.Default
    Short extraBeds = 0;

    @NotNull(message = "Giá cơ bản không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá cơ bản phải lớn hơn 0")
    @Schema(description = "Giá cơ bản mỗi đêm (VNĐ)", example = "1200000.00")
    BigDecimal basePrice;

    @NotNull(message = "Tổng số lượng phòng không được để trống")
    @Min(value = 0, message = "Tổng số lượng phòng không được âm")
    @Schema(description = "Tổng số lượng phòng", example = "10")
    @Builder.Default
    Integer totalQuantity = 0;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    @Builder.Default
    String status = "ACTIVE";

    @Schema(description = "Danh sách ID tiện ích phòng")
    Set<Short> featureIds;
}
