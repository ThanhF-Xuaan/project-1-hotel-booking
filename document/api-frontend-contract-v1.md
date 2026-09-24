# Contract API–frontend v1 (bản đề xuất để triển khai MVP)

Ngày lập: 22/09/2026. **Đây là contract đề xuất cho implementation sắp tới; source hiện chưa có các endpoint dưới đây.** Khi tạo SaveRequest DTO, backend phải đối chiếu với các bảng trong `database/01-init-schema.sql`. Nếu một field không có nơi lưu hoặc không phục vụ quy trình, không cho frontend gửi nó.

## Quy ước chung

- Base path `/api`. Mọi response thành công dùng envelope hiện có: `{ "code": 1000, "message": "...", "result": ... }`. Lỗi dùng `code`, `message` và HTTP status phù hợp; code cụ thể cần chốt trong `ErrorCode` trước khi frontend bắt đầu xử lý theo code.
- Ngày lưu trú là `YYYY-MM-DD`; check-out không tính đêm. Timestamp có timezone. Amount API truyền dưới dạng chuỗi decimal (`"2500000.00"`), không dùng floating point ở frontend.
- `SMALLINT`/`INT` ID là JSON number; `BIGINT` ID là chuỗi decimal. Frontend không tự tạo booking number/invoice number hoặc trạng thái giao dịch.
- Pagination cho danh sách staff chốt một lần theo `result.items`, `result.page`, `result.size`, `result.totalElements`. Backend không trả JPA entity trực tiếp.
- Frontend gửi JWT cho staff endpoints. Backend kiểm tra role/scope theo hotel; không tin hotelId từ URL/body để quyết định quyền.
- Các ví dụ dưới đây là payload tối thiểu. Không coi field trong SQL là field cần lộ ra ngoài API.

## Public read: Backend 1

| Endpoint | Input tối thiểu | `result` tối thiểu | Nguồn SQL |
|---|---|---|---|
| `GET /api/public/hotels` | Không | `[{ id, name, address }]` | `hotels` đang ACTIVE/chưa xóa |
| `GET /api/public/hotels/{hotelId}/room-types?checkIn=...&checkOut=...&adults=...&children=...&infants=...` | hotelId, hai ngày, số khách | `[{ id: hotelRoomTypeId, roomTypeName, maxAdults, maxChildren, maxInfants, maxTotalGuests, availableRooms, quotedTotal }]` | `hotel_room_types`, `room_types`, `room_instances`, `room_slots`, pricing/VAT rules |
| `GET /api/public/room-types/{hotelRoomTypeId}/quote?checkIn=...&checkOut=...&adults=...&children=...&infants=...` | Hạng phòng/đợt lưu trú | `{ hotelRoomTypeId, checkInDate, checkOutDate, availableRooms, currency: "VND", totalAmount, nights: [{ stayDate, basePrice, discountAmount, surchargeAmount, serviceFeeAmount, vatPercent, vatAmount, netPrice }] }` | Pricing engine + `booking_daily_rates` làm mẫu snapshot; không ghi DB |

`quotedTotal`, `totalAmount` và các số tiền trong `nights` là decimal string. `availableRooms` tính theo phòng vật lý đủ **mọi** đêm, không dùng riêng `hotel_room_types.total_quantity` hay counter chưa đồng bộ. Nếu thiếu VAT hoặc không thể tính giá nhất quán, trả lỗi rõ và không trả giá giả định. Public API không trả danh tính khách, tên staff hoặc số phòng vật lý.

## Booking staff: Backend 2

`POST /api/bookings` cần JWT staff và quyền `CREATE` trên resource `BOOKING` tại hotel. MVP chỉ nhận booking FIT, một hạng phòng, số lượng một phòng; các trường SQL `booking_type=FIT`, `quantity=1`, `company_id=NULL` do server đặt. Client gửi `Idempotency-Key: <UUID>` cho mỗi lần bấm xác nhận và giữ nguyên key khi retry cùng yêu cầu. Request:

```json
{
  "hotelId": 1,
  "hotelRoomTypeId": 1,
  "checkInDate": "2026-10-10",
  "checkOutDate": "2026-10-12",
  "adults": 2,
  "children": 0,
  "infants": 0,
  "guest": { "phone": "0900000000", "email": "guest@example.com" }
}
```

`guest.phone` lưu ở `guests.phone`, email tùy chọn ở `guests.email`. Nếu hệ thống tra ra guest trùng thì BE2 định nghĩa chính sách gộp rõ; không tự gộp theo số điện thoại nếu có thể gây lộ lịch sử giữa người dùng. `guest_count` do backend tính từ nhóm tuổi theo policy. Tên/giấy tờ người lưu trú được thu khi check-in và ghi vào `booking_guests`, không invent field của booking header. Không nhận `price`, `VAT`, `bookingNumber`, `roomInstanceId`, `status` hay `paymentStatus` từ client.

Response thành công HTTP 201:

```json
{
  "code": 1000,
  "result": {
    "bookingId": "101",
    "bookingNumber": "BKG-...",
    "hotelId": 1,
    "hotelRoomTypeId": 1,
    "roomNumber": "101",
    "checkInDate": "2026-10-10",
    "checkOutDate": "2026-10-12",
    "status": "CONFIRMED",
    "currency": "VND",
    "totalAmount": "5000000.00"
  }
}
```

