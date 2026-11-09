package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.UnifiedIntent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
            "(?iu)(?:hãy\\s+)?(?:đặt|dat|giữ|giu|hủy|huy|thanh\\s*toán|thanh\\s*toan)\\s*(?:phòng|phong|đơn|don|booking|reservation|mã\\s*\\w+|ma\\s*\\w+|cho\\s*tôi|giúp\\s*tôi|ngay|này|nay)|" +
                    "(?:hủy|huy)\\s*phòng\\s*(?:mã|id|cho|giúp|giup|ngay|này|nay)|" +
                    "\\b(?:book\\s+(?:now|for\\s+me|this)|cancel\\s+(?:booking|reservation|order|my)|pay\\s+now)\\b"
    );

    private static final Pattern POLICY_EXEMPTION_FROM_WRITE_PATTERN = Pattern.compile(
            "(?iu)(?:chính\\s*sách|chinh\\s*sach|quy\\s*định|quy\\s*dinh|điều\\s*khoản|dieu\\s*khoan|điều\\s*kiện|dieu\\s*kien|" +
                    "như\\s*thế\\s*nào|nhu\\s*the\\s*nao|thế\\s*nào|the\\s*nao|ra\\s*sao|có\\s*được.*?không|co\\s*duoc.*?khong|\\?)"
    );

    private static final Pattern POLICY_INQUIRY_MARKERS = Pattern.compile(
            "(?iu)(?:chính\\s*sách|chinh\\s*sach|quy\\s*định|quy\\s*dinh|điều\\s*khoản|dieu\\s*khoan|điều\\s*kiện|dieu\\s*kien|" +
                    "mấy\\s*giờ|may\\s*gio|khi\\s*nào|khi\\s*nao|bao\\s*giờ|bao\\s*gio|bao\\s*lâu|bao\\s*lau|" +
                    "thế\\s*nào|the\\s*nao|ra\\s*sao|như\\s*thế\\s*nào|nhu\\s*the\\s*nao|" +
                    "mở\\s*cửa|mo\\s*cua|đóng\\s*cửa|dong\\s*cua|phục\\s*vụ|phuc\\s*vu|" +
                    "có\\s*(?:được|cho\\s*phép|miễn\\s*phí)?.*?không|co\\s*(?:duoc|cho\\s*phep|mien\\s*phi)?.*?khong|" +
                    "được\\s*không|duoc\\s*khong|check[- ]?in|check[- ]?out|nhận\\s*phòng|tra\\s*phong|trả\\s*phòng|" +
                    "buffet|bữa\\s*sáng|ăn\\s*sáng|an\\s*sang|hồ\\s*bơi|bể\\s*bơi|ho\\s*boi|be\\s*boi|" +
                    "thú\\s*cưng|thu\\s*cung|chó\\s*mèo|cho\\s*meo|hút\\s*thuốc|hut\\s*thuoc|đỗ\\s*xe|do\\s*xe|gửi\\s*xe|gui\\s*xe|" +
                    "phụ\\s*thu|phu\\s*thu|miễn\\s*phí|mien\\s*phi|giường\\s*phụ|giuong\\s*phu)"
    );

    private static final Pattern DYNAMIC_PRICING_INQUIRY_PATTERN = Pattern.compile(
            "(?iu)\\b(giá\\s*phòng\\s*bao\\s*nhiêu|gia\\s*phong\\s*bao\\s*nhieu|còn\\s*phòng\\s*(?:không|ko)|con\\s*phong\\s*(?:khong|ko))\\b"
    );

    private static final Pattern SEARCH_CRITERIA_PATTERN = Pattern.compile(
            "(?iu)\\b(tìm|tim|phòng|phong|khách\\s*sạn|khach\\s*san|đà\\s*nẵng|da\\s*nang|hà\\s*nội|ha\\s*noi|" +
                    "người\\s*lớn|nguoi\\s*lon|trẻ\\s*em|tre\\s*em|bé|be|cuối\\s*tuần|cuoi\\s*tuan|" +
                    "đêm|dem|ngày|ngay|tháng|thang|deluxe|suite|standard|king|queen|twin|single|double)\\b"
    );

    @Value
    @Builder
    public static class ClassificationResult {
        UnifiedIntent intent;
        String searchText;
        String policyQuestion;
        boolean writeIntent;
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

        // 2. Explicit write/mutation commands vs policy questions
        boolean isExplicitWrite = EXPLICIT_WRITE_COMMAND_PATTERN.matcher(raw).find();
        boolean isPolicyExempt = POLICY_EXEMPTION_FROM_WRITE_PATTERN.matcher(raw).find();

        if (isExplicitWrite && !isPolicyExempt) {
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

        for (String clause : clauses) {
            String trimmed = clause.trim();
            if (trimmed.isEmpty()) {
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

        String searchResult = searchPart.toString().trim();
        String policyResult = policyPart.toString().trim();

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

    private boolean isPolicyClause(String clause) {
        if (clause.contains("?")) {
            return true;
        }
        return POLICY_INQUIRY_MARKERS.matcher(clause).find();
    }

    /**
     * Splits sentences by punctuation or conjunctions without separating guest counters or amenities.
     */
    private List<String> splitClausesSafely(String text) {
        List<String> result = new ArrayList<>();
        // Split by question marks or periods or semicolons first
        String[] majorSentences = text.split("[?;.]+");

        for (String sentence : majorSentences) {
            String s = sentence.trim();
            if (s.isEmpty()) {
                continue;
            }

            // Look for comma or " và " or " cho tôi hỏi " boundaries
            String[] tokens = s.split("(?iu)(?:,\\s*|\\s+và\\s+|\\s+cho\\s+tôi\\s+hỏi\\s+)");
            if (tokens.length <= 1) {
                result.add(s);
                continue;
            }

            StringBuilder currentClause = new StringBuilder(tokens[0]);
            for (int i = 1; i < tokens.length; i++) {
                String next = tokens[i].trim();
                if (isPolicyClause(next)) {
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
        String lower = clause.toLowerCase(Locale.ROOT);
        // Matches "1 bé 4 tuổi", "2 người lớn", "1 trẻ em", "có wifi", "hồ bơi", "view biển"
        if (lower.matches("^\\d+\\s*(?:bé|be|trẻ\\s*em|tre\\s*em|người|nguoi|khách|khach|em|lớn|lon).*")) {
            return true;
        }
        return lower.matches("^(?:có\\s+)?(?:wifi|ban\\s*công|ban\\s*cong|view\\s*biển|view\\s*bien|hướng\\s*biển|huong\\s*bien|hồ\\s*bơi|ho\\s*boi).*");
    }
}
