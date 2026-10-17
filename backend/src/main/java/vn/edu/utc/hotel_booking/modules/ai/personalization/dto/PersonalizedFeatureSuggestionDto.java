package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gợi ý đặc tính phòng ưu tiên dựa trên sở thích lịch sử.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedFeatureSuggestionDto {

    private String featureCode;

    /** Điểm số độ yêu thích trong khoảng [0.0, 1.0]. */
    private double affinityScore;

    /** Số lần xuất hiện trong lịch sử lưu trú. */
    private int occurrenceCount;
}
