-- PROPOSED Member 1 supporting objects for a FRESH schema only.
-- Not master data, not an upgrade, not registered in Liquibase.
-- Run only after owner approval and Module 9-10 tables on the same approved target.
-- Requires existing update_updated_at_column() and pgvector installed by the DB owner.
-- Do not apply these objects twice or on top of existing Liquibase-managed objects.
-- Contains no seed data, function definitions, extension installation or business writes.
BEGIN;

-- Source: 20261003-150000-create-utility-tables.sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_utility_meter_code_active
    ON utility_meters (hotel_id, meter_code) WHERE is_deleted = FALSE;

-- Source: 20261003-150000-create-utility-tables.sql
CREATE INDEX IF NOT EXISTS idx_utility_meters_hotel_type
    ON utility_meters (hotel_id, meter_type);

-- Source: 20261003-150000-create-utility-tables.sql
CREATE TRIGGER trg_utility_meters_updated_at
    BEFORE UPDATE ON utility_meters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Source: 20261003-150000-create-utility-tables.sql
CREATE TRIGGER trg_utility_readings_updated_at
    BEFORE UPDATE ON utility_readings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Source: 20261004-120000-create-payroll-summary.sql
CREATE INDEX IF NOT EXISTS idx_payroll_summary_staff ON payroll_summary(staff_id);

-- Source: 20261004-120000-create-payroll-summary.sql
CREATE INDEX IF NOT EXISTS idx_payroll_summary_period ON payroll_summary(period_month, period_year);

-- Source: 20261004-120000-create-payroll-summary.sql
CREATE TRIGGER trg_payroll_summary_updated_at
    BEFORE UPDATE ON payroll_summary
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_personalization_binding_active_subject
    ON personalization_account_bindings (issuer, subject)
    WHERE status = 'ACTIVE';

-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_personalization_binding_active_guest
    ON personalization_account_bindings (guest_id)
    WHERE status = 'ACTIVE';

-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE INDEX IF NOT EXISTS idx_personalization_binding_guest
    ON personalization_account_bindings (guest_id);

-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE TRIGGER trg_personalization_binding_updated_at
    BEFORE UPDATE ON personalization_account_bindings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE INDEX IF NOT EXISTS idx_personalization_consents_hotel
    ON personalization_consents (hotel_id);

-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE TRIGGER trg_personalization_consent_updated_at
    BEFORE UPDATE ON personalization_consents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE INDEX IF NOT EXISTS idx_personalization_consent_events_binding_version
    ON personalization_consent_events (binding_id, binding_version);

-- Source: 20261026-160000-create-u14-local-binding-consent.sql
CREATE INDEX IF NOT EXISTS idx_personalization_consent_events_hotel
    ON personalization_consent_events (hotel_id);

-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE INDEX idx_rag_sources_hotel ON rag_sources(hotel_id);

-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE UNIQUE INDEX uq_rag_document_active ON rag_document_versions(source_key) WHERE status = 'ACTIVE';

-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE INDEX idx_rag_document_scope_status ON rag_document_versions(source_key, status, effective_from, effective_to);

-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE TRIGGER trg_rag_config_updated BEFORE UPDATE ON rag_store_config FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE TRIGGER trg_rag_sources_updated BEFORE UPDATE ON rag_sources FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE TRIGGER trg_rag_documents_updated BEFORE UPDATE ON rag_document_versions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Source: 20261030-230000-create-rag-pgvector-store.sql
CREATE TRIGGER trg_rag_chunks_updated BEFORE UPDATE ON rag_chunks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Source: 20261017-200000-fix-utility-reading-soft-delete-unique.sql
CREATE UNIQUE INDEX uq_utility_reading_active_per_day
    ON utility_readings (meter_id, reading_date) WHERE is_deleted = FALSE;

-- Source: 20261018-120000-add-payroll-summary-unique-index.sql
CREATE UNIQUE INDEX uq_payroll_summary_staff_period_active
ON payroll_summary (staff_id, period_month, period_year)
WHERE is_deleted = FALSE;

COMMIT;
