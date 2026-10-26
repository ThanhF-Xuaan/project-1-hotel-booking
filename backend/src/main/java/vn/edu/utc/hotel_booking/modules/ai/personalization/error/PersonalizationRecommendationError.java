package vn.edu.utc.hotel_booking.modules.ai.personalization.error;

import org.springframework.http.HttpStatus;

public enum PersonalizationRecommendationError {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 1002, false, "Tham số yêu cầu không hợp lệ."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 1007, false, "Không có quyền thực hiện yêu cầu."),
    HOTEL_NOT_FOUND(HttpStatus.NOT_FOUND, 2021, false, "Không tìm thấy khách sạn đang hoạt động."),
    IDENTITY_LINK_REQUIRED(HttpStatus.CONFLICT, 11001, false,
            "Tài khoản chưa có liên kết hồ sơ cho demo U14."),
    CONSENT_REVISION_CONFLICT(HttpStatus.CONFLICT, 11002, false,
            "Consent đã được cập nhật. Tải lại trạng thái trước khi thao tác tiếp."),
    IDENTITY_BINDING_CONFLICT(HttpStatus.CONFLICT, 11004, false,
            "Liên kết hồ sơ đã thay đổi. Tải lại trạng thái consent."),
    DEPENDENCY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 11005, true,
            "Tạm thời không thể tạo gợi ý cá nhân hoá. Hãy thử lại sau.");

    private final HttpStatus httpStatus;
    private final int responseCode;
    private final boolean retryable;
    private final String message;

    PersonalizationRecommendationError(
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

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public int responseCode() {
        return responseCode;
    }

    public boolean retryable() {
        return retryable;
    }

    public String message() {
        return message;
    }
}
