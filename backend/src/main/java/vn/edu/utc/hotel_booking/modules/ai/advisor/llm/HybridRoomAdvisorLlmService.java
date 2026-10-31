package vn.edu.utc.hotel_booking.modules.ai.advisor.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.AdvisorResultDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.BudgetDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.CriteriaPatchDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.RoomSearchCriteriaDto;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.AdvisorStatus;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetBasis;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.BudgetCoverage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class HybridRoomAdvisorLlmService implements RoomAdvisorLlmPort {

    private static final URI GEMINI_ENDPOINT = URI.create(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
    static final int MAX_RESPONSE_BYTES = 65_536;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AdvisorProviderProperties provider;
    private final HttpClient httpClient;
    private final Supplier<String> apiKey;

    /** Offline construction for deterministic callers and evaluation. */
    public HybridRoomAdvisorLlmService() {
        this(new AdvisorProviderProperties());
    }

    @Autowired
    public HybridRoomAdvisorLlmService(AdvisorProviderProperties provider) {
        this(provider, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build(), () -> System.getenv("GEMINI_API_KEY"));
    }

    HybridRoomAdvisorLlmService(AdvisorProviderProperties provider, HttpClient httpClient, Supplier<String> apiKey) {
        this.provider = Objects.requireNonNull(provider);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.apiKey = Objects.requireNonNull(apiKey);
    }

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern VN_DATE_PATTERN = Pattern.compile("\\b(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})\\b");
    private static final Pattern DATE_RANGE_VN_PATTERN = Pattern.compile(
            "\\b(\\d{1,2})\\s*[-–—]\\s*(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})\\b"
    );

    private static final Pattern ADULTS_PATTERN = Pattern.compile("(?i)(\\d+)\\s*(?:người\\s*lớn|nguoi\\s*lon|adults?)");
    private static final Pattern CHILDREN_PATTERN = Pattern.compile("(?i)(\\d+)\\s*(?:trẻ\\s*em|tre\\s*em|children|child)");
    private static final Pattern NO_CHILDREN_PATTERN = Pattern.compile("(?i)(?:không|khong|no|0)\\s*(?:có\\s*)?(?:trẻ\\s*em|tre\\s*em|children|child)");
    private static final Pattern INFANTS_PATTERN = Pattern.compile("(?i)(\\d+)\\s*(?:em\\s*bé|em\\s*be|trẻ\\s*sơ\\s*sinh|infants?|bab(?:y|ies))");
    private static final Pattern NO_INFANTS_PATTERN = Pattern.compile("(?i)(?:không|khong|no|0)\\s*(?:có\\s*)?(?:em\\s*bé|em\\s*be|trẻ\\s*sơ\\s*sinh|infants?|bab(?:y|ies))");
    private static final Pattern EXTRA_BEDS_PATTERN = Pattern.compile("(?i)(\\d+)\\s*(?:giường\\s*phụ|giuong\\s*phu|extra\\s*beds?)");
    private static final Pattern NO_EXTRA_BEDS_PATTERN = Pattern.compile("(?i)(?:không|khong|no|0)\\s*(?:có\\s*)?(?:giường\\s*phụ|giuong\\s*phu|extra\\s*beds?)");
    private static final Pattern ROOMS_COUNT_PATTERN = Pattern.compile("(?i)(\\d+)\\s*(?:phòng|phong|rooms?)");

    private static final Pattern BUDGET_PATTERN_1 = Pattern.compile(
            "(?i)(?:ngân\\s*sách|giá|tài\\s*chính|budget)(?:\\s*(?:là|dưới|tối\\s*đa|không\\s*quá|khoảng|tầm|từ|<|<=))?\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|k|nghìn|vnd|đồng)?");

    private static final Pattern BUDGET_PATTERN_2 = Pattern.compile(
            "(?i)(?:dưới|tối\\s*đa|không\\s*quá|tài\\s*chính)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|k|nghìn|vnd|đồng)?");

    private static final Pattern BUDGET_PATTERN_3 = Pattern.compile(
            "(?i)(?:khoảng|tầm)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|k|nghìn|vnd|đồng)");

    private static final List<String> COMMON_DESTINATIONS = List.of(
            "Đà Nẵng", "Hà Nội", "Nha Trang", "Phú Quốc", "Hồ Chí Minh",
            "Sài Gòn", "Synthetic City", "Central", "Hội An", "Đà Lạt", "Vũng Tàu", "Huế", "Hạ Long"
    );

    @Override
    public CriteriaPatchDto extractCriteriaPatch(String message, RoomSearchCriteriaDto currentCriteria) {
        String redactedMessage = PiiRedactionUtil.redact(message);

        // Check opt-in before even reading credentials. No provider retry on failure.
        if (provider.permitsExternalCall() && !Thread.currentThread().isInterrupted()) {
            String geminiApiKey = apiKey.get();
            if (geminiApiKey != null && !geminiApiKey.isBlank()) {
                CriteriaPatchDto geminiResult = callGeminiExtraction(redactedMessage, geminiApiKey);
                if (geminiResult != null) {
                    return geminiResult;
                }
            }
        }

        // Fallback to deterministic NLU extraction
        return extractDeterministically(redactedMessage, currentCriteria);
    }

    @Override
    public String generateReply(AdvisorResultDto result, String userMessage) {
        if (result == null || result.getStatus() == null) {
            return "Có thể xem chi tiết các phương án phòng. Giá và tồn phòng cần kiểm tra lại.";
        }

        AdvisorStatus status = result.getStatus();
        switch (status) {
            case OUT_OF_SCOPE:
                return "Trợ lý hỗ trợ tìm và xem thông tin phòng. Các thao tác đặt hoặc giữ phòng cần thực hiện tại giao diện thông thường.";

            case NEED_INFO:
                if (result.getMissingFields() != null && result.getMissingFields().contains("roomsCount")) {
                    return "Hiện chỉ hỗ trợ tìm một phòng cho mỗi phương án. Có thể tìm một phòng hoặc dùng giao diện tìm phòng thông thường.";
                }
                if (result.getAmbiguities() != null && !result.getAmbiguities().isEmpty()) {
                    return "Dạ, có nhiều điểm đến phù hợp với yêu cầu của bạn. Bạn vui lòng chọn rõ khách sạn hoặc khu vực cụ thể nhé.";
                }
                if (result.getMissingFields() != null && !result.getMissingFields().isEmpty()) {
                    return "Vui lòng cung cấp thêm thông tin còn thiếu để tìm phòng: " + String.join(", ", result.getMissingFields()) + ".";
                }
                return result.getReply() != null ? result.getReply() : "Vui lòng cung cấp thêm thông tin tìm kiếm phòng.";

            case RESULTS:
                int count = result.getRecommendations() != null ? result.getRecommendations().size() : 0;
                return "Có thể xem chi tiết các phương án phòng (" + count + " phòng phù hợp). Giá và tồn phòng cần kiểm tra lại.";

            case NO_MATCH:
                return "Chưa tìm thấy phòng phù hợp trong phạm vi đã kiểm tra đầy đủ. Có thể điều chỉnh nhu cầu.";

            case INCOMPLETE_SEARCH:
            case TEMP_UNAVAILABLE:
            default:
                return "Chưa kiểm tra được đầy đủ các phương án. Cần thử lại để có thêm kết quả.";
        }
    }

    private CriteriaPatchDto extractDeterministically(String message, RoomSearchCriteriaDto currentCriteria) {
        CriteriaPatchDto.CriteriaPatchDtoBuilder builder = CriteriaPatchDto.builder();
        boolean hasPatch = false;

        // 1. Dates
        List<LocalDate> dates = extractDates(message);
        if (dates.size() >= 2) {
            builder.checkInDate(dates.get(0));
            builder.checkOutDate(dates.get(1));
            hasPatch = true;
        } else if (dates.size() == 1) {
            builder.checkInDate(dates.get(0));
            builder.checkOutDate(dates.get(0).plusDays(1));
            hasPatch = true;
        }

        // 2. Guests & Capacity
        Matcher mAdults = ADULTS_PATTERN.matcher(message);
        if (mAdults.find()) {
            builder.adults(Integer.parseInt(mAdults.group(1)));
            hasPatch = true;
        }

        Matcher mChildren = CHILDREN_PATTERN.matcher(message);
        if (mChildren.find()) {
            builder.children(Integer.parseInt(mChildren.group(1)));
            hasPatch = true;
        } else if (NO_CHILDREN_PATTERN.matcher(message).find()) {
            builder.children(0);
            hasPatch = true;
        }

        Matcher mInfants = INFANTS_PATTERN.matcher(message);
        if (mInfants.find()) {
            builder.infants(Integer.parseInt(mInfants.group(1)));
            hasPatch = true;
        } else if (NO_INFANTS_PATTERN.matcher(message).find()) {
            builder.infants(0);
            hasPatch = true;
        }

        Matcher mExtraBeds = EXTRA_BEDS_PATTERN.matcher(message);
        if (mExtraBeds.find()) {
            builder.extraBeds(Integer.parseInt(mExtraBeds.group(1)));
            hasPatch = true;
        } else if (NO_EXTRA_BEDS_PATTERN.matcher(message).find()) {
            builder.extraBeds(0);
            hasPatch = true;
        }

        Matcher mRooms = ROOMS_COUNT_PATTERN.matcher(message);
        if (mRooms.find()) {
            builder.roomsCount(Integer.parseInt(mRooms.group(1)));
            hasPatch = true;
        }

        // 3. Location Keyword
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        for (String dest : COMMON_DESTINATIONS) {
            if (lowerMessage.contains(dest.toLowerCase(Locale.ROOT))) {
                builder.locationKeyword(dest);
                hasPatch = true;
                break;
            }
        }

        // 4. Preferred Features
        List<String> preferred = new ArrayList<>();
        if (lowerMessage.contains("wifi") || lowerMessage.contains("internet")) {
            preferred.add("WIFI");
        }
        if (lowerMessage.contains("ban công") || lowerMessage.contains("balcony")) {
            preferred.add("BALCONY");
        }
        if (lowerMessage.contains("view biển") || lowerMessage.contains("hướng biển")
                || lowerMessage.contains("gần biển") || lowerMessage.contains("sea view")) {
            preferred.add("SEA_VIEW");
        }
        if (lowerMessage.contains("bồn tắm") || lowerMessage.contains("bathtub")) {
            preferred.add("BATHTUB");
        }
        if (lowerMessage.contains("ăn sáng") || lowerMessage.contains("buffet") || lowerMessage.contains("breakfast")) {
            preferred.add("BREAKFAST");
        }
        if (lowerMessage.contains("hồ bơi") || lowerMessage.contains("bể bơi") || lowerMessage.contains("pool")) {
            preferred.add("POOL");
        }

        if (!preferred.isEmpty()) {
            builder.preferredFeatureCodes(preferred);
            hasPatch = true;
        }

        // 5. Budget
        BudgetDto budget = extractBudget(message);
        if (budget != null) {
            builder.budget(budget);
            hasPatch = true;
        }

        return hasPatch ? builder.build() : null;
    }

    private List<LocalDate> extractDates(String message) {
        List<LocalDate> result = new ArrayList<>();

        // Date range pattern: dd-dd/MM/yyyy or dd–dd/MM/yyyy
        Matcher rangeMatcher = DATE_RANGE_VN_PATTERN.matcher(message);
        if (rangeMatcher.find()) {
            try {
                int d1 = Integer.parseInt(rangeMatcher.group(1));
                int d2 = Integer.parseInt(rangeMatcher.group(2));
                int m = Integer.parseInt(rangeMatcher.group(3));
                int y = Integer.parseInt(rangeMatcher.group(4));
                result.add(LocalDate.of(y, m, d1));
                result.add(LocalDate.of(y, m, d2));
                return result;
            } catch (Exception ignored) {}
        }

        // ISO pattern
        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(message);
        while (isoMatcher.find()) {
            try {
                result.add(LocalDate.parse(isoMatcher.group(1)));
            } catch (Exception ignored) {}
        }

        if (result.size() >= 2) {
            return result;
        }

        // VN pattern dd/MM/yyyy
        Matcher vnMatcher = VN_DATE_PATTERN.matcher(message);
        DateTimeFormatter vnFormatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        while (vnMatcher.find()) {
            try {
                String dStr = vnMatcher.group(1) + "/" + vnMatcher.group(2) + "/" + vnMatcher.group(3);
                result.add(LocalDate.parse(dStr, vnFormatter));
            } catch (Exception ignored) {}
        }

        return result;
    }

    private BudgetDto extractBudget(String message) {
        Matcher matcher = BUDGET_PATTERN_1.matcher(message);
        boolean found = matcher.find();
        if (!found) {
            matcher = BUDGET_PATTERN_2.matcher(message);
            found = matcher.find();
        }
        if (!found) {
            matcher = BUDGET_PATTERN_3.matcher(message);
            found = matcher.find();
        }
        if (!found) {
            return null;
        }

        String numStr = matcher.group(1).replace(",", ".");
        String unit = matcher.group(2) != null ? matcher.group(2).toLowerCase() : "";

        BigDecimal raw = new BigDecimal(numStr);
        BigDecimal amount;

        if (unit.contains("triệu") || unit.contains("tr")) {
            amount = raw.multiply(BigDecimal.valueOf(1_000_000));
        } else if (unit.contains("k") || unit.contains("nghìn")) {
            amount = raw.multiply(BigDecimal.valueOf(1_000));
        } else if (raw.compareTo(BigDecimal.valueOf(100)) <= 0) {
            // assume millions if small number like "ngân sách dưới 2"
            amount = raw.multiply(BigDecimal.valueOf(1_000_000));
        } else {
            amount = raw;
        }

        if (amount.scale() > 0 && amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            amount = amount.setScale(0, RoundingMode.UNNECESSARY);
        }

        String lower = message.toLowerCase(Locale.ROOT);

        // Coverage extraction
        BudgetCoverage coverage = BudgetCoverage.ALL_MANDATORY_CHARGES;
        if (lower.contains("theo tổng api") || lower.contains("không yêu cầu all-in") || lower.contains("khong yeu cau all-in")) {
            coverage = BudgetCoverage.BACKEND_QUOTED_TOTAL;
        }

        // Basis extraction
        BudgetBasis basis = null;
        if (lower.contains("trung bình mỗi đêm") || lower.contains("average per night") || lower.contains("trung binh moi dem")) {
            basis = BudgetBasis.AVERAGE_PER_NIGHT;
        } else if (lower.contains("mỗi đêm") || lower.contains("moi dem") || lower.contains("per night")) {
            // Ambiguous without "trung bình" (e.g. "dưới 900k mỗi đêm") - leave basis & coverage null to trigger NEED_INFO
            basis = null;
            coverage = null;
        } else {
            basis = BudgetBasis.STAY_TOTAL;
        }

        boolean hard = Pattern.compile("(?i)(tối\\s*đa|không\\s*quá|dưới|<|<=)").matcher(message).find();

        return BudgetDto.builder()
                .amount(amount.toPlainString())
                .currency("VND")
                .basis(basis)
                .coverage(coverage)
                .hard(hard)
                .build();
    }

    private CriteriaPatchDto callGeminiExtraction(String text, String apiKey) {
        CompletableFuture<HttpResponse<String>> pending = null;
        try {
            String prompt = "Extract hotel room search criteria from user message into JSON with keys: locationKeyword, checkInDate (yyyy-MM-dd), checkOutDate (yyyy-MM-dd), adults, children, infants, extraBeds, roomsCount, preferredFeatureCodes (array), budget (object with amount, currency, basis, coverage, hard). Return ONLY valid JSON: " + text;

            Map<String, Object> reqBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "candidateCount", 1,
                            "maxOutputTokens", provider.getMaxOutputTokens(),
                            "responseMimeType", "application/json")
            );

            String jsonPayload = objectMapper.writeValueAsString(reqBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(GEMINI_ENDPOINT)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .timeout(Duration.ofMillis(provider.getTimeoutMillis()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            pending = httpClient.sendAsync(request, responseInfo -> new BoundedBodySubscriber());
            HttpResponse<String> response = pending.get(provider.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidate = root.path("candidates").path(0);
                JsonNode candidateText = candidate.path("content").path("parts").path(0).path("text");
                if ("STOP".equals(candidate.path("finishReason").asText()) && candidateText.isTextual()) {
                    String rawJson = candidateText.asText().replaceAll("```json", "").replaceAll("```", "").trim();
                    CriteriaPatchDto patch = objectMapper.readValue(rawJson, CriteriaPatchDto.class);
                    if (patch != null && !objectMapper.valueToTree(patch).isEmpty()) return patch;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Provider exceptions may contain a key, URL, prompt or response body.
            log.warn("Gemini extraction unavailable; using deterministic extraction");
        } finally {
            if (pending != null && !pending.isDone()) pending.cancel(true);
        }
        return null;
    }

    /** Bounds bytes while receiving, including error responses; no file or unbounded body buffer. */
    static final class BoundedBodySubscriber implements HttpResponse.BodySubscriber<String> {
        private final CompletableFuture<String> body = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Flow.Subscription subscription;

        @Override public CompletionStage<String> getBody() { return body; }

        @Override public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override public void onNext(List<ByteBuffer> buffers) {
            if (body.isDone()) return;
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > MAX_RESPONSE_BYTES - bytes.size()) {
                    subscription.cancel();
                    body.completeExceptionally(new IOException("Provider response exceeded limit"));
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }

        @Override public void onError(Throwable error) { body.completeExceptionally(error); }
        @Override public void onComplete() { body.complete(bytes.toString(StandardCharsets.UTF_8)); }
    }
}
