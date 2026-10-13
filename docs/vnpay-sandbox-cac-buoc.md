# VNPay Sandbox — Thông tin tài khoản & Các bước cấu hình / test

> **Cập nhật:** 12/10/2026 — nhận key sandbox từ email VNPay.
> **Mục tiêu:** chạy thật luồng **Thu tiền → VNPay Sandbox → quay về trang kết quả** cho thanh toán Folio/hóa đơn khách sạn.
> **Quy tắc secret:** Hash Secret **không ghi trong file này** — đã lưu ở `.env` (gốc repo, nằm trong `.gitignore`).
> Không commit `.env` lên Git, không screenshot key gửi lung tung, không log key ra console.

---

## 1. Thông tin sandbox đã cấp

| Thông tin | Giá trị | Ghi chú |
|---|---|---|
| TMN Code (`vnp_TmnCode`) | `YD0M5DI1` | Mã website test — đã điền vào `.env` |
| Hash Secret (`vnp_HashSecret`) | *(xem email VNPay — đã lưu trong `.env`)* | Chuỗi ký HMAC-SHA512 — **bí mật, không ghi vào doc/Git** |
| URL thanh toán TEST (`vnp_Url`) | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` | Trùng default của `application.yaml` → **không cần cấu hình thêm** |
| Merchant Admin | <https://sandbox.vnpayment.vn/merchantv2/> | User: `hoangtiendat15072005@gmail.com` — quản lý giao dịch + **cấu hình IPN** |
| SIT — kịch bản test IPN | <https://sandbox.vnpayment.vn/vnpaygw-sit-testing/user/login> | Công cụ test IPN theo kịch bản VNPay cung cấp |
| Tài liệu tích hợp | <https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html> | |
| Code demo | <https://sandbox.vnpayment.vn/apis/vnpay-demo/code-demo-tích-hợp> | |
| Demo cổng thanh toán | <https://sandbox.vnpayment.vn/apis/vnpay-demo/> | Trải nghiệm trước khi tích hợp |
| Hỗ trợ | support.vnpayment@vnpay.vn · Hotline 1900 55 55 77 | |

### Thẻ test (nhập ở trang sandbox khi thanh toán)

| Trường | Giá trị |
|---|---|
| Ngân hàng | **NCB** |
| Số thẻ | `9704198526191432198` |
| Tên chủ thẻ | `NGUYEN VAN A` |
| Ngày phát hành | `07/15` |
| Mật khẩu OTP | `123456` |

---

## 2. Cấu hình key vào dự án

### 2.1 Flow của key (không hardcode trong source)

```
.env  (gốc repo — nằm trong .gitignore)
   ↓  docker compose đọc khi up  (docker-compose.yml dòng 136–142)
Container env: VNPAY_TMN_ID / VNPAY_HASH_SECRET / VNPAY_RETURN_URL / VNPAY_FE_RESULT_URL
   ↓  application.yaml dòng 74–80:  ${VNPAY_TMN_ID:} ...
Spring @Value("${payment.gateway.vnpay.tmn-id}")  →  VnPayGateway
```

| Biến `.env` | Config trong app (`payment.gateway.vnpay.*`) | Giá trị điền |
|---|---|---|
| `VNPAY_TMN_ID` | `tmn-id` | `YD0M5DI1` |
| `VNPAY_HASH_SECRET` | `hash-secret` | *(secret 64 ký tự từ email)* |
| `VNPAY_RETURN_URL` | `return-url` | `http://localhost:8080/api/v1/public/payments/vnpay/return` |
| `VNPAY_FE_RESULT_URL` | `fe-result-url` | `http://localhost:3000/payment/result` |

### 2.2 ⚠️ Bẫy: phải điền ĐỦ 4 dòng

`docker-compose.yml` dòng 138–139 **có truyền** `VNPAY_RETURN_URL` và `VNPAY_FE_RESULT_URL` vào container.
Nếu 2 biến này để trống → container nhận chuỗi rỗng → Spring **không lấy default** nữa
(`${X:default}` chỉ áp dụng khi biến *không tồn tại*, biến rỗng vẫn bị dùng) → `vnp_ReturnUrl` rỗng → VNPay từ chối paymentUrl.

