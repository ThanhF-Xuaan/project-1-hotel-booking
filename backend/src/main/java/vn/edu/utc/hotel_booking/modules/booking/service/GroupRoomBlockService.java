package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupRoomBlockResponse;

import java.time.LocalDate;
import java.util.List;

public interface GroupRoomBlockService {

    /**
     * Lấy tổng số lượng phòng đang bị block bởi các đoàn đã đặt cọc (DEFINITE)
     * mà chưa được gán khách (unpicked-up) cho một hạng phòng và ngày lưu trú cụ thể.
     */
    int getActiveBlockedRooms(Integer hotelRoomTypeId, LocalDate stayDate);

    /**
     * Lấy danh sách các block phòng kèm breakdown chi tiết theo ngày của một hợp đồng.
     */
    List<GroupRoomBlockResponse> getBlocksByContractId(Long contractId);

    /**
     * Tiến trình tự động quét và giải phóng các phòng đoàn chưa gán khách khi đến hoặc quá Cut-off Date.
     * @return Tổng số phòng dôi dư đã được giải phóng về Public Inventory
     */
    int processCutoffAutoRelease();

    /**
     * Tiến trình tự động hủy các hợp đồng TENTATIVE quá hạn đặt cọc 3 ngày giữ chỗ.
     * @return Số lượng hợp đồng đã bị hủy
     */
    int processDepositExpiredAutoCancel();

    /**
     * Tiến trình quét và gửi cảnh báo nhắc nhở hạn chót Cut-off Date cho Sales Staff trước 24h - 48h.
     * @return Số lượng hợp đồng đã được gửi cảnh báo
     */
    int processCutoffReminders();

    /**
     * Thao tác giải phóng thủ công các phòng chưa gán khách cho một block cụ thể của hợp đồng.
     */
    GroupRoomBlockResponse releaseUnpickedUpRoomsManually(Long contractId, Long blockId);
}
