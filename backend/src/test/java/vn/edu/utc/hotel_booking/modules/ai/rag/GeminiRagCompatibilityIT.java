package vn.edu.utc.hotel_booking.modules.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.boot.SpringBootVersion;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import vn.edu.utc.hotel_booking.modules.ai.rag.config.RagProviderConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in only; never part of default Surefire discovery (*IT).
 * Requires separate owner/provider/privacy/cost permission and operator-confirmed budget.
 * T12_RAG_GEMINI_LIVE_POC=true, T12_RAG_GEMINI_POC_APPROVED=true,
 * T12_RAG_GEMINI_POC_BUDGET_CONFIRMED=true, RAG_GEMINI_API_KEY,
 * RAG_GEMINI_CHAT_MODEL, RAG_GEMINI_EMBEDDING_MODEL, RAG_GEMINI_EMBEDDING_DIMENSION.
 * Run explicitly: mvn.cmd -Dtest=GeminiRagCompatibilityIT -DfailIfNoTests=true test
 * Two single-input embedding requests and one structured chat; no retries, DB or corpus ingest.
 */
@EnabledIfEnvironmentVariable(named = "T12_RAG_GEMINI_LIVE_POC", matches = "true")
class GeminiRagCompatibilityIT {
    @Test
    @Timeout(30)
    void verifiesActualEmbeddingShapeAndStructuredChatOnSyntheticEvidence() throws Exception {
        assertEquals("true", System.getenv("T12_RAG_GEMINI_POC_APPROVED"), "Separate live PoC permission is required");
        assertEquals("true", System.getenv("T12_RAG_GEMINI_POC_BUDGET_CONFIRMED"), "Operator must confirm quota and PoC cost bound");
        vn.edu.utc.hotel_booking.modules.ai.rag.service.RagOutboundLedger.requireConfiguredPolicy();
        String chatModel = requiredModel("RAG_GEMINI_CHAT_MODEL");
        String embeddingModel = requiredModel("RAG_GEMINI_EMBEDDING_MODEL");
        String dimensionText = System.getenv("RAG_GEMINI_EMBEDDING_DIMENSION");
        assertTrue(dimensionText != null && dimensionText.matches("[0-9]{1,4}"), "Explicit embedding dimension is required");
        int dimension = Integer.parseInt(dimensionText);
        String key = System.getenv("RAG_GEMINI_API_KEY");
        assertTrue(key != null && !key.isBlank(), "Dedicated RAG key is required");
        // A failed rerun must never leave an earlier PASS receipt looking current.
        Files.deleteIfExists(Path.of("target/rag-gemini-poc.json"));

        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("approved-synthetic-poc", Map.of(
                    "hotel.ai.rag.provider.enabled", "true", "hotel.ai.rag.provider.chat-model", chatModel,
                    "hotel.ai.rag.provider.embedding-model", embeddingModel,
                    "hotel.ai.rag.provider.embedding-dimension", dimensionText)));
            context.register(RagProviderConfiguration.class);
            context.refresh();
            var embedding = context.getBean(EmbeddingModel.class);
            for (var task : List.of(GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_QUERY,
                    GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT)) {
                String text = task == GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_QUERY
                        ? "Khách sạn Giả lập nhận phòng lúc mấy giờ?"
                        : "Khách sạn Giả lập nhận phòng từ 15:00.";
                var response = embedding.call(new EmbeddingRequest(List.of(text),
                        GoogleGenAiTextEmbeddingOptions.builder().taskType(task).build()));
                assertEquals(1, response.getResults().size(), "Embedding cardinality mismatch");
                float[] vector = response.getResult().getOutput();
                assertEquals(dimension, vector.length, "Embedding dimension mismatch");
                double norm = 0;
                for (float v : vector) { assertTrue(Float.isFinite(v), "Non-finite embedding"); norm += (double) v * v; }
                assertTrue(norm > 0, "Embedding vector must not be zero");
            }

            String schema = """
                    {"type":"object","properties":{"answer":{"type":"string"},"sourceId":{"type":"string"}},
                     "required":["answer","sourceId"],"additionalProperties":false}
                    """;
            var chat = context.getBean(ChatModel.class).call(new Prompt(List.of(
                    new SystemMessage("Chỉ trả lời từ bằng chứng synthetic được cung cấp, không dùng công cụ."),
                    new UserMessage("Nguồn SYNTHETIC-POC: Khách sạn Giả lập nhận phòng từ 15:00. Hỏi: mấy giờ nhận phòng?")),
                    GoogleGenAiChatOptions.builder().model(chatModel).maxOutputTokens(768)
                            .responseMimeType("application/json").responseSchema(schema).googleSearchRetrieval(false).build()));
            assertNotNull(chat.getResult(), "Missing chat result");
            assertEquals("STOP", chat.getResult().getMetadata().getFinishReason(), "Chat response must not be truncated");
            var result = new ObjectMapper().readTree(chat.getResult().getOutput().getText());
            assertEquals(2, result.size(), "Unexpected structured fields");
            assertTrue(result.path("sourceId").isTextual() && "SYNTHETIC-POC".equals(result.path("sourceId").asText()),
                    "Structured source reference mismatch");
            assertTrue(result.path("answer").isTextual() && result.path("answer").asText().contains("15:00"),
                    "Structured answer must preserve the synthetic policy time");
            String observedChatModel = chat.getMetadata().getModel();
            assertTrue(observedChatModel != null && observedChatModel.matches("[a-zA-Z0-9._-]{1,128}"),
                    "Provider model metadata must be recorded safely");

            Map<String, Object> receipt = new LinkedHashMap<>();
            receipt.put("status", "PASS_SYNTHETIC_COMPATIBILITY_ONLY");
            receipt.put("observedAt", java.time.Instant.now().toString());
            receipt.put("springBootVersion", SpringBootVersion.getVersion());
            receipt.put("springAiVersion", GoogleGenAiChatModel.class.getPackage().getImplementationVersion());
            receipt.put("googleGenAiVersion", Client.class.getPackage().getImplementationVersion());
            receipt.put("requestedChatModel", chatModel);
            receipt.put("observedChatModel", observedChatModel);
            receipt.put("requestedEmbeddingModel", embeddingModel);
            receipt.put("observedVectorDimension", dimension);
            receipt.put("embeddingRequests", 2);
            receipt.put("structuredChatRequests", 1);
            receipt.put("limitation", "No corpus/retrieval/security/production-quality approval; embedding model ID is requested, vector dimension is observed.");
            Files.createDirectories(Path.of("target"));
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Path.of("target/rag-gemini-poc.json").toFile(), receipt);
        }
        catch (Exception failure) {
            // Do not attach SDK exception messages/body/headers or credentials to Surefire logs.
            throw new AssertionError("Synthetic Gemini compatibility PoC failed: " + failure.getClass().getSimpleName());
        }
    }

    private static String requiredModel(String variable) {
        String value = System.getenv(variable);
        assertTrue(value != null && value.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}"), "Explicit model ID is required: " + variable);
        return value;
    }
}
