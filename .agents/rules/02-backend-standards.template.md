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

## 5. Migration Cơ Sở Dữ Liệu (Database Migration)

- **Nguyên tắc:** Không bao giờ chạy DDL thủ công trực tiếp trên Production. Mọi thay đổi cấu trúc bảng, chỉ mục (Index), dữ liệu khởi tạo (Seed data) phải thông qua công cụ Migration ('PostgreSQL 16').
- **Quy ước file migration:**
  - Định dạng tên file: `V<YYYYMMDD_HHmmss>__<mo_ta_ngan_gon>.sql`
  - Ví dụ: `V20260915_093000__create_table_products.sql`

---

## 6. Phân Quyền & Bảo Mật (RBAC & Permissions)

- Mọi endpoint tạo mới đều phải được khai báo quyền truy cập rõ ràng.
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
