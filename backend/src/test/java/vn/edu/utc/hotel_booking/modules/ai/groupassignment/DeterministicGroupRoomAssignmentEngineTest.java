package vn.edu.utc.hotel_booking.modules.ai.groupassignment;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.GroupRoomDemand;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentCandidate;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview.Status;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.*;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.DeterministicGroupRoomAssignmentEngine;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static vn.edu.utc.hotel_booking.modules.ai.groupassignment.GroupPreviewTestData.*;

class DeterministicGroupRoomAssignmentEngineTest {
    private final DeterministicGroupRoomAssignmentEngine engine = new DeterministicGroupRoomAssignmentEngine();

    @Test
    void cardinalityPrecedesWasteAndReassignmentAvoidsGreedyTrap() {
        var base = request(2, 1);
        var request = new GroupRoomAssignmentPreviewRequest(1, 501L, base.checkInDate(), base.checkOutDate(),
                List.of(new GroupRoomDemand("A", 1, 1), new GroupRoomDemand("B", 4, 2)));
        var result = engine.preview(request, snapshot(request, List.of(room(11, 4, 2, 1), room(12, 1, 3, 1)), 2), NOW);
        assertThat(result.status()).isEqualTo(Status.COMPLETE);
        assertThat(result.assignments()).extracting(a -> a.roomInstanceId()).containsExactly(12, 11);
        assertThat(result.objective().assignedCount()).isEqualTo(2);
        assertThat(result.objective().bedWaste()).isEqualTo(2);
    }

    @Test
    void middleNightQuotaLimitsWholeStayAndZeroIsKnownCapacity() {
        var request = request(2, 3);
        var rooms = List.of(room(11, 2, 1, 1), room(12, 2, 1, 1));
        for (int minimum : new int[] {0, 1, 2}) {
            var result = engine.preview(request, snapshot(request, rooms, 2, minimum, 2), NOW);
            assertThat(result.status()).isEqualTo(Status.COMPLETE);
            assertThat(result.assignments()).hasSize(minimum);
            assertThat(result.unmet()).hasSize(2 - minimum);
            if (minimum == 1) {
                assertThat(result.assignments().getFirst().demandKey()).isEqualTo("D000");
                assertThat(result.assignments().getFirst().roomInstanceId()).isEqualTo(11);
            }
        }
    }

    @Test
    void quotaCannotMoveBetweenBlocksOrRoomTypes() {
        var request = request(2, 1);
        for (boolean changeBlock : new boolean[] {true, false}) {
            var rooms = List.of(room(11, 2, 1, 1), new RoomAssignmentCandidate(12, 1, changeBlock ? 602 : 601,
                    changeBlock ? 101 : 102, 2, 1, 1, true));
            var source = snapshot(request, rooms, 2);
            var quotas = source.quotaRows().stream().map(row -> new QuotaRow(1, 501, row.roomBlockId(), row.hotelRoomTypeId(),
                    row.stayDate(), row.roomBlockId() == 601 && row.hotelRoomTypeId() == 101 ? 0 : 2)).toList();
            var result = engine.preview(request, copy(source, rooms, quotas, source.nightEligibility()), NOW);
            assertThat(result.assignments()).extracting(a -> a.roomInstanceId()).containsExactly(12);
        }
    }

    @Test
    void floorSpanIsAfterWasteButBeforeRoomIdTieBreak() {
        var request = request(2, 1);
        var sameBeds = List.of(room(11, 2, 1, 1), room(12, 2, 1, 10), room(13, 2, 1, 10));
        var result = engine.preview(request, snapshot(request, sameBeds, 3), NOW);
        assertThat(result.assignments()).extracting(a -> a.roomInstanceId()).containsExactly(12, 13);
        assertThat(result.objective().floorSpan()).isZero();
        var extraBed = List.of(room(11, 2, 1, 1), room(12, 2, 1, 10), room(13, 2, 2, 1));
        result = engine.preview(request, snapshot(request, extraBed, 3), NOW);
        assertThat(result.assignments()).extracting(a -> a.roomInstanceId()).containsExactly(11, 12);
        assertThat(result.objective().bedWaste()).isZero();
        assertThat(result.objective().floorSpan()).isEqualTo(9);
    }

