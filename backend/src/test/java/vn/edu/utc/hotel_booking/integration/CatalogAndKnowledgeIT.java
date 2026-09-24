package vn.edu.utc.hotel_booking.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeRepository;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeService;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeIngestionWorker;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.VectorLiteral;
import vn.edu.utc.hotel_booking.aiassistant.service.ChatService;
import vn.edu.utc.hotel_booking.aiassistant.tool.RoomQuoteTool;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;
import vn.edu.utc.hotel_booking.inventory.api.StayCriteria;
import vn.edu.utc.hotel_booking.inventory.service.RoomAvailabilityService;
import vn.edu.utc.hotel_booking.pricing.service.PricingQuoteService;
import vn.edu.utc.hotel_booking.pricing.service.PriceCalculator;
import vn.edu.utc.hotel_booking.pricing.repository.PricingRuleRepository;
import vn.edu.utc.hotel_booking.property.service.RoomTypeCatalogService;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import tools.jackson.databind.ObjectMapper;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mockingDetails;

@SpringBootTest
@Testcontainers
class CatalogAndKnowledgeIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg16-trixie")
                    .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RoomAvailabilityService availability;
    @Autowired PricingQuoteService pricing;
    @Autowired KnowledgeRepository knowledge;
    @Autowired KnowledgeService knowledgeService;
    @Autowired StaffAccessService access;
    @Autowired HotelCatalogService hotels;
    @Autowired RoomQuoteTool roomQuoteTool;
    @Autowired RoomTypeCatalogService roomTypes;
    @Autowired PlatformTransactionManager transactionManager;

    private int hotelId;
    private int typeId;
    private int roomId;
    private int taxId;

    @BeforeEach
    void fixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int region = jdbc.queryForObject("INSERT INTO regions(code,name) VALUES (?,?) RETURNING id",
                Integer.class, "R" + suffix, "Region " + suffix);
        hotelId = jdbc.queryForObject("""
                INSERT INTO hotels(region_id,name,address,service_fee_percent)
                VALUES (?,?,?,5.00) RETURNING id
                """, Integer.class, region, "Hotel " + suffix, "Address " + suffix);
        int roomType = jdbc.queryForObject("INSERT INTO room_types(code,name) VALUES (?,?) RETURNING id",
                Integer.class, "T" + suffix, "Room " + suffix);
        int tax = jdbc.queryForObject("""
                INSERT INTO tax_categories(category_code,category_name) VALUES (?,?) RETURNING id
                """, Integer.class, "TAX" + suffix, "Tax " + suffix);
        taxId = tax;
        typeId = jdbc.queryForObject("""
                INSERT INTO hotel_room_types(hotel_id,room_type_id,tax_category_id,base_price,total_quantity)
                VALUES (?,?,?,?,1) RETURNING id
                """, Integer.class, hotelId, roomType, tax, 100000);
        roomId = jdbc.queryForObject("""
                INSERT INTO room_instances(hotel_id,hotel_room_type_id,room_number)
                VALUES (?,?,?) RETURNING id
                """, Integer.class, hotelId, typeId, "R-" + suffix);
        jdbc.update("""
                INSERT INTO vat_rules(tax_category_id,vat_code,vat_name,vat_percent,start_date)
                VALUES (?,?,?,?,?)
                """, tax, "VAT" + suffix, "VAT", 8,
                Date.valueOf(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void migrationAvailabilityAndQuoteWorkOnRealPostgres() {
        Integer applied = jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertEquals(3, applied);
        var stay = new StayCriteria(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11), 2, 0, 0);
        assertEquals(1, availability.availability(typeId, stay).availableRooms());
        assertEquals("113400.00", pricing.quote(typeId, stay).totalAmount());
        jdbc.update("INSERT INTO room_slots(room_instance_id,slot_date,status) VALUES (?,?, 'BLOCKED')",
                roomId, Date.valueOf(stay.checkIn()));
        assertEquals(0, availability.availability(typeId, stay).availableRooms());
    }

    @ParameterizedTest
    @ValueSource(strings = {"READY", "OCCUPIED", "CLEANING"})
    void currentRoomStatusDoesNotRemoveFutureStock(String status) {
        var stay = new StayCriteria(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11), 2, 0, 0);
        jdbc.update("UPDATE room_instances SET current_status = ? WHERE id = ?", status, roomId);
        jdbc.update("INSERT INTO room_slots(room_instance_id,slot_date,status) VALUES (?,?,'OCCUPIED')",
                roomId, Date.valueOf(stay.checkIn().minusDays(1)));
        jdbc.update("INSERT INTO room_slots(room_instance_id,slot_date,status) VALUES (?,?,'RESERVED')",
                roomId, Date.valueOf(stay.checkOut()));
        assertEquals(1, availability.availability(typeId, stay).availableRooms());
        jdbc.update("INSERT INTO room_slots(room_instance_id,slot_date,status) VALUES (?,?,'OCCUPIED')",
                roomId, Date.valueOf(stay.checkIn()));
        assertEquals(0, availability.availability(typeId, stay).availableRooms());
    }

    @Test
    void maintenanceUsesInclusiveBlockDatesAndKeepsOperationalClosure() {
        var stay = new StayCriteria(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11), 2, 0, 0);
        jdbc.update("UPDATE room_instances SET current_status = 'MAINTENANCE' WHERE id = ?", roomId);
        assertEquals(0, availability.availability(typeId, stay).availableRooms());
        jdbc.update("UPDATE room_instances SET current_status = 'READY' WHERE id = ?", roomId);
        jdbc.update("""
                INSERT INTO room_maintenance_blocks(room_instance_id,start_date,end_date,reason)
                VALUES (?,DATE '2026-10-08',DATE '2026-10-09','Repair')
                """, roomId);
        assertEquals(1, availability.availability(typeId, stay).availableRooms());
        jdbc.update("UPDATE room_maintenance_blocks SET end_date = DATE '2026-10-10' WHERE room_instance_id = ?", roomId);
        assertEquals(0, availability.availability(typeId, stay).availableRooms());
        jdbc.update("UPDATE room_maintenance_blocks SET status = 'CANCELLED' WHERE room_instance_id = ?", roomId);
        assertEquals(1, availability.availability(typeId, stay).availableRooms());
        jdbc.update("""
                UPDATE room_maintenance_blocks SET status = 'ACTIVE', start_date = DATE '2026-10-11',
                    end_date = DATE '2026-10-12' WHERE room_instance_id = ?
                """, roomId);
        assertEquals(1, availability.availability(typeId, stay).availableRooms());
    }

    @Test
    void countersAndSoftDeletedRoomsStillLimitStock() {
        var stay = new StayCriteria(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), 2, 0, 0);
        jdbc.update("""
                INSERT INTO room_availability(hotel_room_type_id,date,total_rooms,booked_rooms)
                VALUES (?,DATE '2026-10-11',1,1)
                """, typeId);
        assertEquals(0, availability.availability(typeId, stay).availableRooms());
        jdbc.update("UPDATE room_availability SET booked_rooms = 0 WHERE hotel_room_type_id = ?", typeId);
        assertEquals(1, availability.availability(typeId, stay).availableRooms());
        jdbc.update("UPDATE room_instances SET is_deleted = true WHERE id = ?", roomId);
        assertEquals(0, availability.availability(typeId, stay).availableRooms());
    }

    @ParameterizedTest
    @CsvSource({"INACTIVE,false", "ACTIVE,true"})
    void disabledOrDeletedHolidayDoesNotIncreasePrice(String status, boolean deleted) {
        jdbc.update("""
                INSERT INTO pricing_rule_types(code,display_name) VALUES ('HOLIDAY','Holiday')
                ON CONFLICT (code) DO NOTHING
                """);
        int holiday = jdbc.queryForObject("""
                INSERT INTO holiday_calendars(name,date) VALUES (?,DATE '2026-10-10') RETURNING id
                """, Integer.class, "Holiday " + UUID.randomUUID());
        jdbc.update("""
                INSERT INTO pricing_rules(hotel_room_type_id,holiday_calendar_id,rule_type,
                    adjustment_type,adjustment_value,start_date,end_date)
                VALUES (?,?,'HOLIDAY','PERCENT',10,DATE '2026-10-10',DATE '2026-10-11')
                """, typeId, holiday);
        var stay = new StayCriteria(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), 2, 0, 0);
        var activeQuote = pricing.quote(typeId, stay);
        assertEquals("10000.00", activeQuote.nights().get(0).adjustmentAmount());
        assertEquals("0.00", activeQuote.nights().get(1).adjustmentAmount());
        jdbc.update("UPDATE holiday_calendars SET status = ?, is_deleted = ? WHERE id = ?", status, deleted, holiday);
        assertEquals("226800.00", pricing.quote(typeId, stay).totalAmount());
    }

    @Test
    void batchedRulesKeepVatCampaignAndSurchargeEffectiveDates() {
        jdbc.update("UPDATE hotel_room_types SET max_adults = 3, max_total_guests = 3 WHERE id = ?", typeId);
        jdbc.update("UPDATE vat_rules SET end_date = DATE '2026-10-11' WHERE tax_category_id = ?", taxId);
        jdbc.update("""
                INSERT INTO vat_rules(tax_category_id,vat_code,vat_name,vat_percent,start_date)
                VALUES (?,?,'New VAT',10,DATE '2026-10-12')
                """, taxId, "VAT_NEXT_" + UUID.randomUUID());
        jdbc.update("""
                INSERT INTO pricing_rule_types(code,display_name) VALUES ('PEAK_SEASON','Peak')
                ON CONFLICT (code) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO pricing_rules(hotel_room_type_id,rule_type,adjustment_type,adjustment_value,start_date,end_date)
                VALUES (?,'PEAK_SEASON','FIXED',20000,DATE '2026-10-11',DATE '2026-10-11'),
                       (?,'PEAK_SEASON','FIXED',99999,DATE '2026-10-14',DATE '2026-10-14')
                """, typeId, typeId);
        jdbc.update("""
                INSERT INTO discount_rule_types(code,display_name,priority)
                VALUES ('LONG_STAY','Long stay',1),('SPECIAL_CAMPAIGN','Campaign',2)
                ON CONFLICT (code) DO NOTHING
                """);
        int campaign = jdbc.queryForObject("""
                INSERT INTO campaigns(hotel_id,name,start_date,end_date)
                VALUES (?,'One day sale',DATE '2026-10-12',DATE '2026-10-12') RETURNING id
                """, Integer.class, hotelId);
        jdbc.update("""
                INSERT INTO discount_rules(hotel_room_type_id,rule_type,conditions,discount_type,
                    discount_value,start_date,end_date)
                VALUES (?,'LONG_STAY','{"minNights":2}','FIXED',5000,DATE '2026-10-10',DATE '2026-10-13')
                """, typeId);
        jdbc.update("""
                INSERT INTO discount_rules(hotel_room_type_id,campaign_id,rule_type,discount_type,
                    discount_value,start_date,end_date)
                VALUES (?,?,'SPECIAL_CAMPAIGN','FIXED',10000,DATE '2026-10-11',DATE '2026-10-13')
                """, typeId, campaign);
        int agePolicy = jdbc.queryForObject("""
                INSERT INTO hotel_age_policies(hotel_id,guest_type,min_age,max_age)
                VALUES (?,'ADULT',18,120) RETURNING id
                """, Integer.class, hotelId);
        jdbc.update("""
                INSERT INTO surcharge_rules(hotel_room_type_id,age_policy_id,rule_type,adjustment_type,
                    adjustment_value,start_date,end_date)
                VALUES (?,?,'EXTRA_PERSON','FIXED',3000,DATE '2026-10-13',DATE '2026-10-13')
                """, typeId, agePolicy);

        var stay = new StayCriteria(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 14), 3, 0, 0);
        var quote = pricing.quote(typeId, stay);
        assertEquals(List.of("8.00", "8.00", "10.00", "10.00"),
                quote.nights().stream().map(PricingQuoteService.DailyQuote::vatPercent).toList());
        assertEquals(List.of("5000.00", "5000.00", "10000.00", "5000.00"),
                quote.nights().stream().map(PricingQuoteService.DailyQuote::discountAmount).toList());
        assertEquals(List.of("0.00", "0.00", "0.00", "3000.00"),
                quote.nights().stream().map(PricingQuoteService.DailyQuote::surchargeAmount).toList());
        assertEquals(List.of("107730.00", "130410.00", "103950.00", "113190.00"),
                quote.nights().stream().map(PricingQuoteService.DailyQuote::netPrice).toList());
        assertEquals("455280.00", quote.totalAmount());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 30})
    void pricingRuleSqlCountDoesNotGrowWithNumberOfNights(int nights) {
        var observedJdbc = spy(jdbc);
        var service = new PricingQuoteService(roomTypes, hotels, availability,
                new PricingRuleRepository(observedJdbc), new PriceCalculator(), new ObjectMapper(), Clock.systemUTC());
        var checkIn = LocalDate.of(2026, 10, 1);
        var quote = service.quote(typeId, new StayCriteria(checkIn, checkIn.plusDays(nights), 2, 0, 0));
        assertEquals(nights, quote.nights().size());
        long selects = mockingDetails(observedJdbc).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("query"))
                // Count the entry overload, not JdbcTemplate's internal query delegation again.
                .filter(invocation -> invocation.getMethod().getParameterCount() == 3
                        && invocation.getMethod().getParameterTypes()[1] == RowMapper.class)
                .count();
        assertEquals(4, selects);
    }

    @Test
    void missingVatForOneNightStillRejectsWholeQuote() {
        jdbc.update("UPDATE vat_rules SET end_date = DATE '2026-10-10' WHERE tax_category_id = ?", taxId);
        var stay = new StayCriteria(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12), 2, 0, 0);
        assertEquals(ErrorCode.PRICING_UNAVAILABLE,
                assertThrows(AppException.class, () -> pricing.quote(typeId, stay)).getErrorCode());
    }


    @Test
    void vectorRetrievalHonorsHotelScopeAndRevocation() {
        UUID document = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        UUID chunk = UUID.randomUUID();
        jdbc.update("INSERT INTO ai_documents(id,title,visibility,hotel_id) VALUES (?,?, 'STAFF',?)",
                document, "Local policy", hotelId);
        jdbc.update("""
                INSERT INTO ai_document_versions(id,document_id,version_number,content,checksum,status)
                VALUES (?,?,1,'Check-in policy',?,'PUBLISHED')
                """, version, document, "0".repeat(64));
        float[] embedding = new float[1536];
        embedding[0] = 1f;
        jdbc.update("INSERT INTO ai_chunks(id,version_id,chunk_index,content,embedding) VALUES (?,?,0,?,?::vector)",
                chunk, version, "Check-in at 14:00", VectorLiteral.of(embedding));

        var allowed = new StaffActor(1, "PROPERTY", hotelId, "PROPERTY_MANAGER", Set.of("USE:AI_ASSISTANT"));
        var denied = new StaffActor(2, "PROPERTY", hotelId + 1, "PROPERTY_MANAGER", Set.of("USE:AI_ASSISTANT"));
        assertEquals(1, knowledge.search(VectorLiteral.of(embedding), allowed, hotelId, 4).size());
        assertTrue(knowledge.search(VectorLiteral.of(embedding), denied, null, 4).isEmpty());
        jdbc.update("UPDATE ai_documents SET status = 'REVOKED' WHERE id = ?", document);
        assertTrue(knowledge.search(VectorLiteral.of(embedding), allowed, hotelId, 4).isEmpty());
        assertTrue(knowledge.source(chunk).isEmpty());
    }

    @Test
    void draftIngestPublishChatAndRevokeStayConsistent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int roleId = jdbc.queryForObject("INSERT INTO roles(name,code) VALUES (?,?) RETURNING id",
                Integer.class, "Test admin", "TEST_ADMIN_" + suffix);
        int staffId = jdbc.queryForObject("""
                INSERT INTO staffs(keycloak_id,role_id,scope_type,username,first_name,last_name,full_name)
                VALUES (?,?,'CHAIN',?,'Test','Admin','Test Admin') RETURNING id
                """, Integer.class, UUID.randomUUID(), roleId, "test_admin_" + suffix);
        var actor = new StaffActor(staffId, "CHAIN", null, "CHAIN_ADMIN",
                Set.of("CREATE:KNOWLEDGE", "PUBLISH:KNOWLEDGE", "REVOKE:KNOWLEDGE", "USE:AI_ASSISTANT"));
        var created = knowledgeService.create(actor, "Check-in policy", "Khách sạn nhận phòng lúc 14:00.",
                "STAFF", null);
        float[] vector = new float[1536];
        vector[0] = 1f;
        EmbeddingModel embedding = mock(EmbeddingModel.class);
        when(embedding.embed(any(String.class))).thenReturn(vector);
        var worker = new KnowledgeIngestionWorker(knowledge, embedding,
                new TransactionTemplate(transactionManager), 1536);
        worker.processOne();
        worker.processOne();
        assertEquals("SUCCEEDED", knowledge.job(created.jobId()).getFirst().status());
        Integer count = jdbc.queryForObject("SELECT count(*) FROM ai_chunks WHERE version_id = ?",
                Integer.class, created.versionId());
        assertEquals(1, count);
        knowledgeService.publish(actor, created.documentId());

        ChatModel model = mock(ChatModel.class);
        when(model.call(any(Message.class), any(Message.class))).thenReturn("Giờ nhận phòng là 14:00.");
        var chat = new ChatService(access, hotels, knowledge, knowledgeService, roomQuoteTool,
                embedding, model, Clock.systemUTC(), 4);
        var reply = chat.chat(actor, "Giờ nhận phòng?", null, null);
        assertEquals("GROUNDED", reply.type());
        assertEquals(1, reply.citations().size());
        assertEquals("Khách sạn nhận phòng lúc 14:00.",
                knowledgeService.source(actor, reply.citations().getFirst().sourceId()).content());

        knowledgeService.revoke(actor, created.documentId());
        assertEquals("NO_SOURCE", chat.chat(actor, "Giờ nhận phòng?", null, null).type());
        assertEquals(3, jdbc.queryForObject("""
                SELECT count(*) FROM audit_logs WHERE entity_name = 'ai_documents' AND entity_id = ?
                """, Integer.class, created.documentId().toString()));
    }

    @Test
    void exhaustedIngestionLeaseIsMarkedFailed() {
        UUID document = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        UUID job = UUID.randomUUID();
        jdbc.update("INSERT INTO ai_documents(id,title,visibility,hotel_id) VALUES (?,?, 'STAFF',?)",
                document, "Stale job", hotelId);
        jdbc.update("""
                INSERT INTO ai_document_versions(id,document_id,version_number,content,checksum)
                VALUES (?,?,1,'Stale content',?)
                """, version, document, "0".repeat(64));
        jdbc.update("""
                INSERT INTO ai_ingest_jobs(id,version_id,status,attempts,claimed_at)
                VALUES (?,?,'RUNNING',3,now() - interval '6 minutes')
                """, job, version);
        assertTrue(knowledge.claimJob().isEmpty());
        assertEquals("FAILED", knowledge.job(job).getFirst().status());
        assertEquals("FAILED", jdbc.queryForObject(
                "SELECT status FROM ai_document_versions WHERE id = ?", String.class, version));
    }
}
