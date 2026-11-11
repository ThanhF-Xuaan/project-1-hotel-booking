# T12 Contract Request: Unified Conversational Flow (Converse Endpoint)

> **Document ID:** CR-T12-20261109-01  
> **Status:** PROPOSED (Member 1)  
> **Target Reviewers:** Member 2 (Core/IAM/API Architecture), Member 4 (Frontend/Widget)  
> **Base SHA:** `origin/develop` @ `08ff40c3`  
> **Scope:** Additive endpoint `POST /api/v1/public/ai/room-advisor/converse` under feature flag `hotel.ai.advisor.unified.enabled`.

---

## 1. Context & Motivation

Currently, the booking portal widget exposes two independent tabs:
1. **Room Advisor (R1):** Discovers room candidates and provides authoritative availability/pricing via Core.
2. **Policy Knowledge (R2):** Grounded RAG FAQ/policy lookup for hotels.

Customers frequently combine search criteria and policy inquiries in natural conversation (e.g., *"Find a double room in Da Nang this weekend for 2 adults and 1 child 4 years old, is the child eligible for free breakfast buffet?"*).

This contract request proposes an **additive** conversational endpoint allowing customers to submit combined inquiries, receiving authoritative room cards alongside grounded policy citations without tab-switching.

---

## 2. Invariants & Backward Compatibility

- **Absolute Zero Business Write:** The `/converse` endpoint has zero capability to hold/reserve inventory, mutate bookings, initiate payments, or alter customer records.
- **R1 Contract Frozen:** The existing `/chat`, `/search`, and `/refresh` endpoints and schema (`advisor.schema.json`) remain 100% frozen and unmodified. `PublicRoomAdvisorController.java` remains untouched.
- **Isolated Controller:** All unified logic is exposed through `[NEW] PublicUnifiedAdvisorController.java` gated by `@ConditionalOnProperty(name = "hotel.ai.advisor.unified.enabled", havingValue = "true")`.
- **Independent Degradation:** Failure or rate-limiting in RAG does not impair Core room search results, and Core failures do not suppress policy lookup if scope is verified.

---

## 3. API Contract Specification

### Endpoint
`POST /api/v1/public/ai/room-advisor/converse`

### Request Headers
- `Content-Type: application/json`
- `Cookie: HOTEL_ADVISOR_SESSION=<token>` (Optional, minted/renewed identically to R1)

### Request Body (`UnifiedAdvisorRequestDto`)
```json
{
  "message": "Tìm phòng đôi ở Đà Nẵng cuối tuần này cho 2 người lớn và 1 bé 4 tuổi, bé có được miễn phí buffet sáng không?",
  "sessionId": "4b68e715-6db5-48d6-9533-5b8cb46f483c",
  "expectedSessionVersion": 1,
  "policyHotelId": 2,
  "compareRoomTypeIds": [101, 102]
}
```
- `message`: String, required, 1..500 characters.
- `sessionId`: UUID, optional.
- `expectedSessionVersion`: Long, optional.
- `policyHotelId`: Integer, optional (1..32767). Used when clicking hotel choice chips to explicitly bind policy scope.
- `compareRoomTypeIds`: Array of Integer, optional (min 2, max 3 items, positive values). Used when comparing specific room types directly.

### Response Body (`ApiResponse<UnifiedAdvisorResultDto>`)
```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "contractVersion": "2.1-proposed",
    "requestId": "7d91e324-4f81-49b8-a612-4a0058b8ef42",
    "intent": "SEARCH_AND_POLICY",
    "advisor": {
      "contractVersion": "2.0",
      "requestId": "7d91e324-4f81-49b8-a612-4a0058b8ef42",
      "sessionId": "4b68e715-6db5-48d6-9533-5b8cb46f483c",
      "sessionVersion": 2,
      "status": "RESULTS",
      "reply": "Tôi đã tìm thấy 1 hạng phòng phù hợp với yêu cầu của bạn tại Đà Nẵng.",
      "recommendations": [ ... ],
      "disclaimers": [ ... ]
    },
    "knowledge": {
      "state": "ANSWERED",
      "hotelId": 2,
      "hotelName": "Khách sạn Đà Nẵng Heritage",
      "policyQuestion": "bé có được miễn phí buffet sáng không?",
      "result": {
        "status": "GROUNDED_ANSWER",
        "answer": "Trẻ em dưới 6 tuổi đi cùng bố mẹ được miễn phí buffet sáng tiêu chuẩn tại nhà hàng khách sạn.",
        "citations": [ ... ]
      },
      "hotelChoices": null,
      "retryAfterSeconds": null
    },
    "comparison": {
      "status": "READY",
      "focus": "FAMILY_KIDS",
      "items": [
        {
          "recommendationId": "rec-1",
          "hotelRoomTypeId": 101,
          "hotelId": 2,
          "hotelName": "Viettel Grand Đà Nẵng",
          "roomTypeCode": "DLX",
          "roomTypeName": "Deluxe Room",
          "quoteId": "quote-rec-1",
          "observedAt": "2026-11-11T12:00:00Z",
          "finalTotalAmount": "3200000",
          "perNightAverageAmount": "1600000",
          "currency": "VND",
          "totalNights": 2,
          "chargeCoverage": "ALL_MANDATORY_CHARGES",
          "serviceFeeStatus": "INCLUDED",
          "budgetCheck": "PASS",
          "priceDeltaFromCheapest": "0",
          "priceDiffPercentFromCheapest": 0.0,
          "maxAdults": 2,
          "maxChildren": 1,
          "maxTotalGuests": 3,
          "extraBedsAllowed": 1,
          "staticAreaAndLayout": "Diện tích 40m², ban công hướng biển",
          "uniqueFeatureNames": ["Ban công hướng biển"],
          "bestForBullets": ["Phương án tối ưu chi phí nhất trong nhóm đang so sánh"],
          "action": {
            "type": "VIEW_ROOM_DETAILS",
            "path": "/hotels/2/rooms/101"
          }
        }
      ],
      "sharedFeatureNames": ["Wi-Fi tốc độ cao", "Smart TV"],
      "maxSavingsAmount": "1800000",
      "maxSavingsPercent": 36.0,
      "tradeOffSummary": "Nếu gia đình có con nhỏ và ưu tiên sự thoải mái...",
      "staticDescriptionCitations": [ ... ]
    }
  }
}
```

---

## 4. Rollback & Safety Gates

- Default configuration: `hotel.ai.advisor.unified.enabled: false`. When disabled, Spring Boot does not register `PublicUnifiedAdvisorController`, returning standard `404 Not Found`.
- Frontend feature flag: `VITE_ADVISOR_UNIFIED=false`. When disabled, the frontend retains the separate two-tab experience.
- Rollback requires zero database migration and zero schema modification.


## Review fixes: session context (PROPOSED, additive to /converse only)

- Unified responses include optional top-level `sessionId` and `sessionVersion`, including policy-only turns. Clients prefer these over nested advisor metadata.
- Explicit public hotel selection stores only a normalized `policyHotelId` in the existing owner-bound session under its existing TTL and optimistic lock. No raw question is added to backend session storage. Every RAG call rechecks public hotel eligibility.
- Policy-only followups reuse the selected hotel or the unique hotel in the last verified cards; multiple hotels still require selection. Material search-criteria changes clear the selection.
- A Core dependency exception in a combined turn produces advisor `TEMP_UNAVAILABLE` without cards; RAG can still answer an explicitly scoped policy. Authorization, session ownership and version errors retain their existing HTTP errors.
- R1 endpoints, DB schema and feature-flag defaults are unchanged. Owner review for the additive unified contract remains required before merge.
