# T05 — scope và kiểm tra workbook kỹ thuật

- **Baseline:** `52dc842c4e361ce729f8dd742fca5236eebe535a`, tiếp tục trên `codex/m1-privacy-audit-followup`; thay đổi U14/T12 từ lượt trước và file untracked của người dùng được bảo vệ.
- **Ủy quyền:** người dùng yêu cầu tiếp tục các việc hợp lý nhất; phạm vi slice này là lỗi scope và tính đúng kỹ thuật của export T05 hiện có.
- **CONFIRMED:** Export/Queue/StayGuest chỉ so hotel khi scopeType=PROPERTY và scopeEntityId khác null; trường hợp khác có thể tiếp tục. Search còn ép Integer sang Short, có thể wrap ID ngoài miền hotel.
- **CONFIRMED:** cutOffHour chưa có giới hạn DTO; export dùng uppercase theo locale JVM và format thời gian theo offset nhận từ DB, trong khi cutoff dùng UTC+7.
- **TARGET:** nhân viên ngoài admin phải resolve được PROPERTY scope hợp lệ (1..32767); mọi request scope sai bị từ chối trước business query/write. cutOffHour chỉ 0..23; null giữ default 23. Workbook giữ 13 cột hiện có, chữ hoa ổn định theo Locale.ROOT, hai cột giờ cùng UTC+7 như cutoff.
- **Giới hạn:** kiểm chứng kỹ thuật của định dạng hiện có; chưa xác nhận template BCA/CT14 chính thức. Không sửa schema, API route/envelope, Pricing/Booking, validator hoặc gold data.

## File allowlist

- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/impl/LodgingExportServiceImpl.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/impl/LodgingQueueServiceImpl.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/impl/StayGuestServiceImpl.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/dto/request/LodgingExportRequest.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/controller/LodgingExportController.java` (mô tả bỏ khẳng định chuẩn chưa được xác minh).
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/repository/LodgingQueueRepository.java` (mốc chốt dùng cận trên exclusive để bao phủ phần giây lẻ cuối giờ).
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/operation/service/LodgingScopeIsolationTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/operation/service/LodgingExportServiceTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/operation/controller/LodgingExportControllerTest.java`
- `frontend/e2e/helpers/xlsx.helper.ts`, `frontend/e2e/api/operations-t04-t05.spec.ts`
- `docs/member1/acceptance/README.md`, báo cáo này.

## Kiểm chứng và rollback

Reproduce bằng regression test trước khi sửa; sau đó chạy Lodging/StayGuest suites, API thật trên sandbox (scope, validation, workbook cells, queue status và export lặp). Parser XLSX test dùng Python standard library qua stdin, không tạo file định danh tạm hay thêm dependency production. Có snapshot trước khi thay fixture; restore và dừng sandbox sau kiểm tra. Hoàn tác từng thay đổi sau khi commit được cho phép; không reset/clean/stash hay chạm DB dùng chung.

Cận giờ chốt giữ nghĩa chốt hết giờ được chọn theo UTC+7: ví dụ cutOffHour=23 bao phủ cả 23:59:59.500, nhưng không gồm 00:00 ngày kế tiếp. Service truyền mốc đầu giờ kế tiếp và repository dùng phép so sánh nhỏ hơn mốc này; API test xác nhận hai phía biên trên PostgreSQL thật.

## Kết quả — 2026-10-25 19:44 +07:00

Kiểm chứng áp dụng cho HEAD/base `52dc842` cộng working diff chưa commit trên branch đã nêu; toàn bộ dữ liệu là synthetic.

| Kiểm tra | Lệnh/phương pháp | Kết quả |
|---|---|---|
| Reproduce trước sửa | `.\mvnw.cmd '-Dtest=LodgingScopeIsolationTest' test` | FAIL đúng lỗi: 16 case, 10 failure; scope sai đi tới query hoặc trả lỗi dữ liệu thay vì UNAUTHORIZED. Log `t05-scope-before.log` giữ nguyên. |
| Regression mục tiêu sau sửa | `.\mvnw.cmd '-Dtest=Lodging*,StayGuestServiceTest' test` | PASS, exit 0: 56/56, gồm 20 scope isolation case, 8 export service, 5 controller validation, 4 converter, 4 queue, 15 stay guest. |
| Toàn backend | `.\mvnw.cmd clean test` với T12_REDIS_IT=true/127.0.0.1:16379/exclusive sandbox Redis | PASS, exit 0: 938 test, 0 failure/error, 1 skip contextLoads có sẵn. Redis real 4/4 thực chạy. |
| API với backend thật | `npm run test:api` | PASS, exit 0: 15/15. |
| Browser | `npm run test:e2e` | PASS, exit 0: 6/6, gồm login/T12/session/rate limit/no-write. |
| Lint | `npm run lint -- --max-warnings=0` | PASS, exit 0: 0 lỗi/cảnh báo. |
| T12 regression | `node tools/t12/verify_lazy_route_reaudit.cjs`; `python tools/t12/validate_r1_contracts.py`; `python tools/t12/test_portability_validation.py` | PASS, exit 0: AST 39 route/22 lazy component; 184 instance/74 case/23 protected hash/34 source hash; portability 11/11. Gold engine evaluation vẫn NOT_RUN. |

Một lượt targeted sau sửa gặp lỗi setup test do SecurityContext mock từ suite trước còn tồn tại. Đã tạo context sạch cho test mới và dọn context sau export tests, giữ strict Mockito; lượt cuối 56/56 và full suite 938 đều PASS. Log đầu `t05-targeted-after.log` và lượt cuối `t05-targeted-final.log` được giữ riêng.

## Bằng chứng API thực thi

- Fault injection chỉ trong DB acceptance: giữ JWT manager_h1 hợp lệ nhưng đổi staff sang CHAIN/null, REGION/1 hoặc PROPERTY/65537. Queue filter, guest filter, queue retry và export đều 403; status queue giữ nguyên. Staff được khôi phục trong finally.
- DB đã có constraint chặn PROPERTY/null. Case này vẫn được kiểm tra phòng thủ ở unit level; fault injection DB sử dụng các scope mà constraint hiện tại cho phép, không hạ hoặc xóa constraint.
- Giờ chốt -1/24 trả HTTP 400 code 1002; chọn hotel khác trả 403 và không thay status.
- Đọc workbook tải từ API bằng Python zipfile/XML standard library qua stdin: xác nhận sheet, 13 header, dòng fixture, ngày sinh/giới tính, số giấy tờ có zero đầu dạng text, giờ so độc lập với PostgreSQL AT TIME ZONE, không có formula cell.
- Sau tạo file, DB có EXPORTED, exported_at và batch_reference. Request export lặp trả 400; status và completion marker không đổi.
- Khi check-in nằm tại 23:59:59.500 UTC+7, export của ngày đó có dòng; check-in đúng 00:00 ngày tiếp theo bị loại, trả 400 code 9072 và queue vẫn PENDING. Timestamp/queue fixture được trả lại sau test.
- Các test thay đổi fixture dùng worker đơn của acceptance suite; không chạy đồng thời với browser suite.

## Môi trường và giới hạn

Snapshot được tạo trước khi seed/test. Liquibase báo DB up to date, không có changeset mới. Sau test đã dừng các app được quản lý, restore snapshot thành công (2 hotels, 5 room types, 305 availability, 5 staff), rồi dừng container; volume giữ lại. Không gọi Gemini hoặc dùng dữ liệu thật.

Log nằm trong `tools/acceptance/.runtime/`: các file `t05-*.log`, không đưa vào Git. Production frontend/dependencies không đổi ở slice này; build/audit không chạy lại, kết quả lần trước ở báo cáo privacy follow-up. `Test.ps1` không chạy để tránh ghi đè report lịch sử; các suite thành phần chạy trực tiếp.

**NOT RUN / chưa nghiệm thu:** đối chiếu template/cổng BCA chính thức; xác nhận đích nhận đã nhận/duyệt file; stress hai export đồng thời; fault I/O giữa chừng. Test workbook hiện tại xác nhận nội dung kỹ thuật, không xác nhận nghĩa pháp lý hoặc compatibility với template chưa cung cấp. Hợp đồng consent/history thật của U14 tiếp tục ở trạng thái PROPOSED; HTTP U14 vẫn chỉ synthetic/admin như slice trước.

**Git:** branch `codex/m1-privacy-audit-followup`, HEAD/base `52dc842`; chưa commit/push/PR. Không sửa schema, dependencies, gold data hoặc các file untracked có từ trước.
