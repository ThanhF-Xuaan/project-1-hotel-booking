package vn.edu.utc.hotel_booking.common.storage.spi.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.common.config.StorageProperties;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.storage.spi.ObjectStorageService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Duration;

/**
 * Adapter lưu trữ tệp cục bộ trên đĩa máy tính (Local Disk Storage Adapter).
 * ĐỊNH VỊ: Chỉ sử dụng làm Fallback cho môi trường Local Dev Offline và Unit Test.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hotel.storage.type", havingValue = "local")
public class LocalStorageService implements ObjectStorageService {

    private final StorageProperties storageProperties;

    @Override
    public void upload(String key, InputStream inputStream, long contentLength, String contentType) {
        Path targetPath = resolvePath(key);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Đã lưu tệp vào đĩa cục bộ: {}", targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Lỗi khi ghi tệp cục bộ: {}", targetPath, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi ghi tệp cục bộ: " + e.getMessage());
        }
    }

    @Override
    public byte[] download(String key) {
        Path targetPath = resolvePath(key);
        if (!Files.exists(targetPath)) {
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND, "Không tìm thấy tệp cục bộ: " + key);
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            log.error("Lỗi khi đọc tệp cục bộ: {}", targetPath, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi đọc tệp cục bộ: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadStream(String key) {
        Path targetPath = resolvePath(key);
        if (!Files.exists(targetPath)) {
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND, "Không tìm thấy tệp cục bộ: " + key);
        }
        try {
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            log.error("Lỗi khi mở luồng đọc tệp cục bộ: {}", targetPath, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi đọc tệp cục bộ: " + e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        Path targetPath = resolvePath(key);
        try {
            Files.deleteIfExists(targetPath);
            log.debug("Đã xóa tệp cục bộ: {}", targetPath);
        } catch (IOException e) {
            log.error("Lỗi khi xóa tệp cục bộ: {}", targetPath, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi xóa tệp cục bộ: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolvePath(key));
    }

    @Override
    public String generatePresignedUrl(String key, Duration duration) {
        // Trên local dev adapter: trả về link giả lập hoặc đường dẫn tương đối
        return "/api/v1/media/local/" + key;
    }

    @Override
    public String getPublicUrl(String key) {
        return "/uploads/" + key;
    }

    @Override
    public String getBucketName() {
        return "local-filesystem";
    }

    private Path resolvePath(String key) {
        String base = storageProperties.getLocal().getBasePath();
        return Paths.get(base).resolve(key).normalize();
    }
}
