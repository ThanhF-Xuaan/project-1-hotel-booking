import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import vn.edu.utc.hotel_booking.modules.ai.rag.embedding.HybridRagEmbeddingService;
import vn.edu.utc.hotel_booking.modules.ai.rag.governance.RagCorpusGovernanceValidator;

/** Rebuild the approved public corpus vectors with the same implementation as the backend. */
class RealDataPilotEmbedding {
    public static void main(String[] args) throws Exception {
        var model = new HybridRagEmbeddingService();
        var governance = new RagCorpusGovernanceValidator();
        if (model.getDimension() != 256 || !model.getModelId().equals("local-multilingual-hashing-v1")) {
            throw new IllegalStateException("Unexpected pilot embedding model");
        }
        try (var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            for (String line; (line = input.readLine()) != null;) {
                String[] fields = line.split("\t", -1);
                if (fields.length != 3) throw new IllegalStateException("Invalid embedding input");
                String title = decode(fields[0]);
                String raw = decode(fields[1]);
                String sanitized = governance.sanitizeChunkContent(raw).getSanitizedText();
                if (!sanitized.equals(decode(fields[2]))) throw new IllegalStateException("Stored sanitization differs");
                float[] vector = model.embedText(title + "\n" + sanitized);
                double norm = 0;
                for (float value : vector) {
                    if (!Float.isFinite(value)) throw new IllegalStateException("Invalid embedding");
                    norm += (double) value * value;
                }
                if (norm <= 0) throw new IllegalStateException("Empty embedding");
                System.out.println(Arrays.toString(vector));
            }
        }
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
