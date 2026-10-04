package vn.edu.utc.hotel_booking.modules.booking.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.*;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface BookingMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    @Mapping(target = "guestId", source = "guest.id")
    @Mapping(target = "guestPhone", source = "guest.phone")
    @Mapping(target = "guestName", source = "guest.phone")
    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "bookingDetails", source = "bookingDetails")
    BookingResponse toResponse(Booking entity);

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "hotelRoomTypeId", source = "hotelRoomType.id")
    @Mapping(target = "bookingRooms", source = "bookingRooms")
    BookingDetailResponse toDetailResponse(BookingDetail entity);

    @Mapping(target = "bookingDetailId", source = "bookingDetail.id")
    @Mapping(target = "roomInstanceId", source = "roomInstance.id")
    @Mapping(target = "roomNumber", source = "roomInstance.roomNumber")
    @Mapping(target = "bookingGuests", source = "stayGuests")
    @Mapping(target = "dailyRates", source = "dailyRates")
    @Mapping(target = "charges", source = "charges")
    BookingRoomResponse toRoomResponse(BookingRoom entity);

    @Mapping(target = "birthDate", source = "dateOfBirth")
    @Mapping(target = "identityNumber", source = "documentNumber")
    @Mapping(target = "identityType", source = "identityType")
    BookingGuestResponse toGuestResponse(StayGuest entity);

    @Mapping(target = "taxCategoryId", source = "taxCategory.id")
    @Mapping(target = "taxCategoryName", source = "taxCategory.categoryName")
    BookingDailyRateResponse toDailyRateResponse(BookingDailyRate entity);

    @Mapping(target = "bookingRoomId", source = "bookingRoom.id")
    @Mapping(target = "bookingGuestId", source = "stayGuest.id")
    BookingChargeResponse toChargeResponse(BookingCharge entity);
}
