# 💳 5 PHƯƠNG THỨC THANH TOÁN — CÁCH HOẠT ĐỘNG · CÓ GÌ · THIẾU GÌ · CÁCH LÀM

> **Đối tượng:** SV năm 4 vừa học vừa code (Member 3 — T06 Payment)
> **Phạm vi:** `cash`, `vnpay`, `momo`, `bank`, `card`
> **Nguồn đối chiếu:** code thật trong repo (đã rà soát, không đoán)
> **Liên quan:** `docs/ke-hoach-member3.md` (kế hoạch tổng) · `docs/tree-sprint1-T06.md` (cây file Sprint 1)
> · **`docs/payment-luong-5-phuong-thuc.md`** (luồng step-by-step + MỌI trường hợp của 5 phương thức — xem khi test/sửa lỗi)

---

## 1. TỔNG QUAN 5 PHƯƠNG THỨC

| # | method (FE gửi) | Use case | Enum DB (`payment_method`) | Reference (`transaction_reference`) | Cách ghi mã |
| :-- | :--- | :--- | :--- | :--- | :--- |
| 1 | `cash` | Khách trả mặt tại quầy | `CASH` | **NULL** hoặc số phiếu thu | FE nhập tay (optional) |
| 2 | `vnpay` | VNPay QR / CardATM | `VNPAY` | VNPay txn_no (VD: `TM126345...`) | **BE tự ghi** từ IPN |
| 3 | `momo` | MoMo wallet | `MOMO` | MoMo orderId | **BE tự ghi** từ callback |
| 4 | `bank` | Chuyển khoản NH | `BANK_TRANSFER` | Mã giao dịch NH (VD: `FT26...`) | FE nhập tay (**bắt buộc**) |
| 5 | `card` | Cà thẻ POS | `CREDIT_CARD` / `DEBIT_CARD` | Approval code POS | FE nhập tay (**bắt buộc**) |

> Enum DB còn có `ZALOPAY`, `OTHER` — **giữ nguyên, không dùng** ở Sprint 1.
> Kiểm tra tại: `backend/.../finance/entity/PaymentMethod.java` + CHECK constraint
> `database/01-init-schema.sql` (dòng 1017-1019).

### Quy tắc validation (áp dụng cho mọi nơi)

```text
CASH           → transactionReference = optional (số phiếu thu nếu có)
BANK_TRANSFER  → transactionReference BẮT BUỘC (mã giao dịch NH, VD: FT26...)
CREDIT_CARD    → transactionReference BẮT BUỘC (Approval code POS)
DEBIT_CARD     → transactionReference BẮT BUỘC (Approval code POS)
VNPAY / MOMO   → CẤM FE nhập tay — BE tự ghi từ webhook khi callback thành công
```

- Viết trong **`PaymentServiceImpl`** (Rules 02: business logic nằm ở Service — không viết trong Controller).
- FE báo lỗi ngay khi ô bắt buộc để trống (không đợi BE trả 400).

---

## 2. CÁCH HOẠT ĐỘNG TỪNG PHƯƠNG THỨC

### 2.1 💵 Cash — khách trả mặt

```text
Khách đưa tiền mặt tại quầy
  → Lễ tân mở FolioPaymentPage, chọn method = cash
  → (optional) nhập số phiếu thu vào ô reference
  → POST /api/v1/payments  { bookingId, totalAmount, paymentMethod: CASH }
  → BE: Payment.status = SUCCESS ngay, paidAt = now     ← ĐÃ CÓ (createPayment)
  → BE: tạo bản ghi transactions (sổ cái)              ← ĐÃ CÓ
  → Xong — không cần webhook, không cần chờ
```

**Trạng thái:** `SUCCESS` ngay tại bước tạo.

### 2.2 🏦 Bank transfer — chuyển khoản ngân hàng

```text
Khách chuyển khoản bằng app NH riêng (ngoài hệ thống), nhận được mã giao dịch (VD: FT26...)
  → Lễ tân chọn method = bank, BẮT BUỘC nhập mã giao dịch (VD: FT26...)
  → POST /api/v1/payments { ..., paymentMethod: BANK_TRANSFER, transactionReference: "FT26..." }
  → BE: validate reference không được rỗng              ← THIẾU (cần làm)
  → BE: Payment.status = PENDING (chờ đối soát)
  → Lễ tân thấy tiền về NH → gọi completePayment(id)    ← ĐÃ CÓ
  → status = SUCCESS
```

**Trạng thái:** `PENDING → SUCCESS` (người xác nhận).

### 2.3 💳 Card — cà thẻ POS

```text
Khách quẹt thẻ trên máy POS tại quầy → máy in ra Approval code
  → Lễ tân chọn method = card, BẮT BUỘC nhập Approval code
  → POST /api/v1/payments { ..., paymentMethod: CREDIT_CARD|DEBIT_CARD, transactionReference: "<approval>" }
  → BE: validate reference                              ← THIẾU (cần làm)
  → BE: status = ?                                      ← XEM MỤC 4 (đang MÂU THUẪN)
  → Lễ tân xác nhận → completePayment(id)
```

### 2.4 📱 VNPay QR / CardATM (online — cần gateway)

```text
① FE CheckoutPage/FolioPaymentPage bấm "VNPAY QR"
② POST /api/v1/payments/vnpay/create [JWT + ROLE_PAYMENT_CREATE]   ← THIẾU (endpoint mới)
     body: { bookingId, method }
③ BE: tạo Payment (PENDING) + build paymentUrl + ký HMAC-SHA512     ← THIẾU (cả thư mục gateway/)
     vnp_TxnRef = txnRef (mã đơn của mình), Redis lock TTL 10'
④ FE nhận { paymentUrl, txnRef, expiresAt } → redirect khách ra cổng thanh toán
⑤ Khách trả tiền trên sandbox VNPay
⑥ VNPay gọi IPN: GET /api/v1/public/payments/vnpay/ipn               ← THIẾU (controller mới)
     → verify chữ ký
     → GHI transactionReference = vnp_TransactionNo (TM126345...)
     → status = SUCCESS, paidAt = now   (idempotent: IPN gửi lại vẫn 200)
⑦ Browser redirect về /payment/result → FE poll trạng thái           ← THIẾU (trang mới)
```

**Trạng thái:** `PENDING → SUCCESS` (bằng webhook — **không** cần người bấm).

### 2.5 📱 MoMo wallet (online — cần gateway)

