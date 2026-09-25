---
name: writing-plans
description: "Sử dụng khi đã có Spec hoặc yêu cầu cho một tác vụ gồm nhiều bước, trước khi chạm vào mã nguồn. Lập kế hoạch chi tiết không chứa placeholder."
---

# 📝 Writing Plans: Lập Kế Hoạch Triển Khai Chi Tiết

## 🎯 Nguyên Tắc Lập Kế Hoạch
- Giả định kỹ sư tiếp nhận có kiến thức lập trình nhưng chưa nắm bối cảnh dự án.
- **Bite-Sized Tasks:** Mỗi bước (step) là một hành động nhỏ hoàn thành trong 2 - 5 phút:
  1. Viết test case thất bại (Failing Test)
  2. Chạy test để xác nhận test thất bại
  3. Viết code tối giản để test vượt qua
  4. Chạy lại test để xác nhận pass
  5. Commit mã nguồn
- **Zero Placeholder:** Tuyệt đối không viết `TODO`, `TBD`, `tự viết logic tương tự Task trước`. Mọi code block trong kế hoạch phải đầy đủ 100%.

---

## 📋 Cấu Trúc File Kế Hoạch Chuẩn

```markdown
# [Tên Tính Năng] Implementation Plan

> **Goal:** [Mô tả mục tiêu 1 câu]
> **Tech Stack:** [Các công nghệ sử dụng]

---

### Task 1: [Tên Thành Phần / Layer]
**Files:**
- Create: `exact/path/to/NewFile.ts`
- Modify: `exact/path/to/ExistingFile.ts:40-60`
- Test: `tests/exact/path/to/TestFile.spec.ts`

- [ ] **Step 1: Viết test case** (kèm code block đầy đủ)
- [ ] **Step 2: Chạy test xác nhận fail** (kèm lệnh chạy và expected fail output)
- [ ] **Step 3: Viết code thực thi** (kèm code block hoàn chỉnh)
- [ ] **Step 4: Chạy test xác nhận pass** (kèm lệnh chạy và expected pass output)
- [ ] **Step 5: Commit** (kèm lệnh git commit)
```
