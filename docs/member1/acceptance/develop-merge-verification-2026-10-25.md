# Xác minh tích hợp Member 1 vào develop — 2026-10-25

**CONFIRMED:** Nghiệm thu chức năng local đạt trên trạng thái merge đã ghép với hai commit mới của develop. Kết quả này không xác nhận sẵn sàng production; dependency audit còn 5 vấn đề mức high.

- Repository kiểm tra: `D:/Project/University/project-1-hotel-booking`.
- Nhánh đích: `develop`, trước merge là `84f72460227ccc578be3e2034c4e84533e65e2c6`.
- Commit nguồn: `c134386d7e8baa56a9797846b15f1bbcb1ea5683`.
- Ancestor chung: `a6b2088f55d2326a51e895ff2cfffbb4fe70831e`.
- Git tree đã xác minh trước khi thêm hồ sơ mới: `88802a72ab23e4c20746b92b458e91ca064fe286`.
- Thời gian kết thúc xác minh và dọn sandbox: `2026-10-25 17:03 +07:00`.
- Người dùng đã yêu cầu merge và push trực tiếp vào develop, bỏ qua PR; quyền quyết định toàn project đã được xác nhận trong phiên làm việc.

## Thay đổi cần thiết khi tích hợp

1. Hợp nhất cấu hình `hotel.scheduling`, `hotel.storage` và `hotel.ai` vào một root `hotel` trong `application.yaml`. Git tự ghép được nội dung nhưng ban đầu tạo hai root trùng tên.
2. Sửa `tools/acceptance/Test.ps1` để reset và đọc `$global:LASTEXITCODE` khi gọi Python. Biến script-local trước đó che mã thoát native và khiến các suite thành công bị báo FAIL. Kiểm tra scope với exit code 0 và 7 đều đạt; không thay đổi gold answers, ngưỡng hoặc assertion nghiệp vụ.

## Bằng chứng thực thi

| Kiểm tra | Lệnh hoặc thao tác | Kết quả |
|---|---|---|
| Maven toàn backend | `.\mvnw.cmd clean test` tại `backend/` | PASS, exit 0; 899 test, 0 failure, 0 error, 5 skip |
| Khởi động hạ tầng | Compose project `hotel-m1-acceptance` | PASS; PostgreSQL, Redis, Keycloak healthy |
| Backup trước migration | `tools/acceptance/Snapshot.ps1` | PASS; giữ thêm bản `pre-merge-migration-snapshot.sql` trong runtime bị ignore |
| Migration và ứng dụng thật | `tools/acceptance/Start.ps1` | PASS; 2 changeset mới chạy thành công; backend 18080 và frontend 13000 sẵn sàng |
| Sáu suite còn lại | `tools/acceptance/Test.ps1 -SkipMaven` | 6 PASS, 0 FAIL, 1 suite SKIPPED; runner trả PARTIAL và exit 1 theo thiết kế vì đã bỏ qua Maven chạy riêng |
| Contract T12 | `python tools/t12/validate_r1_contracts.py` qua runner | PASS; 184 instances, 74 cases, 23 protected hashes |
| Portability | `python tools/t12/test_portability_validation.py` qua runner | PASS, 11/11 |
| Frontend | `npm run lint -- --max-warnings=0`, `npm run build` | PASS; lint 0 lỗi/cảnh báo; build còn cảnh báo bundle lớn hơn 500 KB |
| Playwright API | `npm run test:api` | PASS, 11/11 |
| Playwright browser | `npm run test:e2e` | PASS, 4/4; có request T12 thật và digest dữ liệu trước/sau |
| Kiểm tra exit code runner | Native process trả 0 và 7 trong scope con | PASS; mã thoát và kết quả suite sau đó được đọc đúng |
| Restore và teardown | Dừng ứng dụng, `Restore.ps1`, `Stop.ps1` | PASS; 2 hotels, 5 room types, 305 availability rows, 5 staff; không còn container acceptance chạy; giữ volumes |

Hai changeset mới đã được kiểm tra trong `databasechangelog` sau restore; trigger `trg_prevent_audit_log_modification` tồn tại. Restore dùng snapshot sau migration và trước khi chạy test; bản backup trước migration vẫn được giữ trong runtime local.

Kết quả sáu suite và thời lượng được giữ nguyên trong [report runner](develop-merge-runner-2026-10-25.md). [Report nghiệm thu trước merge](acceptance-test-report.md) được giữ nguyên để bảo toàn lịch sử.

## Các lượt kiểm tra và giới hạn

- Lượt `mvnw test` bị ngắt khỏi giao diện trước đó thực tế đã kết thúc với exit 0: 899 test, 0 lỗi, 5 skip. Năm XML lỗi legacy còn tồn trong thư mục report gây nhầm lẫn lúc kiểm tra ban đầu. `clean test` đã xóa build cũ và xác nhận lại kết quả.
- Lượt runner đầu báo FAILED, exit 1 do lỗi scope `$LASTEXITCODE`, mặc dù output của sáu tiến trình đều thành công. Sau khi sửa, runner được chạy lại và báo đúng 6 PASS, 0 FAIL, PARTIAL do `-SkipMaven`. Log hai lượt được giữ local trong `tools/acceptance/.runtime/` và không đưa vào Git.
- Bốn test `RedisRoomAdvisorSessionStoreRealRedisTest` skip vì chưa bật `T12_REDIS_IT`. Một test `HotelBookingApplicationTests.contextLoads` skip theo điều kiện cần hạ tầng; khởi động ứng dụng thật và E2E đã được kiểm tra riêng.
- Gemini thật không được gọi; launcher xóa `GEMINI_API_KEY`. Dữ liệu OCR, payroll, lịch sử U14 và RAG đều giả lập. T05 vẫn là nghiệm thu kỹ thuật; consent/history U14 thật và mẫu export chính thức cần xác nhận riêng.
- `npm ci` thành công. `npm audit` báo 5 vấn đề high: `brace-expansion`, `follow-redirects`, `source-map-js`, `vite`, `zod`. Chưa nâng dependency hoặc chạy `npm audit fix`; xử lý chúng cần một đợt thay đổi và kiểm tra riêng.
- File runtime, snapshot, log và các file untracked có sẵn của người dùng không được thêm vào commit.
