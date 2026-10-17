package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UtilityMeterUpdateRequest {
    @NotBlank(message = "Meter code is required")
    @Size(max = 50, message = "Meter code must not exceed 50 characters")
    private String meterCode;
    
    @Size(max = 100, message = "Location label must not exceed 100 characters")
    private String locationLabel;
}
