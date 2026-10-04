# Đề xuất hợp nhất nội dung vào AGENTS.md (Cho Member 2 - Project Lead)

File này chứa nội dung cấu hình riêng của Member 1 (AI & Data Specialist) để Member 2 xem xét và gộp vào file `AGENTS.md` (hoặc cấu hình Agent chung của toàn đội) ở gốc repository. Các cấu hình cụ thể chi tiết hơn nằm ở `AGENTS.member1.md`.

## Phần đề xuất thêm/bổ sung vào AGENTS.md

### 1. Ranh giới và Phân quyền (Thêm vào phần quy tắc chung)
- **Contract-first:** Không tự ý thay đổi API (Swagger/OpenAPI) của thành viên khác. Khi có yêu cầu thay đổi, viết đề xuất thay vì trực tiếp sửa code.
- **Không can thiệp chéo:** 
  - Member 1 KHÔNG sửa Entities/Repositories do Member 2 quản lý (T01).
  - KHÔNG sửa cấu hình bảo mật Keycloak của T02.
  - Các thao tác đến Booking/Pricing (T07, T08) hoặc Payment/POS (T06, T09) phải thực hiện qua API nội bộ đã công bố.

### 2. Quy tắc về Dữ liệu Cá nhân & An toàn (PII - Thêm vào phần bảo mật)
- KHÔNG đưa dữ liệu người dùng thật (CCCD, hộ chiếu, số điện thoại, email, lịch sử lưu trú, v.v.) vào file text, file seed, test data, prompt, hay repo. Bắt buộc dùng dữ liệu tổng hợp hoặc giả lập.
- PII phải được che (masking) trong logs.
- Với các module AI/Chatbot: Quyền truy cập/thao tác (đặt phòng, giữ phòng) phải xác thực theo luồng Keycloak và có sự đồng ý rõ ràng từ user qua prompt.
- Chạy `pii-auditor` (hoặc công cụ rà soát PII tương đương) trước khi mở PR cho các task xử lý dữ liệu người dùng (T05, U13, U14, T12).

### 3. Quy trình Đánh giá AI (Dành riêng cho các task AI)
- **Nguyên tắc "Eval trước, code sau":** Các tính năng AI phải có Eval set được phê duyệt trước khi Agent bắt đầu viết code tích hợp.
- **Không sửa đổi Data chuẩn:** Cấm Agent sửa đáp án chuẩn hoặc ngưỡng đánh giá (threshold) để thao túng kết quả.
- **Nguồn sự thật cho RAG:** Giá và tồn phòng luôn lấy từ việc gọi API Backend, tuyệt đối không lấy từ Vector Database.

### 4. Definition of Done (Bổ sung thêm cho các Task AI/Data)
- Ngoài việc 100% Unit test pass, build/lint sạch, Swagger đồng bộ:
  - Task AI bắt buộc đính kèm báo cáo eval (metrics đạt ngưỡng).
  - Task liên quan đến Dữ liệu Cá nhân phải có báo cáo xác nhận không rò rỉ dữ liệu (PII Audit clean).
