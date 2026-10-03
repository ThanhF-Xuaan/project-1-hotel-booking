# 🔁 LUỒNG HOẠT ĐỘNG & CÁC TRƯỜNG HỢP CỦA 5 PHƯƠNG THỨC THANH TOÁN

> **Đối tượng:** SV năm 4 vừa học vừa code (Member 3 — T06 Payment)
> **Mục đích:** mô tả **luồng step-by-step** + **mọi trường hợp có thể xảy ra** cho từng phương thức,
> để tiện đối chiếu khi test / sửa lỗi / phỏng vấn.
> **Nguồn đối chiếu:** code thật trong repo (`PaymentServiceImpl`, `PaymentController`,
> `PaymentCallbackController`, `VnPayGateway`, `MoMoGateway`, `FolioPaymentPage`, `PaymentResultPage`)
> — **không đoán**.
> **Liên quan:** `docs/payment-methods-5-phuong-thuc.md` (kế hoạch + kết quả Giai đoạn A→D)

---

## 1. MÁY TRẠNG THÁI — ÁP DỤNG CHUNG CHO MỌI PHƯƠNG THỨC

```
                 ┌──────── create (CASH / CREDIT_CARD / DEBIT_CARD) ─────────┐
                 │  (SUCCESS ngay, paidAt = now, có Transaction)             │
                 ▼                                                           │
            ┌─────────┐                                                      │
   ┌───────►│ SUCCESS │── refund (refundAmount > 0, ≤ tổng) ──► REFUNDED     │
   │        └────▲────┘                                                      │
   │             │                                                           │
 create (BANK)   │ completePayment (lễ tân xác nhận)                         │
   │             │  hoặc webhook gateway báo OK                              │
   │        ┌────┴────┐                                                      │
   └────────┤ PENDING │───────── cancelPayment ──────► CANCELLED (hết)       │
            └────┬────┘   (chỉ hủy được PENDING — 8006 nếu khác)            │
                 │                                                           │
                 └── webhook gateway báo THẤT BẠI ──► FAILED (hết)          │
```

