# Backlog backend và chatbot AI sau khi có codebase

**Phạm vi:** 2 backend (BE1, BE2) và 1 người phụ trách AI; mục tiêu là hoàn thiện MVP quản lý nội bộ một chuỗi khách sạn, có luồng đặt phòng và thanh toán online **giả lập**, cùng chatbot RAG cho nhân viên. Đây là backlog công việc còn lại, không phải danh sách chức năng đã hoàn thành hay cam kết ngày bàn giao.

Tài liệu lấy [codebase và README hiện tại](../backend/README.md) làm mốc đã triển khai, tham chiếu [kế hoạch nhóm 5 người](ke-hoach-nhom-5-nguoi-quan-ly-chuoi-khach-san-rag.md) cho phạm vi MVP và [thiết kế API dự kiến](api-design-hotel-chain-v1.md) cho hướng contract. Các endpoint, bảng và trạng thái trong tài liệu API vẫn là **đề xuất** cho đến khi có migration, code và test tương ứng. Kế hoạch 4 người/thu tiền mặt trong `implementation-plan-backend-chatbot-frontend.md` phản ánh một phương án cũ, không được dùng để giao việc trong backlog này.

## 1. Mốc đã có và khoảng trống thực tế

| Đã có trong codebase | Chưa được xem là hoàn thành |
|---|---|
| Spring Boot modular monolith, Flyway `V001`–`V003`, Docker Compose, Keycloak JWT, quyền theo một role và một scope/nhân viên, HTTP error envelope, CI và test PostgreSQL/pgvector | Quy trình nâng cấp database cũ trên bản sao; dựng lại toàn bộ môi trường từ một clone sạch và backup/restore có bằng chứng |
| API đọc khách sạn/hạng phòng, khả dụng theo ngày, quote có breakdown; pricing service dùng chung cho REST và AI | Hold bền vững, khóa tồn khi cạnh tranh, booking, snapshot giá giao dịch, gán phòng vật lý và các lệnh vận hành |
| Kho tri thức text, job ingest, publish/revoke, vector search có lọc scope, chat một lượt có citation và tool quote có cấu trúc | Bộ tài liệu nghiệp vụ đã duyệt; metadata nguồn/ngày hiệu lực, thêm version mới và reindex; tool tìm phòng/khả dụng đầy đủ; đánh giá chất lượng trên bộ câu hỏi; provider thật, quota và theo dõi chi phí |
| Test đại diện cho quote, scope, migration, ingest và API cơ bản | Test đồng thời phòng cuối, retry/idempotency, payment callback, toàn bộ vòng đời lưu trú và UAT với FE/QA |

**Quyết định cần chốt trước khi mở luồng ghi:** (1) MVP đặt phòng do nhân viên hay cho khách tự đặt online; (2) chọn **một** cổng mô phỏng thanh toán — đề xuất VNPay sandbox theo thiết kế API hiện có, MoMo để sau; (3) thời hạn hold và thời điểm xác nhận booking; (4) quy tắc hủy/no-show, số dư trước check-out và cách xử lý thanh toán đến muộn. Các quyết định này phải được ghi vào ADR/API contract trước migration. Sandbox chỉ dùng tiền thử; nếu chưa có môi trường nhận callback, dùng mock gateway nội bộ theo cùng contract. FE redirect không tự xác nhận thanh toán.

## 2. BE1 — nền tảng, IAM, tài nguyên và tồn phòng

| ID | Task còn lại | Đầu ra/điều kiện hoàn thành | Phụ thuộc |
|---|---|---|---|
| B1-01 | Củng cố môi trường và dữ liệu mẫu | Có seed lặp lại được cho ít nhất 3 khách sạn/2 vùng, tài khoản demo theo scope; thử backup → restore và baseline database cũ **trên bản sao**, không chạm volume cũ. CI chạy migration/test từ DB mới. | Chốt dữ liệu demo với QA |
| B1-02 | Mở rộng IAM và contract chung cho API mới | Mỗi lệnh/danh sách/nguồn xuất kiểm tra permission và hotel scope ở server; phân quyền đọc PII riêng; bổ sung mã lỗi `409` cho xung đột tồn/booking, giữ `ApiResponse` và OpenAPI nhất quán. Chỉ làm multi-role/multi-scope nếu MVP thật sự yêu cầu. | Contract BE2/AI |
| B1-03 | Hoàn thiện tài nguyên cơ sở | API đọc phòng vật lý và trạng thái vận hành; dữ liệu hạng phòng, tiện ích và cấu hình theo cơ sở đủ cho FE. Chỉ thêm CRUD quản trị tối thiểu khi demo cần sửa dữ liệu, không xây bộ admin tổng quát trước luồng booking. | B1-01 |
| B1-04 | Xây inventory command | Migration cho hold từng đêm, hạn giữ, release, maintenance block và ràng buộc không gán trùng; public service `hold/confirm/release/assign` cho BE2. Hai request phòng cuối chạy đồng thời trên PostgreSQL phải không bán vượt; retry/job hết hạn không giải phóng hai lần. Redis không là nguồn sự thật của tồn phòng. | Chốt state/TTL; BE2 thống nhất transaction boundary |
| B1-05 | Buồng phòng và bảo trì | Chuyển trạng thái `DIRTY → CLEANING → READY`, khóa bán phòng bảo trì; BE2 gọi service của BE1 sau check-out/gán phòng. Phòng chưa Ready hoặc đang bảo trì không được gán. Có audit và test chuyển trạng thái sai/sai scope. | B1-03, B1-04, BE2 check-out |
| B1-06 | Củng cố vận hành | Audit các thao tác nhạy cảm ngoài tri thức AI; request ID/log không lộ PII, health/readiness, kiểm thử ranh giới module, smoke Docker, backup/restore. Chỉ dùng Redis cho cache có thể tái tạo và giới hạn tần suất; mất Redis không làm sai booking/payment. | Các luồng nghiệp vụ đã nối |

