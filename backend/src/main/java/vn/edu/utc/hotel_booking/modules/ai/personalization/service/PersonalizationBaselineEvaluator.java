package vn.edu.utc.hotel_booking.modules.ai.personalization.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.GuestPersonalizationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationEvalReportDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.StayHistoryRecordDto;
import vn.edu.utc.hotel_booking.modules.ai.personalization.privacy.PersonalizationPrivacyGuard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Động cơ đánh giá Holdout Offline so sánh mô hình Time-Decayed với Simple Frequency Baseline (U14).
 *
 * <p><strong>Nguyên tắc Time-Split Holdout chống rò rỉ dữ liệu (Leakage-Free):</strong></p>
 * <ul>
 *   <li>Sắp xếp chuỗi lưu trú theo trình tự thời gian tăng dần ({@code checkInDate ASC}).</li>
 *   <li>Tại mỗi bước dự đoán {@code t} (từ 1 đến N-1), tập huấn luyện chỉ bao gồm các kỳ lưu trú trong quá khứ {@code 0 ... t-1}.</li>
 *   <li>Tuyệt đối không nhìn trước kỳ lưu trú tương lai hoặc dùng nhãn của kỳ lưu trú hiện tại.</li>
 * </ul>
 */
@Service
public class PersonalizationBaselineEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PersonalizationBaselineEvaluator.class);

    private final GuestPersonalizationService personalizationService;
    private final PersonalizationPrivacyGuard privacyGuard;

    public PersonalizationBaselineEvaluator(
            GuestPersonalizationService personalizationService,
            PersonalizationPrivacyGuard privacyGuard
    ) {
        this.personalizationService = personalizationService;
        this.privacyGuard = privacyGuard;
    }

    /**
     * Thực hiện đánh giá Holdout theo thời gian so sánh Time-Decayed với Simple Frequency Baseline.
     *
     * @param request Yêu cầu cá nhân hoá chứa lịch sử lưu trú
     * @param k       Giá trị K cho số lượng gợi ý top-K (mặc định 1 nếu k <= 0)
     * @return Báo cáo kết quả đánh giá kèm số liệu HitRate@K và MRR@K
     */
    public PersonalizationEvalReportDto evaluateHoldout(GuestPersonalizationRequestDto request, int k) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (!request.isConsentGranted()) {
            throw new IllegalArgumentException("Consent is required to evaluate personalization history");
        }

        int evalK = k >= 1 ? k : 1;
        List<StayHistoryRecordDto> rawHistory = request.getStayHistory();

        if (rawHistory == null || rawHistory.size() < 2) {
            return PersonalizationEvalReportDto.builder()
                    .totalEvaluatedStays(0)
                    .k(evalK)
                    .timeDecayedHitRateAtK(0.0)
                    .baselineFrequencyHitRateAtK(0.0)
                    .timeDecayedMrrAtK(0.0)
                    .baselineFrequencyMrrAtK(0.0)
                    .build();
        }

        // 1. Lọc và sắp xếp theo checkInDate ASC
        List<StayHistoryRecordDto> validStays = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (StayHistoryRecordDto r : rawHistory) {
            if (r == null || r.getCheckInDate() == null || r.getCheckOutDate() == null) continue;
            if (!r.getCheckInDate().isBefore(r.getCheckOutDate())) continue;
            validStays.add(privacyGuard.sanitizeStayRecord(r, warnings));
        }

        validStays.sort(Comparator.comparing(StayHistoryRecordDto::getCheckInDate));

        int n = validStays.size();
        if (n < 2) {
            return PersonalizationEvalReportDto.builder()
                    .totalEvaluatedStays(0)
                    .k(evalK)
                    .timeDecayedHitRateAtK(0.0)
                    .baselineFrequencyHitRateAtK(0.0)
                    .timeDecayedMrrAtK(0.0)
                    .baselineFrequencyMrrAtK(0.0)
                    .build();
        }

        double totalHitsTimeDecayed = 0.0;
        double totalMrrTimeDecayed = 0.0;

        double totalHitsBaseline = 0.0;
        double totalMrrBaseline = 0.0;

        int evaluatedSteps = n - 1;

        // 2. Chạy Holdout: Dự đoán stay t dựa trên tập huấn luyện 0 ... t-1
        for (int t = 1; t < n; t++) {
            List<StayHistoryRecordDto> trainingSet = validStays.subList(0, t);
            StayHistoryRecordDto targetStay = validStays.get(t);
            Integer actualRoomTypeId = targetStay.getHotelRoomTypeId();
            LocalDate evalDate = targetStay.getCheckInDate();

            // A. Dự đoán bằng Time-Decayed Model
            List<Integer> tdTopK = predictTimeDecayedTopK(trainingSet, evalDate, targetStay.getHotelId(), evalK);
            int tdRank = tdTopK.indexOf(actualRoomTypeId);
            if (tdRank >= 0) {
                totalHitsTimeDecayed += 1.0;
                totalMrrTimeDecayed += 1.0 / (tdRank + 1);
            }

            // B. Dự đoán bằng Simple Frequency Baseline
            List<Integer> baseTopK = predictFrequencyBaselineTopK(trainingSet, evalK);
            int baseRank = baseTopK.indexOf(actualRoomTypeId);
            if (baseRank >= 0) {
                totalHitsBaseline += 1.0;
                totalMrrBaseline += 1.0 / (baseRank + 1);
            }
        }

        double timeDecayedHitRate = round4(totalHitsTimeDecayed / evaluatedSteps);
        double baselineHitRate = round4(totalHitsBaseline / evaluatedSteps);
        double timeDecayedMrr = round4(totalMrrTimeDecayed / evaluatedSteps);
        double baselineMrr = round4(totalMrrBaseline / evaluatedSteps);

        log.info("U14 Holdout Eval: guestId={}, evaluatedSteps={}, TD_Hit@{}={}, Base_Hit@{}={}, TD_MRR={}, Base_MRR={}",
                request.getGuestId(), evaluatedSteps, evalK, timeDecayedHitRate, evalK, baselineHitRate, timeDecayedMrr, baselineMrr);

        return PersonalizationEvalReportDto.builder()
                .totalEvaluatedStays(evaluatedSteps)
                .k(evalK)
                .timeDecayedHitRateAtK(timeDecayedHitRate)
                .baselineFrequencyHitRateAtK(baselineHitRate)
                .timeDecayedMrrAtK(timeDecayedMrr)
                .baselineFrequencyMrrAtK(baselineMrr)
                .build();
    }

    private List<Integer> predictTimeDecayedTopK(
            List<StayHistoryRecordDto> trainingSet, LocalDate referenceDate, Integer targetHotelId, int k) {

        Map<Integer, Double> scoreMap = new HashMap<>();
        for (StayHistoryRecordDto stay : trainingSet) {
            double weight = personalizationService.computeStayWeight(stay, referenceDate, targetHotelId);
            scoreMap.merge(stay.getHotelRoomTypeId(), weight, Double::sum);
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(k)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<Integer> predictFrequencyBaselineTopK(List<StayHistoryRecordDto> trainingSet, int k) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (StayHistoryRecordDto stay : trainingSet) {
            countMap.merge(stay.getHotelRoomTypeId(), 1, Integer::sum);
        }

        return countMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(k)
                .map(Map.Entry::getKey)
                .toList();
    }

    private double round4(double val) {
        return BigDecimal.valueOf(val).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
