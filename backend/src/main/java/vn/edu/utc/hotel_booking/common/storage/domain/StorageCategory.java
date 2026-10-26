package vn.edu.utc.hotel_booking.common.storage.domain;

/**
 * Phân loại danh mục tài nguyên tệp tin và đa phương tiện toàn hệ thống.
 * Tách biệt nghiệp vụ thuần túy khỏi các ràng buộc kỹ thuật lưu trữ.
 */
public enum StorageCategory {
    BRAND_ASSET,
    HOTEL_ROOM_PHOTO,
    HOTEL_AMENITY,
    MARKETING_BANNER,
    USER_AVATAR,
    GUEST_REVIEW_PHOTO,
    SECURE_CONTRACT,
    GUEST_IDENTITY,
    FINANCIAL_RECEIPT,
    SYSTEM_EXPORT
}
