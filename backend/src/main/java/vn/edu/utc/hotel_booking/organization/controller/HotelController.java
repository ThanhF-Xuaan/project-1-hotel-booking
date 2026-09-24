package vn.edu.utc.hotel_booking.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.web.PageRequest;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService.HotelSummary;

@RestController
@Tag(name = "Hotels")
public class HotelController {
    private final HotelCatalogService hotels;
    private final StaffAccessService access;

    public HotelController(HotelCatalogService hotels, StaffAccessService access) {
        this.hotels = hotels;
        this.access = access;
    }

    @GetMapping("/api/v1/public/hotels")
    @Operation(summary = "List public hotels")
    public ApiResponse<PageResponse<HotelSummary>> publicHotels(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<HotelSummary>>builder().result(hotels.list(new PageRequest(page, size), null)).build();
    }

    @GetMapping("/api/v1/hotels")
    @Operation(summary = "List hotels in the current staff scope")
    public ApiResponse<PageResponse<HotelSummary>> staffHotels(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<HotelSummary>>builder()
                .result(hotels.list(new PageRequest(page, size), access.current())).build();
    }

    @GetMapping("/api/v1/public/hotels/{hotelId}")
    @Operation(summary = "Public hotel details")
    public ApiResponse<HotelSummary> hotel(@PathVariable int hotelId) {
        var found = hotels.find(hotelId);
        return ApiResponse.<HotelSummary>builder().result(new HotelSummary(found.id(), found.name(), found.address())).build();
    }
}
