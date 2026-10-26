# U14 — Consent và nguồn lịch sử do server xác minh

- **Ngày hồ sơ:** 2026-10-26, Asia/Saigon.
- **Status:** **PROPOSED — chưa duyệt contract, chưa triển khai**.
- **Baseline xác minh:** `origin/develop@e027e90bb56a9320e1271614761b8b97f285853e`.
- **Nhánh tài liệu:** `codex/u14-consent-history-spec`; độc lập với nhánh tooling `codex/m1-repeatable-acceptance`.
- **Ủy quyền:** người dùng phụ trách toàn project và yêu cầu tiếp tục công việc hợp lý. Hồ sơ này cụ thể hóa đề xuất U14; không ghi nhận thay người dùng một quyết định về consent, xác minh danh tính hoặc thời hạn lưu dữ liệu.
- **Allowlist lượt này:** chỉ thêm tài liệu này. Mã nguồn, API, schema, dữ liệu và gold giữ nguyên.

## 1. Mục tiêu

Đề xuất một MVP gợi ý hạng phòng dựa trên lịch sử đặt phòng cá nhân đã hoàn tất, thuộc hồ sơ CRM đã được liên kết đáng tin cậy với tài khoản đang đăng nhập. Phạm vi lịch sử và consent giới hạn trong một khách sạn. Gợi ý chỉ mang tính tham khảo; giá/tồn phòng tiếp tục được Core xác nhận trong luồng xem phòng/đặt phòng.

Thông tin booking contact phản ánh hồ sơ đứng tên booking. Việc xác nhận người thực tế lưu trú cần một hợp đồng khác vì source hiện tại chưa có liên kết trực tiếp từ StayGuest tới BookingGuest. MVP cần được phê duyệt với đúng ý nghĩa “lịch sử đặt phòng”, trước khi dùng tên gọi “hành vi lưu trú cá nhân”.

## 2. Những gì đã xác minh

Đường dẫn dưới đây tính từ root repository; tên symbol là symbol hiện có.

| Trạng thái | Bằng chứng hiện tại | Hệ quả |
|---|---|---|
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/personalization/controller/GuestPersonalizationController.java`: `/api/v1/ai/personalization/recommend`, `/evaluate-baseline`; conditional `hotel.ai.personalization.synthetic-evaluation-enabled`; chỉ CHAIN_ADMIN | Giữ nguyên boundary synthetic hiện có. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/personalization/dto/GuestPersonalizationRequestDto.java`: client gửi guestId, consentGranted, referenceDate và stayHistory; tối đa 100 records, maxSuggestions 1..10 | Request hiện có chưa chứng minh quyền đối với dữ liệu thật. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/common/config/KeycloakJwtAuthenticationConverter.java`: principal name ưu tiên preferred_username; token principal là Jwt | Binding mới lấy issuer/sub từ JWT đã xác thực, không dùng Authentication.getName() làm định danh ổn định. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/identity/entity/BookingGuest.java`: bảng booking_guests, có publicId/contact/identity; không có mapping issuer/sub | Cần binding riêng; publicId, email, phone hoặc số giấy tờ do client khai không chứng minh ownership. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/entity/StayGuest.java`: có Booking/BookingRoom và dữ liệu định danh; không có association BookingGuest | Không tự ghép hai loại guest bằng tên/điện thoại/giấy tờ. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/identity/controller/GuestController.java`: hai prefix `/api/v1/booking-guests`, `/api/v1/guests`; history thuộc các role nhân viên | Đây chưa phải API self-service được xác minh ownership cho CUSTOMER. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/identity/service/impl/GuestServiceImpl.java#getStayHistory`; `backend/src/main/java/vn/edu/utc/hotel_booking/modules/identity/dto/response/GuestStayHistoryResponse.java`: bookingDate là createdAt, kèm fullName/phone/totalSpent; không cung cấp room type/check-in/check-out cần cho U14 | Không chuyển DTO CRM này thành stayHistory bằng ngày hoặc giá trị giả định. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/repository/BookingRepository.java#findByGuestIdWithHotel`: lọc bookingGuest.id và sắp createdAt, trả Booking; query này không lọc hotel hoặc completed room | Cần projection bounded riêng trong module sở hữu Booking. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/entity/BookingStatus.java`: CONFIRMED/CANCELLED/NO_SHOW; `backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/entity/BookingRoomStatus.java`: có CHECKED_OUT | Điều kiện hoàn tất phải dựa vào BookingRoom, không tạo BookingStatus.CHECKED_OUT. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/entity/BookingRoom.java`: actualCheckInAt, actualCheckOutAt, bookingDetail; `backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/entity/BookingDetail.java`: HotelRoomType và ngày dự kiến | Có dữ liệu nền để thiết kế projection; chưa có port đáp ứng đầy đủ đặc tả này. |
| CONFIRMED | `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/personalization/service/GuestPersonalizationService.java`: computeStayWeight/computeRoomTypeSuggestions hiện có; personalize còn tính segment, features, services và warnings chứa ngày | Không expose toàn bộ kết quả synthetic qua API self-service mới. |
| UNKNOWN | Chưa thấy kho consent hay mapping account–BookingGuest trong các entity Identity và Liquibase SQL/XML đã tra cứu | Không suy ra consent từ đăng nhập, role admin hoặc checkbox gửi trong recommendation request. |
| UNKNOWN | Chưa có quyết định về identity-link verification, thông báo consent, xử lý merge/unlink, thời hạn giữ consent evidence và phạm vi lịch sử cũ | Chặn triển khai dữ liệu thật cho tới khi các quyết định ở mục 8 được chốt. |

