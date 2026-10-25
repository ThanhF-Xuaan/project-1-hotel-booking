# T05 — export đồng thời và rollback

- **Baseline/branch:** `52dc842c4e361ce729f8dd742fca5236eebe535a` / `codex/m1-privacy-audit-followup`, giữ nguyên thay đổi các slice trước và untracked của người dùng.
- **Ủy quyền:** người dùng đồng ý bước tiếp theo đã đề xuất: kiểm tra đồng thời/I/O/DB fault, sửa lỗi tái hiện, regression, review nhánh và commit/push để người dùng tự tạo PR.
- **CONFIRMED:** query pending hiện chưa có row lock. Transaction bao quanh chọn queue, tạo file và ghi EXPORTED, nhưng hai transaction có thể cùng đọc PENDING trước khi ghi.
- **CONFIRMED:** POI cài tại repo là 5.3.0; kiểm tra bytecode SXSSFWorkbook.close cho thấy nó gọi dispose. Không coi cleanup hiện có là bug; cần test việc xóa file spill thật khi output lỗi.

## Tiêu chí và thiết kế

1. Hai export cùng pending row: một tạo workbook/batch, request còn lại nhận LODGING_EXPORT_EMPTY sau khi transaction thắng commit. Kiểm tra thứ hai thật sự đợi PostgreSQL row lock; không dựa vào sleep hay hai request tình cờ nối tiếp.
2. IOException khi tạo workbook: transaction không ghi EXPORTED/metadata; retry sau lỗi thành công.
3. Lỗi DB sau saveAll và flush thật: dữ liệu vẫn rollback về PENDING, metadata rỗng; retry thành công.
4. Writer thật với hơn 100 dòng spill ra thư mục test riêng phải dọn file tạm cả success và IOException. OutputStream là seam I/O bình thường, fault chỉ nằm trong test.
5. Giữ nguyên route, DTO, 13 cột, timezone và lỗi export-empty. Pessimistic row lock cùng transaction READ_COMMITTED dự kiến bảo vệ pending rows. Không thay schema/dependency hoặc thêm fault flag vào API.

Tách writer thuần tạo XLSX khỏi service điều phối transaction để kiểm tra lỗi I/O độc lập. Guarantee này dành cho hai export cùng queue đang PENDING; manual retry đang cho phép mở lại một queue theo hành vi hiện có, không được coi là deduplication key vĩnh viễn.

## Allowlist slice

- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/service/impl/LodgingExportServiceImpl.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/repository/LodgingQueueRepository.java`
- `backend/src/main/java/vn/edu/utc/hotel_booking/modules/operation/export/LodgingWorkbookWriter.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/operation/service/LodgingExportServiceTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/operation/service/LodgingScopeIsolationTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/operation/service/LodgingExportPostgresTest.java`
- `backend/src/test/java/vn/edu/utc/hotel_booking/modules/operation/export/LodgingWorkbookWriterTest.java`
- `frontend/e2e/api/operations-t04-t05.spec.ts`
- `docs/member1/acceptance/README.md`, báo cáo này.

## Môi trường và stop condition

Integration class chỉ bật bằng T05_EXPORT_IT=true và chứng nhận exclusive riêng; chỉ nhận localhost:15432/hotel_acceptance_db. Kiểm tra fixture synthetic trước khi sửa queue 1, lưu trạng thái và restore sau từng test. Hạ tầng tạo snapshot trước chạy, restore và dừng khi xong. Fault injection chỉ trong test context với repository/writer wrapper; không đổi constraint hay endpoint production. Template BCA, consent/history thật và kiểm tra production nằm ngoài slice.

Review diff theo allowlist; commit/push chỉ chứa file do các slice này tạo, giữ mọi untracked người dùng. Rollback qua revert commit và snapshot sandbox đã xác minh, không reset/clean/stash.

## Kết quả thực thi — 2026-10-25 20:58 +07:00

- **Initial base:** `52dc842`; **delivery parent:** `42690d22717c67fae4eb36b7c499e30abe678361` sau fast-forward từ origin/develop. Commit upstream chỉ đổi `document/word/srs.docx`; không đổi source/config/test và không thuộc diff tính năng này. Kiểm tra cuối áp dụng cho parent 42690d2 cộng working diff được bàn giao.
- **Reproduce:** trước row lock, ba PostgreSQL integration test chạy thật; hai ca rollback PASS, ca concurrent FAIL vì số export thành công là 2 thay vì 1. Test chủ động giữ transaction đầu sau query, bảo đảm request thứ hai vào cùng cửa sổ; không suy luận race từ hai request chạy nối tiếp.
- **Fix:** `@Lock(PESSIMISTIC_WRITE)` cho pending query, thứ tự ổn định theo check-in và queue ID; service dùng `READ_COMMITTED`. Writer được tách thành component chỉ tạo file và nhận OutputStream, không điều khiển queue hoặc transaction.
- **After fix:** test quan sát `pg_stat_activity` thấy request thứ hai chờ lock thật, kết quả một workbook và một LODGING_EXPORT_EMPTY. HTTP E2E cũng xác nhận một 200, một 400/code 9072.
- **I/O fault:** writer test thật với 201 dòng và output lỗi sau 1024 byte quan sát file tạm đang tồn tại rồi xác nhận không còn file sau exception. Luồng thành công cũng dọn sạch file. Đây là kiểm chứng cơ chế close/dispose POI 5.3 đã có, không phải tuyên bố phát hiện leak mới.
- **DB fault:** test gọi repository thật saveAll/flush rồi cố ghi trạng thái không hợp lệ trong cùng transaction. PostgreSQL trả CHECK violation SQLSTATE 23514; queue đọc từ connection mới vẫn PENDING và metadata rỗng. Tắt fault test rồi retry tạo được workbook và metadata. Constraint không bị thay hoặc hạ.

| Kiểm tra | Lệnh/phiên bản | Kết quả cuối |
|---|---|---|
| Unit writer + service + scope | `mvnw.cmd '-Dtest=LodgingWorkbookWriterTest,LodgingExportServiceTest,LodgingScopeIsolationTest' test` | PASS 30/30 sau tách writer. |
| Targeted T05 | `mvnw.cmd '-Dtest=Lodging*,StayGuestServiceTest' test`, bật PostgreSQL IT | PASS 61/61 sau row lock; trước khi nâng DB fault từ exception giả lập sang CHECK thật. |
| Full backend sau nâng fault | Trong backend: `.\mvnw.cmd clean test`, T05_EXPORT_IT/T12_REDIS_IT=true | PASS exit 0; 943 test, 0 failure/error, 1 skip contextLoads có sẵn. PostgreSQL 3/3, Redis 4/4 thực chạy. |
| API backend thật | `npm run test:api` | PASS exit 0; 16/16, gồm scope, cutoff, workbook, đồng thời và U14 synthetic gate. |
| Browser backend thật | `npm run test:e2e` | PASS exit 0; 6/6. |
| Frontend lint/build/audit | `npm run lint -- --max-warnings=0`; `npm run build`; `npm audit --audit-level=high` | PASS exit 0; 0 lint errors/warnings, Vite 8.2.4 entry 246.80 kB, không cảnh báo >500 kB, 0 npm vulnerability. |
| T12 source/artifact/portability | `node tools/t12/verify_lazy_route_reaudit.cjs`; hai Python validator hiện có | PASS exit 0; 39 route/22 lazy component; 184 instance/74 case/23 protected hash/34 source hash; portability 11/11. Gold engine evaluation vẫn NOT_RUN. |

## Tái hiện PostgreSQL integration

1. Dùng Compose project `hotel-m1-acceptance` và runtime acceptance hiện có để bật hạ tầng, giữ backend/frontend tắt. Xác minh container PostgreSQL có label đúng project, bind `127.0.0.1:15432`, DB `hotel_acceptance_db`; tạo snapshot trước khi chạy. Fixture queue 1/hotel 1 phải đúng dữ liệu giả lập. Class test từ chối các host/port/database/user khác, kiểm tra tên hotel đầy đủ và số queue trước khi ghi.
2. Trong process Maven riêng, không gọi Initialize-AcceptanceRuntime: đọc env.json vào biến cấu hình nhưng không in giá trị secret. Bật `T05_EXPORT_IT=true`, `T05_EXPORT_IT_EXCLUSIVE_VERIFIED=true`; truyền `T05_EXPORT_IT_DB_HOST=127.0.0.1`, `..._PORT=15432`, `..._NAME=hotel_acceptance_db`, `..._USER=postgres`, `..._PASSWORD` từ runtime. Chỉ chứng nhận exclusive sau preflight ở bước 1. Cùng lúc chạy full suite có thể bật Redis IT bằng nhóm biến T12_REDIS_* sau khi xác minh Redis sandbox.
3. Chạy `.\mvnw.cmd '-Dtest=LodgingExportPostgresTest,LodgingWorkbookWriterTest' test` trong backend, hoặc `clean test` để regression toàn repo. T05 flag không có thì 3 PostgreSQL test được skip có chủ đích; không coi skip là bằng chứng integration PASS.
4. Test tự khôi phục trạng thái logical/metadata queue sau từng case. Sau cả đợt, restore snapshot rồi dừng container và app; không dùng kết quả test như trạng thái nghiệp vụ thật.

## Đối soát lần chạy chưa đạt

- Guard fixture ban đầu yêu cầu thiếu phần `(Hà Nội)` của tên hotel. Nó chặn test trước khi sửa queue; expected literal đã được sửa theo fixture SQL, giữ điều kiện so khớp chính xác. Log `t05-postgres-fixture-guard.log` giữ riêng.
- Log `t05-postgres-before-lock.log` giữ ca reproduce thật 2 winner.
- Sau khi nâng DB fault thành CHECK thật, một full run bị nhiễm `DB_PORT=15432` từ helper Compose gọi trong cùng process, làm DockerNetworkConfigTest sai expected dev default 5433. Chạy lại process riêng, bỏ generic DB/Redis env và chỉ truyền biến IT chuyên biệt; full suite PASS. Không sửa assertion hoặc cấu hình dev để vượt qua lỗi môi trường.
- Các log cũ không bị dùng làm bằng chứng PASS của code cuối. `m1-final-maven.log` và Surefire XML là kết quả cuối với CHECK thật; `m1-final-maven-compose-env.log` giữ lượt sai môi trường. Runtime logs bị ignore.

## Bàn giao và giới hạn

Snapshot đã restore thành công sau mọi test; DB xác nhận 2 hotel, 5 room type, 305 availability, 5 staff. App/container đã dừng, volume giữ lại. Không gọi Gemini, không thêm migration/dependency hoặc dùng dữ liệu thật.

Các ca này kiểm chứng hai transaction tranh một fixture queue và cleanup writer 201 dòng; không phải benchmark dữ liệu production. Row locks giữ tới commit và có thể làm request khác chờ trong thời gian tạo file. Manual retry và việc client/hệ thống đích nhận file vẫn theo semantics cũ. Không khẳng định exactly-once delivery tới cổng ngoài.

Template export chính thức, nguồn consent/history thật của U14 và gold engine evaluation chưa được phê duyệt/chạy vẫn giữ trạng thái cũ. Không sửa schema/gold/threshold để có kết quả xanh. Các report slice trước là lịch sử tại thời điểm ghi; thông báo mới nhất trong README chỉ rõ trạng thái hiện tại.

Nhánh được chuẩn bị để commit/push theo ủy quyền của người dùng; identity commit lấy từ Git khi bàn giao. Các file untracked không thuộc allowlist người dùng được giữ nguyên, không đưa vào commit.
