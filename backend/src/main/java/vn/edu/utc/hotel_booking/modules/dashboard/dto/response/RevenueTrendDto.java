package vn.edu.utc.hotel_booking.modules.dashboard.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueTrendDto {

    LocalDate date;
    BigDecimal roomRevenue;
    BigDecimal serviceRevenue;
    BigDecimal totalRevenue;
}
