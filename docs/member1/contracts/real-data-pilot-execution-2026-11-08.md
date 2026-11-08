# Đối soát pilot dữ liệu thật Member 1 — 08/11/2026

**CONFIRMED** `develop` trên remote và HEAD worktree cùng ở `57b4017648761f6f770321d033221667e6d947ca`. Các thay đổi RAG và hồ sơ readiness từ trước được giữ lại. Các truy vấn dưới đây chạy trên PostgreSQL local, trong `BEGIN READ ONLY` với timeout 8 giây. Lượt này chỉ khởi động container PostgreSQL hiện có để đọc dữ liệu.

| Luồng | Đã xác minh | Điểm dừng |
|---|---|---|
| Core | `127.0.0.1:5433/hotel_booking_db`, PostgreSQL 16.15. `DATABASECHANGELOG` có 46 changeset, gồm 3 changeset `dev-seed` cho dữ liệu mẫu, lưu trú và điện nước. Thiếu `media_objects.is_primary` mà luồng đọc khách sạn tại SHA đích sử dụng. | Member 2 đối soát schema và xác nhận đích API, tài khoản Keycloak và quyền theo khách sạn. Không coi dữ liệu có trong DB là dữ liệu thật khi chưa có nguồn gốc. |
| T12 R1 | Nguồn DB ban đầu của `hotelId=4` không có hạng phòng hoặc lịch tồn. Sandbox cô lập hiện nạp 4 hạng phòng pilot, tổng số lượng 384 và 1.460 dòng tồn cho 365 ngày. `availableRooms` trong Core là số nhỏ nhất trên các đêm từ check-in đến trước check-out. | **PASS trong pilot** đường đi PostgreSQL → Core Availability/Pricing → Advisor API → thẻ frontend. Giá, số lượng và tồn là dữ liệu pilot tự tạo để thao tác, không phải dữ liệu vận hành production. |
| T12 R2 | `t12_pgvector_pilot_hl`, pgvector 0.8.7. Cả 5 nguồn trong `manifest-approved-five.json` tại `d28b657` có `HOTEL_SCOPED`, hotel `4`, version `1`, `ACTIVE`, mỗi nguồn 1 chunk, `local-multilingual-hashing-v1` 256 chiều. Hash của nội dung DB sau chuẩn hóa NFC/LF khớp hash lưu trong DB và manifest. Manifest không đặt ngày hiệu lực. | **PASS** đối soát DB và tái sử dụng kho. **NOT RUN** `/knowledge/ask` ở SHA đích, ca citation chéo scope và restore. Cần runtime dùng Core reader và datasource pgvector đã cấp. |
| T04 và U22 | Hotel `4` có 0 đồng hồ và 0 chỉ số. Hotel `1` có 5 đồng hồ/96 chỉ số và hotel `2` có 2/4, chưa chứng minh là bản ghi vận hành. | **BLOCKED** chờ danh mục, đơn vị, ngày đo, reset/thay đồng hồ và nguồn công suất vật lý cùng actual room-night theo ngày. |
| T05 và U13 | Hotel `4` có 0 StayGuest. Toàn DB có 4 StayGuest, 4 mục hàng đợi, 0 bản ghi payroll. Biên bản cũ ghi lỗi `lower(bytea)` trên API local. | **BLOCKED** chờ hồ sơ lưu trú được phép, tài khoản đúng scope, mẫu XLSX/CSV chính thức và ảnh được phép xử lý. Lỗi StayGuest chưa tái hiện trên HTTP của SHA đích. |
| U14 | Có 0 binding. Schema hiện chỉ chấp nhận `SYNTHETIC_FIXTURE`. | **BLOCKED** chờ hợp đồng định danh, lịch sử có quyền sử dụng và consent thật. |
| Preview đoàn | Chưa có bằng chứng về snapshot phòng vật lý, giường, tầng và quota từng đêm của pilot. | **BLOCKED** chờ nguồn snapshot đọc từ Core. |

## Mã và kiểm tra

- Adapter T12 dùng public eligibility reader và metadata của kết quả `searchUnified` khi chọn khách sạn. Thẻ chỉ được dựng khi metadata, tồn và báo giá khớp, gồm ID hạng phòng và số đêm của chính báo giá. Không đổi API công khai. Giới hạn ngân sách dạng `1.500.000 VND` được đọc đủ chữ số. Cụm “mỗi đêm” vẫn hỏi rõ nếu chưa biết khách muốn giới hạn từng đêm hay trung bình.
- **PASS** Maven offline, 164 kiểm tra, 0 lỗi và 0 skip. Gồm adapter, lỗi dependency, parser, 74 ca eval R1 giữ nguyên gold, kiểm kiến trúc chỉ đọc và hồi quy RAG. Lần đầu không đọc được JAR trong sandbox. Chạy lại với quyền đọc cache Maven và DB trỏ cổng vô hiệu thì thành công. Bằng chứng: `backend/target/m1-real-data-conversion-test.log`. Đây là kiểm tra mã, không phải nghiệm thu API trên DB thật.
- **NOT RUN** ghi dữ liệu, migration, stage/activate, Gemini, kết nối HTTP/UI và nghiệm thu các luồng nghiệp vụ. Giữ nguyên 5 nguồn đã đối soát. Tiếp theo cần cấu hình cấp quyền runtime cho API RAG và nguồn nghiệp vụ của hotel `4`, sau đó đối chiếu số bản ghi trước/sau từng đợt nhập.

