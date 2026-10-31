package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagCitationDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestRequestDto;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RagGroundedAnswerService {

    private static final Pattern CODE_GEN_PATTERN = Pattern.compile(
            "(?iu)(viết.*code|write.*code|class.*public|public\\s+static\\s+void|đoạn\\s*code\\s*java)"
    );

    private static final Pattern DIRECT_INJECTION_PATTERN = Pattern.compile(
            "(?iu)(bỏ\\s*qua.*(lệnh|quy\\s*tắc)|ignore.*instructions|danh\\s*sách\\s*mật\\s*khẩu|system\\s*prompt|api\\s*key|debug\\s*mode)"
    );

    private static final Pattern GUEST_PII_PATTERN = Pattern.compile(
            "(?iu)(sđt\\s*khách|thông\\s*tin\\s*khách|khách\\s*ở\\s*phòng\\s*\\d+|phòng\\s*305|passport\\s*của\\s*khách)"
    );
    private static final Pattern NON_STATIC_REQUEST = Pattern.compile(
            "(?iu)(payroll|salary|bảng\\s*lương|guest.*(?:email|phone|passport)|employee.*(?:salary|payroll)|giá\\s*phòng\\s*hiện\\s*tại|"
                    + "how\\s+many\\s+rooms.*available|(?:create|cancel|hold)\\s+(?:a\\s+)?(?:booking|inventory)|available\\s+tonight)");

    private static final Pattern ENGLISH_QUESTION_PATTERN = Pattern.compile(
            "(?i)\\b(what\\s*time|when\\s*(can|is|does)|where\\s*is|how\\s*much|is\\s*there|check-?in\\s*time|check-?out\\s*time)\\b"
    );

    private static final Pattern POLICY_LABEL = Pattern.compile("(?iu)(nhận\\s+phòng|check[ -]?in|trả\\s+phòng|check[ -]?out)");
    private static final Pattern POLICY_TIME = Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])([01]?\\d|2[0-3])[:h]([0-5]\\d)(?!\\d)");

    private final ScopedRagRetrievalService retrievalService;
    private final RagCorpusGovernanceValidator governanceValidator;

    public RagGroundedAnswerService(
            ScopedRagRetrievalService retrievalService,
            RagCorpusGovernanceValidator governanceValidator
    ) {
        this.retrievalService = retrievalService;
        this.governanceValidator = governanceValidator;
    }

    public RagQueryResultDto answerQuestion(RagQueryRequestDto request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return RagQueryResultDto.builder()
                    .status(RagAnswerStatus.OUT_OF_SCOPE)
                    .answer("Vui lòng nhập câu hỏi cần hỗ trợ.")
                    .build();
        }

        String question = RagCorpusGovernanceValidator.canonicalSafetyText(request.getQuestion()).trim();
        if (NON_STATIC_REQUEST.matcher(question).find()) {
            return RagQueryResultDto.builder().status(RagAnswerStatus.OUT_OF_SCOPE)
                    .answer("Trợ lý kiến thức chỉ tra cứu chính sách công khai. Vui lòng dùng chức năng nghiệp vụ tương ứng hoặc liên hệ nhân viên.").build();
        }

        // 1. Guardrail checks: Direct Prompt Injection, Code Gen, Customer PII (T12-015, T12-018, T12-019, T12-021)
        if (CODE_GEN_PATTERN.matcher(question).find()) {
            return RagQueryResultDto.builder()
                    .status(RagAnswerStatus.OUT_OF_SCOPE)
                    .answer("Trợ lý chỉ hỗ trợ giải đáp các chính sách, dịch vụ và tiện ích của khách sạn. Chúng tôi không hỗ trợ viết mã nguồn hoặc lập trình.")
                    .build();
        }

        if (DIRECT_INJECTION_PATTERN.matcher(question).find()) {
            return RagQueryResultDto.builder()
                    .status(RagAnswerStatus.OUT_OF_SCOPE)
                    .answer("Yêu cầu không hợp lệ. Trợ lý tuân thủ các quy chuẩn bảo mật và không tiết lộ cấu hình hệ thống hay dữ liệu nhạy cảm.")
                    .build();
        }

        if (GUEST_PII_PATTERN.matcher(question).find()) {
            return RagQueryResultDto.builder()
                    .status(RagAnswerStatus.OUT_OF_SCOPE)
                    .answer("Thông tin cá nhân và lịch trình lưu trú của khách hàng được bảo mật tuyệt đối theo chính sách quyền riêng tư.")
                    .build();
        }

        // 2. Fail-closed scope check for hotel-specific inquiries (T12-005, T12-V2-037)
        if (request.getHotelId() == null && isHotelSpecificInquiry(question)) {
            return RagQueryResultDto.builder()
                    .status(RagAnswerStatus.SCOPE_REQUIRED)
                    .answer("Vui lòng chọn hoặc cung cấp thông tin khách sạn cụ thể để tra cứu chính sách chính xác nhất.")
                    .build();
        }

        // 3. Scoped Retrieval
        List<ScopedRagRetrievalService.ScoredChunk> retrieved = retrievalService.retrieve(request);

        // 4. Insufficient evidence handling (T12-003, T12-017)
        if (retrieved.isEmpty()) {
            return RagQueryResultDto.builder()
                    .status(RagAnswerStatus.INSUFFICIENT_EVIDENCE)
                    .answer("Hiện tại hệ thống tài liệu chính thức của khách sạn chưa có thông tin về vấn đề này. Quý khách vui lòng liên hệ trực tiếp nhân viên lễ tân để được hỗ trợ chi tiết.")
                    .disclaimers(List.of("Tài liệu chính thức hiện tại chưa bao gồm thông tin truy vấn."))
                    .build();
        }

        // 5. Grounded synthesis
        boolean isEnglish = "en".equalsIgnoreCase(request.getLanguage()) || ENGLISH_QUESTION_PATTERN.matcher(question).find();

        List<RagCitationDto> citations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        StringBuilder answerBuilder = new StringBuilder();

        for (ScopedRagRetrievalService.ScoredChunk sc : retrieved) {
            KnowledgeChunk chunk = sc.getChunk();
            if (!ScopedRagRetrievalService.isEligibleEvidence(chunk, request.getHotelId(),
                    request.getAsOfDate() == null ? java.time.LocalDate.now() : request.getAsOfDate())) continue;
            String content = chunk.getSanitizedContent() != null ? chunk.getSanitizedContent() : chunk.getContent();
            if (!governanceValidator.validateForIngestion(RagIngestRequestDto.builder().scopeType(chunk.getScopeType())
                    .hotelId(chunk.getHotelId()).category(chunk.getCategory()).content(content).build()).isValid()) continue;
            var sanitized = governanceValidator.sanitizeChunkContent(content);
            content = sanitized.getSanitizedText();
            if (content.isBlank() || !hasRequestedTimeEvidence(question, content)) continue;
            var citation = sc.toCitation();
            citation.setSnippet(content.length() > 150 ? content.substring(0, 150) + "..." : content);
            citations.add(citation);
            warnings.addAll(sanitized.getWarnings());
            if (chunk.getWarnings() != null) {
                warnings.addAll(chunk.getWarnings());
            }

            if (answerBuilder.length() > 0) {
                answerBuilder.append("\n\n");
            }
            answerBuilder.append(content);
        }

        String rawAnswer = answerBuilder.toString();
        if (citations.isEmpty()) return RagQueryResultDto.builder().status(RagAnswerStatus.INSUFFICIENT_EVIDENCE)
                .answer("Chưa có bằng chứng chính sách hợp lệ. Vui lòng liên hệ lễ tân.").build();
        if (hasConflictingTimes(question, rawAnswer)) return RagQueryResultDto.builder().status(RagAnswerStatus.CONFLICTING_SOURCES)
                .answer("Các tài liệu có thông tin giờ nhận/trả phòng mâu thuẫn. Vui lòng liên hệ lễ tân để xác nhận.")
                .citations(citations).warnings(List.of("CONFLICTING_POLICY_TIMES")).build();
        String finalAnswer;

        if (isEnglish) {
            // Multilingual response (T12-006)
            finalAnswer = synthesizeEnglishAnswer(question, rawAnswer);
        } else {
            finalAnswer = synthesizeVietnameseAnswer(question, rawAnswer);
        }

        return RagQueryResultDto.builder()
                .status(RagAnswerStatus.GROUNDED_ANSWER)
                .answer(finalAnswer)
                .citations(citations)
                .warnings(warnings)
                .disclaimers(List.of(
                        "Thông tin được trích xuất từ tài liệu chính sách tĩnh đã được phê duyệt của khách sạn.",
                        "Giá phòng và tình trạng phòng trống luôn được quản lý trực tiếp qua hệ thống Core Booking."
                ))
                .build();
    }

    private boolean hasRequestedTimeEvidence(String question, String content) {
        String q = question.toLowerCase(Locale.ROOT);
        if (!(q.contains("giờ") || q.contains("time") || q.contains("when"))) return true;
        boolean checkIn = asksCheckIn(q);
        boolean checkOut = asksCheckOut(q);
        if (!checkIn && !checkOut) return true;
        PolicyTimes times = policyTimes(content);
        return (checkIn && !times.checkIn().isEmpty()) || (checkOut && !times.checkOut().isEmpty());
    }

    private record PolicyTimes(Set<String> checkIn, Set<String> checkOut) { }

    private PolicyTimes policyTimes(String content) {
        Set<String> checkIn = new LinkedHashSet<>(), checkOut = new LinkedHashSet<>();
        Matcher label = POLICY_LABEL.matcher(content);
        boolean found = label.find();
        while (found) {
            boolean in = asksCheckIn(label.group().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "));
            int start = label.end();
            found = label.find();
            int end = Math.min(start + 100, found ? label.start() : content.length());
            for (char newline : new char[]{'\r', '\n'}) {
                int nextLine = content.indexOf(newline, start);
                if (nextLine >= 0) end = Math.min(end, nextLine);
            }
            Matcher time = POLICY_TIME.matcher(content.substring(start, end));
            if (time.find()) {
                String normalized = String.format(Locale.ROOT, "%02d:%02d", Integer.parseInt(time.group(1)), Integer.parseInt(time.group(2)));
                (in ? checkIn : checkOut).add(normalized);
            }
        }
        return new PolicyTimes(checkIn, checkOut);
    }

    private boolean asksCheckIn(String q) {
        return q.contains("nhận phòng") || q.contains("check-in") || q.contains("check in") || q.contains("checkin");
    }

    private boolean asksCheckOut(String q) {
        return q.contains("trả phòng") || q.contains("check-out") || q.contains("check out") || q.contains("checkout");
    }

    private boolean hasConflictingTimes(String question, String content) {
        String q = question.toLowerCase(Locale.ROOT);
        PolicyTimes times = policyTimes(content);
        return (asksCheckIn(q) && times.checkIn().size() > 1) || (asksCheckOut(q) && times.checkOut().size() > 1);
    }

    private boolean isHotelSpecificInquiry(String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        return asksCheckIn(lower) || asksCheckOut(lower) ||
                lower.contains("hồ bơi") || lower.contains("bể bơi") ||
                lower.contains("bữa sáng") || lower.contains("ăn sáng");
    }

    private String synthesizeEnglishAnswer(String question, String context) {
        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        List<String> explicitTimes = new ArrayList<>();
        PolicyTimes times = policyTimes(context);

        if (asksCheckIn(normalizedQuestion) && !times.checkIn().isEmpty()) {
            explicitTimes.add("Check-in is from " + times.checkIn().iterator().next());
        }

        if (asksCheckOut(normalizedQuestion) && !times.checkOut().isEmpty()) {
            explicitTimes.add("Check-out is before " + times.checkOut().iterator().next());
        }

        if (!explicitTimes.isEmpty()) {
            return "According to our official policy: " + String.join("; ", explicitTimes) + ".";
        }
        return "According to our official policy:\n" + context;
    }

    private String synthesizeVietnameseAnswer(String question, String context) {
        return context;
    }
}
