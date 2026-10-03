# 🌳 CÂY THƯ MỤC — SPRINT 1: T06 PAYMENT GATEWAY (VNPay / MoMo)

> **Branch:** `feature/T06-payment-webhook` → `feature/T06-checkout-modal`
> **Quy ước:** 🟢 = file MỚI cần tạo • 🔵 = file CÓ SẴN cần SỬA • ⚪ = file có sẵn, không đụng tới
> **Đối tượng:** SV năm 4 vừa học vừa code — mỗi file ghi rõ **Rules áp dụng** để không code sai quy chuẩn.

### 📏 RULES ÁP DỤNG CHO SPRINT NÀY (đọc trước khi code — `.agents/rules/`)

| Rule | Nội dung áp dụng vào Sprint 1 |
| :--- | :--- |
| **Cascade Backend** | Thứ tự tạo: Migration → Entity → DTO → Gateway → Service → Controller → Test (KHÔNG nhảy tầng) |
| **Cascade Frontend** | types → service → components → pages → route |
| **Zero-Placeholder** | Không để `TODO`/`TBD` — mọi file compile được ngay |
| **Liquibase (02)** | Migration PHẢI đặt trong `db/changelogs/incremental/2026/` + `db/sqlFile/incremental/2026/` — không DDL tay |
| **RBAC (02)** | Endpoint `create` phải có `@PreAuthorize("hasRole('ROLE_PAYMENT_CREATE')")` — endpoint IPN/return public |
| **Concurrency** | Payment window → Redis Session Lock **TTL 10 phút** (không 15'); countdown FE lấy `expiresAt` BE trả về |
| **Unit Test (02)** | Đủ 3 kiểu: happy path / validation (sai chữ ký, thiếu param) / edge (param rỗng) |
| **FE Tokens (05)** | Nút `rounded-xl h-12`, modal `rounded-2xl`, CTA `bg-red-600`, chữ `text-neutral-900` |
| **Memory Leak (03)** | Polling trạng thái trong `useEffect` PHẢI có cleanup return |
| **Zero Hallucination** | Tên field/endpoint lấy từ code & contract dưới đây — không tự đặt khác |

---

## 0. TRẠNG THÁI THỰC TẾ CỦA CODE (so sánh trước khi bắt tay vào làm)

> Kết quả rà soát code thật trong repo — biết việc nào **đã có** để không làm lại,
> việc nào **thiếu** để tập trung.

| # | Thành phần | Trạng thái | Vị trí / việc cần làm |
| :-- | :--- | :--- | :--- |
| 1 | Enum 5 method (`CASH/BANK/VNPAY/MOMO/CARD...`) | ✅ **CÓ** | `PaymentMethod.java` + CHECK constraint DB |
| 2 | Cột `transaction_reference` (DB, entity, DTO, FE) | ✅ **CÓ** | `01-init-schema.sql` d.1010, `Payment.java`, `PaymentCreateRequest` |
| 3 | FE nhập mã giao dịch | ✅ **CÓ** | `FolioPaymentPage` ô `payReference` |
| 4 | Route public cho webhook | ✅ **CÓ** | `SecurityConfig`: `/api/v1/public/**` permitAll |
| 5 | CRUD payment (create/complete/refund) | ✅ **CÓ** | `PaymentService(+Impl)` |
| 6 | Validation theo method (bank/card bắt buộc có mã...) | ❌ **THIẾU** | viết trong `PaymentServiceImpl.createPayment()` |
| 7 | Ghi reference tự động từ webhook | ❌ **THIẾU** | tạo `PaymentCallbackController` |
| 8 | Payment Gateway (sign/verify VNPay, MoMo) | ❌ **THIẾU** | tạo cả thư mục `finance/gateway/` từ 0 |
| 9 | 2 endpoint tạo paymentUrl | ❌ **THIẾU** | thêm vào `PaymentController` |
| 10 | FE gọi API thật | ❌ **THIẾU** | `finance.service.ts` chưa có `createVnPay/MoMo` |
| 11 | Label ô input theo method | ❌ **THIẾU** | `FolioPaymentPage.tsx` |
| 12 | ErrorCode signature/callback | ❌ **THIẾU** | `ErrorCode.java` (mới có 8001-8003) |
| 13 | Config sandbox keys | ❌ **THIẾU** | `application.yaml` + `.env` |
| 14 | 3 cột gateway (`gateway_txn_id`...) | ❌ **THIẾU** | Liquibase `incremental/2026/` |
| 15 | Unit test ký/xác thực + idempotency | ❌ **THIẾU** | `src/test/.../finance/` |
| 16 | Trang kết quả + polling | ❌ **THIẾU** | `PaymentResultPage` + route |

> ⚠️ **Kết luận:** nền tảng method/reference **đã đủ** (dòng 1-5) — Sprint 1 chủ yếu làm
> **logic** (dòng 6-16). Không thêm cột `transaction_reference`/`paid_at` vì đã có sẵn.

---

## 0.1. BẢNG PAYMENT METHOD & TRANSACTION REFERENCE (bắt buộc tuân thủ)

> **Code hiện tại đã có sẵn:** enum `PaymentMethod` + cột `transaction_reference` (DB, entity, DTO, FE).
> Việc còn lại = **validation theo method + ghi tự động từ webhook**.

| method (FE gửi) | Use case | Enum DB (`payment_method`) | Reference (`transaction_reference`) | Cách ghi |
| :--- | :--- | :--- | :--- | :--- |
| `cash` | Khách trả mặt | `CASH` | **NULL** hoặc số phiếu thu | FE nhập tay (optional) |
| `vnpay` | VNPay QR / CardATM | `VNPAY` | VNPay txn_no (VD: `TM126345...`) | **BE tự ghi** từ IPN (`vnp_TxnRef`/`vnp_TransactionNo`) |
| `momo` | MoMo wallet | `MOMO` | MoMo orderId | **BE tự ghi** từ callback |
| `bank` | Bank transfer | `BANK_TRANSFER` | Mã giao dịch NH (VD: `FT26...`) | FE nhập tay (**bắt buộc**) |
| `card` | Cà thẻ POS | `CREDIT_CARD` / `DEBIT_CARD` | Approval code POS | FE nhập tay (**bắt buộc**) |

*(Enum DB còn có `ZALOPAY`, `OTHER` — giữ nguyên, không dùng ở Sprint 1.)*

**Quy tắc validation (viết trong `PaymentServiceImpl` — Rules 02: business logic nằm ở Service):**

```text
CASH           → transactionReference = optional (số phiếu thu nếu có)
BANK_TRANSFER  → transactionReference BẮT BUỘC, format bắt đầu "FT" (mã giao dịch NH)
CREDIT_CARD    → transactionReference BẮT BUỘC (Approval code POS)
DEBIT_CARD     → transactionReference BẮT BUỘC (Approval code POS)
VNPAY / MOMO   → KHÔNG cho FE nhập tay — BE tự ghi từ webhook khi callback thành công
```

**Ảnh hưởng tới cây file (đã đánh dấu chi tiết bên dưới):**
- `PaymentServiceImpl` — validate theo method ở `createPayment()` + ghi reference ở `handleGatewayCallback()`
- `PaymentCallbackController` — trích `vnp_TxnRef` / MoMo `orderId` → ghi `transactionReference`
- `FolioPaymentPage.tsx` — label ô reference đổi theo method đã chọn + báo lỗi nếu bắt buộc mà để trống
- Unit test thêm case: bank thiếu mã → ném AppException; vnpay FE gửi reference tay → bỏ qua/override từ webhook

---

## 1. BACKEND

```text
backend/
├── pom.xml                                          🔵 SỬA — thêm dependency:
│                                                      (apache-poi là U12, Sprint 1 CHƯA cần;
│                                                       T06 chỉ cần JDK + Spring, có thể thêm
│                                                       commons-codec nếu chưa có cho HMAC)
│
├── src/main/resources/
│   ├── application.yaml                             🔵 SỬA — thêm block cấu hình gateway:
│   │                                                  vnpay:
│   │                                                    tmn-id: ${VNPAY_TMN_ID:}
│   │                                                    hash-secret: ${VNPAY_HASH_SECRET:}
│   │                                                    url: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
│   │                                                    return-url: ${VNPAY_RETURN_URL:http://localhost:3000/payment/result}
│   │                                                  momo:
│   │                                                    partner-code: ${MOMO_PARTNER_CODE:}
│   │                                                    access-key: ${MOMO_ACCESS_KEY:}
│   │                                                    secret-key: ${MOMO_SECRET_KEY:}
│   │
│   └── db/
│       ├── db-changelog-root.xml                   ⚪ (không sửa — root đã includeAll incremental)
│       ├── changelogs/
│       │   └── incremental/
│       │       └── 2026/
│       │           └── 20260930-1400-payment-gateway.xml   🟢 MỚI — ChangeSet Liquibase
│       │                                                      (ID/author/context đầy đủ,
│       │                                                       gọi SQL tương ứng bên dưới)
│       └── sqlFile/
│           └── incremental/
│               └── 2026/
│                   └── 20260930-1400-payment-gateway.sql    🟢 MỚI — thêm 3 cột vào payments:
│                                                              + gateway_txn_id (varchar, unique)
│                                                              + gateway_response_code (varchar)
│                                                              + raw_callback_payload (jsonb) ← log IPN
│                                                              ✅ ĐÃ CÓ SẴN (không thêm mới):
│                                                               - transaction_reference (xem 1.5)
│                                                               - paid_at, payment_method,
│                                                                 payment_provider
│                                                              ⚠️ Rules 02: KHÔNG chạy DDL tay —
│                                                              chỉ qua Liquibase migration
│
├── src/main/java/vn/edu/utc/hotel_booking/
│   ├── common/
│   │   ├── config/
│   │   │   └── SecurityConfig.java                 🔵 SỬA — permitAll cho webhook callback:
│   │   │                                              .requestMatchers(
│   │   │                                                "/api/v1/public/payments/**"   // VNPay IPN
│   │   │                                              ).permitAll()
│   │   │
│   │   └── exception/
│   │       └── ErrorCode.java                      🔵 SỬA — thêm mã lỗi:
│   │                                                  PAYMENT_SIGNATURE_INVALID,
│   │                                                  PAYMENT_CALLBACK_DUPLICATED,
│   │                                                  PAYMENT_NOT_PENDING
│   │
│   └── modules/finance/
│       ├── controller/
│       │   ├── PaymentController.java              🔵 SỬA — thêm 2 endpoint (Cascade: bước 6):
│       │   │                                          POST /api/v1/payments/vnpay/create
│       │   │                                            @PreAuthorize("hasRole('ROLE_PAYMENT_CREATE')")
│       │   │                                          POST /api/v1/payments/momo/create
│       │   │                                            @PreAuthorize("hasRole('ROLE_PAYMENT_CREATE')")
│       │   │                                          (Rules 02: chỉ nhận HTTP + @Valid + phân quyền,
│       │   │                                           business logic nằm ở Service)
│       │   │
│       │   └── PaymentCallbackController.java      🟢 MỚI — webhook PUBLIC (không JWT):
│       │                                              GET  /api/v1/public/payments/vnpay/ipn
│       │                                              GET  /api/v1/public/payments/vnpay/return   ← browser redirect
│       │                                              POST /api/v1/public/payments/momo/callback
│       │                                              (Rules 02: permitAll đã khai SecurityConfig;
│       │                                               idempotent — xử lý lại vẫn trả 200)
│       │                                              ⚠️ Bắt buộc: trích vnp_TxnRef /
│       │                                               vnp_TransactionNo (VNPay), orderId (MoMo)
│       │                                               → ghi vào transaction_reference
│       │                                               (bảng 0.1 — FE KHÔNG được nhập tay)
│       │
│       ├── gateway/                                🟢 MỚI — thư mục con (cùng cấp service/)
│       │   ├── PaymentGateway.java                 🟢 MỚI — interface chung (Strategy Pattern):
│       │   │                                          createPaymentUrl(...) → String
│       │   │                                          verifyCallback(...)   → CallbackResult
│       │   ├── VnPayGateway.java                   🟢 MỚI — build query + HMAC-SHA512 verify
│       │   ├── MoMoGateway.java                    🟢 MỚI — gọi MoMo API + HMAC verify
│       │   └── dto/
│       │       ├── PaymentUrlResponse.java         🟢 MỚI — { paymentUrl, txnRef, expiresAt }
│       │       ├── CallbackResult.java             🟢 MỚI — { valid, txnRef, amount,
│       │       │                                      responseCode, rawPayload }
│       │       └── CreatePaymentRequest.java       🟢 MỚI — { paymentId?, bookingId, method }
│       │
│       ├── service/
│       │   ├── PaymentService.java                 🔵 SỬA — thêm 2 method:
│       │   │                                          createGatewayPayment(...)
│       │   │                                          handleGatewayCallback(...)  ← idempotency guard
│       │   └── impl/
│       │       └── PaymentServiceImpl.java         🔵 SỬA — implement 2 method trên:
│       │                                                + validate theo method (xem bảng 0.1):
│       │                                                    BANK/CREDIT/DEBIT → reference BẮT BUỘC
│       │                                                    CASH → optional
│       │                                                    VNPAY/MOMO → bỏ input FE, chờ webhook
│       │                                                + if (payment.status != PENDING) → throw
│       │                                                    PAYMENT_CALLBACK_DUPLICATED
│       │                                                + @Transactional để tránh race
│       │                                                + Redis Session Lock TTL 10' khi tạo
│       │                                                  paymentUrl (Rules concurrency —
│       │                                                   giải phóng khi PAID/FAILED/timeout)
│       │
│       ├── entity/
│       │   └── Payment.java                        🔵 SỬA — thêm 3 field khớp migration:
│       │                                              gatewayTxnId, gatewayResponseCode,
│       │                                              rawCallbackPayload
│       │                                              (transactionReference, paidAt,
│       │                                               paymentMethod ĐÃ CÓ sẵn)
│       │
│       └── mapper/
│           └── FinanceMapper.java                  🔵 SỬA — thêm mapping PaymentUrlResponse
│
└── src/test/java/vn/edu/utc/hotel_booking/modules/finance/
    ├── service/
    │   ├── PaymentServiceTest.java                 🔵 SỬA — thêm test:
    │   │                                              + idempotency: callback lần 2 → AppException
    │   │                                              + validation (bảng 0.1): bank thiếu mã → ném
    │   │                                              + edge: vnpay FE gửi reference tay →
    │   │                                                bị webhook override
    │   └── VnPayCallbackTest.java                  🟢 MỚI — test verify chữ ký
    │                                                  (đúng/sai secret, thiếu param)
    └── gateway/
        └── VnPayGatewayTest.java                   🟢 MỚI — unit test thuần:
                                                       buildQueryString + sign/verify round-trip
```

---

## 2. FRONTEND

```text
frontend/
├── .env                                             🔵 SỬA — thêm (nếu cần gọi API tạo URL):
│                                                      VITE_VNPAY_ENABLED=true
│
└── src/
    ├── core/
    │   ├── api/
    │   │   ├── client.ts                            ⚪ (không sửa — đã có intercept JWT)
    │   │   └── types.ts                             ⚪
    │   └── components/ui/
    │       ├── Modal.tsx                            ⚪ dùng lại cho Checkout Modal
    │       ├── Button.tsx                           ⚪
    │       └── CountdownBadge.tsx                   🟢 MỚI — đếm ngược theo expiresAt BE trả
    │                                                  về (Redis TTL 10' — KHÔNG hardcode 15'):
    │                                                  Rules 05: rounded-xl, h-12/h-10, red-600
    │
    ├── modules/
    │   ├── finance/
    │   │   ├── types/
    │   │   │   └── finance.types.ts                 🔵 SỬA — thêm types:
    │   │   │                                              PaymentGatewayStatus,
    │   │   │                                              PaymentUrlResponse,
    │   │   │                                              CallbackStatus ('PENDING'|'PAID'|'FAILED')
    │   │   │
    │   │   ├── services/
    │   │   │   └── finance.service.ts               🔵 SỬA — thêm 2 hàm:
    │   │   │                                              createVnPayPayment(payload)
    │   │   │                                              createMoMoPayment(payload)
    │   │   │                                          (trả về ApiResponse<T> đúng chuẩn client.ts)
    │   │   │
    │   │   ├── components/
    │   │   │   └── payment/
    │   │   │       ├── CheckoutModal.tsx            🟢 MỚI — modal chọn cổng + QR + countdown:
    │   │   │       │                                  Rules 05: rounded-2xl, nút CTA red-600,
    │   │   │       │                                  loading state disable nút submit
    │   │   │       └── PaymentStatusBadge.tsx       🟢 MỚI — PENDING/PAID/FAILED/REFUNDED
    │   │   │
    │   │   └── pages/
    │   │       ├── FolioPaymentPage.tsx             🔵 SỬA — bấm "VNPAY QR" thật:
    │   │       │                                      gọi createVnPayPayment → mở paymentUrl
    │   │       │                                      + label ô payReference đổi theo method
    │   │       │                                        (bảng 0.1): "Mã giao dịch NH" /
    │   │       │                                        "Approval code POS" / "Số phiếu thu"
    │   │       │                                      + báo lỗi nếu bắt buộc mà để trống
    │   │       │
    │   │       └── PaymentResultPage.tsx            🟢 MỚI — trang return từ cổng thanh toán:
    │   │                                                  /payment/result?vnp_ResponseCode=00&...
    │   │                                                  → ✅ Thành công / ❌ Thất bại
    │   │                                                  → poll PENDING → PAID (3s/lần)
    │   │                                                  ⚠️ Rules 03: useEffect PHẢI có
    │   │                                                   cleanup (clearInterval) khi unmount
    │   │
    │   └── booking-portal/
    │       └── pages/
    │           └── CheckoutPage.tsx                 🔵 SỬA — thay <option value="VNPAY"> giả
    │                                                      thành nút gọi API thật + redirect
    │
    └── App.tsx                                      🔵 SỬA — thêm route (Cascade FE: bước cuối):
                                                       /payment/result → PaymentResultPage
                                                       ⚠️ Trang thanh toán là semantic HTML
                                                        (<main>/<section>), CTA red-600, a11y focus
```

---

## 3. ENV & CẤU HÌNH NGOÀI CODE

```text
project-1-hotel-booking/
├── .env                              🔵 SỬA — thêm key gateway (sandbox):
│                                       VNPAY_TMN_ID=...
│                                       VNPAY_HASH_SECRET=...
│                                       MOMO_PARTNER_CODE=...
│                                       MOMO_ACCESS_KEY=...
│                                       MOMO_SECRET_KEY=...
│                                       VNPAY_RETURN_URL=http://localhost:3000/payment/result
│
└── docker-compose.yml                ⚪ không sửa (env truyền qua .env là đủ)
                                       ⚠️ webhook IPN từ VNPay sandbox trỏ vào
                                          localhost — dev dùng ngrok/ssh reverse proxy
```

---

## 4. API CONTRACT (Contract-First — viết trước khi code)

```text
BE → Client (Rules 02: endpoint create có @PreAuthorize("hasRole('ROLE_PAYMENT_CREATE')"))
POST /api/v1/payments/vnpay/create          [JWT]  body: { bookingId, method }
  └─ 200 { paymentUrl, txnRef, expiresAt }   ← expiresAt = TTL lock Redis 10' (Rules concurrency)

POST /api/v1/payments/momo/create           [JWT]  body: { bookingId, method }
  └─ 200 { paymentUrl, txnRef, expiresAt }

VNPay → BE (PUBLIC, verify chữ ký)
GET  /api/v1/public/payments/vnpay/ipn?vnp_TxnRef=...&vnp_SecureHash=...
  └─ 200 { RspCode: "00", Message: "Confirm Success" }
  └─ 400 { RspCode: "97", Message: "Invalid Signature" }

GET  /api/v1/public/payments/vnpay/return?...     ← browser redirect sau khi trả tiền
  └─ 302 → http://localhost:3000/payment/result?status=PAID

MoMo → BE (PUBLIC)
POST /api/v1/public/payments/momo/callback        body: signature + orderId + resultCode
  └─ 200 { statusCode: 0 }                          (idempotent: đã xử lý → vẫn 200)
```

---

## 5. THỨ TỰ TẠO FILE (ĐÚNG CASCADE — mỗi file = 1 commit nhỏ)

> Rules 01: Backend `Entity ➔ DTO ➔ Repository ➔ Service ➔ Controller ➔ Unit Test` •
> Frontend `types ➔ service ➔ components ➔ pages ➔ route`

**Giai đoạn A — Chuẩn bị:**

| # | File | Việc làm | Est |
| :-- | :--- | :--- | :-- |
| 1 | `application.yaml` + `.env` | Cấu hình sandbox keys (không commit secret) | 0.5h |
| 2 | `ErrorCode.java` | Thêm mã lỗi (không placeholder) | 0.5h |

**Giai đoạn B — Backend theo Cascade:**

| # | File | Việc làm | Est |
| :-- | :--- | :--- | :-- |
| 3 | `changelogs/incremental/2026/2026*-payment-gateway.xml` + `sqlFile/incremental/2026/...sql` | Liquibase migration 3 cột mới (Rules 02 — không DDL tay; `transaction_reference`/`paid_at` đã có) | 1h |
| 4 | `Payment.java` | Entity fields khớp migration | 0.5h |
| 5 | `gateway/dto/` (3 file) | DTO contract (Cascade: DTO trước Service) | 0.5h |
| 6 | `gateway/PaymentGateway.java` + `VnPayGateway` + `MoMoGateway` | Strategy interface + 2 implement | 1.5h 📘 |
| 7 | `PaymentService(+Impl)` | createGatewayPayment + idempotency + **validate theo method** (bảng 0.1) + Redis lock TTL 10' | 1.5h |
| 8 | `PaymentCallbackController` + `SecurityConfig` | IPN/return public + `@PreAuthorize` endpoint create | 1h |
| 9 | `VnPayGatewayTest` + `VnPayCallbackTest` + sửa `PaymentServiceTest` | Unit test **3 kiểu** (happy/validation/edge) | 1h ✅ |
| 10 | Verify Swagger UI | Test tay endpoint | 0.5h |

**Giai đoạn C — Frontend theo Cascade:**

| # | File | Việc làm | Est |
| :-- | :--- | :--- | :-- |
| 11 | `finance.types.ts` + `finance.service.ts` | Types + gọi API create | 0.5h |
| 12 | `CountdownBadge` + `CheckoutModal` + `PaymentStatusBadge` | Components (tokens 12/16px, red CTA) | 1.5h |
| 13 | `PaymentResultPage` + `App.tsx` route | Trang kết quả + poll (cleanup useEffect) | 1h |
| 14 | `CheckoutPage`/`FolioPaymentPage` | Gắn nút thanh toán thật + label ô reference theo method (bảng 0.1) | 1h |
| | | **Tổng BE 8.0h + FE 4.0h ≈ 3.0 ngày** | |

> **Gợi ý vibe code (SV năm 4):** Làm đúng thứ tự trên — mỗi file 1 commit.
> File nào chưa hiểu → hỏi AI "giải thích file này giúp tôi" trước khi sửa;
> xong mỗi giai đoạn → chạy build + test (Cascade bước 6) rồi mới sang bước sau.
> Skills hỗ trợ: `hotel-api-development` (file 4-9), `hotel-db-schema-design` (file 3).
