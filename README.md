# 🏨 Smart Hotel Booking Management System (Hệ Thống Quản Trị & Đặt Phòng Khách Sạn Chuỗi)

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-brightgreen?style=for-the-badge&logo=springboot)
![Liquibase](https://img.shields.io/badge/Liquibase-4.x-2962FF?style=for-the-badge&logo=liquibase)
![React](https://img.shields.io/badge/React-19-blue?style=for-the-badge&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-6.0-blue?style=for-the-badge&logo=typescript)
![Vite](https://img.shields.io/badge/Vite-8.2-purple?style=for-the-badge&logo=vite)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-38bdf8?style=for-the-badge&logo=tailwindcss)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=for-the-badge&logo=redis)
![Keycloak](https://img.shields.io/badge/Keycloak-24.0.4-cyan?style=for-the-badge&logo=keycloak)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ed?style=for-the-badge&logo=docker)
![ShedLock](https://img.shields.io/badge/ShedLock-6.2.0-yellow?style=for-the-badge)

**Smart Hotel Booking Management System** là giải pháp nền tảng công nghệ toàn diện cấp Doanh nghiệp (Enterprise-grade Full-stack Platform) phục vụ quản trị, vận hành và kinh doanh chuỗi khách sạn quy mô đa chi nhánh. Hệ thống giải quyết triệt để các bài toán vận hành thực tế khắt khe: quản lý tồn kho buồng phòng đồng thời cao (High-Concurrency Anti-Overbooking), kinh doanh khách đoàn (Group Booking & Corporate Contracts), định tuyến tài chính hóa đơn (Master Folio & Sub-Folio Routing), báo cáo phân tích quản trị BI thời gian thực (RevPAR, ADR, Occupancy Rate), và tích hợp hệ sinh thái Trợ lý AI (Room Advisor, OCR Check-in, RAG Knowledge Assistant).

---

## 📌 Mục Lục

1. [Sáu Trụ Cột Nghiệp Vụ Doanh Nghiệp (Enterprise Pillars)](#-1-sáu-trụ-cột-nghiệp-vụ-doanh-nghiệp-enterprise-pillars)
2. [Tổng Quan Công Nghệ (Technology Stack)](#-2-tổng-quan-công-nghệ-technology-stack)
3. [Kiến Trúc Hệ Thống (System Architecture)](#-3-kiến-trúc-hệ-thống-system-architecture)
4. [Cấu Trúc Thư Mục Dự Án (Directory Structure)](#-4-cấu-trúc-thư-mục-dự-án-directory-structure)
5. [Yêu Cầu Tiền Đề (Prerequisites)](#-5-yêu-cầu-tiền-đề-prerequisites)
6. [Hướng Dẫn Khởi Chạy Nhanh (Quickstart Guide)](#-6-hướng-dẫn-khởi-chạy-nhanh-quickstart-guide)
   - [Cách 1: Khởi chạy toàn bộ bằng Docker Compose (Khuyên dùng)](#cách-1-khởi-chạy-toàn-bộ-bằng-docker-compose-khuyên-dùng)
   - [Cách 2: Chạy Môi Trường Cục Bộ (Hybrid Local Development)](#cách-2-chạy-môi-trường-cục-bộ-hybrid-local-development)
7. [Danh Mục Cổng & Tài Nguyên (Port Mapping & Endpoints)](#-7-danh-mục-cổng--tài-nguyên-port-mapping--endpoints)
8. [Tài Liệu Swagger UI & Đăng Nhập Tự Động (Bearer Token)](#-8-tài-liệu-swagger-ui--đăng-nhập-tự-động-bearer-token)
9. [Định Danh & Phân Quyền (Keycloak IAM Matrix)](#-9-định-danh--phân-quyền-keycloak-iam-matrix)
10. [Quản Lý & Migration Cơ Sở Dữ Liệu (Liquibase)](#-10-quản-lý--migration-cơ-sở-dữ-liệu-liquibase)
11. [Kiểm Thử & Đảm Bảo Chất Lượng (QA & Testing Suites)](#-11-kiểm-thử--đảm-bảo-chất-lượng-qa--testing-suites)
12. [Xử Lý Sự Cố Thường Gặp (Troubleshooting & FAQs)](#-12-xử-lý-sự-cố-thường-gặp-troubleshooting--faqs)

---

## 🏛 1. Sáu Trụ Cột Nghiệp Vụ Doanh Nghiệp (Enterprise Pillars)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                   SMART HOTEL BOOKING ENTERPRISE ARCHITECTURE                          │
├──────────────────┬──────────────────┬──────────────────┬───────────────────────────────┤
│ 1. INVENTORY     │ 2. GROUP BOOKING │ 3. FINANCE &     │ 4. EXECUTIVE BI               │
│    ENGINE        │    & CONTRACTS   │    MASTER FOLIO  │    ANALYTICS                  │
│ • Sinh sẵn 365d  │ • Inquiry ➔ Quote│ • Master Folio   │ • RevPAR / ADR Realtime       │
│ • Lock 2 lớp     │ • Cọc 50% pháp lý│   (Công ty)      │ • Tỷ lệ lấp đầy (Occupancy)   │
│ • Capacity roll  │ • Quỹ phòng riêng│ • Sub-Folio      │ • Tách Doanh thu Phòng vs F&B │
│ • Trigger APIs   │ • Scheduler Cut-off (Cá nhân)       │ • Phân tách Chuỗi vs Vùng     │
├──────────────────┴──────────────────┴──────────────────┴───────────────────────────────┤
│ 5. IAM & MULTI-PROPERTY                      │ 6. AI INTELLIGENCE SUBSYSTEM            │
│ • Keycloak 24 RBAC (6 Roles phân quyền)      │ • AI Room Advisor (Khuyến nghị phòng)   │
│ • Chuỗi ➔ Vùng ➔ Khách sạn ➔ Buồng phòng     │ • OCR Check-in (Căn cước / Hộ chiếu)    │
│ • Cổng thanh toán VNPay Sandbox & MoMo       │ • RAG Assistant (Hỏi đáp chính sách)    │
└──────────────────────────────────────────────┴─────────────────────────────────────────┘
```

### 1.1. Động cơ Tồn kho & Khóa Chống Đặt Trùng (High-Concurrency Inventory Engine)
- **Chiến lược Sinh sẵn Tồn kho (Pre-generation):** Tự động sinh sẵn 365 ngày mở bán liên tục cho mọi hạng phòng; tự động cuộn thêm ngày vào chân trời tương lai lúc `00:05` hằng ngày qua `RoomAvailabilityScheduler` (kết hợp `ShedLock` phân tán).
- **Cơ chế Khóa 2 Lớp (Dual-locking Protection):**
  - **Lớp 1 (Pessimistic Lock):** `SELECT ... FOR UPDATE` khi giữ chỗ (`holdInventory`), đảm bảo tuần tự hóa các giao dịch tranh chấp cùng một hạng phòng trong mili-giây.
  - **Lớp 2 (Optimistic Lock):** Trường `@Version` trên thực thể `RoomAvailability` bảo vệ toàn vẹn dữ liệu khi có nhiều luồng đồng thời cập nhật.
- **Cơ chế Tự Phục Hồi (Self-Healing):** Nếu truy vấn vào ngày chưa có bản ghi (ví dụ hạng phòng mới tạo chưa kịp chạy cronjob), hệ thống tự động sinh bù dữ liệu mà không làm gián đoạn luồng đặt phòng.
- **Lan Truyền Công Suất & Bảo Trì Buồng Phòng (OOO Sync):** Tự động đồng bộ số lượng phòng vật lý và trừ phòng hỏng/bảo trì (`RoomMaintenanceBlock`) vào tồn kho khả dụng.
- **Bộ API Trigger Thủ Công:** Hỗ trợ Developer và QA kích hoạt sinh tồn kho tức thì theo Hạng phòng (`/trigger/room-type/{id}`), Khách sạn (`/trigger/hotel/{id}`) hoặc Toàn chuỗi (`/trigger/chain`).

### 1.2. Phân Hệ Đặt Phòng Khách Đoàn & Hợp Đồng Cọc (Group Booking & Contracts)
- **Chu trình Đặt đoàn Chuẩn Khách sạn Quốc tế:**
  $$\text{Inquiry (Hỏi phòng)} \longrightarrow \text{Quotation (Báo giá)} \longrightarrow \text{Contract (Hợp đồng)} \longrightarrow \text{Deposit 50\% (Đặt cọc)} \longrightarrow \text{Definite (Chốt đoàn)}$$
- **Quy tắc Cọc 50% Bắt Buộc:** Tuân thủ pháp luật hợp đồng Việt Nam (Bộ luật Dân sự 2015), bắt buộc ghi nhận thanh toán cọc tối thiểu 50% giá trị hợp đồng mới chuyển trạng thái giữ phòng chính thức (`DEFINITE`).
- **Quỹ Phòng Riêng (Room Block Allocation):** Trừ trực tiếp vào tồn kho công cộng để giữ số lượng phòng riêng cho đoàn, hỗ trợ phân bổ từng ngày (`GroupRoomBlockDaily`).
- **Scheduler Tự Động Thu Hồi Phòng Khi Đến Hạn (Cut-off Date Scheduler):** Tự động kiểm tra và giải phóng các phòng chưa được thành viên trong đoàn nhận (`unpicked-up rooms`) trở lại Public Inventory khi chạm mốc Cut-off Date.

### 1.3. Tài Chính Doanh Nghiệp & Master Folio (Finance & Cost Routing)
- **Cơ chế Định tuyến Chi phí Tự động (Automated Billing Routing):**
  - **Master Folio:** Hóa đơn tổng công ty, tự động gom toàn bộ tiền phòng theo hợp đồng và tiệc chiêu đãi.
  - **Sub-Folio:** Hóa đơn cá nhân từng khách đoàn, định tuyến tự động các chi phí phát sinh riêng (minibar, giặt là, ăn uống cá nhân tại nhà hàng).
- **Cổng Thanh Toán Trực Tuyến (Payment Gateways):** Tích hợp hoàn chỉnh cổng VNPay Sandbox và MoMo với mã hóa chữ ký số SHA-512, URL Callback và IPN xử lý bất đồng bộ.
- **Đối Soát Ca Đêm Tự Động (Night Audit Engine):** Tự động quét đối soát doanh thu phòng, cập nhật trạng thái No-Show, chốt ngày kế toán và cuộn tồn kho buồng phòng.
- **Báo Cáo Khai Báo Tạm Trú BCA:** Tự động kết xuất danh sách lưu trú theo định dạng chuẩn của Bộ Công an.

### 1.4. Báo Cáo Quản Trị BI Cấp Cao (Executive BI Reporting)
- Tự động tính toán theo thời gian thực các chỉ số tài chính khách sạn chuẩn quốc tế:
  - **ADR (Average Daily Rate):** Doanh thu phòng trung bình trên mỗi phòng có khách ở.
  - **RevPAR (Revenue Per Available Room):** Doanh thu phòng trung bình trên tổng số phòng khả dụng.
  - **Occupancy Rate (%):** Tỷ lệ lấp đầy phòng thực tế.
  - **Doanh thu Phòng vs F&B vs Dịch vụ khác:** Phân tách chi tiết nguồn thu.
- Hỗ trợ cắt lớp đa chiều: Theo cấp Chuỗi toàn quốc (`CHAIN`), Cụm vùng miền (`REGION`), hoặc Khách sạn cơ sở (`HOTEL`).

### 1.5. Phân Hệ Trí Tuệ Nhân Tạo (Smart AI Subsystem)
- **AI Room Advisor:** Trợ lý tư vấn buồng phòng thông minh, trích xuất nhu cầu ngôn ngữ tự nhiên thành bộ tiêu chí truy vấn, đối soát giá và tình trạng phòng khả dụng thực tế.
- **OCR Identity Document:** Trích xuất tự động thông tin Căn cước công dân (CCCD) và Hộ chiếu phục vụ quy trình Check-in không chạm (Touchless Check-in).
- **RAG Knowledge Assistant:** Hệ thống truy hồi tri thức (Retrieval-Augmented Generation) tra cứu chính sách khách sạn, tiện ích, nội quy từ kho tri thức đã qua kiểm duyệt.

---

## 🛠 2. Tổng Quan Công Nghệ (Technology Stack)

| Tầng kiến trúc | Công nghệ sử dụng | Phiên bản | Vai trò & Đặc điểm kỹ thuật |
| :--- | :--- | :--- | :--- |
| **Backend Core** | Java | `21 (LTS)` | Virtual Threads, Pattern Matching, Record patterns |
| **Backend Framework** | Spring Boot | `4.0.7` | Framework ứng dụng doanh nghiệp chuẩn |
| **Security & IAM** | Keycloak / OAuth2 Resource Server | `24.0.4` | Single Sign-On, JWT Bearer Token, Phân quyền RBAC |
| **Database** | PostgreSQL | `16-alpine` | Cơ sở dữ liệu quan hệ, `btree_gist`, PL/pgSQL Triggers |
| **Database Migration** | Liquibase Runner | `4.31-alpine` | Quản lý phiên bản CSDL 3 tầng, Context Isolation |
| **Distributed Cache** | Redis | `7-alpine` | Lưu trữ bộ nhớ đệm hiệu năng cao |
| **Distributed Lock** | ShedLock | `6.2.0` | Đảm bảo scheduler chỉ chạy trên 1 node duy nhất |
| **API Documentation** | SpringDoc OpenAPI / Swagger UI | `3.0.2` | Sinh tài liệu OpenAPI 3.0, tích hợp Bearer Auth |
| **Frontend Core** | React | `19.x` | Giao diện người dùng Reactive |
| **Frontend Language** | TypeScript | `6.0.x` | Đảm bảo Type-safety toàn diện |
| **Frontend Build Tool** | Vite | `8.2.x` | Build tool siêu tốc với Hot Module Replacement |
| **Styling & Design** | Tailwind CSS | `v4.x` | Hệ thống thiết kế doanh nghiệp tương phản cao |
| **Containerization** | Docker & Docker Compose | `v2+` | Đóng gói và điều phối 6 dịch vụ độc lập |

---

## 🏗 3. Kiến Trúc Hệ Thống (System Architecture)

```text
                               ┌────────────────────────────────┐
                               │   Client: Browser / Postman    │
                               └───────────────┬────────────────┘
                                               │
                                       (HTTP / REST / JWT)
                                               │
                                               ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                            SPRING BOOT APPLICATION BACKEND                                  │
│                                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────────────────────┐  │
│  │                              SECURITY & FILTER CHAIN                                  │  │
│  │  • CorsFilter ➔ JwtBlacklistFilter ➔ BearerTokenAuthenticationFilter ➔ RBAC Converter │  │
│  └───────────────────────────────────────────┬───────────────────────────────────────────┘  │
│                                              ▼                                              │
│  ┌───────────────────────────────────────────────────────────────────────────────────────┐  │
│  │                                  CONTROLLER LAYER                                     │  │
│  │  • AvailabilityController • BookingController • MasterFolioController • ReportController│  │
│  └───────────────────────────────────────────┬───────────────────────────────────────────┘  │
│                                              ▼                                              │
│  ┌───────────────────────────────────────────────────────────────────────────────────────┐  │
│  │                                   SERVICE LAYER                                       │  │
│  │  • RoomAvailabilityGenerationService • GroupContractService • ExecutiveReportService  │  │
│  │  • NightAuditService • MasterFolioService • RoomInventoryHoldService                  │  │
│  └──────────────────────┬─────────────────────────────────────────┬──────────────────────┘  │
│                         ▼                                         ▼                         │
│  ┌───────────────────────────────────────────────┐ ┌─────────────────────────────────────┐  │
│  │             PERSISTENCE LAYER                 │ │             SCHEDULERS              │  │
│  │  • Spring Data JPA Repositories (Pessimistic) │ │  • RoomAvailabilityScheduler (00:05)│  │
│  │  • Optimistic Lock Version Control            │ │  • GroupCutoffScheduler (Mỗi giờ)   │  │
│  └──────────────────────┬────────────────────────┘ └──────────────────┬──────────────────┘  │
└─────────────────────────┼─────────────────────────────────────────────┼─────────────────────┘
                          │                                             │
            ┌─────────────┴─────────────┐                         ┌─────┴─────┐
            ▼                           ▼                         ▼           ▼
┌───────────────────────┐   ┌───────────────────────┐   ┌───────────────────────┐
│     POSTGRESQL 16     │   │        REDIS 7        │   │      KEYCLOAK 24      │
│  • 48 Bảng nghiệp vụ  │   │  • Cache danh mục     │   │  • OIDC Identity      │
│  • Triggers / Indexes │   │  • Blacklist JWT      │   │  • JWT Token Issuer   │
└───────────────────────┘   └───────────────────────┘   └───────────────────────┘
```

---

## 📂 4. Cấu Trúc Thư Mục Dự Án (Directory Structure)

```text
hotel-booking-repo/
├── backend/                               # Mã nguồn Spring Boot Backend (Java 21)
│   ├── src/main/java/vn/edu/utc/hotel_booking/
│   │   ├── common/                        # Hạ tầng dùng chung (Config, DTO, Exception, Mapper)
│   │   │   ├── config/                    # SecurityConfig, OpenApiConfig, RedisConfig, ShedLockConfig
│   │   │   ├── dto/                       # Chuẩn hóa ApiResponse<T>
│   │   │   └── exception/                 # AppException, GlobalExceptionHandler, ErrorCode
│   │   └── modules/                       # Phân rã nghiệp vụ theo Domain-Driven Design (DDD)
│   │       ├── ai/                        # Advisor, OCR, RAG Knowledge Lifecycle, Personalization
│   │       ├── booking/                   # Booking, Group Booking, Group Contract, Room Block
│   │       ├── dashboard/                 # Executive BI Reporting (RevPAR, ADR, Occupancy)
│   │       ├── finance/                   # Master Folio, Billing Routing, Payment, Lodging Export
│   │       ├── identity/                  # Keycloak Auth, Staff, Role-Based Access Control
│   │       ├── inventory/                 # Availability, Pre-generation, Pricing, Maintenance Block
│   │       └── organization/              # Chain, Region, Hotel, RoomType, Physical Room
│   ├── src/main/resources/
│   │   ├── application.yaml               # Cấu hình Spring Boot, SpringDoc, Datasource
│   │   └── db/                            # Kiến trúc Liquibase Database Migration 3 tầng
│   │       ├── db-changelog-root.xml      # Master Changelog điều phối
│   │       ├── changelogs/                # ChangeSet XMLs (quản lý ID, contexts, runOnChange)
│   │       └── sqlFile/                   # Mã nguồn SQL thuần túy (DDL, Triggers, Seed Data)
│   ├── src/test/java/                     # Hệ thống Unit Test, Integration Test, Concurrency Test
│   ├── Dockerfile                         # Container đóng gói ứng dụng Backend
│   └── pom.xml                            # Quản lý dependencies Maven
├── frontend/                              # Mã nguồn React Frontend (TypeScript + Vite)
│   ├── src/                               # Components, Pages, Services (Axios Interceptor), Hooks
│   ├── Dockerfile                         # Container đóng gói ứng dụng Frontend
│   └── package.json                       # Quản lý thư viện npm
├── keycloak/
│   └── realm-export.json                  # Cấu hình Realm, Clients và 6 tài khoản mẫu tự động import
├── document/                              # Tài liệu phân tích yêu cầu (SRS, hướng dẫn Docker)
├── .env.docker                            # Biến môi trường chạy qua Docker
├── .env-example                           # Biến môi trường mẫu chạy cục bộ (Local)
├── docker-compose.yml                     # File điều phối toàn bộ 6 dịch vụ Docker
└── README.md                              # Tài liệu tổng quan dự án
```

---

## 📋 5. Yêu Cầu Tiền Đề (Prerequisites)

| Công cụ | Phiên bản tối thiểu | Mục đích sử dụng |
| :--- | :--- | :--- |
| **Git** | `2.40+` | Quản lý mã nguồn |
| **Docker & Docker Compose** | `Docker 25+ / Compose v2.20+` | **Khuyên dùng** để khởi chạy toàn bộ hệ thống |
| **JDK (Java Development Kit)** | `OpenJDK 21 (LTS)` | Biên dịch & chạy Backend cục bộ |
| **Maven** | `3.9+` (hoặc dùng `mvnw` đi kèm) | Quản lý dependencies Java |
| **Node.js & npm** | `Node 20.19+ / 22 LTS`, `npm 10+` | Biên dịch & chạy Frontend React cục bộ |

---

## 🚀 6. Hướng Dẫn Khởi Chạy Nhanh (Quickstart Guide)

### Cách 1: Khởi chạy toàn bộ bằng Docker Compose (Khuyên dùng)

Cách này sẽ tự động tải, cấu hình và khởi chạy đồng bộ cả 6 dịch vụ trên môi trường Docker khép kín:

```bash
# 1. Clone repository về máy
git clone https://github.com/ThanhF-Xuaan/project-1-hotel-booking.git
cd hotel-booking-repo

# 2. Khởi chạy toàn bộ hệ thống (Background daemon)
docker compose up -d --build

# 3. Theo dõi tiến trình khởi chạy
docker compose ps

# 4. Xem log chi tiết của ứng dụng Backend
docker compose logs -f backend
```

> **Thứ tự khởi động tự động của Docker Compose:**
> 1. `hotel_postgres` (CSDL) ➔ Healthcheck `pg_isready` OK.
> 2. `hotel_redis` (Cache) ➔ Healthcheck `redis-cli ping` OK.
> 3. `hotel_keycloak` (IAM) ➔ Healthcheck `/health/ready` OK, tự động nạp `realm-export.json`.
> 4. `hotel_liquibase` (Migration Runner) ➔ Tự động cập nhật toàn bộ DDL & Seed Data rồi thoát mã `0`.
> 5. `hotel_backend` (Spring Boot) ➔ Kết nối DB, xác thực Keycloak và khởi chạy API.
> 6. `hotel_frontend` (React + Vite) ➔ Lắng nghe cổng 3000 phục vụ người dùng.

Để dừng toàn bộ hệ thống:
```bash
docker compose down
```

---

### Cách 2: Chạy Môi Trường Cục Bộ (Hybrid Local Development)

Dành cho lập trình viên cần debug mã nguồn Java hoặc Hot-reload React:

#### 🔹 Bước 2.1: Khởi chạy hạ tầng cơ bản (DB, Redis, Keycloak) qua Docker
```bash
docker compose up -d postgres_db redis keycloak
```

#### 🔹 Bước 2.2: Cấu hình biến môi trường cục bộ
Tạo tệp `.env` từ `.env-example` ở thư mục gốc:
```powershell
# Trên Windows PowerShell:
Copy-Item .env-example .env
```

#### 🔹 Bước 2.3: Khởi chạy Spring Boot Backend
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
*(Backend sẽ tự động kết nối PostgreSQL qua cổng `localhost:5433` và chạy Liquibase migration).*

#### 🔹 Bước 2.4: Khởi chạy React Frontend
```powershell
cd ../frontend
npm install
npm run dev
```
*(Frontend chạy tại `http://localhost:5173` và tự động kết nối Backend tại `http://localhost:8080/api`).*

---

## 🌐 7. Danh Mục Cổng & Tài Nguyên (Port Mapping & Endpoints)

| Tên Dịch Vụ | Tên Container | Cổng Host | URL Truy Cập Trình Duyệt | Chức Năng Chính |
| :--- | :--- | :--- | :--- | :--- |
| **Frontend Web App** | `hotel_frontend` | **`3000`** *(Docker)*<br>**`5173`** *(Local)* | `http://localhost:3000`<br>`http://localhost:5173` | Giao diện người dùng & Bảng điều khiển quản trị |
| **Backend REST API** | `hotel_backend` | **`8080`** | `http://localhost:8080/api` | API Gateway & Nghiệp vụ hệ thống |
| **Swagger UI Docs** | `hotel_backend` | **`8080`** | **`http://localhost:8080/swagger-ui.html`** | Tài liệu API tương tác, hỗ trợ nút Authorize 🔓 |
| **OpenAPI Specification**| `hotel_backend` | **`8080`** | `http://localhost:8080/v3/api-docs` | Định dạng JSON OpenAPI v3.0 |
| **Keycloak IAM Server** | `hotel_keycloak` | **`8081`** | `http://localhost:8081/admin` | Quản trị Định danh & Phân quyền (`admin` / `admin`) |
| **PostgreSQL Database** | `hotel_postgres` | **`5433`** | `localhost:5433` | CSDL: `hotel_booking_db` \| User: `postgres` \| Pass: `123456` |
| **Redis Cache** | `hotel_redis` | **`6379`** | `localhost:6379` | In-memory Cache & Token Blacklist |
| **Liquibase Runner** | `hotel_liquibase` | *-* | Container độc lập | Tự động migrate schema & kết thúc với mã `0` |

---

## 🔑 8. Tài Liệu Swagger UI & Đăng Nhập Tự Động (Bearer Token)

Giao diện **Swagger UI** của hệ thống đã được tích hợp chuẩn **OpenAPI 3.0 Security Scheme (Bearer JWT)** kèm tính năng **Persist Authorization** (tự động ghi nhớ token trong `localStorage` khi F5).

### Các bước test nhanh API trên Swagger UI:
1. Mở trình duyệt truy cập: **`http://localhost:8080/swagger-ui.html`**.
2. Nhấn vào nút **Authorize 🔓** màu xanh ở góc trên bên phải trang.
3. Lấy nhanh JWT Token của tài khoản Quản trị viên (`admin`) bằng lệnh PowerShell một chạm:
   ```powershell
   $res = Invoke-RestMethod -Uri "http://localhost:8081/realms/hotel-realm/protocol/openid-connect/token" `
       -Method Post -Body @{ client_id="hotel-frontend"; grant_type="password"; username="admin"; password="123456" }
   $res.access_token | Set-Clipboard
   Write-Host "✔ Đã copy JWT Token vào Clipboard!" -ForegroundColor Green
   ```
4. Dán chuỗi token vừa copy vào ô **Value** trong modal (không cần gõ thêm chữ `Bearer `) ➔ Bấm **Authorize** ➔ **Close**.
5. Biểu tượng ổ khóa chuyển thành **đóng 🔒**. Mọi thao tác nhấn **Try it out** ➔ **Execute** trên các API yêu cầu quyền hạn đều sẽ được tự động đính kèm header `Authorization: Bearer <token>`.

---

## 👥 9. Định Danh & Phân Quyền (Keycloak IAM Matrix)

Toàn bộ tài khoản mẫu được tự động nạp sẵn vào hệ thống thông qua file [`keycloak/realm-export.json`](file:///keycloak/realm-export.json).

> 🔑 **Mật khẩu dùng chung cho tất cả các tài khoản thử nghiệm:** **`123456`**

| Username | Email | Quyền Hạn (System Role) | Phạm Vi Quyền Hạn & Nghiệp Vụ |
| :--- | :--- | :--- | :--- |
| **`admin`** | `admin@hotel.utc.edu.vn` | `ROLE_CHAIN_ADMIN` | Toàn quyền quản trị hệ thống, quản lý chuỗi, xem báo cáo BI toàn quốc, trigger sinh tồn kho toàn chuỗi. |
| **`manager_hn`** | `manager.hn@hotel.utc.edu.vn` | `ROLE_PROPERTY_MANAGER` | Quản trị điều hành khách sạn cơ sở Hà Nội, duyệt hợp đồng đoàn, trigger tồn kho chi nhánh. |
| **`sales_group`**| `sales@hotel.utc.edu.vn` | `ROLE_SALES_GROUP` | Chuyên viên khách đoàn: Tạo báo giá, lập hợp đồng khách đoàn, quản lý quỹ phòng riêng (Room Block). |
| **`reception_hn`**| `reception.hn@hotel.utc.edu.vn` | `ROLE_RECEPTIONIST` | Lễ tân khách sạn: Tiếp nhận check-in/out, tra cứu tồn phòng, xử lý đặt phòng cá nhân, quản lý Sub-Folio. |
| **`housekeeping_hn`**| `housekeeping.hn@hotel.utc.edu.vn` | `ROLE_HOUSEKEEPING` | Buồng phòng: Cập nhật tình trạng dọn dẹp, báo hỏng hóc bảo trì phòng (OOO Block). |
| **`customer1`** | `customer1@gmail.com` | `ROLE_CUSTOMER` | Khách hàng trực tuyến: Tra cứu phòng, đặt phòng công khai, thanh toán qua VNPay/MoMo. |

---

## 🗃 10. Quản Lý & Migration Cơ Sở Dữ Liệu (Liquibase)

Hệ thống tuân thủ nghiêm ngặt nguyên tắc **Không bao giờ chạy DDL thủ công**. Toàn bộ thay đổi cơ sở dữ liệu được quản lý qua Liquibase Database Migration với kiến trúc 3 tầng:

1. **Root Master Changelog ([`db-changelog-root.xml`](file:///backend/src/main/resources/db/db-changelog-root.xml)):** Điều phối toàn cục và tự động quét các migration mới qua thẻ `<includeAll path="db/changelogs/incremental" />`.
2. **ChangeSet XML ([`changelogs/`](file:///backend/src/main/resources/db/changelogs/)):** Quản lý metadata, contexts, `runOnChange="true"` cho functions/triggers và `splitStatements="false"` cho khối lệnh PL/pgSQL.
3. **Raw SQL Scripts ([`sqlFile/`](file:///backend/src/main/resources/db/sqlFile/)):** Chứa toàn bộ câu lệnh SQL thuần túy tách biệt.

### Các lệnh Liquibase CLI qua Docker (Không cần cài Liquibase trên máy):
```powershell
# Kiểm tra trạng thái các ChangeSets:
docker compose run --rm liquibase status

# Xem lịch sử các phiên bản đã nạp:
docker compose run --rm liquibase history

# Chạy cập nhật migration mới:
docker compose run --rm liquibase update
```

---

## 🧪 11. Kiểm Thử & Đảm Bảo Chất Lượng (QA & Testing Suites)

Dự án áp dụng quy chuẩn kiểm thử nghiêm ngặt bao phủ Unit Test, Integration Test và Concurrency Stress-test với hơn 100+ test cases.

### Lệnh chạy kiểm thử Backend qua Maven Wrapper:

```powershell
# 1. Chạy toàn bộ test suites của module Tồn kho (Bao gồm Concurrency Test)
cmd /c "set JAVA_HOME=D:\TOOL-CONFIG\jdk-21.0.11&& .\mvnw.cmd test -Dtest=RoomAvailability*Test"

# 2. Kiểm thử chống đặt trùng phòng đồng thời (Concurrency Anti-Overbooking)
cmd /c "set JAVA_HOME=D:\TOOL-CONFIG\jdk-21.0.11&& .\mvnw.cmd test -Dtest=RoomAvailabilityConcurrencyTest"

# 3. Kiểm thử phân hệ Khách đoàn & Room Block
cmd /c "set JAVA_HOME=D:\TOOL-CONFIG\jdk-21.0.11&& .\mvnw.cmd test -Dtest=GroupRoomBlock*Test,GroupContract*Test"

# 4. Kiểm thử Báo cáo Quản trị BI & Master Folio
cmd /c "set JAVA_HOME=D:\TOOL-CONFIG\jdk-21.0.11&& .\mvnw.cmd test -Dtest=ExecutiveReport*Test,MasterFolio*Test"

# 5. Chạy hồi quy toàn diện cross-module (Regression Testing)
cmd /c "set JAVA_HOME=D:\TOOL-CONFIG\jdk-21.0.11&& .\mvnw.cmd test -Dtest=RoomAvailability*Test,HotelRoomTypeServiceTest,NightAuditServiceTest,BookingServiceTest,PublicBookingSearchServiceTest"
```

---

## 💡 12. Xử Lý Sự Cố Thường Gặp (Troubleshooting & FAQs)

| Vấn đề gặp phải | Nguyên nhân | Cách khắc phục |
| :--- | :--- | :--- |
| **Cổng bị chiếm dụng (Port already in use)** | Một ứng dụng khác đang chiếm cổng `3000`, `5433`, `6379`, `8080`, hoặc `8081`. | Chạy lệnh `Get-Process -Id (Get-NetTCPConnection -LocalPort <CỔNG>).OwningProcess \| Stop-Process -Force` để giải phóng cổng. |
| **Lỗi kết nối Keycloak (`Connection Refused`)** | Keycloak đang trong giai đoạn khởi động và nạp realm (mất ~25 giây). | Chờ container `hotel_keycloak` chuyển sang trạng thái `healthy` trước khi gọi API đăng nhập. |
| **Gọi API trên Swagger bị lỗi `401 Unauthorized`** | Chưa nhấn nút Authorize hoặc token đã hết hạn (Access Token có hạn 15 phút). | Lấy token mới bằng lệnh PowerShell (Mục 8) và dán lại vào nút **Authorize 🔓**. |
| **Cần Reset hoàn toàn CSDL và Hệ thống từ đầu** | Muốn xóa toàn bộ dữ liệu demo cũ để chạy lại môi trường sạch sẽ. | Chạy lệnh: `docker compose down -v` rồi khởi động lại: `docker compose up -d --build`. Cờ `-v` sẽ xóa sạch các volume dữ liệu. |

---

<p align="center">
  Phát triển với ❤️ bởi <b>UTC Hotel Engineering Team</b><br>
  <i>Smart Hotel Booking System — Enterprise Architecture Platform</i>
</p>
