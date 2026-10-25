# Khắc phục dependency frontend — 2026-10-25

**CONFIRMED:** Đã xử lý năm package bị `npm audit` đánh dấu mức high. Sau khi cài lại từ lockfile, audit đầy đủ trả exit 0 và không còn vulnerability ở bất kỳ mức nào. Lint, build và các luồng đăng nhập/API/T12 được kiểm tra thành công.

- Branch: `codex/fix-frontend-audit`.
- Baseline: `develop@25ac13fc4db8087984a584e572c5ee639e03e9e1`.
- Worktree: `C:/Users/admin/.codex/worktrees/m1-acceptance-stabilization/project-1-hotel-booking`.
- Thời gian hoàn tất kiểm tra và dừng sandbox: `2026-10-25 17:15 +07:00`.
- Kiểm tra được thực hiện trên worktree trước delivery commit. Các file untracked có sẵn trong repository chính được giữ nguyên.

## Phiên bản đã cập nhật

| Package | Trước | Sau | Đường phụ thuộc |
|---|---|---|---|
| `brace-expansion` | 5.0.9 | 5.0.12 | ESLint → minimatch |
| `follow-redirects` | 1.16.0 | 1.16.1 | Axios |
| `source-map-js` | 1.2.1 | 1.2.2 | PostCSS và Tailwind |
| `vite` | 8.2.1 | 8.2.4 | Dev server và production build |
| `zod` | 3.25.76 | 4.6.7 | Form đăng nhập, resolver và ESLint React hooks |

Manifest đặt `vite` thành `^8.2.4` và `zod` thành `^4.6.7`; lockfile ghi đúng các phiên bản trong bảng. Không thêm override, không chạy `npm audit fix --force`. Năm package trên là toàn bộ package node thay đổi trong lockfile.

Source ứng dụng chỉ dùng Zod trong `LoginPage.tsx`: object gồm hai string và kiểm tra `.min(1, message)`. `@hookform/resolvers@5.9.1`, `eslint-plugin-react-hooks@7.1.1` và `zod-validation-error@4.0.2` đều khai báo hỗ trợ Zod 4 theo metadata npm. Vì vậy không cần sửa logic ứng dụng khi nâng major version; kiểm tra runtime bên dưới xác nhận hành vi thực tế.

Nguồn đối chiếu: [Zod migration guide](https://zod.dev/v4/changelog), [Zod advisory](https://github.com/advisories/GHSA-mrhx-c7xc-m3w5), [Vite/launch-editor advisory](https://github.com/advisories/GHSA-587h-7wmj-h5pm), [brace-expansion advisory](https://github.com/advisories/GHSA-qhr7-859c-m2p7), [follow-redirects advisory](https://github.com/advisories/GHSA-q3jh-wrc6-jgq8), [source-map-js advisory](https://github.com/advisories/GHSA-68fv-2mgg-jv7q). Các advisory còn lại trong audit trước vá cũng được loại bỏ bởi các phiên bản đã khóa.

## Kiểm tra thực thi

Các lệnh npm bên dưới chạy tại `frontend/`. Toàn bộ lệnh trong bảng đạt exit 0.

| Lệnh hoặc kiểm tra | Kết quả |
|---|---|
| `npm ci` | PASS; cài sạch từ lockfile, 0 vulnerability |
| `npm audit --json` | PASS; info/low/moderate/high/critical đều 0; trước vá có 5 high |
| `npm run lint -- --max-warnings=0` | PASS; 0 lỗi, 0 cảnh báo |
| `npm run build` | PASS; TypeScript và Vite 8.2.4 build thành công |
| `npm exec playwright test e2e/browser/auth-login.spec.ts` | PASS, 2/2 |
| `npm run test:api` | PASS, 11/11 |
| `npm run test:e2e -- --grep-invert "Login form dependency compatibility"` | PASS, 4/4 test T12 hiện có |
| PowerShell parser cho `tools/acceptance/Test.ps1` | PASS |
| `git diff --check` | PASS |

Hai test đăng nhập mới xác nhận:

1. Form trống hiển thị đúng hai lỗi bắt buộc và không gọi API; sau khi điền username, chỉ lỗi password còn hiển thị.
2. Tài khoản `manager_h1` giả lập đăng nhập qua backend/Keycloak thật; sau reload, request dashboard có xác thực nhận HTTP 200 và access/refresh token vẫn tồn tại. Test không ghi token ra output.

Mười một test API kiểm tra xác thực và RBAC, OCR text, T04/T05, U14 và scoped RAG. Bốn test browser T12 kiểm tra hội thoại, điều hướng chi tiết, session độc lập, rate limit/cooldown và request advisor không đổi digest dữ liệu DB. Tổng số test browser được chạy là 6, chia thành hai lệnh để không chạy lặp test đăng nhập.

`tools/acceptance/Test.ps1` đã được cập nhật mô tả suite browser có kiểm tra login và bỏ số lượng cố định `4/4` trong thông báo PASS. Không thay đổi điều kiện PASS/FAIL hoặc assertion.

## Môi trường và giới hạn

- Backend thật tại `localhost:18080`, Vite dev server 8.2.4 tại `localhost:13000`; PostgreSQL/Redis/Keycloak thuộc Compose project `hotel-m1-acceptance`.
- Liquibase xác nhận 0 changeset mới cần chạy. Đã snapshot trước test, dừng ứng dụng, restore thành công và dừng container/network; giữ Docker volumes.
- Dữ liệu sau restore: 2 hotels, 5 room types, 305 availability rows, 5 staff. Dữ liệu kiểm tra hoàn toàn giả lập; Gemini thật không được gọi.
- Build còn cảnh báo bundle JavaScript lớn hơn 500 KB: 878.71 KB minified, 224.78 KB gzip. Đây là tồn đọng tối ưu kích thước bundle.
- Maven, contract validator và portability: NOT RUN trong đợt này vì không đổi backend, contract hoặc gold data. Kết quả backend tại baseline đã được ghi trong [hồ sơ xác minh merge](develop-merge-verification-2026-10-25.md).
- Audit trước/sau và log kiểm tra được lưu local trong `tools/acceptance/.runtime/`, không đưa vào Git. Các hồ sơ nghiệm thu trước đó được giữ nguyên.

Phạm vi file thay đổi: `frontend/package.json`, `frontend/package-lock.json`, `frontend/e2e/browser/auth-login.spec.ts`, `tools/acceptance/Test.ps1`, README nghiệm thu và hồ sơ này.
