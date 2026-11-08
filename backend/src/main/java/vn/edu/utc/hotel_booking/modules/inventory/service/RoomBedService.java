package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomBedResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomBed;

import java.util.List;

public interface RoomBedService {

    RoomBedResponse create(RoomBedCreateRequest request);

    RoomBedResponse update(Short id, RoomBedUpdateRequest request);

    RoomBedResponse getById(Short id);

    RoomBed getEntityById(Short id);

    PageResponse<RoomBedResponse> filter(RoomBedSearchDto searchDto);

    void deleteBatch(List<Short> ids);
}

