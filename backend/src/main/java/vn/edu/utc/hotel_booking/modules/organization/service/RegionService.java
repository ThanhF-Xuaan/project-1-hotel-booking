package vn.edu.utc.hotel_booking.modules.organization.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.RegionResponse;

import java.util.List;

public interface RegionService {

    PageResponse<RegionResponse> filter(RegionSearchDto searchDto);

    RegionResponse getById(Short id);

    RegionResponse create(RegionCreateRequest request);

    RegionResponse update(Short id, RegionUpdateRequest request);

    void deleteBatch(List<Short> ids);
}
