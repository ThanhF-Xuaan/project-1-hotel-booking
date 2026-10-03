package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityReadingResponse;

import java.util.List;

public interface UtilityReadingService {
    PageResponse<UtilityReadingResponse> search(UtilityReadingSearchDto request);
    UtilityReadingResponse getById(Long id);
    UtilityReadingResponse create(UtilityReadingCreateRequest request);
    UtilityReadingResponse update(Long id, UtilityReadingUpdateRequest request);
    void delete(List<Long> ids);
}
