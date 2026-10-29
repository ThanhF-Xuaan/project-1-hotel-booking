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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties engineProperties;

    @Mock
    private OcrVisionPort ocrVisionPort;

    @Mock
    private OcrUploadValidator uploadValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        engineProperties = new vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties();
        IdentityDocumentOcrController controller =
                new IdentityDocumentOcrController(ocrVisionPort, uploadValidator, engineProperties);
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

    @Test
    @DisplayName("/extract-file thiếu rawOcrText trả UNREADABLE_DOCUMENT, không giả lập OCR")
    void extractFile_missingRawOcrText_returnsUnreadableWithoutCallingExtractor() throws Exception {
        when(uploadValidator.validate(any()))
                .thenReturn(OcrUploadValidator.ValidationResult.ok());

        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile file = new MockMultipartFile("file", "doc.jpg", "image/jpeg", jpegHeader);

        mockMvc.perform(multipart("/api/v1/ai/ocr/extract-file").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("UNREADABLE_DOCUMENT"))
                .andExpect(jsonPath("$.result.overallConfidence").value(0.0))
                .andExpect(jsonPath("$.result.missingOrLowConfidenceFields[0]").value("rawOcrText"))
                .andExpect(jsonPath("$.result.warnings[0]").value(
                        org.hamcrest.Matchers.containsString("không tự nhận dạng nội dung ảnh")));

        verify(ocrVisionPort, never()).extract(any());
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


    @Test
    void enabledImageUsesCompleteBytesAndIgnoresClientText() throws Exception {
        engineProperties.setEnabled(true);
        byte[] bytes = syntheticImage();
        when(uploadValidator.validate(any())).thenReturn(OcrUploadValidator.ValidationResult.ok());
        when(uploadValidator.validateForOcr(any(), any(), any())).thenReturn(OcrUploadValidator.ValidationResult.ok());
        when(ocrVisionPort.extractImage(any(), any())).thenReturn(buildSuccessDraft());
        mockMvc.perform(multipart("/api/v1/ai/ocr/extract-file")
                        .file(new MockMultipartFile("file", "synthetic.png", "image/png", bytes))
                        .param("rawOcrText", "CLIENT_TEXT_MUST_NOT_OVERRIDE_IMAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.requiresHumanVerification").value(true));
        org.mockito.ArgumentCaptor<byte[]> image = org.mockito.ArgumentCaptor.forClass(byte[].class);
        org.mockito.ArgumentCaptor<OcrDocumentExtractRequestDto> metadata = org.mockito.ArgumentCaptor.forClass(OcrDocumentExtractRequestDto.class);
        verify(ocrVisionPort).extractImage(image.capture(), metadata.capture());
        assertThat(image.getValue()).isEqualTo(bytes);
        assertThat(image.getValue().length).isGreaterThan(16);
        assertThat(metadata.getValue().getRawOcrText()).isNull();
        verify(ocrVisionPort, never()).extract(any());
    }

    @Test
    void enabledEngineFailureReturnsSanitized503WithoutTextFallback() throws Exception {
        engineProperties.setEnabled(true);
        when(uploadValidator.validate(any())).thenReturn(OcrUploadValidator.ValidationResult.ok());
        when(uploadValidator.validateForOcr(any(), any(), any())).thenReturn(OcrUploadValidator.ValidationResult.ok());
        when(ocrVisionPort.extractImage(any(), any())).thenThrow(
                new OcrVisionPort.ExtractionUnavailableException("SYNTHETIC_SECRET_STDERR"));
        Logger logger = (Logger) LoggerFactory.getLogger(IdentityDocumentOcrController.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>(); logs.start(); logger.addAppender(logs);
        try {
            String body = mockMvc.perform(multipart("/api/v1/ai/ocr/extract-file")
                            .file(new MockMultipartFile("file", "synthetic.png", "image/png", syntheticImage()))
                            .param("rawOcrText", "SYNTHETIC_SECRET_CLIENT_TEXT"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value(503))
                    .andExpect(jsonPath("$.result").doesNotExist()).andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("SYNTHETIC_SECRET");
            assertThat(logs.list).noneMatch(event -> event.getFormattedMessage().contains("SYNTHETIC_SECRET"));
            verify(ocrVisionPort, never()).extract(any());
        } finally { logger.detachAppender(logs); logs.stop(); }
    }

    @Test
    void enabledInvalidImageReturns400BeforeEngine() throws Exception {
        engineProperties.setEnabled(true);
        when(uploadValidator.validate(any())).thenReturn(OcrUploadValidator.ValidationResult.ok());
        when(uploadValidator.validateForOcr(any(), any(), any())).thenReturn(OcrUploadValidator.ValidationResult.fail("bad image"));
        mockMvc.perform(multipart("/api/v1/ai/ocr/extract-file")
                        .file(new MockMultipartFile("file", "synthetic.png", "image/png", new byte[16])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result.status").value("INVALID_FILE"))
                .andExpect(jsonPath("$.result.requiresHumanVerification").value(true));
        verify(ocrVisionPort, never()).extractImage(any(), any());
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "TESSERACT_EXE", matches = ".+")
    void multipartWithoutTextRunsRealTesseractAndReturnsDraft() throws Exception {
        engineProperties.setEnabled(true);
        engineProperties.setExecutablePath(System.getenv("TESSERACT_EXE"));
        OcrUploadValidator validator = new OcrUploadValidator();
        OcrVisionPort realPort = new vn.edu.utc.hotel_booking.modules.ai.ocr.adapter.TesseractOcrVisionAdapter(
                engineProperties, new vn.edu.utc.hotel_booking.modules.ai.ocr.service.IdentityDocumentOcrService(),
                validator, new vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser());
        MockMvc imageMvc = MockMvcBuilders.standaloneSetup(
                new IdentityDocumentOcrController(realPort, validator, engineProperties)).build();
        imageMvc.perform(multipart("/api/v1/ai/ocr/extract-file")
                        .file(new MockMultipartFile("file", "synthetic.png", "image/png", syntheticImage())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.documentNumber.value").value("079190012345"))
                .andExpect(jsonPath("$.result.fullName.value").value(org.hamcrest.Matchers.containsString("NGUYEN VAN TEST")))
                .andExpect(jsonPath("$.result.requiresHumanVerification").value(true));
    }

    private byte[] syntheticImage() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/ai/ocr/cccd-synthetic.png")) {
            if (input == null) throw new IllegalStateException("Missing synthetic fixture");
            return input.readAllBytes();
        }
    }

    @Test
    void sevenDraftValuesUseStayGuestTypesAndRemainHumanReviewOnly() throws Exception {
        var properties = new vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties();
        var validator = new OcrUploadValidator();
        var port = new vn.edu.utc.hotel_booking.modules.ai.ocr.adapter.TesseractOcrVisionAdapter(
                properties, new vn.edu.utc.hotel_booking.modules.ai.ocr.service.IdentityDocumentOcrService(),
                validator, new vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser());
        var mvc = MockMvcBuilders.standaloneSetup(new IdentityDocumentOcrController(port, validator, properties)).build();
        var request = buildValidRequest();
        request.setRawOcrText("Căn cước công dân\nHọ và tên: NGUYEN VAN TEST\nNgày sinh: 15/06/1990\n"
                + "Giới tính: Nam\nQuốc tịch: Việt Nam\nSố CCCD: 079190012345\nNơi thường trú: 123 Duong Test");
        var response = mvc.perform(post("/api/v1/ai/ocr/extract")
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.requiresHumanVerification").value(true))
                .andExpect(jsonPath("$.result.dateOfBirth.value").value("1990-06-15"))
                .andReturn().getResponse();
        var draft = objectMapper.readTree(response.getContentAsByteArray()).get("result");
        var values = objectMapper.createObjectNode();
        var fields = List.of("fullName", "dateOfBirth", "gender", "nationality",
                "documentType", "documentNumber", "permanentAddress");
        for (String field : fields) {
            assertThat(draft.path(field).path("value").isTextual()).as(field).isTrue();
            values.set(field, draft.get(field).get("value"));
        }
        var mapper = objectMapper.copy().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        var mapped = mapper.treeToValue(values,
                vn.edu.utc.hotel_booking.modules.operation.dto.request.StayGuestCreateRequest.class);
        org.assertj.core.api.SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(mapped.getFullName()).isEqualTo("NGUYEN VAN TEST");
            soft.assertThat(mapped.getDateOfBirth()).isEqualTo(java.time.LocalDate.of(1990, 6, 15));
            soft.assertThat(mapped.getGender()).isEqualTo(vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender.MALE);
            soft.assertThat(mapped.getNationality()).isEqualTo("Việt Nam");
            soft.assertThat(mapped.getDocumentType()).isEqualTo(vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType.CCCD);
            soft.assertThat(mapped.getDocumentNumber()).isEqualTo("079190012345");
            soft.assertThat(mapped.getPermanentAddress()).isEqualTo("123 Duong Test");
        });
        // This is a contract projection, not the M3 form or a call to any write endpoint.
        assertThat(values.has("requiresHumanVerification")).isFalse();
        assertThat(draft.get("requiresHumanVerification").asBoolean()).isTrue();
    }

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

/** Isolated real servlet/filter tests. No application scan, database, Redis, or Keycloak server. */
class OcrMultipartServletRuntimeTest {
    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path directory;

    @Test
    void defaultMultipartProfileActuallySpoolsASyntheticPart() throws Exception {
        try (var runtime = new RuntimeProfile(directory.resolve("defaults"), false, false)) {
            assertThat(runtime.multipart.getFileSizeThreshold()).isZero();
            assertThat(runtime.multipart.getMaxFileSize()).isEqualTo(1_048_576);
            var response = runtime.upload(runtime.token("RECEPTIONIST"), image(), null);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(runtime.inMemory).contains(false);
            // Positive control: the watcher observes creation even if request cleanup deletes the file.
            assertThat(runtime.uploadFilesCreated()).isGreaterThan(0);
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "TESSERACT_EXE", matches = ".+")
    void approvedLocalProfileKeepsValidBoundaryAndRejectedUploadsInMemory() throws Exception {
        for (boolean enabled : new boolean[]{false, true}) {
            try (var runtime = new RuntimeProfile(directory.resolve("memory-" + enabled), true, enabled)) {
                assertThat(runtime.multipart.getFileSizeThreshold()).isEqualTo(6_291_456);
                assertThat(runtime.multipart.getMaxFileSize()).isEqualTo(5_242_880);
                assertThat(runtime.multipart.getMaxRequestSize()).isEqualTo(6_291_456);
                String token = runtime.token("RECEPTIONIST");
                var normal = runtime.upload(token, image(), null);
                assertThat(normal.statusCode()).isEqualTo(200);
                var draft = runtime.json(normal).path("result");
                assertThat(draft.path("requiresHumanVerification").asBoolean()).isTrue();
                if (enabled) assertThat(draft.path("documentNumber").path("value").asText()).isEqualTo("079190012345");
                else assertThat(draft.path("status").asText()).isEqualTo("UNREADABLE_DOCUMENT");

                byte[] boundary = java.util.Arrays.copyOf(image(), 5_242_880);
                assertThat(runtime.upload(token, boundary, null).statusCode()).isEqualTo(200);
                assertThat(runtime.upload(token, java.util.Arrays.copyOf(boundary, boundary.length + 1), null).statusCode()).isEqualTo(413);
                assertThat(runtime.upload(token, new byte[16], null).statusCode()).isEqualTo(400);
                // Reject an over-limit whole request as well as a file exceeding the per-file cap.
                assertThat(runtime.upload(token, image(), "x".repeat(6_291_456)).statusCode()).isEqualTo(413);
                assertThat(runtime.inMemory).isNotEmpty().doesNotContain(false);
                assertThat(runtime.uploadFilesCreated()).isZero();
                try (var files = java.nio.file.Files.list(runtime.parts)) {
                    assertThat(files.toList()).isEmpty();
                }
            }
        }
    }

    @Test
    void realSecurityFiltersAndEngineFailureKeepTheLocalContract() throws Exception {
        for (boolean enabled : new boolean[]{false, true}) {
            try (var runtime = new RuntimeProfile(directory.resolve("security-" + enabled), true, enabled)) {
                assertThat(runtime.upload(null, image(), null).statusCode()).isEqualTo(401);
                assertThat(runtime.upload("invalid-synthetic-token", image(), null).statusCode()).isEqualTo(401);
                assertThat(runtime.upload(runtime.token("GUEST"), image(), null).statusCode()).isEqualTo(403);
                // A real adapter with a deliberately absent executable, not a mocked OCR response.
                runtime.context.getBean(vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties.class)
                        .setExecutablePath(directory.resolve("missing-synthetic-engine").toString());
                var response = runtime.upload(runtime.token("RECEPTIONIST"), image(),
                        "Họ và tên: CLIENT FALLBACK SENTINEL\nSố CCCD: 079190012345");
                if (enabled) {
                    assertThat(response.statusCode()).isEqualTo(503);
                    assertThat(runtime.json(response).path("code").asInt()).isEqualTo(503);
                    assertThat(runtime.json(response).hasNonNull("result")).isFalse();
                    assertThat(response.body()).doesNotContain("CLIENT FALLBACK SENTINEL", "missing-synthetic-engine");
                } else {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(runtime.json(response).path("result").path("fullName").path("value").asText())
                            .contains("CLIENT FALLBACK SENTINEL");
                    assertThat(runtime.json(response).path("result").path("requiresHumanVerification").asBoolean()).isTrue();
                }
                assertThat(runtime.inMemory).doesNotContain(false);
                assertThat(runtime.uploadFilesCreated()).isZero();
            }
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "OCR_KEYCLOAK_ISSUER", matches = ".+")
    void realKeycloakTokenAndJwksAuthorizeOnlyReceptionRole() throws Exception {
        try (var runtime = new RuntimeProfile(directory.resolve("keycloak"), true, true, true)) {
            assertThat(runtime.upload(null, image(), null).statusCode()).isEqualTo(401);
            assertThat(runtime.upload("invalid-token", image(), null).statusCode()).isEqualTo(401);
            assertThat(runtime.upload(System.getenv("OCR_KEYCLOAK_DENIED_TOKEN"), image(), null).statusCode()).isEqualTo(403);
            var response = runtime.upload(System.getenv("OCR_KEYCLOAK_RECEPTION_TOKEN"), image(), null);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(runtime.json(response).path("result").path("documentNumber").path("value").asText()).isEqualTo("079190012345");
            assertThat(runtime.inMemory).isNotEmpty().doesNotContain(false);
            assertThat(runtime.uploadFilesCreated()).isZero();
            if ("true".equals(System.getenv("OCR_RUN_API_SUITE"))) {
                var frontend = java.nio.file.Path.of("../frontend").toAbsolutePath().normalize();
                var builder = new ProcessBuilder("node", frontend.resolve("node_modules/@playwright/test/cli.js").toString(),
                        "test", "e2e/api/ocr-synthetic.spec.ts").directory(frontend.toFile());
                builder.environment().put("VITE_API_URL", runtime.url.substring(0, runtime.url.indexOf("/api/v1/")));
                builder.environment().put("OCR_RECEPTION_TOKEN", System.getenv("OCR_KEYCLOAK_RECEPTION_TOKEN"));
                builder.environment().put("OCR_IMAGE_ENABLED", "true");
                builder.redirectErrorStream(true);
                builder.redirectOutput(java.nio.file.Path.of("target/ocr02-playwright-api.log").toFile());
                var process = builder.start();
                try {
                    assertThat(process.waitFor(90, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                    assertThat(process.exitValue()).as("Playwright API suite").isZero();
                } finally { process.destroyForcibly(); }
            }
        }
    }

    private byte[] image() throws Exception {
        try (var stream = getClass().getResourceAsStream("/ai/ocr/cccd-synthetic.png")) {
            if (stream == null) throw new IllegalStateException("Missing synthetic fixture");
            return stream.readAllBytes();
        }
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.web.servlet.config.annotation.EnableWebMvc
    @org.springframework.context.annotation.Import({IdentityDocumentOcrController.class,
            vn.edu.utc.hotel_booking.common.config.SecurityConfig.class,
            vn.edu.utc.hotel_booking.common.config.JwtBlacklistFilter.class})
    static class RuntimeConfig {
        @org.springframework.context.annotation.Bean
        org.springframework.web.multipart.support.StandardServletMultipartResolver multipartResolver() {
            return new org.springframework.web.multipart.support.StandardServletMultipartResolver();
        }

        @org.springframework.context.annotation.Bean
        OcrUploadValidator validator() { return new OcrUploadValidator(); }

        @org.springframework.context.annotation.Bean
        vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties engineProperties(org.springframework.core.env.Environment env) {
            var properties = new vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties();
            properties.setEnabled(env.getProperty("ocr-test.enabled", Boolean.class, false));
            properties.setExecutablePath(System.getenv().getOrDefault("TESSERACT_EXE", "missing-synthetic-engine"));
            return properties;
        }

        @org.springframework.context.annotation.Bean
        OcrVisionPort port(vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties properties, OcrUploadValidator validator) {
            return new vn.edu.utc.hotel_booking.modules.ai.ocr.adapter.TesseractOcrVisionAdapter(properties,
                    new vn.edu.utc.hotel_booking.modules.ai.ocr.service.IdentityDocumentOcrService(),
                    validator, new vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser());
        }

        @org.springframework.context.annotation.Bean
        vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService blacklist() {
            // Only the Redis boundary is mocked. The production filter/converter/method checks run.
            return org.mockito.Mockito.mock(vn.edu.utc.hotel_booking.modules.identity.service.TokenBlacklistService.class);
        }

        @org.springframework.context.annotation.Bean
        java.security.KeyPair testKeyPair() throws Exception {
            var generator = java.security.KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }

        @org.springframework.context.annotation.Bean
        org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder(java.security.KeyPair pair, org.springframework.core.env.Environment env) {
            if (env.getProperty("ocr-test.keycloak", Boolean.class, false)) {
                String issuer = System.getenv("OCR_KEYCLOAK_ISSUER");
                var remote = org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                        .withJwkSetUri(issuer + "/protocol/openid-connect/certs").build();
                remote.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(issuer));
                return remote;
            }
            var decoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                    .withPublicKey((java.security.interfaces.RSAPublicKey) pair.getPublic()).build();
            decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer("ocr-local-test"));
            return decoder;
        }
    }

    private static class RuntimeProfile implements AutoCloseable {
        private final org.apache.catalina.startup.Tomcat tomcat = new org.apache.catalina.startup.Tomcat();
        private final org.springframework.web.context.support.AnnotationConfigWebApplicationContext context =
                new org.springframework.web.context.support.AnnotationConfigWebApplicationContext();
        private final java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        private final java.nio.file.Path parts;
        private final java.nio.file.WatchService watch;
        private final jakarta.servlet.MultipartConfigElement multipart;
        private final List<Boolean> inMemory = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final String url;

        RuntimeProfile(java.nio.file.Path root, boolean memorySafe, boolean enabled) throws Exception {
            this(root, memorySafe, enabled, false);
        }

        RuntimeProfile(java.nio.file.Path root, boolean memorySafe, boolean enabled, boolean keycloak) throws Exception {
            parts = java.nio.file.Files.createDirectories(root.resolve("parts"));
            watch = parts.getFileSystem().newWatchService();
            parts.register(watch, java.nio.file.StandardWatchEventKinds.ENTRY_CREATE);
            var properties = new org.springframework.boot.servlet.autoconfigure.MultipartProperties();
            properties.setLocation(parts.toString());
            if (memorySafe) {
                var yaml = new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
                yaml.setResources(new org.springframework.core.io.ClassPathResource("application-ocr-local.yaml"));
                var source = new org.springframework.core.env.PropertiesPropertySource("ocr-local", java.util.Objects.requireNonNull(yaml.getObject()));
                new org.springframework.boot.context.properties.bind.Binder(
                        org.springframework.boot.context.properties.source.ConfigurationPropertySources.from(source))
                        .bind("spring.servlet.multipart", org.springframework.boot.context.properties.bind.Bindable.ofInstance(properties));
            }
            var engineGuard = new vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrEngineProperties();
            engineGuard.setEnabled(enabled);
            new vn.edu.utc.hotel_booking.modules.ai.ocr.config.OcrMultipartPrivacyGuard(engineGuard, properties).afterPropertiesSet();
            multipart = properties.createMultipartConfig();
            tomcat.setBaseDir(java.nio.file.Files.createDirectories(root.resolve("server")).toString());
            tomcat.setPort(0);
            tomcat.getConnector().setProperty("address", "127.0.0.1");
            tomcat.getConnector().setProperty("minSpareThreads", "1");
            var servletContext = tomcat.addContext("", java.nio.file.Files.createDirectories(root.resolve("web")).toString());
            servletContext.setParentClassLoader(getClass().getClassLoader());
            context.setServletContext(servletContext.getServletContext());
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                    "ocr-isolated-test", java.util.Map.of("ocr-test.enabled", enabled, "ocr-test.keycloak", keycloak)));
            context.register(RuntimeConfig.class);
            var servlet = org.apache.catalina.startup.Tomcat.addServlet(servletContext, "ocr",
                    new org.springframework.web.servlet.DispatcherServlet(context));
            servlet.setLoadOnStartup(1);
            servlet.setMultipartConfigElement(multipart);
            servletContext.addServletMappingDecoded("/", "ocr");
            addFilter(servletContext, "encoding", new org.springframework.web.filter.CharacterEncodingFilter("UTF-8", true));
            addFilter(servletContext, "observe-parts", (request, response, chain) -> {
                var observed = new jakarta.servlet.http.HttpServletRequestWrapper((jakarta.servlet.http.HttpServletRequest) request) {
                    @Override
                    public java.util.Collection<jakarta.servlet.http.Part> getParts() throws java.io.IOException, jakarta.servlet.ServletException {
                        var result = super.getParts();
                        for (var part : result) {
                            try {
                                var field = org.apache.catalina.core.ApplicationPart.class.getDeclaredField("fileItem");
                                field.setAccessible(true);
                                var item = (org.apache.tomcat.util.http.fileupload.disk.DiskFileItem) field.get(part);
                                inMemory.add(item.isInMemory());
                            } catch (ReflectiveOperationException e) {
                                throw new jakarta.servlet.ServletException("Cannot inspect synthetic multipart storage", e);
                            }
                        }
                        return result;
                    }
                };
                chain.doFilter(observed, response);
            });
            addFilter(servletContext, "security", new org.springframework.web.filter.DelegatingFilterProxy("springSecurityFilterChain", context));
            try {
                tomcat.start();
                url = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/api/v1/ai/ocr/extract-file";
            } catch (Exception e) {
                close();
                throw e;
            }
        }

        private void addFilter(org.apache.catalina.Context servletContext, String name, jakarta.servlet.Filter filter) {
            var definition = new org.apache.tomcat.util.descriptor.web.FilterDef();
            definition.setFilterName(name);
            definition.setFilter(filter);
            servletContext.addFilterDef(definition);
            var mapping = new org.apache.tomcat.util.descriptor.web.FilterMap();
            mapping.setFilterName(name);
            mapping.addURLPattern("/*");
            mapping.setDispatcher("REQUEST");
            servletContext.addFilterMap(mapping);
        }

        String token(String role) throws Exception {
            var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer("ocr-local-test").subject("synthetic-user")
                    .issueTime(new java.util.Date()).expirationTime(java.util.Date.from(java.time.Instant.now().plusSeconds(120)))
                    .claim("realm_access", java.util.Map.of("roles", List.of(role))).build();
            var jwt = new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.RS256), claims);
            jwt.sign(new com.nimbusds.jose.crypto.RSASSASigner(context.getBean(java.security.KeyPair.class).getPrivate()));
            return jwt.serialize();
        }

        java.net.http.HttpResponse<String> upload(String token, byte[] image, String text) throws Exception {
            String boundary = "ocr-test-" + java.util.UUID.randomUUID();
            var bytes = new java.io.ByteArrayOutputStream();
            bytes.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"synthetic.png\"\r\n"
                    + "Content-Type: image/png\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            bytes.write(image);
            if (text != null) bytes.write(("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"rawOcrText\"\r\n"
                    + "Content-Type: text/plain; charset=UTF-8\r\n\r\n" + text).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            bytes.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).timeout(java.time.Duration.ofSeconds(10))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary);
            if (token != null) request.header("Authorization", "Bearer " + token);
            return client.send(request.POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        }

        com.fasterxml.jackson.databind.JsonNode json(java.net.http.HttpResponse<String> response) throws Exception {
            return new ObjectMapper().readTree(response.body());
        }

        int uploadFilesCreated() throws Exception {
            int created = 0;
            for (var key = watch.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS); key != null; key = watch.poll()) {
                for (var event : key.pollEvents()) {
                    assertThat(event.kind()).isNotEqualTo(java.nio.file.StandardWatchEventKinds.OVERFLOW);
                    created += event.count();
                }
                key.reset();
            }
            return created;
        }

        @Override
        public void close() throws Exception {
            try {
                tomcat.stop();
                tomcat.destroy();
            } finally {
                context.close();
                client.close();
                watch.close();
            }
        }
    }
}
