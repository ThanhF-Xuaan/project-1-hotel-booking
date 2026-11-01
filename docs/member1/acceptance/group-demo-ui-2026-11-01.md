# GROUP-D02 — Giao diện demo phân phòng Group

CONFIRMED base develop `2042240e9e6c6b213cd726415903b83da249a1f4`, branch `codex/m1-group-demo-ui-20261101`, worktree ban đầu sạch. User ủy quyền implementation/review/fix/commit/push/PR/merge và cross-member decisions; mock trước/dữ liệu thật sau. Không giả member sign-off hoặc human peer approval. [Plan, evidence và allowlist](../plans/group-demo-ui-2026-11-01.md).

## Hành vi

Lazy route `/ai/demo/group-room-assignment` và sidebar “Phân phòng Group (Demo)” chỉ CHAIN_ADMIN. Trang dùng API GROUP-D01 qua auth client hiện hữu, default FIT_AND_TIE và bảy kịch bản cố định. Có loading, error/retry, AbortController và selection identity để response cũ không ghi đè lựa chọn mới. API chỉ GET với single scenario; URL query scope/action không được đưa vào request. Khi Group tắt, trang báo demo chưa bật;401 theo interceptor về login,403/404/503/network có thông báo an toàn, không echo raw error hoặc dữ liệu dự phòng.

Hiển thị nhãn dữ liệu giả lập/preview-only, mốc31/10/2026 lúc09:00 Việt Nam và ngày lưu trú/check-out không tính đêm. Ba bảng cho demand→room/giường/tầng/kết quả, candidate/block/type/capacity/eligibility và quota từng đêm. COMPLETE được diễn đạt “Đã tính xong”; ZERO_QUOTA có0phương án/2unmet, không được hiển thị đủ phòng. IDs là mã mô phỏng, không liên kết sang nghiệp vụ hoặc có nút apply/hold/assign/booking.

Service kiểm envelope/code200, SYNTHETIC/businessData=false/previewOnly, fixed clock/scenario/catalog/snapshot ID/scope/dates, bounded safe numeric values và typed DTO trước render. Kiểm uniqueness/partition demand-assignment-unmet, assigned room/capacity/bed/eligibility, đủ quota/night rows và scope/block/type mọi đêm; counts, bed waste, floor span/enabled và warning phải nhất quán. Đây là sanity check phương án trả về; không chạy thuật toán tìm/ranking room ở client. Không suy số phòng thật từ quota hay lấy ngày hiện tại để biến fixed replay thành availability live.

UI giữ pattern neutral/red/amber của hai demo hiện có, native select, nhãn và focus rõ, status/alert, caption/th cho bảng và region scroll có tabIndex. Bảng mobile nằm trong vùng cuộn ngang có lời dẫn, không đẩy rộng viewport. Review ảnh đã thay từ “fixture/snapshot/metadata” trong nội dung chính bằng cách gọi dễ hiểu về dữ liệu mô phỏng/thông tin tầng. Không thêm dependency/font/chart hoặc thay auth client/backend.

## Review và kết quả thực chạy

Targeted lượt đầu **27/29PASS,2FAIL** vì `getByLabel` mới khớp cả section aria-label lẫn select. Sửa đúng hai locator thành combobox/name exact; giữ assertion, bảy room oracles và18malformed cases. Targeted sau fix **29/29PASS**. Log FAIL/PASS được giữ riêng. Review tiếp bổ sung lời dẫn cuộn bảng/mobile copy và làm test race chờ handler cũ hoàn tất/frame trước assertion; full Verify sẽ kiểm source cuối, không gọi targeted trước polish là proof riêng của source sau polish.

Route provenance **9/9PASS** gồm3Group mới+6occupancy/utility cũ; LF/CRLF projection exact và tamper guard/path/import/prior routes/missing/duplicate/relocated/unrelated đều bị từ chối. Thêm helper Group rồi chuyển bytes về pre-Group App pin Git base/hash trước khi chuỗi utility/occupancy/T12 lịch sử chạy. Không sửa frozen manifest/audit/gold hoặc oldtest assertions. Source/contract validation, actionlint, PowerShell parse và lint/build ban đầu PASS; final verification/delivery theo receipt/handoff mới nhất.

Mock UI dùng synthetic inputs từ main resource oracle-free và independent literal room/unmet expectations; không gọi backend hoặc Gemini. Tests dùng dummy token chỉ kiểm UI guard, không là Keycloak proof. Runtime tests mới dùng JWT Keycloak thật, admin sidebar→all7API/UI parity/quota/counts; main data/sequence digest trước/sau, GET-only và anonymous/customer/reception không request Group qua route/sidebar. Runtime và off proof phải được ghi riêng với mock/CI.

