package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomFeatureResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;

import java.util.List;
import java.util.Set;

public interface RoomFeatureService {

    RoomFeatureResponse create(RoomFeatureCreateRequest request);

    RoomFeatureResponse update(Short id, RoomFeatureUpdateRequest request);

    RoomFeatureResponse getById(Short id);

    RoomFeature getEntityById(Short id);

    Set<RoomFeature> getEntitiesByIds(Set<Short> ids);

    PageResponse<RoomFeatureResponse> filter(RoomFeatureSearchDto searchDto);

    void deleteBatch(List<Short> ids);
}