> Ngược lại, `VNPAY_URL` compose **cố tình KHÔNG truyền** (dòng 134–135) để giữ default sandbox trong yaml —
> **đừng** thêm `VNPAY_URL` vào compose, thêm vào cũng không có tác dụng.

`.env` đã có sẵn ở gốc repo với key thật (gitignore).
Máy mới / máy sạch: copy một trong 2 template rồi điền key:
`.env-example` (khuyến nghị, đủ mục 1–7) hoặc `.env.example` (bản ngắn) —
`.env.docker` dùng riêng cho môi trường Docker network.

```env
VNPAY_TMN_ID=YD0M5DI1
VNPAY_HASH_SECRET=<64 ký tự từ email — xem file .env, không ghi lại ở đây>
VNPAY_RETURN_URL=http://localhost:8080/api/v1/public/payments/vnpay/return
VNPAY_FE_RESULT_URL=http://localhost:3000/payment/result
```

### 2.3 Nạp lại cấu hình

```powershell
docker compose up -d      # compose tự recreate backend khi env đổi
docker ps                 # hotel_backend Up · hotel_liquibase exited (0)
```

---

## 3. Kiểm tra key đã vào app chưa (2 phút)

```powershell
# 1) Lấy token
$tok = (Invoke-RestMethod -Uri "http://localhost:8081/realms/hotel-realm/protocol/openid-connect/token" `
        -Method Post -Body @{client_id="hotel-frontend"; grant_type="password"; username="admin"; password="123456"}).access_token

# 2) Tạo phiên VNPay cho booking 4
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/payments/vnpay/create" -Method Post `
  -Headers @{Authorization="Bearer $tok"} -ContentType 'application/json' `
  -Body '{"bookingId":4,"method":"VNPAY"}'
```

**Kỳ vọng:** `201` · `result.paymentUrl` bắt đầu `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...` và chứa `vnp_TmnCode=YD0M5DI1` · `result.txnRef` dạng `VNP2026...` · `expiresAt` = now + 10 phút.

**Nếu nhận 8008 "Chưa cấu hình VNPay (VNPAY_TMN_ID / VNPAY_HASH_SECRET)"** → key chưa vào container → kiểm tra lại `.env` và chạy lại `docker compose up -d`.

---

## 4. IPN URL — phần THỦ CÔNG cần làm (quan trọng nhất)

**Vì sao cần:** IPN là server → server. VNPay **không gọi được `localhost`**.
Còn **Return URL thì KHÔNG cần tunnel** (trình duyệt khách tự quay về `localhost:8080` — đã default sẵn).

### Các bước:

1. **Mở tunnel public** cho backend — khuyến nghị `cloudflared` (miễn phí, không có trang chắn giữa chặn request của VNPay):
   ```powershell
   cloudflared tunnel --url http://localhost:8080
   # → được URL dạng https://xxxx-xxxx.trycloudflare.com
   ```
   (Dùng ngrok cũng được `ngrok http 8080` → `https://xxxx.ngrok-free.app`, nhưng bản free có trang cảnh báo
   "ngrok - visit site" có thể khiến VNPay không gửi IPN tới nơi — nếu bị thì đổi sang cloudflared.)

