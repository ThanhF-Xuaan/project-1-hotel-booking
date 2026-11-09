package vn.edu.utc.hotel_booking.modules.ai.rag.config;

import com.google.genai.Client;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.ClientOptions;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.embedding.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagRequestBudget;

import java.util.ArrayList;
import java.util.List;

/** TASK-03 supplies provider models only; it does not replace the local RagEmbeddingPort. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RagProviderProperties.class)
public class RagProviderConfiguration {
    @Bean
    InitializingBean ragProviderValidation(RagProviderProperties properties, Environment environment) {
        return () -> {
            properties.validate();
            if (properties.isEnabled()) apiKey(environment);
        };
    }

    private static String apiKey(Environment environment) {
        String key = environment.getProperty("RAG_GEMINI_API_KEY");
        if (key == null || key.isBlank() || !key.equals(key.trim()) || key.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalStateException("RAG_GEMINI_API_KEY is required when the RAG provider is enabled");
        }
        return key;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "hotel.ai.rag.provider", name = "enabled", havingValue = "true")
    static class GeminiModels {
        @Bean(destroyMethod = "close")
        @Lazy
        Client ragChatClient(RagProviderProperties properties, Environment environment) {
            return client(apiKey(environment), properties.getChatTimeoutMillis(), properties.getChatModel());
        }

        @Bean(destroyMethod = "close")
        @Lazy
        Client ragEmbeddingClient(RagProviderProperties properties, Environment environment) {
            return client(apiKey(environment), properties.getEmbeddingTimeoutMillis(), properties.getEmbeddingModel());
        }

        private static Client client(String key, int timeout, String model) {
            return Client.builder().apiKey(key).vertexAI(false)
                    .clientOptions(ClientOptions.builder().customHttpClient(RagRequestBudget.providerHttpClient(model, timeout)).build())
                    .httpOptions(HttpOptions.builder().baseUrl("https://generativelanguage.googleapis.com")
                            .apiVersion("v1beta").timeout(timeout)
                            .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build()).build();
        }

        @Bean
        @Lazy
        GoogleGenAiChatModel ragChatModel(@Qualifier("ragChatClient") Client client, RagProviderProperties properties) {
            return GoogleGenAiChatModel.builder().genAiClient(client)
                    .options(GoogleGenAiChatOptions.builder().model(properties.getChatModel())
                            .maxOutputTokens(properties.getMaxOutputTokens()).candidateCount(1)
                            .googleSearchRetrieval(false).useCachedContent(false).includeThoughts(false)
                            .toolCallbacks(List.of()).build())
                    .retryTemplate(new RetryTemplate(RetryPolicy.builder().maxRetries(0).build()))
                    .observationRegistry(ObservationRegistry.NOOP).toolCallingManager(new NoTools()).build();
        }

        @Bean
        @Lazy
        GoogleGenAiTextEmbeddingModel ragEmbeddingModel(@Qualifier("ragEmbeddingClient") Client client,
                                                       RagProviderProperties properties) {
            return new TaskAwareEmbeddingModel(client, properties);
        }
    }

    /** No bean resolver or callbacks: even unsolicited function calls cannot execute business operations. */
    static final class NoTools implements ToolCallingManager {
        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions options) {
            if (options.getToolCallbacks() != null && !options.getToolCallbacks().isEmpty()) {
                throw new IllegalArgumentException("RAG provider does not accept tool callbacks");
            }
            return List.of();
        }

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse response) {
            throw new IllegalStateException("RAG provider tool execution is disabled");
        }
    }

    /**
     * Spring AI 2.0.2's embedding model does not forward taskType to the SDK.
     * Keep its Spring model API but construct the fixed embedding request explicitly.
     * This is not a generic SDK executor; model/dimension/task and input sizes are bounded.
     */
    static final class TaskAwareEmbeddingModel extends GoogleGenAiTextEmbeddingModel {
        private final Client client;
        private final String model;
        private final int dimension;
        private final int maxInputs;
        private final int maxCharacters;

        TaskAwareEmbeddingModel(Client client, RagProviderProperties properties) {
            super(GoogleGenAiEmbeddingConnectionDetails.builder().genAiClient(client).build(),
                    GoogleGenAiTextEmbeddingOptions.builder().model(properties.getEmbeddingModel())
                            .dimensions(properties.getEmbeddingDimension())
                            .taskType(GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT)
                            .autoTruncate(false).build(),
                    new RetryTemplate(RetryPolicy.builder().maxRetries(0).build()), ObservationRegistry.NOOP);
            this.client = client;
            this.model = properties.getEmbeddingModel();
            this.dimension = properties.getEmbeddingDimension();
            this.maxInputs = properties.getMaxEmbeddingInputs();
            this.maxCharacters = properties.getMaxEmbeddingInputCharacters();
        }

        @Override
        public int dimensions() { return dimension; } // Never perform a paid dimension-probe request.

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            if (request == null || request.getInstructions() == null || request.getInstructions().isEmpty()
                    || request.getInstructions().size() > maxInputs
                    || request.getInstructions().stream().anyMatch(s -> s == null || s.isBlank() || s.length() > maxCharacters)) {
                throw new IllegalArgumentException("Invalid or oversized RAG embedding input");
            }
            var task = GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT;
            if (request.getOptions() != null) {
                var options = request.getOptions();
                if ((options.getModel() != null && !model.equals(options.getModel()))
                        || (options.getDimensions() != null && dimension != options.getDimensions())) {
                    throw new IllegalArgumentException("RAG embedding identity cannot be overridden");
                }
                if (options instanceof GoogleGenAiTextEmbeddingOptions typed && typed.getTaskType() != null) {
                    task = typed.getTaskType();
                }
            }
            if (task != GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_DOCUMENT
                    && task != GoogleGenAiTextEmbeddingOptions.TaskType.RETRIEVAL_QUERY) {
                throw new IllegalArgumentException("Unsupported RAG embedding task");
            }
            var response = client.models.embedContent(model, request.getInstructions(), EmbedContentConfig.builder()
                    // autoTruncate is Vertex-only; Developer API inputs are bounded above instead.
                    .taskType(task.name()).outputDimensionality(dimension).build());
            var values = response.embeddings().orElse(List.of());
            if (values.size() != request.getInstructions().size()) {
                throw new IllegalStateException("RAG embedding response cardinality mismatch");
            }
            List<Embedding> embeddings = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                var vector = values.get(i).values().orElse(List.of());
                if (vector.size() != dimension) throw new IllegalStateException("RAG embedding dimension mismatch");
                float[] output = new float[dimension];
                double norm = 0;
                for (int j = 0; j < dimension; j++) {
                    Float value = vector.get(j);
                    if (value == null || !Float.isFinite(value)) throw new IllegalStateException("Invalid RAG embedding value");
                    output[j] = value;
                    norm += (double) value * value;
                }
                if (norm == 0) throw new IllegalStateException("RAG embedding vector must not be zero");
                embeddings.add(new Embedding(output, i));
            }
            var metadata = new EmbeddingResponseMetadata();
            metadata.setModel(model);
            return new EmbeddingResponse(embeddings, metadata);
        }
    }
}
