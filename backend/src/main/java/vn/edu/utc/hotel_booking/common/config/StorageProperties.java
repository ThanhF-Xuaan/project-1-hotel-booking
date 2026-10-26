package vn.edu.utc.hotel_booking.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "hotel.storage")
public class StorageProperties {

    /** Loại cơ chế lưu trữ: s3 (MinIO / AWS S3) hoặc local (đĩa cục bộ fallback cho dev) */
    private String type = "s3";

    /** Cấu hình đường dẫn thư mục khi chạy loại local */
    private Local local = new Local();

    /** Cấu hình kết nối AWS S3 / MinIO */
    private S3 s3 = new S3();

    /** Cấu hình tích hợp quét virus */
    private Antivirus antivirus = new Antivirus();

    @Getter
    @Setter
    public static class Local {
        private String basePath = "uploads";
    }

    @Getter
    @Setter
    public static class S3 {
        private String endpoint = "http://localhost:9000";
        private String region = "us-east-1";
        private String accessKey = "admin";
        private String secretKey = "hotelbooking123";
        private String bucketName = "hotel-booking-storage";
        private boolean pathStyleAccess = true;
        private boolean autoCreateBucket = true;
        /** URL CDN công khai phục vụ truy cập nhanh ảnh public (nếu có) */
        private String cdnPublicBaseUrl;
    }

    @Getter
    @Setter
    public static class Antivirus {
        private boolean enabled = false;
        private Clamav clamav = new Clamav();

        @Getter
        @Setter
        public static class Clamav {
            private String host = "clamav";
            private int port = 3310;
        }
    }
}
