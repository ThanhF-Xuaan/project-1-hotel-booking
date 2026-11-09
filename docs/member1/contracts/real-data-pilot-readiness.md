# Hồ sơ pilot dữ liệu thật của Member 1

**Mốc đối chiếu** — GitHub develop tại `57b4017648761f6f770321d033221667e6d947ca`, ngày 08/11/2026. Phạm vi đề xuất là một khách sạn trên staging hoặc sandbox riêng do owner xác nhận. Mỗi dòng dưới đây cần ghi nhận nguồn dữ liệu và phiên bản, người duyệt, đích kết nối, quyền theo khách sạn, thời hạn lưu và cách thu hồi trong biên bản của môi trường. Lưu thông tin kết nối nhạy cảm và backup ngoài Git.

## Nguồn dữ liệu và điều kiện mở pilot

| Luồng | Nguồn và trường cần dùng | Tài khoản, lưu giữ và thu hồi | Điều kiện còn mở |
|---|---|---|---|
| T12 — tài liệu công khai | Manifest 5 nguồn `VP-HL-DELUXE`, `VP-HL-DINING`, `VP-HL-WEDDING`, `VP-HL-MEETING`, `VP-HL-FACILITIES`. Biên nhận sandbox ghi `HOTEL_SCOPED`, hotel `4`, version `1`, model `local-multilingual-hashing-v1`, 256 chiều. | API hỏi đáp công khai chỉ đọc nguồn đủ điều kiện. Tách Core reader, RAG runtime và quyền quản trị kho theo phê duyệt. Owner chốt ngày hiệu lực, chu kỳ rà soát và thời hạn giữ phiên bản. Diễn tập restore vào DB riêng để thu hồi phiên bản đang ACTIVE. | **CONFIRMED** hash NFC/LF của 5 Git blob tại `d28b657` khớp manifest và biên nhận ingest cũ. **BLOCKED** xác minh hiện trạng DB, mapping hotel của đích mới, grants, restore và nghiệm thu HTTP. |
| T04 — đồng hồ và chỉ số | Một danh mục đã duyệt gồm hotel, mã, loại, vị trí, đơn vị, ngày đo, giá trị và bằng chứng reset hoặc thay đồng hồ. Mẫu chưa có phiên bản hoặc checksum được xác nhận. | Kỹ thuật và quản lý có scope PROPERTY. REGION_MANAGER đọc trong vùng. Chủ dữ liệu chốt nơi giữ mẫu và thời hạn lưu. Thu hồi quyền truy cập mẫu khi kết thúc pilot. | **BLOCKED** chờ mẫu và scope pilot. Đối chiếu qua API filter của meter và reading trước khi ghi thử trên dữ liệu giả lập. |
| T05 — lưu trú và lương | Chủ dữ liệu duyệt riêng mẫu XLSX, mã danh mục, cutoff và mẫu CSV, kỳ lương, đơn vị tiền. Chỉ dùng trường cần thiết của từng mẫu. | Lễ tân và quản lý truy cập theo scope đã duyệt. Quyền import lương theo API hiện hành chỉ gồm CHAIN_ADMIN và PROPERTY_MANAGER. Chủ dữ liệu chốt thời hạn lưu file, xử lý bản xuất và thu hồi quyền. | **BLOCKED** chờ mẫu chính thức, scope IAM và phê duyệt sử dụng dữ liệu. |
| U13 — ảnh giấy tờ | Dùng ảnh giả lập hoặc được cấp phép cho kiểm thử. Chốt bộ trường trích xuất và nơi xử lý Tesseract `vie+eng`. | Lễ tân chỉ nhận bản nháp OCR, kiểm tra rồi mới gửi qua luồng StayGuest. Chủ dữ liệu chốt thời hạn lưu và cách xóa ảnh/tệp tạm theo môi trường. | **BLOCKED** chờ owner check-in, Member 2 và chủ dữ liệu duyệt image runtime, quyền và chính sách lưu giữ. |
| U22 — điện nước và công suất | Chuỗi đọc theo ngày cần đơn vị, mốc reset/thay và nguồn đã đối soát. Công suất cần số phòng vật lý đủ điều kiện và actual room-night theo ngày địa phương. | Adapter chỉ đọc trong scope server xác thực. Chủ dữ liệu duyệt chu kỳ giữ snapshot và cách thu hồi nguồn. | **BLOCKED** chờ nguồn có thể xác minh. Thực hiện U22 điện nước sau T04. Dùng `UtilityReadingHistoryPort` và `OccupancyHistoryPort` hiện có. |
| U14 — cá nhân hóa | Binding `issuer/sub`, lịch sử Booking và consent theo hotel, mục đích, phiên bản. | Identity/Booking duyệt quyền sử dụng và thời hạn lưu. Revoke hoặc relink phải chặn kết quả đang tính trước khi trả. | **BLOCKED** chờ hợp đồng khách thật. DB hiện giới hạn `SYNTHETIC_FIXTURE`. |

