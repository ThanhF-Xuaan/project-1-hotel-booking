# Sáu follow-up fix — bằng chứng triển khai

Base `332dfbca444012a1bc7eb1ce084c8c0f751c5098`, branch `codex/m1-review-fixes-20261103`. Đã checkout/pull develop và merge vào nhánh trước sửa; develop `c0a375ee59be516dd589842ba9d0f79ae6c7ea05` không đổi. User duyệt sửa shared files và giao Git, không thay peer approval/branch policy.

## F01 — CONFIRMED fix local

Deep-copy tại session store boundary, refresh dùng criteria tách biệt; CAS lưu result kèm session/version chính xác cho InMemory và Redis. Lỗi discovery bất ngờ trước CAS trả 503 UPSTREAM_UNAVAILABLE, không commit criteria; AccessDenied giữ 403. Không đổi owner/TTL/key/JSON contract.

Regression trước sửa: 3 test, 2 FAIL (log private `followup-f01-before.log`). Sau sửa: 151 test/9 suite PASS, 0 lỗi/skip (`followup-f01-final.log`), gồm 74 frozen evaluation, security và zero-write. Bổ sung HTTP503: 27 test PASS (`followup-f01-http.log`). Redis thật đã thêm assertion version/detached output trong case concurrency hiện hữu; NOT RUN lượt này, chờ sandbox tổng.

Hai test harness cũ dựa vào live-reference được sửa cách setup fixture: expiry/evaluation seed trực tiếp map fixture, assert snapshot tách biệt thay identity. Không sửa expected/gold/threshold. Shared file: PublicRoomAdvisorController chỉ bổ sung mapping exception 503 đã có; không thêm route/quyền.

## Correction OBS-008 — RETRACTED false positive

`LodgingQueue.status` dùng `LodgingQueueStatusConverter`: Java ERROR ghi FAILED, FAILED đọc ERROR. Lỗi từ fixture SQL ghi thẳng ERROR không chứng minh bug JPA. Giữ lịch sử review, rút yêu cầu nới DB constraint; không thêm ERROR vào constraint.

## Trạng thái

Cả6finding đã implement và qua regression từng slice. Full sandbox/CI và delivery còn pending; chưa kết luận acceptance tổng hoặc merge.

## F02 — CONFIRMED fix local

CSV chỉ nhận 7 header snake_case đúng tên/thứ tự, UTF-8 BOM đầu file được bỏ. Sai header trả row1 INVALID_CSV_HEADER trước staff batch lookup/save; dữ liệu mỗi dòng phải đúng7cột. Giữ HTTP200 danh sách lỗi, partial import, scope, precision và upsert.

Trước sửa:9test/8FAIL; sau sửa:30test/3suite PASS,0 lỗi/skip (`followup-f02-before.log`, `followup-f02-after.log`). Gồm header đảo/thừa/thiếu/trùng/case/alias, BOM, partial row error, scope và precision. Chỉ đổi header literal của PayrollSummaryServiceTest, PayrollPrecisionReviewRegressionTest, LodgingExportPostgresTest về contract đã duyệt; giữ assertions/data. PostgreSQL lodging suite chờ sandbox tổng.

## F05 — CONFIRMED fix local

StayGuestTimeValidator so sánh instant: expected strictly sau check-in, actual không trước check-in; actual vượt expected được phép. Create/retry và check-in sync validate detached candidate trước managed mutation; update chỉ kiểm khi patch thời gian. EXPORTED guard/noop đứng trước temporal check; legacy thiếu endpoint hoặc lỗi cũ không bị tự sửa khi patch trường khác.

Trước sửa:9test/5FAIL; sau sửa:50test/3suite PASS,0 lỗi/skip (`followup-f05-before.log`, `followup-f05-after.log`). Có timezone-equivalent boundary, overstay, lỗi không làm bẩn managed entity, legacy và EXPORTED markers. Không đổi DTO/route/Core check-in flow.

## F06 — CONFIRMED fix local

Modal mount state riêng theo hotel/booking-room/association context và unmount khi đóng. List request nhận AbortSignal, hủy request cũ và guard success/catch/finally; create/delete completion từ instance cũ không reset/reload context mới. OCR abort/human review giữ nguyên. Bỏ log Axios có thể chứa thông tin khách ở đường list/delete.

Browser trước sửa4/4FAIL; sau sửa11/11PASS gồm6race +5OCR (`followup-f06-before.log`, `followup-f06-after.log`). Lint --max-warnings=0 và TypeScript/Vite build PASS. Hai lỗi selector icon của test mới đã sửa bằng title nút đã xác minh; không đổi assertions.

Shared/member khác: `frontend/src/modules/booking/components/StayGuestsModal.tsx` sửa lifecycle để không lẫn khách giữa phòng; `frontend/src/modules/operation/services/stayGuest.service.ts` thêm optional AbortSignal, tương thích caller cũ. Thêm fixture/spec browser tổng hợp; không đổi shared API client/auth/route.

## F04 — CONFIRMED fix local/PostgreSQL

Tất cả write utility readings và update/delete meter khóa PESSIMISTIC_WRITE cùng parent meter; bulk khóa ID tăng dần. Sau khóa refresh meter/reading và kiểm scope/deleted lại. Delete kiểm cặp surviving neighbors cuối cùng, không tự đổi reset/giá trị. lock_timeout cố định5s; timeout/deadlock chuyển mã9054 HTTP409 thông báo cố định.

