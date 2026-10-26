# Hợp đồng U14 local v1 — consent và gợi ý hạng phòng

- Trạng thái: **PROPOSED contract**; mục 7 ghi trạng thái triển khai local synthetic. Hợp đồng này không phê duyệt sử dụng dữ liệu khách thật.
- Baseline: **develop@627887bc235d5eb0b59835a8d47744e605430aa8**.
- Ngày: 2026-10-26; múi giờ Asia/Ho_Chi_Minh.
- Đích: acceptance sandbox riêng, PostgreSQL/Keycloak và dữ liệu giả lập.
- Chủ trì: Member 1; Identity sở hữu binding/consent, Booking sở hữu history, Inventory sở hữu catalog, frontend sở hữu UI.
- Lượt lập hợp đồng chỉ thêm ba tài liệu trong `docs/member1/contracts`; slice I01 sau đó bổ sung migration và fixture, giữ nguyên hồ sơ U14 untracked hiện có.

## 1. Quyết định sản phẩm

| Nội dung | Quyết định v1 |
|---|---|
| Người gọi | Tài khoản Keycloak giả lập có role CUSTOMER |
| Dữ liệu | Chỉ booking, hồ sơ, hotel, room type tổng hợp trong sandbox |
| Liên kết | Fixture gắn JWT iss + sub với BookingGuest; không ghép theo tên/email/phone/giấy tờ |
| Ý nghĩa history | Booking đứng tên hồ sơ; không khẳng định người đó thực tế lưu trú |
| Phạm vi | FIT, một phòng, CONFIRMED, BookingRoom CHECKED_OUT; check-out trong 12 tháng gần nhất |
| Giới hạn đọc | Tối đa 100 booking room hợp lệ mỗi request |
| Đầu ra | Gợi ý room type theo một hotel, chỉ mang tính tham khảo |
| Consent | Tự grant/revoke theo hotel và purpose; không chọn sẵn |
| Lưu trữ | PostgreSQL sandbox lưu consent state/event; không lưu cache, history trích xuất hoặc gợi ý |
| External AI | Không gọi LLM, embedding hay provider ngoài |
| Production | Feature tắt; hợp đồng không cho phép dùng dữ liệu khách thật |

Người dùng đã chốt local synthetic, hồ sơ đứng tên booking, 12 tháng, tối đa 100 record và UI tối thiểu. API/schema/mã lỗi bên dưới là **PROPOSED** cụ thể để chốt contract; chúng không mô tả behavior đang chạy.

## 2. Căn cứ source

Các đường dẫn Java dưới đây tính từ `backend/src/main/java/vn/edu/utc/hotel_booking/` tại baseline đã ghi. Fixture Keycloak là `tools/acceptance/keycloak/realm-acceptance.json`; route frontend được đối soát ở `frontend/src/App.tsx`.

| Trạng thái | Bằng chứng đã kiểm tra | Hệ quả |
|---|---|---|
| CONFIRMED | `modules/ai/personalization/controller/GuestPersonalizationController.java`: gate `synthetic-evaluation-enabled`, role `CHAIN_ADMIN` | Giữ API synthetic; route mới dùng flag riêng |
| CONFIRMED | `modules/ai/personalization/dto/GuestPersonalizationRequestDto.java`: caller gửi guestId, consent, referenceDate, stayHistory | Không tái dùng DTO cho self-service |
| CONFIRMED | `common/config/KeycloakJwtAuthenticationConverter.java`: `getName()` ưu tiên `preferred_username` | Khóa ownership bằng JWT `iss` + `sub`, không username |
| CONFIRMED | `modules/identity/entity/BookingGuest.java`: không có issuer/sub; chưa có consent store | Cần binding và consent store riêng trong Identity |
| CONFIRMED | `modules/booking/entity/BookingType.java`, `BookingRoomStatus.java`, `BookingStatus.java`: FIT/GIT/CORPORATE; checkout ở BookingRoom | Lọc hoàn tất ở BookingRoom |
| CONFIRMED | BookingDetail/BookingRoom entity và `RoomSwapRecord`: room type/quantity, actual timestamps, swap riêng | Dùng loại đã đặt; actual timestamps lọc history |
| CONFIRMED | `modules/identity/service/impl/GuestServiceImpl.java#getStayHistory`: dùng createdAt, trả PII/chi tiêu, thiếu actual dates/room type | Không dùng CRM response cho U14 |
| CONFIRMED | `modules/ai/personalization/service/GuestPersonalizationService.java`: room affinity cùng segment/features/services/warnings | Tách core room-affinity cho luồng mới |
| CONFIRMED | `common/dto/ApiResponse.java`, `common/exception/ErrorCode.java`: envelope và registry chung | Không đổi ErrorCode/handler chung |
| CONFIRMED | Acceptance Keycloak fixture chỉ có một CUSTOMER; không có route privacy frontend | Cần thêm CUSTOMER B và trang privacy local |
| UNKNOWN | Chưa có privacy policy khách hàng hoặc retention dữ liệu thật được chốt | Khách thật tiếp tục BLOCKED |

