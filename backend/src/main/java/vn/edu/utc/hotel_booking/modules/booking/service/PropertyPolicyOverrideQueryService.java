package vn.edu.utc.hotel_booking.modules.booking.service;

public interface PropertyPolicyOverrideQueryService {

    /**
     * Kiểm tra xem có cấu hình override nào đang sử dụng chính sách hủy này hay không
     *
     * @param cancellationPolicyId ID chính sách hủy
     * @return true nếu đang được sử dụng trong override chưa bị xóa
     */
    boolean existsByCancellationPolicyId(Integer cancellationPolicyId);

    /**
     * Kiểm tra xem có cấu hình override nào đang sử dụng chính sách vắng mặt này hay không
     *
     * @param noShowPolicyId ID chính sách vắng mặt
     * @return true nếu đang được sử dụng trong override chưa bị xóa
     */
    boolean existsByNoShowPolicyId(Integer noShowPolicyId);
}
