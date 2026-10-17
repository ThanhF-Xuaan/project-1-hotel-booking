package vn.edu.utc.hotel_booking.modules.ai.ocr.util;

/**
 * Tiện ích che giấu thông tin nhận dạng cá nhân (PII) trước khi ghi log.
 *
 * <p>Tất cả số giấy tờ, họ tên, địa chỉ thực sự KHÔNG ĐƯỢC ghi vào log.
 * Mọi thông tin nhạy cảm phải đi qua {@link #redact(String)} trước.</p>
 */
public final class PiiRedactionUtil {

    private static final int VISIBLE_SUFFIX_LENGTH = 3;
    private static final char MASK_CHAR = '*';

    private PiiRedactionUtil() {}

    /**
     * Che giấu giá trị: giữ lại tối đa {@code VISIBLE_SUFFIX_LENGTH} ký tự cuối,
     * phần còn lại thay bằng {@code MASK_CHAR}.
     *
     * <p>Ví dụ: {@code "079201012345"} → {@code "*********345"}.</p>
     *
     * @param value Giá trị cần che giấu (null-safe)
     * @return Chuỗi đã che giấu; "[null]" nếu đầu vào null; "[empty]" nếu rỗng.
     */
    public static String redact(String value) {
        if (value == null) return "[null]";
        if (value.isBlank()) return "[empty]";
        int len = value.length();
        if (len <= VISIBLE_SUFFIX_LENGTH) {
            return String.valueOf(MASK_CHAR).repeat(len);
        }
        int maskedCount = len - VISIBLE_SUFFIX_LENGTH;
        return String.valueOf(MASK_CHAR).repeat(maskedCount) + value.substring(maskedCount);
    }

    /**
     * Che giấu hoàn toàn – không để lộ bất kỳ ký tự nào.
     * Sử dụng cho mã số CCCD, passport, và tên đầy đủ.
     */
    public static String redactFull(String value) {
        if (value == null) return "[null]";
        if (value.isBlank()) return "[empty]";
        return "[REDACTED:" + value.length() + "chars]";
    }
}
