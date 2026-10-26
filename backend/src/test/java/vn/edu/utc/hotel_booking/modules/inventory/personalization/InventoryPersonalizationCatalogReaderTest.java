package vn.edu.utc.hotel_booking.modules.inventory.personalization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import vn.edu.utc.hotel_booking.modules.inventory.repository.PersonalizationCatalogQueryRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.projection.PersonalizationCatalogRow;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryPersonalizationCatalogReaderTest {

    @Mock
    private PersonalizationCatalogQueryRepository repository;

    @Mock
    private PersonalizationCatalogRow roomTypeRow;

    @Mock
    private PersonalizationCatalogRow eligibleHotelWithoutRoomTypesRow;

    @InjectMocks
    private InventoryPersonalizationCatalogReader reader;

    @Test
    void mapsOnlyCurrentEligibleCatalogLabels() {
        when(repository.findActiveCatalog(1)).thenReturn(List.of(roomTypeRow));
        when(roomTypeRow.getHotelRoomTypeId()).thenReturn(101);
        when(roomTypeRow.getRoomTypeCode()).thenReturn("DELUXE");
        when(roomTypeRow.getRoomTypeName()).thenReturn("Deluxe Room");

        var snapshot = reader.findCurrentCatalog(1);

        assertThat(snapshot.hotelEligible()).isTrue();
        assertThat(snapshot.roomTypes()).containsExactly(
                new vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRoomType(
                        101, "DELUXE", "Deluxe Room"
                )
        );
    }

    @Test
    void distinguishesEligibleHotelWithNoRoomTypesFromUnknownOrInactiveHotel() {
        when(repository.findActiveCatalog(1)).thenReturn(List.of(eligibleHotelWithoutRoomTypesRow));
        when(eligibleHotelWithoutRoomTypesRow.getHotelRoomTypeId()).thenReturn(null);
        when(repository.findActiveCatalog(2)).thenReturn(List.of());

        var eligibleEmpty = reader.findCurrentCatalog(1);
        var ineligible = reader.findCurrentCatalog(2);

        assertThat(eligibleEmpty.hotelEligible()).isTrue();
        assertThat(eligibleEmpty.roomTypes()).isEmpty();
        assertThat(ineligible.hotelEligible()).isFalse();
        assertThat(ineligible.roomTypes()).isEmpty();
    }

    @Test
    void rejectsHotelIdsOutsideTheCurrentDatabaseIdRange() {
        assertThatThrownBy(() -> reader.findCurrentCatalog(32768))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotTurnCatalogRepositoryFailureIntoAnEmptyCatalog() {
        when(repository.findActiveCatalog(1))
                .thenThrow(new DataAccessResourceFailureException("synthetic database outage"));

        assertThatThrownBy(() -> reader.findCurrentCatalog(1))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("synthetic database outage");
    }
}