Hồ sơ cũ tại docs/member1/proposed/u14-consent-history-v1-proposed.md được giữ nguyên trong máy local, chưa được version hóa trong PR này. Bộ contract mới ghi đủ quyết định và expected outcomes, không phụ thuộc việc reviewer có hồ sơ cũ.

## 3. API

Các route self-service yêu cầu Bearer JWT hợp lệ và role CUSTOMER. Không có guest ID trên route/body. API synthetic hiện tại không đổi.

| Method | Route | Request | Kết quả |
|---|---|---|---|
| GET | /api/v1/me/personalization-consent?hotelId=1 | hotelId bắt buộc | Consent hiện tại và policy |
| PUT | /api/v1/me/personalization-consent | hotelId, granted, expectedRevision, expectedBindingVersion; policyVersion chỉ khi grant | State sau commit |
| GET | /api/v1/me/personalization/recommendations?hotelId=1&maxSuggestions=3 | hotelId; maxSuggestions mặc định 3, khoảng 1–10 | Status và tối đa N gợi ý |

### Kiểu và envelope

- hotelId là integer 1–32767, khớp Hotel.id kiểu Short.
- Revision/version là int64 không âm; expectedBindingVersion tối thiểu 1.
- Success dùng ApiResponse {code: 200, message, result}. Mọi response do U14 controller tạo, gồm lỗi, thêm Cache-Control: private, no-store; giữ nguyên response 401 của auth filter.
- Không lưu result vào browser storage.
- Reject unknown JSON properties, unknown/repeated query parameters và giá trị ngoài range bằng HTTP 400; không đổi Jackson toàn ứng dụng.
- Recommendation không nhận guestId, iss/sub, history, referenceDate, consent, preference note hoặc model output từ client.

### Consent

Consent view có hotelId, status, granted, revision, bindingVersion, purpose, currentPolicyVersion, acceptedPolicyVersion, updatedAt, historyLookbackMonths, noticeText, privacyPolicyUrl và dataMode.

- status: GRANTED, NOT_GRANTED hoặc RECONSENT_REQUIRED.
- granted chỉ true nếu state đã grant, policy còn hiện hành, binding active và guest chưa soft-delete.
- GET không có state trả NOT_GRANTED, revision 0, acceptedPolicyVersion null, updatedAt null; không tạo row.
- Revoke giữ acceptedPolicyVersion đã chấp nhận gần nhất trong state/event; nếu chưa từng grant thì giá trị này là null. granted=false luôn trả NOT_GRANTED, kể cả acceptedPolicyVersion cũ. State granted=true nhưng policy đã đổi trả RECONSENT_REQUIRED với granted=false; không sửa state khi GET.
- Policy: purpose ROOM_TYPE_RECOMMENDATION; version u14-local-room-affinity-v1; historyLookbackMonths=12; dataMode LOCAL_SYNTHETIC.
- privacyPolicyUrl=/portal/privacy. Trang phải hoạt động trước khi bật UI consent; nội dung chỉ nói về demo synthetic, không tự nhận là policy cho khách thật.

Notice v1:

> Đây là bản demo dùng dữ liệu giả lập. Khi consent có hiệu lực, hệ thống phân tích các booking FIT một phòng đã hoàn tất, đứng tên hồ sơ được liên kết, tại khách sạn này trong 12 tháng gần nhất để xếp hạng hạng phòng, bao gồm booking trước thời điểm cấp consent. Có thể rút consent bất cứ lúc nào. Gợi ý không thay đổi giá, khuyến mại hoặc đặt phòng và không bảo đảm giá hay tồn phòng.

