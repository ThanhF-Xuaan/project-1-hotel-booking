# Member 1 Local Acceptance Dossier & Verification Report

> **Đối soát sau PR #33 (2026-10-26): CONFIRMED — PASS local trên `develop@96d7815`.** Các PR mới bổ sung phòng đoàn/block inventory, master folio, BI và hai migration; đã tạo snapshot trước migration, đối soát hai hash source Core và giữ lịch sử. Full Maven 999 test (0 lỗi, 1 skip; PostgreSQL 3/3, Redis 4/4), API 17/17, browser 6/6, portability 12/12, Core provenance 10/10, validator/source audit, lint/build và npm audit PASS. Regression mới xác nhận T12 refresh theo group-block/release của Core và digest DB không đổi quanh từng advisor request. Snapshot sau migration đã restore; app/container dừng, volume giữ lại. Xem [phạm vi, kết quả và giới hạn](post-merge-revalidation-2026-10-26.md) và [report cuối](runs/acceptance-20261026-111047-405-831989ee.md).

> **Runner tái lập sau PR #28 (2026-10-25 21:43 +07:00): CONFIRMED — PASS.** Repo chính đã cập nhật `develop@e027e90`; diff tooling trên `codex/m1-repeatable-acceptance` chạy được bằng `Verify.ps1`. Full Maven 943 test (0 lỗi, 1 skip contextLoads; PostgreSQL 3/3 và Redis 4/4 thực chạy), API 16/16, browser 6/6, artifact/portability/source audit, lint/build và npm audit đều PASS. Đã restore snapshot riêng, dừng app/container và giữ volume. Các lần chạy lỗi của runner được giữ thành báo cáo riêng. Xem [báo cáo cuối](runs/acceptance-20261025-213907-808-024fdc71.md) và [phạm vi/kiểm tra các trường hợp từ chối](repeatable-runner-2026-10-25.md).

> **T05 concurrency/rollback và bàn giao nhánh (2026-10-25 20:58 +07:00): CONFIRMED — kiểm tra local PASS.** Đã tái hiện hai export cùng thành công trên một queue PENDING, rồi sửa bằng row lock/READ_COMMITTED. PostgreSQL xác nhận một winner, request còn lại export-empty; lỗi I/O và lỗi CHECK thật sau flush đều rollback và retry thành công. File spill thật được dọn ở cả success/failure. Kết quả cuối: Maven 943 test (0 lỗi, 1 skip; Redis 4/4, PostgreSQL 3/3 thực chạy), API 16/16, browser 6/6, lint/build/audit và T12 validators PASS. Template BCA chính thức và U14 dữ liệu thật vẫn chưa được nghiệm thu. Xem [bằng chứng và điều kiện tái hiện](t05-export-concurrency-rollback-2026-10-25.md).

> **T05 technical hardening (2026-10-25 19:44 +07:00): CONFIRMED.** Đã sửa fail-open scope tại Export/Queue/StayGuest, chặn scope ID bị wrap sang Short, validate giờ chốt 0..23 và thống nhất giờ workbook theo UTC+7/uppercase ổn định. API đọc nội dung XLSX thật, metadata EXPORTED, export lặp và hai phía mốc cuối giờ. Maven toàn backend 938 test (0 lỗi, 1 skip, Redis 4/4), test mục tiêu 56/56, API 15/15, browser 6/6, lint và T12 validators đều PASS. Chưa xác nhận template chính thức hoặc concurrency/I/O fault của export. Xem [phạm vi, bằng chứng và giới hạn](t05-technical-hardening-2026-10-25.md).

> **U14 privacy gate và T12 re-audit (2026-10-25 18:59 +07:00): CONFIRMED — kiểm tra kỹ thuật PASS.** U14 HTTP mặc định tắt, chỉ admin được dùng khi bật chế độ đánh giá giả lập; default trả 404 và non-admin bị 403. T12 source được đối soát có version, validator 184 instance và portability 11/11 PASS. Maven toàn backend 909 test (0 lỗi, 1 skip, Redis 4/4 thực chạy), lint/build, audit 0 lỗ hổng, API 12/12 và browser 6/6 PASS. T05 official schema và U14 consent/history thật vẫn chưa được nghiệm thu. Xem [quyết định, phạm vi và kết quả](privacy-audit-followup-2026-10-25.md) và [T12 re-audit](../../t12/re-audits/2026-10-25-lazy-routes-v1.md).