PostgreSQL trước sửa2/2FAIL (race tạo dãy giảm và xóa reset làm âm delta). Sau sửa78test PASS,0 lỗi/skip, gồm6PostgreSQL thực +72utility/scope (`followup-f04-final.log`). Đã quan sát2writer chờ FOR UPDATE trên utility_meters; kiểm timeout~5s, stale update sau soft-delete, meter khác không chờ và bulk delete final survivors. Sandbox mới `m1-followup-*`, localhost15432, có backup `tools/acceptance/.runtime/followup-snapshot.sql`; không chạm DB chính5433.

Shared files: ErrorCode thêm9054 còn trống; UtilityMeterRepository thêm query khóa; UtilityReadingRepository thêm query history để kiểm surviving neighbors. Constructor/API/permission/unique-per-date/usage semantics giữ nguyên. Hai unit harness thêm EntityManager mock và locked fixture trả về; assertions cũ giữ nguyên. Một lỗi khởi tạo DTO test và tên biến patch đã được sửa trước khi đạt PASS; không tính compile failure là reproducer.

## F03 — CONFIRMED fix local/PostgreSQL

Khóa BYTEA nội bộ32byte được cùng SQL helper tính cho service và trigger. Key BOOKING dùng hotel + effective booking (fallback qua BookingRoom/BookingDetail khi FKbookingnull) + document; WALKIN dùng hotel + room + document. Chỉ check-in/chưa checkout/chưa xóa; trim ASCII whitespace, case-sensitive; null/blank/N/A không gộp người. Trigger tính lại và unique index bảo vệ cả writer ngoài service. Không thêm key vào DTO/API.

Service lấy advisory transaction locks theo key tăng dần trước queue/guest lock, refresh/rebase và kiểm key trước ghi; timeout5s và mã9062/HTTP409 cố định. Retry trả cùngID/queue; checkout/delete giải phóng key; không phục hồi active match đã checkout trong lúc chờ. EXPORTED legacy noop giữ FKnull và toàn bộ row/queue markers.

Migration incremental `20261103-160000-active-stay-dedup`, context bắt buộc `@m1-active-stay-dedup`; root includeAll giữ nguyên. Lock maintenance, preflight duplicate/bad links → abort cả transaction, chỉ báo numeric IDs; không auto-delete/merge. Backfill chỉ key, preserve timestamp/export fields. Runtime verifier yêu cầu migration/index/trigger trước app mới. Đã áp bằng Liquibase vào sandbox đã backup: receipt `tools/acceptance/.runtime/followup-f03-migration-receipt.json`, businessRowDigestUnchanged=true.

Phát hiện schema kế thừa `booking_room_id NOT NULL` làm WALKIN không lưu được; migration gỡ NOT NULL theo contract WALKIN đã duyệt. DDL rollback chỉ gỡ technical dedup objects, giữ nullable compatibility để không phá walk-in đã có. Ưu tiên rollback app giữ constraint; DDL rollback cần dừng writers/backup.

Bằng chứng: booked regression trước sửa5test/4FAIL,0error; sau sửa70test PASS gồm14dedup PG +4migration PG +2HTTP409 +50StayGuest/time/EXPORTED (`followup-f03-final.log`). Có concurrency hai key cùng thứ tự, check-in batch duplicate rollback, direct writer constraint, tamper key, placeholder/missing identity, scope/case/whitespace, precheckin/history và stale checkout. Ban đầu4test lỗi fixture walk-in NOT NULL được tách khỏi reproducer, sau đó bổ sung test WALKIN thật sau migration. Không tính fixture/compile lỗi là chứng cứ lỗi dedup.

Shared/member khác: StayGuest entity/repository (Booking) thêm internal read-only key và SQL param queries để bảo vệ invariant chung; ErrorCode thêm9062 còn trống. PersonalizationConsentPostgresIT chỉ thêm context migration cần cho full-app startup trên DB trống; không sửa logic U14. Không sửa BookingService/Pricing/Inventory business logic. StayGuest unit mocks chuyển old lookup sang key lookup, fixture active có check-in; assertions nghiệp vụ giữ nguyên.

### Review F03/F04 và wiring

Review bổ sung normalization document bằng đúng6ASCII whitespace (không strip ký tự điều khiển khác), timeout cho cả unknown/planned identity. Kiểm gộp cuối86test PASS,0 lỗi/skip gồm24operation PostgreSQL +10export PostgreSQL cũ +52StayGuest/HTTP (`followup-operation-final.log`).

CI/local acceptance: Start.ps1 thêm context maintenance explicit trong sandbox; Run-BackendTests.ps1 bắt buộc chạy3operation IT với số case6/14/4, không chấp nhận skip. CI thêm job PostgreSQL disposable có password ngẫu nhiên/mask, refused local target và cleanup chỉ container có ownership label;3unit test guard PASS. Backend gate tăng minimum và bắt buộc các suite mới; frontend CI thêm race/OCR browser. YAML parse và diff --check PASS. CI thực/acceptance tổng ở commit cuối vẫn pending.

### Review F01 Redis cleanup
Snapshot deserialize/copy/serialize lỗi giải phóng WATCH trước503; serialize trước MULTI, discard khi transaction lỗi. Regression trước sửa12test/2FAIL; sau sửa130test PASS,0 lỗi/skip gồm frozen/security/HTTP (followup-f01-cleanup-after.log).

Source receipt mới pin23production files từ332dfbc đến3429a94, compose trước receipt cũ; giữ nguyên gold/historical receipt. Đây là source provenance và AI technical review, không phải peer approval. Sandbox F03/F04 đã restore/stop và config trả nguyên byte.
