package vn.edu.utc.hotel_booking.common.web;

import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;

public record PageRequest(int page, int size) {
    public PageRequest {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
    }

    public int offset() {
        long value = (long) page * size;
        if (value > Integer.MAX_VALUE) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
        return (int) value;
    }
}
