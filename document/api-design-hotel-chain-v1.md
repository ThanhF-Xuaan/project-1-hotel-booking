# Thiết kế API dự kiến — quản lý chuỗi khách sạn và đặt phòng trực tuyến

**Trạng thái:** đề xuất thiết kế, chưa phải API đã triển khai.  
**Ngày:** 24/09/2026.  
**Phạm vi:** một chuỗi khách sạn, nhiều cơ sở; khách đặt phòng FIT trực tuyến, nhân viên vận hành, chatbot RAG, thanh toán **VNPay sandbox**. Không phân chia công việc trong tài liệu này.

Tài liệu này là phương án API mới cho mục tiêu hiện tại. [`api-frontend-contract-v1.md`](api-frontend-contract-v1.md) là đề xuất MVP cũ dùng `/api`, thanh toán tiền mặt và chatbot FAQ; không nên trộn hai contract. Chỉ cập nhật OpenAPI/FE contract sau khi chốt thiết kế và migration tương ứng.

## 1. Ranh giới MVP và các giả định cần chốt

- MVP đặt **một phòng, một hạng phòng, một đợt lưu trú** cho mỗi booking FIT. `checkOut` là ngày rời khách sạn, không tính thêm đêm. Booking nhiều phòng, đoàn/công ty và chia đợt thanh toán để giai đoạn sau.
- Khách không cần tài khoản Keycloak khi tra cứu và đặt phòng. Nhân viên dùng Keycloak. Public booking được bảo vệ bằng token truy cập ngẫu nhiên, phạm vi hẹp, do backend cấp; ID booking đơn thuần không cấp quyền đọc.
- Backend tự tính giá, thuế/phí theo cấu hình và lưu snapshot. Client không gửi `price`, `vat`, `bookingNumber`, `roomInstanceId` hay trạng thái thanh toán.
- MVP thanh toán **toàn bộ** bằng VNPay sandbox. Đây là tích hợp sandbox thật để thử redirect/IPN, không phải nút frontend tự báo thành công. Chưa có hoàn tiền tự động hoặc phát hành hóa đơn điện tử.
- Booking được xác nhận sau khi backend xác thực IPN thành công **và** còn quyền giữ tồn phòng. Nếu tiền về sau khi quyền giữ hết hạn, chuyển sang xử lý đối soát; không tự tạo phòng vượt tồn.
- Thời gian hold/checkout đề xuất 15 phút, được cấu hình trên server. Cần xác nhận với nghiệp vụ trước khi cố định trong UI. Việc thanh toán ở sandbox không bảo đảm callback đến trước thời hạn.

## 2. Quy ước contract

**Cập nhật code base:** search/availability/quote và AI `roomQuery` hiện nhận 1–30 đêm theo `[checkIn, checkOut)`; quá giới hạn trả `400`/`1002`. Các endpoint booking/hold bên dưới vẫn là thiết kế dự kiến. Chi tiết chính sách tra cứu tại [ADR 0002](../docs/adr/0002-stay-limits-and-sale-availability.md).