## 3. Quyết định đề xuất cho MVP

Tất cả quy tắc ở mục này là **PROPOSED**, không phải hành vi đang chạy trên develop.

1. **Self-service:** chỉ CUSTOMER đã xác thực, với issuer/sub của chính token. Role nhân viên hoặc CHAIN_ADMIN không cho quyền chọn một guest bất kỳ ở các endpoint `/me`.
2. **Binding:** một issuer/sub liên kết với tối đa một hồ sơ BookingGuest active; MVP không chia sẻ hồ sơ giữa tài khoản. Liên kết được xác minh qua workflow Identity được duyệt. Chưa tạo public API tự khai guestId rồi nhận ownership; không auto-link bằng email/phone.
3. **Consent:** purpose cố định cho gợi ý hạng phòng, theo binding + hotel + policyVersion. Server lưu trạng thái và revision. Người dùng tự cấp/rút; admin không cấp thay người dùng. Default chưa cấp. Binding bị hủy/đổi hoặc profile bị xóa làm consent cũ hết hiệu lực cho luồng gợi ý.
4. **Lịch sử:** chỉ khách sạn trong consent; chỉ booking cá nhân FIT, gắn đúng BookingGuest, Booking.status=CONFIRMED và BookingRoom.status=CHECKED_OUT. MVP nhận booking có đúng một BookingRoom để hạn chế suy diễn sở thích từ đặt phòng hộ/đoàn. Booking đứng tên contact vẫn chưa chứng minh actual traveler; UI mô tả đúng ý nghĩa ở mục 1.
5. **Đặc trưng:** MVP chỉ tính affinity hạng phòng từ lịch sử đã xác minh. Không đọc free-text preference notes, danh tính, thu nhập, tổng chi tiêu hoặc tính travel segment; features/services chờ nguồn và consent riêng. Không lấy tiện nghi có sẵn của phòng làm bằng chứng khách thích tiện nghi đó.
6. **Xử lý:** không cache/persist lịch sử và kết quả cá nhân hóa ở MVP; không gọi LLM/embedding. Reuse phép tính room affinity hiện có qua một core thuần, tránh đi qua personalize() đang tính các nhóm tín hiệu ngoài MVP.
7. **Cổng bật:** endpoint self-service mặc định tắt bằng cấu hình mới đề xuất `hotel.ai.personalization.customer-enabled=false`. Cờ synthetic hiện hữu độc lập và tiếp tục mặc định tắt.

## 4. Hợp đồng API đề xuất

Các route/DTO/error bên dưới **chưa tồn tại**. Không thay route/DTO/envelope của synthetic controller. Thành công dùng envelope `ApiResponse` hiện hữu với code=200, message, result; mã số lỗi mới cần đối soát registry trước implementation, không tự sửa shared ErrorCode.

| Method/route PROPOSED | Input | Kết quả đề xuất |
|---|---|---|
| GET `/api/v1/me/personalization-consent?hotelId=1` | hotelId dương, trong miền ID Hotel hiện hữu | State của chính binding: granted, policyVersion, revision, hotelId; không trả subject/guestId/identity evidence. |
| PUT `/api/v1/me/personalization-consent` | hotelId, granted boolean, expectedRevision; policyVersion hiện hành bắt buộc khi grant | CAS update state/revision theo server; server timestamp; stale revision hoặc policy khi grant trả 409. Revoke không đòi người dùng duyệt lại policy mới. |
| GET `/api/v1/me/personalization/recommendations?hotelId=1&maxSuggestions=3` | hotelId; maxSuggestions 1..10, mặc định 3 | status, hotelId, roomTypeSuggestions, nonBindingSuggestion=true, pricingOrPromotionMutated=false. |

