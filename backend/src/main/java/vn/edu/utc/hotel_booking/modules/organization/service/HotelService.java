package vn.edu.utc.hotel_booking.modules.organization.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelResponse;

import java.util.List;

public interface HotelService {

    PageResponse<HotelResponse> filter(HotelSearchDto searchDto);

    HotelResponse getById(Short id);

    HotelResponse create(HotelCreateRequest request);

    HotelResponse update(Short id, HotelUpdateRequest request);

    void deleteBatch(List<Short> ids);
}
