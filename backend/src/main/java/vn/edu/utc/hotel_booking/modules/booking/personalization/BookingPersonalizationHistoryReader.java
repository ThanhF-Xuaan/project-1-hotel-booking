package vn.edu.utc.hotel_booking.modules.booking.personalization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationStayRecord;
import vn.edu.utc.hotel_booking.modules.ai.personalization.port.PersonalizationHistoryPort;
import vn.edu.utc.hotel_booking.modules.booking.repository.PersonalizationHistoryQueryRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.projection.PersonalizationHistoryRow;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingPersonalizationHistoryReader implements PersonalizationHistoryPort {

    private final PersonalizationHistoryQueryRepository repository;

    @Override
    public List<PersonalizationStayRecord> findEligibleStays(
            Long guestId,
            Integer hotelId,
            OffsetDateTime fromInclusive,
            OffsetDateTime toInclusive
    ) {
        if (guestId == null || guestId < 1 || hotelId == null || hotelId < 1 || hotelId > Short.MAX_VALUE) {
            throw new IllegalArgumentException("U14 history scope is invalid");
        }
        if (fromInclusive == null || toInclusive == null || fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("U14 history window is invalid");
        }

        List<PersonalizationHistoryRow> rows = repository.findEligibleStays(
                guestId, hotelId, fromInclusive, toInclusive
        );
        return rows.stream()
                .map(row -> new PersonalizationStayRecord(
                        row.getBookingRoomId(),
                        row.getHotelId(),
                        row.getHotelRoomTypeId(),
                        row.getActualCheckInAt().atOffset(ZoneOffset.UTC),
                        row.getActualCheckOutAt().atOffset(ZoneOffset.UTC)
                ))
                .toList();
    }
}
