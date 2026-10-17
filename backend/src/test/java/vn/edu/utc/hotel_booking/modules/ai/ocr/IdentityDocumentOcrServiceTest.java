package vn.edu.utc.hotel_booking.modules.ai.ocr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.enums.OcrExtractionStatus;
import vn.edu.utc.hotel_booking.modules.ai.ocr.service.IdentityDocumentOcrService;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử dịch vụ bóc tách OCR giấy tờ tuỳ thân.
 *
 * <p>Tất cả dữ liệu test là <strong>tổng hợp (synthetic)</strong> –
 * không chứa số CCCD, hộ chiếu hay họ tên thật.</p>
 */
@DisplayName("IdentityDocumentOcrService – Unit Tests")
class IdentityDocumentOcrServiceTest {

    private IdentityDocumentOcrService service;

    @BeforeEach
    void setUp() {
        service = new IdentityDocumentOcrService();
    }

    // =====================================================================
    // TC-01: CCCD Việt Nam đầy đủ, rõ nét → SUCCESS
    // =====================================================================

    @Test
    @DisplayName("TC-01: CCCD đầy đủ thông tin → SUCCESS, 7 trường, requiresHumanVerification=true")
    void tc01_cccdFullData_shouldReturnSuccess() {
        OcrDocumentExtractRequestDto req = buildRequest("""
                Căn cước công dân
                Họ và tên: NGUYEN VAN TEST
                Ngày sinh: 15/06/1990
                Giới tính: Nam
                Quốc tịch: Việt Nam
                Số CCCD: 079190012345
                Nơi thường trú: 123 Đường Test, Quận 1, TP. Hồ Chí Minh
                """);

        OcrStayGuestDraftDto draft = service.extract(req);

        assertThat(draft.getStatus()).isEqualTo(OcrExtractionStatus.SUCCESS);
        assertThat(draft.isRequiresHumanVerification()).isTrue();
        assertThat(draft.getMissingOrLowConfidenceFields()).isEmpty();

        // 7 trường phải có giá trị
        assertThat(draft.getFullName().getValue()).contains("NGUYEN VAN TEST");
        assertThat(draft.getDateOfBirth().getValue()).isNotNull();
        assertThat(draft.getGender().getValue()).isEqualTo(Gender.MALE);
        assertThat(draft.getNationality().getValue()).containsIgnoringCase("Việt");
        assertThat(draft.getDocumentType().getValue()).isIn(DocumentType.CCCD, DocumentType.CMND);
        assertThat(draft.getDocumentNumber().getValue()).isEqualTo("079190012345");
        assertThat(draft.getPermanentAddress().getValue()).isNotBlank();

        assertThat(draft.getOverallConfidence()).isGreaterThan(0.0);
    }

    // =====================================================================
    // TC-02: Hộ chiếu Quốc tế qua MRZ → SUCCESS, không bắt buộc permanentAddress
    // =====================================================================

    @Test
    @DisplayName("TC-02: Passport MRZ hợp lệ → SUCCESS, DocumentType=PASSPORT, no permanentAddress required")
    void tc02_passportMrz_shouldReturnSuccess() {
        // MRZ tổng hợp TD3: dòng 1 = P<USA..., dòng 2 = passport# + dob + gender + expiry
        OcrDocumentExtractRequestDto req = buildRequest("""
                UNITED STATES OF AMERICA
                P<USASMITH<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                A123456784USA9006153M2512314<<<<<<<<<<<<<<06
                """);
        req.setDocumentTypeHint(DocumentType.PASSPORT);

        OcrStayGuestDraftDto draft = service.extract(req);

        assertThat(draft.getStatus()).isEqualTo(OcrExtractionStatus.SUCCESS);
        assertThat(draft.isRequiresHumanVerification()).isTrue();
        assertThat(draft.getDocumentType().getValue()).isEqualTo(DocumentType.PASSPORT);
        assertThat(draft.getDocumentNumber().getValue()).contains("A12345678");
        assertThat(draft.getFullName().getValue()).containsIgnoringCase("SMITH");
        assertThat(draft.getGender().getValue()).isEqualTo(Gender.MALE);
        // Khách Mỹ (USA) không bắt buộc permanentAddress theo chuẩn BCA
        assertThat(draft.getMissingOrLowConfidenceFields())
                .doesNotContain("permanentAddress");
    }

    // =====================================================================
    // TC-03: CCCD thiếu permanentAddress → LOW_CONFIDENCE_REVIEW_REQUIRED
    // =====================================================================

