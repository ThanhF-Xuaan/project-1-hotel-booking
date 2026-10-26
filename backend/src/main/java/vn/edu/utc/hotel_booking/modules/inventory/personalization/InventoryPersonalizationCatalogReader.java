package vn.edu.utc.hotel_booking.modules.inventory.personalization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationCatalogSnapshot;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRoomType;
import vn.edu.utc.hotel_booking.modules.ai.personalization.port.PersonalizationCatalogPort;
import vn.edu.utc.hotel_booking.modules.inventory.repository.PersonalizationCatalogQueryRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.projection.PersonalizationCatalogRow;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryPersonalizationCatalogReader implements PersonalizationCatalogPort {

    private final PersonalizationCatalogQueryRepository repository;

    @Override
    public PersonalizationCatalogSnapshot findCurrentCatalog(Integer hotelId) {
        if (hotelId == null || hotelId < 1 || hotelId > Short.MAX_VALUE) {
            throw new IllegalArgumentException("U14 catalog scope is invalid");
        }

        List<PersonalizationCatalogRow> rows = repository.findActiveCatalog(hotelId);
        if (rows.isEmpty()) {
            return new PersonalizationCatalogSnapshot(false, List.of());
        }

        List<PersonalizationRoomType> roomTypes = rows.stream()
                .filter(row -> row.getHotelRoomTypeId() != null)
                .map(row -> new PersonalizationRoomType(
                        row.getHotelRoomTypeId(),
                        row.getRoomTypeCode(),
                        row.getRoomTypeName()
                ))
                .toList();
        return new PersonalizationCatalogSnapshot(true, roomTypes);
    }
}