> **Đối soát sau merge `develop` (2026-10-25 17:54 +07:00): PARTIAL.** Maven toàn backend 899 test, 0 lỗi, 1 test điều kiện hạ tầng bị skip; Redis integration 4/4, audit 0 lỗ hổng, lint/build, API 11/11 và browser 6/6 đều PASS. T12 contract validator bị chặn vì hash nguồn `frontend/src/App.tsx` khác baseline audit sau thay đổi tách bundle; portability 10/11, ca còn lại gọi lại validator và lỗi cùng nguyên nhân. T05 chỉ xác nhận kỹ thuật XLSX, chưa xác nhận schema chính thức; U14 chỉ dùng consent/history giả lập. Xem [báo cáo đối soát develop](develop-followup-acceptance-2026-10-25.md).

> **Tách bundle theo route (2026-10-25 17:39 +07:00): CONFIRMED — build hết cảnh báo kích thước.** Chunk lớn nhất hiện là 246.80 KB, so với 878.71 KB trước khi tách. Lint, 6 test browser với backend thật và 1 test form trên production preview đều PASS. Xem [bằng chứng tối ưu bundle](frontend-bundle-splitting-2026-10-25.md). Cảnh báo bundle trong các report trước là kết quả lịch sử.

> **Khắc phục dependency (2026-10-25 17:15 +07:00): CONFIRMED trên branch `codex/fix-frontend-audit`.** Từ baseline develop `25ac13f`, năm package bị audit đánh dấu đã được vá; cài lại bằng `npm ci` và chạy audit đầy đủ đạt 0 vulnerability. Lint/build, 11 test API và 6 test browser đều PASS. Xem [bằng chứng khắc phục dependency](dependency-audit-remediation-2026-10-25.md). Các kết quả merge bên dưới là hồ sơ lịch sử trước đợt vá này.

> **Xác minh tích hợp develop (2026-10-25 17:03 +07:00): CONFIRMED — nghiệm thu chức năng local tại thời điểm đó.** Maven `clean test` đạt 899 test (0 lỗi, 5 skip); sáu suite còn lại đạt PASS. Hai migration mới của develop đã chạy trên sandbox; snapshot đã khôi phục và sandbox đã dừng. Runner chạy với `-SkipMaven` giữ kết quả `PARTIAL` vì Maven được xác minh riêng. Xem [bản đối soát merge](develop-merge-verification-2026-10-25.md) và [kết quả runner mới](develop-merge-runner-2026-10-25.md). Khi đó dependency audit còn 5 vấn đề mức high; chúng được xử lý trong đợt sau.

> **Revalidation (2026-10-25 13:29 +07:00): PASS.** The full local acceptance runner completed all 7 suites: Maven 338 tests (0 failures, 4 skipped), T12 contract validation (184 instances), portability validation (11/11), frontend lint (0 errors/warnings) and build, Playwright API (11/11), and browser E2E (4/4). The acceptance launcher removes inherited `GEMINI_API_KEY`, so advisor chat uses deterministic extraction without calling Gemini. The browser no-write check sent an actual advisor request and compared normalized PostgreSQL data dumps before and after; it verifies the exercised path's effect, not read-only DB credentials. Liquibase had no pending changesets. The database was restored from the pre-test snapshot (2 hotels, 5 room types, 305 availability rows, 5 staff), then application processes and containers were stopped with Docker volumes preserved. Full commands and timings are in [acceptance-test-report.md](acceptance-test-report.md). Run used branch `codex/m1-acceptance-stabilization` at base `a6b2088f55d2326a51e895ff2cfffbb4fe70831e`; at that time, changes were uncommitted.

- **Status hiện tại:** **LOCAL TECHNICAL CHECKS PASS — T05/U14 real-data contracts BLOCKED**
- **Environment:** Local Sandbox (`hotel-m1-acceptance`)
**Evaluation Date:** 2026-10-25
- **Historical Worktree Base (13:29 run):** `a6b2088f55d2326a51e895ff2cfffbb4fe70831e` on branch `codex/m1-acceptance-stabilization`

---

## 1. Executive Summary

