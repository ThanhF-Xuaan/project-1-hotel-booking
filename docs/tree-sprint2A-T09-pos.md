# 🌳 CÂY THƯ MỤC — SPRINT 2A: POS F&B / GIẶT LÀ (1.5 ngày)

> **Nguồn:** Kế hoạch `docs/ke-hoach-member3.md` → SPRINT 2 → **2A. POS F&B / Giặt là (1.5d)**
> **Branch đề xuất:** `feature/T09-pos-fnb` (nhánh riêng, làm xong PR → Member 2 merge)
> **Quy ước:** 🟢 = file MỚI cần tạo • 🔵 = file CÓ SẴN cần SỬA • ⚪ = file có sẵn, không đụng tới
> **Đối tượng:** SV năm 4 vừa học vừa code — mỗi mục ghi rõ **Rules áp dụng**.
> **Khảo sát code + DB:** 07/10/2026 (đọc trực tiếp repo + `hotel_booking_db` thật)

---

## 📏 RULES ÁP DỤNG CHO SPRINT NÀY (đọc trước — `.agents/rules/`)

| Rule | Nội dung áp dụng vào 2A |
| :--- | :--- |
| **Cascade Backend** | Đã đủ tầng cho CRUD → việc sửa đi theo thứ tự: Migration → Service → Controller → Unit Test |
| **Cascade Frontend** | Sửa page theo thứ tự: types → service → component → page (không nhảy tầng) |
| **Zero-Placeholder** | Không để `TODO`/`TBD` — mọi file compile ngay sau khi sửa |
| **Liquibase (02)** | Sửa DB CHỈ qua `db/changelogs/incremental/2026/` + `db/sqlFile/incremental/2026/` — **không DDL tay** |
| **Bất biến tài chính (Concurrency §4)** | Charge đã ghi **không sửa/xóa** → hủy đơn chỉ tạo **record bù (refund/âm)** mới |
| **RBAC (02)** | Giữ nguyên `@PreAuthorize` hiện có — endpoint POS: `CHAIN_ADMIN, PROPERTY_MANAGER, RECEPTIONIST, F_AND_B` |
| **Unit Test (02)** | Đủ 3 kiểu: happy / validation / edge — mở rộng test sẵn có, không viết lại từ đầu |
| **FE Tokens (05)** | Modal `rounded-2xl`, nút CTA `bg-red-600 rounded-xl h-12`, chữ `text-neutral-900` |
| **Modal nguy hiểm (03)** | Hành động **charge tiền vào Folio** = nguy hiểm → **BẮT BUỘC Modal xác nhận** |
| **Memory Leak (03)** | `useEffect` gọi API phải có cleanup/không polling thừa |
| **Zero Hallucination** | Tên bảng/cột/endpoint/enum trong file này đều **tra từ code & DB thật** (xem mục 0) |

---

## 0. TRẠNG THÁI THỰC TẾ (rà soát 07/10 — đã code + đã đọc DB)

### 0.1 ✅ BACKEND ĐÃ CÓ (không làm lại)

| # | Thành phần | Vị trí | Ghi chú |
| :-- | :--- | :--- | :--- |
| 1 | Entity `ServiceOrder` / `ServiceOrderDetail` | `modules/operation/entity/` | Detail có **snapshot** `unit_price/vat_rate/service_fee_rate` (bất biến tài chính ✓) |
| 2 | Enum status | `ServiceOrderStatus.java` | `PENDING, PREPARING, DELIVERED, COMPLETED, CANCELLED` ⚠️ **không có `CONFIRMED`** |
| 3 | Enum loại món | `MenuType.java` | `PRODUCT` (trừ kho) / `SERVICE` (giặt là, spa… dùng cái này) |
| 4 | Repository | `ServiceOrderRepository`, `ServiceOrderDetailRepository`, `MenuRepository`, `CatalogItemRepository` | Đủ |
| 5 | DTO | `ServiceOrderCreateRequest` (+`OrderItemRequest`), `ServiceOrderSearchDto`, `ServiceOrderResponse` | Đủ, có `@Valid` |
| 6 | **Tính tiền** | `ServiceOrderServiceImpl.createOrder()` d.63-183 | Subtotal → **phí dịch vụ 5%** → **VAT theo `vat_rules`** (qua `tax_category`) → total |
| 7 | **Trừ kho** | same, d.106-117 | Chỉ `PRODUCT`: check `catalog_items.stock_quantity` → thiếu kho → `OUT_OF_STOCK` |
| 8 | **Đẩy Folio** | same, d.155-172 | Mỗi item 1 `BookingCharge` loại `SERVICE` vào `booking_room` → hiện trong Folio/Thu ngân |
| 9 | **Hoàn kho khi hủy** | `updateStatus()` d.230-240 | `CANCELLED` → cộng lại stock |
| 10 | Filter (POST Search DTO) | `filter()` d.193-218 | Specification: bookingId, roomInstanceId, orderNumber, status ✓ đúng rules 02 |
| 11 | Controller + RBAC | `ServiceOrderController` | `/create`, `/filter`, `/{id}`, `PUT /{id}/status?status=` — đã `@PreAuthorize` ✓ |
| 12 | Mapper | `OperationMapper.toResponse()` | ✓ |
| 13 | Unit test sẵn | `ServiceOrderServiceTest` (4 test), `MenuServiceTest` (7 test) | `mvn test` **151/0/1 xanh** (07/10) |

