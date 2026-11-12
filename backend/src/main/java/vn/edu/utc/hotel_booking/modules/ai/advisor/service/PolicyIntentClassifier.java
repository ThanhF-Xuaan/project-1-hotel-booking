package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.ComparisonFocus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.UnifiedIntent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic intent classifier and clause separator for unified room advisor.
 *
 * <p>Separates natural language messages into search criteria text (routed to R1 room advisor)
 * and policy inquiry questions (routed to R2 RAG knowledge), without stripping room criteria
 * (such as children/guest counts) or incorrectly rejecting cancellation inquiries.</p>
 */
@Service
public class PolicyIntentClassifier {

    private static final Pattern SECURITY_ATTACK_PATTERN = Pattern.compile(
            "(?iu)\\b(delete\\s+from|insert\\s+into|update\\s+\\w+\\s+set|drop\\s+table|https?://|javascript:|<script)\\b"
    );

    private static final Pattern EXPLICIT_WRITE_COMMAND_PATTERN = Pattern.compile(
            "(?iu)(?:hãy\\s+)?(?:đặt|dat|giữ|giu|hủy|huy|thanh\\s*toán|thanh\\s*toan)\\s+.*?(?:mã\\s*\\w+|id\\s*\\w+|đơn\\s*\\w+|don\\s*\\w+|cho\\s*tôi(?!\\s*(?:biết|hỏi|xem))|giúp\\s*tôi(?!\\s*(?:biết|hỏi|xem))|hộ\\s*tôi|ngay|luôn)\\b|" +
                    "(?:hủy|huy)\\s*phòng\\s*(?:mã|id|cho\\s*tôi|giúp\\s*tôi|hộ\\s*tôi|ngay|luôn|này|nay)\\b|" +
                    "(?:đặt|dat|giữ|giu)\\s*phòng\\s*(?:này|nay|cho\\s*tôi|giúp\\s*tôi|hộ\\s*tôi|ngay|luôn)\\b|" +
                    "\\b(?:book\\s+(?:now|for\\s+me|this)|cancel\\s+(?:booking|reservation|order|my)|pay\\s+now)\\b"
    );

    private static final Pattern POLICY_INQUIRY_MARKERS = Pattern.compile(
            "(?iu)(?:chính\\s*sách|chinh\\s*sach|quy\\s*định|quy\\s*dinh|điều\\s*khoản|dieu\\s*khoan|điều\\s*kiện|dieu\\s*kien|" +
                    "mấy\\s*giờ|may\\s*gio|khi\\s*nào|khi\\s*nao|bao\\s*giờ|bao\\s*gio|bao\\s*lâu|bao\\s*lau|" +
                    "thế\\s*nào|the\\s*nao|ra\\s*sao|như\\s*thế\\s*nào|nhu\\s*the\\s*nao|" +
                    "mở\\s*cửa|mo\\s*cua|đóng\\s*cửa|dong\\s*cua|phục\\s*vụ|phuc\\s*vu|" +
                    "có\\s*(?:được|cho\\s*phép|miễn\\s*phí|hoàn|trả)?.*?không|co\\s*(?:duoc|cho\\s*phep|mien\\s*phi|hoan|tra)?.*?khong|" +
                    "được\\s*không|duoc\\s*khong|check[- ]?in|check[- ]?out|nhận\\s*phòng|tra\\s*phong|trả\\s*phòng|" +
                    "buffet|bữa\\s*sáng|ăn\\s*sáng|an\\s*sang|hồ\\s*bơi|bể\\s*bơi|ho\\s*boi|be\\s*boi|" +
                    "thú\\s*cưng|thu\\s*cung|chó\\s*mèo|cho\\s*meo|hút\\s*thuốc|hut\\s*thuoc|đỗ\\s*xe|do\\s*xe|gửi\\s*xe|gui\\s*xe|" +
                    "phụ\\s*thu|phu\\s*thu|miễn\\s*phí|mien\\s*phi|giường\\s*phụ|giuong\\s*phu|" +
                    "hủy\\s*phòng|huy\\s*phong|hoàn\\s*tiền|hoan\\s*tien|hoàn\\s*cọc|hoan\\s*coc|trả\\s*cọc|tra\\s*coc|lấy\\s*lại\\s*cọc|lay\\s*lai\\s*coc|rút\\s*cọc|rut\\s*coc|phí\\s*hủy|phi\\s*huy|mất\\s*phí|mat\\s*phi|tính\\s*phí|tinh\\s*phi|đổi\\s*lịch|doi\\s*lich|đổi\\s*ngày|doi\\s*ngay|dời\\s*ngày|doi\\s*ngay)"
    );

