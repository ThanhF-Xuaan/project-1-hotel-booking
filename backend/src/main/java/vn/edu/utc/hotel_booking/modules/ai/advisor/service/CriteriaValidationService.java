package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CriteriaConfirmation;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.CriteriaOrigin;
import vn.edu.utc.hotel_booking.modules.ai.advisor.model.ValidationResult;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CriteriaValidationService {

    private static final Pattern OUT_OF_SCOPE_PATTERN = Pattern.compile(
            "(?i)(đặt\\s*phòng|dat\\s*phong|giữ\\s*phòng|giu\\s*phong|thanh\\s*toán|thanh\\s*toan|hủy\\s*phòng|huy\\s*phong|hoàn\\s*tiền|hoan\\s*tien|quản\\s*trị|quan\\s*tri|\\b(book|booking|hold|reserve|pay|payment|cancel|refund|admin)\\b)"
    );

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");

    /**
     * Detects if the message intent attempts to perform write/hold/payment/cancellation actions.
     */
    public boolean isOutOfScopeIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return OUT_OF_SCOPE_PATTERN.matcher(message).find();
    }

    /**
     * Merges patch into existing criteria and tracks criteria provenance.
     */
    public RoomSearchCriteriaDto mergeCriteria(
            RoomSearchCriteriaDto existing,
            CriteriaPatchDto patch,
            List<CriteriaEvidenceDto> provenanceTarget
    ) {
        RoomSearchCriteriaDto criteria = (existing != null)
                ? cloneCriteria(existing)
                : RoomSearchCriteriaDto.builder()
                .roomsCount(1)
                .children(0)
                .extraBeds(0)
                .requiredFeatureCodes(new ArrayList<>())
                .preferredFeatureCodes(new ArrayList<>())
                .build();

        Map<String, CriteriaEvidenceDto> evidenceMap = new LinkedHashMap<>();
        if (provenanceTarget != null) {
            for (CriteriaEvidenceDto ev : provenanceTarget) {
                if (ev != null && ev.getField() != null) {
                    evidenceMap.put(ev.getField(), ev);
                }
            }
        }

        if (patch != null) {
            if (patch.getHotelId() != null) {
                criteria.setHotelId(patch.getHotelId());
                recordEvidence(evidenceMap, "hotelId", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getLocationKeyword() != null) {
                criteria.setLocationKeyword(patch.getLocationKeyword());
                recordEvidence(evidenceMap, "locationKeyword", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getCheckInDate() != null) {
                criteria.setCheckInDate(patch.getCheckInDate());
                recordEvidence(evidenceMap, "checkInDate", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getCheckOutDate() != null) {
                criteria.setCheckOutDate(patch.getCheckOutDate());
                recordEvidence(evidenceMap, "checkOutDate", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getAdults() != null) {
                criteria.setAdults(patch.getAdults());
                recordEvidence(evidenceMap, "adults", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getChildren() != null) {
                criteria.setChildren(patch.getChildren());
                recordEvidence(evidenceMap, "children", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getInfants() != null) {
                criteria.setInfants(patch.getInfants());
            }
            if (patch.getExtraBeds() != null) {
                criteria.setExtraBeds(patch.getExtraBeds());
                recordEvidence(evidenceMap, "extraBeds", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getRoomsCount() != null) {
                criteria.setRoomsCount(patch.getRoomsCount());
                recordEvidence(evidenceMap, "roomsCount", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getRequiredFeatureCodes() != null) {
                criteria.setRequiredFeatureCodes(new ArrayList<>(patch.getRequiredFeatureCodes()));
                recordEvidence(evidenceMap, "requiredFeatureCodes", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getPreferredFeatureCodes() != null) {
                criteria.setPreferredFeatureCodes(new ArrayList<>(patch.getPreferredFeatureCodes()));
                recordEvidence(evidenceMap, "preferredFeatureCodes", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
            if (patch.getBudget() != null) {
                criteria.setBudget(patch.getBudget());
                recordEvidence(evidenceMap, "budget.amount", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
                recordEvidence(evidenceMap, "budget.currency", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
                recordEvidence(evidenceMap, "budget.basis", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
                recordEvidence(evidenceMap, "budget.coverage", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
                recordEvidence(evidenceMap, "budget.hard", CriteriaOrigin.USER_PROVIDED, CriteriaConfirmation.CONFIRMED);
            }
        }

        if (provenanceTarget != null) {
            provenanceTarget.clear();
            provenanceTarget.addAll(evidenceMap.values());
        }

        return criteria;
    }

    /**
     * Validates search criteria against R1 business rules and schemas.
     */
    public ValidationResult validateForSearch(
            RoomSearchCriteriaDto criteria,
            List<CriteriaEvidenceDto> provenance
    ) {
        if (criteria == null) {
            return ValidationResult.builder()
                    .valid(false)
                    .status(AdvisorStatus.NEED_INFO)
                    .reply("Vui lòng cung cấp thông tin tìm kiếm phòng.")
                    .missingFields(List.of("locationKeyword", "checkInDate", "checkOutDate", "adults"))
                    .ambiguities(new ArrayList<>())
                    .provenance(provenance != null ? provenance : new ArrayList<>())
                    .build();
        }

        List<String> missingFields = new ArrayList<>();
        List<AmbiguityDto> ambiguities = new ArrayList<>();

        // One-room MVP constraint
        if (criteria.getRoomsCount() != null && criteria.getRoomsCount() > 1) {
            return ValidationResult.builder()
                    .valid(false)
                    .status(AdvisorStatus.NEED_INFO)
                    .reply("Hiện chỉ hỗ trợ tìm một phòng cho mỗi phương án. Có thể tìm một phòng hoặc dùng giao diện tìm phòng thông thường.")
                    .criteria(criteria)
                    .missingFields(List.of("roomsCount"))
                    .ambiguities(ambiguities)
                    .provenance(provenance != null ? provenance : new ArrayList<>())
                    .build();
        }

        // Scope check: hotelId or locationKeyword required
        boolean hasHotel = criteria.getHotelId() != null && criteria.getHotelId() > 0;
        boolean hasLocation = criteria.getLocationKeyword() != null && !criteria.getLocationKeyword().isBlank();
        if (!hasHotel && !hasLocation) {
            missingFields.add("locationKeyword");
        }

        // Dates check
        if (criteria.getCheckInDate() == null) {
            missingFields.add("checkInDate");
        }
        if (criteria.getCheckOutDate() == null) {
            missingFields.add("checkOutDate");
        }
        if (criteria.getCheckInDate() != null && criteria.getCheckOutDate() != null) {
            if (!criteria.getCheckOutDate().isAfter(criteria.getCheckInDate())) {
                missingFields.add("checkOutDate");
                return ValidationResult.builder()
                        .valid(false)
                        .status(AdvisorStatus.NEED_INFO)
                        .reply("Ngày trả phòng phải sau ngày nhận phòng.")
                        .criteria(criteria)
                        .missingFields(missingFields)
                        .ambiguities(ambiguities)
                        .provenance(provenance != null ? provenance : new ArrayList<>())
                        .build();
            }
        }

        // Capacity check
        if (criteria.getAdults() == null || criteria.getAdults() < 1) {
            missingFields.add("adults");
        }

        // Apply defaults if fields are not explicitly set
        if (criteria.getRoomsCount() == null) {
            criteria.setRoomsCount(1);
        }
        if (criteria.getChildren() == null) {
            criteria.setChildren(0);
        }
        if (criteria.getExtraBeds() == null) {
            criteria.setExtraBeds(0);
        }
        if (criteria.getRequiredFeatureCodes() == null) {
            criteria.setRequiredFeatureCodes(new ArrayList<>());
        }
        if (criteria.getPreferredFeatureCodes() == null) {
            criteria.setPreferredFeatureCodes(new ArrayList<>());
        }

        if (!missingFields.isEmpty()) {
            return ValidationResult.builder()
                    .valid(false)
                    .status(AdvisorStatus.NEED_INFO)
                    .reply("Vui lòng cung cấp thêm thông tin còn thiếu để tìm phòng: " + String.join(", ", missingFields) + ".")
                    .criteria(criteria)
                    .missingFields(missingFields)
                    .ambiguities(ambiguities)
                    .provenance(provenance != null ? provenance : new ArrayList<>())
                    .build();
        }

        // Budget semantic validation
        if (criteria.getBudget() != null) {
            BudgetDto budget = criteria.getBudget();
            if (budget.getAmount() == null || !isValidAmount(budget.getAmount())) {
                ambiguities.add(new AmbiguityDto("budget.amount", "BUDGET_BASIS_UNKNOWN"));
            }
            if (budget.getCurrency() == null || !CURRENCY_PATTERN.matcher(budget.getCurrency()).matches()) {
                ambiguities.add(new AmbiguityDto("budget.currency", "CURRENCY_UNKNOWN"));
            }
            if (budget.getBasis() == null) {
                ambiguities.add(new AmbiguityDto("budget.basis", "BUDGET_BASIS_UNKNOWN"));
            }
            if (budget.getCoverage() == null) {
                ambiguities.add(new AmbiguityDto("budget.coverage", "BUDGET_COVERAGE_UNKNOWN"));
            }

            if (!ambiguities.isEmpty()) {
                return ValidationResult.builder()
                        .valid(false)
                        .status(AdvisorStatus.NEED_INFO)
                        .reply("Vui lòng làm rõ thông tin ngân sách.")
                        .criteria(criteria)
                        .missingFields(missingFields)
                        .ambiguities(ambiguities)
                        .provenance(provenance != null ? provenance : new ArrayList<>())
                        .build();
            }
        }

        // All valid and ready for candidate evaluation
        return ValidationResult.builder()
                .valid(true)
                .status(AdvisorStatus.RESULTS)
                .reply("Có thể xem chi tiết các phương án phòng. Giá và tồn phòng cần kiểm tra lại.")
                .criteria(criteria)
                .missingFields(new ArrayList<>())
                .ambiguities(new ArrayList<>())
                .provenance(provenance != null ? provenance : new ArrayList<>())
                .build();
    }

    private boolean isValidAmount(String amountStr) {
        try {
            BigDecimal val = new BigDecimal(amountStr);
            return val.compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void recordEvidence(
            Map<String, CriteriaEvidenceDto> evidenceMap,
            String field,
            CriteriaOrigin origin,
            CriteriaConfirmation confirmation
    ) {
        evidenceMap.put(field, CriteriaEvidenceDto.builder()
                .field(field)
                .origin(origin)
                .confirmation(confirmation)
                .build());
    }

    private RoomSearchCriteriaDto cloneCriteria(RoomSearchCriteriaDto source) {
        return RoomSearchCriteriaDto.builder()
                .hotelId(source.getHotelId())
                .locationKeyword(source.getLocationKeyword())
                .checkInDate(source.getCheckInDate())
                .checkOutDate(source.getCheckOutDate())
                .adults(source.getAdults())
                .children(source.getChildren())
                .infants(source.getInfants())
                .extraBeds(source.getExtraBeds())
                .roomsCount(source.getRoomsCount())
                .requiredFeatureCodes(source.getRequiredFeatureCodes() != null ? new ArrayList<>(source.getRequiredFeatureCodes()) : new ArrayList<>())
                .preferredFeatureCodes(source.getPreferredFeatureCodes() != null ? new ArrayList<>(source.getPreferredFeatureCodes()) : new ArrayList<>())
                .budget(source.getBudget())
                .build();
    }
}
