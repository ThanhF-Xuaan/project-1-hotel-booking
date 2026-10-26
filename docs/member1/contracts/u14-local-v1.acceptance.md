# U14 local v1 — ma trận nghiệm thu và backlog

- Trạng thái: **PARTIAL — I01–I06 đã qua nghiệm thu local bằng dữ liệu synthetic; binding/consent/history khách thật và production rollout chưa thuộc phạm vi**.
- Baseline: develop@627887bc235d5eb0b59835a8d47744e605430aa8.
- Hợp đồng: [u14-local-v1.md](u14-local-v1.md); OpenAPI: [u14-local-v1.openapi.json](u14-local-v1.openapi.json).
- Chỉ dùng fixture synthetic trong acceptance sandbox. Không dùng dữ liệu khách thật.

## 1. Bằng chứng hiện có

**CONFIRMED:** U14 test cũ kiểm tra thuật toán synthetic; chúng không xác minh history từ Booking hoặc recommendation ownership.

**CONFIRMED — I02:** GET/PUT consent đọc `iss/sub` từ JWT, yêu cầu role CUSTOMER, kiểm tra revision/binding version và ghi consent state/event trong cùng transaction. Unit, MVC và PostgreSQL integration đạt 21/21 test.

**CONFIRMED local:** bearer token qua Keycloak test, API và browser acceptance chạy với PostgreSQL/Redis/Keycloak riêng, dữ liệu synthetic và snapshot được khôi phục. **NOT RUN:** xác minh binding sở hữu booking guest cho khách thật, history thật, retention/xóa dữ liệu thật và production rollout. Bộ UI mocked vẫn được giữ như test tương tác riêng. Chi tiết schema nằm trong OpenAPI liên kết ở đầu tài liệu.

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

| ID | Slice / owner | Filescope sơ bộ cần khóa tại SHA triển khai | Exit criteria | Trạng thái hiện tại |
|---|---|---|---|
| I01 | Identity schema/fixtures | Liquibase XML/SQL, binding/consent/event model/repository, acceptance SQL/Keycloak fixture | Migration additive; uniqueness/lifecycle; snapshot restore | **VERIFIED local:** migration/fixture chạy trên acceptance PostgreSQL; snapshot khôi phục; volumes giữ nguyên |
| I02 | Consent API/CAS — Identity | Controller/service/DTO/local error mapper và tests | CUSTOMER-only; revision+binding CAS; transaction atomic; fail closed | **VERIFIED local:** 21/21 targeted; PostgreSQL CAS/rollback; API thật qua Keycloak CUSTOMER và account isolation |
| I03 | History/catalog providers — Booking/Inventory | Port DTO thuộc AI; query provider trong module sở hữu; PostgreSQL IT | Scope trước limit; 12 tháng/100; actual timestamps; empty khác outage | **VERIFIED local:** unit tests, 3 PostgreSQL query scenarios và API thật đọc stay checkout; projection dùng Instant rồi chuẩn hóa UTC |
| I04 | Affinity/orchestration — AI | Tách core, context orchestration, controller/DTO và tests | Synthetic regression; no history without consent; final auth; zero writes | **VERIFIED local:** API thật xác nhận PERSONALIZED_READY từ history synthetic, cold-start cho stay chưa checkout, no-write digest |
| I05 | UI/API/browser — frontend | App.tsx, portal entry, U14/privacy pages, API types/client, Playwright | Login/policy/grant/revoke/errors; không render stale data | **VERIFIED local:** lint/build; API 23/23 và browser 14/14 với backend/Keycloak thật; privacy link/page có browser test; mocked UI tests giữ lại |
| I06 | Provenance và acceptance | T12 re-audit/tests, runner/evidence | Source drift có provenance; validators giữ nguyên; sandbox restore | **PASS:** source/Core audits, validator 184 case, portability 12/12, dependency audit và snapshot restore |

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
| Kiểm tra tại thời điểm hoàn tất I01 | Maven/frontend/API/browser chưa chạy cho I01; nghiệm thu tích hợp mới nhất ghi ở mục I06 bên dưới |
| Docker acceptance Postgres | PASS, chỉ đọc Liquibase status; không chạy update, seed hoặc migration trên volume hiện có |

