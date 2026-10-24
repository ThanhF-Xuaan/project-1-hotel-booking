package vn.edu.utc.hotel_booking.modules.ai.advisor.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorChatRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorRefreshRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorSearchRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.error.RoomAdvisorErrorResponse;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RateLimitExceededException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.ResolvedAdvisorOwner;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorPrincipalExtractor;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorRateLimiter;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionCookieFactory;
import vn.edu.utc.hotel_booking.modules.ai.advisor.security.RoomAdvisorSessionTokenService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.RoomAdvisorOrchestrationService;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionNotFoundException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionSecurityException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionStorageUnavailableException;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.SessionVersionConflictException;

import java.util.Objects;

/**
 * Public Room Advisor entry points.
 *
 * <p>Owner identity is resolved exclusively by {@link RoomAdvisorPrincipalExtractor}; no request
 * header, raw bearer value, client IP or user agent participates in ownership. The controller stays
 * thin: session lifecycle remains in {@link RoomAdvisorOrchestrationService}.</p>
 *
 * <p>Anonymous cookies are written only after a successful orchestration call, exactly once, and
 * only when the extractor minted or renewed a token. Failed calls (notably session 404s) never emit
 * a cookie.</p>
 *
 * <p>Every request is rate limited before any expensive orchestration work starts, using the
 * bounded in-memory {@link RoomAdvisorRateLimiter} (ADR-04). A rejected request is answered with
 * HTTP 429 and never triggers a cookie issuance.</p>
 */
@RestController
@RequestMapping("/api/v1/public/ai/room-advisor")
@RequiredArgsConstructor
public class PublicRoomAdvisorController {

    /** Additive header carrying the remaining wait time; not part of the frozen contract. */
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final RoomAdvisorOrchestrationService orchestrationService;
    private final RoomAdvisorPrincipalExtractor principalExtractor;
    private final RoomAdvisorSessionCookieFactory sessionCookieFactory;
    private final RoomAdvisorRateLimiter rateLimiter;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AdvisorResultDto>> chat(
            @Valid @RequestBody AdvisorChatRequestDto request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        ResolvedAdvisorOwner owner = principalExtractor.resolveOwner(servletRequest);
        enforceRateLimit(owner, servletRequest);
        AdvisorResultDto result = orchestrationService.chat(request, owner.ownerKey());
        writeSessionCookie(owner, servletResponse);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<AdvisorResultDto>> search(
            @Valid @RequestBody AdvisorSearchRequestDto request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        ResolvedAdvisorOwner owner = principalExtractor.resolveOwner(servletRequest);
        enforceRateLimit(owner, servletRequest);
        AdvisorResultDto result = orchestrationService.search(request, owner.ownerKey());
        writeSessionCookie(owner, servletResponse);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AdvisorResultDto>> refresh(
            @Valid @RequestBody AdvisorRefreshRequestDto request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        ResolvedAdvisorOwner owner = principalExtractor.resolveOwner(servletRequest);
        enforceRateLimit(owner, servletRequest);
        AdvisorResultDto result = orchestrationService.refresh(request, owner.ownerKey());
        writeSessionCookie(owner, servletResponse);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Missing, expired and foreign-owner sessions deliberately share one response so clients cannot
     * distinguish them. Only the random request identifier may differ between calls.
     */
    @ExceptionHandler({SessionNotFoundException.class, SessionSecurityException.class})
    public ResponseEntity<RoomAdvisorErrorResponse> handleSessionNotFound() {
        return error(HttpStatus.NOT_FOUND, RoomAdvisorErrorResponse.Reason.SESSION_NOT_FOUND);
    }

    @ExceptionHandler(SessionVersionConflictException.class)
    public ResponseEntity<RoomAdvisorErrorResponse> handleSessionVersionConflict() {
        return error(HttpStatus.CONFLICT, RoomAdvisorErrorResponse.Reason.SESSION_VERSION_CONFLICT);
    }

    @ExceptionHandler(SessionStorageUnavailableException.class)
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

    /**
     * Rate limited requests are answered with the advisor-local error envelope and the additive
     * {@code Retry-After} header. The body never states which budget was exhausted.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<RoomAdvisorErrorResponse> handleRateLimited(RateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(RETRY_AFTER_HEADER, Integer.toString(exception.retryAfterSeconds()))
                .body(RoomAdvisorErrorResponse.forReason(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        RoomAdvisorErrorResponse.Reason.RATE_LIMITED,
                        true));
    }

    /**
     * Rejects a request before any expensive orchestration work happens.
     *
     * <p>The client address is the one resolved by the container; forwarded headers are never
     * consulted (ADR-04).</p>
     */
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