    @Test
    void anyEligibleUnknownFloorDisablesWholeFloorObjectiveWithoutGuessing() {
        var request = request(2, 1);
        var rooms = List.of(room(11, 2, 1, 1), room(12, 2, 1, 10), room(13, 2, 1, null));
        var result = engine.preview(request, snapshot(request, rooms, 3), NOW);
        assertThat(result.assignments()).extracting(a -> a.roomInstanceId()).containsExactly(11, 12);
        assertThat(result.warnings()).containsExactly("UNKNOWN_FLOOR_METADATA");
        assertThat(result.objective().floorSpan()).isNull();
        assertThat(result.objective().floorObjectiveEnabled()).isFalse();
    }

    @Test
    void permutationsOfDemandsRoomsQuotaAndEligibilityDoNotAffectResult() {
        var request = request(3, 2);
        var source = snapshot(request, List.of(room(4, 2, 1, 4), room(2, 2, 1, 2), room(9, 3, 2, 2), room(8, 2, 1, 2)), 2, 2);
        var expected = engine.preview(request, source, NOW);
        Random random = new Random(501);
        for (int i = 0; i < 30; i++) {
            var demands = new ArrayList<>(request.demands()); Collections.shuffle(demands, random);
            var rooms = new ArrayList<>(source.candidates()); Collections.shuffle(rooms, random);
            var quotas = new ArrayList<>(source.quotaRows()); Collections.shuffle(quotas, random);
            var eligibility = new ArrayList<>(source.nightEligibility()); Collections.shuffle(eligibility, random);
            var shuffledRequest = new GroupRoomAssignmentPreviewRequest(1, 501L, request.checkInDate(), request.checkOutDate(), demands);
            assertThat(engine.preview(shuffledRequest, copy(source, rooms, quotas, eligibility), NOW)).isEqualTo(expected);
        }
    }

    @Test
    void inactiveCutoffAndUnavailableRoomsAreExcludedWithExplicitEvidence() {
        var request = request(1, 2);
        for (Eligibility state : List.of(Eligibility.BLOCK_INACTIVE, Eligibility.CUTOFF_EXPIRED, Eligibility.ROOM_UNAVAILABLE)) {
            var room = new RoomAssignmentCandidate(11, 1, 601, 101, 2, 1, 1, false);
            var source = snapshot(request, List.of(room), 1, 1);
            var evidence = source.nightEligibility().stream().map(row -> new NightEligibility(row.roomInstanceId(), 1, 501, 601, 101, row.stayDate(), state)).toList();
            var result = engine.preview(request, copy(source, source.candidates(), source.quotaRows(), evidence), NOW);
            assertThat(result.status()).isEqualTo(Status.COMPLETE);
            assertThat(result.assignments()).isEmpty();
            assertThat(result.unmet()).hasSize(1);
            assertThat(result.warnings()).contains("EXCLUDED_" + state.name());
        }
    }

    @Test
    void budgetExhaustionDiscardsAnyIncumbentInsteadOfClaimingAnOptimum() {
        var request = request(2, 1);
        var result = new DeterministicGroupRoomAssignmentEngine(1).preview(request,
                snapshot(request, List.of(room(11, 2, 1, 1), room(12, 2, 1, 2)), 2), NOW);
        assertThat(result.status()).isEqualTo(Status.TEMP_UNAVAILABLE);
        assertThat(result.reasonCode()).isEqualTo("SEARCH_BUDGET_EXCEEDED");
        assertThat(result.assignments()).isEmpty(); assertThat(result.unmet()).isEmpty(); assertThat(result.objective()).isNull();
    }