BE1 sở hữu thay đổi `inventory` và quy tắc gán phòng. BE2 gọi service công khai của BE1; không cập nhật trực tiếp bảng tồn hoặc slot từ `reservation`. Mọi thay đổi schema dùng migration Flyway mới, không sửa `V001`–`V003` đã áp dụng.

## 3. BE2 — giá, booking, lễ tân và tài chính mô phỏng

| ID | Task còn lại | Đầu ra/điều kiện hoàn thành | Phụ thuộc |
|---|---|---|---|
| B2-01 | Chốt pricing contract và snapshot | Đối soát giá theo đêm, phụ thu, phí, VAT, hiệu lực và làm tròn với ví dụ tính tay. Quote vẫn là thông tin hiện tại; khi tạo booking lưu daily-rate/total snapshot, đổi bảng giá không sửa giao dịch cũ. Thiếu quy tắc thuế/giá trả lỗi rõ. | Pricing hiện có; quyết định nghiệp vụ |
| B2-02 | Hold và booking một phòng | Thiết kế state machine, API tạo/xem/hủy/no-show/expire; dùng hold service của BE1, khóa và kiểm tra lại trong transaction. `Idempotency-Key` cùng payload trả cùng kết quả, payload khác trả `409`; rollback không để sót tồn. Danh sách/detail luôn lọc scope. | B1-04; B2-01 |
| B2-03 | Booker, guest và lễ tân | Lưu người đặt khác người lưu trú; dữ liệu khách tối thiểu và quyền xem PII. Gán phòng, check-in, check-out theo trạng thái booking/phòng/số dư; không nhận status tuỳ ý từ FE. Check-out gọi luồng Dirty của BE1. | B2-02; B1-05 |
| B2-04 | Thanh toán online giả lập | Một `PaymentGateway` adapter cho cổng đã chọn và mock/sandbox; payment attempt, reference duy nhất, trạng thái pending/success/failed, callback được kiểm tra và xử lý idempotent. Backend lấy số tiền từ snapshot; callback thành công sau khi hold hết hạn vào trạng thái cần đối soát, không tự cấp phòng. Không thu tiền thật, không làm refund tự động. | Quyết định cổng; B2-02 |
| B2-05 | Folio và điều chỉnh | Bảng kê tiền phòng/dịch vụ, khoản đã thu và số dư từ dữ liệu backend; điều chỉnh bằng bản ghi có lý do/audit, không ghi đè lịch sử. Folio nội bộ không được gọi là hóa đơn điện tử. | B2-01, B2-04 |
| B2-06 | Lưu trú và báo cáo tối thiểu | Hàng chờ/nhắc hạn, xuất dữ liệu lưu trú theo scope và ghi nhận kết quả nộp thủ công; công suất, doanh thu phòng theo cơ sở/kỳ đối chiếu được với booking/folio. Không tuyên bố đã tích hợp cơ quan nhà nước. | B2-03, B2-05 |

BE2 là owner giá và tiền. FE và AI chỉ dùng quote/folio đã tính ở backend. Lệnh booking, payment phải có test trên PostgreSQL thật cho cạnh tranh, lỗi giữa transaction, retry và callback lặp; test đơn luồng không đủ để nghiệm thu chống overbooking.

## 4. AI — tri thức, RAG, tool và đánh giá

