#!/bin/sh
# ==============================================================================
# Script sinh chứng chỉ SSL tự ký (Self-signed) cho môi trường Dev Localhost
# ==============================================================================

DIR="$(cd "$(dirname "$0")" && pwd)"
CERT_FILE="$DIR/localhost.crt"
KEY_FILE="$DIR/localhost.key"

if [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ]; then
    echo "Chứng chỉ SSL cho localhost đã tồn tại: $CERT_FILE"
    exit 0
fi

echo "Đang sinh cặp chứng chỉ SSL tự ký cho localhost..."
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$KEY_FILE" \
    -out "$CERT_FILE" \
    -subj "/C=VN/ST=Hanoi/L=Hanoi/O=UTC/OU=HotelBooking/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

echo "Đã tạo thành công:"
echo " - Key:  $KEY_FILE"
echo " - Cert: $CERT_FILE"
