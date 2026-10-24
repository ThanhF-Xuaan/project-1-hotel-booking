package vn.edu.utc.hotel_booking.modules.booking.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.utc.hotel_booking.modules.booking.validator.ContractFileValidator.ValidationResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContractFileValidator — Unit Tests")
class ContractFileValidatorTest {

    private ContractFileValidator validator;

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37}; // "%PDF-1.7"
    private static final byte[] DOCX_MAGIC = {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00}; // "PK.."
    private static final byte[] DOC_MAGIC = {
            (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1
    };

    @BeforeEach
    void setUp() {
        validator = new ContractFileValidator();
    }

    private byte[] buildContent(byte[] header, int totalSize) {
        byte[] content = new byte[totalSize];
        System.arraycopy(header, 0, content, 0, header.length);
        for (int i = header.length; i < totalSize; i++) {
            content[i] = (byte) (i % 256);
        }
        return content;
    }

    @Test
    @DisplayName("PDF hợp lệ: Đuôi .pdf, MIME application/pdf, Magic Bytes %PDF, size <= 15MB -> PASS")
    void validPdf_shouldPass() {
        byte[] content = buildContent(PDF_MAGIC, 10240); // 10 KB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hop_dong_kinh_te_fpt_2026.pdf",
                "application/pdf",
                content
        );

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getReason()).isNull();
    }

    @Test
    @DisplayName("DOCX hợp lệ: Đuôi .docx, MIME docx, Magic Bytes PK.. -> PASS")
    void validDocx_shouldPass() {
        byte[] content = buildContent(DOCX_MAGIC, 20480);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "phu_luc_hop_dong_doan.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                content
        );

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("DOC hợp lệ: Đuôi .doc, MIME msword, Magic Bytes OLE Compound -> PASS")
    void validDoc_shouldPass() {
        byte[] content = buildContent(DOC_MAGIC, 20480);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bao_gia_doan_viettel.doc",
                "application/msword",
                content
        );

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("File giả mạo: Đổi đuôi .txt thành .pdf (Sai Magic Bytes) -> FAIL")
    void fakePdf_withPlainTextHeader_shouldFail() {
        byte[] fakeContent = "This is a plain text file pretending to be PDF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake_contract.pdf",
                "application/pdf",
                fakeContent
        );

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("Nội dung nhị phân thực tế của file không khớp");
    }

    @Test
    @DisplayName("File vượt quá 15 MB -> FAIL")
    void fileExceedsLimit_shouldFail() {
        // 15 MB + 1 byte
        byte[] oversized = buildContent(PDF_MAGIC, (int) ContractFileValidator.MAX_FILE_SIZE_BYTES + 1);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oversized_contract.pdf",
                "application/pdf",
                oversized
        );

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("vượt quá giới hạn tối đa 15 MB");
    }

    @Test
    @DisplayName("File rỗng -> FAIL")
    void emptyFile_shouldFail() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("không được để trống");
    }

    @Test
    @DisplayName("Tên file chứa ký tự Path Traversal (../) -> FAIL")
    void pathTraversalFilename_shouldFail() {
        byte[] content = buildContent(PDF_MAGIC, 512);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../etc/passwd.pdf",
                "application/pdf",
                content
        );

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("Path Traversal");
    }

    @Test
    @DisplayName("Phần mở rộng không được hỗ trợ (.exe) -> FAIL")
    void unsupportedExtension_shouldFail() {
        byte[] content = buildContent(PDF_MAGIC, 512);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract.exe",
                "application/pdf",
                content
        );

        ValidationResult result = validator.validate(file);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("Phần mở rộng file không được hỗ trợ");
    }

    @Test
    @DisplayName("Kiểm tra Mã số thuế Việt Nam theo Thông tư 105/2020/TT-BTC")
    void testVietnamTaxCodeValidation() {
        // Doanh nghiệp mẹ: 10 chữ số
        assertThat(validator.isValidVietnamTaxCode("0101234567")).isTrue();
        assertThat(validator.isValidVietnamTaxCode("0312345678")).isTrue();

        // Chi nhánh: 13 chữ số có dấu gạch ngang (XXXXXXXXXX-XXX)
        assertThat(validator.isValidVietnamTaxCode("0101234567-001")).isTrue();
        assertThat(validator.isValidVietnamTaxCode("0312345678-123")).isTrue();

        // Không hợp lệ
        assertThat(validator.isValidVietnamTaxCode("12345")).isFalse();
        assertThat(validator.isValidVietnamTaxCode("0101234567A")).isFalse();
        assertThat(validator.isValidVietnamTaxCode("0101234567-01")).isFalse(); // chỉ có 2 số đuôi
        assertThat(validator.isValidVietnamTaxCode("")).isFalse();
        assertThat(validator.isValidVietnamTaxCode(null)).isFalse();
    }

    @Test
    @DisplayName("Tính toán mã băm SHA-256 toàn vẹn thông điệp dữ liệu theo Luật Giao dịch điện tử 2023")
    void testSha256Calculation() {
        byte[] data = "HOP_DONG_KHACH_DOAN_SO_GCT_20261025_0001".getBytes(StandardCharsets.UTF_8);
        String hash1 = validator.calculateSha256(data);
        String hash2 = validator.calculateSha256(data);

        assertThat(hash1).isNotNull();
        assertThat(hash1).hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
    }
}
