package vn.edu.utc.hotel_booking.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageAccessLevel;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.policy.StoragePolicy;
import vn.edu.utc.hotel_booking.common.storage.security.AntivirusScanner;
import vn.edu.utc.hotel_booking.common.storage.validator.FileSecurityPipeline;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FileSecurityPipeline — Unit Tests")
class FileSecurityPipelineTest {

    private AntivirusScanner antivirusScanner;
    private FileSecurityPipeline pipeline;
    private StoragePolicy photoPolicy;

    @BeforeEach
    void setUp() {
        antivirusScanner = mock(AntivirusScanner.class);
        when(antivirusScanner.scan(any(), any())).thenReturn(AntivirusScanner.ScanResult.clean());

        pipeline = new FileSecurityPipeline(antivirusScanner);

        photoPolicy = StoragePolicy.builder()
                .category(StorageCategory.HOTEL_ROOM_PHOTO)
                .folderPrefix("rooms")
                .defaultAccessLevel(StorageAccessLevel.PUBLIC_READ)
                .allowedMimeTypes(Set.of("image/png", "image/jpeg"))
                .maxSizeBytes(2 * 1024 * 1024L) // 2 MB
                .requireContentParser(true)
                .requireVirusScan(true)
                .build();
    }

    @Test
    @DisplayName("Chấp nhận tệp ảnh PNG hợp lệ vượt qua cả 3 tầng L1, L2, L3")
    void validate_validPngImage_shouldPass() throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] validPngBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "room.png",
                "image/png",
                validPngBytes
        );

        assertThatCode(() -> pipeline.validate(file, photoPolicy))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Từ chối tệp rỗng dung lượng 0 byte")
    void validate_emptyFile_shouldThrowAppException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> pipeline.validate(emptyFile, photoPolicy))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_EMPTY);
    }

    @Test
    @DisplayName("Từ chối tệp vượt quá dung lượng tối đa của chính sách")
    void validate_oversizedFile_shouldThrowAppException() {
        byte[] largeBytes = new byte[3 * 1024 * 1024]; // 3MB > 2MB limit
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        assertThatThrownBy(() -> pipeline.validate(largeFile, photoPolicy))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("Từ chối tệp có định dạng MIME không nằm trong whitelist")
    void validate_disallowedMime_shouldThrowAppException() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "app.exe", "application/x-msdownload", new byte[]{1, 2, 3, 4});

        assertThatThrownBy(() -> pipeline.validate(exeFile, photoPolicy))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_FORMAT_NOT_ALLOWED);
    }

    @Test
    @DisplayName("Tầng L1: Chặn tệp văn bản đổi đuôi thành .png (Magic bytes không khớp)")
    void validate_fakeExtension_shouldBeBlockedByMagicBytes() {
        byte[] fakeBytes = "Hello this is a text file renamed to png".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile("file", "malicious.png", "image/png", fakeBytes);

        assertThatThrownBy(() -> pipeline.validate(fakeFile, photoPolicy))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_CORRUPTED_OR_MALICIOUS);
    }

    @Test
    @DisplayName("Tầng L2: Chặn tệp ảnh có header đúng nhưng nội dung pixel bị hỏng (Parser decode fail)")
    void validate_corruptedImagePixels_shouldBeBlockedByParser() {
        // PNG magic bytes header: 89 50 4E 47 kèm rác khiến ImageIO không thể decode
        byte[] corruptedBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 1, 2, 3};
        MockMultipartFile corruptedFile = new MockMultipartFile("file", "corrupt.png", "image/png", corruptedBytes);

        assertThatThrownBy(() -> pipeline.validate(corruptedFile, photoPolicy))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_CORRUPTED_OR_MALICIOUS);
    }

    @Test
    @DisplayName("Tầng L3: Chặn tệp khi Antivirus phát hiện mã độc")
    void validate_infectedFile_shouldBeBlockedByAntivirus() throws IOException {
        BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        MockMultipartFile infectedFile = new MockMultipartFile("file", "trojan.png", "image/png", pngBytes);

        when(antivirusScanner.scan(any(), any()))
                .thenReturn(AntivirusScanner.ScanResult.infected("Eicar-Test-Signature"));

        assertThatThrownBy(() -> pipeline.validate(infectedFile, photoPolicy))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_CORRUPTED_OR_MALICIOUS);
    }
}
