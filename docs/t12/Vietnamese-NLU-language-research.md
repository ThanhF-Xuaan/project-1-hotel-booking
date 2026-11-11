# T12 Vietnamese search vocabulary — 2026-11-11

Status: CONFIRMED public terminology; PROPOSED lexical coverage, not a frequency study of customer chat logs.
Implementation baseline: feat/m1-t12-unified-conversation, HEAD 2232cf23 plus existing local fixes.

## Sources

- Traveloka hotel help: https://www.traveloka.com/vi-vn/help/hotel — hotel search, meals, extra-bed requests.
- Booking.com Vietnamese FAQ: https://secure.booking.com/faq.vi.html — double versus twin beds are distinct; a generic double-room request does not establish the bed arrangement.
- Vinpearl: https://vinpearl.com/vi/cap-nhat-tong-hop-combo-vinpearl-nha-trang-3-ngay-2-dem — day/night duration, breakfast, children, extra beds.
- Vinpearl booking: https://booking.vinpearl.com/vi-VND/tour/phu-quoc-mua-hanh-phuc-2n1d-phongvilla-vinpearl-an-03-bua-giam-den-30-dich-vu-khach-san-vinwonders-safari-nhieu-uu-dai-khi-luu-tru-tu-02-dem-gn00264 — abbreviated 2N1Đ and explicit number of nights.
- MoMo: https://www.momo.vn/khach-san-theo-gio — hourly, overnight and daily stays are separate products.

These pages establish terminology, not a hotel's policy in chatbot answers. No external policy is ingested into RAG by this change.

## Curated aliases supported by the current criteria contract

| Group | Examples | Interpretation |
| --- | --- | --- |
| Number words | hai người lớn, một bé, hai phòng, ba đêm | Convert only before explicit count nouns; preserve unrelated words/names |
| Guest shorthand | 2NL1TE, 2 lớn 1 nhỏ, hai vợ chồng | Explicit adults/children counts; never infer ages or child-pricing bands |
| Duration | 3 ngày 2 đêm, 3N2Đ, 3n2d, qua đêm | Use night count; a date is still required |
| Relative arrival | tối nay, đêm nay, bữa nay | Today's calendar date in configured Vietnamese clock |
| Weekend | cuối tuần này 1 đêm | Explicit duration wins over existing two-night weekend convention; explicit dates win over weekend wording |
| Money | 500k, 500.000 VND, 1tr5, một triệu rưỡi | Exact numeric amount; scope of charges still requires clarification |
| Place | Da Nang, Ha Noi, TP.HCM, TPHCM, Sài Gòn | Accent-insensitive match against existing destination allowlist |
| Amenities | điểm tâm, bữa sáng, wi-fi, bể bơi, view biển, không cần hồ bơi | Existing preferred feature codes; ignore negated preferences |

## Deliberately unresolved

- "Gần biển" does not imply a sea-view room. Proximity requires an authoritative location/distance contract.
- King/twin, connecting rooms, high floors, quiet rooms and smoking preferences require supported verified fields before filtering or claiming matches.
- Hourly stays, vague holidays, "tuần sau" without dates, and "rẻ/thoải mái" without a budget must not become invented dates or prices.
- Existing Saturday-to-Monday interpretation of an unspecified weekend is a product convention, not a linguistic fact. Cards show resolved dates; explicit dates and duration take precedence.
- Multi-room requests retain existing one-room clarification; no booking/payment operations are added.

## Verification

Targeted tests: HybridRoomAdvisorLlmServiceTest, PolicyIntentClassifierTest, UnifiedAdvisorFlowIntegrationTest.
Synthetic examples only; no paid model calls or customer chat logs.
