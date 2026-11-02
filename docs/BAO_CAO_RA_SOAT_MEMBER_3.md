# BÁO CÁO RÀ SOÁT SOURCE CODE & KẾ HOẠCH TRIỂN KHAI – MEMBER 3
**Dự án:** Hệ thống Quản lý và Đặt phòng Khách sạn Trực tuyến Thông minh  
**Vai trò:** Member 3 – Senior Fullstack Developer + Software Analyst  
**Ngày lập:** 02/11/2026  
**Trạng thái tuân thủ:** READ-ONLY AUDIT (Không sửa source code, không tạo migration, chờ phê duyệt trước khi implement)

---

## 1. TỔNG QUAN HIỆN TRẠNG TOÀN BỘ CHỨC NĂNG MEMBER 3

### 1.1 Bảng tổng hợp trạng thái 8 chức năng cốt lõi (Sprint 1 & Sprint 2)

| STT | Mã Nhiệm Vụ | Tên Chức Năng | Trạng Thái | Backend | Frontend | Testing | Đánh Giá Tóm Tắt |
|:---:|:---|:---|:---:|:---:|:---:|:---:|:---|
| 1 | **T-SP1-07** | OCR CCCD/Passport tại Check-in | 🟡 ĐÃ CÓ MỘT PHẦN | 🟢 Đầy đủ Controller, Service, Adapter | 🟡 Đã có ở StayGuestsModal, thiếu ở Walk-in | 🟢 Unit Test có | API OCR Tesseract hoạt động tốt. Cần tích hợp nút "Tải ảnh CCCD" & Auto-fill vào `WalkInBookingModal.tsx`. |
| 2 | **T-SP1-08** | 6 Trang Quản trị IAM & Organization | 🟡 ĐÃ CÓ MỘT PHẦN | 🟢 Đầy đủ 6 Controllers CRUD & RBAC | 🔴 Chưa có Page, chưa có Route trong App.tsx | 🟢 Controller Unit Tests có | Backend hoàn tất 100%. Frontend mới chỉ có Sidebar Menu link nhưng click vào bị 404/fallback. Cần tạo 6 Pages chuẩn 3-row layout. |
| 3 | **T-SP1-10** | Email Notification Service | 🔴 CHƯA CÓ | 🔴 Chưa có starter-mail, chưa có config SMTP | ➖ Không áp dụng | 🔴 Chưa có | Chưa tích hợp JavaMailSender, chưa có SMTP config, chưa có HTML template và Event Async listener. Cần triển khai mới toàn bộ. |
| 4 | **T-SP1-15** | E2E Testing Luồng FIT | 🟡 ĐÃ CÓ MỘT PHẦN | 🟢 Đầy đủ APIs nghiệp vụ | ➖ Không áp dụng | 🟡 Chỉ có Unit Test độc lập, thiếu E2E Integration | Đã có `BookingServiceTest` (2524 dòng) và `BookingRoomSwapTest`, nhưng thiếu kịch bản `@SpringBootTest` chạy liền mạch Booking ➔ Check-in ➔ Surcharge ➔ Swap ➔ Checkout. |
| 5 | **T-SP2-05** | Excel Rooming List Import (U12) | 🟢 ĐÃ HOÀN THÀNH | 🟢 Đầy đủ Apache POI XSSFWorkbook, Preview, Confirm | 🟢 Đầy đủ Page & Modal UI | 🟢 13/13 BVA Tests PASS | Hoàn chỉnh 100%. Hỗ trợ validate từng dòng, tối đa 500 dòng, Preview trước khi Import, gán phòng tự động/thủ công. Sẵn sàng bàn giao. |
| 6 | **T-SP2-08** | Group Checkout & Master/Sub Folio | 🟡 ĐÃ CÓ MỘT PHẦN | 🟡 Có MasterFolio Entity & Service, thiếu Group Checkout API | 🔴 Chưa có UI Master Folio | 🟡 Có Test Folio cơ bản | Đã có Entity `MasterFolio`, `BillingRoutingRule` tách chi phí phòng và chi phí phát sinh. Thiếu API Group Checkout (trừ cọc, thanh toán gộp) và giao diện điều khiển. |
| 7 | **T-SP2-13** | SQL Optimization + Redis Cache | 🟢 ĐÃ HOÀN THÀNH | 🟢 Đã cấu hình Redis CacheManager (TTL 30p) & 11 DB Indexes | ➖ Không áp dụng | 🟢 4/4 Cache Tests PASS | Đã cache `@Cacheable("ratePlans")`, đã tạo 11 indexes tối ưu truy vấn đặt phòng và thanh toán. Lưu ý kiểm soát N+1 query trong mapper. |
| 8 | **T-SP2-15** | Stress Test 200 CCU + Security RBAC Audit | 🟡 ĐÃ CÓ MỘT PHẦN | 🟢 RBAC `@PreAuthorize` đầy đủ, Concurrency Lock có | ➖ Không áp dụng | 🟡 Thiếu k6/JMeter script ngoại vi | Đã có `RbacSecurityAuditTest` (13/13 PASS) và `BookingConcurrencyTest` (10 threads lock phòng cuối). Thiếu k6 script mô phỏng 200 CCU thực tế. |

