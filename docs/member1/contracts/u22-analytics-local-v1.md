# U22-01 — Contract lịch sử occupancy local v1

## Quyết định và bằng chứng

CONFIRMED: user cho phép agent tự quyết và triển khai phần liên quan của Member 1/team. Contract này chốt phạm vi kỹ thuật local/synthetic theo ủy quyền đó; không giả Member 2 đã xác nhận dữ liệu hoặc security deployment. Base `origin/develop@20e06e5135def269b91531b3997b784b271e17c8`, vừa fetch ngày 30/10/2026. Worktree managed `m1-u22-history` sạch trước task; 19 file T12 đang sửa ở worktree khác được bảo toàn.

| Hiện trạng CONFIRMED | Bằng chứng tại base | Quyết định |
|---|---|---|
| Dashboard lấy số phòng hiện tại làm capacity mọi ngày, fallback occupied từ daily rates | `DashboardServiceImpl#getOccupancyTrends` | Không dùng làm training history |
| Executive report nhân room count hiện tại với số ngày | `ExecutiveReportServiceImpl#calculateRegionPerformance`, `calculateHotelPerformance` | Không coi là capacity lịch sử |
| BookingRoom có actual check-in/out và physical room | `modules/booking/entity/BookingRoom.java` | Là dữ liệu nguồn tiềm năng; chưa đủ chứng minh coverage/dedup/capacity |
| Availability có total/booked/locked/ooo theo ngày; ngày thiếu được tạo virtual từ quantity hiện tại | `RoomAvailability`, `RoomAvailabilityServiceImpl#getAvailability` | Không dùng booked thay actual occupied, không backfill lịch sử bằng current quantity |
| Chưa có implementation forecast hoặc port U22 tại base | Tìm source `modules/ai` | Thêm boundary thuần Java; không HTTP endpoint/bean tự bật |

UNKNOWN: nguồn capacity có hiệu lực theo từng ngày, completeness của actual stays và lịch sử scope hotel/region thật. Quyền user cho phép sửa mã không tự tạo bằng chứng cho dữ liệu chưa tồn tại. Phần tích hợp business giữ **NOT READY — thiếu nguồn lịch sử đáng tin**; không mở API với fixture hoặc trả fixture khi dependency lỗi.

## Contract mới — IMPLEMENTATION TARGET local

- `OccupancyHistoryPort` chỉ đọc một hotel đã được authorize, tối đa 420 ngày, inclusive local dates. Query mang scope hotel/region do authorization port trả, instant `asOf` lấy một lần từ server Clock và khoảng UTC nửa mở tương ứng. Không nhận role/region từ client làm authorization.
- `OccupancyHistoryAuthorizationPort#authorize` giải quyết principal từ trusted server context của adapter tương lai. `Optional.empty` là từ chối; null/exception là dependency unavailable. Service authorize trước history và authorize lại trước khi trả kết quả. Scope đổi hoặc quyền bị thu hồi thì không trả samples. Adapter IAM thật chưa được tạo; test dùng fake có nhãn synthetic.
- `DailyOccupancySample`: hotel ID 1..32767, date, occupiedRooms, capacityRooms và coverage `COMPLETE`/`MISSING`. COMPLETE cần `0 <= occupied <= capacity`; MISSING có cả hai count null. Missing không bằng occupied=0. Capacity=0 với occupied=0 là quan sát hợp lệ nhưng không có occupancy percentage hoặc training target; không chia cho 0.
- Capacity là số phòng vật lý thuộc inventory đủ điều kiện phục vụ trong đêm đó, đã xét thay đổi phòng/ngừng phục vụ có hiệu lực theo ngày. Occupied là số physical room khác nhau thực ở trong đêm, theo actual local check-in inclusive/check-out exclusive; planned dates, daily rates và guest count không thay actual occupancy. Late/day-use/swap phải được nguồn Core reconcile, không cộng hai lần cùng room-night. Không khôi phục capacity từ room count hiện tại.
- Timezone cố định `Asia/Ho_Chi_Minh`. Chỉ ngày trước local today đã hoàn tất. Request end=today được cắt về yesterday trước khi đọc; future end, start>=today, đảo ngày hoặc >420 ngày bị INVALID_REQUEST. Khoảng UTC `[localStart, localEnd+1)`; end=today không lọt dữ liệu hôm nay. Không dùng JVM default zone.
- Source trả batch có dataMode `SYNTHETIC` hoặc `AUTHORITATIVE` và tối đa một sample cho mỗi hotel/date trong query. Batch rỗng là các ngày MISSING, không dependency outage. Null, exception, duplicate, cross-hotel, ngoài khoảng hoặc quá giới hạn row là DEPENDENCY_UNAVAILABLE; không trả partial data từ batch lỗi.
- Service sắp theo date tăng dần, bổ sung ngày thiếu bằng MISSING, không interpolate/zero-fill. `COMPLETE` khi mọi ngày có quan sát, `PARTIAL` khi có ngày MISSING. Data mode luôn giữ từ batch, không đổi synthetic thành actual. Kết quả lỗi không chứa samples/dataMode hoặc raw exception.
- Không log raw response/principal, không PII hoặc business writes. Không cache/lưu trữ mới, không thêm retention policy. Không JPA/Booking/Inventory/Pricing dependency trong AI forecast. Không model outbound, migration, frontend hoặc dependency upgrade.

## Allowlist và kiểm chứng

Các file mới trong `backend/src/main/java/vn/edu/utc/hotel_booking/modules/ai/forecast/`:

- `model/DailyOccupancySample.java`, `model/OccupancyHistoryBatch.java`, `model/OccupancyHistoryResult.java`.
- `port/OccupancyHistoryPort.java`, `port/OccupancyHistoryAuthorizationPort.java`.
- `service/OccupancyHistoryService.java`.

Các file mới trong test: `forecast/DailyOccupancySampleTest.java`, `forecast/OccupancyHistoryServiceTest.java`, `forecast/OccupancyHistoryFixtureTest.java`, `forecast/OccupancyReadOnlyBoundaryTest.java`; resource `ai/u22/occupancy-history-420-days.json`. Contract này và report `docs/member1/acceptance/u22-01-history-2026-10-30.md`.

AC: authorization trước read và kiểm lại trước return; revoke/region mismatch không lộ data; UTC/local boundary; 420 ngày/hotel với capacity đổi theo ngày; missing khác outage; capacity 0 không tạo target; strict bounds/invalid batch; immutable ordered result; không production fixture wiring hoặc Core/write imports. Fixture hai hotel, 420 row/hotel, không phải dữ liệu thực hoặc backtest U22-02.

Kiểm tra targeted Maven `-Dtest=DailyOccupancySampleTest,OccupancyHistoryServiceTest,OccupancyHistoryFixtureTest,OccupancyReadOnlyBoundaryTest`; regression cùng personalization và kiến trúc zero-write hiện có; XML report/diff/allowlist/preservation. Không test HTTP/database khi không thêm API/provider. Rollback local bằng không wire service mới; không DB side effect hoặc thay behavior hiện tại. Commit/push/merge chưa nằm trong lượt này.

U22-02 tiếp tục dùng fixture để làm forecast/backtest, phải chốt model/split/threshold trước run. Trước khi nối dữ liệu thật: cung cấp source/version/coverage reconciliation, lịch sử capacity và scope, adapter authorization với role/Keycloak runtime, rồi API/UI acceptance riêng. Không đánh dấu các mục này PASS từ unit tests.
