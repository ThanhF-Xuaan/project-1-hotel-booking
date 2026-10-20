package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Thông tin xem trước chi phí phạt và hoàn tiền khi hủy đặt phòng FIT")
public class BookingCancelPreviewResponse {

    @Schema(description = "ID đơn đặt phòng", example = "101")
    Long bookingId;

    @Schema(description = "Mã đơn đặt phòng", example = "BK-20261020-001")
    String bookingNumber;

    @Schema(description = "ID phòng đặt (nếu xem trước theo phòng cụ thể)", example = "205")
    Long bookingRoomId;

    @Schema(description = "Số phòng vật lý (nếu đã gán)", example = "302")
    String roomNumber;

    @Schema(description = "Tên hạng phòng", example = "Deluxe Double Ocean View")
    String roomTypeName;

    @Schema(description = "Số lượng phòng sẽ được giải phóng trong đợt hủy này", example = "1")
    Integer cancelledRoomsCount;

    @Schema(description = "Ngày nhận phòng", example = "2026-10-28")
    LocalDate checkInDate;

    @Schema(description = "Ngày hủy hiện tại", example = "2026-10-20")
    LocalDate cancelDate;

    @Schema(description = "Số ngày còn lại đến thời điểm nhận phòng", example = "8")
    Long daysUntilCheckIn;

    @Schema(description = "Tỷ lệ phạt hủy áp dụng (%)", example = "0.0")
    BigDecimal penaltyRatePercentage;

    @Schema(description = "Tiền phòng gốc của các phòng bị hủy", example = "2500000.00")
    BigDecimal roomAmount;

    @Schema(description = "Số tiền phạt hủy giữ lại", example = "0.00")
    BigDecimal cancellationPenalty;

    @Schema(description = "Tiền dịch vụ đặt trước không hoàn (Non-refundable)", example = "150000.00")
    BigDecimal preorderedServicesTotal;

    @Schema(description = "Tiền phụ phí phát sinh trước đó", example = "0.00")
    BigDecimal addonChargesTotal;

    @Schema(description = "Tổng nghĩa vụ giữ lại không được hoàn", example = "150000.00")
    BigDecimal nonRefundableTotal;

    @Schema(description = "Tổng số tiền khách đã thanh toán thành công", example = "2650000.00")
    BigDecimal totalPaid;

    @Schema(description = "Số tiền hoàn lại cho khách (Refund = TotalPaid - NonRefundableTotal)", example = "2500000.00")
    BigDecimal refundAmount;

    @Schema(description = "Phân luồng hoàn tiền: NO_REFUND, AUTO_REFUND (<= 5tr), WAITING_FINANCE_APPROVAL (> 5tr)", example = "AUTO_REFUND")
    String refundRouting;

    @Schema(description = "Diễn giải chi tiết chính sách phạt hủy theo SRS", example = "Đơn khách lẻ FIT hủy trước ngày nhận phòng >= 7 ngày: Miễn phí phạt hủy (Phạt 0%).")
    String policyDescription;

    @Schema(description = "Cờ xác định đây là xem trước hủy toàn bộ các phòng còn lại của đơn hay hủy 1 phòng", example = "false")
    Boolean isFullBookingCancellation;
}