    @Test
    @DisplayName("TC-03: CCCD thiếu permanentAddress (khách VN) → LOW_CONFIDENCE_REVIEW_REQUIRED")
    void tc03_cccdMissingAddress_shouldBeLowConfidence() {
        OcrDocumentExtractRequestDto req = buildRequest("""
                Căn cước công dân
                Họ và tên: TRAN THI TEST
                Ngày sinh: 20/01/1985
                Giới tính: Nữ
                Quốc tịch: Việt Nam
                Số CCCD: 001185055555
                """); // Thiếu nơi thường trú

        OcrStayGuestDraftDto draft = service.extract(req);

        assertThat(draft.getStatus()).isEqualTo(OcrExtractionStatus.LOW_CONFIDENCE_REVIEW_REQUIRED);
        assertThat(draft.isRequiresHumanVerification()).isTrue();
        assertThat(draft.getMissingOrLowConfidenceFields()).contains("permanentAddress");
    }

    // =====================================================================
    // TC-04: Số CCCD sai định dạng → LOW_CONFIDENCE_REVIEW_REQUIRED
    // =====================================================================

    @Test
    @DisplayName("TC-04: CCCD sai số (bắt đầu bằng 1, không phải 0) → LOW_CONFIDENCE_REVIEW_REQUIRED")
    void tc04_cccdInvalidNumber_shouldBeLowConfidence() {
        OcrDocumentExtractRequestDto req = buildRequest("""
                Họ và tên: LE VAN TEST
                Ngày sinh: 10/10/1975
                Giới tính: Nam
                Quốc tịch: Việt Nam
                Số: 123456789012
                Nơi thường trú: 456 Phố Test, Hà Nội
                """); // 123456789012 không khớp pattern CCCD (phải bắt đầu bằng 0)

        OcrStayGuestDraftDto draft = service.extract(req);

        // Không nhận ra CCCD hợp lệ → bị đưa vào lowFields
        assertThat(draft.getMissingOrLowConfidenceFields()).contains("documentNumber");
    }

    // =====================================================================
    // TC-05: Văn bản nhiễu hoàn toàn → UNREADABLE_DOCUMENT
    // =====================================================================

    @Test
    @DisplayName("TC-05: Văn bản nhiễu hoàn toàn → UNREADABLE_DOCUMENT")
    void tc05_unreadableText_shouldReturnUnreadable() {
        OcrDocumentExtractRequestDto req = buildRequest(
                "@@## $$$%%% *** ??? ... !!!");

        OcrStayGuestDraftDto draft = service.extract(req);

        assertThat(draft.getStatus()).isEqualTo(OcrExtractionStatus.UNREADABLE_DOCUMENT);
        assertThat(draft.isRequiresHumanVerification()).isTrue();
        assertThat(draft.getOverallConfidence()).isEqualTo(0.0);
    }

    // =====================================================================
    // TC-06: Hộ chiếu MRZ khách VNM → permanentAddress bắt buộc theo BCA
    // =====================================================================

    @Test
    @DisplayName("TC-06: Passport MRZ quốc tịch VNM → permanentAddress bắt buộc theo BCA")
    void tc06_passportVnm_requiresAddress() {
        OcrDocumentExtractRequestDto req = buildRequest("""
                P<VNMHOA<<THI<<TEST<<<<<<<<<<<<<<<<<<<<<<<<<<<
                B987654323VNM8501019F2812313<<<<<<<<<<<<<<02
                """);

        OcrStayGuestDraftDto draft = service.extract(req);

        // Khách VNM không có địa chỉ trong text → thiếu permanentAddress
        assertThat(draft.getMissingOrLowConfidenceFields()).contains("permanentAddress");
        assertThat(draft.isRequiresHumanVerification()).isTrue();
    }

    // =====================================================================
    // TC-07: requiresHumanVerification luôn true – bất biến kiến trúc
    // =====================================================================

    @Test
    @DisplayName("TC-07: requiresHumanVerification luôn true bất kể kết quả")
    void tc07_requiresHumanVerificationAlwaysTrue() {
        // Success case
        OcrDocumentExtractRequestDto successReq = buildRequest("""
                Họ và tên: PHAM VAN TEST
                Số CCCD: 079901999888
                Ngày sinh: 01/01/2000
                Giới tính: Nam
                Quốc tịch: Việt Nam
                Nơi thường trú: 789 Đường Test, Đà Nẵng
                """);
        assertThat(service.extract(successReq).isRequiresHumanVerification()).isTrue();

        // Unreadable case
        OcrDocumentExtractRequestDto badReq = buildRequest("xyz abc 123 qwerty");
        assertThat(service.extract(badReq).isRequiresHumanVerification()).isTrue();
    }

