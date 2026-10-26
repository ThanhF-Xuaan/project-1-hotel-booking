package vn.edu.utc.hotel_booking.modules.ai.personalization;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRoomType;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationStayRecord;
import vn.edu.utc.hotel_booking.modules.ai.personalization.service.RoomAffinityCore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomAffinityCoreTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 10, 26);
    private static final Instant AS_OF = Instant.parse("2026-10-26T10:00:00Z");
    private final RoomAffinityCore core = new RoomAffinityCore();

    @Test
    void ranksOnlyCurrentCatalogTypesAndNormalizesTheTwoToOneFixture() {
        List<PersonalizationStayRecord> history = List.of(
                stay(1L, 1, 101, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00"),
                stay(2L, 1, 101, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00"),
                stay(3L, 1, 102, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00")
        );

        var ranked = core.rank(history, catalog(101, 102), 1, REFERENCE_DATE, AS_OF, 3);

        assertThat(ranked).extracting(item -> item.hotelRoomTypeId()).containsExactly(101, 102);
        assertThat(ranked).extracting(item -> item.affinityScore()).containsExactly(
                new java.math.BigDecimal("1.0000"), new java.math.BigDecimal("0.5000"));
        assertThat(ranked).allSatisfy(item -> assertThat(item.reasonCode())
                .isEqualTo("HISTORICAL_BOOKING_AFFINITY"));
    }

    @Test
    void removesHiddenTypesBeforeNormalizationAndAppliesSuggestionLimitAfterRanking() {
        List<PersonalizationStayRecord> history = List.of(
                stay(1L, 1, 101, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00"),
                stay(2L, 1, 101, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00"),
                stay(3L, 1, 102, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00")
        );

        var ranked = core.rank(history, catalog(102), 1, REFERENCE_DATE, AS_OF, 1);

        assertThat(ranked).hasSize(1);
        assertThat(ranked.getFirst().hotelRoomTypeId()).isEqualTo(102);
        assertThat(ranked.getFirst().affinityScore()).isEqualByComparingTo("1.0000");
    }

    @Test
    void deduplicatesBookingRoomsAndExcludesWrongHotelInvalidOrFutureHistory() {
        List<PersonalizationStayRecord> history = List.of(
                stay(1L, 1, 101, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00"),
                stay(1L, 1, 101, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00"),
                stay(2L, 2, 101, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00"),
                stay(3L, 1, 101, "2026-10-01T20:00:00+07:00", "2026-10-01T23:00:00+07:00"),
                stay(4L, 1, 101, "2026-10-25T00:00:00+07:00", "2026-10-27T12:00:00+07:00")
        );

        var ranked = core.rank(history, catalog(101), 1, REFERENCE_DATE, AS_OF, 3);

        assertThat(ranked).hasSize(1);
        assertThat(ranked.getFirst().affinityScore()).isEqualByComparingTo("1.0000");
    }

    @Test
    void breaksEqualScoresByCountThenLatestCheckInThenRoomTypeId() {
        List<PersonalizationStayRecord> history = List.of(
                stay(1L, 1, 102, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00"),
                stay(2L, 1, 101, "2026-10-01T16:00:00+07:00", "2026-10-03T12:00:00+07:00")
        );

        var ranked = core.rank(history, catalog(102, 101), 1, REFERENCE_DATE, AS_OF, 3);

        assertThat(ranked).extracting(item -> item.hotelRoomTypeId()).containsExactly(101, 102);
    }

    private PersonalizationStayRecord stay(
            long bookingRoomId,
            int hotelId,
            int roomTypeId,
            String checkIn,
            String checkOut
    ) {
        return new PersonalizationStayRecord(
                bookingRoomId,
                hotelId,
                roomTypeId,
                OffsetDateTime.parse(checkIn),
                OffsetDateTime.parse(checkOut)
        );
    }

    private List<PersonalizationRoomType> catalog(Integer... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> new PersonalizationRoomType(id, "TYPE-" + id, "Room " + id))
                .toList();
    }
}
