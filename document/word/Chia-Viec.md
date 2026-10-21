# KẾ HOẠCH PHÂN CÔNG CÔNG VIỆC 4 FRESHER THEO 2 SPRINT (21 NGÀY: 23/10/2026 – 12/11/2026)
### DỰ ÁN: HỆ THỐNG QUẢN LÝ VÀ ĐẶT PHÒNG KHÁCH SẠN TRỰC TUYẾN THÔNG MINH

> **Cơ sở đối soát:** Tài liệu đặc tả yêu cầu phần mềm SRS (`document/word/srs.md`) – Single Source of Truth  
> **Kế thừa vai trò:** Giữ nguyên 4 vai trò cốt lõi từ phiếu phân công ban đầu (`Chia-Viec.md`)  
> **Nguyên tắc phân công:** **Sắp xếp theo thứ tự ưu tiên nghiệp vụ và chuỗi phụ thuộc (P0 → P1 → P2)**, mã task ưu tiên và unblock được đẩy lên trước, tối đa hóa khả năng làm việc song song độc lập.  
> **Tiến độ khởi điểm:** Codebase hiện tại đã hoàn thành vững chắc ~71% khối lượng nghiệp vụ SRS.

---

## 1. Cơ Chế Điều Phối Song Song & Giải Phóng Điểm Nghẽn (Concurrency Protocol)

Để 4 thành viên có thể triển khai đồng thời ngay từ ngày đầu tiên mà không bị block:
1. **Contract-First & Mock Data:** Các task **P0 (Làn sóng 1)** có trách nhiệm chốt DTO / Swagger Spec / Mock JSON trong 24 giờ đầu tiên. Ngay khi có Spec, Frontend và các service vệ tinh có thể phát triển giao diện và logic tiêu thụ ngay bằng mock dữ liệu mà không cần chờ Backend hoàn thiện 100%.
2. **Phân tách Độc lập Giữa Khách Lẻ (Sprint 1) và Khách Đoàn (Sprint 2):** Toàn bộ các điểm chưa nhất quán và tính năng còn thiếu của luồng khách lẻ FIT được giải quyết dứt điểm ở Sprint 1. Bước sang Sprint 2, cả 4 thành viên cùng tập trung đột phá vào phân hệ Khách đoàn (GIT) và Báo cáo BI cấp cao.
3. **Độc lập Vai trò:**
   - **Member 1 (AI / Data Specialist):** Chủ trì Chatbot AI RAG, OCR CCCD/Passport, Personalization Engine, Predictive Analytics và thuật toán phân phòng đoàn.
   - **Member 2 (Backend Core Lead):** Chủ trì chuẩn hóa Folio vs Invoice, Room Swap BE, Dịch vụ Email Notification, Lõi Khách đoàn GIT, Master Folio và API Báo cáo BI.
   - **Member 3 (Fullstack Developer):** Chủ trì Tích hợp OCR vào UI Check-in Lễ tân, cụm 6 trang UI Quản trị IAM/Tổ chức, Parser Import Excel Rooming List, Quyết toán đoàn và Concurrency Stress-testing.
   - **Member 4 (Frontend Lead):** Chủ trì Floating Chatbot AI Widget trên Portal, Sơ đồ ma trận phòng Timeline Rack View, Modal Đổi phòng, UI Portal Khách đoàn và Executive BI Dashboard.

---

## 2. SPRINT 1 (10 Ngày: 23/10/2026 – 01/11/2026)
### *Chủ đề: Đồng Bộ Inconsistencies, Hoàn Thiện Vận Hành Lễ Tân FIT & Trợ Lý AI Fullstack*