## Kiểm tra API local sau đối soát

- **PASS** đóng gói backend từ worktree hiện tại và khởi động ở `127.0.0.1:18082` bên trong Docker. Runtime dùng hai role pilot hiện hữu qua xác thực loopback đã cấu hình trên PostgreSQL. Core reader có quyền ghi trên 0 bảng nghiệp vụ. Liquibase, scheduler, demo và Gemini đều tắt.
- **FAIL** smoke `/api/v1/public/ai/room-advisor/knowledge/ask` đạt 3/9 ca. Hỏi giá, tồn và yêu cầu tạo booking trả HTTP 200 `OUT_OF_SCOPE`. Sáu ca cần truy vấn tài liệu trả HTTP 503. PostgreSQL ghi `could not access file "$libdir/vector": No such file or directory`. Kho có metadata extension `0.8.7` và 5 nguồn `ACTIVE`, nhưng container `postgres:16-alpine` thiếu `vector.so`. Image local `pgvector/pgvector:pg16` có PostgreSQL 16.15 và vector 0.8.7. Cần diễn tập khôi phục kho RAG riêng trên runtime tương thích trước khi thay đổi đích đang dùng.
- **BLOCKED** tìm phòng thật qua `/api/v1/public/ai/room-advisor/search`. Hotel `4` trả HTTP 200 `TEMP_UNAVAILABLE`, coverage `PARTIAL`, 0 thẻ. PostgreSQL từ chối quyền đọc `room_types` đối với Core reader. Schema Core vẫn thiếu `media_objects.is_primary`. Không cấp thêm quyền hoặc cập nhật schema trong lượt này.
- **PASS trong sandbox cô lập** insert dữ liệu pilot T12 bằng `tools/member1/Seed-T12PilotData.sql`. Không ghi vào DB gốc. Dataset có nhãn pilot và được tạo lại khi chạy launcher; API chỉ đọc bằng role `m1_core_reader`.

## Môi trường local để thử T12 trên giao diện

Từ thư mục gốc, dùng PowerShell 7.5 trở lên, Docker Desktop, Java 21+, Maven và Node.js:

```powershell
pwsh -NoProfile -File tools/member1/Start-RealDataPilot.ps1
```