```text
① FE bấm "MoMo"
② POST /api/v1/payments/momo/create [JWT + ROLE_PAYMENT_CREATE]    ← THIẾU
③ BE: tạo Payment (PENDING) + gọi MoMo API lấy payUrl + ký HMAC    ← THIẾU
④ FE redirect khách sang payUrl
⑤ Khách thanh toán trên app MoMo
⑥ MoMo gọi callback: POST /api/v1/public/payments/momo/callback     ← THIẾU
     → verify signature
     → GHI transactionReference = orderId
     → status = SUCCESS (resultCode = 0) — idempotent
⑦ FE poll trạng thái → hiển thị ✅
```

---

## 3. BẢNG DB LIÊN QUAN

### `payments` — bảng chính (đã có từ đầu, `database/01-init-schema.sql` d.1002)

| Cột | Đã có? | Ghi chú |
| :--- | :---: | :--- |
| `payment_method` VARCHAR(50) + CHECK | ✅ | 8 giá trị, đủ 5 method cần dùng |
| `transaction_reference` VARCHAR(100) | ✅ | **Cột chứa mã tham chiếu — đủ chỗ cho TM..., FT..., approval code** |
| `payment_provider` VARCHAR(50) | ✅ | VD: "VNPAY", "MoMo", tên ngân hàng POS |
| `status`, `paid_at` | ✅ | `PENDING / SUCCESS / FAILED / REFUNDED / CANCELLED` |
| `gateway_txn_id` (unique) | ❌ | Thêm qua Liquibase — mã đơn phía gateway |
| `gateway_response_code` | ❌ | Thêm qua Liquibase — mã trả về của cổng (VD: "00") |
| `raw_callback_payload` JSONB | ❌ | Thêm qua Liquibase — log IPN để debug |

### `transactions` — sổ cái (đã có)

Mỗi payment thành công tạo 1 dòng `transactions` với `reference_code`.
**Lưu ý:** code hiện tại đang ghi `referenceCode = "TX-" + System.currentTimeMillis()`
(`PaymentServiceImpl.createPayment()` d.76) — chỉ là mã hệ thống, **không phải** mã
tham chiếu thật của khách. Khi làm Sprint 1 nên đổi sang dùng `transaction_reference` thật.

---

## 4. CÓ GÌ · THIẾU GÌ (rà soát code thật trong repo)

### ✅ ĐÃ CÓ (không phải làm lại)

| # | Thành phần | Vị trí |
| :-- | :--- | :--- |
| 1 | Enum đủ 5 method + CHECK constraint | `PaymentMethod.java`, `01-init-schema.sql` |
| 2 | Cột `transaction_reference` (DB → entity → DTO → FE) | `Payment.java`, `PaymentCreateRequest`, `finance.types.ts` |
| 3 | Logic **cash/debit SUCCESS ngay** + tạo `transactions` | `PaymentServiceImpl.createPayment()` d.53-81 |
| 4 | `completePayment` / `refundPayment` | `PaymentServiceImpl` |
| 5 | FE nhập mã tham chiếu | `FolioPaymentPage.tsx` ô `payReference` |
| 6 | Route public sẵn cho webhook | `SecurityConfig`: `/api/v1/public/**` permitAll |
| 7 | Endpoint CRUD payment + RBAC | `PaymentController` |

### ❌ THIẾU (cần làm)

| # | Việc thiếu | File cần sửa/tạo | Ưu tiên |
| :-- | :--- | :--- | :--- |
| 1 | **Validation reference theo method** (bảng mục 1) | `PaymentServiceImpl.createPayment()` | ⭐ Làm trước |
| 2 | **Chính sách status cho `CREDIT_CARD`** — hiện `DEBIT_CARD` SUCCESS ngay nhưng `CREDIT_CARD` để `PENDING` (d.53). Quy định: cà thẻ POS có approval code → nên SUCCESS ngay như debit, **hoặc** để lễ tân bấm xác nhận — phải chọn 1 trong 2 và thống nhất | `PaymentServiceImpl.createPayment()` | ⭐ Quyết định |
| 3 | `transactions.reference_code` dùng mã thật thay `TX-<timestamp>` | `PaymentServiceImpl` d.76 | ⭐ |
| 4 | **Toàn bộ Payment Gateway** (interface + sign/verify VNPay/MoMo) | tạo mới `modules/finance/gateway/` | ⭐ Lớn nhất |
| 5 | **Webhook controllers** (IPN/return/callback) + ghi reference tự động | tạo mới `PaymentCallbackController` | ⭐ |
| 6 | 2 endpoint tạo paymentUrl | `PaymentController` | Trung bình |
| 7 | FE gọi API thật (hiện chỉ có `<option>` giả) | `finance.service.ts` | Trung bình |
| 8 | Label ô input đổi theo method + báo lỗi required | `FolioPaymentPage.tsx` | Nhỏ |
| 9 | ErrorCode (`PAYMENT_SIGNATURE_INVALID`, `PAYMENT_CALLBACK_DUPLICATED`) | `ErrorCode.java` (mới có 8001-8003) | Nhỏ |
| 10 | Config sandbox keys | `application.yaml` + `.env` | Nhỏ |
| 11 | 3 cột gateway qua Liquibase `incremental/2026/` | `db/changelogs` + `db/sqlFile` | Nhỏ |
| 12 | Unit test: validate reference, verify chữ ký, idempotency | `src/test/.../finance/` | Trung bình |
| 13 | Trang kết quả + polling (cleanup `useEffect`) | tạo mới `PaymentResultPage` + route | Trung bình |
| 14 | **Flow lỗi: popup → hủy (cancelPayment) → làm lại** (mục 6) | `PaymentController`, `PaymentServiceImpl`, `CheckoutModal`, `PaymentResultPage` | ⭐ |

> 📌 **Kết luận:** nền tảng bảng/cột/enum **đã đủ** — việc còn lại chủ yếu là **logic**
> (validation, webhook, gateway) + **FE wiring**.

---

## 5. CÁCH LÀM (thứ tự thực hiện — mỗi dòng = 1 commit)

> Tuân thủ `.agents/rules/`: Cascade BE `Migration → Entity → DTO → Gateway → Service → Controller → Test`;
> Cascade FE `types → service → components → pages → route`; Zero-Placeholder; không DDL tay.

### Giai đoạn A — Quy ước & chuẩn bị (0.5 - 1 ngày)

