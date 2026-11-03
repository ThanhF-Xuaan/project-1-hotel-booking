package vn.edu.utc.hotel_booking.modules.operation.service.support;

import vn.edu.utc.hotel_booking.common.exception.*;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import java.util.*;

/** Keys are computed by the same PostgreSQL function used by the enforcing trigger. */
public final class ActiveStayDedup {
    private ActiveStayDedup() { }
    public static String normalizeToken(String value) {
        if(value==null) return null;
        int start=0,end=value.length();
        while(start<end && asciiWhitespace(value.charAt(start))) start++;
        while(end>start && asciiWhitespace(value.charAt(end-1))) end--;
        return value.substring(start,end);
    }
    private static boolean asciiWhitespace(char value) {return value==' ' || value>='\t' && value<='\r';}
    public static byte[] key(StayGuestRepository repository, StayGuest guest) {
        try {
            return repository.computeActiveKey(guest.getHotel()==null?null:guest.getHotel().getId(),
                    guest.getBooking()==null?null:guest.getBooking().getId(),
                    guest.getBookingRoom()==null?null:guest.getBookingRoom().getId(),
                    guest.getRoom()==null?null:guest.getRoom().getId(),guest.getRoomNumber(),guest.getDocumentNumber(),
                    guest.getCheckInTime(),guest.getActualCheckOutTime(),guest.getIsDeleted());
        } catch(RuntimeException error) { throw translate(error); }
    }
    public static void lock(StayGuestRepository repository, Collection<byte[]> keys) {
        var sorted=new TreeMap<String,byte[]>();
        for(byte[] key:keys) if(key!=null) sorted.put(HexFormat.of().formatHex(key),key.clone());
        try {
            // Bound subsequent queue/guest locks even for unknown or pre-check-in identities.
            repository.configureDedupLockTimeout();
            for(byte[] key:sorted.values()) repository.lockActiveKey(key);
        } catch(RuntimeException error) { throw translate(error); }
    }
    public static void verifyUnchanged(byte[] before, byte[] after) {
        if(!Arrays.equals(before,after)) throw new AppException(ErrorCode.STAY_GUEST_CONFLICT);
    }
    public static RuntimeException translate(RuntimeException error) {
        for(Throwable cause=error;cause!=null;cause=cause.getCause()) {
            if(cause instanceof java.sql.SQLException sql) {
                if(Set.of("55P03","40P01","40001","23505").contains(sql.getSQLState())) return new AppException(ErrorCode.STAY_GUEST_CONFLICT);
                if("23514".equals(sql.getSQLState()) && sql.getMessage()!=null && sql.getMessage().contains("M1_ACTIVE_STAY_INVALID_LINK"))
                    return new AppException(ErrorCode.INVALID_REQUEST_DATA);
            }
            if(cause instanceof org.springframework.dao.PessimisticLockingFailureException
                    || cause instanceof jakarta.persistence.PessimisticLockException || cause instanceof jakarta.persistence.LockTimeoutException)
                return new AppException(ErrorCode.STAY_GUEST_CONFLICT);
        }
        return error;
    }
}