2. **Đăng ký IPN** trong Merchant Admin (<https://sandbox.vnpayment.vn/merchantv2/>) → mục cấu hình IPN/Webhook:
   ```
   https://<tunnel-của-anh>/api/v1/public/payments/vnpay/ipn
   ```
   (Endpoint hiện tại là **GET** theo demo của VNPay — nếu SIT báo sai method thì báo em đổi.)

3. **Return URL** portal (nếu portal bắt khai): `http://localhost:8080/api/v1/public/payments/vnpay/return` — dùng được ngay, không cần tunnel.

4. **Lưu ý:** URL tunnel (trycloudflare/ngrok free) **đổi mỗi lần chạy lại** → lần sau chạy tunnel mới thì phải cập nhật IPN trong portal.

5. **Test IPN không cần chờ giao dịch thật:** dùng portal SIT
   (<https://sandbox.vnpayment.vn/vnpaygw-sit-testing/user/login>) để bắn các kịch bản IPN (hợp lệ / sai chữ ký / lặp lại).

---

## 5. Checklist test E2E trên giao diện

| # | Bước | Kỳ vọng |
|---|---|---|
| 1 | `docker compose up -d` — kiểm tra `hotel_backend` Up, liquibase `exited (0)` | Stack chạy |
| 2 | Mở <http://localhost:3000> → đăng nhập `admin / 123456` | Vào được |
| 3 | Vào **Folio** (`/finance/folios`) → *Mở Folio* của booking 4 → nút **Thu tiền** | Hiện tổng thu / số dư |
| 4 | Chọn **VNPAY QR** → **Tạo phiên thanh toán** (không bấm lần 2 — lock Redis 10') | Hiện `txnRef VNP...` + link "Mở cổng thanh toán" |
| 5 | Trình duyệt redirect sang `sandbox.vnpayment.vn` → nhập **thẻ test** (Mục 1) → OTP `123456` → xác nhận | Trang sandbox báo thanh toán thành công |
| 6 | VNPay redirect về `/api/v1/public/payments/vnpay/return` → BE verify chữ ký → **302** `/payment/result?status=PAID&paymentId=...` | Không cần bấm gì |
| 7 | Trang **Kết quả thanh toán** hiện "Thành công" + số tiền (poll BE 3s/lần) | Hiển thị `SUCCESS` **sau khi BE xác nhận** |
| 8 | (Đã cấu hình IPN ở Mục 4) log BE | Lần 1: `Confirm Success` · IPN gửi lại: `Already Confirmed` (idempotent) |
| 9 | Kiểm tra DB | `SELECT status, gateway_txn_id FROM payments WHERE booking_id=4;` → `SUCCESS`, mã `VNP...` |
| 10 | **Case thất bại:** tạo phiên mới → trên trang sandbox chọn *Hủy* | `/payment/result?status=FAILED` → `Payment FAILED` → lock đã giải phóng → tạo phiên mới được ngay |

---

---

## 6. Ma Trận Kịch Bản Kiểm Thử Nhập Liệu (Input Test Matrix)

Dưới đây là bộ test case nhập liệu chi tiết đối chiếu theo quy chuẩn kiểm thử ISTQB (`EP/BVA/Decision Table`):

| TC_ID | Kịch Bản Kiểm Thử | Dữ Liệu Nhập Vào (Test Data) | Thao Tác Thực Hiện | Kết Quả Kỳ Vọng |
|---|---|---|---|---|
| `TC_VNP_01` | Tạo phiên VNPay thành công với toàn bộ số dư | `bookingId: 4`, `amount: null` (mặc định lấy số dư) | Mở Folio booking 4 ➔ Chọn VNPAY ➔ Bấm "Tạo phiên thanh toán" | HTTP 201; Trả về `paymentUrl` có `vnp_Amount` = số dư × 100; Tạo Payment `PENDING`; Giữ khóa Redis 10'. |
| `TC_VNP_02` | Tạo phiên VNPay thanh toán một phần (Partial) | `bookingId: 4`, `amount: 500000` (nhỏ hơn số dư nợ) | Nhập số tiền 500.000đ ➔ Chọn VNPAY ➔ Bấm "Tạo phiên thanh toán" | HTTP 201; `vnp_Amount = 50000000`; Payment lưu đúng 500.000đ. |
| `TC_VNP_03` | Chặn tạo phiên khi số tiền vượt quá số dư còn nợ | `bookingId: 4`, `amount: 99999999` (vượt dư nợ) | Nhập số tiền vượt quá số dư ➔ Bấm tạo phiên | HTTP 400 (Mã `8002` / `INVALID_PAYMENT_AMOUNT`); Popup báo: "Số tiền thanh toán vượt quá số dư còn nợ". |
| `TC_VNP_04` | Chặn tạo phiên khi số tiền $\le 0$ | `bookingId: 4`, `amount: 0` hoặc âm | Nhập 0 hoặc để trống ➔ Bấm tạo phiên | Validation chặn ngay: "Số tiền thanh toán phải lớn hơn 0" (Mã `1002`). |
| `TC_VNP_05` | Chặn tạo phiên khi đơn đã thanh toán hoàn tất | Booking đã thanh toán đủ (`balance = 0`) | Bấm tạo phiên VNPay cho đơn đã hết nợ | HTTP 400 (Mã `8003` / `PAYMENT_ALREADY_COMPLETED`); Báo lỗi đơn đã hoàn tất. |
| `TC_VNP_06` | Spam click liên tiếp / Race condition | Click nút "Tạo phiên" 2 lần liên tục trong 10' | Bấm nút tạo phiên lần 2 khi phiên cũ chưa đóng | HTTP 409 (Mã `8005`); Popup thông báo đã có phiên đang diễn ra, yêu cầu chờ hoặc bấm "Hủy & làm lại". |
| `TC_VNP_07` | Khách thanh toán thành công thẻ NCB trên Sandbox | Thẻ NCB: `9704198526191432198`, Tên: `NGUYEN VAN A`, Ngày: `07/15`, OTP: `123456` | Mở trang VNPay Sandbox ➔ Nhập thông tin thẻ test ➔ Nhập OTP ➔ Bấm Thanh toán | Sandbox báo thành công ➔ Redirect về trang kết quả FE (`/payment/result`) ➔ Poll status thành `SUCCESS` ➔ Hóa đơn chuyển `PAID` (nếu hết nợ). |
| `TC_VNP_08` | Khách bấm Hủy trên cổng Sandbox VNPay | Nhấn nút "Hủy giao dịch" tại trang sandbox | Bấm Hủy trên màn hình cổng thanh toán | Sandbox redirect về `/payment/result?status=FAILED` ➔ Payment đổi sang `FAILED` ➔ Redis lock được mở ngay để tạo lại. |
| `TC_VNP_09` | IPN gửi webhook lặp lại (Idempotency) | Giả lập IPN gửi lần 2, lần 3 cùng mã `txnRef` | Bắn POST/GET IPN lặp lại | Server trả về HTTP 200 `{"RspCode":"00","Message":"Already Confirmed"}`; Không cộng dồn tiền lần 2. |
| `TC_VNP_10` | IPN bị sai lệch chữ ký HMAC | Tham số `vnp_SecureHash` bị thay đổi | Bắn IPN với hash sai | Server trả về HTTP 400 `{"RspCode":"97","Message":"Invalid Signature"}`; DB không thay đổi; Log mask chữ ký. |

---

## 7. Các Bước Tiến Hành Chỉnh Sửa & Triển Khai (Action Plan & Implementation Steps)

Để hệ thống hoàn toàn đồng bộ giữa Backend, Frontend và quy trình vận hành khách sạn thực tế, các bước thực hiện tuần tự như sau:

### 🔹 Bước 1: Nâng cấp Tầng Backend Core (G1, G2, G6)
1. **Mở rộng DTO Contract ([`CreatePaymentRequest.java`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/backend/src/main/java/vn/edu/utc/hotel_booking/modules/finance/gateway/dto/CreatePaymentRequest.java)):**
   - Bổ sung trường `BigDecimal amount` (tùy chọn).
   - Đặt validation `@DecimalMin(value = "0.01", message = "Số tiền thanh toán phải lớn hơn 0")`.
2. **Tính số dư còn nợ thực tế trong Service ([`PaymentServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/backend/src/main/java/vn/edu/utc/hotel_booking/modules/finance/service/impl/PaymentServiceImpl.java)):**
   - Inject `InvoiceRepository`.
   - Trong `createGatewayPayment`:
     - Lấy danh sách giao dịch thành công: `existingPayments.stream().filter(p -> status == SUCCESS)...`
     - Tính: $\text{remainingBalance} = \text{booking.totalAmount} - \sum \text{totalPaid}$.
     - Chặn nếu $\text{remainingBalance} \le 0$ $\rightarrow$ ném `PAYMENT_ALREADY_COMPLETED`.
     - Nếu client truyền `amount`: Kiểm tra $0 < \text{amount} \le \text{remainingBalance}$ (nếu vượt quá ném `INVALID_PAYMENT_AMOUNT`).
     - Nếu client không truyền `amount`: Gán $\text{paymentAmount} = \text{remainingBalance}$.
3. **Tự động chuyển Hóa đơn sang `PAID` khi hết nợ (G2):**
   - Trong `handleGatewayCallback`: Khi VNPAY báo thành công (`result.isSuccess()`), kiểm tra tổng tiền đã thanh toán $\ge$ giá trị đơn hàng $\rightarrow$ cập nhật `Invoice` sang `InvoiceStatus.PAID`.
4. **Bảo mật & Mask chữ ký trong Log (G6):**
   - Trước khi ghi log `result.getRawPayload()`, dùng regex thay thế `vnp_SecureHash` thành `****` để tránh lộ chữ ký trên console.

### 🔹 Bước 2: Nâng cấp Tầng Frontend UI & Type Contract (G1, G5)
1. **Cập nhật Interface Type ([`finance.types.ts`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/frontend/src/modules/finance/types/finance.types.ts)):**
   - Thêm `amount?: number` vào `GatewayCreateRequest`.
2. **Sửa lỗi hiển thị thẻ test & Xóa mock giả lập ([`InvoicePaymentFlowModal.tsx`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/frontend/src/modules/finance/components/payment/InvoicePaymentFlowModal.tsx)):**
   - Sửa lỗi typo thẻ NCB: đổi `97041985261914321` (17 số) thành đúng chuẩn 19 số: `9704198526191432198`.
   - Xóa bỏ đoạn mã fallback gán `TmnCode=DEMO001` giả lập nội bộ; chuyển sang hiển thị banner lỗi thật lấy từ backend qua `getApiErrorMessage`.
   - Truyền `amount: grandTotal` vào lời gọi API `financeService.createVnPayPayment`.
3. **Đồng bộ hóa màn hình Thu tiền Folio ([`FolioPaymentPage.tsx`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/frontend/src/modules/finance/pages/FolioPaymentPage.tsx)):**
   - Khi chọn VNPAY, truyền `amount: payAmount` để hệ thống tạo phiên đúng số tiền lễ tân muốn thu đợt này.

### 🔹 Bước 3: Cấu hình Môi trường & Khởi động Hệ thống
1. **Kiểm tra `.env`:** Đảm bảo đủ 4 biến `VNPAY_TMN_ID`, `VNPAY_HASH_SECRET`, `VNPAY_RETURN_URL`, `VNPAY_FE_RESULT_URL`.
2. **Khởi động dịch vụ:**
   ```powershell
   docker compose up -d
   ```
3. **Mở tunnel nhận Webhook IPN:**
   ```powershell
   cloudflared tunnel --url http://localhost:8080
   # Cập nhật URL https://xxxx.trycloudflare.com/api/v1/public/payments/vnpay/ipn lên Merchant Portal
   ```

### 🔹 Bước 4: Chạy Kiểm Thử Tự Động & E2E Verification
1. **Chạy Unit Test Backend:**
   ```powershell
   docker run --rm -v "${PWD}/backend:/app" -w /app maven:3.9.6-eclipse-temurin-21 mvn test "-Dtest=PaymentServiceTest,VnPayGatewayTest,VnPayCallbackTest"
   # Kỳ vọng: 34 tests pass, 0 fail (bao gồm test partial payment, balance limit, mask signature)
   ```
2. **Chạy Build Frontend:**
   ```powershell
   docker exec hotel_frontend npm run build
   # Kỳ vọng: tsc và vite build thành công 0 lỗi.
   ```
3. **Thực thi luồng E2E theo ma trận Mục 6 trên giao diện http://localhost:3000.**

---

## 8. Bảng Đối Chiếu Hiện Trạng Hạng Mục Công Việc

| Mã Hạng Mục | Nội Dung Công Việc | File Phụ Trách | Trạng Thái |
|---|---|---|---|
| **G1** | Tính số tiền theo số dư còn nợ, hỗ trợ thanh toán một phần, chặn vượt nợ | [`PaymentServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/backend/src/main/java/vn/edu/utc/hotel_booking/modules/finance/service/impl/PaymentServiceImpl.java) | 🟢 Đã thiết kế & Unit test pass |
| **G2** | Tự động chuyển Hóa đơn sang `PAID` khi công nợ về 0 | [`PaymentServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/backend/src/main/java/vn/edu/utc/hotel_booking/modules/finance/service/impl/PaymentServiceImpl.java) | 🟢 Đã tích hợp qua InvoiceRepository |
| **G3** | Thêm config query-DR đối soát VNPAY | `application.yaml` | ⚪ Chờ mở rộng giai đoạn sau |
| **G4** | Mẫu `.env.example` chuẩn hóa | Gốc repo | ✅ Đã hoàn tất |
| **G5** | Sửa thẻ test NCB 19 số, bỏ QR code, tập trung Thẻ ATM & bảng đối chiếu VNPay | [`InvoicePaymentFlowModal.tsx`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/frontend/src/modules/finance/components/payment/InvoicePaymentFlowModal.tsx) | 🟢 Đã hoàn tất |
| **G6** | Mask chữ ký trong log callback VNPAY | [`PaymentServiceImpl.java`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/backend/src/main/java/vn/edu/utc/hotel_booking/modules/finance/service/impl/PaymentServiceImpl.java) | 🟢 Đã tích hợp regex mask `****` |
| **G7** | Bổ sung unit tests cho số dư nợ và biên số tiền | [`PaymentServiceTest.java`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/backend/src/test/java/vn/edu/utc/hotel_booking/modules/finance/service/PaymentServiceTest.java) | 🟢 Đã bổ sung 3 test case (34/34 pass) |
| **G8** | Đối chiếu toàn diện dữ liệu giao dịch VNPAY theo Docs v2.1.0 | [`PaymentResultPage.tsx`](file:///c:/Users/Admin/Documents/GitHub/project-1-hotel-booking/frontend/src/modules/finance/pages/PaymentResultPage.tsx) | 🟢 Đã hoàn tất |

---

## 9. Bảng Đối Chiếu Dữ Liệu Kỹ Thuật VNPAY (Theo Chuẩn Docs v2.1.0)

Khi khách hàng thanh toán qua cổng VNPay (Thẻ ATM nội địa), dữ liệu trả về qua Return URL và Webhook IPN bao gồm các trường chuẩn sau:

| Trường Dữ Liệu | Tên Tham Số | Ví Dụ Giá Trị | Giải Nghĩa & Quy Chuẩn |
|---|---|---|---|
| **Mã tham chiếu đơn hàng** | `vnp_TxnRef` | `VNP20261013170530-1234` | Mã giao dịch do Merchant sinh, dùng để tìm bản ghi Payment trong DB. |
| **Mã giao dịch VNPAY** | `vnp_TransactionNo` | `14567890` | Mã giao dịch duy nhất do VNPAY sinh ra tại cổng. |
| **Mã giao dịch Ngân hàng** | `vnp_BankTranNo` | `VNP14567890` | Mã định danh ghi nhận tại ngân hàng thanh toán (NCB). |
| **Ngân hàng thanh toán** | `vnp_BankCode` | `NCB` | Mã ngân hàng phát hành thẻ. |
| **Loại thẻ / Phương thức** | `vnp_CardType` | `ATM` | Thẻ ghi nợ nội địa Napas. |
| **Số tiền thanh toán** | `vnp_Amount` | `150000000` | Số tiền quy đổi về VND bằng cách chia 100 (`1.500.000đ`). |
| **Thời gian thanh toán** | `vnp_PayDate` | `20261013170530` | Định dạng `yyyyMMddHHmmss`, hiển thị `13/10/2026 17:05:30`. |
| **Mã phản hồi kết quả** | `vnp_ResponseCode` | `00` | `00`: Thành công; Khác `00`: Tra cứu bảng mã lỗi VNPay (24: Hủy, 13: Sai OTP...). |
| **Tình trạng giao dịch** | `vnp_TransactionStatus` | `00` | `00`: Thành công. |
| **Mã Website Merchant** | `vnp_TmnCode` | `YD0M5DI1` | Mã Terminal Website của Merchant cấp bởi VNPAY. |
| **Nội dung đơn hàng** | `vnp_OrderInfo` | `Thanh toan dat phong BK...` | Mô tả đơn hàng gửi sang cổng. |
| **Chữ ký toàn vẹn** | `vnp_SecureHash` | `a3f8...512` | Ký HMAC-SHA512 để xác thực tính toàn vẹn dữ liệu. |