PUT gồm hotelId, granted, expectedRevision, expectedBindingVersion và policyVersion. policyVersion bắt buộc khi grant, phải khớp version hiện hành và phải vắng mặt khi revoke.

- Mọi PUT hợp lệ tăng revision đúng 1 và ghi event, kể cả revoke khi ban đầu chưa grant.
- Writer khóa binding trước consent theo cùng thứ tự; xác minh binding active/version rồi so sánh revision.
- Hai request cùng expectedRevision và expectedBindingVersion: một commit, request kia HTTP 409.
- Consent state và event ghi trong một transaction; lỗi event insert rollback cả hai.
- Unlink/relink tăng bindingVersion; grant cũ không cấp consent cho binding mới, kể cả revision mới về 0.
- GET gặp policy cũ trả RECONSENT_REQUIRED; PUT grant gửi policy cũ trả 409 CONSENT_POLICY_CONFLICT. policyVersion phải là chuỗi không rỗng tối đa 100 ký tự; thiếu/sai kiểu/ngoài giới hạn trả 400, version hợp lệ về cú pháp nhưng không khớp trả 409.
- Client không tự retry grant khi conflict; cần đọc lại state và thao tác rõ ràng.

| Race / thứ tự commit | Kết quả bắt buộc |
|---|---|
| Hai writer cùng revision 0, chưa có state | Khóa binding trước; một tạo state/event revision 1, một 409; không trả lỗi unique thành 500 |
| Grant và revoke cùng revision r | Một commit revision r+1; request còn lại 409, không tự retry |
| Event insert lỗi sau update state | Rollback cả state/revision/event |
| Unlink commit trước stale grant | Không còn binding: 409 IDENTITY_LINK_REQUIRED; đã relink version mới: 409 IDENTITY_BINDING_CONFLICT |
| Grant cũ tới sau revoke | expectedRevision cũ: 409 CONSENT_REVISION_CONFLICT; không bật lại consent |
| Revoke/unlink commit trước final authorization | Loại kết quả đã tính theo quy tắc final authorization bên dưới |

Unlink và fixture relink phải khóa cùng binding trước khi đổi lifecycle. Binding revoked không được chuyển lại ACTIVE; relink tạo record/version mới. Guest soft-delete phải được kiểm tra lại ở final authorization; hard delete bị FK chặn khi còn binding/event tham chiếu.

### Recommendation và lỗi

Result gồm status, hotelId, dataMode, roomTypeSuggestions, nonBindingSuggestion=true, pricingOrPromotionMutated=false và reasonCode khi danh sách rỗng. Status dùng enum hiện có CONSENT_REQUIRED, COLD_START_FALLBACK, PERSONALIZED_READY.

Mỗi suggestion chỉ trả hotelRoomTypeId, roomTypeCode, roomTypeName, affinityScore, reasonCode=HISTORICAL_BOOKING_AFFINITY. Score [0.01,1.0] là điểm xếp hạng, không phải xác suất. Không trả guest/booking ID, stay dates, số lần đặt, segment, features, services, free text, giá, promotion hoặc availability.

| Trường hợp | HTTP và kết quả |
|---|---|
| Chưa grant, revoke hoặc policy cũ | 200 CONSENT_REQUIRED; không gọi history |
| History rỗng | 200 COLD_START_FALLBACK, reason NO_ELIGIBLE_HISTORY |
| Catalog lọc hết room type | 200 COLD_START_FALLBACK, reason NO_PUBLIC_ROOM_TYPES |
| Có candidate | 200 PERSONALIZED_READY |
| Không active binding hoặc guest soft-delete | 409 IDENTITY_LINK_REQUIRED |
| Revision/policy/binding version cũ trong PUT, hoặc final check phát hiện thay đổi | 409 với reason tương ứng; không trả suggestions |
| Binding/consent/history/catalog lỗi | 503 DEPENDENCY_UNAVAILABLE, không giả thành cold start |
| Hotel không tồn tại hoặc inactive | 404 HOTEL_NOT_FOUND |

