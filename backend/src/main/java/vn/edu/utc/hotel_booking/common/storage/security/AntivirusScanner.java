package vn.edu.utc.hotel_booking.common.storage.security;

import lombok.Value;

/**
 * SPI giao diện kiểm tra virus và mã độc cho tệp tải lên.
 */
public interface AntivirusScanner {

    ScanResult scan(byte[] data, String filename);

    @Value
    class ScanResult {
        boolean clean;
        String details;

        public static ScanResult clean() {
            return new ScanResult(true, "OK");
        }

        public static ScanResult infected(String details) {
            return new ScanResult(false, details);
        }
    }
}
