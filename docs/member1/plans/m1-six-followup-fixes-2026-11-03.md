# Sáu follow-up finding — kế hoạch được user duyệt

CONFIRMED base332dfbc, branch codex/m1-review-fixes-20261103 sạch; đã checkout/pull develop và merge vào nhánh trước implementation. User cho phép shared/member khác, commit/push và PR theo workflow đã có. Không thay human review/branch policy.

Thứ tự F01 snapshot/CAS → F02 header payroll → F05 thời gian StayGuest → F06 stale UI → F04 meter concurrency → F03 active-stay dedup/migration. CSV đúng7header theo thứ tự; dedup có service lock + DBconstraint, chỉ đã check-in/chưa checkout/chưa xóa; placeholder N/A không phải định danh. Giữ EXPORTED, scopes, gold và các thuật toán.

Allowlist: Advisor orchestration/session/controller và snapshot helper/tests; Payroll importer/header helper/tests; StayGuest service/temporal helper/repository/entity và dedup helper/migration/tests; UtilityReading/UtilityMeter locking/repository/tests; StayGuestsModal/client/E2E; ErrorCode; provenance/CI/acceptance wiring và báo cáo. Shared files phải ghi lý do ở acceptance report.

Mỗi slice regression FAIL → fix → PASS → review → commit. PostgreSQL/Redis integration trong sandbox độc lập có backup/restore; migration explicit context @m1-active-stay-dedup. Không production DB hoặc paid provider. Preflight dữ liệu trùng thì dừng, không tự xóa/gộp. Receipt mới compose đúng delta từ332dfbc, giữ receipt/gold cũ. Rollback ưu tiên ứng dụng, giữ chốt DB; DDL rollback chỉ khi dừng writers và backup.

OBS-008 cũ là false positive: LodgingQueue dùng @Convert ERROR↔FAILED; lỗi SQL trực tiếp ERROR của fixture không chứng minh JPA persistence sai. Giữ lịch sử báo cáo nhưng rút kết luận cần đổi schema trạng thái.