| Quy ước | Thiết kế |
|---|---|
| Base path | `/api/v1`; resource số nhiều, path kebab-case. `/public/**` chỉ chứa nghiệp vụ công khai hoặc truy cập bằng token hẹp. |
| Auth nhân viên | `Authorization: Bearer <Keycloak JWT>`. Backend kiểm tra permission và hotel scope, kể cả khi `hotelId` nằm trong URL. Không có API login/password riêng. |
| Auth public booking | `X-Booking-Access-Token: <opaque token>` trên API đọc booking/tạo payment attempt. Chỉ lưu hash token và thời hạn trong PostgreSQL; không ghi token vào URL hoặc log. |
| Ngày/giờ | Ngày `YYYY-MM-DD`; timestamp ISO 8601 có offset hoặc `Z`. Quy tắc đêm tính theo timezone của khách sạn. |
| Tiền | VND, JSON string decimal như `"1500000.00"`; backend dùng decimal chính xác. Số tiền sandbox gửi VNPay được nhân 100 theo tài liệu tích hợp. |
| ID | ID `BIGINT` trả dạng chuỗi để TypeScript không mất độ chính xác; ID `SMALLINT`/`INT` có thể là number. Hold ID dùng UUID/opaque ID mới. |
| Thành công | Áp dụng envelope đang có: `{ "code": 1000, "message": "Success", "result": ... }`. `201` có `Location`; `204` không body. IPN của VNPay là ngoại lệ, trả đúng format cổng thanh toán yêu cầu. |
| Danh sách | `page=0&size=20&sort=createdAt,desc`, tối đa `size=100`; `result` là `{items,page,size,totalElements,totalPages}`. |
| Retry ghi | Header `Idempotency-Key: <UUID>` cho hold, tạo booking và payment attempt. Cùng key + cùng request trả lại kết quả cũ; cùng key + payload khác trả `409`. |
| Giới hạn | Public search/chat/hold áp dụng rate limit theo IP và tín hiệu chống lạm dụng; không đưa dữ liệu nhạy cảm vào log/chat. |

Ví dụ lỗi dự kiến (mã nghiệp vụ chưa tồn tại trong `ErrorCode`, phải bổ sung trước khi FE phụ thuộc):

```json
{
  "code": 4101,
  "message": "Thời gian giữ phòng đã hết. Vui lòng tìm phòng và báo giá lại.",
  "result": null
}
```

`4101` là mã **đề xuất** cho `HOLD_EXPIRED`, chưa có trong `ErrorCode`; cần chốt dải mã số ổn định trước khi FE phụ thuộc. Các nhóm lỗi cần có: validation `400`, chưa xác thực `401`, thiếu quyền `403`, không tìm thấy `404`, hết phòng/hold hết hạn/giá thay đổi/idempotency conflict `409`, không thể báo giá do cấu hình thiếu `422`, gateway lỗi tạm thời `502/503`.

## 3. Bản đồ API MVP

### 3.1. Khách tra cứu, đặt phòng

| Method | Path | Input chính | `result` chính | Quy tắc |
|---|---|---|---|---|
| `GET` | `/api/v1/public/hotels` | `page,size`, vùng/từ khóa tùy chọn | Danh sách `id,name,address` | Chỉ cơ sở đang bán. |
| `GET` | `/api/v1/public/hotels/{hotelId}` | Path ID | Thông tin cơ sở, giờ nhận/trả phòng, tiện ích public đã duyệt | Không lộ dữ liệu nội bộ. |
| `GET` | `/api/v1/public/hotels/{hotelId}/room-types` | `checkIn,checkOut,adults,children,infants,page,size` | Hạng phòng, sức chứa, `availableRooms`, giá từ/giá tham khảo | Availability phải xét đủ từng đêm và phòng vật lý hợp lệ. |
| `GET` | `/api/v1/public/room-types/{hotelRoomTypeId}/quote` | Ngày, số khách | `totalAmount`, chi tiết từng đêm, `pricedAt`, `currency` | Read-only; giá chưa giữ phòng. |
| `POST` | `/api/v1/public/holds` | Hạng phòng, ngày, số khách; `Idempotency-Key` | `holdId`, `expiresAt`, giá chốt cho phiên checkout | Lock tồn theo từng đêm trong transaction; không chọn số phòng vật lý. |
| `POST` | `/api/v1/public/bookings` | `holdId`, thông tin liên hệ, `Idempotency-Key` | `bookingId`, `bookingNumber`, `status=PENDING_PAYMENT`, `accessToken`, `expiresAt` | Gắn booking với hold; giữ snapshot giá. Chỉ trả token một lần cho client hợp lệ. |
| `POST` | `/api/v1/public/bookings/{bookingId}/payment-attempts` | Booking token, `Idempotency-Key`; body rỗng | `paymentId`, `paymentUrl`, `expiresAt` | Backend tính amount từ snapshot, tạo reference không trùng và URL VNPay đã ký. |
| `GET` | `/api/v1/public/bookings/{bookingId}/status` | Booking token | `bookingNumber`, `bookingStatus`, `paymentStatus`, `amount`, `expiresAt` | FE dùng sau redirect/polling; chỉ trả thông tin tối thiểu. |

