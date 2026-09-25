---
name: subagent-orchestration
description: "Sử dụng khi cần phân rã một bài toán lớn, phức tạp thành các tác vụ độc lập và điều phối nhiều Subagents thực thi song song."
---

# 🤖 Subagent Orchestration: Điều Phối Multi-Agent Song Song

## 🎯 Khi Nào Cần Sử Dụng Subagent?
- Khi một tác vụ có nhiều phần việc độc lập (ví dụ: Viết 5 module CRUD riêng biệt, viết Unit Test cho 10 service độc lập).
- Tránh làm ô nhiễm hoặc quá tải ngữ cảnh (Context Window Overflow) của Agent chính.

---

## 📋 Quy Trình Điều Phối 4 Bước

```text
[1. Phân Tích & Phân Rã] ──► [2. Soạn Thảo Prompt Đóng Gói] ──► [3. Dispatch Subagents]
                                                                        │
                                                                        ▼
                                   [5. Tích Hợp & Review] ◄── [4. Thu Hồi Kết Quả]
```

1. **Phân rã nhiệm vụ (Task Decomposition):** Đảm bảo mỗi Subagent chỉ chịu trách nhiệm trên 1 tập file riêng biệt, không chỉnh sửa trùng file.
2. **Soạn prompt đóng gói (Self-contained Prompt):** Cung cấp đủ thông tin: Mục tiêu, đường dẫn file cần sửa, ràng buộc coding rules, tiêu chí nghiệm thu.
3. **Dispatch song song:** Khởi chạy các subagents đồng thời.
4. **Thu hồi & Kiểm tra xung đột (Conflict Resolution):** Kiểm tra mã nguồn sinh ra từ từng subagent.
5. **Chạy Build tổng thể:** Đảm bảo toàn bộ dự án biên dịch thành công sau khi ghép nối.
