package vn.edu.utc.hotel_booking.inventory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.inventory.api.StayCriteria;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StayCriteriaTest {
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 10, 1);

    @ParameterizedTest
    @ValueSource(ints = {1, 30})
    void acceptsBoundaryStayLengths(int nights) {
        assertDoesNotThrow(() -> new StayCriteria(CHECK_IN, CHECK_IN.plusDays(nights), 2, 0, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 31, 36524})
    void rejectsInvalidStayLengths(int nights) {
        var error = assertThrows(AppException.class,
                () -> new StayCriteria(CHECK_IN, CHECK_IN.plusDays(nights), 2, 0, 0));
        assertEquals(ErrorCode.VALIDATION_ERROR, error.getErrorCode());
    }
}