Kiểm tra JSON Schema ở trên không được báo thành nghiệm thu endpoint hoặc full OpenAPI tooling validation.

I01 trên nhánh implementation từ `develop@a683712ae430eeafe492bf4a01b64ef8580751df` thêm migration additive, binding/state synthetic và fixture CUSTOMER B. Migration đã chạy trên PostgreSQL 16 dùng tmpfs: Liquibase áp dụng 32/32 changeset, lần cập nhật lặp lại có 0 changeset, fixture chạy hai lượt; DB còn đúng 2 active binding synthetic, 0 consent và 0 event. Kiểm tra duplicate active guest, source ngoài synthetic, grant thiếu policy và binding version sai đều bị DB từ chối. Container tạm đã bị xóa.

Volume acceptance hiện có được mở chỉ để kiểm tra trạng thái: 30 changeset đã áp dụng; Liquibase báo còn `db/changelogs/05-db-dev-seed.xml::1.0.0-05-dev-seed::utc-hotel-team` và changeset U14 `20261026-160000-create-u14-local-binding-consent` chưa áp dụng. Không chạy `update`, seed hay migration vì việc đó sẽ ghi cả dev seed ngoài lát I01. Container PostgreSQL chỉ dùng cho lần đọc này đã được gỡ; named volume được giữ. Snapshot `tools/acceptance/.runtime/snapshot.sql` vẫn có SHA-256 `E20EAD6FDDFB33D2FE02B2DE639D944E2DB6C31A1545E28A2191153EAC887632`.

Chưa kiểm tra restore snapshot trên acceptance volume. Keycloak realm export có CUSTOMER B cho lần import realm mới; volume đã khởi tạo không tự nhận user mới từ file. Cần provision additive B trước khi chạy API/browser A/B với volume hiện tại; không reset volume.

Đối soát C01–C09: C01–C02 nằm ở mục 1–2 hợp đồng; C03–C04 ở schema/state machine/race table; C05 ở history/catalog và fixture ranking; C06 ở OpenAPI; C07 ở UI và ma trận này. C08 là self-review source, schema, ví dụ và expected outcomes trong lượt bàn giao; không thay thế review của người duyệt PR. C09 bàn giao ba tài liệu và backlog I01–I06; trạng thái duyệt/merge được xác nhận qua PR, không tự ghi đã phê duyệt API/schema khi chưa có bằng chứng.

I01 đã thay đổi file migration/fixture trong nhánh `codex/u14-local-v1-binding-storage`. Kết quả migration/fixture chỉ áp dụng cho PostgreSQL tạm. Volume acceptance mới được kiểm tra trạng thái, không phải nghiệm thu migration, restore, API hoặc UI.

Kiểm tra I02 ngày 2026-10-26 trên cùng nhánh làm việc, base `bdf8267cd0279ebaded67d79af5e5ae49d7749fa`:

- Targeted `PersonalizationConsentServiceImplTest`, `PersonalizationConsentControllerTest`, `PersonalizationConsentPostgresIT`: **PASS 21/21, 0 skip**. PostgreSQL 16 dùng tmpfs và DB `u14_i02_test`; chạy migration trên DB tạm, kiểm tra grant/revoke, hai writer cùng revision (200/409) và rollback state/event khi event insert lỗi. Container đã được dừng và tmpfs bị xóa.
- `.\mvnw.cmd test`: **PASS 1.017 test, 0 failure, 0 error, 8 skipped** trên DB `u14_i02_regression` trong PostgreSQL 16 tmpfs, map cổng mặc định 5433. Lần chạy đầu dùng cổng 15433 khiến test cấu hình môi trường mặc định fail; lần chạy lại ở cổng mặc định PASS. Container và DB tạm đã bị xóa.
- `git diff --check`: PASS. Frontend/API browser, Keycloak thật, acceptance snapshot/restore và migration trên volume sandbox: NOT RUN.
- Thay dependency `liquibase-core` bằng `spring-boot-starter-liquibase`: xác minh cần thiết vì Spring Boot 4 không tự cấu hình runner từ `liquibase-core` đơn lẻ. Test IT bật Liquibase rõ ràng trên DB disposable; Docker Compose/acceptance vẫn dùng migration runner riêng.

