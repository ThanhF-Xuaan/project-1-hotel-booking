package vn.edu.utc.hotel_booking.modules.ai.personalization.service;

import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRoomType;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationRoomTypeSuggestion;
import vn.edu.utc.hotel_booking.modules.ai.personalization.dto.PersonalizationStayRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoomAffinityCore {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String REASON_CODE = "HISTORICAL_BOOKING_AFFINITY";

    public List<PersonalizationRoomTypeSuggestion> rank(
            List<PersonalizationStayRecord> history,
            List<PersonalizationRoomType> currentCatalog,
            Integer hotelId,
            LocalDate referenceDate,
            Instant asOf,
            int maxSuggestions
    ) {
        if (history == null || currentCatalog == null || hotelId == null || referenceDate == null || asOf == null
                || maxSuggestions < 1 || maxSuggestions > 10) {
            throw new IllegalArgumentException("U14 affinity inputs are invalid");
        }

        Map<Integer, PersonalizationRoomType> candidates = currentCatalog.stream()
                .filter(roomType -> roomType != null && roomType.hotelRoomTypeId() != null
                        && roomType.hotelRoomTypeId() > 0)
                .collect(LinkedHashMap::new,
                        (map, roomType) -> map.putIfAbsent(roomType.hotelRoomTypeId(), roomType),
                        LinkedHashMap::putAll);
        if (candidates.isEmpty() || history.isEmpty()) {
            return List.of();
        }

        Map<Integer, RoomTypeScore> scores = new HashMap<>();
        Set<Long> seenBookingRooms = new HashSet<>();
        for (PersonalizationStayRecord stay : history) {
            if (!isEligible(stay, hotelId, referenceDate, asOf, candidates.keySet())) {
                continue;
            }
            if (!seenBookingRooms.add(stay.bookingRoomId())) {
                continue;
            }

            LocalDate checkInDate = stay.actualCheckInAt().atZoneSameInstant(HOTEL_ZONE).toLocalDate();
            LocalDate checkOutDate = stay.actualCheckOutAt().atZoneSameInstant(HOTEL_ZONE).toLocalDate();
            long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            long daysAgo = Math.max(0, ChronoUnit.DAYS.between(checkInDate, referenceDate));
            double monthsAgo = daysAgo / 30.4375;
            double weight = (1.0 / (1.0 + 0.15 * monthsAgo)) * Math.min(3.0, 1.0 + 0.1 * nights);
            if (hotelId.equals(stay.hotelId())) {
                weight *= 1.25;
            }

            scores.computeIfAbsent(stay.hotelRoomTypeId(), ignored -> new RoomTypeScore())
                    .add(weight, checkInDate);
        }
        if (scores.isEmpty()) {
            return List.of();
        }

        double maxScore = scores.values().stream().mapToDouble(score -> score.totalWeight).max().orElse(1.0);
        List<RankedSuggestion> ranked = new ArrayList<>();
        scores.forEach((hotelRoomTypeId, score) -> {
            PersonalizationRoomType roomType = candidates.get(hotelRoomTypeId);
            double normalized = score.totalWeight / maxScore;
            BigDecimal affinity = BigDecimal.valueOf(Math.min(1.0, Math.max(0.01, normalized)))
                    .setScale(4, RoundingMode.HALF_UP);
            ranked.add(new RankedSuggestion(
                    new PersonalizationRoomTypeSuggestion(
                            hotelRoomTypeId,
                            roomType.roomTypeCode(),
                            roomType.roomTypeName(),
                            affinity,
                            REASON_CODE
                    ),
                    score.bookingCount,
                    score.latestCheckIn
            ));
        });

        ranked.sort(Comparator
                .comparing((RankedSuggestion item) -> item.suggestion().affinityScore()).reversed()
                .thenComparing(Comparator.comparingInt(RankedSuggestion::bookingCount).reversed())
                .thenComparing(RankedSuggestion::latestCheckIn, Comparator.reverseOrder())
                .thenComparing(item -> item.suggestion().hotelRoomTypeId()));

        return ranked.stream().limit(maxSuggestions).map(RankedSuggestion::suggestion).toList();
    }

    private boolean isEligible(
            PersonalizationStayRecord stay,
            Integer targetHotelId,
            LocalDate referenceDate,
            Instant asOf,
            Set<Integer> currentCatalogIds
    ) {
        return stay != null
                && stay.bookingRoomId() != null
                && stay.hotelId() != null
                && stay.hotelId().equals(targetHotelId)
                && stay.hotelRoomTypeId() != null
                && currentCatalogIds.contains(stay.hotelRoomTypeId())
                && stay.actualCheckInAt() != null
                && stay.actualCheckOutAt() != null
                && stay.actualCheckOutAt().toInstant().isAfter(stay.actualCheckInAt().toInstant())
                && !stay.actualCheckOutAt().toInstant().isAfter(asOf)
                && stay.actualCheckInAt().atZoneSameInstant(HOTEL_ZONE).toLocalDate().isBefore(referenceDate.plusDays(1))
                && stay.actualCheckInAt().atZoneSameInstant(HOTEL_ZONE).toLocalDate()
                        .isBefore(stay.actualCheckOutAt().atZoneSameInstant(HOTEL_ZONE).toLocalDate());
    }

    private static class RoomTypeScore {
        private double totalWeight;
        private int bookingCount;
        private LocalDate latestCheckIn;

        void add(double weight, LocalDate checkInDate) {
            totalWeight += weight;
            bookingCount++;
            if (latestCheckIn == null || checkInDate.isAfter(latestCheckIn)) {
                latestCheckIn = checkInDate;
            }
        }
    }

    private record RankedSuggestion(
            PersonalizationRoomTypeSuggestion suggestion,
            int bookingCount,
            LocalDate latestCheckIn
    ) {
    }
}
