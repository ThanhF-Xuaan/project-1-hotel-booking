package vn.edu.utc.hotel_booking.modules.ai.groupassignment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.GroupRoomDemand;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentCandidate;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview.Status;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentAuthorizationPort;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentAuthorizationPort.Scope;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.*;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.DeterministicGroupRoomAssignmentEngine;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.GroupRoomAssignmentPreviewService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static vn.edu.utc.hotel_booking.modules.ai.groupassignment.GroupPreviewTestData.*;

class GroupRoomAssignmentPreviewServiceTest {
    GroupAssignmentAuthorizationPort authorization;
    GroupAssignmentDataPort data;
    DeterministicGroupRoomAssignmentEngine engine;
    GroupRoomAssignmentPreviewService service;
    GroupRoomAssignmentPreviewRequest request;
    Snapshot source;

    @BeforeEach
    void setup() {
        authorization = mock(GroupAssignmentAuthorizationPort.class); data = mock(GroupAssignmentDataPort.class);
        engine = spy(new DeterministicGroupRoomAssignmentEngine());
        service = new GroupRoomAssignmentPreviewService(authorization, data, engine, Clock.fixed(NOW, ZoneOffset.UTC));
        request = request(2, 2); source = snapshot(request, List.of(room(11, 2, 1, 1), room(12, 2, 1, 2)), 2, 2);
        when(authorization.authorize(1, 501)).thenReturn(Optional.of(SCOPE));
        when(data.read(SCOPE, request.checkInDate(), request.checkOutDate(), NOW)).thenReturn(source);
    }

    @Test
    void authorizationPrecedesReadAndRevalidationFollowsSearch() {
        var result = service.preview(request);
        assertThat(result.status()).isEqualTo(Status.COMPLETE);
        assertThat(result.previewOnly()).isTrue(); assertThat(result.dataMode()).isEqualTo("SYNTHETIC");
        var order = inOrder(authorization, data, engine);
        order.verify(authorization).authorize(1, 501);
        order.verify(data).read(SCOPE, request.checkInDate(), request.checkOutDate(), NOW);
        order.verify(engine).preview(request, source, NOW);
        order.verify(authorization).authorize(1, 501);
        order.verifyNoMoreInteractions();
        assertThatThrownBy(() -> result.assignments().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> source.candidates().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deniedOrMalformedAuthorizationNeverReadsData() {
        when(authorization.authorize(1, 501)).thenReturn(Optional.empty());
        assertFailure(service.preview(request), Status.ACCESS_DENIED);
        when(authorization.authorize(1, 501)).thenReturn(Optional.of(new Scope(2, 10, 501)));
        assertFailure(service.preview(request), Status.TEMP_UNAVAILABLE);
        when(authorization.authorize(1, 501)).thenReturn(Optional.of(new Scope(1, 0, 501)));
        assertFailure(service.preview(request), Status.TEMP_UNAVAILABLE);
        when(authorization.authorize(1, 501)).thenReturn(null);
        assertFailure(service.preview(request), Status.TEMP_UNAVAILABLE);
        verifyNoInteractions(data, engine);
    }

    @Test
    void revokedOrChangedScopeDiscardsAssignments() {
        when(authorization.authorize(1, 501)).thenReturn(Optional.of(SCOPE), Optional.empty());
        assertFailure(service.preview(request), Status.ACCESS_DENIED);
        when(authorization.authorize(1, 501)).thenReturn(Optional.of(SCOPE), Optional.of(new Scope(1, 11, 501)));
        assertFailure(service.preview(request), Status.ACCESS_DENIED);
    }

    @Test
    void dependencyExceptionsAndNullNeverBecomeEmptySuccessOrExposePayload() {
        when(data.read(any(), any(), any(), any())).thenReturn(null);
        assertFailure(service.preview(request), Status.TEMP_UNAVAILABLE);
        when(data.read(any(), any(), any(), any())).thenThrow(new IllegalStateException("PRIVATE_SOURCE_MESSAGE"));
        var result = service.preview(request);
        assertFailure(result, Status.TEMP_UNAVAILABLE);
        assertThat(result.toString()).doesNotContain("PRIVATE_SOURCE_MESSAGE");
        when(authorization.authorize(1, 501)).thenThrow(new IllegalStateException("PRIVATE_AUTH_MESSAGE"));
        result = service.preview(request);
        assertFailure(result, Status.TEMP_UNAVAILABLE);
        assertThat(result.toString()).doesNotContain("PRIVATE_AUTH_MESSAGE");
    }

    @Test
    void snapshotExpiryDuringSearchDiscardsResult() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW, NOW, NOW.plusSeconds(56));
        service = new GroupRoomAssignmentPreviewService(authorization, data, engine, clock);
        assertFailure(service.preview(request), Status.TEMP_UNAVAILABLE);
    }

