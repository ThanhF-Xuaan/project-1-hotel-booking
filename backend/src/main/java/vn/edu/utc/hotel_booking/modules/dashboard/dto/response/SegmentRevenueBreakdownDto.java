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
public class SegmentRevenueBreakdownDto {

    BigDecimal fitRevenue;       // Doanh thu khách lẻ (FIT)
    BigDecimal gitRevenue;       // Doanh thu khách đoàn & Doanh nghiệp (GIT)
    Double fitPercentage;        // Tỷ trọng khách lẻ %
    Double gitPercentage;        // Tỷ trọng khách đoàn %
    Integer fitBookingCount;     // Số lượt booking lẻ
    Integer gitBookingCount;     // Số lượt booking đoàn / hợp đồng
    Integer fitRoomNights;       // Số đêm phòng khách lẻ
    Integer gitRoomNights;       // Số đêm phòng khách đoàn
    Double fitSharePercentage;   // Tỷ trọng khách lẻ %
    Double gitSharePercentage;   // Tỷ trọng khách đoàn %
}
