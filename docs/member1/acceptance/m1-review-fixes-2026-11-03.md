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

## FIX-006

DestinationResolutionStatus nội bộ phân biệt RESOLVED/AMBIGUOUS/UNRESOLVED/UNAVAILABLE; giữ builder cũ tương thích. Chat/search/refresh dùng chung nhánh fail-closed và cập nhật session bằng owner-bound CAS hiện hữu. Upstream lỗi/null/invalid trả TEMP_UNAVAILABLE với coverage không COMPLETE; điểm đến chưa xác định trả NEED_INFO; catalog hợp lệ rỗng vẫn NO_MATCH/COMPLETE. Refresh áp dụng hotel vừa resolve trước query. Adapter log mã cố định, không echo query/exception.

FAIL trước sửa: 5/5 regression đúng ba entrypoint destination outage, unresolved và null room payload. PASS đầu117/117. Final **158/158 PASS,0failure/error/skip**, gồm8 dependency regressions,74 frozen eval, security integration MockMvc, session và zero-write architecture. Bổ sung null destination, valid empty catalog và refresh resolve hotel/version persistence. Không đổi public DTO/status, thuật toán/gold hoặc Booking/Pricing service. Logs review-fix006-before/after/final.log; các integration security này là servlet mock, chưa thay bằng chứng Keycloak thực. Một lệnh format nhầm đường dẫn backend/backend thất bại trước Maven; không thay source và không dùng làm test evidence.

## FIX-007

Widget dùng generation và AbortController cho chat/refresh/409-resync; guard đồng bộ bằng ref chặn request trùng, reset/unmount abort và vô hiệu generation. Mọi success/catch/finally kiểm generation trước mutation, không để finally cũ mở khóa request mới. Reset xóa input/loading/session/version/cooldown; đóng/mở drawer vẫn giữ hội thoại. Client nhận AbortSignal qua options, giữ legacy string overload, cookie và timeout cũ; không thêm endpoint hoặc server-side session deletion.

FAIL trước sửa: browser delayed200 tái hiện input vẫn disabled sau reset. PASS sau sửa: **10/10 Chromium tests**, gồm6 reset/session cases và4 FAQ regressions. Bao gồm response200/429/409 trễ, reset trong409-resync, explicit refresh và đóng/mở; phiên mới không mang session cũ và không bị version99 ghi đè. Lint --max-warnings=0 và TypeScript/Vite build PASS. Chưa có test unmount độc lập; cleanup được review qua code. Logs review-fix007-before/after/final/lint/build.log. Shared frontend widget/service sửa để cô lập lifecycle request; ci.yml bổ sung spec vào frontend job hiện hành, không khôi phục workflow cũ.

## Review bổ sung và verification tổng thể

Lần full `20261103-124228-209-cf7e7d2f` giữ **FAIL**, không kết luận PASS: backend1569/1569, U14 opt-in6/6, browser106/106 PASS; API39/41 PASS,2FAIL ở fixture dựng trạng thái ERROR không được DB cho phép. Các case HTTP mới về anonymous401/cross-hotel403, association400, EXPORTED no-op/retry/409 và payroll precision đều PASS. Đã sửa riêng fixture cutoff/concurrent thành PENDING, xóa completion marker cho lượt tổng hợp mới; không giảm assertion export hoặc cho production retry mở lại EXPORTED. Snapshot/analytics cleanup/restore/stop đều exit0, original runtime env được khôi phục. Giữ báo cáo FAIL gốc trong acceptance/runs.

Review FIX-004 phát hiện guest predicate có thể được SQL đánh giá trước khi chờ queue lock. Hai regression unit đã FAIL thực: không ném LODGING_EXPORT_EMPTY sau khi refresh guest vượt cutoff/đổi category. Đã thêm kiểm tra lại active/check-in/cutoff/quốc tịch sau refresh trước workbook; trường hợp không còn eligible trả9072 và không đổi queue. Hai fixture unit workbook cũ dùng ngày request cố định nhưng check-in theo clock hiện tại được sửa về ngày cố định trong phạm vi; giữ nguyên assertion nội dung/formula safety. Lượt đầu bổ sung PG test compile FAIL vì DTO update không có checkInTime; đã sửa test theo contract thật, không thêm field DTO. PG cutoff case dùng transaction queue-first trực tiếp; category case gọi StayGuestService thật. Final **69/69 PASS**, gồm10 real-PG và59 unit,0skip; cả hai race mới xác nhận PostgreSQL lock wait và không tạo workbook/batch sai scope. Evidence pg004-snapshot-20261103135439-8a5105; backup/restore/stop exit0, original env khôi phục.

