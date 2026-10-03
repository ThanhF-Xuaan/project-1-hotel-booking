package vn.edu.utc.hotel_booking.modules.identity.service;

import vn.edu.utc.hotel_booking.modules.identity.dto.request.LoginRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.LogoutRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RefreshTokenRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.TokenResponse;

public interface AuthService {

    /**
     * Đăng nhập xác thực tài khoản qua Keycloak Token Endpoint (grant_type=password).
     */
    TokenResponse login(LoginRequest request);

    /**
     * Đăng xuất hủy session trên Keycloak và đưa Access Token vào Redis Blacklist.
     */
    void logout(String authHeader, LogoutRequest request);

    /**
     * Làm mới Access Token thông qua Refresh Token (grant_type=refresh_token).
     */
    TokenResponse refreshToken(RefreshTokenRequest request);
}
