# Sử dụng môi trường Node.js 20 bản Alpine siêu nhẹ
FROM node:20-alpine

# Set thư mục làm việc trong container
WORKDIR /app

# Copy các file quản lý thư viện vào trước (Tận dụng Cache)
COPY package.json package-lock.json* ./

# Cài đặt toàn bộ thư viện
RUN npm install

# Copy toàn bộ source code của React/Vite vào container
COPY . .

# Mở cổng 3000 (khớp với cấu hình port trong vite.config.ts)
EXPOSE 3000

# Khởi động Vite ở chế độ Development (Hỗ trợ Hot-reload)
CMD ["npm", "run", "dev"]