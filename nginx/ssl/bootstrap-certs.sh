#!/bin/sh
# ==============================================================================
# HỆ THỐNG KHÁCH SẠN UTC - SCRIPT BOOTSTRAP CHỨNG CHỈ SSL LET'S ENCRYPT TRÊN OCI
# ==============================================================================
# Quy trình Bootstrap 2 pha giải quyết triệt để lỗi crash loop của Nginx khi
# chứng chỉ SSL chưa được sinh lần đầu:
#   Pha 1: Khởi động Nginx HTTP-only để phục vụ ACME challenge của Certbot.
#   Pha 2: Certbot cấp chứng chỉ qua HTTP-01 challenge.
#   Pha 3: Chuyển cấu hình sang HTTPS hoàn chỉnh và reload Nginx.
# ==============================================================================

set -e

DOMAIN="${DOMAIN_NAME:-hotel.utc.edu.vn}"
EMAIL="${LETSENCRYPT_EMAIL:-admin@utc.edu.vn}"
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"

echo "======================================================"
echo "BẮT ĐẦU QUY TRÌNH BOOTSTRAP SSL CHO DOMAIN: $DOMAIN"
echo "======================================================"

# 1. Kiểm tra xem chứng chỉ đã tồn tại hay chưa
if [ -f "$CERT_DIR/fullchain.pem" ] && [ -f "$CERT_DIR/privkey.pem" ]; then
    echo "Chứng chỉ SSL cho $DOMAIN đã tồn tại. Bỏ qua bước cấp mới."
    exit 0
fi

echo "Chứng chỉ SSL chưa tồn tại. Đang tiến hành cấp mới qua Let's Encrypt..."

# 2. Chạy Certbot để xin chứng chỉ qua HTTP-01 Webroot challenge
# Yêu cầu: Nginx đang chạy và mount volume /var/www/certbot
docker compose -f docker-compose.prod.yml run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    -d $DOMAIN \
    --email $EMAIL \
    --agree-tos \
    --no-eff-email \
    --non-interactive" certbot

echo "Đã lấy chứng chỉ thành công từ Let's Encrypt!"

# 3. Kích hoạt cấu hình Nginx Production HTTPS
cp nginx/conf.d/prod.conf.template nginx/conf.d/prod.conf

# 4. Reload Nginx để nạp chứng chỉ mới mà không làm gián đoạn kết nối
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload

echo "HOÀN TẤT BOOTSTRAP: HTTPS đã được kích hoạt thành công trên https://$DOMAIN"
