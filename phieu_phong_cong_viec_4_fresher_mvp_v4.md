Hệ thống Quản lý Khách sạn AI v4.0 | Kế hoạch Phân công Công việc 4 Fresher 

**KẾ HOẠCH PHÂN CÔNG CÔNG VIỆC 4 FRESHER (BẢN TỐI ƯU V4.0)** 

_Dự án: Hệ thống Quản lý và Đặt phòng Khách sạn Trực tuyến Thông minh Chiến lược Phụ tải Cân bằng & Phát triển Song song Song hành RAG Chatbot_ 

- **📌 Điểm Cải Tiến Cốt Lõi Trong Bản Phân Công v4.0:** 

- **Tối ưu phụ tải cho Member 2 (Backend Lead):** Giao lại toàn bộ Database Verification & JPA Entities, Keycloak IAM, Core Booking Engine và Pricing Pipeline cho Member 2 cầm chắc cốt lõi kiến trúc. Giảm bớt các module nghiệp vụ vệ tinh cho Member 1 và Member 3. 

- **Member 1 (AI & Data Specialist):** Giai đoạn đầu (Sprints 1-2) tập trung hỗ trợ phát triển Backend các sub-modules (Utility Meters, Payroll Summary Import, Lodging Queue). Khi Backend Core đủ điều kiện (APIs/Schemas sẵn sàng), chuyển sang phát triển RAG Chatbot. Trong Cải tiến 1 & 2, đảm nhận AI Personalization, AI Passport/CCCD OCR Parser, và AI Predictive Analytics để đóng góp xuyên suốt dự án. **• Member 3 (Fullstack Developer):** Gánh thêm phần lớn nghiệp vụ Backend vệ tinh từ Member 2 (Payment Webhooks, POS Order APIs, Maintenance Ticket APIs) và xây dựng giao diện tương ứng. 

- **Member 4 (Frontend Lead):** Tập trung tối đa vào Design System Tokens, Sơ đồ phòng dạng Timeline Grid, Luồng Đặt phòng UI và Executive Dashboard Charts. 

# **1. Tổng Quan Vai Trò & Mô Hình Phát Triển Song Song** 

Hệ thống áp dụng mô hình kiến trúc **Contract-First API & Decoupled Architecture** . Việc phân chia công việc được thiết kế để giải phóng sự phụ thuộc giữa Backend và Frontend ngay từ tuần đầu tiên, giúp 4 Fresher hoạt động độc lập và đạt hiệu suất tối đa. 

|**Thành Viên**|**Vai Trò Định Danh**|**Trọng Tâm Nhiệm Vụ**|**Công Nghệ Chủ**<br>**Đạo**|
|---|---|---|---|
|**Member 1**|AI / Data Specialist &<br>BE Support|Đầu MVP: Xây dựng BE<br>Sub-modules (Utility,<br>Payroll, Lodging). Khi BE<br>Core ready: Phát triển RAG<br>Chatbot. Cải tiến 1&2: AI<br>OCR Parser & Predictive<br>Analytics.|Python, Spring AI,<br>LangChain, Vector<br>DB, PostgreSQL|
|**Member 2**|Backend Core Lead|Chịu trách nhiệm Cốt lõi:|Java 21, Spring Boot|



Trang 1 

Hệ thống Quản lý Khách sạn AI v4.0 | Kế hoạch Phân công Công việc 4 Fresher 

||(Project Lead)|DB Verification & JPA<br>Entities, Keycloak<br>IAM/OAuth2, Booking<br>Engine FIT/GIT, Hold 15m<br>Lock, Pricing Pipeline<br>Pattern.|4.0.7, PostgreSQL<br>16, Keycloak, Redis|
|---|---|---|---|
|**Member 3**|Fullstack Developer<br>(BE + FE)|Đảm nhận BE + FE cho<br>Payment Gateway<br>(VNPay/MoMo Webhook),<br>POS Dịch vụ (F&B/Giặt<br>là), Buồng phòng &<br>Maintenance Tickets<br>(OOO/OOS).|Java 21, Spring Boot,<br>React 19,<br>TypeScript, Tailwind<br>CSS|
|**Member 4**|Frontend Lead|Chủ trì Frontend: Design<br>System Tokens, Interactive<br>Room Timeline Grid,<br>Luồng Đặt phòng UI,<br>Executive Dashboard &<br>Charts.|React 19, TypeScript<br>6, Vite 8, Tailwind<br>CSS v4, Recharts|



