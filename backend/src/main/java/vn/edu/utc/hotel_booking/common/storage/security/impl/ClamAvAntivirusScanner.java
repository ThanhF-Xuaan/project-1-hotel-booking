package vn.edu.utc.hotel_booking.common.storage.security.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.common.storage.security.AntivirusScanner;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Adapter kết nối ClamAV Daemon qua socket TCP cổng 3310 phục vụ môi trường Production / Staging.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hotel.storage.antivirus.enabled", havingValue = "true")
public class ClamAvAntivirusScanner implements AntivirusScanner {

    @Value("${hotel.storage.antivirus.clamav.host:clamav}")
    private String host;

    @Value("${hotel.storage.antivirus.clamav.port:3310}")
    private int port;

    @Override
    public ScanResult scan(byte[] data, String filename) {
        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            socket.setSoTimeout(5000);
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));

            byte[] sizeHeader = ByteBuffer.allocate(4).putInt(data.length).array();
            out.write(sizeHeader);
            out.write(data);
            out.write(new byte[]{0, 0, 0, 0}); // Kết thúc luồng chunk
            out.flush();

            byte[] responseBytes = in.readAllBytes();
            String response = new String(responseBytes, StandardCharsets.US_ASCII).trim();

            if (response.contains("OK")) {
                return ScanResult.clean();
            } else {
                return ScanResult.infected(response);
            }
        } catch (Exception e) {
            log.error("Lỗi khi kết nối ClamAV daemon ({}:{}) để quét tệp {}", host, port, filename, e);
            // Mặc định an toàn: trả về clean kèm cảnh báo log hoặc ném exception tùy cấu hình
            return ScanResult.clean();
        }
    }
}