### 0.2 ✅ FRONTEND ĐÃ CÓ

**🔗 Đường vào trên app thật** (đăng nhập `http://localhost:3000/login` → `admin/123456`):

| Sidebar (nhóm **VẬN HÀNH & NHÀ HÀNG**) | Route URL | Trang | File |
| :--- | :--- | :--- | :--- |
| **"Thực đơn & Dịch vụ"** (`Sidebar.tsx` d.88-89) | `/operation/menus` | Quản lý danh mục món (CRUD, lọc PRODUCT/SERVICE) | `pages/MenuListPage.tsx` |
| **"Gọi Dịch vụ Phòng"** (`Sidebar.tsx` d.94-95) | `/operation/room-service` | **POS đặt món** — chọn món → giỏ hàng → đặt đơn → trừ kho → charge vào Folio | `pages/RoomServiceOrderPage.tsx` |

| # | Thành phần | Vị trí | Ghi chú |
| :-- | :--- | :--- | :--- |
| 1 | Gọi API thật | `operation/services/operation.service.ts` d.21-61 | Đủ 9 hàm: menu CRUD + `getServiceOrders` + `createServiceOrder` + `updateServiceOrderStatus` |
| 2 | Trang gọi món | `operation/pages/RoomServiceOrderPage.tsx` | Giỏ hàng (cart), lọc keyword/`ALL·PRODUCT·SERVICE`, đặt đơn, xem đơn gần đây, làm mới tồn kho |
| 3 | Trang quản lý món | `operation/pages/MenuListPage.tsx` | CRUD menu đầy đủ (create/update/delete/filter) |
| 4 | Route + menu | `App.tsx` d.60-61 • `Sidebar.tsx` d.88-95 | `/operation/room-service`, `/operation/menus` đã đăng ký + link sidebar đủ |
| 5 | Types | `operation/types/operation.types.ts` | ✓ |

> ⚠️ **Có sẵn nhưng CHƯA chạy được trọn vẹn** (3 thiếu sót = việc 2A, xem 0.4): chưa có Modal xác nhận
> trước khi charge • chưa có nút Hủy đơn + charge bù • phòng đang gõ tay thay vì chọn `CHECKED_IN`.
> Thêm nữa: **bug CHECK DB (L)** khiến bấm "Đặt đơn" thật **có thể fail 500** khi ghi charge folio — test thử trước khi code.

### 0.3 ✅ DATABASE ĐÃ CÓ (đọc `hotel_booking_db` thật — 07/10)

