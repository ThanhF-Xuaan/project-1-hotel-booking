CREATE TABLE IF NOT EXISTS payroll_summary (
    id BIGSERIAL PRIMARY KEY,
    staff_id INT NOT NULL,
    period_month INT NOT NULL,
    period_year INT NOT NULL,
    base_salary NUMERIC(15, 2) NOT NULL DEFAULT 0,
    bonus NUMERIC(15, 2) NOT NULL DEFAULT 0,
    deductions NUMERIC(15, 2) NOT NULL DEFAULT 0,
    net_salary NUMERIC(15, 2) NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(50),
    version BIGINT DEFAULT 0,

    CONSTRAINT fk_payroll_staff FOREIGN KEY (staff_id) REFERENCES staffs(id)
);

CREATE INDEX idx_payroll_summary_staff ON payroll_summary(staff_id);
CREATE INDEX idx_payroll_summary_period ON payroll_summary(period_month, period_year);

-- Thêm quyền RBAC cho Payroll Import
INSERT INTO roles_permissions (role_name, permission) VALUES
('ROLE_CHAIN_ADMIN', 'ROLE_PAYROLL_IMPORT'),
('ROLE_PROPERTY_MANAGER', 'ROLE_PAYROLL_IMPORT')
ON CONFLICT DO NOTHING;
