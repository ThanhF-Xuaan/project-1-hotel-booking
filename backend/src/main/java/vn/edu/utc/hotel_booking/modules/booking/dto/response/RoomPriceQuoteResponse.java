package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomPriceQuoteResponse {

    @Schema(description = "ID cấu hình loại phòng", example = "1")
    Integer hotelRoomTypeId;

    @Schema(description = "Mã loại phòng", example = "DELUXE")
    String roomTypeCode;

    @Schema(description = "Tên loại phòng", example = "Phòng Deluxe Hướng Biển")
    String roomTypeName;

    @Schema(description = "Ngày nhận phòng", example = "2026-10-15")
    LocalDate checkInDate;

    @Schema(description = "Ngày trả phòng", example = "2026-10-17")
    LocalDate checkOutDate;

    @Schema(description = "Tổng số đêm lưu trú", example = "2")
    Integer totalNights;

    @Schema(description = "Số người lớn yêu cầu", example = "3")
    Short requestedAdults;

    @Schema(description = "Số trẻ em yêu cầu", example = "1")
    Short requestedChildren;

    @Schema(description = "Số trẻ sơ sinh yêu cầu", example = "1")
    Short requestedInfants;

    @Schema(description = "Số giường phụ yêu cầu", example = "1")
    Short requestedExtraBeds;

    @Schema(description = "Bóc tách chi tiết các khoản phụ thu phát sinh")
    ItemizedSurchargeDto surchargeSummary;

    @Schema(description = "Chi tiết toàn bộ bảng giá qua 6-Stage Pricing Pipeline")
    PriceBreakdownDto priceBreakdown;
}
