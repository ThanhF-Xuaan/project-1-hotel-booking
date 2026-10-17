package vn.edu.utc.hotel_booking.modules.ai.ocr.model;

/**
 * Đóng gói giá trị bóc tách từng trường OCR kèm độ tin cậy và kết quả validation.
 *
 * @param <T> Kiểu dữ liệu của giá trị bóc tách (String, LocalDate, Gender, DocumentType...)
 */
public class ExtractedFieldDto<T> {

    /** Giá trị bóc tách được (null nếu không đọc được). */
    private final T value;

    /**
     * Điểm tin cậy trong khoảng [0.0, 1.0].
     * Ngưỡng chấp nhận: >= 0.85.
     */
    private final double confidence;

    /** true nếu giá trị hợp lệ về mặt nghiệp vụ (không chỉ là đọc được). */
    private final boolean valid;

    /** Thông điệp mô tả lý do không hợp lệ (null nếu valid == true). */
    private final String validationMessage;

    private ExtractedFieldDto(T value, double confidence, boolean valid, String validationMessage) {
        this.value = value;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.valid = valid;
        this.validationMessage = validationMessage;
    }

    public static <T> ExtractedFieldDto<T> of(T value, double confidence) {
        return new ExtractedFieldDto<>(value, confidence, true, null);
    }

    public static <T> ExtractedFieldDto<T> invalid(T value, double confidence, String reason) {
        return new ExtractedFieldDto<>(value, confidence, false, reason);
    }

    public static <T> ExtractedFieldDto<T> missing() {
        return new ExtractedFieldDto<>(null, 0.0, false, "Không đọc được / bị thiếu");
    }

    public T getValue() { return value; }
    public double getConfidence() { return confidence; }
    public boolean isValid() { return valid; }
    public String getValidationMessage() { return validationMessage; }

    /** Trả về true khi trường này đạt ngưỡng tin cậy và hợp lệ. */
    public boolean isHighConfidenceAndValid(double threshold) {
        return valid && confidence >= threshold;
    }

    @Override
    public String toString() {
        return "ExtractedFieldDto{confidence=" + confidence + ", valid=" + valid + "}";
    }
}
