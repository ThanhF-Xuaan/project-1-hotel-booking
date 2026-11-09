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
  "policyHotelId": 2
}
```
- `message`: String, required, 1..500 characters.
- `sessionId`: UUID, optional.
- `expectedSessionVersion`: Long, optional.
- `policyHotelId`: Integer, optional (1..32767). Used when clicking hotel choice chips to explicitly bind policy scope.

### Response Body (`ApiResponse<UnifiedAdvisorResultDto>`)
```json
{
  "success": true,
  "data": {
    "contractVersion": "2.1-proposed",
    "requestId": "7d91e324-4f81-49b8-a612-4a0058b8ef42",
    "intent": "SEARCH_AND_POLICY",
    "advisor": {
      "status": "MATCHED",
      "sessionId": "4b68e715-6db5-48d6-9533-5b8cb46f483c",
      "sessionVersion": 2,
      "cards": [ ... ],
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
        "citations": [
          {
            "hotelId": 2,
            "documentId": "POLICY-CHILD-BREAKFAST",
            "section": "Chính sách buffet sáng cho trẻ em",
            "content": "Trẻ em dưới 6 tuổi đi cùng bố mẹ được miễn phí buffet sáng..."
          }
        ]
      },
      "hotelChoices": null,
      "retryAfterSeconds": null
    }
  },
  "message": null
}
```

---

## 4. Rollback & Safety Gates

- Default configuration: `hotel.ai.advisor.unified.enabled: false`. When disabled, Spring Boot does not register `PublicUnifiedAdvisorController`, returning standard `404 Not Found`.
- Frontend feature flag: `VITE_ADVISOR_UNIFIED=false`. When disabled, the frontend retains the separate two-tab experience.
- Rollback requires zero database migration and zero schema modification.
