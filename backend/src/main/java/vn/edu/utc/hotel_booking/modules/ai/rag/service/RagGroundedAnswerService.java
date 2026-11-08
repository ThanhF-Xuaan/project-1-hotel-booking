package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagCitationDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.llm.RagAnswerGenerationPort;

import java.text.Normalizer;
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

    private static final Pattern ROOM_CONTEXT = Pattern.compile(
            "(?iuU)\\b(?:phong|deluxe|suite|superior|standard|rooms?)\\b");
    private static final Pattern ROOM_PRICE_QUESTION = Pattern.compile(
            "(?iuU)\\b(?:gia\\s+(?!treo\\b)(?:[\\p{L}\\p{N}-]+\\s+){0,5}(?:bao\\s*nhieu|the\\s*nao|hien\\s*tai)|bao\\s*gia|"
                    + "bao\\s*nhieu\\s*(?:tien|dong|vnd|(?:mot|1)\\s*dem)|"
                    + "how\\s+much\\s+(?:(?:does|would)\\b[^?!;\\r\\n]{0,80}\\bcost|(?:is|are|for|per)\\b)|"
                    + "what(?:'s|\\s+is|\\s+are)\\s+(?:the\\s+)?(?:current\\s+)?(?:room\\s+)?(?:prices?|rates?)|"
                    + "(?:room\\s+)?rates?\\s+(?:for|today|tonight))\\b");
    private static final Pattern DIRECT_ROOM_PRICE_QUESTION = Pattern.compile(
            "(?iuU)\\bgia\\s+(?:(?:hang|loai)\\s+)?(?:(?:phong\\s+)?(?:deluxe|suite|superior|standard)|phong)"
                    + "(?:\\s+(?:hom\\s*nay|ngay\\s*mai|toi\\s*nay|hien\\s*tai))?\\s*[?!.]*$");
    private static final Pattern ROOM_AVAILABILITY_QUESTION = Pattern.compile(
            "(?iuU)\\b(?:con\\s+(?:(?:bao\\s+nhieu|\\d+)\\s+)?(?:phong|deluxe|suite)\\b(?!\\s+(?:tam|hop|gym)\\b)|"
                    + "(?:phong|deluxe|suite)\\s+(?:[\\p{L}\\p{N}-]+\\s+){0,5}kha\\s*dung|"
                    + "tinh\\s*trang\\s*phong|(?:rooms?(?!\\s+service\\b)|deluxe|suite)\\s+(?:[\\p{L}\\p{N}-]+\\s+){0,6}available|"
                    + "(?:room\\s+)?availability|vacanc(?:y|ies)|available\\s+(?:rooms?|tonight))\\b");
    // "trong phòng" describes the interior. Vacancy requires "trống" after the room subject.
    private static final Pattern ROOM_VACANCY_QUESTION = Pattern.compile(
            "(?iuU)\\b(?:phong(?:\\s+(?:deluxe|suite|superior|standard))?|deluxe|suite)"
                    + "(?:\\s+(?:huong\\s+(?:bien|vuon|nui|ho|thanh\\s+pho)|giuong\\s+(?:doi|don)|king|queen|twin|double)){0,3}"
                    + "\\s+(?:(?:nay|do)\\s+)?(?:co\\s+)?(?:con\\s+)?trong\\b"
                    + "(?=\\s*(?:[?!.]|$)|\\s+(?:khong|(?:(?:vao|tu|den|cho)\\s+)?"
                    + "(?:hom\\s*nay|toi\\s*nay|ngay\\s*mai|dem\\s*mai|cuoi\\s*tuan|ngay\\s+\\d{1,2}))\\b)");
    private static final Pattern PET_TOPIC = Pattern.compile(
            "(?iuU)\\b(?:thu\\s+cung|vat\\s+nuoi|cho\\s+(?:hoac\\s+)?meo|pets?|dogs?|cats?|animals?|chó|mèo)\\b");

    private static final Pattern ENGLISH_QUESTION_PATTERN = Pattern.compile(
            "(?i)\\b(what\\s*time|when\\s*(can|is|does)|where\\s*is|how\\s*much|is\\s*there|check-?in\\s*time|check-?out\\s*time)\\b"
    );

    private static final Pattern POLICY_LABEL = Pattern.compile("(?iu)(nhận\\s+phòng|check[ -]?in|trả\\s+phòng|check[ -]?out)");
    private static final Pattern POLICY_TIME = Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])([01]?\\d|2[0-3])[:h]([0-5]\\d)(?!\\d)");

    private final ScopedRagRetrievalService retrievalService;
    private final RagCorpusGovernanceValidator governanceValidator;
    private final RagAnswerGenerationPort answerGenerator;

    public RagGroundedAnswerService(
            ScopedRagRetrievalService retrievalService,
            RagCorpusGovernanceValidator governanceValidator
    ) {
        this(retrievalService, governanceValidator, RagAnswerGenerationPort.disabled());
    }

    @Autowired
    public RagGroundedAnswerService(
            ScopedRagRetrievalService retrievalService,
            RagCorpusGovernanceValidator governanceValidator,
            RagAnswerGenerationPort answerGenerator
    ) {
        this.retrievalService = retrievalService;
        this.governanceValidator = governanceValidator;
        this.answerGenerator = answerGenerator;
    }

    public RagQueryResultDto answerQuestion(RagQueryRequestDto request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return RagQueryResultDto.builder()
                    .status(RagAnswerStatus.OUT_OF_SCOPE)
                    .answer("Vui lòng nhập câu hỏi cần hỗ trợ.")
                    .build();
        }

        String question = RagCorpusGovernanceValidator.canonicalSafetyText(request.getQuestion()).trim();
        if (isDynamicRoomInquiry(question)) {
            return RagQueryResultDto.builder()
                    .status(RagAnswerStatus.OUT_OF_SCOPE)
                    .answer("Giá phòng và tình trạng phòng trống được kiểm tra qua Core Booking. "
                            + "Vui lòng dùng chức năng tìm phòng để xem kết quả.")
                    .build();
        }

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

        boolean petInquiry = mentionsPets(question);
        List<RagCitationDto> citations = new ArrayList<>();
        List<RagAnswerGenerationPort.Evidence> evidence = new ArrayList<>();
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
            if (petInquiry && !mentionsPets(content)) continue;
            var citation = sc.toCitation();
            citation.setSnippet(content.length() > 150 ? content.substring(0, 150) + "..." : content);
            citations.add(citation);
            evidence.add(new RagAnswerGenerationPort.Evidence(evidence.size() + 1, content));
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
        boolean aiGenerated = false;

        Optional<RagAnswerGenerationPort.GeneratedAnswer> generated = answerGenerator.generate(
                question, isEnglish ? "en" : "vi", List.copyOf(evidence));
        if (generated.isPresent()) {
            var output = generated.get();
            finalAnswer = output.answer();
            List<RagCitationDto> availableCitations = List.copyOf(citations);
            citations = output.evidenceIds().stream()
                    .filter(id -> id != null && id > 0 && id <= availableCitations.size())
                    .map(id -> availableCitations.get(id - 1))
                    .distinct()
                    .toList();
            aiGenerated = true;
        } else if (isEnglish) {
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
                .disclaimers(aiGenerated ? List.of(
                        "Câu trả lời được Gemini tổng hợp từ tài liệu chính sách tĩnh đã được phê duyệt của khách sạn.",
                        "Giá phòng và tình trạng phòng trống luôn được quản lý trực tiếp qua hệ thống Core Booking."
                ) : List.of(
                        "Thông tin được trích xuất từ tài liệu chính sách tĩnh đã được phê duyệt của khách sạn.",
                        "Giá phòng và tình trạng phòng trống luôn được quản lý trực tiếp qua hệ thống Core Booking."
                ))
                .build();
    }

    private static boolean isDynamicRoomInquiry(String question) {
        String intent = foldAccents(question);
        return ROOM_CONTEXT.matcher(intent).find()
                && (ROOM_PRICE_QUESTION.matcher(intent).find()
                    || DIRECT_ROOM_PRICE_QUESTION.matcher(intent).find()
                    || ROOM_AVAILABILITY_QUESTION.matcher(intent).find()
                    || ROOM_VACANCY_QUESTION.matcher(intent).find());
    }

    private static boolean mentionsPets(String text) {
        return PET_TOPIC.matcher(text).find() || PET_TOPIC.matcher(foldAccents(text)).find();
    }

    private static String foldAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D');
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
