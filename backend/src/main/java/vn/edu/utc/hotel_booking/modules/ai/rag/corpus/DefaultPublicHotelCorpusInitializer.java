package vn.edu.utc.hotel_booking.modules.ai.rag.corpus;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagActivationRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagIngestRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagDocumentCategory;
import vn.edu.utc.hotel_booking.modules.ai.rag.enums.RagScopeType;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagKnowledgeLifecycleService;
import vn.edu.utc.hotel_booking.modules.ai.rag.store.RagVectorStorePort;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class DefaultPublicHotelCorpusInitializer {

    private final RagKnowledgeLifecycleService lifecycleService;
    private final RagVectorStorePort vectorStore;
    private final boolean demoCorpusEnabled;
    private final AtomicBoolean seeded = new AtomicBoolean(false);

    public DefaultPublicHotelCorpusInitializer(
            RagKnowledgeLifecycleService lifecycleService,
            RagVectorStorePort vectorStore,
            @Value("${hotel.ai.rag.demo-corpus.enabled:false}") boolean demoCorpusEnabled
    ) {
        this.lifecycleService = lifecycleService;
        this.vectorStore = vectorStore;
        this.demoCorpusEnabled = demoCorpusEnabled;
    }

    @PostConstruct
    public void init() {
        seedDefaultCorpus();
    }

    public synchronized void seedDefaultCorpus() {
        if (!demoCorpusEnabled) {
            log.info("Synthetic RAG demo corpus is disabled.");
            return;
        }
        if (seeded.get()) {
            return;
        }

        log.info("Seeding synthetic public hotel corpus for local RAG demonstrations...");

        // 1. POLICY-CHECKIN (HN-01, hotelId = 1)
        ingestAndActivate(
                "POLICY-CHECKIN",
                RagScopeType.HOTEL_SCOPED,
                1,
                RagDocumentCategory.POLICY,
                "Chính sách nhận trả phòng cơ sở HN-01",
                "Quy định nhận phòng từ 14:00 và trả phòng trước 12:00 trưa hàng ngày tại cơ sở HN-01.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 2. POLICY-LATE-ARRIVAL (HN-01, hotelId = 1)
        ingestAndActivate(
                "POLICY-LATE-ARRIVAL",
                RagScopeType.HOTEL_SCOPED,
                1,
                RagDocumentCategory.POLICY,
                "Chính sách đến muộn cơ sở HN-01",
                "Khách đến muộn sau 18:00 vui lòng liên hệ trước với lễ tân để giữ phòng đến 22:00.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 3. AMENITY-POOL (HN-01, hotelId = 1)
        ingestAndActivate(
                "AMENITY-POOL",
                RagScopeType.HOTEL_SCOPED,
                1,
                RagDocumentCategory.AMENITY,
                "Tiện ích hồ bơi vô cực",
                "Hồ bơi vô cực nằm ở tầng 5. Giờ mở cửa từ 06:00 và đóng cửa lúc 21:00 hàng ngày. Trẻ em dưới 12 tuổi phải có người lớn đi kèm.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 4. POLICY-BREAKFAST (HN-01, hotelId = 1)
        ingestAndActivate(
                "POLICY-BREAKFAST",
                RagScopeType.HOTEL_SCOPED,
                1,
                RagDocumentCategory.POLICY,
                "Bữa sáng cơ sở HN-01",
                "Bữa sáng tại khách sạn HN-01 phục vụ từ 06:30 đến 09:30 tại nhà hàng Sen.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 5. POLICY-BREAKFAST (DN-01, hotelId = 2)
        ingestAndActivate(
                "POLICY-BREAKFAST",
                RagScopeType.HOTEL_SCOPED,
                2,
                RagDocumentCategory.POLICY,
                "Bữa sáng cơ sở DN-01",
                "Bữa sáng tại khách sạn DN-01 phục vụ từ 06:00 đến 10:00 tại nhà hàng Biển Xanh.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 6. FAQ-CANCELLATION (CHAIN_PUBLIC)
        ingestAndActivate(
                "FAQ-CANCELLATION",
                RagScopeType.CHAIN_PUBLIC,
                null,
                RagDocumentCategory.FAQ,
                "Quy định hủy đặt phòng chung toàn hệ thống",
                "Khách hàng được hủy phòng miễn phí trước 48 giờ so với giờ nhận phòng. Hủy sau thời gian này sẽ tính phí đêm đầu tiên.",
                LocalDate.of(2026, 1, 1),
                1
        );

        seeded.set(true);
        log.info("Default public hotel corpus seeded successfully.");
    }

    public synchronized void resetOrClear() {
        vectorStore.clearAll();
        seeded.set(false);
    }

    private void ingestAndActivate(
            String sourceId,
            RagScopeType scopeType,
            Integer hotelId,
            RagDocumentCategory category,
            String title,
            String content,
            LocalDate effectiveFrom,
            int version
    ) {
        lifecycleService.ingestAndStage(
                RagIngestRequestDto.builder()
                        .sourceId(sourceId)
                        .scopeType(scopeType)
                        .hotelId(hotelId)
                        .category(category)
                        .title(title)
                        .content(content)
                        .effectiveFrom(effectiveFrom)
                        .build(),
                version
        );

        lifecycleService.activate(
                RagActivationRequestDto.builder()
                        .sourceId(sourceId)
                        .scopeType(scopeType)
                        .hotelId(hotelId)
                        .version(version)
                        .build()
        );
    }
}
