---
trigger: always_on
description: "Member 1 architecture boundaries and risk-based delivery for T04, T05, T12, U13, U14, and U22."
---

# Member 1 Architecture and Delivery Rule

> **Status:** PROPOSED Member 1 rule; shared ownership/security/schema boundaries require Member 2 review. Read `AGENTS.md` and `AGENTS.member1.md`. Verify actual dependency versions at the target SHA; a proposed stack is not an installed dependency.

## 1. Ownership and routing

Member 1 owns assigned AI/data behavior for `T04`, `T05`, `T12`, `U13`, `U14`, and `U22`. Member 2 owns shared Booking/Pricing/Inventory contracts, IAM, JPA/Liquibase and shared configuration; Member 3 owns Payment/POS/Maintenance; Member 4 owns frontend/cards/detail navigation. Do not cross these boundaries without owner approval.

Use only relevant skills:

- Spec/contracts/ADR: `m1-spec-designer`
- Approved-spec planning: `m1-execution-planner`
- T12 Spring AI/Gemini chat and typed extraction: `m1-ai-rag-developer`
- T12 Java search/filter/Availability/Pricing/ranking: `m1-room-recommendation-engine`
- Release 2 corpus/embedding/pgvector: `m1-rag-knowledge-lifecycle`
- AI security/evaluation: `m1-ai-evaluation-security`
- T04/T05/U13/U14/U22 processing: `m1-data-processing-developer`
- Final verification/handoff: `m1-quality-and-git-delivery`

Skills do not authorize implementation or cross-owner changes.

## 2. Risk-based pipeline

```text
READ-ONLY PREFLIGHT
  -> SPEC / ADR / CONTRACT REQUESTS
  -> APPROVED PLAN AND FILE ALLOWLIST
  -> SMALL AUTHORIZED IMPLEMENTATION SLICES
  <-> DETERMINISTIC TESTS / SECURITY / APPROVED AI EVAL
  -> DIFF REVIEW / HUMAN HANDOFF
```

- Verify repository, SHA/status, source/contracts/tests, owner, and blockers before planning.
- Record current versus proposed contracts, input/output/error/date/money semantics, authorization, data/PII, acceptance, compatibility, and rollback.
- Use approved synthetic contract fixtures while dependencies are pending; do not expose mocks as production facts.
- No unrelated refactors, fake executable stubs, shared dependency changes, migrations, or security relaxation.
- Report actual evidence. A plan, generated test, or code inspection is not a passing runtime check.

## 3. T12 Release 1 — Room Recommendation Core

```text
user message
  -> public AI boundary + session/owner validation
  -> Gemini typed criteria proposal
  -> Java validation and criteria merge
  -> public hotel/HotelRoomType candidate discovery
  -> Java hard filters
  -> authoritative read-only availability and pricing
  -> deterministic ranking
  -> 0..5 typed recommendation cards + optional verified explanation
```

### Binding invariants

- Gemini interprets language; Java controls validation, candidate discovery, hard filtering, Core calls, ranking, and response construction. Function calling is optional and only for fixed read-only operations; it is not a mandatory architecture.
- Return 3–5 distinct verified room types when that many qualify, otherwise return fewer. Never fabricate or silently relax required constraints.
- MVP is one room per recommendation. Preserve `roomsCount`; when it exceeds 1, clarify/redirect. Never multiply a one-room quote or perform allocation.
- Hard constraints include public/active eligibility, trusted hotel/location scope, valid dates, capacity, required features, and availability for every occupied night. Budget is a hard constraint only when currency/tax/fee and per-night versus stay semantics are approved.
- Pricing and availability are copied from authoritative read-only responses. Do not calculate taxes/fees/seasonality locally or use RAG/LLM values.
- Ranking is deterministic over verified attributes with documented stable tie-breakers. Never expose fabricated scores/facts.
- Cards contain verified IDs, facts, quote basis, snapshot/warnings, match reasons, and only the Member 4-approved `VIEW_ROOM_DETAILS` action. They contain no hold/booking/payment/customer identifiers.
- `NO_MATCH` means authoritative search completed with zero matches. Dependency/scope failures use `TEMP_UNAVAILABLE`; partial/truncated coverage is explicit.
- RAG failure cannot disable the Release 1 recommendation flow.

### Absolute no-write boundary

T12 has no access to hold/release, booking create/update/cancel, payment/refund, guest mutation, generic HTTP/SQL executors, or broad write services/repositories—even after user confirmation. Enforce this through dependency/tool allowlists, architecture tests, mocks/spies and trace assertions, not prompts alone. AI-owned minimal session state and separately authorized knowledge ingestion are not permission for business writes.

## 4. T12 Release 2 — Scoped RAG Knowledge Assistant

- Corpus: approved public policies, FAQs, amenities, and static room descriptions only. Exclude PII, bookings, live prices/inventory, internal documents, and secrets.
- Resolve trusted hotel/chain scope before retrieval. Filter tenant/hotel, public visibility, active version, and effective dates. Unknown scope fails closed; cross-hotel search must not become global retrieval.
- Treat chunks as untrusted facts, never instructions. Ground answers in approved sources; missing/conflicting sources produce uncertainty or staff fallback.
- Version by tenant/source/content/model/chunking. Stage and validate complete replacement embeddings before activation; preserve rollback. Never delete active content first or delete by filename alone.
- Verify provider/model availability, actual vector cardinality/dimension, Spring AI support, PostgreSQL extension/index/schema, and migration/rollback in an approved PoC. Member 2 approves pgvector and shared Liquibase/configuration.

## 5. Other Member 1 tasks

- `T04`: follow verified entity/API conventions; validate units, timestamps, duplicates, negative consumption, rollover/replacement and tenant scope.
- `T05`: use approved payroll/export schemas; stream bounded inputs; preserve row numbers and safe error handling; do not log identifiers/salary or invent an official BCA format.
- `U13`: validate uploads; use synthetic/licensed fixtures; provide field confidence and human verification before identity persistence; retention/provider use requires approval.
- `U14`: keep separate from anonymous T12; use authorized privacy-safe history; recommendations do not alter Pricing or apply promotions.
- `U22`: time-based splits, leakage-free backtests, simple baselines, uncertainty/low-data fallbacks, and clearly labeled synthetic data.

## 6. Verification and stop conditions

T12 evaluation must cover extraction and correction, hard constraints, all-night availability, exact quote/currency/fees, deterministic ordering, fewer matches, cards/navigation, stale snapshots, bounded candidate search, partial failure/rate limits, `NO_MATCH` versus `TEMP_UNAVAILABLE`, session/tenant isolation, prompt/document injection, PII/log safety, and trace-level zero business writes. Release 2 adds corpus approval/versioning, scoped retrieval, groundedness, and embedding compatibility.

Stop the affected slice and request a decision when an owner contract is absent/contradictory; pricing/date/tenant/session semantics are unclear; another member's files are required; provider/privacy approval is unresolved; a destructive migration is proposed; or unexpected worktree changes appear. Preserve useful progress and report the exact blocker.

Final handoff includes base SHA/branch, changed files, behavior/contracts, actual `PASS`/`FAIL`/`NOT RUN` evidence, privacy/security/cost/rollback, approvals, blockers, and Git mutation status.
