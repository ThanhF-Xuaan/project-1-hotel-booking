package vn.edu.utc.hotel_booking.aiassistant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeRepository;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeService;
import vn.edu.utc.hotel_booking.audit.service.AuditService;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnowledgeScopeTest {
    private final KnowledgeRepository repository = mock(KnowledgeRepository.class);
    private final HotelCatalogService hotels = mock(HotelCatalogService.class);
    private final KnowledgeService service = new KnowledgeService(repository,
            new StaffAccessService(null), hotels, mock(AuditService.class));

    @ParameterizedTest
    @ValueSource(ints = {100, 200})
    void samePropertyCanCreateDocumentsAndReadCitations(int hotelId) {
        when(hotels.find(hotelId)).thenReturn(hotel(hotelId));
        var actor = actor(hotelId);
        assertEquals("DRAFT", service.create(actor, "Policy", "Content", "STAFF", hotelId).status());
        var chunkId = UUID.randomUUID();
        var source = new KnowledgeRepository.SourceChunk(chunkId, UUID.randomUUID(), "Policy",
                "Content", hotelId, 1, 1.0);
        when(repository.source(chunkId)).thenReturn(List.of(source));
        assertEquals(source, service.source(actor, chunkId));
    }

    @Test
    void differentPropertyCannotCreateOrReadCitation() {
        when(hotels.find(201)).thenReturn(hotel(201));
        var denied = assertThrows(AppException.class,
                () -> service.create(actor(200), "Policy", "Content", "STAFF", 201));
        assertEquals(ErrorCode.UNAUTHORIZED, denied.getErrorCode());
        verifyNoInteractions(repository);
        var chunkId = UUID.randomUUID();
        when(repository.source(chunkId)).thenReturn(List.of(new KnowledgeRepository.SourceChunk(
                chunkId, UUID.randomUUID(), "Policy", "Content", 201, 1, 1.0)));
        assertEquals(ErrorCode.UNAUTHORIZED,
                assertThrows(AppException.class, () -> service.source(actor(200), chunkId)).getErrorCode());
    }

    @Test
    void propertyWithMissingScopeIsDenied() {
        when(hotels.find(200)).thenReturn(hotel(200));
        assertEquals(ErrorCode.UNAUTHORIZED,
                assertThrows(AppException.class, () -> service.authorizeScope(actor(null), 200)).getErrorCode());
    }

    private StaffActor actor(Integer hotelId) {
        return new StaffActor(1, "PROPERTY", hotelId, "PROPERTY_MANAGER",
                Set.of("CREATE:KNOWLEDGE", "USE:AI_ASSISTANT"));
    }

    private HotelCatalogService.HotelInfo hotel(int id) {
        return new HotelCatalogService.HotelInfo(id, 1, "Hotel", "Address", BigDecimal.ZERO);
    }
}