```
[NGÀY 1 - 3: LÀN SÓNG 1 (P0 - NỀN TẢNG & MỞ KHÓA)]
 ├── T-SP1-01 (M2: Folio vs Invoice, Room Swap BE) ──► Unblocks T-SP1-06 (M4: UI Room Swap, Timeline Rack)
 ├── T-SP1-02 (M1: API AI OCR CCCD/Passport)      ──► Unblocks T-SP1-07 (M3: UI Scan CCCD Check-in)
 ├── T-SP1-03 (M2: Cụm REST API IAM & Department)  ──► Unblocks T-SP1-08 (M3: 6 Trang Quản trị UI)
 └── T-SP1-04 (M1: API AI Advisor Chatbot RAG)     ──► Unblocks T-SP1-09 (M4: UI Chatbot Widget)

[NGÀY 4 - 8: LÀN SÓNG 2 (P1 - THỰC HIỆN SONG SONG ĐỒNG THỜI)]
 ├── T-SP1-06 (M4: Sơ đồ ma trận phòng Timeline Rack & Modal Đổi phòng)
 ├── T-SP1-07 (M3: Tích hợp nút Quét ảnh CCCD vào Modal Check-in Lễ tân)
 ├── T-SP1-08 (M3: Xây dựng 6 Trang Quản trị IAM/Tổ chức, sạch lỗi 404 Sidebar)
 ├── T-SP1-09 (M4: Floating Chatbot AI Widget trên Portal)
 ├── T-SP1-10 (M2: Dịch vụ gửi Email tự động FR-NOT qua JavaMailSender)
 ├── T-SP1-11 (M4: Validation chặn FIT >= 6 phòng chuyển hướng Inquiry đoàn)
 └── T-SP1-12 (M1: AI Guest Personalization Engine U14)

[NGÀY 9 - 10: LÀN SÓNG 3 (P2 - TÍCH HỢP, KIỂM THỬ & NGHIỆM THU SPRINT 1)]
 ├── T-SP1-13 (M3: Audit Log AOP Interceptor tự động ghi vết)
 ├── T-SP1-14 (M1: AI Predictive Model công suất phòng 30 ngày)
 └── T-SP1-15 (M3 + M4: E2E Testing & Regression Check luồng FIT)
```

### Bảng Chi Tiết Task Sprint 1 (Sắp Xếp Theo Thứ Tự Ưu Tiên)

