package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;

/**
 * Service đồng bộ và cấp phát Just-In-Time (JIT) cho hồ sơ Staff trong PostgreSQL
 * từ thông tin xác thực của Keycloak IAM.
 */
public interface StaffProvisioningService {

    /**
     * Đồng bộ hoặc khởi tạo mới (JIT Provisioning) hồ sơ Staff trong PostgreSQL từ JWT Access Token.
     * Đảm bảo Idempotent, độc lập Transaction (REQUIRES_NEW) để commit ngay trước khi ghi audit logs.
     *
     * @param accessToken Keycloak JWT Access Token
     * @param fallbackUsername Tên đăng nhập dự phòng nếu token không có claim preferred_username
     * @return Staff entity đã được đồng bộ trong PostgreSQL, hoặc null nếu token không hợp lệ
     */
    Staff provisionOrUpdateFromJwt(String accessToken, String fallbackUsername);
}
