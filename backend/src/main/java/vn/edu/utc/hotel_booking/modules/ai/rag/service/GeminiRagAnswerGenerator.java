package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.PiiRedactionUtil;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagProviderProperties;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Rollback generation independently via hotel.ai.rag.generation-enabled=false. No tools or retry loop. */
@Service
@ConditionalOnProperty(prefix = "hotel.ai.rag.provider", name = "enabled", havingValue = "true")
public class GeminiRagAnswerGenerator implements RagAnswerGeneratorPort {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SCHEMA = """
            {"type":"object","properties":{"answer":{"type":"string","minLength":1,"maxLength":6000},
             "references":{"type":"array","minItems":1,"maxItems":5,"items":{"type":"object",
              "properties":{"chunkId":{"type":"string","format":"uuid"},"supportingQuote":{"type":"string","minLength":1}},
              "required":["chunkId","supportingQuote"],"additionalProperties":false}}},
             "required":["answer","references"],"additionalProperties":false}
            """;

    private final ChatModel chatModel;
    private final String modelId;
    private final int maxOutputTokens;
    private final boolean enabled;

    public GeminiRagAnswerGenerator(@Lazy @Qualifier("ragChatModel") ChatModel chatModel,
                                   RagProviderProperties properties, Environment environment) {
        properties.validate();
        this.chatModel = chatModel;
        this.modelId = properties.getChatModel();
        this.maxOutputTokens = properties.getMaxOutputTokens();
        this.enabled = properties.isEnabled() && environment.getProperty("hotel.ai.rag.generation-enabled", Boolean.class, true);
    }

    @Override public boolean isEnabled() { return enabled; }

    @Override
    public RawGeneration generate(Request request) {
        try {
            if (!enabled || TransactionSynchronizationManager.isActualTransactionActive() || request == null
                    || request.question() == null || request.question().isBlank() || request.question().length() > 2000
                    || !List.of("vi", "en").contains(request.language())) throw new GenerationUnavailableException();
            String question = PiiRedactionUtil.redact(RagCorpusGovernanceValidator.canonicalSafetyText(request.question()));
            if (question.isBlank() || question.length() > 2000) throw new GenerationUnavailableException();
            RagGeneratedAnswerValidator.evidenceJson(request.evidence());
            String payload = JSON.writeValueAsString(Map.of("question", question, "language", request.language(),
                    "evidence", request.evidence()));
            // Static system resource + a separate JSON data message: no user/document template interpolation.
            String instructions = new ClassPathResource("ai/rag/rag-answer-v1.st").getContentAsString(StandardCharsets.UTF_8);
            var options = GoogleGenAiChatOptions.builder().model(modelId).maxOutputTokens(maxOutputTokens).candidateCount(1)
                    .responseMimeType("application/json").responseSchema(SCHEMA).googleSearchRetrieval(false)
                    .useCachedContent(false).includeThoughts(false).toolCallbacks(List.of()).build();
            var response = chatModel.call(new Prompt(List.of(new SystemMessage(instructions), new UserMessage(payload)), options));
            if (response == null || response.getResults().size() != 1 || response.getResult() == null
                    || response.getResult().getOutput() == null || !response.getResult().getOutput().getToolCalls().isEmpty()) {
                throw new GenerationUnavailableException();
            }
            return new RawGeneration(response.getResult().getOutput().getText(), response.getResult().getMetadata().getFinishReason());
        }
        catch (Exception failure) { throw new GenerationUnavailableException(); }
    }
}
