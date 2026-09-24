package vn.edu.utc.hotel_booking.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.web.PageRequest;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.inventory.api.StayCriteria;
import vn.edu.utc.hotel_booking.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService;
import vn.edu.utc.hotel_booking.pricing.service.PricingQuoteService;
import vn.edu.utc.hotel_booking.property.service.RoomTypeCatalogService;

import java.time.LocalDate;

@RestController
@Tag(name = "Room search and quotes")
public class RoomSearchController {
    private final HotelCatalogService hotels;
    private final RoomTypeCatalogService roomTypes;
    private final RoomAvailabilityService availability;
    private final PricingQuoteService pricing;
    private final StaffAccessService access;

    public RoomSearchController(HotelCatalogService hotels, RoomTypeCatalogService roomTypes,
                                RoomAvailabilityService availability, PricingQuoteService pricing,
                                StaffAccessService access) {
        this.hotels = hotels;
        this.roomTypes = roomTypes;
        this.availability = availability;
        this.pricing = pricing;
        this.access = access;
    }

    @GetMapping("/api/v1/public/hotels/{hotelId}/room-types")
    @Operation(summary = "Search room types with available room count and current quote",
            description = StayCriteria.API_DESCRIPTION)
    public ApiResponse<PageResponse<RoomOption>> publicRoomTypes(@PathVariable int hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int adults, @RequestParam(defaultValue = "0") int children,
            @RequestParam(defaultValue = "0") int infants,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var stay = new StayCriteria(checkIn, checkOut, adults, children, infants);
        hotels.find(hotelId);
        var types = roomTypes.list(hotelId, new PageRequest(page, size), adults, children, infants);
        var options = types.items().stream()
                .map(type -> {
                    var quote = pricing.quote(type.id(), stay);
                    return new RoomOption(type.id(), type.name(), type.maxAdults(), type.maxChildren(),
                            type.maxInfants(), quote.availableRooms(), quote.totalAmount(), quote.currency());
                }).toList();
        return ApiResponse.<PageResponse<RoomOption>>builder()
                .result(PageResponse.of(options, types.page(), types.size(), types.totalElements())).build();
    }

    @GetMapping("/api/v1/public/room-types/{hotelRoomTypeId}/quote")
    @Operation(summary = "Current, non-reserved room price breakdown", description = StayCriteria.API_DESCRIPTION)
    public ApiResponse<PricingQuoteService.Quote> publicQuote(@PathVariable int hotelRoomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int adults, @RequestParam(defaultValue = "0") int children,
            @RequestParam(defaultValue = "0") int infants) {
        return ApiResponse.<PricingQuoteService.Quote>builder().result(pricing.quote(hotelRoomTypeId,
                new StayCriteria(checkIn, checkOut, adults, children, infants))).build();
    }

    @GetMapping("/api/v1/hotels/{hotelId}/room-types/{hotelRoomTypeId}/availability")
    @Operation(summary = "Availability in current hotel scope", description = StayCriteria.API_DESCRIPTION)
    public ApiResponse<RoomAvailabilityService.Availability> staffAvailability(@PathVariable int hotelId,
            @PathVariable int hotelRoomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int adults, @RequestParam(defaultValue = "0") int children,
            @RequestParam(defaultValue = "0") int infants) {
        var stay = new StayCriteria(checkIn, checkOut, adults, children, infants);
        authorizeType(hotelId, hotelRoomTypeId);
        return ApiResponse.<RoomAvailabilityService.Availability>builder().result(availability.availability(
                hotelRoomTypeId, stay)).build();
    }

    @GetMapping("/api/v1/hotels/{hotelId}/room-types/{hotelRoomTypeId}/quote")
    @Operation(summary = "Quote in current hotel scope", description = StayCriteria.API_DESCRIPTION)
    public ApiResponse<PricingQuoteService.Quote> staffQuote(@PathVariable int hotelId,
            @PathVariable int hotelRoomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int adults, @RequestParam(defaultValue = "0") int children,
            @RequestParam(defaultValue = "0") int infants) {
        var stay = new StayCriteria(checkIn, checkOut, adults, children, infants);
        authorizeType(hotelId, hotelRoomTypeId);
        return ApiResponse.<PricingQuoteService.Quote>builder().result(pricing.quote(hotelRoomTypeId, stay)).build();
    }

    private void authorizeType(int hotelId, int hotelRoomTypeId) {
        var hotel = hotels.find(hotelId);
        access.requireHotel(access.current(), "VIEW:INVENTORY", hotel.id(), hotel.regionId());
        if (roomTypes.find(hotelRoomTypeId).hotelId() != hotelId) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
    }

    public record RoomOption(int id, String name, int maxAdults, int maxChildren, int maxInfants,
                             int availableRooms, String quotedTotal, String currency) {}
}
