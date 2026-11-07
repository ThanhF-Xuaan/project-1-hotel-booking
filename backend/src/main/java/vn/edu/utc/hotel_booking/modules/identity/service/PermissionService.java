package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionCreateRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionSearchDto;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.PermissionUpdateRequest;
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

    /**
     * Tạo mới một quyền hạn trong hệ thống.
     *
     * @param request dữ liệu tạo mới
     * @return PermissionResponse
     */
    PermissionResponse create(PermissionCreateRequest request);

    /**
     * Cập nhật thông tin quyền hạn theo ID.
     *
     * @param id ID quyền hạn cần cập nhật
     * @param request dữ liệu cập nhật
     * @return PermissionResponse
     */
    PermissionResponse update(Short id, PermissionUpdateRequest request);

    /**
     * Xóa mềm danh sách quyền hạn theo IDs (có kiểm tra ràng buộc toàn vẹn).
     *
     * @param ids danh sách ID quyền hạn cần xóa
     */
    void deleteBatch(List<Short> ids);

    /**
     * Lấy danh sách tất cả các tài nguyên (resource) phân hệ đang có trong DB.
     *
     * @return danh sách resource viết hoa
     */
    List<String> getAllResources();

    /**
     * Lấy danh sách tất cả các hành động (action) đang có trong DB.
     *
     * @return danh sách action viết hoa
     */
    List<String> getAllActions();
}
