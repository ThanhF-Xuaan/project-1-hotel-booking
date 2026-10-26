package vn.edu.utc.hotel_booking.common.storage.domain;

/**
 * Trạng thái vòng đời nghiệp vụ của tệp tin đa phương tiện.
 */
public enum MediaLifecycleStatus {
    /** Đang soạn thảo bởi nhân viên / quản trị viên, chưa xuất bản */
    DRAFT,

    /** Đang chờ Antivirus quét mã độc */
    PENDING_SCAN,

    /** Đang chờ kiểm duyệt nội dung (ví dụ: ảnh khách upload review) */
    PENDING_MODERATION,

    /** Đã phê duyệt và xuất bản chính thức */
    PUBLISHED,

    /** Bị từ chối kiểm duyệt hoặc nhiễm mã độc */
    REJECTED,

    /** Đã thu hồi / xóa an toàn theo chính sách lưu trữ bảo vệ dữ liệu PII */
    PURGED
}
