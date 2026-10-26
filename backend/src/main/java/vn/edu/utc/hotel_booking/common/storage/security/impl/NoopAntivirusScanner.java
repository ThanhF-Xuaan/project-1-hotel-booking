package vn.edu.utc.hotel_booking.common.storage.security.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.common.storage.security.AntivirusScanner;

/**
 * Adapter quét virus mặc định cho môi trường Local / Dev / Unit Test.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hotel.storage.antivirus.enabled", havingValue = "false", matchIfMissing = true)
public class NoopAntivirusScanner implements AntivirusScanner {

    @Override
    public ScanResult scan(byte[] data, String filename) {
        log.debug("NoopAntivirusScanner: Bỏ qua quét virus tệp '{}' trên môi trường dev/local", filename);
        return ScanResult.clean();
    }
}
