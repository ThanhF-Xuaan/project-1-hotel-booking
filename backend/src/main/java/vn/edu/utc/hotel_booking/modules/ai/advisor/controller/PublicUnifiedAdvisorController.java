package vn.edu.utc.hotel_booking.modules.ai.advisor.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.UnifiedAdvisorRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.UnifiedAdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.error.RoomAdvisorErrorResponse;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RateLimitExceededException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.ResolvedAdvisorOwner;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorPrincipalExtractor;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorRateLimiter;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionCookieFactory;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionTokenService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.AdvisorDependencyUnavailableException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.UnifiedAdvisorService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionNotFoundException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionSecurityException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionStorageUnavailableException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;

import java.util.Objects;

/**
 * Public controller for the unified conversational flow (/converse), combining
 * room discovery recommendations and grounded hotel knowledge lookup.
 *
 * <p>Isolated from {@link PublicRoomAdvisorController} to preserve the frozen R1 reflection contract.
 * Activated conditionally via property {@code hotel.ai.advisor.unified.enabled=true}.</p>
 */
@RestController
@RequestMapping("/api/v1/public/ai/room-advisor")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hotel.ai.advisor.unified.enabled", havingValue = "true")
public class PublicUnifiedAdvisorController {

    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final UnifiedAdvisorService unifiedAdvisorService;
    private final RoomAdvisorPrincipalExtractor principalExtractor;
    private final RoomAdvisorSessionCookieFactory sessionCookieFactory;
    private final RoomAdvisorRateLimiter rateLimiter;

    @PostMapping("/converse")
    public ResponseEntity<ApiResponse<UnifiedAdvisorResultDto>> converse(
            @Valid @RequestBody UnifiedAdvisorRequestDto request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        ResolvedAdvisorOwner owner = principalExtractor.resolveOwner(servletRequest);
        enforceRateLimit(owner, servletRequest);
        UnifiedAdvisorResultDto result = unifiedAdvisorService.converse(request, owner.ownerKey());
        writeSessionCookie(owner, servletResponse);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ExceptionHandler({SessionNotFoundException.class, SessionSecurityException.class})
    public ResponseEntity<RoomAdvisorErrorResponse> handleSessionNotFound() {
        return error(HttpStatus.NOT_FOUND, RoomAdvisorErrorResponse.Reason.SESSION_NOT_FOUND);
    }

    @ExceptionHandler(SessionVersionConflictException.class)
    public ResponseEntity<RoomAdvisorErrorResponse> handleSessionVersionConflict() {
        return error(HttpStatus.CONFLICT, RoomAdvisorErrorResponse.Reason.SESSION_VERSION_CONFLICT);
    }

    @ExceptionHandler({SessionStorageUnavailableException.class, AdvisorDependencyUnavailableException.class})
    public ResponseEntity<RoomAdvisorErrorResponse> handleStorageUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(RoomAdvisorErrorResponse.forReason(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        RoomAdvisorErrorResponse.Reason.UPSTREAM_UNAVAILABLE,
                        true));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RoomAdvisorErrorResponse> handleScopeDenied() {
        return error(HttpStatus.FORBIDDEN, RoomAdvisorErrorResponse.Reason.SCOPE_DENIED);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<RoomAdvisorErrorResponse> handleInvalidRequest() {
        return error(HttpStatus.BAD_REQUEST, RoomAdvisorErrorResponse.Reason.INVALID_REQUEST);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<RoomAdvisorErrorResponse> handleRateLimited(RateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(RETRY_AFTER_HEADER, Integer.toString(exception.retryAfterSeconds()))
                .body(RoomAdvisorErrorResponse.forReason(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        RoomAdvisorErrorResponse.Reason.RATE_LIMITED,
                        true));
    }

    private void enforceRateLimit(ResolvedAdvisorOwner owner, HttpServletRequest request) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(request, "request must not be null");
        rateLimiter.checkAllowed(
                owner.ownerKey(),
                owner.tokenSource() == ResolvedAdvisorOwner.TokenSource.AUTHENTICATED,
                request.getRemoteAddr());
    }

    private void writeSessionCookie(ResolvedAdvisorOwner owner, HttpServletResponse response) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(response, "response must not be null");
        owner.tokenToWrite().ifPresent(token -> response.addHeader(
                HttpHeaders.SET_COOKIE,
                sessionCookieFactory
                        .build(token, RoomAdvisorSessionTokenService.DEFAULT_SESSION_TTL)
                        .toString()));
    }

    private static ResponseEntity<RoomAdvisorErrorResponse> error(
            HttpStatus status,
            RoomAdvisorErrorResponse.Reason reason
    ) {
        return ResponseEntity.status(status)
                .body(RoomAdvisorErrorResponse.forReason(status.value(), reason, false));
    }
}
