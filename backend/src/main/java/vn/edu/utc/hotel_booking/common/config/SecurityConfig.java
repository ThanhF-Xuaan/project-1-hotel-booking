package vn.edu.utc.hotel_booking.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Mở CORS để Frontend (React/Vite) gọi được API mà không bị chặn
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Tắt CSRF (Cross-Site Request Forgery) vì REST API dùng JWT (Stateless)
                // không cần cái này
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Phân quyền các Endpoints (Cửa ngõ)
                .authorizeHttpRequests(auth -> auth
                        // Cửa 1: Cho phép tất cả mọi người vào xem tài liệu API (Swagger UI)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Cửa 2: Mở một số API công khai không cần đăng nhập (vd: xem phòng trống)
                        .requestMatchers("/api/public/**").permitAll()

                        // Cửa 3: Tất cả các API còn lại bắt buộc phải có Token (JWT) hợp lệ
                        .anyRequest().authenticated())

                // 4. Bật chế độ OAuth2 Resource Server để Spring Boot tự động xác thực JWT
                // Token qua Keycloak
                .oauth2ResourceServer(oauth2 -> oauth2
                        // (Custom JWT converter sẽ cấu hình sau để đọc Roles từ Keycloak, tạm thời cứ
                        // để mặc định)
                        .jwt(jwt -> {
                        }))

                // 5. Cấu hình Stateless Session (Server không thèm nhớ User là ai, bắt buộc
                // phải gửi Token mỗi lần request)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    // CẤU HÌNH CHI TIẾT CHO CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // CHỈ ĐỊNH ĐÍCH DANH CỔNG CỦA FRONTEND ĐƯỢC PHÉP VÀO
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // CÁC HÀNH ĐỘNG ĐƯỢC PHÉP
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // CÁC THÔNG TIN ĐƯỢC PHÉP KÈM THEO TRONG HEADER (đặc biệt là Authorization chứa
        // Token)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // Cho phép gửi kèm cookie hoặc thông tin xác thực nếu cần
        configuration.setAllowCredentials(true);

        // Áp dụng luật CORS này cho TOÀN BỘ API
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}