---
name: systematic-debugging
description: "Quy trình chẩn đoán lỗi chuyên sâu theo 4 giai đoạn nghiêm ngặt để tìm ra nguyên nhân gốc rễ trước khi sửa mã nguồn."
---

# 🔍 Systematic Debugging: Chẩn Đoán Lỗi Chuyên Sâu

## 🛑 Nguyên Tắc Vàng
> **KHÔNG SỬA CODE MÒ (No Guess-and-Check):** Không được sửa code ngẫu nhiên để hy vọng lỗi biến mất. Mọi hành động sửa đổi phải dựa trên bằng chứng và nguyên nhân gốc rễ (Root Cause) đã được chứng minh.

---

## 🔬 Quy Trình 4 Giai Đoạn (4-Phase Debugging)

```text
[Phase 1: Tái Hiện Lỗi] ──► [Phase 2: Cô Lập Phạm Vi] ──► [Phase 3: Tìm Nguyên Nhân Gốc]
                                                                        │
                                                                        ▼
                                   [Phase 4: Sửa Đổi & Viết Regression Test]
```

### Phase 1: Tái Hiện Lỗi Ổn Định (Reproduce)
- Thu thập đầy đủ log, stack trace, input đầu vào.
- Viết 1 test case tái hiện chính xác lỗi đó (Reproducing Test).

### Phase 2: Cô Lập Phạm Vi (Isolate)
- Xác định tầng bị lỗi: Frontend UI, HTTP Interceptor, Backend Controller, Service Logic, Database Query, hay Network Payload.

### Phase 3: Tìm & Chứng Minh Nguyên Nhân Gốc (Root Cause)
- Đặt câu hỏi: *"Tại sao giá trị này bị Null / Sai lệch ở thời điểm này?"*
- Truy vết ngược luồng dữ liệu (Backtracking) để tìm chính xác điểm phát sinh lỗi.

### Phase 4: Sửa Đổi & Kiểm Thử Hồi Quy (Fix & Regression Test)
- Thực hiện sửa đổi tối thiểu và chính xác nhất.
- Chạy lại Reproducing Test ➔ Đảm bảo test đã PASS.
- Chạy toàn bộ Test Suite của dự án ➔ Đảm bảo không làm hỏng tính năng khác.