**Phân bổ kết quả:**
- 🟢 **ĐÃ HOÀN THÀNH:** 2/8 chức năng (25%) – `T-SP2-05`, `T-SP2-13`
- 🟡 **ĐÃ CÓ MỘT PHẦN:** 5/8 chức năng (62.5%) – `T-SP1-07`, `T-SP1-08`, `T-SP1-15`, `T-SP2-08`, `T-SP2-15`
- 🔴 **CHƯA CÓ:** 1/8 chức năng (12.5%) – `T-SP1-10`

---

## 2. CHI TIẾT ĐÁNH GIÁ SPRINT 1

### 2.1 T-SP1-07 – OCR CCCD/Passport tại Check-in (🟡 ĐÃ CÓ MỘT PHẦN)

#### Bằng chứng thực tế trong Source Code:
- **Backend:**
  - File Controller: `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/controller/IdentityDocumentOcrController.java`
    - `POST /api/v1/ai/ocr/extract`: Tiếp nhận payload Base64.
    - `POST /api/v1/ai/ocr/extract-file`: Tiếp nhận file `MultipartFile` (`image/jpeg`, `image/png`, `application/pdf`, tối đa 10MB).
    - Phân quyền RBAC: `@PreAuthorize("hasAnyRole('CHAIN_ADMIN', 'PROPERTY_MANAGER', 'RECEPTIONIST')")`.
  - Service: `IdentityDocumentOcrServiceImpl.java` phối hợp với `TesseractOcrVisionAdapter.java`.
  - DTO: `OcrStayGuestDraftDto.java` trích xuất 7 trường thông tin:
    - `fullName`, `idCardNumber`, `birthDate`, `gender`, `nationality`, `address`, `issueDate`, `documentType`.
    - Trả về độ tin cậy từng trường (`fieldsConfidence`) và tổng quan (`overallConfidence`), cờ `verificationStatus` ("CONFIRMED" hoặc "UNCONFIRMED").
- **Frontend:**
  - `frontend/src/modules/booking/components/StayGuestsModal.tsx`:
    - Đã có input tải file ảnh CCCD/Passport.
    - Gọi API qua `identityOcr.service.ts` (`extractIdentityImage`).
    - Có form xem lại kết quả OCR, checklist xác nhận "Tôi đã đối chiếu giấy tờ gốc", và nút "Tự động điền".
  - `frontend/src/modules/booking/pages/WalkInBookingPage.tsx` & `WalkInBookingModal.tsx`:
    - **CHƯA CÓ OCR:** Tại Bước 2 (Thông tin khách hàng & phòng), Lễ tân vẫn đang phải gõ tay toàn bộ Họ tên, Số CCCD, Ngày sinh, Địa chỉ, Email, Số điện thoại.

#### Kế hoạch triển khai bổ sung:
1. **Backend:** Giữ nguyên (API và Adapter đã hoàn thiện và hoạt động chuẩn).
2. **Frontend:**
   - Cập nhật `WalkInBookingModal.tsx` (hoặc `WalkInBookingPage.tsx`):
   - Thêm nút component: `[ 📷 Tải ảnh CCCD / Passport ]` cạnh tiêu đề "Thông tin khách chính".
   - Tích hợp modal upload/preview ảnh tương tự `StayGuestsModal`.
   - Kết nối `identityOcrService.extractIdentityImage(file)`.
   - Khi API trả về, tự động điền (Auto-fill) vào các trường: `fullName`, `identityNumber`, `dateOfBirth`, `address`, `gender`.

