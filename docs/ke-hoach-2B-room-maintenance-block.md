# 📋 KẾ HOẠCH CHI TIẾT — TASK 2B: ROOM MAINTENANCE BLOCK (OOO/OOS)

> **Nguồn:** `docs/ke-hoach-member3.md` → SPRINT 2 → **2B. Maintenance Tickets & Dated Room Block OOO/OOS (2.0 ngày)**
> **Branch (khi được lệnh code):** `feature/T09-maintenance-ticket` — KHÔNG tự merge vào `develop`/`main`
> **Ký hiệu:** 🟢 = file MỚI cần tạo • 🔵 = file CÓ SẴN cần SỬA • ⚪ = có sẵn, không đụng
> **Khảo sát source + DB thật:** 18/10/2026 (đọc trực tiếp repo; mọi tên bảng/cột/file/line đều tra từ code — Zero Hallucination)
> **Trạng thái tài liệu:** CHỈ LÊN KẾ HOẠCH — chưa code (lệnh hiện tại: *chỉ viết docs*)

---

## 0. RULES ÁP DỤNG (đọc trước — `.agents/rules/` + skills)

| Rule / Skill | Nội dung áp dụng vào 2B |
| :--- | :--- |
| **Cascade Backend** (01/02) | Thứ tự bắt buộc: Migration → Entity/Repo → DTO/SearchDto → Service (IF+Impl) → Controller → Unit Test |
| **Cascade Frontend** (01/03) | types → helper/service → component → page → route (không nhảy tầng) |
| **Zero-Placeholder** (01) | Không `TODO`/`TBD` — mọi file compile ngay sau khi sửa |
| **Liquibase 3 tầng** (02) | DB chỉ đổi qua `db/changelogs/incremental/2026/YYYYMMDD-HHmmss-*.xml` + `db/sqlFile/incremental/2026/*.sql` — **không DDL tay** |
| **POST /filter** (02) | ≥2 trường lọc → `POST /api/v1/<resource>/filter` + `@RequestBody <Entity>SearchDto` |
| **RBAC** (02) | Mọi endpoint mới phải `@PreAuthorize("hasAnyRole(...)")` theo style codebase hiện tại (xem §7) |
| **Unit test 3 kiểu** (02) | Happy / Validation / Edge — mockito `@ExtendWith(MockitoExtension.class)` |
| **Sparse data** (skill DB + concurrency) | Không sinh slot trống trước; block là "trạng thái lệch chuẩn" → chỉ INSERT khi có sự kiện thật |
| **Lock thứ tự** (concurrency) | `room_slots` TRƯỚC → `room_availability` SAU (tránh deadlock) |
| **FE tokens** (05) | CTA `bg-red-600 rounded-xl h-12`, card/modal `rounded-2xl`, input `rounded-xl h-12`, bảng 3 hàng |
| **Modal lỗi nguy hiểm** (03) | Lỗi API → **Modal popup** (`core/api/error.ts` + `Modal.tsx`, KHÔNG toast); thao tác hủy/kết thúc → `ConfirmModal` |
| **Cross-module** (02) | Giao tiếp module qua **Service interface** — không gọi Repository module khác |

---

## 1. THỰC TRẠNG (tra code thật 18/10/2026)

### 1.1 ✅ BACKEND ĐÃ CÓ (không làm lại)

| # | Thành phần | Vị trí | Ghi chú |
| :-- | :--- | :--- | :--- |
| 1 | **Entity `RoomMaintenanceBlock`** | `modules/inventory/entity/RoomMaintenanceBlock.java` (L20–60) | Bảng `room_maintenance_blocks`; field: `roomInstance(@ManyToOne)`, `blockType` default `"OOO"`, `startDate/endDate (LocalDate)`, `reason`, `status` default `"ACTIVE"`, `maintenanceTicketId`, `createdByStaffId`, `createdAt/updatedAt` (KHÔNG kế thừa BaseEntity, tự `@CreationTimestamp/@UpdateTimestamp`) |
| 2 | **Repository** | `modules/inventory/repository/RoomMaintenanceBlockRepository.java` (L10–13) | Chỉ có `JpaRepository` + `findByRoomInstanceId(Integer)` — **thiếu query overlap/status** |
| 3 | **DB bảng + chống trùng** | `db/sqlFile/01-init-schema.sql:410–440`; `02-functions-and-triggers.sql:182–189` | CHECK `block_type IN ('OOO','OOS')`, `status IN ('ACTIVE','COMPLETED','CANCELLED')`, `start_date <= end_date`; **EXCLUDE gist** `ex_room_maintenance_overlap`: 1 phòng không có 2 block ACTIVE chồng nhau (daterange `'[]'` — kín cả 2 đầu) |
| 4 | **`room_slots` gắn block** | `01-init-schema.sql:979`, entity `RoomSlot.java:32–34` | Cột `maintenance_block_id FK → room_maintenance_blocks (ON DELETE SET NULL)`; status CHECK có sẵn `'BLOCKED'`, `'MAINTENANCE'` |
| 5 | **`room_availability.ooo_rooms`** | `01-init-schema.sql:456–464`, entity `RoomAvailability.java:46–57` | Cột `ooo_rooms` + col sinh `available_count = total - booked - locked - ooo` (**DB tự tính**), có `@Version version` |
| 6 | **Trạng thái phòng** | `RoomInstance.java:36–38` | `currentStatus String` default `"READY"` — comment: `READY, OCCUPIED, CLEANING, MAINTENANCE` (**KHÔNG có enum Java, KHÔNG có `DIRTY` trong CHECK DB**, không có `SELECTED` — grep 0 kết quả) |
| 7 | **Housekeeping module** | `modules/operation/` — `HousekeepingController` (`/api/v1/housekeeping`, L27/38), `HousekeepingServiceImpl` | `VALID_STATUSES = READY, OCCUPIED, CLEANING, DIRTY, MAINTENANCE` (L35); set status + mirror vào slot hôm nay (L67–72); **nút "Bảo trì" hiện chỉ set `MAINTENANCE` không có ngày/không có lý do** |
| 8 | **Chuyển trạng thái phòng (4 chỗ)** | `BookingServiceImpl` d.555 (check-in→OCCUPIED), d.588 (check-out→CLEANING); `RoomInstanceServiceImpl.updateRoomStatus` d.97–104 (`PATCH /{id}/status`, **không validate giá trị**); `HousekeepingServiceImpl` d.53–75 | |
| 9 | **Tìm phòng trống (cấp loại)** | `PublicBookingSearchServiceImpl.checkAvailableRooms` d.348–370 → `RoomAvailabilityServiceImpl.getAvailability` d.48–90 → `min(availableCount)` cả kỳ | **Không hề xét trạng thái phòng vật lý hay block** |
| 10 | **Chống overbooking hiện tại** | `holdInventory` d.111–119, `batchHoldInventory` d.198–206 (`ROOM_NOT_AVAILABLE` 409); `@Version` trên `RoomAvailability`; DB `chk_inventory_consistency` (`booked+locked <= total`, **chưa gồm `ooo_rooms`**); `BookingRoomRepository.findOverlappingAssignedRooms` (d.25–34) tại `createBooking` d.195 và `assignRoom` d.467 | |
| 11 | **Lock sẵn có NHƯNG CHƯA AI DÙNG** | `RoomAvailabilityRepository` d.24–37 và `RoomSlotRepository` d.24–37: 4 method `@Lock(PESSIMISTIC_WRITE) ...WithLock(...)` | grep `WithLock(` trong main → **0 chỗ gọi** |
| 12 | **Resolve nhân viên tạo** | Pattern `UtilityReadingServiceImpl.resolveCurrentStaffId()` d.183–191 (`SecurityUtils.getCurrentUserKeycloakId → staffRepository`) | Dùng lại y hệt cho `createdByStaffId` / `updatedByStaffId` |
| 13 | **ErrorCode / Exception** | `common/exception/ErrorCode.java`, `AppException`, `GlobalHandlerException` | Đã có `ROOM_NOT_AVAILABLE(5031,409)`, `ROOM_ALREADY_ASSIGNED(7022,409)`, `OPTIMISTIC_LOCK_CONFLICT(4091)`; **handler `ROOM_ALREADY_BLOCKED` bị COMMENT tại `GlobalHandlerException.java:104,122`** và `ErrorCode.ROOM_ALREADY_BLOCKED` chưa tồn tại |
| 14 | **Roles/Permissions** | Keycloak `realm-export.json` L46–47: `HOUSEKEEPING`, `ENGINEERING`; DB `04-master-data.sql:34–35,97–101` + `20260928-143000-add-rbac-roles.sql:27–35,93–100` | Đã có permission `ROOM_STATUS`(VIEW/UPDATE), `MAINTENANCE_TICKET`(CREATE); HOUSEKEEPING + ENGINEERING + PROPERTY_MANAGER đã được gán 2 permission này; **chưa có resource `MAINTENANCE_BLOCK`** |

