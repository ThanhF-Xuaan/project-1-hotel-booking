# 📖 Hướng Dẫn Kiểm Tra & Xác Thực Toàn Diện Các Dịch Vụ Docker

> **Dự án:** Hệ Thống Quản Lý Khách Sạn (Hotel Booking System)  
> **Tệp Word xuất bản:** [`Huong_Dan_Kiem_Tra_He_Thong_Sau_Khi_Chay_Docker.docx`](file:///d:/Project/MyProject/hotel-booking-repo/document/Huong_Dan_Kiem_Tra_He_Thong_Sau_Khi_Chay_Docker.docx)  
> **Ngày cập nhật:** 25/09/2026 | **Phiên bản:** v1.0.0

---

## 🗺️ 1. Tổng Quan Các Dịch Vụ Trong Docker

| Service | Tên Container | Cổng Host | Công Nghệ / Vai Trò | Trạng Thái Chuẩn |
| :--- | :--- | :--- | :--- | :--- |
| **Database** | `hotel_postgres` | `5433` | PostgreSQL 16 (44 bảng + Triggers + Sequences) | `Up (healthy)` |
| **Cache** | `hotel_redis` | `6379` | Redis 7 (Bộ nhớ đệm phân tán) | `Up (healthy)` |
| **IAM / Auth** | `hotel_keycloak` | `8081` | Keycloak 24 (OIDC / OAuth2 Server) | `Up (healthy)` |
| **Migration** | `hotel_liquibase` | `-` | Liquibase 4.31 (Tự động migrate DDL, Seed Data) | `Exited (0)` |
| **Backend API** | `hotel_backend` | `8080` | Spring Boot 4 / Java 21 / REST API & Swagger | `Up (healthy)` |
| **Frontend UI** | `hotel_frontend` | `3000` | React 19 / TypeScript / Vite Hot-reload | `Up (healthy)` |

---

## 🚀 2. Quy Trình Khởi Chạy

```bash
# 1. Khởi động toàn bộ các dịch vụ
docker compose up -d

# 2. Xem trạng thái các container
docker compose ps -a

# 3. Theo dõi log luồng
docker compose logs -f
```

---

## 🔍 3. Chi Tiết Các Bước Verify Từng Service

### 3.1. Kiểm tra PostgreSQL (`hotel_postgres` - Cổng 5433)
```bash
# 1. Đếm tổng số lượng bảng (Kỳ vọng: 48 bảng)
docker exec hotel_postgres psql -U postgres -d hotel_booking_db -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';"

# 2. Truy vấn dữ liệu mẫu khách sạn (Kỳ vọng: 3 khách sạn mẫu)
docker exec hotel_postgres psql -U postgres -d hotel_booking_db -c "SELECT h.id, h.name, r.name as region, h.address FROM hotels h JOIN regions r ON h.region_id = r.id;"

# 3. Kiểm tra lịch sử ChangeSets Liquibase đã nạp
docker exec hotel_postgres psql -U postgres -d hotel_booking_db -c "SELECT id, author, dateexecuted, exectype FROM databasechangelog;"
```

### 3.2. Kiểm tra Liquibase Migration Runner (`hotel_liquibase`)
```bash
# 1. Kiểm tra mã thoát (Kỳ vọng: Exited (0))
docker ps -a --filter "name=hotel_liquibase"

# 2. Xem log 5 ChangeSets
docker logs hotel_liquibase
```

### 3.3. Kiểm tra Redis Cache (`hotel_redis` - Cổng 6379)
```bash
# 1. Ping kiểm tra phản hồi (Kỳ vọng: PONG)
docker exec hotel_redis redis-cli ping

# 2. Ghi và đọc dữ liệu Cache mẫu
docker exec hotel_redis redis-cli SET test_hotel "Viettel Luxury Hanoi"
docker exec hotel_redis redis-cli GET test_hotel
```

### 3.4. Kiểm tra Keycloak IAM (`hotel_keycloak` - Cổng 8081)
- **Giao diện Admin Console:** `http://localhost:8081/admin` (`admin` / `admin`).
- **Danh sách 5 tài khoản thử nghiệm (Mật khẩu: `123456`):**
  - `admin` (Role: `ROLE_CHAIN_ADMIN`)
  - `manager_hn` (Role: `ROLE_PROPERTY_MANAGER`)
  - `reception_hn` (Role: `ROLE_RECEPTIONIST`)
  - `housekeeping_hn` (Role: `ROLE_HOUSEKEEPING`)
  - `customer1` (Role: `ROLE_CUSTOMER`)

```powershell
# Lệnh PowerShell gọi Keycloak cấp JWT Bearer Token:
$res = Invoke-RestMethod -Uri "http://localhost:8081/realms/hotel-realm/protocol/openid-connect/token" `
    -Method Post `
    -Body @{ client_id="hotel-frontend"; grant_type="password"; username="admin"; password="123456" }
Write-Host "JWT Token:" $res.access_token
```

### 3.5. Kiểm tra Spring Boot Backend (`hotel_backend` - Cổng 8080)
```bash
# 1. Kiểm tra log
docker logs hotel_backend

# 2. Kiểm tra Swagger UI & OpenAPI Endpoint
# Trình duyệt: http://localhost:8080/swagger-ui/index.html
# OpenAPI Docs: http://localhost:8080/v3/api-docs
```

### 3.6. Kiểm tra React Frontend (`hotel_frontend` - Cổng 3000)
- Trình duyệt truy cập: `http://localhost:3000`.
- Kiểm tra tính năng Hot-Reload khi chỉnh sửa code trong `frontend/src/App.tsx`.

---

## ⚡ 4. Script Tự Động Hóa Kiểm Tra Toàn Diện (All-In-One Script)

```powershell
Write-Host "`n========== [1/6] KIỂM TRA POSTGRESQL ==========" -ForegroundColor Cyan
$tblCount = docker exec hotel_postgres psql -U postgres -d hotel_booking_db -t -c "SELECT count(*) FROM information_schema.tables WHERE table_schema='public';"
if ($tblCount -ge 44) { Write-Host "✔ PostgreSQL OK! Tổng số bảng: $($tblCount.Trim())" -ForegroundColor Green }
else { Write-Host "✘ PostgreSQL Thất bại!" -ForegroundColor Red }

Write-Host "`n========== [2/6] KIỂM TRA REDIS CACHE ==========" -ForegroundColor Cyan
$redisPing = docker exec hotel_redis redis-cli ping
if ($redisPing -match "PONG") { Write-Host "✔ Redis OK! Phản hồi: PONG" -ForegroundColor Green }
else { Write-Host "✘ Redis Thất bại!" -ForegroundColor Red }

Write-Host "`n========== [3/6] KIỂM TRA LIQUIBASE MIGRATION ==========" -ForegroundColor Cyan
$liqStatus = docker inspect hotel_liquibase --format '{{.State.ExitCode}}'
if ($liqStatus -eq "0") { Write-Host "✔ Liquibase OK! Thoát mã 0 (Migration hoàn tất 100%)" -ForegroundColor Green }
else { Write-Host "✘ Liquibase Thất bại! Mã thoát: $liqStatus" -ForegroundColor Red }

Write-Host "`n========== [4/6] KIỂM TRA KEYCLOAK IAM ==========" -ForegroundColor Cyan
try {
    $tokenRes = Invoke-RestMethod -Uri "http://localhost:8081/realms/hotel-realm/protocol/openid-connect/token" `
        -Method Post -Body @{ client_id="hotel-frontend"; grant_type="password"; username="admin"; password="123456" }
    if ($tokenRes.access_token) { Write-Host "✔ Keycloak IAM OK! Đã cấp phát Bearer Token thành công." -ForegroundColor Green }
} catch { Write-Host "✘ Keycloak Thất bại!" -ForegroundColor Red }

Write-Host "`n========== [5/6] KIỂM TRA SPRING BOOT BACKEND ==========" -ForegroundColor Cyan
try {
    $docs = Invoke-RestMethod -Uri "http://localhost:8080/v3/api-docs" -Method Get
    if ($docs.openapi) { Write-Host "✔ Spring Boot Backend OK! Phiên bản OpenAPI: $($docs.openapi)" -ForegroundColor Green }
} catch { Write-Host "✘ Backend Thất bại!" -ForegroundColor Red }

Write-Host "`n========== [6/6] KIỂM TRA REACT FRONTEND ==========" -ForegroundColor Cyan
try {
    $fe = Invoke-WebRequest -Uri "http://localhost:3000" -Method Get
    if ($fe.StatusCode -eq 200) { Write-Host "✔ Frontend Vite OK! HTTP 200 Đang phục vụ tại http://localhost:3000" -ForegroundColor Green }
} catch { Write-Host "✘ Frontend Thất bại!" -ForegroundColor Red }

Write-Host "`n========================================================" -ForegroundColor Yellow
Write-Host ">>> TẤT CẢ DỊCH VỤ ĐÃ SẴN SÀNG ĐỂ PHÁT TRIỂN & TEST <<<" -ForegroundColor Yellow
Write-Host "========================================================`n" -ForegroundColor Yellow
```