---

### 2.2 T-SP1-08 – 6 Trang Quản trị IAM & Organization (🟡 ĐÃ CÓ MỘT PHẦN)

#### Bằng chứng thực tế trong Source Code:
- **Backend:** Hoàn tất 100% cả 6 bộ Controller, Service, Repository, DTO và phân quyền:
  1. **Department:** `DepartmentController.java` (`/api/v1/departments`) – CRUD, phân trang `POST /filter`, RBAC `ROLE_DEPARTMENT_VIEW/CREATE/UPDATE/DELETE`.
  2. **Staff:** `StaffController.java` (`/api/v1/staffs`) – CRUD nhân sự, gán phòng ban, chức vụ, trạng thái làm việc.
  3. **Role:** `RoleController.java` (`/api/v1/roles`) – Danh sách vai trò Keycloak, danh sách quyền hạn granular.
  4. **Guest:** `GuestController.java` (`/api/v1/guests`) & `BookingGuestController.java` (`/api/v1/booking-guests`) – Quản lý hồ sơ khách hàng, phân hạng thành viên, lịch sử đặt phòng.
  5. **Company:** `CompanyController.java` (`/api/v1/companies`) – Quản lý khách hàng doanh nghiệp B2B, mã số thuế, công nợ.
  6. **AuditLog:** `AuditLogController.java` (`/api/v1/audit-logs`) – Nhật ký thao tác hệ thống, IP, user, module, thay đổi dữ liệu.
- **Frontend:**
  - `frontend/src/core/components/Sidebar.tsx`: Các menu item đã khai báo sẵn:
    - `/organization/departments`
    - `/identity/staffs`
    - `/identity/roles`
    - `/identity/guests`
    - `/identity/companies`
    - `/identity/audit-logs`
  - `frontend/src/App.tsx`: **KHÔNG CÓ ROUTE NÀO ĐƯỢC ĐĂNG KÝ.**
  - `frontend/src/modules/identity/` & `frontend/src/modules/organization/`: **CHƯA CÓ CÁC FILE PAGE HOẶC MODAL COMPONENT NÀO.**
  - **Hậu quả:** Khi người dùng click vào menu trên thanh Sidebar, hệ thống rơi vào fallback điều hướng hoặc báo 404 Not Found.

#### Kế hoạch triển khai bổ sung:
- **Frontend:**
  1. Tạo thư mục và components theo chuẩn Module Pattern và Layout 3 hàng (Quy chuẩn 03 & 05):
     - `src/modules/organization/pages/DepartmentListPage.tsx` & `DepartmentModalForm.tsx`
     - `src/modules/identity/pages/StaffListPage.tsx` & `StaffModalForm.tsx`
     - `src/modules/identity/pages/RoleListPage.tsx` & `RolePermissionModal.tsx`
     - `src/modules/identity/pages/GuestListPage.tsx` & `GuestDetailDrawer.tsx`
     - `src/modules/organization/pages/CompanyListPage.tsx` & `CompanyModalForm.tsx`
     - `src/modules/identity/pages/AuditLogListPage.tsx` & `AuditLogFilterBar.tsx`
  2. Tạo các HTTP services tương ứng trong `src/modules/identity/services/` và `src/modules/organization/services/`.
  3. Khai báo 6 Routes trong `frontend/src/App.tsx` dưới `DashboardLayout` và bọc `ProtectedRoute`.

---

### 2.3 T-SP1-10 – Email Notification Service (🔴 CHƯA CÓ)

#### Bằng chứng thực tế trong Source Code:
- `backend/pom.xml`: Hoàn toàn **chưa có** dependency `org.springframework.boot:spring-boot-starter-mail`.
- `backend/src/main/resources/application.yml`: Không có bất kỳ khối cấu hình `spring.mail.*` nào (host, port, username, password, properties).
- Quét toàn bộ project backend: Không có `JavaMailSender`, không có class nào tên `EmailService`, `MailService` hay HTML email template (`resources/templates/mail/`).
- Các sự kiện đặt phòng (`BookingCreatedEvent`), thanh toán (`PaymentSuccessEvent`) hiện tại chưa được bắn (publish) để kích hoạt gửi email.

