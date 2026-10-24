package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.modules.booking.service.ContractStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileSystemContractStorageService implements ContractStorageService {

    private final Path rootStoragePath;

    public LocalFileSystemContractStorageService(
            @Value("${hotel.storage.contracts.base-path:uploads/contracts}") String basePath) {
        this.rootStoragePath = Paths.get(basePath).toAbsolutePath().normalize();
        initStorageDirectory();
    }

    private void initStorageDirectory() {
        try {
            if (!Files.exists(rootStoragePath)) {
                Files.createDirectories(rootStoragePath);
                log.info("Khởi tạo thư mục lưu trữ hợp đồng: {}", rootStoragePath);
            }
        } catch (IOException e) {
            log.error("Không thể khởi tạo thư mục lưu trữ hợp đồng: {}", rootStoragePath, e);
            throw new IllegalStateException("Không thể khởi tạo thư mục lưu trữ hợp đồng", e);
        }
    }

    @Override
    public String storeContractFile(MultipartFile file, Short hotelId, Long contractId) throws IOException {
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String hotelDir = "hotel_" + (hotelId != null ? hotelId : "common");

        Path targetDirectory = rootStoragePath.resolve(hotelDir).resolve(year).resolve(month).normalize();
        if (!targetDirectory.startsWith(rootStoragePath)) {
            throw new SecurityException("Phát hiện lỗi Path Traversal khi khởi tạo thư mục lưu trữ");
        }
        Files.createDirectories(targetDirectory);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFileName = String.format("%d_%s.%s", contractId, UUID.randomUUID(), extension);

        Path targetFilePath = targetDirectory.resolve(uniqueFileName).normalize();
        if (!targetFilePath.startsWith(rootStoragePath)) {
            throw new SecurityException("Phát hiện lỗi Path Traversal khi lưu tệp");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Trả về đường dẫn tương đối để lưu trữ trong database
        Path relativePath = rootStoragePath.relativize(targetFilePath);
        String formattedRelativePath = relativePath.toString().replace('\\', '/');
        log.info("Lưu trữ tệp hợp đồng thành công: {}", formattedRelativePath);
        return formattedRelativePath;
    }

    @Override
    public byte[] loadContractFile(String relativeFilePath) throws IOException {
        Path resolvedPath = rootStoragePath.resolve(relativeFilePath).normalize();
        if (!resolvedPath.startsWith(rootStoragePath)) {
            throw new SecurityException("Phát hiện truy cập tệp bất hợp pháp (Path Traversal)");
        }
        if (!Files.exists(resolvedPath) || !Files.isRegularFile(resolvedPath)) {
            throw new IOException("Tệp hợp đồng không tồn tại trên hệ thống lưu trữ: " + relativeFilePath);
        }
        return Files.readAllBytes(resolvedPath);
    }

    @Override
    public void deleteContractFile(String relativeFilePath) throws IOException {
        Path resolvedPath = rootStoragePath.resolve(relativeFilePath).normalize();
        if (!resolvedPath.startsWith(rootStoragePath)) {
            throw new SecurityException("Phát hiện thao tác tệp bất hợp pháp (Path Traversal)");
        }
        if (Files.exists(resolvedPath)) {
            Files.delete(resolvedPath);
            log.info("Đã xóa tệp hợp đồng vật lý: {}", relativeFilePath);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "pdf";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "pdf";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
