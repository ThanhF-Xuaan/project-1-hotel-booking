package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;

import java.util.Collection;
import java.util.List;

public interface PermissionService {

    /**
     * Tìm kiếm và phân trang danh mục quyền hạn (Permissions) trong hệ thống.
     *
     * @param searchDto tiêu chí tìm kiếm và phân trang
     * @return PageResponse chứa danh sách PermissionResponse
     */
    PageResponse<PermissionResponse> filter(PermissionSearchDto searchDto);

    /**
     * Lấy toàn bộ danh mục quyền hạn (Permissions) đang hoạt động trong hệ thống.
     *
     * @return danh sách PermissionResponse
     */
    List<PermissionResponse> getAllPermissions();

    /**
     * Tra cứu danh sách các thực thể Permission theo tập IDs.
     *
     * @param ids tập ID quyền hạn cần lấy
     * @return danh sách Permission entities
     */
    List<Permission> getEntitiesByIds(Collection<Short> ids);

    /**
     * Tra cứu chi tiết một thực thể Permission theo ID.
     *
     * @param id ID của quyền hạn
     * @return thực thể Permission
     */
    Permission getEntityById(Short id);
}
