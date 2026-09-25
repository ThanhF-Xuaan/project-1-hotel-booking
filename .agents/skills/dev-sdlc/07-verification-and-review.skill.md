---
name: verification-and-review
description: "Quy trình kiểm thử, hồi quy, rà soát mã nguồn (Code Review) và dọn dẹp trước khi hoàn tất hoặc merge nhánh."
---

# 🛡️ Verification & Code Review: Nghiệm Thu Mã Nguồn

## 📋 Checklist Nghiệm Thu Trước Khi Kết Thúc (Pre-completion Gate)

Trước khi thông báo hoàn thành nhiệm vụ hoặc tạo Pull Request, Agent phải tự rà soát theo danh sách:

- [ ] **1. Compilation & Build Gate:** Chạy lệnh build dự án (ví dụ `npm run build` hoặc `./mvnw clean compile`) ➔ 0 Error, 0 Warning nghiêm trọng.
- [ ] **2. Automated Test Suite:** Toàn bộ Unit Test và Integration Test phải PASS 100%.
- [ ] **3. Linting & Formatting:** Mã nguồn tuân thủ ESLint / Checkstyle / Prettier.
- [ ] **4. Placeholder Cleanliness:** Tìm kiếm toàn bộ repo đảm bảo không còn sót `TODO`, `FIXME`, `debugger`, `console.log`.
- [ ] **5. Git Diff Review:** Rà soát lại `git status` và `git diff`:
  - Không commit các file rác (`.DS_Store`, `Thumbs.db`, `.tmp`, file log).
  - Viết commit message theo chuẩn Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`).

---

## 📝 Mẫu Mô Tả Pull Request (PR Template)

```markdown
### 🎯 Mục Tiêu Thay Đổi
- [Mô tả 1-2 câu về tính năng hoặc lỗi đã sửa]

### 🔍 Danh Sách File Thay Đổi Chính
- `src/...`: [Giải thích ngắn gọn]

### 🧪 Kịch Bản Đã Kiểm Thử (Verification Proof)
- [x] Unit Test đã chạy: 15 tests passed
- [x] Build Production: Thành công
- [x] Manual Test: Đã kiểm tra UI và API response đúng chuẩn
```
