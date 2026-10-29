package vn.edu.utc.hotel_booking.modules.ai.ocr.port;

import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto;

/**
 * Port bóc tách giấy tờ bằng văn bản OCR hoặc ảnh local.
 *
 * <p>Văn bản OCR hiện có được phân tích tất định. Ảnh được chuyển cho engine
 * local khi feature flag bật; không gọi provider bên ngoài.</p>
 *
 * <p><strong>Hợp đồng bất biến:</strong>
 * Mọi triển khai PHẢI đặt {@code requiresHumanVerification = true} trong kết quả.
 * Không được tự động lưu bất kỳ dữ liệu nào vào database.</p>
 */
public interface OcrVisionPort {

    /**
     * Bóc tách thông tin giấy tờ tuỳ thân từ văn bản OCR hiện có.
     *
     * @param request Yêu cầu chứa văn bản OCR thô và metadata file
     * @return Bản nháp trường dữ liệu để nhân viên xem xét – KHÔNG lưu tự động
     */
    OcrStayGuestDraftDto extract(OcrDocumentExtractRequestDto request);

    /**
     * Đọc ảnh trong bộ nhớ bằng engine local rồi dùng cùng bộ trích xuất tất định.
     * Implementations phải giữ cờ xác minh thủ công trong kết quả.
     */
    OcrStayGuestDraftDto extractImage(byte[] imageBytes, OcrDocumentExtractRequestDto metadata);

    /** Lỗi runtime local; caller có thể ánh xạ sang lỗi dependency unavailable. */
    final class ExtractionUnavailableException extends RuntimeException {
        public ExtractionUnavailableException(String message) { super(message); }
        public ExtractionUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
