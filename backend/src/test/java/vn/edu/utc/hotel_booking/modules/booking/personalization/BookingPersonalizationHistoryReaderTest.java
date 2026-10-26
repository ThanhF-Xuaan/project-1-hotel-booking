package vn.edu.utc.hotel_booking.modules.booking.personalization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import vn.edu.utc.hotel_booking.modules.booking.repository.PersonalizationHistoryQueryRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.projection.PersonalizationHistoryRow;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPersonalizationHistoryReaderTest {

    @Mock
    private PersonalizationHistoryQueryRepository repository;

    @Mock
    private PersonalizationHistoryRow row;

    @InjectMocks
    private BookingPersonalizationHistoryReader reader;

    @Test
    void mapsOnlyTheMinimalStayProjectionAndPreservesTheRequestedScope() {
        OffsetDateTime from = OffsetDateTime.parse("2025-10-25T17:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-10-26T10:00:00Z");
        OffsetDateTime checkIn = OffsetDateTime.parse("2026-10-01T07:00:00+07:00");
        OffsetDateTime checkOut = OffsetDateTime.parse("2026-10-03T07:00:00+07:00");
        when(repository.findEligibleStays(8L, 1, from, to)).thenReturn(List.of(row));
        when(row.getBookingRoomId()).thenReturn(41L);
        when(row.getHotelId()).thenReturn(1);
        when(row.getHotelRoomTypeId()).thenReturn(101);
        when(row.getActualCheckInAt()).thenReturn(checkIn.toInstant());
        when(row.getActualCheckOutAt()).thenReturn(checkOut.toInstant());

        var stays = reader.findEligibleStays(8L, 1, from, to);

        assertThat(stays).hasSize(1);
        assertThat(stays.getFirst().bookingRoomId()).isEqualTo(41L);
        assertThat(stays.getFirst().hotelId()).isEqualTo(1);
        assertThat(stays.getFirst().hotelRoomTypeId()).isEqualTo(101);
        assertThat(stays.getFirst().actualCheckInAt().toInstant()).isEqualTo(checkIn.toInstant());
        assertThat(stays.getFirst().actualCheckOutAt().toInstant()).isEqualTo(checkOut.toInstant());
        verify(repository).findEligibleStays(8L, 1, from, to);
    }

    @Test
    void rejectsInvalidGuestOrHotelScopeBeforeReading() {
        OffsetDateTime from = OffsetDateTime.parse("2025-10-25T17:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-10-26T10:00:00Z");

        assertThatThrownBy(() -> reader.findEligibleStays(null, 1, from, to))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reader.findEligibleStays(8L, 32768, from, to))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnInvertedHistoryWindowBeforeReading() {
        OffsetDateTime from = OffsetDateTime.parse("2026-10-26T10:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2025-10-25T17:00:00Z");

        assertThatThrownBy(() -> reader.findEligibleStays(8L, 1, from, to))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keepsAnEmptyHistoryDistinctFromARepositoryFailure() {
        OffsetDateTime from = OffsetDateTime.parse("2025-10-25T17:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-10-26T10:00:00Z");
        when(repository.findEligibleStays(8L, 1, from, to)).thenReturn(List.of());

        assertThat(reader.findEligibleStays(8L, 1, from, to)).isEmpty();

        when(repository.findEligibleStays(9L, 1, from, to))
                .thenThrow(new DataAccessResourceFailureException("synthetic database outage"));
        assertThatThrownBy(() -> reader.findEligibleStays(9L, 1, from, to))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("synthetic database outage");
    }
}
