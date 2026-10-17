package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Báo cáo kết quả đánh giá Holdout Offline so sánh mô hình Time-Decayed với Simple Frequency Baseline.
 */
@Getter
@ToString
@Builder
public class PersonalizationEvalReportDto {

    private final int totalEvaluatedStays;
    private final int k;

    private final double timeDecayedHitRateAtK;
    private final double baselineFrequencyHitRateAtK;

    private final double timeDecayedMrrAtK;
    private final double baselineFrequencyMrrAtK;

    /**
     * Bất biến kiến trúc U14/U22: Đánh giá chia tách theo thời gian, chống rò rỉ dữ liệu tương lai.
     */
    @Builder.Default
    private final boolean leakageFreeTimeSplit = true;

    public PersonalizationEvalReportDto(
            int totalEvaluatedStays,
            int k,
            double timeDecayedHitRateAtK,
            double baselineFrequencyHitRateAtK,
            double timeDecayedMrrAtK,
            double baselineFrequencyMrrAtK,
            boolean leakageFreeTimeSplit
    ) {
        this.totalEvaluatedStays = totalEvaluatedStays;
        this.k = k;
        this.timeDecayedHitRateAtK = timeDecayedHitRateAtK;
        this.baselineFrequencyHitRateAtK = baselineFrequencyHitRateAtK;
        this.timeDecayedMrrAtK = timeDecayedMrrAtK;
        this.baselineFrequencyMrrAtK = baselineFrequencyMrrAtK;
        this.leakageFreeTimeSplit = true;
    }
}
