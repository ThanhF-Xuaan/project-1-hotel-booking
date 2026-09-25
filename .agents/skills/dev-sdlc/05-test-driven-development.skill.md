---
name: test-driven-development
description: "Thực hiện quy trình phát triển hướng kiểm thử (TDD) với chu kỳ Red-Green-Refactor nghiêm ngặt."
---

# 🔴🟢🔵 Test-Driven Development (TDD)

## 🛑 Điều Kiện Tiên Quyết
> **KHÔNG ĐƯỢC PHÉP** viết mã nguồn triển khai (Production code) trước khi có một Test Case thất bại tương ứng.

---

## 🔄 Chu Kỳ 3 Bước Red - Green - Refactor

```text
     ┌──────────────────────────────────────────────────────────┐
     │                                                          │
     ▼                                                          │
┌──────────────┐          ┌────────────────┐          ┌────────────────┐
│  1. RED      │ ───────► │  2. GREEN      │ ───────► │  3. REFACTOR   │
│  Viết test   │          │  Viết code     │          │  Làm sạch code │
│  chắc chắn   │          │  tối giản để   │          │  nhưng test    │
│  thất bại    │          │  test PASS     │          │  vẫn xanh      │
└──────────────┘          └────────────────┘          └────────────────┘
```

1. **Phase 1 (RED):**
   - Viết Unit Test mô tả đúng hành vi mong đợi.
   - Chạy test và xác nhận test thất bại vì đúng nguyên nhân (hàm chưa tồn tại, logic chưa trả về kết quả đúng).
2. **Phase 2 (GREEN):**
   - Viết lượng mã nguồn tối thiểu cần thiết để test case chuyển sang màu xanh (PASS).
   - Không vội tối ưu hay viết thừa logic ở bước này.
3. **Phase 3 (REFACTOR):**
   - Dọn dẹp code, khử mã trùng (DRY), tối ưu hiệu năng, đổi tên biến rõ nghĩa.
   - Chạy lại test suite để đảm bảo mã nguồn vẫn PASS 100%.
