package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationPolicy;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPolicy;

/**
 * Service tra cứu dùng chung cho phân hệ Chính sách (Policy Sub-domain),
 * đóng vai trò Façade giải phóng khớp nối chéo và loại bỏ nguy cơ vòng lặp phụ thuộc (Circular Dependency).
 */
public interface PolicyLookupService {

    /**
     * Kiểm tra xem chính sách hủy có đang được cấu hình ghi đè (PropertyPolicyOverride) tham chiếu không
     *
     * @param cancellationPolicyId ID của chính sách hủy
     * @return true nếu có ít nhất 1 cấu hình ghi đè đang tham chiếu và chưa xóa mềm
     */
    boolean existsOverrideByCancellationPolicyId(Integer cancellationPolicyId);

    /**
     * Kiểm tra xem chính sách vắng mặt có đang được cấu hình ghi đè (PropertyPolicyOverride) tham chiếu không
     *
     * @param noShowPolicyId ID của chính sách vắng mặt
     * @return true nếu có ít nhất 1 cấu hình ghi đè đang tham chiếu và chưa xóa mềm
     */
    boolean existsOverrideByNoShowPolicyId(Integer noShowPolicyId);

    /**
     * Tra cứu thực thể CancellationPolicy theo ID
     *
     * @param id ID của chính sách hủy
     * @return Thực thể CancellationPolicy
     * @throws vn.edu.utc.hotel_booking.common.exception.AppException nếu không tìm thấy (CANCELLATION_POLICY_NOT_FOUND)
     */
    CancellationPolicy getCancellationPolicyEntityById(Integer id);

    /**
     * Tra cứu thực thể NoShowPolicy theo ID
     *
     * @param id ID của chính sách vắng mặt
     * @return Thực thể NoShowPolicy
     * @throws vn.edu.utc.hotel_booking.common.exception.AppException nếu không tìm thấy (NO_SHOW_POLICY_NOT_FOUND)
     */
    NoShowPolicy getNoShowPolicyEntityById(Integer id);
}