Provenance: thêm receipt riêng pin đúng19 application files/base/commit/hash/patch; compositor chỉ trả bytes baseline sau kiểm tra chính xác delta mới, không sửa manifest/gold/historical receipts. Trạng thái receipt AI_TECHNICAL_REVIEW_ONLY, humanPeerApproval=false. CI hiện hành bổ sung kiểm receipt/tamper và yêu cầu5 regression suites mới; giữ nguyên5 jobs và historical detached audit. Tooling:62 T12 tests PASS;4 tamper tests chạy lại sau tối ưu skip path ngoài allowlist PASS;10 CI tooling và8 route tests PASS. Acceptance tooling lần đầu34test/63subtestfail/4error vì ancestor của TEMP mặc định có inode0; kiểm tra fail-closed hoạt động đúng. Đổi TEMP riêng sang directory owned trên D: có identity hợp lệ,34/34 PASS; không sửa verifier, không giảm guard. Directory ignored: D:/Project/University/project-1-hotel-booking/tools/acceptance/.runtime/review-fixes-tmp-e4f92f17.

### Finding ngoài phạm vi bảy fix — M1-OBS-008, NEED_FIX riêng

CONFIRMED: LodgingQueueStatus có ERROR; StayGuestServiceImpl.syncLodgingQueue và LodgingQueueServiceImpl.retryValidate gán ERROR khi thiếu trường. Constraint chk_lodging_queue_status trong backend/src/main/resources/db/sqlFile/incremental/2026/20261005-020000-create-stay-guest-and-lodging-queue.sql chỉ nhận PENDING/PROCESSING/EXPORTED/FAILED/CANCELLED. INSERT/UPDATE ERROR thực bị PostgreSQL từ chối trong lần full trên. Đây là mismatch kế thừa, không phải finding EXPORTED hoặc lỗi phát sinh bởi delta mới. Không đổi schema/API status trong đợt sửa được duyệt này. Task riêng cần thống nhất public queue status, rồi migration incremental hoặc mapping FAILED phù hợp; thêm real-PG regression cho create/retry thiếu trường và rollback. Không coi các test completed-guest PASS là bằng chứng nhánh incomplete hoạt động đúng.

### File shared hoặc thuộc frontend/Core đã sửa

- backend/src/main/java/vn/edu/utc/hotel_booking/modules/booking/repository/StayGuestRepository.java: query guest theo cả booking-room/hotel/active để chặn rò PII.
- backend/src/main/java/vn/edu/utc/hotel_booking/common/exception/ErrorCode.java: thêm9073/9074 với HTTP409 cho export-state protection; giữ tuple cũ.
- frontend/src/modules/booking-portal/components/RoomAdvisor/RoomAdvisorWidget.tsx: hủy request và guard generation khi reset/unmount.
- frontend/src/modules/booking-portal/services/roomAdvisorService.ts: truyền AbortSignal, giữ overload/cookie/timeout.
- frontend/e2e/api/operations-t04-t05.spec.ts và frontend/e2e/browser/t12-session-reset.spec.ts: bằng chứng real-token API và browser race; fixture mới tương ứng contract EXPORTED.
- .github/workflows/ci.yml: chạy regression mới và kiểm receipt; không thay lịch sử CI/human approval.
- tools/acceptance/Run-BackendTests.ps1, Verify.ps1 và tools/t12/validate_r1_contracts.py, verify_widget_source_reaudit.py, verify_current_core_reaudit.py: yêu cầu đủ số PG test thực, báo trạng thái suite chính xác và compose provenance mới. Các Java service/helper/test còn lại thuộc phạm vi Member1 Operation/Finance/AI.

## CI và giao Git — bằng chứng trước merge

