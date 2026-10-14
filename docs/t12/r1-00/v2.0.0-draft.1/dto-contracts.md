# DTO và API contracts — draft 2.0.0-draft.1

## Quy ước DTO

JSON Schema là draft chuẩn cho wire; tài liệu này là mapping sang Java 21, chưa tạo runtime DTO/controller. Khi được phép triển khai, DTO thuộc module AI của Member 1, không chỉnh DTO của Pricing/Inventory. Không dùng entity JPA làm response.

| DTO | Field và Java type | Validation/quy tắc |
|---|---|---|
| RoomSearchCriteria | hotelId: Short?, locationKeyword: String?, checkInDate/checkOutDate: LocalDate, adults/children/infants/extraBeds/roomsCount: Short; requiredFeatureCodes/preferredFeatureCodes: List<String>; budget: Budget? | Full SEARCH cần hotelId hoặc locationKeyword; không coi keyword là tenant scope. adults ≥1; các counts khác ≥0; roomsCount ≥1 và còn giới hạn one-room ở orchestration. |
| Budget | amount: BigDecimal, currency: String, basis: BudgetBasis, coverage: BudgetCoverage, hard: boolean | Amount >0 bằng semantic validation, wire decimal string. Currency ISO code chưa có nguồn authoritative tại baseline. Không mặc định all-in hoặc mỗi đêm. |
| ChatRequest | contractVersion: String, message: String, sessionId: UUID?, expectedSessionVersion: Long?, criteriaPatch: CriteriaPatch? | Tối đa 2000 ký tự; sessionId/version đi cùng. Patch chỉ có field đã biết, omitted = giữ giá trị; không dùng null để xoá. |
| CriteriaExtractionProposal | criteriaPatch: CriteriaPatch, provenance: List<CriteriaEvidence>, ambiguities: List<Ambiguity> | Internal output proposal từ model; Java validate và kiểm tra lại origin/confirmation. Không expose như credential hoặc nhận metadata owner từ client. |
| CriteriaEvidence | field: CriteriaField, origin: USER_PROVIDED/INFERRED/DEFAULT, confirmation: CONFIRMED/UNCONFIRMED/AMBIGUOUS | Mỗi field có một record; không lưu raw message/PII. Inferred/default không được nâng thành xác nhận của khách. |
| SearchRequest | contractVersion, criteria: RoomSearchCriteria, sessionId/version? | Structured search không gọi LLM hoặc RAG. Hotel scope luôn lấy từ server context. |
| RefreshRequest | contractVersion, sessionId: UUID, expectedSessionVersion: Long | Không nhận message/criteria/room IDs từ client; refresh toàn bộ search đã lưu trong session, chạy discovery + quote + availability mới. |
| AdvisorResponse | ApiResponse<AdvisorResult> | Success code 200. AdvisorResult gồm status, session/version, criteria, cards, coverage, warnings; `knowledgeStatus=DISABLED`, `sources=[]`. |
| RoomRecommendation | recommendationId: String, rank: int, stay: StayInterval, room: RoomSummary, price: PriceQuote, reasons: List<Reason>, action: ViewRoomDetails | stay mang checkIn/checkOut/roomsCount=1; ID do server tạo, unique trong response. Không phải booking ID/token hold; rank liên tục 1..N; HotelRoomType ID unique. |
| RoomSummary | hotelId: Short, hotelRoomTypeId: Integer, roomTypeId: Short, names/code, standard/max sức chứa (standardAdults/standardChildren/maxAdults/maxChildren/maxInfants), extraBedsAllowed, extraBeds, features | Public projection đề xuất, không đưa totalQuantity/locked/booked/audit/guest fields lên card. Không bịa ảnh/giường. |
| PriceQuote | quoteId, observedAt: OffsetDateTime, source, hotelRoomTypeId, totalNights; currency: String?; BigDecimal totals; serviceFeeStatus/chargeCoverage/budgetCheck | quoteId/observedAt là adapter observation, không phải Core guarantee/token. Giá đến từ Backend, JSON string khi xuất qua AI DTO; native Core fixture giữ JSON number. |
| Coverage | status, catalogComplete, discovered/evaluated/failed/remaining/verifiedMatch counts, reasons | Phân biệt “không có phòng” với “chưa kiểm tra đủ”. Các counters tính trên tập ID unique đã discovery trong scope. |

