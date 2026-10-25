package vn.edu.utc.hotel_booking.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientInfoResolver — IP & User-Agent Resolution Unit Tests")
class ClientInfoResolverTest {

    @Test
    @DisplayName("X-Forwarded-For có nhiều IP qua proxy -> Lấy IP đầu tiên của client thực")
    void xForwardedFor_multipleIps_shouldTakeFirst() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "113.190.234.56, 10.0.0.1, 192.168.1.1");

        String ip = ClientInfoResolver.getClientIp(request);
        assertThat(ip).isEqualTo("113.190.234.56");
    }

    @Test
    @DisplayName("X-Real-IP đơn lẻ -> Lấy chính xác IP")
    void xRealIp_shouldReturnIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "14.232.188.99");

        String ip = ClientInfoResolver.getClientIp(request);
        assertThat(ip).isEqualTo("14.232.188.99");
    }

    @Test
    @DisplayName("Fallback remoteAddr khi không có header proxy")
    void fallbackRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");

        String ip = ClientInfoResolver.getClientIp(request);
        assertThat(ip).isEqualTo("192.168.1.50");
    }

    @Test
    @DisplayName("IPv6 localhost 0:0:0:0:0:0:0:1 -> Chuẩn hóa thành 127.0.0.1")
    void ipv6Localhost_shouldNormalize() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        String ip = ClientInfoResolver.getClientIp(request);
        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("User-Agent từ header HTTP")
    void userAgent_fromHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");

        String ua = ClientInfoResolver.getUserAgent(request);
        assertThat(ua).contains("Chrome/120.0.0.0");
    }

    @Test
    @DisplayName("Khi chạy ngoài Web Context (RequestContextHolder null) -> Fallback SYSTEM_BACKGROUND_JOB")
    void outsideWebContext_shouldFallback() {
        RequestContextHolder.resetRequestAttributes();

        ClientInfoResolver.ClientInfo info = ClientInfoResolver.getCurrentClientInfo();
        assertThat(info.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(info.userAgent()).isEqualTo("SYSTEM_BACKGROUND_JOB");
    }

    @Test
    @DisplayName("Khi chạy trong Web Context -> Lấy đúng IP và User-Agent")
    void insideWebContext_shouldResolveCorrectly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "42.115.88.12");
        request.addHeader("User-Agent", "PostmanRuntime/7.36.0");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            ClientInfoResolver.ClientInfo info = ClientInfoResolver.getCurrentClientInfo();
            assertThat(info.ipAddress()).isEqualTo("42.115.88.12");
            assertThat(info.userAgent()).isEqualTo("PostmanRuntime/7.36.0");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
