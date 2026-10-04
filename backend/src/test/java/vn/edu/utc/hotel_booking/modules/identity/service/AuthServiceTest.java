package vn.edu.utc.hotel_booking.modules.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.LoginRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.LogoutRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RefreshTokenRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.KeycloakTokenResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.TokenResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.impl.AuthServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "serverUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(authService, "realm", "hotel-realm");
        ReflectionTestUtils.setField(authService, "clientId", "hotel-backend");
        ReflectionTestUtils.setField(authService, "clientSecret", "secret123");
        ReflectionTestUtils.setField(authService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("Login success returns TokenResponse from Keycloak")
    void login_Success() {
        LoginRequest request = LoginRequest.builder().username("admin").password("123456").build();

        KeycloakTokenResponse kcResponse = new KeycloakTokenResponse();
        kcResponse.setAccessToken("access_123");
        kcResponse.setRefreshToken("refresh_123");
        kcResponse.setExpiresIn(3600L);
        kcResponse.setTokenType("Bearer");

        when(restTemplate.postForEntity(anyString(), any(), eq(KeycloakTokenResponse.class)))
                .thenReturn(new ResponseEntity<>(kcResponse, HttpStatus.OK));

        TokenResponse result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access_123");
        assertThat(result.getRefreshToken()).isEqualTo("refresh_123");
        assertThat(result.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Login failure with 401 throws INVALID_CREDENTIALS")
    void login_InvalidCredentials_ThrowsException() {
        LoginRequest request = LoginRequest.builder().username("wrong").password("wrong").build();

        when(restTemplate.postForEntity(anyString(), any(), eq(KeycloakTokenResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Logout blacklists access token and revokes refresh token")
    void logout_Success() {
        LogoutRequest logoutRequest = LogoutRequest.builder().refreshToken("refresh_123").build();
        String authHeader = "Bearer access_123";

        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        authService.logout(authHeader, logoutRequest);

        verify(tokenBlacklistService, times(1)).blacklistToken(authHeader);
    }

    @Test
    @DisplayName("Refresh token success returns new TokenResponse")
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("valid_refresh_token").build();

        KeycloakTokenResponse kcResponse = new KeycloakTokenResponse();
        kcResponse.setAccessToken("new_access_123");
        kcResponse.setRefreshToken("new_refresh_123");
        kcResponse.setExpiresIn(3600L);
        kcResponse.setTokenType("Bearer");

        when(restTemplate.postForEntity(anyString(), any(), eq(KeycloakTokenResponse.class)))
                .thenReturn(new ResponseEntity<>(kcResponse, HttpStatus.OK));

        TokenResponse result = authService.refreshToken(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("new_access_123");
        assertThat(result.getRefreshToken()).isEqualTo("new_refresh_123");
    }
}
