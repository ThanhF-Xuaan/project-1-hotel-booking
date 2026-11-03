package vn.edu.utc.hotel_booking.modules.ai.advisor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.utc.hotel_booking.modules.ai.advisor.adapter.PublicBookingCoreAdapter;
import vn.edu.utc.hotel_booking.modules.ai.advisor.dto.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.enums.*;
import vn.edu.utc.hotel_booking.modules.ai.advisor.llm.RoomAdvisorLlmPort;
import vn.edu.utc.hotel_booking.modules.ai.advisor.session.InMemoryRoomAdvisorSessionStore;
import vn.edu.utc.hotel_booking.modules.booking.service.PublicBookingSearchService;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.DestinationSuggestionResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.DestinationType;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdvisorDependencyReviewRegressionTest {
    final PublicBookingSearchService core = mock(PublicBookingSearchService.class);
    final InMemoryRoomAdvisorSessionStore sessions = new InMemoryRoomAdvisorSessionStore();
    final RoomAdvisorOrchestrationService service = new RoomAdvisorOrchestrationService(new CriteriaValidationService(),
            new DeterministicRoomRecommendationEngine(), new PublicBookingCoreAdapter(core), sessions, mock(RoomAdvisorLlmPort.class));
    RoomSearchCriteriaDto criteria() {
        return RoomSearchCriteriaDto.builder().locationKeyword("SYNTHETIC_CITY").adults(2).roomsCount(1)
                .checkInDate(LocalDate.now().plusDays(1)).checkOutDate(LocalDate.now().plusDays(2)).build();
    }
    AdvisorResultDto invoke(String route) {
        var c = criteria();
        return switch(route) {
            case "chat" -> service.chat(AdvisorChatRequestDto.builder().message("Phòng cho hai người")
                    .criteriaPatch(CriteriaPatchDto.builder().locationKeyword(c.getLocationKeyword()).adults(2).roomsCount(1)
                            .checkInDate(c.getCheckInDate()).checkOutDate(c.getCheckOutDate()).build()).build(), "synthetic-owner");
            case "search" -> service.search(AdvisorSearchRequestDto.builder().criteria(c).build(), "synthetic-owner");
            case "refresh" -> {
                var session = sessions.getOrCreateSession(null,"synthetic-owner");
                session = sessions.updateSession(session.getSessionId(),"synthetic-owner",session.getSessionVersion(),c,List.of(),null);
                yield service.refresh(AdvisorRefreshRequestDto.builder().sessionId(session.getSessionId()).expectedSessionVersion(session.getSessionVersion()).build(),"synthetic-owner");
            }
            default -> throw new IllegalArgumentException();
        };
    }
    @ParameterizedTest @ValueSource(strings={"chat","search","refresh"})
    void destinationOutageCannotBecomeNoMatch(String route) {
        when(core.getDestinationSuggestions(anyString())).thenThrow(new RuntimeException("SYNTHETIC_OUTAGE"));
        var result = invoke(route);
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.TEMP_UNAVAILABLE);
        assertThat(result.getRecommendations()).isEmpty();
        assertThat(result.getCoverage().getStatus()).isNotEqualTo(CoverageStatus.COMPLETE);
        verify(core,never()).searchRooms(any()); verify(core,never()).searchHotels(any());
    }
    @Test void unresolvedDestinationNeedsInformationInsteadOfClaimingNoMatches() {
        when(core.getDestinationSuggestions(anyString())).thenReturn(List.of());
        assertThat(invoke("search").getStatus()).isEqualTo(AdvisorStatus.NEED_INFO);
        verify(core,never()).searchRooms(any());
    }
    @Test void nullRoomPayloadIsDependencyFailureRatherThanEmptySearch() {
        var c = criteria(); c.setHotelId(1);
        when(core.searchRooms(any())).thenReturn(null);
        var result = service.search(AdvisorSearchRequestDto.builder().criteria(c).build(),"synthetic-owner");
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.TEMP_UNAVAILABLE);
    }
    @Test void nullDestinationPayloadFailsClosed() {
        when(core.getDestinationSuggestions(anyString())).thenReturn(null);
        assertThat(invoke("search").getStatus()).isEqualTo(AdvisorStatus.TEMP_UNAVAILABLE);
        verify(core, never()).searchRooms(any());
    }
    @Test void validEmptyCatalogRemainsCompleteNoMatch() {
        var c = criteria(); c.setHotelId(1);
        when(core.searchRooms(any())).thenReturn(List.of());
        var result = service.search(AdvisorSearchRequestDto.builder().criteria(c).build(), "synthetic-owner");
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NO_MATCH);
        assertThat(result.getCoverage().getStatus()).isEqualTo(CoverageStatus.COMPLETE);
    }
    @Test void refreshUsesResolvedHotelAndPersistsNewVersion() {
        when(core.getDestinationSuggestions(anyString())).thenReturn(List.of(
                DestinationSuggestionResponse.builder().id(2).type(DestinationType.HOTEL).title("SYNTHETIC_H2").build()));
        when(core.searchRooms(any())).thenReturn(List.of());
        var result = invoke("refresh");
        assertThat(result.getStatus()).isEqualTo(AdvisorStatus.NO_MATCH);
        verify(core).searchRooms(argThat(request -> Integer.valueOf(2).equals(request.getHotelId())));
        var stored = sessions.getSession(result.getSessionId(), "synthetic-owner").orElseThrow();
        assertThat(stored.getSessionVersion()).isEqualTo(result.getSessionVersion());
        assertThat(stored.getLastResult().getStatus()).isEqualTo(AdvisorStatus.NO_MATCH);
    }
}
