package vn.edu.utc.hotel_booking.modules.ai.personalization.port;

import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationStayRecord;

import java.time.OffsetDateTime;
import java.util.List;

/** Read-only boundary to the booking module's bounded U14 history query. */
public interface PersonalizationHistoryPort {

    int MAX_HISTORY_RECORDS = 100;

    List<PersonalizationStayRecord> findEligibleStays(
            Long guestId,
            Integer hotelId,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    );
}
