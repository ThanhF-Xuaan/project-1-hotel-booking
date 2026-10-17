package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Gợi ý hạng phòng cá nhân hoá dựa trên phân tích lịch sử lưu trú.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedRoomTypeSuggestionDto {

    private Integer hotelId;
    private Integer hotelRoomTypeId;
    private String roomTypeCode;
    private String roomTypeName;

    /** Điểm số độ tương thích cá nhân hoá, trong khoảng [0.0, 1.0]. */
    private double affinityScore;

    /** Số lần khách đã lưu trú tại hạng phòng này trong lịch sử. */
    private int historicalStayCount;

    /** Ngày lưu trú gần nhất tại hạng phòng này. */
    private LocalDate lastStayDate;

    /** Các đặc tính phòng phù hợp với sở thích của khách. */
    private List<String> matchedFeatures;

    /** Lý giải minh bạch vì sao gợi ý hạng phòng này (dựa trên dữ liệu xác thực). */
    private List<String> explanationReasons;
}
