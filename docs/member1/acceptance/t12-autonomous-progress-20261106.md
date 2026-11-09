# T12 — kết quả phần việc tự động

CONFIRMED: base f1e26b80268c166dae0c6d7ee91d7b7718190e54; worktree riêng C:/Users/admin/.codex/worktrees/m1-t12-chatbot-20261106. Kế hoạch chưa hoàn thành toàn bộ. Dừng trước provisioning vì cần Member 2 duyệt target DB/grants/IAM; người dùng vừa yêu cầu dừng khi gặp phụ thuộc owner.

## Lựa chọn và lý do

- Giữ cập nhật Lombok của upstream, chỉ ghép dependency T12: tránh ghi đè thay đổi nhóm. Kết quả 43 file áp nguyên + POM ghép riêng.
- Chọn JDK21 theo repo thay cho Java25 mặc định: phù hợp annotation processors của nền mới.
- Thêm pilot eval v1 riêng; không sửa gold cũ. Dùng đúng 5 nội dung công khai đã duyệt, không tự thêm chính sách/giá/phòng vào corpus. Sáu câu thiếu nguồn phải trả thiếu bằng chứng, không tạo dữ liệu giả để có câu trả lời.
- Chọn sandbox mới, không dùng lại stack acceptance đang chạy: tránh thay đổi dữ liệu hoặc realm của công việc khác.
- Chưa tự tạo IAM, role hoặc dòng hotels. Đây là dependency Member 2, không phải lựa chọn thông thường được tự chốt.

## Bằng chứng

| Kiểm tra | Kết quả và giới hạn |
|---|---|
| Tích hợp source trên base mới | PASS cho compile và regression chọn lọc; không phải toàn bộ suite |
| Backend regression | 306 tổng: 305 PASS, 1 calibration skipped, 0 failure/error; log backend/target-integration-check.log |
| Frontend TypeScript/Vite build | PASS; log frontend/build-integration-check.log |
| Pilot pack integrity | PASS_DATA_INTEGRITY_ONLY: 5 docs, 20 RAG cases, 12 R1 specifications, 5 tamper checks rejected, 12 frozen files unchanged |
| git diff --check | PASS |
| Live model, DB/IAM mới, ingest Gemini | NOT RUN |
| 20 câu RAG/12 kịch bản R1 mới qua application | NOT RUN; mới khóa đặc tả và kiểm tính toàn vẹn |

Chạy validator từ root: `python tools/t12/validate_pilot_eval.py`. Builder từ chối ghi đè pack tồn tại. Manifest lưu hash dữ liệu mới và gold được bảo vệ. Integrity PASS không xác nhận chất lượng ngữ nghĩa chatbot.

Không đọc/in credential file, không gọi Google, không sửa DB, không commit/push. Phiên bản source đang ở worktree riêng; repo D: được giữ nguyên. Yêu cầu duyệt cụ thể tại `docs/member1/contracts/t12-live-sandbox-member2-review.md`.
