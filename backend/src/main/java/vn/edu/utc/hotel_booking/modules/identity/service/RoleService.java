package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RoleUpdateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {

    PageResponse<RoleResponse> filter(RoleSearchDto searchDto);

    RoleResponse getById(Short id);

    List<PermissionResponse> getAllPermissions();

    RoleResponse create(RoleCreateRequest request);

    RoleResponse update(Short id, RoleUpdateRequest request);

    void deleteBatch(List<Short> ids);
}
