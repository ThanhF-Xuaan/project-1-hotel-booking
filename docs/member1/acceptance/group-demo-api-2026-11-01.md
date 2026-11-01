# GROUP-D01 — API demo phương án phân phòng Group

**CONFIRMED — PASS local/synthetic.** API đọc bảy kịch bản giả lập qua engine Group hiện có. Giao diện là subtask GROUP-D02. User ủy quyền implementation, review/fix, commit/push, PR và merge; cho phép sửa phạm vi member khác và yêu cầu ghi rõ file/lý do. Không giả chữ ký member hoặc human peer approval. [Contract và allowlist](../contracts/group-demo-api-v1.md).

Base `a70b833f8d138621e539fdbcade595eb56165a4c`, branch `codex/m1-group-demo-api-20261101`; worktree ban đầu sạch. Không nhập thay đổi từ repo D hoặc worktree khác.

## Hành vi và giới hạn

`GET /api/v1/ai/demo/group-room-assignment?scenario=FIT_AND_TIE`, mặc định FIT_AND_TIE. Bảy alias: FIT_AND_TIE, QUOTA_ONE, MIDDLE_NIGHT_LIMIT, ZERO_QUOTA, QUOTA_CONTROL, NO_TRANSFER_BLOCK, NO_TRANSFER_ROOM_TYPE. Chỉ nhận một scenario hợp lệ; unknown/empty/lowercase/duplicate/extra query trả400 với thông báo cố định. Không nhận scope, ngày, danh sách phòng/khách, quota, quyền, clock hoặc lệnh ghi do client cung cấp.

Profile `m1-demo` và cờ `hotel.ai.demo.group.enabled=true` phải cùng bật; mặc định tắt, loại cả controller/provider nếu có `prod` hoặc `production`. Cờ Group độc lập occupancy/utility. CHAIN_ADMIN qua security hiện có; anonymous401, customer/reception403 khi bật; authenticated404 khi tắt. Service kiểm trusted SecurityContext trước/sau và qua authorization/data ports, không cache role. Không tuyên bố đã kiểm remote Keycloak role revocation giữa request.

Response giữ ApiResponse/code200/result, đánh dấu SYNTHETIC/businessData=false/previewOnly=true và notice tiếng Việt. Có catalog, request/snapshot giả lập, preview và allDemandsAssigned. `COMPLETE` chỉ nghĩa đã tính xong: ZERO_QUOTA vẫn COMPLETE với0assignment/2unmet và allDemandsAssigned=false. Quota được kiểm mọi đêm theo block/type, giữ mục tiêu cardinality → bed waste → floor span → stable room-ID tie của engine. Không chuyển quota giữa block/type hoặc suy quota từ số phòng.

Clock replay cố định `2026-10-31T02:00:00Z`; TTL chỉ được đánh giá tại mốc này. Ngày/ID/giường/tầng/phòng/quota đều thuộc fixture, không phải availability hiện tại. Không giữ, phân, trả phòng nghiệp vụ, Booking/Payment/StayGuest writes hoặc SQL/network/Core dependencies. Lỗi kết quả/domain trả503 an toàn, không tạo preview dự phòng. Resource hỏng dừng khởi tạo demo.

Main resource `ai/demo/group-preview-v1.json` là projection25487bytes/7cases của frozen `src/test/resources/ai/group-assignment/preview-cases.json`. Chỉ giữ metadata và id/request/snapshot; bỏ expected/source/C01 oracle metadata. Main SHA256 `8d569695a769c55b9bc94ec465d47063555a87917eb47b415a1341405d1994cf`; frozen source SHA256 `2dbd7da729831b0f21cc53c21412bb744fd8e620aaa050c6fe9a2b2a89ddc476`. Bound128000bytes, pin hash, typed immutable cache và fixed classpath path; không đọc file test ở runtime.

## Review và bằng chứng

| Kiểm tra thực chạy | Kết quả |
|---|---|
| `backend/.\\mvnw.cmd -Dtest=GroupDemoApiTest,*GroupRoomAssignment*Test,GroupPreview*Test -DfailIfNoTests=true test` với JDK21 | PASS85/85:29test mới +56test Group cũ;0failure/error/skip |
| `pwsh -NoProfile -File tools/acceptance/Verify.ps1` với cả ba demo bật | [PASS run20261101-163255-118-6a39a95e](runs/acceptance-20261101-163255-118-6a39a95e.md):1480backend,39API,69browser;0failure/error/skip trong executions |
| Source/contract/portability/lint/build/audit trong Verify | PASS; npm audit còn12moderate tooling advisories baseline,0high/critical; không thay dependency |
| Targeted ESLint/actionlint/PowerShell parse/`git diff --check` | PASS |
| Matrix cờ độc lập, Keycloak thật | PASS Group-off/occupancy+utility-on và Group-only:18checks/mode,36executions riêng; API tắt404/bật200, RBAC và digest vẫn kiểm |
| Cleanup | PASS snapshot restore cho full Verify và matrix; config ban đầu khôi phục nguyên byte, owned apps/containers/listeners đã dừng |

