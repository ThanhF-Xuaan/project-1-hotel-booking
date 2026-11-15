package vn.edu.utc.hotel_booking.modules.inventory.entity;

/**
 * Trạng thái vật lý tức thời của căn phòng tại thời điểm hiện tại (Real-time Housekeeping / Physical Status).
 */
public enum RoomInstanceStatus {
    READY("Phòng sạch, sẵn sàng nhận khách"),
    OCCUPIED("Phòng đang có khách lưu trú tại quầy"),
    DIRTY("Phòng bẩn sau check-out hoặc đổi phòng, chờ dọn"),
    CLEANING("Phòng đang được nhân viên buồng phòng dọn dẹp"),
    MAINTENANCE("Phòng đang khóa bảo trì, sửa chữa (Out of Order / Out of Service)");

    private final String description;

    RoomInstanceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValid(String status) {
        if (status == null) return false;
        for (RoomInstanceStatus s : values()) {
            if (s.name().equalsIgnoreCase(status.trim())) {
                return true;
            }
        }
        return false;
    }
}

