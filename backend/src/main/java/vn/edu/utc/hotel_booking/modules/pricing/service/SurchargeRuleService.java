package vn.edu.utc.hotel_booking.modules.pricing.service;

import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomSurchargePolicyDto;

import java.time.LocalDate;
import java.util.List;

public interface SurchargeRuleService {

    List<RoomSurchargePolicyDto> getActiveSurchargePolicies(
            Integer hotelRoomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate
    );
}
