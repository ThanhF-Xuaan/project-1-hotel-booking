package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

import java.util.List;

/** Distinguishes an ineligible hotel from an eligible hotel with no active room types. */
public record PersonalizationCatalogSnapshot(
        boolean hotelEligible,
        List<PersonalizationRoomType> roomTypes
) {
    public PersonalizationCatalogSnapshot {
        roomTypes = roomTypes == null ? List.of() : List.copyOf(roomTypes);
    }
}
