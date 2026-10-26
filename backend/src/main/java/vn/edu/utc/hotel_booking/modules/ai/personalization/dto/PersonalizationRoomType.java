package vn.edu.utc.hotel_booking.modules.ai.personalization.dto;

/** Current catalog labels for one eligible hotel room type. */
public record PersonalizationRoomType(
        Integer hotelRoomTypeId,
        String roomTypeCode,
        String roomTypeName
) {
}
