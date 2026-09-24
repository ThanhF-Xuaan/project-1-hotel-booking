package vn.edu.utc.hotel_booking.inventory.api;

import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.property.service.RoomTypeCatalogService.RoomTypeInfo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record StayCriteria(LocalDate checkIn, LocalDate checkOut, int adults, int children, int infants) {
    public static final int MAX_NIGHTS = 30;
    public static final String API_DESCRIPTION = "Stay dates use [checkIn, checkOut); 1 to "
            + MAX_NIGHTS + " nights. Longer stays return HTTP 400.";

    public StayCriteria {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)
                || ChronoUnit.DAYS.between(checkIn, checkOut) > MAX_NIGHTS
                || adults < 1 || children < 0 || infants < 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
    }

    public void validateCapacity(RoomTypeInfo type) {
        if (adults > type.maxAdults() || children > type.maxChildren() || infants > type.maxInfants()
                || adults + children + infants > type.maxTotalGuests()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
