---
trigger: always_on
description: "Thư viện Prompt mẫu cho 'Hệ thống Quản lý Khách sạn': Prompt tạo module mới, đồng bộ Swagger DTO, Modal xác nhận xóa, CRUD Toast"
---

# 📝 Thư Viện Prompt Mẫu Cho Lập Trình Viên (Prompts Library)

Các mẫu prompt dưới đây được thiết kế sẵn để lập trình viên copy và sử dụng ngay nhằm đạt chất lượng sinh code cao nhất từ AI Agent.

---

## 📌 Prompt 1: Tạo Module Tính Năng Mới Hoàn Chỉnh (Full Module Generator)

```markdown
Hãy tạo module tính năng mới [TÊN_MODULE] cho dự án 'Hệ thống Quản lý Khách sạn' theo đúng quy chuẩn:
1. Tech Stack: Backend ('Java 21, Spring Boot 4.0.7'), Frontend ('React 19, TypeScript 6.0, Vite 8.2, Tailwind CSS v4, React Hook Form, Zod').
2. Quy trình Cascade bắt buộc:
   - configs/[tên].model.ts (Interface, DTO, Enum, comment đầy đủ các trường).
   - services/[tên]-helper.service.ts (mapToUI, mapToApi, STATUS_OPTIONS).
   - services/[tên].service.ts (kế thừa BaseHttpService).
   - UI List Component với Layout 3 hàng chuẩn.
   - Form Modal/Drawer thêm mới & cập nhật bản ghi.
   - Đăng ký routing và menu.
3. Kiểm tra: Không để placeholder TODO, đảm bảo xử lý unsubscribe chống rò rỉ bộ nhớ.
```

---

## 📌 Prompt 2: Đồng Bộ DTO Khi Backend Đổi Swagger / OpenAPI

```markdown
Backend vừa cập nhật API cho resource [TÊN_RESOURCE]. Dưới đây là schema Swagger mới:
[DÁN_SCHEMA_SWAGGER_HOẶC_JSON_TẠI_ĐÂY]

Hãy thực hiện:
1. Cập nhật lại file `configs/[tên].model.ts` tương ứng.
2. Kiểm tra và sửa đổi các hàm mapping `mapToUI()` và `mapToApi()` trong Helper Service.
3. Kiểm tra các trường hiển thị trên bảng dữ liệu `table-cfg.ts` và Form nhập liệu.
4. Báo cáo danh sách các file đã được đồng bộ.
```

---

## 📌 Prompt 3: Tạo Modal Xác Nhận Xóa Chuẩn

```markdown
Hãy tích hợp Modal xác nhận xóa cho chức năng [TÊN_CHỨC_NĂNG]:
- Hỗ trợ cả 2 trường hợp: Xóa 1 bản ghi từ action table và Xóa hàng loạt từ checkbox toolbar.
- Nút xác nhận màu đỏ cảnh báo (Danger), có loading spinner.
- Sau khi xóa thành công: Gọi API reload lại bảng, hiển thị Toast thông báo thành công và bỏ chọn các checkbox.
```

---

## 📌 Prompt 4: Sinh Bộ Kịch Bản Test Cho QA Lead

```markdown
Hãy phân tích tài liệu yêu cầu / User Story dưới đây và sinh bộ Test Case chi tiết cho chức năng [TÊN_CHỨC_NĂNG]:
[DÁN_NỘI_DUNG_USER_STORY_HOẶC_MÔ_TẢ_NGHIỆP_VỤ]

Yêu cầu kỹ thuật:
- Áp dụng đầy đủ: Phân vùng tương đương (EP), Phân tích giá trị biên (BVA), Bảng quyết định.
- Bao phủ: UI, Validation, Business Rule, Phân quyền Role, Xử lý lỗi ngoại lệ.
- Format xuất bản: Bảng kịch bản với mã TC_ID tuần tự (TC_001, TC_002...) và cột Steps phân cách bằng dấu `;`.
```