Số tiền và phòng trong response là kết quả transaction cuối cùng, có thể khác quote đọc trước đó. `roomNumber` lấy từ `room_instances`. Booking number do backend tạo **có truyền giá trị** để tránh trigger LPAD hiện tại cắt sequence sau 9.999; dùng mã không trùng đủ chỗ trong `VARCHAR(50)` và để UNIQUE constraint chốt cuối. Có thể dùng `BKG-<UUID từ Idempotency-Key>` để retry cùng key đọc lại booking cũ; nếu cùng key nhưng payload khác thì trả 409. BE2 phải kiểm tra payload hiện có trước khi trả lại response, vì schema không có cột request hash riêng. Không tự retry POST với key mới khi chưa biết lần trước đã commit hay chưa.

Các endpoint còn lại:

| Endpoint | Input | `result` chính | Ghi chú |
|---|---|---|---|
| `GET /api/bookings?hotelId=...&status=...&page=0&size=20` | Staff JWT, filter | `items` gồm ID, number, guest phone được che phần giữa, ngày, status, total | BE3 scope check; tránh N+1 |
| `GET /api/bookings/{bookingId}` | Staff JWT | Booking header, một detail, room assignment, daily rates, status, total | Đọc giá snapshot, không lấy base_price hiện thời làm giá lịch sử |
| `POST /api/bookings/{bookingId}/cancel` | Staff JWT; lý do có thể chỉ nằm ở audit log vì schema không có cột reason | Booking status `CANCELLED` | Transaction giải phóng slot; không xóa booking/financial history |

Check-in/check-out và nhiều phòng trong một booking được thêm sau MVP, dùng status có sẵn (`EXPECTED`, `CHECKED_IN`, `CHECKED_OUT`). Nếu đưa vào MVP, contract occupant phải tuân CHECK: adult có identity type và number; không gọi check-in chỉ bằng nút status.

## Payment staff: Backend 3

| Endpoint | Input | `result` | Quy tắc |
|---|---|---|---|
| `GET /api/bookings/{bookingId}/payments` | Staff JWT | `{ bookingId, totalAmount, paidAmount, remainingAmount, payments: [{ paymentId, method, purpose, status, amount, paidAt }] }` | Tổng do backend tính từ booking snapshot và payment SUCCESS; chỉ đúng hotel scope |
| `POST /api/bookings/{bookingId}/payments/cash` | Staff JWT; header `Idempotency-Key: <UUID>`; body rỗng | `{ paymentId, status: "SUCCESS", amount, remainingAmount }` | Backend khóa booking, kiểm tra key cũ, tính số còn phải thu, ghi `payments` + `transactions` cùng transaction |

MVP chỉ thu CASH toàn bộ số còn lại. `payment_method=CASH`, `payment_purpose=FULL_PAYMENT`, `status=SUCCESS`; `transaction_type=PAYMENT`. Backend ánh xạ idempotency key vào `payments.transaction_reference` theo quy ước cố định, kiểm tra lại dưới khóa booking trước khi ghi. Cùng key trả cùng kết quả. Do cột reference chưa UNIQUE, đây là bảo vệ **trong phạm vi service và transaction hiện tại**; mọi writer thanh toán phải dùng cùng service. Để mở gateway/callback bất đồng bộ hoặc nhiều writer, BE3 phải trình một migration payment riêng có unique constraint phù hợp sau khi contract event/provider được xác định.

Không có endpoint cho client nhập `amount` hoặc tự đặt SUCCESS. Refund, bank transfer tự động, VNPAY/MOMO và invoice phát hành ra ngoài nằm ngoài MVP. `invoices` trong schema có thể dùng sau, nhưng chưa tự sinh hóa đơn pháp lý chỉ vì payment đã thành công.

## Chatbot: người AI

`POST /api/public/chat` (thêm rate limit), request `{ "message": "...", "context": { "hotelId": 1 } }`; `hotelId` tùy chọn, `message` giới hạn độ dài và không chứa token. Response `{ "answer": "...", "suggestedRoomTypeIds": [1, 2], "asOf": "<timestamp>" }`. `suggestedRoomTypeIds` chỉ được lấy từ public room/quote API; không đưa price vào response chat nếu model tự suy ra. UI lấy giá bằng API quote khi mở room card. `asOf` là thời điểm hệ thống trả lời, không phải giá chốt. Chat history MVP nằm trong state trình duyệt hoặc Redis TTL nếu có nhu cầu phiên; không sửa PostgreSQL.

Chatbot không truy vấn DB/JPA, không gọi API tạo booking/thanh toán, không trả trạng thái booking của khách từ public channel. FAQ chỉ dùng nội dung được duyệt hoặc dữ liệu public từ backend. Không có provider/model cố định trong contract; adapter giữ khóa API phía server.

## HTTP/error contract cần thống nhất ngày đầu

| HTTP | Trường hợp | Frontend |
|---:|---|---|
| 400 | Ngày/số khách/request sai | Hiển thị lỗi ngay cạnh field |
| 401 | Chưa xác thực staff | Chuyển tới login |
| 403 | Có JWT nhưng không có quyền/hotel scope | Trang không có quyền |
| 404 | Booking/hotel/type không tồn tại hoặc không được phép lộ | Trang không tìm thấy |
| 409 | Phòng vừa hết, giá cần xác nhận lại, booking đã hủy, payment lặp khác key hoặc xung đột ghi | Refetch quote/detail, không tự retry POST với key mới |
| 422 | Không thể báo giá do thiếu VAT/policy hoặc quy tắc không xác định | Giải thích chưa thể đặt ở ngày đã chọn; không hiển thị giá mặc định |
| 500 | Lỗi backend/DB ngoài dự kiến | Retry an toàn, giữ idempotency key nếu đang thanh toán |

Backend cần chốt mã lỗi ổn định trong `ErrorCode`; bảng trên là semantics thiết kế, chưa phải behavior hiện có. UI không parse exception message hoặc SQL error để đưa ra quyết định.