| Mã Task | Mức Ưu Tiên | Tên Hạng Mục / Chức Năng | Điều Kiện Phụ Thuộc / Khả Năng Song Song | Phụ Trách | Estimate | Mục Tiêu Đầu Ra (Deliverable) |
|:---:|:---:|:---|:---|:---:|:---:|:---|
| **T-SP1-01** | **P0 (Cao nhất)** | **Chuẩn hóa Folio vs Invoice & API Đổi phòng (Room Swap)** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI Room Swap T-SP1-06. | Member 2 | 2.5d | API `/booking-rooms/{id}/swap-room`, phân tách rõ Folio chi phí phòng mở liên tục vs Invoice chốt thanh toán. |
| **T-SP1-02** | **P0 (Cao nhất)** | **API AI OCR Bóc tách CCCD/Passport** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI Scan CCCD Check-in T-SP1-07. | Member 1 | 2.5d | API `POST /api/v1/ai/ocr/parse-identity` nhận ảnh (base64/multipart), trả về JSON họ tên, số CCCD, ngày sinh, địa chỉ chuẩn. |
| **T-SP1-03** | **P0 (Cao nhất)** | **Cụm REST API Quản trị IAM & Department** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho 6 trang UI IAM T-SP1-08. | Member 2 | 2.5d | Bộ API đầy đủ: Department, Staff, Role, BookingGuest, Company, AuditLog query theo chuẩn Layered Architecture. |
| **T-SP1-04** | **P0 (Cao nhất)** | **API Chatbot AI Advisor & RAG Pipeline** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI Chatbot Widget T-SP1-09. | Member 1 | 2.5d | API `POST /api/v1/ai/advisor/chat` gắn chặt Core Price/Availability, tích hợp pgvector cho hỏi đáp chính sách/FAQ. |
| **T-SP1-05** | **P1** | **Thiết kế DB Schema & Liquibase Khách đoàn (GIT)** | **Làm song song ngay Ngày 1.** Chuẩn bị sẵn DB cho Sprint 2, không block luồng FIT. | Member 2 | 2.5d | File ChangeSet XML Liquibase: `RoomBlock`, `GroupContract`, `MasterFolio` + JPA Entities & Repositories. |
| **T-SP1-06** | **P1** | **Sơ đồ ma trận phòng (Timeline Rack) & Modal Đổi phòng** | **Song song Ngày 3-6** (phụ thuộc contract T-SP1-01). | Member 4 | 3.0d | Giao diện Timeline Grid View (FR-FD-01) với 3 mã màu trạng thái `DIRTY`, `CLEANING`, `READY` + Modal Đổi phòng trực quan. |
| **T-SP1-07** | **P1** | **Tích hợp Quét ảnh CCCD vào Modal Check-in Lễ tân** | **Song song Ngày 3-6** (phụ thuộc API T-SP1-02). | Member 3 | 2.5d | Nút *"Tải ảnh CCCD/Hộ chiếu"* trên `StayGuestsModal` & `WalkInBookingPage`, tự động điền form khai báo lưu trú. |
| **T-SP1-08** | **P1** | **Xây dựng 6 Trang Quản trị IAM & Tổ chức trên Frontend** | **Song song Ngày 3-7** (phụ thuộc API T-SP1-03). | Member 3 | 3.0d | 6 màn hình chuẩn: Department, Staff, Role, Guest, Company, AuditLog; đăng ký routes, xóa sạch lỗi fallback Sidebar. |
| **T-SP1-09** | **P1** | **Giao diện Chatbot AI Widget trên Portal** | **Song song Ngày 3-6** (phụ thuộc API T-SP1-04). | Member 4 | 3.0d | Floating Chat Widget ở góc Portal: Tin nhắn real-time, streaming text, hiển thị Card gợi ý phòng có nút *"Xem chi tiết"*. |
| **T-SP1-10** | **P1** | **Dịch vụ Thông báo Email tự động (FR-NOT)** | **Làm song song độc lập Ngày 4-7.** Không bị ai block. | Member 2 | 2.5d | `JavaMailSender` + Template HTML gửi email tự động: Xác nhận đặt phòng, mã tra cứu, thông báo hủy phòng/hoàn tiền. |
| **T-SP1-11** | **P2** | **Validation Chặn FIT ≥ 6 Phòng trên Search Portal** | **Làm song song độc lập Ngày 5-7.** Không bị ai block. | Member 4 | 2.0d | Form tìm kiếm phát hiện yêu cầu đặt từ 6 phòng trở lên thì hiển thị Modal điều hướng sang Inquiry Khách đoàn (FR-SRCH-05). |
| **T-SP1-12** | **P2** | **AI Guest Personalization Engine (U14)** | **Làm song song độc lập Ngày 5-8.** Không bị ai block. | Member 1 | 2.5d | Phân tích lịch sử lưu trú, tự động gợi ý hạng phòng yêu thích và ưu đãi phù hợp cho khách cũ khi quay lại tìm kiếm. |
| **T-SP1-13** | **P2** | **AOP Audit Log Interceptor Tự Động Ghi Vết** | **Làm song song Ngày 7-9** (khi các action nhạy cảm đã hoàn thành). | Member 3 | 2.5d | AOP Aspect tự động ghi bản ghi vào bảng `audit_logs` cho các thao tác: Sửa giá, đổi phòng, hủy đơn, hoàn tiền. |
| **T-SP1-14** | **P2** | **AI Predictive Model Công Suất Phòng 30 Ngày (U22)** | **Làm song song độc lập Ngày 7-9.** Không bị ai block. | Member 1 | 2.5d | Thuật toán Machine Learning dự báo tỷ lệ lấp đầy phòng (Occupancy) và phát hiện bất thường tiêu thụ điện/nước. |
| **T-SP1-15** | **P1 (Chốt)** | **Kiểm thử E2E & Chống Hồi Quy Toàn Bộ Luồng FIT** | **Thực hiện Ngày 9-10.** Cần toàn bộ task Làn sóng 1 & 2 hoàn thành. | Member 3, Member 4 | 2.0d | Bộ Test Suite E2E kiểm thử khép kín: Đặt phòng → Nhận Email → Quét CCCD check-in → Timeline rack → Đổi phòng → Check-out Folio. |

---

## 3. SPRINT 2 (11 Ngày: 02/11/2026 – 12/11/2026)
### *Chủ đề: Đột Phá Khách Đoàn (GIT), Báo Cáo Quản Trị BI & Tối Ưu Hóa Toàn Hệ Thống*

