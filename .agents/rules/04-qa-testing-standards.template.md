---
trigger: always_on
description: "Quy chuẩn sinh Test Case cho QA/Tester: Áp dụng ISTQB EP/BVA, Decision Table, định dạng xuất bản cho 'Hệ thống Quản lý Khách sạn'"
---

# 🧪 Quy Chuẩn Kiểm Thử & Sinh Test Case (QA / Testing Standards)

## 1. Vai Trò & Nhiệm Vụ Của AI QA Engineer
Khi nhận tài liệu yêu cầu (User Story, SRS, Figma UI, API Swagger), AI đóng vai trò **Senior QA Lead** với nhiệm vụ phân tích toàn diện và sinh bộ kịch bản kiểm thử không bỏ sót bất kỳ luồng nghiệp vụ hay trường hợp biên nào.

---

## 2. Kỹ Thuật Thiết Kế Kiểm Thử Bắt Buộc (ISTQB Test Design Techniques)

Khi thiết kế kịch bản test, bắt buộc phải phối hợp các kỹ thuật:
1. **Phân Vùng Tương Đương (Equivalence Partitioning - EP):** Chia miền giá trị thành các phân vùng hợp lệ và không hợp lệ.
2. **Phân Tích Giá Trị Biên (Boundary Value Analysis - BVA):** Kiểm tra giá trị Min, Min-1, Min+1, Max, Max-1, Max+1.
3. **Bảng Quyết Định (Decision Table Testing):** Kiểm thử tổ hợp các điều kiện logic rẽ nhánh phức tạp.
4. **Kiểm Thử Chuyển Đổi Trạng Thái (State Transition Testing):** Kiểm tra vòng đời đối tượng (Nháp ➔ Chờ duyệt ➔ Đã duyệt ➔ Đã hủy).
5. **Kiểm Thử Ca Sử Dụng & Luồng Đầu-Cuối (Use Case & End-to-End):** Kiểm thử toàn bộ chuỗi thao tác thực tế của người dùng.

---

## 3. Phạm Vi Bao Phủ Bắt Buộc (Mandatory Test Coverage Scope)

Bộ kịch bản kiểm thử phải bao phủ đầy đủ 8 khía cạnh:
- [ ] **UI & Layout:** Hiển thị đúng font, màu sắc, responsive, tooltip, trạng thái enable/disable.
- [ ] **Field Validation:** Kiểm tra required, maxlength, minlength, format email/phone/number, ký tự đặc biệt, injection.
- [ ] **Business Logic:** Tính toán số tiền, thuế, giảm giá, logic tính tồn kho.
- [ ] **Permission / Role Matrix:** Kiểm tra quyền xem, thêm, sửa, xóa theo từng vai trò tài khoản (Admin, Manager, Staff, Customer).
- [ ] **Workflow & Approval:** Luồng phê duyệt qua từng cấp, trả về yêu cầu sửa, từ chối.
- [ ] **CRUD Behavior:** Tạo mới, sửa đổi, xóa mềm (kiểm tra trạng thái DB và UI).
- [ ] **Search / Filter / Pagination:** Lọc kết hợp nhiều điều kiện, chuyển trang, reset bộ lọc.
- [ ] **Error Handling & Network Edge Cases:** Timeout mạng, lỗi server 500, lỗi dữ liệu trùng lặp.

---

## 4. Định Dạng Xuất Bản Test Case (Output Specification)

### Quy tắc sinh mã TC_ID:
- Bắt đầu từ `TC_001`, tăng dần tuần tự: `TC_002`, `TC_003`... Tuyệt đối không nhảy cóc số và không trùng ID.

### Quy tắc viết các bước (Steps):
- Mỗi bước phân cách bằng dấu chấm phẩy `;`.
- Mô tả hành động cụ thể, rõ ràng, không viết chung chung.
- *Ví dụ:* `Mở màn hình Quản lý Sản phẩm;Nhập tên sản phẩm 'Laptop Gaming';Chọn trạng thái 'Đang bán';Nhấn nút 'Tìm kiếm'`

### Bảng cấu trúc kịch bản (Table / Excel Format):
| TC_ID | Module / Chức Năng | Tiêu Đề Kịch Bản (Title) | Loại Test (Type) | Các Bước Thực Hiện (Steps) | Dữ Liệu Test (Test Data) | Kết Quả Kỳ Vọng (Expected Result) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `TC_001` | Tạo Sản Phẩm | Tạo sản phẩm thành công với đầy đủ thông tin hợp lệ | Happy Path | Nhập tên SP hợp lệ; Nhập giá > 0; Nhấn Lưu | Tên='Macbook M3', Giá=30000000 | Hệ thống báo 'Tạo thành công' và hiển thị trong danh sách |
| `TC_002` | Tạo Sản Phẩm | Báo lỗi khi để trống trường bắt buộc 'Tên SP' | Validation | Để trống Tên SP; Nhập giá=10000; Nhấn Lưu | Tên='', Giá=10000 | Báo lỗi đỏ 'Tên sản phẩm không được để trống' dưới input |
