# ==============================================================================
# QUY TẮC ĐỊNH TUYẾN DÙNG CHUNG CHO CẢ HTTP & HTTPS (COMMON LOCATIONS SNIPPET)
# LƯU Ý: Tệp này có đuôi .inc để Nginx không tự động nạp ở khối http mà chỉ
# được nạp tường minh qua chỉ thị include bên trong khối server { ... }.
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. REST API BACKEND (Spring Boot)
# ------------------------------------------------------------------------------
location /api/ {
    proxy_pass http://backend_upstream/api/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Port $server_port;

    # Timeouts dài cho các tác vụ BI Báo cáo & Hợp đồng nặng
    proxy_connect_timeout 60s;
    proxy_send_timeout 120s;
    proxy_read_timeout 120s;
}

# ------------------------------------------------------------------------------
# 2. SWAGGER UI & OPENAPI DOCS (SpringDoc)
# ------------------------------------------------------------------------------
location /swagger-ui/ {
    proxy_pass http://backend_upstream/swagger-ui/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /v3/api-docs {
    proxy_pass http://backend_upstream/v3/api-docs;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# ------------------------------------------------------------------------------
# 3. KEYCLOAK IAM (OIDC Endpoints & Resources)
# ------------------------------------------------------------------------------
location /realms/ {
    proxy_pass http://keycloak_upstream/realms/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;
    proxy_buffer_size 128k;
    proxy_buffers 4 256k;
    proxy_busy_buffers_size 256k;
}

location /resources/ {
    proxy_pass http://keycloak_upstream/resources/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# ------------------------------------------------------------------------------
# 4. S3 STATIC ASSET GATEWAY (MinIO Reverse Proxy - READ-ONLY)
# Chỉ cho phép phương thức GET và HEAD. Chặn đứng PUT/POST/DELETE từ bên ngoài.
# ------------------------------------------------------------------------------
location /storage/ {
    limit_except GET HEAD {
        deny all;
    }

    proxy_pass http://minio_storage_upstream/hotel-booking-storage/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    
    # Ẩn các header nhạy cảm của MinIO
    proxy_hide_header x-amz-request-id;
    proxy_hide_header x-minio-deployment-id;
    proxy_hide_header Server;

    # Client-side cache 1 ngày cho hình ảnh và tệp tĩnh công khai
    add_header Cache-Control "public, max-age=86400, immutable";
}

# ------------------------------------------------------------------------------
# 5. FRONTEND SPA ROUTING (React Vite Dev Server + WebSocket HMR)
# ------------------------------------------------------------------------------
location / {
    proxy_pass http://frontend_dev_upstream;
    proxy_http_version 1.1;

    # Cấu hình WebSocket HMR cho React Vite
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    proxy_cache_bypass $http_upgrade;
}
