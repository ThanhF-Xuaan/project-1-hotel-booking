package vn.edu.utc.hotel_booking.modules.inventory.service;

import java.time.LocalDate;

public interface RoomAvailabilityGenerationService {

    /**
     * Sinh sẵn các bản ghi tồn kho cho một hạng phòng cụ thể từ startDate trong số ngày horizonDays.
     * Tự động bỏ qua các ngày đã có bản ghi (Idempotent).
     *
     * @param hotelRoomTypeId ID của cấu hình hạng phòng khách sạn
     * @param startDate       Ngày bắt đầu sinh tồn kho
     * @param horizonDays     Số ngày mở bán cần sinh (thường là 365 ngày)
     * @return Số lượng bản ghi được sinh mới thành công
     */
    int generateForRoomType(Integer hotelRoomTypeId, LocalDate startDate, int horizonDays);

    /**
     * Cuộn chân trời tồn kho cho toàn bộ các hạng phòng ACTIVE trong chuỗi lên đến horizonDays (mặc định hôm nay + 365 ngày).
     *
     * @param horizonDays Số ngày của chân trời tồn kho
     * @return Tổng số lượng bản ghi được sinh mới trong toàn chuỗi
     */
    int rollHorizonForAllActiveRoomTypes(int horizonDays);

    /**
     * Cuộn chân trời tồn kho cho một khách sạn cụ thể (phục vụ đối soát ca đêm Night Audit).
     *
     * @param hotelId     ID khách sạn
     * @param horizonDays Số ngày của chân trời tồn kho
     * @return Số lượng bản ghi được sinh mới cho khách sạn
     */
    int rollHorizonForHotel(Short hotelId, int horizonDays);

    /**
     * Lan truyền sự thay đổi tổng số phòng (totalQuantity) cho các ngày từ ngày mai trở đi.
     *
     * @param hotelRoomTypeId  ID của cấu hình hạng phòng
     * @param newTotalQuantity Số lượng phòng thực tế mới
     * @return Số lượng bản ghi ngày tương lai được cập nhật
     */
    int propagateCapacityChange(Integer hotelRoomTypeId, int newTotalQuantity);

    /**
     * Đồng bộ số lượng phòng Out-of-Order (OOO) từ bảng room_maintenance_blocks vào room_availability.
     *
     * @param hotelRoomTypeId ID cấu hình hạng phòng
     * @param startDate       Ngày bắt đầu
     * @param endDate         Ngày kết thúc
     */
    void syncOooRooms(Integer hotelRoomTypeId, LocalDate startDate, LocalDate endDate);
}
