package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ComparisonFocus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ComparisonStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagCitationDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagPublicQueryFacade;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagPublicQueryOutcome;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic multi-criteria room comparison and trade-off analysis engine.
 *
 * <p>All prices, deltas, percentages, capacities, and amenities originate strictly from verified Core
 * {@link RoomRecommendationDto} records. Approved RAG {@code STATIC_ROOM_DESCRIPTION} chunks enrich only
 * static spatial context (area/layout), queried at most once per unique hotel ID.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomComparisonService {

    private static final int MAX_COMPARISON_ROOMS = 3;
    private static final Pattern ROOM_LINE_CODE_PREFIX = Pattern.compile("^\\[([A-Z0-9_-]+)]\\s*[^:]*:\\s*(.+)$");
    private static final Pattern AREA_SQM_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:m2|m²|mét\\s*vuông)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final RagPublicQueryFacade ragFacade;

    public RoomComparisonMatrixDto compareRooms(
            AdvisorResultDto advisorResult,
            List<String> targetRoomHints,
            ComparisonFocus focus,
            List<Integer> compareRoomTypeIds
    ) {
        ComparisonFocus safeFocus = focus != null ? focus : ComparisonFocus.GENERAL;

        List<RoomRecommendationDto> available = advisorResult != null && advisorResult.getRecommendations() != null
                ? advisorResult.getRecommendations().stream().filter(Objects::nonNull).toList()
                : List.of();

        if (available.size() < 2) {
            boolean needsSearch = advisorResult == null || advisorResult.getRecommendations() == null
                    || available.isEmpty();
            return RoomComparisonMatrixDto.builder()
                    .status(needsSearch ? ComparisonStatus.NEED_SEARCH_FIRST : ComparisonStatus.INSUFFICIENT_CANDIDATES)
                    .focus(safeFocus)
                    .tradeOffSummary(needsSearch
                            ? "Hiện chưa có kết quả tìm phòng trong phiên để đối chiếu giá và sức chứa. Quý khách vui lòng cho biết địa điểm, ngày nhận/trả phòng và số khách trước nhé."
                            : "Hiện chỉ có 1 hạng phòng phù hợp trong kết quả tìm kiếm nên chưa đủ tối thiểu 2 phòng để lập bảng so sánh.")
                    .build();
        }

        List<RoomRecommendationDto> selected = resolveRoomsToCompare(available, targetRoomHints, compareRoomTypeIds);
        if (selected.size() < 2) {
            return RoomComparisonMatrixDto.builder()
                    .status(ComparisonStatus.INSUFFICIENT_CANDIDATES)
                    .focus(safeFocus)
                    .tradeOffSummary("Cần ít nhất 2 hạng phòng khác nhau để lập bảng đối chiếu chi tiết.")
                    .build();
        }

        // 1. Query RAG STATIC_ROOM_DESCRIPTION at most once per unique hotelId
        Map<Integer, String> hotelStaticTextMap = new LinkedHashMap<>();
        List<RagCitationDto> staticCitations = new ArrayList<>();
        fetchStaticDescriptionsOncePerHotel(selected, hotelStaticTextMap, staticCitations);

        // 2. Shared vs. Unique features
        List<String> sharedFeatures = computeSharedFeatures(selected);

        // 3. Parse prices and verify uniform currency & totalNights
        Map<RoomRecommendationDto, BigDecimal> parsedAmounts = new IdentityHashMap<>();
        boolean uniformPricing = checkUniformPricing(selected, parsedAmounts);

        BigDecimal cheapestAmount = null;
        BigDecimal mostExpensiveAmount = null;
        if (uniformPricing) {
            for (BigDecimal amt : parsedAmounts.values()) {
                if (cheapestAmount == null || amt.compareTo(cheapestAmount) < 0) {
                    cheapestAmount = amt;
                }
                if (mostExpensiveAmount == null || amt.compareTo(mostExpensiveAmount) > 0) {
                    mostExpensiveAmount = amt;
                }
            }
        }

        // 4. Build item columns
        List<RoomComparisonItemDto> items = new ArrayList<>(selected.size());
        for (RoomRecommendationDto rec : selected) {
            RoomSummaryDto room = rec.getRoom();
            PriceQuoteDto price = rec.getPrice();
            List<String> roomFeatures = extractFeatureNames(room);
            List<String> uniqueFeatures = roomFeatures.stream()
                    .filter(f -> !sharedFeatures.contains(f))
                    .toList();

            String staticDesc = extractStaticRoomDescription(room, hotelStaticTextMap);

            BigDecimal amount = parsedAmounts.get(rec);
            String perNightAvg = null;
            String deltaStr = null;
            BigDecimal diffPct = null;

            if (uniformPricing && amount != null && price != null && price.getTotalNights() != null && price.getTotalNights() > 0) {
                int scale = "VND".equalsIgnoreCase(price.getCurrency()) ? 0 : 2;
                perNightAvg = amount.divide(BigDecimal.valueOf(price.getTotalNights()), scale, RoundingMode.HALF_UP)
                        .toPlainString();
                if (cheapestAmount != null) {
                    BigDecimal delta = amount.subtract(cheapestAmount);
                    deltaStr = delta.toPlainString();
                    if (cheapestAmount.compareTo(BigDecimal.ZERO) > 0) {
                        diffPct = delta.multiply(BigDecimal.valueOf(100))
                                .divide(cheapestAmount, 1, RoundingMode.HALF_UP);
                    }
                }
            }

            List<String> bullets = buildBestForBullets(room, deltaStr, diffPct, uniqueFeatures, staticDesc, safeFocus);

            items.add(RoomComparisonItemDto.builder()
                    .recommendationId(rec.getRecommendationId())
                    .hotelRoomTypeId(room != null ? room.getHotelRoomTypeId() : null)
                    .hotelId(room != null ? room.getHotelId() : null)
                    .hotelName(room != null ? room.getHotelName() : null)
                    .roomTypeCode(room != null ? room.getRoomTypeCode() : null)
                    .roomTypeName(room != null ? room.getRoomTypeName() : null)
                    .quoteId(price != null ? price.getQuoteId() : null)
                    .observedAt(price != null ? price.getObservedAt() : null)
                    .finalTotalAmount(price != null ? price.getFinalTotalAmount() : null)
                    .perNightAverageAmount(perNightAvg)
                    .currency(price != null ? price.getCurrency() : null)
                    .totalNights(price != null ? price.getTotalNights() : null)
                    .chargeCoverage(price != null ? price.getChargeCoverage() : null)
                    .serviceFeeStatus(price != null ? price.getServiceFeeStatus() : null)
                    .budgetCheck(price != null ? price.getBudgetCheck() : null)
                    .priceDeltaFromCheapest(deltaStr)
                    .priceDiffPercentFromCheapest(diffPct)
                    .maxAdults(room != null ? room.getMaxAdults() : null)
                    .maxChildren(room != null ? room.getMaxChildren() : null)
                    .maxTotalGuests(room != null ? room.getMaxTotalGuests() : null)
                    .extraBedsAllowed(room != null ? room.getExtraBedsAllowed() : null)
                    .staticAreaAndLayout(staticDesc)
                    .uniqueFeatureNames(uniqueFeatures)
                    .bestForBullets(bullets)
                    .action(rec.getAction())
                    .build());
        }

        String maxSavingsAmount = null;
        BigDecimal maxSavingsPercent = null;
        if (uniformPricing && cheapestAmount != null && mostExpensiveAmount != null
                && mostExpensiveAmount.compareTo(cheapestAmount) > 0) {
            BigDecimal savings = mostExpensiveAmount.subtract(cheapestAmount);
            maxSavingsAmount = savings.toPlainString();
            if (mostExpensiveAmount.compareTo(BigDecimal.ZERO) > 0) {
                maxSavingsPercent = savings.multiply(BigDecimal.valueOf(100))
                        .divide(mostExpensiveAmount, 1, RoundingMode.HALF_UP);
            }
        }

        String tradeOffSummary = generateTradeOffSummary(items, safeFocus, maxSavingsAmount, maxSavingsPercent);

        return RoomComparisonMatrixDto.builder()
                .status(ComparisonStatus.READY)
                .focus(safeFocus)
                .items(items)
                .sharedFeatureNames(sharedFeatures)
                .maxSavingsAmount(maxSavingsAmount)
                .maxSavingsPercent(maxSavingsPercent)
                .tradeOffSummary(tradeOffSummary)
                .staticDescriptionCitations(staticCitations)
                .build();
    }

    private List<RoomRecommendationDto> resolveRoomsToCompare(
            List<RoomRecommendationDto> available,
            List<String> targetRoomHints,
            List<Integer> compareRoomTypeIds
    ) {
        List<RoomRecommendationDto> selected = new ArrayList<>();
        Set<Integer> usedRoomTypeIds = new HashSet<>();

        // 1. Explicit ID selection (e.g. Quick Compare Chips / UI check)
        if (compareRoomTypeIds != null && !compareRoomTypeIds.isEmpty()) {
            for (Integer id : compareRoomTypeIds) {
                if (id == null) continue;
                for (RoomRecommendationDto rec : available) {
                    Integer hrtId = rec.getRoom() != null ? rec.getRoom().getHotelRoomTypeId() : null;
                    if (Objects.equals(hrtId, id) && usedRoomTypeIds.add(hrtId)) {
                        selected.add(rec);
                        if (selected.size() >= MAX_COMPARISON_ROOMS) {
                            return selected;
                        }
                        break;
                    }
                }
            }
        }

        // 2. Match natural language room hints ("deluxe", "executive", "family", "superior", ...)
        if (selected.isEmpty() && targetRoomHints != null && !targetRoomHints.isEmpty()) {
            List<String> unmatchedHints = new ArrayList<>();
            // Pass 2a: Primary exact matches
            for (String hint : targetRoomHints) {
                boolean matched = false;
                for (RoomRecommendationDto rec : available) {
                    if (rec.getRoom() == null) continue;
                    Integer hrtId = rec.getRoom().getHotelRoomTypeId();
                    if (hrtId != null && usedRoomTypeIds.contains(hrtId)) continue;
                    if (selected.contains(rec)) continue;

                    if (matchesPrimaryRoomHint(rec.getRoom(), hint)) {
                        if (hrtId != null) usedRoomTypeIds.add(hrtId);
                        selected.add(rec);
                        matched = true;
                        if (selected.size() >= MAX_COMPARISON_ROOMS) {
                            return selected;
                        }
                        break;
                    }
                }
                if (!matched) {
                    unmatchedHints.add(hint);
                }
            }

            // Pass 2b: Fallback matches for any still-unmatched hints
            if (selected.size() < MAX_COMPARISON_ROOMS && !unmatchedHints.isEmpty()) {
                for (String hint : unmatchedHints) {
                    for (RoomRecommendationDto rec : available) {
                        if (rec.getRoom() == null) continue;
                        Integer hrtId = rec.getRoom().getHotelRoomTypeId();
                        if (hrtId != null && usedRoomTypeIds.contains(hrtId)) continue;
                        if (selected.contains(rec)) continue;

                        if (matchesFallbackRoomHint(rec.getRoom(), hint)) {
                            if (hrtId != null) usedRoomTypeIds.add(hrtId);
                            selected.add(rec);
                            if (selected.size() >= MAX_COMPARISON_ROOMS) {
                                return selected;
                            }
                            break;
                        }
                    }
                }
            }
        }

        // 3. Pair single match or fallback to top-ranked distinct rooms (up to 3)
        if (selected.size() < 2) {
            int targetLimit = selected.size() == 1 ? 2 : MAX_COMPARISON_ROOMS;
            for (RoomRecommendationDto rec : available) {
                Integer hrtId = rec.getRoom() != null ? rec.getRoom().getHotelRoomTypeId() : null;
                if (hrtId != null && usedRoomTypeIds.contains(hrtId)) continue;
                if (selected.contains(rec)) continue;

                if (hrtId != null) usedRoomTypeIds.add(hrtId);
                selected.add(rec);
                if (selected.size() >= Math.min(targetLimit, available.size())) {
                    break;
                }
            }
        }

        return selected;
    }

    private boolean matchesPrimaryRoomHint(RoomSummaryDto room, String hint) {
        if (room == null || hint == null || hint.isBlank()) {
            return false;
        }
        String h = hint.toLowerCase(Locale.ROOT);
        String name = room.getRoomTypeName() != null ? room.getRoomTypeName().toLowerCase(Locale.ROOT) : "";
        String code = room.getRoomTypeCode() != null ? room.getRoomTypeCode().toLowerCase(Locale.ROOT) : "";

        return switch (h) {
            case "executive", "exe" -> code.equals("exe") || name.contains("executive");
            case "suite", "ste", "sui" -> (code.equals("ste") || code.equals("sui") || name.contains("suite"))
                    && !code.equals("exe") && !name.contains("executive");
            case "deluxe", "dlx" -> code.equals("dlx") || name.contains("deluxe");
            case "family", "fam" -> code.equals("fam") || name.contains("family") || name.contains("gia đình");
            case "superior", "sup" -> code.equals("sup") || name.contains("superior");
            case "standard", "std" -> code.equals("std") || name.contains("standard") || name.contains("tiêu chuẩn");
            default -> (code.equals(h) || name.contains(h))
                    && (!h.equals("suite") || (!code.equals("exe") && !name.contains("executive")));
        };
    }

    private boolean matchesFallbackRoomHint(RoomSummaryDto room, String hint) {
        if (room == null || hint == null || hint.isBlank()) {
            return false;
        }
        String h = hint.toLowerCase(Locale.ROOT);
        String name = room.getRoomTypeName() != null ? room.getRoomTypeName().toLowerCase(Locale.ROOT) : "";
        String code = room.getRoomTypeCode() != null ? room.getRoomTypeCode().toLowerCase(Locale.ROOT) : "";

        if (h.equals("suite") || h.equals("ste") || h.equals("sui")) {
            return code.equals("exe") || name.contains("suite");
        }
        return name.contains(h) || code.contains(h);
    }

    private void fetchStaticDescriptionsOncePerHotel(
            List<RoomRecommendationDto> selected,
            Map<Integer, String> hotelStaticTextMap,
            List<RagCitationDto> staticCitations
    ) {
        Set<Integer> uniqueHotelIds = new LinkedHashSet<>();
        for (RoomRecommendationDto rec : selected) {
            if (rec.getRoom() != null && rec.getRoom().getHotelId() != null) {
                uniqueHotelIds.add(rec.getRoom().getHotelId());
            }
        }

        for (Integer hotelId : uniqueHotelIds) {
            try {
                RagPublicQueryOutcome outcome = ragFacade.ask(
                        "Mô tả diện tích m2 và bố cục các hạng phòng của khách sạn",
                        hotelId
                );
                if (outcome == null || outcome.getState() != RagPublicQueryOutcome.State.ANSWERED || outcome.getResult() == null) {
                    continue;
                }
                if (outcome.getResult().getStatus() != RagAnswerStatus.GROUNDED_ANSWER) {
                    continue;
                }

                List<RagCitationDto> citations = outcome.getResult().getCitations();
                if (citations == null || citations.isEmpty()) {
                    continue;
                }

                List<RagCitationDto> validStaticCitations = citations.stream()
                        .filter(c -> c != null
                                && c.getCategory() == RagDocumentCategory.STATIC_ROOM_DESCRIPTION
                                && (c.getHotelId() == null || Objects.equals(c.getHotelId(), hotelId)))
                        .toList();

                if (!validStaticCitations.isEmpty() && outcome.getResult().getAnswer() != null) {
                    hotelStaticTextMap.put(hotelId, outcome.getResult().getAnswer());
                    staticCitations.addAll(validStaticCitations);
                }
            } catch (Exception ex) {
                log.debug("Optional RAG room description lookup skipped for hotelId={} (type={})",
                        hotelId, ex.getClass().getSimpleName());
            }
        }
    }

    private String extractStaticRoomDescription(RoomSummaryDto room, Map<Integer, String> hotelStaticTextMap) {
        if (room == null || room.getHotelId() == null) {
            return null;
        }
        String docText = hotelStaticTextMap.get(room.getHotelId());
        if (docText == null || docText.isBlank()) {
            return null;
        }
        String code = room.getRoomTypeCode() != null ? room.getRoomTypeCode().trim().toUpperCase(Locale.ROOT) : "";
        String nameLower = room.getRoomTypeName() != null ? room.getRoomTypeName().trim().toLowerCase(Locale.ROOT) : "";
        String[] lines = docText.split("\\R");

        // Pass 1: exact code prefix match [CODE] across all lines
        if (!code.isEmpty()) {
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                Matcher prefixMatcher = ROOM_LINE_CODE_PREFIX.matcher(trimmed);
                if (prefixMatcher.matches()) {
                    String lineCode = prefixMatcher.group(1).toUpperCase(Locale.ROOT);
                    if (lineCode.equals(code)) {
                        return prefixMatcher.group(2).trim();
                    }
                }
            }
        }

        // Pass 2: fallback name match only on lines that do NOT belong to a different room code
        if (!nameLower.isEmpty()) {
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                Matcher prefixMatcher = ROOM_LINE_CODE_PREFIX.matcher(trimmed);
                if (prefixMatcher.matches()) {
                    String lineCode = prefixMatcher.group(1).toUpperCase(Locale.ROOT);
                    if (!code.isEmpty() && !lineCode.equals(code)) {
                        continue; // belongs to another room code, skip
                    }
                }
                if (trimmed.toLowerCase(Locale.ROOT).contains(nameLower)) {
                    int colonIdx = trimmed.indexOf(':');
                    return colonIdx >= 0 && colonIdx + 1 < trimmed.length()
                            ? trimmed.substring(colonIdx + 1).trim()
                            : trimmed;
                }
            }
        }
        return null;
    }

    private List<String> extractFeatureNames(RoomSummaryDto room) {
        if (room == null || room.getFeatures() == null) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (RoomFeatureDto f : room.getFeatures()) {
            if (f != null && f.getName() != null && !f.getName().isBlank()) {
                names.add(f.getName().trim());
            }
        }
        return List.copyOf(names);
    }

    private List<String> computeSharedFeatures(List<RoomRecommendationDto> selected) {
        if (selected.isEmpty()) {
            return List.of();
        }
        List<String> firstFeatures = extractFeatureNames(selected.get(0).getRoom());
        if (firstFeatures.isEmpty()) {
            return List.of();
        }
        Set<String> intersection = new LinkedHashSet<>(firstFeatures);
        for (int i = 1; i < selected.size(); i++) {
            Set<String> nextSet = new HashSet<>(extractFeatureNames(selected.get(i).getRoom()));
            intersection.retainAll(nextSet);
        }
        return List.copyOf(intersection);
    }

    private boolean checkUniformPricing(
            List<RoomRecommendationDto> selected,
            Map<RoomRecommendationDto, BigDecimal> parsedAmounts
    ) {
        String baseCurrency = null;
        Integer baseNights = null;

        for (RoomRecommendationDto rec : selected) {
            PriceQuoteDto price = rec.getPrice();
            if (price == null || price.getCurrency() == null || price.getCurrency().isBlank()
                    || price.getTotalNights() == null || price.getTotalNights() <= 0) {
                return false;
            }
            BigDecimal amount = parseSafeAmount(price.getFinalTotalAmount());
            if (amount == null) {
                return false;
            }
            if (baseCurrency == null) {
                baseCurrency = price.getCurrency();
                baseNights = price.getTotalNights();
            } else if (!baseCurrency.equalsIgnoreCase(price.getCurrency())
                    || !Objects.equals(baseNights, price.getTotalNights())) {
                return false;
            }
            parsedAmounts.put(rec, amount);
        }
        return !parsedAmounts.isEmpty();
    }

    private BigDecimal parseSafeAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> buildBestForBullets(
            RoomSummaryDto room,
            String deltaStr,
            BigDecimal diffPct,
            List<String> uniqueFeatures,
            String staticDesc,
            ComparisonFocus focus
    ) {
        List<String> bullets = new ArrayList<>();
        if (deltaStr != null && new BigDecimal(deltaStr).compareTo(BigDecimal.ZERO) == 0) {
            bullets.add("Phương án tối ưu chi phí nhất trong nhóm đang so sánh");
        } else if (diffPct != null) {
            bullets.add("Hạng phòng nâng cấp (+%s%% so với lựa chọn tiết kiệm nhất)".formatted(diffPct.toPlainString()));
        }

        if (room != null) {
            int children = room.getMaxChildren() != null ? room.getMaxChildren() : 0;
            int extraBeds = room.getExtraBedsAllowed() != null ? room.getExtraBedsAllowed() : 0;
            if (focus == ComparisonFocus.FAMILY_KIDS && (children > 0 || extraBeds > 0)) {
                bullets.add("Sức chứa tối đa %d trẻ em%s".formatted(
                        children,
                        extraBeds > 0 ? ", hỗ trợ kê thêm tối đa %d giường phụ".formatted(extraBeds) : ""
                ));
            } else if (extraBeds > 0) {
                bullets.add("Cho phép kê thêm tối đa %d giường phụ".formatted(extraBeds));
            }
        }

        if (staticDesc != null && !staticDesc.isBlank()) {
            Matcher areaMatcher = AREA_SQM_PATTERN.matcher(staticDesc);
            if (areaMatcher.find()) {
                bullets.add("Không gian %s".formatted(areaMatcher.group(0)));
            }
        }

        if (!uniqueFeatures.isEmpty()) {
            bullets.add("Tiện ích riêng: " + String.join(", ", uniqueFeatures.stream().limit(2).toList()));
        }

        return bullets.stream().limit(3).toList();
    }

    private String generateTradeOffSummary(
            List<RoomComparisonItemDto> items,
            ComparisonFocus focus,
            String maxSavingsAmount,
            BigDecimal maxSavingsPercent
    ) {
        if (items.size() < 2) {
            return "";
        }

        RoomComparisonItemDto budgetPick = items.stream()
                .filter(i -> parseSafeAmount(i.getFinalTotalAmount()) != null)
                .min(Comparator.comparing(i -> parseSafeAmount(i.getFinalTotalAmount())))
                .orElse(items.get(0));

        RoomComparisonItemDto spacePick = items.stream()
                .max(Comparator
                        .comparingInt((RoomComparisonItemDto i) -> extractAreaValue(i.getStaticAreaAndLayout()))
                        .thenComparingInt(i -> i.getMaxTotalGuests() != null ? i.getMaxTotalGuests() : 0)
                        .thenComparingInt(i -> i.getExtraBedsAllowed() != null ? i.getExtraBedsAllowed() : 0)
                        .thenComparing(i -> parseSafeAmount(i.getFinalTotalAmount()) != null ? parseSafeAmount(i.getFinalTotalAmount()) : BigDecimal.ZERO))
                .orElse(items.get(items.size() - 1));

        String savingsClause = formatSavingsClause(budgetPick.getCurrency(), maxSavingsAmount, maxSavingsPercent);

        if (Objects.equals(budgetPick.getHotelRoomTypeId(), spacePick.getHotelRoomTypeId())
                && Objects.equals(budgetPick.getRecommendationId(), spacePick.getRecommendationId())) {
            return "%s hiện vừa có mức giá tốt nhất%s vừa đáp ứng tốt không gian và sức chứa (tối đa %d khách)."
                    .formatted(
                            budgetPick.getRoomTypeName(),
                            savingsClause.isEmpty() ? "" : " (" + savingsClause + ")",
                            budgetPick.getMaxTotalGuests() != null ? budgetPick.getMaxTotalGuests() : 2
                    );
        }

        String spaceHighlight = buildSpaceHighlight(spacePick);

        return switch (focus) {
            case FAMILY_KIDS ->
                    "Nếu gia đình có con nhỏ và ưu tiên sự thoải mái khi sinh hoạt trong phòng, %s là lựa chọn lý tưởng (%s). Ngược lại, nếu gia đình chủ yếu đi tham quan ban ngày và chỉ cần chỗ nghỉ tiêu chuẩn, chọn %s sẽ giúp %s."
                            .formatted(
                                    spacePick.getRoomTypeName(),
                                    spaceHighlight,
                                    budgetPick.getRoomTypeName(),
                                    savingsClause.isEmpty() ? "tối ưu chi phí hơn" : savingsClause
                            );
            case BUDGET_SAVER ->
                    "Để tối ưu ngân sách chuyến đi, %s giúp bạn %s so với %s trong khi vẫn đảm bảo đầy đủ các tiện ích cơ bản dùng chung."
                            .formatted(
                                    budgetPick.getRoomTypeName(),
                                    savingsClause.isEmpty() ? "tiết kiệm chi phí đáng kể" : savingsClause,
                                    spacePick.getRoomTypeName()
                            );
            case SPACE_COMFORT ->
                    "Nếu bạn ưu tiên trải nghiệm nghỉ dưỡng và không gian rộng rãi, %s nổi bật hơn (%s). Nếu muốn cân bằng chi phí, %s giúp %s."
                            .formatted(
                                    spacePick.getRoomTypeName(),
                                    spaceHighlight,
                                    budgetPick.getRoomTypeName(),
                                    savingsClause.isEmpty() ? "giảm chi phí lưu trú" : savingsClause
                            );
            case WORK_TRIP ->
                    "Nếu cần không gian rộng rãi để tiếp khách hoặc làm việc thoải mái, hãy cân nhắc %s (%s). Nếu chỉ cần phòng gọn gàng để nghỉ ngơi giữa lịch công tác, %s giúp %s."
                            .formatted(
                                    spacePick.getRoomTypeName(),
                                    spaceHighlight,
                                    budgetPick.getRoomTypeName(),
                                    savingsClause.isEmpty() ? "tiết kiệm chi phí hơn" : savingsClause
                            );
            default ->
                    "%s nổi bật về không gian & tiện nghi (%s), trong khi %s là lựa chọn kinh tế hơn (%s)."
                            .formatted(
                                    spacePick.getRoomTypeName(),
                                    spaceHighlight,
                                    budgetPick.getRoomTypeName(),
                                    savingsClause.isEmpty() ? "chi phí hợp lý hơn" : savingsClause
                            );
        };
    }

    private String buildSpaceHighlight(RoomComparisonItemDto item) {
        List<String> parts = new ArrayList<>();
        if (item.getStaticAreaAndLayout() != null && !item.getStaticAreaAndLayout().isBlank()) {
            Matcher m = AREA_SQM_PATTERN.matcher(item.getStaticAreaAndLayout());
            if (m.find()) {
                parts.add("diện tích " + m.group(0));
            }
        }
        int maxGuests = item.getMaxTotalGuests() != null ? item.getMaxTotalGuests() : 0;
        if (maxGuests > 0) {
            parts.add("tối đa " + maxGuests + " khách");
        }
        int extraBeds = item.getExtraBedsAllowed() != null ? item.getExtraBedsAllowed() : 0;
        if (extraBeds > 0) {
            parts.add("kê thêm tối đa " + extraBeds + " giường phụ");
        }
        if (item.getUniqueFeatureNames() != null && !item.getUniqueFeatureNames().isEmpty()) {
            parts.add("có " + item.getUniqueFeatureNames().get(0));
        }
        return parts.isEmpty() ? "tiện nghi cao cấp hơn" : String.join(", ", parts);
    }

    private int extractAreaValue(String staticDesc) {
        if (staticDesc == null || staticDesc.isBlank()) {
            return 0;
        }
        Matcher m = AREA_SQM_PATTERN.matcher(staticDesc);
        if (m.find()) {
            try {
                return (int) Math.round(Double.parseDouble(m.group(1).replace(',', '.')));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String formatSavingsClause(String currency, String maxSavingsAmount, BigDecimal maxSavingsPercent) {
        BigDecimal savings = parseSafeAmount(maxSavingsAmount);
        if (savings == null || savings.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        String formattedMoney = formatAmount(savings, currency);
        if (maxSavingsPercent != null && maxSavingsPercent.compareTo(BigDecimal.ZERO) > 0) {
            return "tiết kiệm %s (~%s%%)".formatted(formattedMoney, maxSavingsPercent.toPlainString());
        }
        return "tiết kiệm " + formattedMoney;
    }

    private String formatAmount(BigDecimal amount, String currency) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = "VND".equalsIgnoreCase(currency)
                ? new DecimalFormat("#,##0", symbols)
                : new DecimalFormat("#,##0.##", symbols);
        String ccy = currency != null && !currency.isBlank() ? " " + currency : "";
        return df.format(amount) + ccy;
    }
}
