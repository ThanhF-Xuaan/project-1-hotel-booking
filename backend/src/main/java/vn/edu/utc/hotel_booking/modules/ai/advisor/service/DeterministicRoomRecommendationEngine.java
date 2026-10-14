package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.CandidateAvailability;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.RoomCandidate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeterministicRoomRecommendationEngine {

    public AdvisorResultDto evaluateAndRank(
            UUID requestId,
            UUID sessionId,
            long sessionVersion,
            RoomSearchCriteriaDto criteria,
            List<CriteriaEvidenceDto> provenance,
            List<RoomCandidate> candidates,
            Map<Integer, List<CandidateAvailability>> availabilityByRoomTypeId,
            Map<Integer, PriceQuoteDto> quoteByRoomTypeId,
            boolean catalogComplete,
            Set<Integer> failedRoomTypeIds,
            List<CoverageReason> coverageReasons
    ) {
        if (requestId == null) {
            requestId = UUID.randomUUID();
        }
        if (sessionId == null) {
            sessionId = UUID.randomUUID();
        }

        List<CriteriaEvidenceDto> finalProvenance = (provenance != null)
                ? new ArrayList<>(provenance)
                : new ArrayList<>();

        if (candidates == null || candidates.isEmpty()) {
            SearchCoverageDto coverage = SearchCoverageDto.builder()
                    .status(catalogComplete ? CoverageStatus.COMPLETE : CoverageStatus.PARTIAL)
                    .catalogComplete(catalogComplete)
                    .discoveredCount(0)
                    .evaluatedCount(0)
                    .failedCount(failedRoomTypeIds != null ? failedRoomTypeIds.size() : 0)
                    .remainingCount(0)
                    .verifiedMatchCount(0)
                    .reasons(coverageReasons != null ? new ArrayList<>(coverageReasons) : new ArrayList<>())
                    .build();

            List<String> warnings = buildDefaultWarnings(coverage.getStatus());

            return AdvisorResultDto.builder()
                    .contractVersion("2.0")
                    .requestId(requestId)
                    .sessionId(sessionId)
                    .sessionVersion(sessionVersion)
                    .status(catalogComplete ? AdvisorStatus.NO_MATCH : AdvisorStatus.TEMP_UNAVAILABLE)
                    .reply(catalogComplete
                            ? "Chưa tìm thấy phòng phù hợp trong phạm vi đã kiểm tra đầy đủ. Có thể điều chỉnh nhu cầu."
                            : "Chưa kiểm tra được đầy đủ các phương án. Cần thử lại để có thêm kết quả.")
                    .criteria(criteria)
                    .missingFields(new ArrayList<>())
                    .recommendations(new ArrayList<>())
                    .coverage(coverage)
                    .knowledgeStatus("DISABLED")
                    .sources(Collections.emptyList())
                    .warnings(warnings)
                    .criteriaProvenance(finalProvenance)
                    .ambiguities(new ArrayList<>())
                    .build();
        }

        int discoveredCount = candidates.size();
        int failedCount = failedRoomTypeIds != null ? failedRoomTypeIds.size() : 0;
        int evaluatedCount = discoveredCount;
        int remainingCount = discoveredCount - evaluatedCount;

        long totalNights = ChronoUnit.DAYS.between(criteria.getCheckInDate(), criteria.getCheckOutDate());

        // 1. Hard filters
        List<CandidateEvaluation> qualifying = new ArrayList<>();

        for (RoomCandidate candidate : candidates) {
            if (candidate == null || candidate.getHotelRoomTypeId() == null) {
                continue;
            }

            Integer hrtId = candidate.getHotelRoomTypeId();

            if (failedRoomTypeIds != null && failedRoomTypeIds.contains(hrtId)) {
                continue;
            }

            // Public active check
            if (!"ACTIVE".equalsIgnoreCase(candidate.getStatus())) {
                continue;
            }

            // Scope check: hotelId if specified
            if (criteria.getHotelId() != null && !criteria.getHotelId().equals(candidate.getHotelId())) {
                continue;
            }

            // Capacity checks
            if (!passesCapacity(criteria, candidate)) {
                continue;
            }

            // Nightly availability check
            if (!passesAvailability(criteria, candidate, totalNights, availabilityByRoomTypeId.get(hrtId))) {
                continue;
            }

            // Required features check
            if (!passesRequiredFeatures(criteria, candidate)) {
                continue;
            }

            // Price quote check
            PriceQuoteDto quote = quoteByRoomTypeId.get(hrtId);
            if (quote == null || quote.getTotalNights() == null || quote.getTotalNights() != totalNights) {
                continue;
            }

            // Budget check
            BudgetCheckResult budgetResult = checkBudget(criteria.getBudget(), quote, totalNights);
            if (criteria.getBudget() != null && criteria.getBudget().isHard()) {
                if (budgetResult.check != BudgetCheck.PASS) {
                    continue; // Rejected by hard budget constraint
                }
            }

            qualifying.add(new CandidateEvaluation(candidate, quote, budgetResult));
        }

        // 2. Deterministic Lexicographic Ranking (PROPOSED_LEXICOGRAPHIC_V1)
        qualifying.sort((a, b) -> {
            // (1) Preferred feature count DESC
            int prefA = countPreferredFeatures(criteria, a.candidate);
            int prefB = countPreferredFeatures(criteria, b.candidate);
            if (prefA != prefB) {
                return Integer.compare(prefB, prefA);
            }

            // (2) Budget distance ASC
            BigDecimal distA = a.budgetResult.distance;
            BigDecimal distB = b.budgetResult.distance;
            int distComp = distA.compareTo(distB);
            if (distComp != 0) {
                return distComp;
            }

            // (3) Final total amount ASC
            BigDecimal priceA = new BigDecimal(a.quote.getFinalTotalAmount());
            BigDecimal priceB = new BigDecimal(b.quote.getFinalTotalAmount());
            int priceComp = priceA.compareTo(priceB);
            if (priceComp != 0) {
                return priceComp;
            }

            // (4) Deterministic tie-breaker: hotelRoomTypeId ASC
            return Integer.compare(a.candidate.getHotelRoomTypeId(), b.candidate.getHotelRoomTypeId());
        });

        int verifiedMatchCount = qualifying.size();

        // 3. Recommendation cards builder (up to top 5)
        List<RoomRecommendationDto> recommendations = new ArrayList<>();
        int rank = 1;
        for (CandidateEvaluation item : qualifying) {
            if (rank > 5) {
                break;
            }
            recommendations.add(buildRecommendationCard(rank, item, criteria, totalNights));
            rank++;
        }

        // 4. Determine search coverage & status
        CoverageStatus coverageStatus;
        if (catalogComplete && failedCount == 0) {
            coverageStatus = CoverageStatus.COMPLETE;
        } else {
            coverageStatus = CoverageStatus.PARTIAL;
        }

        List<CoverageReason> reasonsList = new ArrayList<>();
        if (coverageReasons != null) {
            reasonsList.addAll(coverageReasons);
        }
        if (coverageStatus == CoverageStatus.PARTIAL && reasonsList.isEmpty()) {
            if (failedCount > 0) {
                reasonsList.add(CoverageReason.PRICE_FAILED);
            } else if (!catalogComplete) {
                reasonsList.add(CoverageReason.CATALOG_PAGE_FAILED);
            }
        }

        SearchCoverageDto coverage = SearchCoverageDto.builder()
                .status(coverageStatus)
                .catalogComplete(catalogComplete)
                .discoveredCount(discoveredCount)
                .evaluatedCount(evaluatedCount)
                .failedCount(failedCount)
                .remainingCount(remainingCount)
                .verifiedMatchCount(verifiedMatchCount)
                .reasons(reasonsList)
                .build();

        List<String> warnings = buildDefaultWarnings(coverageStatus);

        AdvisorStatus outcomeStatus;
        String reply;

        if (coverageStatus == CoverageStatus.COMPLETE) {
            if (verifiedMatchCount > 0) {
                outcomeStatus = AdvisorStatus.RESULTS;
                reply = "Có thể xem chi tiết các phương án phòng. Giá và tồn phòng cần kiểm tra lại.";
            } else {
                outcomeStatus = AdvisorStatus.NO_MATCH;
                reply = "Chưa tìm thấy phòng phù hợp trong phạm vi đã kiểm tra đầy đủ. Có thể điều chỉnh nhu cầu.";
            }
        } else {
            if (verifiedMatchCount > 0) {
                outcomeStatus = AdvisorStatus.RESULTS;
                reply = "Có thể xem chi tiết các phương án phòng. Giá và tồn phòng cần kiểm tra lại.";
            } else if (failedCount == discoveredCount) {
                outcomeStatus = AdvisorStatus.TEMP_UNAVAILABLE;
                reply = "Chưa kiểm tra được đầy đủ các phương án. Cần thử lại để có thêm kết quả.";
            } else {
                outcomeStatus = AdvisorStatus.INCOMPLETE_SEARCH;
                reply = "Chưa kiểm tra được đầy đủ các phương án. Cần thử lại để có thêm kết quả.";
            }
        }

        return AdvisorResultDto.builder()
                .contractVersion("2.0")
                .requestId(requestId)
                .sessionId(sessionId)
                .sessionVersion(sessionVersion)
                .status(outcomeStatus)
                .reply(reply)
                .criteria(criteria)
                .missingFields(new ArrayList<>())
                .recommendations(recommendations)
                .coverage(coverage)
                .knowledgeStatus("DISABLED")
                .sources(Collections.emptyList())
                .warnings(warnings)
                .criteriaProvenance(finalProvenance)
                .ambiguities(new ArrayList<>())
                .build();
    }

    private boolean passesCapacity(RoomSearchCriteriaDto criteria, RoomCandidate candidate) {
        int reqAdults = criteria.getAdults() != null ? criteria.getAdults() : 1;
        int reqChildren = criteria.getChildren() != null ? criteria.getChildren() : 0;
        int reqInfants = criteria.getInfants() != null ? criteria.getInfants() : 0;
        int reqExtraBeds = criteria.getExtraBeds() != null ? criteria.getExtraBeds() : 0;

        int maxAdults = candidate.getMaxAdults() != null ? candidate.getMaxAdults() : 0;
        int maxChildren = candidate.getMaxChildren() != null ? candidate.getMaxChildren() : 0;
        int maxInfants = candidate.getMaxInfants() != null ? candidate.getMaxInfants() : Integer.MAX_VALUE;
        int maxTotalGuests = candidate.getMaxTotalGuests() != null ? candidate.getMaxTotalGuests() : 0;
        int extraBedsAllowed = candidate.getExtraBedsAllowed() != null ? candidate.getExtraBedsAllowed() : 0;

        if (reqAdults > maxAdults) {
            return false;
        }
        if (reqChildren > maxChildren) {
            return false;
        }
        if (reqInfants > maxInfants) {
            return false;
        }
        if ((reqAdults + reqChildren + reqInfants) > maxTotalGuests) {
            return false;
        }
        if (reqExtraBeds > extraBedsAllowed) {
            return false;
        }

        return true;
    }

    private boolean passesAvailability(
            RoomSearchCriteriaDto criteria,
            RoomCandidate candidate,
            long totalNights,
            List<CandidateAvailability> availabilities
    ) {
        if (availabilities == null || availabilities.isEmpty()) {
            return false;
        }

        Map<LocalDate, Integer> countByDate = new HashMap<>();
        for (CandidateAvailability item : availabilities) {
            if (item != null && item.getAvailabilityDate() != null) {
                countByDate.put(item.getAvailabilityDate(), item.getAvailableCount() != null ? item.getAvailableCount() : 0);
            }
        }

        LocalDate current = criteria.getCheckInDate();
        LocalDate end = criteria.getCheckOutDate();

        while (current.isBefore(end)) {
            Integer count = countByDate.get(current);
            if (count == null || count < 1) {
                return false;
            }
            current = current.plusDays(1);
        }

        return true;
    }

    private boolean passesRequiredFeatures(RoomSearchCriteriaDto criteria, RoomCandidate candidate) {
        if (criteria.getRequiredFeatureCodes() == null || criteria.getRequiredFeatureCodes().isEmpty()) {
            return true;
        }

        Set<String> candidateCodes = (candidate.getFeatures() != null)
                ? candidate.getFeatures().stream()
                .filter(f -> f != null && f.getCode() != null)
                .map(f -> f.getCode().toUpperCase())
                .collect(Collectors.toSet())
                : Collections.emptySet();

        for (String req : criteria.getRequiredFeatureCodes()) {
            if (req != null && !candidateCodes.contains(req.toUpperCase())) {
                return false;
            }
        }

        return true;
    }

    private int countPreferredFeatures(RoomSearchCriteriaDto criteria, RoomCandidate candidate) {
        if (criteria.getPreferredFeatureCodes() == null || criteria.getPreferredFeatureCodes().isEmpty()) {
            return 0;
        }

        Set<String> candidateCodes = (candidate.getFeatures() != null)
                ? candidate.getFeatures().stream()
                .filter(f -> f != null && f.getCode() != null)
                .map(f -> f.getCode().toUpperCase())
                .collect(Collectors.toSet())
                : Collections.emptySet();

        int match = 0;
        for (String pref : criteria.getPreferredFeatureCodes()) {
            if (pref != null && candidateCodes.contains(pref.toUpperCase())) {
                match++;
            }
        }
        return match;
    }

    private BudgetCheckResult checkBudget(BudgetDto budget, PriceQuoteDto quote, long totalNights) {
        if (budget == null) {
            return new BudgetCheckResult(BudgetCheck.NOT_REQUESTED, BigDecimal.ZERO);
        }

        // Currency verification
        if (quote.getCurrency() == null || !quote.getCurrency().equalsIgnoreCase(budget.getCurrency())) {
            return new BudgetCheckResult(BudgetCheck.UNVERIFIABLE, BigDecimal.ZERO);
        }

        // Coverage verification: ALL_MANDATORY_CHARGES requires backend confirmation
        if (budget.getCoverage() == BudgetCoverage.ALL_MANDATORY_CHARGES) {
            if (quote.getChargeCoverage() != BudgetCoverage.ALL_MANDATORY_CHARGES ||
                    quote.getServiceFeeStatus() != ServiceFeeStatus.INCLUDED) {
                return new BudgetCheckResult(BudgetCheck.UNVERIFIABLE, BigDecimal.ZERO);
            }
        }

        BigDecimal quoteAmount = new BigDecimal(quote.getFinalTotalAmount());
        BigDecimal budgetAmount = new BigDecimal(budget.getAmount());

        BigDecimal applicableAmount;
        if (budget.getBasis() == BudgetBasis.STAY_TOTAL) {
            applicableAmount = quoteAmount;
        } else if (budget.getBasis() == BudgetBasis.AVERAGE_PER_NIGHT) {
            if (totalNights <= 0) {
                return new BudgetCheckResult(BudgetCheck.UNVERIFIABLE, BigDecimal.ZERO);
            }
            applicableAmount = quoteAmount.divide(BigDecimal.valueOf(totalNights), 4, RoundingMode.HALF_UP);
        } else {
            // EACH_NIGHT requires authoritative per-night breakdown which is not provided in single total
            return new BudgetCheckResult(BudgetCheck.UNVERIFIABLE, BigDecimal.ZERO);
        }

        if (applicableAmount.compareTo(budgetAmount) <= 0) {
            return new BudgetCheckResult(BudgetCheck.PASS, BigDecimal.ZERO);
        } else {
            // Over budget
            BigDecimal distance = applicableAmount.subtract(budgetAmount)
                    .divide(budgetAmount, 4, RoundingMode.HALF_UP);
            return new BudgetCheckResult(BudgetCheck.UNVERIFIABLE, distance);
        }
    }

    private RoomRecommendationDto buildRecommendationCard(
            int rank,
            CandidateEvaluation item,
            RoomSearchCriteriaDto criteria,
            long totalNights
    ) {
        RoomCandidate c = item.candidate;
        PriceQuoteDto origQuote = item.quote;

        PriceQuoteDto cardQuote = PriceQuoteDto.builder()
                .hotelRoomTypeId(origQuote.getHotelRoomTypeId())
                .source("BACKEND_PRICING")
                .quoteId(origQuote.getQuoteId())
                .observedAt(origQuote.getObservedAt())
                .totalNights(origQuote.getTotalNights())
                .currency(origQuote.getCurrency())
                .preTaxAmount(origQuote.getPreTaxAmount())
                .vatPercent(origQuote.getVatPercent())
                .vatAmount(origQuote.getVatAmount())
                .finalTotalAmount(origQuote.getFinalTotalAmount())
                .serviceFeeStatus(origQuote.getServiceFeeStatus())
                .serviceFeeAmount(origQuote.getServiceFeeAmount())
                .chargeCoverage(origQuote.getChargeCoverage())
                .budgetCheck(item.budgetResult.check)
                .displayLabel(origQuote.getDisplayLabel())
                .build();

        RoomSummaryDto roomSummary = RoomSummaryDto.builder()
                .hotelId(c.getHotelId())
                .hotelName(c.getHotelName())
                .hotelRoomTypeId(c.getHotelRoomTypeId())
                .roomTypeId(c.getRoomTypeId())
                .roomTypeCode(c.getRoomTypeCode())
                .roomTypeName(c.getRoomTypeName())
                .maxAdults(c.getMaxAdults())
                .maxChildren(c.getMaxChildren())
                .maxTotalGuests(c.getMaxTotalGuests())
                .extraBedsAllowed(c.getExtraBedsAllowed())
                .features(c.getFeatures() != null ? new ArrayList<>(c.getFeatures()) : new ArrayList<>())
                .build();

        List<RecommendationReasonDto> reasons = new ArrayList<>();
        reasons.add(RecommendationReasonDto.builder()
                .code(ReasonCode.AVAILABLE_ALL_NIGHTS)
                .text("Còn phòng trong cả hai đêm theo API.")
                .evidenceFields(List.of("availability.availableCount"))
                .build());

        int prefCount = countPreferredFeatures(criteria, c);
        if (prefCount > 0) {
            reasons.add(RecommendationReasonDto.builder()
                    .code(ReasonCode.PREFERRED_FEATURE_MATCH)
                    .text("Đáp ứng " + prefCount + " tiện ích ưu tiên.")
                    .evidenceFields(List.of("room.features"))
                    .build());
        }

        if (item.budgetResult.check == BudgetCheck.PASS) {
            reasons.add(RecommendationReasonDto.builder()
                    .code(ReasonCode.BUDGET_MATCH)
                    .text("Nằm trong ngân sách dự kiến.")
                    .evidenceFields(List.of("price.finalTotalAmount", "criteria.budget.amount"))
                    .build());
        }

        StayIntervalDto stay = StayIntervalDto.builder()
                .checkInDate(criteria.getCheckInDate())
                .checkOutDate(criteria.getCheckOutDate())
                .roomsCount(1)
                .build();

        RecommendationActionDto action = RecommendationActionDto.builder()
                .type("VIEW_ROOM_DETAILS")
                .path("/portal/hotels/" + c.getHotelId() + "/room-types/" + c.getHotelRoomTypeId())
                .build();

        return RoomRecommendationDto.builder()
                .recommendationId("synthetic-rec-" + c.getHotelRoomTypeId())
                .rank(rank)
                .room(roomSummary)
                .price(cardQuote)
                .reasons(reasons)
                .action(action)
                .stay(stay)
                .build();
    }

    private List<String> buildDefaultWarnings(CoverageStatus coverageStatus) {
        List<String> warnings = new ArrayList<>();
        warnings.add(WarningCode.SERVICE_FEE_UNVERIFIED);
        warnings.add(WarningCode.CURRENCY_SYNTHETIC_ASSUMPTION);
        if (coverageStatus == CoverageStatus.PARTIAL) {
            warnings.add(WarningCode.INCOMPLETE_COVERAGE);
        }
        return warnings;
    }

    private static class CandidateEvaluation {
        final RoomCandidate candidate;
        final PriceQuoteDto quote;
        final BudgetCheckResult budgetResult;

        CandidateEvaluation(RoomCandidate candidate, PriceQuoteDto quote, BudgetCheckResult budgetResult) {
            this.candidate = candidate;
            this.quote = quote;
            this.budgetResult = budgetResult;
        }
    }

    private static class BudgetCheckResult {
        final BudgetCheck check;
        final BigDecimal distance;

        BudgetCheckResult(BudgetCheck check, BigDecimal distance) {
            this.check = check;
            this.distance = distance != null ? distance : BigDecimal.ZERO;
        }
    }
}
