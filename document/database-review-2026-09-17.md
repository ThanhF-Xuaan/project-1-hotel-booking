**Review database — Hotel Booking System — 17/09/2026**

Database có nền tảng phù hợp để phát triển hệ thống đặt phòng, nhưng chưa đủ cơ sở để dùng cho booking và thanh toán trên production. Các vấn đề chính nằm ở dữ liệu seed sai phạm vi khách sạn, quyền sở hữu snapshot giá, tính nhất quán giữa các bảng, và cơ chế ghi dữ liệu đồng thời chưa được triển khai.

**Phạm vi và mức độ xác minh**

Đã đọc mã nguồn ứng dụng backend, frontend, cấu hình build/deployment, README và toàn bộ 8 script SQL: 2.422 dòng SQL, 44 bảng, 28 index khai báo riêng trong script 03. Backend có 8 class Java chính và một test `contextLoads`; frontend mới hiển thị trang thông báo. Không có controller, service, repository, entity nghiệp vụ hoặc SaveRequest DTO để đối chiếu contract.

Source thực tế dùng Java 21, Spring Boot, React và PostgreSQL 16. Review này dựa trên repository hiện tại, đồng thời giữ nguyên nguyên tắc backend/database là nguồn dữ liệu chính và không đề xuất thêm trường chỉ vì frontend cần hiển thị.

Các con số seed bên dưới được tính bằng cách đọc và mở rộng tập giá trị trong script, chưa phải kết quả truy vấn database đang chạy. Docker daemon không hoạt động và không có `psql`, nên chưa thực thi bootstrap, kiểm tra catalog thực tế, chạy `EXPLAIN ANALYZE` hoặc stress test concurrency. Không khẳng định script đã chạy thành công trên PostgreSQL 16.

Phân loại: **P1** cần giải quyết trước khi triển khai nghiệp vụ liên quan trên production; **P2** cần xử lý trong quá trình hoàn thiện schema và vận hành. Một số P1 là thiếu bảo đảm trong thiết kế, không phải lỗi runtime đã tái hiện, vì API nghiệp vụ chưa tồn tại.

**Những điểm nên giữ lại**

- Tách `room_types`, `hotel_room_types` và `room_instances`: phân biệt danh mục toàn hệ thống, cấu hình từng khách sạn và phòng vật lý.
- Tách `booking_details` khỏi `booking_rooms`: có cơ sở cho đặt theo hạng phòng rồi phân phòng.
- Dùng `NUMERIC` cho tiền, `TIMESTAMP WITH TIME ZONE` cho thời điểm và identity cho khóa chính.
- `available_count` là generated column; CHECK hiện tại chặn counter âm và tổng booked/locked vượt `total_rooms` trong cùng một bản ghi.
- Unique constraint theo `(hotel_room_type_id, date)` và `(room_instance_id, slot_date)` là các chốt bảo vệ cần thiết.
- Snapshot giá/thuế/phí đã xuất hiện trong `booking_daily_rates`, `booking_charges`, `invoice_details` và `service_order_details`. Không cần ép thêm `unit_price` vào mọi bảng nếu thông tin đã có chủ sở hữu đúng ở bảng chi tiết khác.
- Exclusion constraint dùng GiST để chặn rule chồng thời gian là hướng tốt. Sparse inventory nên được giữ, tránh tạo sẵn hàng triệu slot READY.

**1. [P1 — lỗi seed xác định] Inclusions liên kết menu của khách sạn khác**

Nguồn: `database/07-seed-data-inventory-mapping-intance.sql:127`, `:140`, `:154`.

Cả ba câu INSERT đều dùng `CROSS JOIN menus`, chỉ lọc theo tên menu, không ràng buộc `m.hotel_id = h.id`. Script 06 tạo cùng menu ở cả ba khách sạn, nên mỗi loại quà tặng khớp ba menu thay vì một menu thuộc cơ sở đó.