### 1.2 ✅ FRONTEND ĐÃ CÓ

| # | Thành phần | Vị trí | Ghi chú |
| :-- | :--- | :--- | :--- |
| 1 | **HousekeepingPage** | `frontend/src/modules/operation/pages/HousekeepingPage.tsx` (329 dòng) | KPI + tab READY/OCCUPIED/DIRTY/CLEANING/MAINTENANCE + lưới thẻ phòng; nút **"Bảo trì" d.311–318 chỉ PUT status `MAINTENANCE`** (không ngày/giờ/lý do) |
| 2 | **Gọi API HK** | `modules/operation/services/operation.service.ts` d.66–77 | `getHousekeepingRooms(hotelId,status)` → `GET /housekeeping/rooms`; `updateRoomHousekeepingStatus` → `PUT /housekeeping/rooms/{id}/status` |
| 3 | **RoomInstanceListPage** | `modules/inventory/pages/RoomInstanceListPage.tsx` (636 dòng) | CRUD phòng + badge MAINTENANCE + select đổi status nhanh; layout 3 hàng ✓; `ConfirmModal` xóa ✓ |
| 4 | **Route HK** | `App.tsx` d.63–65 `/operation/housekeeping` | Roles: `CHAIN_ADMIN, PROPERTY_MANAGER, RECEPTIONIST, HOUSEKEEPING` — **thiếu `ENGINEERING`** |
| 5 | **Cơ chế lỗi** | `core/api/error.ts` (`getApiErrorMessage`), `core/components/ui/Modal.tsx` (`Modal`, `ConfirmModal`), pattern `finance/components/payment/PaymentErrorModal.tsx` | **Popup Modal — KHÔNG toast** (quy ước dự án) |
| 6 | Types/thư mục | `modules/<feature>/{pages,services,types}` (KHÔNG có `src/pages/`, `src/router/`) | Theo codebase hiện có, không theo rule mẫu `src/app/...` |

> ❌ **Chưa có:** types/service/component/section nào cho maintenance block; bảng danh sách block; form tạo block theo ngày; xử lý lỗi trên HousekeepingPage mới chỉ `console.error` (d.30–31,47–48) — viễn cảnh lỗi API → Modal.

### 1.3 ✅ DATABASE ĐÃ CÓ / ⚠️ THIẾU

| Bảng/cột | Trạng thái |
| :--- | :--- |
| `room_maintenance_blocks` | ✅ Có (baseline `01-init-schema.sql:410–440` + tạo lại `20260930-1805-repair-missing-tables.sql:48–63`) — **0 dòng dữ liệu, chưa từng dùng** |
| `ex_room_maintenance_overlap` (EXCLUDE gist, ACTIVE) | ✅ Chống 2 block ACTIVE trùng nhau ngay ở DB |
| `room_availability.ooo_rooms` | ✅ Cột có — ❌ **Java chưa bao giờ ghi (`setOooRooms` = 0 chỗ gọi)**; `getOooRooms` chỉ đọc ở `RoomAvailabilityServiceImpl:69,114,201` |
| `room_slots.maintenance_block_id` + status `'BLOCKED'` | ✅ Cột/CHECK có — ❌ chưa code ghi |
| Index cho `room_maintenance_blocks` | ❌ **Chưa có** (`03-indexes.sql` không có gì cho bảng này) |
| Cột `updated_by` / `updated_by_staff_id` | ❌ Bảng block **chỉ có `created_by_staff_id`** — thiếu "người cập nhật" |
| `maintenance_tickets` (bảng phiếu bảo trì) | ❌ **Chưa tồn tại** (chỉ có cột nullable `maintenance_ticket_id` không FK) → **OUT OF SCOPE 2B lần này** (xem §11) |
| `chk_room_instance_status` | ⚠️ Chỉ cho `READY, OCCUPIED, CLEANING, MAINTENANCE` — `HousekeepingServiceImpl` lại ghi `DIRTY` → **latent bug sẵn có** (xem §11) |

### 1.4 ❌ KHOẢNG TRỐNG = VIỆC CỦA 2B

