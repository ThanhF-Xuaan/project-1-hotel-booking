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

    private static final Pattern SECURITY_OR_ADMIN_PATTERN = Pattern.compile(
            "(?i)(quản\\s*trị|quan\\s*tri|giải\\s*phóng|cập\\s*nhật\\s*hồ\\s*sơ|lưu\\s*giấy\\s*tờ|\\b(admin|guest|identity|delete\\s+from|insert\\s+into|update\\s+\\w+\\s+set|drop\\s+table|https?://|javascript:|<script)\\b)"
    );

    private static final Pattern EXPLICIT_WRITE_COMMAND_PATTERN = Pattern.compile(
            "(?iu)(?:hãy\\s+)?(?:đặt|dat|giữ|giu|hủy|huy|thanh\\s*toán|thanh\\s*toan)\\s+.*?(?:mã\\s*\\w+|id\\s*\\w+|đơn\\s*\\w+|don\\s*\\w+|cho\\s*tôi(?!\\s*(?:biết|hỏi|xem))|giúp\\s*tôi(?!\\s*(?:biết|hỏi|xem))|hộ\\s*tôi|ngay|luôn)\\b|"
                    + "(?:hủy|huy)\\s*phòng\\s*(?:mã|id|cho\\s*tôi|giúp\\s*tôi|hộ\\s*tôi|ngay|luôn|này|nay)\\b|"
                    + "(?:đặt|dat|giữ|giu)\\s*phòng\\s*(?:này|nay|cho\\s*tôi|giúp\\s*tôi|hộ\\s*tôi|ngay|luôn)\\b|"
                    + "\\b(?:book\\s+(?:now|for\\s+me|this)|cancel\\s+(?:booking|reservation|order|my)|pay\\s+now)\\b"
    );

    private static final Pattern POLICY_INQUIRY_PATTERN = Pattern.compile(
            "(?iu)(?:chính\\s*sách|chinh\\s*sach|quy\\s*định|quy\\s*dinh|điều\\s*khoản|dieu\\s*khoan|điều\\s*kiện|dieu\\s*kien|"
                    + "có\\s*(?:được|cho\\s*phép|miễn\\s*phí|hoàn|trả)?.*?không|co\\s*(?:duoc|cho\\s*phep|mien\\s*phi)?.*?khong|"
                    + "được\\s*không|duoc\\s*khong|thế\\s*nào|the\\s*nao|ra\\s*sao|như\\s*thế\\s*nào|nhu\\s*the\\s*nao|"
                    + "mất\\s*phí\\s*(?:không|ko)|mat\\s*phi\\s*(?:khong|ko)|tính\\s*phí\\s*(?:không|ko)|tinh\\s*phi\\s*(?:khong|ko)|"
                    + "tính\\s*tiền\\s*(?:thêm|không|ko)|tinh\\s*tien\\s*(?:them|khong|ko)|thêm\\s*tiền|them\\s*tien|"
                    + "phụ\\s*thu|phu\\s*thu|phí\\s*hủy|phi\\s*huy|bao\\s*nhiêu\\s*%|bao\\s*nhieu\\s*%|"
                    + "lỡ.*?thì\\s*có|nếu.*?thì\\s*có|khi\\s*nào|bao\\s*lâu|trước\\s*(?:mấy|bao\\s*nhiêu)\\s*ngày|"
                    + "nhận\\s*phòng\\s*sớm|trả\\s*phòng\\s*muộn|thú\\s*cưng|vật\\s*nuôi|poodle|"
                    + "cho\\s*(?:mình|tôi|em)\\s*hỏi|hỏi\\s*về|hỏi\\s*xem|hướng\\s*dẫn|hoàn\\s*cọc|trả\\s*cọc|lấy\\s*lại\\s*cọc|rút\\s*cọc)\\b"
    );

    private static final Pattern GENERAL_WRITE_OR_MUTATION_PATTERN = Pattern.compile(
            "(?i)(đặt\\s*phòng|dat\\s*phong|giữ\\s*phòng|giu\\s*phong|thanh\\s*toán|thanh\\s*toan|hủy\\s*phòng|huy\\s*phong|hoàn\\s*tiền|hoan\\s*tien|\\b(book|booking|hold|reserve|pay|payment|cancel|refund|release)\\b)"
    );

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");

    /**
     * Determines whether the user message is an inquiry regarding policies, cancellation rules,
     * refund conditions, deposit returns, or date changes rather than an imperative transaction write.
     */
    public boolean isPolicyInquiry(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (SECURITY_OR_ADMIN_PATTERN.matcher(message).find() || EXPLICIT_WRITE_COMMAND_PATTERN.matcher(message).find()) {
            return false;
        }
        if (POLICY_INQUIRY_PATTERN.matcher(message).find()) {
            return true;
        }
        if (message.contains("?")) {
            String lower = message.toLowerCase(Locale.ROOT);
            return lower.contains("hủy") || lower.contains("hoàn") || lower.contains("cọc")
                    || lower.contains("phí") || lower.contains("chính sách") || lower.contains("quy định");
        }
        return false;
    }

    /**
     * Detects if the message intent attempts to perform write/hold/payment/cancellation actions.
     * Genuine policy questions (cancellation, refunds, deposit rules) are distinguished and not blocked as out-of-scope.
     */
    public boolean isOutOfScopeIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (SECURITY_OR_ADMIN_PATTERN.matcher(message).find()) {
            return true;
        }
        if (EXPLICIT_WRITE_COMMAND_PATTERN.matcher(message).find()) {
            return true;
        }
        if (isPolicyInquiry(message)) {
            return false;
        }
        return GENERAL_WRITE_OR_MUTATION_PATTERN.matcher(message).find();
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
                String currentLocation = criteria.getLocationKeyword();
                String requestedLocation = patch.getLocationKeyword();
                boolean locationChanged = !Objects.equals(
                        currentLocation == null ? null : currentLocation.trim().toLowerCase(Locale.ROOT),
                        requestedLocation.trim().toLowerCase(Locale.ROOT));
                if (locationChanged && patch.getHotelId() == null) {
                    criteria.setHotelId(null);
                    evidenceMap.remove("hotelId");
                }
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
                    .missingFields(List.of("location", "locationKeyword", "checkInDate", "checkOutDate", "adults"))
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
                    .reply("Hiện chỉ hỗ trợ tìm một phòng cho mỗi phương án. Quý khách muốn đặt từ 2 phòng trở lên vui lòng nhấn [Liên hệ Báo Giá Đoàn] hoặc gọi Hotline để nhận ưu đãi tốt nhất.")
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
            missingFields.add("location");
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
                    .reply(missingCriteriaReply(missingFields))
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
                    .reply(budgetClarificationReply(budget))
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

    private String missingCriteriaReply(List<String> fields) {
        Set<String> labels = new LinkedHashSet<>();
        for (String field : fields) {
            labels.add(switch (field) {
                case "location", "locationKeyword" -> "khách sạn hoặc khu vực";
                case "checkInDate" -> "ngày nhận phòng";
                case "checkOutDate" -> "ngày trả phòng";
                case "adults" -> "số người lớn";
                default -> "thông tin tìm phòng";
            });
        }
        return "Bạn cho mình biết thêm " + String.join(", ", labels) + " nhé.";
    }

    private String budgetClarificationReply(BudgetDto budget) {
        List<String> questions = new ArrayList<>();
        if (budget.getAmount() == null || !isValidAmount(budget.getAmount())) {
            questions.add("Bạn muốn giới hạn ở mức bao nhiêu?");
        }
        if (budget.getCurrency() == null || !CURRENCY_PATTERN.matcher(budget.getCurrency()).matches()) {
            questions.add("Bạn dùng đơn vị tiền nào?");
        }
        if (budget.getBasis() == null) {
            questions.add("Bạn muốn tính ngân sách cho cả kỳ hay trung bình mỗi đêm?");
        }
        if (budget.getCoverage() == null) {
            questions.add("Bạn muốn so với báo giá hệ thống hay yêu cầu bao gồm mọi khoản bắt buộc?");
        }
        return String.join(" ", questions);
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
