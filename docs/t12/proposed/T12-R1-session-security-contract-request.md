# T12-R1-Session-Security Contract Reconciliation & Deferred-Expansion Proposal

> **Document Type:** Non-normative Proposal & Contract Reconciliation (Member 1 Documentation Territory)
> **Target Path:** `docs/t12/proposed/T12-R1-session-security-contract-request.md`
> **Status:** PROPOSED — Non-normative; outside frozen baseline. Does not modify frozen contracts or grant integration approval.
> **Feature:** `T12-R1-Session-Security`
> **Task:** `TASK-005` (Revision 8 Plan, Handoff H035)
> **Base SHA:** `ca07a3ffa129e16843a884a4c5e8b29cda7043c3`
> **Branch:** `feature/T12-r1-review-fixes`
> **Worktree:** `D:/Project/University/project-1-hotel-booking-t12`

---

## 1. Executive Summary & Purpose

This document satisfies **TASK-005** of the approved Revision 8 plan for `T12-R1-Session-Security`.
Its objectives are:
1. Reconcile implemented runtime behavior from TASK-001 through TASK-004 against the frozen R1-00 contract (`docs/t12/r1-00/v2.0.0-draft.1/**`), providing a verified gap inventory labeled with exact governance classifications (`CONFIRMED`, `PROPOSED`, `BLOCKED`).
2. Document the formal resolution of contract gaps under **DEC-06-01** enacted by **PROJECT OWNER / FINAL AUTHORITY**.
3. Specify deferred session-lifecycle endpoints (`GET /session/{sessionId}`, `DELETE /session/{sessionId}`) as a **PROPOSED** future expansion without modifying current frozen schemas or wire behaviors.
4. Record condition matrices for Member 2 (Security/Infrastructure) and Member 4 (Frontend/Consumer), the proposed request for CORS `Retry-After` header exposure, and the consumer checklist.
5. Reconcile the evaluation artifact hash discrepancy (`evaluationCasesSha256`) observed between coordination branches.

---

## 2. Frozen Contract Integrity (AC-5.2 & AC-5.4)

### 2.1 Explicit Preservation Statement
The following contract baseline artifacts are **strictly frozen** and have **NOT** been modified in any way during this task or preceding tasks:
- `docs/t12/r1-00/v2.0.0-draft.1/**` (OpenAPI specification `openapi.yaml`, JSON schemas `ErrorResponse.json`, `AdvisorResponse.json`, `CriteriaExtractionProposal.json`, and all examples).
- `tools/t12/validate_r1_contracts.py` (Offline validator script).
- `eval/**` (Evaluation dataset, including `eval/t12/r1/v0.1.0-draft/cases.json`, schemas, manifest, and governance coverage).

### 2.2 Git Verification Evidence
```bash
$ git -C D:/Project/University/project-1-hotel-booking-t12 status --porcelain -- docs/t12/r1-00 tools/t12 eval
# Result: EMPTY (Exit Code 0 — Zero modifications across all frozen paths)
```

### 2.3 Offline Contract Validator Execution
```bash
$ python D:/Project/University/project-1-hotel-booking-t12/tools/t12/validate_r1_contracts.py
{
  "status": "PASS",
  "scope": "ARTIFACT_VALIDATION_ONLY",
  "engineEvaluation": "NOT_RUN",
  "counters": {
    "schemas_validated": 3,
    "refs_resolved": 53,
    "openapi_documents_validated": 1,
    "instances_validated": 184,
    "response_invariants_checked": 6,
    "core_fixture_triplets_checked": 26,
    "evaluation_cases_checked": 74,
    "evaluation_patches_resolved": 69,
    "governance_refs_resolved": 6,
    "governance_requirements_mapped": 35,
    "protected_user_hashes_checked": 23,
    "source_evidence_hashes_checked": 34,
    "negative_cases_rejected": 33,
    "markdown_links_resolved": 29
  },
  "caseCategories": {
    "hard_constraints": 12,
    "ranking": 9,
    "pricing": 13,
    "multi_turn": 11,
    "security": 7,
    "no_write": 9,
    "errors": 13
  },
  "evaluationCasesSha256": "8b8f086d5da5f08679f63053e9d17dbc1cbaa510b68a8c16c68e2da680c6bf5c",
  "contractArtifactVersion": "2.0.0-draft.1",
  "evaluationVersion": "0.1.0-draft"
}
```
Exit code: `0`, Status: `PASS`.