Recommendation request không nhận guestId, consentGranted, referenceDate, history hoặc free text. Reject tham số ngoài allowlist với 400 để tránh client tưởng dữ liệu giả đã được chấp nhận. Body consent cũng reject field ngoài allowlist. Không đổi Jackson behavior chung cho toàn project.

Response recommendation mới không có raw history, thời điểm lưu trú, booking IDs, guestId, detectedSegment, feature/service suggestions hoặc warnings echo nội dung nguồn. Mỗi suggestion chỉ có verified hotelRoomTypeId, nhãn công khai, affinityScore và reason code chung. Affinity score là điểm xếp hạng, không phải xác suất hoặc giá/tồn phòng được bảo đảm.

| Tình huống | HTTP/result đề xuất |
|---|---|
| Feature tắt | 404, không query consent/history. |
| JWT thiếu/sai hoặc role không hợp lệ | 401/403 theo security/error envelope hiện hữu; không đọc lịch sử. |
| Tài khoản chưa có binding active đã xác minh | 409, lỗi mới đề xuất IDENTITY_LINK_REQUIRED; không trả thông tin hồ sơ khác. |
| Chưa grant, đã revoke hoặc grant thuộc policy/binding cũ | 200, CONSENT_REQUIRED, danh sách rỗng; không query history. |
| Có consent, history query thành công nhưng không có bản ghi hợp lệ | 200, COLD_START_FALLBACK, danh sách rỗng; UI hướng sang tìm kiếm thông thường. |
| Có lịch sử hợp lệ và room type còn đủ điều kiện công khai | 200, PERSONALIZED_READY, tối đa maxSuggestions, chỉ hotel đích. |
| Binding/consent/history/catalog dependency lỗi | 503, không trả cached/fallback cá nhân hóa và không đổi thành cold start. |
| expectedRevision stale; grant policyVersion cũ | 409; client đọc lại trạng thái, không tự gửi lại grant với revision mới. |

Tên ba result status nêu trên đã có trong PersonalizationStatus; response mới và các lỗi khác vẫn là đề xuất. Catalog filtering thành công nhưng loại hết candidate trả cold start với reason code chung, không tạo room type thay thế.

## 5. Binding, consent và nguồn lịch sử

### Identity/consent boundary — PROPOSED

Identity sở hữu binding và consent. Logical records dự kiến:

- Binding: issuer, subject, guestId, verification state/provenance tham chiếu tối thiểu, bindingVersion, revokedAt. Unique active binding theo cả account và guest trong MVP. Không lưu raw identity document trong record này.
- Consent: bindingId + bindingVersion + hotelId + purpose + policyVersion, granted state, revision, server decision timestamp. Không dùng tên/email trong khóa.
- Grant/revoke kiểm tra và tăng revision atomically; hai lần ghi cùng expectedRevision chỉ một lần thành công. Lượt grant cũ đến sau revoke phải conflict, không làm sống lại consent ngoài ý muốn.
- Hồ sơ consent thiếu do lỗi storage không được diễn giải thành “chưa consent”. Phân biệt absent với unavailable.
- Quy trình link/unlink/merge và quyền operator chưa duyệt. Xác minh binding không tự grant consent. Thời hạn giữ binding/consent/audit và xử lý purge cần quyết định riêng trước DDL/migration.

### Booking history boundary — PROPOSED

Module Booking cung cấp port chỉ đọc mới đề xuất `VerifiedBookingPreferenceHistoryPort`; tên này chưa có source. Nó nhận context nội bộ đã xác minh từ orchestration, không nhận object “verified” do HTTP client khai.

Projection tối thiểu: source bookingRoom ID dùng nội bộ để deduplicate, hotelId, hotelRoomTypeId, public room label/code đã đối soát, actualCheckInAt/actualCheckOutAt. Không trả Booking entity hoặc DTO CRM đầy PII cho module AI.

