package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomInclusionDto;

import java.util.List;

public interface HotelRoomTypeInclusionService {

    List<RoomInclusionDto> getInclusionsByHotelRoomTypeId(Integer hotelRoomTypeId);
}
