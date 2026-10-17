package vn.edu.utc.hotel_booking.modules.ai.ocr.enums;

/**
 * Trạng thái kết quả bóc tách OCR cho giấy tờ tuỳ thân.
 * <ul>
 *   <li>{@code SUCCESS} – Bóc tách thành công, đủ độ tin cậy. Vẫn bắt buộc xác minh thủ công.</li>
 *   <li>{@code LOW_CONFIDENCE_REVIEW_REQUIRED} – Bóc tách được nhưng ≥1 trường dưới ngưỡng tin cậy.</li>
 *   <li>{@code UNREADABLE_DOCUMENT} – Không thể đọc được số giấy tờ và/hoặc họ tên.</li>
 *   <li>{@code INVALID_FILE} – File không hợp lệ (sai định dạng, quá dung lượng, path traversal...).</li>
 * </ul>
 */
public enum OcrExtractionStatus {
    SUCCESS,
    LOW_CONFIDENCE_REVIEW_REQUIRED,
    UNREADABLE_DOCUMENT,
    INVALID_FILE
}