- Query kiểm tra đúng guest binding + hotel trước LIMIT, cùng các điều kiện FIT/single-room/completed ở mục 3; loại CANCELLED/NO_SHOW, missing timestamps, out<in và timestamp tương lai.
- Bound tối đa 100 bản ghi gần nhất, ordering actualCheckOutAt DESC rồi bookingRoom ID DESC, deduplicate trước limit. Phân biệt zero rows với query failure. Khoảng thời gian được phép khai thác dữ liệu cũ cần quyết định tại mục 8.
- Ngày phân tích lấy từ Clock server tại Asia/Ho_Chi_Minh; chuyển actual timestamps về cùng timezone trước khi tính đêm. BookingDetail dates là ngày dự kiến, không tự thay actual timestamps thiếu. Bản ghi không tạo được ít nhất một đêm bị loại trong MVP; không tự ép thành một đêm.
- Room code/name phải được đối soát theo Catalog; scope hotel và public eligibility được kiểm tra trước khi trả suggestion. Không dựng mã, tên, tiện nghi hoặc nhãn public từ ghi chú booking.
- Không dùng `GuestService.getStayHistory` làm projection này: DTO thiếu actual dates/room types, dùng createdAt và chứa PII/chi tiêu không cần cho MVP.

### Orchestration và revoke — PROPOSED

Trình tự: verified JWT → active binding → hotel/policy/consent → bounded history → room-affinity core → public catalog filter → kiểm tra lại bindingVersion/consent revision → response tối thiểu.

Không có consent hợp lệ thì không gọi history port. Recommendation chỉ đọc; PUT consent chỉ được ghi kho consent/audit được duyệt. Không ghi Booking/Inventory/Pricing/Payment/guest profile hoặc đưa context này vào session T12 anonymous.

Nếu revoke/unlink commit trước lần kiểm tra quyền cuối của recommendation đang chạy, hủy kết quả và trả CONSENT_REQUIRED hoặc lỗi binding phù hợp. Điểm kiểm tra quyền cuối là mốc authorization; response đã được cấp quyền trước revoke có thể đang truyền trên mạng. Không hứa xóa nội dung đã tải vào trình duyệt bằng thao tác revoke.

## 6. Ma trận nghiệm thu đề xuất

Tất cả scenario mới bên dưới **NOT RUN**. Chỉ dùng fixture giả lập cho tới khi identity/privacy contract được duyệt.

| ID | Tình huống | Bằng chứng bắt buộc |
|---|---|---|
| U14-C01 | Feature mặc định tắt | HTTP 404, zero binding/consent/history calls; synthetic routes giữ gate hiện có. |
| U14-C02 | Anonymous, JWT giả, staff-only/admin-only | 401/403; không dùng role admin để bypass self ownership. |
| U14-C03 | Cùng username, issuer/sub khác; rename username | Không đổi/chiếm binding của tài khoản khác; verified issuer/sub là khóa. |
| U14-C04 | Không binding; binding revoked/guest deleted | Fail closed trước history; không auto-link bằng phone/email/publicId. |
| U14-C05 | Chưa consent, revoked, policyVersion cũ | CONSENT_REQUIRED và zero history calls. |
| U14-C06 | Fake guestId/history/consentGranted trong request | 400, không nhận các field ngoài contract. |
| U14-C07 | Guest A/B, hotel A/B | Chỉ exact binding + hotel được query; không kéo cả chuỗi rồi lọc phía UI. |
| U14-C08 | GIT/CORPORATE/multi-room, cancelled/no-show/not checked out | Không vào projection; không dùng BookingStatus.CHECKED_OUT không tồn tại. |
| U14-C09 | Actual dates thiếu/đảo/future, biên UTC+7, bookingDate khác actual | Không dùng createdAt/planned dates bù; tính ngày theo timezone đã chọn. |
| U14-C10 | Hơn 100 rows, timestamp bằng nhau, join trùng | Bound, stable ordering và dedup đúng; query scope trước limit. |
| U14-C11 | History/consent/catalog outage so với history empty | 503 phân biệt cold start; không có gợi ý cá nhân hóa giả. |
| U14-C12 | Grant/revoke cùng revision, stale grant retry | Một writer thắng; stale request 409; revoked state không bị sống lại tự động. |
| U14-C13 | Revoke/unlink commit trước final auth check | Barrier test xác nhận response không giải phóng suggestions cũ; request sau revoke không đọc history. |
| U14-C14 | PII/free text/segment/services/features | Không query/compute/return các nhóm ngoài MVP; log/response không echo raw history. |
| U14-C15 | Ranking core sau tách | Synthetic room-affinity cases giữ expected ranking/score hiện hữu; không sửa gold/threshold. |
| U14-C16 | No business writes và rollback consent | Spies/DB digest cho recommendation; grant/revoke chỉ ghi consent/audit, lỗi transaction không để state dở dang. |

