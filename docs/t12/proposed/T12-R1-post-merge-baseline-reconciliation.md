# T12-R1 Post-Merge Source Baseline Reconciliation & Audit Trail

> **Document Type:** Versioned Governance Audit Record & Baseline Reconciliation
> **Target Path:** `docs/t12/proposed/T12-R1-post-merge-baseline-reconciliation.md`
> **Status:** OWNER_AUTHORIZED — Enacted under explicit Project Owner Decision on 2026-10-21
> **Feature:** `T12-R1-Session-Security`
> **PR:** #17 (`feature/T12-r1-review-fixes` -> `develop`)
> **Local Merge Commit:** `8c2c74ac5c83a11f8c4968381cfff22923d1b1ab`
> **Historical Baseline Commit:** `0e6d379eba8ab6ac7cf3573728d92d97f3e659e6` (Frozen on 2026-10-08)
> **Merged Develop Commit:** `bf666917e43c8dcb2e55a70929d4a26346b3ab4b`
> **Evaluation Dataset SHA-256:** `8b8f086d5da5f08679f63053e9d17dbc1cbaa510b68a8c16c68e2da680c6bf5c` (Unmodified)

---

## 1. Executive Summary & Authorization Basis

On 2026-10-21, following the integration of the latest `develop` branch (`bf666917e43c8dcb2e55a70929d4a26346b3ab4b`) into `feature/T12-r1-review-fixes` (Merge Commit `8c2c74ac5c83a11f8c4968381cfff22923d1b1ab`), the offline artifact validator (`tools/t12/validate_r1_contracts.py`) detected differences between the current working tree and the historical audit evidence in `docs/t12/r1-00/v2.0.0-draft.1/baseline-source-evidence.json`.

The **PROJECT OWNER / FINAL AUTHORITY** authorized a narrowly scoped, traceable reconciliation of the T12 source-evidence baseline to reflect legitimate upstream changes while permanently preserving historical audit evidence.

This document establishes the permanent, tamper-evident audit record of that reconciliation.

---

## 2. Reconciled Files & Hash Audit Matrix

Four shared source files listed in `baseline-source-evidence.json` were modified on `develop` prior to the merge. None of these files belong to Member 1 or the T12 Room Advisor module; all changes originated from approved Member 2 and Member 3 pull requests merged into `develop`.

