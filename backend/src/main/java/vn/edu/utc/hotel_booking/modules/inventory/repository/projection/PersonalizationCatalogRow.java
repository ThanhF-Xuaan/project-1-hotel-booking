package vn.edu.utc.hotel_booking.modules.inventory.repository.projection;

/** Native-query projection with a null room type when the hotel is active but has no eligible catalog rows. */
public interface PersonalizationCatalogRow {
    Short getHotelId();

    Integer getHotelRoomTypeId();

    String getRoomTypeCode();

    String getRoomTypeName();
}
