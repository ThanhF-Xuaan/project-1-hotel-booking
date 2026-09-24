package vn.edu.utc.hotel_booking.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi chưa được phân loại", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Khóa mã lỗi không hợp lệ", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(1002, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    NOT_FOUND(1004, "Không tìm thấy dữ liệu", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Chưa xác thực (Vui lòng đăng nhập)", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    CONFLICT(1009, "Dữ liệu đã thay đổi, vui lòng thử lại", HttpStatus.CONFLICT),
    METHOD_NOT_ALLOWED(1010, "Phương thức HTTP không được hỗ trợ", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(1011, "Định dạng nội dung không được hỗ trợ", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    NOT_ACCEPTABLE(1012, "Không thể trả nội dung theo định dạng yêu cầu", HttpStatus.NOT_ACCEPTABLE),
    HTTP_REQUEST_ERROR(1013, "Không thể xử lý yêu cầu HTTP", HttpStatus.BAD_REQUEST),
    PRICING_UNAVAILABLE(2101, "Chưa thể báo giá cho lựa chọn này", HttpStatus.UNPROCESSABLE_ENTITY),
    AI_UNAVAILABLE(3101, "Chatbot hiện chưa sẵn sàng", HttpStatus.SERVICE_UNAVAILABLE),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
