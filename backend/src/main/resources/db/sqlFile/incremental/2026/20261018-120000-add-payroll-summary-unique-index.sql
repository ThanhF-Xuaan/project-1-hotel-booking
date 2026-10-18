-- ==============================================================================
-- INCREMENTAL MIGRATION: ADD UNIQUE INDEX FOR PAYROLL SUMMARY (TASK T05)
-- Ngăn ngừa trùng lặp bản ghi lương active của nhân viên trong cùng một kỳ kế toán
-- ==============================================================================

CREATE UNIQUE INDEX uq_payroll_summary_staff_period_active
ON payroll_summary (staff_id, period_month, period_year)
WHERE is_deleted = FALSE;