Kiểm tra I03 ngày 2026-10-26, base code `bdf8267cd0279ebaded67d79af5e5ae49d7749fa`:

- Targeted `BookingPersonalizationHistoryReaderTest`, `InventoryPersonalizationCatalogReaderTest`, `PersonalizationConsentServiceImplTest`, `PersonalizationConsentControllerTest`, `U14ZeroWriteAndPricingIsolationTest`: **PASS 30/30, 0 fail, 0 error, 0 skip**. Bao gồm scope/window validation, empty-vs-outage, mapping projection, zero-write isolation và hồi quy I02.
- `PersonalizationProviderPostgresIT`: **PASS 3/3** trên PostgreSQL 16 disposable; test thực thi nguyên văn hai native query trong schema tạm. Đã xác nhận scope guest/hotel trước LIMIT, FIT/CONFIRMED/one detail/quantity 1/one checked-out room, timestamp và local-night boundary, 100 record + stable order, và catalog chỉ gồm Hotel/HotelRoomType/RoomType active, chưa xóa. Container `u14-i03-postgres` và schema test đã bị xóa; không kết nối acceptance volume.
- `.\mvnw.cmd test`: **PASS 1.025 test, 0 failure, 0 error, 8 skipped**. Các opt-in integrations bị skip trong suite này; U14 I03 PostgreSQL IT ở dòng trên được chạy riêng.
- `git diff --check`: PASS. Keycloak/app API, recommendation orchestration, acceptance volume và frontend: NOT RUN trong I03.

Kiểm tra I04 ngày 2026-10-26, base code `bdf8267cd0279ebaded67d79af5e5ae49d7749fa`:

- Recommendation service/API và room-affinity core đã được nối qua các read-only ports. Authorization ban đầu kiểm tra binding, khách sạn và consent trước khi đọc history; không consent thì không gọi history/catalog. Sau khi tính kết quả, Identity đọc mới binding/consent trong `REQUIRES_NEW`; unlink/rebind, revoke hoặc revision conflict không trả gợi ý cũ. API chỉ trả hạng phòng hiện hành, yêu cầu CUSTOMER, giới hạn query, `private, no-store`; feature flag local demo mặc định tắt.
- Targeted I04 + regression I02: **PASS 43/43**. Sau khi tăng kiểm thử MVC cho role, no-store và default-off, targeted recommendation/core: **PASS 15/15**.
- `.\mvnw.cmd test` (thực chạy từ `backend/`): **PASS 1.047 test, 0 failure, 0 error, 8 skipped**. Các PostgreSQL provider tests I03 riêng 3/3 như ghi ở trên; chúng không nằm trong lần Maven full này.
- `git diff --check`: PASS. Backend app với DB/Keycloak thật, API/browser E2E, database digest no-write trên sandbox, Liquibase/migration và frontend: **NOT RUN**. Không khởi động Docker, không kết nối acceptance volume, không chạy migration.

Kiểm tra I05 ngày 2026-10-26:

