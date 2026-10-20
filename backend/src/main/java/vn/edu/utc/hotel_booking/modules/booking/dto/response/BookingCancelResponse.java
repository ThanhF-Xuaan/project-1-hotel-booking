package vn.edu.utc.hotel_booking.modules.booking.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Kết quả sau khi thực hiện hủy đặt phòng")
public class BookingCancelResponse {

    @Schema(description = "ID đơn đặt phòng", example = "101")
    Long bookingId;

    @Schema(description = "Mã đơn đặt phòng", example = "BK-20261020-001")
    String bookingNumber;

    @Schema(description = "ID phòng đặt (nếu hủy phòng đơn lẻ)", example = "205")
    Long bookingRoomId;

    @Schema(description = "Số lượng phòng đã được giải phóng", example = "1")
    Integer cancelledRoomsCount;

    @Schema(description = "Trạng thái tổng thể của đơn đặt phòng sau khi hủy", example = "CONFIRMED")
    String bookingStatus;

    @Schema(description = "Trạng thái của phòng đặt", example = "CANCELLED")
    String roomStatus;

    @Schema(description = "Số tiền phạt hủy đã áp dụng", example = "500000.00")
    BigDecimal cancellationPenalty;

    @Schema(description = "Số tiền hoàn lại cho khách", example = "1000000.00")
    BigDecimal refundAmount;

    @Schema(description = "Mã giao dịch hoàn tiền tham chiếu (nếu phát sinh hoàn tiền)", example = "REFUND-BK-20261020-001")
    String refundPaymentReference;

    @Schema(description = "Phân luồng xử lý hoàn tiền (NO_REFUND, AUTO_GATEWAY_REFUND, WAITING_FINANCE_APPROVAL)", example = "AUTO_GATEWAY_REFUND")
    String refundRouting;

    @Schema(description = "Tài khoản thực hiện hủy", example = "receptionist_an")
    String cancelledBy;

    @Schema(description = "Thời điểm thực hiện hủy", example = "2026-10-20T10:30:00+07:00")
    OffsetDateTime cancelledAt;

    @Schema(description = "Thông báo kết quả cho người dùng", example = "Hủy phòng thành công. Tiền hoàn dự kiến sẽ về tài khoản quý khách trong 3 - 7 ngày làm việc.")
    String message;
}