    private static final Pattern DYNAMIC_PRICING_INQUIRY_PATTERN = Pattern.compile(
            "(?iu)\\b(giá\\s*phòng\\s*bao\\s*nhiêu|gia\\s*phong\\s*bao\\s*nhieu|còn\\s*phòng\\s*(?:không|ko)|con\\s*phong\\s*(?:khong|ko))\\b"
    );

    private static final Pattern SEARCH_REQUEST_PATTERN = Pattern.compile(
            "(?iu)\\b(?:tìm(?:\\s+kiếm)?|tim(?:\\s+kiem)?|cần|can|search|find)\\s+"
                    + "(?:(?:giúp|giup|cho|tôi|toi|for|me|một|mot|a|1)\\s+){0,4}"
                    + "(?:phòng|phong|khách\\s*sạn|khach\\s*san|rooms?|hotels?|deluxe|suite)\\b");

    private static final Pattern SEARCH_CRITERIA_PATTERN = Pattern.compile(
            "(?iu)\\b(tìm|tim|phòng|phong|khách\\s*sạn|khach\\s*san|đà\\s*nẵng|da\\s*nang|hà\\s*nội|ha\\s*noi|" +
                    "người\\s*lớn|nguoi\\s*lon|trẻ\\s*em|tre\\s*em|bé|be|cuối\\s*tuần|cuoi\\s*tuan|" +
                    "đêm|dem|ngày|ngay|tháng|thang|deluxe|suite|standard|king|queen|twin|single|double)\\b"
    );

    private static final Pattern COMPARISON_MARKERS = Pattern.compile(
            "(?Uiu)(?:\\b(?:so\\s*sánh|so\\s*sanh|đối\\s*chiếu|doi\\s*chieu|phân\\s*vân\\s*giữa|phan\\s*van\\s*giua|compare|comparison)\\b|" +
                    "\\b(?:nên|nen)\\s*(?:ở|o|chọn|chon)\\s*(?:hạng\\s*|loại\\s*)?phòng\\s*nào\\b|" +
                    "\\b(?:hạng\\s*|loại\\s*)?phòng\\s*nào\\s*(?:hơn|tốt\\s*hơn|rẻ\\s*hơn|rộng\\s*hơn|phù\\s*hợp\\s*hơn|kinh\\s*tế\\s*hơn|đáng\\s*tiền\\s*hơn)\\b|" +
                    "\\bchọn\\s+(?:phòng\\s+)?(?:deluxe|executive|suite|superior|family|standard|dlx|exe|fam|sup|std|ste|sui)\\s+hay\\b|" +
                    "(?:(?:deluxe|executive|suite|superior|family|standard|dlx|exe|fam|sup|std|ste|sui|hai\\s*phòng|2\\s*phòng|các\\s*phòng|hạng\\s*phòng)\\s*.*?\\b(?:khác\\s*(?:gì\\s*nhau|gì|nhau)|hơn\\s*kém)\\b)|" +
                    "(?:\\b(?:khác\\s*(?:gì\\s*nhau|gì|nhau)|hơn\\s*kém)\\b.*?\\b(?:deluxe|executive|suite|superior|family|standard|dlx|exe|fam|sup|std|ste|sui|hai\\s*phòng|2\\s*phòng|các\\s*phòng|hạng\\s*phòng)\\b))"
    );

