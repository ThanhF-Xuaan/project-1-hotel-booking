package vn.edu.utc.hotel_booking.modules.ai.rag.corpus;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;
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
            @Lazy RagVectorStorePort vectorStore,
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
                "Quy định hủy đặt phòng và hoàn tiền cọc chung toàn hệ thống",
                "Khách hàng được hủy phòng miễn phí và hoàn trả 100% tiền đặt cọc nếu thực hiện hủy trước 48 giờ so với giờ nhận phòng tiêu chuẩn. Tiền cọc hoàn trả sẽ được chuyển lại vào tài khoản ngân hàng hoặc phương thức thanh toán ban đầu của quý khách trong vòng 3-7 ngày làm việc. Trường hợp hủy phòng trong vòng 48 giờ trước ngày nhận phòng hoặc khách không đến nhận phòng (no-show), khách sạn sẽ tính phí đêm đầu tiên và không hoàn lại tiền đặt cọc tương ứng để đảm bảo chi phí giữ phòng.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 7. POLICY-CHILD-BREAKFAST (DN-01, hotelId = 2)
        ingestAndActivate(
                "POLICY-CHILD-BREAKFAST",
                RagScopeType.HOTEL_SCOPED,
                2,
                RagDocumentCategory.POLICY,
                "Chính sách buffet sáng cho trẻ em",
                "Trẻ em dưới 6 tuổi đi cùng bố mẹ được miễn phí buffet sáng tiêu chuẩn tại nhà hàng khách sạn. Trẻ em từ 6 đến 11 tuổi áp dụng mức phụ thu theo quy định được thông báo tại quầy lễ tân khi nhận phòng. Mỗi phòng được áp dụng miễn phí tối đa cho 1 trẻ em dưới 6 tuổi.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 8. ROOM-DESC-HN01 (HN-01, hotelId = 1)
        ingestAndActivate(
                "ROOM-DESC-HN01",
                RagScopeType.HOTEL_SCOPED,
                1,
                RagDocumentCategory.STATIC_ROOM_DESCRIPTION,
                "Mô tả diện tích và bố cục các hạng phòng cơ sở HN-01",
                "[STD] Standard: Diện tích 26m², bố cục gọn gàng yên tĩnh phù hợp khách công tác ngắn ngày.\n" +
                        "[SUP] Superior: Diện tích 32m², cửa sổ lớn thoáng đãng với bàn làm việc.\n" +
                        "[DLX] Deluxe: Diện tích 38m², không gian rộng rãi có sofa thư giãn và khu vực bàn trà.\n" +
                        "[STE] Suite: Diện tích 48m², bố cục phòng ngủ và khu tiếp khách tiện nghi, có ban công thoáng mát và bồn tắm nằm cao cấp.\n" +
                        "[FAM] Family Room: Diện tích 50m², bố cục 2 giường đôi lớn tối ưu cho gia đình có trẻ nhỏ.\n" +
                        "[EXE] Executive Suite: Diện tích 58m², có phòng khách tách biệt, bồn tắm nằm và không gian sinh hoạt rộng rãi cho gia đình.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 9. ROOM-DESC-DN01 (DN-01, hotelId = 2)
        ingestAndActivate(
                "ROOM-DESC-DN01",
                RagScopeType.HOTEL_SCOPED,
                2,
                RagDocumentCategory.STATIC_ROOM_DESCRIPTION,
                "Mô tả diện tích và bố cục các hạng phòng cơ sở DN-01",
                "[STD] Standard: Diện tích 28m², bố cục hiện đại gọn gàng.\n" +
                        "[SUP] Superior: Diện tích 32m², có ban công thoáng mát.\n" +
                        "[DLX] Deluxe: Diện tích 40m², ban công hướng biển, bố cục cân đối cho cặp đôi hoặc gia đình nhỏ.\n" +
                        "[STE] Suite: Diện tích 50m², ban công hướng biển trực diện, có bồn tắm nằm và khu vực thư giãn sang trọng.\n" +
                        "[FAM] Family Room: Diện tích 54m², không gian sàn rộng thoáng phù hợp cho gia đình có bé nhỏ.\n" +
                        "[EXE] Executive Suite: Diện tích 62m², phòng khách riêng biệt, bồn tắm nằm và tầm nhìn toàn cảnh biển.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 10. ROOM-DESC-SP01 (SP-01, hotelId = 3)
        ingestAndActivate(
                "ROOM-DESC-SP01",
                RagScopeType.HOTEL_SCOPED,
                3,
                RagDocumentCategory.STATIC_ROOM_DESCRIPTION,
                "Mô tả diện tích và bố cục các hạng phòng cơ sở SP-01",
                "[STD] Standard: Diện tích 24m², thiết kế ấm cúng đậm nét vùng cao, yên tĩnh và tiện nghi gọn gàng.\n" +
                        "[SUP] Superior: Diện tích 30m², cửa sổ nhìn ra thung lũng mây mù Sapa, thoáng đãng và ấm áp.\n" +
                        "[DLX] Deluxe: Diện tích 36m², ban công view núi lãng mạn, không gian thư giãn tiện nghi.\n" +
                        "[FAM] Family Room: Diện tích 45m², bố cục 2 giường lớn cho gia đình ngắm cảnh Tây Bắc.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 11. POLICY-PETS (CHAIN_PUBLIC)
        ingestAndActivate(
                "POLICY-PETS",
                RagScopeType.CHAIN_PUBLIC,
                null,
                RagDocumentCategory.POLICY,
                "Quy định mang thú cưng và vật nuôi toàn chuỗi khách sạn",
                "Khách sạn quy định không cho phép mang theo thú cưng, vật nuôi (bao gồm chó, mèo, cún poodle và các loại động vật cảnh khác) vào khuôn viên phòng nghỉ cũng như các khu vực dịch vụ chung nhằm đảm bảo tiêu chuẩn vệ sinh và không gian yên tĩnh cho toàn bộ khách lưu trú. Trường hợp động vật dẫn đường hoặc hỗ trợ người khuyết tật (service animals) được phép lưu trú nhưng quý khách cần thông báo trước cho ban quản lý khi tiến hành đặt phòng.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 12. POLICY-EARLY-CHECKIN (CHAIN_PUBLIC)
        ingestAndActivate(
                "POLICY-EARLY-CHECKIN",
                RagScopeType.CHAIN_PUBLIC,
                null,
                RagDocumentCategory.POLICY,
                "Chính sách nhận phòng sớm và trả phòng muộn",
                "Khách sạn hỗ trợ quý khách nhận phòng sớm hoặc trả phòng muộn tùy thuộc vào tình trạng phòng trống thực tế tại thời điểm yêu cầu.\n" +
                        "Quy định phụ thu khi đến sớm:\n" +
                        "- Đến trước 06:00 sáng: Áp dụng phụ thu 100% giá phòng một đêm (kèm dịch vụ phòng đầy đủ).\n" +
                        "- Đến từ 06:00 sáng đến trước 12:00 trưa: Áp dụng mức phụ thu 50% giá phòng một đêm.\n" +
                        "- Đến từ 12:00 trưa đến trước 14:00 chiều: Được hỗ trợ nhận phòng sớm miễn phí nếu khách sạn có sẵn phòng sạch.\n" +
                        "Quy định phụ thu khi rời phòng muộn:\n" +
                        "- Rời phòng từ sau 12:00 trưa đến 18:00 chiều: Phụ thu 50% giá phòng một đêm.\n" +
                        "- Rời phòng sau 18:00 tối: Phụ thu 100% giá phòng một đêm.\n" +
                        "Quý khách vui lòng liên hệ trước với bộ phận lễ tân để được kiểm tra tình trạng phòng và hỗ trợ tốt nhất.",
                LocalDate.of(2026, 1, 1),
                1
        );

        // 13. POLICY-CHILD-SURCHARGE (CHAIN_PUBLIC)
        ingestAndActivate(
                "POLICY-CHILD-SURCHARGE",
                RagScopeType.CHAIN_PUBLIC,
                null,
                RagDocumentCategory.POLICY,
                "Chính sách phụ thu trẻ em và em bé sơ sinh lưu trú",
                "Chính sách phụ thu trẻ em và em bé tại hệ thống khách sạn:\n" +
                        "- Trẻ sơ sinh và trẻ nhỏ dưới 6 tuổi (từ 0 đến 5 tuổi, bao gồm bé sơ sinh vài tháng tuổi): Được miễn phí hoàn toàn tiền phòng khi ngủ chung giường có sẵn với bố mẹ (áp dụng tối đa 1 trẻ em dưới 6 tuổi cho mỗi phòng).\n" +
                        "- Trẻ em từ 6 đến 11 tuổi: Phụ thu theo quy định phòng tiêu chuẩn hoặc phụ thu giường phụ (extra bed) theo bảng giá niêm yết tại từng cơ sở khách sạn.\n" +
                        "- Trẻ em từ 12 tuổi trở lên: Được tính như người lớn theo sức chứa tiêu chuẩn của hạng phòng.\n" +
                        "Khách sạn hỗ trợ cung cấp cũi trẻ em (nôi em bé sơ sinh) miễn phí cho gia đình có bé nhỏ tùy thuộc vào số lượng sẵn có tại cơ sở.",
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
