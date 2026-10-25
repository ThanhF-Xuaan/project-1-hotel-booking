# Tách bundle frontend theo route — 2026-10-25

**CONFIRMED:** `npm run build` không còn cảnh báo chunk lớn hơn 500 KB. Chunk JavaScript lớn nhất là 246,800 bytes; kiểm tra thư mục output xác nhận mọi chunk đều dưới 500,000 bytes.

- Branch: `codex/fix-frontend-audit`, baseline `25ac13fc4db8087984a584e572c5ee639e03e9e1`.
- Thay đổi tiếp nối đợt [vá dependency](dependency-audit-remediation-2026-10-25.md) đã được kiểm tra trên cùng worktree.
- Thời gian hoàn tất kiểm tra và dọn sandbox: `2026-10-25 17:39 +07:00`.
- Source thay đổi trong đợt này: `frontend/src/App.tsx` và test `frontend/e2e/browser/auth-login.spec.ts`. Hồ sơ nghiệm thu được bổ sung, các report cũ được giữ nguyên.

## Thay đổi

`App.tsx` trước đây import đồng bộ toàn bộ trang khách và trang quản trị, khiến mã các module cùng đi vào một bundle khoảng 879 KB. Hiện 21 page component và layout quản trị được khai báo bằng `React.lazy` ở module scope; mã trang được tải khi route thực sự render. `Suspense` hiển thị trạng thái “Đang tải trang...” trong lúc chờ.

Giữ nguyên route, redirect, role guard và API. `ProtectedRoute` vẫn được import đồng bộ và bao ngoài layout/trang quản trị. Các module chỉ có named export được ánh xạ sang `default` trong promise của `lazy`.

Không đổi `chunkSizeWarningLimit`, không thêm cấu hình chia vendor thủ công và không thay dependency trong đợt tách bundle. Nguồn đối chiếu: [React lazy](https://react.dev/reference/react/lazy), [Vite async chunk loading](https://vite.dev/guide/features#async-chunk-loading-optimization).

## Kích thước build

| Artifact | Minified | Gzip |
|---|---:|---:|
| Entry trước khi tách | 878.71 KB | 224.78 KB |
| Entry sau khi tách, cũng là chunk lớn nhất | 246.80 KB | 79.68 KB |
| LoginPage sau khi tách | 124.64 KB | 38.16 KB |
| BookingListPage sau khi tách | 77.75 KB | 17.34 KB |

Output có 94 file JavaScript do Vite tách cả page và phần dùng chung. Các số trên mô tả từng chunk; không phải tổng dung lượng JavaScript của toàn ứng dụng. Build dùng Vite 8.2.4 và các URL public của sandbox, không chứa secret.

## Kiểm tra thực thi

| Kiểm tra | Kết quả |
|---|---|
| `npm run lint -- --max-warnings=0` | PASS, exit 0; không lỗi/cảnh báo lint |
| `npm run build` | PASS, exit 0; không cảnh báo kích thước chunk |
| Kiểm tra mọi file `dist/assets/*.js` | PASS; lớn nhất 246,800 bytes, tất cả dưới 500,000 bytes |
| `npm run test:e2e` với origin sandbox `localhost:13000` | PASS, 6/6; backend/DB/Keycloak local thật |
| `npm exec -- playwright test e2e/browser/auth-login.spec.ts --grep "shows required-field errors"` với preview `localhost:13001` | PASS, 1/1; kiểm tra form từ artifact production |
| `git diff --check` | PASS |
| Restore snapshot và dừng sandbox | PASS; ứng dụng/container đã dừng, giữ Docker volumes |

Test đăng nhập được mở rộng để xác nhận không có request script `BookingListPage` trước khi điều hướng đến danh sách đặt phòng. Sau khi click link, script mới được yêu cầu và heading của trang xuất hiện; quay lại dashboard vẫn hoạt động. Test cũng kiểm tra đăng nhập thật, reload phiên và request dashboard nhận HTTP 200.

Bốn test T12 còn lại xác nhận tìm phòng qua hội thoại, điều hướng chi tiết, session độc lập, rate limit/cooldown và request advisor không thay đổi digest dữ liệu DB. Test form production xác nhận thông báo lỗi bắt buộc và không gửi request khi dữ liệu chưa hợp lệ.

## Phạm vi môi trường và các lượt chạy

- Production preview phục vụ `dist/` tại port 13001; HTML được kiểm tra có đường dẫn `/assets/index-...` và không dùng `/src/main`. Preview chỉ được dùng cho test form không gọi API. Sáu E2E có API chạy trên origin sandbox 13000 với backend 18080, đúng cấu hình CORS hiện tại.
- Lượt preview đầu dùng `npm exec` thiếu dấu `--` phân cách, nên npm giữ lại cờ `--grep` và chạy thừa test đăng nhập thật. Kết quả lượt đó: 1 PASS, 1 timeout. Origin 13001 không được CORS cho phép gọi login; preflight xác nhận HTTP 403. Đã sửa lệnh chọn test và chạy lại đúng phạm vi đạt 1/1; không thay CORS hoặc assertion để bỏ qua lỗi.
- Backend giữ nguyên, Liquibase không có changeset mới. Đã snapshot trước test, restore dữ liệu 2 hotels, 5 room types, 305 availability rows, 5 staff và dừng cả preview lẫn sandbox.
- Maven, API-only suite, contract validator và audit không chạy lặp trong đợt này vì không đổi backend, DTO, gold data hoặc dependency. Kết quả audit 0 vulnerability và API 11/11 của đợt vá dependency vẫn được giữ trong report tương ứng.
- Log build/lint/E2E và harness preview nằm trong `tools/acceptance/.runtime/`, bị ignore khỏi Git. Dữ liệu/tài khoản kiểm thử là giả lập; không gọi Gemini thật.
