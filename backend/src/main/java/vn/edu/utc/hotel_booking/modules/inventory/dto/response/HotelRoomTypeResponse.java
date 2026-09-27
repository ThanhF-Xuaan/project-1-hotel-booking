package vn.edu.utc.hotel_booking.modules.inventory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRoomTypeResponse {

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer id;

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "Tên khách sạn", example = "Grand Hotel Da Nang")
    String hotelName;

    @Schema(description = "ID loại phòng danh mục", example = "1")
    Short roomTypeId;

    @Schema(description = "Mã loại phòng", example = "DELUXE")
    String roomTypeCode;

    @Schema(description = "Tên loại phòng", example = "Phòng Deluxe Hướng Biển")
    String roomTypeName;

    @Schema(description = "ID nhóm thuế", example = "1")
    Integer taxCategoryId;

    @Schema(description = "Số người lớn tiêu chuẩn", example = "2")
    Short standardAdults;

    @Schema(description = "Số trẻ em tiêu chuẩn", example = "0")
    Short standardChildren;

    @Schema(description = "Số người lớn tối đa", example = "2")
    Short maxAdults;

    @Schema(description = "Số trẻ em tối đa", example = "1")
    Short maxChildren;

    @Schema(description = "Số trẻ sơ sinh tối đa", example = "1")
    Short maxInfants;

    @Schema(description = "Tổng số khách tối đa", example = "3")
    Short maxTotalGuests;

    @Schema(description = "Số giường tối đa", example = "1")
    Short maxBeds;

    @Schema(description = "Số giường phụ cho phép", example = "1")
    Short extraBeds;

    @Schema(description = "Giá cơ bản mỗi đêm", example = "1200000.00")
    BigDecimal basePrice;

    @Schema(description = "Tổng số lượng phòng", example = "10")
    Integer totalQuantity;

    @Schema(description = "Trạng thái", example = "ACTIVE")
    String status;

    @Schema(description = "Danh sách tiện ích phòng")
    Set<RoomFeatureResponse> features;

    @Schema(description = "Thời gian tạo")
    OffsetDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    OffsetDateTime updatedAt;
}