| # | Khoảng trống | Mức độ |
| :-- | :--- | :--- |
| **G1** | ❌ **Thiếu cả tầng Service/Controller/DTO/Mapper/ErrorCode** cho block (entity+repo trơ trọi) | 🔴 Core |
| **G2** | ❌ `room_availability.ooo_rooms` không được ghi → **block không ảnh hưởng tìm phòng** (DoD T09 "Block OOO → booking engine không nhận đặt" sẽ FAIL) | 🔴 Core |
| **G3** | ❌ `createBooking` (chọn `roomInstanceId` walk-in) d.186–201, `assignRoom` d.441–488, `checkIn` d.532–572 **không kiểm block nào cả** → gán/check-in được phòng đang bảo trì | 🔴 Core |
| **G4** | ❌ Chưa có `ErrorCode` block + handler bị comment (`GlobalHandlerException.java:104,122`) | 🔴 |
| **G5** | ❌ Lock (`...WithLock`) khai báo nhưng 0 chỗ dùng → race block-vs-booking chưa được serialize | 🟡 |
| **G6** | ❌ FE không có form/danh sách block theo ngày; nút "Bảo trì" hiện tại không đủ dữ liệu (không ngày/không lý do) | 🔴 |
| **G7** | ❌ Index + `updated_by_staff_id` cho `room_maintenance_blocks` | 🟡 |

---

## 2. QUYẾT ĐỊNH NGHIỆP VỰ (trả lời trực tiếp các câu hỏi của task)

### 2.1 Ý nghĩa OOO vs OOS (theo SRS US21/FR-INV-06 — `document/word/srs.md`)

| | **OOO (Out of Order)** | **OOS (Out of Service)** |
| :--- | :--- | :--- |
| Ý nghĩa | Phòng **trừ khỏi quỹ phòng bán được** trong kỳ | Ngừng phục vụ — **chỉ chặn gán phòng vật lý này** |
| `room_availability.ooo_rooms` | **++/-- mỗi đêm** trong `[startDate..endDate]` → `available_count` tự giảm (DB sinh) | **Không đụng** (quỹ phòng loại vẫn giữ — vẫn bán được phòng khác cùng loại) |
| `room_slots` (từng đêm trong kỳ, từ hôm nay trở đi) | Tạo/cập slot → `status='BLOCKED'`, `maintenance_block_id=block.id` | Không đụng slot |
| Chặn gán phòng (`assignRoom`/`checkIn`/`createBooking` với `roomInstanceId`) | ✅ Chặn | ✅ Chặn |
| `room_instances.current_status` | → `MAINTENANCE` | → `MAINTENANCE` |

> Ghi chú OOO: cột `available_count` là col sinh của DB → Java chỉ việc ghi `ooo_rooms` (tăng khi tạo block, giảm khi kết thúc/hủy). Với dòng `room_availability` chưa tồn tại (sparse) → **get-or-create** đúng như fallback trong `getAvailability` d.73–85 (`totalRooms = hotelRoomType.getTotalQuantity()`).

### 2.2 Quy ước thời gian (ranh giới, timezone)

- Schema hiện tại là **`DATE`** (không có giờ) → 2B dùng **ngày**, không tự ý đổi sang `TIMESTAMP` (đổi schema = migration lớn, không cần trong scope).
- Block hiệu lực **kín cả 2 đầu**: `[startDate..endDate]` (khớp EXCLUDE `daterange(...,'[]')`).
- Booking chiếm đêm `[checkInDate .. checkOutDate-1]` (quy ước `[checkIn, checkOut)` như `findOverlappingAssignedRooms`).
- **Công thức xung đột block ↔ booking (dùng nguyên trong JPQL):**
  ```
  block.startDate <= bd.checkOutDate  AND  block.endDate >= bd.checkInDate
  ```
  (tương đương giao 2 tập đêm; đã viết lại so với công thức booking↔booking `checkIn < :out AND checkOut > :in` vì đầu `endDate` là KÍN).
- Thời gian server: `LocalDate.now()` phía BE — **FE chỉ gửi ngày, không gửi giờ/phạm vi tin cậy**; không tin `status`, `createdByStaffId` từ FE.

### 2.3 Quy tắc TẠO block (BE validate — thứ tự)

1. Phòng tồn tại, `isDeleted = false` → sai → `ROOM_INSTANCE_NOT_FOUND(5021)`.
2. `blockType ∈ {OOO, OOS}`; `startDate <= endDate` (DTO `@NotNull` + service check) → sai → mã lỗi thời gian mới (§7).
3. **Chồng block:** query `findActiveOverlap(roomId, start, end)` → đã có ACTIVE overlap → `ROOM_ALREADY_BLOCKED` (409). *Bảo hiểm cuối:* DB EXCLUDE bắt `DataIntegrityViolationException` → map về cùng mã lỗi.
4. **Phòng đang `OCCUPIED`** (có khách check-in) → **TỪ CHỐI** (`ROOM_ALREADY_BLOCKED`, message rõ "đang có khách lưu trú"). Không có cách nào tạo block mà ảnh hưởng booking đã xác nhận mà không có phương án xử lý → **không cho tạo**.
5. **Booking đã gán phòng này** (EXPECTED/CHECKED_IN, giao công thức §2.2) → **TỪ CHỐI**. (Booking chưa gán phòng cụ thể — loại online — không chặn tạo block; việc gán phòng sau đó sẽ tự tránh block.)
   - **`Selected`:** grep toàn repo = **0 kết quả** → trạng thái này KHÔNG tồn tại trong hệ thống; việc "khách chọn phòng" là walk-in truyền `roomInstanceId` thẳng vào `createBooking` → đã covered bởi bước 5 + 6.
6. **OOO thêm:** với mọi đêm `[start..end]`: `ooo_rooms + booked + locked >= total` (dòng sparse chưa có thì total = `totalQuantity`) → nếu `available - 1 < 0` → **TỪ CHỐI** `ROOM_NOT_AVAILABLE(5031)` (tránh đẩy quỹ phòng âm → overbooking ngầm). OOS bỏ qua bước này.
7. Ghi dữ liệu: `status='ACTIVE'`, `createdByStaffId` = `resolveCurrentStaffId()` (pattern `UtilityReadingServiceImpl:183–191`), `createdAt/updatedAt` tự có. **Không nhận `status`/`createdByStaffId` từ FE.**

### 2.4 Quy tắc CẬP NHẬT / KẾT THÚC / HỦY (không phá lịch sử)

```
              ┌──────────────► COMPLETED  (kết thúc — trước/sau endDate)
  ACTIVE ─────┤
              └──────────────► CANCELLED  (hủy trước/sở tại)
  COMPLETED / CANCELLED ──(update/complete/cancel lại)──► 400 AppException (kết thúc, không sửa ngược)
```

