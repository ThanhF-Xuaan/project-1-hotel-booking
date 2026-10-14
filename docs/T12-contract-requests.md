# T12 Contract Requests and Approval Tracker

> **Status:** DRAFT / PENDING OWNER REVIEW. Proposed contracts are not implemented endpoints or approved production behavior. Baseline evidence is from `origin/develop` at `0e6d379eba8ab6ac7cf3573728d92d97f3e659e6`.

## Status vocabulary

- `CONFIRMED`: observed in source at the recorded SHA.
- `PROPOSED`: requested design awaiting owner decision.
- `UNKNOWN`: evidence is insufficient.
- `BLOCKED`: T12 production integration cannot safely proceed without the decision.

## Requests

| ID | Contract / decision | Current evidence | Requested acceptance | Owner | Status |
|---|---|---|---|---|---|
| CR-01 | Public-safe `HotelRoomType` discovery/detail | `PublicBookingSearchController` and `PublicBookingSearchServiceImpl` at `0e6d379` provide `POST /api/v1/public/booking/search` with active room types, capacities, amenities, inclusions, and prices | Bounded public filter/detail with active/public eligibility, hotel/region scope, capacity and public features; no internal room numbers/admin fields | Member 2 | CONFIRMED AT 0e6d379 |
| CR-02 | Availability range and quantity semantics | `PublicBookingSearchServiceImpl` at `0e6d379` queries `roomAvailabilityService.findAvailability(hotelId, hotelRoomTypeId, checkInDate, checkOutDate.minusDays(1))` checking `minAvailable >= request.getRoomsCount()` (checkout-exclusive nights) | Owner confirms recommendation adapter queries check-in through checkout-minus-one-day, completeness for every occupied night, sparse-row semantics, `availableCount`, timezone/snapshot, and one-room quantity | Member 2 | CONFIRMED AT 0e6d379 |
| CR-03 | Pricing money/fee/currency semantics | `TaxAndFeeHandler` at `0e6d379` computes 5% service fee and 10% VAT; `RoomPriceQuoteResponse` returns `currency: "VND"` and itemized `PriceBreakdownDto` | Confirm currency, precision/rounding, VAT/fee inclusion, children/extra-bed/campaign behavior, quote freshness, and budget per-night versus whole-stay comparison | Member 2 | CONFIRMED AT 0e6d379 |
| CR-04 | Public hotel eligibility and location semantics | `BookingSearchRequest` supports `city`, `district`, `hotelId` filtering across active properties | Confirm which status is publicly eligible and how city/region/location search is represented; frontend/user scope must not become authorization | Member 2 | CONFIRMED AT 0e6d379 |
| CR-05 | Recommendation card and room-detail navigation | `/portal/search` exists; action path supports `/portal/search?hotelId=...&hotelRoomTypeId=...` and future `/portal/hotels/{id}/room-types/{id}` | Approve typed card fields and `VIEW_ROOM_DETAILS` route/intent using verified IDs; click revalidates price/availability and never implies hold | Member 4, with Member 2 DTO review | PROPOSED / PENDING APPROVAL |
| CR-06 | Anonymous/authenticated conversation ownership | Redis dependency exists; `SecurityConfig.java` allows `/api/v1/public/**`; no T12/session contract exists on baseline | Approve owner binding, cookie/token behavior, TTL, criteria-only retention, invalidation, CORS/CSRF/rate-limit policy, and deletion; `sessionId` alone is not authorization | Member 2/security owner | BLOCKED / PENDING APPROVAL |
| CR-07 | Spring AI + Gemini dependency/model compatibility | No Spring AI dependency or AI configuration on baseline | Approve compatible Spring AI dependency, supported chat/embedding provider path, configuration/feature flag and approved provider data handling; model IDs remain configuration verified in PoC | Member 2 + Member 1 | PROPOSED / PENDING APPROVAL |
| CR-08 | PostgreSQL/pgvector schema and operations | Baseline Compose uses `postgres:16-alpine`; no pgvector migration on `develop` | Approve extension/image, Liquibase schema/index/grants, vector dimension based on observed model output, backup, non-destructive rollout, reindex and rollback | Member 2/DB owner | BLOCKED for Release 2 |
| CR-09 | Knowledge ingestion authorization | No T12 ingestion contract on baseline | Approve admin authority mapping, tenant/source ownership, public corpus review, staged activation, version rollback and scoped delete; no PII/live business data | Member 2/security + Member 1 corpus owner | BLOCKED for Release 2 |
| CR-10 | Public AI API/status envelope | `SecurityConfig.java` permits `/api/v1/public/**`; T12 endpoints mapped to `/api/v1/public/ai/room-advisor/*` | Review request/response fields and `NEED_INFO`, `RESULTS`, `NO_MATCH`, `TEMP_UNAVAILABLE`, partial warnings; preserve existing `ApiResponse` convention | Member 1 proposal; Member 2 API review; Member 4 consumer review | PROPOSED |

## Prohibited Business Write Services (Zero-Write Invariant)

Under the strict `x-business-write: false` architecture, T12 Advisor must NEVER invoke or depend upon:
- `PublicBookingSessionController` (`/api/v1/public/booking/sessions/*`)
- `BookingSessionService` / `BookingSessionServiceImpl` (`initiateDraftSession`, `cancelSession`, `addServiceToSession`)
- `RoomInventoryHoldService` / `RoomInventoryHoldServiceImpl` (`holdRooms`, `releaseRooms`, `batchHoldInventory`, `batchReleaseInventory`)
- Any booking persistence, folio/payment, or customer identity modification APIs.

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
| CR-01–04 | Member 2 | CONFIRMED IN CODE | 2026-10-11 / 0e6d379 | Implemented via `PublicBookingSearchController` and `PublicBookingSearchServiceImpl` |
| CR-06–09 | Member 2 | PENDING | — | — |
| CR-05, CR-10 frontend consumption | Member 4 | PENDING | — | — |
| CR-07, CR-09 Member 1 implementation choices | Member 1 | PENDING implementation review | — | — |
