package vn.edu.utc.hotel_booking.common.exception;

import org.springframework.security.access.AccessDeniedException;

import jakarta.persistence.PessimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;

@ControllerAdvice
@Slf4j
public class GlobalHandlerException {

    record DataIntegrityResult(int code, String message) {}

    @ExceptionHandler(value = org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handlingDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException exception) {
        log.error("DataIntegrityViolationException chi tiết: ", exception);
        String rootMsg = exception.getRootCause() != null ? exception.getRootCause().getMessage() : (exception.getMessage() != null ? exception.getMessage() : "");

        DataIntegrityResult result = resolveFriendlyDataIntegrityInfo(rootMsg);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.<Void>builder()
                        .code(result.code())
                        .message(result.message())
                        .build()
        );
    }

    DataIntegrityResult resolveFriendlyDataIntegrityInfo(String rootMsg) {
        if (rootMsg == null || rootMsg.isBlank()) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc toàn vẹn. Vui lòng kiểm tra lại.");
        }

        // 1. Ánh xạ theo tên Unique Constraint / Index
        if (rootMsg.contains("regions_code_key") || rootMsg.contains("uq_regions_code_active")) {
            return new DataIntegrityResult(ErrorCode.REGION_CODE_ALREADY_EXISTS.getCode(), "Mã khu vực này đã tồn tại trên hệ thống. Vui lòng chọn mã khác.");
        }
        if (rootMsg.contains("departments_code_key") || rootMsg.contains("uq_departments_code_active")) {
            return new DataIntegrityResult(ErrorCode.DEPARTMENT_CODE_ALREADY_EXISTS.getCode(), "Mã phòng ban này đã tồn tại trên hệ thống. Vui lòng chọn mã khác.");
        }
        if (rootMsg.contains("roles_code_key") || rootMsg.contains("uq_roles_code_active")) {
            return new DataIntegrityResult(ErrorCode.ROLE_CODE_ALREADY_EXISTS.getCode(), "Mã vai trò này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("staffs_username_key") || rootMsg.contains("uq_staffs_username_active")) {
            return new DataIntegrityResult(ErrorCode.USERNAME_ALREADY_EXISTS.getCode(), "Tên đăng nhập đã được sử dụng. Vui lòng chọn tên đăng nhập khác.");
        }
        if (rootMsg.contains("staffs_email_key") || rootMsg.contains("uq_staffs_email_active")) {
            return new DataIntegrityResult(ErrorCode.EMAIL_ALREADY_EXISTS.getCode(), "Địa chỉ email đã được sử dụng bởi nhân viên khác.");
        }
        if (rootMsg.contains("staffs_phone_key") || rootMsg.contains("uq_staffs_phone_active")) {
            return new DataIntegrityResult(ErrorCode.PHONE_ALREADY_EXISTS.getCode(), "Số điện thoại đã được sử dụng bởi nhân viên khác.");
        }
        if (rootMsg.contains("companies_tax_code_key") || rootMsg.contains("uq_companies_tax_code_active")) {
            return new DataIntegrityResult(ErrorCode.TAX_CODE_ALREADY_EXISTS.getCode(), "Mã số thuế doanh nghiệp này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("room_types_code_key") || rootMsg.contains("uq_room_types_code_active")) {
            return new DataIntegrityResult(ErrorCode.ROOM_TYPE_CODE_ALREADY_EXISTS.getCode(), "Mã loại phòng này đã tồn tại trên hệ thống. Vui lòng chọn mã khác.");
        }
        if (rootMsg.contains("uk_hotel_room_type") || rootMsg.contains("uq_hotel_room_types_active") || rootMsg.contains("hotel_room_types_hotel_id_room_type_id_key")) {
            return new DataIntegrityResult(ErrorCode.HOTEL_ROOM_TYPE_ALREADY_EXISTS.getCode(), "Loại phòng này đã được cấu hình cho khách sạn.");
        }
        if (rootMsg.contains("uk_room_number") || rootMsg.contains("uq_room_instances_number_active")) {
            return new DataIntegrityResult(ErrorCode.ROOM_NUMBER_ALREADY_EXISTS.getCode(), "Số phòng này đã tồn tại trong khách sạn. Vui lòng chọn số phòng khác.");
        }
        if (rootMsg.contains("room_features_code_key") || rootMsg.contains("uq_room_features_code_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Mã tiện ích phòng này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("room_beds_name_key") || rootMsg.contains("uq_room_beds_name_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Tên loại giường này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("tax_categories_category_code_key") || rootMsg.contains("uq_tax_categories_code_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Mã danh mục thuế này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("vat_rules_vat_code_key") || rootMsg.contains("uq_vat_rules_code_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Mã quy tắc thuế VAT này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("vouchers_voucher_code_key") || rootMsg.contains("uq_vouchers_code_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Mã voucher này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("master_items_code_key") || rootMsg.contains("uq_master_items_code_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Mã mặt hàng/dịch vụ này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("uq_permission_action_resource") || rootMsg.contains("uq_permissions_action_resource_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Quyền hạn với hành động và tài nguyên này đã tồn tại.");
        }
        if (rootMsg.contains("uq_holiday_name_date") || rootMsg.contains("uq_holiday_name_date_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Ngày lễ với tên và ngày này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("hotel_age_policies_hotel_id_guest_type_key") || rootMsg.contains("uk_hotel_guest_type") || rootMsg.contains("uq_hotel_age_policies_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Chính sách độ tuổi cho loại khách này đã tồn tại trong khách sạn.");
        }
        if (rootMsg.contains("chk_unique_hotel_night_audit") || rootMsg.contains("uq_night_audits_hotel_date_active")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Khách sạn đã được chốt sổ đêm cho ngày này rồi.");
        }
        if (rootMsg.contains("cancellation_policies_code_key") || rootMsg.contains("uq_cancellation_policies_code_active")) {
            return new DataIntegrityResult(ErrorCode.CANCELLATION_POLICY_CODE_EXISTS.getCode(), "Mã chính sách hủy phòng này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("no_show_policies_code_key") || rootMsg.contains("uq_no_show_policies_code_active")) {
            return new DataIntegrityResult(ErrorCode.NO_SHOW_POLICY_CODE_EXISTS.getCode(), "Mã chính sách No-Show này đã tồn tại trên hệ thống.");
        }
        if (rootMsg.contains("hotel_room_type_beds_pkey") || rootMsg.contains("uq_hotel_room_type_beds")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Loại giường này đã được thêm vào cấu hình hạng phòng. Vui lòng không chọn trùng giường.");
        }
        if (rootMsg.contains("hotel_room_type_features_pkey") || rootMsg.contains("uq_hotel_room_type_features")) {
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Tiện ích này đã được gán cho hạng phòng.");
        }

        // 2. Fallback trích xuất cặp key-value từ câu thông báo chi tiết của PostgreSQL
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("Key \\((.*?)\\)=\\((.*?)\\) already exists").matcher(rootMsg);
        if (matcher.find()) {
            String field = matcher.group(1);
            String value = matcher.group(2);
            return new DataIntegrityResult(HttpStatus.CONFLICT.value(), String.format("Giá trị '%s' của trường '%s' đã tồn tại trên hệ thống. Vui lòng kiểm tra lại.", value, field));
        }

        // 3. Fallback chung an toàn, bảo mật thông tin nội bộ
        return new DataIntegrityResult(HttpStatus.CONFLICT.value(), "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc toàn vẹn. Vui lòng kiểm tra lại thông tin nhập.");
    }

    @ExceptionHandler(value = TransactionSystemException.class)
    ResponseEntity<ApiResponse<Void>> handlingTransactionSystemException(TransactionSystemException exception) {
        log.error("TransactionSystemException chi tiết: ", exception);
        Throwable rootCause = exception.getRootCause() != null ? exception.getRootCause() : exception;

        if (rootCause instanceof ConstraintViolationException cve) {
            String validationMsg = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Void>builder()
                            .code(ErrorCode.INVALID_REQUEST_DATA.getCode())
                            .message("Dữ liệu không hợp lệ: " + validationMsg)
                            .build()
            );
        }

        String rootMsg = rootCause.getMessage() != null ? rootCause.getMessage() : "";
        DataIntegrityResult result = resolveFriendlyDataIntegrityInfo(rootMsg);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.<Void>builder()
                        .code(result.code())
                        .message(result.message())
                        .build()
        );
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handlingConstraintViolation(ConstraintViolationException exception) {
        log.error("ConstraintViolationException: ", exception);
        String validationMsg = exception.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(
                ApiResponse.<Void>builder()
                        .code(ErrorCode.INVALID_REQUEST_DATA.getCode())
                        .message("Dữ liệu không hợp lệ: " + validationMsg)
                        .build()
        );
    }

    @ExceptionHandler(value = {IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<ApiResponse<Void>> handlingIllegalArguments(RuntimeException exception) {
        log.error("IllegalArgumentException / IllegalStateException: ", exception);
        String msg = (exception.getMessage() != null && !exception.getMessage().isBlank())
                ? exception.getMessage()
                : "Tham số yêu cầu không hợp lệ";
        return ResponseEntity.badRequest().body(
                ApiResponse.<Void>builder()
                        .code(ErrorCode.INVALID_REQUEST_DATA.getCode())
                        .message(msg)
                        .build()
        );
    }

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException exception) {
        log.error("Exception chưa được phân loại: ", exception);
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        String msg = (exception.getMessage() != null && !exception.getMessage().isBlank())
                ? exception.getMessage()
                : ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage();
        apiResponse.setMessage(msg);

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode()).body(
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingValidation(MethodArgumentNotValidException exception) {
        log.error("Lỗi: ", exception);
        String enumKey = exception.getFieldError().getDefaultMessage();

        // Mặc định: mã lỗi yêu cầu dữ liệu không hợp lệ (1002)
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_DATA;
        String message = (enumKey != null) ? enumKey : errorCode.getMessage();

        try {
            // Tôn trọng DTO/message nào đó cố tình dùng TÊN enum ErrorCode làm message
            errorCode = ErrorCode.valueOf(enumKey);
            message = errorCode.getMessage();
        } catch (IllegalArgumentException e) {
            // Message tiếng Việt từ @NotNull/@DecimalMin/... → trả NGUYÊN message đó cho user
            // (hành vi cũ: fallback INVALID_KEY 1001 "Khóa mã lỗi không hợp lệ" → vô nghĩa với user)
        }

        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(message);

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        log.error("Lỗi parse JSON hoặc sai Enum: {}", exception.getMostSpecificCause().getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.<Void>builder()
                        .code(400)
                        .message("Dữ liệu đầu vào không hợp lệ hoặc giá trị không tồn tại!")
                        .build());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        log.warn("Xảy ra đụng độ Optimistic Lock khi cập nhật phòng!", e);

        ErrorCode errorCode = ErrorCode.OPTIMISTIC_LOCK_CONFLICT;

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(apiResponse);
    }

    @ExceptionHandler({PessimisticLockException.class, CannotAcquireLockException.class})
    public ResponseEntity<ApiResponse<Void>> handleLockTimeout(Exception e) {
        log.warn("Lỗi tranh chấp khóa Database: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.<Void>builder()
                        .code(ErrorCode.ROOM_ALREADY_BLOCKED.getCode())
                        .message("Hệ thống đang quá tải hoặc phòng đang được người khác thao tác. Vui lòng thử lại sau giây lát.")
                        .build()
        );
    }
}