| Trạng thái | Ý nghĩa | Ai đưa vào | Ra bằng cách nào |
| :--- | :--- | :--- | :--- |
| `PENDING` | Chờ xác nhận (bank) **hoặc** chờ khách trả trên cổng (gateway, TTL lock 10') | create bank / tạo phiên gateway | `complete` / `cancel` / webhook |
| `SUCCESS` | Tiền đã xác nhận vào | create cash-thẻ / complete / webhook | `refund` |
| `FAILED` | Gateway báo thất bại (khách hủy ở cổng, hết hạn mức…) | webhook | — (hết, tạo phiên mới) |
| `CANCELLED` | Đã hủy phiên (flow lỗi §6 hoặc lễ tân hủy) | `cancelPayment` | — (hết) |
| `REFUNDED` | Đã hoàn tiền | `refundPayment` | — (hết) |

---

## 2. BẢNG ENDPOINT & PHÂN QUYỀN (chung)

### 2.1 Base URL & tài nguyên liên quan

| Tài nguyên | URL |
| :--- | :--- |
| **Base API** | `http://localhost:8080/api/v1` |
| **Swagger UI** (test thử, không cần JWT) | `http://localhost:8080/swagger-ui.html` |
| **OpenAPI JSON** | `http://localhost:8080/v3/api-docs` |
| **Lấy JWT** (Keycloak, bắt buộc cho mọi endpoint trừ `/public/**`) | `POST http://localhost:8081/realms/hotel-realm/protocol/openid-connect/token` |
| **FE trang kết quả** | `http://localhost:3000/payment/result` |
| **FE thu ngân** | `http://localhost:3000/finance/folios` |

> ⚠️ Repo đang chạy **HTTP** trên localhost (dev) — không có TLS. Khi lên production phải đặt
> sau reverse-proxy HTTPS, đồng thời đổi `VNPAY_RETURN_URL` / `VNPAY_FE_RESULT_URL` /
> `MOMO_IPN_URL` / `MOMO_REDIRECT_URL` sang domain HTTPS (gateway bắt buộc).

**Lấy token (dùng cho mọi lệnh ví dụ bên dưới):**

```bash
curl -s -X POST http://localhost:8081/realms/hotel-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=hotel-frontend&grant_type=password&username=admin&password=123456" \
  | jq -r .access_token        # → dán vào header  Authorization: Bearer <token>
```

### 2.2 Danh sách đầy đủ URL (payment API)

**Nhóm 1 — CRUD thanh toán (cần JWT + role):**

| # | Full URL | Method | Vai trò | HTTP khi OK |
| :-- | :--- | :--- | :--- | :--- |
| 1 | `http://localhost:8080/api/v1/payments/create` | POST | ADMIN · PROPERTY_MANAGER · RECEPTIONIST · CUSTOMER | **201** |
| 2 | `http://localhost:8080/api/v1/payments/filter` | POST | ADMIN · REGION_MANAGER · PROPERTY_MANAGER · RECEPTIONIST | 200 |
| 3 | `http://localhost:8080/api/v1/payments/{id}` | GET | ADMIN · REGION · PROPERTY · RECEPTION | 200 |
| 4 | `http://localhost:8080/api/v1/payments/booking/{bookingId}` | GET | 4 role trên + CUSTOMER | 200 |
| 5 | `http://localhost:8080/api/v1/payments/{id}/complete` | POST | ADMIN · PROPERTY · RECEPTION | 200 |
| 6 | `http://localhost:8080/api/v1/payments/{id}/cancel` | POST | ADMIN · PROPERTY · RECEPTION | 200 |
| 7 | `http://localhost:8080/api/v1/payments/{id}/refund?refundAmount=…&reason=…` | POST | **chỉ** ADMIN · PROPERTY_MANAGER | 200 |

**Nhóm 2 — Tạo phiên gateway (cần JWT + role):**

| # | Full URL | Method | HTTP khi OK |
| :-- | :--- | :--- | :--- |
| 8 | `http://localhost:8080/api/v1/payments/vnpay/create` | POST | **201** → `{paymentUrl, paymentId, txnRef, expiresAt}` |
| 9 | `http://localhost:8080/api/v1/payments/momo/create` | POST | **201** → `{paymentUrl, paymentId, txnRef, expiresAt}` |

**Nhóm 3 — Webhook / redirect công khai (`/public/**`, permitAll — KHÔNG JWT):**

| # | Full URL | Method | Ai gọi | HTTP khi OK |
| :-- | :--- | :--- | :--- | :--- |
| 10 | `http://localhost:8080/api/v1/public/payments/vnpay/ipn` | GET | **VNPay server** (server-để-server, có retry) | 200 `{RspCode:"00"}` |
| 11 | `http://localhost:8080/api/v1/public/payments/vnpay/return` | GET | **Trình duyệt khách** sau khi trả tiền | **302** → `http://localhost:3000/payment/result?…` |
| 12 | `http://localhost:8080/api/v1/public/payments/momo/callback` | POST | **MoMo server** | 200 `{statusCode:0}` |

**Nhóm 4 — URL phía gateway (hệ thống GỌI ra / gateway gọi vào — cấu hình trong `.env`):**

| URL | Giá trị dev | Dùng để |
| :--- | :--- | :--- |
| VNPay sandbox endpoint | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` | BE **gọi ra** (dựng paymentUrl) |
| VNPay `vnp_ReturnUrl` | `http://localhost:8080/api/v1/public/payments/vnpay/return` | VNPay redirect trình duyệt về (số 11) |
| VNPay IPN (notify) | `http://localhost:8080/api/v1/public/payments/vnpay/ipn` | VNPay gọi báo server (số 10) — **cần ngrok khi test thật** |
| MoMo sandbox endpoint | `https://test-payment.momo.vn/v2/gateway/api/create` | BE **gọi ra** lấy payUrl |
| MoMo `ipnUrl` | `http://localhost:8080/api/v1/public/payments/momo/callback` | MoMo gọi báo server (số 12) — **cần ngrok khi test thật** |
| MoMo `redirectUrl` | `http://localhost:3000/payment/result` | MoMo redirect trình duyệt về FE |

### 2.3 Ví dụ gọi thật (PowerShell / curl)

```powershell
# 0) Lấy JWT
$tok = (Invoke-RestMethod -Uri "http://localhost:8081/realms/hotel-realm/protocol/openid-connect/token" `
  -Method Post -Body @{client_id="hotel-frontend";grant_type="password";username="admin";password="123456"}).access_token
$H = @{Authorization="Bearer $tok"}