### 2.4 Reconciliation of `evaluationCasesSha256` Discrepancy
- **Observed Discrepancy:**
  - Handoffs H015–H031 reported: `evaluationCasesSha256 = 8b8f086d5da5f08679f63053e9d17dbc1cbaa510b68a8c16c68e2da680c6bf5c`.
  - Handoff H034 reported: `evaluationCasesSha256 = e307d4a9ae9ff3b6b31b1704b7a063c2c9b25e70bb7e0b65b2a5cc3305efd8f2`.
- **Root Cause Analysis (CONFIRMED by Git & Filesystem Inspection):**
  1. The coordination repository root (`D:/Project/University/project-1-hotel-booking`) is on branch `feature/T05-lodging-payroll-review-fixes`. In that branch, `eval/t12/r1/v0.1.0-draft/cases.json` has SHA-256 `e307d4a9ae9ff3b6b31b1704b7a063c2c9b25e70bb7e0b65b2a5cc3305efd8f2`.
  2. The implementation worktree (`D:/Project/University/project-1-hotel-booking-t12`) is on branch `feature/T12-r1-review-fixes` at HEAD `ca07a3ffa129e16843a884a4c5e8b29cda7043c3`. In this branch, `eval/t12/r1/v0.1.0-draft/cases.json` has SHA-256 `8b8f086d5da5f08679f63053e9d17dbc1cbaa510b68a8c16c68e2da680c6bf5c`.
  3. During H034, the validator command was inadvertently executed from the coordination repository root instead of the implementation worktree.
  4. As previously recorded in Handoff H027 (line 1822):
     > *`python tools/t12/validate_r1_contracts.py` (trong coordination root) | PASS nhưng khác hash | `evaluationCasesSha256=e307d4a9...` — cảnh báo: coordination root ở nhánh `feature/T05-lodging-payroll-review-fixes` dùng bản `eval/` khác; luôn validate trong `...-t12`.*
  5. The true baseline hash for feature `T12-R1-Session-Security` on branch `feature/T12-r1-review-fixes` is **`8b8f086d5da5f08679f63053e9d17dbc1cbaa510b68a8c16c68e2da680c6bf5c`**. The frozen evaluation data on this branch has remained completely unmodified.

---

## 3. Verified Gap Inventory & Classification (AC-5.1 & AC-5.5)

Every gap between the implemented runtime behavior and the frozen R1-00 contract is documented below with its verified classification:

| # | Topic | Implemented Runtime Behavior | Frozen R1-00 Baseline Contract | Status | Rationale & Governance Evidence |
|---|---|---|---|---|---|
| 1 | **HTTP 429 & `Retry-After` Header** | Controller emits HTTP 429 with integer `code: 429`, advisor-local `RoomAdvisorErrorResponse` (`reason: "RATE_LIMITED"`), and an additive HTTP response header `Retry-After: <seconds>` (positive integer). | Frozen OpenAPI defines HTTP 429 with `ErrorResponse` schema but does not declare `Retry-After` header under 429 response headers. | **CONFIRMED** | Additive header is permitted by **PROJECT OWNER final DEC-06-01** (Revision 8 §13.7) with conditional concurrence from Member 2 and Member 4. Verified in `PublicRoomAdvisorControllerTest` and `RoomAdvisorRateLimiterTest`. OpenAPI remains frozen (no edit). Limitation: cross-origin clients cannot read `Retry-After` until Member 2 exposes it in CORS. |
| 2 | **Cookie Name & Signature** | Cookie name remains `t12_session`. The cookie *value* contains an HMAC-SHA256 signature (`<sessionId>.<expiryEpochSeconds>.<signatureHex>`). No custom client identity headers are required or accepted. | Frozen OpenAPI `securitySchemes` defines `SessionCookie` as an `apiKey` in `cookie` named `t12_session` with description: *"Signed anonymous session cookie issued and refreshed by Room Advisor"*. | **CONFIRMED** | No wire format change. The earlier proposal to rename the cookie to `t12_anon_token` or add `X-Advisor-Anonymous-Token` is **formally withdrawn**. The signed value conforms directly to the description in frozen OpenAPI. |
| 3 | **HTTP 404 Indistinguishability** | Absent sessions, expired sessions, and sessions belonging to a foreign owner return identical HTTP 404 responses: status 404, integer `code: 404`, message `"Session not found"`, reason `"SESSION_NOT_FOUND"`, `retryable: false`. Only the per-request UUID `requestId` differs. | Frozen OpenAPI defines HTTP 404 with `ErrorResponse`. | **CONFIRMED** | Governed by **PROJECT OWNER final DEC-06-01**. Supersedes previous speculative full-byte identity requirement (since each request generates a unique UUID `requestId`). Non-disclosure prevents session enumeration and cross-tenant snooping. |
| 4 | **Absence of GET/DELETE Session Endpoints** | Exactly 3 public POST endpoints exist: `/api/v1/public/room-advisor/chat`, `/api/v1/public/room-advisor/search`, `/api/v1/public/room-advisor/refresh`. No GET or DELETE session endpoints are implemented. | Frozen OpenAPI declares only the 3 POST operations. No GET or DELETE endpoints exist in R1-00. | **PROPOSED (Expansion Deferred)** | The absence of GET/DELETE is intentional in R1-00. Adding lifecycle endpoints is a breaking/expansive contract change requiring a new contract version (`v2.1.0-draft` or `v3.0.0`) and owner approvals. Specified below in Section 4 as PROPOSED. |
| 5 | **HTTP 403 Exclusivity** | HTTP 403 is emitted exclusively for authorization / scope denial (`reason: "SCOPE_DENIED"`). Session owner mismatches never emit 403; they emit 404. | Frozen OpenAPI defines 403 as `SCOPE_DENIED`. | **CONFIRMED** | Conforms strictly to frozen contract and DEC-06-01 rule 4. Prevents information leakage regarding session existence. |
| 6 | **HTTP 503 Reuse for Storage Outage** | Storage / Redis failure (e.g. `SessionStorageUnavailableException`) returns HTTP 503 with advisor-local `RoomAdvisorErrorResponse` (`reason: "UPSTREAM_UNAVAILABLE"`, `retryable: true`). | Frozen OpenAPI 503 description states: *"Core API tạm thời không khả dụng"*. Enum value in `ErrorResponse.json` is `"UPSTREAM_UNAVAILABLE"`. | **CONFIRMED** | Governed by **PROJECT OWNER final DEC-06-01**. The enum `UPSTREAM_UNAVAILABLE` is reused for temporary session storage failure. Historical description in OpenAPI remains untouched. Verified in `PublicRoomAdvisorControllerTest` and `RedisRoomAdvisorSessionStoreTest`. |

---

## 4. Deferred Expansion Specification (AC-5.3)

> **CLASSIFICATION: PROPOSED — NOT IMPLEMENTED**
> Adding session management endpoints (`GET` / `DELETE`) is an expansive contract change. It is **NOT** implemented in `T12-R1-Session-Security` and is **NOT** included in the R1-00 frozen baseline. This section formalizes the technical specification for future review under a separate minor/major contract version (e.g., R1-01 / v2.1.0).

### 4.1 Endpoint: GET `/api/v1/public/room-advisor/session/{sessionId}`
- **Purpose:** Allow clients to query session metadata, current version, active criteria, and TTL status without executing a chat or recommendation search.
- **HTTP Method:** `GET`
- **Path Parameter:** `sessionId` (string, UUID format, required)
- **Security & Authorization:**
  - Authenticated user: verified JWT `sub` must match session owner.
  - Anonymous user: `t12_session` cookie must be present, validly signed, and match `sessionId`.
