# 🏨 Hệ Thống Quản Lý & Đặt Phòng Khách Sạn (Hotel Booking System)

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

Hệ thống Quản lý và Đặt phòng Khách sạn (Hotel Booking System) là một giải pháp Enterprise Full-stack hiện đại, hỗ trợ quản lý danh mục phòng, đặt phòng, tính giá linh hoạt (Pricing Engine), xác thực người dùng tập trung (IAM với Keycloak), bộ nhớ đệm hiệu năng cao (Redis Cache) và quản lý phiên bản cơ sở dữ liệu tự động (Liquibase Database Migration).

---

## 📌 Mục Lục
- [Tổng Quan Công Nghệ](#-tổng-quan-công-nghệ)
- [Cấu Trúc Thư Mục Dự Án](#-cấu-trúc-thư-mục-dự-án)
- [Yêu Cầu Tiền Đề (Prerequisites)](#-yêu-cầu-tiền-đề-prerequisites)
- [Hướng Dẫn Setup Chi Tiết](#-hướng-dẫn-setup-chi-tiết)
  - [Bước 1: Clone Repository](#bước-1-clone-repository)
  - [Bước 2: Cấu hình Môi Trường (.env)](#bước-2-cấu-hình-môi-trường-env)
  - [Bước 3: Khởi Chạy Dự Án](#bước-3-khởi-chạy-dự-án)
    - [Cách 1: Khởi chạy toàn bộ bằng Docker Compose (Khuyên dùng)](#cách-1-khởi-chạy-toàn-bộ-bằng-docker-compose-khuyên-dùng)
    - [Cách 2: Chạy Môi Trường Cục Bộ (Local Development)](#cách-2-chạy-môi-trường-cục-bộ-local-development)
- [Danh Sách Cổng & Dịch Vụ](#-danh-sách-cổng--dịch-vụ)
- [Quản Lý & Migration Cơ Sở Dữ Liệu (Liquibase)](#-quản-lý--migration-cơ-sở-dữ-liệu-liquibase)
- [Định Danh & Phân Quyền (Keycloak IAM)](#-định-danh--phân-quyền-keycloak-iam)
- [Lưu Ý & Khắc Phục Lỗi Thường Gặp](#-lưu-ý--khắc-phục-lỗi-thường-gặp)

---

## 🚀 Tổng Quan Công Nghệ

### 🛠 Backend
* **Java 21**: Ngôn ngữ lập trình chính, tận dụng các tính năng mới của JDK 21.
* **Spring Boot 4.0.7**: Framework phát triển ứng dụng Java Backend.
* **Liquibase 4.x**: Quản lý phiên bản cơ sở dữ liệu tự động (Schema Versioning, ChangeSet Tracking, Context Isolation).
* **Spring Data JPA / Hibernate**: Quản lý truy vấn và đối soát schema tự động với `ddl-auto: validate`.
* **Spring Security & OAuth2 Resource Server**: Bảo mật ứng dụng, xác thực JWT Token được phát hành bởi Keycloak.
* **Keycloak Admin Client**: Quản lý tài khoản, phân quyền và kết nối Keycloak IAM Server.
* **SpringDoc OpenAPI 3 / Swagger UI**: Tự động sinh tài liệu API tương tác.
* **Lombok & MapStruct**: Tối ưu mã nguồn, tự động ánh xạ giữa Entity và DTO.

### 🎨 Frontend
* **React 19**: Library xây dựng giao diện người dùng reactive.
* **TypeScript 6.0**: Đảm bảo type-safety và nâng cao trải nghiệm phát triển.
* **Vite 8.2**: Build tool siêu nhanh hỗ trợ HMR (Hot Module Replacement).
* **Tailwind CSS v4**: Utility-first CSS framework cho giao diện hiện đại, responsive.

### 🗄 Cơ Sở Dữ Liệu & Cache
* **PostgreSQL 16**: Cơ sở dữ liệu quan hệ lưu trữ toàn bộ nghiệp vụ, tích hợp extension `btree_gist`, trigger tự động hóa và exclusion constraints.
* **Redis 7**: Bộ nhớ đệm (In-memory caching) giúp tăng tốc độ truy vấn danh mục phòng và giá.

### 🔐 Định Danh & Bảo Mật (IAM)
* **Keycloak 24.0.4**: Giải pháp quản lý định danh và truy cập tập trung (Single Sign-On / OIDC / OAuth2).

### 🐳 Containerization & Orchestration
* **Docker & Docker Compose**: Đóng gói toàn bộ ứng dụng thành các dịch vụ độc lập, giúp đồng bộ môi trường phát triển và triển khai.

---

## 📂 Cấu Trúc Thư Mục Dự Án

```text
hotel-booking-repo/
├── backend/                  # Mã nguồn Spring Boot Backend (Java 21)
│   ├── src/main/java/        # Controllers, Services, Repositories, Entities
│   ├── src/main/resources/
│   │   ├── application.yaml  # Cấu hình Spring Boot & Liquibase
│   │   └── db/               # Kiến trúc Liquibase Migration 3 tầng
│   │       ├── db-changelog-root.xml        # Master Changelog điều phối
│   │       ├── changelogs/                  # ChangeSet XMLs (Context, runOnChange, splitStatements)
│   │       │   ├── 01-db-init-schema.xml    # Baseline 44 bảng DDL
│   │       │   ├── 02-db-functions-and-triggers.xml
│   │       │   ├── 03-db-indexes.xml
│   │       │   ├── 04-db-master-data.xml    # context="master-data"
│   │       │   ├── 05-db-dev-seed.xml       # context="dev-seed"
│   │       │   └── incremental/             # ChangeSets phát sinh trong tương lai
│   │       └── sqlFile/                     # File mã nguồn SQL thuần
│   │           ├── 01-init-schema.sql
│   │           ├── 02-functions-and-triggers.sql
│   │           ├── 03-indexes.sql
│   │           ├── 04-master-data.sql
│   │           ├── 05-dev-seed.sql
│   │           └── incremental/             # SQL scripts phục vụ incremental
│   ├── Dockerfile            # Cấu hình đóng gói Docker image cho Backend
│   └── pom.xml               # Quản lý dependencies Maven
├── frontend/                 # Mã nguồn React Frontend (TypeScript + Vite)
│   ├── src/                  # React components, pages, hooks, styles
│   ├── Dockerfile            # Cấu hình đóng gói Docker image cho Frontend
│   └── package.json          # Quản lý npm packages
├── .env-example              # Mẫu tệp biến môi trường hệ thống
├── docker-compose.yml        # Định nghĩa các dịch vụ Docker (DB, Redis, Keycloak, Backend, Frontend)
└── README.md                 # Tài liệu hướng dẫn dự án
```

---

## 📋 Yêu Cầu Tiền Đề (Prerequisites)

Trước khi khởi chạy dự án, hãy đảm bảo máy tính của bạn đã cài đặt:

1. **Git**: Quản lý mã nguồn.
2. **Docker & Docker Desktop** (Bao gồm Docker Compose v2+): *(Khuyên dùng để khởi chạy toàn bộ hệ thống nhanh nhất)*.
3. **JDK 21** và **Maven 3.8+**: *(Cần thiết khi chạy/debug Backend cục bộ)*.
4. **Node.js (v20.19+ hoặc v22 LTS)** và **npm (v9+)**: *(Cần thiết khi chạy Frontend cục bộ)*.

---

## ⚙️ Hướng Dẫn Setup Chi Tiết

### Bước 1: Clone Repository
Mở Terminal / PowerShell và chạy lệnh:
```bash
git clone https://github.com/ThanhF-Xuaan/project-1-hotel-booking.git
cd hotel-booking-repo
```

---

### Bước 2: Cấu hình Môi Trường (.env)

#### 1. Cấu hình biến môi trường gốc (Root `.env`):
Tạo file `.env` từ file mẫu `.env-example` ở thư mục gốc:

- **Linux / macOS:**
  ```bash
  cp .env-example .env
  ```
- **Windows (PowerShell):**
  ```powershell
  Copy-Item .env-example .env
  ```
- **Windows (CMD):**
  ```cmd
  copy .env-example .env
  ```

*Nội dung cấu hình biến môi trường chuẩn:*
```properties
# Cơ sở dữ liệu PostgreSQL 16
DB_HOST=localhost
DB_PORT=5433
DB_NAME=hotel_booking_db
DB_USER=postgres
DB_PASSWORD=123456

# Liquibase Database Migration
SPRING_LIQUIBASE_ENABLED=true
LIQUIBASE_CONTEXTS=master-data,dev-seed

# Redis Cache 7
REDIS_HOST=localhost
REDIS_PORT=6379

# Keycloak IAM 24
KEYCLOAK_PORT=8081
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_REALM=hotel-realm
KEYCLOAK_CLIENT_ID=hotel-frontend
KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/hotel-realm
KEYCLOAK_JWK_SET_URI=http://localhost:8081/realms/hotel-realm/protocol/openid-connect/certs

# Application Ports
BACKEND_PORT=8080
FRONTEND_PORT=3000

# Frontend React / Vite
VITE_API_BASE_URL=http://localhost:8080/api
VITE_KEYCLOAK_URL=http://localhost:8081
VITE_KEYCLOAK_REALM=hotel-realm
VITE_KEYCLOAK_CLIENT_ID=hotel-frontend
```

#### 2. Cấu hình biến môi trường Frontend (`frontend/.env`):
Tạo file `.env` cho thư mục `frontend`:

- **Linux / macOS:**
  ```bash
  cp frontend/.env-example frontend/.env
  ```
- **Windows (PowerShell / CMD):**
  ```powershell
  Copy-Item frontend\.env-example frontend\.env
  ```

---

### Bước 3: Khởi Chạy Dự Án

Bạn có thể lựa chọn 1 trong 2 cách triển khai bên dưới:

---

#### Cách 1: Khởi chạy toàn bộ bằng Docker Compose (Khuyên dùng)

Cách này sẽ tự động tải, build và khởi chạy tất cả 5 dịch vụ (PostgreSQL, Redis, Keycloak, Backend, Frontend) trong các Docker Containers. Spring Boot Backend sẽ tự động chạy Liquibase Migration khi kết nối tới PostgreSQL.

1. **Khởi chạy hệ thống:**
   ```bash
   docker compose up -d --build
   ```
   *(Lần đầu tiên chạy có thể mất vài phút để tải Docker Images và build Backend/Frontend).*

2. **Kiểm tra trạng thái các container:**
   ```bash
   docker compose ps
   ```

3. **Xem logs của ứng dụng:**
   * Xem log tất cả dịch vụ:
     ```bash
     docker compose logs -f
     ```
   * Xem log chi tiết của Backend (quá trình Liquibase migration):
     ```bash
     docker compose logs -f backend
     ```

4. **Dừng toàn bộ hệ thống:**
   ```bash
   docker compose down
   ```

---

#### Cách 2: Chạy Môi Trường Cục Bộ (Local Development)

Dành cho nhà phát triển muốn code và debug trực tiếp trên máy cục bộ (Hot-reload Frontend & Live-debug Backend).

##### 🔹 3.1. Khởi chạy Dịch vụ Cơ sở hạ tầng (Database, Cache, Keycloak) qua Docker
```bash
docker compose up -d postgres_db redis keycloak
```

##### 🔹 3.2. Khởi chạy Backend (Spring Boot)
1. Di chuyển vào thư mục `backend`:
   ```bash
   cd backend
   ```
2. Khởi chạy ứng dụng:
   * **Windows:**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   * **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```
   *(Khi Backend khởi động, Liquibase sẽ tự động áp dụng các ChangeSets vào database `hotel_booking_db`)*.

##### 🔹 3.3. Khởi chạy Frontend (React + Vite)
1. Di chuyển vào thư mục `frontend` (từ thư mục gốc):
   ```bash
   cd frontend
   ```
2. Cài đặt thư viện dependencies:
   ```bash
   npm install
   ```
3. Chạy môi trường Development:
   ```bash
   npm run dev
   ```
   *(Frontend Local Dev sẽ chạy tại `http://localhost:5173`)*.

---

## 🌐 Danh Sách Cổng & Dịch Vụ

Sau khi khởi chạy thành công, các dịch vụ sẽ hoạt động tại các địa chỉ sau:

| Dịch vụ | Địa chỉ Web / Endpoint | Tài khoản mặc định / Ghi chú |
| :--- | :--- | :--- |
| **Frontend Web App** | `http://localhost:3000` (Docker) / `http://localhost:5173` (Local Dev) | Giao diện cho khách hàng & Quản trị viên |
| **Backend REST API** | `http://localhost:8080/api` | Spring Boot API Service |
| **Swagger UI (API Docs)** | `http://localhost:8080/swagger-ui.html` | Tài liệu API tương tác SpringDoc |
| **Keycloak Admin Console** | `http://localhost:8081` | **User:** `admin` \| **Pass:** `admin` |
| **PostgreSQL Database** | `localhost:5433` | **DB Name:** `hotel_booking_db` \| **User:** `postgres` \| **Pass:** `123456` |
| **Redis Cache** | `localhost:6379` | In-memory Data Store |
| **Liquibase Runner** | Container `hotel_liquibase` | Tự động chạy migration khi khởi động rồi kết thúc (`exit 0`) |

---

## 🗃 Quản Lý & Migration Cơ Sở Dữ Liệu (Liquibase)

Hệ thống sử dụng **Liquibase 4.x chạy qua Docker Container độc lập** (`liquibase/liquibase:4.31-alpine`) để quản lý phiên bản cơ sở dữ liệu tự động trước khi Backend khởi chạy, loại bỏ hoàn toàn rủi ro Schema Drift.

### 1. Kiến trúc 3 tầng quản lý:
* **Root Entrypoint ([`db-changelog-root.xml`](file:///backend/src/main/resources/db/db-changelog-root.xml)):** Điều phối thứ tự nạp các ChangeSet và tự động quét các migration mới qua `<includeAll path="db/changelogs/incremental" />`.
* **ChangeSet XMLs ([`changelogs/`](file:///backend/src/main/resources/db/changelogs/)):** Quản lý phiên bản, Contexts, điều khiển `runOnChange="true"` cho functions/triggers và `splitStatements="false"` cho khối lệnh PL/pgSQL.
* **Raw SQL Scripts ([`sqlFile/`](file:///backend/src/main/resources/db/sqlFile/)):** Chứa toàn bộ câu lệnh DDL, Functions, Indexes và Data Scripts thuần túy.

### 2. Phân tách Liquibase Contexts:
* `master-data`: Dữ liệu bắt buộc hệ thống (Roles, Permissions, Thuế, Tiền tệ...) — được kích hoạt trên **mọi môi trường** (Dev, Staging, Production).
* `dev-seed`: Dữ liệu mẫu (Khách sạn mẫu, phòng mẫu, menu món ăn...) — **chỉ kích hoạt trên môi trường Dev / Local**.

### 3. Thực thi Lệnh Liquibase CLI qua Docker (Ad-hoc Commands):
Bạn có thể thực hiện mọi thao tác quản trị database bằng Liquibase trực tiếp qua Docker mà **không cần cài đặt Liquibase trên máy trạm**:
* **Kiểm tra trạng thái migration:**
  ```powershell
  docker compose run --rm liquibase status
  ```
* **Xem lịch sử các ChangeSet đã chạy:**
  ```powershell
  docker compose run --rm liquibase history
  ```
* **Chạy cập nhật migration mới:**
  ```powershell
  docker compose run --rm liquibase update
  ```

### 4. Quy trình thêm Migration mới (Incremental Migration):
Khi phát triển tính năng mới cần thay đổi DB:
1. Tạo file XML trong `backend/src/main/resources/db/changelogs/incremental/<YYYY>/YYYYMMDD-HHmmss-<mo_ta>.xml`.
2. Tạo file SQL tương ứng trong `backend/src/main/resources/db/sqlFile/incremental/<YYYY>/YYYYMMDD-HHmmss-<mo_ta>.sql`.
3. Khởi động lại hệ thống — Container Liquibase sẽ tự động phát hiện, áp dụng và ghi nhận vào bảng `databasechangelog`.

---

## 🔐 Định Danh & Phân Quyền (Keycloak IAM)

Hệ thống sử dụng **Keycloak 24.0.4 chạy thuần Docker** để quản lý định danh và phân quyền tập trung (RBAC).

### 1. Cơ chế Declarative Auto-Import:
* Khi khởi động, Keycloak tự động nạp cấu hình Realm `hotel-realm`, Clients (`hotel-frontend`, `hotel-backend`), Roles và Users mẫu từ tệp [`keycloak/realm-export.json`](file:///keycloak/realm-export.json).
* Dữ liệu runtime được lưu trữ an toàn trong Docker Volume `keycloak_data`.

### 2. Danh sách Tài khoản & Mật khẩu Mặc định:
> **Mật khẩu chung cho tất cả tài khoản mẫu:** `123456`

| Username | Email | Họ & Tên | Role (Quyền hạn) | Chức năng chính |
| :--- | :--- | :--- | :--- | :--- |
| **`admin`** | `admin@hotel.utc.edu.vn` | System Admin | `CHAIN_ADMIN` | Quản trị toàn chuỗi khách sạn |
| **`manager_hn`** | `manager.hn@hotel.utc.edu.vn` | Manager Hà Nội | `PROPERTY_MANAGER` | Quản trị khách sạn cơ sở |
| **`reception_hn`** | `reception.hn@hotel.utc.edu.vn` | Lễ Tân Hà Nội | `RECEPTIONIST` | Tiếp nhận đặt phòng, check-in |
| **`housekeeping_hn`** | `housekeeping.hn@hotel.utc.edu.vn` | Buồng Phòng Hà Nội | `HOUSEKEEPING` | Cập nhật tình trạng phòng |
| **`customer1`** | `customer1@gmail.com` | Nguyễn Văn An | `CUSTOMER` | Khách hàng đặt phòng |

### 3. Hướng dẫn Force Re-import Cấu hình Keycloak:
Khi bạn cập nhật file `keycloak/realm-export.json` (thêm Role hoặc Client mới), hãy chạy lệnh sau để ép Keycloak nạp lại cấu hình:
```powershell
docker compose rm -s -v -f keycloak
docker volume rm hotel-booking-repo_keycloak_data
docker compose up -d keycloak
```

---

## 💡 Lưu Ý & Khắc Phục Lỗi Thường Gặp

1. **Xung đột cổng (Port In Use):**
   * Đảm bảo các cổng `3000`, `5433`, `6379`, `8080`, `8081` trên máy bạn chưa bị chiếm dụng bởi ứng dụng khác.
2. **Backend báo lỗi ddl-auto validate mismatch:**
   * Hãy chắc chắn rằng bạn đã định nghĩa đúng kiểu dữ liệu trong Java Entity khớp với cột trong bảng DB tương ứng.
3. **Reset hoàn toàn Hệ thống (Clean Reset):**
   * Nếu bạn muốn xóa toàn bộ dữ liệu (PostgreSQL, Redis, Keycloak) và khởi tạo lại sạch sẽ từ đầu:
     ```bash
     docker compose down -v
     docker compose up -d --build
     ```
     *(Cờ `-v` sẽ xóa sạch toàn bộ Docker Volumes `pg_data`, `redis_data` và `keycloak_data`)*.

---

Chúc bạn có trải nghiệm phát triển tuyệt vời với **Hotel Booking System**! 🚀