    /**
     * Matches only explicit new search criteria (verb, location, dates, quantified guests, or budget amount)
     * so follow-up comparison questions containing room names or "con nhỏ" stay ROOM_COMPARISON.
     */
    private static final Pattern NEW_SEARCH_FOR_COMPARISON_PATTERN = Pattern.compile(
            "(?iu)(?:\\b(?:tìm(?!\\s*hiểu)|tim(?!\\s*hieu)|tìm\\s*kiếm|tim\\s*kiem|kiếm\\s*phòng|kiem\\s*phong|cần\\s*tìm|can\\s*tim|search|find)\\b|" +
                    "\\b(?:đà\\s*nẵng|da\\s*nang|hà\\s*nội|ha\\s*noi|hạ\\s*long|ha\\s*long|sapa|sa\\s*pa|phú\\s*quốc|phu\\s*quoc|nha\\s*trang|hội\\s*an|hoi\\s*an|" +
                    "hồ\\s*chí\\s*minh|ho\\s*chi\\s*minh|sài\\s*gòn|sai\\s*gon|đà\\s*lạt|da\\s*lat|vũng\\s*tàu|vung\\s*tau|huế|hue)\\b|" +
                    "(?:\\d{1,2}[/.-]\\d{1,2}(?:[/.-]\\d{2,4})?|\\d{4}-\\d{2}-\\d{2}|\\b(?:từ\\s*ngày|tu\\s*ngay|đến\\s*ngày|den\\s*ngay|cuối\\s*tuần|cuoi\\s*tuan|hôm\\s*nay|hom\\s*nay|ngày\\s*mai|ngay\\s*mai|\\d+\\s*đêm|\\d+\\s*dem)\\b)|" +
                    "\\b(?:cho\\s+\\d+\\s*(?:người|nguoi|khách|khach|bé|be|trẻ|tre)|\\d+\\s*(?:người\\s*lớn|nguoi\\s*lon|trẻ\\s*em|tre\\s*em))\\b|" +
                    "\\b(?:\\d+(?:[.,]\\d+)?\\s*(?:triệu|tr\\b|nghìn|k\\b|vnd|đ\\b)|ngân\\s*sách\\s+\\d+))"
    );

    private static final Pattern ROOM_TYPE_HINT_COMPOUND_PATTERN = Pattern.compile(
            "(?iu)\\b(executive\\s+suite|deluxe|superior|family(?:\\s+room)?|standard|suite|dlx|exe|sup|fam|std|ste|sui)\\b"
    );

    private static final Pattern FOCUS_FAMILY_KIDS = Pattern.compile(
            "(?Uiu)(?:\\b(?:con\\s*nhỏ|con\\s*nho|trẻ\\s*em|tre\\s*em|em\\s*bé|em\\s*be|gia\\s*đình|gia\\s*dinh|chạy\\s*nhảy|chay\\s*nhay|giường\\s*phụ|giuong\\s*phu)\\b|" +
                    "\\b(?:có|co|mang|kèm|kem|cho|dắt|dat|dẫn|dan|cùng|cung|\\d+)\\s*(?:em\\s*)?(?:bé|be)\\b|" +
                    "\\b(?:bé|be)\\s*(?:nhỏ|nho|mầm\\s*non|mam\\s*non|sơ\\s*sinh|so\\s*sinh|gái|gai|trai|\\d+\\s*tuổi|\\d+\\s*tuoi|\\d+\\s*tháng|\\d+\\s*thang)\\b|" +
                    "\\bnhà\\s*có\\s*bé\\b|\\bnha\\s*co\\s*be\\b)"
    );

    private static final Pattern FOCUS_BUDGET_SAVER = Pattern.compile(
            "(?Uiu)(?:tiết\\s*kiệm|tiet\\s*kiem|tối\\s*ưu\\s*chi\\s*phí|toi\\s*uu\\s*chi\\s*phi|rẻ\\s*hơn|re\\s*hon|kinh\\s*tế|kinh\\s*te|ngân\\s*sách|ngan\\s*sach|đi\\s*tour|di\\s*tour|cheaper|budget)"
    );

