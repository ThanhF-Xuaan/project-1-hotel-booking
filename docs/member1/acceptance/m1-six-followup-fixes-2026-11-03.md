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
