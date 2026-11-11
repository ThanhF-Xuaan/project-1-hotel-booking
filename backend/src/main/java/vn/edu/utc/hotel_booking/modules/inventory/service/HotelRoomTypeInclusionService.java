package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomInclusionDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeInclusionRequest;

import java.util.List;

public interface HotelRoomTypeInclusionService {

    List<RoomInclusionDto> getInclusionsByHotelRoomTypeId(Integer hotelRoomTypeId);

    RoomInclusionDto createInclusion(Integer hotelRoomTypeId, HotelRoomTypeInclusionRequest request);

    RoomInclusionDto updateInclusion(Long inclusionId, HotelRoomTypeInclusionRequest request);

    void deleteInclusion(Long inclusionId);
}