- UI route `/portal/personalization?hotelId=...` dùng ProtectedRoute CUSTOMER, có kiểm tra CUSTOMER bổ sung ở trang, và được nối từ trang tìm phòng theo khách sạn đang chọn. API service gửi JWT qua client hiện có; consent checkbox mặc định bỏ chọn; chỉ gọi recommendation sau khi GET consent hoặc PUT grant xác nhận `granted=true`. Khi revoke, đổi khách sạn hoặc đổi account từ tab khác, kết quả cũ được xóa/bỏ qua. UI xử lý cold start, 409/503, không hiển thị affinity score hay dữ liệu lịch sử/PII.
- `npm run build`: **PASS**. `npm run lint`: **PASS** toàn frontend.
- `npx playwright test e2e/browser/u14-personalization.spec.ts` với Vite local tại `127.0.0.1:13001`, API endpoints được Playwright mock: **PASS 5/5** (grant sau checkbox; không tự grant lại sau 409; retry khi 503; xóa kết quả khi revoke; chặn role khác trước API). Đây là UI contract test, không phải API/backend E2E.
- `git diff --check`: PASS. Backend thật, Keycloak, DB/no-write digest, acceptance volume và migration: **NOT RUN**; không khởi động Docker hoặc ghi database.

Kiểm tra I06 tích hợp local ngày 2026-10-26, run `20261026-153528-558-84ef82d3`, branch `codex/u14-local-v1-binding-storage`, base SHA `bdf8267cd0279ebaded67d79af5e5ae49d7749fa`:

- `tools/acceptance/Verify.ps1`: **PASS**. Liquibase áp dụng 1 changeset mới; snapshot 320.541 byte được tạo trước migration/seed rồi restore sau suite. SHA-256 snapshot: `712E9413B0EB9D94D217577C7ADED844CFC877D560329E9B80B8EA56550F3650`. PostgreSQL, Redis, Keycloak và app đã dừng; Docker volumes được giữ.
- Maven: **1.047 test, 0 failure, 0 error, 0 skip** trong evidence cuối. `HotelBookingApplicationTests` chạy riêng với DB/Redis/Keycloak loopback; Liquibase và scheduling tắt trong smoke context. PostgreSQL Lodging 3/3 và Redis 4/4 chạy thật.
- Playwright API **20/20**, browser **13/13** trên backend thật. Bao gồm CUSTOMER auth, grant/revoke/cold-start, account/hotel isolation, private no-store, no-write digest và đăng nhập deep-link giữ `hotelId`/hash.
- Frontend lint **0 lỗi/0 cảnh báo**, build/typecheck PASS; `npm audit --audit-level=high` PASS. T12 contract validator: 184 instances, 74 case, 34 source hashes; portability **12/12**. T12, U14 và Core source audit cùng regression audit PASS.
- Nguồn được reconcile có provenance: `backend/pom.xml`, `frontend/src/App.tsx`, `frontend/src/modules/booking-portal/pages/SearchPage.tsx`. Validator và gold/eval data không bị nới/sửa.
- Giới hạn: nghiệm thu dùng dữ liệu synthetic; recommendation API đi qua cold-start thay vì lịch sử khách thật. Không xác nhận quyền sở hữu lịch sử thật, hiệu quả nghiệp vụ, staging hoặc production readiness. Chi tiết log/suite nằm trong [báo cáo nghiệm thu](../acceptance/runs/acceptance-20261026-152551-489-22b0caf2.md).

Kiểm tra bổ sung I03–I05 ngày 2026-10-26, run `20261026-164152-385-46663e64`:

