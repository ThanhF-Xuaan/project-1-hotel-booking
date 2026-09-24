CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE ai_documents (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('PUBLIC', 'STAFF')),
    hotel_id SMALLINT REFERENCES hotels(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REVOKED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE ai_document_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES ai_documents(id),
    version_number INT NOT NULL CHECK (version_number > 0),
    content TEXT NOT NULL,
    checksum CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'READY', 'PUBLISHED', 'SUPERSEDED', 'FAILED')),
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_ai_version_number UNIQUE (document_id, version_number)
);

CREATE UNIQUE INDEX uq_ai_one_published_version
    ON ai_document_versions(document_id) WHERE status = 'PUBLISHED';

-- text-embedding-3-small defaults to 1536 dimensions. A model change needs a new migration.
CREATE TABLE ai_chunks (
    id UUID PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES ai_document_versions(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL CHECK (chunk_index >= 0),
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_ai_chunk_position UNIQUE (version_id, chunk_index)
);

CREATE TABLE ai_ingest_jobs (
    id UUID PRIMARY KEY,
    version_id UUID NOT NULL UNIQUE REFERENCES ai_document_versions(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    attempts INT NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    claimed_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_docs_hotel_visibility ON ai_documents(hotel_id, visibility, status);
CREATE INDEX idx_ai_versions_document ON ai_document_versions(document_id, status);
CREATE INDEX idx_ai_chunks_version ON ai_chunks(version_id);
CREATE INDEX idx_ai_jobs_claim ON ai_ingest_jobs(status, claimed_at, created_at);

CREATE TRIGGER set_updated_at BEFORE UPDATE ON ai_documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER set_updated_at BEFORE UPDATE ON ai_document_versions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER set_updated_at BEFORE UPDATE ON ai_chunks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER set_updated_at BEFORE UPDATE ON ai_ingest_jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

INSERT INTO permissions(action, resource)
VALUES ('USE', 'AI_ASSISTANT'), ('VIEW', 'KNOWLEDGE'), ('CREATE', 'KNOWLEDGE'),
       ('PUBLISH', 'KNOWLEDGE'), ('REVOKE', 'KNOWLEDGE')
ON CONFLICT (action, resource) DO NOTHING;