    static Stream<Arguments> invalidRequests() {
        var request = request(2, 2);
        return Stream.of(Arguments.of((Object) null), Arguments.of(request(101, 1)), Arguments.of(request(0, 1)),
                Arguments.of(request(1, 0)), Arguments.of(request(1, 32)),
                Arguments.of(new GroupRoomAssignmentPreviewRequest(0, 501L, request.checkInDate(), request.checkOutDate(), request.demands())),
                Arguments.of(new GroupRoomAssignmentPreviewRequest(1, null, request.checkInDate(), request.checkOutDate(), request.demands())),
                Arguments.of(new GroupRoomAssignmentPreviewRequest(1, 501L, request.checkInDate(), request.checkOutDate(), List.of(new GroupRoomDemand("A", 0, 1)))),
                Arguments.of(new GroupRoomAssignmentPreviewRequest(1, 501L, request.checkInDate(), request.checkOutDate(), List.of(new GroupRoomDemand("A", 1, 0)))),
                Arguments.of(new GroupRoomAssignmentPreviewRequest(1, 501L, request.checkInDate(), request.checkOutDate(), List.of(new GroupRoomDemand("private name", 1, 1)))),
                Arguments.of(new GroupRoomAssignmentPreviewRequest(1, 501L, request.checkInDate(), request.checkOutDate(), List.of(new GroupRoomDemand("A", 1, 1), new GroupRoomDemand("A", 2, 1)))));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidRequestStopsBeforeAnyDependency(GroupRoomAssignmentPreviewRequest invalid) {
        assertFailure(service.preview(invalid), Status.INVALID_REQUEST);
        verifyNoInteractions(authorization, data, engine);
    }

    static Stream<Arguments> invalidSnapshots() {
        List<UnaryOperator<Snapshot>> faults = List.of(
                s -> replace(s, new Scope(2, 10, 501), s.observedAt(), s.validUntil(), s.basis(), s.dataMode(), s.coverage()),
                s -> replace(s, s.scope(), NOW.plusSeconds(1), s.validUntil(), s.basis(), s.dataMode(), s.coverage()),
                s -> replace(s, s.scope(), NOW.minusSeconds(61), s.validUntil(), s.basis(), s.dataMode(), s.coverage()),
                s -> replace(s, s.scope(), s.observedAt(), NOW, s.basis(), s.dataMode(), s.coverage()),
                s -> replace(s, s.scope(), s.observedAt(), s.validUntil(), "UNKNOWN", s.dataMode(), s.coverage()),
                s -> replace(s, s.scope(), s.observedAt(), s.validUntil(), s.basis(), "AUTHORITATIVE", s.coverage()),
                s -> replace(s, s.scope(), s.observedAt(), s.validUntil(), s.basis(), s.dataMode(), Coverage.PARTIAL),
                s -> copy(s, s.candidates(), s.quotaRows().subList(1, s.quotaRows().size()), s.nightEligibility()),
                s -> { var rows = new ArrayList<>(s.quotaRows()); rows.add(rows.getFirst()); return copy(s, s.candidates(), rows, s.nightEligibility()); },
                s -> { var q = s.quotaRows().getFirst(); return copy(s, s.candidates(), List.of(new QuotaRow(2, 501, q.roomBlockId(), q.hotelRoomTypeId(), q.stayDate(), 1)), s.nightEligibility()); },
                s -> { var rows = s.quotaRows().stream().map(q -> new QuotaRow(1, 501, q.roomBlockId(), q.hotelRoomTypeId(), q.stayDate(), -1)).toList(); return copy(s, s.candidates(), rows, s.nightEligibility()); },
                s -> copy(s, s.candidates(), s.quotaRows(), s.nightEligibility().subList(1, s.nightEligibility().size())),
                s -> { var rows = new ArrayList<>(s.nightEligibility()); rows.add(rows.getFirst()); return copy(s, s.candidates(), s.quotaRows(), rows); },
                s -> { var rows = s.nightEligibility().stream().map(e -> new NightEligibility(e.roomInstanceId(), 1, 501, e.roomBlockId(), e.hotelRoomTypeId(), e.stayDate(), Eligibility.CUTOFF_EXPIRED)).toList(); return copy(s, s.candidates(), s.quotaRows(), rows); },
                s -> copy(s, List.of(s.candidates().getFirst(), s.candidates().getFirst()), s.quotaRows(), s.nightEligibility()),
                s -> { var room = s.candidates().getFirst(); return copy(s, List.of(new RoomAssignmentCandidate(room.roomInstanceId(), 2, 601, 101, 2, 1, 1, true)), s.quotaRows(), s.nightEligibility()); },
                s -> { var rooms = new ArrayList<RoomAssignmentCandidate>(); for (int i = 1; i <= 301; i++) rooms.add(room(i, 2, 1, 1)); return copy(s, rooms, s.quotaRows(), s.nightEligibility()); }
        );
        return faults.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("invalidSnapshots")
    void invalidEvidenceFailsClosedWithoutSuccessfulAssignments(UnaryOperator<Snapshot> fault) {
        when(data.read(any(), any(), any(), any())).thenReturn(fault.apply(source));
        assertFailure(service.preview(request), Status.TEMP_UNAVAILABLE);
    }

    @Test
    void completeEmptySnapshotIsKnownNoCapacityNotOutage() {
        when(data.read(any(), any(), any(), any())).thenReturn(snapshot(request, List.of(), 0, 0));
        var result = service.preview(request);
        assertThat(result.status()).isEqualTo(Status.COMPLETE);
        assertThat(result.assignments()).isEmpty(); assertThat(result.unmet()).hasSize(2);
    }

    @Test
    void thirtyOneNightBoundaryRequiresAllNightsButNotCheckout() {
        var request = request(1, 31);
        int[] quota = new int[31]; java.util.Arrays.fill(quota, 1);
        var source = snapshot(request, List.of(room(11, 2, 1, 1)), quota);
        var result = new DeterministicGroupRoomAssignmentEngine().preview(request, source, NOW);
        assertThat(result.status()).isEqualTo(Status.COMPLETE);
        assertThat(result.assignments()).hasSize(1);
        assertThat(source.quotaRows()).extracting(QuotaRow::stayDate).doesNotContain(request.checkOutDate());
    }

    @Test
    void checkoutRowsWrongRoomKeysAndNullQuotaAreInvalidEvidenceNotCapacityZero() {
        var quotas = new ArrayList<>(source.quotaRows());
        var quota = quotas.getFirst();
        quotas.add(new QuotaRow(1, 501, quota.roomBlockId(), quota.hotelRoomTypeId(), request.checkOutDate(), 2));
        var badCheckout = copy(source, source.candidates(), quotas, source.nightEligibility());
        var nullQuota = copy(source, source.candidates(), source.quotaRows().stream().map(q -> new QuotaRow(1, 501,
                q.roomBlockId(), q.hotelRoomTypeId(), q.stayDate(), null)).toList(), source.nightEligibility());
        var wrongRoom = copy(source, source.candidates(), source.quotaRows(), source.nightEligibility().stream().map(e ->
                new NightEligibility(999, 1, 501, e.roomBlockId(), e.hotelRoomTypeId(), e.stayDate(), e.state())).toList());
        for (var snapshot : List.of(badCheckout, nullQuota, wrongRoom)) {
            when(data.read(any(), any(), any(), any())).thenReturn(snapshot);
            assertFailure(service.preview(request), Status.TEMP_UNAVAILABLE);
        }
    }

    private static Snapshot replace(Snapshot s, Scope scope, Instant observed, Instant until, String basis, String mode, Coverage coverage) {
        return new Snapshot(scope, s.snapshotId(), observed, until, basis, mode, coverage, s.candidates(), s.quotaRows(), s.nightEligibility());
    }
    private static void assertFailure(RoomAssignmentPreview result, Status status) {
        assertThat(result.status()).isEqualTo(status);
        assertThat(result.previewOnly()).isTrue(); assertThat(result.assignments()).isEmpty();
        assertThat(result.unmet()).isEmpty(); assertThat(result.objective()).isNull(); assertThat(result.dataMode()).isNull();
    }
}
