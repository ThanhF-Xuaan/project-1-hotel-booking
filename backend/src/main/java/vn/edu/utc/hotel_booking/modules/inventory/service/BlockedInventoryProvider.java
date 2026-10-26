package vn.edu.utc.hotel_booking.modules.inventory.service;

import java.time.LocalDate;

/**
 * Service Provider Interface (SPI) cho phép các module khác (như booking - Group Room Block)
 * cung cấp số lượng phòng đang bị phong tỏa (block) theo từng hạng phòng và ngày lưu trú,
 * tuân thủ nguyên lý Đảo ngược phụ thuộc (DIP) để loại bỏ Circular Dependency giữa Inventory và Booking.
 */
public interface BlockedInventoryProvider {

    /**
     * Lấy số lượng phòng đang bị block bởi các đoàn đã xác nhận cọc (DEFINITE)
     * mà chưa được phân bổ (unpicked-up) cho ngày lưu trú cụ thể.
     *
     * @param hotelRoomTypeId ID hạng phòng tại cơ sở khách sạn
     * @param stayDate Ngày lưu trú cần kiểm tra
     * @return Số lượng phòng đang bị giữ riêng cho đoàn
     */
    int getActiveBlockedRooms(Integer hotelRoomTypeId, LocalDate stayDate);
}