    @Test
    void floorSearchBudgetCannotLeakAnAlreadyAvailableUnconstrainedMatching() {
        var request = request(2, 1);
        var unknown = snapshot(request, List.of(room(11, 2, 1, 1), room(12, 2, 1, 10), room(13, 2, 1, null)), 3);
        long low = 1, high = 10000;
        while (low < high) {
            long middle = (low + high) / 2;
            var result = new DeterministicGroupRoomAssignmentEngine(middle).preview(request, unknown, NOW);
            if (result.status() == Status.COMPLETE) high = middle;
            else { assertThat(result.reasonCode()).isEqualTo("SEARCH_BUDGET_EXCEEDED"); low = middle + 1; }
        }
        assertThat(new DeterministicGroupRoomAssignmentEngine(low).preview(request, unknown, NOW).status()).isEqualTo(Status.COMPLETE);
        var known = snapshot(request, List.of(room(11, 2, 1, 1), room(12, 2, 1, 10), room(13, 2, 1, 10)), 3);
        var result = new DeterministicGroupRoomAssignmentEngine(low).preview(request, known, NOW);
        assertThat(result.reasonCode()).isEqualTo("SEARCH_BUDGET_EXCEEDED");
        assertThat(result.assignments()).isEmpty(); assertThat(result.unmet()).isEmpty(); assertThat(result.objective()).isNull();
    }

    @Test
    void maximumIdsBedWasteAndFloorSpanDoNotOverflow() {
        var request = request(2, 1);
        var rooms = List.of(room(1, 2, Integer.MAX_VALUE, Integer.MIN_VALUE), room(Integer.MAX_VALUE, 2, Integer.MAX_VALUE, Integer.MAX_VALUE));
        var result = engine.preview(request, snapshot(request, rooms, 2), NOW);
        assertThat(result.status()).isEqualTo(Status.COMPLETE);
        assertThat(result.assignments()).extracting(a -> a.roomInstanceId()).containsExactly(1, Integer.MAX_VALUE);
        assertThat(result.objective().bedWaste()).isEqualTo(2L * (Integer.MAX_VALUE - 1L));
        assertThat(result.objective().floorSpan()).isEqualTo(4294967295L);
    }

