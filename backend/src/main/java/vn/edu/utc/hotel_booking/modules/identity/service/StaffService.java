package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.StaffUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.StaffResponse;

import java.util.List;
import java.util.UUID;

public interface StaffService {

    PageResponse<StaffResponse> filter(StaffSearchDto searchDto);

    StaffResponse getById(Integer id);

    StaffResponse getByKeycloakId(UUID keycloakId);

    StaffResponse create(StaffCreateRequest request);

    StaffResponse update(Integer id, StaffUpdateRequest request);

    void deleteBatch(List<Integer> ids);
}