Enums và trường đầy đủ nằm trong `$defs` của [Advisor Schema](schemas/advisor.schema.json). Schema cố ý từ chối unknown fields như ownerId, role, tenantId, holdToken hoặc operation write.

## Ba thao tác

| Operation | Proposed endpoint | Điều kiện |
|---|---|---|
| CHAT | POST `/api/v1/public/ai/room-advisor/chat` | Extract nhu cầu từ message đã redacted; Java validate patch, hỏi lại phần thiếu/mơ hồ, không để LLM chọn IDs/giá/link hoặc gọi business tools. |
| SEARCH | POST `/api/v1/public/ai/room-advisor/search` | Full criteria; deterministic candidate discovery, eligibility, quote, rank sau R1-00. |
| REFRESH | POST `/api/v1/public/ai/room-advisor/refresh` | Ownership + optimistic version trước khi đọc dữ liệu; revalidate cả catalog eligibility, availability, pricing. |

Cookie `t12_session`, header `X-CSRF-Token`, TTL, error envelope/status là **PROPOSED**; M2 cần duyệt cùng Security owner, chưa triển khai Security. Session identity không được tin từ UUID riêng lẻ; bind opaque server credential với owner. User khác đọc session nhận 404 không lộ metadata. Session version cũ nhận 409, không ghi đè state. Nghiệp vụ chỉ đọc; cập nhật conversation state được giới hạn trong module AI và không phải booking write.

Owner credential phải tồn tại ngay cả khi request chưa có sessionId. Cách bootstrap/cấp cookie cho khách anonymous hoặc bind với Keycloak còn UNKNOWN và cần Member 2 duyệt; không suy diễn một endpoint cấp credential đã có. Draft ghi HTTP 401/AUTH_REQUIRED khi không xác thực owner; credential hợp lệ nhưng conversation hết hạn/không thuộc owner là 404. OpenAPI security requirement là gate đề xuất, không xác nhận public guest access đã hoạt động.

Trường `criteria` trong response có thể partial ở NEED_INFO. Input nhiều phòng vẫn hợp lệ về schema và trả NEED_INFO; không trả cards hoặc quote cho nhu cầu đó. Response OUT_OF_SCOPE cho yêu cầu thực hiện write, kể cả người dùng xác nhận. SEARCH mới/CHAT mới không session tạo phiên; session và version được trả sau validation/ownership.

Response bổ sung `criteriaProvenance` và `ambiguities`. SEARCH có fields explicit được Java ghi USER_PROVIDED sau validation; CHAT phân biệt inferred/default. Không persist transcript để chứng minh provenance. Ambiguity về location, relative date hoặc budget basis/coverage phải được giải quyết trước RESULTS; metadata không thể cấp quyền hotel scope. Các defaults cần reviewer duyệt, không ngầm coi 2 adults/0 children/1 room là lời khách đã xác nhận.

## Giá và hard budget

Không suy diễn currency từ locale/hotel name. `currency=null`, serviceFee UNKNOWN hoặc thiếu authoritative coverage thì không thể PASS hard budget all-in. `serviceFeeAmount=null` biểu thị chưa biết; UNKNOWN không được gán 0. M2 phải xác nhận nguồn currency và nghĩa finalTotalAmount trước integration.

- STAY_TOTAL: so sánh tổng báo giá đủ coverage với amount.
- AVERAGE_PER_NIGHT: tổng cùng coverage chia totalNights, dùng BigDecimal và so sánh không làm tròn hiển thị.
- EACH_NIGHT: cần authoritative breakdown all-in từng đêm. Native daily netAmount trước VAT không đủ; không tự chia VAT/service fee để khẳng định mọi đêm dưới ngân sách.
- BACKEND_QUOTED_TOTAL là phạm vi người dùng chủ động chấp nhận so với tổng API đã báo; phải giải thích phí chưa xác minh. Không chọn phạm vi này ngầm để vượt hard budget all-in.
- Giá budget equality được PASS nếu coverage/currency đủ; quote lỗi/sai ID/sai số đêm/sai ngày bị loại. Không thay bằng basePrice, nhân nhiều phòng, đổi currency hoặc tính lại Pricing Engine.

