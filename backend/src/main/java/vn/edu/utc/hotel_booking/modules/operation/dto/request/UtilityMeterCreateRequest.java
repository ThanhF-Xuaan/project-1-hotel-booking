package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.MeterType;

@Data
public class UtilityMeterCreateRequest {
    @NotNull(message = "Hotel ID is required")
    private Short hotelId;
    
    @NotBlank(message = "Meter code is required")
    @Size(max = 50, message = "Meter code must not exceed 50 characters")
    private String meterCode;
    
    @NotNull(message = "Meter type is required")
    private MeterType meterType;
    
    @Size(max = 100, message = "Location label must not exceed 100 characters")
    private String locationLabel;
}