Member 2 cần đối chiếu SHA ứng dụng, định danh datasource và lịch sử `DATABASECHANGELOG` với đích thực tế. Xác minh context DB nghiệp vụ theo cấu hình deploy là `master-data`, kiểm tra `dev-seed` tắt. Chỉ cấp `@rag-pgvector` cho đích RAG đã duyệt. Backup cần khôi phục thử vào DB riêng và kiểm lại định danh, dữ liệu, quyền và khả năng chặn ghi. DDL trong PR #72 là tài liệu đối chiếu.

## Yêu cầu review theo owner

### Member 2 — StayGuest và provenance

- **PROPOSED** tái hiện `POST /api/v1/stay-guests/filter` trên PostgreSQL và Keycloak của sandbox được duyệt. Kiểm body hợp lệ với keyword vắng, `null`, rỗng, khớp tên/số giấy tờ giả lập và ngoài scope. Dữ liệu kiểm thử phải có ít nhất một kết quả khớp và một bản ghi ngoài hotel để chứng minh bộ lọc giữ scope. Trường hợp ngoài scope kỳ vọng HTTP 403. Dấu hiệu cần xác nhận trên base mới là `function lower(bytea) does not exist` từ đường Hibernate thật.
- Nếu tái hiện, giới hạn thay đổi ở query của `StayGuestRepository` và test liên quan. Ép `:keyword` thành kiểu chuỗi ở nhánh kiểm null và hai biểu thức `CONCAT`. Giữ lọc hotel, booking/phòng, trạng thái và phân trang hiện hành. Xác minh hồ sơ staff tương ứng với Keycloak subject/username có PROPERTY scope hợp lệ. Dùng quy trình provisioning IAM được duyệt cho tài khoản thử nghiệm.
- Hai bộ đối soát source hiện báo lệch ở `SecurityConfig.java` và `ErrorCode.java`. Yêu cầu Member 2 xác nhận delta đã merge và duyệt biên nhận cập nhật cho SHA đích. Giữ nguyên eval, gold và ngưỡng.

### Member 4 — T12 và T04

- **T12** review phần `HotelKnowledgePanel`, `hotelKnowledgeService` và test FAQ trong patch ngày 06/11. Giữ envelope `OUT_OF_SCOPE` với hướng dẫn dùng chức năng tìm phòng cho giá/tồn phòng. Giữ xử lý citation theo hotel, bỏ response trễ, và kiểm 429/503. Tách UI khỏi wiring Gemini.
- **T04** dùng commit `39240446dd76512a2076b0c37a80b432d9cde32c` làm tham chiếu cho màn hình, form, service, route và sidebar. Giữ nhánh PROPERTY của `ENGINEERING` dùng hotel đã xác thực. API danh sách `/api/v1/hotels/filter` hiện dành cho các role có quyền tương ứng, gồm CHAIN_ADMIN và REGION_MANAGER. Cùng IAM kiểm lại contract tự tra staff qua `/api/v1/staffs/keycloak/{keycloakId}`. Source ở base dùng `isAuthenticated()` và lookup theo UUID, cần xác nhận thực tế rằng tài khoản không đọc được hồ sơ staff khác phạm vi. Nếu cần, đề xuất contract self-scope chỉ trả loại scope và ID thay vì cấp thêm quyền.
- API T04 dùng `POST /filter`, `GET /{id}`, `POST`, `PUT /{id}` và `DELETE /delete` với mảng ID trên resource meter và reading. Đối chiếu DTO và kiểm mã đồng hồ tối đa 50 ký tự, chỉ số tối đa 9 chữ số nguyên và 3 chữ số thập phân, ngày đo không ở tương lai. Browser và API phải chứng minh ENGINEERING được thao tác đúng hotel, REGION_MANAGER chỉ đọc, sai scope bị từ chối, trùng chỉ số bị chặn và xóa mềm đúng contract. Tắt mục điều hướng pilot nếu nghiệm thu lỗi.

### Core — preview phân phòng đoàn

**PROPOSED** cung cấp snapshot đọc qua `GroupAssignmentDataPort`. Phạm vi gồm hotel/contract, một khoảng đêm lưu trú với checkout loại trừ, `observedAt`/ `validUntil`, phòng vật lý có giường và tầng nếu xác minh được, trạng thái đủ điều kiện từng phòng/đêm và quota từng block/loại phòng/đêm. Core xác định quota và trạng thái. Scope lấy từ tài khoản trên server. Ca nghiệm thu gồm thiếu quota giữa kỳ, quota bằng 0, sai scope, snapshot hết hạn, phòng thiếu thông tin tầng và quyền bị thu hồi trong lúc đọc. Thiếu hoặc mâu thuẫn dữ liệu thì dừng preview theo contract. Giữ thao tác áp phân phòng ở Booking. [Contract local hiện có](group-room-assignment-preview-local-v1.md)

