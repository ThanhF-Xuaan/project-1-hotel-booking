package vn.edu.utc.hotel_booking.modules.identity.personalization.controller;

import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.identity.personalization.dto.PersonalizationConsentErrorDetail;
import vn.edu.utc.hotel_booking.modules.identity.personalization.error.PersonalizationConsentError;
import vn.edu.utc.hotel_booking.modules.identity.personalization.error.PersonalizationConsentException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PersonalizationConsentController.class)
public class PersonalizationConsentExceptionHandler {

    private static final String CACHE_CONTROL_VALUE = "private, no-store";

    @ExceptionHandler(PersonalizationConsentException.class)
    public ResponseEntity<ApiResponse<PersonalizationConsentErrorDetail>> handleConsentError(
            PersonalizationConsentException exception
    ) {
        return error(exception.getError());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<PersonalizationConsentErrorDetail>> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return error(PersonalizationConsentError.FORBIDDEN);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<PersonalizationConsentErrorDetail>> handleInvalidRequest(Exception exception) {
        return error(PersonalizationConsentError.INVALID_REQUEST);
    }

    @ExceptionHandler({DataAccessException.class, PersistenceException.class, TransactionException.class})
    public ResponseEntity<ApiResponse<PersonalizationConsentErrorDetail>> handleStorageUnavailable(
            Exception exception
    ) {
        log.warn("U14 consent storage is unavailable");
        return error(PersonalizationConsentError.DEPENDENCY_UNAVAILABLE);
    }

    private ResponseEntity<ApiResponse<PersonalizationConsentErrorDetail>> error(
            PersonalizationConsentError error
    ) {
        ApiResponse<PersonalizationConsentErrorDetail> body = ApiResponse.<PersonalizationConsentErrorDetail>builder()
                .code(error.getResponseCode())
                .message(error.getMessage())
                .result(PersonalizationConsentErrorDetail.from(error))
                .build();
        return ResponseEntity.status(error.getHttpStatus())
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .body(body);
    }
}
