# 📋 KẾ HOẠCH CÔNG VIỆC — MEMBER 3 (Fullstack Developer)

> **Hồ sơ:** Sinh viên **năm 4**, phong cách **vừa học vừa code (vibe code)** — kết hợp làm sản phẩm + tích lũy kỹ năng cho đồ án/tốt nghiệp.
> **Vai trò:** BE + FE cho Payment Gateway (VNPay/MoMo), POS Dịch vụ (F&B/Giặt là), Buồng phòng & Maintenance Tickets, Excel Rooming List, Optimization & Audit.
> **Tổng khối lượng:** 11.5 ngày công (MVP 6.5d + Cải tiến 1: 2.5d + Cải tiến 2: 2.5d)

---

## 1. Nhận diện hiện trạng codebase (KHÔNG code trùng — khảo sát 28/09)

### ✅ ĐÃ CÓ (người khác build sẵn — đọc hiểu & nối tiếp)

| Phần | File hiện có | Trạng thái |
| :--- | :--- | :--- |
| Finance BE | `modules/finance/` (Payment, Invoice, Transaction, PaymentController: create/filter/complete/refund) | Sẵn CRUD, **thiếu webhook cổng thanh toán** |
| POS BE | `modules/operation/` (ServiceOrder, Menu, Housekeeping, NightAudit controllers) | Sẵn API POST chi phí vào Folio |
| Room Block | `inventory/entity/RoomMaintenanceBlock` + Repository | **Chỉ có Entity/Repo — thiếu Service/Controller** |
| FE Thanh toán | `modules/finance/pages/FolioPaymentPage.tsx`, `booking-portal/pages/CheckoutPage.tsx` | Chọn VNPAY/MoMo ở UI nhưng **chưa gọi API thật** |
| FE POS & HK | `RoomServiceOrderPage.tsx`, `HousekeepingPage.tsx`, `MenuListPage.tsx` | Sẵn khung |

### ❌ THIẾU (đây là việc của bạn)

1. **T06:** VNPay/MoMo webhook + verify chữ ký + idempotency (chưa có dòng nào).
2. **T09:** Maintenance Ticket API + Dated Room Block (OOO/OOS) UI — entity có sẵn, thiếu endpoint.
3. **U12:** Excel import — `pom.xml` **chưa có Apache POI**.
4. **U24:** Query optimization, Redis cache, RBAC audit, stress test.

---

## 2. ⚠️ RULES BẮT BUỘC ĐỌC TRƯỚC KHI CODE (`.agents/rules/`)

Đây là "pháp luật dự án" — vi phạm = PR bị reject. Tóm tắt cho từng nhóm việc của bạn:

### 2.1. Rules cốt lõi (01-core-ai-guidelines)
- **Zero Hallucination:** không đoán tên cột/method/endpoint — tra code thật hoặc hỏi.
- **Zero-Placeholder:** KHÔNG để `TODO`/`TBD`/`// thêm logic tại đây` — file phải compile được ngay.
- **Cascade Backend bắt buộc:** `Entity ➔ DTO & SearchDto ➔ Repository ➔ Service (IF + Impl) ➔ Controller ➔ Unit Test`.
- **Cascade Frontend bắt buộc:** `DTO/Model ➔ Helper/Mapper ➔ API Service ➔ UI Component ➔ Routes & Menu`.
- Thay đổi Entity dùng chung → kiểm tra module phụ thuộc trước.

### 2.2. Rules Backend (02-backend-standards)
- Filter ≥2 trường → **`POST /api/v1/<resource>/filter`** + `@RequestBody SearchDto` (không GET query dài).
- **Migration chỉ qua Liquibase**, cấu trúc 3 tầng:
  `db/changelogs/incremental/<YYYY>/YYYYMMDD-HHmmss-<mo_ta>.xml` + `db/sqlFile/incremental/...sql`
  (không DDL tay — file của bạn nằm ở `incremental/2026/`, KHÔNG phải `changelogs/06-...`).
- **RBAC mọi endpoint mới:** `@PreAuthorize("hasRole('ROLE_<MODULE>_<ACTION>')")`.
  Roles hệ thống: `ROLE_CHAIN_ADMIN`, `ROLE_REGION_MANAGER`, `ROLE_PROPERTY_MANAGER`,
  `ROLE_RECEPTIONIST`, `ROLE_HOUSEKEEPING`, `ROLE_CUSTOMER`.
- Unit test Service phải đủ **3 kịch bản**: happy path / validation (ID sai, data sai) / edge case (list rỗng, ký tự `%_`).
- Controller chỉ nhận HTTP + validate + phân quyền — **không business logic**, không trả Entity (luôn DTO).
- Cross-module: gọi qua **Service interface**, không gọi Repository module khác.