    @Test
    void representativeHundredDemandsThreeHundredRoomsCompletesWithinBound() {
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            var request = request(100, 31);
            var rooms = new ArrayList<RoomAssignmentCandidate>();
            for (int id = 1; id <= 300; id++) rooms.add(new RoomAssignmentCandidate(id, 1, 600 + id, 101, 2, 1, 1, true));
            int[] quota = new int[31]; Arrays.fill(quota, 100);
            var result = engine.preview(request, snapshot(request, rooms, quota), NOW);
            assertThat(result.status()).as("reason=%s", result.reasonCode()).isEqualTo(Status.COMPLETE);
            assertThat(result.assignments()).hasSize(100);
            for (int i = 0; i < 100; i++) assertThat(result.assignments().get(i).roomInstanceId()).isEqualTo(i + 1);
        });
    }

    @Test
    void independentExhaustiveOracleMatchesNinetySmallRandomInstances() {
        Random random = new Random(20261031);
        for (int scenario = 0; scenario < 90; scenario++) {
            var base = request(1 + random.nextInt(4), 2);
            var demands = base.demands().stream().map(d -> new GroupRoomDemand(d.demandKey(), 1 + random.nextInt(4), 1 + random.nextInt(3))).toList();
            var request = new GroupRoomAssignmentPreviewRequest(1, 501L, base.checkInDate(), base.checkOutDate(), demands);
            var rooms = new ArrayList<RoomAssignmentCandidate>();
            int roomCount = random.nextInt(6);
            for (int id = 1; id <= roomCount; id++) {
                rooms.add(new RoomAssignmentCandidate(id, 1, 601 + random.nextInt(2), 101, 1 + random.nextInt(4), 1 + random.nextInt(3),
                        random.nextInt(6) == 0 ? null : random.nextInt(4), random.nextBoolean()));
            }
            var source = snapshot(request, rooms, 3, 3);
            var quotas = source.quotaRows().stream().map(q -> new QuotaRow(1, 501, q.roomBlockId(), q.hotelRoomTypeId(), q.stayDate(), random.nextInt(4))).toList();
            source = copy(source, rooms, quotas, source.nightEligibility());
            var actual = engine.preview(request, source, NOW);
            assertThat(actual.status()).as("scenario %s", scenario).isEqualTo(Status.COMPLETE);
            Oracle oracle = new Oracle(request, source);
            oracle.enumerate(0, new RoomAssignmentCandidate[demands.size()], new HashSet<>());
            long[] vector = new long[demands.size()]; Arrays.fill(vector, Long.MAX_VALUE);
            for (var assignment : actual.assignments()) {
                int index = Integer.parseInt(assignment.demandKey().substring(1)); vector[index] = assignment.roomInstanceId();
            }
            assertThat(vector).as("oracle vector scenario %s", scenario).containsExactly(oracle.bestVector);
            assertThat(actual.objective().assignedCount()).isEqualTo(oracle.bestCount);
            assertThat(actual.objective().bedWaste()).isEqualTo(oracle.bestWaste);
            if (actual.objective().floorObjectiveEnabled()) assertThat(actual.objective().floorSpan()).isEqualTo(oracle.bestSpan);
        }
    }

    /** Independent brute-force feasible-vector enumeration; no engine validation/flow/cost helper is used. */
    private static final class Oracle {
        final GroupRoomAssignmentPreviewRequest request;
        final Snapshot source;
        final boolean floor;
        int bestCount = -1;
        long bestWaste, bestSpan;
        long[] bestVector;
        Oracle(GroupRoomAssignmentPreviewRequest request, Snapshot source) {
            this.request = request; this.source = source;
            floor = source.candidates().stream().filter(RoomAssignmentCandidate::eligibleForEntireStay).allMatch(r -> r.floorNumber() != null);
        }
        void enumerate(int index, RoomAssignmentCandidate[] assignment, Set<Integer> used) {
            if (index == assignment.length) { consider(assignment); return; }
            assignment[index] = null; enumerate(index + 1, assignment, used);
            var demand = request.demands().get(index);
            for (var room : source.candidates()) {
                if (!room.eligibleForEntireStay() || used.contains(room.roomInstanceId()) || room.capacityPersons() < demand.persons() || room.bedUnits() < demand.requiredBedUnits()) continue;
                assignment[index] = room; used.add(room.roomInstanceId()); enumerate(index + 1, assignment, used); used.remove(room.roomInstanceId());
            }
            assignment[index] = null;
        }
        void consider(RoomAssignmentCandidate[] assignment) {
            for (var quota : source.quotaRows()) {
                long consumed = Arrays.stream(assignment).filter(Objects::nonNull)
                        .filter(room -> room.roomBlockId() == quota.roomBlockId() && room.hotelRoomTypeId() == quota.hotelRoomTypeId()).count();
                if (consumed > quota.previewableRoomCount()) return;
            }
            int count = 0;
            long waste = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            long[] vector = new long[assignment.length]; Arrays.fill(vector, Long.MAX_VALUE);
            for (int i = 0; i < assignment.length; i++) if (assignment[i] != null) {
                count++; waste += (long) assignment[i].bedUnits() - request.demands().get(i).requiredBedUnits();
                vector[i] = assignment[i].roomInstanceId();
                if (floor) { min = Math.min(min, assignment[i].floorNumber()); max = Math.max(max, assignment[i].floorNumber()); }
            }
            long span = floor && count > 0 ? max - min : 0;
            boolean better = count > bestCount || count == bestCount && (waste < bestWaste || waste == bestWaste &&
                    (span < bestSpan || span == bestSpan && Arrays.compare(vector, bestVector) < 0));
            if (better) { bestCount = count; bestWaste = waste; bestSpan = span; bestVector = vector; }
        }
    }
}
