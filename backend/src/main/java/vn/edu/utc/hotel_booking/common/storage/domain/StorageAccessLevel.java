package vn.edu.utc.hotel_booking.common.storage.domain;

/**
 * Cấp độ quyền truy cập kỹ thuật tại tầng hạ tầng (S3 / CDN).
 */
public enum StorageAccessLevel {
    /** Công khai: Cho phép người dùng và frontend tải trực tiếp qua CDN/MinIO public URL */
    PUBLIC_READ,

    /** Người dùng hợp lệ: Cần JWT Bearer Token để truy cập qua Gateway hoặc Presigned URL */
    AUTHENTICATED_READ,

    /** Bảo mật nghiêm ngặt: Khóa hoàn toàn, chỉ truy cập qua SigV4 Presigned URL ngắn hạn có kiểm soát RBAC */
    PRIVATE_RESTRICTED
}
