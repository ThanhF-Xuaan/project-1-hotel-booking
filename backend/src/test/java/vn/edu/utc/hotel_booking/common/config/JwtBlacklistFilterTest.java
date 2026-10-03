package vn.edu.utc.hotel_booking.common.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistFilterTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtBlacklistFilter jwtBlacklistFilter;

    @Test
    @DisplayName("Filter blocks blacklisted token with 401 Unauthorized")
    void doFilter_BlacklistedToken_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked_token");
        request.setRequestURI("/api/v1/staffs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistService.isBlacklisted("revoked_token")).thenReturn(true);

        jwtBlacklistFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("1009");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter passes valid non-blacklisted token to next filter")
    void doFilter_ValidToken_Proceeds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid_token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistService.isBlacklisted("valid_token")).thenReturn(false);

        jwtBlacklistFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter gracefully handles garbage bearer header ('Bearer   ') and proceeds to filter chain")
    void doFilter_GarbageBearerToken_ProceedsToFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtBlacklistFilter.doFilterInternal(request, response, filterChain);

        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter gracefully handles missing Authorization header and proceeds to filter chain")
    void doFilter_MissingAuthHeader_ProceedsToFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtBlacklistFilter.doFilterInternal(request, response, filterChain);

        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
