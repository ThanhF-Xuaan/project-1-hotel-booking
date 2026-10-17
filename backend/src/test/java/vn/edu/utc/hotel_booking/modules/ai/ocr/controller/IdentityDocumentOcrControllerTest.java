package vn.edu.utc.hotel_booking.modules.ai.ocr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.enums.OcrExtractionStatus;
import vn.edu.utc.hotel_booking.modules.ai.ocr.port.OcrVisionPort;
import vn.edu.utc.hotel_booking.modules.ai.ocr.validator.OcrUploadValidator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử Controller OCR qua MockMvc (standaloneSetup).
 *
 * <p>Dữ liệu tổng hợp – không chứa thông tin cá nhân thật.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdentityDocumentOcrController – MockMvc Tests")
class IdentityDocumentOcrControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OcrVisionPort ocrVisionPort;

    @Mock
    private OcrUploadValidator uploadValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        IdentityDocumentOcrController controller =
                new IdentityDocumentOcrController(ocrVisionPort, uploadValidator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // =====================================================================
    // TC-19: /extract – Happy path → 200 OK
    // =====================================================================

    @Test
    @DisplayName("TC-19: POST /extract với dữ liệu hợp lệ → 200 OK")
    void tc19_extract_validRequest_returns200() throws Exception {
        when(ocrVisionPort.extract(any())).thenReturn(buildSuccessDraft());

        OcrDocumentExtractRequestDto req = buildValidRequest();

        mockMvc.perform(post("/api/v1/ai/ocr/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // =====================================================================
    // TC-20: /extract – Thiếu rawOcrText → 400 BAD REQUEST (Bean Validation)
    // =====================================================================

    @Test
    @DisplayName("TC-20: POST /extract thiếu rawOcrText → 400 Bean Validation")
    void tc20_extract_missingRawOcrText_returns400() throws Exception {
        OcrDocumentExtractRequestDto req = new OcrDocumentExtractRequestDto();
        req.setFileName("doc.jpg");
        req.setContentType("image/jpeg");
        req.setFileSizeBytes(1024L);
        // rawOcrText bị bỏ trống

        mockMvc.perform(post("/api/v1/ai/ocr/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    // TC-21: /extract – Thiếu fileName → 400 BAD REQUEST
    // =====================================================================

    @Test
    @DisplayName("TC-21: POST /extract thiếu fileName → 400 Bean Validation")
    void tc21_extract_missingFileName_returns400() throws Exception {
        OcrDocumentExtractRequestDto req = new OcrDocumentExtractRequestDto();
        // fileName null
        req.setContentType("image/jpeg");
        req.setFileSizeBytes(1024L);
        req.setRawOcrText("NGUYEN VAN TEST 079190012345");

        mockMvc.perform(post("/api/v1/ai/ocr/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    // TC-22: /extract – Bean Validation (fileSizeBytes = 0) → 400
    // =====================================================================

    @Test
    @DisplayName("TC-22: POST /extract fileSizeBytes=0 → 400 Bean Validation")
    void tc22_extract_zeroFileSize_returns400() throws Exception {
        OcrDocumentExtractRequestDto req = new OcrDocumentExtractRequestDto();
        req.setFileName("doc.jpg");
        req.setContentType("image/jpeg");
        req.setFileSizeBytes(0L); // vi phạm @Min(1)
        req.setRawOcrText("NGUYEN VAN TEST 079190012345");

        mockMvc.perform(post("/api/v1/ai/ocr/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    // TC-23: /extract-file – File không hợp lệ → 400 BAD REQUEST
    // =====================================================================

    @Test
    @DisplayName("TC-23: /extract-file với validator từ chối → 400")
    void tc23_extractFile_invalidFile_returns400() throws Exception {
        when(uploadValidator.validate(any()))
                .thenReturn(OcrUploadValidator.ValidationResult.fail("Định dạng file không được hỗ trợ"));

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                "fake content".getBytes());

        mockMvc.perform(multipart("/api/v1/ai/ocr/extract-file")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    // TC-24: /extract-file – File hợp lệ → 200 OK
    // =====================================================================

    @Test
    @DisplayName("TC-24: /extract-file với file hợp lệ → 200 OK")
    void tc24_extractFile_validFile_returns200() throws Exception {
        when(uploadValidator.validate(any()))
                .thenReturn(OcrUploadValidator.ValidationResult.ok());
        when(ocrVisionPort.extract(any())).thenReturn(buildSuccessDraft());

        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile file = new MockMultipartFile("file", "doc.jpg", "image/jpeg", jpegHeader);

        mockMvc.perform(multipart("/api/v1/ai/ocr/extract-file")
                        .file(file)
                        .param("rawOcrText", "Họ và tên: TEST\nSố CCCD: 079190099999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // =====================================================================
    // TC-25: PreAuthorize xác nhận – Controller dùng đúng annotation
    // =====================================================================

    @Test
    @DisplayName("TC-25: Controller có @PreAuthorize với đúng 3 role (kiểm tra Reflection)")
    void tc25_controllerHasCorrectPreAuthorize() throws Exception {
        // Kiểm tra annotation tồn tại trên class
        Class<?> controllerClass = IdentityDocumentOcrController.class;
        org.springframework.security.access.prepost.PreAuthorize preAuth =
                controllerClass.getAnnotation(
                        org.springframework.security.access.prepost.PreAuthorize.class);

        org.junit.jupiter.api.Assertions.assertNotNull(preAuth,
                "Controller phải có @PreAuthorize");
        org.junit.jupiter.api.Assertions.assertTrue(
                preAuth.value().contains("CHAIN_ADMIN"),
                "@PreAuthorize phải bao gồm CHAIN_ADMIN");
        org.junit.jupiter.api.Assertions.assertTrue(
                preAuth.value().contains("PROPERTY_MANAGER"),
                "@PreAuthorize phải bao gồm PROPERTY_MANAGER");
        org.junit.jupiter.api.Assertions.assertTrue(
                preAuth.value().contains("RECEPTIONIST"),
                "@PreAuthorize phải bao gồm RECEPTIONIST");
    }

    // =====================================================================
    // TC-26: /extract – Không ghi log tên file chứa PII (khắc phục LOW-01)
    // =====================================================================

    @Test
    @DisplayName("TC-26: POST /extract – không ghi log tên file chứa thông tin PII giả lập")
    void tc26_extract_doesNotLogRawFileNameWithPii() throws Exception {
        Logger controllerLogger = (Logger) LoggerFactory.getLogger(IdentityDocumentOcrController.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        controllerLogger.addAppender(listAppender);
        Level originalLevel = controllerLogger.getLevel();
        controllerLogger.setLevel(Level.DEBUG);

        try {
            when(ocrVisionPort.extract(any())).thenReturn(buildSuccessDraft());

            String sensitiveFileName = "CCCD_079201012345_NGUYEN_VAN_TEST.jpg";
            OcrDocumentExtractRequestDto req = buildValidRequest();
            req.setFileName(sensitiveFileName);

            mockMvc.perform(post("/api/v1/ai/ocr/extract")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            boolean leakedSensitiveFileName = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("079201012345")
                            || event.getFormattedMessage().contains("NGUYEN_VAN_TEST"));
            assertThat(leakedSensitiveFileName)
                    .as("Log không được chứa số CCCD hoặc tên trong fileName")
                    .isFalse();

            boolean hasSafeMessage = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("U13 OCR /extract request received"));
            assertThat(hasSafeMessage)
                    .as("Log phải chứa thông báo tĩnh an toàn")
                    .isTrue();
        } finally {
            controllerLogger.setLevel(originalLevel);
            controllerLogger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    // =====================================================================
    // TC-27: /extract-file – Không ghi log tên file chứa PII (khắc phục LOW-01)
    // =====================================================================

    @Test
    @DisplayName("TC-27: POST /extract-file – không ghi log tên file chứa thông tin PII giả lập")
    void tc27_extractFile_doesNotLogRawFileNameWithPii() throws Exception {
        Logger controllerLogger = (Logger) LoggerFactory.getLogger(IdentityDocumentOcrController.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        controllerLogger.addAppender(listAppender);
        Level originalLevel = controllerLogger.getLevel();
        controllerLogger.setLevel(Level.DEBUG);

        try {
            when(uploadValidator.validate(any())).thenReturn(OcrUploadValidator.ValidationResult.ok());
            when(ocrVisionPort.extract(any())).thenReturn(buildSuccessDraft());

            String sensitiveFileName = "PASSPORT_B9876543_JOHN_TEST.jpg";
            byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                    0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
            MockMultipartFile file = new MockMultipartFile("file", sensitiveFileName, "image/jpeg", jpegHeader);

            mockMvc.perform(multipart("/api/v1/ai/ocr/extract-file")
                            .file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            boolean leakedSensitiveFileName = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("B9876543")
                            || event.getFormattedMessage().contains("JOHN_TEST"));
            assertThat(leakedSensitiveFileName)
                    .as("Log không được chứa số Passport hoặc tên trong fileName")
                    .isFalse();

            boolean hasSafeMessage = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("U13 OCR /extract-file: sizeBytes="));
            assertThat(hasSafeMessage)
                    .as("Log phải chứa thông báo sizeBytes an toàn")
                    .isTrue();
        } finally {
            controllerLogger.setLevel(originalLevel);
            controllerLogger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private OcrDocumentExtractRequestDto buildValidRequest() {
        OcrDocumentExtractRequestDto req = new OcrDocumentExtractRequestDto();
        req.setFileName("doc.jpg");
        req.setContentType("image/jpeg");
        req.setFileSizeBytes(102400L);
        req.setRawOcrText("Họ và tên: NGUYEN VAN TEST\nSố CCCD: 079190012345\nNgày sinh: 15/06/1990");
        return req;
    }

    private OcrStayGuestDraftDto buildSuccessDraft() {
        OcrStayGuestDraftDto draft = new OcrStayGuestDraftDto();
        draft.setStatus(OcrExtractionStatus.SUCCESS);
        draft.setOverallConfidence(0.90);
        draft.setMissingOrLowConfidenceFields(List.of());
        draft.setWarnings(List.of());
        return draft;
    }
}
