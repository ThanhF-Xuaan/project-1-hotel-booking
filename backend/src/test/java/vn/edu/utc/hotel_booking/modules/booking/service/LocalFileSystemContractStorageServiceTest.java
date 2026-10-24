package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.LocalFileSystemContractStorageService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalFileSystemContractStorageService — Unit Tests")
class LocalFileSystemContractStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileSystemContractStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileSystemContractStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Lưu tệp hợp đồng và đọc lại thành công")
    void storeAndLoadContractFile_shouldSucceed() throws IOException {
        byte[] content = "%PDF-1.7 synthetic test contract content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hop_dong_fpt.pdf",
                "application/pdf",
                content
        );

        String relativePath = storageService.storeContractFile(file, (short) 1, 1001L);

        assertThat(relativePath).isNotNull();
        assertThat(relativePath).startsWith("hotel_1/");
        assertThat(relativePath).endsWith(".pdf");

        byte[] loadedBytes = storageService.loadContractFile(relativePath);
        assertThat(loadedBytes).isEqualTo(content);
    }

    @Test
    @DisplayName("Xóa tệp hợp đồng thành công")
    void deleteContractFile_shouldSucceed() throws IOException {
        byte[] content = "%PDF-1.7 test delete content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "delete_me.pdf",
                "application/pdf",
                content
        );

        String relativePath = storageService.storeContractFile(file, (short) 2, 1002L);
        assertThat(storageService.loadContractFile(relativePath)).isNotEmpty();

        storageService.deleteContractFile(relativePath);

        assertThatThrownBy(() -> storageService.loadContractFile(relativePath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    @DisplayName("Chặn truy cập Path Traversal khi load file trái phép")
    void loadContractFile_pathTraversal_shouldThrowSecurityException() {
        assertThatThrownBy(() -> storageService.loadContractFile("../../etc/passwd"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path Traversal");
    }
}
