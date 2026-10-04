package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UtilityMeterUpdateRequest {
    @NotBlank(message = "Meter code is required")
    private String meterCode;
    
    private String locationLabel;
}