| ID | Task còn lại | Đầu ra/điều kiện hoàn thành | Phụ thuộc |
|---|---|---|---|
| AI-01 | Chạy provider thật có kiểm soát | Bật AI trên môi trường thử với API key ngoài repository; đo thời gian, token/chi phí mẫu, timeout và lỗi provider. Backend catalog/booking vẫn phục vụ khi provider lỗi. CI tiếp tục dùng model giả để kết quả ổn định và không tốn phí. | Tài liệu thử, môi trường thử |
| AI-02 | Hoàn thiện vòng đời tri thức | Chuẩn hoá bộ tài liệu đã duyệt cho chuỗi/cơ sở (mục tiêu tham khảo: 30 mục, 3 cơ sở); bổ sung nguồn gốc, ngày hiệu lực, người duyệt, version mới/reindex khi sửa. Ingest lại không tạo chunk trùng; revoke/hết hiệu lực ngừng xuất hiện trong retrieval và citation. | BE1 review migration/permission |
| AI-03 | Nâng chất lượng retrieval và câu trả lời | Bộ lọc phạm vi/chính sách có hiệu lực, chunking và ngưỡng similarity được đánh giá bằng dữ liệu thật; citation gắn đúng đoạn dùng để trả lời. Không có nguồn hoặc nguồn mâu thuẫn thì nêu rõ thiếu căn cứ; tài liệu không thể ra lệnh cho model. | AI-02 |
| AI-04 | Hoàn thiện tool chỉ đọc | Bổ sung tra cứu catalog và khả dụng theo ngày/số khách; tool quote hiện có phải cho kết quả cùng actor/input với REST. Xác thực tham số tại server, báo dữ liệu vừa thay đổi/không còn phòng; không cấp tool tạo hold, booking, payment hoặc đọc hồ sơ khách. | BE1 inventory read; BE2 pricing contract |
| AI-05 | Bảo vệ và vận hành chat | Rate limit/quota theo actor bằng Redis, giới hạn input/context/output và số request đồng thời, timeout/fallback, trace không lưu prompt chứa PII mặc định. Có đường hướng dẫn liên hệ nhân viên khi không đủ thông tin; thống nhất trạng thái lỗi/citation với FE. Chat P0 chỉ cho nhân viên đã đăng nhập. | AI-01, AI-03, FE contract |
| AI-06 | Đánh giá và bàn giao | Version hoá tập tối thiểu 20 câu phát triển, mở lên khoảng 100 ca nghiệm thu cùng QA: đúng nguồn, sai scope, không nguồn, prompt injection, giá thay đổi, tool/provider lỗi. Ghi tỷ lệ đạt trước/sau chỉnh retrieval, độ trễ và chi phí; hướng dẫn thêm/sửa/gỡ tài liệu và thay model/dimension. | AI-02 đến AI-05 |

AI sở hữu `aiassistant` và test của module. BE1 review quyền/migration, BE2 review tool giá; QA đánh giá độc lập. Không để model gọi SQL hoặc tự quyết định actor/scope. Chat công khai cho khách, hội thoại nhiều lượt và tool ghi thuộc giai đoạn sau MVP.

## 5. Thứ tự triển khai và điểm giao

1. **Chốt contract trước luồng ghi:** BE1 và BE2 thống nhất hold/state/transaction; BE2 chốt chính sách giá và một cổng thanh toán giả lập; AI chốt metadata tri thức, response và citation với FE/QA. Ghi thay đổi vào OpenAPI/ADR.
2. **Làm song song:** BE1 triển khai inventory command; BE2 hoàn thiện snapshot/booking quanh public service của BE1; AI làm corpus, provider smoke và evaluation set. Mỗi phần có fixture và test độc lập.
3. **Nối luồng chính:** BE2 tích hợp payment mock/callback, gán phòng và check-in/out; BE1 hoàn thiện housekeeping/maintenance; AI nối catalog/availability/quote tool với service thật. FE nhận ví dụ request/response và mã lỗi sau mỗi API ổn định.
4. **Khóa MVP:** Chạy kịch bản tìm phòng → quote → hold → booking → payment giả lập → gán phòng → check-in → folio → check-out → Dirty → Ready → báo cáo. Cùng lúc kiểm tra chat chính sách có nguồn, giá/phòng trống khớp REST và cơ sở khác không đọc được dữ liệu.

**Definition of Done cho mỗi task:** có migration nếu thay DB; API/DTO và ví dụ lỗi cập nhật; permission/scope và audit tương ứng; unit test cho rule, integration test PostgreSQL cho SQL/transaction, test hợp đồng cho FE/AI; `mvn verify` qua CI. Task liên quan payment hoặc tồn phòng chỉ hoàn thành sau test retry/cạnh tranh; task AI chỉ hoàn thành sau đánh giá nguồn và quyền trên dữ liệu đã duyệt.

**Ngoài MVP:** đặt nhiều phòng/khách đoàn, chatbot công khai, multi-role/multi-scope, hoàn tiền tự động, hóa đơn điện tử, tích hợp cơ quan nhà nước, Night Audit đầy đủ, dự báo giá và OCR. Chỉ đưa vào backlog riêng sau khi luồng P0 và kiểm thử đạt.