- **Update (PUT /{id}):** chỉ khi `status = ACTIVE`; cho sửa `reason, notes, startDate, endDate`; **bắt buộc chạy lại TOÀN BỘ validation §2.3** (chồng block, booking, công suất OOO) trên khoảng mới — trong cùng transaction: nhả `ooo_rooms` khoảng cũ → áp khoảng mới. Không sửa được `status`, `createdByStaffId`.
- **Complete/Cancel:** chỉ từ `ACTIVE`; ghi `updatedByStaffId` (cột mới, §9) + `updatedAt` (trigger `update_updated_at_column()` đã có).
- **KHÔNG có endpoint xóa** (kể cả soft-delete) → bảo toàn lịch sử bảo trì (task: *"không cho sửa dữ liệu tùy tiện làm sai lịch sử"*).
- **Đưa phòng về trạng thái sau khi kết thúc/hủy — KHÔNG tự `READY` cứng:**

| Điều kiện (ưu tiên từ trên xuống) | `current_status` sau unblock |
| :--- | :--- |
| Còn ≥1 block ACTIVE khác của phòng | giữ `MAINTENANCE` |
| Có khách `CHECKED_IN`/phòng `OCCUPIED` | `OCCUPIED` (phòng sạch, trả khách) |
| Slot hôm nay `DIRTY`/`CLEANING` hoặc status hiện tại `CLEANING` | `CLEANING` |
| Còn lại | `READY` |

- Mirror về `room_slots` hôm nay (nếu dòng slot tồn tại) đúng như `HousekeepingServiceImpl` d.67–72 — slot `BLOCKED` của block vừa kết thúc → `READY`, xóa `maintenance_block_id` (giữ dòng, không xóa — nhất quán `releaseRoomSlots` d.424–437).

### 2.5 Ba tầng dữ liệu phải đồng bộ trong 1 transaction (viết hoa cho dễ map code)

| Tạo block OOO | Kết thúc/Hủy block OOO |
| :--- | :--- |
| ① `room_instances.current_status = 'MAINTENANCE'` | ① Tính lại `current_status` theo bảng §2.4 |
| ② `room_slots` từng đêm ≥ hôm nay → `BLOCKED` + `maintenance_block_id` | ② slot那些 → `READY` + clear `maintenance_block_id` |
| ③ `room_availability.ooo_rooms++` từng đêm (get-or-create sparse) | ③ `ooo_rooms--` từng đêm (floor 0, chỉ trừ đêm đã cộng) |

OOS chỉ làm ① (và chặn gán phòng ở luồng booking — §6).

### 2.6 Chống race / overbooking

1. **DB EXCLUDE** `ex_room_maintenance_overlap` chặn block-vs-block ở mức DB (transaction cuối).
2. **Serialize block-vs-booking/assignment** bằng悲观 lock dòng `room_instances`: thêm method `findByIdWithLock` (`@Lock(PESSIMISTIC_WRITE)`) vào **`RoomInstanceRepository`** (cùng module inventory — hợp lệ), gọi ở: `create/update/complete/cancel` của block service **và** `assignRoom`/`checkIn`/`createBooking(roomInstanceId)` của `BookingServiceImpl` (booking đang inject sẵn `roomInstanceRepository` → không vi phạm mới).
3. **Thứ tự lock** theo rule: nếu 1 luồng cần cả slot + availability → `room_slots` trước, `room_availability` sau (khớp `hotel-concurrency-and-pricing.md`).
4. Optimistic `@Version` trên `room_availability` là lớp bảo vệ cuối khi ghi `ooo_rooms` → bắt `OptimisticLockingFailureException` → 409 (`ErrorCode.OPTIMISTIC_LOCK_CONFLICT(4091)` đã có; **handler đang comment — xem §11 rủi ro**).
5. OOO tạo dòng sparse mới → check lại công suất ngay trong transaction (§2.3 bước 6) — tránh 2 luồng OOO cùng lúc đẩy `available_count` xuống âm (DB hiện **không có CHECK `ooo_rooms >= 0`** → bổ sung trong migration §9, chờ duyệt).

### 2.7 Phân quyền (theo style codebase — `@PreAuthorize` + system roles)

> Plan gốc ghi `ROLE_HOUSEKEEPING_*` nhưng thực tế 100+ endpoint trong repo dùng `hasAnyRole('CHAIN_ADMIN','PROPERTY_MANAGER',...)` (vd `HousekeepingController.java:28,38`) → **theo codebase, không tạo role mới**.

| Endpoint | Roles |
| :--- | :--- |
| Tạo / Update / Complete / Cancel | `CHAIN_ADMIN, PROPERTY_MANAGER, HOUSEKEEPING, ENGINEERING` |
| Filter / Get / Get-by-room | các role trên **+ `RECEPTIONIST`** (lễ tân cần thấy phòng đang block trước khi gán) |

- FE: route `/operation/housekeeping` hiện **thiếu `ENGINEERING`** → 🔵 sửa `App.tsx` d.63–65 (bổ sung khi code FE).
- Không tin `userId`/`status` từ FE; staff_id lấy qua `SecurityUtils` phía service.

---

## 3. API CONTRACT (mới — chưa có endpoint nào trùng)

```text
POST /api/v1/maintenance-blocks/create                [JWT + CHAIN_ADMIN|PROPERTY_MANAGER|HOUSEKEEPING|ENGINEERING]
  body: { roomInstanceId, blockType: "OOO"|"OOS", startDate:"YYYY-MM-DD", endDate:"YYYY-MM-DD", reason, notes? }
  └─ 201 { id, roomInstanceId, roomNumber, blockType, startDate, endDate, reason, notes,
           status:"ACTIVE", createdByStaffId, createdAt }
     OOO: ooo_rooms++ từng đêm + slot BLOCKED + room → MAINTENANCE
     409 ROOM_ALREADY_BLOCKED (chồng block / đang OCCUPIED / có booking gán)
     409 ROOM_NOT_AVAILABLE (OOO làm available_count < 0)
     400 khoảng thời gian sai

PUT  /api/v1/maintenance-blocks/{id}                   [JWT + 4 roles trên]
  body: { startDate?, endDate?, reason?, notes? }      → chỉ khi status=ACTIVE, re-validate toàn bộ

PUT  /api/v1/maintenance-blocks/{id}/complete          [JWT + 4 roles]  ACTIVE → COMPLETED (+giải phóng)
PUT  /api/v1/maintenance-blocks/{id}/cancel            [JWT + 4 roles]  ACTIVE → CANCELLED (+giải phóng)

POST /api/v1/maintenance-blocks/filter                 [JWT + RECEPTIONIST]
  body: { roomInstanceId?, hotelId?, blockType?, status?, fromDate?, toDate?, page?, pageSize? }   (SearchDto)
GET  /api/v1/maintenance-blocks/{id}                   [JWT]
GET  /api/v1/maintenance-blocks/room/{roomInstanceId}  [JWT]   — lịch sử block theo phòng (đã có sẵn repo method → mở rộng)
```

- Response chuẩn `ApiResponse.success(result)` / `PageResponse` (như `UtilityMeterController`).
- Lọc ≥2 trường → `POST /filter` đúng rule 02.