# 1) CASH — 201 SUCCESS ngay
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payments/create" -Method Post -Headers $H `
  -ContentType "application/json" `
  -Body '{"bookingId":2,"totalAmount":200000,"paymentPurpose":"DEPOSIT","paymentMethod":"CASH","transactionReference":"PT-001"}'

# 2) BANK thiếu mã → 400 {"code":8007}   (đối chiếu lỗi §8)
#    Body: {... "paymentMethod":"BANK_TRANSFER","transactionReference":""}

# 3) VNPay create — 201 + paymentUrl
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payments/vnpay/create" -Method Post -Headers $H `
  -ContentType "application/json" -Body '{"bookingId":2,"method":"VNPAY"}'

# 4) Tạo lần 2 khi lock còn hạn → 409 {"code":8005}

# 5) Hủy phiên dở (flow lỗi §6) — 200 CANCELLED + mở khóa Redis
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payments/9/cancel" -Method Post -Headers $H

# 6) Xem danh sách thanh toán của đơn — 200
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payments/booking/2" -Method Get -Headers $H
```

### 2.4 Bảng endpoint tóm tắt (nhìn nhanh)

| Endpoint | Method | Ai gọi | HTTP khi OK |
| :--- | :--- | :--- | :--- |
| `/api/v1/payments/create` | POST | ADMIN · PROPERTY_MANAGER · RECEPTIONIST · CUSTOMER | **201** |
| `/api/v1/payments/vnpay/create` | POST | 4 role trên | **201** |
| `/api/v1/payments/momo/create` | POST | 4 role trên | **201** |
| `/api/v1/payments/{id}` | GET | ADMIN · REGION_MANAGER · PROPERTY_MANAGER · RECEPTIONIST | 200 |
| `/api/v1/payments/booking/{bookingId}` | GET | 4 role trên + CUSTOMER | 200 |
| `/api/v1/payments/filter` | POST | ADMIN · REGION · PROPERTY · RECEPTION | 200 |
| `/api/v1/payments/{id}/complete` | POST | ADMIN · PROPERTY · RECEPTION | 200 |
| `/api/v1/payments/{id}/cancel` | POST | ADMIN · PROPERTY · RECEPTION | 200 |
| `/api/v1/payments/{id}/refund` | POST | **chỉ** ADMIN · PROPERTY_MANAGER | 200 |
| `/api/v1/public/payments/vnpay/ipn` | GET | **VNPay server** (không JWT) | 200 |
| `/api/v1/public/payments/vnpay/return` | GET | **trình duyệt khách** (không JWT) | **302** → FE |
| `/api/v1/public/payments/momo/callback` | POST | **MoMo server** (không JWT) | 200 |

> Không JWT → **401** · có JWT nhưng thiếu role → **403** · sai body → **400/1002** · không thấy id → **404/8001**.

---

## 3. 💵 CASH — tiền mặt

### 3.1 Luồng

```text
① Khách đưa tiền mặt tại quầy
② Lễ tân mở Folio → "Thu tiền" → method = CASH (ô phiếu thu TÙY CHỌN)
③ FE validate: amount > 0  →  POST /payments/create
      { bookingId, totalAmount, paymentMethod: "CASH", transactionReference?: "PT-..." }
④ BE (createPayment):
      - booking không tồn tại?                → 404 7001
      - amount null hoặc ≤ 0?                  → 400 8002
      - reference: trim() — CASH KHÔNG bắt buộc
      - isImmediateSuccess = true              → status = SUCCESS, paidAt = now
      - lưu Payment + tạo Transaction (sổ cái, status COMPLETED)
        · có phiếu thu  → referenceCode = số phiếu thu
        · không có      → referenceCode = TX-<timestamp>   (fallback B3)
