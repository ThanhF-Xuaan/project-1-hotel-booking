package vn.edu.utc.hotel_booking.modules.ai.advisor.error;

import java.util.Objects;
import java.util.UUID;

/**
 * Advisor-local error envelope matching the frozen R1-00 {@code $defs.ErrorResponse} shape.
 *
 * <p>Successful responses keep using the shared {@code ApiResponse}. Errors are deliberately
 * represented by this narrow type so the advisor surface cannot accidentally emit the shared
 * {@code result}/{@code data} fields or a non-approved reason. Messages are generic and never
 * contain session identifiers, owner keys, criteria, provenance or exception details.</p>
 */
public record RoomAdvisorErrorResponse(int code, String message, ErrorDetail error) {

    public static final String CONTRACT_VERSION = "2.0";
    public static final int MIN_HTTP_CODE = 400;
    public static final int MAX_HTTP_CODE = 599;
    public static final int MAX_MESSAGE_LENGTH = 200;

    public RoomAdvisorErrorResponse {
        if (code < MIN_HTTP_CODE || code > MAX_HTTP_CODE) {
            throw new IllegalArgumentException("Advisor error code must be an HTTP status between 400 and 599");
        }
        if (message == null || message.isBlank() || message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException(
                    "Advisor error message must be non-blank and at most " + MAX_MESSAGE_LENGTH + " characters");
        }
        Objects.requireNonNull(error, "error metadata must not be null");
    }

    /** Nested error metadata required by the frozen schema. */
    public record ErrorDetail(String contractVersion, UUID requestId, Reason reason, boolean retryable) {

        public ErrorDetail {
            if (!CONTRACT_VERSION.equals(contractVersion)) {
                throw new IllegalArgumentException("Advisor error contractVersion must be " + CONTRACT_VERSION);
            }
            Objects.requireNonNull(requestId, "requestId must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /** Exact reason enum from the frozen R1-00 contract. Do not add values here. */
    public enum Reason {
        INVALID_REQUEST,
        SESSION_NOT_FOUND,
        SESSION_VERSION_CONFLICT,
        SCOPE_DENIED,
        RATE_LIMITED,
        UPSTREAM_UNAVAILABLE,
        AUTH_REQUIRED
    }

    /**
     * Builds a generic error body for one approved reason.
     *
     * @param code      HTTP status code (also mirrored into the numeric {@code code} field)
     * @param reason    approved frozen reason
     * @param retryable caller-supplied retry policy for this status
     */
    public static RoomAdvisorErrorResponse forReason(int code, Reason reason, boolean retryable) {
        Objects.requireNonNull(reason, "reason must not be null");
        return new RoomAdvisorErrorResponse(
                code,
                genericMessage(reason),
                new ErrorDetail(CONTRACT_VERSION, UUID.randomUUID(), reason, retryable));
    }

    private static String genericMessage(Reason reason) {
        return switch (reason) {
            case INVALID_REQUEST -> "Yêu cầu không hợp lệ. Vui lòng kiểm tra lại dữ liệu đã gửi.";
            case SESSION_NOT_FOUND -> "Không tìm thấy phiên tư vấn. Vui lòng bắt đầu phiên mới.";
            case SESSION_VERSION_CONFLICT ->
                    "Phiên tư vấn đã thay đổi. Vui lòng tải lại trước khi tiếp tục.";
            case SCOPE_DENIED -> "Phạm vi tìm kiếm không được phép.";
            case RATE_LIMITED -> "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.";
            case UPSTREAM_UNAVAILABLE -> "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.";
            case AUTH_REQUIRED -> "Cần xác thực để tiếp tục.";
        };
    }
}
