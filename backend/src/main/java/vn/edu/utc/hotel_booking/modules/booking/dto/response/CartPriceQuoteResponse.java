package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartPriceQuoteResponse {

    @Schema(description = "ID khách sạn", example = "1")
    Short hotelId;

    @Schema(description = "Ngày nhận phòng", example = "2026-10-15")
    LocalDate checkInDate;

    @Schema(description = "Ngày trả phòng", example = "2026-10-17")
    LocalDate checkOutDate;

    @Schema(description = "Tổng số đêm lưu trú", example = "2")
    Integer totalNights;

    @Schema(description = "Tổng số phòng trong giỏ hàng", example = "2")
    Integer totalRooms;

    @Schema(description = "Mã voucher đã áp dụng", example = "SUMMER2026")
    String appliedVoucherCode;

    @Schema(description = "Tên chương trình voucher/khuyến mại áp dụng")
    String appliedPromotionName;

    @Schema(description = "Tổng giá gốc toàn bộ các phòng", example = "4000000.00")
    BigDecimal totalBasePrice;

    @Schema(description = "Tổng tiền phụ thu toàn bộ các phòng", example = "400000.00")
    BigDecimal totalSurchargeAmount;

    @Schema(description = "Tổng tiền giảm giá khuyến mại", example = "440000.00")
    BigDecimal totalDiscountAmount;

    @Schema(description = "Tổng tiền net trước thuế & phí", example = "3960000.00")
    BigDecimal totalRoomNetAmount;

    @Schema(description = "Tổng phí dịch vụ phòng (5%)", example = "198000.00")
    BigDecimal totalRoomServiceFee;

    @Schema(description = "Tổng thuế VAT phòng", example = "415800.00")
    BigDecimal totalRoomVat;

    @Schema(description = "Tổng tiền phòng sau thuế & phí", example = "4573800.00")
    BigDecimal finalTotalAmount;

    @Schema(description = "Tổng tiền dịch vụ trước thuế & phí", example = "500000.00")
    @Builder.Default
    BigDecimal totalServiceSubtotal = BigDecimal.ZERO;

    @Schema(description = "Tổng phí phục vụ dịch vụ (5%)", example = "25000.00")
    @Builder.Default
    BigDecimal totalServiceFee = BigDecimal.ZERO;

    @Schema(description = "Tổng thuế VAT dịch vụ (10%)", example = "52500.00")
    @Builder.Default
    BigDecimal totalServiceVat = BigDecimal.ZERO;

    @Schema(description = "Tổng tiền dịch vụ đã gồm thuế & phí", example = "577500.00")
    @Builder.Default
    BigDecimal totalServiceGrossAmount = BigDecimal.ZERO;

    @Schema(description = "Tổng cộng thanh toán toàn bộ đơn hàng (Phòng + Dịch vụ)", example = "5151300.00")
    BigDecimal grandTotal;

    @Schema(description = "Chi tiết báo giá từng hạng phòng trong giỏ hàng")
    @Builder.Default
    List<RoomPriceQuoteResponse> itemizedQuotes = new ArrayList<>();

    @Schema(description = "Chi tiết các dịch vụ bổ sung được chọn kèm theo")
    @Builder.Default
    List<ServiceQuoteItemResponse> serviceItems = new ArrayList<>();
}