- `tools/acceptance/Verify.ps1`: **PASS**, Maven 1.047 test/0 skip; T12/U14/Core audits, validator, portability, dependency audit, lint và build đều PASS. Snapshot SHA-256 `0888BA66228D10F06ACE6BF53F4E8D9E1465689052CFF86D48AEAB94BBC88DB5` được restore; app và sandbox containers đã dừng, volumes được giữ.
- API **21/21**, browser **13/13** trên PostgreSQL/Keycloak/backend thật. CUSTOMER B có một FIT booking đã checkout bằng dữ liệu synthetic và nhận `PERSONALIZED_READY` với hạng phòng catalog `DLX`; CUSTOMER A chỉ có booking đang lưu trú và nhận `COLD_START_FALLBACK`. Recommendation response không chứa lịch sử/PII và digest DB không đổi.
- Khi kiểm tra API có lịch sử thật, phát hiện PostgreSQL/Hibernate trả timestamp native dưới dạng `Instant`; projection trước đó khai báo `OffsetDateTime` và gây 503. Projection đã đổi sang `Instant`, reader chuẩn hóa instant sang UTC `OffsetDateTime`; regression unit test và E2E thật đạt.
- Chi tiết lệnh, suite và evidence: [báo cáo nghiệm thu](../acceptance/runs/acceptance-20261026-164152-385-46663e64.md). Giới hạn vẫn là synthetic data; chưa xác nhận lịch sử hoặc consent khách thật, staging hay production readiness.

Kiểm tra bổ sung C13 consent conflict ngày 2026-10-26, run `20261026-174456-198-a6e9aa8e`:

- API E2E thử revision cũ, policy cũ, binding version cũ và grant thiếu policy. Kết quả lần lượt 409/409/409/400 với reason code đúng; revision/granted giữ nguyên và database digest không đổi, nên không phát sinh event khi request bị từ chối.
- `tools/acceptance/Verify.ps1`: **PASS**; Maven 1.047/0 skip, API **22/22**, browser **13/13**, lint/build/audits PASS. Snapshot SHA-256 `86E2931FF347442B8E0DB645287622F1F47DEBAF772A2F16CFB66F6A4F873773` restored; ứng dụng và containers đã dừng, volumes giữ nguyên.
- Chi tiết: [báo cáo nghiệm thu](../acceptance/runs/acceptance-20261026-174456-198-a6e9aa8e.md). Đây vẫn là xác minh kỹ thuật với dữ liệu giả lập.

Kiểm tra bổ sung identity-link fail-closed và hotel scope ngày 2026-10-26, run `20261026-180041-037-84f7a235`:

- API E2E xác nhận CUSTOMER không có binding nhận `409 IDENTITY_LINK_REQUIRED` cho consent và recommendation; hotel không tồn tại nhận `404 HOTEL_NOT_FOUND` ở cả hai route. DB digest trước/sau giống nhau.
- Sửa hotel scope lookup thành truy vấn boolean qua `HotelService`; không để exception từ Organization đánh dấu transaction authorization U14 là rollback-only và biến 404 thành 503.
- `tools/acceptance/Verify.ps1`: **PASS**; Maven **1.047/0 skip**, API **23/23**, browser **13/13**, lint/build, dependency/source audits và portability PASS. Snapshot SHA-256 `C2792A4877B9A37B8E6E506815F351ED18393A90C835E5FB9D075E9A0891C336` được restore; app và sandbox containers đã dừng, volumes được giữ.
- Chi tiết: [báo cáo nghiệm thu](../acceptance/runs/acceptance-20261026-180041-037-84f7a235.md). Xác minh chỉ dùng dữ liệu synthetic; không kết luận về identity ownership hoặc lịch sử khách thật.

Kiểm tra bổ sung trang privacy local demo ngày 2026-10-26, run `20261026-183941-474-ddccfd77`:

- Thêm public lazy route `/portal/privacy`, trang nêu purpose, dữ liệu synthetic và sandbox lưu fixture, cách quay lại đúng khách sạn để rút consent, cùng giới hạn không áp dụng cho khách thật. Consent screen dùng `privacyPolicyUrl` trả về từ API.
- Browser test privacy navigation và nội dung: **PASS**. `tools/acceptance/Verify.ps1`: **PASS**; Maven **1.047/0 skip**, API **23/23**, browser **14/14**, lint/build, dependency audit và T12/U14/Core source audits PASS. Snapshot SHA-256 `E5FDF369CC7E390784C55048B51BDE240B8F12874E3C752CD1D6C5E0B9796275` restored; app và containers đã dừng, volumes được giữ.
- Chi tiết: [báo cáo nghiệm thu](../acceptance/runs/acceptance-20261026-183941-474-ddccfd77.md). Xác nhận vẫn chỉ thuộc demo local dùng dữ liệu giả lập.