    private static final Pattern FOCUS_SPACE_COMFORT = Pattern.compile(
            "(?Uiu)(?:rộng(?:\\s*(?:hơn|rãi))?|rong(?:\\s*(?:hon|rai))?|thoải\\s*mái|thoai\\s*mai|nghỉ\\s*dưỡng|nghi\\s*duong|bồn\\s*tắm|bon\\s*tam|sang\\s*trọng|sang\\s*trong|diện\\s*tích|dien\\s*tich|\\bbé\\s*(?:quá|hơn)\\b|\\bbe\\s*(?:qua|hon)\\b|spacious|larger)"
    );

    private static final Pattern FOCUS_WORK_TRIP = Pattern.compile(
            "(?Uiu)(?:công\\s*tác|cong\\s*tac|làm\\s*việc|lam\\s*viec|yên\\s*tĩnh|yen\\s*tinh|business)"
    );

    private static final Pattern COMPARISON_ROOM_COUNT_PATTERN = Pattern.compile(
            "(?Uiu)\\b(?:so\\s*sánh|so\\s*sanh|đối\\s*chiếu|doi\\s*chieu|phân\\s*vân\\s*giữa|phan\\s*van\\s*giua|giữa|giua)\\s+([23])\\s*(?:hạng\\s*|loại\\s*)?(?:phòng|phong|rooms?)\\b"
    );

    @Value
    @Builder
    public static class ClassificationResult {
        UnifiedIntent intent;
        String searchText;
        String policyQuestion;
        boolean writeIntent;
        @Builder.Default
        List<String> targetRoomHints = List.of();
        @Builder.Default
        ComparisonFocus comparisonFocus = ComparisonFocus.GENERAL;
    }

    public ClassificationResult classifyAndSplit(String message) {
        if (message == null || message.isBlank()) {
            return ClassificationResult.builder()
                    .intent(UnifiedIntent.SEARCH_ONLY)
                    .searchText("")
                    .policyQuestion(null)
                    .writeIntent(false)
                    .build();
        }

        String raw = message.trim();

        // 1. Strict security check
        if (SECURITY_ATTACK_PATTERN.matcher(raw).find()) {
            return ClassificationResult.builder()
                    .intent(UnifiedIntent.OUT_OF_SCOPE)
                    .searchText(raw)
                    .policyQuestion(null)
                    .writeIntent(true)
                    .build();
        }

        // 2. Explicit write/mutation commands (without exemptions)
        if (EXPLICIT_WRITE_COMMAND_PATTERN.matcher(raw).find()) {
            return ClassificationResult.builder()
                    .intent(UnifiedIntent.OUT_OF_SCOPE)
                    .searchText(raw)
                    .policyQuestion(null)
                    .writeIntent(true)
                    .build();
        }

        // 3. Split clauses without breaking guest count ("và 1 bé 4 tuổi") or amenities
        List<String> clauses = splitClausesSafely(raw);

        StringBuilder searchPart = new StringBuilder();
        StringBuilder policyPart = new StringBuilder();
        boolean hasComparison = false;

        for (String clause : clauses) {
            String trimmed = clause.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (isComparisonClause(trimmed)) {
                hasComparison = true;
                if (!searchPart.isEmpty()) {
                    searchPart.append(", ");
                }
                searchPart.append(trimmed);
                continue;
            }

            boolean isPolicy = isPolicyClause(trimmed);
            boolean isDynamicPrice = DYNAMIC_PRICING_INQUIRY_PATTERN.matcher(trimmed).find();

            // Dynamic price inquiries belong to Core, never RAG
            if (isPolicy && !isDynamicPrice) {
                if (!policyPart.isEmpty()) {
                    policyPart.append(" ");
                }
                policyPart.append(trimmed);
            } else {
                if (!searchPart.isEmpty()) {
                    searchPart.append(", ");
                }
                searchPart.append(trimmed);
            }
        }

        if (!hasComparison && isComparisonClause(raw)) {
            hasComparison = true;
        }

        String searchResult = searchPart.toString().trim();
        String policyResult = policyPart.toString().trim();

        if (hasComparison) {
            List<String> hints = extractRoomHints(raw);
            ComparisonFocus focus = detectComparisonFocus(raw);
            boolean hasNewSearch = NEW_SEARCH_FOR_COMPARISON_PATTERN.matcher(raw).find();
            UnifiedIntent compIntent = hasNewSearch ? UnifiedIntent.SEARCH_AND_COMPARISON : UnifiedIntent.ROOM_COMPARISON;
            String sanitizedSearch = stripComparisonRoomCount(searchResult.isEmpty() ? raw : searchResult);
            return ClassificationResult.builder()
                    .intent(compIntent)
                    .searchText(sanitizedSearch)
                    .policyQuestion(policyResult.isEmpty() ? null : policyResult)
                    .writeIntent(false)
                    .targetRoomHints(hints)
                    .comparisonFocus(focus)
                    .build();
        }

        boolean hasSearch = !searchResult.isEmpty() && SEARCH_CRITERIA_PATTERN.matcher(searchResult).find();
        boolean hasPolicy = !policyResult.isEmpty();

        UnifiedIntent intent;
        if (hasSearch && hasPolicy) {
            intent = UnifiedIntent.SEARCH_AND_POLICY;
        } else if (hasPolicy) {
            intent = UnifiedIntent.POLICY_ONLY;
        } else {
            intent = UnifiedIntent.SEARCH_ONLY;
            searchResult = raw;
        }

        return ClassificationResult.builder()
                .intent(intent)
                .searchText(searchResult.isEmpty() ? raw : searchResult)
                .policyQuestion(hasPolicy ? policyResult : null)
                .writeIntent(false)
                .build();
    }