    // =====================================================================
    // TC-08 (TC-31): Ngày sinh không hợp lệ trên lịch (31/02/1995) → LOW_CONFIDENCE, không ném 500
    // =====================================================================

    @Test
    @DisplayName("TC-08: Ngày sinh OCR bị mờ/sai lịch (31/02/1995) → bắt DateTimeException, không ném HTTP 500")
    void tc08_invalidCalendarDate_shouldHandleGracefullyWithoutThrowing500() {
        OcrDocumentExtractRequestDto req = buildRequest("""
                Căn cước công dân
                Họ và tên: NGUYEN VAN TEST
                Ngày sinh: 31/02/1995
                Giới tính: Nam
                Quốc tịch: Việt Nam
                Số CCCD: 079195012345
                Nơi thường trú: 123 Đường Test, Quận 1, TP. Hồ Chí Minh
                """);

        OcrStayGuestDraftDto draft = service.extract(req);

        assertThat(draft.getStatus()).isEqualTo(OcrExtractionStatus.LOW_CONFIDENCE_REVIEW_REQUIRED);
        assertThat(draft.isRequiresHumanVerification()).isTrue();
        assertThat(draft.getDateOfBirth().isValid()).isFalse();
        assertThat(draft.getMissingOrLowConfidenceFields()).contains("dateOfBirth");
    }

    // =====================================================================
    // TC-09 (TC-32): MRZ check digit không khớp → LOW_CONFIDENCE_REVIEW_REQUIRED
    // =====================================================================

    @Test
    @DisplayName("TC-09: Passport MRZ check digit sai lệch (OCR mờ) → LOW_CONFIDENCE_REVIEW_REQUIRED, confidence < 0.85")
    void tc09_mrzCheckDigitMismatch_shouldRequireReview() {
        // Thay check digit của số hộ chiếu từ 4 thành 9 (sai lệch)
        OcrDocumentExtractRequestDto req = buildRequest("""
                UNITED STATES OF AMERICA
                P<USASMITH<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                A123456789USA9006153M2512314<<<<<<<<<<<<<<06
                """);
        req.setDocumentTypeHint(DocumentType.PASSPORT);

        OcrStayGuestDraftDto draft = service.extract(req);

        assertThat(draft.getStatus()).isEqualTo(OcrExtractionStatus.LOW_CONFIDENCE_REVIEW_REQUIRED);
        assertThat(draft.getDocumentNumber().getConfidence()).isLessThan(0.85);
        assertThat(draft.getDocumentNumber().isValid()).isFalse();
        assertThat(draft.getMissingOrLowConfidenceFields()).contains("documentNumber");
        assertThat(draft.getWarnings()).anyMatch(w -> w.contains("Check digit số hộ chiếu không khớp"));
    }

    // =====================================================================
    // TC-10 (TC-33): MRZ năm sinh cuối thập niên 1920 (1928) → nhận diện đúng thế kỷ 20
    // =====================================================================

    @Test
    @DisplayName("TC-10: Passport MRZ năm sinh 1928 (YY=28) → giải mã đúng 1928, không bị đẩy sang tương lai 2028")
    void tc10_mrzLate1920sBirthYear_shouldParseAsTwentiethCentury() {
        // DOB = 280515, check digit = 1 (1928-05-15)
        OcrDocumentExtractRequestDto req = buildRequest("""
                UNITED STATES OF AMERICA
                P<USASMITH<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                A123456784USA2805151M2512314<<<<<<<<<<<<<<06
                """);
        req.setDocumentTypeHint(DocumentType.PASSPORT);

        OcrStayGuestDraftDto draft = service.extract(req);

        assertThat(draft.getDateOfBirth().getValue()).isEqualTo(LocalDate.of(1928, 5, 15));
        assertThat(draft.getDateOfBirth().isValid()).isTrue();
        assertThat(draft.getDateOfBirth().getConfidence()).isGreaterThanOrEqualTo(0.85);
        assertThat(draft.getMissingOrLowConfidenceFields()).doesNotContain("dateOfBirth");
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private OcrDocumentExtractRequestDto buildRequest(String rawOcrText) {
        OcrDocumentExtractRequestDto req = new OcrDocumentExtractRequestDto();
        req.setFileName("test-doc.jpg");
        req.setContentType("image/jpeg");
        req.setFileSizeBytes(102400L);
        req.setRawOcrText(rawOcrText);
        return req;
    }
}
