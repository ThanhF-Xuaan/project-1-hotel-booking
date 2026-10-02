-- Liquibase SQL: Tạo bảng Dead Letter Queue lưu vết task đồng bộ Keycloak thất bại
CREATE TABLE IF NOT EXISTS keycloak_sync_dead_letters (
    id BIGSERIAL PRIMARY KEY,
    keycloak_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    payload TEXT,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 3,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_keycloak_dead_letters_status_created 
    ON keycloak_sync_dead_letters (status, created_at);

CREATE INDEX IF NOT EXISTS idx_keycloak_dead_letters_keycloak_id 
    ON keycloak_sync_dead_letters (keycloak_id);

-- Tạo bảng shedlock cho khóa phân tán multi-node Kubernetes
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