#### Kiến trúc Email Service đề xuất triển khai:
1. **Dependency:** Bổ sung `spring-boot-starter-mail` và `spring-boot-starter-thymeleaf` vào `backend/pom.xml`.
2. **Cấu hình SMTP:**
   ```yaml
   spring:
     mail:
       host: ${SMTP_HOST:smtp.gmail.com}
       port: ${SMTP_PORT:587}
       username: ${SMTP_USERNAME}
       password: ${SMTP_PASSWORD}
       properties:
         mail:
           smtp:
             auth: true
             starttls:
               enable: true
   ```
3. **Mô hình Bất đồng bộ (Async & Event-Driven):**
   - Không được gửi mail đồng bộ trong Request Thread (gây block HTTP response từ 2 - 5 giây).
   - Sử dụng Spring Application Event (`ApplicationEventPublisher`):
     - `BookingSuccessEvent` ➔ `EmailNotificationListener` (`@Async` thread pool riêng).
     - `PaymentReceiptEvent` ➔ `EmailNotificationListener`.
     - `BookingCancelledEvent` ➔ `EmailNotificationListener`.
4. **HTML Email Templates (Thymeleaf):**
   - `templates/mail/booking-confirmation.html`: Logo khách sạn, Mã tra cứu (Booking Code), Mã QR check-in, Thời gian nhận/trả phòng, Chi tiết giá tiền.
   - `templates/mail/payment-receipt.html`: Biên lai thanh toán thành công (VNPAY/Cash), Mã giao dịch ngân hàng.
   - `templates/mail/booking-cancellation.html`: Xác nhận hủy phòng, số tiền phạt hủy (nếu có), hạn hoàn tiền.

---

### 2.4 T-SP1-15 – E2E Testing Luồng FIT (🟡 ĐÃ CÓ MỘT PHẦN)

#### Bằng chứng thực tế trong Source Code:
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/booking/service/BookingServiceTest.java`:
  - 2,524 dòng code với các bài Unit Test dùng `@ExtendWith(MockitoExtension.class)`.
  - Đã test riêng lẻ: `createBooking`, `assignRoom`, `addCharge`, `previewCancel`, `cancelBooking`, `markBookingAsNoShow`, `checkoutSettlement`.
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/booking/service/BookingRoomSwapTest.java`:
  - Test riêng logic đổi phòng (Room Swap), tính phụ phí chênh lệch hạng phòng.
- **Thiếu sót:**
  - Chưa có bài kiểm thử Tích hợp Đầu-Cuối (End-to-End Integration Test) chạy trên môi trường Spring Context thật (`@SpringBootTest` kết hợp cơ sở dữ liệu test).
  - Luồng khách lẻ (FIT Journey) chưa được xâu chuỗi liên tục từ bước đầu tới bước cuối trong cùng một vòng đời đặt phòng.

#### Đề xuất kịch bản E2E Test (`FitGuestJourneyE2ETest.java`):
- **Bước 1 (Create Booking):** Khách đặt 1 phòng Deluxe 3 đêm qua API, tính toán giá cọc.
- **Bước 2 (Check-in & Assign Room):** Lễ tân check-in, gán phòng 302, trạng thái đổi thành `CHECKED_IN`, phòng chuyển sang `OCCUPIED`.
- **Bước 3 (In-stay Charges):** Khách sử dụng dịch vụ ăn uống Minibar, gọi API `POST /charges` ghi nhận 250,000 VND vào Folio phòng 302.
- **Bước 4 (Room Swap):** Khách yêu cầu chuyển sang phòng 405 (Suite) do điều hòa hỏng. Hệ thống tính phí nâng hạng, giải phóng phòng 302 về trạng thái dọn dẹp (`DIRTY`), chuyển khách sang 405.
- **Bước 5 (Check-out Settlement & Payment):** Lễ tân thực hiện thanh toán toán bộ số dư còn lại (Tiền phòng + Minibar + Chênh lệch đổi phòng - Tiền cọc). Cập nhật trạng thái `CHECKED_OUT`, xuất hóa đơn điện tử, chuyển phòng 405 về `DIRTY`.

---

## 3. CHI TIẾT ĐÁNH GIÁ SPRINT 2 (TRỌNG TÂM ƯU TIÊN)

### 3.1 T-SP2-05 – Excel Rooming List Import (U12) (🟢 ĐÃ HOÀN THÀNH)

