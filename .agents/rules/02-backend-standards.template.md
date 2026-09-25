---
trigger: always_on
description: "Quy chuẩn sinh code Backend: Kiến trúc phân tầng, API, DTO, Search Filter, Soft Delete, Migration và Unit Test cho 'Hệ thống Quản lý Khách sạn'"
---

# ⚙️ Quy Chuẩn Phát Triển Backend (Backend Coding Standards)

> **Công nghệ áp dụng:** 'Java 21, Spring Boot 4.0.7'  
> **Cơ sở dữ liệu & Migration:** 'PostgreSQL 16'

---

## 1. Kiểu Dữ Liệu & Quy Ước Trường (Data Types & Fields)

- **Trường Trạng Thái / Loại (Dropdown/Enum):**
  - Khuyến nghị dùng kiểu số nguyên (`Integer` hoặc `Long`) thay vì chuỗi thuần để tối ưu hiệu năng đánh index trong DB.
  - Ví dụ: `statusId` (0 = Inactive, 1 = Active, 2 = Pending).
  - Khai báo rõ ràng ghi chú comment ý nghĩa từng giá trị.
- **Trường Thời Gian:** Dùng kiểu timestamp có timezone (`OffsetDateTime`, `Instant`, hoặc `ZonedDateTime`).

---

## 2. Thiết Kế API Tìm Kiếm & Lọc Dữ Liệu (Search & Filter API)

- **Nguyên tắc:** Endpoint tìm kiếm có từ 2 trường lọc trở lên **BẮT BUỘC** dùng phương thức `POST` với `@RequestBody` (Search DTO), **KHÔNG** dùng `GET` nối chuỗi query param dài.
  - *Lý do:* Tránh tràn URL, bảo mật dữ liệu tìm kiếm, hỗ trợ filter danh sách mảng ID phức tạp.
- **Quy ước đặt tên Search DTO:** `<Entity>SearchDto` (ví dụ: `ProductSearchDto`).
- **Quy ước Endpoint:** `POST /api/v1/<resource>/filter`.
- **Quy ước so khớp dữ liệu:**
  - Dropdown, Ngày tháng, ID ➔ So khớp chính xác (**Exact Match**).
  - Text input (Tên, Mã, Email) ➔ So khớp gần đúng (**LIKE / Case-Insensitive**).

---

## 3. Kiến Trúc Phân Tầng Chuẩn (Layered Architecture)

Khi tạo một tính năng/module mới, bắt buộc triển khai đầy đủ các tầng theo thứ tự:

```text
1. Entity Layer       ➔ Map trực tiếp bảng DB, kế thừa BaseEntity (id, createdAt, createdBy...)
2. DTO Layer          ➔ Chuyển đổi dữ liệu ra ngoài API, không bao giờ chứa thông tin nhạy cảm
3. Search DTO Layer   ➔ Chứa các trường filter optional (phân trang page, pageSize, sort)
4. Repository Layer   ➔ Kế thừa BaseRepository, viết query tối ưu
5. Service Interface  ➔ Khai báo hợp đồng nghiệp vụ
6. Service Impl Layer ➔ Xử lý logic, mapping Entity ↔ DTO, quản lý @Transactional
7. Controller Layer   ➔ Tiếp nhận HTTP Request, bắt lỗi Validate @Valid, phân quyền RBAC
8. Unit Test Layer    ➔ Viết test cho Service và Controller
```

---

## 4. Xóa Mềm & Xóa Hàng Loạt (Soft Delete & Batch Delete)

- **Soft Delete:**
  - Bảng dữ liệu sử dụng cột cờ `is_deleted` (0 = Active, 1 = Deleted) hoặc `deleted_at`.
  - Mọi câu truy vấn tìm kiếm dữ liệu mặc định phải kèm điều kiện `is_deleted = 0`.
- **Gộp Xóa Đơn & Xóa Nhiều (Unified Batch Delete):**
  - Gom chung thành 1 endpoint duy nhất: `DELETE /api/v1/<resource>/delete` nhận Request Body `List<Long> ids`.
  - *Xóa 1 bản ghi:* Client gửi mảng 1 phần tử `[101]`.
  - *Xóa nhiều bản ghi:* Client gửi mảng `[101, 102, 103]`.
  - Giúp giảm thiểu trùng lặp mã nguồn và đơn giản hóa API client.

---

## 5. Migration Cơ Sở Dữ Liệu (Liquibase Database Migration)

