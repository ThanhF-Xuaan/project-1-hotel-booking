package vn.edu.utc.hotel_booking.modules.ai.advisor.session;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.service.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.RoomAdvisorLlmPort;
import java.util.*;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionSnapshotFollowupTest {
    final InMemoryRoomAdvisorSessionStore store = new InMemoryRoomAdvisorSessionStore();
    RoomSearchCriteriaDto criteria() {
        return RoomSearchCriteriaDto.builder().locationKeyword("SYNTHETIC").adults(2).roomsCount(1)
                .checkInDate(LocalDate.now().plusDays(1)).checkOutDate(LocalDate.now().plusDays(2))
                .requiredFeatureCodes(new ArrayList<>(List.of("WIFI"))).budget(BudgetDto.builder().amount("1000").build()).build();
    }
    @Test void inputAndReturnedSnapshotsCannotMutateStoredData() {
        var s=store.getOrCreateSession(null,"owner"); var c=criteria();
        var result=AdvisorResultDto.builder().reply("SYNTHETIC").sessionVersion(1L).build();
        var updated=store.updateSession(s.getSessionId(),"owner",s.getSessionVersion(),c,List.of(),result);
        c.setHotelId(9); c.getRequiredFeatureCodes().clear(); c.getBudget().setAmount("0"); result.setReply("MUTATED");
        updated.getCriteria().setHotelId(8); updated.getLastResult().setReply("MUTATED_RETURN");
        var stored=store.getSession(s.getSessionId(),"owner").orElseThrow();
        assertThat(stored.getCriteria().getHotelId()).isNull();
        assertThat(stored.getCriteria().getRequiredFeatureCodes()).containsExactly("WIFI");
        assertThat(stored.getCriteria().getBudget().getAmount()).isEqualTo("1000");
        assertThat(stored.getLastResult().getReply()).isEqualTo("SYNTHETIC");
        assertThat(stored.getLastResult().getSessionVersion()).isEqualTo(stored.getSessionVersion());
        stored.getCriteria().setHotelId(10);
        assertThat(store.getOrCreateSession(s.getSessionId(),"owner").getCriteria().getHotelId()).isNull();
    }
    @Test void failedRefreshCannotChangeStoredCriteria() {
        var s=store.getOrCreateSession(null,"owner");
        s=store.updateSession(s.getSessionId(),"owner",s.getSessionVersion(),criteria(),List.of(),null);
        var port=mock(RoomAdvisorCorePort.class);
        when(port.resolveDestination(anyString(),isNull(),isNull())).thenReturn(DestinationResolutionResult.builder().hotelId(7).build());
        when(port.discoverAndQuoteCandidates(any(),isNull(),anyInt())).thenThrow(new RuntimeException("SYNTHETIC_FAILURE"));
        var service=new RoomAdvisorOrchestrationService(new CriteriaValidationService(),new DeterministicRoomRecommendationEngine(),port,store,mock(RoomAdvisorLlmPort.class));
        var request=AdvisorRefreshRequestDto.builder().sessionId(s.getSessionId()).expectedSessionVersion(s.getSessionVersion()).build();
        assertThatThrownBy(()->service.refresh(request,"owner")).isInstanceOf(AdvisorDependencyUnavailableException.class)
                .hasMessage("Advisor dependency unavailable");
        var after=store.getSession(s.getSessionId(),"owner").orElseThrow();
        assertThat(after.getSessionVersion()).isEqualTo(s.getSessionVersion());
        assertThat(after.getCriteria().getHotelId()).isNull();
    }
    @Test void losingCasCannotMutateWinner() {
        var s=store.getOrCreateSession(null,"owner");
        s=store.updateSession(s.getSessionId(),"owner",s.getSessionVersion(),criteria(),List.of(),null);
        var stale=store.getSession(s.getSessionId(),"owner").orElseThrow();
        var winner=criteria(); winner.setHotelId(2);
        store.updateSession(s.getSessionId(),"owner",s.getSessionVersion(),winner,List.of(),null);
        stale.getCriteria().setHotelId(99);
        assertThatThrownBy(()->store.updateSession(stale.getSessionId(),"owner",stale.getSessionVersion(),stale.getCriteria(),List.of(),null))
                .isInstanceOf(SessionVersionConflictException.class);
        assertThat(store.getSession(stale.getSessionId(),"owner").orElseThrow().getCriteria().getHotelId()).isEqualTo(2);
    }
}
