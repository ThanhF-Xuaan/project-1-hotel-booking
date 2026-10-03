package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityMeterResponse;

import java.util.List;

public interface UtilityMeterService {
    PageResponse<UtilityMeterResponse> search(UtilityMeterSearchDto request);
    UtilityMeterResponse getById(Integer id);
    UtilityMeterResponse create(UtilityMeterCreateRequest request);
    UtilityMeterResponse update(Integer id, UtilityMeterUpdateRequest request);
    void delete(List<Integer> ids);
}
