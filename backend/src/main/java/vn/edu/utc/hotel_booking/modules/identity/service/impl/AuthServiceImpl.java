package vn.edu.utc.hotel_booking.modules.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.LoginRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.LogoutRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.request.RefreshTokenRequest;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.KeycloakTokenResponse;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.TokenResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.AuthService;
import vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final TokenBlacklistService tokenBlacklistService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id:hotel-backend}")
    private String clientId;

    @Value("${keycloak.client-secret:hotel-backend-secret-key-123456}")
    private String clientSecret;

    private String getTokenEndpoint() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    private String getLogoutEndpoint() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        log.info("Processing login request for user: {}", request.getUsername());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", request.getUsername().trim());
        body.add("password", request.getPassword());
        body.add("scope", "openid profile email roles");

        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
                    getTokenEndpoint(),
                    httpEntity,
                    KeycloakTokenResponse.class
            );

            KeycloakTokenResponse keycloakToken = response.getBody();
            if (keycloakToken == null || keycloakToken.getAccessToken() == null) {
                log.error("Empty token response from Keycloak for user: {}", request.getUsername());
                throw new AppException(ErrorCode.KEYCLOAK_COMMUNICATION_ERROR);
            }

            log.info("User '{}' logged in successfully via Keycloak", request.getUsername());
            return TokenResponse.builder()
                    .accessToken(keycloakToken.getAccessToken())
                    .expiresIn(keycloakToken.getExpiresIn())
                    .refreshExpiresIn(keycloakToken.getRefreshExpiresIn())
                    .refreshToken(keycloakToken.getRefreshToken())
                    .tokenType(keycloakToken.getTokenType())
                    .scope(keycloakToken.getScope())
                    .build();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("Invalid credentials for user: {} (Keycloak response: {})", request.getUsername(), e.getResponseBodyAsString());
                throw new AppException(ErrorCode.INVALID_CREDENTIALS);
            }
            log.error("HTTP error from Keycloak during login for user: {}", request.getUsername(), e);
            throw new AppException(ErrorCode.KEYCLOAK_COMMUNICATION_ERROR);
        } catch (ResourceAccessException e) {
            log.error("Unable to connect to Keycloak IAM server at: {}", getTokenEndpoint(), e);
            throw new AppException(ErrorCode.KEYCLOAK_COMMUNICATION_ERROR);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", request.getUsername(), e);
            throw new AppException(ErrorCode.KEYCLOAK_COMMUNICATION_ERROR);
        }
    }

    @Override
    public void logout(String authHeader, LogoutRequest request) {
        log.info("Processing logout request");

        // 1. Thu hồi phiên trên Keycloak (hủy refresh token)
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("client_id", clientId);
                body.add("client_secret", clientSecret);
                body.add("refresh_token", request.getRefreshToken().trim());

                HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, headers);
                restTemplate.postForEntity(getLogoutEndpoint(), httpEntity, Void.class);
                log.info("Keycloak session revoked successfully for refresh token");
            } catch (Exception e) {
                log.warn("Keycloak logout endpoint returned error (token may already be invalid/expired): {}", e.getMessage());
            }
        }

        // 2. Đưa Access Token hiện tại vào Redis Blacklist
        if (authHeader != null && !authHeader.isBlank()) {
            tokenBlacklistService.blacklistToken(authHeader);
        }
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Processing refresh token request");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", request.getRefreshToken().trim());

        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
                    getTokenEndpoint(),
                    httpEntity,
                    KeycloakTokenResponse.class
            );

            KeycloakTokenResponse keycloakToken = response.getBody();
            if (keycloakToken == null || keycloakToken.getAccessToken() == null) {
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }

            log.info("Access token refreshed successfully");
            return TokenResponse.builder()
                    .accessToken(keycloakToken.getAccessToken())
                    .expiresIn(keycloakToken.getExpiresIn())
                    .refreshExpiresIn(keycloakToken.getRefreshExpiresIn())
                    .refreshToken(keycloakToken.getRefreshToken())
                    .tokenType(keycloakToken.getTokenType())
                    .scope(keycloakToken.getScope())
                    .build();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("Invalid refresh token (Keycloak response: {})", e.getResponseBodyAsString());
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            log.error("HTTP error from Keycloak during refresh token: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.KEYCLOAK_COMMUNICATION_ERROR);
        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.KEYCLOAK_COMMUNICATION_ERROR);
        }
    }
}
