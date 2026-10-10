package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailableRoomTypeResponse {

    @Schema(description = "ID cấu hình hạng phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Mã loại phòng", example = "DLX")
    String roomTypeCode;

    @Schema(description = "Tên loại phòng", example = "Deluxe Hướng Biển")
    String roomTypeName;

    @Schema(description = "Mô tả chi tiết hạng phòng")
    String description;

    @Schema(description = "Số người lớn tiêu chuẩn", example = "2")
    Short standardAdults;

    @Schema(description = "Số người lớn tối đa", example = "2")
    Short maxAdults;

    @Schema(description = "Số trẻ em tối đa", example = "1")
    Short maxChildren;

    @Schema(description = "Tổng số khách tối đa", example = "3")
    Short maxTotalGuests;

    @Schema(description = "Số giường tối đa", example = "1")
    Short maxBeds;

    @Schema(description = "Số giường phụ cho phép kê thêm", example = "1")
    Short extraBeds;

    @Schema(description = "Số phòng khả dụng liên tục trong toàn bộ kỳ lưu trú", example = "4")
    Integer availableRooms;

    @Schema(description = "Chi tiết định giá phòng qua 6-Stage Pricing Pipeline (đã bao gồm phí dịch vụ 5% và VAT)")
    PriceBreakdownDto priceBreakdown;

    @Schema(description = "Danh sách tiện ích của phòng")
    List<RoomAmenityDto> amenities;

    @Schema(description = "Danh sách dịch vụ / quà tặng kèm miễn phí của phòng")
    List<RoomInclusionDto> inclusions;

    @Schema(description = "Chính sách phụ thu hiện có (chỉ bao gồm thêm người và thêm giường)")
    List<RoomSurchargePolicyDto> surchargePolicies;
}
