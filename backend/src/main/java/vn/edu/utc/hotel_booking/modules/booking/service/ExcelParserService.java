package vn.edu.utc.hotel_booking.modules.booking.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListPreviewResponse;

public interface ExcelParserService {

    /**
     * Phân tích và kiểm tra toàn diện cấu trúc file Excel Rooming List,
     * trả về danh sách dòng preview kèm lỗi chi tiết của từng dòng (1-based row index).
     *
     * @param file Tệp Excel (.xlsx) tải lên
     * @return RoomingListPreviewResponse chứa thông tin thống kê và các dòng dữ liệu
     */
    RoomingListPreviewResponse parseRoomingList(MultipartFile file);
}