### 2.3. Rules Frontend (03 + 05 design tokens)
- **Màu:** CTA/nút chính = đỏ `red-600`/`#EE0000` • nền `bg-white` • chữ tiêu đề `text-neutral-900` • không dark mode.
- **Tokens:** nút/input radius `12px` (`rounded-xl`), card/modal `16px` (`rounded-2xl`);
  chiều cao control `48px` desktop (`h-12`) / `40px` ≤920px (`h-10`).
- **Bảng CRUD = layout 3 hàng:** (1) Filter bar → (2) Toolbar → (3) Table + Pagination.
- Mobile-first, semantic HTML (`<main>/<section>`…), nút icon có `aria-label`, focus `focus:ring-2 focus:ring-red-600`.
- **Chống memory leak:** polling/subscription trong `useEffect` PHẢI có cleanup return.
- Form: lỗi hiện khi `touched`, disable nút khi loading, `trim()` trước khi submit.
- Hành động nguy hiểm (xóa/refund) → **Modal xác nhận** (nút đỏ danger + loading).
- ⚠️ *Lưu ý cấu trúc:* rule viết mẫu `src/app/pages/...` nhưng codebase đang dùng
  `src/modules/<feature>/{pages,services,types}` + `core/api/client.ts` → **theo codebase hiện có** (giữ nhất quán).

