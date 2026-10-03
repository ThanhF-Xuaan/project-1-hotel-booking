package vn.edu.utc.hotel_booking.modules.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.LoginRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.LogoutRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RefreshTokenRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.TokenResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Identity - Authentication", description = "APIs Đăng nhập, Đăng xuất & Quản lý Token (Keycloak IAM & Redis)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống (Lấy Access Token & Refresh Token qua Keycloak)")
    public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ApiResponse.<TokenResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Đăng nhập thành công")
                .result(response)
                .build();
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản (Thu hồi phiên Keycloak & Lưu Access Token vào Redis Blacklist)")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request
    ) {
        authService.logout(authHeader, request);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Đăng xuất thành công")
                .build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới Access Token thông qua Refresh Token")
    public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ApiResponse.<TokenResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Làm mới token thành công")
                .result(response)
                .build();
    }
}
