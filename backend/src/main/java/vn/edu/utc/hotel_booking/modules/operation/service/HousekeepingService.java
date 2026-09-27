package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.modules.operation.dto.request.RoomStatusUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.RoomHousekeepingStatusResponse;

import java.util.List;

public interface HousekeepingService {

    List<RoomHousekeepingStatusResponse> getRoomsByHotel(Short hotelId, String status);

    RoomHousekeepingStatusResponse updateRoomStatus(Integer roomInstanceId, RoomStatusUpdateRequest request);
}
