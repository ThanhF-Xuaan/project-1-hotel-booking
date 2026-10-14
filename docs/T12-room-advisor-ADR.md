# T12 AI Room Recommendation Assistant — Draft ADR

> **Status:** DRAFT FOR REVIEW — product direction confirmed by Member 1; shared API/schema/security and frontend navigation decisions remain pending Member 2/4 approval. This document does not authorize implementation or migration.
>
> **Audited baseline:** `origin/develop` at `afbac57ae463ee2dc8705a2b3e9a7a8b5fca85bb` on 2026-10-08.

## Context and verified baseline

| State | Evidence | Finding |
|---|---|---|
| CONFIRMED | `backend/pom.xml` | Java 21 and Spring Boot 4.0.7; no Spring AI dependency on the audited baseline |
| CONFIRMED | `backend/src/main/java/.../modules/ai/` | AI module is absent on the audited baseline |
| CONFIRMED | `PublicHotelController` | Public hotel filter/list/detail operations exist |
| CONFIRMED | `PublicAvailabilityController` and `RoomAvailabilityServiceImpl` | Public availability search exists; `startDate` through `endDate` is inclusive; the same public controller also exposes prohibited hold/release operations that T12 must never depend on |
| CONFIRMED | `PublicPricingController`, `PriceCalculationRequest`, `PriceBreakdownDto`, `PriceEngineImpl` | Public price calculation exists and uses checkout-exclusive nights; response includes final total/VAT/surcharge but no explicit currency field |
| CONFIRMED | `HotelRoomTypeController` | Room-type CRUD/filter is under `/api/v1/inventory/...`; no public-safe room-type discovery contract was found |
| CONFIRMED | `frontend/src/App.tsx` | Public search and checkout routes exist; no public room-detail route was found |
| CONFIRMED | `docker-compose.yml` | Baseline uses `postgres:16-alpine`, not a pgvector image; pgvector installation/migration is not established |
| CONFIRMED | `feature/T12-rag-chatbot` | Legacy branch contains reference implementation/eval material, but it is not merged into the audited baseline and is not the rebuild base |
| UNKNOWN | runtime OpenAPI/integration tests | No approved running environment was started during this documentation task |

## Decision

### Confirmed product decisions

1. T12 is a read-only **AI Room Recommendation Assistant**, not an autonomous booking agent.
2. Use the existing Java/Spring Boot application with **Spring AI + Google Gemini** after dependency/model compatibility is approved and verified.
3. **Release 1 — Room Recommendation Core** is primary and works without RAG:
   - Gemini proposes typed natural-language criteria.
   - Java validates/merges criteria, discovers public hotels and `HotelRoomType` candidates, applies hard filters, queries authoritative read-only Availability/Pricing, ranks deterministically, and builds structured cards.
   - Return 3–5 distinct verified matches when available; return fewer when fewer qualify. Never fabricate results.
   - Each MVP option represents one room. Preserve `roomsCount`, but clarify/redirect requests for more than one room.
4. **Release 2 — Scoped RAG Knowledge Assistant** adds approved public policy/FAQ/amenities/static descriptions using PostgreSQL + pgvector. It is independently degradable and cannot override live Core facts.
5. Cards provide only a Member 4-approved `VIEW_ROOM_DETAILS` action. They do not reserve inventory and contain no booking/payment/hold identifiers.

### Absolute architecture invariant

T12 must have no path to hold/release inventory, create/update/cancel bookings, initiate payment/refund, or mutate guest/business records—even after customer confirmation or role claims. No generic HTTP/SQL tool or write-capable callback may be registered. AI-owned minimal session storage and separately authorized knowledge ingestion do not grant business write access.

## Responsibility split

| Owner | Responsibility |
|---|---|
| Member 1 | AI API/orchestration, typed criteria, Java recommendation/ranking, fixed read-only adapters, RAG lifecycle, AI evaluation and safety evidence |
| Member 2 | Public Catalog/Inventory/Availability/Pricing contracts, date/money semantics, shared DB/Liquibase/pgvector, IAM/security/session ownership |
| Member 3 | Payment/POS/Maintenance unchanged; no T12 integration required |
| Member 4 | Chat widget, typed cards, loading/error UX, room-detail route and click-through revalidation |

## Consequences and trade-offs

- Deterministic Java orchestration is more testable and prevents the model from deciding business eligibility or money, at the cost of explicit adapters and ranking rules.
- Release separation reduces coupling and lets room discovery remain available during RAG outages, at the cost of two independent readiness gates.
- A strict zero-write dependency graph reduces feature scope but materially limits prompt-injection impact.
- Missing public room-type discovery, currency semantics, detail navigation, session ownership, and pgvector approval block production integration; synthetic fixtures may support Member 1 tests but cannot be exposed as real data.

## Rejected or superseded options

- Python/LangChain or another vector service for the T12 MVP.
- Vector-first or mandatory LLM function-calling pipeline.
- Legacy S5 hold/booking tools after user confirmation.
- LLM-generated ranking, IDs, prices, availability, routes, or fallback cards.
- Using `/portal/checkout` as a fabricated detail route.
- Copying/cherry-picking the legacy T12 branch wholesale.

## Acceptance before implementation

- Member 2 resolves the Core, money/date, session/security, pgvector and dependency requests in [`T12-contract-requests.md`](T12-contract-requests.md).
- Member 4 approves the typed card and detail navigation contract.
- Human evaluator approves the versioned Room Advisor eval and thresholds.
- A new implementation worktree/branch is explicitly authorized from a freshly verified `origin/develop` SHA.

## Sign-off

| Area | Owner | Status | Decision/date |
|---|---|---|---|
| Product and Member 1 architecture | Member 1 | CONFIRMED for documentation | User-provided decision, 2026-10-08 |
| Shared API/schema/security | Member 2 | PENDING APPROVAL | — |
| Cards/detail route | Member 4 | PENDING APPROVAL | — |
| Eval cases/thresholds | Human evaluation approver | PENDING APPROVAL | — |
