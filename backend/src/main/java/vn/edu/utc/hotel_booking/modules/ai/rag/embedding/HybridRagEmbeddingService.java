package vn.edu.utc.hotel_booking.modules.ai.rag.embedding;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class HybridRagEmbeddingService implements RagEmbeddingPort {

    private final int dimension;
    private final String modelId;

    public HybridRagEmbeddingService() {
        this(256, "local-multilingual-hashing-v1");
    }

    public HybridRagEmbeddingService(int dimension, String modelId) {
        this.dimension = dimension;
        this.modelId = modelId;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public float[] embedText(String text) {
        if (text == null || text.isBlank()) {
            return new float[dimension];
        }

        float[] vector = new float[dimension];
        String normalized = text.toLowerCase(Locale.ROOT).trim();

        // 1. Domain semantic cluster feature boosting
        applyDomainClusters(normalized, vector);

        // 2. Token and N-gram feature hashing
        String[] tokens = normalized.split("[\\s,;:.?!()\\[\\]\"'-]+");
        for (String token : tokens) {
            if (token.isBlank()) continue;
            int hash1 = Math.abs(token.hashCode());
            vector[hash1 % dimension] += 1.5f;

            // Character 3-grams
            if (token.length() >= 3) {
                for (int i = 0; i <= token.length() - 3; i++) {
                    String gram = token.substring(i, i + 3);
                    int hGram = Math.abs(gram.hashCode() * 31);
                    vector[hGram % dimension] += 0.5f;
                }
            }
        }

        // 3. Normalize L2 length to 1.0
        double sumSq = 0.0;
        for (float v : vector) {
            sumSq += v * v;
        }

        if (sumSq > 1e-9) {
            float norm = (float) Math.sqrt(sumSq);
            for (int i = 0; i < dimension; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null) {
            return Collections.emptyList();
        }
        List<float[]> list = new ArrayList<>(texts.size());
        for (String t : texts) {
            list.add(embedText(t));
        }
        return list;
    }

    private void applyDomainClusters(String text, float[] vector) {
        // Semantic cluster 1: Check-in / Nhận phòng / Giờ nhận
        if (text.contains("check-in") || text.contains("check in") || text.contains("nhận phòng") ||
                text.contains("checkin") || text.contains("arrival time") || text.contains("giờ nhận")) {
            boostRange(vector, 0, 16, 2.5f);
        }

        // Semantic cluster 2: Check-out / Trả phòng / Giờ trả
        if (text.contains("check-out") || text.contains("check out") || text.contains("trả phòng") ||
                text.contains("checkout") || text.contains("departure time") || text.contains("giờ trả")) {
            boostRange(vector, 16, 32, 2.5f);
        }

        // Semantic cluster 3: Late arrival / Đến muộn / Nhận muộn
        if (text.contains("đến muộn") || text.contains("late arrival") || text.contains("nhận muộn") ||
                text.contains("arrive late") || text.contains("giữ phòng sau")) {
            boostRange(vector, 32, 48, 2.5f);
        }

        // Semantic cluster 4: Swimming pool / Hồ bơi / Bể bơi / Tiện ích bơi
        if (text.contains("hồ bơi") || text.contains("bể bơi") || text.contains("swimming pool") ||
                text.contains("pool") || text.contains("tầng") || text.contains("đóng cửa")) {
            boostRange(vector, 48, 64, 2.5f);
        }

        // Semantic cluster 5: Pets / Thú cưng / Chó mèo
        if (text.contains("thú cưng") || text.contains("chó mèo") || text.contains("pet") ||
                text.contains("chó") || text.contains("mèo")) {
            boostRange(vector, 64, 80, 2.5f);
        }

        // Semantic cluster 6: EV charging / Sạc xe điện / Trạm sạc
        if (text.contains("sạc xe điện") || text.contains("trạm sạc") || text.contains("ev charging") ||
                text.contains("electric vehicle")) {
            boostRange(vector, 80, 96, 2.5f);
        }

        // Semantic cluster 7: Breakfast / Bữa sáng / Ăn sáng
        if (text.contains("bữa sáng") || text.contains("ăn sáng") || text.contains("breakfast") ||
                text.contains("buffet sáng")) {
            boostRange(vector, 96, 112, 2.5f);
        }

        // Semantic cluster 8: Cancellation / Hủy phòng
        if (text.contains("hủy phòng") || text.contains("chính sách hủy") || text.contains("cancellation") ||
                text.contains("cancel policy")) {
            boostRange(vector, 112, 128, 2.5f);
        }

        // Semantic cluster 9: Facility / Cơ sở / Khách sạn HN-01
        if (text.contains("hn-01") || text.contains("hà nội") || text.contains("hanoi")) {
            boostRange(vector, 128, 144, 2.0f);
        }
    }

    private void boostRange(float[] vector, int start, int end, float weight) {
        int actualStart = start % dimension;
        int actualEnd = Math.min(end, dimension);
        for (int i = actualStart; i < actualEnd; i++) {
            vector[i] += weight;
        }
    }
}
