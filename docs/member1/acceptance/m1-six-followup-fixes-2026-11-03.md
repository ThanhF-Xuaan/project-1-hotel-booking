# Sáu follow-up fix — bằng chứng triển khai

Base `332dfbca444012a1bc7eb1ce084c8c0f751c5098`, branch `codex/m1-review-fixes-20261103`. Đã checkout/pull develop và merge vào nhánh trước sửa; develop `c0a375ee59be516dd589842ba9d0f79ae6c7ea05` không đổi. User duyệt sửa shared files và giao Git, không thay peer approval/branch policy.

## F01 — CONFIRMED fix local

Deep-copy tại session store boundary, refresh dùng criteria tách biệt; CAS lưu result kèm session/version chính xác cho InMemory và Redis. Lỗi discovery bất ngờ trước CAS trả 503 UPSTREAM_UNAVAILABLE, không commit criteria; AccessDenied giữ 403. Không đổi owner/TTL/key/JSON contract.

Regression trước sửa: 3 test, 2 FAIL (log private `followup-f01-before.log`). Sau sửa: 151 test/9 suite PASS, 0 lỗi/skip (`followup-f01-final.log`), gồm 74 frozen evaluation, security và zero-write. Bổ sung HTTP503: 27 test PASS (`followup-f01-http.log`). Redis thật đã thêm assertion version/detached output trong case concurrency hiện hữu; NOT RUN lượt này, chờ sandbox tổng.

Hai test harness cũ dựa vào live-reference được sửa cách setup fixture: expiry/evaluation seed trực tiếp map fixture, assert snapshot tách biệt thay identity. Không sửa expected/gold/threshold. Shared file: PublicRoomAdvisorController chỉ bổ sung mapping exception 503 đã có; không thêm route/quyền.

## Correction OBS-008 — RETRACTED false positive

`LodgingQueue.status` dùng `LodgingQueueStatusConverter`: Java ERROR ghi FAILED, FAILED đọc ERROR. Lỗi từ fixture SQL ghi thẳng ERROR không chứng minh bug JPA. Giữ lịch sử review, rút yêu cầu nới DB constraint; không thêm ERROR vào constraint.

## Trạng thái

F02/F05/F06/F04/F03 đang triển khai. Chưa kết luận toàn bộ PASS; full sandbox/CI và source receipt delta chưa chạy. Chưa merge.

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