---

## 4. CÂY THƯ MỤC (dự kiến — ước lượng ~20 file)

```text
backend/src/main/resources/db/
├── changelogs/incremental/2026/
│   └── YYYYMMDD-HHmmss-maintenance-block-index-audit.xml     🟢 (gọi sqlFile — CHỜ DUYỆT §9)
└── sqlFile/incremental/2026/
    └── YYYYMMDD-HHmmss-maintenance-block-index-audit.sql     🟢 — index + updated_by_staff_id + CHECK ooo_rooms

backend/src/main/java/vn/edu/utc/hotel_booking/
├── common/exception/
│   ├── ErrorCode.java                                        🔵 + 3 mã block (§7, TRA dãy số trước)
│   └── GlobalHandlerException.java                           🔵 bật lại handler ROOM_ALREADY_BLOCKED
│                                                              (d.104,122 — đang comment)
├── modules/inventory/
│   ├── entity/RoomMaintenanceBlock.java                      🔵 nhẹ — thêm @Column notes? (nếu dùng cột notes → migration §9)
│   ├── repository/RoomMaintenanceBlockRepository.java        🔵 + findActiveOverlap, findByStatusPaged, ...
│   ├── repository/RoomInstanceRepository.java                🔵 + findByIdWithLock (@Lock PESSIMISTIC_WRITE)
│   ├── dto/request/  RoomMaintenanceBlockCreateRequest.java  🟢 (@Valid, @NotNull roomInstanceId/blockType/dates/reason)
│   │                        RoomMaintenanceBlockUpdateRequest.java 🟢
│   │                        RoomMaintenanceBlockSearchDto.java     🟢 (page,pageSize + 6 filter)
│   ├── dto/response/ RoomMaintenanceBlockResponse.java       🟢
│   ├── mapper/       RoomMaintenanceBlockMapper.java         🟢 (componentModel="spring")
│   ├── service/      RoomMaintenanceBlockService.java        🟢 (create/update/complete/cancel/filter/getByRoom)
│   ├── service/impl/ RoomMaintenanceBlockServiceImpl.java    🟢 — core nghiệp vụ §2
│   └── controller/   RoomMaintenanceBlockController.java     🟢 — @PreAuthorize §2.7
├── modules/inventory/service/RoomAvailabilityService.java    🔵 + applyOooDelta(roomTypeId, from, to, delta)
│   └── impl/RoomAvailabilityServiceImpl.java                 🔵 +impl (gộp get-or-create + @Version + check công suất)
├── modules/booking/service/BookingService.java               🔵 + isRoomBookedBetween(roomId, from, to)
│   └── impl/BookingServiceImpl.java                          🔵 +impl query overlap
│                                   + createBooking (d.186–201) 🔵 chặn block khi chọn roomInstanceId
│                                   + assignRoom   (d.441–488)  🔵 chặn block (bên cạnh OCCUPIED d.461)
│                                   + checkIn      (d.532–572)  🔵 chặn block đang hiệu lực hôm nay
│   └── impl/PublicBookingSearchServiceImpl.java              ⚪ KHÔNG sửa — tự động nhờ ooo_rooms (§6)

backend/src/test/java/.../modules/inventory/service/
└── RoomMaintenanceBlockServiceTest.java                      🟢 (Mockito — mục 8)
    + modules/booking/service/BookingServiceTest.java         🔵 +3 test gán/check-in phòng bị block

frontend/src/modules/operation/
├── types/operation.types.ts                                  🔵 + MaintenanceBlock types + OOO/OOS/STATUS_OPTIONS
├── services/operation.service.ts                             🔵 + 7 hàm gọi API (§3)
├── components/maintenance-block/
│   ├── MaintenanceBlockFormModal.tsx                         🟢 (form tạo/sửa: phòng, loại, ngày, lý do, notes)
│   ├── MaintenanceBlockList.tsx                              🟢 (bảng 3 hàng: filter → toolbar → table+pagination)
│   └── MaintenanceBlockDetailDrawer.tsx                      🟢 (tuỳ chọn — chi tiết theo phòng)
├── pages/HousekeepingPage.tsx                                🔵 + nút "Khóa phòng (OOO/OOS)" thật + tab danh sách block
│                                                              + thay console.error → Modal lỗi (error.ts)
└── App.tsx                                                   🔵 d.63–65 + ENGINEERING vào route housekeeping
```

*(Không đụng: `PublicBookingSearchServiceImpl` ⚪, `payment-*` docs ⚪, mọi file WIP finance ⚪)*

---

## 5. CÁC BƯỚC TRIỂN KHAI (Cascade — mỗi bước 1 commit trên `feature/T09-maintenance-ticket`)

> ⛔ Điều kiện: được lệnh **"bảo build mới làm"** + **duyêt migration** (§9) trước Buổi 1.
> ⚠️ `mvn test` hiện FAIL do WIP test VNPay của anh (`VnPayCallbackTest` lỗi constructor) — khi code 2B cần WIP đó compile trước, nếu không sẽ không đọc được kết quả test.

### 📅 Buổi 1 (0.5d) — 📘 Đọc + 🔴 Migration + ErrorCode

| # | Bước | Chi tiết |
| :-- | :--- | :--- |
| 1.1 | 📘 Đọc lại flow | `RoomAvailabilityServiceImpl.getAvailability/holdInventory` (sparse), `HousekeepingServiceImpl.updateRoomStatus` (mirror slot), `BookingServiceImpl.assignRoom/checkIn` — 30' |
| 1.2 | 🔴 Tạo 2 file Liquibase | theo template `20261011-210000-add-full-name...xml` (id/author/sqlFile — xem §9) — **CHỜ ANH DUYỆT rồi mới chạy** |
| 1.3 | 🔵 ErrorCode | đọc `ErrorCode.java` xác nhận dãy trống → thêm 3 mã (§7) |
| 1.4 | 🔵 Bật handler | `GlobalHandlerException.java` bỏ comment nhánh `ROOM_ALREADY_BLOCKED` (d.104,122) → 409 |
| 1.5 | ✅ Test tay DB | `docker compose run --rm liquibase status` → thấy changeset pending → `update` → `psql` xác nhận index/cột |

### 📅 Buổi 2 (0.75d) — 🔵 Backend core (Cascade 1→6)