Ví dụ tạo hold:

```http
POST /api/v1/public/holds
Idempotency-Key: 4ca4b912-c218-4720-9a79-497e6c359ed8
Content-Type: application/json

{"hotelRoomTypeId":12,"checkIn":"2026-10-10","checkOut":"2026-10-12","adults":2,"children":0,"infants":0}
```

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "holdId": "57f9b0e5-70d6-4800-8e5e-31b84d868272",
    "expiresAt": "2026-09-24T09:15:00Z",
    "currency": "VND",
    "totalAmount": "3200000.00",
    "nights": [
      {"stayDate":"2026-10-10","basePrice":"1400000.00","discountAmount":"0.00","surchargeAmount":"0.00","serviceFeeAmount":"100000.00","vatAmount":"100000.00","netPrice":"1600000.00"},
      {"stayDate":"2026-10-11","basePrice":"1400000.00","discountAmount":"0.00","surchargeAmount":"0.00","serviceFeeAmount":"100000.00","vatAmount":"100000.00","netPrice":"1600000.00"}
    ]
  }
}
```

Số tiền ví dụ chỉ minh họa cấu trúc, **không phải thuế suất/phí mặc định**. Giá trị thực lấy từ pricing và quy tắc thuế hợp lệ. Quote và hold cùng cấu trúc tiền để FE hiển thị nhất quán.

Ví dụ tạo booking:

```json
{
  "holdId": "57f9b0e5-70d6-4800-8e5e-31b84d868272",
  "contact": {"phone": "0900000000", "email": "guest@example.com"}
}
```

`contact` ánh xạ sang `guests.phone/email` hiện có. Không gộp người đặt theo số điện thoại nếu chưa có chính sách xác minh. Họ tên và giấy tờ của **người lưu trú thực tế** thu tại check-in, ghi vào `booking_guests` theo ràng buộc schema.

### 3.2. VNPay sandbox

| Method | Path | Caller | Hành vi |
|---|---|---|---|
| `GET` | `/api/v1/payment-callbacks/vnpay/ipn` | VNPay server | Xác thực checksum, `vnp_TmnCode`, `vnp_TxnRef`, amount và cả response/transaction status; xử lý idempotent rồi trả `RspCode/Message` theo VNPay. Public ở mức mạng nhưng không tin request chưa xác thực chữ ký. |
| FE route | `/payment/vnpay/return` | Trình duyệt sau redirect | Hiển thị trạng thái tạm và gọi `GET .../bookings/{id}/status`; **không** đánh dấu thanh toán thành công theo query string của return URL. |

Luồng: tạo `payments(PENDING, VNPAY)` → redirect tới VNPay → VNPay gọi IPN → backend khóa payment/booking/hold, kiểm tra reference và amount, cập nhật payment/transaction và booking trong một transaction → FE đọc trạng thái. IPN trùng lặp phải trả kết quả idempotent. Nếu IPN báo thành công nhưng hold đã hết hoặc booking không còn xác nhận được, payment được ghi nhận để đối soát, booking chuyển `PAYMENT_REVIEW`; không cấp phòng tự động. Nếu IPN chưa đến, FE tiếp tục hiển thị `PENDING_PAYMENT`, không suy luận từ return URL. Job đối soát dùng API truy vấn của VNPay là bước tăng cường sau MVP nhưng cần ít nhất cảnh báo vận hành cho giao dịch treo.

Theo [tài liệu VNPay](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html), `vnp_Amount` là số tiền VND nhân 100, `vnp_TxnRef` là mã giao dịch riêng, Return URL dùng cho trình duyệt còn IPN để merchant cập nhật kết quả. IPN phải nhận được từ internet khi kiểm thử end-to-end; localhost riêng máy không đủ. Khóa bí mật VNPay chỉ ở server/secret store.

### 3.3. Nhân viên vận hành

| Method | Path | Quyền dự kiến | Input/đầu ra chính |
|---|---|---|---|
| `GET` | `/api/v1/me` | Staff JWT | `staffId`, permission và hotel scope hiệu lực cho UI. |
| `GET` | `/api/v1/hotels` | `hotel:read` theo scope | Danh sách cơ sở nhân viên được truy cập. |
| `GET` | `/api/v1/hotels/{hotelId}/rooms` | `room:read` | `roomId,roomNumber,roomTypeId,currentStatus`; phân trang/lọc. |
| `GET` | `/api/v1/bookings` | `booking:read` | Filter `hotelId,status,checkInFrom,checkInTo`; danh sách phân trang, liên hệ được che theo quyền. |
| `GET` | `/api/v1/bookings/{bookingId}` | `booking:read` + scope | Header, booking detail/room, giá snapshot, thanh toán đã ghi nhận; không trả JPA entity. |
| `POST` | `/api/v1/bookings/{bookingId}/room-assignments` | `frontdesk:assign` | `{bookingRoomId,roomId}`; phòng cùng cơ sở/hạng, không trùng slot lưu trú. `201`. |
| `POST` | `/api/v1/bookings/{bookingId}/check-ins` | `frontdesk:check-in` | `{bookingRoomId,occupants:[...]}`; đủ giấy tờ người lớn, phòng sẵn sàng, booking đã xác nhận. `201`. |
| `POST` | `/api/v1/bookings/{bookingId}/check-outs` | `frontdesk:check-out` | `{bookingRoomId}`; kiểm tra folio/số dư theo chính sách, cập nhật stay và trạng thái phòng. `201`. |
| `POST` | `/api/v1/hotels/{hotelId}/rooms/{roomId}/housekeeping-transitions` | `housekeeping:update` | `{toStatus:"CLEANING"}` hoặc `READY`; kiểm tra chuyển trạng thái hợp lệ, audit. `201`. |
| `GET` | `/api/v1/bookings/{bookingId}/folio` | `billing:read` | Tiền phòng snapshot, charge, payment success, số dư; chỉ là folio nội bộ. |
| `POST` | `/api/v1/bookings/{bookingId}/cancellations` | `booking:cancel` | Lý do, chính sách hủy, giải phóng tồn nếu hợp lệ, audit. `201`. |

Ví dụ check-in chỉ minh họa dữ liệu tối thiểu; các trường tên/giấy tờ đã có trong `booking_guests`:

```json
{
  "bookingRoomId": "501",
  "occupants": [
    {"firstName":"An","lastName":"Nguyễn","guestType":"ADULT","identityType":"CCCD","identityNumber":"************"}
  ]
}
```

Giá trị giấy tờ trong ví dụ đã che; request thật chứa giá trị hợp lệ qua HTTPS và phải tránh log body. Phạm vi quyền đọc PII cần định nghĩa chi tiết khi triển khai IAM. Backend không nhận `status=CHECKED_IN` trần từ FE; check-in là một nghiệp vụ có điều kiện.

### 3.4. Chatbot RAG

| Method | Path | Input | Output | Giới hạn |
|---|---|---|---|---|
| `POST` | `/api/v1/public/ai/chat` | `{message,hotelId?}` | `{answer,sources:[{sourceId,title}],suggestedRoomTypeIds,asOf}` | RAG chỉ trên tài liệu public đã duyệt; truy vấn phòng/giá qua service nghiệp vụ, không từ vector store. Rate limit; không tiết lộ booking/PII. |
| `POST` | `/api/v1/ai/chat` | Staff JWT, `{message,hotelId?}` | Cùng cấu trúc, chỉ nguồn nội bộ theo permission/scope | Không cho model tự thực hiện booking, payment hay sửa dữ liệu. |

Nếu câu hỏi cần giá/phòng trống, chatbot yêu cầu ngày và số khách, gọi service `inventory/pricing`, rồi trả `suggestedRoomTypeIds`; FE gọi quote chính thức để hiển thị tiền. `asOf` là thời điểm dữ liệu được đọc, không xác nhận còn phòng ở thời điểm đặt. Nếu không có nguồn phù hợp, trả lời không đủ thông tin thay vì tự dựng chính sách/thuế. Dữ liệu RAG phải có `sourceId`, phiên bản, phạm vi public/staff và quy trình duyệt; cấu trúc lưu trữ là migration mới, không có trong schema hiện tại.

## 4. Luồng trạng thái và bất biến

```text
Search → Quote → Hold(ACTIVE)
                   → Booking(PENDING_PAYMENT) + hold gắn booking
                   → Payment(PENDING) → VNPay IPN
                                      ├─ hợp lệ, hold còn hiệu lực → Payment(SUCCESS) → Booking(CONFIRMED)
                                      ├─ thất bại → Payment(FAILED); có thể thử lại trước khi hết hạn
                                      └─ thành công muộn/hết phòng → Payment(SUCCESS) → Booking(PAYMENT_REVIEW)
