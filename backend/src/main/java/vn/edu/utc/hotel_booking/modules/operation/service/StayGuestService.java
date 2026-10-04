package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.StayGuestResponse;

import java.util.List;

public interface StayGuestService {

    StayGuestResponse createOrCheckIn(StayGuestCreateRequest request);

    StayGuestResponse update(Long id, StayGuestUpdateRequest request);

    PageResponse<StayGuestResponse> search(StayGuestSearchDto request);

    StayGuestResponse getById(Long id);

    void delete(List<Long> ids);
}
