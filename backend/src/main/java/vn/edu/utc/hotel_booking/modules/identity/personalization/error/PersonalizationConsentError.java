package vn.edu.utc.hotel_booking.modules.identity.personalization.error;

import org.springframework.http.HttpStatus;

public enum PersonalizationConsentError {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 1002, false, "Dữ liệu đầu vào không hợp lệ."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 1007, false, "Không có quyền thực hiện yêu cầu."),
    HOTEL_NOT_FOUND(HttpStatus.NOT_FOUND, 2021, false, "Không tìm thấy khách sạn đang hoạt động."),
    IDENTITY_LINK_REQUIRED(HttpStatus.CONFLICT, 11001, false,
            "Tài khoản chưa có liên kết hồ sơ cho demo U14."),
    CONSENT_REVISION_CONFLICT(HttpStatus.CONFLICT, 11002, false,
            "Consent đã thay đổi. Hãy tải lại trạng thái trước khi thử lại."),
    CONSENT_POLICY_CONFLICT(HttpStatus.CONFLICT, 11003, false,
            "Phiên bản chính sách không còn hiệu lực. Hãy tải lại thông báo."),
    IDENTITY_BINDING_CONFLICT(HttpStatus.CONFLICT, 11004, false,
            "Liên kết hồ sơ đã thay đổi. Hãy tải lại trạng thái."),
    DEPENDENCY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 11005, true,
            "Tạm thời không thể xử lý consent. Hãy thử lại sau.");

    private final HttpStatus httpStatus;
    private final int responseCode;
    private final boolean retryable;
    private final String message;

    PersonalizationConsentError(
            HttpStatus httpStatus,
            int responseCode,
            boolean retryable,
            String message
    ) {
        this.httpStatus = httpStatus;
        this.responseCode = responseCode;
        this.retryable = retryable;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getMessage() {
        return message;
    }
}
