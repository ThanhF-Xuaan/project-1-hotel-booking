package vn.edu.utc.hotel_booking.inventory;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.aiassistant.tool.RoomQuoteTool;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.inventory.controller.RoomSearchController;
import vn.edu.utc.hotel_booking.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService;
import vn.edu.utc.hotel_booking.pricing.service.PricingQuoteService;
import vn.edu.utc.hotel_booking.property.service.RoomTypeCatalogService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StayEntryPointsTest {
    @Test
    void rejectsOversizedStaysBeforeCallingDatabaseBackedServices() {
        var hotels = mock(HotelCatalogService.class);
        var roomTypes = mock(RoomTypeCatalogService.class);
        var availability = mock(RoomAvailabilityService.class);
        var pricing = mock(PricingQuoteService.class);
        var access = mock(StaffAccessService.class);
        var controller = new RoomSearchController(hotels, roomTypes, availability, pricing, access);
        var tool = new RoomQuoteTool(access, hotels, roomTypes, pricing);
        var checkIn = LocalDate.of(2026, 10, 1);
        var checkOut = checkIn.plusDays(31);

        assertThrows(AppException.class,
                () -> controller.publicRoomTypes(1, checkIn, checkOut, 2, 0, 0, 0, 20));
        assertThrows(AppException.class, () -> controller.publicQuote(1, checkIn, checkOut, 2, 0, 0));
        assertThrows(AppException.class, () -> controller.staffQuote(1, 1, checkIn, checkOut, 2, 0, 0));
        assertThrows(AppException.class, () -> controller.staffAvailability(1, 1, checkIn, checkOut, 2, 0, 0));
        assertThrows(AppException.class,
                () -> tool.quote(null, new RoomQuoteTool.RoomQuery(1, 1, checkIn, checkOut, 2, 0, 0)));
        verifyNoInteractions(hotels, roomTypes, availability, pricing, access);
    }
}
