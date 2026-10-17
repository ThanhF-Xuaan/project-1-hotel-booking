package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gợi ý dịch vụ ưu tiên dựa trên lịch sử sử dụng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedServiceSuggestionDto {

    private String serviceCode;

    /** Điểm số độ yêu thích trong khoảng [0.0, 1.0]. */
    private double affinityScore;

    /** Số lần sử dụng trong lịch sử lưu trú. */
    private int occurrenceCount;
}
