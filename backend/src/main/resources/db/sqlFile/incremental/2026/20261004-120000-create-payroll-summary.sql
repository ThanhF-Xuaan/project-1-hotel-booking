-- ==============================================================================
-- INCREMENTAL MIGRATION: TẠO BẢNG PAYROLL_SUMMARY (TASK T05)
-- Quản lý bảng lương nhân viên import từ tệp CSV
-- ==============================================================================

CREATE TABLE IF NOT EXISTS payroll_summary (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    staff_id        INT NOT NULL REFERENCES staffs(id),
    period_month    INT NOT NULL,
    period_year     INT NOT NULL,
    base_salary     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    bonus           NUMERIC(15, 2) NOT NULL DEFAULT 0,
    deductions      NUMERIC(15, 2) NOT NULL DEFAULT 0,
    net_salary      NUMERIC(15, 2) NOT NULL DEFAULT 0,

    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Chỉ mục tối ưu truy vấn tra cứu lương
CREATE INDEX IF NOT EXISTS idx_payroll_summary_staff ON payroll_summary(staff_id);
CREATE INDEX IF NOT EXISTS idx_payroll_summary_period ON payroll_summary(period_month, period_year);

-- Trigger tự động cập nhật updated_at
DROP TRIGGER IF EXISTS trg_payroll_summary_updated_at ON payroll_summary;
CREATE TRIGGER trg_payroll_summary_updated_at
    BEFORE UPDATE ON payroll_summary
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Phân quyền RBAC chuẩn qua bảng permissions và role_permissions
INSERT INTO permissions (action, resource, status) VALUES
    ('IMPORT', 'PAYROLL', 'ACTIVE'),
    ('VIEW',   'PAYROLL', 'ACTIVE')
ON CONFLICT (action, resource) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('CHAIN_ADMIN', 'PROPERTY_MANAGER')
  AND p.resource = 'PAYROLL'
  AND p.action IN ('IMPORT', 'VIEW')
ON CONFLICT DO NOTHING;
