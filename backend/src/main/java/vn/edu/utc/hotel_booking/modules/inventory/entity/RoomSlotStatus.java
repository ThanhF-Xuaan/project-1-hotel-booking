package vn.edu.utc.hotel_booking.modules.inventory.entity;

/**
 * Trạng thái giữ chỗ / sử dụng phòng theo từng ngày lưu trú (Date-Specific Slot Status - Sparse Data).
 */
public enum RoomSlotStatus {
    READY("Trống, sẵn sàng bán hoặc xếp chỗ"),
    RESERVED("Đã gán cho một BookingRoom, giữ chỗ cho khách sắp đến"),
    OCCUPIED("Khách đã check-in và đang lưu trú thực tế trong đêm này"),
    BLOCKED("Bị khóa bởi lệnh bảo trì RoomMaintenanceBlock (OOO/OOS)"),
    DIRTY("Bẩn sau khi khách check-out hoặc chuyển phòng"),
    CLEANING("Đang được buồng phòng xử lý vệ sinh"),
    MAINTENANCE("Bảo trì / Kỹ thuật");

    private final String description;

    RoomSlotStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isBusy(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return RESERVED.name().equals(normalized)
                || OCCUPIED.name().equals(normalized)
                || BLOCKED.name().equals(normalized)
                || MAINTENANCE.name().equals(normalized)
                || DIRTY.name().equals(normalized)
                || CLEANING.name().equals(normalized);
    }
}

