package vn.edu.utc.hotel_booking.modules.identity.service;

import java.util.List;

/**
 * Hợp đồng dịch vụ tra cứu nhân sự (Boundary Facade).
 * Phục vụ đóng gói (Encapsulation) ranh giới module identity, ngăn chặn các module
 * và aggregate khác truy cập trực tiếp vào StaffRepository theo Mục 3 của 02-backend-standards.
 */
public interface StaffLookupService {

    /**
     * Kiểm tra xem có nhân viên đang hoạt động (chưa bị xóa mềm) thuộc danh sách phòng ban hay không.
     * Sử dụng để ngăn chặn xóa phòng ban khi còn nhân sự trực thuộc (FR-ORG-02).
     *
     * @param departmentIds Danh sách ID phòng ban cần kiểm tra
     * @return true nếu còn ít nhất 1 nhân viên, ngược lại false
     */
    boolean hasActiveStaffInDepartments(List<Short> departmentIds);

    /**
     * Kiểm tra xem có nhân viên đang hoạt động (chưa bị xóa mềm) được gán các vai trò hay không.
     * Sử dụng để ngăn chặn xóa vai trò khi còn nhân sự đang đảm nhiệm (FR-IAM-05).
     *
     * @param roleIds Danh sách ID vai trò cần kiểm tra
     * @return true nếu còn ít nhất 1 nhân viên, ngược lại false
     */
    boolean hasActiveStaffWithRoles(List<Short> roleIds);

    /**
     * Lấy entity Staff theo ID phục vụ liên kết khóa ngoại nội bộ giữa các aggregate root.
     *
     * @param id ID của nhân viên
     * @return Staff entity hoặc null nếu không tồn tại
     */
    vn.edu.utc.hotel_booking.modules.identity.entity.Staff getStaffEntityById(Integer id);
}
