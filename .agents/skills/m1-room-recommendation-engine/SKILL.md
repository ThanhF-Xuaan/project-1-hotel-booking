---
name: m1-room-recommendation-engine
description: Design, implement, or review T12 deterministic Java room candidate discovery, hard filtering, authoritative read-only availability/pricing checks, stable ranking, and verified recommendation cards. Use for Release 1 search/ranking, not for LLM prompts, RAG ingestion, UI, or booking writes.
---

# M1 Room Recommendation Engine

Make Java—not the LLM—the authority for which room options are eligible and how they are ranked.

## Required inputs

- Validated typed criteria, including source/ambiguity metadata.
- Member 2-approved public hotel and `HotelRoomType` discovery/detail, Availability, and Pricing contracts.
- Confirmed date-range, capacity, currency, VAT/fee, and quote-freshness semantics.
- Member 4-approved card/detail navigation contract.

Use synthetic contract fixtures while approvals are pending. Never present fixture values as production results.

## Process

1. **Validate:** check-in/check-out, adults/children, `roomsCount`, hotel/location scope, budget basis/currency, and required versus preferred features. For `roomsCount > 1`, clarify or redirect; do not allocate or multiply quotes.
2. **Discover candidates:** query bounded public/active hotel room types from an approved contract. Do not use admin endpoints or physical `RoomInstance` data as a shortcut.
3. **Apply hard filters in Java:** public eligibility, scope, one-room capacity, required features, and any other confirmed constraints. Unknown required facts fail closed.
4. **Check the full stay:** translate checkout into the exact Availability date semantics confirmed by Member 2 and require one available room for every occupied night. Never call hold/release.
5. **Obtain authoritative quotes:** call approved Pricing for each bounded candidate or an approved batch operation. Preserve returned amount/currency/tax/fee/basis. Do not recreate Pricing logic. Apply a hard budget only when semantics are comparable.
6. **Rank deterministically:** compute documented scores from verified preferred attributes, then apply stable tie-breakers. Record bounded candidate/fan-out behavior and disclose truncated coverage.
7. **Build cards:** return up to five distinct verified `HotelRoomType` options. Target 3–5 when available; return 0–2 when only that many qualify. Include verified identifiers/facts, quote basis, snapshot/warnings, match reasons, and only an approved view-details action.
8. **Classify outcomes:** authoritative zero results are `NO_MATCH`; Core/model/timeouts or incomplete coverage are `TEMP_UNAVAILABLE` or explicit partial results. Never loosen hard filters silently.

## Deliverables

- Criteria validator, bounded candidate adapter, hard-filter pipeline, Availability/Pricing read adapters, stable ranker, and card mapper inside Member 1's authorized module.
- Synthetic fixtures for pending contracts.
- Unit, contract, integration, and no-write tests.

## Verification

Cover invalid/ambiguous criteria, corrections, capacity, required/preferred features, every occupied night, budget per-night versus stay, currency/fee uncertainty, deterministic ties, duplicates, fewer-than-three matches, stale snapshots, partial failures, rate limits, and IDs/navigation that resolve under approved contracts.

Mocks/spies or architecture rules must assert zero calls to hold/release/booking/payment/guest writes. Do not claim integration PASS until real owner-approved contracts are exercised.

## Stop conditions

Stop integration when public room-type discovery, Availability date semantics, Pricing currency/fee semantics, tenant/public eligibility, or detail navigation is not approved. Submit/update the contract request; do not implement another member's missing contract.
