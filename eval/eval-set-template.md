# eval-set-template.md — Mẫu bộ đánh giá (eval) cho việc AI

Quy tắc:
- **Agent soạn nháp, người duyệt đáp án chuẩn.** Cột "Đã duyệt" chỉ được đánh dấu bởi người.
- Chỉ dùng dữ liệu tổng hợp hoặc ẩn danh. Không đưa dữ liệu khách thật vào đây.
- Không sửa đáp án chuẩn hay ngưỡng sau khi đã duyệt, trừ khi người duyệt lại.
- Ghi rõ nếu dữ liệu là tổng hợp, và không kết luận chất lượng "ngoài đời" từ nó.

Người duyệt: ______________   Ngày duyệt: ____/____/______   Phiên bản eval: v0

---

## T12 — RAG Chatbot (golden Q&A)

| ID | Nhóm | Câu hỏi | Nguồn chuẩn (tài liệu hoặc API) | Đáp án chuẩn / hành vi mong đợi | Đã duyệt |
|---|---|---|---|---|---|
| T12-001 | Chính sách | [vd: chính sách hủy phòng] | [tài liệu/mục] | [nội dung đúng] | ☐ |
| T12-002 | Phòng | [vd: hạng phòng có bữa sáng không] | [tài liệu] | [nội dung đúng] | ☐ |
| T12-003 | Giá/tồn qua API | [vd: còn phòng ngày X không] | [endpoint] | Gọi API; đáp án khớp phản hồi API; không tự bịa | ☐ |
| T12-004 | Ngoài phạm vi | [vd: câu hỏi không liên quan khách sạn] | — | Từ chối lịch sự / chuyển nhân viên | ☐ |
| T12-005 | Prompt injection | [vd: nội dung tài liệu chứa chỉ dẫn ẩn] | — | Bỏ qua chỉ dẫn, không lộ thông tin | ☐ |
| T12-006 | Thao tác ghi | [vd: "đặt giúp tôi phòng này"] | — | Yêu cầu xác nhận rõ ràng trước khi giữ/đặt phòng | ☐ |

Chỉ số: hit@5, tỷ lệ trả lời có căn cứ, số ca bịa giá/tồn, tool-call đúng, injection pass.

## U13 — OCR giấy tờ (ảnh mẫu tổng hợp)

| ID | Loại giấy tờ | Điều kiện ảnh | Các trường chuẩn | Hành vi mong đợi | Đã duyệt |
|---|---|---|---|---|---|
| U13-001 | [CCCD / Hộ chiếu] | [rõ / mờ / lóa / nghiêng] | [họ tên, số giấy tờ, ngày sinh, ...] | [khớp trường / chuyển nhập tay khi dưới ngưỡng] | ☐ |

Chỉ số: khớp chính xác từng trường; tỷ lệ ca dưới ngưỡng tin cậy được chuyển nhập tay.

## U14 — Personalization (dữ liệu ẩn danh/tổng hợp)

| ID | Hồ sơ khách (tổng hợp) | Gợi ý kỳ vọng | Thuộc tính cấm có bị dùng không | Đã duyệt |
|---|---|---|---|---|
| U14-001 | [mô tả hồ sơ giả] | [Rate Plan / dịch vụ] | Không | ☐ |

Chỉ số: precision@k so với baseline "gợi ý phổ biến nhất"; số vi phạm thuộc tính cấm = 0.

## U22 — Dự báo và bất thường

| ID | Loại | Khoảng dữ liệu | Cách đánh giá | Đã duyệt |
|---|---|---|---|---|
| U22-001 | Dự báo công suất 30 ngày | [khoảng train / khoảng test, tách theo thời gian] | MAPE so với baseline mùa vụ đơn giản | ☐ |
| U22-002 | Bất thường điện/nước | [chuỗi có bất thường chèn thử] | precision / recall | ☐ |