This dossier records local technical checks and their limits for T04, T05, T12, U13, U14, and Scoped RAG. The latest results are in the dated notices above; T05 official export compatibility and U14 use of real consent/history remain unverified.

The historical 13:29 run completed 7 suites (338 Maven unit/integration tests, 184 contract validation instances, 11 portability tests, 0 ESLint errors/warnings, TypeScript 6 + Vite 8.2 build, 11 Playwright API tests, and 4 browser tests). This does not establish the outstanding real-data contracts.

---

## 2. Sandbox Topology & Port Allocation

The acceptance sandbox runs in Docker Compose project `hotel-m1-acceptance`; its published service ports bind to loopback only. `Prepare.ps1` writes fixed synthetic credentials for this disposable local test environment; it does not generate random secrets. Do not reuse these credentials outside the local sandbox.

| Service | Target | Port | Protocol / Realm | Notes |
|---|---|---|---|---|
| **PostgreSQL** | `acceptance_postgres` | **15432** | `hotel_acceptance_db` | Liquibase master-data + acceptance fixtures (2 hotels, 5 room types, 300 inventory rows) |
| **Redis** | `acceptance_redis` | **16379** | Standalone DB 0 | Dedicated test cache |
| **Keycloak IAM** | `acceptance_keycloak` | **18081** | `hotel-acceptance` | 6 preconfigured acceptance users (`admin`, `region_mgr`, `manager_h1`, `manager_h2`, `reception_h1`, `test_customer`); local master admin defaults to `admin/admin` |
| **Backend API** | Host Spring Boot 4.0.7 | **18080** | REST / OAuth2 Resource Server | Java 25 runtime, connected to sandbox Keycloak & PostgreSQL |
| **Frontend UI** | Host Vite 8.2 / React 19 | **13000** | HTTP Single Page App | Connected to `http://localhost:18080`, Keycloak realm `hotel-acceptance` |

---

## 3. Verified Acceptance Evidence Summary

Detailed test execution report generated by `tools/acceptance/Test.ps1` (saved at `docs/member1/acceptance/acceptance-test-report.md`):

| Category | Suite | Command | Status | Duration | Details / Coverage |
|---|---|---|:---:|---:|---|
| **Backend** | Maven Core Modules | `mvnw.cmd test -Dtest=*Ocr*,*Rag*,*Personalization*,*Advisor*,*Utility*,*Lodging*,*Payroll*` | **PASS** | 19.14s | **338 tests executed, 0 failures, 0 errors, 4 skipped** (real-redis conditional) |
| **Contracts** | T12 Contract Schema | `python tools/t12/validate_r1_contracts.py` | **PASS** | 1.50s | **184 instances, 74 cases, 23 protected hashes matched** |
| **Contracts** | Portability Validation | `python tools/t12/test_portability_validation.py` | **PASS** | 2.10s | **11 unit tests passed** (cross-platform LF/CRLF normalization & tamper detection) |
| **Frontend** | ESLint Verification | `npm run lint -- --max-warnings=0` | **PASS** | 9.62s | **0 errors, 0 warnings** across all frontend source files |
| **Frontend** | Vite Build & Types | `npm run build` | **PASS** | 7.25s | **TypeScript 6 + Vite 8.2 bundle compiled cleanly** |
| **Integration** | Playwright API Suite | `npm run test:api` | **PASS** | 8.04s | **11/11 tests passed** across 6 Keycloak RBAC roles, OCR, T04, T05, U14, RAG |
| **E2E Browser** | Playwright Browser Suite | `npm run test:e2e` | **PASS** | 11.09s | **4/4 browser tests passed** (T12 conversational flow and no-write state check, session isolation, rate limiting) |

**Totals:** 7 Suites | 7 PASSED | 0 FAILED | 0 suites skipped | **Pass Rate: 100%** (Maven conditionally skipped 4 real-Redis tests)

---

## 4. Key Architectural & Invariant Verifications

