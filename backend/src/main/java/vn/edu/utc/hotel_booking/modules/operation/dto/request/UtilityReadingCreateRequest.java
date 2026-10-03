package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.NotNull;
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
    private BigDecimal readingValue;
    
    private Boolean isMeterReset = false;
}
