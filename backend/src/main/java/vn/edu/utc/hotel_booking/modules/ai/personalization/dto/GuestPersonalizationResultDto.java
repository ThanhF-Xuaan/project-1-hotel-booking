package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import vn.edu.utc.hotel_booking.modules.ai.personalization.enums.PersonalizationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.enums.TravelPatternSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả phân tích và gợi ý cá nhân hoá cho khách hàng.
 *
 * <p><strong>Bất biến kiến trúc U14:</strong></p>
 * <ul>
 *   <li>{@code nonBindingSuggestion = true}: Mọi gợi ý đều mang tính tham khảo không ràng buộc.</li>
 *   <li>{@code pricingOrPromotionMutated = false}: Tuyệt đối không thay đổi giá authoritative hay áp dụng voucher/khuyến mãi.</li>
 * </ul>
 */
@Getter
@ToString
@Builder
public class GuestPersonalizationResultDto {

    private final Long guestId;
    private final PersonalizationStatus status;
    private final TravelPatternSegment detectedSegment;

    @Builder.Default
    private final List<PersonalizedRoomTypeSuggestionDto> roomTypeSuggestions = new ArrayList<>();

    @Builder.Default
    private final List<PersonalizedFeatureSuggestionDto> featureSuggestions = new ArrayList<>();

    @Builder.Default
    private final List<PersonalizedServiceSuggestionDto> serviceSuggestions = new ArrayList<>();

    /**
     * Bất biến kiến trúc U14: Gợi ý chỉ mang tính tham khảo, không tạo cam kết hay đặt chỗ.
     */
    @Builder.Default
    private final boolean nonBindingSuggestion = true;

    /**
     * Bất biến kiến trúc U14: Tuyệt đối không can thiệp hay thay đổi giá phòng và khuyến mãi.
     */
    @Builder.Default
    private final boolean pricingOrPromotionMutated = false;

    @Builder.Default
    private final List<String> warnings = new ArrayList<>();

    public GuestPersonalizationResultDto(
            Long guestId,
            PersonalizationStatus status,
            TravelPatternSegment detectedSegment,
            List<PersonalizedRoomTypeSuggestionDto> roomTypeSuggestions,
            List<PersonalizedFeatureSuggestionDto> featureSuggestions,
            List<PersonalizedServiceSuggestionDto> serviceSuggestions,
            boolean nonBindingSuggestion,
            boolean pricingOrPromotionMutated,
            List<String> warnings
    ) {
        this.guestId = guestId;
        this.status = status;
        this.detectedSegment = detectedSegment;
        this.roomTypeSuggestions = roomTypeSuggestions != null ? roomTypeSuggestions : new ArrayList<>();
        this.featureSuggestions = featureSuggestions != null ? featureSuggestions : new ArrayList<>();
        this.serviceSuggestions = serviceSuggestions != null ? serviceSuggestions : new ArrayList<>();
        this.nonBindingSuggestion = true;
        this.pricingOrPromotionMutated = false;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}