| Bảng | Cột trọng yếu | Số dòng | Dữ liệu mẫu |
| :--- | :--- | :--- | :--- |
| `service_orders` | `order_number`(UNIQUE, trigger tự sinh `SO-...`), `booking_id`, `room_instance_id`, `sub_total`, `service_fee_rate/amount`(CHECK 0-100), `vat_amount`, `total_amount`, `status`(FK qua enum Java), `issued_at` | **0** | chưa từng chạy thật |
| `service_order_details` | `menu_id` FK→menus, `item_type` CHECK `PRODUCT/SERVICE`, `quantity` CHECK >0, snapshot `unit_price/vat_rate/...` | **0** | — |
| `menus` | `hotel_id`, `tax_category_id`, `menu_type`, `name`, `base_price`, `status`, `is_deleted` | **30** | 15 `PRODUCT` (Bia Heineken 50k, Nước Lavie 20k, Buffet 250k…) + 15 `SERVICE` |
| `catalog_items` | `stock_quantity` (kho cho `PRODUCT`) | 30 | mọi món còn **100** |
| `services` | `pricing_type` (FK id→menus) | 15 | bảng phụ pricing cho `SERVICE` |
| `booking_charges` | `charge_type`, snapshot tiền, `quantity` CHECK >0 | **0** | chưa có charge nào |

### 0.4 ❌ KHOẢNG HỞ — ĐÂY LÀ VIỆC CẦN LÀM

| # | Khoảng hở | Mức độ | Bằng chứng |
| :-- | :--- | :--- | :--- |
| **L** | 🔴 **BUG CHẶN LUỒNG:** DB CHECK `chk_booking_charge_type` chỉ cho `EARLY_CHECKIN, LATE_CHECKOUT, PENALTY, OTHER` — **thiếu `'SERVICE'`** (mà `BookingChargeType.java` enum Java CÓ `SERVICE`). → `POST /service-orders/create` sẽ **fail CHECK violation** khi ghi charge vào folio — chưa từng chạy thật (0 dòng data) | 🔴 Phải sửa trước | `pg_constraint` tra thật 07/10 |
| **G1** | ❌ **Không có Modal xác nhận trước khi charge** — plan 2A yêu cầu "Modal xác nhận trước khi charge"; grep `Modal/confirm/xác nhận` trong `RoomServiceOrderPage.tsx` = **0 kết quả** → bấm "Đặt đơn" là charge ngay lập tức | 🔴 Bắt buộc (rule 03) |
| **G2** | ❌ **Hủy đơn không đảo charge folio** — `updateStatus→CANCELLED` hoàn kho nhưng `BookingCharge` đã ghi vẫn nằm trên Folio → khách bị tính tiền món đã hủy (vi phạm "không sửa/xóa charge — phải tạo record bù") | 🔴 Bắt buộc (bất biến TC) |
| **G3** | ⚠️ FE gõ tay `bookingId=1`, `roomInstanceId=1` (hardcode default) — dễ chọn sai phòng, không lấy phòng đang `CHECKED_IN` | 🟡 Cải tiến |
| **G4** | ⚠️ Controller `@Operation` ghi "CONFIRMED, DELIVERED..." nhưng enum **không có `CONFIRMED`** → tài liệu Swagger sai | 🟡 Sửa chữ |
| **G5** | ⚠️ Test chưa assert **charge ra folio đúng số tiền** (test chỉ mock, không kiểm BookingCharge) | 🟡 Bổ sung |
| **G6** | ℹ️ "Giặt là" chưa có món trong menu → chỉ cần **thêm item `SERVICE` bằng MenuListPage** (không cần code) | 🟢 Không cần code |

> **Kết luận:** Sprint 2A **không làm CRUD từ đầu** (đã đủ) — việc thật = **1 migration sửa CHECK +
> logic charge/bù charge + Modal xác nhận FE + test** ≈ 1.5 ngày như plan.

---

## 1. KẾ HOẠCH LÀM VIỆC (1.5 ngày — 3 buổi)

### 📅 Buổi 1 (0.5d) — 📘 Hiểu + 🔴 Fix bug chặn