```
[NGÀY 1 - 4: LÀN SÓNG 1 (P0 - LÕI KHÁCH ĐOÀN)]
 ├── T-SP2-01 (M2: Backend Inquiry, Hợp đồng cọc 50%) ──► Unblocks T-SP2-05 (M3: Excel) & T-SP2-06 (M4: UI GIT)
 ├── T-SP2-02 (M2: Backend Room Block & Cut-off Scheduler)──► Unblocks T-SP2-06 (M4: UI Phân phòng đoàn)
 ├── T-SP2-03 (M2: Backend Master Folio & Cost Routing)   ──► Unblocks T-SP2-07 (M4: UI Master Folio)
 └── T-SP2-04 (M1: Thuật toán Phân phòng Tự động Đoàn)    ──► Kết hợp cùng T-SP2-05

[NGÀY 5 - 8: LÀN SÓNG 2 (P1 - FULLSTACK GIT & BÁO CÁO BI CẤP CAO)]
 ├── T-SP2-05 (M3: Parser Import Excel Rooming List)
 ├── T-SP2-06 (M4: Giao diện Quản lý Khách đoàn GIT Portal)
 ├── T-SP2-07 (M4: Giao diện Master Folio & Quyết toán đoàn)
 ├── T-SP2-08 (M3: Backend Quyết toán & Xuất hóa đơn gộp/tách)
 ├── T-SP2-09 (M2: API Báo cáo Quản trị BI Cấp cao)      ──► Unblocks T-SP2-10 (M4: UI Dashboard BI)
 ├── T-SP2-10 (M4: Nâng cấp Executive BI Dashboard)
 ├── T-SP2-11 (M1: RAG AI Safety & Groundedness Audit)
 └── T-SP2-12 (M1: AI Utility Anomaly Detection Điện/Nước)

[NGÀY 9 - 11: LÀN SÓNG 3 (P1/P2 - TỐI ƯU TOÀN DIỆN & BÀN GIAO 100% SRS)]
 ├── T-SP2-13 (M3: Tối ưu SQL Index, Transaction & Redis Cache)
 ├── T-SP2-14 (M1: Tối ưu Latency AI & OCR < 2s)
 ├── T-SP2-15 (M3 + M2: Concurrency Stress-Test 200 CCU không overbooking & RBAC Audit)
 └── T-SP2-16 (M4 + Toàn đội: Polish Design System, A11Y, Đóng gói bàn giao 100% SRS)
```

### Bảng Chi Tiết Task Sprint 2 (Sắp Xếp Theo Thứ Tự Ưu Tiên)