Mã lỗi domain U14 dưới đây là PROPOSED, chỉ ánh xạ trong U14 controller; không sửa registry ErrorCode chung.

| HTTP | ApiResponse.code | result.reasonCode |
|---:|---:|---|
| 400 | 1002 | INVALID_REQUEST |
| 403 | 1007 | FORBIDDEN |
| 404 | 2021 | HOTEL_NOT_FOUND |
| 409 | 11001 | IDENTITY_LINK_REQUIRED |
| 409 | 11002 | CONSENT_REVISION_CONFLICT |
| 409 | 11003 | CONSENT_POLICY_CONFLICT |
| 409 | 11004 | IDENTITY_BINDING_CONFLICT |
| 503 | 11005 | DEPENDENCY_UNAVAILABLE |

401 do OAuth2 Resource Server hiện tại trả về; contract không buộc body 401 thành ApiResponse. Flag tắt trả 404 cho JWT hợp lệ; anonymous vẫn bị auth filter từ chối.

## 4. Schema logical đề xuất

Tên bảng/kiểu dưới đây là contract cho migration additive I01. Migration đã được viết và kiểm tra trên PostgreSQL 16 tạm; chưa áp dụng trên volume acceptance.

### personalization_account_bindings

| Cột | Kiểu/ràng buộc |
|---|---|
| id | BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY |
| issuer | VARCHAR(512) NOT NULL |
| subject | VARCHAR(255) NOT NULL |
| guest_id | BIGINT NOT NULL REFERENCES booking_guests(id) ON DELETE RESTRICT |
| binding_version | BIGINT NOT NULL CHECK > 0 |
| source | VARCHAR(50) NOT NULL CHECK = SYNTHETIC_FIXTURE |
| status | VARCHAR(20) NOT NULL CHECK IN (ACTIVE, REVOKED) |
| created_at | TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP; trigger cập nhật khi lifecycle đổi |
| revoked_at | TIMESTAMPTZ NULL; ACTIVE cần null, REVOKED cần khác null |

Unique (issuer, subject, binding_version), partial unique active index theo (issuer,subject), partial unique active index theo guest_id. Index các FK. Relink dùng max version + 1, không tái dùng version.

### personalization_consents

| Cột | Kiểu/ràng buộc |
|---|---|
| binding_id | BIGINT NOT NULL REFERENCES binding(id) ON DELETE RESTRICT |
| hotel_id | SMALLINT NOT NULL REFERENCES hotels(id) ON DELETE RESTRICT |
| purpose | VARCHAR(50) NOT NULL CHECK = ROOM_TYPE_RECOMMENDATION |
| granted | BOOLEAN NOT NULL |
| revision | BIGINT NOT NULL CHECK >= 1 |
| policy_version | VARCHAR(100) NULL; bắt buộc nếu granted |
| updated_at | TIMESTAMPTZ NOT NULL, do server gán |

Primary/unique key (binding_id, hotel_id, purpose). Không có row tương đương revision 0, granted=false. PUT đầu tiên tạo revision 1.

### personalization_consent_events

| Cột | Kiểu/ràng buộc |
|---|---|
| id | BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY |
| binding_id, hotel_id, purpose | FK/giá trị như consent state |
| binding_version | BIGINT NOT NULL CHECK > 0 |
| revision | BIGINT NOT NULL CHECK >= 1 |
| granted | BOOLEAN NOT NULL |
| policy_version | VARCHAR(100) NULL |
| event_at | TIMESTAMPTZ NOT NULL, do server gán |
| unique | (binding_id, hotel_id, purpose, revision); FK ghép (binding_id, binding_version) tới đúng phiên bản binding |

Application không có update/delete cho event. Không ghi request/response, PII, JWT, IP hoặc user agent. Không áp dụng AOP audit chung cho consent endpoint vì aspect có thể serialize tham số; state + event cùng transaction là evidence v1.

Tạo trigger updated_at trên binding và state table; event append-only không cập nhật. Migration theo Liquibase incremental convention. I01 đã được kiểm tra trên PostgreSQL 16 tạm. Volume acceptance có 30 changeset đã áp dụng; Liquibase status-only cho thấy dev seed dùng chung và changeset U14 còn pending. Không chạy update trên volume đó; do vậy changeset U14 vẫn chưa chạy ở acceptance.

