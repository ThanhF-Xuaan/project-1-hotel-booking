# U14 local v1 — ma trận nghiệm thu và backlog

- Trạng thái: **PROPOSED — ca kiểm thử API mới đều NOT RUN**.
- Baseline: develop@627887bc235d5eb0b59835a8d47744e605430aa8.
- Hợp đồng: [u14-local-v1.md](u14-local-v1.md); OpenAPI: [u14-local-v1.openapi.json](u14-local-v1.openapi.json).
- Chỉ dùng fixture synthetic trong acceptance sandbox. Không dùng dữ liệu khách thật.

## 1. Bằng chứng hiện có

**CONFIRMED:** U14 tests hiện tại kiểm tra thuật toán synthetic, route gate mặc định và giới hạn CHAIN_ADMIN. Chúng không xác minh binding server-side, consent store/revision, history từ Booking hoặc ownership.

**NOT RUN:** mọi route/schema/UI mới ở contract này. Kết quả Maven/API/browser cũ không phải bằng chứng cho endpoint mới.

Route cần triển khai và nghiệm thu: `GET/PUT /api/v1/me/personalization-consent`; `GET /api/v1/me/personalization/recommendations`. Chi tiết schema nằm trong OpenAPI liên kết ở đầu tài liệu.

Hồ sơ cũ docs/member1/proposed/u14-consent-history-v1-proposed.md giữ nguyên 16 scenario PROPOSED. Ma trận dưới đây bổ sung expected outcome local v1; không sửa gold answers hoặc xóa hồ sơ cũ.

## 2. Fixture sandbox

| Fixture | Mục đích |
|---|---|
| Customer A + BookingGuest A | Binding chính xác theo JWT iss/sub |
| Customer B + BookingGuest B | Kiểm tra account/profile isolation |
| Hotel 1 và Hotel 2 | Tách consent, history và catalog |
| Hai active room types + một inactive/deleted | Catalog filter và ranking |
| FIT, một phòng, checked-out, actual timestamps hợp lệ | History hợp lệ |
| Multi-room, GIT, CORPORATE, cancelled/no-show, expected/check-in | Negative history |
| Actual timestamp thiếu/đảo/cùng ngày/future | Boundary validation |
| Checkout timestamp trùng và join duplicate | Ordering/dedup |
| Hai client cùng expected revision | CAS concurrency |
| Hai subject khác nhau có thể cùng preferred_username | Khẳng định ownership theo iss/sub |

Thêm CUSTOMER B bằng UUID tổng hợp riêng. Binding fixture chỉ được nạp qua seed acceptance idempotent; không có endpoint tự link. Mỗi test dùng marker synthetic và cleanup bằng snapshot/transaction harness.

## 3. Acceptance cases