Với dữ liệu seed hiện tại, các câu INSERT mở rộng thành **42 inclusion**, trong đó **28 liên kết khác khách sạn**; chỉ 14 liên kết thuộc đúng cơ sở. Hạng DLX Hà Nội nhận sáu dòng buffet/nước thay vì hai dòng.

Cần JOIN menu theo khách sạn và xác minh subtype. `NOT EXISTS` hiện chỉ chống lặp từng ID, không sửa được phạm vi dữ liệu sai. Nếu dữ liệu đã được nạp, cần migration sửa dữ liệu cũ; sửa SELECT không tự loại bỏ 28 dòng sai. Việc sửa không cần thêm trường vào DTO.

**2. [P1 — hành vi hàm đã đối chiếu tài liệu] Mã booking/service order bị cắt sau bốn chữ số**

Nguồn: `database/02-functions-and-triggers.sql:59`, `:84`.

`LPAD(nextval(...)::text, 4, '0')` không giữ nguyên số dài hơn bốn ký tự: PostgreSQL cắt phần bên phải. Ví dụ `10000` và `10001` cùng thành `1000`; nếu tạo trong cùng ngày, chúng có cùng mã và đụng UNIQUE. Sequence hiện là sequence toàn hệ thống, không reset theo ngày, nên đổi ngày không giải quyết giới hạn này. Hành vi cắt chuỗi được mô tả trong [tài liệu LPAD của PostgreSQL 16](https://www.postgresql.org/docs/16/functions-string.html).

Cần coi bốn chữ số là độ dài tối thiểu, giữ toàn bộ giá trị sequence khi lớn hơn 9.999. Giữ nguyên sequence và mã cũ; không reset sequence để chữa lỗi. Backend/UI tương lai không được giả định suffix luôn dài đúng bốn ký tự. Đây là sửa logic sinh mã dùng chung, không cần thêm một bộ sinh mã ở từng service.

**3. [P1 — khoảng trống schema] Snapshot giá có thể không thuộc booking nào và bị lặp theo đêm**

Nguồn: `database/01-init-schema.sql:882`.

`booking_daily_rates.booking_room_id` nullable; bảng không có FK trực tiếp tới booking hoặc booking detail. Một dòng giá với owner NULL có thể được lưu nhưng không có đường liên kết xác định về booking. Ngoài ra không có UNIQUE `(booking_room_id, stay_date)`, nên hai dòng giá của cùng phòng/cùng đêm đều được chấp nhận.

Đối với mô hình một dòng giá cuối cùng mỗi phòng mỗi đêm, owner phải bắt buộc và cặp owner/ngày phải unique. Nếu cần adjustment hoặc revision, phải phân biệt chúng với dòng giá chính bằng mô hình nghiệp vụ cụ thể; không để duplicate tự do. Cần kiểm tra orphan/duplicate trước khi thêm constraint vào dữ liệu đã tồn tại.

**4. [P1 — quyết định kiến trúc cần chốt] Snapshot hiện phụ thuộc việc phân phòng vật lý**

Nguồn: `database/01-init-schema.sql:828`, `:838`, `:884`.

`booking_details` có `selection_deadline`, nhưng snapshot giá lại chỉ gắn với `booking_rooms`, trong khi `booking_rooms.room_instance_id` bắt buộc. Nếu flow dự kiến là xác nhận đặt theo hạng phòng rồi phân phòng sau, hiện không có chỗ lưu snapshot có owner đúng ở thời điểm xác nhận. Cho FK snapshot bằng NULL không giải quyết được vấn đề.

Cần chọn một trong hai flow: luôn phân phòng trước khi xác nhận; hoặc lưu giá đã chốt theo booking detail trước, rồi liên kết việc phân phòng sau. Phương án thứ hai phù hợp hơn nếu cần đổi phòng mà vẫn giữ giá đã thỏa thuận. Không nên tính lại giá lịch sử từ `hotel_room_types.base_price` lúc phân phòng. Đây là thay đổi ownership của dữ liệu nghiệp vụ, cần thiết kế trước khi tạo Entity/SaveRequest; không nhân đôi snapshot ở nhiều bảng.

**5. [P1 — khoảng trống toàn vẹn] FK chưa bảo đảm các đối tượng cùng khách sạn, hạng phòng hoặc booking**

Nguồn: `database/01-init-schema.sql:370`, `:814`, `:838`, `:905`, `:934`, `:984`, `:1051`.

Các liên kết sau chỉ kiểm tra ID tồn tại:

- `room_instances.hotel_id` có thể khác khách sạn của `hotel_room_type_id`.
- Booking detail có thể chọn hạng phòng của khách sạn khác với booking header.
- Booking room có thể phân phòng khác hạng so với booking detail.
- Room slot có thể trỏ tới booking room được phân cho phòng khác; ngày slot cũng chưa được ràng buộc vào khoảng lưu trú.
- Booking charge có thể chọn một `booking_guest_id` thuộc phòng booking khác.
- Transaction có thể dùng payment của booking A nhưng khai báo `booking_id` của booking B.
- Service order có thể chỉ định phòng khác khách sạn với booking; menu của order detail cũng có thể thuộc cơ sở khác.

Cần lập danh sách invariant và thực thi bằng composite FK khi các cột hiện có cho phép, hoặc trigger/transaction service tập trung cho quan hệ phải đi qua nhiều bảng. Ví dụ có thể dùng cặp hotel/type hiện có để bảo vệ `room_instances`, và cân nhắc bỏ `transactions.booking_id` nếu luôn suy ra từ payment. Không thêm `hotel_id` vào mọi bảng một cách máy móc. FK đơn không thể chứng minh các invariant trên.

**6. [P1 — giới hạn mô hình] Một `locked_until` không mô tả được nhiều lượt giữ phòng độc lập**

Nguồn: `database/01-init-schema.sql:409`.

Một bản ghi tổng hợp theo hạng phòng/ngày có counter `locked_rooms` và một thời điểm `locked_until`. Ví dụ khách A giữ đến 10:05, khách B giữ đến 10:08: counter bằng hai nhưng không có thông tin để biết phòng nào phải trả về lúc 10:05. Đổi deadline theo khách B giữ quá lâu lượt A; dùng deadline A có thể giải phóng sớm lượt B.

Nếu hỗ trợ nhiều lượt giữ phòng có TTL riêng, cần dữ liệu owner và thời hạn của từng lượt giữ, còn bảng tổng hợp giữ vai trò counter. Chỉ thiết kế các trường cần cho flow giữ/xác nhận/hủy sau khi contract nghiệp vụ được chốt. Nếu không có hold riêng từng khách, cần định nghĩa lại ý nghĩa counter/deadline; không dùng một deadline tổng hợp như deadline của từng reservation.

**7. [P1 — chưa có implementation để xác minh] Unique và `version` chưa tạo thành flow chống double booking**

Nguồn: `database/01-init-schema.sql:429`, `:439`, `:947`; toàn bộ `backend/src/main/java`.

Chưa có code giao dịch giữ phòng, tăng counter, ghi slot, xác nhận, hủy hoặc retry. Cột `version` không tự động so sánh/tăng chỉ vì được khai báo trong SQL; cần JPA `@Version` hoặc UPDATE có điều kiện version và kiểm tra số dòng ghi được.

Với sparse inventory, SELECT FOR UPDATE không khóa được một row chưa tồn tại. PostgreSQL khóa các row thực sự được SELECT trả về; cần xử lý cạnh tranh tạo row đầu tiên bằng unique/upsert và chiến lược giao dịch thích hợp. Unique slot chặn hai row trùng khóa nhưng không chặn việc UPDATE cùng row sang một booking khác. Tham chiếu: [row locking của PostgreSQL 16](https://www.postgresql.org/docs/16/explicit-locking.html).

Cần một flow ghi inventory thống nhất: cập nhật có điều kiện đủ tồn kho, ghi booking/slot/snapshot trong transaction, rollback toàn bộ nếu một đêm thất bại, và khóa theo thứ tự ổn định để giảm deadlock. Phải định nghĩa cách đồng bộ `room_availability` với `room_slots`, trạng thái maintenance và hủy booking. Giữ sparse storage; không pre-generate row chỉ để né vấn đề khóa. Chưa thể kết luận có lỗi double booking đang xảy ra khi chưa có service để chạy.

**8. [P1 nếu dùng làm tồn kho bán — dữ liệu seed xác định] Tổng tồn kho không khớp phòng vật lý**

Nguồn: `database/06-seed-data-catalog-and-room-configuration.sql:127`; `database/07-seed-data-inventory-mapping-intance.sql:172`.

| Khách sạn | Tổng `total_quantity` | Phòng vật lý được seed |
|---|---:|---:|
| Hà Nội | 32 | 12 |
| Đà Nẵng | 50 | 6 |
| Sapa | 35 | 5 |
| Tổng | 117 | 23 |

Đà Nẵng SUP khai báo 30 phòng nhưng chỉ có ba room instance. Nếu fallback khi thiếu availability row dùng `total_quantity`, kết quả bán phòng sẽ vượt số phòng có thể phân trong bộ dữ liệu này.

Nếu room instance seed chỉ là một phần mẫu, cần ghi rõ dataset chưa dùng được để kiểm tra phân phòng đầy đủ. Với dataset nhất quán, chọn một nguồn tính tồn kho vật lý, hoặc định nghĩa rõ inventory bán độc lập và chính sách overbooking. Hiện không có code/trigger đồng bộ khi thêm, xóa hoặc chuyển hạng phòng. `room_availability.total_rooms` cũng là một bản sao theo ngày cần quy tắc cập nhật rõ.

**9. [P1 — khoảng trống schema] NULL có thể làm lọt rule qua exclusion constraint**

Nguồn: `database/01-init-schema.sql:639`, `:666`, `:699`, `:730`; `database/02-functions-and-triggers.sql:143`, `:154`, `:168`, `:179`.

`is_deleted` tại các bảng rule/campaign có DEFAULT FALSE nhưng thiếu NOT NULL. Nếu INSERT ghi rõ NULL, DEFAULT không được áp dụng và predicate `WHERE (is_deleted = FALSE)` không chọn row đó vào exclusion constraint. Như vậy một rule có thể chồng thời gian mà không bị chốt này chặn.

Ngoài ra 24 bảng có cột `status` chưa có CHECK giới hạn tập giá trị; nhiều status và timestamp/boolean bắt buộc về mặt vận hành vẫn nullable. CHECK `status IN (...)` ở một số bảng khác cũng không thay cho NOT NULL. PostgreSQL chấp nhận CHECK có kết quả NULL/unknown; xem [tài liệu constraint](https://www.postgresql.org/docs/16/ddl-constraints.html).

Cần backfill NULL theo chính sách dữ liệu đã xác minh rồi đặt NOT NULL cho flag/status cần thiết. Các status nghiệp vụ phải có tập giá trị riêng; không dùng `ActiveStatus` cho booking, invoice và payment. Không tự backfill mọi status thành ACTIVE.

**10. [P1 cho booking năm 2027 — dữ liệu seed xác định] VAT thiếu coverage và không chặn policy chồng lấp**

Nguồn: `database/04-seed-data-global-master-data.sql:225`; `database/01-init-schema.sql:176`; `database/08-pricing-engine.sql:76`, `:105`, `:156`.

Seed có một rule VAT `ROOM`, kết thúc ngày 31/12/2026, trong khi nhiều rule giá/phụ thu chạy đến 30/06/2027. Nếu engine tra theo ngày lưu trú, từ 01/01/2027 không có VAT ROOM tương ứng trong seed. Nếu hệ thống chọn ngày tính thuế khác, cần ghi rõ contract đó và kiểm tra coverage theo ngày đã chọn.

`vat_rules` kiểm tra start/end của từng dòng, nhưng không chặn hai rule cùng tax category có thời gian trùng nhau. `hotel_age_policies` cũng chỉ kiểm tra min <= max, không chặn dải tuổi chồng lấp/âm hoặc tuổi không được phân loại.

Cần xử lý thiếu và nhiều policy bằng lỗi nghiệp vụ rõ, bổ sung dữ liệu có căn cứ và constraint cho policy đang hiệu lực. Không mặc định VAT bằng 0 hoặc lấy một dòng bất kỳ bằng LIMIT 1. Review này xác định vấn đề coverage dữ liệu; không xác nhận tính pháp lý của các mức thuế được seed.

**11. [P1 — khoảng trống bảo vệ lịch sử] Snapshot tài chính chưa được bảo vệ sau khi chốt/phát hành**

Nguồn: `database/01-init-schema.sql:882`, `:905`, `:1001`, `:1020`, `:1072`; `database/02-functions-and-triggers.sql:14`.

Schema có snapshot nhưng không có guard chặn UPDATE/DELETE phần giá/thuế/phí khi đã chốt, và backend chưa có guard thay thế. Trigger `updated_at` chỉ ghi giờ thay đổi. Xóa một invoice có thể cascade xóa `invoice_details`; snapshot giá cũng có thể sửa trực tiếp.

Cần xác định chính xác thời điểm freeze, cho phép sửa DRAFT và bảo vệ số liệu đã phát hành. Nếu có điều chỉnh/refund, ghi sự kiện điều chỉnh thay vì sửa giá lịch sử. Có thể kết hợp service guard, quyền DB và trigger cho invariant quan trọng. Không cần cấm mọi thay đổi metadata và không cần ép mọi bảng thành append-only. Migration phải phân loại trạng thái cũ trước khi áp dụng guard.

**12. [P1 trước payment callback — khoảng trống schema] Chưa có chốt idempotency cho thanh toán**

Nguồn: `database/01-init-schema.sql:958`, `:984`.

`payments.transaction_reference` và `transactions.reference_code` không có uniqueness/idempotency constraint. Schema chấp nhận ghi lặp một giao dịch ngoài hệ thống; retry/callback lặp có thể tạo nhiều bản ghi cùng sự kiện nếu service tương lai không xử lý.

Cần xác định identity của sự kiện theo provider và contract callback, lưu identity ổn định và unique theo phạm vi phù hợp; ghi payment/ledger/status trong một transaction. Không unique `booking_id`, vì một booking có thể thanh toán nhiều lần. Phải phân biệt mã giao dịch, mã event và refund; không áp một unique key cho mọi trường hợp khi chưa biết semantics. Việc refund còn cần kiểm soát tổng hoàn và concurrency theo payment gốc.

**13. [P2 — khoảng trống validation] Số khách, số lượng và tiền chưa được ràng buộc đủ**

Nguồn: `database/01-init-schema.sql:301`, `:814`, `:882`, `:958`, `:1001`, `:1020`.

- `adult_count`, `child_count`, `infant_count` không có CHECK không âm; `guest_count` chỉ kiểm tra > 0, không kiểm tra cách đối chiếu với từng nhóm khách.
- Sức chứa/giường của hạng phòng còn thiếu giới hạn không âm; `max_adults >= standard_adults` vẫn có thể đúng khi cả hai âm.
- `discount_rules` chỉ kiểm tra discount > 0; mức PERCENT > 100 vẫn hợp lệ ở DB.
- Nhiều base price, unit price, amount và rate trong `booking_daily_rates` không có CHECK miền giá trị; subtotal/fee/VAT/total có thể mâu thuẫn.

Cần chốt count tính theo mỗi phòng hay toàn booking detail trước khi thêm phép đối chiếu, và chốt tiền trước/sau thuế, cơ sở tính service fee, thứ tự discount/surcharge và rounding. Dùng CHECK cho invariant cùng row, validation giao dịch cho tổng nhiều row. Không áp CHECK amount >= 0 vào mọi ledger nếu nghiệp vụ cho phép adjustment âm; phải định nghĩa quy ước dấu trước.

**14. [P2 — mâu thuẫn seed với quy tắc được ghi trong source] Giường phụ vượt trần vật lý**

Nguồn: `database/01-init-schema.sql:325`, `:487`; `database/06-seed-data-catalog-and-room-configuration.sql:142`, `:148`, `:164`, `:176`; `database/07-seed-data-inventory-mapping-intance.sql:6`.

Source ghi quy tắc `base_quantity + extra_beds <= max_beds`. Nhưng Hà Nội DLX/STE và Đà Nẵng SUP có một giường cơ bản, một giường phụ, trần một; Đà Nẵng FAM có hai giường cơ bản, một giường phụ, trần hai. Bốn cấu hình này mâu thuẫn với quy tắc đã ghi.

Cần sửa seed hoặc làm rõ `max_beds` có tính giường phụ không. Nếu giữ nghĩa trần vật lý hiện tại, validation phải tính tổng giường cơ bản trên tất cả loại giường, không chỉ từng dòng `hotel_room_type_beds`. Không thể dùng CHECK thông thường để truy vấn tổng của bảng khác.

**15. [P2 — thiết kế dư và thiếu toàn vẹn] Polymorphic reference chưa tận dụng bảng cha menus**

Nguồn: `database/01-init-schema.sql:251`, `:271`, `:279`, `:567`, `:1023`; `database/06-seed-data-catalog-and-room-configuration.sql:117`.

Inclusions dùng `reference_type/reference_id` dù product/service đã chia sẻ ID ở bảng `menus`; `reference_id` không có FK, không có unique theo room type/item và `quantity` có thể âm. Một menu cũng có thể tồn tại ở cả `catalog_items` lẫn `services`, hoặc không có subtype; `menus.menu_type` chưa có CHECK bảo vệ.

Có thể dùng FK về bảng cha menus và suy subtype từ menu nếu contract xác nhận cùng semantics; điều này giảm reference đa hình không cần thiết. Với `invoice_details.reference_id`, các nguồn khác loại vẫn cần quy tắc xác minh nguồn và chống ghi cùng nguồn hai lần nếu nghiệp vụ yêu cầu. Chưa nên tạo một framework reference tổng quát cho cả dự án chỉ để giải quyết các bảng này.

`services.pricing_type` được chú thích bằng PER_STAY/PER_NIGHT/PER_PERSON nhưng seed dùng PER_USE; DB hiện chấp nhận vì VARCHAR không có CHECK. Đây là từ vựng contract cần thống nhất, chưa phải lỗi SQL constraint đang tái hiện.

**16. [P2 — toàn vẹn phạm vi quyền] Staff scope không có referential integrity**

Nguồn: `database/01-init-schema.sql:94`, `:118`; `backend/src/main/java/vn/edu/utc/hotel_booking/common/config/SecurityConfig.java:26`.

CHECK hiện chỉ xác minh scope type và việc NULL/non-NULL. Nhân viên PROPERTY/REGION có thể trỏ `scope_entity_id` tới ID không tồn tại; xóa region cũng không có FK từ scope để phát hiện liên kết này. Tập role/permission đã seed nhưng backend chưa thực thi data scope/RBAC tương ứng; SecurityConfig hiện yêu cầu authentication cho các endpoint còn lại.

Cần một mô hình scope có khả năng xác minh target tồn tại, cùng chính sách role/scope trong service dùng chung. Có thể chấp nhận reference đa hình nếu validation và lifecycle được thiết kế rõ, hoặc dùng FK tường minh khi nghiệp vụ cần. Không coi các bảng permission là bằng chứng hệ thống đã phân quyền đầy đủ, và không thêm RLS phức tạp trước khi có truy vấn/contract cụ thể.

**17. [P2 — cần đo khi có workload] Index còn trùng và thiếu ở các liên kết giao dịch**

Nguồn: `database/03-indexes-and-seed-data.sql:11`, `:15`, `:28`; `database/01-init-schema.sql:89`, `:339`, `:849`.

`idx_staffs_keycloak_id` trùng khóa với index tự tạo bởi UNIQUE `staffs.keycloak_id`. Index riêng `hotel_room_types(hotel_id)` và `booking_rooms(booking_detail_id)` có khóa đã là prefix của unique index, nên là ứng viên xem xét loại bỏ sau khi đo; không khẳng định mọi index prefix đều vô ích vì kích thước và workload có thể khác.

Các FK giao dịch chưa có index theo cột dẫn đầu đáng xem xét gồm `bookings.hotel_id`, `booking_rooms.room_instance_id`, `booking_guests.booking_room_id`, `room_slots.booking_room_id`, `transactions.payment_id/booking_id` và `service_order_details.service_order_id`. Điều này ảnh hưởng query theo owner và kiểm tra FK khi cập nhật/xóa parent ở dữ liệu lớn. PostgreSQL không tự tạo index cho phía tham chiếu của FK; xem [tài liệu FK](https://www.postgresql.org/docs/16/ddl-constraints.html).

Cần thiết kế index từ query thực tế, cân nhắc key khách sạn/trạng thái/ngày và xác minh bằng EXPLAIN khi có workload. Không thêm hàng loạt index đơn cho mọi flag/status chỉ để đạt checklist.

**Vận hành: [P1 trước khi nâng cấp database có dữ liệu] Chưa có migration được quản lý phiên bản**

Nguồn: `docker-compose.yml:16`; `backend/pom.xml:27`; `backend/src/main/resources/application.yaml:18`.

Schema chỉ được mount vào `/docker-entrypoint-initdb.d`; không có Flyway/Liquibase hoặc runner migration tương đương. PostgreSQL Docker image xử lý init script khi data directory chưa được khởi tạo; sửa script rồi restart không nâng cấp volume đã có dữ liệu. Nếu init lỗi giữa chừng, lần khởi động sau có thể bỏ qua init vì data directory đã tồn tại. Hành vi được mô tả trong [tài liệu image PostgreSQL chính thức](https://github.com/docker-library/docs/blob/master/postgres/README.md).

Các file hiện cũng không được bao toàn bộ bằng transaction tường minh. Chạy lại thủ công không phải giải pháp nâng cấp an toàn: script 04 có seed đụng unique, script 06 chèn thêm menus, script 08 chèn surcharge không xử lý conflict; script 07 còn ghi đè current_status về giá trị seed. Vì vậy không chạy lại toàn bộ bộ seed trên database có dữ liệu nghiệp vụ.

Cần baseline schema hiện tại, migration có version/checksum, tách dữ liệu mẫu khỏi master data bắt buộc và mỗi thay đổi có kế hoạch transaction/backfill. Giữ `ddl-auto: validate` làm bước đối chiếu mapping, nhưng không dùng nó thay migration. Với backend chưa có Entity nghiệp vụ, Hibernate cũng chưa đối chiếu 44 bảng với model ứng dụng. Thêm IF NOT EXISTS khắp nơi không chứng minh schema cũ đã đúng và có thể che schema drift.

**Điểm ngoài database có ảnh hưởng đến kiểm tra backend**

- `GlobalHandlerException.java:32` trả HTTP 400 cho RuntimeException dù `UNCATEGORIZED_EXCEPTION` định nghĩa HTTP 500. Lỗi DB không được phân loại sẽ có status dễ khiến client hiểu sai nguyên nhân.
- `GlobalHandlerException.java:3` import `java.nio.file.AccessDeniedException`, không phải exception phân quyền của Spring Security. Handler này chưa bảo đảm bắt được lỗi authorization; lỗi trong filter chain còn cần handler của Security tương ứng.
- Các handler optimistic/pessimistic lock tại `GlobalHandlerException.java:92` và `:110` đang comment. Khi triển khai booking phải thống nhất conflict/retry và response, không chỉ có version column.
- Backend test hiện chỉ có `contextLoads`. Chưa có integration test nạp schema thật, kiểm tra seed hoặc transaction nghiệp vụ. Frontend chưa gọi API nên chưa thể dùng UI làm bằng chứng schema/contract đúng.
- README cho phép Node 18+, nhưng lockfile Vite 8.2.1 yêu cầu Node `^20.19.0 || >=22.12.0`. Đây là lệch yêu cầu setup; build trên Node 24 hiện tại đạt.

**Thứ tự xử lý đề xuất**

1. Sửa lỗi seed inclusions và sinh mã; tạo dataset inventory/bed/VAT nhất quán để làm nền kiểm tra.
2. Chốt flow booking và owner snapshot: khi nào xác nhận, khi nào phân phòng, khi nào freeze giá.
3. Chốt invariant cùng khách sạn/booking, bổ sung NOT NULL/UNIQUE/CHECK và xử lý dữ liệu cũ bằng migration.
4. Thiết kế flow inventory/hold theo transaction, xử lý sparse row đầu tiên và đồng bộ counter/slot.
5. Hoàn thiện payment idempotency, refund và bảo vệ chứng từ đã phát hành.
6. Bổ sung migration runner và kiểm tra trên PostgreSQL 16 thật; sau đó mới tối ưu index theo query.

Không cần viết lại toàn bộ 44 bảng. Nên giữ cấu trúc danh mục/phòng/booking hiện tại, sửa ownership và invariant trước; các thay đổi shared model cần được tái sử dụng cho tất cả module.

**Các quyết định nghiệp vụ còn thiếu**

- Có xác nhận booking khi chưa chọn phòng vật lý không? Đổi phòng xử lý giá/folio thế nào?
- `guest_count` tính theo từng phòng hay toàn dòng booking detail? Infant có tính vào tổng sức chứa không?
- Inventory bán có thể cao hơn inventory vật lý không, hay `total_quantity` luôn phải khớp số phòng đang được phép bán?
- Rule giá lấy ưu tiên cao nhất hay cộng dồn? Nhiều promo code/campaign cùng thời gian có được phép không? Exclusion discount hiện chỉ phân biệt room type và rule type, nên chặn cả promo code khác nhau nếu chồng thời gian.
- Policy INACTIVE có còn giữ chỗ trong exclusion không? Predicate hiện xét is_deleted, chưa xét status; đây là quyết định lifecycle cần thống nhất, không tự sửa thành chỉ ACTIVE.
- Ngày chọn VAT, công thức service fee, cách làm tròn và identity của callback/refund là gì?

Các câu hỏi này cần được phản ánh vào contract backend trước khi thêm trường hoặc constraint phụ thuộc vào câu trả lời.

**Kết quả kiểm tra**

| Kiểm tra | Kết quả |
|---|---|
| Đọc source ứng dụng/cấu hình và 8 script SQL | Hoàn tất |
| Đếm seed và mở rộng liên kết inclusions từ các tập VALUES | 117 tồn kho; 23 phòng vật lý; 42 inclusion, 28 khác khách sạn |
| `npm.cmd run build` | Đạt trên Node 24.14.1 |
| `npm.cmd run lint` | Đạt |
| `mvn -o -DskipTests compile` | Chưa chạy được: parent Spring Boot 4.0.7 chưa có trong cache offline; không kết luận source compile lỗi |
| Bootstrap/constraint/catalog trên PostgreSQL 16 | Chưa chạy: Docker daemon không hoạt động, không có psql |
| Stress test booking/hold/payment | Chưa chạy: chưa có implementation nghiệp vụ |
| Query plan và hiệu năng thực tế | Chưa đo |

Đợt review này chỉ tạo báo cáo; không thay đổi source nghiệp vụ, SQL hoặc dữ liệu database.