| Mã Task | Mức Ưu Tiên | Tên Hạng Mục / Chức Năng | Điều Kiện Phụ Thuộc / Khả Năng Song Song | Phụ Trách | Estimate | Mục Tiêu Đầu Ra (Deliverable) |
|:---:|:---:|:---|:---|:---:|:---:|:---|
| **T-SP2-01** | **P0 (Cao nhất)** | **Backend Đặt phòng Khách đoàn & Hợp đồng Cọc 50%** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI GIT T-SP2-06 và Excel Parser T-SP2-05. | Member 2 | 3.0d | API Inquiry → Quotation → Contract cọc 50% (bắt buộc đặt cọc tối thiểu 50% mới giữ chỗ đoàn). |
| **T-SP2-02** | **P0 (Cao nhất)** | **Room Block & Scheduler Hạn chót (Cut-off Date)** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho Quỹ phòng đoàn T-SP2-06. | Member 2 | 3.0d | Quỹ phòng riêng đoàn cấn trừ Public Inventory; Scheduler tự động giải phóng phòng chưa gán tên khi đến Cut-off Date. |
| **T-SP2-03** | **P0 (Cao nhất)** | **Master Folio & Định tuyến Chi phí Tự động** | **Khởi chạy ngay Ngày 1.** Mở khóa (`Unblocks`) cho UI Master Folio T-SP2-07. | Member 2 | 2.5d | Cơ chế gom chi phí tiền phòng/tiệc về Master Folio công ty, định tuyến chi phí phát sinh cá nhân về Sub-Folio. |
| **T-SP2-04** | **P1** | **Thuật toán Tự động Phân phòng Đoàn** | **Làm song song Ngày 1-4.** Kết hợp cùng Excel Parser T-SP2-05. | Member 1 | 3.0d | Thuật toán tự động xếp khách từ Rooming List vào các phòng trong Room Block tối ưu theo tầng và loại giường. |
| **T-SP2-05** | **P1** | **Parser Import File Excel Rooming List (U12)** | **Song song Ngày 4-7** (phụ thuộc contract T-SP2-01). | Member 3 | 3.0d | Đọc file Excel/CSV danh sách đoàn: validate CCCD/Passport, tự động tạo hàng loạt bản ghi `StayGuest` gắn vào phòng. |
| **T-SP2-06** | **P1** | **Giao diện Quản lý Khách đoàn (GIT Portal - U15)** | **Song song Ngày 4-8** (phụ thuộc API T-SP2-01, T-SP2-02). | Member 4 | 3.5d | Portal gửi inquiry đoàn, theo dõi hợp đồng & tiến độ cọc 50%, kéo-thả upload Excel, bảng phân phòng đoàn trực quan. |
| **T-SP2-07** | **P1** | **Giao diện Master Folio & Quyết toán Đoàn** | **Song song Ngày 5-8** (phụ thuộc API T-SP2-03). | Member 4 | 2.5d | Màn hình Master Folio cho Thu ngân: Xem chi tiết chi phí gộp, cấn trừ cọc 50%, tách hóa đơn cá nhân khi Check-out. |
| **T-SP2-08** | **P1** | **Backend Quyết toán & Xuất Hóa đơn Gộp/Tách Đoàn** | **Song song Ngày 5-8** (phụ thuộc T-SP2-03). | Member 3 | 2.5d | API Check-out đoàn, cấn trừ tiền cọc, xuất hóa đơn tài chính cho Master Folio và các Sub-Folio. |
| **T-SP2-09** | **P1** | **API Báo cáo Quản trị BI Cấp cao (U21)** | **Làm song song Ngày 4-7.** Mở khóa (`Unblocks`) cho UI Dashboard T-SP2-10. | Member 2 | 2.5d | API tính toán RevPAR, ADR, Công suất phòng (Occupancy Rate), Doanh thu phòng vs F&B phân tách theo Vùng/Chuỗi. |
| **T-SP2-10** | **P1** | **Nâng cấp Executive BI Dashboard (U23)** | **Song song Ngày 7-9** (phụ thuộc API T-SP2-09). | Member 4 | 2.5d | Dashboard cấp Chuỗi: Biểu đồ Recharts xu hướng, Bộ lọc đa cấp Vùng → Khách sạn, Xuất báo cáo PDF/Excel. |
| **T-SP2-11** | **P2** | **Đánh giá & Bảo mật Trợ lý AI (RAG Triad & Safety)** | **Làm song song độc lập Ngày 5-7.** Không bị ai block. | Member 1 | 2.5d | Đánh giá Groundedness, Context Relevance; chống prompt injection; khóa chặn 100% quyền tự ý đặt cọc/booking của AI. |
| **T-SP2-12** | **P2** | **AI Anomaly Detection Điện / Nước Chi nhánh** | **Làm song song độc lập Ngày 6-8.** Không bị ai block. | Member 1 | 2.5d | Mô hình phát hiện rò rỉ, gian lận hoặc biến động bất thường chỉ số tiêu thụ điện nước chi nhánh từ `UtilityReading`. |
| **T-SP2-13** | **P1** | **Tối ưu Hóa SQL Index, Transaction & Redis Cache** | **Làm song song Ngày 8-10.** | Member 3 | 2.5d | Tối ưu hóa Index PostgreSQL, Transaction Isolation Level, cấu hình Redis Cache cho Rate Plans và Master Data. |
| **T-SP2-14** | **P1** | **Tối ưu Hóa Latency Trợ lý AI & OCR (< 2s)** | **Làm song song Ngày 8-10.** | Member 1 | 2.5d | Benchmark và giảm độ trễ phản hồi của Chatbot AI và OCR xuống dưới 2 giây; cấu hình cache ngữ cảnh hội thoại. |
| **T-SP2-15** | **P1** | **Stress-test 200 CCU Đồng thời & Security Audit** | **Thực hiện Ngày 9-11.** | Member 3, Member 2 | 2.5d | Bài kiểm tra tải đồng thời 200 CCU tranh chấp phòng cuối cùng không overbooking; rà soát an toàn RBAC 100% endpoint. |
| **T-SP2-16** | **P1 (Chốt)** | **Hoàn thiện Design System, A11Y & Đóng Gói 100% SRS** | **Thực hiện Ngày 10-11.** Toàn bộ hệ thống hoàn thành. | Member 4, Toàn đội | 2.5d | Rà soát UI Tokens (Radius 12px/16px, Controls 48px/40px), Responsive Mobile, nghiệm thu 100% tính năng SRS sẵn sàng bảo vệ. |

