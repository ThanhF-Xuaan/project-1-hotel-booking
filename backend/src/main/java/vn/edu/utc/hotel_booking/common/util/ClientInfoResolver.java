package vn.edu.utc.hotel_booking.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Tiện ích bóc tách thông tin địa chỉ IP nguồn và User-Agent của Client.
 * Tuân thủ quy định lưu vết tại Điều 26 Luật An ninh mạng 2018 và Nghị định 53/2022/NĐ-CP.
 */
@UtilityClass
public class ClientInfoResolver {

    public record ClientInfo(String ipAddress, String userAgent) {}

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
            "X-Real-IP"
    };

    /**
     * Lấy thông tin ClientInfo từ ngữ cảnh HTTP hiện tại.
     * Nếu chạy ngoài Web context (Async task / Scheduled job), trả về thông tin hệ thống mặc định.
     */
    public static ClientInfo getCurrentClientInfo() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            String ip = getClientIp(request);
            String ua = getUserAgent(request);
            return new ClientInfo(ip, ua);
        }
        return new ClientInfo("127.0.0.1", "SYSTEM_BACKGROUND_JOB");
    }

    /**
     * Bóc tách IP thực của client, hỗ trợ các lớp Reverse Proxy / Cloudflare / Load Balancer.
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (StringUtils.hasText(ipList) && !"unknown".equalsIgnoreCase(ipList.trim())) {
                // Header X-Forwarded-For có thể chứa danh sách: client, proxy1, proxy2
                String firstIp = ipList.split(",")[0].trim();
                return normalizeIp(firstIp);
            }
        }

        return normalizeIp(request.getRemoteAddr());
    }

    /**
     * Lấy chuỗi User-Agent từ request header.
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN_CLIENT";
        }
        String ua = request.getHeader("User-Agent");
        return StringUtils.hasText(ua) ? ua.trim() : "UNKNOWN_CLIENT";
    }

    private static String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "127.0.0.1";
        }
        String clean = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(clean) || "::1".equals(clean)) {
            return "127.0.0.1";
        }
        return clean;
    }
}
