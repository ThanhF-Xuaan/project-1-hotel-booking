# T12 Contract Requests and Approval Tracker

> **Status:** DRAFT / PENDING OWNER REVIEW. Proposed contracts are not implemented endpoints or approved production behavior. Baseline evidence is from `origin/develop` at `afbac57ae463ee2dc8705a2b3e9a7a8b5fca85bb`.

## Status vocabulary

- `CONFIRMED`: observed in source at the recorded SHA.
- `PROPOSED`: requested design awaiting owner decision.
- `UNKNOWN`: evidence is insufficient.
- `BLOCKED`: T12 production integration cannot safely proceed without the decision.

## Requests

| ID | Contract / decision | Current evidence | Requested acceptance | Owner | Status |
|---|---|---|---|---|---|
| CR-01 | Public-safe `HotelRoomType` discovery/detail | Admin-style `/api/v1/inventory/hotel-room-types` CRUD/filter exists; no public controller found | Bounded public filter/detail with active/public eligibility, hotel/region scope, capacity and public features; no internal room numbers/admin fields | Member 2 | BLOCKED / PENDING APPROVAL |
| CR-02 | Availability range and quantity semantics | Public search exists; service returns both endpoints inclusive. Holds use checkout-exclusive dates | Owner confirms recommendation adapter queries check-in through checkout-minus-one-day, completeness for every occupied night, sparse-row semantics, `availableCount`, timezone/snapshot, and one-room quantity | Member 2 | BLOCKED / PENDING APPROVAL |
| CR-03 | Pricing money/fee/currency semantics | Public calculate exists; response includes totals, VAT and surcharge; no currency field observed | Confirm currency, precision/rounding, VAT/fee inclusion, children/extra-bed/campaign behavior, quote freshness, and budget per-night versus whole-stay comparison | Member 2 | BLOCKED / PENDING APPROVAL |
| CR-04 | Public hotel eligibility and location semantics | Public hotel filter/detail exists and returns `status`, region and address | Confirm which status is publicly eligible and how city/region/location search is represented; frontend/user scope must not become authorization | Member 2 | PROPOSED / PENDING APPROVAL |
| CR-05 | Recommendation card and room-detail navigation | `/portal/search` and `/portal/checkout` exist; no detail route found | Approve typed card fields and `VIEW_ROOM_DETAILS` route/intent using verified IDs; click revalidates price/availability and never implies hold | Member 4, with Member 2 DTO review | BLOCKED / PENDING APPROVAL |
| CR-06 | Anonymous/authenticated conversation ownership | Redis dependency exists; no T12/session contract exists on baseline | Approve owner binding, cookie/token behavior, TTL, criteria-only retention, invalidation, CORS/CSRF/rate-limit policy, and deletion; `sessionId` alone is not authorization | Member 2/security owner | BLOCKED / PENDING APPROVAL |
| CR-07 | Spring AI + Gemini dependency/model compatibility | No Spring AI dependency or AI configuration on baseline | Approve compatible Spring AI dependency, supported chat/embedding provider path, configuration/feature flag and approved provider data handling; model IDs remain configuration verified in PoC | Member 2 + Member 1 | PROPOSED / PENDING APPROVAL |
| CR-08 | PostgreSQL/pgvector schema and operations | Baseline Compose uses `postgres:16-alpine`; no pgvector migration on `develop` | Approve extension/image, Liquibase schema/index/grants, vector dimension based on observed model output, backup, non-destructive rollout, reindex and rollback | Member 2/DB owner | BLOCKED for Release 2 |
| CR-09 | Knowledge ingestion authorization | No T12 ingestion contract on baseline | Approve admin authority mapping, tenant/source ownership, public corpus review, staged activation, version rollback and scoped delete; no PII/live business data | Member 2/security + Member 1 corpus owner | BLOCKED for Release 2 |
| CR-10 | Public AI API/status envelope | No AI module on baseline | Review request/response fields and `NEED_INFO`, `RESULTS`, `NO_MATCH`, `TEMP_UNAVAILABLE`, partial warnings; preserve existing `ApiResponse` convention and backward compatibility if a consumer exists | Member 1 proposal; Member 2 API review; Member 4 consumer review | PROPOSED |

## Proposed minimum DTO semantics (not an implemented contract)

The response should expose validated criteria, missing fields, typed status/warnings, and `recommendations[]`. Each recommendation represents one room and contains verified `hotelId`, `hotelRoomTypeId`, public names/capacity/features, stay interval, authoritative quote and currency/fee basis, snapshot time, match reasons, and an owner-approved view-details action. It must not contain `holdId`, `bookingId`, `reservationId`, `paymentUrl`, physical room number, internal ranking data, or invented image/route values.

## Contract tests expected after approval

- Public eligibility and tenant/scope enforcement.
- Full-stay Availability mapping and one-room quantity.
- Price amount/currency/VAT/fee/rounding/budget comparisons.
- Stable IDs and card/detail route resolution.
- Session owner isolation, expiry, criteria invalidation and rate limiting.
- RAG tenant/source/version isolation and model-vector compatibility.
- Dependency/tool/network assertions showing zero hold/release/booking/payment/guest writes.

## Owner sign-off record

| Request IDs | Owner | Decision | Date/SHA | Conditions |
|---|---|---|---|---|
| CR-01–04, CR-06–09 | Member 2 | PENDING | — | — |
| CR-05, CR-10 frontend consumption | Member 4 | PENDING | — | — |
| CR-07, CR-09 Member 1 implementation choices | Member 1 | PENDING implementation review | — | — |
