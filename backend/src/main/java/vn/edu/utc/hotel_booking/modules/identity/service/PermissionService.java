package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.modules.identity.dto.response.PermissionResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.Permission;

import java.util.Collection;
import java.util.List;

public interface PermissionService {

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