#### Bằng chứng thực tế trong Source Code:
- **Thư viện Apache POI:** Đã tích hợp `org.apache.poi:poi-ooxml:5.3.0` trong `pom.xml`.
- **Backend Service:**
  - `backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/service/impl/ExcelParserServiceImpl.java` (Sử dụng `XSSFWorkbook` đọc định dạng `.xlsx`).
  - Đọc đúng Sheet quy ước: `"RoomingList"`.
  - Hỗ trợ linh hoạt 5 định dạng ngày tháng: `yyyy-MM-dd`, `dd/MM/yyyy`, `MM/dd/yyyy`, `d/M/yyyy`, `yyyy/MM/dd`.
  - Cơ chế **Validate từng dòng độc lập (Row-by-Row Validation)**: Một dòng sai không làm hỏng toàn bộ quá trình parse. Hệ thống gom tất cả lỗi vào `List<RoomingListErrorDto>` (chỉ rõ số dòng, tên cột, thông báo lỗi cụ thể).
  - Bắt lỗi trùng lặp số CCCD/Passport giữa các dòng trong cùng một file Excel.
  - Giới hạn bảo vệ hệ thống: Tối đa 500 dòng dữ liệu, file upload không vượt quá 10MB.
- **API Endpoints:**
  - `POST /api/v1/bookings/{id}/rooming-list/preview`: Upload file, đọc dữ liệu, kiểm tra tính hợp lệ và trả về bảng Preview (Dòng hợp lệ vs Dòng lỗi).
  - `POST /api/v1/bookings/{id}/rooming-list/confirm`: Xác nhận import danh sách hợp lệ, tự động gán hoặc giữ phòng trống, sinh bản ghi `StayGuest`.
  - `GET /api/v1/bookings/vacant-rooms`: Lấy danh sách phòng còn trống theo khoảng thời gian để phân bổ.
- **Frontend Interface:**
  - `frontend/src/modules/booking/pages/RoomingListImportPage.tsx` (Route: `/booking/rooming-list`).
  - `frontend/src/modules/booking/components/RoomingListImportModal.tsx` tích hợp sẵn trong trang danh sách đặt phòng.
  - Giao diện kéo thả file Excel, hiển thị bảng Preview trực quan với màu sắc cảnh báo: Xanh (Hợp lệ) và Đỏ (Lỗi chi tiết).
- **Kiểm thử BVA (Boundary Value Analysis):**
  - File test: `backend/src/test/java/vn/edu/utc/hotel_booking/modules/booking/service/RoomingListImportBvaTest.java`.
  - Đã chạy kiểm thử thành công 13/13 ca test biên: File rỗng, file vượt quá 500 dòng, sai tên sheet, ngày sinh trong tương lai, trùng số định danh... Kết quả: **PASS 100%**.
  - Dữ liệu mẫu kiểm thử đã có sẵn trong: `docs/test-data/U12/` và `frontend/public/test-data/`.

---

### 3.2 T-SP2-08 – Group Checkout & Master/Sub Folio (🟡 ĐÃ CÓ MỘT PHẦN)

#### Bằng chứng thực tế trong Source Code:
- **Backend Entities & Architecture:**
  - `backend/src/main/java/vn/edu/utc/hotel_booking/modules/billing/entity/MasterFolio.java`:
    - Quản lý tài khoản gộp cho đoàn khách, liên kết `GroupBooking` / `Company`.
  - `backend/src/main/java/vn/edu/utc/hotel_booking/modules/billing/entity/MasterFolioItemAdjustment.java`:
    - Ghi nhận các bút toán điều chỉnh, bù trừ tiền cọc, chuyển giao chi phí.
  - `MasterFolioServiceImpl.java` & `BillingRoutingRule.java`:
    - Hỗ trợ quy tắc định tuyến chi phí: Chi phí tiền phòng (Room Charges) chuyển về Master Folio (Công ty/Trưởng đoàn trả), chi phí cá nhân (Minibar, Giặt là, Ăn uống) giữ lại ở Sub-Folio từng phòng (Khách tự trả khi check-out).
