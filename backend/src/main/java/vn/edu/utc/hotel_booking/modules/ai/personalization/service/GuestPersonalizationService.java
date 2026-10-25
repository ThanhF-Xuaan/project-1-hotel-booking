package vn.edu.utc.hotel_booking.modules.ai.personalization.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationResultDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizedFeatureSuggestionDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizedRoomTypeSuggestionDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizedServiceSuggestionDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.StayHistoryRecordDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.enums.PersonalizationStatus;
import vn.edu.utc.hotel_booking.modules.ai.personalization.enums.TravelPatternSegment;
import vn.edu.utc.hotel_booking.modules.ai.personalization.privacy.PersonalizationPrivacyGuard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dịch vụ phân tích sở thích và gợi ý cá nhân hoá cho khách hàng dựa trên lịch sử lưu trú (U14).
 *
 * <p><strong>Các nguyên tắc cốt lõi:</strong></p>
 * <ol>
 *   <li>Tuân thủ quyền riêng tư: Kiểm tra {@code consentGranted} và loại bỏ thuộc tính nhạy cảm qua {@link PersonalizationPrivacyGuard}.</li>
 *   <li>Thuật toán suy giảm theo thời gian (Time-Decayed Recency Weighting): Kỳ lưu trú gần đây có trọng số cao hơn kỳ lưu trú trong quá khứ.</li>
 *   <li>Ưu tiên khách sạn mục tiêu: Áp dụng hệ số boost x1.25 khi hạng phòng thuộc {@code targetHotelId}.</li>
 *   <li>Bất biến kiến trúc: Gợi ý mang tính tham khảo (non-binding), hoàn toàn không can thiệp vào Pricing hay khuyến mãi.</li>
 * </ol>
 */
@Service
public class GuestPersonalizationService {

    private static final Logger log = LoggerFactory.getLogger(GuestPersonalizationService.class);

    private final PersonalizationPrivacyGuard privacyGuard;

    public GuestPersonalizationService(PersonalizationPrivacyGuard privacyGuard) {
        this.privacyGuard = privacyGuard;
    }

    /**
     * Phân tích lịch sử lưu trú và sinh danh sách gợi ý cá nhân hoá.
     *
     * @param request Yêu cầu cá nhân hoá kèm lịch sử lưu trú
     * @return Kết quả gợi ý kèm trạng thái và cảnh báo an toàn
     */
    public GuestPersonalizationResultDto personalize(GuestPersonalizationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Personalization request cannot be null");
        }

        List<String> warnings = new ArrayList<>();

        // 1. Kiểm tra sự đồng ý (Consent check)
        if (!request.isConsentGranted()) {
            log.debug("U14 Personalization: consent required");
            return GuestPersonalizationResultDto.builder()
                    .guestId(request.getGuestId())
                    .status(PersonalizationStatus.CONSENT_REQUIRED)
                    .detectedSegment(TravelPatternSegment.GENERAL)
                    .roomTypeSuggestions(Collections.emptyList())
                    .featureSuggestions(Collections.emptyList())
                    .serviceSuggestions(Collections.emptyList())
                    .warnings(List.of("CONSENT_REQUIRED: Khách hàng chưa cấp quyền sử dụng lịch sử lưu trú để cá nhân hóa gợi ý."))
                    .build();
        }

        LocalDate referenceDate = request.getReferenceDate() != null ? request.getReferenceDate() : LocalDate.now();
        List<StayHistoryRecordDto> rawHistory = request.getStayHistory();

        if (rawHistory == null || rawHistory.isEmpty()) {
            log.debug("U14 Personalization: cold start");
            return buildColdStartResult(request.getGuestId(), warnings, "COLD_START: Chưa có lịch sử lưu trú trước đó.");
        }

        // 2. Lọc dữ liệu hợp lệ và làm sạch PII / thuộc tính nhạy cảm
        List<StayHistoryRecordDto> validRecords = new ArrayList<>();
        int totalRawPreferenceSignals = 0;
        int totalSafePreferenceSignals = 0;

