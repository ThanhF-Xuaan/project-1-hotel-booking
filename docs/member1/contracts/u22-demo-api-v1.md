# U22-D01 — API occupancy demo v1

User ủy quyền triển khai và giao Git; steering mới cho phép mock trước, dữ liệu thật sau. CONFIRMED base `edebaabcd225e22c21c63107997b56a44142c39c`, worktree sạch, branch `codex/m1-u22-demo-api-20261101`. Owner Member 1 theo ủy quyền user, không giả chữ ký member khác.

## Contract và acceptance

- Subtask này chỉ API history/forecast; UI là subtask sau. Không thay thuật toán/threshold hoặc fixture/gold cũ, schema, auth chung hay endpoint business thật.
- Endpoint mới `GET /api/v1/ai/demo/occupancy?scenario=HOTEL_A|HOTEL_B`, default HOTEL_A. Chỉ nhận tối đa một query scenario; unknown/empty/duplicate/extra query trả400 với message cố định. Chọn scenario tổng hợp, không nhận hotelId/region/role/clock từ client.
- Bật đồng thời profile `m1-demo` và `hotel.ai.demo.forecast.enabled=true`; profile prod/production luôn vô hiệu. Default disabled. Auth dùng SecurityConfig/Keycloak hiện có, method chỉ ROLE_CHAIN_ADMIN. Kiểm quyền lần nữa trong port authorization của history service trước/sau read. Anonymous401; CUSTOMER/RECEPTIONIST403 khi bật; khi tắt authenticated404. Không public route mới.
- ApiResponse hiện có bao result chứa scenario, dataMode=SYNTHETIC, businessData=false, notice tiếng Việt, referenceInstant, history và forecast DTO hiện có. Mốc lịch sử/forecast cố định theo fixture; không nói đây là ngày hiện tại hoặc dự báo dữ liệu thật.
- Dataset riêng trong main/resources/ai/demo là bản sao synthetic từ fixture occupancy đã khóa; không gọi test resource trong runtime. Pin SHA, bound file, kiểm schema/mode khi load; dependency lỗi không thành mock fallback. Core forecast/history giữ nguyên, adapter demo không truy cập DB/network/Core business.
- Test: gates off/on/prod, chain-admin/customer/anonymous, hai scenario, invalid selectors, clock/dates/coverage/missing/30forecast points, synthetic markers, deterministic response, no write dependencies. Full backend/CI và real Keycloak HTTP trên sandbox synthetic đã snapshot; runtime flag/profile opt-in có kiểm trước khi khởi chạy và khôi phục config sau test. Không dùng flags demo để đóng gate dữ liệu thật.

## Allowlist và delivery

Code mới `modules/ai/demo/forecast/{OccupancyDemoConfiguration,OccupancyDemoController,OccupancyDemoService}.java`; resource `ai/demo/occupancy-history-v1.json` và attributes giữ bytes. Tests trong `ai/demo/forecast`; chỉ thêm test mới, không sửa oracle cũ. Tooling `Run-Application.ps1`, `Test.ps1`; API acceptance spec mới `frontend/e2e/api/u22-demo.spec.ts`; contract/report/README Member1. Nếu runtime reproducer cần sửa file khác, ghi rõ lý do trước sửa. CI minimum chỉ tăng theo test mới đã chạy, không giảm.

CONFIRMED sau targeted run: 19test mới +14regression PASS; CI minimum tăng1391→1410 để giữ độ đầy đủ của suite. Không thay min/gold/threshold của thuật toán.

Review cuối: test-only Basic auth configuration đổi sang @TestConfiguration để không bị component-scan vào full Boot test context; chạy lại Verify default-off trên cùng source runtime. Phạm vi không đổi; giữ bằng chứng mode-on đã chạy và source hash runtime.

Quy trình: implement → targeted/security review → fix → full local verification → final review → commit/push/PR/green checks/merge theo quyền user đã có. Rollback bằng revert qua PR hoặc tắt flag/profile; không DB rollback, không force/reset/stash/đổi worktree khác. Mock business API/UI thật, U22 utility và Group vẫn là các slice tiếp theo; không công bố toàn SRS/production hoàn tất.