⑤ FE nhận 201 → danh sách giao dịch hiện chip SUCCESS → số "Còn phải thu" giảm
```

### 3.2 Các trường hợp

| # | Trường hợp | Kết quả | Code/HTTP |
| :-- | :--- | :--- | :--- |
| 1 | Happy path (đủ tiền, có/không phiếu thu) | `SUCCESS` ngay + có dòng `transactions` | **201** |
| 2 | `totalAmount` = 0 / âm / null | "Số tiền thanh toán phải lớn hơn 0" | 400 · **8002** |
| 3 | `bookingId` không tồn tại | "Không tìm thấy đơn đặt phòng" | 404 · **7001** |
| 4 | Không gửi JWT | — | **401** |
| 5 | Role không có quyền (vd HOUSEKEEPING) | — | **403** |
| 6 | FE nhập tay reference cho VNPAY/MOMO qua `/create` | BE **cắt bỏ** reference (webhook tự ghi) | 201 · ref = null |
| 7 | Bấm `complete` trên payment đã SUCCESS | "Thanh toán này đã hoàn tất" | 400 · **8003** |
| 8 | Bấm `cancel` trên payment SUCCESS | "Chỉ hủy được PENDING" | 400 · **8006** |
| 9 | `refund` amount ≤ 0 hoặc > tổng | "Số tiền hoàn không hợp lệ" | 400 · **8002** |
| 10 | `refund` hợp lệ (payment đang SUCCESS) | `SUCCESS → REFUNDED` + Transaction `REFUND` | 200 |
| 11 | Tạo 2 dòng CASH cho cùng booking (thu 2 lần) | **Vẫn tạo được** — không chống trùng cho cash (khách đóng 2 lần thật thì thu 2 lần là đúng) | 201 |

---

## 4. 🏦 BANK_TRANSFER — chuyển khoản ngân hàng

### 4.1 Luồng

```text
① Khách chuyển khoản bằng app NH riêng → có mã giao dịch (VD: FT26...)
② Lễ tân chọn "Chuyển khoản" → BẮT BUỘC nhập mã (FE bắt trước, không đợi BE)
③ POST /payments/create { paymentMethod: "BANK_TRANSFER", transactionReference: "FT26..." }
④ BE: thiếu/blank reference → 400 8007
      reference OK         → status = PENDING (CHƯA tạo Transaction — chưa đối soát xong)
⑤ Lễ tân thấy tiền về tài khoản → bấm "Xác nhận" → POST /payments/{id}/complete
      - nếu đang SUCCESS → 400 8003 (bấm lặp)
      - PENDING → SUCCESS + Transaction (referenceCode = FT26... thật)
⑥ hoặc khách/lễ tân hủy → POST /payments/{id}/cancel → PENDING → CANCELLED
```

### 4.2 Các trường hợp

| # | Trường hợp | Kết quả | Code/HTTP |
| :-- | :--- | :--- | :--- |
| 1 | Happy path (nhập mã GD) | `PENDING` — **chưa** có Transaction | **201** |
| 2 | Thiếu mã / mã toàn space | FE: popup + `focus()` ô (giữ dữ liệu) · BE cũng bắt | 400 · **8007** |
| 3 | `complete` khi PENDING | → `SUCCESS` + Transaction mã FT | 200 |
| 4 | `complete` khi đã SUCCESS | "Thanh toán này đã hoàn tất" | 400 · **8003** |
| 5 | `complete` khi payment đang `CANCELLED`/`FAILED` | ⚠️ **vẫn chuyển SUCCESS** — guard chỉ chặn `SUCCESS`; *edge case của code, đọc kỹ trước khi bấm complete bừa* | 200 |
| 6 | `cancel` khi PENDING | → `CANCELLED` (+ giải phóng Redis lock nếu có) | 200 |
| 7 | `cancel` khi SUCCESS/FAILED/CANCELLED | "Chỉ hủy được giao dịch đang chờ" | 400 · **8006** |
| 8 | `refund` khi chưa SUCCESS | "Chỉ có thể hoàn tiền cho giao dịch đã thanh toán thành công" | 400 · **8002** |
| 9 | id không tồn tại | "Không tìm thấy thanh toán" | 404 · **8001** |

---

## 5. 💳 CREDIT_CARD / DEBIT_CARD — cà thẻ POS

### 5.1 Luồng

```text
① Khách quẹt thẻ trên máy POS → máy in Approval code (VD: AP778899)
② Lễ tân chọn "Thẻ tín dụng" hoặc "Thẻ ghi nợ" → BẮT BUỘC nhập Approval code
③ POST /payments/create { paymentMethod: "CREDIT_CARD"|"DEBIT_CARD",
                          transactionReference: "AP778899" }
④ BE: thiếu mã → 400 8007
      có mã    → tiền ĐÃ quẹt thành công trên POS → SUCCESS ngay (như CASH)
              → Payment SUCCESS + Transaction (referenceCode = approval code)
   ※ Nếu quẹt THẤT BẠI ở POS → không có approval code → không tạo payment