        for (StayHistoryRecordDto r : rawHistory) {
            if (r == null) continue;

            if (r.getCheckInDate() == null || r.getCheckOutDate() == null) {
                warnings.add("Bản ghi lưu trú thiếu ngày check-in hoặc check-out, đã bị loại khỏi phân tích.");
                continue;
            }

            if (!r.getCheckInDate().isBefore(r.getCheckOutDate())) {
                warnings.add("Bản ghi lưu trú có ngày check-in không trước ngày check-out ("
                        + r.getCheckInDate() + " -> " + r.getCheckOutDate() + "), đã bị loại khỏi phân tích.");
                continue;
            }

            if (r.getCheckInDate().isAfter(referenceDate)) {
                warnings.add("Bản ghi lưu trú có ngày check-in nằm trong tương lai so với ngày tham chiếu ("
                        + r.getCheckInDate() + " > " + referenceDate + "), đã bị loại khỏi phân tích.");
                continue;
            }

            // Đếm tín hiệu sở thích thô đầu vào
            if (r.getPreferredFeatureCodes() != null) {
                totalRawPreferenceSignals += (int) r.getPreferredFeatureCodes().stream()
                        .filter(f -> f != null && !f.isBlank())
                        .count();
            }
            if (r.getUsedServiceCodes() != null) {
                totalRawPreferenceSignals += (int) r.getUsedServiceCodes().stream()
                        .filter(s -> s != null && !s.isBlank())
                        .count();
            }
            if (r.getGuestPreferenceNote() != null && !r.getGuestPreferenceNote().isBlank()) {
                totalRawPreferenceSignals++;
            }

            // Làm sạch thuộc tính nhạy cảm
            StayHistoryRecordDto safeRecord = privacyGuard.sanitizeStayRecord(r, warnings);
            validRecords.add(safeRecord);

            // Đếm tín hiệu an toàn sau khi làm sạch
            if (safeRecord.getPreferredFeatureCodes() != null) {
                totalSafePreferenceSignals += safeRecord.getPreferredFeatureCodes().size();
            }
            if (safeRecord.getUsedServiceCodes() != null) {
                totalSafePreferenceSignals += safeRecord.getUsedServiceCodes().size();
            }
            if (safeRecord.getGuestPreferenceNote() != null
                    && !safeRecord.getGuestPreferenceNote().isBlank()
                    && !"[REDACTED_SENSITIVE_PREFERENCE]".equals(safeRecord.getGuestPreferenceNote())) {
                totalSafePreferenceSignals++;
            }
        }

        if (validRecords.isEmpty()) {
            return buildColdStartResult(request.getGuestId(), warnings,
                    "COLD_START: Tất cả bản ghi lịch sử đều không hợp lệ về mặt ngày tháng hoặc phạm vi.");
        }

        // Kiểm tra L1: Khách hàng có truyền tín hiệu sở thích nhưng 100% đều là thuộc tính nhạy cảm bị nghiêm cấm
        if (totalRawPreferenceSignals > 0 && totalSafePreferenceSignals == 0) {
            String rejectMsg = "SENSITIVE_INPUT_REJECTED: Toàn bộ tín hiệu sở thích và ghi chú được cung cấp đều là thuộc tính nhạy cảm bị nghiêm cấm. Hệ thống từ chối tạo hồ sơ sở thích cá nhân hoá.";
            if (!warnings.contains(rejectMsg)) {
                warnings.add(rejectMsg);
            }
            log.warn("U14 Personalization: sensitive-only attributes rejected");
            return GuestPersonalizationResultDto.builder()
                    .guestId(request.getGuestId())
                    .status(PersonalizationStatus.SENSITIVE_INPUT_REJECTED)
                    .detectedSegment(TravelPatternSegment.GENERAL)
                    .roomTypeSuggestions(Collections.emptyList())
                    .featureSuggestions(Collections.emptyList())
                    .serviceSuggestions(Collections.emptyList())
                    .warnings(warnings)
                    .build();
        }

        // 3. Phân khúc hành vi lưu trú (TravelPatternSegment)
        TravelPatternSegment segment = detectSegment(validRecords);

        // 4. Tính toán trọng số suy giảm theo thời gian và tính điểm hạng phòng
        List<PersonalizedRoomTypeSuggestionDto> roomSuggestions = computeRoomTypeSuggestions(
                validRecords, request.getTargetHotelId(), referenceDate, request.getMaxSuggestions());

        // 5. Tính toán sở thích đặc tính phòng (Features)
        List<PersonalizedFeatureSuggestionDto> featureSuggestions = computeFeatureSuggestions(
                validRecords, referenceDate);

        // 6. Tính toán sở thích dịch vụ (Services)
        List<PersonalizedServiceSuggestionDto> serviceSuggestions = computeServiceSuggestions(
                validRecords, referenceDate);

