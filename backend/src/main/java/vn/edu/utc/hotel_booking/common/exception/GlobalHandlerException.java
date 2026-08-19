package vn.edu.utc.hotel_booking.common.exception;

import java.nio.file.AccessDeniedException;

import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import lombok.extern.slf4j.Slf4j;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;

@ControllerAdvice
@Slf4j
public class GlobalHandlerException {
    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException exception) {
        log.error("Exception: ", exception);
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

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

        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {

        }

        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

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

//    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
//    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
//        log.warn("Xảy ra đụng độ Optimistic Lock khi giữ phòng!", e);
//
//        // Lấy ErrorCode đã định nghĩa
//        ErrorCode errorCode = ErrorCode.ROOM_CONCURRENCY_CONFLICT;
//
//        // Build ApiResponse chuẩn theo format dự án của bạn
//        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
//                .code(errorCode.getCode())
//                .message(errorCode.getMessage())
//                .build();
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(apiResponse);
//    }
//
//    @ExceptionHandler({PessimisticLockException.class, CannotAcquireLockException.class})
//    public ResponseEntity<ApiResponse<Void>> handleLockTimeout(Exception e) {
//        log.warn("Lỗi tranh chấp khóa Database: {}", e.getMessage());
//        return ResponseEntity.status(HttpStatus.CONFLICT).body(
//                ApiResponse.<Void>builder()
//                        .code(ErrorCode.ROOM_ALREADY_BLOCKED.getCode())
//                        .message("Hệ thống đang quá tải hoặc phòng đang được người khác thao tác. Vui lòng thử lại sau giây lát.")
//                        .build()
//        );
//    }
}