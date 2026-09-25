---
name: executing-plans
description: "Sử dụng để thực thi bản kế hoạch triển khai đã được phê duyệt, theo từng batch có kiểm soát và xác thực kết quả."
---

# ⚡ Executing Plans: Thực Thi Kế Hoạch Theo Checkpoint

## 📋 Quy Trình Thực Thi

1. **Nạp bản kế hoạch:** Đọc kỹ danh sách các Task và Checkbox từ file plan đã tạo.
2. **Thực thi theo từng Batch (1 - 3 tasks nhỏ):**
   - Tạo/sửa đúng các file đã khai báo.
   - Chạy các lệnh kiểm thử được chỉ định trong plan.
   - Không tự ý thêm bớt các tính năng nằm ngoài phạm vi task hiện tại.
3. **Checkpoints & Validation:**
   - Sau khi hoàn thành một Batch, chạy toàn bộ test suite để đảm bảo không hồi quy (No regression).
   - Đánh dấu `[x]` vào checkbox của task tương ứng.
4. **Báo cáo tiến độ:** Thông báo ngắn gọn cho người dùng những task đã hoàn thành và chuẩn bị thực thi batch tiếp theo.
