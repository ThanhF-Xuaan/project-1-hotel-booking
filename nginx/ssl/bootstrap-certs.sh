#!/bin/sh
# ==============================================================================
# HỆ THỐNG SMARTSTAY - SCRIPT BOOTSTRAP CHỨNG CHỈ SSL LET'S ENCRYPT TRÊN OCI
# ==============================================================================
# Quy trình Bootstrap 2 giai đoạn giải quyết triệt để lỗi crash loop của Nginx khi
# chứng chỉ SSL chưa được sinh lần đầu:
#   Giai đoạn 1A: Kích hoạt Nginx HTTP-only (cổng 80) phục vụ ACME challenge.
#   Giai đoạn 1B: Certbot cấp chứng chỉ qua HTTP-01 cho smartstay.space và www.smartstay.space.
#   Giai đoạn 1C: Kích hoạt cấu hình HTTPS chính thức và reload Nginx an toàn.
# ==============================================================================

set -e

DOMAIN="${DOMAIN_NAME:-smartstay.space}"
EMAIL="${LETSENCRYPT_EMAIL:-admin@smartstay.space}"
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"

echo "======================================================"
echo "BẮT ĐẦU QUY TRÌNH BOOTSTRAP SSL CHO DOMAIN: $DOMAIN"
echo "======================================================"

# 1. Kiểm tra xem chứng chỉ đã tồn tại hay chưa
if [ -f "$CERT_DIR/fullchain.pem" ] && [ -f "$CERT_DIR/privkey.pem" ]; then
    echo "Chứng chỉ SSL cho $DOMAIN đã tồn tại. Đảm bảo cấu hình HTTPS đang hoạt động..."
    cp nginx/conf.d/prod.conf.template nginx/conf.d/prod.conf
    docker compose -f docker-compose.prod.yml exec nginx nginx -s reload || true
    echo "Hoàn tất kiểm tra SSL."
    exit 0
fi

echo "Chứng chỉ SSL chưa tồn tại. Đang tiến hành cấp mới qua Let's Encrypt..."

# 2. Tạm ẩn cấu hình dev default.conf nếu có để tránh xung đột cổng
if [ -f "nginx/conf.d/default.conf" ]; then
    echo "Đổi tên default.conf -> default.conf.dev để chỉ nạp cấu hình production..."
    mv nginx/conf.d/default.conf nginx/conf.d/default.conf.dev
fi

# 3. Giai đoạn 1A: Kích hoạt cấu hình HTTP Bootstrap (chỉ mở cổng 80, không yêu cầu SSL)
echo "[Giai đoạn 1A] Kích hoạt Nginx HTTP Bootstrap phục vụ ACME Challenge..."
cp nginx/conf.d/prod-bootstrap.conf nginx/conf.d/prod.conf
docker compose -f docker-compose.prod.yml up -d nginx
sleep 3

# 4. Giai đoạn 1B: Chạy Certbot xin cấp chứng chỉ cho cả apex domain và www
echo "[Giai đoạn 1B] Chạy Certbot cấp phát chứng chỉ Let's Encrypt..."
docker compose -f docker-compose.prod.yml run --rm --entrypoint "\
  certbot certonly \
    --webroot \
    -w /var/www/certbot \
    -d \"$DOMAIN\" \
    -d \"www.$DOMAIN\" \
    --email \"$EMAIL\" \
    --agree-tos \
    --no-eff-email \
    --non-interactive" certbot

echo "Đã lấy chứng chỉ thành công từ Let's Encrypt!"

# 5. Giai đoạn 1C: Kích hoạt cấu hình Nginx Production HTTPS đầy đủ
echo "[Giai đoạn 1C] Kích hoạt cấu hình Nginx Production HTTPS..."
cp nginx/conf.d/prod.conf.template nginx/conf.d/prod.conf

# 6. Reload Nginx để nạp chứng chỉ mới mà không làm gián đoạn kết nối
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload

echo "======================================================"
echo "HOÀN TẤT BOOTSTRAP: HTTPS đã được kích hoạt thành công trên https://$DOMAIN"
echo "======================================================"