| Bước | Việc | File |
| :--- | :--- | :--- |
| A1 | Thêm config gateway (sandbox keys qua env) | `application.yaml` + `.env` |
| A2 | Thêm mã lỗi mới | `ErrorCode.java` |
| A3 | Liquibase migration 3 cột gateway | `db/changelogs/incremental/2026/*.xml` + `db/sqlFile/incremental/2026/*.sql` |
| A4 | Entity khớp migration | `Payment.java` |

### Giai đoạn B — Logic 5 method còn thiếu (1 ngày)

| Bước | Việc | File |
| :--- | :--- | :--- |
| B1 | **Validate reference theo method** (mục 1) — ném `AppException` khi bank/card thiếu mã | `PaymentServiceImpl.createPayment()` |
| B2 | **Quyết định + sửa status `CREDIT_CARD`** (mục 4-#2) | `PaymentServiceImpl` d.53 |
| B3 | Ghi `transactions.reference_code` = mã thật | `PaymentServiceImpl` d.76 |
| B4 | Unit test **3 kiểu**: happy (đủ mã) / validation (thiếu mã bank) / edge (vnpay FE gửi tay → bỏ qua) | `PaymentServiceTest` |

### Giai đoạn C — Gateway online VNPay/MoMo (1.5 ngày — Sprint 1 T06)

| Bước | Việc | File |
| :--- | :--- | :--- |
| C1 | Strategy interface + 2 implement (ký/xác thực) | `gateway/PaymentGateway.java`, `VnPayGateway`, `MoMoGateway` + dto |
| C2 | `createGatewayPayment` (Redis lock TTL 10') + `handleGatewayCallback` (idempotency) | `PaymentService(+Impl)` |
| C3 | Webhook public + **tự ghi `transactionReference`** từ IPN/callback | `PaymentCallbackController` |
| C4 | 2 endpoint create + `@PreAuthorize("hasRole('ROLE_PAYMENT_CREATE')")` | `PaymentController` |
| C5 | Test verify chữ ký + idempotency | `VnPayGatewayTest`, `VnPayCallbackTest` |

### Giai đoạn D — Frontend (1 ngày)

| Bước | Việc | File |
| :--- | :--- | :--- |
| D1 | Types + 2 hàm gọi API | `finance.types.ts`, `finance.service.ts` |
| D2 | Label ô reference theo method + báo lỗi required | `FolioPaymentPage.tsx` |
| D3 | Nút thanh toán thật + modal QR + countdown (tokens 12/16px, CTA red-600) | `CheckoutModal`, `CountdownBadge` |
| D4 | Trang kết quả + poll (cleanup `useEffect`) + route | `PaymentResultPage`, `App.tsx` |

**Tổng ước tính ≈ 4 ngày** (A 1d + B 1d + C 1.5d + D 1d — khớp quota T06 3.0d nếu
A gộp chung và giai đoạn C làm song song khi chờ review).

---

## 6. XỬ LÝ LỖI — POPUP → HỦY → LÀM LẠI (bắt buộc)

> **Yêu cầu:** Bất kỳ ErrorCode nào BE trả về khi thanh toán → **(1) hiện popup lỗi →
> (2) hủy lần thanh toán đang dở → (3) quay về đầu flow để khách làm lại.**

### 6.1 Flow chuẩn

```text
FE gọi API (create / poll trạng thái) trả về ErrorCode (400 / 409 / 502 ...)
  → ① Hiện POPUP lỗi — dùng lại core/components/ui/Modal.tsx (KHÔNG thêm thư viện toast mới)
       hiển thị message từ ApiResponse + mã lỗi (code: 8004, 8007, 8008...)
  → ② Nút "Hủy & thanh toán lại":
       + Payment đã tạo còn PENDING → gọi POST /payments/{id}/cancel → status = CANCELLED
       + Lỗi xảy ra TRƯỚC khi tạo payment (validation) → không có gì để hủy
  → ③ Reset state FE: đóng modal, clear ô nhập, countdown = null, poll dừng
  → ④ Khách bắt đầu TẠO LẠI từ đầu: chọn method → nhập liệu → gọi API mới
```

### 6.2 Bảng ErrorCode → hành vi

| ErrorCode | HTTP | Gặp ở đâu | Hành vi FE |
| :--- | :--: | :--- | :--- |
| `PAYMENT_REFERENCE_REQUIRED` (8007) | 400 | Create — bank/card thiếu mã | Popup → focus lại ô reference (không mất dữ liệu đã nhập) |
| `PAYMENT_SIGNATURE_INVALID` (8004) | 400 | Webhook (log server) | Poll thấy FAILED → popup → hủy → làm lại |
| `PAYMENT_CALLBACK_DUPLICATED` (8005) | 409 | Webhook gọi lần 2 | BE tự idempotent (trả 200 cho gateway) — **FE không thấy** |
| `PAYMENT_NOT_PENDING` (8006) | 400 | Thao tác trên payment đã xong/hủy | Popup → reload danh sách |
| `GATEWAY_ERROR` (8008) | 502 | Gọi API tạo paymentUrl (gateway sập) | Popup → **hủy payment PENDING** → làm lại từ đầu |
| `INVALID_PAYMENT_AMOUNT` (8002) | 400 | Create — số tiền ≤ 0 | Popup → focus ô tiền |

### 6.3 BE bổ sung cho flow hủy (Giai đoạn B)

| Việc | File |
| :--- | :--- |
| `cancelPayment(Long id)` — chỉ chấp nhận `PENDING → CANCELLED` (giống pattern `completePayment`) | `PaymentService(+Impl)` |
| `POST /api/v1/payments/{id}/cancel` — `@PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN', 'ROLE_PROPERTY_MANAGER', 'ROLE_RECEPTIONIST')")` (đúng vai trò của endpoint `complete` hiện tại) | `PaymentController` |

> ✅ `PaymentStatus.CANCELLED` **đã có sẵn** trong enum + CHECK constraint DB — không cần migration thêm.

---

## 7. CHECKLIST TRƯỚC KHI XONG

- [x] `cash` tạo payment → `SUCCESS` ngay, reference optional
- [x] `bank` thiếu mã giao dịch → BE trả lỗi, FE báo lỗi ngay ô input *(BE ✅ Giai đoạn B — FE ✅ Giai đoạn D: `REFERENCE_CONFIG` theo method + popup 8007 + focus lại ô, giữ dữ liệu)*
- [x] `card` nhập approval code → đúng quy tắc status đã chọn (mục 4-#2) *(quyết định: SUCCESS ngay)*
- [x] `vnpay`/`momo` FE **không gửi** reference tay; mã do webhook ghi *(BE ✅ Giai đoạn B cắt tay + Giai đoạn C webhook tự ghi — FE ✅ Giai đoạn D: chọn VNPAY/MOMO ẩn ô reference, hiện note "tự ghi từ webhook")*
- [x] IPN gọi lần 2 → vẫn 200, không tạo trùng dòng (idempotency) *(BE ✅ Giai đoạn C — guard `status == PENDING` → ném 8005, controller bắt trả 200 `RspCode:00` cho gateway; test `VnPayCallbackTest`)*
- [x] **Mọi ErrorCode → popup lỗi → hủy payment dở (CANCELLED) → quay về đầu flow** (mục 6) *(FE ✅ Giai đoạn D: `PaymentErrorModal` + `getApiErrorMessage` ở Folio/Checkout/Result — BE ✅ `cancelPayment` PENDING→CANCELLED + `POST /payments/{id}/cancel` + giải phóng lock Redis, 2 test)*
- [x] Chữ ký sai → 400 + log `raw_callback_payload` *(BE ✅ Giai đoạn C — IPN trả `{"RspCode":"97"}` 400 / MoMo `{"statusCode":1}` 400; payload `log.warn`; ghi cột DB `raw_callback_payload` khi callback HỢP LỆ — xem ghi chú §10)*
- [x] Unit test đủ 3 kiểu (happy/validation/edge) — Rules 02 *(83/83 suite pass — Giai đoạn B + C + D thêm 2 test `cancelPayment`)*
- [x] Swagger UI cập nhật endpoint mới *(✅ Giai đoạn C+D — `/v3/api-docs` có đủ 6 path: `vnpay/create`, `momo/create`, `vnpay/ipn`, `vnpay/return`, `momo/callback`, `{id}/cancel`)*
- [x] Không còn `TODO`/`TBD` trong file đã sửa (Zero-Placeholder) *(✅ rà soát 6 file Stage C + 10 file Stage D — không có)*

---

## 8. GIAI ĐOẠN A — KẾT QUẢ ĐÃ LÀM (30/09/2026)

> Ghi lại **đúng những file đã sửa thật** trong lần build A.

| Bước | File đã sửa/tạo | Nội dung cụ thể |
| :--- | :--- | :--- |
| A1 | `backend/src/main/resources/application.yaml` | Thêm block `payment.gateway:` (vnpay: tmn-id/hash-secret/url/return-url; momo: partner-code/access-key/secret-key/url) — mọi secret qua `${ENV:}` mặc định rỗng |
| A1 | `.env` | Thêm 6 key sandbox: `VNPAY_TMN_ID`, `VNPAY_HASH_SECRET`, `VNPAY_RETURN_URL`, `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY` (đang để trống — lấy key từ sandbox sau) |
| A1 | `.env-example` | Thêm mục 7 mẫu key (giá trị `your_..._here`) + chú thích link sandbox |
| A2 | `ErrorCode.java` | Thêm 5 mã lỗi finance: `PAYMENT_SIGNATURE_INVALID`(8004, 400), `PAYMENT_CALLBACK_DUPLICATED`(8005, 409), `PAYMENT_NOT_PENDING`(8006, 400), `PAYMENT_REFERENCE_REQUIRED`(8007, 400), `GATEWAY_ERROR`(8008, 502) |
| A3 | `db/changelogs/incremental/2026/20260930-1729-payment-gateway.xml` 🟢 | ChangeSet `1.1.0-payment-gateway` / author `utc-hotel-team` / **không đặt context** (luôn chạy mọi môi trường) |
| A3 | `db/sqlFile/incremental/2026/20260930-1729-payment-gateway.sql` 🟢 | `ALTER TABLE payments ADD COLUMN` 3 cột: `gateway_txn_id VARCHAR(100) UNIQUE`, `gateway_response_code VARCHAR(50)`, `raw_callback_payload JSONB` |
| A4 | `Payment.java` | Thêm 3 field khớp migration (`gatewayTxnId`, `gatewayResponseCode`, `rawCallbackPayload` với `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition="jsonb"` — theo pattern `AuditLog`) |

**Sửa thêm ngoài phạm vi A (gây tắc build/boot — cần báo Member phụ trách module tương ứng):**

| File | Module | Lỗi | Sửa |
| :--- | :--- | :--- | :--- |
| `BookingServiceImpl.java` (d.283-296) | Booking | Lỗi compile **có sẵn**: biến `cur` gán lại trong vòng lặp → không effectively final → không dùng được trong lambda `orElseGet` | Tạo `LocalDate slotDate = cur;` trong vòng lặp, dùng `slotDate` trong lambda (1 dòng, không đổi logic) |
| `HolidayCalendarRepository.java` | Pricing | JPQL truy vấn `hc.startDate`/`hc.endDate` — entity `HolidayCalendar` chỉ có **1 cột `date`** → boot fail (`UnknownPathException`) | Đổi query thành `hc.date = :date` (1 dòng) |
| `RoomBedRepository.java` | Inventory | Derived query `findByBedCode` — entity `RoomBed` không có property `bedCode` (và **không chỗ nào gọi** method này) → boot fail (`PropertyReferenceException`) | Đổi tên method thành `findByName` (khớp property `name` có sẵn) |

> Cả 3 bug đều **chưa từng lộ** vì DB cũ chưa bao giờ chạy qua hết JPA validate
> (schema lệch nên backend dừng ở lỗi bảng thiếu trước khi tới tầng query).

### ✅ Trạng thái verify (đã hoàn tất — 30/09/2026)

Do DB dev cũ lệch schema nặng (thiếu 8 bảng + 38 cột + PK `booking_rooms` sai, `databasechangelog` rỗng),
đã theo **Hướng 3: Backup → Rebuild → Restore** (có duyệt):

| Bước | Kết quả |
| :--- | :--- |
| Backup | 🟢 Dump `pricing_rules`(22) + `discount_rules`(9) + `surcharge_rules`(12) → `database/backup/20260930-backup-pricing.sql` |
| Rebuild | 🟢 `docker compose down -v` → `up -d` — Liquibase chạy từ đầu **7/7 changesets OK** (baseline 01-05 + `1.1.0-payment-gateway` + repair) |
| Restore | 🟢 Đủ 22/9/12 dòng (surcharge cần fix positional do schema mới thêm cột `pricing_type` → insert lại với tên cột tường minh) |
| Verify DB | 🟢 `payments` đã có đủ 3 cột gateway · 48 bảng · JPA **validate qua** |
| Verify BE | 🟢 **`Started HotelBookingApplication` ~10s, restarts=0** |
| Data parity | 🟢 20/22 bảng bằng đúng cũ hoặc nhiều hơn · `catalog_items` 30 = 15 catalog + 15 services (schema mới tách bảng — không mất) |

**Chênh lệch đã chấp nhận (theo script hiện hành):**

- `role_permissions` cũ 52 → mới **42**: 2 file seed (`database/04` và `db/sqlFile/04-master-data`)
  trùng filter 100%, 42 = đúng spec của seed mới; bản 52 cũ sinh ra từ catalog permission
  22-permission đã retire — không dựng lại được đúng 1-1.
- `roles` 4 → 5 (thêm `HOUSEKEEPING`), `permissions` 22 → 24 — seed mới đầy đủ hơn.
- `hotel_room_type_catalog_items` (31 dòng) — bàn thừa ngoài schema chuẩn, không entity nào map → bỏ;
  file archive giữ tại `database/backup/20260930-backup-hotel_room_type_catalog_items.sql`.

---

## 9. GIAI ĐOẠN B — KẾT QUẢ ĐÃ LÀM (30/09/2026)

> Ghi lại **đúng những file đã sửa thật** + kết quả verify (quy tắc: làm xong là ghi).

| Bước | File đã sửa | Nội dung cụ thể |
| :--- | :--- | :--- |
| B1 | `PaymentServiceImpl.createPayment()` | Validate theo method: `BANK_TRANSFER`/`CREDIT_CARD`/`DEBIT_CARD` thiếu mã → ném `AppException(PAYMENT_REFERENCE_REQUIRED)` (8007) · `VNPAY`/`MOMO` FE gửi tay → **cắt bỏ** (webhook tự ghi) · reference được `trim()` · `CASH` giữ optional |
| B2 | `PaymentServiceImpl.createPayment()` | **Quyết định (đã chọn): `CREDIT_CARD` SUCCESS ngay** khi có approval code — đồng bộ với `CASH`/`DEBIT_CARD`; `isImmediateSuccess` giờ gồm 3 method |
| B3 | `PaymentServiceImpl` (`createPayment` + `completePayment`) | Thêm helper `buildReferenceCode()` — `transactions.reference_code` dùng **mã thật** (phiếu thu / approval code / `FT26...`), chỉ fallback `TX-<timestamp>` khi không có mã |
| B4 | `PaymentServiceTest.java` | Thêm **8 test** → tổng **11 test** trong class: B1 happy (bank + mã → PENDING, giữ mã, không tạo Transaction) · B1 validation (thiếu mã / mã toàn space trắng → 8007) · B1 edge (VNPAY FE gửi tay → bị bỏ qua) · B2 (credit + approval code → SUCCESS + Transaction đúng mã) · B3 (cash có phiếu thu → mã thật; không có → `TX-`; `completePayment` bank → `FT26...`) |

### Verify

| Hạng mục | Kết quả |
| :--- | :--- |
| `PaymentServiceTest` | 🟢 **11/11 pass** |
| Toàn bộ test suite | 🟢 **63 tests, 0 fail** (1 skip sẵn có = `HotelBookingApplicationTests` context test) |
| Build + boot | 🟢 Image build lại → `docker compose up -d` → **`Started`, restarts=0** |

> **Ghi chú môi trường:** máy dev không cài Java local → chạy test bằng container
> `maven:3.9.6-eclipse-temurin-21-alpine`, mount `backend/` + volume cache `hotel_m2_cache`
> (lần sau không tải lại deps). Lệnh mẫu:
> `docker run --rm -v "<repo>/backend:/build" -v hotel_m2_cache:/root/.m2 -w /build maven:3.9.6-eclipse-temurin-21-alpine mvn test`

---

## 10. GIAI ĐOẠN C — BẮT ĐẦU (01/10/2026) → KẾT QUẢ ĐÃ LÀM (01/10/2026)

> **Phạm vi:** Gateway online VNPay/MoMo — Strategy interface, ký/xác thực chữ ký,
> `createGatewayPayment` (Redis lock TTL 10'), webhook public (IPN/callback, idempotency,
> tự ghi `transactionReference`), 2 endpoint create + RBAC, unit test ký/xác thực + idempotency.
> **Các bước:** C1 → C5 (xem §5).

| Bước | File đã sửa/tạo | Nội dung cụ thể |
| :--- | :--- | :--- |
| C1 | `gateway/PaymentGateway.java` 🟢 | Interface Strategy: `supportedMethod()`, `createPaymentUrl(txnRef, amount, orderInfo, ipAddress)`, `verifyCallback(params) → CallbackResult` |
| C1 | `gateway/VnPayGateway.java` 🟢 | Ký/xác thực **HMAC-SHA512** chuẩn VNPay v2.1.0: sort field ASCII + encode URL (space→`%20`), `vnp_Amount × 100`, timezone `Asia/Ho_Chi_Minh`; verify loại `vnp_SecureHash`/`vnp_SecureHashType` khỏi chuỗi ký; **guard key rỗng** → `GATEWAY_ERROR`(8008) lúc tạo, `valid=false` lúc verify |
| C1 | `gateway/MoMoGateway.java` 🟢 | `createPaymentUrl` POST JSON sang MoMo (RestClient) ký **HMAC-SHA256** đúng thứ tự field; `verifyCallback` verify signature callback (`resultCode=0` là thành công); guard key rỗng như trên; **+2 key config** `momo.ipn-url`/`momo.redirect-url` vào `application.yaml` |
| C1 | `gateway/dto/` 🟢 (3 file) | `PaymentUrlResponse` {paymentUrl, txnRef, expiresAt} · `CallbackResult` {valid, txnRef, transactionReference, amount, responseCode, success, rawPayload} · `CreatePaymentRequest` {paymentId?, bookingId, method} |
| C2 | `PaymentService.java` | +2 method: `createGatewayPayment(request, ip)`, `handleGatewayCallback(method, params)` |
| C2 | `PaymentServiceImpl.java` | **`createGatewayPayment`**: validate chỉ VNPAY/MOMO (1002) → Redis `setIfAbsent` lock `payment:lock:booking:{id}` **TTL 10'** (đang giữ → 8005) → tạo/tái sử dụng Payment PENDING + `gatewayTxnId` (`VNP-/MOMO-<ts>-<rand>`) → trả `{paymentUrl, txnRef, expiresAt=+10'}`; lỗi giữa chừng → giải phóng lock. **`handleGatewayCallback`** (`@Transactional`): verify chữ ký trước (sai → 8004, không đụng DB) → tìm theo `gatewayTxnId` (thiếu → 8001) → **idempotency guard `status != PENDING` → 8005** → khớp số tiền (8002) → SUCCESS: `paidAt` + **tự ghi `transactionReference`** + tạo `Transaction` (referenceCode = mã gateway) / FAILED: ghi payload + responseCode → giải phóng lock |
| C3 | `PaymentCallbackController.java` 🟢 | Webhook PUBLIC 3 endpoint: `GET .../public/payments/vnpay/ipn` → 200 `{RspCode:"00"}` (đã xử lý lần 2 → vẫn 200 `"Already Confirmed"`; chữ ký sai → **400 `{RspCode:"97"}`**) · `GET .../vnpay/return` → verify rồi **302** về `payment/result?status=PAID\|FAILED&txnRef=...` · `POST .../momo/callback` → 200 `{statusCode:0}` (dup vẫn 200; sai → 400 `{statusCode:1}`) |
| C4 | `PaymentController.java` | +2 endpoint `POST /payments/vnpay/create`, `POST /payments/momo/create` — `@Valid` + `@PreAuthorize("hasAnyRole('ROLE_CHAIN_ADMIN','ROLE_PROPERTY_MANAGER','ROLE_RECEPTIONIST','ROLE_CUSTOMER')")` (đúng convention thật, không phải `ROLE_PAYMENT_CREATE` như doc cũ) + helper `resolveClientIp` (X-Forwarded-For) cho `vnp_IpAddr` |
| C4 | `PaymentRepository.java` | +`findByGatewayTxnId(String)` |
| C5 | `gateway/VnPayGatewayTest.java` 🟢 | **8 test** ký/xác thực thuần: field URL + ×100 · round-trip sign→verify · sai secret · sửa payload sau ký · thiếu `vnp_TxnRef` · thiếu `vnp_SecureHash` · responseCode≠00 → valid nhưng fail · key rỗng → 8008 |
| C5 | `service/VnPayCallbackTest.java` 🟢 | **7 test** flow Service + Gateway THẬT: thành công → SUCCESS + ghi reference `TM126...` + Transaction + giải phóng lock · **idempotency lần 2 → 8005, không save lần 2** · sai chữ ký → 8004 không đụng DB · thiếu param → 8004 · lệch tiền → 8002 · responseCode 24 → FAILED không tạo Transaction · không tìm thấy → 8001 |
| C5 | `PaymentServiceTest.java` | +3 test `createGatewayPayment` (happy path lock TTL/expiry/gatewayTxnId · lock đang giữ → 8005 · CASH → 1002) → tổng **14 test** |

### Verify

| Hạng mục | Kết quả |
| :--- | :--- |
| Toàn bộ test suite | 🟢 **81 tests, 0 fail** (1 skip sẵn có) — VnPayGatewayTest 8 + VnPayCallbackTest 7 + PaymentServiceTest 14 |
| Build + boot | 🟢 Image build lại → **`Started HotelBookingApplication` ~10s, restarts=0** |
| Smoke IPN sai chữ ký | 🟢 `GET /api/v1/public/payments/vnpay/ipn?...` → **HTTP 400** `{"RspCode":"97","Message":"Invalid Signature"}` |
| Smoke MoMo sai signature | 🟢 `POST .../momo/callback` → **HTTP 400** `{"statusCode":1,"message":"Invalid Signature"}` |
| Smoke RBAC | 🟢 `POST /payments/vnpay/create` không JWT → **HTTP 401** |
| Swagger | 🟢 5 path mới có trong `/v3/api-docs` |
| Zero-Placeholder | 🟢 Không `TODO`/`TBD` trong 6 file Stage C |

### ⚠️ Ghi chú / deviation (đã tự xử lý, báo lại để biết)

1. **Chữ ký sai KHÔNG ghi cột `raw_callback_payload` vào DB** — Service ném 8004 (rollback mọi ghi trong transaction), nên payload được **`log.warn` ra console** để debug; cột DB chỉ ghi khi callback **hợp lệ** (payload đã verify mới đáng tin). Checklist §7 ghi "log" → satisfied bằng log console.
2. **Doc cũ ghi `@PreAuthorize("hasRole('ROLE_PAYMENT_CREATE')")`** nhưng role này không tồn tại trong seed — dùng convention thật `hasAnyRole(4 role)` như các endpoint `/payments/create` khác.
3. **`FinanceMapper` không phải sửa** (doc tree ghi 🔵) — `PaymentUrlResponse` dựng trực tiếp trong Service, không map từ entity.
4. **Key sandbox đang rỗng** (`.env` chưa có tài khoản VNPay/MoMo) → `createPaymentUrl` trả 8008 "Chưa cấu hình..." thay vì crash; unit test dùng secret giả. E2E thật cần set `VNPAY_*`/`MOMO_*` trong `.env` + tunnel (ngrok) cho IPN localhost.
5. **`cancelPayment` (mục 6.3)**: hủy Payment `PENDING → CANCELLED` + endpoint để FE restart flow khi lỗi. ✅ **Đã làm trong Giai đoạn D** (03/10/2026): `PaymentService.cancelPayment` (guard `PAYMENT_NOT_PENDING` 8006, giải phóng Redis lock) + `POST /api/v1/payments/{id}/cancel` (4 role) + 2 unit test — vì flow lỗi §6.1 bắt buộc có nó.
6. Đã sửa 1 lỗi boot phát sinh trong C: `PaymentCallbackController` cần **constructor tường minh** với `@Value` (Lombok `@RequiredArgsConstructor` + `makeFinal` kéo field `String` vào constructor → Spring không inject được config).

---

## 11. GIAI ĐOẠN D — KẾT QUẢ ĐÃ LÀM (03/10/2026)

> **Phạm vi (FE):** types → service (`createVnPayPayment`/`createMoMoPayment`) → components
> (CheckoutModal, CountdownBadge) → trang kết quả (`PaymentResultPage`) → routes.
> Bắt buộc theo §6: **mọi ErrorCode → popup `Modal.tsx` → hủy payment dở (`PENDING → CANCELLED`) →
> reset state → restart flow từ đầu**; countdown đồng bộ `expiresAt` BE trả về (TTL 10', không hardcode).

### D1 — Types + service

| File | Nội dung cụ thể |
| :--- | :--- |
| `finance/types/finance.types.ts` 🔵 | +`GatewayMethod` (`'VNPAY' \| 'MOMO'`), `GatewayCreateRequest`, `PaymentUrlResponse` (`paymentId`/`paymentUrl`/`txnRef`/`expiresAt`), `CallbackStatus` |
| `finance/services/finance.service.ts` 🔵 | +3 hàm: `createVnPayPayment`, `createMoMoPayment` (POST `/vnpay/create`, `/momo/create`), `cancelPayment(id)` (POST `/{id}/cancel`) |
| `core/api/error.ts` 🟢 | `getApiErrorMessage(err)` trích `(code, message)` từ ApiResponse chuẩn — dùng cho mọi popup §6 |

### D2 — `FolioPaymentPage.tsx` 🔵

- `REFERENCE_CONFIG` theo method: CASH *"Số phiếu thu (tùy chọn)"* / BANK *"Mã giao dịch ngân hàng \*"* / CARD *"Approval code (POS) \*"* — **FE validate bắt buộc trước khi gửi** → popup 8007 + `focus()` lại ô, **giữ nguyên dữ liệu đã nhập**.
- Chọn VNPAY/MOMO → **ẩn ô reference + ô provider**, hiện note blue *"mã tham chiếu tự ghi từ webhook"*, nút đổi thành *"Tạo phiên thanh toán"* → gọi API thật → mở `CheckoutModal`.
- Thay `alert()` bằng **popup lỗi §6** (dùng `core/components/ui/Modal`, không toast mới).
- Bảng giao dịch đổi chip inline → `<PaymentStatusBadge>` (5 trạng thái, 5 màu).

### D3 — Components 🟢 mới (`modules/finance/components/payment/`)

| File | Nội dung |
| :--- | :--- |
| `CountdownBadge.tsx` | Đếm ngược theo `expiresAt` ISO từ BE (**không hardcode 10'**), tick 1s có cleanup, nhãn 12px / số 16px (tokens Rules 05), đỏ khi ≤2 phút hoặc hết hạn |
| `PaymentErrorModal.tsx` | Popup lỗi §6: icon đỏ + message + `Mã lỗi: 8008` + nút **"Hủy & thanh toán lại"** (CTA red-600) / "Đóng" |
| `CheckoutModal.tsx` | Modal phiên thanh toán: số tiền + `CountdownBadge` + txnRef mono + link gateway + nút *"Mở trang thanh toán"* (`window.open`) + *"Hủy & thanh toán lại"* |
| `PaymentStatusBadge.tsx` | Chip PENDING/SUCCESS/FAILED/REFUNDED/CANCELLED — dùng ở bảng Folio + trang kết quả |
| `CheckoutPage.tsx` 🔵 (portal) | Thay option VNPAY giả: sau khi `bookingService.create` xong → `createVnPayPayment({bookingId})` → `window.location.href = paymentUrl`; lỗi (8008…) → popup → *"Thử lại"* = tạo lại phiên cho **booking đã có** (không tạo lại booking) |

### D4 — Trang kết quả + route

| File | Nội dung |
| :--- | :--- |
| `pages/PaymentResultPage.tsx` 🟢 | Nhận `?status=PAID\|FAILED&txnRef=&paymentId=&bookingId=` (BE `/vnpay/return`) hoặc `?resultCode=&orderId=` (MoMo redirect) → 3 trạng thái ✅/⏳/❌; **poll `getPaymentById` 3s/lần đến khi ≠PENDING, `clearInterval` trong cleanup useEffect (Rules 03)**; nút *"Thử thanh toán lại"* = `navigate(-1)` (quay đầu flow) |
| `App.tsx` 🔵 | Route `/payment/result` **ngoài** `AdminLayout` (khách portal không vào layout admin) |

### BE bổ sung cho flow D (bắt buộc theo §6.1/§6.3 — không có thì FE không hủy được)

| File | Nội dung |
| :--- | :--- |
| `PaymentService(+Impl)` 🔵 | +`cancelPayment(Long id)`: guard `PENDING` (không phải → 8006), `PENDING → CANCELLED`, **giải phóng Redis lock** để tạo phiên mới ngay |
| `PaymentController` 🔵 | +`POST /api/v1/payments/{id}/cancel` — `@PreAuthorize` 4 role (đúng convention §10) |
| `gateway/dto/PaymentUrlResponse` 🔵 | +`paymentId` — FE cần để hủy phiên khi lỗi |
| `PaymentCallbackController` 🔵 | `vnPayReturn` đính kèm `&paymentId=&bookingId=` khi xử lý tại chỗ (FE poll được); config tách `fe-result-url` |
| `application.yaml` + `.env` + `.env-example` 🔵 | `vnpay.return-url` → **BE endpoint** `/api/v1/public/payments/vnpay/return` (contract §4 — chữ ký chỉ verify ở server); +key `vnpay.fe-result-url` = `http://localhost:3000/payment/result` |

### ✅ Trạng thái verify (03/10/2026)

| Kiểm tra | Kết quả |
| :--- | :--- |
| `npm run build` (tsc -b && vite build, container FE) | 🟢 PASS — 0 lỗi type (lần 1 fail do `node_modules` container rỗng sau khi Docker restart → `npm install` là đủ) |
| Unit test BE (`mvn test`, Docker Maven) | 🟢 **83/83 pass, 0 fail** (81 cũ + 2 `cancelPayment`), 1 skip có sẵn |
| Rebuild image + boot backend | 🟢 `BUILD SUCCESS` → `Started HotelBookingApplication in 10.5s` |
| Smoke `POST /payments/1/cancel` không JWT | 🟢 **401** (endpoint tồn tại, được bảo vệ) |
| Smoke `GET /vnpay/return` (chữ ký sai/param giả) | 🟢 **302 → `http://localhost:3000/payment/result?status=FAILED&txnRef=VNP-SMOKE-1`** |
| Swagger `/v3/api-docs` | 🟢 Đủ **12 payment path**, có `/payments/{id}/cancel` |
| Browser: `/payment/result?status=FAILED...` | 🟢 Render đúng ❌ + message + 2 nút, console **0 lỗi** |
| Browser: `/payment/result?...paymentId=...` | 🟢 Render ⏳ "Đang xác nhận..." + **poll 3s tự động** (401 khi smoke không JWT — user thật có token sẽ poll tới SUCCESS) |

### ⚠️ Ghi chú / hạn chế của D

1. **QR trong CheckoutModal**: chưa render ảnh QR thật vì **chưa thêm dependency** (`qrcode.react`) — đang hiển thị **link thanh toán + nút mở tab mới** (sandbox VNPay/MoMo tự hiện QR trên trang của họ). Muốn QR inline thì cần duyệt thêm package.
2. **Số tiền gateway**: `createVnPayPayment`/`createMoMoPayment` chỉ gửi `{bookingId, method}` — BE thu theo **`booking.totalAmount`** (đã là quyết định C). Nếu khách đã đặt cọc trước (balanceDue < totalAmount) thì cổng vẫn thu tổng đơn — **hạn chế known, mở rộng "thu thiếu qua gateway" để sau**.
3. **MoMo redirect** về thẳng FE (`momo.redirect-url`) với `?resultCode=&orderId=` → trang kết quả hiển thị theo param, **không poll được theo paymentId** (không có endpoint tra theo orderId); trạng thái thật do callback ghi. VNPay thì qua BE return nên poll đầy đủ.
4. **CheckoutPage**: booking tạo **trước** khi gọi gateway → lỗi gateway không hủy booking (chỉ popup + "Thử lại" tạo lại phiên) — đúng §6 vì *chưa có payment nào để hủy*.
5. File build FE hay gặp warning `chunk >500 kB` (bundle chưa code-split) — **có sẵn từ trước**, không thuộc phạm vi D.

### 🖥️ Kiểm tra API + UI toàn diện — "chạy lại URL xem đủ giao diện chưa" (03/10/2026)

**Phạm vi:** chạy lại **12/12 URL payment** (§2.2 `payment-luong-5-phuong-thuc.md`) + duyệt toàn bộ
giao diện liên quan. Kết quả: **12/12 endpoint đúng như doc — không phát hiện bug code mới.**

**① 12 URL (JWT admin, dữ liệu mẫu booking `BK1791018407738719`):**

| # | Endpoint | Ca test | KQ |
| :-- | :--- | :--- | :--- |
| 1 | `POST /payments/create` | CASH 150.000 | 🟢 201 `SUCCESS` (id 13) |
| 2 | `POST /payments/filter` | `{bookingId:2}` | 🟢 200 danh sách |
| 3 | `GET /payments/{id}` | id=1 | 🟢 200 |
| 4 | `GET /payments/booking/{id}` | booking=2 | 🟢 200 |
| 5 | `POST /{id}/complete` | trên SUCCESS | 🟢 400 **8003** · happy (bank PENDING) → 🟢 200 `SUCCESS` |
| 6 | `POST /{id}/cancel` | PENDING → gọi lần 2 | 🟢 200 `CANCELLED` → lần 2 🟢 400 **8006** |
| 7 | `POST /{id}/refund` | `refundAmount=150000` | 🟢 200 → `REFUNDED` |
| 8 | `POST /vnpay/create` | happy · booking 99999 · method=CASH · trùng lock | 🟢 201 (paymentUrl sandbox thật) · 404 **7001** · 400 **1002** · 409 **8005** |
| 9 | `POST /momo/create` | lock rỗng + key rỗng | 🟢 502 **8008** |
| 10 | `GET /vnpay/ipn` (chữ ký giả) | — | 🟢 400 `RspCode:97 Invalid Signature` |
| 11 | `GET /vnpay/return` (chữ ký giả) | — | 🟢 **302** → `FE/payment/result?status=FAILED&txnRef=…` (curl) |
| 12 | `POST /momo/callback` (chữ ký giả) | — | 🟢 400 `statusCode:1 Invalid Signature` |

**② Swagger** `http://localhost:8080/swagger-ui.html`: 🟢 đủ **12/12 operation** Payment + Payment Callback.

**③ Giao diện (browser thật, console 0 lỗi ở các trang payment):**

| Màn hình | KQ |
| :--- | :--- |
| **Folio** `/finance/folios` | 🟢 Search → đủ 3 card Charges/Paid/Balance + bảng giao dịch + invoice |
| Form thu tiền | 🟢 Đủ **6 option** (5 phương thức) + purpose + provider/ref + 2 nút |
| Nhánh **VNPAY/MOMO** | 🟢 Ẩn ô ref + provider, hiện note "mã tham chiếu tự ghi từ webhook", nút → **"Tạo phiên thanh toán"** |
| **CheckoutModal** | 🟢 countdown theo `expiresAt` + link sandbox thật + "Mở trang thanh toán" + **"Hủy & thanh toán lại"** |
| Popup **8007** (bank thiếu mã) | 🟢 message đúng + `Mã lỗi: 8007` + nút Đóng / Hủy & thanh toán lại |
| Popup **8008** (MoMo key rỗng) | 🟢 `Lỗi kết nối cổng thanh toán` + `Mã lỗi: 8008` → bấm hủy → reset state, không để payment PENDING mồ côi (rollback + lock mở) |
| **"Hủy & thanh toán lại"** trên modal | 🟢 PAY-19 `PENDING → CANCELLED`, Redis lock trống, UI về đầu flow |
| **`/payment/result`** ❌ FAILED | 🟢 render đủ message + txnRef + 2 nút thử lại |
| **`/payment/result`** ⏳ poll | 🟢 BE hủy PAY-21 → trang **tự chuyển "Đang chờ" → "Đã hủy" ≤3s** (poll hoạt động thật) |
| Portal `/portal/search` | 🟢 render, 0 lỗi console |

**④ Fix trong lần chạy này (dữ liệu mẫu — không sửa code):**

- Balance lệch **-100.000** do payment test thừa → `refund` PAY-12/13/18 + `cancel` PAY-17 →
  balance về đúng **4.903.000** (đơn 5.103.000 − cọc 200.000).
- 2 fix môi trường đã áp từ đầu phiên test (chi tiết §11 ghi chú D / `payment-luong` §10):
  **(a)** issuer Keycloak mismatch → compose `${KEYCLOAK_ISSUER_URI:-…}` (hết 401 "iss claim is not valid");
  **(b)** Redis host sai (`SPRING_REDIS_HOST` là tiền tố Boot 2 bị Boot 3/4 bỏ qua) → compose set
  `REDIS_HOST: redis` + `SPRING_DATA_REDIS_HOST` + map riêng `VNPAY_*`/`MOMO_*` (**không** map
  `VNPAY_URL`/`MOMO_URL` rỗng — Spring `${X:default}` vẫn lấy `""` khi biến tồn tại → paymentUrl mất base).

**Trạng thái cuối:** dữ liệu mẫu sạch (1 SUCCESS cọc + các bản test REFUNDED/CANCELLED), `payment:lock:*` trống,
không PENDING treo.
