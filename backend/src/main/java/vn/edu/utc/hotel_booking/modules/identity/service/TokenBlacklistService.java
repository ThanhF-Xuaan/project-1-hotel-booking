package vn.edu.utc.hotel_booking.modules.identity.service;

public interface TokenBlacklistService {

    /**
     * Đưa Access Token vào Redis Blacklist với TTL = thời gian hiệu lực còn lại của Token.
     * @param token Chuỗi Access Token JWT nguyên bản (hoặc trích xuất từ Bearer header)
     */
    void blacklistToken(String token);

    /**
     * Kiểm tra xem Access Token có nằm trong danh sách đen hay không.
     * @param token Chuỗi Access Token JWT
     * @return true nếu token đã bị thu hồi / đăng xuất, false nếu hợp lệ
     */
    boolean isBlacklisted(String token);
}
