package vn.edu.utc.hotel_booking.modules.ai.ocr.port;

import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrDocumentExtractRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.ocr.dto.OcrStayGuestDraftDto;

/**
 * Port giao tiếp với bộ nhận diện ảnh bên ngoài (Vision provider).
 *
 * <p>Triển khai mặc định ({@code DeterministicOcrVisionAdapter}) sử dụng
 * phân tích văn bản tất định (regex + pattern matching) trên {@code rawOcrText}.
 * Khi có phê duyệt chính thức, có thể cắm thêm adapter gọi Vision API bên ngoài.</p>
 *
 * <p><strong>Hợp đồng bất biến:</strong>
 * Mọi triển khai PHẢI đặt {@code requiresHumanVerification = true} trong kết quả.
 * Không được tự động lưu bất kỳ dữ liệu nào vào database.</p>
 */
public interface OcrVisionPort {

    /**
     * Bóc tách thông tin giấy tờ tuỳ thân từ dữ liệu OCR thô.
     *
     * @param request Yêu cầu chứa văn bản OCR thô và metadata file
     * @return Bản nháp trường dữ liệu để nhân viên xem xét – KHÔNG lưu tự động
     */
    OcrStayGuestDraftDto extract(OcrDocumentExtractRequestDto request);
}
