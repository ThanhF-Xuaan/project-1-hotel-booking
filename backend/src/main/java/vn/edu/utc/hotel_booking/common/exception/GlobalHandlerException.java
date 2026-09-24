package vn.edu.utc.hotel_booking.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalHandlerException {
    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> app(AppException exception) {
        return error(exception.getErrorCode());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException exception) {
        return error(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, String>>> validation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(field ->
                fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        exception.getBindingResult().getGlobalErrors().forEach(global ->
                fields.putIfAbsent(global.getObjectName(), global.getDefaultMessage()));
        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(code.getStatusCode()).body(
                ApiResponse.<Map<String, String>>builder().code(code.getCode()).message(code.getMessage()).result(fields).build());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    ResponseEntity<ApiResponse<Void>> invalidInput(Exception exception) {
        return error(ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler({CannotAcquireLockException.class, ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ApiResponse<Void>> conflict(Exception exception) {
        log.warn("Database write conflict: {}", exception.getClass().getSimpleName());
        return error(ErrorCode.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> fallback(Exception exception) {
        if (exception instanceof ErrorResponse response) {
            // MVC owns the protocol status and headers (e.g. Allow on a 405).
            // Keep the application envelope without exposing exception details.
            ErrorCode code = switch (response.getStatusCode().value()) {
                case 400 -> ErrorCode.VALIDATION_ERROR;
                case 404 -> ErrorCode.NOT_FOUND;
                case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
                case 406 -> ErrorCode.NOT_ACCEPTABLE;
                case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
                default -> response.getStatusCode().is5xxServerError()
                        ? ErrorCode.UNCATEGORIZED_EXCEPTION : ErrorCode.HTTP_REQUEST_ERROR;
            };
            if (response.getStatusCode().is5xxServerError()) {
                log.error("Framework request failure", exception);
            }
            return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(
                    ApiResponse.<Void>builder().code(code.getCode()).message(code.getMessage()).build());
        }
        log.error("Unhandled request failure", exception);
        return error(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    private ResponseEntity<ApiResponse<Void>> error(ErrorCode code) {
        return ResponseEntity.status(code.getStatusCode()).body(
                ApiResponse.<Void>builder().code(code.getCode()).message(code.getMessage()).build());
    }
}