| ID | Scenario | Expected |
|---|---|---|
| C01 | Flag mặc định false | JWT hợp lệ nhận 404; không gọi binding/consent/history |
| C02 | Anonymous, JWT lỗi, thiếu issuer/sub | 401 trước controller |
| C03 | Staff/admin không có CUSTOMER | 403, không gọi provider |
| C04 | Username trùng/đổi nhưng subject khác | Ownership không đổi theo username |
| C05 | Không binding, binding revoke, guest soft-delete | 409 IDENTITY_LINK_REQUIRED, không query history |
| C06 | Gửi guestId/history/consent/referenceDate/free text/unknown property; unknown hoặc repeated query parameter | 400, không phân tích; không đổi Jackson toàn ứng dụng |
| C07 | Customer A/B hoặc hotel A/B | Chỉ đúng binding + hotel được query |
| C08 | GET consent chưa có state | NOT_GRANTED, revision 0; không tạo row/event |
| C09 | Grant theo policy hiện hành | State và event cùng commit, revision tăng 1 |
| C10 | Revoke | Event và state false commit, giữ acceptedPolicyVersion trước đó; request kế tiếp không query history |
| C11 | Revoke trước grant | Revision 1, event ghi nhận, consent vẫn false |
| C12 | Hai PUT cùng revision/binding version | Một thành công, một 409 |
| C13 | Stale revision/policy; policyVersion thiếu/sai kiểu/rỗng/quá dài | Stale hợp lệ cú pháp: 409; input sai: 400; không ghi state/event |
| C14 | Grant cũ sau unlink/relink | 409 theo bindingVersion, không consent binding mới |
| C15 | Consent event insert lỗi | Transaction rollback; state/revision/event không đổi |
| C16 | FIT, một detail quantity 1, một checked-out room | Chọn khi scope và actual dates hợp lệ |
| C17 | GIT/CORPORATE/multi-detail/multi-room/cancelled/no-show/not checked-out | Bị loại |
| C18 | Actual thiếu/đảo/cùng local date/future; planned dates khác; UTC boundary | Không dùng planned/createdAt; timezone Asia/Ho_Chi_Minh |
| C19 | Checkout ở bound 12 tháng, ngay trước/sau bound | Inclusive từ local start-of-day; ngoài cửa sổ bị loại |
| C20 | Trên 100 record, join duplicate, checkout trùng | Dedupe trước limit; checkout DESC rồi room ID DESC |
| C21 | Room swap/upgrade | Dùng hạng trong BookingDetail ban đầu |
| C22 | Catalog inactive/deleted/sai hotel | Bị lọc trước top N; label lấy từ catalog |
| C23 | Permutation history, tie scores, fixture 101/102 trong contract | Score 1.0000/0.5000; ẩn 101 thì 102 đạt 1.0000; tie theo ID tăng dần |
| C24 | History rỗng so với provider timeout | Cold start khác 503 |
| C25 | Revoke/unlink/rebind hoặc revoke rồi grant lại giữa history query và final auth | Revoke: 200 CONSENT_REQUIRED; unlink/deleted: 409 identity link; rebind: 409 binding conflict; revision mới: 409 revision conflict; tất cả bỏ suggestions cũ |
| C26 | Recommendation/fallback | DB digest, consent, booking, pricing/payment/inventory không đổi |
| C27 | Response/log inspection; Cache-Control trên success/error | Không chứa PII, identity, booking ID/date, free text hay raw history; U14 response có private, no-store, giữ nguyên 401 auth filter |
| C28 | UI đổi hotel/account, stale response, 409/503 | Không render kết quả cũ; không auto-grant |
| C29 | UI consent | Checkbox chưa chọn; notice synthetic; privacy link hoạt động; revoke xóa result |
| C30 | Trang privacy | Nêu purpose, dữ liệu giả lập, nơi fixture tồn tại, revoke và giới hạn demo |

## 4. Kiểm thử và báo cáo khi có implementation

1. Unit test state machine/CAS với Clock cố định.
2. PostgreSQL integration test cho unique index, lock, rollback và scoped history query.
3. Spring Security MVC test cho JWT claims, CUSTOMER role, feature flag và response envelope.
4. Trong thư mục backend chạy Maven wrapper với filter *Personalization*, U14*.
5. Backend regression bằng Maven wrapper không filter.
6. API Playwright thật trong sandbox: grant/revoke, A/B ownership, hotel isolation, 409/503 và DB digest.
7. Browser Playwright: CUSTOMER login, privacy link, grant/revoke/reload, đổi account/hotel và stale response.
8. Chạy npm run lint, npm run build, API/browser suites và tools/acceptance/Verify.ps1; fixture chỉ synthetic.
9. Kiểm tra diff, OpenAPI JSON parse, mọi $ref, whitespace check, snapshot/restore, hashes và log không secret.
10. Nếu sửa frontend/src/App.tsx, tạo T12 source re-audit có provenance; không nới validator/gold.

Ghi SHA, config không secret, command/exit code, fixture marker, snapshot hash và report. Ghi rõ PASS/FAIL/NOT RUN; suite skip không phải PASS. Không gọi Gemini/provider ngoài, staging hoặc production. Fault injection chỉ trong harness.

## 5. Backlog implementation chia lát

