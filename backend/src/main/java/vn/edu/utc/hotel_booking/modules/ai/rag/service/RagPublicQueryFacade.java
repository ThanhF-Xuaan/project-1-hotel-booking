package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagQueryResultDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagAnswerStatus;
import vn.edu.utc.hotel_booking.modules.organization.service.PublicHotelEligibilityReader;

/**
 * Public RAG query facade wrapping in-process calls with bounds checks,
 * hotel eligibility verification, rate budgets, and PII redaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPublicQueryFacade {

    private final RagGroundedAnswerService ragGroundedAnswerService;
    private final PublicHotelEligibilityReader hotelEligibility;
    private final RagRequestBudget requestBudget;

    public RagPublicQueryOutcome ask(String question, Integer hotelId) {
        if (question == null || question.isBlank() || question.length() > 500) {
            return RagPublicQueryOutcome.scopeRequired(RagQueryResultDto.builder()
                    .status(RagAnswerStatus.SCOPE_REQUIRED)
                    .answer("Câu hỏi không hợp lệ hoặc vượt quá 500 ký tự.")
                    .build());
        }

        if (hotelId != null && (hotelId < 1 || hotelId > 32767)) {
            return RagPublicQueryOutcome.scopeRequired(RagQueryResultDto.builder()
                    .status(RagAnswerStatus.SCOPE_REQUIRED)
                    .answer("Mã khách sạn không hợp lệ.")
                    .build());
        }

        try {
            return requestBudget.execute(() -> {
                if (hotelId != null && !hotelEligibility.isActivePublicHotel(hotelId.shortValue())) {
                    return RagPublicQueryOutcome.scopeRequired(RagQueryResultDto.builder()
                            .status(RagAnswerStatus.SCOPE_REQUIRED)
                            .answer("Vui lòng chọn khách sạn đang hoạt động trong danh sách công khai.")
                            .build());
                }

                String redacted = RagRequestBudget.redactQuestion(question);
                RagQueryRequestDto request = RagQueryRequestDto.builder()
                        .hotelId(hotelId)
                        .question(redacted)
                        .language("vi")
                        .topK(5)
                        .build();

                RagQueryResultDto result = ragGroundedAnswerService.answerQuestion(request);
                if (result != null && result.getStatus() == RagAnswerStatus.SCOPE_REQUIRED) {
                    return RagPublicQueryOutcome.scopeRequired(result);
                }
                return RagPublicQueryOutcome.answer(result);
            });
        } catch (RagRequestBudget.LimitExceededException limited) {
            log.warn("T12-V2-031: RAG query rate limited; wait {} seconds", limited.retryAfterSeconds());
            return RagPublicQueryOutcome.rateLimited(limited.retryAfterSeconds());
        } catch (Exception e) {
            log.warn("T12-V2-032: RAG query failed or temporarily unavailable (type={})", e.getClass().getSimpleName());
            return RagPublicQueryOutcome.unavailable();
        }
    }
}