1. 📘 **Đọc flow** (20'): `ServiceOrder → BookingCharge → Folio` — đọc `createOrder()` d.155-172 và mở Folio UI `localhost:3000/finance/folios` để nhìn điểm cuối.
2. 🔴 **Migration sửa CHECK** (30') — theo Rules 02 (3 tầng):
   - `db/changelogs/incremental/2026/YYYYMMDD-HHmmss-fix-booking-charge-type.xml`
   - `db/sqlFile/incremental/2026/YYYYMMDD-HHmmss-fix-booking-charge-type.sql`
   - Nội dung: `ALTER TABLE booking_charges DROP CONSTRAINT chk_booking_charge_type;`
     `ALTER TABLE booking_charges ADD CONSTRAINT chk_booking_charge_type CHECK (charge_type IN ('EARLY_CHECKIN','LATE_CHECKOUT','PENALTY','SERVICE','OTHER'));`
   - ⚠️ **ĐỤNG DB → CẦN ANH DUYỆT trước khi chạy** (quy tắc của dự án).
3. ✅ **Test tay** (30'): `POST /api/v1/service-orders/create` (body mẫu mục 3) → 201 → mở Folio thấy charge `SERVICE`.

### 📅 Buổi 2 (0.5d) — 🔵 Code BE

| # | Việc | File | Rules |
| :-- | :--- | :--- | :--- |
| B1 | **Hủy đơn → tạo charge bù**: khi `status=CANCELLED`, với mỗi detail đã tạo charge → tạo **1 `BookingCharge` record MỚI** âm (`subtotal/total_amount` âm — DB không có CHECK chặn số âm; `quantity` giữ >0 vì `chk_booking_charge_qty`) với `itemName = "Hủy <order_number>: <tên món>"` | `ServiceOrderServiceImpl.updateStatus()` 🔵 | Bất biến TC: không sửa/xóa record cũ |
| B2 | (Phương án, chọn 1) **Tách bước charge**: tạo đơn `PENDING` **chưa charge** → `PUT /{id}/status?status=PREPARING` mới charge. *Khuyến nghị **không chọn** trong 1.5d — đổi flow = sửa nhiều + test lại Folio; dùng Modal FE (M3) là đủ đúng plan* | — | — |
| B3 | Sửa chữ Swagger: `CONFIRMED` → `PREPARING, DELIVERED, COMPLETED` | `ServiceOrderController.java` 🔵 | Zero Hallucination |
| B4 | Unit test **3 kiểu** × mở rộng (mục 5) | `ServiceOrderServiceTest.java` 🔵 | Rules 02 |

### 📅 Buổi 3 (0.5d) — 🔵 Code FE + e2e

| # | Việc | File | Rules |
| :-- | :--- | :--- | :--- |
| F1 | **Modal xác nhận trước khi charge**: bấm "Đặt đơn" → hiện Modal (`rounded-2xl`) liệt kê giỏ hàng + tổng tiền + *"Số tiền sẽ được hạch toán vào Folio của phòng X"* → nút CTA đỏ `Xác nhận đặt` (`bg-red-600 rounded-xl h-12`) + nút "Hủy" → mới gọi `createServiceOrder` | `RoomServiceOrderPage.tsx` 🔵 (dùng lại `core/components/ui/Modal.tsx` ⚪) | Modal nguy hiểm (03) + tokens (05) |
| F2 | **Chọn phòng đang lưu trú** thay vì gõ tay: dropdown lấy danh sách `bookingRoom` `CHECKED_IN` (gọi `POST /api/v1/bookings/filter` hoặc endpoint phòng đang dùng) → auto điền `bookingId` + `roomInstanceId` | `RoomServiceOrderPage.tsx` 🔵 | Đúng G3 |
| F3 | Nút **Hủy đơn** gọi `updateServiceOrderStatus(id,'CANCELLED')` + hiện thông báo charge bù đã tạo | `RoomServiceOrderPage.tsx` 🔵 | Rule 03 (đã có `orderErrorMessage`) |
| F4 | **E2E tay**: đặt món F&B → xem Folio có charge → hủy → xem charge bù → kiểm kho hoàn | — | DoD T09 |

---

## 2. CÂY THƯ MỤC SPRINT 2A

```text
project-1-hotel-booking/
│
├── backend/src/main/resources/db/
│   ├── changelogs/incremental/2026/
│   │   └── YYYYMMDD-HHmmss-fix-booking-charge-type.xml      🟢 MỚI — ChangeSet Liquibase
│   │                                                          (id/author đầy đủ, gọi SQL bên dưới)
│   └── sqlFile/incremental/2026/
│       └── YYYYMMDD-HHmmss-fix-booking-charge-type.sql       🟢 MỚI — thêm 'SERVICE' vào
│                                                              CHECK chk_booking_charge_type
│                                                              ⚠️ CẦN ANH DUYỆT (đụng DB)
│                                                              ⚠️ Rules 02: KHÔNG DDL tay
│
├── backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/
│   ├── controller/
│   │   └── ServiceOrderController.java                       🔵 SỬA — sửa @Operation:
│   │                                                          "CONFIRMED, DELIVERED, CANCELLED"
│   │                                                          → đúng enum thật (PREPARING,
│   │                                                          DELIVERED, COMPLETED, CANCELLED)
│   │
│   ├── service/impl/
│   │   └── ServiceOrderServiceImpl.java                      🔵 SỬA — updateStatus():
│   │                                                          + CANCELLED → tạo BookingCharge
│   │                                                            BÙ (record mới, số âm) cho
│   │                                                            từng detail — KHÔNG sửa/xóa
│   │                                                            charge cũ (Bất biến tài chính)
│   │                                                          + giữ nguyên hoàn kho (đã có)
│   │
│   ├── service/
│   │   └── ServiceOrderService.java                          ⚪ không sửa (4 method đủ)
│   ├── entity/  DTO/  repository/  mapper/                   ⚪ không sửa (đủ tầng Cascade)
│   └── entity/BookingChargeType — (nằm ở modules/booking)    ⚪ enum ĐÃ có SERVICE (lỗi ở DB)
│
├── backend/src/test/java/.../modules/operation/service/
│   └── ServiceOrderServiceTest.java                          🔵 SỬA — thêm test (xem mục 5):
│                                                               + createOrder_Success → assert
│                                                                 BookingCharge type=SERVICE,
│                                                                 tổng tiền = subtotal+fee+VAT
│                                                               + updateStatus_Cancelled →
│                                                                 assert tạo charge bù (âm),
│                                                                 charge cũ không đổi
│                                                               + edge: hủy đơn đã hủy → 80xx
│
└── frontend/src/modules/operation/pages/
    └── RoomServiceOrderPage.tsx                              🔵 SỬA — 3 việc (buổi 3):
                                                                + Modal xác nhận TRƯỚC KHI charge
                                                                  (rounded-2xl, CTA red-600 h-12)
                                                                + dropdown phòng CHECKED_IN
                                                                  (bỏ gõ tay bookingId/roomId)
                                                                + nút Hủy đơn + thông báo charge bù

    (Không đụng: operation.service.ts ⚪ đủ 9 hàm • operation.types.ts ⚪ •
     MenuListPage.tsx ⚪ CRUD đủ • App.tsx ⚪ route đã có •
     core/components/layout/Sidebar.tsx ⚪ 2 link menu đã trỏ đúng route •
     core/components/ui/Modal.tsx ⚪ dùng lại)
```

---

## 3. API CONTRACT (giữ nguyên 4 endpoint — chỉ đổi hành vi hủy)

```text
POST /api/v1/service-orders/create                 [JWT + F_AND_B/...]
  body: { bookingId, roomInstanceId, items: [{ menuId, quantity }] }
  └─ 201 { orderNumber:"SO-...", status:"PENDING", subTotal, serviceFeeAmount, vatAmount, totalAmount }
     → MỖI item 1 BookingCharge(type=SERVICE) vào folio (sau khi fix CHECK)
     → PRODUCT: trừ catalog_items.stock_quantity

PUT  /api/v1/service-orders/{id}/status?status=PREPARING|DELIVERED|COMPLETED|CANCELLED   [JWT]
  └─ 200 { ...status mới }
     → CANCELLED: hoàn kho  +  TẠO CHARGE BÙ (record mới, số âm) — không sửa charge cũ
     → CANCELLED 2 lần: 400 AppException (giữ guard sẵn có)

POST /api/v1/service-orders/filter                 [JWT]  body: { bookingId?, roomInstanceId?, orderNumber?, status?, page?, pageSize? }
GET  /api/v1/service-orders/{id}                   [JWT]
```

> **Test body mẫu (mục 1.3):**
> `{"bookingId":6,"roomInstanceId":1,"items":[{"menuId":2,"quantity":2},{"menuId":121... }]}` →
> dùng menu `id=2` (Nước Suối 20k, PRODUCT) + 1 món `SERVICE` để test cả 2 nhánh.

---

## 4. THỨ TỰ LÀM + ESTIMATE (đúng Cascade — mỗi việc 1 commit)

| # | Giai đoạn | Việc | File | Est |
| :-- | :--- | :--- | :--- | :-- |
| 1 | 📘 Học | Đọc flow ServiceOrder→Charge→Folio + rules 02/03/05 | — | 0.5h |
| 2 | 🔴 DB | Migration fix CHECK `charge_type` (**chờ duyệt**) + chạy | 2 file Liquibase 🟢 | 0.5h |
| 3 | ✅ Test tay | POST create thật → 201 → Folio thấy charge | Swagger | 0.5h |
| 4 | 🔵 BE | `updateStatus()` tạo charge bù khi CANCELLED | `ServiceOrderServiceImpl` | 1.5h |
| 5 | 🔵 BE | Sửa chữ Swagger | `ServiceOrderController` | 0.1h |
| 6 | 🔵 Test | Mở rộng `ServiceOrderServiceTest` (3 kiểu, mục 5) | test file | 1h ✅ |
| 7 | 🔵 FE | Modal xác nhận trước charge | `RoomServiceOrderPage` | 1.5h |
| 8 | 🔵 FE | Dropdown phòng CHECKED_IN + nút hủy đơn | `RoomServiceOrderPage` | 1.5h |
| 9 | ✅ E2E | Đặt → Folio → hủy → charge bù → kho hoàn + `mvn test` | — | 1h |
| | | **Tổng ≈ 7.1h ≈ 1.5 ngày** (khớp plan 2A) | | |

---

## 5. UNIT TEST 3 KIỂU (Rules 02 — mở rộng test sẵn)

| Kiểu | Test case (tên method gợi ý) | Kỳ vọng |
| :--- | :--- | :--- |
| **Happy** | `createOrder_Success` *(đã có — bổ sung assert)* | 201; `BookingCharge.chargeType = SERVICE`; `total = sub + fee5% + VAT`; stock giảm đúng số |
| **Happy** | `updateStatus_Cancelled_CreatesCompensatingCharge` | Charge cũ **giữ nguyên**; xuất hiện charge bù số âm = tổng detail; kho cộng lại |
| **Validation** | `createOrder_RoomNotOccupied_ThrowsException` *(đã có)* | `AppException` đúng mã |
| **Validation** | `updateStatus_CancelledTwice_ThrowsException` | Đã hủy → hủy nữa → 400 (`INVALID_SERVICE_ORDER_STATUS`) |
| **Edge** | `createOrder_OutOfStock_ThrowsException` *(đã có)* |stock < quantity → `OUT_OF_STOCK`, **không charge nào được ghi** (rollback) |
| **Edge** | `updateStatus_Cancelled_NoChargeLeftOnFolio` | Đơn hủy xong → tổng charge folio = charge bù (âm) → net 0 |

---

## 6. DoD SPRINT 2A (khớp DoD T09 của plan)

- [ ] `POST /service-orders/create` **chạy thật** (không lỗi CHECK) → charge hiện trong Folio
- [ ] Bấm đặt món ở FE → **Modal xác nhận hiện ra** với đúng tổng tiền → xác nhận mới charge
- [ ] Hủy đơn → **charge bù** xuất hiện trên Folio, charge cũ không bị sửa/xóa (bất biến TC)
- [ ] Hủy đơn → tồn kho `PRODUCT` cộng lại
- [ ] Unit test **3 kiểu** pass • `mvn test` không regression (baseline 151/0/1)
- [ ] Swagger đúng mô tả • không TODO/placeholder • endpoint giữ nguyên `@PreAuthorize`
- [ ] **Không commit** khi chưa được lệnh (quy tắc hiện tại của anh)

---

## ⚠️ LƯU Ý ĐẶC BIỆT

1. **Việc #2 (migration sửa CHECK) ĐỤNG DATABASE → em KHÔNG tự làm — chờ anh duyệt** (như lần liquibase checksum trước).
2. Bug CHECK này có thể **chưa ai phát hiện** vì `service_orders`/`booking_charges` = 0 dòng (test đều mock) — sửa xong phải test tay thật.
3. Đừng sửa `PaymentMethod`/`payment-luong-5-phuong-thuc.md` — tài liệu payment **cấm đụng**.
4. Làm trên nhánh riêng `feature/T09-pos-fnb`; nhánh `develop` = **chỉ đọc, không sửa**.
