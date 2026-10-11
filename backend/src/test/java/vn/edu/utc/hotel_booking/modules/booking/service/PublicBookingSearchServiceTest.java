package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingSearchRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.DestinationType;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.*;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.PublicBookingSearchServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomFeatureResponse;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeInclusionService;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelResponse;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.RegionResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.organization.service.RegionService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PriceCalculationRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;
import vn.edu.utc.hotel_booking.modules.pricing.service.PriceEngine;
import vn.edu.utc.hotel_booking.modules.pricing.service.SurchargeRuleService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicBookingSearchServiceTest {

    @Mock RegionService regionService;
    @Mock HotelService hotelService;
    @Mock HotelRoomTypeService hotelRoomTypeService;
    @Mock RoomAvailabilityService roomAvailabilityService;
    @Mock HotelRoomTypeInclusionService hotelRoomTypeInclusionService;
    @Mock SurchargeRuleService surchargeRuleService;
    @Mock PriceEngine priceEngine;
    @Mock vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository menuRepository;
    @Mock vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository vatRuleRepository;

    @InjectMocks
    PublicBookingSearchServiceImpl publicBookingSearchService;

    private LocalDate checkIn;
    private LocalDate checkOut;

    @BeforeEach
    void setUp() {
        checkIn = LocalDate.now().plusDays(2);
        checkOut = LocalDate.now().plusDays(4); // 2 nights
    }

    @Test
    @DisplayName("searchHotels: Tìm khách sạn thành công, loại trừ khách sạn hết phòng và tính đúng minPricePerNight")
    void searchHotels_Success_ReturnsOnlyAvailableHotels() {
        BookingSearchRequest req = new BookingSearchRequest();
        req.setRegionId((short) 1);
        req.setCheckInDate(checkIn);
        req.setCheckOutDate(checkOut);
        req.setRoomsCount(1);

        HotelResponse h1 = HotelResponse.builder()
                .id((short) 10)
                .name("Khách Sạn A")
                .regionName("Hà Nội")
                .build();
        HotelResponse h2 = HotelResponse.builder()
                .id((short) 20)
                .name("Khách Sạn B (Hết phòng)")
                .regionName("Hà Nội")
                .build();

        when(hotelService.filter(any(HotelSearchDto.class))).thenReturn(
                PageResponse.<HotelResponse>builder().content(List.of(h1, h2)).build()
        );

        HotelRoomTypeResponse rt1 = HotelRoomTypeResponse.builder()
                .id(101)
                .roomTypeCode("DLX")
                .roomTypeName("Deluxe")
                .standardAdults((short) 2)
                .build();

        HotelRoomTypeResponse rt2 = HotelRoomTypeResponse.builder()
                .id(201)
                .roomTypeCode("STD")
                .roomTypeName("Standard")
                .standardAdults((short) 2)
                .build();

        when(hotelRoomTypeService.filterHotelRoomTypes(any(HotelRoomTypeSearchDto.class)))
                .thenAnswer(inv -> {
                    HotelRoomTypeSearchDto dto = inv.getArgument(0);
                    if (dto.getHotelId() != null && dto.getHotelId() == 10) {
                        return PageResponse.<HotelRoomTypeResponse>builder().content(List.of(rt1)).build();
                    } else {
                        return PageResponse.<HotelRoomTypeResponse>builder().content(List.of(rt2)).build();
                    }
                });

        // rt1: 2 nights, both available
        RoomAvailabilityResponse av1 = RoomAvailabilityResponse.builder().date(checkIn).availableCount(3).build();
        RoomAvailabilityResponse av2 = RoomAvailabilityResponse.builder().date(checkIn.plusDays(1)).availableCount(2).build();

        // rt2: night 2 is 0
        RoomAvailabilityResponse av3 = RoomAvailabilityResponse.builder().date(checkIn).availableCount(2).build();
        RoomAvailabilityResponse av4 = RoomAvailabilityResponse.builder().date(checkIn.plusDays(1)).availableCount(0).build();

        when(roomAvailabilityService.getAvailability(any(RoomAvailabilitySearchDto.class)))
                .thenAnswer(inv -> {
                    RoomAvailabilitySearchDto dto = inv.getArgument(0);
                    if (dto.getHotelRoomTypeId() == 101) {
                        return List.of(av1, av2);
                    } else {
                        return List.of(av3, av4);
                    }
                });

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .finalTotalAmount(new BigDecimal("2000000.00")) // 2 nights -> 1,000,000 / night
                .totalRoomGrossAmount(new BigDecimal("2000000.00"))
                .build();
        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(breakdown);

        PageResponse<AvailableHotelResponse> response = publicBookingSearchService.searchHotels(req);

        assertThat(response.getContent()).hasSize(1);
        AvailableHotelResponse matched = response.getContent().get(0);
        assertThat(matched.getHotelId()).isEqualTo(10);
        assertThat(matched.getHotelName()).isEqualTo("Khách Sạn A");
        assertThat(matched.getAvailableRoomTypesCount()).isEqualTo(1);
        assertThat(matched.getMinPricePerNight()).isEqualByComparingTo("1000000.00");
    }

    @Test
    @DisplayName("searchHotels: Báo lỗi khi checkOutDate <= checkInDate")
    void searchHotels_InvalidDates_ThrowsAppException() {
        BookingSearchRequest req = new BookingSearchRequest();
        req.setCheckInDate(checkIn);
        req.setCheckOutDate(checkIn); // same day -> invalid

        assertThatThrownBy(() -> publicBookingSearchService.searchHotels(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("searchRooms: Trả về đầy đủ tiện ích, tặng kèm, chính sách phụ thu thêm người/thêm giường và giá pipeline")
    void searchRooms_Success_ReturnsFullRoomDetails() {
        BookingSearchRequest req = new BookingSearchRequest();
        req.setHotelId(10);
        req.setCheckInDate(checkIn);
        req.setCheckOutDate(checkOut);
        req.setRoomsCount(1);
        req.setVoucherCode("SALE10");

        RoomFeatureResponse feature = RoomFeatureResponse.builder()
                .id((short) 1)
                .code("BALCONY")
                .name("Ban công ngắm biển")
                .description("VIEW")
                .build();

        HotelRoomTypeResponse rt = HotelRoomTypeResponse.builder()
                .id(101)
                .roomTypeCode("DLX")
                .roomTypeName("Deluxe Hướng Biển")
                .standardAdults((short) 2)
                .standardChildren((short) 1)
                .maxAdults((short) 3)
                .maxChildren((short) 1)
                .maxInfants((short) 1)
                .features(Set.of(feature))
                .build();

        when(hotelRoomTypeService.filterHotelRoomTypes(any(HotelRoomTypeSearchDto.class))).thenReturn(
                PageResponse.<HotelRoomTypeResponse>builder().content(List.of(rt)).build()
        );

        RoomAvailabilityResponse av1 = RoomAvailabilityResponse.builder().date(checkIn).availableCount(5).build();
        RoomAvailabilityResponse av2 = RoomAvailabilityResponse.builder().date(checkIn.plusDays(1)).availableCount(4).build();
        when(roomAvailabilityService.getAvailability(any(RoomAvailabilitySearchDto.class))).thenReturn(List.of(av1, av2));

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .finalTotalAmount(new BigDecimal("2200000.00"))
                .totalRoomGrossAmount(new BigDecimal("2200000.00"))
                .totalDiscountAmount(new BigDecimal("200000.00"))
                .totalRoomServiceFee(new BigDecimal("100000.00"))
                .totalRoomVat(new BigDecimal("160000.00"))
                .build();
        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(breakdown);

        RoomInclusionDto inclusion = RoomInclusionDto.builder()
                .id(1L)
                .name("Buffet Sáng Tiêu Chuẩn")
                .quantity(1)
                .build();
        when(hotelRoomTypeInclusionService.getInclusionsByHotelRoomTypeId(101)).thenReturn(List.of(inclusion));

        RoomSurchargePolicyDto surchargeExtraPerson = RoomSurchargePolicyDto.builder()
                .ruleType("EXTRA_PERSON")
                .adjustmentValue(new BigDecimal("200000.00"))
                .formattedDescription("Phụ thu thêm người: 200,000 VND / đêm")
                .build();
        RoomSurchargePolicyDto surchargeExtraBed = RoomSurchargePolicyDto.builder()
                .ruleType("EXTRA_BED")
                .adjustmentValue(new BigDecimal("30.00"))
                .formattedDescription("Phụ thu kê thêm giường: 30% giá phòng / đêm")
                .build();
        when(surchargeRuleService.getActiveSurchargePolicies(101, checkIn, checkOut))
                .thenReturn(List.of(surchargeExtraPerson, surchargeExtraBed));

        List<AvailableRoomTypeResponse> result = publicBookingSearchService.searchRooms(req);

        assertThat(result).hasSize(1);
        AvailableRoomTypeResponse room = result.get(0);
        assertThat(room.getHotelRoomTypeId()).isEqualTo(101);
        assertThat(room.getStandardChildren()).isEqualTo((short) 1);
        assertThat(room.getMaxInfants()).isEqualTo((short) 1);
        assertThat(room.getAvailableRooms()).isEqualTo(4); // min(5, 4) = 4
        assertThat(room.getPriceBreakdown().getFinalTotalAmount()).isEqualByComparingTo("2200000.00");

        // Verify Amenities
        assertThat(room.getAmenities()).hasSize(1);
        assertThat(room.getAmenities().get(0).getCode()).isEqualTo("BALCONY");

        // Verify Inclusions
        assertThat(room.getInclusions()).hasSize(1);
        assertThat(room.getInclusions().get(0).getName()).isEqualTo("Buffet Sáng Tiêu Chuẩn");

        // Verify Surcharges (extra person, extra bed)
        assertThat(room.getSurchargePolicies()).hasSize(2);
        assertThat(room.getSurchargePolicies()).extracting(RoomSurchargePolicyDto::getRuleType)
                .containsExactlyInAnyOrder("EXTRA_PERSON", "EXTRA_BED");
    }

    @Test
    @DisplayName("searchRooms: Báo lỗi khi thiếu hotelId")
    void searchRooms_MissingHotelId_ThrowsAppException() {
        BookingSearchRequest req = new BookingSearchRequest();
        req.setCheckInDate(checkIn);
        req.setCheckOutDate(checkOut);
        // hotelId is null

        assertThatThrownBy(() -> publicBookingSearchService.searchRooms(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("searchRooms: Loại bỏ hạng phòng nếu một trong các đêm không đủ số lượng phòng yêu cầu")
    void searchRooms_InsufficientInventory_FilteredOut() {
        BookingSearchRequest req = new BookingSearchRequest();
        req.setHotelId(10);
        req.setCheckInDate(checkIn);
        req.setCheckOutDate(checkOut);
        req.setRoomsCount(2); // Cần 2 phòng

        HotelRoomTypeResponse rt = HotelRoomTypeResponse.builder().id(101).build();
        when(hotelRoomTypeService.filterHotelRoomTypes(any(HotelRoomTypeSearchDto.class))).thenReturn(
                PageResponse.<HotelRoomTypeResponse>builder().content(List.of(rt)).build()
        );

        // Đêm 1 có 2 phòng, Đêm 2 chỉ còn 1 phòng -> không đủ
        RoomAvailabilityResponse av1 = RoomAvailabilityResponse.builder().date(checkIn).availableCount(2).build();
        RoomAvailabilityResponse av2 = RoomAvailabilityResponse.builder().date(checkIn.plusDays(1)).availableCount(1).build();
        when(roomAvailabilityService.getAvailability(any(RoomAvailabilitySearchDto.class))).thenReturn(List.of(av1, av2));

        List<AvailableRoomTypeResponse> result = publicBookingSearchService.searchRooms(req);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchUnified: Tìm kiếm theo HOTEL từ 1 ô Dropdown (destinationType=HOTEL, destinationId=10)")
    void searchUnified_HotelDestination_ReturnsRoomsAndSelectedHotel() {
        BookingSearchRequest req = new BookingSearchRequest();
        req.setDestinationType(DestinationType.HOTEL);
        req.setDestinationId(10);
        req.setCheckInDate(checkIn);
        req.setCheckOutDate(checkOut);

        HotelResponse hotel = HotelResponse.builder()
                .id((short) 10)
                .name("Viettel Luxury Hà Nội")
                .address("123 Trần Duy Hưng")
                .phone("02466668888")
                .regionName("Hà Nội")
                .build();
        when(hotelService.getById((short) 10)).thenReturn(hotel);

        HotelRoomTypeResponse rt = HotelRoomTypeResponse.builder().id(101).build();
        when(hotelRoomTypeService.filterHotelRoomTypes(any(HotelRoomTypeSearchDto.class))).thenReturn(
                PageResponse.<HotelRoomTypeResponse>builder().content(List.of(rt)).build()
        );
        RoomAvailabilityResponse av = RoomAvailabilityResponse.builder().date(checkIn).availableCount(2).build();
        when(roomAvailabilityService.getAvailability(any(RoomAvailabilitySearchDto.class))).thenReturn(List.of(av, av));
        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(
                PriceBreakdownDto.builder().finalTotalAmount(BigDecimal.valueOf(1000000)).build()
        );

        UnifiedBookingSearchResponse unified = publicBookingSearchService.searchUnified(req);

        assertThat(unified.getSearchType()).isEqualTo("ROOMS");
        assertThat(unified.getRooms()).hasSize(1);
        assertThat(unified.getSelectedHotel()).isNotNull();
        assertThat(unified.getSelectedHotel().getHotelName()).isEqualTo("Viettel Luxury Hà Nội");
    }

    @Test
    @DisplayName("searchUnified: Tìm kiếm theo REGION từ 1 ô Dropdown (destinationType=REGION, destinationId=1)")
    void searchUnified_RegionDestination_ReturnsHotels() {
        BookingSearchRequest req = new BookingSearchRequest();
        req.setDestinationType(DestinationType.REGION);
        req.setDestinationId(1);
        req.setCheckInDate(checkIn);
        req.setCheckOutDate(checkOut);

        HotelResponse h = HotelResponse.builder().id((short) 10).name("Khách sạn A").build();
        when(hotelService.filter(any(HotelSearchDto.class))).thenReturn(
                PageResponse.<HotelResponse>builder().content(List.of(h)).build()
        );
        HotelRoomTypeResponse rt = HotelRoomTypeResponse.builder().id(101).build();
        when(hotelRoomTypeService.filterHotelRoomTypes(any(HotelRoomTypeSearchDto.class))).thenReturn(
                PageResponse.<HotelRoomTypeResponse>builder().content(List.of(rt)).build()
        );
        RoomAvailabilityResponse av = RoomAvailabilityResponse.builder().date(checkIn).availableCount(2).build();
        when(roomAvailabilityService.getAvailability(any(RoomAvailabilitySearchDto.class))).thenReturn(List.of(av, av));
        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(
                PriceBreakdownDto.builder().finalTotalAmount(BigDecimal.valueOf(1000000)).build()
        );

        UnifiedBookingSearchResponse unified = publicBookingSearchService.searchUnified(req);

        assertThat(unified.getSearchType()).isEqualTo("HOTELS");
        assertThat(unified.getHotels().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("searchUnified: Ném lỗi khi cả 4 trường định danh điểm đến đều null")
    void searchUnified_NoDestinationSelected_ThrowsAppException() {
        BookingSearchRequest req = new BookingSearchRequest();
        req.setCheckInDate(checkIn);
        req.setCheckOutDate(checkOut);

        assertThatThrownBy(() -> publicBookingSearchService.searchUnified(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }

    @Test
    @DisplayName("getDestinationSuggestions: Trả về danh sách phân loại gồm cả Khu vực và Khách sạn")
    void getDestinationSuggestions_ReturnsRegionsAndHotels() {
        RegionResponse r = RegionResponse.builder().id((short) 1).name("Hà Nội").code("HAN").build();
        when(regionService.filter(any(RegionSearchDto.class))).thenReturn(
                PageResponse.<RegionResponse>builder().content(List.of(r)).build()
        );

        HotelResponse h = HotelResponse.builder().id((short) 10).name("Viettel Luxury Hà Nội").address("Hà Nội").build();
        when(hotelService.filter(any(HotelSearchDto.class))).thenReturn(
                PageResponse.<HotelResponse>builder().content(List.of(h)).build()
        );

        List<DestinationSuggestionResponse> suggestions = publicBookingSearchService.getDestinationSuggestions("Hà Nội");

        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.get(0).getType()).isEqualTo(DestinationType.REGION);
        assertThat(suggestions.get(0).getTitle()).isEqualTo("Hà Nội");
        assertThat(suggestions.get(1).getType()).isEqualTo(DestinationType.HOTEL);
        assertThat(suggestions.get(1).getTitle()).isEqualTo("Viettel Luxury Hà Nội");
    }

    @Test
    @DisplayName("calculateRoomQuote: Tính toán lại giá phòng và phụ thu thành công với đầy đủ bóc tách")
    void calculateRoomQuote_Success() {
        // Arrange
        HotelRoomTypeResponse rt = HotelRoomTypeResponse.builder()
                .id(101)
                .roomTypeCode("DLX")
                .roomTypeName("Deluxe Sea View")
                .standardAdults((short) 2)
                .standardChildren((short) 0)
                .maxAdults((short) 4)
                .maxChildren((short) 2)
                .maxInfants((short) 2)
                .maxTotalGuests((short) 5)
                .extraBeds((short) 1)
                .build();

        when(hotelRoomTypeService.getHotelRoomTypeById(101)).thenReturn(rt);

        PriceBreakdownDto breakdown = PriceBreakdownDto.builder()
                .hotelRoomTypeId(101)
                .totalBasePrice(new BigDecimal("2000000.00"))
                .totalExtraAdultsSurcharge(new BigDecimal("400000.00"))
                .totalExtraChildrenSurcharge(new BigDecimal("200000.00"))
                .totalExtraBedsSurcharge(new BigDecimal("600000.00"))
                .totalSurchargeAmount(new BigDecimal("1200000.00"))
                .totalRoomGrossAmount(new BigDecimal("3528000.00"))
                .finalTotalAmount(new BigDecimal("3528000.00"))
                .build();

        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(breakdown);

        RoomPriceQuoteRequest req = RoomPriceQuoteRequest.builder()
                .hotelRoomTypeId(101)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .adults((short) 3)
                .children((short) 1)
                .infants((short) 1)
                .extraBeds((short) 1)
                .build();

        // Act
        RoomPriceQuoteResponse quote = publicBookingSearchService.calculateRoomQuote(req);

        // Assert
        assertThat(quote).isNotNull();
        assertThat(quote.getHotelRoomTypeId()).isEqualTo(101);
        assertThat(quote.getRequestedAdults()).isEqualTo((short) 3);
        assertThat(quote.getRequestedChildren()).isEqualTo((short) 1);
        assertThat(quote.getRequestedInfants()).isEqualTo((short) 1);
        assertThat(quote.getRequestedExtraBeds()).isEqualTo((short) 1);

        ItemizedSurchargeDto summary = quote.getSurchargeSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.getExtraAdultsCount()).isEqualTo((short) 1);
        assertThat(summary.getExtraAdultsSurchargeTotal()).isEqualByComparingTo(new BigDecimal("400000.00"));
        assertThat(summary.getExtraChildrenCount()).isEqualTo((short) 1);
        assertThat(summary.getExtraChildrenSurchargeTotal()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(summary.getInfantsCount()).isEqualTo((short) 1);
        assertThat(summary.getInfantsSurchargeTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getExtraBedsCount()).isEqualTo((short) 1);
        assertThat(summary.getExtraBedsSurchargeTotal()).isEqualByComparingTo(new BigDecimal("600000.00"));
        assertThat(summary.getTotalSurchargeAmount()).isEqualByComparingTo(new BigDecimal("1200000.00"));
        assertThat(summary.getSurchargeNotes()).hasSize(4);
    }

    @Test
    @DisplayName("calculateRoomQuote: Ném ngoại lệ khi số người lớn vượt quá maxAdults")
    void calculateRoomQuote_AdultsExceedMax_ThrowsException() {
        HotelRoomTypeResponse rt = HotelRoomTypeResponse.builder()
                .id(101)
                .maxAdults((short) 2)
                .build();

        when(hotelRoomTypeService.getHotelRoomTypeById(101)).thenReturn(rt);

        RoomPriceQuoteRequest req = RoomPriceQuoteRequest.builder()
                .hotelRoomTypeId(101)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .adults((short) 3)
                .build();

        assertThatThrownBy(() -> publicBookingSearchService.calculateRoomQuote(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA)
                .hasMessageContaining("vượt quá giới hạn tối đa cho phép của hạng phòng");
    }

    @Test
    @DisplayName("calculateRoomQuote: Ném ngoại lệ khi tổng số khách vượt quá maxTotalGuests")
    void calculateRoomQuote_TotalGuestsExceedMax_ThrowsException() {
        HotelRoomTypeResponse rt = HotelRoomTypeResponse.builder()
                .id(101)
                .maxAdults((short) 3)
                .maxChildren((short) 2)
                .maxInfants((short) 2)
                .maxTotalGuests((short) 4)
                .build();

        when(hotelRoomTypeService.getHotelRoomTypeById(101)).thenReturn(rt);

        RoomPriceQuoteRequest req = RoomPriceQuoteRequest.builder()
                .hotelRoomTypeId(101)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .adults((short) 2)
                .children((short) 2)
                .infants((short) 1) // Tổng = 5 > 4
                .build();

        assertThatThrownBy(() -> publicBookingSearchService.calculateRoomQuote(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA)
                .hasMessageContaining("vượt quá sức chứa tối đa của phòng");
    }

    @Test
    @DisplayName("calculateCartQuote: Tính báo giá giỏ hàng nhiều phòng thành công kèm voucher")
    void calculateCartQuote_Success() {
        HotelRoomTypeResponse rt1 = HotelRoomTypeResponse.builder()
                .id(101)
                .hotelId((short) 1)
                .roomTypeCode("STD")
                .roomTypeName("Standard")
                .standardAdults((short) 2)
                .maxAdults((short) 2)
                .maxChildren((short) 1)
                .maxInfants((short) 1)
                .maxTotalGuests((short) 3)
                .extraBeds((short) 1)
                .basePrice(BigDecimal.valueOf(1000000))
                .build();

        HotelRoomTypeResponse rt2 = HotelRoomTypeResponse.builder()
                .id(102)
                .hotelId((short) 1)
                .roomTypeCode("DLX")
                .roomTypeName("Deluxe")
                .standardAdults((short) 2)
                .maxAdults((short) 3)
                .maxChildren((short) 2)
                .maxInfants((short) 1)
                .maxTotalGuests((short) 4)
                .extraBeds((short) 1)
                .basePrice(BigDecimal.valueOf(2000000))
                .build();

        when(hotelRoomTypeService.getHotelRoomTypeById(101)).thenReturn(rt1);
        when(hotelRoomTypeService.getHotelRoomTypeById(102)).thenReturn(rt2);

        PriceBreakdownDto breakdown1 = PriceBreakdownDto.builder()
                .totalBasePrice(BigDecimal.valueOf(2000000))
                .totalSurchargeAmount(BigDecimal.ZERO)
                .totalDiscountAmount(BigDecimal.valueOf(200000))
                .preTaxAmount(BigDecimal.valueOf(1800000))
                .totalRoomServiceFee(BigDecimal.valueOf(90000))
                .vatAmount(BigDecimal.valueOf(189000))
                .finalTotalAmount(BigDecimal.valueOf(2079000))
                .dailyPrices(Collections.emptyList())
                .build();

        PriceBreakdownDto breakdown2 = PriceBreakdownDto.builder()
                .totalBasePrice(BigDecimal.valueOf(4000000))
                .totalSurchargeAmount(BigDecimal.valueOf(300000))
                .totalDiscountAmount(BigDecimal.valueOf(400000))
                .preTaxAmount(BigDecimal.valueOf(3900000))
                .totalRoomServiceFee(BigDecimal.valueOf(195000))
                .vatAmount(BigDecimal.valueOf(409500))
                .finalTotalAmount(BigDecimal.valueOf(4504500))
                .dailyPrices(Collections.emptyList())
                .build();

        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class)))
                .thenReturn(breakdown1)
                .thenReturn(breakdown2);

        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.CartItemQuoteRequest item1 =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.CartItemQuoteRequest.builder()
                        .hotelRoomTypeId(101)
                        .quantity((short) 1)
                        .adults((short) 2)
                        .children((short) 0)
                        .infants((short) 0)
                        .extraBeds((short) 0)
                        .build();

        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.CartItemQuoteRequest item2 =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.CartItemQuoteRequest.builder()
                        .hotelRoomTypeId(102)
                        .quantity((short) 1)
                        .adults((short) 2)
                        .children((short) 1)
                        .infants((short) 0)
                        .extraBeds((short) 1)
                        .build();

        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest cartReq =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.builder()
                        .hotelId((short) 1)
                        .checkInDate(checkIn)
                        .checkOutDate(checkOut)
                        .voucherCode("SUMMERPROMO")
                        .items(List.of(item1, item2))
                        .build();

        CartPriceQuoteResponse response = publicBookingSearchService.calculateCartQuote(cartReq);

        assertThat(response).isNotNull();
        assertThat(response.getItemizedQuotes()).hasSize(2);
        assertThat(response.getTotalBasePrice()).isEqualByComparingTo(BigDecimal.valueOf(6000000));
        assertThat(response.getTotalSurchargeAmount()).isEqualByComparingTo(BigDecimal.valueOf(300000));
        assertThat(response.getTotalDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(600000));
        assertThat(response.getFinalTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(6583500));
    }

    @Test
    @DisplayName("calculateCartQuote: Ném ngoại lệ khi giỏ hàng rỗng")
    void calculateCartQuote_EmptyItems_ThrowsException() {
        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest cartReq =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.builder()
                        .hotelId((short) 1)
                        .checkInDate(checkIn)
                        .checkOutDate(checkOut)
                        .items(Collections.emptyList())
                        .build();

        assertThatThrownBy(() -> publicBookingSearchService.calculateCartQuote(cartReq))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA)
                .hasMessageContaining("Giỏ hàng không được để trống");
    }

    @Test
    @DisplayName("calculateCartQuote: Tính toán chính xác giá dịch vụ kèm theo (SERVICE only) và Grand Total")
    void calculateCartQuote_WithServiceAddons_CalculatesTaxesAndGrandTotal() {
        HotelRoomTypeResponse roomType1 = HotelRoomTypeResponse.builder()
                .id(1)
                .hotelId((short) 1)
                .roomTypeCode("DLX")
                .roomTypeName("Deluxe Room")
                .maxAdults((short) 2)
                .maxChildren((short) 1)
                .maxInfants((short) 1)
                .maxTotalGuests((short) 3)
                .extraBeds((short) 1)
                .build();
        when(hotelRoomTypeService.getHotelRoomTypeById(1)).thenReturn(roomType1);

        PriceBreakdownDto breakdown1 = PriceBreakdownDto.builder()
                .totalBasePrice(BigDecimal.valueOf(2000000))
                .totalSurchargeAmount(BigDecimal.ZERO)
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalRoomNetAmount(BigDecimal.valueOf(2000000))
                .totalRoomServiceFee(BigDecimal.valueOf(100000))
                .totalRoomVat(BigDecimal.valueOf(210000))
                .finalTotalAmount(BigDecimal.valueOf(2310000))
                .build();
        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(breakdown1);

        vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory taxCat = vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory.builder()
                .id(10)
                .categoryName("Dịch vụ VAT 10%")
                .build();
        vn.edu.utc.hotel_booking.modules.operation.entity.Menu serviceMenu = vn.edu.utc.hotel_booking.modules.operation.entity.Menu.builder()
                .id(100)
                .name("Đón tiễn sân bay")
                .menuType(vn.edu.utc.hotel_booking.modules.operation.entity.MenuType.SERVICE)
                .basePrice(BigDecimal.valueOf(500000))
                .status("ACTIVE")
                .taxCategory(taxCat)
                .build();
        when(menuRepository.findByIdAndIsDeletedFalse(100)).thenReturn(java.util.Optional.of(serviceMenu));

        vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule vatRule = vn.edu.utc.hotel_booking.modules.pricing.entity.VatRule.builder()
                .id(1)
                .taxCategory(taxCat)
                .vatPercent(BigDecimal.valueOf(10))
                .build();
        when(vatRuleRepository.findActiveVatRule(any(), any())).thenReturn(java.util.Optional.of(vatRule));

        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.CartItemQuoteRequest item =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.CartItemQuoteRequest.builder()
                        .hotelRoomTypeId(1)
                        .quantity((short) 1)
                        .build();

        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.SelectedServiceRequest serviceReq =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.SelectedServiceRequest.builder()
                        .menuId(100)
                        .quantity(1)
                        .build();

        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest cartReq =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.builder()
                        .hotelId((short) 1)
                        .checkInDate(checkIn)
                        .checkOutDate(checkOut)
                        .items(List.of(item))
                        .selectedServices(List.of(serviceReq))
                        .build();

        CartPriceQuoteResponse response = publicBookingSearchService.calculateCartQuote(cartReq);

        assertThat(response).isNotNull();
        assertThat(response.getFinalTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2310000));
        assertThat(response.getServiceItems()).hasSize(1);
        assertThat(response.getTotalServiceSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        assertThat(response.getTotalServiceFee()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(response.getTotalServiceVat()).isEqualByComparingTo(BigDecimal.valueOf(52500));
        assertThat(response.getTotalServiceGrossAmount()).isEqualByComparingTo(BigDecimal.valueOf(577500));
        assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(2887500));
    }

    @Test
    @DisplayName("calculateCartQuote: Ném ngoại lệ khi cố tình chọn PRODUCT (Chỉ cho phép SERVICE)")
    void calculateCartQuote_WithProductAddons_ThrowsException() {
        HotelRoomTypeResponse roomType1 = HotelRoomTypeResponse.builder()
                .id(1)
                .hotelId((short) 1)
                .roomTypeCode("DLX")
                .roomTypeName("Deluxe Room")
                .maxAdults((short) 2)
                .maxChildren((short) 1)
                .maxInfants((short) 1)
                .maxTotalGuests((short) 3)
                .extraBeds((short) 1)
                .build();
        when(hotelRoomTypeService.getHotelRoomTypeById(1)).thenReturn(roomType1);

        PriceBreakdownDto breakdown1 = PriceBreakdownDto.builder()
                .finalTotalAmount(BigDecimal.valueOf(2000000))
                .build();
        when(priceEngine.calculatePrice(any(PriceCalculationRequest.class))).thenReturn(breakdown1);

        vn.edu.utc.hotel_booking.modules.operation.entity.Menu productMenu = vn.edu.utc.hotel_booking.modules.operation.entity.Menu.builder()
                .id(200)
                .name("Bia Heineken lon")
                .menuType(vn.edu.utc.hotel_booking.modules.operation.entity.MenuType.PRODUCT)
                .basePrice(BigDecimal.valueOf(35000))
                .status("ACTIVE")
                .build();
        when(menuRepository.findByIdAndIsDeletedFalse(200)).thenReturn(java.util.Optional.of(productMenu));

        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.CartItemQuoteRequest item =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.CartItemQuoteRequest.builder()
                        .hotelRoomTypeId(1)
                        .quantity((short) 1)
                        .build();

        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.SelectedServiceRequest productReq =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.SelectedServiceRequest.builder()
                        .menuId(200)
                        .quantity(2)
                        .build();

        vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest cartReq =
                vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest.builder()
                        .hotelId((short) 1)
                        .checkInDate(checkIn)
                        .checkOutDate(checkOut)
                        .items(List.of(item))
                        .selectedServices(List.of(productReq))
                        .build();

        assertThatThrownBy(() -> publicBookingSearchService.calculateCartQuote(cartReq))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA)
                .hasMessageContaining("Chỉ được phép chọn dịch vụ (SERVICE) kèm theo khi đặt phòng online");
    }
}