---

## 4. Bảng Tổng Hợp Cân Bằng Khối Lượng Công Việc (Workload Balance)

Kế hoạch 21 ngày đảm bảo khối lượng công việc được phân bổ đồng đều và tối ưu năng lực của cả 4 thành viên:

| Thành Viên | Vai Trò Định Danh | Sprint 1 (Ngày) | Sprint 2 (Ngày) | Tổng Ngày Công | Trọng Tâm Đóng Góp |
|:---:|:---|:---:|:---:|:---:|:---|
| **Member 1** | **AI / Data Specialist** | 10.0d (4 tasks) | 10.5d (4 tasks) | **20.5d** | Chatbot AI, OCR CCCD, Personalization, Phân phòng đoàn, Tối ưu AI |
| **Member 2** | **Backend Core Lead** | 10.0d (4 tasks) | 11.0d (4 tasks) | **21.0d** | Folio Clean, Room Swap BE, Email NOT, GIT Backend, Master Folio, BI APIs |
| **Member 3** | **Fullstack Developer** | 10.0d (4 tasks) | 10.5d (4 tasks) | **20.5d** | UI Scan CCCD, 6 Trang IAM, Excel Rooming List Parser, Quyết toán đoàn, Stress-test |
| **Member 4** | **Frontend Lead** | 10.0d (4 tasks) | 11.0d (4 tasks) | **21.0d** | AI Chat Widget, Timeline Rack View, GIT Portal, Master Folio UI, BI Dashboard |
| **TỔNG CỘNG** | **4 Thành Viên** | **40.0d** | **43.0d** | **83.0d công** | **Bao phủ 100% yêu cầu chức năng & phi chức năng trong SRS** |

---

## 5. Quy Trình Phối Hợp Kỹ Thuật & Tiêu Chuẩn Nghiệm Thu (Definition of Done)

### A. Quy tắc Quản lý Nhánh Git:
1. **Quy ước đặt tên Branch:**  
   `feature/<sprint>-<member>-<task-id>-<short-description>`  
   *(Ví dụ: `feature/sp1-m2-t01-room-swap`, `feature/sp1-m4-t09-ai-chat-widget`).*
2. **Quy trình Code Review & Merge:**  
   Mọi Pull Request (PR) phải có ít nhất 1 Peer Review và Member 2 (Project Lead) phê duyệt trước khi merge vào nhánh `develop`. Tuyệt đối không force-push hay ghi đè nhánh của thành viên khác.

### B. Tiêu chuẩn nghiệm thu (DoD) sau 2 Sprint:
- [ ] **100% Yêu cầu chức năng trong SRS** (từ FR-ORG đến FR-AI) đều có mã nguồn Backend và Giao diện Frontend hoàn chỉnh.
- [ ] Toàn bộ các mục menu trên Sidebar đều có trang đích hoạt động, không còn trang fallback *"Mô-đun đang được phát triển"*.
- [ ] Luồng đặt phòng Khách đoàn (GIT) hoàn tất từ Báo giá → Cọc 50% → Giữ Room Block → Upload Excel Rooming list → Check-out Master Folio.
- [ ] Trợ lý AI Chatbot và AI OCR CCCD hoạt động mượt mà trên giao diện người dùng với thời gian phản hồi < 2s.
- [ ] Kiểm thử tải đồng thời 200 CCU không phát sinh Overbooking; 100% Unit Tests & Integration Tests vượt qua kiểm tra (`BUILD SUCCESS`).