| ID | Slice / owner | Filescope sơ bộ cần khóa tại SHA triển khai | Exit criteria |
|---|---|---|---|
| I01 | Identity schema/fixtures | Liquibase XML/SQL, binding/consent/event model/repository, acceptance SQL/Keycloak fixture | Migration additive; uniqueness/lifecycle; snapshot restore |
| I02 | Consent API/CAS — Identity | Controller/service/DTO/local error mapper và tests | CUSTOMER-only; revision+binding CAS; transaction atomic; fail closed |
| I03 | History/catalog providers — Booking/Inventory | Port DTO thuộc AI; query provider trong module sở hữu; PostgreSQL IT | Scope trước limit; 12 tháng/100; actual timestamps; empty khác outage |
| I04 | Affinity/orchestration — AI | Tách core, context orchestration, controller/DTO và tests | Synthetic regression; no history without consent; final auth; zero writes |
| I05 | UI/API/browser — frontend | App.tsx, portal entry, U14/privacy pages, API types/client, Playwright | Login/policy/grant/revoke/errors; không render stale data |
| I06 | Provenance và acceptance | T12 re-audit/tests, runner/evidence | Source drift có provenance; validators giữ nguyên; sandbox restore |

Filescope là dự kiến, chưa phải allowlist. Trước mỗi slice ghi SHA, status, file cụ thể, snapshot, lệnh và rollback. Chưa chạy test/migration trong lượt lập contract.

## 6. Gate, rollback và giới hạn

Contract local sẵn sàng triển khai khi schema/API/examples khớp nhau, 30 ca có expected outcome, fixture không có dữ liệu thật và không còn TBD ảnh hưởng local v1. Slice I05 phải tạo và kiểm tra trang privacy trước khi consent UI được bật. Real customer binding/retention là contract riêng.

Local acceptance không chứng nhận model accuracy, hiệu quả nghiệp vụ hoặc production readiness. Consent trong sandbox không phải consent khách thật. Rollback code bằng tắt flag, ẩn UI và revert từng slice; không drop consent/event hoặc reset volume ngoài snapshot đã xác minh.

## 7. Nhật ký

Kiểm tra bộ tài liệu trước commit ngày 2026-10-26:

| Kiểm tra thực chạy | Kết quả |
|---|---|
| `python -m json.tool docs/member1/contracts/u14-local-v1.openapi.json` | PASS: JSON hợp lệ |
| Python inline dùng `jsonschema.Draft202012Validator`, resolver local và format checker | PASS: 9 schema, 18 example; 2 mutation hợp lệ, 10 input sai bị loại, 2 recommendation sai bị loại |
| Python inline đối soát `$ref`, route ở ba tài liệu, 30 case và trailing whitespace | PASS |
| `validate_copy.py` với `--register eng-impersonal --doctype rfc` cho hai file Markdown | PASS |
| Maven, frontend lint/build, API/browser, Docker và migration | NOT RUN: scope chỉ gồm tài liệu, chưa có implementation mới |

Kiểm tra JSON Schema ở trên không được báo thành nghiệm thu endpoint hoặc full OpenAPI tooling validation.

Đối soát C01–C09: C01–C02 nằm ở mục 1–2 hợp đồng; C03–C04 ở schema/state machine/race table; C05 ở history/catalog và fixture ranking; C06 ở OpenAPI; C07 ở UI và ma trận này. C08 là self-review source, schema, ví dụ và expected outcomes trong lượt bàn giao; không thay thế review của người duyệt PR. C09 bàn giao ba tài liệu và backlog I01–I06; trạng thái duyệt/merge được xác nhận qua PR, không tự ghi đã phê duyệt API/schema khi chưa có bằng chứng.

| Ngày | Thay đổi | Bằng chứng |
|---|---|---|
| 2026-10-26 | Tạo contract local theo lựa chọn người dùng | Source inspection baseline 627887bc; API/schema mới NOT RUN |
| 2026-10-26 | Rà soát trước commit | Chốt race outcomes, giữ policy khi revoke, final authorization và fixture ranking; kiểm tra schema/examples riêng, runtime vẫn NOT RUN |