| # | Bước | File | Rules |
| :-- | :--- | :--- | :--- |
| 2.1 | Repository | `RoomMaintenanceBlockRepository` 🔵: `findActiveOverlap(roomId, start, end)` (JPQL `status='ACTIVE' AND startDate <= :end AND endDate >= :start`), `findByStatus...paged` (配合 filter), `findByRoomInstanceIdAndStatus` | JPQL preferred |
| 2.2 | Lock | `RoomInstanceRepository.findByIdWithLock` 🔵 | `@Lock(PESSIMISTIC_WRITE)` |
| 2.3 | DTO/SearchDto | 3 file request + 1 response 🟢 | `@Valid`, `@Schema` |
| 2.4 | Mapper | `RoomMaintenanceBlockMapper` 🟢 | componentModel spring |
| 2.5 | Cross-module BE | `RoomAvailabilityService.applyOooDelta(...)` 🔵 — get-or-create sparse từng đêm, `ooo_rooms += delta`, bắt `@Version` conflict → 409; chặn `available-1 < 0` khi `delta=+1` |同一 module inventory |
| 2.6 | Cross-module BE | `BookingService.isRoomBookedBetween(roomId, from, to)` 🔵 (dùng lại JPQL shape của `findOverlappingAssignedRooms`) | Service interface, không gọi repo booking từ inventory |
| 2.7 | **Service core** | `RoomMaintenanceBlockService` 🟢 (IF) + `Impl` 🟢: `create` (§2.3 7 bước + 3 tầng đồng bộ §2.5, `@Transactional`), `update` (re-validate), `complete/cancel` (bảng §2.4), `filter` (POST SearchDto), `getByRoom` | Lock thứ tự §2.6; `resolveCurrentStaffId` pattern |
| 2.8 | 🔵 Booking chặn block | `BookingServiceImpl`: `createBooking` d.186–201 (+check block), `assignRoom` d.461 (+check block, lock roomInstance), `checkIn` d.545 (+check block hôm nay) | Không đổi hành vi sẵn có, chỉ thêm guard |

### 📅 Buổi 3 (0.5d) — 🔵 Controller + Tests BE

| # | Bước | File |
| :-- | :--- | :--- |
| 3.1 | Controller 7 endpoint (§3) | `RoomMaintenanceBlockController` 🟢 — `@PreAuthorize` §2.7, `@Valid`, trả `ApiResponse`/`PageResponse`, Swagger `@Tag` |
| 3.2 | Unit test 3 kiểu (§8) | `RoomMaintenanceBlockServiceTest` 🟢 + bổ sung `BookingServiceTest` 🔵 |
| 3.3 | ✅ `mvn test` | so với baseline (151/0/1 của 2A) — không regression |

### 📅 Buổi 4 (0.75d) — 🔵 Frontend (Cascade FE)

| # | Bước | File | Chi tiết |
| :-- | :--- | :--- | :--- |
| 4.1 | types | `operation.types.ts` 🔵 | `MaintenanceBlockDto`, `CreateMaintenanceBlockRequest`, `BLOCK_TYPE_OPTIONS`, `BLOCK_STATUS_OPTIONS` |
| 4.2 | service | `operation.service.ts` 🔵 | 7 hàm theo §3 (kế thừa `core/api/client.ts`) |
| 4.3 | Form modal | `MaintenanceBlockFormModal.tsx` 🟢 | phòng (dropdown từ danh sách HK), radio OOO/OOS (giải thích 2 loại), 2 ô `type="date"`, lý do `required`, notes; react-hook-form + zod (`end >= start`), trim trước submit, disable nút khi loading |
| 4.4 | Danh sách | `MaintenanceBlockList.tsx` 🟢 | layout **3 hàng**: (1) filter (loại/trạng thái/phòng/khoảng ngày) → (2) toolbar "Khóa phòng" → (3) table + `Pagination`; cột: phòng, loại (badge), ngày, lý do, trạng thái, người tạo |
| 4.5 | Tích hợp | `HousekeepingPage.tsx` 🔵 | nút **"Khóa phòng (OOO/OOS)"** mở form (thay cho nút "Bảo trì" setStatus d.311–318 — giữ nút cũ nếu cần chuyển trạng thái nhanh); tab/section danh sách block; **cảnh báo** nếu phòng có khách (`currentStatus==='OCCUPIED'` → confirm.warning + BE vẫn là phán quyết cuối); lỗi → `getApiErrorMessage` + **Modal** (mẫu `PaymentErrorModal`), hủy/kết thúc → **`ConfirmModal`**; `useEffect` có cleanup |
| 4.6 | Route/roles | `App.tsx` 🔵 d.63–65 thêm `ENGINEERING` |
| 4.7 | ✅ Build FE | `npm run build` (tsc -b + vite) + `npm run lint` |

### 📅 Cuối (0.25d) — ✅ E2E tay (§10) + báo cáo

---

## 6. TÍCH HỢP VỚI LUỒNG ĐẶT PHÒNG (nơi quan trọng nhất)

| Luồng | File:line hiện tại | Việc 2B |
| :--- | :--- | :--- |
| **Tìm phòng trống** | `PublicBookingSearchServiceImpl.checkAvailableRooms` d.348–370 → `getAvailability` → `min(availableCount)` | ⚪ **Không sửa** — OOO tự động loại nhờ `ooo_rooms++` (DB trừ vào `available_count`). ✅ Test bằng mắt: tạo OOO → `POST /api/v1/public/availability/search` giảm đúng 1 phòng/đêm |
| **Hold (online)** | `holdInventory` d.111–119 / `batchHoldInventory` d.198–206 | ⚪ Không sửa — `available = total-booked-locked-ooo` đã xét block |
| **Tạo booking (walk-in chọn phòng)** | `BookingServiceImpl.createBooking` d.186–201 (đang check `findOverlappingAssignedRooms`) | 🔵 thêm guard `isRoomBookedBetween` **+ block overlap** → `ROOM_ALREADY_BLOCKED` |
| **Gán phòng** | `assignRoom` d.441–488 (chỉ chặn `OCCUPIED` d.461) | 🔵 chặn block ACTIVE giao khoảng booking; lock `room_instances` row |
| **Check-in** | `checkIn` d.532–572 (chặn `OCCUPIED` d.545) | 🔵 chặn phòng có block ACTIVE bao hôm nay → 409, message tiếng Việt rõ ràng |
| **Check-out** | d.574–606 | ⚪ Không đổi (không đụng lịch sử booking) |
| **Race 2 người cùng lúc** | EXCLUDE constraint + pessimistic lock `room_instances` (§2.6) + `@Version` `room_availability` | 409 thay vì overbooking |
| **Timeline (Member 4 – T10)** | đọc `room_availability.ooo_rooms` | OOO ghi đúng cột sẵn có → tự hiển thị; **không cần code chung** |

> **Bất biến:** block **không bao giờ** sửa/đổi trạng thái booking đã có — mọi xung đột → **từ chối tạo block** (§2.3.4–5) hoặc **từ chối gán/check-in** (§6) bằng lỗi nghiệp vụ rõ ràng.

---

## 7. ERRORCODE DỰ KIẾN (⚠️ bắt buộc tra `ErrorCode.java` trước khi thêm — Zero Hallucination)

