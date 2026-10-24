package vn.edu.utc.hotel_booking.modules.ai.advisor.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AmbiguityDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.PriceQuoteDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomFeatureDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AmbiguityReason;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCheck;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCoverage;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CoverageReason;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ServiceFeeStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.CandidateAvailability;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.RoomCandidate;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingSearchRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.DestinationType;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.*;
import vn.edu.utc.hotel_booking.modules.booking.service.PublicBookingSearchService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PriceBreakdownDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicBookingCoreAdapter implements RoomAdvisorCorePort {

    private final PublicBookingSearchService publicBookingSearchService;

    @Override
    public DestinationResolutionResult resolveDestination(String locationQuery, Integer hotelId, Short regionId) {
        if (hotelId != null && hotelId > 0) {
            return DestinationResolutionResult.builder()
                    .hotelId(hotelId)
                    .resolvedName("Hotel " + hotelId)
                    .ambiguous(false)
                    .ambiguities(new ArrayList<>())
                    .build();
        }

        if (regionId != null && regionId > 0) {
            return DestinationResolutionResult.builder()
                    .regionId(regionId)
                    .resolvedName("Region " + regionId)
                    .ambiguous(false)
                    .ambiguities(new ArrayList<>())
                    .build();
        }

        if (locationQuery == null || locationQuery.isBlank()) {
            return DestinationResolutionResult.builder()
                    .ambiguous(false)
                    .ambiguities(new ArrayList<>())
                    .build();
        }

        try {
            List<DestinationSuggestionResponse> suggestions = publicBookingSearchService.getDestinationSuggestions(locationQuery.trim());
            if (suggestions == null || suggestions.isEmpty()) {
                return DestinationResolutionResult.builder()
                        .resolvedName(locationQuery.trim())
                        .ambiguous(false)
                        .ambiguities(new ArrayList<>())
                        .build();
            }

            if (suggestions.size() == 1) {
                DestinationSuggestionResponse single = suggestions.get(0);
                if (single.getType() == DestinationType.HOTEL) {
                    return DestinationResolutionResult.builder()
                            .hotelId(single.getId())
                            .resolvedName(single.getTitle())
                            .ambiguous(false)
                            .ambiguities(new ArrayList<>())
                            .build();
                } else {
                    return DestinationResolutionResult.builder()
                            .regionId(single.getId() != null ? single.getId().shortValue() : null)
                            .resolvedName(single.getTitle())
                            .ambiguous(false)
                            .ambiguities(new ArrayList<>())
                            .build();
                }
            }

            // Check if one exact match title exists
            String trimmedQuery = locationQuery.trim();
            Optional<DestinationSuggestionResponse> exactMatch = suggestions.stream()
                    .filter(s -> s.getTitle() != null && s.getTitle().equalsIgnoreCase(trimmedQuery))
                    .findFirst();

            if (exactMatch.isPresent()) {
                DestinationSuggestionResponse matched = exactMatch.get();
                if (matched.getType() == DestinationType.HOTEL) {
                    return DestinationResolutionResult.builder()
                            .hotelId(matched.getId())
                            .resolvedName(matched.getTitle())
                            .ambiguous(false)
                            .ambiguities(new ArrayList<>())
                            .build();
                } else {
                    return DestinationResolutionResult.builder()
                            .regionId(matched.getId() != null ? matched.getId().shortValue() : null)
                            .resolvedName(matched.getTitle())
                            .ambiguous(false)
                            .ambiguities(new ArrayList<>())
                            .build();
                }
            }

            // Multiple ambiguous destinations: fail closed with MULTIPLE_LOCATIONS ambiguity
            AmbiguityDto ambiguity = new AmbiguityDto("locationKeyword", AmbiguityReason.MULTIPLE_LOCATIONS.name());
            return DestinationResolutionResult.builder()
                    .ambiguous(true)
                    .ambiguities(List.of(ambiguity))
                    .build();

        } catch (Exception e) {
            log.error("Failed to resolve destination suggestions for query: {}", locationQuery, e);
            return DestinationResolutionResult.builder()
                    .resolvedName(locationQuery)
                    .ambiguous(false)
                    .ambiguities(new ArrayList<>())
                    .build();
        }
    }

    @Override
    public CoreCandidateDiscoveryResult discoverAndQuoteCandidates(
            RoomSearchCriteriaDto criteria,
            Short regionId,
            int maxCandidates
    ) {
        if (criteria == null || criteria.getCheckInDate() == null || criteria.getCheckOutDate() == null) {
            return CoreCandidateDiscoveryResult.builder()
                    .catalogComplete(true)
                    .allFailed(false)
                    .build();
        }

        long totalNights = ChronoUnit.DAYS.between(criteria.getCheckInDate(), criteria.getCheckOutDate());
        if (totalNights <= 0) {
            return CoreCandidateDiscoveryResult.builder()
                    .catalogComplete(true)
                    .allFailed(false)
                    .build();
        }

        int limit = maxCandidates > 0 ? maxCandidates : 50;

        List<AvailableRoomTypeResponse> rawRooms = new ArrayList<>();
        Map<Integer, Integer> hotelIdByHrt = new HashMap<>();
        Map<Integer, String> hotelNameByHrt = new HashMap<>();
        Map<Integer, String> currencyByHrt = new HashMap<>();

        boolean catalogComplete = true;
        List<CoverageReason> coverageReasons = new ArrayList<>();
        Set<Integer> failedRoomTypeIds = new HashSet<>();

        try {
            if (criteria.getHotelId() != null && criteria.getHotelId() > 0) {
                BookingSearchRequest req = new BookingSearchRequest();
                req.setHotelId(criteria.getHotelId());
                req.setCheckInDate(criteria.getCheckInDate());
                req.setCheckOutDate(criteria.getCheckOutDate());
                req.setRoomsCount(1);
                req.normalize();

                List<AvailableRoomTypeResponse> rooms = publicBookingSearchService.searchRooms(req);
                if (rooms != null) {
                    for (AvailableRoomTypeResponse r : rooms) {
                        if (rawRooms.size() >= limit) {
                            catalogComplete = false;
                            coverageReasons.add(CoverageReason.EVALUATION_LIMIT);
                            break;
                        }
                        rawRooms.add(r);
                        hotelIdByHrt.put(r.getHotelRoomTypeId(), criteria.getHotelId());
                        hotelNameByHrt.put(r.getHotelRoomTypeId(), "Khách sạn " + criteria.getHotelId());
                    }
                }
            } else if (regionId != null && regionId > 0) {
                BookingSearchRequest hReq = new BookingSearchRequest();
                hReq.setRegionId(regionId);
                hReq.setCheckInDate(criteria.getCheckInDate());
                hReq.setCheckOutDate(criteria.getCheckOutDate());
                hReq.setRoomsCount(1);
                hReq.setPage(0);
                hReq.setPageSize(20);
                hReq.normalize();

                PageResponse<AvailableHotelResponse> hotelPage = publicBookingSearchService.searchHotels(hReq);
                if (hotelPage != null && hotelPage.getContent() != null) {
                    for (AvailableHotelResponse hotel : hotelPage.getContent()) {
                        if (rawRooms.size() >= limit) {
                            catalogComplete = false;
                            coverageReasons.add(CoverageReason.EVALUATION_LIMIT);
                            break;
                        }

                        try {
                            BookingSearchRequest rReq = new BookingSearchRequest();
                            rReq.setHotelId(hotel.getHotelId());
                            rReq.setCheckInDate(criteria.getCheckInDate());
                            rReq.setCheckOutDate(criteria.getCheckOutDate());
                            rReq.setRoomsCount(1);
                            rReq.normalize();

                            List<AvailableRoomTypeResponse> rooms = publicBookingSearchService.searchRooms(rReq);
                            if (rooms != null) {
                                for (AvailableRoomTypeResponse r : rooms) {
                                    if (rawRooms.size() >= limit) {
                                        catalogComplete = false;
                                        coverageReasons.add(CoverageReason.EVALUATION_LIMIT);
                                        break;
                                    }
                                    rawRooms.add(r);
                                    hotelIdByHrt.put(r.getHotelRoomTypeId(), hotel.getHotelId());
                                    hotelNameByHrt.put(r.getHotelRoomTypeId(), hotel.getHotelName());
                                    if (hotel.getCurrency() != null && !hotel.getCurrency().isBlank()) {
                                        currencyByHrt.put(r.getHotelRoomTypeId(), hotel.getCurrency());
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            log.warn("Failed to fetch rooms for hotelId {}: {}", hotel.getHotelId(), ex.getMessage());
                            catalogComplete = false;
                            coverageReasons.add(CoverageReason.CATALOG_PAGE_FAILED);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Core upstream search failed: {}", e.getMessage(), e);
            return CoreCandidateDiscoveryResult.builder()
                    .allFailed(true)
                    .catalogComplete(false)
                    .coverageReasons(List.of(CoverageReason.PRICE_FAILED))
                    .build();
        }

        // Map responses to Candidate, Availability, and PriceQuote
        List<RoomCandidate> candidateList = new ArrayList<>();
        Map<Integer, List<CandidateAvailability>> availabilityMap = new HashMap<>();
        Map<Integer, PriceQuoteDto> quoteMap = new HashMap<>();

        for (AvailableRoomTypeResponse r : rawRooms) {
            if (r == null || r.getHotelRoomTypeId() == null) {
                continue;
            }

            Integer hrtId = r.getHotelRoomTypeId();
            Integer hotelId = hotelIdByHrt.getOrDefault(hrtId, criteria.getHotelId() != null ? criteria.getHotelId() : 1);
            String hotelName = hotelNameByHrt.getOrDefault(hrtId, "Khách sạn " + hotelId);

            // Determine if recalculation of room quote with surcharges is needed
            PriceBreakdownDto activePb = r.getPriceBreakdown();
            boolean needsQuoteRecalc = needsDetailedQuote(criteria, r);

            if (needsQuoteRecalc) {
                try {
                    RoomPriceQuoteRequest quoteReq = RoomPriceQuoteRequest.builder()
                            .hotelRoomTypeId(hrtId)
                            .checkInDate(criteria.getCheckInDate())
                            .checkOutDate(criteria.getCheckOutDate())
                            .adults(criteria.getAdults() != null ? criteria.getAdults().shortValue() : (short) 1)
                            .children(criteria.getChildren() != null ? criteria.getChildren().shortValue() : (short) 0)
                            .infants(criteria.getInfants() != null ? criteria.getInfants().shortValue() : (short) 0)
                            .extraBeds(criteria.getExtraBeds() != null ? criteria.getExtraBeds().shortValue() : (short) 0)
                            .build();

                    RoomPriceQuoteResponse quoteResp = publicBookingSearchService.calculateRoomQuote(quoteReq);
                    if (quoteResp != null && quoteResp.getPriceBreakdown() != null) {
                        activePb = quoteResp.getPriceBreakdown();
                    } else {
                        activePb = null;
                        failedRoomTypeIds.add(hrtId);
                        coverageReasons.add(CoverageReason.PRICE_FAILED);
                    }
                } catch (Exception e) {
                    log.warn("calculateRoomQuote failed for hrtId {}: {}", hrtId, e.getMessage());
                    failedRoomTypeIds.add(hrtId);
                    coverageReasons.add(CoverageReason.PRICE_FAILED);
                    activePb = null;
                }
            }

            if (activePb == null || activePb.getFinalTotalAmount() == null) {
                failedRoomTypeIds.add(hrtId);
                continue;
            }

            // Features mapping
            List<RoomFeatureDto> features = new ArrayList<>();
            if (r.getAmenities() != null) {
                for (RoomAmenityDto a : r.getAmenities()) {
                    if (a != null && a.getCode() != null) {
                        features.add(new RoomFeatureDto(a.getCode(), a.getName()));
                    }
                }
            }
            if (r.getInclusions() != null) {
                for (RoomInclusionDto inc : r.getInclusions()) {
                    if (inc != null && inc.getName() != null) {
                        String code = (inc.getReferenceType() != null ? inc.getReferenceType() : "INCLUSION") + "_" + inc.getId();
                        features.add(new RoomFeatureDto(code, inc.getName()));
                    }
                }
            }

            // RoomCandidate - do not fabricate capacity defaults
            RoomCandidate candidate = RoomCandidate.builder()
                    .hotelId(hotelId)
                    .hotelName(hotelName)
                    .hotelRoomTypeId(hrtId)
                    .roomTypeCode(r.getRoomTypeCode())
                    .roomTypeName(r.getRoomTypeName())
                    .standardAdults(r.getStandardAdults() != null ? r.getStandardAdults().intValue() : null)
                    .standardChildren(r.getStandardChildren() != null ? r.getStandardChildren().intValue() : null)
                    .maxAdults(r.getMaxAdults() != null ? r.getMaxAdults().intValue() : null)
                    .maxChildren(r.getMaxChildren() != null ? r.getMaxChildren().intValue() : null)
                    .maxInfants(r.getMaxInfants() != null ? r.getMaxInfants().intValue() : null)
                    .maxTotalGuests(r.getMaxTotalGuests() != null ? r.getMaxTotalGuests().intValue() : null)
                    .extraBedsAllowed(r.getExtraBeds() != null ? r.getExtraBeds().intValue() : null)
                    .extraBeds(r.getExtraBeds() != null ? r.getExtraBeds().intValue() : null)
                    .features(features)
                    .status("ACTIVE")
                    .build();

            candidateList.add(candidate);

            // CandidateAvailability: continuous availableCount for all nights, breakdown unknown -> null
            int availableRooms = r.getAvailableRooms() != null ? r.getAvailableRooms() : 0;
            List<CandidateAvailability> avList = new ArrayList<>();
            LocalDate cur = criteria.getCheckInDate();
            while (cur.isBefore(criteria.getCheckOutDate())) {
                avList.add(CandidateAvailability.builder()
                        .hotelRoomTypeId(hrtId)
                        .availabilityDate(cur)
                        .totalRooms(null)
                        .bookedRooms(null)
                        .lockedRooms(null)
                        .oooRooms(null)
                        .availableCount(availableRooms)
                        .build());
                cur = cur.plusDays(1);
            }
            availabilityMap.put(hrtId, avList);

            // PriceQuoteDto mapping: authoritative fields, transparent displayLabel, BACKEND_QUOTED_TOTAL
            String quoteCurrency = currencyByHrt.get(hrtId);

            ServiceFeeStatus feeStatus = activePb.getTotalRoomServiceFee() != null
                    ? ServiceFeeStatus.INCLUDED
                    : ServiceFeeStatus.UNKNOWN;

            String feeAmount = activePb.getTotalRoomServiceFee() != null
                    ? activePb.getTotalRoomServiceFee().toPlainString()
                    : null;

            String preTax = activePb.getTotalRoomNetAmount() != null
                    ? activePb.getTotalRoomNetAmount().toPlainString()
                    : (activePb.getPreTaxAmount() != null ? activePb.getPreTaxAmount().toPlainString() : null);

            String vatPercent = activePb.getVatPercent() != null
                    ? activePb.getVatPercent().toPlainString()
                    : null;

            String vatAmount = activePb.getTotalRoomVat() != null
                    ? activePb.getTotalRoomVat().toPlainString()
                    : (activePb.getVatAmount() != null ? activePb.getVatAmount().toPlainString() : null);

            PriceQuoteDto quote = PriceQuoteDto.builder()
                    .hotelRoomTypeId(hrtId)
                    .source("BACKEND_PRICING")
                    .quoteId("quote-" + hrtId + "-" + System.currentTimeMillis())
                    .observedAt(OffsetDateTime.now())
                    .totalNights((int) totalNights)
                    .currency(quoteCurrency)
                    .preTaxAmount(preTax)
                    .vatPercent(vatPercent)
                    .vatAmount(vatAmount)
                    .finalTotalAmount(activePb.getFinalTotalAmount().toPlainString())
                    .serviceFeeStatus(feeStatus)
                    .serviceFeeAmount(feeAmount)
                    .chargeCoverage(BudgetCoverage.BACKEND_QUOTED_TOTAL)
                    .budgetCheck(BudgetCheck.NOT_REQUESTED)
                    .displayLabel("Giá ước tính từ Core (gồm tiền phòng, thuế/phí theo báo giá; chưa bảo đảm toàn bộ phụ phí bắt buộc ngoài hệ thống).")
                    .build();

            quoteMap.put(hrtId, quote);
        }

        return CoreCandidateDiscoveryResult.builder()
                .candidates(candidateList)
                .availabilityByRoomTypeId(availabilityMap)
                .quoteByRoomTypeId(quoteMap)
                .catalogComplete(catalogComplete)
                .failedRoomTypeIds(failedRoomTypeIds)
                .coverageReasons(coverageReasons)
                .totalDiscoveredCount(rawRooms.size())
                .allFailed(false)
                .build();
    }

    private boolean needsDetailedQuote(RoomSearchCriteriaDto criteria, AvailableRoomTypeResponse r) {
        int reqAdults = criteria.getAdults() != null ? criteria.getAdults() : 1;
        int reqChildren = criteria.getChildren() != null ? criteria.getChildren() : 0;
        int reqInfants = criteria.getInfants() != null ? criteria.getInfants() : 0;
        int reqExtraBeds = criteria.getExtraBeds() != null ? criteria.getExtraBeds() : 0;

        int stdAdults = r.getStandardAdults() != null ? r.getStandardAdults().intValue() : 1;
        int stdChildren = r.getStandardChildren() != null ? r.getStandardChildren().intValue() : 0;

        return reqAdults > stdAdults || reqChildren > stdChildren || reqInfants > 0 || reqExtraBeds > 0;
    }
}
