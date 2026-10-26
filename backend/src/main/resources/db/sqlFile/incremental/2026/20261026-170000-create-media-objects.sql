-- Liquibase SQL: Tao bang media_objects quan ly metadata, vong doi va bao mat tep tin S3/MinIO
CREATE TABLE IF NOT EXISTS media_objects (
    id BIGSERIAL PRIMARY KEY,
    storage_key VARCHAR(500) NOT NULL,
    bucket_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    access_level VARCHAR(30) NOT NULL DEFAULT 'PRIVATE_RESTRICTED',
    hotel_id SMALLINT NULL,
    reference_id BIGINT NULL,
    uploaded_by VARCHAR(100) NOT NULL,
    scan_status VARCHAR(30) NOT NULL DEFAULT 'CLEAN',
    scan_result TEXT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_media_objects_storage_key UNIQUE (storage_key)
);

CREATE INDEX IF NOT EXISTS idx_media_objects_category_status ON media_objects(category, lifecycle_status) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_media_objects_hotel_id ON media_objects(hotel_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_media_objects_reference ON media_objects(category, reference_id) WHERE is_deleted = FALSE;