| Mã (đề xuất, cần xác minh ô trống) | Tên | HTTP | Dùng khi |
| :--- | :--- | :--- | :--- |
| 5041? | `MAINTENANCE_BLOCK_NOT_FOUND` | 404 | id không tồn tại / đã xóa |
| 5042? | `ROOM_ALREADY_BLOCKED` | 409 | chồng block • phòng OCCUPIED • có booking gán • (check-in/assign phòng bị block) — handler **đã có sẵn đang comment** |
| 5043? | `MAINTENANCE_BLOCK_TIME_INVALID` | 400 | `startDate > endDate`, block không ACTIVE khi update/complete/cancel |
| Tái dùng | `ROOM_NOT_AVAILABLE(5031)` | 409 | OOO làm `available_count < 0` |
| Tái dùng | `ROOM_INSTANCE_NOT_FOUND(5021)` | 404 | phòng không tồn tại |

*(Message tiếng Việt cụ thể cho từng trường, vd: "Phòng 302 đang có khách lưu trú, không thể tạo block".)*

---

## 8. UNIT TEST 3 KIỂU → MAP 12 CASE CỦA TASK

| # | Case (task) | Test method (gợi ý) | Kỳ vọng |
| :-- | :--- | :--- | :--- |
| 1 | Tạo OOO thành công | `create_Ooo_Success` | save với `status=ACTIVE`, `createdByStaffId` ≠ null; mock `applyOooDelta(+1)` từng đêm; slot → `BLOCKED`; room → `MAINTENANCE` |
| 2 | Tạo OOS thành công | `create_Oos_Success` | save OK; **`applyOooDelta` KHÔNG được gọi**; room → `MAINTENANCE` |
| 3 | Khoảng thời gian sai | `create_StartAfterEnd_Throws` | `MAINTENANCE_BLOCK_TIME_INVALID` 400, `save` không gọi |
| 4 | Trùng/chồng block | `create_OverlapActiveBlock_Throws` | `ROOM_ALREADY_BLOCKED` 409 (query overlap trả về 1 bản ghi) |
| 5 | Trùng booking/khách | `create_RoomOccupied_Throws` + `create_BookedOverlap_Throws` | 409; `applyOooDelta` không gọi (không side-effect khi reject) |
| 6 | Phòng block không hiện khi tìm | *Unit:* `create_Ooo_DecrementsAvailability` (assert đúng đêm/mọi đêm) · *E2E tay:* tạo OOO → availability search giảm | min(availableCount) giảm đúng 1 |
| 7 | BE từ chối booking/gán phòng bị block | `BookingServiceTest`: `assignRoom_Blocked_Throws`, `checkIn_Blocked_Throws`, `createWalkIn_Blocked_Throws` 🔵 | `ROOM_ALREADY_BLOCKED` 409 |
| 8 | Dùng lại sau khi block kết thúc | `complete_ReleasesEverything` | `ooo_rooms` trừ về đúng, slot `READY` + clear blockId, `assignRoom` OK (mock không còn overlap) |
| 9 | Không tự READY sai | `complete_RoomStillDirty_StaysCleaning` | `current_status = 'CLEANING'` (không READY) khi slot hôm nay DIRTY; case còn block khác → giữ `MAINTENANCE` |
| 10 | Người không đủ quyền | *Unit:* không test annotation bằng mock · **Test tay API** (§10) bằng token `RECEPTIONIST` gọi `POST /create` → 403; token `HOUSEKEEPING` → 201 | `@PreAuthorize` chặn thật |
| 11 | Transaction/đồng thời | Mockito không đủ hạ tầng → **stress tay** theo skill `hotel-pricing-concurrency-verify`: 2 thread song song `POST /create` cùng phòng/kỳ → đúng 1 thread 201, còn lại 409 EXCLUDE | Ghi rõ "chưa chạy do môi trường" nếu không làm được |
| 12 | API + FE test | FE **không có test framework** (package.json không có script test) → **e2e tay theo §10**; Swagger test 7 endpoint | Ghi nhận giới hạn môi trường |

*(Đủ 3 kiểu: happy 1–2,6,8 · validation 3–5,7,9,10 · edge 9,11 + list rỗng filter, keyword `%_` theo rule 02.)*

---

## 9. LIQUIBASE (chỉ phần cần thêm — bảng block ĐÃ CÓ)

**Không cần tạo bảng block** (đã có + đã có EXCLUDE). Chỉ cần 1 changeset mới (**⚠️ ĐỤNG DB → CHỜ ANH DUYỆT**, như quy tắc "không DDL tay" + lịch duyệt lần trước):

`db/changelogs/incremental/2026/YYYYMMDD-HHmmss-maintenance-block-index-audit.xml`
```xml
<changeSet id="YYYYMMDD-HHmmss-maintenance-block-index-audit" author="utc-hotel-team">
    <comment>Index + cột audit + CHECK ooo_rooms cho tính năng block OOO/OOS</comment>
    <sqlFile path="db/sqlFile/incremental/2026/YYYYMMDD-HHmmss-maintenance-block-index-audit.sql"
             relativeToChangelogFile="false" splitStatements="false" stripComments="false"/>
</changeSet>
```
`db/sqlFile/incremental/2026/YYYYMMDD-HHmmss-maintenance-block-index-audit.sql` (nội dung dự kiến — idempotent):
```sql
CREATE INDEX IF NOT EXISTS idx_room_maintenance_block_room_status
    ON room_maintenance_blocks (room_instance_id, status);
CREATE INDEX IF NOT EXISTS idx_room_maintenance_block_range
    ON room_maintenance_blocks (start_date, end_date);
-- partial index cho truy vấn block hiệu lực (overlap check)
CREATE INDEX IF NOT EXISTS idx_room_maintenance_block_active
    ON room_maintenance_blocks (room_instance_id, start_date, end_date)
    WHERE status = 'ACTIVE';
ALTER TABLE room_maintenance_blocks
    ADD COLUMN IF NOT EXISTS updated_by_staff_id INT REFERENCES staffs(id);
-- CHECK ooo_rooms không âm (đặt đúng bảng room_availability):
ALTER TABLE room_availability
    DROP CONSTRAINT IF EXISTS chk_ooo_rooms_non_negative;
ALTER TABLE room_availability
    ADD CONSTRAINT chk_ooo_rooms_non_negative CHECK (ooo_rooms >= 0);
```
> ⚠️ Trong lúc implement phải **tra lại tên/cột** cho khớp repo. Lưu ý `chk_inventory_consistency` hiện **chưa gồm `ooo_rooms`** — cân nhắc mở rộng cùng changeset (cần duyệt riêng vì sửa constraint đang chạy).
> Chạy: `docker compose run --rm liquibase status|history|update` rồi restart backend để Liquibase validate.
> **KHÔNG** seed dữ liệu block; **KHÔNG** đụng `01-init-schema.sql`/migration đã chạy (checksum).

