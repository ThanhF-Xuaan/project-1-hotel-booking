---
trigger: always_on
description: "Master Rules Index: Điều hướng toàn bộ quy chuẩn sinh code và kiến trúc cho dự án 'Hệ thống Quản lý Khách sạn'"
---

# 📚 'Hệ thống Quản lý Khách sạn' Rules — Index & Directory Router

> **Dự án:** 'Hệ thống Quản lý Khách sạn' ('Hệ thống quản lý chuỗi khách sạn nội bộ tập trung, đồng bộ dữ liệu thời gian thực tích hợp AI tư vấn đặt phòng và tự động hóa vận hành đa phân hệ')  
> **Tech Stack:** 'Java 21, Spring Boot 4.0.7' | 'React 19, TypeScript 6.0, Vite 8.2, Tailwind CSS v4, React Hook Form, Zod' | 'PostgreSQL 16'  
> **Phiên bản Rules:** 1.0.0

---

## 🗺️ Bản Đồ Điều Hướng Quy Chuẩn (Rules Map)

| Danh Mục | Tệp Quy Chuẩn | Mô Tả Trọng Tâm |
| :--- | :--- | :--- |
| **Cốt Lõi AI** | [`01-core-ai-guidelines.md`](file:///./01-core-ai-guidelines.md) | Nguyên tắc không ảo giác, quy trình cascade sinh code, kiểm soát ngữ cảnh. |
| **Backend** | [`02-backend-standards.md`](file:///./02-backend-standards.md) | Kiến trúc phân tầng, Search DTO, Soft Delete, Batch Delete, Migration, RBAC. |
| **Frontend** | [`03-frontend-standards.md`](file:///./03-frontend-standards.md) | Component-Service, Reactive State, Memory Leak, Form Validation, Clean UI. |
| **QA / Tester** | [`04-qa-testing-standards.md`](file:///./04-qa-testing-standards.md) | ISTQB EP/BVA, Decision Table, định dạng xuất testcase Excel / Markdown. |
| **Design System**| [`05-design-system-tokens.md`](file:///./05-design-system-tokens.md) | Bảng màu Tokens, Spacing, Border Radius, Button, Input, Modal. |
| **Thư Viện Prompt**| [`06-prompts-library.md`](file:///./06-prompts-library.md) | Prompt mẫu tạo Module, đồng bộ Swagger DTO, Modal xác nhận xóa, CRUD toast. |

---

## ⚠️ Nguyên Tắc Bắt Buộc Cao Nhất (Non-Negotiable Rules)

1. **Tuân thủ quy trình Cascade:**
   - **Backend:** `Entity` ➔ `DTO & Search DTO` ➔ `Repository` ➔ `Service` ➔ `Controller` ➔ `Unit Test`.
   - **Frontend:** `DTO/Model` ➔ `Helper/Mapper` ➔ `API Service` ➔ `UI Component` ➔ `Routes & Menu`.
2. **Zero-Placeholder Policy:**
   - Tuyệt đối KHÔNG sinh code dở dang có chứa `TODO`, `TBD`, `// thêm logic tại đây`. Mọi file sinh ra phải biên dịch được ngay.
3. **Quản lý bộ nhớ & Hủy đăng ký:**
   - Mọi luồng Async / Stream / Subscription trên Frontend bắt buộc phải được hủy khi component bị destroy.
4. **Bảo mật & Phân quyền:**
   - Mọi API endpoint mới bắt buộc phải có annotation phân quyền RBAC và ghi nhận vào bảng cấu hình quyền hạn.