Mở [giao diện T12](http://127.0.0.1:13002/t12-pilot.html). Bấm **Mở Trợ lý Tìm phòng AI**, chọn **Chính sách & tiện ích**, chọn **Vinpearl Resort & Spa Hạ Long** rồi hỏi “Vinpearl Resort & Spa Hạ Long nằm trên đảo nào?”. Kỳ vọng thấy đảo Rều và nguồn `VP-HL-FACILITIES`. Với câu rút gọn “Khách sạn nằm trên đảo nào?”, model local hiện trả chưa đủ nguồn. Hỏi “Đêm mai Deluxe giá bao nhiêu?” để kiểm tra hướng dẫn sang luồng tìm phòng.

Launcher đọc hotel `4` và 5 nguồn đã duyệt từ DB gốc, đối chiếu SHA-256, chạy lại sanitizer và dựng vector 256 chiều bằng bộ embedding hiện hành. Bản sao local dùng pgvector 0.8.7, schema Core theo changelog `master-data`, 0 changeset `dev-seed`. Hai role của backend chỉ đọc. Phạm vi API được kiểm tra là T12 công khai, với một khách sạn và danh mục phòng trống. DB gốc vẫn dùng runtime thiếu `vector.so`. Các ca có giá, tồn, khách lưu trú hay dữ liệu điện nước thật vẫn chờ nguồn nghiệp vụ.

Kết quả: **PASS** 164 kiểm tra Maven, frontend lint/build, 9/9 ca HTTP RAG và 4/4 ca Playwright chạy với backend và PostgreSQL của bản sao. E2E kiểm citation, đổi phạm vi, chuyển câu hỏi giá, kết quả tìm phòng rỗng và hội thoại hai lượt. Số bản ghi nghiệp vụ và hash của corpus đối chiếu trước/sau E2E khớp. Bộ browser chung đã được dừng sau khi phát hiện lệnh npm chọn thêm các file ngoài phạm vi. Kết quả E2E trên đây dùng đúng file pilot.

Chạy lại E2E từ thư mục `frontend` khi dự án đang chạy:

```powershell
$env:T12_REAL_DATA_PILOT = 'true'
$env:VITE_BASE_URL = 'http://127.0.0.1:13002'
.\node_modules\.bin\playwright.cmd test e2e/browser/t12-real-data-pilot.spec.ts
```

Dừng frontend bằng Ctrl+C, rồi chạy `pwsh -NoProfile -File tools/member1/Start-RealDataPilot.ps1 -Stop` ở thư mục gốc. Lệnh chỉ dừng hai container có nhãn pilot và bỏ bản sao dùng cho phiên thử. Chạy lại để tạo bản sao mới.

## Nghiệm thu mục Tìm phòng trong chatbot

Mở lại [trang thử T12](http://127.0.0.1:13002/t12-pilot.html), bấm **Mở Trợ lý Tìm phòng AI** và chọn **Tìm phòng**. Trang hiển thị ví dụ ngày nhận và trả còn hiệu lực.

| Lượt | Tin nhắn | Kết quả cần thấy |
|---|---|---|
| 1 | Tìm phòng ở Hạ Long, nhận phòng 2026-11-10 cho 2 người lớn, ngân sách tối đa 1.500.000 VND mỗi đêm | Chatbot hỏi ngày trả phòng. |
| 2 | Trả phòng 2026-11-12 | Chatbot giữ ngày nhận đã nhập và hỏi cách tính ngân sách. |
| 3 | Trung bình mỗi đêm theo báo giá hệ thống | Giữ số tiền 1.500.000 VND. API trả `NO_MATCH` vì ngân sách thấp hơn toàn bộ báo giá Core, không phải vì thiếu dữ liệu. |
| 4 | Giữ phòng này giúp tôi | Chatbot từ chối thao tác giữ phòng. Phiên và phiên bản hội thoại giữ nguyên. |
| 5 | Đổi thành 1 người lớn | Tìm lại trong cùng phiên, dùng số khách mới và giữ nguyên kỳ lưu trú. |

Advisor lấy báo giá từ Core theo số khách đã nhập nếu cấu hình khách khác báo giá danh mục. Kiểm tra mã adapter đã xác nhận ca 1 người lớn và 1 trẻ em gọi đúng hợp đồng Core. Với yêu cầu không giới hạn ngân sách, hotel `4` trả 4 thẻ từ dữ liệu pilot trong PostgreSQL; giá gồm service fee 5% và VAT theo Pricing Core.

**PASS** Maven Advisor/RAG 347 ca đạt, 4 ca Redis thật skip, 0 lỗi. Lượt thay đổi dataset chạy lại 107 ca T12 trọng tâm, 0 lỗi. Frontend lint và build đạt. **PASS** 9/9 ca Playwright qua API và PostgreSQL của pilot. Các ca kiểm prompt mẫu lần đầu ở viewport 375px, thẻ phòng lấy từ DB, lọc hard budget còn đúng một thẻ, requote tại trang chi tiết, ngày trả, làm rõ ngân sách và từ chối giữ phòng mà vẫn tiếp tục phiên. Các phép đối chiếu trước/sau E2E cho thấy catalog, lịch tồn, số bản ghi nghiệp vụ và hash corpus khớp. Không sửa logic Booking/Pricing/Inventory, IAM hay DB gốc.

## Đối soát nguồn hạng phòng, giá và tồn — 08/11/2026

- **CONFIRMED** các khách sạn `1`–`3` có hạng phòng và lịch tồn nhưng các bản ghi này được tạo bởi `05-dev-seed.sql`; không dùng làm dữ liệu nghiệm thu thật. Hai file backup Pricing ngày 30/09 cũng tham chiếu các ID của bộ dữ liệu này và không có provenance nghiệp vụ độc lập.
- **CONFIRMED** website chính thức của Vinpearl công bố danh sách hạng phòng, diện tích, sức chứa và tiện nghi của Vinpearl Resort & Spa Hạ Long. Báo cáo thường niên chính thức xác nhận tổng quy mô 384 phòng. Các nguồn này không công bố phân bổ số lượng theo từng hạng hoặc lịch tồn từng ngày.
- **CONFIRMED** trang giá chính thức nêu giá thay đổi theo thời gian, lịch trình và dịch vụ, đồng thời hướng người dùng sang hệ thống booking để lấy giá theo kỳ. Giá “từ” trên trang giới thiệu không đủ làm báo giá Core cho một kỳ lưu trú cụ thể.
- **CONFIRMED theo yêu cầu người dùng** dữ liệu nghiệm thu có thể là dữ liệu pilot tự tạo, miễn được lưu trong DB và frontend truy xuất qua API thật. Dataset cô lập vì vậy phân bổ tổng 384 phòng vào 4 hạng, đặt giá cơ sở VND và lịch tồn 365 ngày; các giá trị này được ghi rõ là pilot, không được trình bày như dữ liệu vận hành của Vinpearl.

Dataset pilot hiện có định danh hạng phòng ổn định, sức chứa, `basePrice` VND, service fee 5%, VAT từ master-data Core, tổng số phòng theo hạng và `totalRooms`, `bookedRooms`, `lockedRooms`, `oooRooms` cho từng ngày. E2E lưu checksum catalog/tồn trước và sau để chứng minh chatbot không ghi dữ liệu nghiệp vụ.