## 5. History, catalog và ranking

### Ownership và port

- AI khai báo DTO/port tối thiểu trong modules.ai.personalization; không import entity/repository Booking, Identity, Inventory, JPA, Pricing hoặc Payment.
- Identity triển khai binding/consent resolver; Booking triển khai bounded history reader; Inventory triển khai public catalog lookup.
- Không trả Booking, BookingRoom, BookingGuest, StayGuest hoặc Hibernate entity sang AI.
- Recommendation không gọi GuestService.getStayHistory().

History projection nội bộ chỉ gồm booking-room ID để deduplicate, hotel ID, hotel-room-type ID, actual check-in và actual check-out. Reader nhận guest ID từ binding server-side, hotel ID, fromInclusive, toInclusive và limit 100.

Một record hợp lệ phải thỏa:

1. Đúng BookingGuest chưa soft-delete và đúng hotel.
2. BookingType FIT, BookingStatus CONFIRMED.
3. Đúng một BookingDetail quantity 1 và đúng một BookingRoom.
4. BookingRoom CHECKED_OUT; actual timestamps đủ, checkout > checkin, checkout không tương lai.
5. Checkout trong [00:00 ngày referenceDate trừ 12 tháng theo Asia/Ho_Chi_Minh, thời điểm hiện tại].
6. Sau khi đổi timezone, actual checkout date sau actual check-in date để có ít nhất một đêm.
7. Dedupe trước limit; sort checkout instant DESC rồi booking-room ID DESC; lấy tối đa 100.

Không dùng createdAt/planned dates bù actual timestamps thiếu. Timestamp không hợp lệ bị loại; query failure trả 503 và khác 0 record. Guest/hotel scope nằm trong query trước sort/limit.

Room type lấy từ BookingDetail đã đặt ban đầu; RoomSwapRecord/RoomInstance sau swap không đổi tín hiệu đó. Catalog chỉ trả room type thuộc hotel đã chọn, Hotel/HotelRoomType/RoomType ACTIVE và chưa soft-delete. Lọc trước top N; nhãn lấy từ catalog hiện tại.

Reference date và mốc kết thúc cửa sổ history lấy từ cùng một instant của server Clock. Chuyển timestamp sang Asia/Ho_Chi_Minh. nights là số ngày giữa actual local check-in và check-out; daysAgo=max(0, số ngày từ actual local check-in đến referenceDate); monthsAgo=daysAgo/30.4375. Trọng số mỗi record: 1/(1+0.15*monthsAgo) × min(3,1+0.1*nights) × 1.25 cho hotel đã chọn. Cộng trọng số theo hotel-room-type ID, lọc catalog hợp lệ, chia mỗi tổng cho tổng lớn nhất còn lại, clamp [0.01,1.0] rồi round 4 chữ số HALF_UP. Tie-break trên điểm đã round: affinity DESC, booking count DESC, local actual check-in gần nhất DESC, hotel-room-type ID ASC. Trong một hotel, boost là hệ số chung; giữ để tương thích nhưng không đổi thứ tự. Không gọi Availability, Pricing, promotion hoặc booking write path.

Fixture ranking: referenceDate=2026-10-26, ba booking-room khác ID có cùng actual local dates 2026-10-01 → 2026-10-03; hai record thuộc room type 101, một thuộc 102. Khi cả hai candidate active: thứ tự 101 (score 1.0000), 102 (0.5000). Khi 101 bị ẩn: chỉ 102, score 1.0000. Nếu 101 và 102 có cùng số record/ngày/điểm: 101 đứng trước nhờ ID nhỏ hơn. Mỗi fixture chỉ nạp trong sandbox và chọn Clock phù hợp cửa sổ 12 tháng.

