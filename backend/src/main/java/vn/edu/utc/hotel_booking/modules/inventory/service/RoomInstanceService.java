package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomInstanceResponse;

import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;

import java.util.List;

public interface RoomInstanceService {

    RoomInstanceResponse createRoomInstance(RoomInstanceCreateRequest request);

    RoomInstanceResponse updateRoomInstance(Integer id, RoomInstanceUpdateRequest request);

    RoomInstanceResponse updateRoomStatus(Integer id, String status);

    RoomInstanceResponse getRoomInstanceById(Integer id);

    RoomInstance getEntityById(Integer id);

    PageResponse<RoomInstanceResponse> filterRoomInstances(RoomInstanceSearchDto searchDto);

    List<RoomInstance> getRoomsByHotelAndStatus(Short hotelId, String status);

    RoomInstance updateRoomStatusEntity(Integer id, String status);

    int countOccupiedRooms(Short hotelId);

    void deleteRoomInstances(List<Integer> ids);

    List<RoomInstance> getActiveRoomsByHotelIds(List<Short> hotelIds);
}
