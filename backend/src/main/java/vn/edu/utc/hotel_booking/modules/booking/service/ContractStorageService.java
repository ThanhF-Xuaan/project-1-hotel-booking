package vn.edu.utc.hotel_booking.modules.booking.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Hợp đồng giao tiếp lưu trữ tệp tin hợp đồng số.
 * Thiết kế trừu tượng hóa cho phép dễ dàng chuyển đổi giữa Local Storage, MinIO, hoặc AWS S3.
 */
public interface ContractStorageService {

    /**
     * Lưu trữ tệp tin hợp đồng vào hệ thống tệp an toàn.
     *
     * @param file Tệp tải lên
     * @param hotelId ID khách sạn cơ sở (cô lập tenant)
     * @param contractId ID hợp đồng đoàn
     * @return Đường dẫn tương đối an toàn của tệp (relative path)
     * @throws IOException Nếu xảy ra lỗi ghi tệp
     */
    String storeContractFile(MultipartFile file, Short hotelId, Long contractId) throws IOException;

    /**
     * Tải nội dung tệp tin hợp đồng theo đường dẫn tương đối.
     *
     * @param relativeFilePath Đường dẫn tương đối đã lưu trong DB
     * @return Mảng byte dữ liệu của tệp
     * @throws IOException Nếu xảy ra lỗi đọc tệp hoặc tệp không tồn tại
     */
    byte[] loadContractFile(String relativeFilePath) throws IOException;

    /**
     * Xóa tệp tin hợp đồng khỏi hệ thống lưu trữ vật lý (chỉ dùng khi rollback giao dịch tải lên lỗi).
     *
     * @param relativeFilePath Đường dẫn tương đối của tệp
     * @throws IOException Nếu xảy ra lỗi I/O
     */
    void deleteContractFile(String relativeFilePath) throws IOException;
}