Schema chỉ kiểm tra hình dạng; những quan hệ số tiền, ngày, identity, budget và scope cần semantic validation khi triển khai. Validator R1-00 chỉ kiểm tra invariants của artifact/examples, không thực hiện recommendation engine.

## Candidate selection và ranking oracle draft

Không dùng 5 hotels/20 candidates làm giới hạn toàn cục. Duyệt pagination trong phạm vi cho phép, stable ID sort, deduplicate và lọc hard constraints trước quote. Batch 20 là kích thước truyền dữ liệu; budget runtime/time limit phải báo PARTIAL và remainingCount. Chỉ được nói “phù hợp nhất trong các phương án đã kiểm tra”, không khẳng định tối ưu toàn hệ thống nếu chưa đầy đủ.

Oracle `PROPOSED_LEXICOGRAPHIC_V1`: loại ứng viên vi phạm hard constraints trước; xếp theo số preferred features có evidence giảm dần, sau đó budget distance tăng dần khi currency/coverage so sánh được, sau đó finalTotalAmount tăng dần chỉ trong cùng currency/coverage, cuối cùng hotelRoomTypeId tăng dần. Với hard budget PASS, budget distance bằng 0; với soft budget đủ dữ liệu, distance = max(0, applicableAmount - budgetAmount)/budgetAmount. Budget chưa đủ dữ liệu không cho điểm giá; ưu tiên feature rồi ID, kèm cảnh báo. Không so giá khác currency. Fixture ranking cơ bản không budget và mọi phòng cùng preference/currency/coverage. Chính sách này cần review; chưa là thuật toán production hoặc đáp án đã duyệt.

## Semantic invariants

1. CheckOut > checkIn; stay `[checkIn,checkOut)`; occupied nights đủ và không trùng. Availability native endDate = checkOut -1. availableCount ≥1 mọi đêm, hrt ID nhất quán.
2. maxAdults/maxChildren/maxTotalGuests/extraBeds và required features là điều kiện cứng; không tự coi extra bed tăng maxTotalGuests.
3. Hotel scope từ credential/context được server kiểm tra trên mọi candidate/detail/refresh; public room published/active/not-deleted phải được Core đảm bảo.
4. RESULTS chỉ roomsCount=1; hard budget cards có budgetCheck PASS trên currency + coverage khớp; không budget là NOT_REQUESTED. Không trả UNVERIFIABLE dưới hard budget.
5. COMPLETE cần catalogComplete, evaluated=discovered, remaining=0, failed=0. PARTIAL phải có lý do và warning INCOMPLETE_COVERAGE. failed nằm trong evaluated; verifiedMatch ≤ evaluated-failed; remaining=discovered-evaluated. Không cộng số totalElements chưa discovery vào remaining; page chưa tải ghi catalogComplete=false.
6. NO_MATCH chỉ khi complete và verifiedMatch=0. Lỗi toàn bộ upstream = TEMP_UNAVAILABLE; partial không có verified match = INCOMPLETE_SEARCH. NEED_INFO/OUT_OF_SCOPE không tạo cards.
7. Card path đề xuất `/portal/hotels/{hotelId}/room-types/{hotelRoomTypeId}` chỉ xem detail, khớp room IDs; không nhận URL từ model hoặc catalog text. M4 chưa duyệt route này.
8. REFRESH invalidates snapshot cũ; card IDs gắn phiên/version mới. Giá tại card là observation, không guarantee; detail UI phải revalidate qua flow do M2/4 thống nhất.
9. Stay của card phải khớp criteria đã validate và quote totalNights. Provenance không lặp field; field có metadata phải có giá trị tương ứng. Không có ambiguity chưa xử lý trong RESULTS.

## Security contract

Read adapters allowlist: public discovery/detail sau M2 duyệt, availability search, pricing calculate; không broad controller/service access. Hold/release/booking/cancel/payment/refund/guest writes có expectedCalls=0. Không có tool call capability cho LLM trong R1-00.

Redact CCCD/passport/payment/phone/email trước provider và log; chỉ dùng sentinel giả trong test. Catalog/quote/error text là untrusted data, không instructions. Server giới hạn message/feature lengths, rate limit, timeouts; semantic scope/ownership ở Java, không nhờ prompt đảm bảo. Không đưa internal inventory counts hoặc raw upstream exception vào response. Những controls này là acceptance criteria cho task sau, chưa được runtime chứng minh tại R1-00.
