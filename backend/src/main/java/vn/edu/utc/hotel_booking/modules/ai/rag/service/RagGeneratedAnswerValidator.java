package vn.edu.utc.hotel_booking.modules.ai.rag.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.PiiRedactionUtil;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;

import java.util.*;
import java.util.regex.Pattern;

/** Structural/quote validation is deterministic; semantic entailment still needs TASK-10 evaluation. */
public final class RagGeneratedAnswerValidator {
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(8).maxStringLength(12_000).build())
            .build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Pattern LINK_OR_MARKUP = Pattern.compile("(?iu)(https?://|www\\.|<[^>]+>|\\]\\()");

    public record ValidatedAnswer(String answer, List<UUID> chunkIds) {
        public ValidatedAnswer { chunkIds = List.copyOf(chunkIds); }
    }

    /** Size includes serialized UUIDs, JSON escaping and separators, not just content length. */
    public static String evidenceJson(List<RagAnswerGeneratorPort.Evidence> evidence) {
        if (evidence == null || evidence.isEmpty() || evidence.size() > RagAnswerGeneratorPort.MAX_CHUNKS) {
            throw invalid();
        }
        Set<UUID> ids = new HashSet<>();
        for (var item : evidence) {
            if (item == null || item.chunkId() == null || !ids.add(item.chunkId())
                    || item.content() == null || item.content().isBlank()
                    || item.content().length() > RagAnswerGeneratorPort.MAX_EVIDENCE_CHARACTERS) throw invalid();
        }
        try {
            String serialized = JSON.writeValueAsString(evidence);
            if (serialized.length() > RagAnswerGeneratorPort.MAX_EVIDENCE_CHARACTERS) throw invalid();
            return serialized;
        }
        catch (Exception failure) { throw invalid(); }
    }

    public ValidatedAnswer validate(RagAnswerGeneratorPort.RawGeneration raw,
                                    List<RagAnswerGeneratorPort.Evidence> evidence) {
        evidenceJson(evidence);
        if (raw == null || !"STOP".equals(raw.finishReason()) || raw.json() == null
                || raw.json().isBlank() || raw.json().length() > 16_000) throw invalid();
        try {
            JsonNode root = JSON.readTree(raw.json());
            requireFields(root, Set.of("answer", "references"));
            if (!root.path("answer").isTextual()) throw invalid();
            String answer = RagCorpusGovernanceValidator.canonicalSafetyText(root.path("answer").asText()).trim();
            if (answer.isBlank() || answer.length() > 6000 || LINK_OR_MARKUP.matcher(answer).find()
                    || !PiiRedactionUtil.redact(answer).equals(answer)) throw invalid();
            var governance = new RagCorpusGovernanceValidator();
            if (!governance.validateForIngestion(RagIngestRequestDto.builder().scopeType(RagScopeType.CHAIN_PUBLIC)
                    .category(RagDocumentCategory.FAQ).content(answer).build()).isValid()) throw invalid();
            var sanitized = governance.sanitizeChunkContent(answer);
            if (!sanitized.getWarnings().isEmpty()
                    || !sanitized.getSanitizedText().equals(RagCorpusGovernanceValidator.canonicalSafetyText(answer))) throw invalid();
            JsonNode refs = root.path("references");
            if (!refs.isArray() || refs.isEmpty() || refs.size() > RagAnswerGeneratorPort.MAX_CHUNKS) throw invalid();
            Map<UUID, String> contents = new HashMap<>();
            evidence.forEach(item -> contents.put(item.chunkId(), item.content()));
            List<UUID> ids = new ArrayList<>();
            for (JsonNode ref : refs) {
                requireFields(ref, Set.of("chunkId", "supportingQuote"));
                if (!ref.path("chunkId").isTextual() || !ref.path("supportingQuote").isTextual()) throw invalid();
                String idText = ref.path("chunkId").asText(); UUID id = UUID.fromString(idText);
                if (!id.toString().equalsIgnoreCase(idText) || ids.contains(id)) throw invalid();
                String quote = ref.path("supportingQuote").asText();
                if (!contents.containsKey(id) || quote.isBlank() || quote.length() > 10_000
                        || !contents.get(id).contains(quote)) throw invalid();
                ids.add(id);
            }
            return new ValidatedAnswer(answer, ids);
        }
        catch (Exception failure) { throw invalid(); }
    }

    private static void requireFields(JsonNode node, Set<String> expected) {
        if (node == null || !node.isObject()) throw invalid();
        Set<String> actual = new HashSet<>(); node.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) throw invalid();
    }

    private static IllegalArgumentException invalid() {
        // Never retain raw model text, document text, parser diagnostics or provider causes.
        return new IllegalArgumentException("Invalid RAG generated answer or evidence context");
    }
}
