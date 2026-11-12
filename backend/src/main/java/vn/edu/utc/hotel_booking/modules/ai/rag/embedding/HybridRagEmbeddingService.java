package vn.edu.utc.hotel_booking.modules.ai.rag.embedding;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
@ConditionalOnProperty(prefix = "hotel.ai.rag.provider", name = "enabled", havingValue = "false", matchIfMissing = true)
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
        // Semantic cluster 1: Check-in / Nhận phòng / Giờ nhận / Nhận phòng sớm
        if (text.contains("check-in") || text.contains("check in") || text.contains("nhận phòng") ||
                text.contains("nhan phong") || text.contains("nhận phòng sớm") || text.contains("nhan phong som") ||
                text.contains("checkin") || text.contains("arrival time") || text.contains("giờ nhận") ||
                text.contains("early check-in") || text.contains("early checkin")) {
            boostRange(vector, 0, 16, 2.5f);
        }

        // Semantic cluster 2: Check-out / Trả phòng / Giờ trả / Trả phòng muộn
        if (text.contains("check-out") || text.contains("check out") || text.contains("trả phòng") ||
                text.contains("tra phong") || text.contains("trả phòng muộn") || text.contains("tra phong muon") ||
                text.contains("checkout") || text.contains("departure time") || text.contains("giờ trả") ||
                text.contains("late check-out") || text.contains("late checkout")) {
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

        // Semantic cluster 5: Pets / Thú cưng / Chó mèo / Poodle / Cún
        if (text.contains("thú cưng") || text.contains("thu cung") ||
                text.contains("chó mèo") || text.contains("cho meo") ||
                text.contains("pet") || text.contains("chó") || text.contains("mèo") ||
                text.contains("cún") || text.contains("cun") ||
                text.contains("poodle") || text.contains("vật nuôi") || text.contains("vat nuoi") ||
                text.contains("động vật cảnh") || text.contains("dong vat")) {
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

        // Semantic cluster 8: Cancellation / Hủy phòng / Hoàn cọc / Hoàn tiền
        if (text.contains("hủy phòng") || text.contains("huy phong") ||
                text.contains("chính sách hủy") || text.contains("chinh sach huy") ||
                text.contains("cancellation") || text.contains("cancel policy") ||
                text.contains("hoàn tiền") || text.contains("hoan tien") ||
                text.contains("hoàn cọc") || text.contains("hoan coc") ||
                text.contains("tiền cọc") || text.contains("tien coc") ||
                text.contains("đặt cọc") || text.contains("dat coc") ||
                text.contains("refund") || text.contains("deposit")) {
            boostRange(vector, 112, 128, 2.5f);
        }

        // Semantic cluster 9: Facility / Cơ sở / Khách sạn HN-01
        if (text.contains("hn-01") || text.contains("hà nội") || text.contains("hanoi")) {
            boostRange(vector, 128, 144, 2.0f);
        }

        // Semantic cluster 10: Static room description / Diện tích & bố cục hạng phòng
        if (text.contains("diện tích") || text.contains("dien tich") ||
                text.contains("m2") || text.contains("m²") ||
                text.contains("mét vuông") || text.contains("met vuong") ||
                text.contains("bố cục") || text.contains("bo cuc") ||
                text.contains("hạng phòng") || text.contains("hang phong") ||
                text.contains("phòng khách") || text.contains("phong khach") ||
                text.contains("bồn tắm") || text.contains("bon tam") ||
                text.contains("room size") || text.contains("layout") || text.contains("square meter")) {
            boostRange(vector, 144, 160, 3.5f);
        }

        // Semantic cluster 11: Children & Infant surcharge / Phụ thu trẻ em & em bé sơ sinh
        if (text.contains("trẻ sơ sinh") || text.contains("tre so sinh") ||
                text.contains("sơ sinh") || text.contains("so sinh") ||
                text.contains("em bé") || text.contains("em be") ||
                text.contains("phụ thu trẻ") || text.contains("phu thu tre") ||
                text.contains("phụ thu bé") || text.contains("phu thu be") ||
                text.contains("phụ thu") || text.contains("phu thu") ||
                text.contains("tính thêm tiền") || text.contains("tinh them tien") ||
                text.contains("tính tiền thêm") || text.contains("tinh tiền") ||
                text.contains("ngủ chung giường") || text.contains("ngu chung giuong") ||
                text.contains("giường phụ") || text.contains("giuong phu") ||
                text.contains("cũi") || text.contains("nôi") ||
                text.contains("infant") || text.contains("baby") ||
                (text.contains("trẻ em") && (text.contains("tiền") || text.contains("phí") || text.contains("giá")))) {
            boostRange(vector, 160, 176, 3.5f);
        }

        // Semantic cluster 12: Early check-in & Late check-out / Nhận phòng sớm & Trả phòng muộn
        if (text.contains("nhận phòng sớm") || text.contains("nhan phong som") ||
                text.contains("trả phòng muộn") || text.contains("tra phong muon") ||
                text.contains("đến sớm") || text.contains("den som") ||
                text.contains("rời phòng muộn") || text.contains("roi phong muon") ||
                text.contains("early check-in") || text.contains("early checkin") ||
                text.contains("late check-out") || text.contains("late checkout")) {
            boostRange(vector, 176, 192, 3.5f);
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
