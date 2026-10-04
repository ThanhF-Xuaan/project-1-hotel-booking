package vn.edu.utc.hotel_booking.modules.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.LoginRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.LogoutRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RefreshTokenRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.TokenResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Đăng nhập thành công")
    void login_Success() throws Exception {
        LoginRequest request = LoginRequest.builder().username("admin").password("123456").build();
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access_token_mock")
                .refreshToken("refresh_token_mock")
                .expiresIn(3600L)
                .tokenType("Bearer")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.access_token").value("access_token_mock"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Đăng xuất thành công")
    void logout_Success() throws Exception {
        LogoutRequest request = LogoutRequest.builder().refreshToken("refresh_token_mock").build();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access_token_mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

        verify(authService).logout(eq("Bearer access_token_mock"), any(LogoutRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Làm mới token thành công")
    void refresh_Success() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("refresh_token_mock").build();
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new_access_token")
                .expiresIn(3600L)
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.access_token").value("new_access_token"));
    }
}
