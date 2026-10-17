package vn.edu.utc.hotel_booking.modules.ai.personalization.enums;

/**
 * Phân khúc mẫu hành vi lưu trú của khách hàng.
 */
public enum TravelPatternSegment {
    /** Khách công tác thường xuyên (đi một mình, tần suất cao, ưu tiên bàn làm việc, tầng cao, dịch vụ văn phòng). */
    BUSINESS_FREQUENT,

    /** Gia đình đi nghỉ dưỡng (có trẻ em, ưu tiên phòng rộng, kết nối, tiện ích trẻ em). */
    FAMILY_LEISURE,

    /** Cặp đôi du lịch (2 người lớn, ưu tiên view biển, bồn tắm, không gian lãng mạn). */
    COUPLE_GETAWAY,

    /** Khách du lịch một mình (1 người lớn, không có trẻ em). */
    SOLO_TRAVELER,

    /** Phân khúc khách thông thường / chưa xác định rõ mẫu hành vi. */
    GENERAL
}
