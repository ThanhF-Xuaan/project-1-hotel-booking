package vn.edu.utc.hotel_booking.modules.ai.advisor.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorChatRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorRefreshRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorSearchRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomAdvisorOrchestrationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionNotFoundException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionSecurityException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/ai/room-advisor")
@RequiredArgsConstructor
public class PublicRoomAdvisorController {

    private final RoomAdvisorOrchestrationService orchestrationService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AdvisorResultDto>> chat(
            @Valid @RequestBody AdvisorChatRequestDto request,
            HttpServletRequest servletRequest
    ) {
        String ownerKey = extractOwnerKey(servletRequest);
        AdvisorResultDto result = orchestrationService.chat(request, ownerKey);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<AdvisorResultDto>> search(
            @Valid @RequestBody AdvisorSearchRequestDto request,
            HttpServletRequest servletRequest
    ) {
        String ownerKey = extractOwnerKey(servletRequest);
        AdvisorResultDto result = orchestrationService.search(request, ownerKey);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AdvisorResultDto>> refresh(
            @Valid @RequestBody AdvisorRefreshRequestDto request,
            HttpServletRequest servletRequest
    ) {
        String ownerKey = extractOwnerKey(servletRequest);
        AdvisorResultDto result = orchestrationService.refresh(request, ownerKey);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(SessionNotFoundException e) {
        log.warn("Advisor session not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                        .code(404)
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(SessionSecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurity(SessionSecurityException e) {
        log.warn("Advisor session security violation: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>builder()
                        .code(403)
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(SessionVersionConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(SessionVersionConflictException e) {
        log.warn("Advisor session version conflict: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.<Void>builder()
                        .code(409)
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        log.warn("Advisor invalid request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .code(400)
                        .message(e.getMessage())
                        .build());
    }

    private String extractOwnerKey(HttpServletRequest request) {
        if (request == null) {
            return "anonymous-owner";
        }

        // 1. Explicit owner key header (for testing or gateway-forwarded identities)
        String customOwner = request.getHeader("X-Session-Owner");
        if (customOwner != null && !customOwner.isBlank()) {
            return customOwner.trim();
        }

        // 2. Authorization header / JWT token fingerprint
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return hashString("jwt:" + authHeader.substring(7));
        }

        // 3. Cookie t12_session
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("t12_session".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return "cookie:" + cookie.getValue().trim();
                }
            }
        }

        // 4. Fallback to client IP + User-Agent fingerprint
        String remoteAddr = request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
        String userAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown-agent";
        return hashString("anon:" + remoteAddr + ":" + userAgent);
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
