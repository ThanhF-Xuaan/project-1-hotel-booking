package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import java.math.BigDecimal;

/** Public, non-binding room-type suggestion. Contains no history or identity details. */
public record PersonalizationRoomTypeSuggestion(
        Integer hotelRoomTypeId,
        String roomTypeCode,
        String roomTypeName,
        BigDecimal affinityScore,
        String reasonCode
) {
}
