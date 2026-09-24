package vn.edu.utc.hotel_booking.aiassistant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeRepository;
import vn.edu.utc.hotel_booking.aiassistant.knowledge.KnowledgeService;
import vn.edu.utc.hotel_booking.aiassistant.service.ChatService;
import vn.edu.utc.hotel_booking.aiassistant.tool.RoomQuoteTool;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;
import vn.edu.utc.hotel_booking.organization.service.HotelCatalogService;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    @Test
    void noApprovedSourceDoesNotCallLanguageModel() {
        StaffAccessService access = mock(StaffAccessService.class);
        HotelCatalogService hotels = mock(HotelCatalogService.class);
        KnowledgeRepository repository = mock(KnowledgeRepository.class);
        KnowledgeService knowledge = mock(KnowledgeService.class);
        RoomQuoteTool quoteTool = mock(RoomQuoteTool.class);
        EmbeddingModel embedding = mock(EmbeddingModel.class);
        ChatModel model = mock(ChatModel.class);
        var actor = new StaffActor(1, "CHAIN", null, "CHAIN_ADMIN", Set.of("USE:AI_ASSISTANT"));
        when(embedding.embed("Giờ nhận phòng là mấy giờ?")).thenReturn(new float[1536]);
        when(repository.search(any(), any(), isNull(), anyInt())).thenReturn(List.of());
        var service = new ChatService(access, hotels, repository, knowledge, quoteTool,
                embedding, model, Clock.systemUTC(), 4);

        var result = service.chat(actor, "Giờ nhận phòng là mấy giờ?", null, null);

        assertEquals("NO_SOURCE", result.type());
        verifyNoInteractions(model);
    }
}
