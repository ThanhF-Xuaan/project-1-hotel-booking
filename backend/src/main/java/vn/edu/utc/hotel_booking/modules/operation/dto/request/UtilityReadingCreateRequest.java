package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UtilityReadingCreateRequest {
    @NotNull(message = "Meter ID is required")
    private Integer meterId;
    
    @NotNull(message = "Reading date is required")
    @PastOrPresent(message = "Reading date cannot be in the future")
    private LocalDate readingDate;
    
    @NotNull(message = "Reading value is required")
    @PositiveOrZero(message = "Reading value must be non-negative")
    @Digits(integer = 9, fraction = 3, message = "Reading value must have at most 9 integer digits and 3 decimal places")
    private BigDecimal readingValue;
    
    private Boolean isMeterReset = false;
}
