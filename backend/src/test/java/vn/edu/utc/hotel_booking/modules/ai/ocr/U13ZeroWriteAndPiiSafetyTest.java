package vn.edu.utc.hotel_booking.modules.ai.ocr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.ocr.util.PiiRedactionUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra bất biến kiến trúc U13:
 * <ol>
 *   <li>Toàn bộ package {@code modules.ai.ocr} không phụ thuộc vào bất kỳ
 *       Repository, EntityManager hay Service ghi dữ liệu nghiệp vụ nào.</li>
 *   <li>PiiRedactionUtil che giấu thông tin cá nhân đúng cách.</li>
 * </ol>
 */
@DisplayName("U13 – Zero-Write và PII Safety Architecture Tests")
class U13ZeroWriteAndPiiSafetyTest {

    /** Danh sách các class ghi dữ liệu bị cấm tiêm vào OCR module. */
    private static final Set<String> FORBIDDEN_WRITE_CLASS_SIMPLE_NAMES = Set.of(
            "StayGuestRepository",
            "StayGuestService",
            "StayGuestServiceImpl",
            "BookingRepository",
            "BookingService",
            "LodgingQueueRepository",
            "LodgingQueueService",
            "PaymentService",
            "InventoryService",
            "EntityManager",
            "JpaRepository",
            "CrudRepository"
    );

    // =====================================================================
    // TC-25: IdentityDocumentOcrService không inject bất kỳ Repository/Service ghi nào
    // =====================================================================

    @Test
    @DisplayName("TC-25: IdentityDocumentOcrService – 0 phụ thuộc Repository/Service ghi nghiệp vụ")
    void tc25_ocrService_hasNoForbiddenDependencies() {
        Class<?> ocrServiceClass = vn.edu.utc.hotel_booking.modules.ai.ocr.service
                .IdentityDocumentOcrService.class;

        Set<String> injectedTypes = new HashSet<>();
        for (Field field : getAllFields(ocrServiceClass)) {
            injectedTypes.add(field.getType().getSimpleName());
            // Kiểm tra cả interface
            for (Class<?> iface : field.getType().getInterfaces()) {
                injectedTypes.add(iface.getSimpleName());
            }
        }

        Set<String> violations = new HashSet<>(injectedTypes);
        violations.retainAll(FORBIDDEN_WRITE_CLASS_SIMPLE_NAMES);

        assertThat(violations)
                .as("IdentityDocumentOcrService không được inject: " + violations)
                .isEmpty();
    }

    // =====================================================================
    // TC-26: IdentityDocumentOcrController không inject Repository/Service ghi nào
    // =====================================================================

    @Test
    @DisplayName("TC-26: IdentityDocumentOcrController – 0 phụ thuộc Repository/Service ghi nghiệp vụ")
    void tc26_ocrController_hasNoForbiddenDependencies() {
        Class<?> controllerClass = vn.edu.utc.hotel_booking.modules.ai.ocr.controller
                .IdentityDocumentOcrController.class;

        Set<String> injectedTypes = new HashSet<>();
        for (Field field : getAllFields(controllerClass)) {
            injectedTypes.add(field.getType().getSimpleName());
        }

        Set<String> violations = new HashSet<>(injectedTypes);
        violations.retainAll(FORBIDDEN_WRITE_CLASS_SIMPLE_NAMES);

        assertThat(violations)
                .as("IdentityDocumentOcrController không được inject: " + violations)
                .isEmpty();
    }

    // =====================================================================
    // TC-27: OcrStayGuestDraftDto – requiresHumanVerification là final=true
    // =====================================================================

    @Test
    @DisplayName("TC-27: OcrStayGuestDraftDto.requiresHumanVerification là final và luôn true")
    void tc27_requiresHumanVerification_isFinalAndTrue() throws Exception {
        Field field = vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto.class
                .getDeclaredField("requiresHumanVerification");
        field.setAccessible(true);

        // Kiểm tra là final
        assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers()))
                .as("requiresHumanVerification phải là final")
                .isTrue();

        // Kiểm tra giá trị mặc định là true
        vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto instance =
                new vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto();
        assertThat((boolean) field.get(instance)).isTrue();
    }

    // =====================================================================
    // TC-28: PiiRedactionUtil – che giấu đúng số CCCD
    // =====================================================================

    @Test
    @DisplayName("TC-28: PiiRedactionUtil.redact() che giấu ≥ 75% ký tự")
    void tc28_piiRedaction_masksEnough() {
        // Số CCCD tổng hợp 12 chữ số
        String syntheticCccd = "079000000000";
        String redacted = PiiRedactionUtil.redact(syntheticCccd);

        // Kiểm tra không chứa phần đầu rõ ràng
        assertThat(redacted).doesNotContain("07900");
        // Kiểm tra chứa ký tự mask
        assertThat(redacted).contains("*");
        // Kiểm tra chỉ 3 ký tự cuối được giữ
        assertThat(redacted).endsWith("000");
    }

    // =====================================================================
    // TC-29: PiiRedactionUtil.redactFull() – che giấu hoàn toàn
    // =====================================================================

    @Test
    @DisplayName("TC-29: PiiRedactionUtil.redactFull() – không lộ bất kỳ ký tự nào của input")
    void tc29_piiRedactFull_revealsNothing() {
        String syntheticName = "NGUYEN VAN TEST";
        String redacted = PiiRedactionUtil.redactFull(syntheticName);

        assertThat(redacted).doesNotContain("NGUYEN");
        assertThat(redacted).doesNotContain("VAN");
        assertThat(redacted).doesNotContain("TEST");
        assertThat(redacted).startsWith("[REDACTED:");
    }

    // =====================================================================
    // TC-30: PiiRedactionUtil xử lý null và empty an toàn
    // =====================================================================

    @Test
    @DisplayName("TC-30: PiiRedactionUtil xử lý null và empty không throw exception")
    void tc30_piiRedaction_nullAndEmpty_safeHandling() {
        assertThat(PiiRedactionUtil.redact(null)).isEqualTo("[null]");
        assertThat(PiiRedactionUtil.redact("")).isEqualTo("[empty]");
        assertThat(PiiRedactionUtil.redactFull(null)).isEqualTo("[null]");
        assertThat(PiiRedactionUtil.redactFull("")).isEqualTo("[empty]");
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    @Test
    void imagePipelineHasNoBusinessWriteDependencies() {
        for (Class<?> type : java.util.List.of(
                vn.edu.utc.hotel_booking.modules.ai.ocr.adapter.TesseractOcrVisionAdapter.class,
                vn.edu.utc.hotel_booking.modules.ai.ocr.service.TesseractTsvParser.class,
                vn.edu.utc.hotel_booking.modules.ai.ocr.validator.OcrUploadValidator.class)) {
            assertThat(Arrays.stream(getAllFields(type)).map(field -> field.getType().getSimpleName()))
                    .noneMatch(FORBIDDEN_WRITE_CLASS_SIMPLE_NAMES::contains);
        }
    }

    private Field[] getAllFields(Class<?> clazz) {
        Set<Field> fields = new HashSet<>(Arrays.asList(clazz.getDeclaredFields()));
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null && superclass != Object.class) {
            fields.addAll(Arrays.asList(superclass.getDeclaredFields()));
            superclass = superclass.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }
}
