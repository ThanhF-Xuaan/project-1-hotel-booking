package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.modules.identity.entity.Role;

import java.util.List;
import java.util.Map;

/**
 * Hợp đồng dịch vụ tra cứu vai trò (Boundary Facade / Lookup Service).
 * Phục vụ đóng gói ranh giới aggregate Role, cho phép các service khác như PermissionService
 * hoặc StaffService giao tiếp qua tầng Service mà không vi phạm ranh giới repository
 * và tuyệt đối ngăn chặn Circular Dependency theo Mục 3 của 02-backend-standards.
 */
public interface RoleLookupService {

    /**
     * Lấy danh sách các vai trò đang hoạt động kèm theo quyền hạn đã gán (eager fetch permissions).
     *
     * @return Danh sách Role entities đang hoạt động
     */
    List<Role> getActiveRolesWithPermissions();

    /**
     * Tra cứu bảng ánh xạ permissionId -> danh sách các Role đang sở hữu quyền.
     *
     * @return Map với key là permissionId và value là danh sách Role
     */
    Map<Short, List<Role>> getPermissionRoleMap();

    /**
     * Lấy entity Role theo ID phục vụ liên kết khóa ngoại nội bộ giữa các aggregate root.
     *
     * @param id ID của vai trò
     * @return Role entity hoặc null nếu không tồn tại
     */
    Role getRoleEntityById(Short id);
}

