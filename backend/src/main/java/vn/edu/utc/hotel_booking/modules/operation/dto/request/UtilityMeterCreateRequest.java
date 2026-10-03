package vn.edu.utc.hotel_booking.modules.operation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.MeterType;

@Data
public class UtilityMeterCreateRequest {
    @NotNull(message = "Hotel ID is required")
    private Short hotelId;
    
    @NotBlank(message = "Meter code is required")
    private String meterCode;
    
    @NotNull(message = "Meter type is required")
    private MeterType meterType;
    
    private String locationLabel;
}
