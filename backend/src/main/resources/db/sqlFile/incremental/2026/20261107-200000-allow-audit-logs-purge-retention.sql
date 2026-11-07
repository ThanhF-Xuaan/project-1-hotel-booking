-- ==============================================================================
-- Liquibase Incremental Migration: Cập nhật cơ chế bảo vệ audit_logs
-- Cho phép xóa (DELETE) các bản ghi cũ ngoài top 50 (Retention Policy)
-- Vẫn cấm tuyệt đối chỉnh sửa (UPDATE) để bảo vệ tính toàn vẹn kiểm toán
-- ==============================================================================

DROP TRIGGER IF EXISTS trg_prevent_audit_log_modification ON audit_logs;

CREATE OR REPLACE FUNCTION prevent_audit_log_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Nhat ky kiem toan audit_logs la bat bien, khong duoc phep SUA (UPDATE)!';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_audit_log_update ON audit_logs;

CREATE TRIGGER trg_prevent_audit_log_update
BEFORE UPDATE ON audit_logs
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_log_update();