- **Response Format:**
  - `200 OK`: Shared `ApiResponse<RoomAdvisorSessionMetadata>`
    ```json
    {
      "code": 200,
      "message": "Session retrieved successfully",
      "result": {
        "sessionId": "4b8b8a0c-a5ae-46f1-8840-73191d1d046f",
        "sessionVersion": 3,
        "createdAt": "2026-10-21T10:00:00Z",
        "updatedAt": "2026-10-21T10:15:00Z",
        "remainingTtlSeconds": 1200,
        "criteria": {
          "locationKeyword": "Da Nang",
          "checkInDate": "2026-11-20",
          "checkOutDate": "2026-11-22",
          "adults": 2,
          "children": 0,
          "roomsCount": 1
        }
      }
    }
    ```
  - `404 Not Found`: Indistinguishable `RoomAdvisorErrorResponse` (`reason: "SESSION_NOT_FOUND"`) for absent, expired, or foreign-owner sessions.
  - `429 Too Many Requests`: `RoomAdvisorErrorResponse` (`reason: "RATE_LIMITED"`).
  - `503 Service Unavailable`: `RoomAdvisorErrorResponse` (`reason: "UPSTREAM_UNAVAILABLE"`).
- **Invariants:**
  - `x-business-write: false` (strictly read-only).
  - Does NOT return raw chat message history (preserves VULN-04 boundary).

### 4.2 Endpoint: DELETE `/api/v1/public/room-advisor/session/{sessionId}`
- **Purpose:** Explicit client-side session termination (e.g., user clicking "Start New Conversation" or logging out).
- **HTTP Method:** `DELETE`
- **Path Parameter:** `sessionId` (string, UUID format, required)
- **Security & Authorization:**
  - Strict ownership match via JWT `sub` or signed `t12_session` cookie.
- **Response Format:**
  - `200 OK`: Shared `ApiResponse<Void>`
    ```json
    {
      "code": 200,
      "message": "Session terminated successfully",
      "result": null
    }
    ```
  - Cookie Header on 200: `Set-Cookie: t12_session=; Path=/api/v1/public/room-advisor; Max-Age=0; HttpOnly; Secure; SameSite=Lax` (clears cookie).
  - `404 Not Found`: Indistinguishable `RoomAdvisorErrorResponse` (`reason: "SESSION_NOT_FOUND"`) for absent/expired/foreign owner.
- **Invariants:**
  - `x-business-write: false` (strictly removes advisor session key; zero impact on core bookings, holds, or customer accounts).

### 4.3 Governance & Validator Arrangements for Future Expansion
- If approved in a future cycle, these endpoints must be introduced in a new contract directory (e.g., `docs/t12/r1-01/v2.1.0-draft.1/`).
- A dedicated or version-branched validator script (e.g., `validate_r1_01_contracts.py`) must be created; `validate_r1_contracts.py` remains pinned to R1-00.

---

## 5. Domain-Owner Condition Reconciliation (AC-5.6)

### 5.1 Member 2 (Security & Infrastructure) Conditions
1. **Zero Shared Architecture Modifications:**
   - No modifications to `SecurityConfig.java`, shared `pom.xml`, `docker-compose.yml`, or Booking Core modules.
   - All security components reside exclusively in `vn.edu.utc.hotel_booking.modules.ai.advisor.security.*` and `error.*`.
2. **Owner Identity & Zero-Trust Headers:**
   - Identity resolved strictly from validated Spring Security JWT `sub` (authenticated) or HMAC-signed `t12_session` cookie (anonymous).
   - `X-Session-Owner` and `X-Internal-Gateway-Key` are completely ignored and stripped of trust.
3. **Session TTL & Renewal Lifecycle:**
   - Session TTL is strictly 1800 seconds (30 minutes).
   - Proactive rolling renewal occurs only when remaining TTL is $\le 900$ seconds.
   - Anonymous session UUID is preserved across renewal (`UUID.exp.HMAC`).
   - Cookie `Max-Age` equals remaining seconds until expiry (`exp - now`).
