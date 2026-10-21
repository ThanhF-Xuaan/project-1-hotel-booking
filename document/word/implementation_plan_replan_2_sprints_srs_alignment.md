# Kế Hoạch 2 Sprint (21 Ngày: 23/10/2026 – 12/11/2026) Theo Thứ Tự Ưu Tiên & Luồng Thực Hiện Song Song

> **Dự án:** Hệ thống Quản lý và Đặt phòng Khách sạn Trực tuyến Thông minh  
> **Cơ sở đối soát:** Tài liệu đặc tả yêu cầu phần mềm [srs.md](file:///D:/Project/MyProject/hotel-booking-repo/document/word/srs.md) (Single Source of Truth)  
> **Quy tắc phân công:** Giữ nguyên 4 vai trò từ [Chia-Viec.md](file:///D:/Project/MyProject/hotel-booking-repo/document/word/Chia-Viec.md) (Member 1: AI/Data, Member 2: BE Core Lead, Member 3: Fullstack, Member 4: FE Lead)  
> **Nguyên tắc sắp xếp:** **Sắp xếp theo thứ tự ưu tiên nghiệp vụ và chuỗi phụ thuộc (P0 $\rightarrow$ P1 $\rightarrow$ P2)**, mã task ưu tiên/unblock được đẩy lên trước, tối đa hóa làm việc song song độc lập.

---

## 1. Cơ Chế Điều Phối Song Song & Giải Phóng Điểm Nghẽn (Concurrency Protocol)

Để 4 thành viên có thể code song song ngay từ ngày đầu tiên mà không bị block:
1. **Contract-First & Mock Data:** Các task **P0 (Làn sóng 1)** cung cấp DTO / Swagger Spec / Mock JSON trong 24h đầu. Ngay khi có Spec, Frontend và các service vệ tinh có thể code giao diện và logic tiêu thụ ngay bằng mock mà không cần đợi Backend hoàn thành 100%.
2. **Phân tách Độc lập Giữa Khách Lẻ (Sprint 1) và Khách Đoàn (Sprint 2):** Toàn bộ các điểm chưa nhất quán của luồng FIT được giải quyết dứt điểm ở Sprint 1. Bước sang Sprint 2, cả 4 thành viên cùng tập trung đột phá vào phân hệ Khách đoàn (GIT) và Báo cáo BI.

---

## 2. SPRINT 1 (10 Ngày: 23/10/2026 – 01/11/2026)
### *Đồng Bộ Inconsistencies, Hoàn Thiện Vận Hành FIT & Trợ Lý AI Fullstack*

```
[NGÀY 1 - 3: LÀN SÓNG 1 (P0)]
 ├── T-SP1-01 (M2: Folio vs Invoice, Room Swap BE) ──► Unblocks T-SP1-06 (M4: UI Room Swap, Rack)
 ├── T-SP1-02 (M1: API AI OCR CCCD/Passport)      ──► Unblocks T-SP1-07 (M3: UI Scan CCCD Check-in)
 ├── T-SP1-03 (M2: Cụm REST API IAM & Org)         ──► Unblocks T-SP1-08 (M3: 6 Trang Quản trị UI)
 └── T-SP1-04 (M1: API AI Advisor Chatbot RAG)     ──► Unblocks T-SP1-09 (M4: UI Chatbot Widget)

[NGÀY 4 - 8: LÀN SÓNG 2 (P1: LÀM SONG SONG ĐỒNG THỜI)]
 ├── T-SP1-06 (M4: Sơ đồ ma trận phòng Timeline Rack & Modal Đổi phòng)
 ├── T-SP1-07 (M3: Tích hợp nút Quét ảnh CCCD vào Modal Check-in Lễ tân)
 ├── T-SP1-08 (M3: Xây dựng 6 Trang Quản trị IAM/Tổ chức, sạch lỗi 404 Sidebar)
 ├── T-SP1-09 (M4: Floating Chatbot AI Widget trên Portal)
 ├── T-SP1-10 (M2: Dịch vụ gửi Email tự động FR-NOT qua JavaMailSender)
 ├── T-SP1-11 (M4: Validation chặn FIT >= 6 phòng chuyển hướng Inquiry đoàn)
 └── T-SP1-12 (M1: AI Guest Personalization Engine U14)

[NGÀY 9 - 10: LÀN SÓNG 3 (P2: TÍCH HỢP & NGHIỆM THU)]
 ├── T-SP1-13 (M3: Audit Log AOP Interceptor tự động ghi vết)
 ├── T-SP1-14 (M1: AI Predictive Model công suất phòng 30 ngày)
 └── T-SP1-15 (M3 + M4: E2E Testing & Regression Check luồng FIT)
```

### Bảng Chi Tiết Task Sprint 1 (Xếp Theo Thứ Tự Ưu Tiên)

| Thứ tự / Mã Task | Mức ưu tiên | Tên Hạng Mục / Chức Năng | Điều Kiện Phụ Thuộc / Khả Năng Song Song | Phụ trách | Est. | Mục Tiêu Đầu Ra (Deliverable) |
| :---: | :---: | :--- | :--- | :---: | :---: | :--- |
| **T-SP1-01** | **P0 (Cao nhất)** | **Chuẩn hóa Folio vs Invoice & API Đổi phòng (Room Swap)** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI Room Swap T-SP1-06. | Member 2 | 2.5d | API `/booking-rooms/{id}/swap-room`, phân tách Folio chi phí phòng vs Invoice thanh toán. |
| **T-SP1-02** | **P0 (Cao nhất)** | **API AI OCR Bóc tách CCCD/Passport** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI Scan CCCD Check-in T-SP1-07. | Member 1 | 2.5d | API `/ai/ocr/parse-identity` nhận ảnh, trả JSON họ tên, số CCCD, ngày sinh, địa chỉ chuẩn. |
| **T-SP1-03** | **P0 (Cao nhất)** | **Cụm REST API Quản trị IAM & Department** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho 6 trang UI IAM T-SP1-08. | Member 2 | 2.5d | Bộ API đầy đủ: Department, Staff, Role, BookingGuest, Company, AuditLog query. |
| **T-SP1-04** | **P0 (Cao nhất)** | **API Chatbot AI Advisor & RAG Pipeline** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI Chatbot Widget T-SP1-09. | Member 1 | 2.5d | API `POST /ai/advisor/chat` gắn chặt Core Price/Availability, pgvector hỏi chính sách/FAQ. |
| **T-SP1-05** | **P1** | **Thiết kế DB Schema & Liquibase Khách đoàn (GIT)** | **Làm song song ngay Ngày 1.** Chuẩn bị DB sẵn cho Sprint 2, không block luồng FIT. | Member 2 | 2.5d | ChangeSet XML: `RoomBlock`, `GroupContract`, `MasterFolio` + JPA Entities. |
| **T-SP1-06** | **P1** | **Sơ đồ ma trận phòng (Timeline Rack) & Modal Đổi phòng** | **Song song Ngày 3-6** (phụ thuộc contract T-SP1-01). | Member 4 | 3.0d | Timeline Grid View (FR-FD-01) 3 mã màu `DIRTY`, `CLEANING`, `READY` + Modal Đổi phòng. |
| **T-SP1-07** | **P1** | **Tích hợp Quét ảnh CCCD vào Modal Check-in Lễ tân** | **Song song Ngày 3-6** (phụ thuộc API T-SP1-02). | Member 3 | 2.5d | Nút *"Tải ảnh CCCD"* trên `StayGuestsModal` & `WalkInBookingPage`, tự động điền form. |
| **T-SP1-08** | **P1** | **Xây dựng 6 Trang Quản trị IAM & Tổ chức trên Frontend** | **Song song Ngày 3-7** (phụ thuộc API T-SP1-03). | Member 3 | 3.0d | 6 màn hình: Department, Staff, Role, Guest, Company, AuditLog; xóa sạch fallback Sidebar. |
| **T-SP1-09** | **P1** | **Giao diện Chatbot AI Widget trên Portal** | **Song song Ngày 3-6** (phụ thuộc API T-SP1-04). | Member 4 | 3.0d | Floating Chat Widget góc Portal: Tin nhắn real-time, Card gợi ý phòng có nút *"Xem chi tiết"*. |
| **T-SP1-10** | **P1** | **Dịch vụ Thông báo Email tự động (FR-NOT)** | **Làm song song độc lập Ngày 4-7.** Không bị ai block. | Member 2 | 2.5d | `JavaMailSender` + Template HTML gửi email xác nhận đặt phòng, mã tra cứu, hủy/hoàn tiền. |
| **T-SP1-11** | **P2** | **Validation Chặn FIT $\ge$ 6 Phòng trên Search Portal** | **Làm song song độc lập Ngày 5-7.** Không bị ai block. | Member 4 | 2.0d | Thanh tìm kiếm phát hiện $\ge 6$ phòng thì bật Modal điều hướng sang Inquiry Khách đoàn (FR-SRCH-05). |
| **T-SP1-12** | **P2** | **AI Guest Personalization Engine (U14)** | **Làm song song độc lập Ngày 5-8.** Không bị ai block. | Member 1 | 2.5d | Logic phân tích lịch sử lưu trú, tự động gợi ý hạng phòng ưu thích cho khách cũ. |
| **T-SP1-13** | **P2** | **AOP Audit Log Interceptor Tự Động Ghi Vết** | **Làm song song Ngày 7-9** (khi các action nhạy cảm đã hoàn thành). | Member 3 | 2.5d | AOP Aspect bắt các thao tác: Sửa giá, đổi phòng, hủy đơn, hoàn tiền $\rightarrow$ ghi vào `audit_logs`. |
| **T-SP1-14** | **P2** | **AI Predictive Model Công Suất Phòng 30 Ngày (U22)** | **Làm song song độc lập Ngày 7-9.** Không bị ai block. | Member 1 | 2.5d | Thuật toán Machine Learning dự báo tỷ lệ lấp đầy phòng và bất thường điện/nước. |
| **T-SP1-15** | **P1 (Chốt)** | **Kiểm thử E2E & Chống Hồi Quy Toàn Bộ Luồng FIT** | **Thực hiện Ngày 9-10.** Cần toàn bộ task Làn sóng 1 & 2 hoàn thành. | Member 3, Member 4 | 2.0d | Bộ Test Suite E2E kiểm tra toàn bộ luồng khách lẻ: Đặt $\rightarrow$ Check-in $\rightarrow$ Ở $\rightarrow$ Đổi $\rightarrow$ Trả. |

---

## 3. SPRINT 2 (11 Ngày: 02/11/2026 – 12/11/2026)
### *Đột Phá Khách Đoàn (GIT), Báo Cáo Quản Trị BI & Tối Ưu Hóa Toàn Hệ Thống*

```
[NGÀY 1 - 4: LÀN SÓNG 1 (P0: LÕI KHÁCH ĐOÀN)]
 ├── T-SP2-01 (M2: Backend Inquiry, Hợp đồng cọc 50%) ──► Unblocks T-SP2-05 (M3: Excel) & T-SP2-06 (M4: UI GIT)
 ├── T-SP2-02 (M2: Backend Room Block & Cut-off Scheduler)──► Unblocks T-SP2-06 (M4: UI Phân phòng đoàn)
 ├── T-SP2-03 (M2: Backend Master Folio & Cost Routing)   ──► Unblocks T-SP2-07 (M4: UI Master Folio)
 └── T-SP2-04 (M1: Thuật toán Phân phòng Tự động Đoàn)    ──► Kết hợp cùng T-SP2-05

[NGÀY 5 - 8: LÀN SÓNG 2 (P1: FULLSTACK GIT & BÁO CÁO BI CẤP CAO)]
 ├── T-SP2-05 (M3: Parser Import Excel Rooming List)
 ├── T-SP2-06 (M4: Giao diện Quản lý Khách đoàn GIT Portal)
 ├── T-SP2-07 (M4: Giao diện Master Folio & Quyết toán đoàn)
 ├── T-SP2-08 (M3: Backend Quyết toán & Xuất hóa đơn gộp/tách)
 ├── T-SP2-09 (M2: API Báo cáo Quản trị BI Cấp cao)      ──► Unblocks T-SP2-10 (M4: UI Dashboard BI)
 ├── T-SP2-10 (M4: Nâng cấp Executive BI Dashboard)
 ├── T-SP2-11 (M1: RAG AI Safety & Groundedness Audit)
 └── T-SP2-12 (M1: AI Utility Anomaly Detection Điện/Nước)

[NGÀY 9 - 11: LÀN SÓNG 3 (P1/P2: TỐI ƯU TOÀN DIỆN & BÀN GIAO 100% SRS)]
 ├── T-SP2-13 (M3: Tối ưu SQL Index, Transaction & Redis Cache)
 ├── T-SP2-14 (M1: Tối ưu Latency AI & OCR < 2s)
 ├── T-SP2-15 (M3 + M2: Concurrency Stress-Test 200 CCU không overbooking & RBAC Audit)
 └── T-SP2-16 (M4 + Toàn đội: Polish Design System, A11Y, Đóng gói bàn giao 100% SRS)
```

### Bảng Chi Tiết Task Sprint 2 (Xếp Theo Thứ Tự Ưu Tiên)

| Thứ tự / Mã Task | Mức ưu tiên | Tên Hạng Mục / Chức Năng | Điều Kiện Phụ Thuộc / Khả Năng Song Song | Phụ trách | Est. | Mục Tiêu Đầu Ra (Deliverable) |
| :---: | :---: | :--- | :--- | :---: | :---: | :--- |
| **T-SP2-01** | **P0 (Cao nhất)** | **Backend Đặt phòng Khách đoàn & Hợp đồng Cọc 50%** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI GIT T-SP2-06 và Excel Parser T-SP2-05. | Member 2 | 3.0d | API Inquiry $\rightarrow$ Quotation $\rightarrow$ Contract cọc 50% (bắt buộc cọc mới giữ phòng). |
| **T-SP2-02** | **P0 (Cao nhất)** | **Room Block & Scheduler Hạn chót (Cut-off Date)** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho Quỹ phòng đoàn T-SP2-06. | Member 2 | 3.0d | Quỹ phòng riêng đoàn trừ Public Inventory; Scheduler tự động giải phóng phòng khi đến Cut-off Date. |
| **T-SP2-03** | **P0 (Cao nhất)** | **Master Folio & Định tuyến Chi phí Tự động** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI Master Folio T-SP2-07. | Member 2 | 2.5d | Gom tiền phòng/tiệc về Master Folio công ty, định tuyến chi phí cá nhân về Sub-Folio. |
| **T-SP2-04** | **P1** | **Thuật toán Tự động Phân phòng Đoàn** | **Làm song song Ngày 1-4.** Kết hợp với Excel Parser T-SP2-05. | Member 1 | 3.0d | Thuật toán tự động xếp khách từ Rooming list vào phòng trong Room Block tối ưu tầng/giường. |
| **T-SP2-05** | **P1** | **Parser Import File Excel Rooming List (U12)** | **Song song Ngày 4-7** (phụ thuộc contract T-SP2-01). | Member 3 | 3.0d | Đọc file Excel/CSV danh sách đoàn: validate CCCD/Passport, tạo `StayGuest` hàng loạt. |
| **T-SP2-06** | **P1** | **Giao diện Quản lý Khách đoàn (GIT Portal - U15)** | **Song song Ngày 4-8** (phụ thuộc API T-SP2-01, T-SP2-02). | Member 4 | 3.5d | Portal gửi inquiry đoàn, xem hợp đồng & trạng thái cọc 50%, upload Excel, bảng gán phòng. |
| **T-SP2-07** | **P1** | **Giao diện Master Folio & Quyết toán Đoàn** | **Song song Ngày 5-8** (phụ thuộc API T-SP2-03). | Member 4 | 2.5d | UI Master Folio: Chi tiết chi phí gộp, cấn trừ cọc 50%, tách hóa đơn cá nhân khi Check-out. |
| **T-SP2-08** | **P1** | **Backend Quyết toán & Xuất Hóa đơn Gộp/Tách Đoàn** | **Song song Ngày 5-8** (phụ thuộc T-SP2-03). | Member 3 | 2.5d | API Check-out đoàn, cấn trừ tiền cọc, xuất hóa đơn Master Folio và các Sub-Folio. |
| **T-SP2-09** | **P1** | **API Báo cáo Quản trị BI Cấp cao (U21)** | **Làm song song Ngày 4-7.** Mở khóa (`Unblocks`) cho UI Dashboard T-SP2-10. | Member 2 | 2.5d | API tính RevPAR, ADR, Occupancy Rate, Doanh thu phòng vs F&B phân tách theo Vùng/Chuỗi. |
| **T-SP2-10** | **P1** | **Nâng cấp Executive BI Dashboard (U23)** | **Song song Ngày 7-9** (phụ thuộc API T-SP2-09). | Member 4 | 2.5d | Dashboard cấp Chuỗi: Biểu đồ Recharts xu hướng, Bộ lọc Vùng $\rightarrow$ Khách sạn, Xuất PDF/Excel. |
| **T-SP2-11** | **P2** | **Đánh giá & Bảo mật Trợ lý AI (RAG Triad & Safety)** | **Làm song song độc lập Ngày 5-7.** Không bị ai block. | Member 1 | 2.5d | Đánh giá Groundedness, chống prompt injection, khóa chặn 100% quyền ghi cọc/booking của AI. |
| **T-SP2-12** | **P2** | **AI Anomaly Detection Điện / Nước Chi nhánh** | **Làm song song độc lập Ngày 6-8.** Không bị ai block. | Member 1 | 2.5d | Mô hình phát hiện rò rỉ, gian lận hoặc biến động bất thường chỉ số điện nước chi nhánh. |
| **T-SP2-13** | **P1** | **Tối ưu Hóa SQL Index, Transaction & Redis Cache** | **Làm song song Ngày 8-10.** | Member 3 | 2.5d | Tối ưu truy vấn SQL nặng, cấu hình Redis Cache cho Rate Plans và Master Data. |
| **T-SP2-14** | **P1** | **Tối ưu Hóa Latency Trợ lý AI & OCR (< 2s)** | **Làm song song Ngày 8-10.** | Member 1 | 2.5d | Benchmark và giảm độ trễ phản hồi của Chatbot AI và OCR dưới 2 giây. |
| **T-SP2-15** | **P1** | **Stress-test 200 CCU Đồng thời & Security Audit** | **Thực hiện Ngày 9-11.** | Member 3, Member 2 | 2.5d | Bài test tải 200 CCU đồng thời tranh chấp phòng cuối cùng không overbooking; rà soát RBAC. |
| **T-SP2-16** | **P1 (Chốt)** | **Hoàn thiện Design System, A11Y & Đóng Gói 100% SRS** | **Thực hiện Ngày 10-11.** Toàn bộ hệ thống hoàn thành. | Member 4, Toàn đội | 2.5d | Rà soát UI Tokens, Responsive Mobile, nghiệm thu 100% tính năng SRS sẵn sàng bảo vệ. |

---

## 4. Tóm Tắt Khối Lượng Theo Thành Viên (Workload Verification)

Mặc dù sắp xếp theo thứ tự ưu tiên nghiệp vụ, tổng khối lượng công việc thực tế của 4 thành viên vẫn hoàn toàn cân bằng qua 2 Sprint:

* **Member 1 (AI / Data Specialist):** SP1: 10.0d (4 tasks) + SP2: 10.5d (4 tasks) = **20.5 ngày công**
* **Member 2 (Backend Core Lead):** SP1: 10.0d (4 tasks) + SP2: 11.0d (4 tasks) = **21.0 ngày công**
* **Member 3 (Fullstack Developer):** SP1: 10.0d (4 tasks) + SP2: 10.5d (4 tasks) = **20.5 ngày công**
* **Member 4 (Frontend Lead):** SP1: 10.0d (4 tasks) + SP2: 11.0d (4 tasks) = **21.0 ngày công**

---

## 5. Tiêu Chuẩn Nghiệm Thu & Định Nghĩa Hoàn Thành (DoD)

1. **Không bị Block trong quá trình làm việc:** Mọi task P1 của Frontend đều có Mock Schema hoặc API P0 sẵn sàng trước khi code.
2. **Loại bỏ 100% Inconsistencies:** Không còn trang 404/fallback trên Sidebar; phân tách Folio vs Invoice; Timeline Rack View đầy đủ.
3. **Phân hệ Khách đoàn (GIT) hoàn thiện trọn vẹn:** Luồng Báo giá $\rightarrow$ Cọc 50% $\rightarrow$ Giữ Room Block $\rightarrow$ Upload Excel Rooming list $\rightarrow$ Check-out Master Folio.
4. **Trợ lý AI & OCR hoạt động mượt mà:** Chatbot AI trên Portal và OCR Scan CCCD tại quầy Check-in đạt thời gian phản hồi < 2s.
5. **Độ tin cậy & Hiệu năng cao:** 100% Test pass, không Overbooking khi tải đồng thời 200 CCU, đáp ứng trọn vẹn 100% SRS.