| Kiểm tra đã chạy | Kết quả |
|---|---|
| `pwsh -NoProfile -File tools/acceptance/Verify.ps1`, all3demos-on | [PASS run20261101-215324-284-994942eb](runs/acceptance-20261101-215324-284-994942eb.md):1480backend/39API/100browser,0failure/error/skip trong executions |
| `npx playwright test e2e/browser/group-demo-ui.spec.ts` | PASS29/29 sau sửa locator;29mock tests nằm trong100browser, không cộng lặp |
| Group runtime trong full Verify | PASS2/2 subset: real Keycloak all7UI/API/quota/counts và unauthorized/no Group request; main data/sequence digest unchanged, GET-only |
| `python -m unittest discover -s tools/member1 -p 'test*.py' -v` | PASS9/9:3Group mới+6oldroute, giữ old assertions |
| Source/contract/portability/lint/build/audit/actionlint/PowerShell parse/`git diff --check` | PASS; npm còn12moderate tooling advisories baseline,0high/critical; không đổi dependencies |

Backend1480 qua clean test/context smoke riêng; không gọi là một lệnh Maven. Child application-suites giữ PARTIAL/exit1 do skip Maven; supervisor chỉ PASS sau backend.json riêng và sáu suite còn lại cùng PASS. Final source gồm polish/race strengthening đã thực sự chạy trong full Verify;10non-doc hashes khóa trước app suites. Full snapshot restore/stop PASS. CI selected1472backend/176reports dự kiến theo scope có sẵn, không đồng nhất với1480local; final Git/CI theo PR/receipt/canonical handoff.

Disabled Group proof **PASS7/7** riêng:5Group API tests+2runtime browser tests với Keycloak thật, occupancy/utility vẫn bật. Admin thấy404/safe disabled state và0table; anonymous/customer/reception không request Group qua route/sidebar; direct API role/status và digest cũng được kiểm. Không cộng7vào39API/100browser của full run. Off snapshot restore PASS; original config khôi phục nguyên byte, compose regenerated, owned apps/containers/listeners đã dừng.

Final self-review **PASS trong scope GROUP-D02**:14files/10non-doc+4docs/evidence, không finding chặn còn mở trong UI demo. API-only GET/role guard/no business action, response consistency, stale-response/error/reset, mobile/keyboard và historical audit compatibility có bằng chứng thực chạy. Backend/source/gold giữ nguyên;10source hashes khớp từ full Verify tới delivery. Các gate dữ liệu thật/official BCA/SSH authentication giữ riêng, không được đóng bằng mock hoặc user delegation.

## File sửa thuộc member khác và dùng chung

| File | Thay đổi và lý do |
|---|---|
| `frontend/src/App.tsx` — Member4/shared routes | Chỉ thêm lazy import và CHAIN_ADMIN Group route, để admin mở trang mới |
| `frontend/src/core/components/layout/Sidebar.tsx` — Member4/shared UI | Thêm item Group trong tổng quan, role CHAIN_ADMIN và icon BedDouble sẵn có |
| `frontend/src/modules/analytics/pages/GroupDemoPage.tsx` — file UI mới trong khu vực Member4 | Trang xem phương án, trạng thái/selector/error và ba bảng read-only |
| `frontend/src/modules/analytics/services/groupDemoService.ts` — frontend integration mới | GET và typed/sanity validation trước render qua auth client hiện có |
| `frontend/e2e/browser/group-demo-ui.spec.ts`, `group-demo-runtime.spec.ts` — frontend tests mới |29mock tests và2runtime tests, bảo vệ malformed data/race/role/no-write/mobile |
| `tools/member1/reconcile_group_route.cjs`, `test_group_route.py`, `reconcile_utility_route.cjs` — provenance tooling | Xác minh exact additive route mới rồi giữ audit lịch sử;3test mới, không bỏ old checks |
| `.github/workflows/member1-ci.yml` — CI dùng chung | Thêm Group mock suite vào existing browser command; giữ backend minimum1472 và mọi required suites/gates |
| Plan/report/acceptance README/generated run report | Lưu phạm vi, bằng chứng và giới hạn; Git/CI cuối theo PR/receipt |

Không sửa các file nghiệp vụ Booking/Pricing/Inventory/Payment của Member2/3, schema/Liquibase/IAM/backend/engine/frozen fixture/gold/dependencies; auth client/ProtectedRoute hiện hữu là read-only dependency. Không reset/stash/overwrite repo D hoặc protected worktrees. Canonical handoff/plan chỉ append sau delivery.

## Giới hạn và rollback

Demo dùng giả lập/fixed replay, không phải inventory hoặc phân phòng nghiệp vụ thật. Dữ liệu physical bed/floor/quota/all-night provider/IAM thật và mẫu BCA vẫn PENDING_INPUT. OCR-02 đã COMPLETE/MERGED PR40; Group API PR50 không thay production gate. Upstream deployment hiện BLOCKED vì SSH authentication; không tự tạo hoặc thay credentials/server access trong UI subtask.

Rollback scoped whole PR hoặc tắt Group backend flag/profile; không DB/schema rollback. Báo cáo CI selected/mock khác full local Keycloak/runtime. Logs/ảnh/source hashes/config-canonical backups/artifacts/receipt tại `C:/Users/admin/.codex/outputs/group-demo-ui-2026-11-01/`; credential/snapshot/runtime config không commit.
