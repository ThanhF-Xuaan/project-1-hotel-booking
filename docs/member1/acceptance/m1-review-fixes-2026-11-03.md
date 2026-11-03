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
