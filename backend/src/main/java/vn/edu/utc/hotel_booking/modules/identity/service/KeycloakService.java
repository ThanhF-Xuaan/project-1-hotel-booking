package vn.edu.utc.hotel_booking.modules.identity.service;

import java.util.List;
import java.util.UUID;

public interface KeycloakService {

    /**
     * Tạo tài khoản người dùng trên Keycloak IAM.
     * Nếu password rỗng, sinh mật khẩu tạm ngẫu nhiên và yêu cầu đổi mật khẩu ở lần đăng nhập đầu tiên.
     */
    UUID createUser(String username, String email, String firstName, String lastName, String roleCode, String password);

    /**
     * Cập nhật thông tin tài khoản người dùng trên Keycloak IAM.
     */
    void updateUser(UUID keycloakId, String email, String firstName, String lastName, String roleCode, String password, Boolean enabled);

    /**
     * Vô hiệu hóa tài khoản trên Keycloak IAM (bắt gọn 404 nếu không tìm thấy).
     */
    void disableUser(UUID keycloakId);

    /**
     * Hủy toàn bộ phiên đăng nhập của người dùng trên Keycloak IAM (bắt gọn 404 nếu không tìm thấy).
     */
    void logoutUser(UUID keycloakId);

    /**
     * Xóa vĩnh viễn tài khoản người dùng trên Keycloak IAM (Compensating Transaction, bắt gọn 404).
     */
    void deleteUser(UUID keycloakId);

    /**
     * Tác vụ bất đồng bộ vô hiệu hóa và đăng xuất hàng loạt người dùng với cơ chế Retry.
     */
    void asyncDisableAndLogoutUsers(List<UUID> keycloakIds);

    /**
     * Fallback cứu hộ khi retry 3 lần thất bại, lưu vết vào Dead Letter Table.
     */
    void recoverAsyncDisable(Exception e, List<UUID> keycloakIds);
}