    public boolean hasExplicitPolicyMarker(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return POLICY_INQUIRY_MARKERS.matcher(text).find();
    }

    private boolean isPolicyClause(String clause) {
        if (isComparisonClause(clause)) {
            return false;
        }
        // A requested room feature is a Core criterion, even when its name is also a policy topic.
        if (SEARCH_REQUEST_PATTERN.matcher(clause).find()
                || isSubordinateGuestOrAmenityClause(clause)) {
            return false;
        }
        if (POLICY_INQUIRY_MARKERS.matcher(clause).find()) {
            return true;
        }
        if (clause.contains("?")) {
            String trimmed = clause.trim();
            boolean isExplicitSearchPrompt = trimmed.matches("(?iu)^(?:tìm|tim|cần|can|search)\\s+.*");
            return !isExplicitSearchPrompt;
        }
        return false;
    }

    private boolean isComparisonClause(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return COMPARISON_MARKERS.matcher(text).find();
    }

    /**
     * Splits sentences by punctuation or conjunctions without separating guest counters or amenities.
     */
    private List<String> splitClausesSafely(String text) {
        List<String> result = new ArrayList<>();
        // Split by question marks or periods or semicolons first while keeping '?' on the preceding clause
        String[] majorSentences = text.split("(?<=[?;])(?![?;])\\s*|(?<!\\d)\\.+\\s*|\\.(?!\\d)\\s*");

        for (String sentence : majorSentences) {
            String s = sentence.trim();
            if (s.isEmpty()) {
                continue;
            }

            // Look for comma or " và " or " cho tôi hỏi " boundaries
            String[] tokens = s.split("(?iu)(?:(?<!\\d),\\s*|,(?!\\d)\\s*|\\s+và\\s+|\\s+cho\\s+tôi\\s+hỏi\\s+)");
            if (tokens.length <= 1) {
                result.add(s);
                continue;
            }

            StringBuilder currentClause = new StringBuilder(tokens[0]);
            for (int i = 1; i < tokens.length; i++) {
                String next = tokens[i].trim();
                boolean currentIsPolicy = isPolicyClause(currentClause.toString());
                boolean nextIsPolicy = isPolicyClause(next);

                if (nextIsPolicy) {
                    result.add(currentClause.toString().trim());
                    currentClause = new StringBuilder(next);
                } else if (currentIsPolicy) {
                    result.add(currentClause.toString().trim());
                    currentClause = new StringBuilder(next);
                } else if (isSubordinateGuestOrAmenityClause(next)) {
                    // Keep attached to search criteria
                    currentClause.append(" và ").append(next);
                } else {
                    currentClause.append(", ").append(next);
                }
            }
            if (!currentClause.isEmpty()) {
                result.add(currentClause.toString().trim());
            }
        }

        return result;
    }