Booking(CONFIRMED) → room assignment → check-in → check-out
```

- Hold và booking chờ thanh toán cùng chiếm tồn một lần; không cộng đôi. Job hết hạn giải phóng hold/booking chờ thanh toán theo transaction có khóa; payment URL cũng không được sống lâu hơn deadline đã công bố. Nếu cổng vẫn gửi thành công muộn, chuyển review như trên.
- Availability cho khoảng ngày là giao của phòng khả dụng từng đêm. Kiểm tra capacity của `hotel_room_types` và phòng vật lý đủ toàn bộ kỳ lưu trú; không chỉ dựa vào `total_quantity` hoặc `room_availability.available_count` nếu seed/counter chưa đồng bộ.
- Khi xác nhận booking, snapshot `booking_daily_rates` và `bookings.total_amount` không bị sửa theo giá hiện hành. Hủy/no-show/refund là nghiệp vụ riêng, không xóa chứng từ tài chính.
- Physical room assignment độc lập với bán hạng phòng. Kiểm tra `room_slots` và maintenance; một phòng không có hai booking chồng đêm.
- Trạng thái booking, booking room/stay, payment, housekeeping và room operation không gộp thành một enum. Sau check-out, phòng chuyển DIRTY/CLEANING rồi READY theo housekeeping.

## 5. Migration và thay đổi nền tảng bắt buộc trước khi code luồng public

| Phần hiện có | Thiếu/khác contract | Thay đổi đề xuất |
|---|---|---|
| `bookings.status` chỉ `CONFIRMED/CANCELLED/NO_SHOW` | Không chứa checkout chưa thanh toán hoặc payment về muộn | Thêm `PENDING_PAYMENT`, `EXPIRED`, `PAYMENT_REVIEW`; chốt transition và job hết hạn. |
| `room_availability.locked_until` ở mức dòng tổng hợp từng ngày | Không biểu diễn nhiều hold độc lập | Bảng hold + hold nights/quantity/expiry/booking link; cập nhật counter/lock theo transaction, index và job giải phóng. |
| Không có idempotency store | Retry POST có thể tạo booking/payment lặp | Bảng key + operation + request hash + response/reference, unique constraint và thời hạn phù hợp. Response chứa booking access token chỉ được lưu dạng mã hóa trong thời hạn retry; token xác thực vẫn chỉ lưu hash. |
| `payments.transaction_reference` chưa unique | IPN/reference trùng có thể ghi hai lần | Unique theo provider/reference, lưu mã giao dịch VNPay và transaction outcome để đối soát. |
| Không có booking access token | Public không thể xem trạng thái an toàn bằng ID | Lưu hash token, scope `status/payment-attempt`, expiry và cơ chế xoay/thu hồi. |
| `room_instances.current_status` chưa có `DIRTY` | Check-out/housekeeping không cùng ngôn ngữ với `room_slots` | Thêm `DIRTY` hoặc mô hình trạng thái phòng thống nhất; xác định transition trước khi mở endpoint. |
| Chưa có RAG knowledge/vector schema | Không thể lưu nguồn, phiên bản, quyền truy cập | Tạo tài liệu/chunk/embedding metadata hoặc kho tri thức có version; phân quyền public/staff. Redis chỉ cache/TTL, không là nguồn dữ liệu booking/payment. |
| `SecurityConfig` hiện permit `/api/public/**` | Không khớp `/api/v1/public/**`/IPN | Cập nhật security matchers, xác thực JWT và application IAM cho staff; rate limit/callback signature. |
| `ErrorCode` hiện chỉ lỗi chung | FE không xử lý ổn định lỗi nghiệp vụ | Thêm mã số, HTTP mapping, OpenAPI schemas và global exception handler. |

`room_instances` seed và `hotel_room_types.total_quantity` phải được đối soát trước khi dùng public availability. Không xem schema hiện có là đã hỗ trợ quy trình hold/payment chỉ vì đã có bảng `room_availability` và `payments`.

## 6. Giai đoạn sau MVP

| Nhóm | API mở rộng dự kiến | Điều kiện |
|---|---|---|
| Quản trị chuỗi | CRUD `/regions`, `/hotels`, `/staff`, `/roles`, `/scope-assignments` | Thiết kế IAM nhiều vai trò/phạm vi thay cho một role/scope hiện tại. |
| Hàng phòng/giá | CRUD hạng phòng, phòng vật lý, maintenance block, pricing rule, lịch giá | Audit, versioning, policy giá và kiểm tra ảnh hưởng booking. |
| Booking mở rộng | Nhiều phòng, group/corporate, sửa ngày, no-show, danh sách chờ | Mô hình inventory và reprice rõ ràng. |
| Billing/payment | Phụ thu dịch vụ, tiền cọc, thu nhiều lần, refund, MoMo | State machine và đối soát riêng; không suy ra hóa đơn điện tử từ bảng `invoices`. |
| Reporting/audit | `/reports/occupancy`, `/reports/revenue`, `/audit-events` | Scope theo cơ sở/vùng/chuỗi, nguồn số liệu và timezone thống nhất. |
| AI quản trị | `/ai/knowledge-documents` upload/duyệt/reindex, đánh giá câu trả lời | Chỉ staff có quyền, lưu provenance và version. |

## 7. Kiểm thử hợp đồng tối thiểu trước khi nối FE

1. Hai request hold đồng thời cho phòng cuối: tối đa một request thành công; không bán vượt tồn dù nằm trên hai instance backend.
2. Cùng `Idempotency-Key` retry tạo booking/payment: trả cùng tài nguyên; payload khác trả `409`.
3. IPN success lặp, IPN sai checksum/amount/reference, IPN sau expiry: không ghi payment/booking trùng và không tự xác nhận sai.
4. Return URL báo thành công nhưng IPN chưa xác nhận: UI vẫn `PENDING_PAYMENT`.
5. Staff khác hotel scope không đọc booking/folio hay chuyển trạng thái phòng của cơ sở đó.
6. Chatbot không bịa giá từ nội dung RAG, không lộ nguồn staff cho public và không thao tác booking/payment.

**Nguồn đối chiếu:** schema [`database/01-init-schema.sql`](../database/01-init-schema.sql), quy ước [API trong repo](../.agents/rules/api-design-standards.md), [VNPay PAY/IPN](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html). Các endpoint và migration trên là **đề xuất thiết kế**; chưa có controller triển khai trong source hiện tại.
