package vn.edu.utc.hotel_booking.aiassistant.tool;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;
import vn.edu.utc.hotel_booking.inventory.api.StayCriteria;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService;
import vn.edu.utc.hotel_booking.pricing.service.PricingQuoteService;
import vn.edu.utc.hotel_booking.property.service.RoomTypeCatalogService;

import java.time.LocalDate;

@Component
public class RoomQuoteTool {
    private final StaffAccessService access;
    private final HotelCatalogService hotels;
    private final RoomTypeCatalogService roomTypes;
    private final PricingQuoteService pricing;

    public RoomQuoteTool(StaffAccessService access, HotelCatalogService hotels,
                         RoomTypeCatalogService roomTypes, PricingQuoteService pricing) {
        this.access = access;
        this.hotels = hotels;
        this.roomTypes = roomTypes;
        this.pricing = pricing;
    }

    public PricingQuoteService.Quote quote(StaffActor actor, RoomQuery query) {
        var stay = new StayCriteria(query.checkIn(), query.checkOut(),
                query.adults(), query.children(), query.infants());
        var hotel = hotels.find(query.hotelId());
        access.requireHotel(actor, "VIEW:INVENTORY", hotel.id(), hotel.regionId());
        if (roomTypes.find(query.hotelRoomTypeId()).hotelId() != hotel.id()) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        return pricing.quote(query.hotelRoomTypeId(), stay);
    }

    @Schema(description = StayCriteria.API_DESCRIPTION)
    public record RoomQuery(@Positive int hotelId, @Positive int hotelRoomTypeId,
                            @NotNull LocalDate checkIn, @NotNull LocalDate checkOut,
                            @Min(1) int adults, @PositiveOrZero int children,
                            @PositiveOrZero int infants) {}
}
