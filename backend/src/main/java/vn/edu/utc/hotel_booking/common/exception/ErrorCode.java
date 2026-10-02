package vn.edu.utc.hotel_booking.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi chưa được phân loại", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Khóa mã lỗi không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_DATA(1002, "Dữ liệu yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Chưa xác thực (Vui lòng đăng nhập)", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),

    // Organization Module Errors
    REGION_NOT_FOUND(2001, "Không tìm thấy khu vực/vùng yêu cầu", HttpStatus.NOT_FOUND),
    REGION_CODE_ALREADY_EXISTS(2002, "Mã khu vực đã tồn tại trên hệ thống", HttpStatus.CONFLICT),
    DEPARTMENT_NOT_FOUND(2011, "Không tìm thấy phòng ban yêu cầu", HttpStatus.NOT_FOUND),
    DEPARTMENT_CODE_ALREADY_EXISTS(2012, "Mã phòng ban đã tồn tại trên hệ thống", HttpStatus.CONFLICT),
    HOTEL_NOT_FOUND(2021, "Không tìm thấy khách sạn cơ sở", HttpStatus.NOT_FOUND),
    HOTEL_NAME_ALREADY_EXISTS(2022, "Tên khách sạn đã tồn tại trong khu vực này", HttpStatus.CONFLICT),
    HOTEL_HAS_ACTIVE_ROOMS(2023, "Khách sạn đang có phòng hoạt động, không thể xóa", HttpStatus.CONFLICT),

    // Identity & IAM Module Errors
    ROLE_NOT_FOUND(3001, "Không tìm thấy vai trò (Role)", HttpStatus.NOT_FOUND),
    ROLE_CODE_ALREADY_EXISTS(3002, "Mã vai trò đã tồn tại", HttpStatus.CONFLICT),
    PERMISSION_NOT_FOUND(3011, "Không tìm thấy quyền hạn (Permission)", HttpStatus.NOT_FOUND),
    STAFF_NOT_FOUND(3021, "Không tìm thấy thông tin nhân viên", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS(3022, "Tên tài khoản đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(3023, "Địa chỉ email đã được sử dụng", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(3024, "Số điện thoại đã được sử dụng", HttpStatus.CONFLICT),
    INVALID_SCOPE_CONFIGURATION(3025, "Cấu hình phạm vi quản lý (Scope) của nhân viên không hợp lệ", HttpStatus.BAD_REQUEST),
    KEYCLOAK_USER_CREATION_FAILED(3026, "Không thể tạo tài khoản trên Keycloak IAM", HttpStatus.BAD_GATEWAY),
    KEYCLOAK_USER_UPDATE_FAILED(3027, "Không thể cập nhật thông tin tài khoản trên Keycloak IAM", HttpStatus.BAD_GATEWAY),
    KEYCLOAK_USER_DELETE_FAILED(3028, "Không thể xóa hoặc vô hiệu hóa tài khoản trên Keycloak IAM", HttpStatus.BAD_GATEWAY),
    KEYCLOAK_COMMUNICATION_ERROR(3029, "Không thể kết nối tới máy chủ Keycloak IAM", HttpStatus.SERVICE_UNAVAILABLE),
    KEYCLOAK_USER_ALREADY_EXISTS(3030, "Tài khoản người dùng đã tồn tại trên Keycloak IAM", HttpStatus.CONFLICT),
    GUEST_NOT_FOUND(3031, "Không tìm thấy thông tin khách hàng", HttpStatus.NOT_FOUND),
    COMPANY_NOT_FOUND(3041, "Không tìm thấy thông tin doanh nghiệp đối tác", HttpStatus.NOT_FOUND),
    TAX_CODE_ALREADY_EXISTS(3042, "Mã số thuế doanh nghiệp đã tồn tại", HttpStatus.CONFLICT),

    // Concurrency & Locking
    OPTIMISTIC_LOCK_CONFLICT(4091, "Dữ liệu vừa được cập nhật bởi một phiên làm việc khác. Vui lòng thử lại.", HttpStatus.CONFLICT),
    PESSIMISTIC_LOCK_CONFLICT(4092, "Hệ thống đang xử lý giao dịch tại tài nguyên này. Vui lòng thử lại sau.", HttpStatus.CONFLICT),
    RESOURCE_IN_USE(4093, "Bản ghi đang được tham chiếu bởi dữ liệu khác, không thể xóa", HttpStatus.CONFLICT),

    // Inventory Module Errors
    ROOM_TYPE_NOT_FOUND(5001, "Không tìm thấy loại phòng", HttpStatus.NOT_FOUND),
    ROOM_TYPE_CODE_ALREADY_EXISTS(5002, "Mã loại phòng đã tồn tại", HttpStatus.CONFLICT),
    HOTEL_ROOM_TYPE_NOT_FOUND(5011, "Không tìm thấy cấu hình loại phòng khách sạn", HttpStatus.NOT_FOUND),
    HOTEL_ROOM_TYPE_ALREADY_EXISTS(5012, "Loại phòng này đã được cấu hình cho khách sạn", HttpStatus.CONFLICT),
    ROOM_INSTANCE_NOT_FOUND(5021, "Không tìm thấy phòng vật lý", HttpStatus.NOT_FOUND),
    ROOM_NUMBER_ALREADY_EXISTS(5022, "Số phòng đã tồn tại trong khách sạn", HttpStatus.CONFLICT),
    ROOM_NOT_AVAILABLE(5031, "Không đủ phòng trống trong khoảng thời gian yêu cầu", HttpStatus.CONFLICT),

    // Pricing Module Errors
    PRICING_RULE_NOT_FOUND(6001, "Không tìm thấy quy tắc giá", HttpStatus.NOT_FOUND),
    CAMPAIGN_NOT_FOUND(6011, "Không tìm thấy chiến dịch khuyến mại", HttpStatus.NOT_FOUND),
    TAX_CATEGORY_NOT_FOUND(6021, "Không tìm thấy nhóm thuế", HttpStatus.NOT_FOUND),
    VAT_RULE_NOT_FOUND(6031, "Không tìm thấy quy tắc thuế VAT", HttpStatus.NOT_FOUND),

    // Booking Module Errors (7000s)
    BOOKING_NOT_FOUND(7001, "Không tìm thấy thông tin đơn đặt phòng", HttpStatus.NOT_FOUND),
    BOOKING_NUMBER_ALREADY_EXISTS(7002, "Mã đặt phòng đã tồn tại", HttpStatus.CONFLICT),
    INVALID_BOOKING_DATES(7003, "Ngày nhận phòng phải trước ngày trả phòng", HttpStatus.BAD_REQUEST),
    INVALID_BOOKING_STATUS(7004, "Trạng thái đơn đặt phòng không hợp lệ cho thao tác này", HttpStatus.BAD_REQUEST),
    BOOKING_DETAIL_NOT_FOUND(7011, "Không tìm thấy chi tiết đặt phòng", HttpStatus.NOT_FOUND),
    BOOKING_ROOM_NOT_FOUND(7021, "Không tìm thấy thông tin phòng đặt", HttpStatus.NOT_FOUND),
    ROOM_ALREADY_ASSIGNED(7022, "Phòng vật lý này đã được xếp trong khoảng thời gian trên", HttpStatus.CONFLICT),
    INSUFFICIENT_ROOM_AVAILABILITY(7031, "Không đủ phòng trống để thực hiện đặt phòng", HttpStatus.CONFLICT),

    // Finance & Payment Module Errors (8000s)
    PAYMENT_NOT_FOUND(8001, "Không tìm thấy thông tin thanh toán", HttpStatus.NOT_FOUND),
    INVALID_PAYMENT_AMOUNT(8002, "Số tiền thanh toán không hợp lệ", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_COMPLETED(8003, "Thanh toán đã được hoàn tất trước đó", HttpStatus.BAD_REQUEST),
    INVOICE_NOT_FOUND(8011, "Không tìm thấy hóa đơn", HttpStatus.NOT_FOUND),
    INVOICE_ALREADY_ISSUED(8012, "Hóa đơn đã được phát hành, không thể chỉnh sửa", HttpStatus.BAD_REQUEST),
    TRANSACTION_FAILED(8021, "Giao dịch thanh toán thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    // Operation & Housekeeping & POS Module Errors (9000s)
    MENU_ITEM_NOT_FOUND(9001, "Không tìm thấy món ăn / dịch vụ trong thực đơn", HttpStatus.NOT_FOUND),
    OUT_OF_STOCK(9002, "Mặt hàng này đã hết tồn kho", HttpStatus.CONFLICT),
    SERVICE_ORDER_NOT_FOUND(9011, "Không tìm thấy đơn dịch vụ", HttpStatus.NOT_FOUND),
    INVALID_SERVICE_ORDER_STATUS(9012, "Trạng thái đơn dịch vụ không hợp lệ cho thao tác này", HttpStatus.BAD_REQUEST),
    ROOM_NOT_OCCUPIED(9021, "Phòng này hiện chưa có khách lưu trú để gọi dịch vụ", HttpStatus.BAD_REQUEST),
    NIGHT_AUDIT_ALREADY_EXECUTED(9031, "Quy trình đối soát đêm ngày này đã được thực hiện", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}
