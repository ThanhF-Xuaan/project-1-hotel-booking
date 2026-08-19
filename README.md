# 🏨 Hệ Thống Quản Lý & Đặt Phòng Khách Sạn (Hotel Booking System)

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-brightgreen?style=for-the-badge&logo=springboot)
![React](https://img.shields.io/badge/React-19-blue?style=for-the-badge&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-6.0-blue?style=for-the-badge&logo=typescript)
![Vite](https://img.shields.io/badge/Vite-8.2-purple?style=for-the-badge&logo=vite)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-38bdf8?style=for-the-badge&logo=tailwindcss)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=for-the-badge&logo=redis)
![Keycloak](https://img.shields.io/badge/Keycloak-24.0.4-cyan?style=for-the-badge&logo=keycloak)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ed?style=for-the-badge&logo=docker)

Hệ thống Quản lý và Đặt phòng Khách sạn (Hotel Booking System) là một giải pháp Enterprise Full-stack hiện đại, hỗ trợ quản lý danh mục phòng, đặt phòng, tính giá linh hoạt (Pricing Engine), xác thực người dùng tập trung (IAM với Keycloak) và bộ nhớ đệm hiệu năng cao (Redis Cache).

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
- [Khởi Tạo Cơ Sở Dữ Liệu (Database Scripts)](#-khởi-tạo-cơ-sở-dữ-liệu-database-scripts)
- [Lưu Ý & Khắc Phục Lỗi Thường Gặp](#-lưu-ý--khắc-phục-lỗi-thường-gặp)

---

## 🚀 Tổng Quan Công Nghệ

### 🛠 Backend
* **Java 21**: Ngôn ngữ lập trình chính, tận dụng các tính năng mới của JDK 21.
* **Spring Boot 4.0.7**: Framework phát triển ứng dụng Java Backend.
* **Spring Data JPA / Hibernate**: Quản lý truy vấn và tương tác với cơ sở dữ liệu PostgreSQL.
* **Spring Security & OAuth2 Resource Server**: Bảo mật ứng dụng, xác thực JWT Token được phát hành bởi Keycloak.
* **Keycloak Admin Client**: Quản lý tài khoản, phân quyền và kết nối Keycloak IAM Server.
* **SpringDoc OpenAPI 3 / Swagger UI**: Tự động sinh tài liệu API tương tác.
* **Lombok & MapStruct**: Tối ưu mã nguồn, tự động ánh xạ giữa Entity và DTO.

### 🎨 Frontend
* **React 19**: Library xây dựng giao diện người dùng reactive.
* **TypeScript**: Đảm bảo type-safety và nâng cao trải nghiệm phát triển.
* **Vite 8**: Build tool siêu nhanh hỗ trợ HMR (Hot Module Replacement).
* **Tailwind CSS v4**: Utility-first CSS framework cho giao diện hiện đại, responsive.

### 🗄 Cơ Sở Dữ Liệu & Cache
* **PostgreSQL 16**: Cơ sở dữ liệu quan hệ lưu trữ dữ liệu hệ thống, tích hợp các SQL scripts tự động khởi tạo dữ liệu master và động cơ tính giá (Pricing Engine).
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
│   ├── src/                  # Controllers, Services, Repositories, Entities
│   ├── Dockerfile            # Cấu hình đóng gói Docker image cho Backend
│   └── pom.xml               # Quản lý dependencies Maven
├── frontend/                 # Mã nguồn React Frontend (TypeScript + Vite)
│   ├── src/                  # React components, pages, hooks, styles
│   ├── Dockerfile            # Cấu hình đóng gói Docker image cho Frontend
│   └── package.json          # Quản lý npm packages
├── database/                 # Thư mục SQL scripts khởi tạo PostgreSQL DB
│   ├── 01-init-schema.sql
│   ├── 02-functions-and-triggers.sql
│   ├── 03-indexes-and-seed-data.sql
│   ├── 04-seed-data-global-master-data.sql
│   ├── 05-seed-data-organization-base.sql
│   ├── 06-seed-data-catalog-and-room-configuration.sql
│   ├── 07-seed-data-inventory-mapping-intance.sql
│   └── 08-pricing-engine.sql
├── .env-example              # Mẫu tệp biến môi trường hệ thống
├── docker-compose.yml        # Định nghĩa các dịch vụ Docker (DB, Redis, Keycloak, Backend, Frontend)
└── README.md                 # Tài liệu hướng dẫn dự án
```

---

## 📋 Yêu Cầu Tiền Đề (Prerequisites)

Trước khi khởi chạy dự án, hãy đảm bảo máy tính của bạn đã cài đặt:

1. **Git**: Dùng để quản lý mã nguồn.
2. **Docker & Docker Desktop** (Bao gồm Docker Compose v2+): *(Khuyên dùng để khởi chạy dự án nhanh nhất)*.
3. **JDK 21** và **Maven 3.8+**: *(Chỉ cần thiết nếu bạn muốn chạy/debug Backend cục bộ không qua Docker)*.
4. **Node.js (v18+)** và **npm (v9+)**: *(Chỉ cần thiết nếu bạn muốn chạy Frontend cục bộ không qua Docker)*.

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

Cách này sẽ tự động tải, build và khởi chạy tất cả 5 dịch vụ (PostgreSQL, Redis, Keycloak, Backend, Frontend) trong các Docker Containers.

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
   * Xem log cụ thể từng dịch vụ (ví dụ Backend hoặc Keycloak):
     ```bash
     docker compose logs -f backend
     docker compose logs -f keycloak
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
1. Di chuyển vào thư mục backend:
   ```bash
   cd backend
   ```
2. Khởi chạy dự án:
   * **Windows:**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   * **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```
   *(Backend sẽ chạy tại `http://localhost:8080`)*

##### 🔹 3.3. Khởi chạy Frontend (React + Vite)
1. Di chuyển vào thư mục frontend (từ thư mục gốc):
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
   *(Frontend Local Dev sẽ chạy tại `http://localhost:5173` hoặc `http://localhost:3000`)*

---

## 🌐 Danh Sách Cổng & Dịch Vụ

Sau khi khởi chạy thành công, các dịch vụ sẽ hoạt động tại các địa chỉ sau:

| Dịch vụ | Địa chỉ Web / Endpoint | Tài khoản mặc định / Ghi chú |
| :--- | :--- | :--- |
| **Frontend Web App** | `http://localhost:3000` (Docker) / `http://localhost:5173` (Local Dev) | Giao diện cho khách hàng & Admin |
| **Backend REST API** | `http://localhost:8080/api` | Spring Boot API Service |
| **Swagger UI (API Docs)** | `http://localhost:8080/swagger-ui.html` | Tài liệu API tương tác |
| **Keycloak Admin Console** | `http://localhost:8081` | **User:** `admin` \| **Pass:** `admin` |
| **PostgreSQL Database** | `localhost:5433` | **DB Name:** `hotel_booking_db` \| **User:** `postgres` \| **Pass:** `123456` |
| **Redis Cache** | `localhost:6379` | In-memory Data Store |

---

## 🗃 Khởi Tạo Cơ Sở Dữ Liệu (Database Scripts)

Khi container `postgres_db` khởi chạy lần đầu tiên, Docker sẽ tự động nạp và thực thi theo thứ tự các script SQL nằm trong thư mục `./database`:

1. `01-init-schema.sql`: Khởi tạo cấu trúc các bảng dữ liệu (Users, Hotels, Rooms, Bookings, Payment, v.v.).
2. `02-functions-and-triggers.sql`: Định nghĩa các Hàm (Functions) và Trigger tự động hóa.
3. `03-indexes-and-seed-data.sql`: Đánh chỉ mục (Indexes) tăng tốc độ truy vấn.
4. `04-seed-data-global-master-data.sql`: Nạp dữ liệu danh mục tĩnh toàn cục.
5. `05-seed-data-organization-base.sql`: Nạp dữ liệu cơ sở tổ chức & chuỗi khách sạn.
6. `06-seed-data-catalog-and-room-configuration.sql`: Nạp dữ liệu mẫu về hạng phòng và cấu hình phòng.
7. `07-seed-data-inventory-mapping-intance.sql`: Quản lý tình trạng phòng khả dụng và tồn kho phòng.
8. `08-pricing-engine.sql`: Cấu hình quy tắc tính giá phòng động và chính sách ưu đãi.

---

## 💡 Lưu Ý & Khắc Phục Lỗi Thường Gặp

1. **Xung đột cổng (Port In Use):**
   * Đảm bảo các cổng `3000`, `5433`, `6379`, `8080`, `8081` trên máy bạn chưa bị chiếm dụng bởi ứng dụng khác.
2. **Backend không kết nối được Cơ sở dữ liệu hoặc Keycloak:**
   * Nếu chạy qua Docker, cấu hình `depends_on` với `healthcheck` đã đảm bảo DB và Redis sẵn sàng trước khi Backend khởi chạy.
   * Nếu chạy Local Dev, hãy chắc chắn bạn đã khởi chạy `postgres_db`, `redis`, và `keycloak` thành công trước khi chạy `mvnw spring-boot:run`.
3. **Reset hoàn toàn Cơ sở dữ liệu (Clean Reset):**
   * Nếu bạn muốn xóa toàn bộ dữ liệu DB hiện tại và chạy lại toàn bộ SQL Scripts từ đầu:
     ```bash
     docker compose down -v
     docker compose up -d --build
     ```
     *(Lệnh `-v` sẽ xóa các Docker Volumes `pg_data` và `redis_data`)*.

---

Chúc bạn có trải nghiệm phát triển tuyệt vời với **Hotel Booking System**! 🚀