4. **Cryptographic Secret Lifecycle:**
   - Secret key requires $\ge 32$ bytes (256 bits) of high-entropy CSPRNG data.
   - Application startup validates secret length and fails fast on invalid configuration.
   - Primary and secondary secret configuration supports zero-downtime key rotation.
5. **Redis Namespace & Concurrency:**
   - Redis keyspace is isolated to `advisor:session:{sessionId}`. Prefix `advisor:lock:*` is reserved.
   - Concurrency uses native optimistic locking: `WATCH` $\rightarrow$ `MULTI` $\rightarrow$ `SET` $\rightarrow$ `EXEC`. No external distributed locks or custom Lua scripts.
   - Integration testing uses dedicated test keys and safe scoped cleanup (`SCAN MATCH advisor:*` $\rightarrow$ `DEL`), verified with sentinel key survival. No `FLUSHDB`, `FLUSHALL`, or `KEYS *`.

### 5.2 Member 4 (Frontend / Booking Portal) Conditions & Consumer Checklist
1. **Frontend Isolation:**
   - The frontend (`frontend/**`) has not been touched in this feature.
   - The public API continues to expose exactly 3 POST endpoints: `/chat`, `/search`, `/refresh`.
2. **Consumer Action Checklist for Member 4:**
   - [ ] **HTTP 404 Recovery:** If any advisor endpoint returns 404, clear any stored advisor `sessionId` from client state or localStorage. Omit `sessionId` on the next `/chat` or `/search` call to obtain a fresh session. Display a generic expiration message. Never treat 404 as an authentication failure or prompt for re-login.
   - [ ] **HTTP 409 Conflict Recovery:** If an endpoint returns 409 (`SESSION_VERSION_CONFLICT`), disable the send button in flight, invoke `POST /api/v1/public/room-advisor/refresh` to obtain the latest `sessionVersion`, or omit `expectedSessionVersion` to overwrite.
   - [ ] **HTTP 429 Rate Limit Cooldown:** If 429 is received, disable request submission. If `Retry-After` header is present and readable, use its integer value as the countdown timer (in seconds). If unreadable due to CORS, fall back to a default cooldown (e.g., 5–10 seconds).
   - [ ] **Dedicated API Client:** Configure an advisor-specific Axios/fetch instance with `withCredentials: true`. Do not add custom auth headers to advisor requests.
   - [ ] **Localhost QA Setup:** Test against `http://localhost:5173` (FE) and `http://localhost:8080` (BE). The `t12_session` cookie is issued with `SameSite=Lax; Secure; HttpOnly`. Modern browsers treat `localhost` as a secure context for `Secure` cookies.

### 5.3 PROPOSED Owner Request to Member 2 (CORS Exposure)
- **Current State:** `SecurityConfig.java` defines CORS configuration but does not include `Access-Control-Expose-Headers: Retry-After`.
- **Limitation:** Cross-origin browser requests (e.g. from `http://localhost:5173` to `http://localhost:8080`) cannot read the `Retry-After` header via JavaScript `response.headers.get('Retry-After')`.
- **Proposed Action (PROPOSED — Outside Feature Scope):**
  - Member 1 formally requests that Member 2 consider adding `Retry-After` to `exposedHeaders` in `SecurityConfig.java` during a future shared configuration update:
    ```java
    // PROPOSED request for Member 2 review (NOT modified in this feature):
    configuration.setExposedHeaders(List.of("Authorization", "Link", "X-Total-Count", "Retry-After"));
    ```
  - This is documented as a proposal for Member 2 approval; no shared file has been modified by Member 1.

---

## 6. PROJECT OWNER / FINAL AUTHORITY Evidence (DEC-06-01)

As established on 2026-10-19 and recorded in Revision 8 §13.7, the Project Owner exercised final authority to resolve all cross-member ambiguities:

1. **Success Envelope:** All successful advisor responses use shared `ApiResponse<T>` (`code: number`, `message: string`, `result: T`).
2. **Error Envelope:** All error responses use advisor-local `RoomAdvisorErrorResponse` conforming strictly to frozen `ErrorResponse.json`:
   - Top-level: integer HTTP `code`, generic `message`, nested `error`.
   - Nested `error`: `contractVersion: "2.0"`, UUID `requestId`, enum `reason`, boolean `retryable`.
3. **404 Non-Disclosure:** Absent, expired, and foreign-owner sessions return identical 404 structures (`code: 404`, `reason: "SESSION_NOT_FOUND"`, `retryable: false`). Only the UUID `requestId` varies. Full-response byte-identity is superseded by semantic field equivalence.
4. **Status-to-Reason Mapping:**
   - `400` $\rightarrow$ `INVALID_REQUEST` (`retryable: false`)
   - `403` $\rightarrow$ `SCOPE_DENIED` (`retryable: false`)
   - `404` $\rightarrow$ `SESSION_NOT_FOUND` (`retryable: false`)
   - `409` $\rightarrow$ `SESSION_VERSION_CONFLICT` (`retryable: true`)
   - `429` $\rightarrow$ `RATE_LIMITED` (`retryable: true`, with positive `Retry-After`)
   - `503` $\rightarrow$ `UPSTREAM_UNAVAILABLE` (`retryable: true`)
5. **Module-Local DTO Authorization:** A module-local DTO at `vn.edu.utc.hotel_booking.modules.ai.advisor.error.RoomAdvisorErrorResponse` is authorized without modifying shared Core classes.

---

## 7. Canonical Six-Task Mapping & Traceability

The canonical six tasks defined in Revision 8 and the NDT workflow are preserved without omission or alteration:

| Canonical Task ID | Name | Current Status | Primary Ownership Boundary | Verification Gates |
|---|---|---|---|---|
| **TASK-001** | Core Token & Principal Security Infrastructure | `TASK_PASSED` (H027) | `modules/ai/advisor/security/**` | Unit & properties suites, fail-fast, HMAC validation |
| **TASK-002** | Controller Hardening, DTOs & Session Validation | `TASK_PASSED` (H029) | `PublicRoomAdvisorController.java`, `error/**` | Controller suites, 404 equality, 3 POST preservation |
| **TASK-003** | In-Memory Sliding-Window Rate Limiting | `TASK_PASSED` (H031) | `modules/ai/advisor/security/**` | Sliding-window tests, 429 envelope, `Retry-After` |
| **TASK-004** | Redis Atomic Session Store & Concurrency | `TASK_PASSED` (H034) | `modules/ai/advisor/session/**` | Unit suite, wiring suite, **AC-4.6 Real-Redis test container** |
| **TASK-005** | Contract Reconciliation & Deferred-Expansion Proposal | `TASK_READY_FOR_REVIEW` (H035) | `docs/t12/proposed/**` | Frozen contract validator, gap inventory, AC-5.1..5.6 |
| **TASK-006** | Full-Feature Security, Concurrency & Zero-Write Regression | `TASK_BLOCKED` | `backend/src/test/.../advisor/**` | Full security integration test, zero-write scan, full regression |

---

## 8. Outstanding Sign-Offs & Feature Gates

Before `T12-R1-Session-Security` can be merged or deployed, the following gates remain:
1. **GATE-01 (Technical Plan Approval):** PASSED (Revision 8 accepted).
2. **GATE-02 (Member 2 Security & Infrastructure Approval):** CONDITIONAL APPROVED. Final live integration sign-off and operational secret provisioning remain pending.
3. **GATE-03 (Member 4 Frontend Acceptance):** CONDITIONAL APPROVED. Frontend integration, browser cookie verification, and UI cooldown handling remain Member 4 deliverables.
4. **TASK-005 Review:** Awaiting independent review by REVIEW agent (`TASK_REVIEW — TASK-005`).
5. **TASK-006 Execution & Review:** Awaiting unblocking after TASK-005 passes.
6. **FULL_FEATURE_REVIEW:** Mandatory independent review after all six tasks pass.
7. **Git Delivery Authorization:** Human authorization required for commit, push, PR creation, and branch merge.