Branch đã push; PR https://github.com/ThanhF-Xuaan/project-1-hotel-booking/pull/57. Connector GitHub tạo PR bị403 integration permission; gh đăng nhập bằng tài khoản có quyền push đã tạo thành công, không sửa permission. CI run41593263647 trên616bb6a:4/5job PASS (contract/tooling, analytics PG, frontend và Docker), backend1558test/1error ở LodgingQueueReviewRegressionTest.exportedRetryIsReadOnlyNoop, lỗi UNAUTHORIZED. Test mới trước đó dùng lại SecurityContextHolder.getContext() có thể đang chứa mock của test cũ. Đã tạo context rỗng mới trong setup bốn regression class để độc lập thứ tự chạy, giữ @AfterEach clear; không đổi production guard, assertions hoặc CI scope. Xác nhận sau sửa: CI run41593851817 trên13f3808 **5/5job PASS**, local4 regression suites **70/70 PASS**,0skip. Đây là test-isolation delta,19 application files vẫn đúng receipt b8495f9.

## Kết quả kỹ thuật cuối — CONFIRMED

Full run **20261103-135830-561-239cf9f6 PASS**, baseline của run616bb6a; sau đó13f3808 chỉ sửa setup test như trên. Backend1573/1573 (gồm10 PostgreSQL export/payroll,4 Redis và application context thực), U14 opt-in PG6/6, API41/41, browser106/106;0test skip trong backend. Lint --max-warnings=0, build, dependency audit mức high, source/contract/tamper và DB-backed synthetic demo PASS. Suite con skip Maven nên PARTIAL theo thiết kế; supervisor đã kiểm Maven riêng và kết luận PASS, không tính skip là pass. Báo cáo chi tiết: runs/acceptance-20261103-135830-561-239cf9f6.md. Snapshot SHA256 F95F243840FCBCB5E618618DFB9020B102A2590784E13A9AE85B8D781FF23B5B; analytics cleanup/restore/stop exit0 và env.json khôi phục đúng bytes. Runtime account/dataset do runner sandbox quản lý đã cleanup; không migration vào DB nghiệp vụ.

| Finding | Verdict kỹ thuật | Bằng chứng chính |
|---|---|---|
| M1-REV-001 | PASS | Scoped repository; unit và real-token HTTP own200/foreign403/anonymous401, không lộ document |
| M1-REV-002 | PASS | Effective association validation trước mutation; foreign/mismatch tests và HTTP400/không đổi guest |
| M1-REV-003 | PASS | Scope range trước narrowing;30 invalid Utility cases và existing scope/runtime denies |
| M1-REV-004 | PASS | EXPORTED no-op/retry giữ marker, content409;10 PG cases gồm lock wait/rollback/cutoff/category; HTTP/export content |
| M1-REV-005 | PASS | Exact NUMERIC(15,2), unit11cases, PG max/equation/no invalid row và HTTP rejected precision |
| M1-REV-006 | PASS | 3 entrypoint fail-closed,8 regressions,74 frozen eval, coverage và session/security checks |
| M1-REV-007 | PASS | Generation/abort, delayed200/429/409/resync/refresh browser cases, close/reopen và lint/build |

Peer approval của con người không được tạo hoặc giả lập. Các kết quả trên là technical review/test trong synthetic sandbox và CI. Unmount cleanup đã review qua code, chưa có browser reproducer riêng. Chưa kiểm AI provider trả phí, production Keycloak, dữ liệu vận hành thật hoặc định dạng BCA chính thức; không dùng synthetic PASS để đóng các nghiệm thu đó. M1-OBS-008 giữ NEED_FIX riêng như mô tả ở trên.

Rollback: revert toàn PR cùng receipt/compositor và các test liên quan, chạy lại verification; không revert riêng application file rồi bỏ qua provenance check. DB sandbox đã restore trước khi giao; không có migration nghiệp vụ cần rollback. Trạng thái merge/check SHA cuối được ghi trên PR và báo cáo giao trong chat, sau khi CI của commit tài liệu này hoàn tất; không dùng AI self-review làm peer approval hoặc bypass branch policy.


### Đính chính sau kiểm tra converter — 2026-11-03

M1-OBS-008 được rút lại (RETRACTED false positive), thay cho NEED_FIX nêu trong lịch sử ở trên. `LodgingQueue.status` dùng `@Convert(LodgingQueueStatusConverter.class)`: ERROR được ghi thành FAILED và FAILED được đọc thành ERROR. Fixture SQL ghi trực tiếp ERROR bị constraint từ chối không chứng minh lỗi đường JPA. Không nới constraint hoặc thêm ERROR. Xem [bằng chứng follow-up](m1-six-followup-fixes-2026-11-03.md).
