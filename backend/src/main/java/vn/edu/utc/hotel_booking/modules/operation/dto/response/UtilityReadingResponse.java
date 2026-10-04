package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UtilityReadingResponse {
    private Long id;
    private Integer meterId;
    private String meterCode;
    private LocalDate readingDate;
    private BigDecimal readingValue;
    private Boolean isMeterReset;
    private BigDecimal usage; // Calculated from previous reading
    private Integer recordedBy;
    private Integer updatedBy;
}
