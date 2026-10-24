package vn.edu.utc.hotel_booking.modules.ai.advisor.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AmbiguityReason;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCoverage;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CoverageReason;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ServiceFeeStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.RoomCandidate;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingSearchRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.DestinationType;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.*;
import vn.edu.utc.hotel_booking.modules.booking.service.PublicBookingSearchService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicBookingCoreAdapterTest {

    @Mock
    private PublicBookingSearchService publicBookingSearchService;

    private PublicBookingCoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PublicBookingCoreAdapter(publicBookingSearchService);
    }

    @Test
    @DisplayName("Should resolve single hotel destination")
    void shouldResolveSingleHotelDestination() {
        DestinationSuggestionResponse suggestion = DestinationSuggestionResponse.builder()
                .id(10)
                .type(DestinationType.HOTEL)
                .title("Khách sạn Grand Hà Nội")
                .build();

        when(publicBookingSearchService.getDestinationSuggestions("Grand Hà Nội"))
                .thenReturn(List.of(suggestion));

        DestinationResolutionResult result = adapter.resolveDestination("Grand Hà Nội", null, null);

        assertThat(result.isAmbiguous()).isFalse();
        assertThat(result.getHotelId()).isEqualTo(10);
        assertThat(result.getResolvedName()).isEqualTo("Khách sạn Grand Hà Nội");
    }

    @Test
    @DisplayName("Should flag MULTIPLE_LOCATIONS ambiguity when multiple distinct destinations match")
    void shouldFlagMultipleLocationsAmbiguity() {
        DestinationSuggestionResponse s1 = DestinationSuggestionResponse.builder()
                .id(1)
                .type(DestinationType.HOTEL)
                .title("Khách sạn Biển 1")
                .build();
        DestinationSuggestionResponse s2 = DestinationSuggestionResponse.builder()
                .id(2)
                .type(DestinationType.HOTEL)
                .title("Khách sạn Biển 2")
                .build();

        when(publicBookingSearchService.getDestinationSuggestions("Biển"))
                .thenReturn(List.of(s1, s2));

        DestinationResolutionResult result = adapter.resolveDestination("Biển", null, null);

        assertThat(result.isAmbiguous()).isTrue();
        assertThat(result.getAmbiguities()).hasSize(1);
        assertThat(result.getAmbiguities().get(0).getReason()).isEqualTo(AmbiguityReason.MULTIPLE_LOCATIONS.name());
    }

    @Test
    @DisplayName("Should discover and map room candidates and pricing quotes")
    void shouldDiscoverAndQuoteCandidates() {
        PriceBreakdownDto pb = PriceBreakdownDto.builder()
                .hotelRoomTypeId(101)
                .totalNights(2)
                .totalRoomNetAmount(new BigDecimal("1000000"))
                .vatPercent(new BigDecimal("10"))
                .totalRoomVat(new BigDecimal("105000"))
                .totalRoomServiceFee(new BigDecimal("50000"))
                .finalTotalAmount(new BigDecimal("1155000"))
                .build();

        AvailableRoomTypeResponse room = AvailableRoomTypeResponse.builder()
                .hotelRoomTypeId(101)
                .roomTypeCode("DELUXE")
                .roomTypeName("Deluxe Sea View")
                .standardAdults((short) 2)
                .standardChildren((short) 0)
                .maxAdults((short) 2)
                .maxChildren((short) 1)
                .maxTotalGuests((short) 3)
                .extraBeds((short) 1)
                .availableRooms(3)
                .priceBreakdown(pb)
                .amenities(List.of(RoomAmenityDto.builder().code("WIFI").name("Wi-Fi").build()))
                .build();

        when(publicBookingSearchService.searchRooms(any(BookingSearchRequest.class)))
                .thenReturn(List.of(room));

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .hotelId(1)
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .build();

        CoreCandidateDiscoveryResult result = adapter.discoverAndQuoteCandidates(criteria, null, 50);

        assertThat(result.isAllFailed()).isFalse();
        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().get(0).getHotelRoomTypeId()).isEqualTo(101);
        assertThat(result.getCandidates().get(0).getFeatures()).anyMatch(f -> "WIFI".equals(f.getCode()));

        assertThat(result.getAvailabilityByRoomTypeId()).containsKey(101);
        assertThat(result.getAvailabilityByRoomTypeId().get(101)).hasSize(2); // 2 nights
        assertThat(result.getAvailabilityByRoomTypeId().get(101).get(0).getAvailableCount()).isEqualTo(3);
        assertThat(result.getAvailabilityByRoomTypeId().get(101).get(0).getTotalRooms()).isNull();
        assertThat(result.getAvailabilityByRoomTypeId().get(101).get(0).getBookedRooms()).isNull();
        assertThat(result.getAvailabilityByRoomTypeId().get(101).get(0).getLockedRooms()).isNull();
        assertThat(result.getAvailabilityByRoomTypeId().get(101).get(0).getOooRooms()).isNull();

        assertThat(result.getQuoteByRoomTypeId()).containsKey(101);
        assertThat(result.getQuoteByRoomTypeId().get(101).getFinalTotalAmount()).isEqualTo("1155000");
        assertThat(result.getQuoteByRoomTypeId().get(101).getChargeCoverage()).isEqualTo(BudgetCoverage.BACKEND_QUOTED_TOTAL);
        assertThat(result.getQuoteByRoomTypeId().get(101).getServiceFeeStatus()).isEqualTo(ServiceFeeStatus.INCLUDED);
        assertThat(result.getQuoteByRoomTypeId().get(101).getCurrency()).isNull();
    }

    @Test
    @DisplayName("Should invoke calculateRoomQuote when requested guests exceed standard capacity")
    void shouldInvokeCalculateRoomQuoteForSurcharges() {
        PriceBreakdownDto basePb = PriceBreakdownDto.builder()
                .hotelRoomTypeId(101)
                .finalTotalAmount(new BigDecimal("1000000"))
                .build();

        PriceBreakdownDto surchargePb = PriceBreakdownDto.builder()
                .hotelRoomTypeId(101)
                .totalRoomNetAmount(new BigDecimal("1200000"))
                .totalRoomServiceFee(new BigDecimal("60000"))
                .vatPercent(new BigDecimal("10"))
                .totalRoomVat(new BigDecimal("126000"))
                .finalTotalAmount(new BigDecimal("1386000"))
                .build();

        AvailableRoomTypeResponse room = AvailableRoomTypeResponse.builder()
                .hotelRoomTypeId(101)
                .standardAdults((short) 1)
                .maxAdults((short) 2)
                .maxTotalGuests((short) 2)
                .availableRooms(2)
                .priceBreakdown(basePb)
                .build();

        when(publicBookingSearchService.searchRooms(any(BookingSearchRequest.class)))
                .thenReturn(List.of(room));

        when(publicBookingSearchService.calculateRoomQuote(any(RoomPriceQuoteRequest.class)))
                .thenReturn(RoomPriceQuoteResponse.builder().priceBreakdown(surchargePb).build());

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .hotelId(1)
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2) // Exceeds standardAdults 1
                .roomsCount(1)
                .build();

        CoreCandidateDiscoveryResult result = adapter.discoverAndQuoteCandidates(criteria, null, 50);

        verify(publicBookingSearchService).calculateRoomQuote(any(RoomPriceQuoteRequest.class));
        assertThat(result.getQuoteByRoomTypeId().get(101).getFinalTotalAmount()).isEqualTo("1386000");
    }

    @Test
    @DisplayName("Should preserve null capacity fields from core without fabricating defaults")
    void shouldPreserveNullCapacityFieldsWithoutFabricatingDefaults() {
        PriceBreakdownDto pb = PriceBreakdownDto.builder()
                .hotelRoomTypeId(201)
                .totalNights(1)
                .finalTotalAmount(new BigDecimal("800000"))
                .build();

        AvailableRoomTypeResponse room = AvailableRoomTypeResponse.builder()
                .hotelRoomTypeId(201)
                .roomTypeCode("BASIC")
                .roomTypeName("Basic Single")
                .availableRooms(2)
                .priceBreakdown(pb)
                // standardAdults, standardChildren, maxAdults, maxChildren, maxInfants, maxTotalGuests, extraBeds all null
                .build();

        when(publicBookingSearchService.searchRooms(any(BookingSearchRequest.class)))
                .thenReturn(List.of(room));

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .hotelId(1)
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 21))
                .adults(1)
                .roomsCount(1)
                .build();

        CoreCandidateDiscoveryResult result = adapter.discoverAndQuoteCandidates(criteria, null, 50);

        assertThat(result.getCandidates()).hasSize(1);
        RoomCandidate c = result.getCandidates().get(0);
        assertThat(c.getStandardAdults()).isNull();
        assertThat(c.getStandardChildren()).isNull();
        assertThat(c.getMaxAdults()).isNull();
        assertThat(c.getMaxChildren()).isNull();
        assertThat(c.getMaxInfants()).isNull();
        assertThat(c.getMaxTotalGuests()).isNull();
        assertThat(c.getExtraBedsAllowed()).isNull();
    }

    @Test
    @DisplayName("Should map currency from hotel search during region discovery")
    void shouldMapCurrencyFromHotelInRegionSearch() {
        AvailableHotelResponse hotel = AvailableHotelResponse.builder()
                .hotelId(10)
                .hotelName("Khách sạn Hà Nội Luxury")
                .currency("VND")
                .build();

        PageResponse<AvailableHotelResponse> hotelPage = PageResponse.<AvailableHotelResponse>builder()
                .content(List.of(hotel))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(publicBookingSearchService.searchHotels(any(BookingSearchRequest.class)))
                .thenReturn(hotelPage);

        PriceBreakdownDto pb = PriceBreakdownDto.builder()
                .hotelRoomTypeId(301)
                .totalNights(1)
                .finalTotalAmount(new BigDecimal("900000"))
                .build();

        AvailableRoomTypeResponse room = AvailableRoomTypeResponse.builder()
                .hotelRoomTypeId(301)
                .availableRooms(5)
                .priceBreakdown(pb)
                .build();

        when(publicBookingSearchService.searchRooms(any(BookingSearchRequest.class)))
                .thenReturn(List.of(room));

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .locationKeyword("Hà Nội")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 21))
                .adults(1)
                .roomsCount(1)
                .build();

        CoreCandidateDiscoveryResult result = adapter.discoverAndQuoteCandidates(criteria, (short) 5, 50);

        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getQuoteByRoomTypeId().get(301).getCurrency()).isEqualTo("VND");
    }

    @Test
    @DisplayName("Should mark PRICE_FAILED and exclude candidate when detailed quote calculation fails")
    void shouldExcludeCandidateWhenDetailedQuoteFails() {
        PriceBreakdownDto basePb = PriceBreakdownDto.builder()
                .hotelRoomTypeId(101)
                .finalTotalAmount(new BigDecimal("1000000"))
                .build();

        AvailableRoomTypeResponse room = AvailableRoomTypeResponse.builder()
                .hotelRoomTypeId(101)
                .standardAdults((short) 1)
                .maxAdults((short) 2)
                .maxTotalGuests((short) 2)
                .availableRooms(2)
                .priceBreakdown(basePb)
                .build();

        when(publicBookingSearchService.searchRooms(any(BookingSearchRequest.class)))
                .thenReturn(List.of(room));

        when(publicBookingSearchService.calculateRoomQuote(any(RoomPriceQuoteRequest.class)))
                .thenThrow(new RuntimeException("Pricing engine error for surcharge"));

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .hotelId(1)
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2) // Exceeds standardAdults 1, triggers calculateRoomQuote
                .roomsCount(1)
                .build();

        CoreCandidateDiscoveryResult result = adapter.discoverAndQuoteCandidates(criteria, null, 50);

        // Candidate must NOT silently fall back to basePb, it must be excluded and marked PRICE_FAILED
        assertThat(result.getCandidates()).isEmpty();
        assertThat(result.getFailedRoomTypeIds()).contains(101);
        assertThat(result.getCoverageReasons()).contains(CoverageReason.PRICE_FAILED);
        assertThat(result.getTotalDiscoveredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return allFailed = true when publicBookingSearchService encounters exception")
    void shouldHandleCoreUpstreamException() {
        when(publicBookingSearchService.searchRooms(any(BookingSearchRequest.class)))
                .thenThrow(new RuntimeException("Database timeout"));

        RoomSearchCriteriaDto criteria = RoomSearchCriteriaDto.builder()
                .hotelId(1)
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 22))
                .adults(2)
                .roomsCount(1)
                .build();

        CoreCandidateDiscoveryResult result = adapter.discoverAndQuoteCandidates(criteria, null, 50);

        assertThat(result.isAllFailed()).isTrue();
        assertThat(result.isCatalogComplete()).isFalse();
        assertThat(result.getCoverageReasons()).contains(CoverageReason.PRICE_FAILED);
    }
}