- **Phần còn thiếu sót:**
  - **Backend:** Chưa có Endpoint xử lý Check-out gộp cho cả đoàn (`POST /api/v1/master-folios/{id}/checkout` hoặc `POST /api/v1/group-bookings/{id}/group-checkout`).
    - Thiếu logic tự động cấn trừ số tiền cọc (Deposit Netting) của hợp đồng đoàn vào tổng hóa đơn cuối cùng của Master Folio.
    - Thiếu cơ chế đồng loạt chuyển trạng thái tất cả các phòng thuộc đoàn sang `CHECKED_OUT` và `DIRTY`.
  - **Frontend:** Chưa có màn hình quản lý Master Folio và giao diện Lễ tân thực hiện "Group Check-out & Settle".

#### Kế hoạch triển khai bổ sung:
1. **Backend:**
   - Xây dựng phương thức `checkoutGroup(Long masterFolioId, GroupCheckoutRequestDto dto)` trong `MasterFolioServiceImpl`.
   - Thực thi trong Transaction `@Transactional`:
     - Kiểm tra tất cả Sub-Folio xem còn công nợ cá nhân chưa thanh toán không (nếu có, cảnh báo).
     - Tổng hợp toàn bộ tiền phòng của các phòng thành viên vào Master Folio.
     - Lấy số tiền cọc ban đầu từ hợp đồng (`GroupContract.depositAmount`), tạo giao dịch giảm trừ `DEPOSIT_DEDUCTION`.
     - Xác nhận phương thức thanh toán khoản chênh lệch còn lại (Chuyển khoản / Thẻ / Công nợ B2B).
     - Cập nhật tất cả các `Booking` con sang `CHECKED_OUT`.
     - Xuất hóa đơn VAT gộp (`GroupMasterInvoice`).
2. **Frontend:**
   - Xây dựng component `GroupCheckoutModal.tsx` và trang `MasterFolioDetailPage.tsx` cho phép Lễ tân xem bảng phân bổ chi phí và nhấn nút "Thanh toán & Check-out toàn đoàn".

---

### 3.3 T-SP2-13 – SQL Optimization + Redis Cache (🟢 ĐÃ HOÀN THÀNH)

#### Bằng chứng thực tế trong Source Code:
- **Cấu hình Redis Cache:**
  - `backend/src/main/java/vn/edu/utc/hotel_booking/common/config/RedisConfig.java`:
    - Kích hoạt `@EnableCaching`.
    - Thiết lập `RedisCacheConfiguration` serialize JSON bằng Jackson.
    - Cấu hình TTL (Time-To-Live) mặc định 30 phút cho cache `"ratePlans"`.
  - `PricingRuleServiceImpl.java`:
    - Áp dụng `@Cacheable(value = "ratePlans", key = "#id")` khi tra cứu chính sách giá phòng.
    - Áp dụng `@CacheEvict(value = "ratePlans", allEntries = true)` khi tạo mới, cập nhật hoặc xóa chính sách giá để đảm bảo tính nhất quán dữ liệu.
  - Đã có test case kiểm thử cache: `RatePlanCacheTest.java` (PASS 4/4 kịch bản).
- **Tối ưu hóa Database Indexes (Liquibase):**
  - File SQL Migration: `backend/src/main/resources/db/sqlFile/incremental/2026/20261101-233000-system-optimization-indexes.sql`.
  - Đã đánh 11 Composite Indexes chuyên sâu:
    1. `idx_bookings_hotel_status_dates` trên bảng `bookings(hotel_id, status, check_in_date, check_out_date)`.
    2. `idx_bookings_guest_history` trên bảng `bookings(guest_id, created_at DESC)`.
    3. `idx_room_slots_occupancy` trên bảng `room_slots(hotel_id, date, status, room_type_id)`.
    4. `idx_payments_booking_status` trên bảng `payments(booking_id, status)`.
    5. Các index tăng tốc trên bảng `pricing_rules`, `audit_logs`, `stay_guests`.
- **Cảnh báo cần lưu ý:**
  - Trong phương thức `BookingServiceImpl.filter()`, việc map danh sách đặt phòng sang DTO có truy cập vào các quan hệ Lazy-loaded (`stayGuests`, `charges`). Cần đảm bảo sử dụng `JOIN FETCH` hoặc `@EntityGraph` để triệt tiêu hoàn toàn nguy cơ N+1 query khi dữ liệu lớn.

---

### 3.4 T-SP2-15 – Stress Test 200 CCU + Security/RBAC Audit (🟡 ĐÃ CÓ MỘT PHẦN)

