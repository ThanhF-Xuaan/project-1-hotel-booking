# Member 1 — bằng chứng sửa review 03/11/2026

Base `c0a375ee59be516dd589842ba9d0f79ae6c7ea05`; branch `codex/m1-review-fixes-20261103`. Đã checkout/pull develop và merge fast-forward vào nhánh làm việc trước khi tạo nhánh fix. User duyệt cả shared/member khác; không thay human peer approval.

## FIX-001

Scoped guest read kiểm quyền từ khách sạn của BookingRoom trước query khách. Repository query bổ sung hotel và isDeleted=false; không đọc PII rồi mới filter scope. Null ID giữ hành vi danh sách rỗng; ID không tồn tại giữ lỗi lookup hiện hữu.

PASS: `mvnw.cmd -Dtest=StayGuestReviewRegressionTest,StayGuestServiceTest,LodgingScopeIsolationTest -DfailIfNoTests=true test`: 39 test, 0 failure/error/skip. 4 regression mới gồm cross-hotel deny trước query, own-hotel query, admin scoped query và missing-room deny. API/Keycloak integration cuối còn pending.

Lượt đầu bị hai Maven build chồng nhau và stale class, không dùng làm bằng chứng repro. Clean build sau đó phát hiện lỗi mới do gọi getIsDeleted không tồn tại trên BookingRoom; đã sửa theo model thật và chạy lại PASS. Các log FAIL được giữ, không coi compilation failure là functional regression reproduction. Functional repro trước đó được ghi tại review trong chat với mocks/synthetic.

Shared file `booking.repository.StayGuestRepository` được sửa để thêm scoped read; không đổi Booking service hoặc schema. Log ignored: tools/acceptance/.runtime/review-fix001-*.log. Các finding khác và verification cuối giữ NEED_FIX/PENDING cho tới khi đủ bằng chứng.

## FIX-002

Tách StayGuestAssociationValidator kiểm tuple có hiệu lực trước mutation; suy booking/phòng từ BookingRoom rồi kiểm lại hotel, parent booking, physical room và roomNumber. Update walk-in gắn cả booking/phòng nhất quán. BookingRoom model không có soft-delete flag; physical room có và bị từ chối khi đã xóa. Không sửa model Core để giả định flag.

FAIL trước fix: StayGuestReviewRegressionTest có 7 test/3 failure, đúng ba nhánh foreign BookingRoom create/update và physical room mismatch không ném lỗi. PASS sau fix: 46 test/3 suite, 0 failure/error/skip; gồm 11 regression mới và 35 existing StayGuest/scope tests. Bổ sung own-room create/update, roomNumber mismatch và parent missing. Giữ nguyên assertion/gold cũ. Logs: review-fix002-before/after/final.log trong ignored runtime. Review diff: không Core write mới, không thay schema, validation trước save và queue sync.

## FIX-003

OperationStaffScopeResolver dùng chung cho UtilityMeter, UtilityReading, StayGuest, LodgingQueue và LodgingExport. Resolve identity giữ thứ tự Keycloak ID rồi username; scope PROPERTY/REGION bắt buộc ID trong 1..32767 trước chuyển kiểu. Mỗi service giữ role policy hiện hữu; không thêm quyền hoặc đổi IAM. Helper static/narrow giữ nguyên constructor và không cache identity qua request.

FAIL trước fix: ma trận Utility 30 test/6 failure, gồm PROPERTY_MANAGER/ENGINEERING/REGION_MANAGER và meter/reading với 65537 alias hotel/region1. PASS sau fix: 130 test/8 suite, 0 failure/error/skip, gồm 30 case null/0/âm/32768/65537 và regression của năm service. Logs: review-fix003-before/after.log. Get-by-ID vẫn lấy metadata tài nguyên để kiểm scope như trước, nhưng không trả payload khi actor/scope không hợp lệ; search kiểm scope trước query dữ liệu. Review: chỉ Operation helper/services/tests, không sửa SecurityUtils toàn hệ thống.

## FIX-004

