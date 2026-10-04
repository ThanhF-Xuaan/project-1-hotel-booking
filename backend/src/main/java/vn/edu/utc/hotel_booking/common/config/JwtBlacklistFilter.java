package vn.edu.utc.hotel_booking.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Kiểm tra an toàn: authHeader phải có nội dung và bắt đầu bằng "Bearer " (case-insensitive)
        if (StringUtils.hasText(authHeader) && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authHeader.substring(7).trim();

            // Chỉ kiểm tra Redis nếu token có nội dung thực tế (chống Garbage Token "Bearer  ")
            if (StringUtils.hasText(token) && tokenBlacklistService.isBlacklisted(token)) {
                log.warn("Blocked request to '{}' due to revoked / blacklisted token", request.getRequestURI());

                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");

                ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                        .code(ErrorCode.TOKEN_EXPIRED_OR_REVOKED.getCode())
                        .message(ErrorCode.TOKEN_EXPIRED_OR_REVOKED.getMessage())
                        .build();

                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return; // Chặn chuỗi Filter, không cho request đi tiếp
            }
        }

        // Cho phép đi tiếp để Spring Security BearerTokenAuthenticationFilter xử lý chuẩn OAuth2
        filterChain.doFilter(request, response);
    }
}
