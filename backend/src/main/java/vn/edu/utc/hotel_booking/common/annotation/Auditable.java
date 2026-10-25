package vn.edu.utc.hotel_booking.common.annotation;

import java.lang.annotation.*;

/**
 * Annotation đánh dấu các phương thức Controller hoặc Service cần được tự động ghi vết kiểm toán (AOP Audit Log).
 * Hỗ trợ cú pháp Spring Expression Language (SpEL) để đánh giá động các tham số hoặc kết quả trả về.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * Loại hành vi kiểm toán (VD: BOOKING_ROOM_SWAP, BOOKING_CANCELLED, PAYMENT_REFUND, PRICING_RULE_UPDATE).
     */
    String actionType();

    /**
     * Tên thực thể hoặc bảng dữ liệu bị tác động (VD: Booking, BookingRoom, Payment, PricingRule, Staff).
     */
    String entityName();

    /**
     * Biểu thức SpEL để trích xuất ID của thực thể bị tác động.
     * Ví dụ: "#id", "#bookingRoomId", "#request.bookingId", "#result.id".
     * Nếu để trống, hệ thống sẽ cố gắng tìm tham số có tên "id" hoặc "bookingId".
     */
    String entityId() default "";

    /**
     * Biểu thức SpEL hoặc chuỗi mô tả chi tiết hành động.
     * Ví dụ: "'Đổi phòng ' + #request.fromRoomNumber + ' sang ' + #request.toRoomNumber".
     */
    String description() default "";

    /**
     * Có chụp snapshot tham số request và lưu vào newValues không.
     */
    boolean captureRequest() default true;

    /**
     * Có chụp snapshot kết quả trả về của hàm và lưu vào newValues không (nếu captureRequest = false hoặc bổ sung).
     */
    boolean captureResponse() default false;

    /**
     * Danh sách các tên trường nhạy cảm cần ẩn danh hóa bổ sung (ngoài các trường mặc định như password, cccd, creditCard).
     */
    String[] maskFields() default {};
}