LodgingExportStateGuard so sánh candidate tách khỏi managed entity, bảo vệ 12 trường export và các liên kết. EXPORTED no-op không save guest/queue; thay nội dung trả 9073/HTTP409; actual checkout/image có thể đổi mà không requeue. Retry EXPORTED trả hiện trạng, PROCESSING/CANCELLED bị từ chối bằng9074/HTTP409; sync không kích hoạt lại terminal queue. Legacy guest có BookingRoom nhưng thiếu redundant booking/room FK được validate nhưng không tự backfill khi không có association patch.

Derived queue lookup dùng pessimistic write. Export dùng native SQL tham số hóa, chỉ khóa q theo q.id rồi refresh guest; workbook vẫn sắp theo check-in/id. Update/retry dùng cùng queue lock trước mutation. Shared ErrorCode thêm hai tuple, không sửa tuple cũ; không có migration hoặc đổi template.

FAIL trước sửa: 14 test/3 failure đúng ba nhánh EXPORTED bị requeue. Lượt sau: 66 tests/3 wiring errors EntityManager trong unit; tiếp theo 80/1 mapper wiring error trong test mới. Đã sửa wiring, giữ nguyên assertions; 80/80 PASS. Lượt cuối trên source đã giữ tương thích legacy: **88/88 PASS, 0 failure/error/skip**, gồm 7 PostgreSQL test và81 unit/controller. 4 concurrency cases mới kiểm export/update và export/retry ở cả hai thứ tự, assert transaction chờ PostgreSQL lock và nội dung workbook/status đúng; 3 export atomicity/concurrency cũ giữ nguyên.

Evidence ignored: `.runtime/review-fixes/pg004-20261103120932-020c72/` (snapshot trước seed, services/seed/tests/restore/stop logs). Chỉ sandbox hotel_acceptance_db:15432 với volume prefix m1-review mới, không hotel_booking_db. Restore và stop exit0; env.json khôi phục đúng byte. Lượt trước không chạy test vì Docker Desktop tắt; config đã khôi phục, không seed. Đã khởi động Docker Desktop ở nền; main containers tự lên theo restart policy, không chỉnh dữ liệu của chúng. Full HTTP/Keycloak cuối vẫn pending.

## FIX-005

PayrollMoneyValidator kiểm NUMERIC(15,2) trước công thức và trước persistence, không làm tròn. Kiểm precision/scale hiệu dụng trước setScale để chặn exponent cực lớn; giới hạn token tiền128ký tự. Giữ representation bộ nhớ của tiền đã có scale<=2 (1000 vẫn1000); chỉ bỏ trailing zeroes vượt scale bằng UNNECESSARY (1.2300→1.23). SQL lưu chính xác ở scale2. Dòng lỗi giữ row number, thông báo field cố định không echo tiền; các dòng hợp lệ khác tiếp tục batch như contract hiện hữu.

FAIL trước fix: 2/2 regression, importer nhận 100.005 và số vượt13integerdigits. Lượt đầu sau fix: 27 test/3failure chỉ do representation1000.00 khác assertion cũ1000. Đã giữ representation hợp lệ như trước, không sửa assertion cũ; final unit/controller/security **27/27 PASS**. Regression mới11case gồm boundary max, trailing zeros, exponent, net và mixed-file.

PostgreSQL cuối **116/116 PASS,0failure/error/skip**, gồm8PG cases và108unit/controller. Case mới round-trip max9999999999999.99 xác nhận công thức trong DB; CSV100.005+0.005=100.010 bị từ chối, không có rowinvalid. Case dùng staff1/period2099:11..12 sau kiểm tra trống, cleanup đúng các key test. 7PG queue/export cases cũ tiếp tục PASS.

Evidence `.runtime/review-fixes/pg005-20261103122355-21f4f3/`; namespace mới, snapshot trước seed, restore/stop exit0. Không migration/entity/dependency thay đổi. HTTP payroll vẫn200 với ErrorRowDto; không chuyển sang atomic whole-file import. Source finance thuộc T05 Member1, không đổi Payment/Invoice.
