package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MasterItemResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;

import java.util.List;

public interface MasterItemService {

    MasterItem getEntityById(Integer id);

    MasterItem saveEntity(MasterItem masterItem);

    MasterItemResponse getById(Integer id);

    MasterItemResponse create(MasterItemCreateRequest request);

    MasterItemResponse update(Integer id, MasterItemUpdateRequest request);

    PageResponse<MasterItemResponse> filter(MasterItemSearchDto searchDto);

    void deleteBatch(List<Integer> ids);

    List<MasterItemResponse> getAllActive();
}