Khi triển khai: targeted tests → Maven liên quan → regression backend → sandbox API/browser consent/revoke với Keycloak thật; ghi SHA/config/fixture digest, exit code và skip. Dataset synthetic hiện hữu không tự trở thành gold được duyệt hoặc bằng chứng quyền dùng dữ liệu thật.

## 7. Trình tự triển khai sau khi chốt quyết định

**PROPOSED, chưa phải file allowlist triển khai đã duyệt:**

1. Identity owner chốt workflow link/unlink và state/revision của consent; review schema/migration incremental. Test trước trên DB riêng đã được chọn và backup. Schema mới chưa được tạo trong lượt này.
2. Booking/Catalog owner chốt projection chỉ đọc, semantics FIT/single-room/completed, ordering/bounds và public eligibility. Triển khai provider trong module sở hữu, xác minh tenant bằng PostgreSQL fixtures.
3. M1 tách room-affinity core khỏi full synthetic personalize; thêm self-service orchestration/DTO/controller sau contract request. Golden assertions cũ được giữ nguyên; output mới có test riêng. Không mở endpoint self-service trước khi hai provider trên có đủ bằng chứng.
4. UI hiển thị giải thích phạm vi/policy rõ ràng và consent không chọn sẵn; có thao tác revoke, xử lý 409/503 và cold start. Browser E2E kiểm chứng bằng tài khoản giả lập riêng.

Trước mỗi slice phải khóa file allowlist cụ thể theo source thực tế và spec đã duyệt; không sửa toàn module Identity/Booking/frontend. Rollback bằng tắt cờ self-service, revert từng commit slice; không xóa consent/audit hay rollback schema phá hủy dữ liệu. Khôi phục DB test bằng snapshot đã kiểm chứng khi cần.

## 8. Các quyết định cần chốt

| ID | Khuyến nghị | Trạng thái |
|---|---|---|
| D1 | Chấp nhận MVP sở thích đặt phòng cá nhân FIT/single-room/completed theo CRM contact; UI mô tả đúng nguồn | PROPOSED; nếu cần actual traveler thì phải bổ sung mapping đã xác minh khác trước. |
| D2 | Identity binding theo issuer/sub, một account–một CRM profile, workflow verification/link/unlink/merge rõ ràng | BLOCKED cho dữ liệu thật: workflow và bằng chứng xác minh chưa được chọn. |
| D3 | Consent theo hotel/purpose/policy, chỉ chính CUSTOMER grant/revoke, revision CAS | PROPOSED; nội dung thông báo/policyVersion chưa chốt. |
| D4 | Phạm vi lịch sử cũ, retention/purge cho binding/consent/audit, xử lý profile deletion | BLOCKED cho migration/dữ liệu thật: không tự đặt số ngày giữ dữ liệu. |
| D5 | API self-service và HTTP semantics ở mục 4; assigned numeric error codes, bounds đã đối soát với schema | PROPOSED; cần contract review trước code. |
| D6 | Chỉ room affinity; bỏ segment/free text/features/services ở MVP; không persist/cache kết quả | PROPOSED; cần chấp nhận scope sản phẩm trước khi tách core. |

Người dùng có toàn quyền quyết định project; các cột trên phản ánh thiếu quyết định cụ thể về sản phẩm/dữ liệu, không phải yêu cầu xin thêm quyền từ các member khác. Không có chữ ký/ngày phê duyệt được tạo thay người dùng.

## 9. Kiểm tra và Git của lượt tài liệu

- Source inspection: **CONFIRMED**, theo bảng mục 2; các path/symbol được kiểm tra tại baseline đã ghi.
- Runtime test của contract mới, Maven/lint/build/Docker/migration: **NOT RUN**, chỉ thêm tài liệu, chưa có implementation mới để nghiệm thu.
- Kết quả runner local trước đó thuộc commit `368f6e466cacfd5f7836e9d24b0908b9e1c1a132` trên nhánh riêng đã push; không dùng kết quả đó làm PASS cho contract U14 mới.
- File untracked có sẵn được bảo vệ; không reset/clean/stash hoặc mang dữ liệu vào branch. Hồ sơ này chưa commit/push; PR runner do người dùng tự tạo từ nhánh đã push.
