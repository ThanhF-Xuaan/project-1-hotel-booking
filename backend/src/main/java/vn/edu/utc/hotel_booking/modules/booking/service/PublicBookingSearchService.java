package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingSearchRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.AvailableHotelResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.AvailableRoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.DestinationSuggestionResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.UnifiedBookingSearchResponse;

import vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CartPriceQuoteResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomPriceQuoteResponse;

import java.util.List;

public interface PublicBookingSearchService {

    List<DestinationSuggestionResponse> getDestinationSuggestions(String keyword);

    UnifiedBookingSearchResponse searchUnified(BookingSearchRequest request);

    PageResponse<AvailableHotelResponse> searchHotels(BookingSearchRequest request);

    List<AvailableRoomTypeResponse> searchRooms(BookingSearchRequest request);

    RoomPriceQuoteResponse calculateRoomQuote(RoomPriceQuoteRequest request);

    CartPriceQuoteResponse calculateCartQuote(CartPriceQuoteRequest request);
}

