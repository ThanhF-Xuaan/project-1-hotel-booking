# 🏨 Smart Hotel Booking Management System (Hệ Thống Quản Trị & Đặt Phòng Khách Sạn Chuỗi)

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-brightgreen?style=for-the-badge&logo=springboot)
![Liquibase](https://img.shields.io/badge/Liquibase-4.31-2962FF?style=for-the-badge&logo=liquibase)
![React](https://img.shields.io/badge/React-19-blue?style=for-the-badge&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-6.0-blue?style=for-the-badge&logo=typescript)
![Vite](https://img.shields.io/badge/Vite-8.2-purple?style=for-the-badge&logo=vite)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-38bdf8?style=for-the-badge&logo=tailwindcss)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=for-the-badge&logo=redis)
![Keycloak](https://img.shields.io/badge/Keycloak-24.0.4-cyan?style=for-the-badge&logo=keycloak)
![MinIO](https://img.shields.io/badge/MinIO-S3_Storage-c72c48?style=for-the-badge&logo=minio)
![Nginx](https://img.shields.io/badge/Nginx-1.27_Gateway-009639?style=for-the-badge&logo=nginx)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions)
![Docker](https://img.shields.io/badge/Docker-9_Containers-2496ed?style=for-the-badge&logo=docker)

> 🌐 **Hệ Thống Đang Triển Khai Thực Tế (Live Production):** [https://smartstay.space](https://smartstay.space)<br>
> 🔗 **Tài Liệu API Trực Tuyến (Swagger UI):** [https://smartstay.space/swagger-ui/index.html](https://smartstay.space/swagger-ui/index.html)<br>
> 🔐 **Cổng Xác Thực Định Danh (Keycloak IAM):** [https://smartstay.space/realms/hotel-realm](https://smartstay.space/realms/hotel-realm)

**Smart Hotel Booking Management System** là giải pháp nền tảng công nghệ toàn diện cấp Doanh nghiệp (Enterprise-grade Full-stack Platform) phục vụ quản trị, vận hành và kinh doanh chuỗi khách sạn quy mô đa chi nhánh. Hệ thống giải quyết triệt để các bài toán vận hành thực tế khắt khe: quản lý tồn kho buồng phòng đồng thời cao (High-Concurrency Anti-Overbooking), kinh doanh khách đoàn (Group Booking & Corporate Contracts), định tuyến tài chính hóa đơn (Master Folio & Sub-Folio Routing), báo cáo phân tích quản trị BI thời gian thực (RevPAR, ADR, Occupancy Rate), và tích hợp hệ sinh thái Trợ lý AI (Room Advisor, OCR Check-in CCCD/Hộ chiếu, RAG Knowledge Assistant).

---

## 📌 Mục Lục

1. [Sáu Trụ Cột Nghiệp Vụ Doanh Nghiệp (Enterprise Pillars)](#-1-sáu-trụ-cột-nghiệp-vụ-doanh-nghiệp-enterprise-pillars)
2. [Tổng Quan Công Nghệ (Technology Stack)](#-2-tổng-quan-công-nghệ-technology-stack)
3. [Kiến Trúc Hệ Thống & 9 Containers (System Architecture)](#-3-kiến-trúc-hệ-thống--9-containers-system-architecture)
4. [Cấu Trúc Thư Mục Dự Án (Directory Structure)](#-4-cấu-trúc-thư-mục-dự-án-directory-structure)
5. [Yêu Cầu Tiền Đề (Prerequisites)](#-5-yêu-cầu-tiền-đề-prerequisites)
6. [Hướng Dẫn Khởi Chạy Nhanh (Quickstart Guide)](#-6-hướng-dẫn-khởi-chạy-nhanh-quickstart-guide)
   - [Cách 1: Khởi chạy toàn bộ bằng Docker Compose](#cách-1-khởi-chạy-toàn-bộ-bằng-docker-compose-khuyên-dùng)
   - [Cách 2: Chạy Môi Trường Cục Bộ (Hybrid Local Development)](#cách-2-chạy-môi-trường-cục-bộ-hybrid-local-development)
7. [Hướng Dẫn Kết Nối Cơ Sở Dữ Liệu Qua DBeaver Cho Dev](#-7-hướng-dẫn-kết-nối-cơ-sở-dữ-liệu-qua-dbeaver-cho-dev)
8. [Danh Mục Cổng & Tài Nguyên (Port Mapping & Endpoints)](#-8-danh-mục-cổng--tài-nguyên-port-mapping--endpoints)
9. [Tài Liệu Swagger UI & Đăng Nhập Tự Động (Bearer Token)](#-9-tài-liệu-swagger-ui--đăng-nhập-tự-động-bearer-token)
10. [Định Danh & Phân Quyền (Keycloak IAM Matrix)](#-10-định-danh--phân-quyền-keycloak-iam-matrix)
11. [Quản Lý & Migration Cơ Sở Dữ Liệu (Liquibase - 74 Bảng Nghiệp Vụ)](#-11-quản-lý--migration-cơ-sở-dữ-liệu-liquibase---74-bảng-nghiệp-vụ)
12. [Hệ Thống Tự Động Hóa CI/CD & Triển Khai OCI (CI/CD Pipeline)](#-12-hệ-thống-tự-động-hóa-cicd--triển-khai-oci-cicd-pipeline)
13. [Kiểm Thử & Đảm Bảo Chất Lượng (QA & 1478+ Test Cases)](#-13-kiểm-thử--đảm-bảo-chất-lượng-qa--1478-test-cases)
14. [Xử Lý Sự Cố Thường Gặp (Troubleshooting & FAQs)](#-14-xử-lý-sự-cố-thường-gặp-troubleshooting--faqs)

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
│ • Cổng thanh toán VNPay Sandbox & MoMo       │ • RAG Assistant & MinIO Storage         │
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
- **Báo Cáo Khai Báo Tạm Trú BCA:** Tự động kết xuất danh sách lưu trú theo định dạng chuẩn của Bộ Công an, tuân thủ Luật Cư trú 2020 và Nghị định 96/2016/NĐ-CP.

### 1.4. Báo Cáo Quản Trị BI Cấp Cao (Executive BI Reporting)
- Tự động tính toán theo thời gian thực các chỉ số tài chính khách sạn chuẩn quốc tế:
  - **ADR (Average Daily Rate):** Doanh thu phòng trung bình trên mỗi phòng có khách ở.
  - **RevPAR (Revenue Per Available Room):** Doanh thu phòng trung bình trên tổng số phòng khả dụng.
  - **Occupancy Rate (%):** Tỷ lệ lấp đầy phòng thực tế.
  - **Doanh thu Phòng vs F&B vs Dịch vụ khác:** Phân tách chi tiết nguồn thu.
- Hỗ trợ cắt lớp đa chiều: Theo cấp Chuỗi toàn quốc (`CHAIN`), Cụm vùng miền (`REGION`), hoặc Khách sạn cơ sở (`HOTEL`).

### 1.5. Phân Hệ Trí Tuệ Nhân Tạo (Smart AI Subsystem)
- **T12 AI Room Recommendation Advisor:** Trợ lý tư vấn buồng phòng thông minh, trích xuất nhu cầu ngôn ngữ tự nhiên qua mô hình Google Gemini thành bộ tiêu chí truy vấn có cấu trúc; thuật toán xếp hạng xác định (Deterministic Ranking) kết hợp nguyên tắc **Zero Business Write Invariant** (tuyệt đối không tự ý ghi/sửa booking hoặc thanh toán).
- **U13 OCR Identity Document Check-in:** Trích xuất tự động thông tin Căn cước công dân (CCCD) và Hộ chiếu qua Tesseract OCR đa ngôn ngữ (Tiếng Việt & Tiếng Anh), hỗ trợ check-in nhanh không chạm.
- **RAG Knowledge Assistant:** Hệ thống truy hồi tri thức (Retrieval-Augmented Generation) tra cứu chính sách khách sạn, tiện ích, nội quy từ kho tri thức đã qua kiểm duyệt.
- **MinIO Object Storage:** Lưu trữ phân tán tài liệu hợp đồng, ảnh CCCD/Hộ chiếu phục vụ đối soát định danh khách lưu trú chuẩn bảo mật.

---

## 🛠 2. Tổng Quan Công Nghệ (Technology Stack)

| Tầng kiến trúc | Công nghệ sử dụng | Phiên bản | Vai trò & Đặc điểm kỹ thuật |
| :--- | :--- | :--- | :--- |
| **Ingress Gateway** | Nginx | `1.27-alpine` | Reverse Proxy, SSL/TLS Termination, Gzip, Buffer Tuning |
| **Backend Core** | Java | `21 (LTS)` | Virtual Threads, Pattern Matching, Record patterns |
| **Backend Framework** | Spring Boot | `4.0.7` | Framework ứng dụng doanh nghiệp chuẩn |
| **Security & IAM** | Keycloak / OAuth2 Resource Server | `24.0.4` | Single Sign-On, JWT Bearer Token, Phân quyền RBAC |
| **Database** | PostgreSQL | `16-alpine` | CSDL quan hệ, 74 bảng nghiệp vụ, PL/pgSQL Triggers |
| **Database Migration** | Liquibase Runner | `4.31-alpine` | Quản lý phiên bản CSDL 3 tầng, Context Isolation |
| **Distributed Cache** | Redis | `7-alpine` | Lưu trữ bộ nhớ đệm hiệu năng cao & Token Blacklist |
| **Object Storage** | MinIO (S3-Compatible) | `Latest` | Lưu trữ hợp đồng PDF, ảnh CCCD và media khách sạn |
| **Distributed Lock** | ShedLock | `6.2.0` | Đảm bảo scheduler chỉ chạy trên 1 node duy nhất |
| **API Documentation** | SpringDoc OpenAPI / Swagger UI | `3.0.2` | Sinh tài liệu OpenAPI 3.0, tích hợp Bearer Auth |
| **Frontend Core** | React | `19.x` | Giao diện người dùng Reactive |
| **Frontend Language** | TypeScript | `6.0.x` | Đảm bảo Type-safety toàn diện |
| **Frontend Build Tool** | Vite | `8.2.x` | Build tool siêu tốc với Hot Module Replacement |
| **Styling & Design** | Tailwind CSS | `v4.x` | Hệ thống thiết kế doanh nghiệp tương phản cao |
| **Containerization** | Docker & Docker Compose | `v2+` | Đóng gói và điều phối 9 dịch vụ độc lập |
| **CI/CD Pipeline** | GitHub Actions | `v4` | 5-Job Quality Gate & Deploy OCI tự động |

---

## 🏗 3. Kiến Trúc Hệ Thống & 9 Containers (System Architecture)

```text
                                ┌────────────────────────────────────────┐
                                │       Internet / Trình duyệt Web       │
                                └───────────────────┬────────────────────┘
                                                    │
                                         (HTTPS Port 443 / SSL)
                                                    │
                                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                          NGINX INGRESS GATEWAY (hotel_nginx)                                    │
│   • SSL Termination (Let's Encrypt / Certbot)       • Gzip Compression & Buffer Optimization   │
│   • / ➔ Frontend SPA       • /api, /v3/api-docs ➔ Backend REST       • /realms ➔ Keycloak       │
│   • /storage ➔ MinIO S3 Bucket                      • /nginx-health ➔ Container Healthcheck     │
└──────────────┬────────────────────────────┬─────────────────────────────┬───────────────────────┘
               │                            │                             │
        (Port 3000 / HTTP)           (Port 8080 / HTTP)            (Port 8080 / HTTP)
               │                            │                             │
               ▼                            ▼                             ▼
    ┌──────────────────────┐     ┌──────────────────────┐      ┌──────────────────────┐
    │   REACT FRONTEND     │     │ SPRING BOOT BACKEND  │      │    KEYCLOAK IAM      │
    │  (hotel_frontend)    │     │   (hotel_backend)    │      │   (hotel_keycloak)   │
    │  • React 19 / Vite   │     │  • Java 21 / Spring  │      │  • OAuth2 / OIDC     │
    │  • Tailwind CSS v4   │     │  • 177 Test Suites   │      │  • Realm: hotel-realm│
    └──────────────────────┘     └──────────┬───────────┘      └──────────┬───────────┘
                                            │                             │
                        ┌───────────────────┼───────────────────┐         │
                        │                   │                   │         │
                        ▼                   ▼                   ▼         │
             ┌─────────────────────┐┌───────────────┐ ┌───────────────────┴───┐
             │    POSTGRESQL 16    ││    REDIS 7    │ │    MINIO OBJECT STORAGE   │
             │   (hotel_postgres)  ││ (hotel_redis) │ │       (hotel_minio)       │
             │ • 74 Bảng nghiệp vụ ││ • Cache & Lock│ │ • Cổng API: 9000          │
             │ • Liquibase Migrated││ • Port: 6379  │ │ • Web Console: 9001       │
             └─────────────────────┘└───────────────┘ └───────────────────────────┘
```

---

## 📂 4. Cấu Trúc Thư Mục Dự Án (Directory Structure)

```text
hotel-booking-repo/
├── .github/workflows/                     # Pipeline CI/CD tự động hóa
│   ├── ci.yml                             # Codebase CI Quality Gate (5 jobs)
│   └── deploy.yml                         # Automated OCI Deployment & 3-Tier Healthchecks
├── backend/                               # Mã nguồn Spring Boot Backend (Java 21)
│   ├── src/main/java/vn/edu/utc/hotel_booking/
│   │   ├── common/                        # Cấu hình dùng chung, Exception, DTO, Security
│   │   └── modules/                       # Phân rã nghiệp vụ theo Domain-Driven Design (DDD)
│   │       ├── ai/                        # T12 Room Advisor, U13 OCR, RAG Knowledge Lifecycle
│   │       ├── booking/                   # Booking, Group Booking, Group Contract, Room Block
│   │       ├── dashboard/                 # Executive BI Reporting (RevPAR, ADR, Occupancy)
│   │       ├── finance/                   # Master Folio, Billing Routing, Payment, Lodging Export
│   │       ├── identity/                  # Keycloak Auth, Staff, Role-Based Access Control
│   │       ├── inventory/                 # Availability, Pre-generation, Pricing, Maintenance Block
│   │       └── organization/              # Chain, Region, Hotel, RoomType, Physical Room
│   ├── src/main/resources/
│   │   ├── application.yaml               # Cấu hình Spring Boot, Datasource, Redis, Keycloak
│   │   └── db/                            # Kiến trúc Liquibase Database Migration 3 tầng
│   │       ├── db-changelog-root.xml      # Master Changelog điều phối
│   │       ├── changelogs/                # ChangeSet XMLs (quản lý metadata, contexts)
│   │       └── sqlFile/                   # Mã nguồn SQL DDL, Triggers, Indexes, Master Data
│   ├── src/test/java/                     # Hệ thống 1478+ Unit, Integration & Concurrency Tests
│   ├── Dockerfile                         # Container đóng gói ứng dụng Backend
│   └── pom.xml                            # Quản lý dependencies Maven
├── frontend/                              # Mã nguồn React Frontend (TypeScript + Vite)
│   ├── src/                               # Components, Pages, Services (Axios Interceptors), Hooks
│   ├── Dockerfile                         # Container đóng gói ứng dụng Frontend (Dev)
│   ├── Dockerfile.prod                    # Multi-stage Nginx Container đóng gói Frontend (Production)
│   └── package.json                       # Quản lý thư viện npm
├── nginx/                                 # Cấu hình Ingress Gateway & SSL
│   ├── nginx.conf                         # Cấu hình Nginx Core, Buffer, Gzip
│   ├── conf.d/
│   │   ├── default.conf.disabled          # Cấu hình Virtual Host cho Local Dev
│   │   └── prod.conf                      # Cấu hình Virtual Host Production HTTPS smartstay.space
│   └── ssl/                               # Chứng chỉ SSL Localhost phục vụ dev
├── keycloak/
│   └── realm-export.json                  # Cấu hình Realm, Clients và tài khoản mẫu tự động import
├── docker-compose.yml                     # Điều phối 9 dịch vụ chuẩn Container Local
├── docker-compose.dev.yml                 # Dev Override: Mở cổng ra host cho DBeaver & RedisInsight
├── docker-compose.prod.yml                # File điều phối Production độc lập trên OCI VM
└── README.md                              # Tài liệu tổng quan dự án
```

---

## 📋 5. Yêu Cầu Tiền Đề (Prerequisites)

| Công cụ | Phiên bản tối thiểu | Mục đích sử dụng |
| :--- | :--- | :--- |
| **Git** | `2.40+` | Quản lý mã nguồn |
| **Docker & Docker Compose** | `Docker 25+ / Compose v2.20+` | **Khuyên dùng** để khởi chạy toàn bộ 9 container |
| **JDK (Java Development Kit)** | `OpenJDK 21 (LTS)` | Biên dịch & chạy Backend cục bộ |
| **Maven** | `3.9+` (hoặc dùng `.\mvnw.cmd` / `./mvnw`) | Quản lý dependencies Java |
| **Node.js & npm** | `Node 20.19+ / 22 LTS`, `npm 10+` | Biên dịch & chạy Frontend React cục bộ |
| **DBeaver Community** | `24.x+` (Tùy chọn) | Công cụ trực quan quản trị cơ sở dữ liệu PostgreSQL |

---

## 🚀 6. Hướng Dẫn Khởi Chạy Nhanh (Quickstart Guide)

### Cách 1: Khởi chạy toàn bộ bằng Docker Compose (Khuyên dùng)

Cách này sẽ tự động tải, cấu hình và khởi chạy đồng bộ cả 9 dịch vụ trên môi trường Docker khép kín:

```bash
# 1. Clone repository về máy
git clone https://github.com/ThanhF-Xuaan/project-1-hotel-booking.git
cd hotel-booking-repo

# 2. Khởi chạy toàn bộ hệ thống
docker compose up -d --build

# 3. Theo dõi trạng thái hoạt động của các container
docker compose ps

# 4. Xem log khởi động Backend
docker compose logs -f backend
```

> **Thứ tự khởi động tự động của Docker Compose:**
> 1. `hotel_postgres` (CSDL) ➔ Healthcheck `pg_isready` OK.
> 2. `hotel_redis` (Cache) ➔ Healthcheck `redis-cli ping` OK.
> 3. `hotel_minio` (S3 Storage) ➔ Healthcheck sẵn sàng lưu trữ.
> 4. `hotel_keycloak` (IAM) ➔ Healthcheck `/health/ready` OK, tự động nạp `realm-export.json`.
> 5. `hotel_liquibase` (Migration Runner) ➔ Tự động cập nhật toàn bộ DDL & Master Data rồi thoát mã `0`.
> 6. `hotel_backend` (Spring Boot) ➔ Kết nối DB, xác thực Keycloak và khởi chạy REST API.
> 7. `hotel_frontend` (React + Vite) ➔ Lắng nghe cổng 3000 phục vụ người dùng.
> 8. `hotel_nginx` (Ingress Gateway) ➔ Lắng nghe cổng 80 & 443 điều phối toàn bộ lưu lượng.

Để dừng toàn bộ hệ thống:
```bash
docker compose down
```

---

### Cách 2: Chạy Môi Trường Cục Bộ (Hybrid Local Development)

Dành cho lập trình viên phát triển hàng ngày (nhẹ máy, có Hot-reload React và Debug Java trong IDE):

#### 🔹 Bước 2.1: Khởi chạy 4 dịch vụ nền qua Docker
```bash
docker compose up -d postgres_db redis minio keycloak
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
# Trên Linux/macOS: ./mvnw spring-boot:run
```
*(Backend tự động kết nối PostgreSQL và nạp Master Data).*

#### 🔹 Bước 2.4: Khởi chạy React Frontend
```powershell
cd ../frontend
npm install
npm run dev
```
*(Frontend chạy tại `http://localhost:3000` và tự động Hot-Reload khi sửa code).*

---

## 🔌 7. Hướng Dẫn Kết Nối Cơ Sở Dữ Liệu Qua DBeaver Cho Dev

Mặc định trong file `docker-compose.yml`, cổng PostgreSQL `5432` chỉ mở nội bộ giữa các container để bảo mật và tránh xung đột với các phiên bản PostgreSQL đã cài sẵn trên máy Windows của lập trình viên.

Khi cần kết nối DBeaver / pgAdmin để truy vấn và kiểm tra dữ liệu, hãy làm theo các bước sau:

### Bước 1: Khởi động Docker kèm tệp `docker-compose.dev.yml`
Tệp `docker-compose.dev.yml` sẽ tự động mở cổng kết nối trực tiếp ra `127.0.0.1`:
```bash
# Mở cổng PostgreSQL phục vụ DBeaver:
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d postgres_db
```

### Bước 2: Thiết lập kết nối mới trên DBeaver
1. Mở DBeaver ➔ Nhấn biểu tượng **New Database Connection (Cắm phích điện 🔌)** ➔ Chọn **PostgreSQL**.
2. Nhập các thông số kết nối chuẩn:

| Trường thông tin | Giá trị cấu hình | Ghi chú |
| :--- | :--- | :--- |
| **Host** | `localhost` hoặc `127.0.0.1` | Địa chỉ máy cục bộ |
| **Port** | `5432` *(hoặc `5433` nếu có cấu hình trong `.env`)* | Cổng được ánh xạ từ container |
| **Database** | `hotel_booking_db` | Tên CSDL hệ thống |
| **Username** | `postgres` | Tài khoản quản trị |
| **Password** | `123456` | Mật khẩu mặc định |

3. Nhấn **Test Connection ...**:
   - DBeaver sẽ tự động tải Driver PostgreSQL nếu là lần kết nối đầu tiên.
   - Nhận thông báo **Connected (Status: Success)** ➔ Bấm **Finish**.

### Bước 3: Các câu lệnh SQL kiểm tra nhanh trên DBeaver
```sql
-- 1. Kiểm tra 74 bảng nghiệp vụ hệ thống trong schema public:
SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';

-- 2. Xem danh sách khách sạn cơ sở:
SELECT id, name, address, phone, status FROM hotels;

-- 3. Xem các hạng phòng và giá niêm yết:
SELECT id, hotel_id, name, base_price, total_quantity, status FROM hotel_room_types;

-- 4. Xem lịch sử các phiên bản migration đã nạp:
SELECT id, author, dateexecuted, exectype FROM databasechangelog ORDER BY dateexecuted DESC;
```

---

## 🌐 8. Danh Mục Cổng & Tài Nguyên (Port Mapping & Endpoints)

| Tên Dịch Vụ | Tên Container | Cổng Host | URL Truy Cập Trình Duyệt | Chức Năng Chính |
| :--- | :--- | :--- | :--- | :--- |
| **Ingress Gateway** | `hotel_nginx` | **`80` / `443`** | `http://localhost`<br>`https://localhost` | API Gateway, SSL Termination, Điều phối toàn hệ thống |
| **Frontend Web App** | `hotel_frontend` | **`3000`** *(Docker)*<br>**`3000`** *(Local)* | `http://localhost:3000` | Giao diện người dùng & Bảng điều khiển quản trị SPA |
| **Backend REST API** | `hotel_backend` | **`8080`** | `http://localhost:8080/api` | API Gateway & Xử lý nghiệp vụ chính |
| **Swagger UI Docs** | `hotel_backend` | **`8080`** | **`http://localhost:8080/swagger-ui.html`** | Tài liệu tương tác API OpenAPI 3.0 (Nút Authorize 🔓) |
| **OpenAPI Spec** | `hotel_backend` | **`8080`** | `http://localhost:8080/v3/api-docs` | Định dạng JSON OpenAPI v3.0 |
| **Keycloak IAM** | `hotel_keycloak` | **`8081`** | `http://localhost:8081/admin` | Quản trị Định danh & Phân quyền (`admin` / `admin`) |
| **MinIO S3 API** | `hotel_minio` | **`9000`** | `http://localhost:9000` | S3-Compatible Object Storage REST API |
| **MinIO Console** | `hotel_minio` | **`9001`** | **`http://localhost:9001`** | Giao diện Web quản trị bucket (`admin` / `hotelbooking123`) |
| **PostgreSQL DB** | `hotel_postgres` | **`5432` / `5433`**| `localhost:5432` | CSDL: `hotel_booking_db` \| User: `postgres` \| Pass: `123456` |
| **Redis Cache** | `hotel_redis` | **`6380` / `6379`**| `localhost:6380` | In-memory Cache & Token Blacklist |
| **Liquibase Runner**| `hotel_liquibase`| *-* | Container độc lập | Tự động migrate schema & thoát với mã `0` |

---

## 🔑 9. Tài Liệu Swagger UI & Đăng Nhập Tự Động (Bearer Token)

Giao diện **Swagger UI** của hệ thống đã được tích hợp chuẩn **OpenAPI 3.0 Security Scheme (Bearer JWT)** kèm tính năng **Persist Authorization** (tự động ghi nhớ token trong `localStorage` khi F5).

### Các bước test nhanh API trên Swagger UI:
1. Mở trình duyệt truy cập: **`http://localhost:8080/swagger-ui.html`** (hoặc `https://smartstay.space/swagger-ui/index.html` trên Production).
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

## 👥 10. Định Danh & Phân Quyền (Keycloak IAM Matrix)

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

## 🗃 11. Quản Lý & Migration Cơ Sở Dữ Liệu (Liquibase - 74 Bảng Nghiệp Vụ)

Hệ thống tuân thủ nghiêm ngặt nguyên tắc **Không bao giờ chạy DDL thủ công trực tiếp trên Production**. Toàn bộ thay đổi cơ sở dữ liệu được quản lý qua Liquibase Database Migration với kiến trúc 3 tầng:

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

## 🚀 12. Hệ Thống Tự Động Hóa CI/CD & Triển Khai OCI (CI/CD Pipeline)

Dự án thiết lập quy trình tự động hóa kiểm thử và triển khai liên tục chuẩn Enterprise trên nền tảng **GitHub Actions** và hạ tầng **Oracle Cloud Infrastructure (OCI)**:

### 12.1. Codebase CI Quality Gate ([`.github/workflows/ci.yml`](file:///d:/Project/MyProject/hotel-booking-repo/.github/workflows/ci.yml))
Mỗi Pull Request hoặc commit vào `develop` bắt buộc phải vượt qua 5 tầng kiểm soát chất lượng nghiêm ngặt:
1. **Backend Verification (Java 21):** Kiểm thử tự động toàn bộ 1478+ test cases, kiểm tra Tesseract OCR engine, Redis độc lập (port 16379), xác thực báo cáo Surefire Reports.
2. **Frontend Verification (Node 22 / Vite):** ESLint (`--max-warnings=0`), Typecheck TypeScript, Vite Production Build, Playwright UI tests.
3. **Docker Packaging & Compose Validation:** Dry-run `docker compose config` kiểm tra cú pháp và tính toàn vẹn của image đóng gói.
4. **Contracts, Tooling & Route Safety:** Kiểm tra định tuyến qua TypeScript AST phân tích `App.tsx`, đảm bảo quyền hạn `CHAIN_ADMIN` và `CUSTOMER` trên từng route.
5. **Analytics Synthetic PostgreSQL Integration:** Kiểm tra tự động các kịch bản tích hợp PostgreSQL trong container sandbox cô lập.

### 12.2. Automated OCI Production Deployment ([`.github/workflows/deploy.yml`](file:///d:/Project/MyProject/hotel-booking-repo/.github/workflows/deploy.yml))
Khi code được merge vào `develop`:
1. **Đóng gói Docker Images trên GitHub Runner:** Tận dụng 4 vCPU / 16GB RAM miễn phí để build và đẩy image lên GitHub Container Registry (GHCR).
2. **Deploy bảo mật qua SSH lên OCI VM:** Kéo image từ GHCR về VPS Ubuntu trên Oracle Cloud (`149.118.33.230`).
3. **Chạy Liquibase Migration tự động:** Cập nhật các bảng CSDL mới trước khi chuyển đổi ứng dụng.
4. **Kiểm tra Sức khỏe 3 Tầng (3-Tier Healthcheck trong 90s):**
   - **Tầng 1 (Gateway):** `https://smartstay.space/nginx-health` $\rightarrow$ Trả về `HTTP 200 OK`.
   - **Tầng 2 (Context):** `https://smartstay.space/v3/api-docs` $\rightarrow$ Trả về `HTTP 200 OK`.
   - **Tầng 3 (Data Query):** `https://smartstay.space/api/v1/public/booking-search/destinations` $\rightarrow$ Truy vấn CSDL thật trả về `code: 200`.
5. **Cơ chế Tự Động Rollback (Zero-Downtime Safeguard):** Nếu phát hiện bất kỳ lỗi nào ở 3 tầng kiểm tra, hệ thống tự động hoàn tác (revert) về commit an toàn trước đó, đảm bảo trang web không bao giờ bị gián đoạn.

---

## 🧪 13. Kiểm Thử & Đảm Bảo Chất Lượng (QA & 1478+ Test Cases)

Dự án áp dụng quy chuẩn kiểm thử nghiêm ngặt bao phủ Unit Test, Integration Test và Concurrency Stress-test với hơn **177 test suites** và **1478+ test cases**.

### Lệnh chạy kiểm thử Backend qua Maven Wrapper (Đa nền tảng):

```bash
# 1. Chạy toàn bộ 1478+ bài kiểm thử tự động:
./mvnw test
# Trên Windows PowerShell: .\mvnw.cmd test

# 2. Kiểm thử chống đặt trùng phòng đồng thời (Concurrency Anti-Overbooking):
./mvnw test -Dtest=RoomAvailabilityConcurrencyTest

# 3. Kiểm thử phân hệ Khách đoàn & Quỹ phòng riêng (Group Room Block):
./mvnw test -Dtest=GroupRoomBlock*Test,GroupContract*Test

# 4. Kiểm thử Báo cáo Quản trị BI & Master Folio:
./mvnw test -Dtest=ExecutiveReport*Test,MasterFolio*Test

# 5. Kiểm thử phân hệ Trí tuệ Nhân tạo & Khuyến nghị phòng:
./mvnw test -Dtest=PublicRoomAdvisor*Test,SessionSnapshot*Test
```

---

## 💡 14. Xử Lý Sự Cố Thường Gặp (Troubleshooting & FAQs)

| Vấn đề gặp phải | Nguyên nhân | Cách khắc phục |
| :--- | :--- | :--- |
| **Cổng bị chiếm dụng (Port already in use)** | Một ứng dụng khác đang chiếm cổng `3000`, `5432`, `6379`, `8080`, hoặc `8081`. | Chạy lệnh `Get-Process -Id (Get-NetTCPConnection -LocalPort <CỔNG>).OwningProcess \| Stop-Process -Force` để giải phóng cổng. |
| **DBeaver không kết nối được PostgreSQL** | Container PostgreSQL mặc định chỉ mở cổng nội bộ container (`expose: 5432`). | Khởi chạy qua lệnh dev: `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d postgres_db` (Xem Mục 7). |
| **Lỗi kết nối Keycloak (`Connection Refused`)** | Keycloak đang trong giai đoạn khởi động và nạp realm (mất ~25 giây). | Chờ container `hotel_keycloak` chuyển sang trạng thái `healthy` trước khi gọi API đăng nhập. |
| **Gọi API trên Swagger bị lỗi `401 Unauthorized`** | Chưa nhấn nút Authorize hoặc token đã hết hạn (Access Token có hạn 15 phút). | Lấy token mới bằng lệnh PowerShell (Mục 9) và dán lại vào nút **Authorize 🔓**. |
| **Deploy OCI bị lỗi Docs=502 khi có migration mới** | Migration mới được gắn context đặc biệt mà Liquibase chưa quét. | Cập nhật file `20261103-...xml` bỏ thuộc tính `context` hoặc chạy lệnh nạp thủ công 1 lần trên OCI. |
| **Cần Reset hoàn toàn CSDL và Hệ thống từ đầu** | Muốn xóa toàn bộ dữ liệu demo cũ để chạy lại môi trường sạch sẽ. | Chạy lệnh: `docker compose down -v` rồi khởi động lại: `docker compose up -d --build`. Cờ `-v` sẽ xóa sạch các volume dữ liệu. |

---

<p align="center">
  Phát triển với ❤️ bởi <b>UTC Hotel Engineering Team</b><br>
  <i>Smart Hotel Booking System — Enterprise Architecture Platform</i>
</p>
