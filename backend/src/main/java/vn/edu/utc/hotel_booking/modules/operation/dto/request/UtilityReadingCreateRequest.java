package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UtilityReadingCreateRequest {
    @NotNull(message = "Meter ID is required")
    private Integer meterId;
    
    @NotNull(message = "Reading date is required")
    private LocalDate readingDate;
    
    @NotNull(message = "Reading value is required")
    @PositiveOrZero(message = "Reading value must be non-negative")
    private BigDecimal readingValue;
    
    private Boolean isMeterReset = false;
}
