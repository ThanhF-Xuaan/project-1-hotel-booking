package vn.edu.utc.hotel_booking.modules.dashboard.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExecutiveKpiSummaryDto {

    Double occupancyRate;         // Tỷ lệ lấp đầy OCC %
    BigDecimal averageDailyRate;  // Giá phòng trung bình ngày ADR
    BigDecimal revPar;            // Doanh thu trên phòng sẵn có RevPAR

    Integer totalRooms;           // Tổng số phòng vật lý
    Integer totalActiveRooms;     // Số phòng hoạt động
    Long availableRoomNights;     // Số đêm phòng khả dụng
    Integer occupiedRoomNights;   // Số đêm phòng có khách
    Integer availableRooms;       // Số phòng sẵn sàng đón khách
    Integer occupiedRooms;        // Số phòng có khách lưu trú
    Integer outOfServiceRooms;    // Số phòng đang bảo trì / dọn dẹp

    BigDecimal totalRoomRevenue;  // Doanh thu buồng phòng
    BigDecimal totalFnbRevenue;   // Doanh thu F&B (Nhà hàng, Buffet, Tiệc đoàn)
    BigDecimal totalOtherRevenue; // Doanh thu dịch vụ khác (Minibar, Giặt ủi...)
    BigDecimal totalGrossRevenue; // Tổng doanh thu toàn bộ
    BigDecimal totalRevenue;      // Alias tổng doanh thu
}