    private boolean isSubordinateGuestOrAmenityClause(String clause) {
        // Questions about a feature remain knowledge inquiries; bare requested features do not.
        if (clause.contains("?") || clause.matches("(?iu).*(?:mấy\\s*giờ|may\\s*gio|bao\\s*nhiêu|bao\\s*nhieu|"
                + "khi\\s*nào|khi\\s*nao|thế\\s*nào|the\\s*nao|chính\\s*sách|chinh\\s*sach|"
                + "quy\\s*định|quy\\s*dinh|phụ\\s*thu|phu\\s*thu|mở\\s*cửa|mo\\s*cua|"
                + "đóng\\s*cửa|dong\\s*cua|phục\\s*vụ|phuc\\s*vu|không$|khong$).*$")) {
            return false;
        }
        String lower = clause.toLowerCase(Locale.ROOT);
        // Continuation fragments after "và" often start with a request verb rather than the
        // amenity noun itself: "cần 1 giường phụ", "thêm 1 giường phụ", "có ăn sáng".
        if (lower.matches("^(?:cần|can|thêm|them|có|co|kèm|kem)\\s+(?:\\d+\\s+)?"
                + "(?:giường\\s*phụ|giuong\\s*phu|extra\\s*beds?|ăn\\s*sáng|an\\s*sang|"
                + "bữa\\s*sáng|buffet|wifi|hồ\\s*bơi|ho\\s*boi|bể\\s*bơi|be\\s*boi).*")) {
            return true;
        }
        // Matches "1 bé 4 tuổi", "2 người lớn", "1 trẻ em", "có wifi", "hồ bơi", "view biển"
        if (lower.matches("^\\d+\\s*(?:bé|be|trẻ\\s*em|tre\\s*em|người|nguoi|khách|khach|em|lớn|lon|giường\\s*phụ|giuong\\s*phu|extra\\s*beds?).*")) {
            return true;
        }
        return lower.matches("^(?:có\\s+|kèm\\s+)?(?:wifi|ban\\s*công|ban\\s*cong|view\\s*biển|view\\s*bien|hướng\\s*biển|huong\\s*bien|hồ\\s*bơi|ho\\s*boi|bể\\s*bơi|be\\s*boi|ăn\\s*sáng|an\\s*sang|bữa\\s*sáng|buffet|giường\\s*phụ|giuong\\s*phu).*");
    }

    public List<String> extractRoomHints(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        Set<String> hints = new LinkedHashSet<>();
        Matcher matcher = ROOM_TYPE_HINT_COMPOUND_PATTERN.matcher(message);
        while (matcher.find()) {
            String token = matcher.group(1).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            if (token.equals("executive suite")) {
                hints.add("executive");
            } else if (token.startsWith("family")) {
                hints.add("family");
            } else if (token.equals("ste") || token.equals("sui")) {
                hints.add("suite");
            } else {
                hints.add(token);
            }
        }
        return List.copyOf(hints);
    }

    public static String stripComparisonRoomCount(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return COMPARISON_ROOM_COUNT_PATTERN.matcher(text).replaceAll("so sánh các phòng");
    }

    public ComparisonFocus detectComparisonFocus(String message) {
        if (message == null || message.isBlank()) {
            return ComparisonFocus.GENERAL;
        }
        if (FOCUS_FAMILY_KIDS.matcher(message).find()) {
            return ComparisonFocus.FAMILY_KIDS;
        }
        if (FOCUS_BUDGET_SAVER.matcher(message).find()) {
            return ComparisonFocus.BUDGET_SAVER;
        }
        if (FOCUS_SPACE_COMFORT.matcher(message).find()) {
            return ComparisonFocus.SPACE_COMFORT;
        }
        if (FOCUS_WORK_TRIP.matcher(message).find()) {
            return ComparisonFocus.WORK_TRIP;
        }
        return ComparisonFocus.GENERAL;
    }
}
