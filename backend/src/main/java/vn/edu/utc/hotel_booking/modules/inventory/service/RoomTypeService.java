package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomTypeResponse;

import java.util.List;

public interface RoomTypeService {

    RoomTypeResponse createRoomType(RoomTypeCreateRequest request);

    RoomTypeResponse updateRoomType(Short id, RoomTypeUpdateRequest request);

    RoomTypeResponse getRoomTypeById(Short id);

    PageResponse<RoomTypeResponse> filterRoomTypes(RoomTypeSearchDto searchDto);

    void deleteRoomTypes(List<Short> ids);
}
