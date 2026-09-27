package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomInstanceUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomInstanceResponse;

import java.util.List;

public interface RoomInstanceService {

    RoomInstanceResponse createRoomInstance(RoomInstanceCreateRequest request);

    RoomInstanceResponse updateRoomInstance(Integer id, RoomInstanceUpdateRequest request);

    RoomInstanceResponse updateRoomStatus(Integer id, String status);

    RoomInstanceResponse getRoomInstanceById(Integer id);

    PageResponse<RoomInstanceResponse> filterRoomInstances(RoomInstanceSearchDto searchDto);

    void deleteRoomInstances(List<Integer> ids);
}