Trình tự authorization: JWT → CUSTOMER → active binding/guest → hotel → current consent → history → catalog → affinity → đọc mới binding/consent → response. Final check phải thấy transaction mới, không dùng entity/cache/snapshot cũ. Binding biến mất/revoked hoặc guest bị xóa: 409 IDENTITY_LINK_REQUIRED; binding đã đổi: 409 IDENTITY_BINDING_CONFLICT; consent đã revoke hoặc policy không còn hiệu lực: 200 CONSENT_REQUIRED với danh sách rỗng; consent vẫn grant nhưng revision đã đổi: 409 CONSENT_REVISION_CONFLICT. Mọi trường hợp này loại kết quả đã tính; storage failure trả 503. Request bắt đầu sau revoke không gọi history. Revoke/unlink đã commit trước final check phải được quan sát; thay đổi commit sau final check không nằm trong bảo đảm thu hồi của v1.

## 6. UI, privacy và rollout

- Route /portal/personalization?hotelId=1 dùng ProtectedRoute CUSTOMER; link từ portal giữ hotelId.
- Tạo /portal/privacy trước khi bật consent UI. Trang chỉ mô tả synthetic demo, mục đích, nơi fixture tồn tại và cách rút; không phải policy khách thật.
- Checkbox không chọn sẵn; chỉ gọi recommendation sau khi consent được xác nhận.
- Revoke xóa suggestions. Đổi account/hotel, logout, storage event hoặc response cũ về muộn phải xóa/bỏ kết quả; dùng generation token hoặc AbortController.
- 401 theo auth hiện tại; 403 báo role; 404 quay lại tìm phòng; 409 đọc lại consent, không auto-grant; 503 không hiện result cũ, cho retry thủ công.
- Không render score như xác suất; không render giá/availability/history. CTA /portal/search vì v1 không có stay dates/quote.
- Flag hotel.ai.personalization.local-demo-enabled=false, độc lập synthetic-evaluation-enabled; chỉ bật với acceptance DB/Keycloak/fixtures cố định.
- Fixture và consent vẫn tồn tại trong Docker volume sau khi dừng container; chỉ reset/restore đã kiểm soát mới thay đổi dữ liệu đó. Cửa sổ phân tích 12 tháng không phải thời hạn lưu dữ liệu.
- Không đổi ApiResponse, Jackson global, JWT converter, SecurityConfig, ErrorCode chung, Booking public API, Pricing hoặc T12 route/DTO.
- Nếu sửa frontend/src/App.tsx, tạo T12 source re-audit có provenance mới; không nới hash validator.
- Rollback code bằng tắt flag, ẩn UI và revert từng slice; không drop event hoặc restore snapshot chưa xác minh.

Dùng dữ liệu khách thật bị BLOCKED cho tới khi có identity verification/link/unlink/merge, privacy policy được duyệt và retention/deletion policy cụ thể. Không tự đặt hạn lưu dữ liệu thật.

## 7. Quyết định và trạng thái

| ID | Trạng thái |
|---|---|
| D1: hồ sơ đứng tên booking | Người dùng chọn cho local v1; không chứng minh actual traveler |
| D2: local synthetic | Người dùng chọn; real binding workflow chưa có |
| D3: 12 tháng, limit 100 | Người dùng chọn; cửa sổ phân tích, không phải retention |
| D4: UI tối thiểu | Người dùng chọn; privacy route cần tồn tại trước khi bật |
| D5: API/error detail | PROPOSED tại tài liệu này; chưa có implementation. Schema additive I01 đã có trên nhánh implementation |
| D6: dữ liệu khách thật | BLOCKED bởi verification, privacy policy và retention chưa chốt |

Tại thời điểm chốt thiết kế ban đầu, API/UI runtime: **NOT RUN**; schema migration khi đó mới được kiểm tra trên PostgreSQL 16 tạm.

Cập nhật triển khai local ngày 2026-10-26, run `20261026-183941-474-ddccfd77`: I01–I06 đã được triển khai trên nhánh `codex/u14-local-v1-binding-storage`; `tools/acceptance/Verify.ps1` **PASS** với Maven 1.047/0 skip, API 23/23, browser 14/14, source audits, lint/build và snapshot restore. Bằng chứng chi tiết nằm trong [ma trận nghiệm thu](u14-local-v1.acceptance.md) và [báo cáo run](../acceptance/runs/acceptance-20261026-183941-474-ddccfd77.md). Đây chỉ là nghiệm thu synthetic local; không xác nhận quyền sở hữu hồ sơ khách thật, hiệu quả nghiệp vụ hoặc production readiness.
