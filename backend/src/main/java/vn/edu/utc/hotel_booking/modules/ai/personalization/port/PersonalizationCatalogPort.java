package vn.edu.utc.hotel_booking.modules.ai.personalization.port;

import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationCatalogSnapshot;

/** Read-only boundary to the inventory module's hotel-scoped public catalog. */
public interface PersonalizationCatalogPort {

    PersonalizationCatalogSnapshot findCurrentCatalog(Integer hotelId);
}
