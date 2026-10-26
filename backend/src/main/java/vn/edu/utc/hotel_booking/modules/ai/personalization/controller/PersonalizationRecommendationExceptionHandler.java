package vn.edu.utc.hotel_booking.modules.ai.personalization.controller;

import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRecommendationErrorDetail;
import vn.edu.utc.hotel_booking.modules.ai.personalization.error.PersonalizationRecommendationError;
import vn.edu.utc.hotel_booking.modules.ai.personalization.error.PersonalizationRecommendationException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PersonalizationRecommendationController.class)
public class PersonalizationRecommendationExceptionHandler {

    private static final String CACHE_CONTROL_VALUE = "private, no-store";

    @ExceptionHandler(PersonalizationRecommendationException.class)
    public ResponseEntity<ApiResponse<PersonalizationRecommendationErrorDetail>> handleRecommendationError(
            PersonalizationRecommendationException exception
    ) {
        return error(exception.error());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<PersonalizationRecommendationErrorDetail>> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return error(PersonalizationRecommendationError.FORBIDDEN);
    }

    @ExceptionHandler({DataAccessException.class, PersistenceException.class, TransactionException.class})
    public ResponseEntity<ApiResponse<PersonalizationRecommendationErrorDetail>> handleStorageUnavailable(
            Exception exception
    ) {
        log.warn("U14 recommendation dependencies are unavailable");
        return error(PersonalizationRecommendationError.DEPENDENCY_UNAVAILABLE);
    }

    private ResponseEntity<ApiResponse<PersonalizationRecommendationErrorDetail>> error(
            PersonalizationRecommendationError error
    ) {
        ApiResponse<PersonalizationRecommendationErrorDetail> body = ApiResponse
                .<PersonalizationRecommendationErrorDetail>builder()
                .code(error.responseCode())
                .message(error.message())
                .result(PersonalizationRecommendationErrorDetail.from(error))
                .build();
        return ResponseEntity.status(error.httpStatus())
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .body(body);
    }
}
