# Member 1 Agent System and Roadmap

> **Status:** Member 1 documentation draft synchronized for the redesigned T12. Shared contracts and rules remain pending Member 2/4 approval. See [`T12-room-advisor-ADR.md`](T12-room-advisor-ADR.md) and [`T12-contract-requests.md`](T12-contract-requests.md).

## Historical allocation preserved

The original four-fresher plan assigned six Member 1 tasks and estimated 18.0 days:

| Stage | Task | Historical estimate |
|---|---|---:|
| MVP | T04 Utility & Master Data BE | 2.5 days |
| MVP | T05 Lodging & Payroll BE | 2.5 days |
| MVP | T12 RAG Chatbot AI Core | 4.0 days |
| Improvement 1 | U13 Passport/CCCD OCR | 3.0 days |
| Improvement 1 | U14 Guest Personalization | 3.0 days |
| Improvement 2 | U22 Predictive & Anomaly | 3.0 days |

The allocation is retained as historical planning context, not rewritten as a current commitment. The old four-day T12 Q&A estimate predates deterministic room discovery/cards, Core contract work, session/security, and two release gates. A revised estimate must follow contract approval and implementation planning; this document does not silently change the original team allocation.

## Operating model

Use a risk-based workflow:

1. Read-only preflight: repository/SHA/status, owner, current source/contracts/tests.
2. Specification/ADR and contract requests with explicit decision status.
3. Approved plan with small slices, file allowlist, fixtures, verification and rollback.
4. Authorized implementation only within Member 1 scope.
5. Deterministic tests, approved AI evaluation, privacy/security/no-write evidence.
6. Human handoff; Member 2 coordinates shared merges.

The always-on rules stay compact. Load only relevant skills from `.agents/skills/`:

| Need | Skill |
|---|---|
| Spec, ADR, contract requests | `m1-spec-designer` |
| Safe implementation slicing | `m1-execution-planner` |
| Spring AI/Gemini conversation orchestration | `m1-ai-rag-developer` |
| Java discovery/filtering/Pricing/Availability/ranking | `m1-room-recommendation-engine` |
| Release 2 corpus/embedding/pgvector | `m1-rag-knowledge-lifecycle` |
| AI quality/security/no-write eval | `m1-ai-evaluation-security` |
| T04/T05/U13/U14/U22 data pipelines | `m1-data-processing-developer` |
| Final verification/Git handoff | `m1-quality-and-git-delivery` |

## Task routes

| Task | Primary evidence gate | Start condition and owner dependency |
|---|---|---|
| T04 | CRUD/domain tests plus actual API/build checks | Approved shared entity/schema/API contracts from Member 2 |
| T05 payroll | Parser/import tests, bounded resource use and sanitized errors | Approved file schema, transaction/idempotency and privacy rules |
| T05 lodging | Export conformance and PII/security review | Approved official export format and retention/completion semantics |
| T12 Release 1 | Deterministic recommendation/security eval | Member 2 read contracts/session decisions, Member 4 card/detail contract, approved eval |
| T12 Release 2 | Scoped retrieval/groundedness/lifecycle eval | Approved corpus, pgvector/migration/provider compatibility and human eval |
| U13 | Field accuracy/confidence/manual-review eval | Approved provider/privacy/retention and synthetic/licensed images |
| U14 | Offline baseline and privacy/fairness checks | Authorized history/features; no Pricing mutation |
| U22 | Leakage-free time backtest versus baseline | Approved data source and reporting contract |

## T12 roadmap — superseding the old S1–S6 flow

The old roadmap described `S5 Tools ghi` for room hold/booking after confirmation. That advice is **SUPERSEDED**. T12 has zero business write capability under all circumstances. The old RAG-first order is also superseded: RAG is Release 2 and must not gate room recommendations.

### Release 1 — Room Recommendation Core