### 2.4. Rules Concurrency & Pricing (`hotel-concurrency-and-pricing.md`)
- **Payment window → Redis Session Lock TTL 10 phút** (không phải 15') — tránh cạn pool DB khi khách redirect ra cổng thanh toán; thanh toán fail/hết hạn → worker tự giải phóng kho.
- **Checkout online → Optimistic Lock** (`@Version`): conflict → `OptimisticLockingFailureException` → **HTTP 409**.
- **Walk-in → Double Pessimistic Lock**, thứ tự chống deadlock: `room_slots` TRƯỚC → `room_availability` SAU.
- **Sparse data:** không sinh slot trống trước cho ngày tương lai (cấm cron/seed tạo row rỗng).
- **Tài chính bất biến:** snapshot `unit_price`, `vat_rate`, `discount_amount` vào `booking_details`/`invoice_details`/`booking_charges`; lịch sử **không JOIN lại catalog**.
- Module isolation: giao tiếp giữa module qua service interface, chung 1 DB.

### 2.5. Rules QA (04) — áp khi viết test case
- ISTQB: EP, BVA, Decision Table, State Transition.
- Mã `TC_001` tuần tự, Steps phân cách bằng dấu `;`, đủ 8 phạm vi (UI, Validation, Business, Permission, Workflow, CRUD, Search, Error Handling).

---

## 3. QUY TRÌNH "VỪA HỌC VỪA CODE" (mỗi task = 3 giai đoạn)

```
📘 HỌC (20-30'):  Đọc rules liên quan + tài liệu + để AI giải thích codebase có sẵn
🤖 CODE (60-90'): AI generate theo Cascade → BẮT BUỘC đọc hiểu từng dòng → sửa theo style dự án
✅ TEST (20-30'): Unit test 3 kiểu (happy/validation/edge) + test tay Swagger → ghi sổ tay
```

**Quy tắc vàng (SV năm 4 — học để hiểu, không học thuộc):**
- Không copy code mà không hiểu — tập **giải thích lại** (hỏi AI: "giải thích dòng này làm gì?").
- Mỗi session = **1 task nhỏ** → commit git ngay (tuân thủ tên branch).
- Ghi lỗi & bài học vào `docs/nhat-ky-member3.md` — đến cuối kỳ bạn có sổ tay kỹ thuật + portfolio.
- **Đọc rules trước mỗi sprint** (file 01→06, ~10 phút) — tránh code xong bị reject vì vi phạm quy chuẩn.

**Skills hỗ trợ trong project — gọi khi làm việc tương ứng:**

| Skill | Dùng khi |
| :--- | :--- |
| `hotel-api-development` | Tạo Controller/Service/Repository/DTO Backend |
| `hotel-db-schema-design` | Thêm bảng/cột/index + Liquibase migration |
| `hotel-frontend-feature` | Tạo trang/component React mới |
| `hotel-pricing-concurrency-verify` | Test đồng thời, chống double-book, verify pricing |

---

## 4. LỊCH TRIỂN KHAI THEO SPRINT

### 🟢 SPRINT 1 — T06: Payment Gateway BE & FE (3.0 ngày)
> 📄 **Cây thư mục chi tiết:** `docs/tree-sprint1-T06.md`

**Mục tiêu:** Khách bấm "Thanh toán VNPay/MoMo" → redirect cổng thanh toán → IPN webhook → cập nhật Payment/Booking.

**Ngày 1 — Học (0.5d) + Thiết kế (0.5d)**
- 📘 Học: flow VNPay sandbox (create URL → redirect → IPN → verify HMAC-SHA512), webhook vs callback, idempotency.
- 📘 Đọc code: `PaymentController`, `PaymentServiceImpl`, `CheckoutPage.tsx`, `FolioPaymentPage.tsx`.
- 📐 Contract-First — viết API spec trước khi code:
  ```
  POST /api/v1/payments/vnpay/create              [JWT + @PreAuthorize ROLE_PAYMENT_CREATE]
  GET  /api/v1/public/payments/vnpay/ipn          [PUBLIC — verify chữ ký, idempotent]
  GET  /api/v1/public/payments/vnpay/return       [PUBLIC — browser redirect → FE]
  POST /api/v1/public/payments/momo/callback      [PUBLIC — idempotent]
  ```
- ⚙️ Cấu hình sandbox keys vào `application.yaml` + `.env` (không commit secret).

**Ngày 2 — Code BE (1.0d)** — theo đúng Cascade:
- Branch: `feature/T06-payment-webhook`
- Thứ tự: Migration (Liquibase incremental) → `Payment` fields → `gateway/` (Interface + VnPay + MoMo) → Service (+idempotency, `@Transactional`) → Callback Controller (+`SecurityConfig` permitAll) → Unit Test (3 kiểu).
- **Điểm học quan trọng:** idempotency — IPN gọi lại 2 lần không được update 2 lần → guard `status == PENDING`.

**Ngày 3 — Code FE (1.0d)** — theo đúng Cascade FE:
- Branch: `feature/T06-checkout-modal`
- Thứ tự: types → service (`createVnPayPayment`) → components (CheckoutModal, CountdownBadge) → trang (`PaymentResultPage`) → routes.
- Countdown đồng bộ `expiresAt` BE trả về (Redis TTL lock **10'** theo rules — không hardcode 15').
- ⚠️ Polling trạng thái PHẢI có cleanup trong `useEffect`.
- ✅ Test e2e bằng thẻ sandbox VNPay.

**DoD T06:**
- [ ] Thanh toán sandbox thành công VNPAY + MOMO
- [ ] Callback gọi 2 lần → chỉ update 1 lần (idempotency)
- [ ] 2 endpoint `create` có `@PreAuthorize`, endpoint IPN/return public
- [ ] Unit test 3 kiểu pass • Swagger cập nhật • không TODO/compile error

---

### 🟢 SPRINT 2 — T09: POS Dịch vụ & Maintenance/Buồng phòng (3.5 ngày)

**2A. POS F&B / Giặt là (1.5d)** — trên nền `ServiceOrder` có sẵn
- 📘 Hiểu: ServiceOrder → BookingCharge → Folio (post chi phí).
- FE: chọn món → tạo order → xác nhận post vào folio → **Modal xác nhận** trước khi charge.
- **Bất biến tài chính:** charge đã ghi không sửa/xóa — chỉ tạo record mới (refund).

**2B. Maintenance Tickets & Dated Room Block OOO/OOS (2.0d)**
- Branch: `feature/T09-maintenance-ticket`
- BE: Liquibase `incremental/2026/...-maintenance-ticket.xml` → Entity → DTO → Repo → Service → Controller (`@PreAuthorize ROLE_HOUSEKEEPING_*`) → Test; link `RoomMaintenanceBlock` (entity sẵn).
- **Sparse data:** block chỉ INSERT row khi có sự kiện block thật — không sinh slot trống.
- FE: form OOO/OOS theo ngày + hiển thị trên HousekeepingPage (3-row layout).
- 📘 Status machine: `DIRTY → CLEANING → READY`, `READY → MAINTENANCE → READY`.

**DoD T09:**
- [ ] POST order F&B vào folio thành công
- [ ] Block OOO → booking engine không nhận đặt phòng đó (test thật)
- [ ] Unit test 3 kiểu + Swagger + phân quyền

---

### 🟡 SPRINT 3 — U12: Excel Rooming List Import (2.5 ngày — Cải tiến 1)

- Branch: `feature/U12-excel-rooming-list`
- 📘 Học: Apache POI (`XSSFWorkbook`), map row → BookingGuest/StayGuest.
- BE: thêm POI vào `pom.xml` → `ExcelParserService` → `POST /api/v1/bookings/{id}/rooming-list/import`.
- Xử lý lỗi từng row (dòng sai → báo đúng dòng đó, không fail cả file) — test theo **BVA** (file 0 dòng, file 0 cột, file >500 dòng).
- FE: upload → **bảng preview** → confirm (cascade: types → service → upload component → route).
- ✅ Chuẩn bị file Excel mẫu (hợp lệ + file lỗi) để test.

**DoD U12:** import 20 dòng → đúng 20 bản ghi; file lỗi → báo đúng dòng lỗi.

---

### 🟡 SPRINT 4 — U24: System Optimization & Security Audit (2.5 ngày — Cải tiến 2)

- 📘 Học: `EXPLAIN ANALYZE`, Redis cache `@Cacheable`, `@PreAuthorize`.
- Tasks:
  1. `EXPLAIN ANALYZE` query chậm → thêm index (qua Liquibase `sqlFile/incremental`).
  2. Redis Cache cho Rate Plans (`@Cacheable`) — tôn trọng TTL rules.
  3. **RBAC audit:** rà mọi endpoint mới của Member 3 đã có `@PreAuthorize` chưa; map theo `ROLE_<MODULE>_<ACTION>`.
  4. Stress test: gọi song parallel API booking (skill `hotel-pricing-concurrency-verify`) → verify optimistic lock trả 409, không double-book.
- Deliverable: báo cáo trước/sau (ms) + checklist lỗ hổng bảo mật.

---

## 5. QUY TRÌNH GIT & DOD (phiếu phân công)

```bash
feature/T06-payment-webhook      feature/U12-excel-rooming-list
feature/T06-checkout-modal       feature/U24-security-audit
feature/T09-maintenance-ticket

# Quy trình: push branch → mở PR → ≥1 Peer Review → Member 2 merge
```

**DoD chung:** không lỗi lint/compiler • 100% unit test pass • Swagger cập nhật • không regression • **không TODO placeholder** • endpoint mới có RBAC.

---

## 6. CHECKLIST KỸ NĂNG (tích lũy — làm nền cho đồ án tốt nghiệp)

### Backend (Java 21 / Spring Boot 4)
- [ ] Layered Architecture 8 tầng theo Cascade
- [ ] Search DTO + `POST /filter` • Soft delete `is_deleted = 0`
- [ ] Liquibase migration 3 tầng (changelogs + sqlFile + root)
- [ ] Spring Security OAuth2 + `@PreAuthorize` RBAC (Keycloak)
- [ ] Unit test JUnit 5 + Mockito (3 kiểu)
- [ ] `@Transactional`, idempotency, optimistic/pessimistic locking
- [ ] Apache POI (Excel) — *U12* • Redis Cache — *U24*

### Frontend (React 19 / TypeScript / Tailwind v4)
- [ ] Cascade FE: types → helper → service → component → route
- [ ] Design tokens (radius 12/16, height 48/40, red CTA, bảng 3 hàng)
- [ ] react-hook-form + zod • A11Y (aria-label, focus ring, semantic HTML)
- [ ] Cleanup `useEffect` chống memory leak • Modal xác nhận hành động nguy hiểm

### Kiến trúc & Nghiệp vụ
- [ ] Webhook flow (verify chữ ký + idempotency) • Contract-First
- [ ] Hybrid Locking Matrix (Redis TTL 10' / Optimistic / Pessimistic)
- [ ] Bất biến tài chính (snapshot, không JOIN lịch sử) • Sparse data
- [ ] Git flow & Code Review • ISTQB test design

---

## 7. PHỤ THUỘC TEAM

- **T06** cần Booking DRAFT→CONFIRMED của **Member 2 (T08)** → hỏi sớm.
- **U12** cần GIT booking (Member 2 - U11) → làm song song parser BE được.
- **T09** OOO block liên kết `room_availability` → đồng bộ **Member 4 (Timeline Grid T10)**.

---

## 8. MỤC TIÊU HÀNG TUẦN (nhịp độ SV năm 4)

| Tuần | Việc | Kết quả học được |
| :--- | :--- | :--- |
| Tuần 1 | Rules + T06 ngày 1-2 (học + BE webhook) | Hiểu flow thanh toán + Liquibase + RBAC |
| Tuần 2 | T06 ngày 3 (FE) + review | Idempotency + useEffect cleanup + tokens |
| Tuần 3 | T09 POS (1.5d) | Folio flow + bất biến tài chính |
| Tuần 4 | T09 Maintenance (2.0d) | Entity-Service-Controller cascade thành thạo |
| Tuần 5 | U12 Excel (2.5d) | Apache POI + xử lý lỗi từng row |
| Tuần 6 | U24 Optimization (2.5d) | EXPLAIN + cache + RBAC audit + stress test |

> 💡 **Mẹo vibe code (năm 4 — hướng tới portfolio & phỏng vấn):**
> Mỗi buổi: mở đầu 10' đọc rules liên quan + để AI giải thích file sắp sửa;
> giữa buổi code theo cascade; kết thúc commit + ghi 3 dòng sổ tay
> "hôm nay học được gì, lỗi nào gặp, cách fix". Cuối kỳ bạn có:
> sản phẩm chạy thật + sổ tay kỹ thuật + 4 task có unit test để phỏng vấn.
