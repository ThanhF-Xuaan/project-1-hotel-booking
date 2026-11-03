package vn.edu.utc.hotel_booking.modules.operation.service.support;

import org.springframework.beans.BeanUtils;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class LodgingExportStateGuard {
    private LodgingExportStateGuard() { }

    /** Detached scalar patch target; association references are read, never changed in place. */
    public static StayGuest copy(StayGuest guest) {
        StayGuest copy = new StayGuest(); BeanUtils.copyProperties(guest, copy); return copy;
    }
    public static void verifyChange(LodgingQueue queue, StayGuest before, StayGuest after) {
        if (queue == null) return;
        if (queue.getStatus() == LodgingQueueStatus.PROCESSING) throw stateConflict();
        if (queue.getStatus() == LodgingQueueStatus.EXPORTED && !exportValues(before).equals(exportValues(after))) {
            throw new AppException(ErrorCode.LODGING_EXPORTED_RECORD_LOCKED);
        }
    }
    public static boolean sameMutableValues(StayGuest before, StayGuest after) {
        return exportValues(before).equals(exportValues(after))
                && Objects.equals(instant(before.getActualCheckOutTime()), instant(after.getActualCheckOutTime()))
                && Objects.equals(before.getDocumentImageUrl(), after.getDocumentImageUrl());
    }
    public static boolean preservesQueue(LodgingQueue queue) {
        return queue.getStatus() == LodgingQueueStatus.EXPORTED || queue.getStatus() == LodgingQueueStatus.CANCELLED;
    }
    public static AppException stateConflict() { return new AppException(ErrorCode.LODGING_QUEUE_STATE_CONFLICT); }
    private static Object instant(OffsetDateTime value) { return value == null ? null : value.toInstant(); }
    private static List<Object> exportValues(StayGuest guest) {
        return Arrays.asList(guest.getFullName(), guest.getDateOfBirth(), guest.getGender(), guest.getNationality(),
                guest.getDocumentType(), guest.getDocumentNumber(), guest.getPermanentAddress(), guest.getCurrentAddress(),
                instant(guest.getCheckInTime()), instant(guest.getExpectedCheckOutTime()), guest.getRoomNumber(), guest.getReasonForStay(),
                guest.getHotel() == null ? null : guest.getHotel().getId(),
                guest.getBooking() == null ? null : guest.getBooking().getId(),
                guest.getBookingRoom() == null ? null : guest.getBookingRoom().getId(),
                guest.getRoom() == null ? null : guest.getRoom().getId());
    }
}
