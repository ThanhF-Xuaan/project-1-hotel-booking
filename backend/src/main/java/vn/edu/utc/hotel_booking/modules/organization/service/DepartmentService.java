package vn.edu.utc.hotel_booking.modules.organization.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.DepartmentUpdateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    PageResponse<DepartmentResponse> filter(DepartmentSearchDto searchDto);

    DepartmentResponse getById(Short id);

    DepartmentResponse create(DepartmentCreateRequest request);

    DepartmentResponse update(Short id, DepartmentUpdateRequest request);

    void deleteBatch(List<Short> ids);
}