### 4.1. T12 Room Recommendation Assistant
- **Conversational Search:** Natural language search for Hanoi (`2026-11-01` to `2026-11-03`, 2 adults) retrieves verified room cards from Core contracts (`Acceptance Luxury Hanoi`).
- **MVP Single-Room Boundary:** Queries requesting multi-room reservations (e.g. 5 rooms for 20 people) trigger deterministic single-room clarification without booking allocation or price multiplication.
- **Card Navigation:** Clicking "Xem chi tiết" navigates to verified room details URL `/portal/hotels/:hotelId/room-types/:hotelRoomTypeId` with query parameters.
- **Zero Business Writes:**
  - Room details page has zero booking submit or payment mutation buttons.
  - Playwright sends a real local advisor chat request and compares a SHA-256 digest of PostgreSQL data before and after; this verifies the exercised flow leaves persisted data unchanged, not that the backend connection uses a read-only database role.
  - Architecture tests confirm `RoomAdvisorOrchestrationService` has zero dependencies on booking/payment repositories.
- **Session Security & Isolation:**
  - Anonymous session cookie `t12_session` scoped to path `/api/v1/public/ai/room-advisor`.
  - Independent browser contexts maintain completely isolated session IDs and criteria states.
  - Rate limiting cooldown banner activates upon rapid submissions with a 5-second countdown timer and disables the send button.

### 4.2. U13 Document OCR & Receptionist Extraction
- Synthetic CCCD / Passport text extraction returns draft guest records with confidence metrics.
- Missing or corrupted OCR text safely returns `UNREADABLE_DOCUMENT` draft without invoking external extractors with empty strings.
- Path traversal in document file names is rejected with `400 Bad Request`.

### 4.3. T04 Utility Meters & Readings
- Meter readings filtered and scoped by hotel ID (`hotelId = 1`).
- Anomaly detection verifies non-negative consumption and duplicate prevention.

### 4.4. T05 Lodging Reports & Payroll Import
- Lodging export streams a technical XLSX report for authorized staff. Compatibility with an official BCA template requires a supplied schema/sample and remains unverified.
- Property managers/receptionists must resolve a valid PROPERTY hotel scope; wrong/missing/out-of-range scope fails closed before business queries or writes.
- Current technical workbook uses 13 existing columns, preserves document numbers as text, renders times in UTC+7, and includes all instants before the end of the selected cutoff hour. EXPORTED means file creation completed, not delivery/approval by an external authority.
- Pending export rows are locked through the READ_COMMITTED transaction: overlapping exports of the same pending row create one batch. Generation/DB failure rolls back metadata and permits retry. Manual queue retry can intentionally reopen an exported row; this is not a permanent idempotency key.
- Synthetic payroll CSV import processes employee lodging deductions with complete row tracking.

### 4.5. U14 Privacy-Safe Personalization
- The HTTP controller is disabled by default. Only `CHAIN_ADMIN` can call it when `hotel.ai.personalization.synthetic-evaluation-enabled=true`.
- In this synthetic mode, `consentGranted=false` returns HTTP 200 with `CONSENT_REQUIRED`; `true` exercises the deterministic algorithm with supplied fake history. This client flag is not proof of actual consent or history ownership.
- Pricing remains unchanged. A server-authoritative consent/history contract is required before enabling a real-data flow.

### 4.6. Scoped RAG Lifecycle
- In-memory vector store with local hashing embeddings operates with tenant scoping (`hotelId`).
- Cross-tenant queries are strictly isolated; knowledge queries for hotel 1 never leak into hotel 2.

---

## 5. Deterministic Orchestration Runbook

### Quy trình đầy đủ sau PR #28

Từ repo chính, khi sandbox đã dừng và dependencies đã cài (`npm ci`, Chromium Playwright, Java/Maven wrapper, Python có jsonschema, Docker Desktop):

```powershell
pwsh -NoProfile -File tools/acceptance/Verify.ps1
```

Runner kiểm tra đích local cố định và khóa độc quyền, dựng hạ tầng/fixture, tạo snapshot riêng, chạy toàn bộ Maven với PostgreSQL/Redis IT thật trước khi mở ứng dụng. Sau đó chạy source re-audit, npm audit, contract/portability, lint/build và API/browser E2E. Maven dùng process riêng đã bỏ generic DB/Redis/app env để không ảnh hưởng kiểm tra cấu hình mặc định. SHA, script hashes, lệnh, exit code và skip thật được ghi cùng evidence; báo cáo mỗi lần có tên riêng dưới `runs/`.

