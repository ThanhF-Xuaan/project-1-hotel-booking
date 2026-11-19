package vn.edu.utc.hotel_booking.modules.finance.entity;

/**
 * Vòng đời trạng thái của Sổ cái phòng (Guest Folio) theo chuẩn Oracle Opera PMS.
 */
public enum FolioStatus {
    /**
     * Sổ cái đang mở trong kỳ lưu trú. Cho phép ghi nhận Nợ (DEBIT) và Có (CREDIT).
     */
    OPEN,

    /**
     * Khóa tạm thời khi lễ tân mở Modal Check-out để tính tiền (ngăn chặn Race Condition).
     */
    LOCKED_FOR_SETTLEMENT,

    /**
     * Toàn bộ công nợ đã được tất toán về 0 đ (Balance Due = 0 đ). Đủ điều kiện Check-out.
     */
    SETTLED,

    /**
     * Đã hoàn tất Check-out và xuất hóa đơn VAT tài chính. Sổ cái bị đóng bất biến (Immutable).
     */
    CLOSED,

    /**
     * Sổ cái bị hủy bỏ (áp dụng cho đơn hủy hoặc No-Show đã xử lý phạt cọc).
     */
    VOID
}