# **2. Phân Công Chi Tiết Theo Giai Đoạn (MVP -> Cải Tiến 1 -> Cải Tiến 2)** 

**Giai Đoạn 1: MVP Cốt Lõi (Phát Triển Độc Lập & Khởi Tạo RAG Chatbot)** 

**Chiến lược Sprint 1-2:** Member 2 xác minh DB & dựng JPA Entities, sau đó Member 1, 2, 3 cùng viết Backend để hoàn thiện bộ REST APIs và Swagger Spec. Khi BE Core ổn định, Member 1 tập trung 100% vào RAG Chatbot, Member 2 xử lý Booking Engine & Pricing Pipeline, Member 3 xử lý Payment & POS, Member 4 hoàn thiện UI. 

|**Mã**|**Hạng Mục /**|**Mô Tả Chi Tiết Công Việc**|**Phụ**|**Estimate **|**Deliverable**|
|---|---|---|---|---|---|
|**Task**|**Chức Năng**||**Trách**|||
|**T01**|Database Verify|Xác minh PostgreSQL|Member 2|2.0d|JPA Entity|
||& JPA Mapping|schema hiện có; Sinh JPA<br>Entities, Repositories,<br>Flyway Migration Baseline|||Layer|



Trang 2 

Hệ thống Quản lý Khách sạn AI v4.0 | Kế hoạch Phân công Công việc 4 Fresher 

|||& DTO contracts.||||
|---|---|---|---|---|---|
|**T02**|Keycloak IAM<br>& OAuth2|Cấu hình Keycloak Realm,<br>Client ID, Roles<br>(CHAIN_ADMIN,<br>PROPERTY_MANAGER,<br>FRONT_DESK, HK,<br>GUEST). Tích hợp Spring<br>Security OAuth2 JWT<br>Resource Server.|Member 2|2.5d|Security<br>Subsystem|
|**T03**|UI Tokens &<br>Layout Core|Xây dựng Tailwind CSS v4<br>Theme Tokens (Radius<br>12px/16px, Controls<br>48px/40px). Dựng Main App<br>Shell, Header, Sidebar,<br>Responsive Layout.|Member 4|2.0d|Design<br>Tokens &<br>Layout|
|**T04**|Utility & Master<br>Data BE|Xây dựng CRUD APIs cho<br>danh mục master data<br>(Region, Property, Room<br>Type) và module Điện/Nước<br>(UtilityMeter,<br>UtilityReading).|Member 1|2.5d|Master Data<br>& Utility<br>APIs|
|**T05**|Lodging &<br>Payroll BE|Xây dựng Hàng chờ Tạm trú<br>(Lodging Queue), quy trình<br>thu thập thông tin StayGuest<br>và xuất file khai báo Bộ<br>Công An. API Import<br>Payroll Summary.|Member 1|2.5d|Lodging &<br>Payroll<br>APIs|
|**T06**|Payment<br>Gateway BE &<br>FE|Tích hợp SDK/Webhook<br>VNPay, MoMo, ZaloPay.<br>Xử lý Callback, cập nhật<br>Folio/Payment Status. Dựng<br>UI Checkout Modal & QR<br>Code.|Member 3|3.0d|Payment<br>Integration|



Trang 3 

Hệ thống Quản lý Khách sạn AI v4.0 | Kế hoạch Phân công Công việc 4 Fresher 

|**T07**|Pricing Pipeline<br>Pattern|Cài đặt Pipeline Pattern tính<br>giá cơ bản: BaseRateHandler<br>-> SeasonalityHandler -><br>PromotionHandler -><br>SurchargeHandler -><br>TaxAndFeeHandler.|Member 2|3.0d|Pricing<br>Engine<br>Pipeline|
|---|---|---|---|---|---|
|**T08**|Core Booking<br>Engine (FIT)|Xử lý đặt phòng lẻ (FIT 1-5<br>phòng). Cơ chế Session<br>Lock TTL 15m trên Redis<br>chống trùng lặp. Đổi trạng<br>thái DRAFT -><br>CONFIRMED.|Member 2|3.5d|Booking<br>FIT API|
|**T09**|POS Dịch Vụ &<br>HK/Maintenanc<br>e|Xây dựng APIs + UI Order<br>dịch vụ F&B, Giặt là (Post<br>chi phí vào Folio) và Module<br>Buồng phòng / Dated Room<br>Block (OOO/OOS).|Member 3|3.5d|POS &<br>Housekeepi<br>ng Module|
|**T10**|Timeline Grid &<br>Room Status UI|<br>Dựng Sơ đồ phòng dạng<br>Timeline Grid tương tác trực<br>quan. Chuyển đổi trạng thái<br>DIRTY -> CLEANING -><br>READY. Tích hợp Room<br>Block OOO.|Member 4|3.5d|Interactive<br>Timeline<br>Grid|
|**T11**|Guest Booking<br>UI Flow|Dựng luồng Đặt phòng<br>Khách lẻ: Tìm kiếm, Chọn<br>hạng phòng, Đếm ngược<br>15m Hold session, Điền<br>thông tin StayGuest, Xác<br>nhận.|Member 4|3.0d|Booking UI<br>Flow|
|**T12**|RAG Chatbot<br>AI Core|Khi BE APIs đã sẵn sàng:<br>Dựng RAG Pipeline (Spring<br>AI / LangChain + Vector<br>DB). Ingest dữ liệu|Member 1|4.0d|RAG AI<br>Booking<br>Chatbot|



Trang 4 

Hệ thống Quản lý Khách sạn AI v4.0 | Kế hoạch Phân công Công việc 4 Fresher 

Phòng/Chính sách. Dựng UI Chatbot tư vấn đặt phòng. 

**Giai Đoạn 2: Cải Tiến 1 (Nghiệp Vụ Nâng Cao & AI Personalization)** 

**Trọng tâm:** Mở rộng Đặt phòng Khách đoàn (GIT >= 6 phòng), Hợp đồng cọc 50%, Room Block giữ phòng, Import Excel Rooming List, kết hợp AI Personalization Engine & OCR Parser do Member 1 chủ trì. 

|**Mã**<br>**Task**|**Hạng Mục /**<br>**Chức Năng**|**Mô Tả Chi Tiết Công Việc**|**Phụ**<br>**Trách**|**Estimate **|**Deliverable**|
|---|---|---|---|---|---|
|**U11**|GIT Booking &<br>Contract BE|Nghiệp vụ Đặt phòng Khách<br>đoàn (>= 6 phòng): Tạo Báo<br>giá, Hợp đồng cọc 50%,<br>Lịch trình thanh toán, Tạo<br>Room Block giữ chỗ.|Member 2|3.0d|GIT<br>Booking<br>Subsystem|
|**U12**|Excel Rooming<br>List & Helper<br>APIs|Viết Parser xử lý Import file<br>Excel danh sách khách đoàn<br>(Rooming List), tự động ánh<br>xạ thông tin StayGuest vào<br>từng phòng.|Member 3|2.5d|Excel<br>Import &<br>Group APIs|
|**U13**|AI OCR<br>Passport/CCCD<br>Parser|Tích hợp mô hình OCR tự<br>động bóc tách thông tin Hộ<br>chiếu/CCCD từ ảnh chụp gửi<br>qua Lễ tân để nạp thẳng vào<br>StayGuest.|Member 1|3.0d|AI OCR<br>Identity<br>Parser|
|**U14**|AI Guest<br>Personalization<br>Engine|Phân tích lịch sử lưu trú và<br>hành vi khách hàng; tự động<br>đề xuất Rate Plan ưu đãi và<br>dịch vụ gợi ý phù hợp khi<br>khách quay lại.|Member 1|3.0d|AI<br>Personalizat<br>ion Engine|
|**U15**|GIT Booking UI<br>& Group|<br>Dựng giao diện Quản lý<br>Khách đoàn: Luồng báo giá,<br>Quản lý đặt cọc, Upload|Member 4|3.0d|GIT Group<br>UI Portal|



Trang 5 

Hệ thống Quản lý Khách sạn AI v4.0 | Kế hoạch Phân công Công việc 4 Fresher 

Management Rooming List và Gán phòng hàng loạt. 

# **Giai Đoạn 3: Cải Tiến 2 (Executive Analytics & Optimization)** 

|**Mã**<br>**Task**|**Hạng Mục /**<br>**Chức Năng**|**Mô Tả Chi Tiết Công Việc**|**Phụ**<br>**Trách**|**Estimate **|**Deliverable**|
|---|---|---|---|---|---|
|**U21**|Executive<br>Analytics<br>Backend|Tổng hợp dữ liệu đa chi<br>nhánh: Công suất phòng<br>(RevPAR, ADR, Occupancy<br>Rate), Doanh thu phòng vs<br>Dịch vụ, Chi phí Điện/Nước.|Member 2|2.5d|Executive<br>Analytics<br>APIs|
|**U22**|AI Predictive<br>Analytics &<br>Anomaly|Mô hình AI dự báo công<br>suất phòng 30 ngày tới &<br>Thuật toán phát hiện bất<br>thường tiêu thụ điện nước<br>chi nhánh.|Member 1|3.0d|AI<br>Predictive<br>Model|
|**U23**|Executive<br>Dashboard & BI<br>Charts|<br>Dựng Dashboard báo cáo<br>cao cấp cho Chủ chuỗi<br>(Chain Executive): Biểu đồ<br>Recharts, Bộ lọc theo<br>Region/Property, Export<br>PDF/Excel.|Member 4|3.0d|BI<br>Dashboard<br>Portal|
|**U24**|System<br>Optimization &<br>Security Audit|Tối ưu hóa Query SQL, Cấu<br>hình Redis Cache cho Rate<br>Plans, Rà soát lỗ hổng bảo<br>mật RBAC & Stress test<br>toàn hệ thống.|Member 3|2.5d|Optimizatio<br>n & Audit|



# **3. Bảng Tổng Hợp Khối Lượng Công Việc & Điểm Cân Bằng (Workload Balance)** 

|**Thành Viên**|**Khối Lượng**|**Cải Tiến 1**|**Cải Tiến 2**|**Tổng Số Ngày**|
|---|---|---|---|---|
||**MVP (Ngày)**|**(Ngày)**|**(Ngày)**|**Công**|



Trang 6 

Hệ thống Quản lý Khách sạn AI v4.0 | Kế hoạch Phân công Công việc 4 Fresher 

**Member 1** 9.0 ngày (4 tasks) 6.0 ngày (2 tasks) 3.0 ngày (1 task) **18.0 ngày công (AI / Data) Member 2** 11.0 ngày (4 tasks) 3.0 ngày (1 task) 2.5 ngày (1 task) **16.5 ngày công (BE Core Lead) Member 3** 6.5 ngày (2 tasks) 2.5 ngày (1 task) 2.5 ngày (1 task) **11.5 ngày công (Fullstack) (+FE support) Member 4** 11.5 ngày (4 tasks) 3.0 ngày (1 task) 3.0 ngày (1 task) **17.5 ngày công (FE Lead)** 

**4. Quy Trình Phối Hợp Git & Tiêu Chí Nghiệm Thu (Definition of Done)** 

**1. Quy tắc đặt tên Branch:** Mọi tính năng phát triển trên nhánh riêng: **feature/<task-code><short-description>** (Ví dụ: feature/T07-pricing-pipeline, feature/T12-rag-chatbot). **2. Quy trình Code Review:** Tất cả Pull Request (PR) phải có ít nhất 1 Peer Review phê duyệt. Member 2 (Project Lead) là người duy nhất merge vào nhánh **main** hoặc **develop** . 

**3. Tiêu chí nghiệm thu (DoD):** Mã nguồn không chứa lỗi Lint/Compiler; 100% Unit Test pass; Swagger Spec được cập nhật đồng bộ; chạy thử nghiệm không có lỗi hồi quy (No regression). 

Trang 7 

