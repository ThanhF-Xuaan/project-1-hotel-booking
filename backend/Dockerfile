# ==========================================
# STAGE 1: BUILD (Đóng gói ứng dụng)
# ==========================================
# Sử dụng image Maven có sẵn Java 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Set thư mục làm việc trong container
WORKDIR /build

# Copy file pom.xml vào trước và tải thư viện
# Mục đích: Tận dụng Cache của Docker, lần sau build sẽ không phải tải lại thư viện
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy toàn bộ source code vào container
COPY src ./src

# Build ra file .jar và bỏ qua bước Test để build nhanh hơn
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: RUN (Chạy ứng dụng)
# ==========================================
# Chỉ sử dụng JRE (Môi trường chạy) thay vì JDK để image cực nhẹ
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy file .jar đã được build thành công từ Stage 1 sang Stage 2
COPY --from=builder /build/target/*.jar app.jar

# Mở cổng 8080 cho Spring Boot
EXPOSE 8080

# Lệnh khởi động Backend
ENTRYPOINT ["java", "-jar", "app.jar"]