| Ngày | Thay đổi | Bằng chứng |
|---|---|---|
| 2026-10-26 | Tạo contract local theo lựa chọn người dùng | Source inspection baseline 627887bc; API/schema mới NOT RUN |
| 2026-10-26 | Rà soát trước commit | Chốt race outcomes, giữ policy khi revoke, final authorization và fixture ranking; kiểm tra schema/examples riêng, runtime vẫn NOT RUN |
| 2026-10-26 | I01: migration và fixture binding | Base a683712; PostgreSQL 16 tmpfs; Liquibase 32/32, rerun 0; seed hai lượt; active bindings=2, consent/event=0; constraint checks PASS. Acceptance status-only: 30 applied, dev seed và U14 pending; không ghi DB; snapshot hash giữ nguyên. Acceptance migration/restore và Keycloak volume NOT RUN |
| 2026-10-26 | I02: consent API/CAS | Base bdf8267; CUSTOMER-only, JWT `iss/sub`, revision/binding version CAS, lock binding + guest, atomic state/event. Targeted 21/21; full Maven 1.017 test, 0 lỗi, 8 skip; DB chỉ PostgreSQL 16 tmpfs. Keycloak thật, UI, recommendation và acceptance volume NOT RUN |
| 2026-10-26 | I03: history/catalog read providers | Base bdf8267; AI minimal read ports; Booking bounded scoped native query; Inventory active catalog query. Targeted 30/30 và PostgreSQL 16 disposable query checks 3/3. API orchestration, Keycloak thật, acceptance volume, UI NOT RUN |
| 2026-10-26 | I04: affinity/orchestration | Room-affinity core tách khỏi segment; final consent/binding check; synthetic regression và no-write assertions. Full backend suite và API flow PASS trong I06; real guest history chưa dùng |
| 2026-10-26 | I05: UI/API/browser | Lint/build PASS; real Keycloak/backend API 20/20, browser 13/13; mocked UI interaction tests vẫn được giữ |
| 2026-10-26 | I06: provenance và acceptance | Verify run `20261026-153528-558-84ef82d3` PASS; Maven 1.047/0 skip, API 20/20, browser 13/13; T12/U14/Core audits PASS, validator 184 instances, portability 12/12; snapshot SHA-256 `712E9413...F3650` restored, sandbox stopped |
| 2026-10-26 | I03–I05 follow-up: personalized history real-path coverage | Verify run `20261026-164152-385-46663e64` PASS; Maven 1.047/0 skip, API 21/21, browser 13/13; PERSONALIZED_READY và COLD_START_FALLBACK qua fixture synthetic, timestamp projection sửa sang Instant, no-write digest PASS, snapshot restored và sandbox stopped |
| 2026-10-26 | C13 follow-up: consent conflicts qua API thật | Verify run `20261026-174456-198-a6e9aa8e` PASS; API 22/22; stale revision/policy/binding và grant thiếu policy bị từ chối; DB digest không đổi; snapshot restored và sandbox stopped |
| 2026-10-26 | Follow-up: CUSTOMER chưa liên kết và hotel scope | Verify run `20261026-180041-037-84f7a235` PASS; API 23/23, browser 13/13, Maven 1.047/0 skip; identity-link trả 409, hotel không tồn tại trả 404, DB digest không đổi; snapshot restored và sandbox stopped |
| 2026-10-26 | Follow-up: trang privacy U14 | Verify run `20261026-183941-474-ddccfd77` PASS; API 23/23, browser 14/14, Maven 1.047/0 skip; privacy link/page/revoke guidance test PASS, provenance hashes giữ nguyên contract T12; snapshot restored và sandbox stopped |
