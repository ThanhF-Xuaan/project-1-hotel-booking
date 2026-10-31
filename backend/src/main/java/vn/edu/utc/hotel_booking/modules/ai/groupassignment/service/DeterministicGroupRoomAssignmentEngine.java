package vn.edu.utc.hotel_booking.modules.ai.groupassignment.service;

import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.GroupRoomDemand;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentCandidate;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview.*;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.Snapshot;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.GroupAssignmentValidation.Block;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.GroupAssignmentValidation.Validated;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

/** Exact quota-constrained matching with deterministic bounded search; never writes business state. */
public class DeterministicGroupRoomAssignmentEngine {
    public static final long MAX_WORK = 30_000_000;
    private final long workLimit;

    public DeterministicGroupRoomAssignmentEngine() { this(MAX_WORK); }
    public DeterministicGroupRoomAssignmentEngine(long workLimit) {
        if (workLimit < 1 || workLimit > MAX_WORK) throw new IllegalArgumentException("Invalid preview search limit");
        this.workLimit = workLimit;
    }

    public RoomAssignmentPreview preview(GroupRoomAssignmentPreviewRequest request, Snapshot snapshot, Instant asOf) {
        if (!GroupAssignmentValidation.validRequest(request)) return RoomAssignmentPreview.failure(Status.INVALID_REQUEST, request, "INVALID_REQUEST");
        Validated input;
        try { input = GroupAssignmentValidation.snapshot(request, snapshot, asOf); }
        catch (IllegalArgumentException exception) { return RoomAssignmentPreview.failure(Status.TEMP_UNAVAILABLE, request, "INCOMPLETE_OR_INVALID_SNAPSHOT"); }
        try {
            Budget budget = new Budget(workLimit);
            Solution best = solve(input, input.eligible(), budget);
            if (input.floorEnabled() && best.count() > 1 && best.span() > 0) {
                List<Integer> floors = input.eligible().stream().map(RoomAssignmentCandidate::floorNumber).distinct().sorted().toList();
                int right = 0;
                for (int left = 0; left < floors.size(); left++) {
                    right = Math.max(left, right);
                    while (right < floors.size()) {
                        int low = floors.get(left), high = floors.get(right);
                        List<RoomAssignmentCandidate> subset = input.eligible().stream()
                                .filter(room -> room.floorNumber() >= low && room.floorNumber() <= high).toList();
                        Solution candidate = solve(input, subset, budget);
                        if (candidate.count() == best.count() && candidate.waste() == best.waste()) {
                            if (better(candidate, best)) best = candidate;
                            break;
                        }
                        right++;
                    }
                    if (right == floors.size()) break;
                }
            }
            List<Assignment> assignments = new ArrayList<>();
            List<Unmet> unmet = new ArrayList<>();
            for (int i = 0; i < input.demands().size(); i++) {
                String key = input.demands().get(i).demandKey();
                if (best.rooms()[i] == null) unmet.add(new Unmet(key, "NO_ELIGIBLE_CAPACITY"));
                else assignments.add(new Assignment(key, best.rooms()[i].roomInstanceId()));
            }
            return new RoomAssignmentPreview(Status.COMPLETE, null, snapshot.dataMode(), request.hotelId(), request.contractId(),
                    request.checkInDate(), request.checkOutDate(), snapshot.observedAt(), assignments, unmet, input.warnings(),
                    new Objective(best.count(), best.waste(), input.floorEnabled() ? best.span() : null, input.floorEnabled()), true);
        } catch (SearchLimit exception) {
            return RoomAssignmentPreview.failure(Status.TEMP_UNAVAILABLE, request, "SEARCH_BUDGET_EXCEEDED");
        }
    }

    private boolean better(Solution candidate, Solution current) {
        if (candidate.span() != current.span()) return candidate.span() < current.span();
        for (int i = 0; i < candidate.rooms().length; i++) {
            long a = candidate.rooms()[i] == null ? Long.MAX_VALUE : candidate.rooms()[i].roomInstanceId();
            long b = current.rooms()[i] == null ? Long.MAX_VALUE : current.rooms()[i].roomInstanceId();
            if (a != b) return a < b;
        }
        return false;
    }

