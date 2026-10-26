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
public class DepartmentRevenueBreakdownDto {

    BigDecimal roomRevenue;           // Tiền phòng lưu trú
    BigDecimal breakfastRevenue;      // Buffet sáng
    BigDecimal banquetRevenue;        // Tiệc & Hội nghị khách đoàn
    BigDecimal restaurantRevenue;     // Gọi món nhà hàng
    BigDecimal totalFnbRevenue;       // Tổng ẩm thực F&B

    BigDecimal spaRevenue;            // Spa / Massage
    BigDecimal minibarRevenue;        // Minibar phòng
    BigDecimal laundryRevenue;        // Giặt ủi
    BigDecimal transportRevenue;      // Đưa đón / Xe
    BigDecimal miscellaneousRevenue;  // Doanh thu vãng lai / khác
    BigDecimal otherServicesRevenue;  // Dịch vụ khác
    BigDecimal totalOtherRevenue;     // Tổng dịch vụ ngoài F&B

    BigDecimal totalServiceRevenue;   // Tổng doanh thu dịch vụ (F&B + Other)
    BigDecimal totalDepartmentRevenue;// Grand total (Room + Services)

    Double roomRevenuePercentage;     // Tỷ trọng tiền phòng %
    Double fnbRevenuePercentage;      // Tỷ trọng F&B %
    Double otherRevenuePercentage;    // Tỷ trọng dịch vụ khác %

    Double roomPercentage;            // Alias tỷ trọng tiền phòng %
    Double fnbPercentage;             // Alias tỷ trọng F&B %
    Double otherPercentage;           // Alias tỷ trọng dịch vụ khác %
}