- **Nguyên tắc:** Không bao giờ chạy DDL thủ công trực tiếp trên Production. Mọi thay đổi cấu trúc bảng, chỉ mục (Index), dữ liệu khởi tạo (Seed data) phải thông qua **Liquibase Database Migration** (`backend/src/main/resources/db/`).
- **Cấu trúc 3 tầng quản lý bắt buộc:**
  - `db/db-changelog-root.xml`: File điều phối chính (Root Master Changelog).
  - `db/changelogs/`: Chứa các ChangeSet XML (quản lý ID, author, context, runOnChange, splitStatements).
  - `db/sqlFile/`: Chứa mã nguồn SQL thuần túy (DDL, PL/pgSQL Functions, Triggers, Indexes, Data Scripts).
- **Quy ước Incremental ChangeSet:**
  - Khi thêm tính năng hoặc chỉnh sửa DB sau v1.0.0, tạo file XML trong `db/changelogs/incremental/<YYYY>/YYYYMMDD-HHmmss-<mo_ta>.xml` và file SQL tương ứng trong `db/sqlFile/incremental/<YYYY>/YYYYMMDD-HHmmss-<mo_ta>.sql`.
  - Master Changelog tự động quét và thực thi qua `<includeAll path="db/changelogs/incremental" />`.
- **Quy ước Contexts:**
  - `context="master-data"`: Dữ liệu nền tảng hệ thống bắt buộc (Permissions, Roles, Tax, Currencies...). Chạy trên mọi môi trường.
  - `context="dev-seed"`: Dữ liệu mẫu (Khách sạn mẫu, phòng mẫu, menu demo...). Chỉ chạy trên Local/Dev/Staging.

---

## 6. Phân Quyền & Bảo Mật (RBAC & Keycloak IAM)

- **Cơ chế xác thực:** Hệ thống sử dụng Keycloak OAuth2 Resource Server. `KeycloakJwtAuthenticationConverter` tự động trích xuất `realm_access.roles` thành các `GrantedAuthority` chuẩn `ROLE_<ROLE_NAME>`.
- **Vai trò cấp cao (System Roles):**
  - `ROLE_CHAIN_ADMIN`: Toàn quyền quản trị hệ thống và chuỗi khách sạn.
  - `ROLE_REGION_MANAGER`: Giám đốc quản lý cụm khách sạn theo vùng.
  - `ROLE_PROPERTY_MANAGER`: Tổng quản lý điều hành khách sạn cơ sở.
  - `ROLE_RECEPTIONIST`: Nhân viên lễ tân tiếp nhận đặt phòng, check-in, check-out.
  - `ROLE_HOUSEKEEPING`: Nhân viên buồng phòng cập nhật trạng thái dọn dẹp.
  - `ROLE_CUSTOMER`: Khách hàng đặt phòng trực tuyến.
- **Quyền hạn chi tiết (Granular Permissions):**
  - Mọi endpoint tạo mới đều phải được khai báo quyền truy cập rõ ràng qua `@PreAuthorize("hasRole('ROLE_...')")` hoặc `@PreAuthorize("hasAuthority('ROLE_...')")`.
  - **Convention:** `ROLE_<MODULE>_<ACTION>` (viết hoa, phân cách bằng dấu gạch dưới `_`).
    - `POST /api/v1/product/filter` ➔ `ROLE_PRODUCT_VIEW`
    - `POST /api/v1/product/create` ➔ `ROLE_PRODUCT_CREATE`
    - `PUT /api/v1/product/update/{id}` ➔ `ROLE_PRODUCT_UPDATE`
    - `DELETE /api/v1/product/delete` ➔ `ROLE_PRODUCT_DELETE`

---

## 7. Tiêu Chuẩn Viết Unit Test (Unit Testing Standards)

- **Tầng Service:**
  - Sử dụng Mocking framework ('JUnit 5, Mockito, AssertJ, MockMvc').
  - Phải kiểm thử đủ 3 kịch bản:
    1. **Happy path:** Dữ liệu hợp lệ ➔ Trả về kết quả đúng.
    2. **Validation / Error path:** Dữ liệu sai, không tồn tại ID ➔ Ném đúng ngoại lệ (Custom Exception).
    3. **Edge cases:** Danh sách rỗng, trường tìm kiếm chứa ký tự đặc biệt `%_`.
- **Tầng Controller:**
  - Kiểm tra tính đúng đắn của HTTP status code, format JSON trả về, và cơ chế validation `@Valid`.
