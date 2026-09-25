# 🔐 Hướng Dẫn Vận Hành Keycloak IAM (Dockerized)

Thư mục này chứa cấu hình Declarative IAM cho hệ thống Hotel Booking, phục vụ tự động hóa khởi tạo Realm, Clients, Roles và Users mẫu khi chạy qua Docker Compose.

---

## 📋 1. Danh Sách Tài Khoản Mẫu (Seed Accounts)

Tất cả các tài khoản mặc định đều có mật khẩu là: **`123456`**

| Username | Email | Họ & Tên | Vai Trò (Role) | Mô Tả Quyền Hạn |
| :--- | :--- | :--- | :--- | :--- |
| **`admin`** | `admin@hotel.utc.edu.vn` | System Admin | `CHAIN_ADMIN` | Quản trị toàn bộ chuỗi khách sạn |
| **`manager_hn`** | `manager.hn@hotel.utc.edu.vn` | Manager Hà Nội | `PROPERTY_MANAGER` | Quản lý khách sạn cơ sở Hà Nội |
| **`reception_hn`** | `reception.hn@hotel.utc.edu.vn` | Lễ Tân Hà Nội | `RECEPTIONIST` | Tiếp nhận đặt phòng, check-in/check-out |
| **`housekeeping_hn`** | `housekeeping.hn@hotel.utc.edu.vn` | Buồng Phòng Hà Nội | `HOUSEKEEPING` | Đổi trạng thái phòng, dọn dẹp |
| **`customer1`** | `customer1@gmail.com` | Nguyễn Văn An | `CUSTOMER` | Khách hàng đặt phòng online |

---

## ⚙️ 2. Cơ Chế Auto-Import & Quản Lý Dữ Liệu

1. **Khởi động lần đầu:** Container `hotel_keycloak` chạy với cờ `start-dev --import-realm` sẽ nạp toàn bộ cấu hình từ `keycloak/realm-export.json` vào cơ sở dữ liệu nội bộ lưu tại volume `keycloak_data`.
2. **Bảo toàn dữ liệu:** Mọi thay đổi dữ liệu trong quá trình runtime (tạo thêm tài khoản mới, đổi mật khẩu) sẽ được lưu trên volume `keycloak_data` và không bị mất khi restart container.

---

## 🔄 3. Hướng Dẫn Force Re-import (Khi Chỉnh Sửa `realm-export.json`)

Khi bạn cập nhật thêm Roles, Clients hoặc cấu hình mới vào tệp `realm-export.json`, cờ `--import-realm` mặc định sẽ **không ghi đè** nếu Realm `hotel-realm` đã tồn tại trong volume. 

Để ép Keycloak nạp lại cấu hình mới nhất từ file JSON:

### 👉 Cách 1: Chỉ xóa riêng volume Keycloak (Giữ nguyên CSDL PostgreSQL & Redis)
```powershell
docker compose rm -s -v -f keycloak
docker volume rm hotel-booking-repo_keycloak_data
docker compose up -d keycloak
```

### 👉 Cách 2: Xóa toàn bộ volumes của tất cả services (Clean Reset)
```powershell
docker compose down -v
docker compose up -d
```

---

## 📥 4. Xuất Cấu Hình Từ Admin Console Ra File JSON (Export Realm)

Nếu bạn thay đổi cấu hình trực tiếp trên giao diện Web Admin Console (`http://localhost:8081`) và muốn lưu lại vào mã nguồn:

```powershell
docker exec -it hotel_keycloak /opt/keycloak/bin/kc.sh export --dir /opt/keycloak/data/import/ --realm hotel-realm --users realm_file
```
Sau đó copy file đã export ra thư mục `keycloak/realm-export.json` trên máy trạm.
