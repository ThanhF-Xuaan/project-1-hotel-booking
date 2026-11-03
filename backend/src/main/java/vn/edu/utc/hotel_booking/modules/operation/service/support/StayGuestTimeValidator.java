package vn.edu.utc.hotel_booking.modules.operation.service.support;

import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;

/** Compare instants, including offset-equivalent times; legacy missing endpoints are not repaired. */
public final class StayGuestTimeValidator {
    private StayGuestTimeValidator() { }
    public static void validate(StayGuest candidate) {
        var checkIn = candidate.getCheckInTime();
        if (checkIn == null) return;
        var expected = candidate.getExpectedCheckOutTime();
        var actual = candidate.getActualCheckOutTime();
        if (expected != null && !expected.toInstant().isAfter(checkIn.toInstant())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Giờ trả phòng dự kiến phải sau giờ nhận phòng");
        }
        if (actual != null && actual.toInstant().isBefore(checkIn.toInstant())) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Giờ trả phòng thực tế không được trước giờ nhận phòng");
        }
    }
}
