# GIT-01 — Preview phân phòng nhóm local v1, 31/10/2026

## Quyết định và bằng chứng trước implementation/test

CONFIRMED: user là người quyết định, cho phép tự triển khai phần Member 1/team liên quan. Base vừa fetch `origin/develop@20e06e5135def269b91531b3997b784b271e17c8`. Managed worktree mới m1-group-preview sạch; bảo toàn22file U22,19file T12 và contract C01 tại worktree cũ. Không giả member sign-off hoặc chứng minh quyền/dữ liệu bằng fixture.

CONFIRMED tại base: GroupRoomBlockController có GET block và POST release/scheduler có write; RoomInstance/response chưa có floorNumber/capacity/beds; HotelRoomType có maxBeds/extraBeds nhưng chưa có physical-room bed mapping được xác minh. GroupRoomBlockDaily có blocked/picked/released/status nhưng AI không tính quota bằng phép trừ các cột. Current room status không chứng minh availability toàn stay. Không có engine/port/API preview.

Contract C01 đã review quota ở H006: `C:/Users/admin/.codex/worktrees/m1-completion-a00/project-1-hotel-booking/docs/member1/contracts/group-room-assignment-preview-v1.md`; đọc nguyên vẹn và giữ hash trong protected-preflight.json. Giữ objective và oracle P01–P15, đặc biệt aggregate quota theo block/type/mọi occupied night. Contract này chốt các chi tiết local còn thiếu theo ủy quyền user, trước test/metric; không đổi semantics thành nghiệp vụ thật.

UNKNOWN: production read-only provider toàn stay, physical bed mapping, floor metadata và trusted IAM scope. Triển khai pure engine/service với synthetic port; không controller/bean/UI hoặc API business tự bật. Preview không hoàn tất SRS upload rooming list/tạo StayGuest/apply check-in. Không hold/assign/release/booking/guest/payment write, kể cả sau confirm.

## Input, snapshot và an toàn

- Một request hotel/contract và interval check-in inclusive/check-out exclusive theo Asia/Ho_Chi_Minh;1–31nights trong local profile để bound evidence. Demands1–100, opaque unique ASCII demandKey1–64 `[A-Za-z0-9_-]`, persons/bedUnits integer dương. Không tên/CCCD/passport/guest ID.
- Tối đa300distinct candidate physical rooms. Candidate gồm roomInstanceId>0, hotelId, roomBlockId>0, hotelRoomTypeId>0, capacityPersons/bedUnits dương, nullable floorNumber và eligibleForEntireStay. Floor có thể âm; span tính long để không overflow. Không nhận roomNumber và không suy tầng từ số phòng.
- Trusted authorization port giải quyết principal/scope hotel/region/contract trước data read; client selectors không phải quyền. Empty=denied, null/exception/malformed scope=dependency unavailable. Kiểm lại scope/quyền sau search và không trả assignments nếu bị thu hồi/đổi scope.
- Read-only data port trả một snapshot: scope, snapshotId, observedAt, validUntil, basis, mode, coverage, candidates, quotaRows, nightEligibility. Local chỉ chấp nhận mode SYNTHETIC và basis `SYNTHETIC_PREVIEWABLE_ROOMS_V1`; không coi đó là production basis đã được phê duyệt.
- observedAt không tương lai, tuổi tối đa60giây và now<validUntil; checked lại sau read và trước return. Đây là local consistency policy, không SLA production. Coverage phải COMPLETE. Exception không echo/log raw payload/PII; partial/outage không thành empty success.
- Mỗi candidate phải có đúng một eligibility row cho mọi occupied night, matching toàn bộ scope/block/type/room/date; state ELIGIBLE/ROOM_UNAVAILABLE/BLOCK_INACTIVE/CUTOFF_EXPIRED. Candidate boolean phải khớp all-night states; mâu thuẫn/thiếu/trùng/extra row dừng cả snapshot. Inactive/cutoff/room unavailable được loại có warning, không tự gọi release/hold để sửa.
- Mỗi key block/type có candidate phải có đúng một quota row cho mỗi occupied night, matching hotel/contract/block/type/date, count integer>=0. Không suy missing=0, cộng duplicate, chuyển quota giữa key hoặc derive từ candidate count. Checkout không tiêu quota. Với một interval chung, capacity graph của key là min quota qua các đêm; tương đương invariant từng đêm của C01.
- Snapshot bound:300rooms, tối đa300×31eligibility và300×31quota rows; không truncate. Capacity0 có nghĩa đã biết, khác thiếu evidence. Snapshot rỗng COMPLETE hợp lệ cho no capacity; fake không được wire thành production fallback.