| Gate | Outcome | Owner/dependency |
|---|---|---|
| R1.0 Contract and eval | ADR, contract requests, synthetic fixtures, draft eval reviewed | M1; M2/M4/human approvals |
| R1.1 AI boundary | Spring AI/Gemini compatibility, typed criteria and safe response statuses | M1; dependency/provider approval |
| R1.2 Candidate discovery | Bounded public hotels and `HotelRoomType` candidates | M2 public-safe contract |
| R1.3 Hard filters and live reads | Capacity/required features, every occupied night, authoritative quote | M2 Availability/Pricing semantics |
| R1.4 Deterministic rank/cards | Stable ranking, 3–5 when available or fewer, one room per option | M1; M4 card/detail contract |
| R1.5 Session/resilience/security | Owner-bound minimal state, rate limits, partial/outage semantics, no-write trace | M2 security/session review |
| R1.6 Release evidence | Deterministic tests, adversarial eval, PII/log audit, relevant regression | Human evaluation/security review |

Release 1 is not implementation-ready while the `BLOCKED` requests in [`T12-contract-requests.md`](T12-contract-requests.md) remain unresolved. Synthetic fixtures may unblock Member 1 development after plan approval, but production integration cannot claim completion.

### Release 2 — Scoped RAG Knowledge Assistant

| Gate | Outcome | Owner/dependency |
|---|---|---|
| R2.0 Corpus governance | Approved public sources, scope, effective/version rules | M1 corpus owner + security/privacy review |
| R2.1 Compatibility PoC | Supported Gemini embeddings, observed dimension/cardinality, Spring AI and pgvector compatibility | M1/M2 approval; no assumed model ID |
| R2.2 Storage lifecycle | Member 2-approved Liquibase/pgvector, staged activation, rollback and precise deletion | M2 DB/schema owner |
| R2.3 Scoped retrieval | Trusted hotel/chain filters, public/active/effective content and bounded retrieval | M1; tenant tests |
| R2.4 Grounded answers | Sources, uncertainty/conflict behavior, malicious-document defense | M1; approved eval |
| R2.5 Independent degradation | RAG-off/outage does not disable Release 1 recommendations | M1 integration evidence |

## T12 product and evidence gates

- Gemini proposes typed criteria and may explain verified results. Java validates, discovers, filters, checks Availability/Pricing, ranks, and constructs cards.
- Return 3–5 distinct verified `HotelRoomType` cards when available, otherwise fewer. No fake cards or silently relaxed required features.
- Each MVP option is one room. `roomsCount > 1` is clarified/redirected; no quote multiplication or multi-room allocation.
- Dynamic availability, price, currency, tax and fees are authoritative Core facts. RAG/model output is never authoritative.
- Cards use only a Member 4-approved room-detail action and verified IDs. Clicking does not hold or book.
- `NO_MATCH` is not an outage. Use `TEMP_UNAVAILABLE` or partial warnings for incomplete/dependency failure.
- Security tests assert zero hold/release/booking/payment/guest writes at dependency/tool/network trace level.
- AI evaluation uses synthetic data and an approved gold version. Draft thresholds and unrun cases are not pass evidence.

## Other task guardrails preserved

- T04/T05 remain deterministic backend/data work and retain their existing business purpose; T12 rules do not change them.
- T05 lodging export waits for the approved actual format; no invented government schema or retention period.
- U13 uses human verification for low-confidence identity extraction and no real identity fixtures in agent sessions.
- U14 remains a nonbinding recommendation experiment and cannot alter prices/promotions.
- U22 uses time-based backtesting, simple baselines, uncertainty and labeled synthetic data.

## Git and review

- Preserve all dirty/untracked work. For a newly approved T12 implementation, use an isolated worktree from a freshly verified `origin/develop`; do not rebase/cherry-pick/merge the legacy T12 branch wholesale.
- Branch/commit/push/PR/merge/migration/paid-model actions each require appropriate authorization. Human peer review is mandatory; AI reviewers supplement it.
- Every handoff reports exact base SHA, changed files/contracts, actual `PASS`/`FAIL`/`NOT RUN` evidence, unverified areas, privacy/security/cost/rollback, approvals, and Git mutation status.

## Pending approvals

- **Member 2:** global `AGENTS.md` and shared `00`/`01`; public room-type discovery; Availability/Pricing/date/money semantics; public eligibility; session/security; Spring AI/shared dependencies; pgvector/Liquibase.
- **Member 4:** card schema, missing/loading/error behavior, room-detail route and revalidation UX.
- **Human evaluation approver:** Room Advisor v2 cases, gold behavior and numeric thresholds.
- **Privacy/security owner:** Gemini/provider terms, session retention, corpus governance, U13/U14/T05 data rules.
