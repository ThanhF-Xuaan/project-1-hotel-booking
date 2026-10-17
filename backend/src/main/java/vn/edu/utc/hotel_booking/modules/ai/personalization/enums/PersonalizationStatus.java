package vn.edu.utc.hotel_booking.modules.ai.personalization.enums;

/**
 * Trạng thái kết quả phân tích cá nhân hoá sở thích khách hàng.
 */
public enum PersonalizationStatus {
    /** Đã phân tích thành công và có gợi ý phù hợp dựa trên lịch sử lưu trú. */
    PERSONALIZED_READY,

    /** Chưa có hoặc không đủ lịch sử hợp lệ (Cold Start) -> gợi ý dự phòng tiêu chuẩn. */
    COLD_START_FALLBACK,

    /** Khách hàng chưa cấp quyền sử dụng dữ liệu lịch sử lưu trú (consentGranted == false). */
    CONSENT_REQUIRED,

    /** Yêu cầu chứa toàn bộ thuộc tính nhạy cảm bị nghiêm cấm phân loại hồ sơ. */
    SENSITIVE_INPUT_REJECTED
}
