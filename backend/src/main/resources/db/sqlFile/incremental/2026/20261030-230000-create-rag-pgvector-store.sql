CREATE TABLE rag_store_config (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    embedding_model_id VARCHAR(128) NOT NULL,
    embedding_dimension INTEGER NOT NULL CHECK (embedding_dimension BETWEEN 1 AND 16000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(embedding_model_id, embedding_dimension)
);
CREATE TABLE rag_sources (
    id BIGSERIAL PRIMARY KEY,
    scope_type VARCHAR(32) NOT NULL CHECK (scope_type IN ('HOTEL_SCOPED', 'CHAIN_PUBLIC')),
    hotel_id SMALLINT REFERENCES hotels(id),
    hotel_key SMALLINT GENERATED ALWAYS AS (coalesce(hotel_id, 0)) STORED,
    source_id VARCHAR(128) NOT NULL CHECK (source_id ~ '^[A-Z0-9][A-Z0-9._:-]{0,127}$'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((scope_type = 'HOTEL_SCOPED' AND hotel_id IS NOT NULL AND hotel_id > 0) OR (scope_type = 'CHAIN_PUBLIC' AND hotel_id IS NULL)),
    UNIQUE(scope_type, hotel_key, source_id)
);
CREATE INDEX idx_rag_sources_hotel ON rag_sources(hotel_id);
CREATE TABLE rag_document_versions (
    id UUID PRIMARY KEY,
    source_key BIGINT NOT NULL REFERENCES rag_sources(id),
    version INTEGER NOT NULL CHECK (version > 0),
    status VARCHAR(32) NOT NULL CHECK (status IN ('STAGED', 'ACTIVE', 'SUPERSEDED', 'ROLLED_BACK')),
    embedding_model_id VARCHAR(128) NOT NULL,
    embedding_dimension INTEGER NOT NULL CHECK (embedding_dimension BETWEEN 1 AND 16000),
    content_hash CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    effective_from DATE,
    effective_to DATE,
    payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object' AND octet_length(payload::text) <= 270336),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    activated_at TIMESTAMPTZ,
    CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from),
    FOREIGN KEY(embedding_model_id, embedding_dimension) REFERENCES rag_store_config(embedding_model_id, embedding_dimension),
    UNIQUE(source_key, version),
    UNIQUE(id, embedding_dimension)
);
CREATE UNIQUE INDEX uq_rag_document_active ON rag_document_versions(source_key) WHERE status = 'ACTIVE';
CREATE INDEX idx_rag_document_scope_status ON rag_document_versions(source_key, status, effective_from, effective_to);
CREATE TABLE rag_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    embedding_dimension INTEGER NOT NULL,
    embedding vector NOT NULL,
    payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object' AND octet_length(payload::text) <= 34816),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY(document_id, embedding_dimension) REFERENCES rag_document_versions(id, embedding_dimension) ON DELETE CASCADE,
    CHECK (vector_dims(embedding) = embedding_dimension),
    UNIQUE(document_id, chunk_index)
);
CREATE TRIGGER trg_rag_config_updated BEFORE UPDATE ON rag_store_config FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_rag_sources_updated BEFORE UPDATE ON rag_sources FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_rag_documents_updated BEFORE UPDATE ON rag_document_versions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_rag_chunks_updated BEFORE UPDATE ON rag_chunks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