```

### 5.2 Các trường hợp

| # | Trường hợp | Kết quả | Code/HTTP |
| :-- | :--- | :--- | :--- |
| 1 | Happy (nhập Approval code) | `SUCCESS` ngay + Transaction | **201** |
| 2 | Thiếu Approval code | popup 8007 + focus lại ô (FE bắt trước) | 400 · **8007** |
| 3 | Chọn nhầm Credit/Debit | BE nhận đúng enum FE gửi → ghi `payment_method` tương ứng (sửa được ở FE trước khi gửi) | 201 |
| 4 | complete/cancel/refund các ca như CASH | giống §3.2 (8003 / 8006 / 8002) | — |

---

## 6. 🌐 VNPAY QR / CardATM — qua cổng thanh toán

Có **3 cửa** (3 endpoint): **create** (tạo phiên) · **return** (trình duyệt quay về) · **ipn** (server ↔ server).

### 6.1 Luồng

```text
[CREATE]  FE CheckoutModal → POST /payments/vnpay/create { bookingId, method: "VNPAY" }
 ① method không phải VNPAY/MOMO        → 400 1002
 ② booking không tồn tại               → 404 7001
 ③ Redis setIfAbsent  payment:lock:booking:{id}  TTL 10'
      · lock đang giữ (bấm lần 2…)     → 409 8005
 ④ (tùy chọn) paymentId truyền kèm tái sử dụng Payment PENDING:
      · không thấy id                   → 404 8001
      · không phải PENDING              → 400 8006
      · đã có gatewayTxnId              → 409 8005
 ⑤ không truyền paymentId → tạo Payment mới PENDING, tổng tiền = booking.totalAmount
      · totalAmount null/≤0             → 400 8002
 ⑥ ký HMAC-SHA512 → dựng paymentUrl (vnp_Amount ×100, vnp_TmnCode, vnp_ReturnUrl = BE)
      · key sandbox rỗng                → 502 8008  (lock GIẢI PHÓNG → thử lại ngay được)
      · lỗi khác khi dựng URL          → 502 8008  (lock giải phóng)
 ⑦ trả 201 { paymentUrl, paymentId, txnRef (VNP-<ts>-<rand>), expiresAt = now + 10' }

[RETURN]  khách trả tiền xong → VNPay redirect GET /public/payments/vnpay/return?...
 ① verify chữ ký SAI                   → log.warn, KHÔNG đụng DB → 302 FE ?status=FAILED
 ② chữ ký ĐÚG + xử lý tại chỗ:
      · responseCode 00 → SUCCESS + ref = vnp_TransactionNo + Transaction + mở lock
                          → 302 FE ?status=PAID&paymentId&bookingId   (FE poll 3s)
      · responseCode ≠ 00 (khách hủy ở cổng) → FAILED + mở lock
                          → 302 FE ?status=FAILED&paymentId&bookingId
 ③ chữ ký ĐÚG nhưng ĐÃ xử lý qua IPN trước (idempotent)
                          → 302 theo vnp_ResponseCode (PAID/FAILED)

[IPN]     VNPay server gọi GET /public/payments/vnpay/ipn  (không JWT, retry nếu fail)
 ① chữ ký sai                           → 400 {RspCode:"97"} — không ghi DB
 ② txnRef không khớp payment nào       → 404 8001 (VNPay sẽ retry)
 ③ payment ≠ PENDING (xử lý rồi)       → 200 {RspCode:"00", "Already Confirmed"} (idempotent)
 ④ lệch số tiền vs payment             → 400 8002
 ⑤ responseCode 00 → SUCCESS + tự ghi transactionReference + Transaction + MỞ LOCK
    responseCode ≠00 → FAILED (ghi gatewayResponseCode + raw payload) + MỞ LOCK
```

### 6.2 Các trường hợp

| # | Trường hợp | Kết quả | Code/HTTP |
| :-- | :--- | :--- | :--- |
| 1 | Happy: tạo phiên lần đầu | 201 + `paymentUrl` sandbox + `txnRef` + `expiresAt` | **201** |
| 2 | Bấm "Tạo phiên" lần 2 khi lock còn hạn (10') | "Đã có phiên thanh toán đang diễn ra" | 409 · **8005** |
| 3 | `MOMO_*`/`VNPAY_*` key rỗng (chưa cấu hình) | "Chưa cấu hình VNPay/MoMo…" — lock đã giải phóng | 502 · **8008** |
| 4 | Gateway sập / không kết nối được | như trên | 502 · **8008** |
| 5 | Khách **không trả tiền**, để quá 10' | lock hết hạn **tự động** → tạo phiên mới OK · payment cũ vẫn `PENDING` treo (chỉ `cancel` mới dọn — FE có nút "Hủy & thanh toán lại") | — |
| 6 | Khách hủy ngay khi lỗi (§6.1) | `POST /{id}/cancel`: PENDING → CANCELLED + **mở lock** → làm lại từ đầu | 200 |
| 7 | Return chữ ký sai (fake param) | 302 FE `?status=FAILED&txnRef=...` → trang kết quả ❌ cho bấm thử lại | 302 |
| 8 | IPN gọi **lần 2, 3** (retry) | 200 "Already Confirmed", **không** update lần 2 | 200 |
| 9 | IPN chữ ký sai (giả mạo) | 400 `RspCode:97`, payload chỉ `log.warn`, không ghi DB | 400 |
| 10 | IPN báo lệch số tiền | "Số tiền callback không khớp" | 400 · **8002** |
| 11 | Khách **hủy ở cổng VNPay** (responseCode ≠ 00) | payment → `FAILED` + mở lock → FE ❌ → tạo phiên mới được ngay | 200 IPN |
| 12 | IPN + Return đua nhau (cùng xử lý) | một bên xử lý, bên kia idempotent 200/302 theo kết quả | ✔ |
| 13 | Đã cancel rồi mà IPN về sau | payment ≠ PENDING → **8005** (idempotent) — *khách đã trả tiền nhưng payment bị hủy → cần đối soát tay* ⚠️ | 200 cho gateway |
| 14 | Trang kết quả không có `paymentId` | FE **không poll**, hiển thị ❌/⏳ theo param | — |

---

## 7. 📱 MOMO — ví MoMo

### 7.1 Luồng

```text
[CREATE]  FE → POST /payments/momo/create { bookingId, method: "MOMO" }
 ①–⑤ giống §6.1 (method/booking/lock/paymentId/totalAmount)
 ⑥ key MOMO rỗng → 8008 ngay (guard trước khi ký)
      BE ký HMAC-SHA256 rồi POST JSON sang https://test-payment.momo.vn/.../create
      MoMo trả resultCode ≠ 0 hoặc payUrl rỗng → 502 8008
      mạng lỗi                          → 502 8008   (lock giải phóng)
 ⑦ trả 201 { paymentUrl, paymentId, txnRef (MOMO-<ts>-<rand>), expiresAt }

[REDIRECT]  khách trả xong → MoMo redirect thẳng FE /payment/result?resultCode=&orderId=
      ⚠️ KHÔNG qua BE → KHÔNG có paymentId → trang kết quả hiển thị theo resultCode,
         KHÔNG poll được (khác VNPay — xem ghi chú D.3)

[CALLBACK]  MoMo server POST /public/payments/momo/callback (JSON body, không JWT)
 ① signature sai → 400 {statusCode:1, "Invalid Signature"} — không ghi DB
 ② orderId không khớp payment        → 404 8001
 ③ payment ≠ PENDING (xử lý rồi)    → 200 {statusCode:0}  (idempotent — MoMo ngừng retry)
 ④ lệch số tiền                      → 400 8002
 ⑤ resultCode "0"  → SUCCESS + transactionReference = orderId + Transaction + MỞ LOCK
    resultCode ≠ "0"→ FAILED (ghi payload + responseCode) + MỞ LOCK
```

### 7.2 Các trường hợp

| # | Trường hợp | Kết quả | Code/HTTP |
| :-- | :--- | :--- | :--- |
| 1 | Happy: tạo phiên (key sandbox hợp lệ) | 201 + payUrl MoMo + txnRef | **201** |
| 2 | Lock đang giữ (còn phiên VNPay/MoMo cũ chưa xong) | "Đã có phiên thanh toán đang diễn ra" — **lock dùng chung theo booking**, không phân biệt cổng | 409 · **8005** |
| 3 | `MOMO_PARTNER_CODE/ACCESS_KEY/SECRET_KEY` rỗng | "Chưa cấu hình MoMo…" | 502 · **8008** |
| 4 | MoMo từ chối (`resultCode ≠ 0` khi create) / mạng lỗi | "MoMo từ chối tạo giao dịch: …" / "Không kết nối được MoMo" | 502 · **8008** |
| 5 | Callback thành công | `SUCCESS` + ref = `orderId` + Transaction + mở lock | 200 `{statusCode:0}` |
| 6 | Callback gọi lại lần 2 | idempotent, không update lần 2 | 200 `{statusCode:0}` |
| 7 | Callback signature giả mạo | 400 `{statusCode:1}` | 400 |
| 8 | Callback báo lỗi (hết hạn mức, hủy…) | payment → `FAILED` + mở lock → FE ❌ → làm lại được | 200 |
| 9 | Lệch số tiền callback | "Số tiền callback không khớp" | 400 · **8002** |
| 10 | Redirect về FE **không có paymentId** | trang kết quả hiển thị theo `resultCode`, **không poll** (biết hạn chế §11 D.3) | — |

---

## 8. CÁC CA LỖI CHUNG (mọi phương thức) — xử lý theo §6

```text
Bất kỳ ErrorCode nào từ BE (8001…8008, 1002, 7001…)
  → FE popup lỗi (Modal.tsx — KHÔNG toast)
  → nếu payment đang PENDING:  POST /payments/{id}/cancel   (8006 nếu đã xong)
  → reset state FE (đóng modal, xóa ref đã nhập nếu cần)
  → restart flow từ đầu (tạo lại phiên / nhập lại từ đầu)
```

| Code | HTTP | Xuất hiện ở | Hành vi FE |
| :--- | :--- | :--- | :--- |
| **8001** | 404 | id không tồn tại (create/cancel/complete/refund/webhook) | popup → reload danh sách |
| **8002** | 400 | amount ≤ 0 · lệch tiền callback · refund sai | popup → sửa số tiền |
| **8003** | 400 | complete khi đã SUCCESS | popup → reload |
| **8004** | 400 | chữ ký webhook sai (log server) | poll thấy FAILED → popup → hủy → làm lại |
| **8005** | 409 | bấm tạo phiên lần 2 · callback idempotent · tái sử dụng payment đã có phiên | popup "đã xử lý trước đó" → FE không tạo trùng |
| **8006** | 400 | cancel/complete/reuse trên payment ≠ PENDING | popup → reload |
| **8007** | 400 | bank/card thiếu mã tham chiếu | popup + `focus()` ô, **giữ dữ liệu đã nhập** |
| **8008** | 502 | key rỗng · gateway sập · MoMo từ chối | popup → **hủy PENDING** → "Hủy & thanh toán lại" |
| **1002** | 400 | method sai cho endpoint (vd CASH gửi vào `/vnpay/create`) | popup |
| **7001** | 404 | bookingId không tồn tại | popup |

---

## 9. KHÓA REDIS — mọi ca liên quan

**Key:** `payment:lock:booking:{bookingId}` · **TTL:** 10 phút · **Value:** txnRef của phiên.

| Hành động | Ảnh hưởng tới lock |
| :--- | :--- |
| Tạo phiên gateway **thành công** | **giữ** lock (setIfAbsent — đã có thì 8005) |
| Tạo phiên **lỗi giữa chừng** (8008, 8002…) | **giải phóng** ngay trong `catch` → thử lại được liền |
| Webhook báo SUCCESS / FAILED | **giải phóng** |
| `POST /{id}/cancel` (PENDING → CANCELLED) | **giải phóng** |
| Không ai làm gì quá 10' | **hết hạn tự động** (TTL) — dùng làm fallback khi BE crash giữa chừng |
| `releaseLock` gặp lỗi Redis | **bỏ qua** (log.warn) — webhook không bao giờ fail vì Redis |

**4 dẫn đến 8005 (trùng phiên):** ① bấm tạo 2 lần trong 10' · ② vẫn giữ khóa khi gọi tiếp ·
③ payment đã có `gatewayTxnId` mà truyền `paymentId` muốn tái sử dụng · ④ webhook gọi lại (idempotent).

---

## 10. TRẠNG THÁI KIỂM CHỨNG (03/10/2026 — API thật, dữ liệu mẫu)

### 10.1 Chạy lại đủ 12 URL (§2.2) — 12/12 ✅

| # | URL | Ca test | Kết quả |
| :-- | :--- | :--- | :--- |
| 1 | `POST /payments/create` | CASH | 🟢 201 `SUCCESS` |
| 2 | `POST /payments/filter` | filter theo booking | 🟢 200 |
| 3 | `GET /payments/{id}` | id=1 | 🟢 200 |
| 4 | `GET /payments/booking/{bookingId}` | booking=2 | 🟢 200 |
| 5 | `POST /{id}/complete` | bank PENDING → SUCCESS · gọi trên SUCCESS | 🟢 200 · 🟢 400 **8003** |
| 6 | `POST /{id}/cancel` | PENDING · gọi lần 2 | 🟢 200 `CANCELLED` · 🟢 400 **8006** |
| 7 | `POST /{id}/refund` | `refundAmount>0, ≤tổng` | 🟢 200 `REFUNDED` |
| 8 | `POST /vnpay/create` | happy · booking sai · method sai · trùng lock | 🟢 201 (URL sandbox) · **7001** · **1002** · **8005** |
| 9 | `POST /momo/create` | lock rỗng, key chưa cấu hình | 🟢 502 **8008** |
| 10 | `GET /vnpay/ipn` | chữ ký giả | 🟢 400 `RspCode:97` |
| 11 | `GET /vnpay/return` | chữ ký giả | 🟢 **302** → `FE/payment/result?status=FAILED` |
| 12 | `POST /momo/callback` | chữ ký giả | 🟢 400 `statusCode:1` |

### 10.2 Giao diện (browser, console 0 lỗi) — đủ ✅

| Màn hình | Kết quả |
| :--- | :--- |
| Swagger `swagger-ui.html` | 🟢 đủ **12/12** operation payment |
| Folio search + 3 card + bảng giao dịch | 🟢 |
| Form thu tiền: **đủ 6 option** (5 phương thức) | 🟢 |
| Nhánh VNPAY/MOMO: ẩn ref + note + nút "Tạo phiên thanh toán" | 🟢 |
| CheckoutModal: countdown + link sandbox + hủy/LM lại | 🟢 |
| Popup **8007** (thiếu mã) · popup **8008** (MoMo key rỗng) | 🟢 message + `Mã lỗi` + 2 nút |
| "Hủy & thanh toán lại" → `CANCELLED` + mở lock + reset UI | 🟢 (không để payment mồ côi — 8008 rollback) |
| `/payment/result` ❌ FAILED | 🟢 |
| `/payment/result` ⏳ poll → BE hủy → tự chuyển "Đã hủy" **≤3s** | 🟢 |
| Portal `/portal/search` | 🟢 |

### 10.3 Các ca test đơn lẻ (lần chạy đầu)

| Ca | Kết quả |
| :--- | :--- |
| CASH tạo (booking `BK1791018407738719`, cọc 200.000) | 🟢 201 `SUCCESS` + Transaction |
| BANK thiếu mã tham chiếu | 🟢 400 · **8007** |
| VNPay create (key test `SANDBOXTEST01`) | 🟢 201 — `paymentUrl = https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...`, `txnRef = VNP20261003161208-7771`, `vnp_Amount = 510300000` (= 5.103.000 × 100) |
| VNPay create lần 2 (lock còn hạn) | 🟢 409 · **8005** |
| MoMo create (lock rỗng, key chưa cấu hình) | 🟢 502 · **8008** |
| `POST /payments/{id}/cancel` khi PENDING | 🟢 200 → `CANCELLED` + lock mở → tạo lại được ngay |
| `POST /payments/{id}/cancel` khi SUCCESS | 🟢 400 · **8006** |
| Unit test BE (`mvn test`) | 🟢 **83/83 pass** (ký/verify, idempotency, 8004/8005/8007/8008) |

> **Fix trong lần chạy lại (03/10):** chỉ **dữ liệu mẫu** (refund/cancel payment test thừa → balance
> về 4.903.000) — **không sửa code**. 2 fix môi trường (issuer Keycloak, Redis host) đã ghi ở
> `payment-methods-5-phuong-thuc.md` §11.
> **Chưa test e2e được:** trả tiền thật trên sandbox (cần key sandbox VNPay/MoMo thật) +
> tunnel (ngrok) để gateway gọi IPN về `localhost:8080`.

---

## 11. HẠN CHẾ ĐÃ BIẾT (đọc trước khi mở rộng)

1. **Gateway luôn thu `booking.totalAmount`** — chưa hỗ trợ "thu thiếu" qua cổng (đã đặt cọc 200k rồi vẫn bị thu tổng). Muốn thu thiếu → dùng cash/bank/card.
2. **MoMo redirect không qua BE** → trang kết quả thiếu `paymentId` → không poll được (khác VNPay).
3. **Payment `PENDING` treo** khi khách bỏ dở quá TTL → không tự hủy; cần `cancel` để dọn (FE đã có nút "Hủy & thanh toán lại").
4. **`complete` không chặn `CANCELLED`/`FAILED`** (chỉ chặn `SUCCESS` với 8003) — cẩn thận khi bấm complete bừa.
5. **IPN về sau khi đã cancel** → 8005, tiền có thể đã bị trừ nhưng payment là `CANCELLED` → cần đối soát thủ công.
6. Chưa render QR inline trong `CheckoutModal` (cần thêm package `qrcode.react` — chờ duyệt).