Khi xong hoặc lỗi test, runner dừng đúng tiến trình trong manifest, restore snapshot rồi dừng container và giữ volume. Lỗi ownership/restore giữ hạ tầng để kiểm tra, trả FAIL. Snapshot là dữ liệu giả lập sau migration/seed của lần chạy, không phải backup dữ liệu trước khi khởi tạo sandbox. Không dùng quy trình này cho dữ liệu cần bảo toàn.

Test.ps1 riêng vẫn ghi PARTIAL/exit 1 khi skip Maven; Verify chỉ kết luận sau khi kiểm tra evidence Maven riêng và từng suite còn lại. T05 official format và consent/history U14 thật tiếp tục ngoài nghiệm thu kỹ thuật này. Xem [thiết kế và kết quả runner](repeatable-runner-2026-10-25.md).

### Quy trình từng bước hiện hữu

The acceptance lifecycle can be reproduced from scratch using the PowerShell tool suite in `tools/acceptance/`:

`Prepare.ps1` explicitly enables synthetic U14 only for this sandbox. Existing runtime files that omit the flag keep U14 disabled; stop the sandbox and regenerate its runtime configuration before running the synthetic API suite. The flag does not determine whether an input is real or fake; use controlled synthetic fixtures only.

The PostgreSQL export tests require a separate explicit `T05_EXPORT_IT` opt-in and exclusive sandbox DB confirmation. Run them with application processes stopped and credentials passed through the dedicated `T05_EXPORT_IT_DB_*` variables; do not inherit Compose's generic DB/Redis variables into Maven default-configuration tests. See the latest concurrency/rollback report for guards, commands and actual skips.

```powershell
# 1. Preflight check: verify port availability and write the disposable local runtime configuration
powershell -File tools/acceptance/Prepare.ps1

# 2. Launch Docker services, apply Liquibase master-data migrations, and seed UTF-8 acceptance fixtures
powershell -File tools/acceptance/Start.ps1

# 3. Create clean database snapshot for idempotent test resets
powershell -File tools/acceptance/Snapshot.ps1

# 4. Execute all 7 acceptance suites (Maven, Contracts, ESLint, Build, API, Browser)
powershell -File tools/acceptance/Test.ps1

# 5. Restore database state from snapshot to verify rollback and data integrity
powershell -File tools/acceptance/Restore.ps1

# 6. Tear down Docker containers and networks when testing is complete
powershell -File tools/acceptance/Stop.ps1
```

---

## 6. Git State & Integrity — historical 13:29 run

- **Active Worktree:** `C:/Users/admin/.codex/worktrees/m1-acceptance-stabilization/project-1-hotel-booking`
- **Branch:** `codex/m1-acceptance-stabilization`
- **Base HEAD:** `a6b2088f55d2326a51e895ff2cfffbb4fe70831e`
- **Remote / Commit Status:** This evidence predates the local delivery commit. No push, rebase, or external PR merge has been performed.


## Cập nhật nghiệm thu Member 1 — 31/10/2026

CONFIRMED — technical acceptance local/synthetic PASS; V03 business data integration vẫn NOT READY. Báo cáo hiện tại: [m1-feature-completion-report-2026-10-31.md](m1-feature-completion-report-2026-10-31.md), [benchmark](m1-latency-benchmark.md), [SAFE](safe-01-local-2026-10-31.md) và [V02](v02-t04-t05-regression-2026-10-31.md). Các phần bên trên là lịch sử, không thay kết quả hiện tại.

Lệnh đầy đủ: `.\tools\acceptance\Verify.ps1 -IncludeM1Benchmark -IncludeM1AdditionalTests`. Profile lần này khai báo HOTEL_AI_OCR_ENABLED=true, TESSERACT_EXE trỏ executable có vie+eng và ACCEPTANCE_VOLUME_PREFIX riêng trong runtime local được Git ignore; không áp vào production. Benchmark kiểm volume khớp prefix, không dùng lại dữ liệu ngoài sandbox. Pgvector opt-in chạy riêng và restore riêng; main HTTP dùng in-memory. Hướng dẫn này không biến các optional test chưa chạy thành PASS.


## GitHub delivery review - 31/10/2026

Latest review/fix evidence and Git authorization: [m1-github-delivery-review-2026-10-31.md](m1-github-delivery-review-2026-10-31.md). Earlier local reports retain their historical Git status. Actual delivery is recorded in the PR and post-merge receipt. Business data/official-format gates remain NOT READY.
