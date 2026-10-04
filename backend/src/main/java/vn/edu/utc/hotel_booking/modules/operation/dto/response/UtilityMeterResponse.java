package vn.edu.utc.hotel_booking.modules.operation.dto.response;

import lombok.Data;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.MeterType;

@Data
public class UtilityMeterResponse {
    private Integer id;
    private Short hotelId;
    private String meterCode;
    private MeterType meterType;
    private String locationLabel;
    private Boolean isDeleted;
}