| # | File Path | Historical Hash (`0e6d379`) | Reconciled Hash (`bf66691` / `HEAD`) | Originating Commit / PR | Owner |
|---|---|---|---|---|---|
| 1 | `backend/src/main/java/vn/edu/utc/hotel_booking/common/exception/ErrorCode.java` | `407fb57045db44baadd3665d8b25ee81cb8972439a2480af4b1f7a34a8708a9f` | `b6c113e6dca4514cf335132168d9889e5dc187e421858ff624f88a807ed41f98` | Commit `c7fc19f` (PR #20 "Feature/thanh f xuaan/t08 fit") | Member 2 |
| 2 | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/inventory/service/impl/RoomAvailabilityServiceImpl.java` | `1e54ee1d889eddfc6462aaca5a13e1cec533a7efca74b42bae880996db56cc21` | `6d1ec1dc376f503ff9a23b9e9a1262120f11e64aa57fce2d4cff7d4ba1ef7cfd` | Commit `c7fc19f` (PR #20 "Feature/thanh f xuaan/t08 fit") | Member 2 |
| 3 | `backend/pom.xml` | `a9cdaf257a014858242c2844453503b59714258e793b94367ae04e217410d3cc` | `1eaa80c0bb6458a2c550f19378a22818649ada190f5c23e2aed44d64e1850f0c` | Commit `c7fc19f` (PR #20 "Feature/thanh f xuaan/t08 fit") | Member 2 |
| 4 | `frontend/src/App.tsx` | `74548b3331c764736f311d0d3810ec67fd3381f376f9f506b7748e29aa36f2d3` | `1284c1e4eae2b6902431cf6737b7417c11f7cd3a92c4a259cc6906d012877351` | Commit `67d573a` (PR #19) & `c7fc19f` (PR #20) | Member 3 / Member 2 |

*(Note: The remaining 30 files in `baseline-source-evidence.json` and all 23 files in `protected-user-files.json` remained 100% byte-identical).*

---

## 3. Technical Justification & Zero-Breakage Verification

### 3.1 `ErrorCode.java`
- **Upstream Change:** PR #20 added business error codes for booking cancellation, No-Show policies (7100s), check-in/out rules, and surcharge fees.
- **T12 Impact:** **Zero impact**. Under PROJECT OWNER decision **DEC-06-01**, T12 uses an advisor-local error response envelope (`vn.edu.utc.hotel_booking.modules.ai.advisor.error.RoomAdvisorErrorResponse`) and enum `RoomAdvisorErrorResponse.Reason`. T12 does not import or depend on `ErrorCode.java`.

### 3.2 `RoomAvailabilityServiceImpl.java`
- **Upstream Change:** PR #20 strengthened `findAvailableRooms` with active-status checks (`is_active = 1`) and date boundaries.
- **T12 Impact:** **Zero impact / strictly beneficial**. The T12 adapter `PublicBookingCoreAdapter` calls `RoomAvailabilityService` read-only. Candidate discovery benefits from tighter availability guarantees.

### 3.3 `backend/pom.xml`
- **Upstream Change:** PR #20 added test and utility dependencies for Member 2's booking policy pipelines.
- **T12 Impact:** **Zero impact**. All T12 dependencies and existing Maven build definitions remain intact.

### 3.4 `frontend/src/App.tsx`
- **Upstream Change:** PR #19 and PR #20 registered new portal routes for user settings, policy administration, and checkout settlement.
- **T12 Impact:** **Zero impact**. Room Advisor UI has not yet been integrated into `App.tsx` (remains a Member 4 pending deliverable under GATE-03).

---

## 4. Preservation of Historical Evidence & Governance Integrity

1. **Historical Records Preserved:**
   - The historical decision register `docs/t12/r1-00/v2.0.0-draft.1/decisions-and-approvals.json` retains its original `"createdAt": "2026-10-08"` and historical `"baselineCommit": "0e6d379eba8ab6ac7cf3573728d92d97f3e659e6"`. It is NOT rewritten.
   - `baseline-source-evidence.json` retains `"gitSha": "0e6d379eba8ab6ac7cf3573728d92d97f3e659e6"` while embedding the historical SHA and reconciled SHA for each of the four updated files.
2. **Contract Artifact Invariance:**
   - Frozen OpenAPI specification `docs/t12/r1-00/v2.0.0-draft.1/openapi.json`: **UNTOUCHED**.
   - Offline contract validator `tools/t12/validate_r1_contracts.py`: **UNTOUCHED**.
   - Evaluation dataset `eval/t12/r1/v0.1.0-draft/cases.json` (`8b8f086d5da5f08679f63053e9d17dbc1cbaa510b68a8c16c68e2da680c6bf5c`): **UNTOUCHED**.
3. **Full Regression Verification:**
   - Full Backend Suite: 812 tests run, 0 failures, 0 errors, 5 skipped.
   - T12 Security & Recommendation Suite: 239 tests run, 0 failures, 0 errors.
   - Architecture Zero-Business-Write: 2 tests run, 0 failures, 0 errors.

---

## 5. Addendum — Integration of Merged PR #21 (Member 2 CORS & Retry-After Fix)

### 5.1 Authorization & Context
- **Date:** 2026-10-23
- **Integrated PR:** Pull Request #21 (`feature/m2-cors-retry-after` -> `develop`)
- **PR Title:** `fix(security): expose Retry-After and support configured CORS origins`
- **Merge Commit into develop:** `36be1edb0069d35e18c0bc3c71e4e28cb6ee9bbc` (Merged by repository authority `NDTrung36`)
- **T12 Merge Commit:** `a191f14` (`merge(develop): integrate PR 21 CORS fix into feature/T12-r1-review-fixes`)
- **Owning Member:** Member 2 (Backend Infrastructure & Security Lead)

### 5.2 Reconciled File & Hash Audit Matrix

| # | File Path | Historical Hash (`0e6d379` / pre-PR #21) | Reconciled Hash on disk (`36be1ed` / `HEAD`) | Reconciled Unix LF Hash | Originating PR | Owner |
|---|---|---|---|---|---|---|
| 5 | `backend/src/main/java/vn/edu/utc/hotel_booking/common/config/SecurityConfig.java` | `8e62328594bc3ed31317db512a822f451205ac8d93dd4f5e1f255d56580d3609` | `aa55bde7f8ebcec77e0ab0be3f987541a8e15bd0ba20535d1bae3e73ceb48b1f` | `88d6258c602dcd769bc6176f8119c3506f038a822db3cfb62f02e980b5541dcb` | PR #21 | Member 2 |

### 5.3 Technical Justification & Zero-Breakage Verification
- **Upstream Change:** PR #21 updated `SecurityConfig.java` to:
  1. Add `.exposedHeaders(List.of("Retry-After"))` to `CorsConfiguration`.
  2. Support configurable frontend origins via `@Value("${app.cors.allowed-origins:http://localhost:5173}")`.
- **T12 Impact:** **Zero negative impact / strictly enables GATE-02 closure and frontend integration**.
  T12 rate-limiting middleware emits `429 Too Many Requests` with a `Retry-After` header. Exposing `Retry-After` in CORS headers allows frontend clients (Member 4 / GATE-03) to read the backoff duration cleanly. Configurable origins allow production/staging environments to connect without CORS errors.
- **Contract & Governance Invariance:**
  - Frozen OpenAPI (`openapi.json`): **UNTOUCHED**.
  - Contract validator (`tools/t12/validate_r1_contracts.py`): **UNTOUCHED** (Status: `PASS`).
  - Evaluation cases (`eval/t12/r1/v0.1.0-draft/cases.json`): **UNTOUCHED**.
  - Decisions registry (`decisions-and-approvals.json`): **UNTOUCHED**.