## Exact objective và resource bound

1. Tối đa số demands được gán, mỗi demand/phòng tối đa một assignment; fit đủ persons và bedUnits.
2. Giảm tổng bedUnits-requiredBedUnits, tính long.
3. Giảm global floor span nếu mọi eligible candidate có floor. Bất kỳ eligible floor null thì tắt objective tầng cho toàn request, floorSpan null và warning UNKNOWN_FLOOR_METADATA; không ưu tiên phòng thiếu thông tin.
4. Tie-break vector roomInstanceId theo demandKey ASCII; unmatched sentinel=max room ID+1 (long). Output sort demandKey, input permutation không đổi kết quả.

Thuật toán min-cost max-flow qua demand→physical room→block/type quota→sink, residual reassignment để không mắc greedy. BigInteger cost encode bed waste trước vector lexicographic, không overflow ID/cost. Cardinality tối đa trước cost; quota nằm trong graph, không cắt nghiệm sau matching. Khi cần floor objective, sliding floor windows với monotonic right endpoint, solve exact từng window và so cardinality/waste với optimum toàn tập; chọn span nhỏ nhất rồi vector nhỏ nhất.

Work budget cố định tối đa30.000.000edge examinations cho toàn search, gồm mọi floor window. Vượt budget trả TEMP_UNAVAILABLE/SEARCH_BUDGET_EXCEEDED, assignments/unmet/objective rỗng; không trả nghiệm gần đúng dưới nhãn tối ưu. Budget nhỏ hơn chỉ constructor test/fault injection, không có public config/client override. Input hợp lệ không đồng nghĩa mọi case khó chắc chắn hoàn thành trong budget. Complexity test100/300 kiểm representative bound, không SLA hoặc performance proof toàn không gian dữ liệu.

## Output, allowlist, kiểm chứng và rollback

Logical output COMPLETE/INVALID_REQUEST/ACCESS_DENIED/TEMP_UNAVAILABLE, reasonCode, SYNTHETIC marker/scope/dates/snapshot time, assignments, unmet NO_ELIGIBLE_CAPACITY, warnings, objective metadata và previewOnly=true. Lỗi không có partial assignments/unmet hoặc ID từ snapshot sai scope. Không HTTP route/envelope/role mới.

File mới trong modules/ai/groupassignment: model/GroupRoomDemand.java, RoomAssignmentCandidate.java, RoomAssignmentPreview.java; dto/GroupRoomAssignmentPreviewRequest.java; port/GroupAssignmentDataPort.java, GroupAssignmentAuthorizationPort.java; service/GroupAssignmentValidation.java, DeterministicGroupRoomAssignmentEngine.java, GroupRoomAssignmentPreviewService.java. Không separate HTTP response/controller khi chưa expose API.

Test mới: DeterministicGroupRoomAssignmentEngineTest, GroupRoomAssignmentPreviewServiceTest, GroupPreviewFixtureTest, GroupPreviewReadOnlyBoundaryTest và helper GroupPreviewTestData; resource ai/group-assignment/preview-cases.json. Hai tài liệu mới: contract này và report git-01-preview-2026-10-31.md. Không sửa source/schema/gold/contract lịch sử.

AC: C01 P01–P15, quota1/middle-night/zero/control/no-transfer; independent exhaustive small-case oracle gồm cardinality/waste/span/vector; permutations cả3input lists; false eligibility/partial/stale/cutoff; malformed/cross-scope/duplicates/bounds; authorization before provider và revoke; exact failure khi work exhausted; representative100/300 và non-overflow IDs/floors; zero Core/write/network dependencies. Fixture/schema/expected phải được chốt trước test đầu, không chỉnh gold để engine pass.

Lệnh Maven targeted engine/service/fixture/architecture; regression cùng GroupRoomBlock/RoomInstance và T12 architecture tests. Không DB digest vì không API/provider thật hoặc DB access. Source/runtime IAM/bed/floor/quota consistency cần evidence riêng trước tích hợp; user authorization không tự tạo dữ liệu đó. Rollback không wire service mới; không migration/data mutation. Chưa stage/commit/push/merge trong slice này.
