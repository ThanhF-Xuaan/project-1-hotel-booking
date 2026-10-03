package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UtilityReadingUpdateRequest {
    @NotNull(message = "Reading value is required")
    private BigDecimal readingValue;
    
    private Boolean isMeterReset = false;
}
