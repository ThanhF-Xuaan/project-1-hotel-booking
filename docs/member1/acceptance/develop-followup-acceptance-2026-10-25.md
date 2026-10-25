# Đối soát nghiệm thu tiếp theo trên `develop`

- **Thời điểm:** 2026-10-25 17:54 +07:00
- **Repository:** `D:\Project\University\project-1-hotel-booking`
- **Branch/SHA:** `develop` / `52dc842c4e361ce729f8dd742fca5236eebe535a`
- **Môi trường:** local sandbox, PostgreSQL/Redis/Keycloak acceptance, tài khoản và dữ liệu giả lập.
- **Kết luận tổng:** **PARTIAL**. Test thực thi ứng dụng đạt; validator T12 phát hiện evidence source đã cũ; chưa có căn cứ để xác nhận schema export T05 hoặc quyền truy cập consent/history U14 thật.

## Kết quả chạy

| Hạng mục | Lệnh/quy trình | Kết quả | Ghi chú |
|---|---|---|---|
| Maven backend toàn repo | Trong `backend/`: `.\mvnw.cmd clean test`; bật `T12_REDIS_IT=true` và trỏ tới Redis disposable riêng | **PASS** | 899 test, 0 failure/error, 1 skip. `HotelBookingApplicationTests.contextLoads` bị skip do điều kiện hạ tầng của test. |
| Redis session integration | `RedisRoomAdvisorSessionStoreRealRedisTest` trong lệnh Maven trên | **PASS** | 4/4 test với Redis 7 thật, port `127.0.0.1:16379`, project và volume mới chỉ dành cho test. Sau suite `DBSIZE=0`; project/volume tạm đã xóa. |
| T12 contract validator | `python tools/t12/validate_r1_contracts.py` | **FAIL — evidence stale** | Báo `Source changed after audit: frontend/src/App.tsx`. Record mong đợi `542e550f…`; working copy hiện tại `CD4E7B5E…`. Soát toàn bộ danh sách `baseline-source-evidence.json` thấy đây là đường dẫn duy nhất không khớp. Không sửa hash/validator/gold data để ép PASS. |
| Portability validator | `python tools/t12/test_portability_validation.py` | **FAIL — 10/11** | 10 test helper PASS; test còn lại chạy full contract validator và lỗi cùng hash `App.tsx` stale. |
| Dependency audit | `npm ci`; `npm audit --audit-level=high` | **PASS** | Cài đúng lockfile: Vite 8.2.4, Zod 4.6.7; 0 lỗ hổng npm. |
| Frontend lint | `npm run lint -- --max-warnings=0` | **PASS** | 0 lỗi, 0 cảnh báo. |
| Frontend build | `npm run build` | **PASS** | Vite 8.2.4; entry chunk lớn nhất 246.80 kB; không còn cảnh báo chunk >500 kB. |
| Playwright API | `npm run test:api` | **PASS** | 11/11, gồm OCR text giả lập, scope T04, export/payroll T05, U14 consent flag và RAG local. |
| Browser E2E | `npm run test:e2e` | **PASS** | 6/6, gồm login, T12 chat/session/rate limit/no-write và điều hướng. |
| Liquibase và fixture | `tools/acceptance/Start.ps1` | **PASS** | DB up to date, 0 changeset chạy; fixture xác nhận 2 khách sạn, 5 loại phòng, 305 availability, 5 nhân viên. |
| Khôi phục/dọn sandbox | `Restore.ps1`, sau đó `Stop.ps1` | **PASS** | Snapshot khôi phục thành công, các số lượng trên được xác nhận; app/container dừng, Docker volume acceptance được giữ. |

## Giới hạn nghiệm thu nghiệp vụ

- **T04 — PASS cho luồng đã chạy:** API xác nhận đọc chỉ số trong hotel scope và từ chối hotel khác; Maven toàn repo PASS. Không khẳng định mọi tình huống biên của quy trình đo đã được kiểm thử trong lần này.
- **T05 — PASS kỹ thuật, BLOCKED nghiệp vụ:** API xác nhận sinh file XLSX và import CSV giả lập/scope payroll. Test export chỉ kiểm tra MIME type và chữ ký ZIP `PK`; không đọc cột hay đối chiếu mẫu BCA chính thức. Chưa có mẫu hoặc schema được cung cấp để xác nhận tương thích nghiệp vụ.
- **T12 R1 — PASS cho các luồng thực thi; validator BLOCKED:** browser kiểm tra hội thoại, session độc lập, rate limit/cooldown và no-business-write; Maven chứa 4 Redis test thực chạy. Validator contract chưa thể PASS vì audit nguồn ghi nhận `App.tsx` trước thay đổi lazy route đã merge. Cần thực hiện re-audit có phiên bản mới và giải thích thay đổi, giữ nguyên artifact lịch sử.
- **RAG local — PASS trong dữ liệu demo:** API test luồng stage/activate/query và cô lập hotel; không dùng Gemini hoặc corpus thật.
- **OCR text — PASS trong dữ liệu giả lập:** API kiểm tra CCCD text, nội dung rỗng/không đọc được và filename path traversal. Không kiểm tra OCR ảnh/provider thật.
- **U14 — PASS thuật toán với dữ liệu giả lập, BLOCKED quyền riêng tư thật:** API gửi `consentGranted` và `stayHistory` ngay trong request. Chưa xác nhận nguồn consent server-side, xác thực guest sở hữu lịch sử, hay nguồn lịch sử lưu trú thật. Không dùng dữ liệu cá nhân thật.

## Bằng chứng và trạng thái môi trường

Các log local nằm trong `tools/acceptance/.runtime/`: `maven-develop-redis-full.log`, `develop-contract.log`, `develop-portability.log`, `develop-api.log`, `develop-browser.log`, và `develop-frontend-build.log`. Thư mục runtime bị ignore, không chứa secrets trong báo cáo được commit.

Không gọi Gemini. Redis dùng project disposable mới và đã xóa container, network, volume sau khi test. Sandbox ứng dụng khôi phục snapshot trước khi dừng; các Docker volume acceptance được giữ lại. Không chạy migration trên dữ liệu ngoài sandbox.

`git status` sau đối soát: `develop` khớp `origin/develop`; các file untracked có từ trước vẫn được giữ nguyên. Báo cáo này và cập nhật liên kết trong `README.md` là thay đổi tài liệu chưa commit; không có thay đổi mã nguồn sau merge.

## Việc tiếp theo

1. Re-audit T12 từ SHA `52dc842` thành evidence version mới; ghi rõ lazy route chỉ thay cách tải trang, xác nhận route/guard/contract giữ nguyên, và giữ nguyên bộ gold/threshold.
2. Chốt định dạng T05 bằng file mẫu/schema chính thức; sau đó kiểm tra nội dung workbook, scope, import/export và lỗi/partial semantics.
3. Chốt cách server xác minh consent và guest sở hữu stay history cho U14; bổ sung test từ chối guest khác và test nguồn history được phép trước khi dùng dữ liệu thật.