---

## 10. KIỂM THỬ TAY (E2E — điều kiện: backend 8080 + FE 3000 + Keycloak đang chạy)

1. **Tạo OOO:** đăng nhập `housekeeping_hn/123456` → Housekeeping → "Khóa phòng (OOO/OOS)" → phòng X, OOO, [hôm nay..+2], lý do "Sửa điều hòa" → 201.
2. **Xác minh quỹ phòng:** `POST /api/v1/public/availability/search` (public) trước/sau → `availableCount` của loại phòng X giảm đúng 1/đêm.
3. **Xác minh DB:** `SELECT ooo_rooms FROM room_availability ...`; `SELECT status, maintenance_block_id FROM room_slots WHERE room_instance_id=X`; `room_instances.current_status='MAINTENANCE'`.
4. **Chồng block:** tạo lần 2 cùng kỳ → 409 `ROOM_ALREADY_BLOCKED` (popup Modal trên FE).
5. **Thời gian sai:** `endDate < startDate` → 400.
6. **Phòng có khách:** phòng `OCCUPIED` → 409.
7. **Gán/check-in phòng block:** lễ tân `reception_hn` gán phòng X vào kỳ block → 409; `POST /bookings/create` walk-in chọn `roomInstanceId=X` → 409.
8. **Quyền:** token lễ tân gọi `POST /maintenance-blocks/create` → **403**.
9. **Kết thúc:** `PUT /{id}/complete` → `ooo_rooms` về cũ, slot `READY`, room → `READY`/`CLEANING` đúng bảng §2.4 (test case có slot DIRTY để thấy KHÔNG về READY).
10. **Dùng lại:** gán phòng X sau complete → thành công.
11. **`mvn test`** + **`npm run build`** xanh.

---

## 11. DoD (khớp DoD T09 + task 2B)

- [ ] Block OOO → `available_count` giảm → **booking engine không nhận đặt phòng đó** (test thật mục 10.2)
- [ ] Gán/check-in/walk-in vào phòng bị block trong kỳ → **409, không silent-change booking nào**
- [ ] Chồng block → 409 (BE + DB EXCLUDE); ngày sai → 400
- [ ] Complete/Cancel: `ooo_rooms`/slot/`current_status` đồng bộ theo bảng §2.4 — **không tự READY khi còn bẩn/khách/block khác**
- [ ] 7 endpoint đều có `@PreAuthorize`; token sai quyền → 403
- [ ] Unit test 3 kiểu pass · `mvn test` không regression · `npm run build` xanh
- [ ] Liquibase changeset được **duyệt** rồi mới chạy; Swagger mô tả đúng
- [ ] Không TODO/placeholder · không seed · không sửa migration đã chạy

---

## 12. RỦI RO / VẤN ĐỀ PHÁT HIỆN (ngoài scope 2B nhưng phải biết)

1. ⚠️ **`DIRTY` không nằm trong CHECK `chk_room_instance_status`** (`01-init-schema.sql:398–407`) nhưng `HousekeepingServiceImpl` đang ghi `DIRTY` → có thể fail CHECK ở DB thật. **2B tránh ghi `DIRTY` vào `room_instances`** (chỉ ghi `CLEANING`); sửa CHECK là việc riêng, cần migration + duyệt.
2. ⚠️ Handler optimistic/pessimistic + `ROOM_ALREADY_BLOCKED` trong `GlobalHandlerException` (d.99–126) **đang bị comment** → conflict `@Version` hôm nay rơi về 400/9999. 2B bật lại nhánh `ROOM_ALREADY_BLOCKED`; nhánh `@Version` (4091) cân nhắc bật cùng (🔵 sửa chung 1 file).
3. ⚠️ `RoomInstanceController PATCH /{id}/status` không validate giá trị → ai cũng set `MAINTENANCE`/`READY` được mà không qua block. Không sửa trong 2B (out of scope) nhưng ghi nhận.
4. ⚠️ `chk_inventory_consistency` **chưa gồm `ooo_rooms`** → thêm CHECK `ooo_rooms >= 0` ở changeset §9 (chờ duyệt).
5. ⚠️ `NightAuditServiceImpl` đọc `room_slots` (d.92) — cần **kiểm tra slot `BLOCKED` không bị tính nhầm** khi chạy Night Audit (bước đọc lúc implement, nếu cần sửa → báo riêng).
6. ⚠️ FE: `core/types/auth.types.ts` union roles cũ (thiếu ENGINEERING/F_AND_B/FINANCE...), `HousekeepingPage` lỗi chỉ `console.error` — 2B sửa phần mình (4.5, 4.6), không refactor cả file.
7. ⚠️ **`mvn test` đang đỏ vì WIP test VNPay** (`VnPayCallbackTest:64` constructor `PaymentServiceImpl` lệch — do WIP thêm `InvoiceRepository`) → anh sửa WIP trước khi mình đọc kết quả test 2B.
8. ℹ️ `maintenance_tickets` (bảng phiếu) — plan 2B tên gồm "Maintenance Ticket" nhưng task lần này chỉ yêu cầu **block** → để riêng (cột `maintenance_ticket_id` giữ nguyên nullable, không FK).
9. ℹ️ FE **không có test framework** (không script `test`) → phần 12 của task chạy bằng e2e tay, ghi rõ trong báo cáo.

---

## ⚠️ LƯU Ý QUY TẮC HIỆN TẠI

1. **Tài liệu này là docs — chưa code.** Mọi file trong §4 chỉ là dự kiến; code khi được lệnh ("bảo build mới làm").
2. Migration §9 **ĐỤNG DB → chờ duyệt** trước khi chạy (như lần checksum Liquibase trước).
3. **Bảo toàn WIP:** các file finance (`PaymentController`, `PaymentCallbackController`, `VnPayGateway`, `InvoiceServiceImpl`, `PaymentServiceImpl`, `PaymentServiceTest`, `VnPayCallbackTest`) + L2 fix (11 file keyword `""`) đang uncommit — **không đụng, không commit** khi chưa được lệnh.
4. **Không sửa** `docs/payment-methods-5-phuong-thuc.md`; tài liệu payment chỉ ghi vào `docs/payment-luong-5-phuong-thuc.md`.
5. Làm trên nhánh riêng `feature/T09-maintenance-ticket`; `develop` = chỉ đọc; không tự merge.
6. Đồng bộ Member 4 (Timeline T10): block ghi vào `room_availability.ooo_rooms` + `room_slots.status='BLOCKED'` — 2 nguồn dữ liệu T10 đã đọc sẵn.
