package vn.edu.utc.hotel_booking.modules.dashboard.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExecutiveReportFilterDto {

    @Builder.Default
    String scope = "CHAIN"; // 'CHAIN', 'REGION', 'HOTEL'

    Short regionId;
    Short hotelId;

    LocalDate startDate;
    LocalDate endDate;
    LocalDate targetDate;

    @Builder.Default
    String granularity = "DAY"; // 'DAY', 'MONTH', 'YEAR'
}
