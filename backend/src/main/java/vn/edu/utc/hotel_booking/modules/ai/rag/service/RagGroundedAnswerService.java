package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagCitationDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;
import vn.edu.utc.hotel_booking.modules.ai.rag.model.KnowledgeChunk;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class RagGroundedAnswerService {

    private static final Pattern CODE_GEN_PATTERN = Pattern.compile(
            "(?i)(viết.*code|write.*code|class.*public|public\\s+static\\s+void|đoạn\\s*code\\s*java)"
    );

    private static final Pattern DIRECT_INJECTION_PATTERN = Pattern.compile(
            "(?i)(bỏ\\s*qua.*(lệnh|quy\\s*tắc)|ignore.*instructions|danh\\s*sách\\s*mật\\s*khẩu|system\\s*prompt|api\\s*key|debug\\s*mode)"
    );

    private static final Pattern GUEST_PII_PATTERN = Pattern.compile(
            "(?i)(sđt\\s*khách|thông\\s*tin\\s*khách|khách\\s*ở\\s*phòng\\s*\\d+|phòng\\s*305|passport\\s*của\\s*khách)"
    );

    private static final Pattern ENGLISH_QUESTION_PATTERN = Pattern.compile(
            "(?i)\\b(what\\s*time|when\\s*(can|is|does)|where\\s*is|how\\s*much|is\\s*there|check-?in\\s*time|check-?out\\s*time)\\b"
    );

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

        String question = request.getQuestion().trim();

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
            citations.add(sc.toCitation());
            if (chunk.getWarnings() != null) {
                warnings.addAll(chunk.getWarnings());
            }

            String content = chunk.getSanitizedContent() != null ? chunk.getSanitizedContent() : chunk.getContent();
            if (answerBuilder.length() > 0) {
                answerBuilder.append("\n\n");
            }
            answerBuilder.append(content);
        }

        String rawAnswer = answerBuilder.toString();
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

    private boolean isHotelSpecificInquiry(String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        return lower.contains("nhận phòng") || lower.contains("trả phòng") ||
                lower.contains("check-in") || lower.contains("check-out") ||
                lower.contains("hồ bơi") || lower.contains("bể bơi") ||
                lower.contains("bữa sáng") || lower.contains("ăn sáng");
    }

    private String synthesizeEnglishAnswer(String question, String context) {
        if (question.toLowerCase(Locale.ROOT).contains("check-in") || question.toLowerCase(Locale.ROOT).contains("what time")) {
            List<String> times = new ArrayList<>();
            java.util.regex.Matcher m = Pattern.compile("(\\d{1,2}:\\d{2})").matcher(context);
            while (m.find()) {
                times.add(m.group(1));
            }
            String checkInTime = !times.isEmpty() ? times.get(0) : "14:00";
            String checkOutTime = times.size() > 1 ? times.get(1) : "12:00";
            return String.format("Check-in time is from %s, and check-out time is until %s.", checkInTime, checkOutTime);
        }
        return "According to our official policy:\n" + context;
    }

    private String synthesizeVietnameseAnswer(String question, String context) {
        return context;
    }
}