        log.debug("U14 Personalization: analysis completed");

        return GuestPersonalizationResultDto.builder()
                .guestId(request.getGuestId())
                .status(PersonalizationStatus.PERSONALIZED_READY)
                .detectedSegment(segment)
                .roomTypeSuggestions(roomSuggestions)
                .featureSuggestions(featureSuggestions)
                .serviceSuggestions(serviceSuggestions)
                .warnings(warnings)
                .build();
    }

    private GuestPersonalizationResultDto buildColdStartResult(Long guestId, List<String> warnings, String reason) {
        if (reason != null && !warnings.contains(reason)) {
            warnings.add(reason);
        }
        return GuestPersonalizationResultDto.builder()
                .guestId(guestId)
                .status(PersonalizationStatus.COLD_START_FALLBACK)
                .detectedSegment(TravelPatternSegment.GENERAL)
                .roomTypeSuggestions(Collections.emptyList())
                .featureSuggestions(Collections.emptyList())
                .serviceSuggestions(Collections.emptyList())
                .warnings(warnings)
                .build();
    }

    /**
     * Tính toán trọng số suy giảm thời gian:
     * w_i = (1.0 / (1.0 + 0.15 * monthsAgo)) * min(3.0, 1.0 + 0.1 * nights)
     */
    double computeStayWeight(StayHistoryRecordDto stay, LocalDate referenceDate, Integer targetHotelId) {
        long nights = Math.max(1, ChronoUnit.DAYS.between(stay.getCheckInDate(), stay.getCheckOutDate()));
        long daysAgo = Math.max(0, ChronoUnit.DAYS.between(stay.getCheckInDate(), referenceDate));
        double monthsAgo = daysAgo / 30.4375;

        double recencyFactor = 1.0 / (1.0 + 0.15 * monthsAgo);
        double durationFactor = Math.min(3.0, 1.0 + 0.1 * nights);
        double weight = recencyFactor * durationFactor;

        // Ưu tiên khách sạn mục tiêu
        if (targetHotelId != null && targetHotelId.equals(stay.getHotelId())) {
            weight *= 1.25;
        }

        return weight;
    }

    private List<PersonalizedRoomTypeSuggestionDto> computeRoomTypeSuggestions(
            List<StayHistoryRecordDto> stays, Integer targetHotelId, LocalDate referenceDate, Integer maxSuggestions) {

        int limit = (maxSuggestions != null && maxSuggestions >= 1 && maxSuggestions <= 10) ? maxSuggestions : 3;

        Map<Integer, RoomTypeAggregation> aggMap = new LinkedHashMap<>();

        for (StayHistoryRecordDto stay : stays) {
            double weight = computeStayWeight(stay, referenceDate, targetHotelId);

            RoomTypeAggregation agg = aggMap.computeIfAbsent(stay.getHotelRoomTypeId(),
                    k -> new RoomTypeAggregation(stay.getHotelId(), stay.getHotelRoomTypeId(),
                            stay.getRoomTypeCode(), stay.getRoomTypeName()));

            agg.addStay(stay, weight);
        }

        if (aggMap.isEmpty()) {
            return Collections.emptyList();
        }

        // Tìm điểm số cao nhất để chuẩn hóa về [0.0, 1.0]
        double maxScore = aggMap.values().stream()
                .mapToDouble(a -> a.accumulatedScore)
                .max()
                .orElse(1.0);

        List<PersonalizedRoomTypeSuggestionDto> result = new ArrayList<>();

        for (RoomTypeAggregation agg : aggMap.values()) {
            double rawAffinity = maxScore > 0.0 ? (agg.accumulatedScore / maxScore) : 1.0;
            double affinity = BigDecimal.valueOf(Math.min(1.0, Math.max(0.01, rawAffinity)))
                    .setScale(4, RoundingMode.HALF_UP)
                    .doubleValue();

            List<String> explanations = new ArrayList<>();
            explanations.add("Khách đã lưu trú " + agg.stayCount + " lần tại hạng phòng này (lần gần nhất: "
                    + agg.lastStayDate + ").");

            if (targetHotelId != null && targetHotelId.equals(agg.hotelId)) {
                explanations.add("Được ưu tiên do thuộc đúng khách sạn mục tiêu (ID " + targetHotelId + ") mà khách đang quan tâm.");
            }

            if (!agg.matchedFeatures.isEmpty()) {
                explanations.add("Phù hợp với các đặc tính yêu thích trong lịch sử: "
                        + String.join(", ", agg.matchedFeatures) + ".");
            }

            result.add(PersonalizedRoomTypeSuggestionDto.builder()
                    .hotelId(agg.hotelId)
                    .hotelRoomTypeId(agg.hotelRoomTypeId)
                    .roomTypeCode(agg.roomTypeCode)
                    .roomTypeName(agg.roomTypeName)
                    .affinityScore(affinity)
                    .historicalStayCount(agg.stayCount)
                    .lastStayDate(agg.lastStayDate)
                    .matchedFeatures(new ArrayList<>(agg.matchedFeatures))
                    .explanationReasons(explanations)
                    .build());
        }

        // Sắp xếp tất định: affinityScore DESC -> historicalStayCount DESC -> lastStayDate DESC -> hotelRoomTypeId ASC
        result.sort((a, b) -> {
            int c = Double.compare(b.getAffinityScore(), a.getAffinityScore());
            if (c != 0) return c;
            c = Integer.compare(b.getHistoricalStayCount(), a.getHistoricalStayCount());
            if (c != 0) return c;
            if (a.getLastStayDate() == null && b.getLastStayDate() != null) return 1;
            if (a.getLastStayDate() != null && b.getLastStayDate() == null) return -1;
            if (a.getLastStayDate() != null && b.getLastStayDate() != null) {
                c = b.getLastStayDate().compareTo(a.getLastStayDate());
                if (c != 0) return c;
            }
            return Integer.compare(a.getHotelRoomTypeId(), b.getHotelRoomTypeId());
        });

        if (result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    private List<PersonalizedFeatureSuggestionDto> computeFeatureSuggestions(
            List<StayHistoryRecordDto> stays, LocalDate referenceDate) {

        Map<String, FeatureAggregation> map = new HashMap<>();

        for (StayHistoryRecordDto stay : stays) {
            double weight = computeStayWeight(stay, referenceDate, null);
            if (stay.getPreferredFeatureCodes() != null) {
                for (String code : stay.getPreferredFeatureCodes()) {
                    if (code != null && !code.isBlank()) {
                        String clean = code.trim().toUpperCase(Locale.ROOT);
                        map.computeIfAbsent(clean, k -> new FeatureAggregation(clean)).add(weight);
                    }
                }
            }
        }

        if (map.isEmpty()) {
            return Collections.emptyList();
        }

        double maxScore = map.values().stream().mapToDouble(a -> a.accumulatedScore).max().orElse(1.0);

        List<PersonalizedFeatureSuggestionDto> list = new ArrayList<>();
        for (FeatureAggregation agg : map.values()) {
            double rawAffinity = maxScore > 0.0 ? (agg.accumulatedScore / maxScore) : 1.0;
            double score = BigDecimal.valueOf(Math.min(1.0, Math.max(0.01, rawAffinity)))
                    .setScale(4, RoundingMode.HALF_UP)
                    .doubleValue();
            list.add(PersonalizedFeatureSuggestionDto.builder()
                    .featureCode(agg.code)
                    .affinityScore(score)
                    .occurrenceCount(agg.count)
                    .build());
        }

        list.sort((a, b) -> {
            int c = Double.compare(b.getAffinityScore(), a.getAffinityScore());
            if (c != 0) return c;
            c = Integer.compare(b.getOccurrenceCount(), a.getOccurrenceCount());
            if (c != 0) return c;
            return a.getFeatureCode().compareTo(b.getFeatureCode());
        });

        return list.size() > 5 ? list.subList(0, 5) : list;
    }

    private List<PersonalizedServiceSuggestionDto> computeServiceSuggestions(
            List<StayHistoryRecordDto> stays, LocalDate referenceDate) {

        Map<String, FeatureAggregation> map = new HashMap<>();

        for (StayHistoryRecordDto stay : stays) {
            double weight = computeStayWeight(stay, referenceDate, null);
            if (stay.getUsedServiceCodes() != null) {
                for (String code : stay.getUsedServiceCodes()) {
                    if (code != null && !code.isBlank()) {
                        String clean = code.trim().toUpperCase(Locale.ROOT);
                        map.computeIfAbsent(clean, k -> new FeatureAggregation(clean)).add(weight);
                    }
                }
            }
        }

        if (map.isEmpty()) {
            return Collections.emptyList();
        }

        double maxScore = map.values().stream().mapToDouble(a -> a.accumulatedScore).max().orElse(1.0);

        List<PersonalizedServiceSuggestionDto> list = new ArrayList<>();
        for (FeatureAggregation agg : map.values()) {
            double rawAffinity = maxScore > 0.0 ? (agg.accumulatedScore / maxScore) : 1.0;
            double score = BigDecimal.valueOf(Math.min(1.0, Math.max(0.01, rawAffinity)))
                    .setScale(4, RoundingMode.HALF_UP)
                    .doubleValue();
            list.add(PersonalizedServiceSuggestionDto.builder()
                    .serviceCode(agg.code)
                    .affinityScore(score)
                    .occurrenceCount(agg.count)
                    .build());
        }

        list.sort((a, b) -> {
            int c = Double.compare(b.getAffinityScore(), a.getAffinityScore());
            if (c != 0) return c;
            c = Integer.compare(b.getOccurrenceCount(), a.getOccurrenceCount());
            if (c != 0) return c;
            return a.getServiceCode().compareTo(b.getServiceCode());
        });

        return list.size() > 5 ? list.subList(0, 5) : list;
    }

    private TravelPatternSegment detectSegment(List<StayHistoryRecordDto> stays) {
        boolean hasChildren = stays.stream().anyMatch(s -> s.getChildren() > 0);
        if (hasChildren) {
            return TravelPatternSegment.FAMILY_LEISURE;
        }

        double avgAdults = stays.stream().mapToInt(StayHistoryRecordDto::getAdults).average().orElse(1.0);
        if (avgAdults >= 1.8 && avgAdults <= 2.2) {
            return TravelPatternSegment.COUPLE_GETAWAY;
        }

        if (avgAdults <= 1.2) {
            // Kiểm tra các đặc tính công tác
            boolean hasWorkFeatures = stays.stream().anyMatch(s -> {
                boolean hasWorkDesk = s.getPreferredFeatureCodes() != null
                        && s.getPreferredFeatureCodes().stream().anyMatch(f -> f.equalsIgnoreCase("WORK_DESK")
                        || f.equalsIgnoreCase("QUIET_ROOM") || f.equalsIgnoreCase("HIGH_FLOOR"));
                boolean hasWorkService = s.getUsedServiceCodes() != null
                        && s.getUsedServiceCodes().stream().anyMatch(srv -> srv.equalsIgnoreCase("AIRPORT_TRANSFER")
                        || srv.equalsIgnoreCase("LAUNDRY") || srv.equalsIgnoreCase("BUSINESS_CENTER"));
                return hasWorkDesk || hasWorkService;
            });

            if (stays.size() >= 2 && hasWorkFeatures) {
                return TravelPatternSegment.BUSINESS_FREQUENT;
            }
            return TravelPatternSegment.SOLO_TRAVELER;
        }

        return TravelPatternSegment.GENERAL;
    }

    // Helper nội bộ để tích lũy điểm hạng phòng
    private static class RoomTypeAggregation {
        final Integer hotelId;
        final Integer hotelRoomTypeId;
        final String roomTypeCode;
        final String roomTypeName;
        double accumulatedScore = 0.0;
        int stayCount = 0;
        LocalDate lastStayDate = null;
        final Set<String> matchedFeatures = new LinkedHashSet<>();

        RoomTypeAggregation(Integer hotelId, Integer hotelRoomTypeId, String roomTypeCode, String roomTypeName) {
            this.hotelId = hotelId;
            this.hotelRoomTypeId = hotelRoomTypeId;
            this.roomTypeCode = roomTypeCode;
            this.roomTypeName = roomTypeName;
        }

        void addStay(StayHistoryRecordDto stay, double weight) {
            this.accumulatedScore += weight;
            this.stayCount++;
            if (this.lastStayDate == null || stay.getCheckInDate().isAfter(this.lastStayDate)) {
                this.lastStayDate = stay.getCheckInDate();
            }
            if (stay.getPreferredFeatureCodes() != null) {
                this.matchedFeatures.addAll(stay.getPreferredFeatureCodes());
            }
        }
    }

    private static class FeatureAggregation {
        final String code;
        double accumulatedScore = 0.0;
        int count = 0;

        FeatureAggregation(String code) {
            this.code = code;
        }

        void add(double weight) {
            this.accumulatedScore += weight;
            this.count++;
        }
    }
}