#### Bằng chứng thực tế trong Source Code:
- **Bảo mật & Phân quyền (RBAC):**
  - Đã có file test toàn diện: `backend/src/test/java/vn/edu/utc/hotel_booking/modules/security/RbacSecurityAuditTest.java`.
  - Đã kiểm tra 13 test case độc lập:
    - Chặn truy cập ẩn danh (401 Unauthorized) trên tất cả các endpoint nghiệp vụ.
    - Chặn sai quyền (403 Forbidden) khi Khách hàng/Lễ tân cố tình truy cập chức năng Quản trị viên.
    - Chấp thuận đúng vai trò cho Lễ tân (`ROLE_RECEPTIONIST`), Quản lý (`ROLE_PROPERTY_MANAGER`), và Quản trị chuỗi (`ROLE_CHAIN_ADMIN`).
- **Xử lý tranh chấp phòng & Chống Overbooking:**
  - Đã có test case: `backend/src/test/java/vn/edu/utc/hotel_booking/modules/booking/service/BookingConcurrencyTest.java`.
  - Mô phỏng 10 luồng song song cùng tranh chấp đặt 1 phòng trống duy nhất còn lại.
  - Cơ chế Database Locking (`PESSIMISTIC_WRITE` trên bảng tồn kho phòng) đảm bảo chỉ đúng 1 luồng thành công, 9 luồng còn lại nhận lỗi hết phòng. Không xảy ra tình trạng Overbooking.
- **Phần còn thiếu sót:**
  - Chưa có bộ script tải ngoại vi (External Load Test Script) bằng k6 hoặc JMeter để mô phỏng tải 200 CCU thực tế qua cổng mạng HTTP/HTTPS kèm theo báo cáo độ trễ (Latency P95, P99).

#### Đề xuất script k6 kiểm thử 200 CCU (`load-tests/k6-booking-stress-test.js`):
- **Kịch bản:** 200 Virtual Users (VUs) gửi đồng thời request tìm kiếm phòng trống và thực hiện đặt phòng trong vòng 60 giây.
- **Tiêu chí chấp nhận (SLA):**
  - Tỷ lệ lỗi HTTP 5xx < 0.1%.
  - Response time P95 < 800ms đối với API tìm kiếm phòng.
  - Response time P95 < 1200ms đối với API xác nhận đặt phòng.
  - Số lượng phòng thực tế trong cơ sở dữ liệu không bao giờ bị âm (Tuyệt đối không Overbooking).

---

## 4. KẾ HOẠCH & THỨ TỰ TRIỂN KHAI CHO MEMBER 3 (SAU KHI ĐƯỢC DUYỆT)

Nhằm tối ưu hóa hiệu quả và không gây xung đột code, thứ tự triển khai khuyến nghị chia làm 4 giai đoạn:

```text
Giai đoạn 1: Hoàn thiện Frontend 6 Trang Quản trị IAM & Org (T-SP1-08)
  └── Backend đã có sẵn 100%, triển khai UI giúp mở khóa toàn bộ chức năng quản trị trên Sidebar.

Giai đoạn 2: Tích hợp OCR CCCD vào Walk-in Booking (T-SP1-07)
  └── Tái sử dụng `identityOcrService` để hỗ trợ Lễ tân nhận diện giấy tờ nhanh tại quầy.

Giai đoạn 3: Xây dựng Email Notification Service (T-SP1-10)
  └── Tích hợp Spring Mail, cấu hình SMTP, tạo HTML templates và cơ chế Async Event.

Giai đoạn 4: Group Checkout API & UI (T-SP2-08) + E2E / Stress Test Suite (T-SP1-15, T-SP2-15)
  └── Hoàn thiện luồng thanh toán cấn trừ cọc cho khách đoàn và đóng gói bộ test tự động.
```

---

## 5. CAM KẾT TUÂN THỦ NGUYÊN TẮC
1. Báo cáo này hoàn toàn dựa trên bằng chứng kiểm tra thực tế trong mã nguồn của kho lưu trữ.
2. Tuyệt đối không thay đổi mã nguồn, không chạy script migration và không can thiệp cơ sở dữ liệu trong bước rà soát này.
3. Chờ phản hồi **"OK / Proceed"** từ Quản lý dự án / User trước khi tiến hành viết code.

