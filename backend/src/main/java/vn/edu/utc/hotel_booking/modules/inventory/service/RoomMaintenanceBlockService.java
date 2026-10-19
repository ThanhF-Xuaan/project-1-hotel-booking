package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomMaintenanceBlockResponse;

import java.util.List;

public interface RoomMaintenanceBlockService {

    RoomMaintenanceBlockResponse create(RoomMaintenanceBlockCreateRequest request);

    RoomMaintenanceBlockResponse update(Long id, RoomMaintenanceBlockUpdateRequest request);

    RoomMaintenanceBlockResponse complete(Long id);

    RoomMaintenanceBlockResponse cancel(Long id);

    PageResponse<RoomMaintenanceBlockResponse> filter(RoomMaintenanceBlockSearchDto searchDto);

    RoomMaintenanceBlockResponse getById(Long id);

    List<RoomMaintenanceBlockResponse> getByRoomInstanceId(Integer roomInstanceId);
}
