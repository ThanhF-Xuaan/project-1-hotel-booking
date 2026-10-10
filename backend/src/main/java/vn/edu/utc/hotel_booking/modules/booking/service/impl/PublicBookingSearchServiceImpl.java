package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingSearchRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.DestinationType;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.*;
import vn.edu.utc.hotel_booking.modules.booking.service.PublicBookingSearchService;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.HotelRoomTypeResponse;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicBookingSearchServiceImpl implements PublicBookingSearchService {

    private final RegionService regionService;
    private final HotelService hotelService;
    private final HotelRoomTypeService hotelRoomTypeService;
    private final RoomAvailabilityService roomAvailabilityService;
    private final HotelRoomTypeInclusionService hotelRoomTypeInclusionService;
    private final SurchargeRuleService surchargeRuleService;
    private final PriceEngine priceEngine;

    @Override
    @Transactional(readOnly = true)
    public List<DestinationSuggestionResponse> getDestinationSuggestions(String keyword) {
        List<DestinationSuggestionResponse> suggestions = new ArrayList<>();

        // 1. Tìm các khu vực (Regions) đang hoạt động
        try {
            RegionSearchDto regionCriteria = RegionSearchDto.builder()
                    .keyword(keyword)
                    .status("ACTIVE")
                    .page(0)
                    .pageSize(10)
                    .sortBy("name")
                    .sortDirection("ASC")
                    .build();
            PageResponse<RegionResponse> regionPage = regionService.filter(regionCriteria);
            if (regionPage != null && regionPage.getContent() != null) {
                for (RegionResponse r : regionPage.getContent()) {
                    suggestions.add(DestinationSuggestionResponse.builder()
                            .id(r.getId() != null ? r.getId().intValue() : null)
                            .type(DestinationType.REGION)
                            .title(r.getName())
                            .subTitle("Khu vực / Tỉnh thành (" + r.getCode() + ")")
                            .badge("Khu vực")
                            .build());
                }
            }
        } catch (Exception ex) {
            log.warn("Lỗi gợi ý khu vực với keyword='{}': {}", keyword, ex.getMessage());
        }

        // 2. Tìm các khách sạn (Hotels) đang hoạt động
        try {
            HotelSearchDto hotelCriteria = HotelSearchDto.builder()
                    .keyword(keyword)
                    .status("ACTIVE")
                    .page(0)
                    .pageSize(20)
                    .sortBy("name")
                    .sortDirection("ASC")
                    .build();
            PageResponse<HotelResponse> hotelPage = hotelService.filter(hotelCriteria);
            if (hotelPage != null && hotelPage.getContent() != null) {
                for (HotelResponse h : hotelPage.getContent()) {
                    String sub = (h.getAddress() != null ? h.getAddress() : "")
                            + (h.getRegionName() != null ? " - " + h.getRegionName() : "");
                    suggestions.add(DestinationSuggestionResponse.builder()
                            .id(h.getId() != null ? h.getId().intValue() : null)
                            .type(DestinationType.HOTEL)
                            .title(h.getName())
                            .subTitle(sub)
                            .badge("Khách sạn")
                            .build());
                }
            }
        } catch (Exception ex) {
            log.warn("Lỗi gợi ý khách sạn với keyword='{}': {}", keyword, ex.getMessage());
        }

        return suggestions;
    }

    @Override
    @Transactional(readOnly = true)
    public UnifiedBookingSearchResponse searchUnified(BookingSearchRequest request) {
        request.normalize();
        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        if (request.getDestinationType() == null && request.getHotelId() == null && request.getRegionId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Vui lòng chọn khách sạn hoặc khu vực để tìm kiếm");
        }

        if (request.getDestinationType() == DestinationType.HOTEL || request.getHotelId() != null) {
            List<AvailableRoomTypeResponse> rooms = searchRooms(request);

            AvailableHotelResponse selectedHotel = null;
            try {
                HotelResponse h = hotelService.getById(request.getHotelId().shortValue());
                if (h != null) {
                    selectedHotel = AvailableHotelResponse.builder()
                            .hotelId(h.getId() != null ? h.getId().intValue() : null)
                            .hotelName(h.getName())
                            .address(h.getAddress())
                            .phone(h.getPhone())
                            .regionName(h.getRegionName())
                            .availableRoomTypesCount(rooms.size())
                            .currency("VND")
                            .build();
                }
            } catch (Exception ex) {
                log.warn("Không thể lấy thông tin khách sạn id={}: {}", request.getHotelId(), ex.getMessage());
            }

            return UnifiedBookingSearchResponse.builder()
                    .searchType("ROOMS")
                    .rooms(rooms)
                    .selectedHotel(selectedHotel)
                    .build();
        } else {
            PageResponse<AvailableHotelResponse> hotels = searchHotels(request);
            return UnifiedBookingSearchResponse.builder()
                    .searchType("HOTELS")
                    .hotels(hotels)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AvailableHotelResponse> searchHotels(BookingSearchRequest request) {
        request.normalize();
        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        int roomsCount = (request.getRoomsCount() != null && request.getRoomsCount() > 0) ? request.getRoomsCount() : 1;

        HotelSearchDto hotelSearchDto = HotelSearchDto.builder()
                .regionId(request.getRegionId())
                .status("ACTIVE")
                .page(0)
                .pageSize(100)
                .sortBy("id")
                .sortDirection("ASC")
                .build();

        PageResponse<HotelResponse> hotelPage = hotelService.filter(hotelSearchDto);
        List<AvailableHotelResponse> matchedHotels = new ArrayList<>();

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) {
            nights = 1;
        }

        for (HotelResponse hotel : hotelPage.getContent()) {
            HotelRoomTypeSearchDto roomSearchDto = new HotelRoomTypeSearchDto();
            roomSearchDto.setHotelId(hotel.getId());
            roomSearchDto.setStatus("ACTIVE");
            roomSearchDto.setPage(0);
            roomSearchDto.setPageSize(100);
            roomSearchDto.setSortBy("id");
            roomSearchDto.setSortDirection("ASC");

            PageResponse<HotelRoomTypeResponse> roomTypePage = hotelRoomTypeService.filterHotelRoomTypes(roomSearchDto);

            int availableRoomTypeCount = 0;
            BigDecimal minPricePerNight = null;

            for (HotelRoomTypeResponse roomType : roomTypePage.getContent()) {
                int availableCount = checkAvailableRooms(roomType.getId(), request.getCheckInDate(), request.getCheckOutDate());
                if (availableCount >= roomsCount) {
                    availableRoomTypeCount++;
                    try {
                        PriceBreakdownDto breakdown = calculateRoomPipelinePrice(
                                roomType, request.getCheckInDate(), request.getCheckOutDate(), request.getVoucherCode()
                        );
                        BigDecimal totalAmount = (breakdown != null && breakdown.getFinalTotalAmount() != null)
                                ? breakdown.getFinalTotalAmount()
                                : (breakdown != null ? breakdown.getTotalRoomGrossAmount() : null);
                        if (totalAmount != null) {
                            BigDecimal pricePerNight = totalAmount
                                    .divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
                            if (minPricePerNight == null || pricePerNight.compareTo(minPricePerNight) < 0) {
                                minPricePerNight = pricePerNight;
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Lỗi tính giá phòng roomTypeId={}: {}", roomType.getId(), ex.getMessage());
                    }
                }
            }

            if (availableRoomTypeCount > 0) {
                AvailableHotelResponse hotelResp = AvailableHotelResponse.builder()
                        .hotelId(hotel.getId() != null ? hotel.getId().intValue() : null)
                        .hotelName(hotel.getName())
                        .address(hotel.getAddress())
                        .phone(hotel.getPhone())
                        .regionName(hotel.getRegionName())
                        .availableRoomTypesCount(availableRoomTypeCount)
                        .minPricePerNight(minPricePerNight)
                        .currency("VND")
                        .build();
                matchedHotels.add(hotelResp);
            }
        }

        int pageNum = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        int fromIndex = Math.min(pageNum * pageSize, matchedHotels.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedHotels.size());
        List<AvailableHotelResponse> pagedList = matchedHotels.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) matchedHotels.size() / pageSize);

        return PageResponse.<AvailableHotelResponse>builder()
                .content(pagedList)
                .pageNumber(pageNum)
                .pageSize(pageSize)
                .totalElements((long) matchedHotels.size())
                .totalPages(totalPages)
                .first(pageNum == 0)
                .last(pageNum >= totalPages - 1)
                .empty(matchedHotels.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableRoomTypeResponse> searchRooms(BookingSearchRequest request) {
        request.normalize();
        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        if (request.getHotelId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Vui lòng chọn khách sạn để tra cứu phòng");
        }
        int roomsCount = (request.getRoomsCount() != null && request.getRoomsCount() > 0) ? request.getRoomsCount() : 1;

        HotelRoomTypeSearchDto roomSearchDto = new HotelRoomTypeSearchDto();
        roomSearchDto.setHotelId(request.getHotelId().shortValue());
        roomSearchDto.setStatus("ACTIVE");
        roomSearchDto.setPage(0);
        roomSearchDto.setPageSize(100);
        roomSearchDto.setSortBy("id");
        roomSearchDto.setSortDirection("ASC");

        PageResponse<HotelRoomTypeResponse> roomTypePage = hotelRoomTypeService.filterHotelRoomTypes(roomSearchDto);
        List<AvailableRoomTypeResponse> results = new ArrayList<>();

        for (HotelRoomTypeResponse roomType : roomTypePage.getContent()) {
            int availableCount = checkAvailableRooms(roomType.getId(), request.getCheckInDate(), request.getCheckOutDate());
            if (availableCount < roomsCount) {
                continue; // Loại bỏ hạng phòng không đủ số lượng phòng trống liên tục
            }

            // 1. Định giá phòng trọn gói qua Pricing Pipeline (Room-only)
            PriceBreakdownDto priceBreakdown = calculateRoomPipelinePrice(
                    roomType, request.getCheckInDate(), request.getCheckOutDate(), request.getVoucherCode()
            );

            // 2. Tiện ích của phòng (Room Amenities)
            List<RoomAmenityDto> amenities = Collections.emptyList();
            if (roomType.getFeatures() != null) {
                amenities = roomType.getFeatures().stream()
                        .map(f -> RoomAmenityDto.builder()
                                .id(f.getId())
                                .code(f.getCode())
                                .name(f.getName())
                                .category(f.getDescription())
                                .build())
                        .toList();
            }

            // 3. Tặng kèm của phòng (Room Inclusions)
            List<RoomInclusionDto> inclusions = hotelRoomTypeInclusionService.getInclusionsByHotelRoomTypeId(roomType.getId());

            // 4. Chính sách phụ thu hiện có (Chỉ bao gồm thêm người và thêm giường)
            List<RoomSurchargePolicyDto> surchargePolicies = surchargeRuleService.getActiveSurchargePolicies(
                    roomType.getId(), request.getCheckInDate(), request.getCheckOutDate()
            );

            AvailableRoomTypeResponse response = AvailableRoomTypeResponse.builder()
                    .hotelRoomTypeId(roomType.getId())
                    .roomTypeCode(roomType.getRoomTypeCode())
                    .roomTypeName(roomType.getRoomTypeName())
                    .standardAdults(roomType.getStandardAdults())
                    .maxAdults(roomType.getMaxAdults())
                    .maxChildren(roomType.getMaxChildren())
                    .maxTotalGuests(roomType.getMaxTotalGuests())
                    .maxBeds(roomType.getMaxBeds())
                    .extraBeds(roomType.getExtraBeds())
                    .availableRooms(availableCount)
                    .priceBreakdown(priceBreakdown)
                    .amenities(amenities)
                    .inclusions(inclusions)
                    .surchargePolicies(surchargePolicies)
                    .build();

            results.add(response);
        }

        return results;
    }

    private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày nhận phòng và trả phòng không được để trống");
        }
        if (!checkInDate.isBefore(checkOutDate)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Ngày nhận phòng phải trước ngày trả phòng ít nhất 1 đêm");
        }
    }

    private int checkAvailableRooms(Integer hotelRoomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        LocalDate endDate = checkOutDate.minusDays(1);
        RoomAvailabilitySearchDto searchDto = RoomAvailabilitySearchDto.builder()
                .hotelRoomTypeId(hotelRoomTypeId)
                .startDate(checkInDate)
                .endDate(endDate)
                .build();

        List<RoomAvailabilityResponse> availList = roomAvailabilityService.getAvailability(searchDto);
        if (availList == null || availList.isEmpty()) {
            return 0;
        }

        long expectedNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (availList.size() < expectedNights) {
            return 0; // Thiếu dữ liệu tồn phòng của một hoặc nhiều đêm trong kỳ lưu trú
        }

        return availList.stream()
                .mapToInt(a -> a.getAvailableCount() != null ? a.getAvailableCount() : 0)
                .min()
                .orElse(0);
    }

    private PriceBreakdownDto calculateRoomPipelinePrice(
            HotelRoomTypeResponse roomType,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String voucherCode
    ) {
        PriceCalculationRequest calcReq = PriceCalculationRequest.builder()
                .hotelRoomTypeId(roomType.getId())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .adults(roomType.getStandardAdults() != null ? roomType.getStandardAdults() : (short) 2)
                .children((short) 0)
                .extraBeds((short) 0)
                .voucherCode(voucherCode)
                .build();
        return priceEngine.calculatePrice(calcReq);
    }
}
