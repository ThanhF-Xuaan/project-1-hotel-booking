-- ==============================================================================
-- Liquibase Incremental Migration: Bảo vệ tính bất biến của nhật ký kiểm toán (audit_logs)
-- Luật áp dụng: Luật Kế toán 2015 (Điều 41) & Luật An ninh mạng 2018 (Điều 26)
-- Tác giả: Member 2
-- ==============================================================================

CREATE OR REPLACE FUNCTION prevent_audit_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'VI PHAM PHAP LUAT (Luat Ke Toan 2015 Dieu 41): Nhat ky kiem toan audit_logs la bat bien, khong duoc phep SUA (UPDATE) hoac XOA (DELETE)!';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_audit_log_modification ON audit_logs;

CREATE TRIGGER trg_prevent_audit_log_modification
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_log_modification();