## Bàn giao kỹ thuật và điểm dừng

- **Đã tích hợp** guard RAG và hồi quy trên SHA đích. Câu hỏi giá và tồn phòng khớp các mẫu đã kiểm được chuyển hướng trước retrieval. Câu hỏi thú cưng chỉ dùng chunk có bằng chứng về chủ đề. Status, DTO và API Core giữ theo contract hiện có. Có thể rollback bằng hoàn nguyên riêng phần guard và test mới trước khi đưa vào pilot.
- **Chờ runtime RAG** đối chiếu 5 nguồn với manifest trên đúng kho, kiểm Core hotel eligibility, thử scope khác, 503 và chạy lại R1. Diễn tập restore snapshot vào DB riêng trước khi thử thu hồi phiên bản ACTIVE. Ghi kết quả HTTP, phiên bản, hash, citation và dấu vết zero business writes vào biên bản. Giữ thử nghiệm Gemini ở đợt riêng sau khi chốt model, dimension, prompt, corpus, ngân sách, tập dev và gate acceptance.
- **Chờ nghiệm thu T05/U13** dùng API thật với fixture giả lập trước khi dùng dữ liệu được duyệt. T05 kiểm một batch thắng khi export đồng thời, rollback khi ghi file lỗi, CSV hỗn hợp và độ chính xác `NUMERIC(15,2)`. U13 kiểm ảnh vượt giới hạn, timeout, dọn dữ liệu tạm và log/audit không chứa ảnh, số giấy tờ hoặc nội dung OCR. Xác minh riêng ảnh hưởng giới hạn multipart mới tới các endpoint dùng chung.


## Bằng chứng kiểm thử tại SHA đối chiếu

| Kiểm tra | Kết quả |
|---|---|
| Maven lọc các bộ Operation, Finance, Advisor, OCR và RAG trước sửa | **PASS** 649 ca, 17 skip phụ thuộc môi trường. |
| `RagReviewRegressionTest` trước sửa guard | **FAIL** 20/40 ca. Lỗi tái hiện nằm ở câu hỏi giá/tồn phòng và bằng chứng thú cưng. |
| Maven offline toàn backend trên bản sửa cuối | **PASS** 1.727 ca, 18 skip. Gồm 42 ca hồi quy RAG, 74 ca eval R1 và 32 ca an toàn RAG. |
| Maven OCR với Tesseract `vie+eng` cục bộ | **PASS** 74 ca, 1 ca Keycloak skip. Bao gồm OCR ảnh giả lập, multipart servlet, bộ kiểm tra upload và kiểm tra riêng tư. |
| Frontend trên develop | **PASS** lint, build và 15 ca Playwright với API giả lập, gồm FAQ 503, Advisor 429 và OCR có xác nhận của nhân viên. Cả hai phần diff UI T12 và T04 qua `git apply --check` trên SHA đích. |
| `validate_r1_contracts.py --artifacts-only` | **PASS** 184 instance và 74 case. Hai kiểm tra provenance source đầy đủ **FAIL** tại `SecurityConfig.java` và `ErrorCode.java` trên base. |
| `Verify.ps1 -IncludeM1AdditionalTests -IncludePersistentRag`, StayGuest HTTP/PostgreSQL, restore kho pilot, Gemini live | **NOT RUN**. Chờ xác nhận sandbox hoặc staging và phê duyệt theo từng luồng. |

Chạy Maven từ backend bằng `mvn.cmd -o "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" '-Dacceptance.integration=false' '-DfailIfNoTests=true' test` vì wrapper Windows lỗi khởi tạo. Tiến trình test tắt Liquibase, JPA DDL và Gemini, dùng URL DB loopback cổng 1. Log tổng ở `backend/target/m1-backend-offline-final.log`. Bộ OCR đặt thêm `TESSERACT_EXE` và dùng filter `-Dtest=*Ocr*Test,*Tesseract*Test,U13ZeroWriteAndPiiSafetyTest`. Log OCR ở `backend/target/m1-ocr-local.log`.

Khi bật OCR ảnh, `OcrMultipartPrivacyGuard` yêu cầu tối đa 5 MiB mỗi file và 6 MiB mỗi request, với ngưỡng giữ trong bộ nhớ ít nhất bằng giới hạn request. `application.yaml` của SHA đích đặt 10 MB và 20 MB nên bật engine bằng cấu hình mặc định sẽ bị guard chặn khi khởi động. Profile `ocr-local` đặt lại giới hạn cho toàn tiến trình. Member 2 và owner check-in cần kiểm cả upload ảnh khách sạn trước khi chọn cấu hình staging.
