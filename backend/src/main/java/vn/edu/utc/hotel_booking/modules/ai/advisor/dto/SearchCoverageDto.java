package vn.edu.utc.hotel_booking.modules.ai.advisor.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CoverageReason;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CoverageStatus;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchCoverageDto {
    CoverageStatus status;
    boolean catalogComplete;
    int discoveredCount;
    int evaluatedCount;
    int failedCount;
    int remainingCount;
    int verifiedMatchCount;

    @Builder.Default
    List<CoverageReason> reasons = new ArrayList<>();
}
