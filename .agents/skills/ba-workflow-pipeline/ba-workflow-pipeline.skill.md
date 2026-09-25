---
name: ba-workflow-pipeline
description: "Khung quy trình toàn diện cho Business Analyst (BA): Phân tích yêu cầu (PTYC), Quy trình nghiệp vụ (QTCT), Thiết kế chi tiết (TKCT), Thiết kế CSDL (TKCSDL), User Story và Ma trận truy vết (Traceability Matrix)."
---

# 📊 BA Workflow Pipeline: Chuẩn Hóa Phân Tích Nghiệp Vụ

## 🎯 Mục Tiêu
Cung cấp luồng làm việc chuẩn mực cho AI trong vai trò Senior Business Analyst, giúp chuyển hóa yêu cầu người dùng từ sơ khai thành tài liệu SRS và đặc tả kỹ thuật chi tiết.

---

## 🧭 Quy Trình 5 Giai Đoạn (5-Stage BA Pipeline)

```text
[1. Tiếp Nhận & Khảo Sát] ──► [2. Phân Tích Yêu Cầu (PTYC)] ──► [3. Quy Trình Nghiệp Vụ (QTCT)]
                                                                               │
                                                                               ▼
[5. Handoff & Traceability] ◄── [4. Thiết Kế Chi Tiết (TKCT / TKCSDL)] ◄──────┘
```

### 1. Phân Tích Yêu Cầu Nghiệp Vụ (PTYC)
- Xác định bối cảnh nghiệp vụ (Business Context), các bên liên quan (Stakeholders), mục tiêu (Objectives) và phạm vi (In-scope / Out-of-scope).
- Bóc tách danh sách Yêu cầu chức năng (FR) và Yêu cầu phi chức năng (NFR: Hiệu năng, Bảo mật, Khả năng mở rộng).

### 2. Mô Tả Quy Trình Nghiệp Vụ (QTCT)
- Vẽ biểu đồ Activity Diagram (Luồng hoạt động) mô tả các bước từ khi bắt đầu đến khi kết thúc.
- Xây dựng ma trận phân quyền (RACI Matrix): Ai là người Tạo, Duyệt, Thực thi, Nhận thông báo.

### 3. Thiết Kế Chi Tiết Chức Năng (TKCT)
- Đặc tả từng màn hình (Screen Specifications):
  - Danh sách trường dữ liệu: Tên trường, Kiểu dữ liệu, Bắt buộc/Không, Ràng buộc min/max/regex.
  - Luồng sự kiện chính (Happy Path) và luồng sự kiện phụ / lỗi (Alternative / Exception Paths).

### 4. Thiết Kế Mô Hình Dữ Liệu (TKCSDL)
- Vẽ biểu đồ ERD (Entity Relationship Diagram).
- Định nghĩa chi tiết các bảng: Khóa chính (PK), Khóa ngoại (FK), Chỉ mục (Indexes) và Ràng buộc toàn vẹn.

### 5. Ma Trận Truy Vết (Traceability Matrix)
- Đảm bảo 100% Yêu cầu nghiệp vụ (Business Need) ➔ Có User Story ➔ Có Thiết kế TKCT ➔ Có Kịch bản Test Case tương ứng.
