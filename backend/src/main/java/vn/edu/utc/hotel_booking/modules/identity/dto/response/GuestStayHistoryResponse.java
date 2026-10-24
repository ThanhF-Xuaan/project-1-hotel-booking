package vn.edu.utc.hotel_booking.modules.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Thông tin thống kê lịch sử lưu trú và chi tiêu của khách hàng (CRM Guest Profile)")
public class GuestStayHistoryResponse {

    @Schema(description = "ID khách hàng", example = "101")
    Long guestId;

    @Schema(description = "Họ và tên khách hàng", example = "Nguyễn Văn A")
    String fullName;

    @Schema(description = "Số điện thoại liên hệ", example = "0987654321")
    String phone;

    @Schema(description = "Tổng số lượt đặt phòng / lưu trú", example = "5")
    Integer totalStays;

    @Schema(description = "Tổng số tiền đã chi tiêu tích lũy (VNĐ)", example = "15000000.00")
    BigDecimal totalSpent;

    @Schema(description = "Danh sách các đơn đặt phòng gần nhất của khách")
    List<GuestBookingSummaryDto> bookings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Tóm tắt đơn đặt phòng trong lịch sử khách hàng")
    public static class GuestBookingSummaryDto {
        @Schema(description = "ID đơn đặt phòng", example = "1")
        Long bookingId;

        @Schema(description = "Mã đơn đặt phòng", example = "BK261024001")
        String bookingNumber;

        @Schema(description = "Tên khách sạn lưu trú", example = "Khách sạn UTC Hà Nội")
        String hotelName;

        @Schema(description = "Trạng thái đơn đặt phòng", example = "CHECKED_OUT")
        String status;

        @Schema(description = "Tổng giá trị đơn phòng", example = "3500000.00")
        BigDecimal totalPrice;

        @Schema(description = "Ngày tạo đơn", example = "2026-10-24")
        LocalDate bookingDate;
    }
}
