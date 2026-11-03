# Member 1 — sửa finding review ngày 03/11/2026

CONFIRMED: user duyệt kế hoạch FIX-001..007 và yêu cầu checkout/pull develop, merge trước implementation. Worktree sạch trước sửa. Đã thực hiện tuần tự; base mới `c0a375ee59be516dd589842ba9d0f79ae6c7ea05`, branch `codex/m1-review-fixes-20261103`. CI đã được team hợp nhất vào ci.yml; không khôi phục member1-ci.yml đã xóa.

Quyết định của user: EXPORTED không cho thay đổi nội dung đã xuất (409), no-op giữ nguyên; payroll không tự làm tròn số vượt NUMERIC(15,2). Sửa từng finding bằng regression FAIL → fix → PASS → review → commit. Không sửa gold, ngưỡng hoặc schema; dùng synthetic sandbox cho DB/API/browser.

Allowlist theo slice: FIX-001/002 StayGuest service, association validator và scoped read trong booking.StayGuestRepository; FIX-003 Operation scope helper và năm service Operation; FIX-004 queue/export services, repository lock, export-state helper, hai ErrorCode 409; FIX-005 payroll importer/validator; FIX-006 advisor adapter/internal resolution/orchestration; FIX-007 widget/client. Test tương ứng được thêm/mở rộng; source-provenance và CI chỉ bổ sung bằng chứng của đúng delta. Shared/member khác được user ủy quyền sửa, phải ghi file/lý do trong báo cáo.

Giữ API envelopes, vai trò, thuật toán, dữ liệu Core/Utility/demo hiện hữu. T12 không có business write. Một PR riêng, commit theo finding; peer approval và branch protection không bị thay thế bằng AI self-review. Rollback bằng revert commit và các phụ thuộc, không sửa dữ liệu nghiệp vụ.

Verification: targeted unit/controller, real PostgreSQL transaction/round-trip trong sandbox, frontend lint/build và delayed-response browser; full acceptance/CI cuối. Mỗi finding giữ NEED_FIX cho đến khi có bằng chứng đầy đủ. Report ghi base/head, file shared, FAIL/PASS/NOT RUN và Git trạng thái.