85targeted là subset của1480backend;5Group API tests nằm trong39API. Backend clean test và context smoke chạy riêng; không gọi tổng1480 là một lệnh Maven. Child application-suites giữ PARTIAL/exit1 vì skip Maven; supervisor kiểm Maven riêng và sáu suite còn lại trước kết luận PASS. Real Keycloak admin đọc đủ bảy scenario, repeat deterministic, quyền/status/invalid selectors đúng; digest dữ liệu và sequence main DB trước/sau bằng nhau. Runtime API chỉ GET; test write-method405 với valid Basic auth/CSRF là narrow servlet evidence, không thay proof Keycloak/runtime.

Test mới kiểm dual gate/default-off/prod exclusions, quyền qua filters+method/direct service, strict selectors, fixed clock/determinism, partial coverage, quota zero/middle/no-transfer và bed/floor oracle độc lập. Projection parity/corruption/bounds, immutable data và closed import allowlist cũng được kiểm. Không sửa engine, old tests, frozen fixture/gold hoặc nới threshold. Review chưa phát hiện finding chặn trong scope API demo.

Lần đầu chạy matrix độc lập không thực thi được vì Docker Desktop daemon đã dừng; giữ log `independent-flags-daemon-unavailable.log`, không gọi là lỗi assertion hoặc PASS. Sau đó khởi động lại Docker local để kiểm tra riêng hai mode.

Logs, source hashes, backups, harness và receipt ở `C:/Users/admin/.codex/outputs/group-demo-api-2026-11-01/`; credentials/snapshots/runtime config không vào Git. Kết quả CI exact-head và post-merge theo PR/receipt/canonical handoff, không đồng nhất1480local với CI selected scope.

## File thay đổi và phạm vi member khác

| File/nhóm file | Lý do |
|---|---|
| `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/demo/group/GroupDemoConfiguration.java`, `GroupDemoController.java`, `GroupDemoService.java` | Ba file Member1 mới: gate, HTTP boundary và adapter replay chỉ đọc |
| `backend/src/main/resources/ai/demo/group-preview-v1.json`, `.gitattributes` | Corpus giả lập không oracle; giữ byte/hash nhất quán Windows/Linux |
| `backend/src/test/java/vn/edu/utc/hotel_booking/modules/ai/demo/group/GroupDemoApiTest.java` |29test mới, giữ nguyên oracle/engine cũ |
| `frontend/e2e/api/group-demo.spec.ts` | File mới trong frontend test area thường do Member4 quản lý: kiểm API với Keycloak local và digest; không sửa frontend business logic/UI/routes |
| `tools/acceptance/Run-Application.ps1`, `tools/acceptance/Test.ps1` | Tooling acceptance dùng chung: bật Group đúng config, mặc định false; truyền trạng thái cờ cho E2E |
| `.github/workflows/member1-ci.yml` | CI dùng chung: minimum1443→1472 đúng29test mới và require-suite GroupDemoApiTest; giữ các gate cũ |
| Contract này, report, acceptance README, generated run report | Lưu contract, bằng chứng và giới hạn để review |

Không sửa file nghiệp vụ của Member2/3/4, SecurityConfig/IAM, shared schema/Liquibase, Booking/Pricing/Inventory/Payment, dependencies hoặc pure Group engine. Các thay đổi trong khu vực frontend test và tooling/CI dùng chung đã được user ủy quyền; không suy thành member sign-off.

## Chạy lại, rollback và phần còn lại

Trong sandbox synthetic đã chuẩn bị, thêm `HOTEL_AI_DEMO_GROUP_ENABLED="true"` vào config local Git-ignore, chạy launcher acceptance và gọi GET bằng bearer CHAIN_ADMIN. Không opt-in thì API tắt. Tắt flag/profile hoặc revert scoped PR để rollback; không DB/schema rollback. Không production/paid-provider call.

GROUP-D02 UI là subtask tiếp theo. Physical beds/floors/quota/all-night availability provider và IAM scope nghiệp vụ thật vẫn PENDING_INPUT; official BCA cũng chưa có mẫu xác minh. Dữ liệu mock không đóng các gate này. OCR-02 đã hoàn tất ở PR40; report này không tuyên bố toàn feature/SRS/production hoàn tất.