    private Solution solve(Validated input, List<RoomAssignmentCandidate> rooms, Budget budget) {
        int demands = input.demands().size();
        if (rooms.isEmpty()) return new Solution(new RoomAssignmentCandidate[demands], 0, 0, 0);
        List<Block> blocks = rooms.stream().map(room -> new Block(room.roomBlockId(), room.hotelRoomTypeId())).distinct()
                .sorted(Comparator.comparingLong(Block::id).thenComparingInt(Block::type)).toList();
        Map<Block, Integer> blockIndex = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) blockIndex.put(blocks.get(i), i);
        int roomStart = 1 + demands, blockStart = roomStart + rooms.size(), sink = blockStart + blocks.size();
        List<List<Edge>> graph = new ArrayList<>();
        for (int i = 0; i <= sink; i++) graph.add(new ArrayList<>());
        long sentinel = (long) input.eligible().stream().mapToInt(RoomAssignmentCandidate::roomInstanceId).max().orElse(0) + 1;
        BigInteger base = BigInteger.valueOf(sentinel + 1);
        BigInteger wasteWeight = base.pow(demands);
        for (int i = 0; i < demands; i++) {
            add(graph, 0, i + 1, 1, BigInteger.ZERO);
            GroupRoomDemand demand = input.demands().get(i);
            BigInteger position = base.pow(demands - 1 - i);
            for (int j = 0; j < rooms.size(); j++) {
                budget.examine();
                RoomAssignmentCandidate room = rooms.get(j);
                if (room.capacityPersons() < demand.persons() || room.bedUnits() < demand.requiredBedUnits()) continue;
                BigInteger cost = wasteWeight.multiply(BigInteger.valueOf((long) room.bedUnits() - demand.requiredBedUnits()))
                        .add(position.multiply(BigInteger.valueOf((long) room.roomInstanceId() - sentinel)));
                add(graph, i + 1, roomStart + j, 1, cost);
            }
        }
        for (int j = 0; j < rooms.size(); j++) {
            var room = rooms.get(j);
            add(graph, roomStart + j, blockStart + blockIndex.get(new Block(room.roomBlockId(), room.hotelRoomTypeId())), 1, BigInteger.ZERO);
        }
        for (int i = 0; i < blocks.size(); i++) add(graph, blockStart + i, sink, input.limits().get(blocks.get(i)), BigInteger.ZERO);
        BigInteger[] potentials = initialPotentials(graph, budget);
        int count = 0;
        while (count < demands && augment(graph, sink, potentials, budget)) count++;
        RoomAssignmentCandidate[] assigned = new RoomAssignmentCandidate[demands];
        long waste = 0, minimumFloor = Long.MAX_VALUE, maximumFloor = Long.MIN_VALUE;
        for (int i = 0; i < demands; i++) {
            for (Edge edge : graph.get(i + 1)) {
                if (edge.to >= roomStart && edge.to < blockStart && edge.capacity == 0) {
                    assigned[i] = rooms.get(edge.to - roomStart);
                    waste += (long) assigned[i].bedUnits() - input.demands().get(i).requiredBedUnits();
                    if (assigned[i].floorNumber() != null) {
                        minimumFloor = Math.min(minimumFloor, assigned[i].floorNumber()); maximumFloor = Math.max(maximumFloor, assigned[i].floorNumber());
                    }
                }
            }
        }
        return new Solution(assigned, count, waste, minimumFloor == Long.MAX_VALUE ? 0 : maximumFloor - minimumFloor);
    }

    /** Initial residual graph is a DAG in node-index order; one pass handles negative lex costs. */
    private BigInteger[] initialPotentials(List<List<Edge>> graph, Budget budget) {
        BigInteger[] potentials = new BigInteger[graph.size()];
        potentials[0] = BigInteger.ZERO;
        for (int node = 0; node < graph.size(); node++) {
            if (potentials[node] == null) continue;
            for (Edge edge : graph.get(node)) {
                budget.examine();
                if (edge.capacity == 0) continue;
                BigInteger next = potentials[node].add(edge.cost);
                if (potentials[edge.to] == null || next.compareTo(potentials[edge.to]) < 0) potentials[edge.to] = next;
            }
        }
        for (int i = 0; i < potentials.length; i++) if (potentials[i] == null) potentials[i] = BigInteger.ZERO;
        return potentials;
    }

    private boolean augment(List<List<Edge>> graph, int sink, BigInteger[] potentials, Budget budget) {
        BigInteger[] distance = new BigInteger[graph.size()];
        int[] previousNode = new int[graph.size()], previousEdge = new int[graph.size()];
        PriorityQueue<Visit> queue = new PriorityQueue<>(Comparator.comparing(Visit::distance).thenComparingInt(Visit::node));
        distance[0] = BigInteger.ZERO; queue.add(new Visit(0, BigInteger.ZERO));
        while (!queue.isEmpty()) {
            Visit visit = queue.remove();
            int node = visit.node();
            if (!visit.distance().equals(distance[node])) continue;
            for (int i = 0; i < graph.get(node).size(); i++) {
                budget.examine();
                Edge edge = graph.get(node).get(i);
                if (edge.capacity == 0) continue;
                BigInteger reduced = edge.cost.add(potentials[node]).subtract(potentials[edge.to]);
                if (reduced.signum() < 0) throw new IllegalStateException("Invalid preview residual potential");
                BigInteger next = distance[node].add(reduced);
                if (distance[edge.to] == null || next.compareTo(distance[edge.to]) < 0) {
                    distance[edge.to] = next; previousNode[edge.to] = node; previousEdge[edge.to] = i;
                    queue.add(new Visit(edge.to, next));
                }
            }
        }
        if (distance[sink] == null) return false;
        for (int i = 0; i < potentials.length; i++) if (distance[i] != null) potentials[i] = potentials[i].add(distance[i]);
        for (int node = sink; node != 0; node = previousNode[node]) {
            Edge edge = graph.get(previousNode[node]).get(previousEdge[node]);
            edge.capacity--; graph.get(node).get(edge.reverse).capacity++;
        }
        return true;
    }

    private void add(List<List<Edge>> graph, int from, int to, int capacity, BigInteger cost) {
        Edge forward = new Edge(to, graph.get(to).size(), capacity, cost);
        Edge reverse = new Edge(from, graph.get(from).size(), 0, cost.negate());
        graph.get(from).add(forward); graph.get(to).add(reverse);
    }

    private record Solution(RoomAssignmentCandidate[] rooms, int count, long waste, long span) { }
    private record Visit(int node, BigInteger distance) { }
    private static final class Edge {
        final int to, reverse;
        final BigInteger cost;
        int capacity;
        Edge(int to, int reverse, int capacity, BigInteger cost) { this.to = to; this.reverse = reverse; this.capacity = capacity; this.cost = cost; }
    }
    private static final class Budget {
        long remaining;
        Budget(long remaining) { this.remaining = remaining; }
        void examine() { if (--remaining < 0) throw new SearchLimit(); }
    }
    private static final class SearchLimit extends RuntimeException { }
}
