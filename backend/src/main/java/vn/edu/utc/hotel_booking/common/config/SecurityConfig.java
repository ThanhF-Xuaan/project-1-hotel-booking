package vn.edu.utc.hotel_booking.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Mở CORS để Frontend (React/Vite) gọi được API mà không bị chặn
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Tắt CSRF (Cross-Site Request Forgery) vì REST API dùng JWT (Stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Phân quyền các Endpoints (Cửa ngõ)
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập tài liệu API (Swagger UI / OpenAPI)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Mở các API công khai không cần đăng nhập (vd: xem phòng trống, danh mục)
                        .requestMatchers("/api/public/**", "/api/v1/public/**").permitAll()

                        // Tất cả các API nghiệp vụ còn lại bắt buộc phải có Token (JWT) hợp lệ
                        .anyRequest().authenticated())

                // 4. Bật chế độ OAuth2 Resource Server với Custom JWT Role Converter
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())))

                // 5. Cấu hình Stateless Session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }

    // CẤU HÌNH CHI TIẾT CHO CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // CHỈ ĐỊNH ĐÍCH DANH CỔNG CỦA FRONTEND (Docker: 3000, Local Dev: 5173)
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173"
        ));

        // CÁC HÀNH ĐỘNG ĐƯỢC PHÉP
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // CÁC THÔNG TIN ĐƯỢC PHÉP KÈM THEO TRONG HEADER
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));

        // Cho phép gửi kèm cookie hoặc thông tin xác thực nếu cần
        configuration.setAllowCredentials(true);

        // Áp dụng luật CORS này cho TOÀN BỘ API
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}