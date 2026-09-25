---
trigger: always_on
description: "Nguyên tắc cốt lõi AI: Chống hallucination, quản lý ngữ cảnh, và tác phong lập trình an toàn cho 'Hệ thống Quản lý Khách sạn'"
---

# 🛡️ Nguyên Tắc Cốt Lõi Dành Cho AI Agent (Core AI Guidelines)

## 1. Triết Lý Phát Triển Cốt Lõi (Core Engineering Principles)

1. **Không Ảo Giác (Zero Hallucination):**
   - Không đoán tên biến, tên cột DB, tên method, hoặc tên endpoint API. Nếu chưa rõ, bắt buộc phải tra cứu mã nguồn hiện có hoặc hỏi người dùng.
2. **Bảo Tồn Toàn Vẹn Mã Nguồn (Codebase Integrity):**
   - Giữ nguyên các comment, docstring, logic nghiệp vụ không liên quan đến yêu cầu hiện tại.
   - Không tự ý refactor các file ngoài phạm vi yêu cầu (No unsolicited refactoring).
3. **Mã Nguồn Chạy Được Ngay (Compilation Ready):**
   - Mọi đoạn code được sinh ra phải hoàn chỉnh, có import đầy đủ, không cắt xén bằng các ký hiệu `// ... code cũ giữ nguyên ...` hoặc `// TODO`.

---

## 2. Quy Trình 6 Bước Sinh Mã Nguồn (Cascade Workflow)

Mỗi khi nhận lệnh tạo hoặc mở rộng tính năng mới, Agent phải thực thi tuần tự theo chuỗi thác (Cascade Chain):

```text
[1. Contract / Model Layer] ──► [2. Mapping & Helper Layer] ──► [3. Core Service Layer]
                                                                        │
                                                                        ▼
[6. Build & Lint Validation] ◄── [5. Wiring & Routing] ◄────── [4. UI / API Presentation]
```

- **Bước 1:** Định nghĩa DTO / Interface / Model với comment rõ ràng cho từng trường.
- **Bước 2:** Xây dựng tầng chuyển đổi dữ liệu (`mapToUI`, `mapToApi`, formatter, validator).
- **Bước 3:** Hiện thực tầng xử lý nghiệp vụ chính (`Service` kế thừa Base Service).
- **Bước 4:** Xây dựng tầng giao tiếp (Controller API hoặc Component UI).
- **Bước 5:** Đăng ký định tuyến (Routes, Menu, Dependency Injection).
- **Bước 6:** Chạy kiểm thử tự động, build và kiểm tra lint trước khi hoàn tất.

---

## 3. Kiểm Soát Ngữ Cảnh & Bộ Nhớ AI (Context Management)

- **Tách file nhỏ:** Ưu tiên chia nhỏ các file lớn (>300 dòng) thành các sub-components hoặc helper functions để AI đọc và chỉnh sửa chính xác hơn.
- **Chỉ nạp file liên quan:** Không đọc toàn bộ repository vào ngữ cảnh cùng một lúc; chỉ nạp các file trong cùng module và các file Base dùng chung.
- **Kiểm tra tương thích ngược:** Bất kỳ thay đổi nào với Model/Entity dùng chung phải kiểm tra xem có làm hỏng các module khác đang phụ thuộc hay không.
