package vn.edu.utc.hotel_booking.modules.ai.groupassignment.service;

import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.GroupRoomDemand;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentCandidate;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentAuthorizationPort.Scope;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

final class GroupAssignmentValidation {
    private GroupAssignmentValidation() { }
    record Block(long id, int type) { }
    record QuotaKey(Block block, LocalDate date) { }
    record NightKey(int room, LocalDate date) { }
    record Validated(List<GroupRoomDemand> demands, List<RoomAssignmentCandidate> eligible,
                     Map<Block, Integer> limits, List<String> warnings, boolean floorEnabled) { }

    static boolean validRequest(GroupRoomAssignmentPreviewRequest request) {
        if (request == null || request.hotelId() == null || request.hotelId() < 1 || request.hotelId() > Short.MAX_VALUE
                || request.contractId() == null || request.contractId() < 1 || request.checkInDate() == null
                || request.checkOutDate() == null || request.demands() == null
                || request.demands().isEmpty() || request.demands().size() > 100) return false;
        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        if (nights < 1 || nights > 31) return false;
        Set<String> keys = new HashSet<>();
        for (GroupRoomDemand demand : request.demands()) {
            if (demand == null || demand.demandKey() == null || !demand.demandKey().matches("[A-Za-z0-9_-]{1,64}")
                    || demand.persons() < 1 || demand.requiredBedUnits() < 1 || !keys.add(demand.demandKey())) return false;
        }
        return true;
    }

    static boolean validScope(Scope scope, GroupRoomAssignmentPreviewRequest request) {
        return scope != null && scope.hotelId() == request.hotelId() && scope.contractId() == request.contractId()
                && scope.regionId() > 0 && scope.regionId() <= Short.MAX_VALUE;
    }

    static boolean fresh(Snapshot snapshot, Instant now) {
        return snapshot != null && now != null && snapshot.observedAt() != null && snapshot.validUntil() != null
                && !snapshot.observedAt().isAfter(now) && Duration.between(snapshot.observedAt(), now).compareTo(Duration.ofSeconds(60)) <= 0
                && snapshot.validUntil().isAfter(now);
    }

    static Validated snapshot(GroupRoomAssignmentPreviewRequest request, Snapshot snapshot, Instant now) {
        if (!fresh(snapshot, now) || !validScope(snapshot.scope(), request) || snapshot.snapshotId() == null
                || !snapshot.snapshotId().matches("[A-Za-z0-9_.:-]{1,128}")
                || !"SYNTHETIC".equals(snapshot.dataMode()) || !"SYNTHETIC_PREVIEWABLE_ROOMS_V1".equals(snapshot.basis())
                || snapshot.coverage() != Coverage.COMPLETE || snapshot.candidates() == null || snapshot.candidates().size() > 300
                || snapshot.quotaRows() == null || snapshot.quotaRows().size() > 9300
                || snapshot.nightEligibility() == null || snapshot.nightEligibility().size() > 9300) throw invalid();
        Map<Integer, RoomAssignmentCandidate> rooms = new HashMap<>();
        Set<Block> blocks = new HashSet<>();
        for (RoomAssignmentCandidate room : snapshot.candidates()) {
            if (room == null || room.roomInstanceId() < 1 || room.hotelId() != request.hotelId() || room.roomBlockId() < 1
                    || room.hotelRoomTypeId() < 1 || room.capacityPersons() < 1 || room.bedUnits() < 1
                    || rooms.putIfAbsent(room.roomInstanceId(), room) != null) throw invalid();
            blocks.add(new Block(room.roomBlockId(), room.hotelRoomTypeId()));
        }
        Set<LocalDate> nights = new HashSet<>();
        for (LocalDate date = request.checkInDate(); date.isBefore(request.checkOutDate()); date = date.plusDays(1)) nights.add(date);
        Map<QuotaKey, Integer> quota = new HashMap<>();
        for (QuotaRow row : snapshot.quotaRows()) {
            Block block = new Block(row.roomBlockId(), row.hotelRoomTypeId());
            if (row.hotelId() != request.hotelId() || row.contractId() != request.contractId() || !blocks.contains(block)
                    || !nights.contains(row.stayDate()) || row.previewableRoomCount() == null || row.previewableRoomCount() < 0
                    || quota.putIfAbsent(new QuotaKey(block, row.stayDate()), row.previewableRoomCount()) != null) throw invalid();
        }
        if (quota.size() != blocks.size() * nights.size()) throw invalid();
        Map<NightKey, Eligibility> eligibility = new HashMap<>();
        Set<String> warnings = new TreeSet<>();
        for (NightEligibility row : snapshot.nightEligibility()) {
            RoomAssignmentCandidate room = rooms.get(row.roomInstanceId());
            if (room == null || row.hotelId() != request.hotelId() || row.contractId() != request.contractId()
                    || row.roomBlockId() != room.roomBlockId() || row.hotelRoomTypeId() != room.hotelRoomTypeId()
                    || !nights.contains(row.stayDate()) || row.state() == null
                    || eligibility.putIfAbsent(new NightKey(row.roomInstanceId(), row.stayDate()), row.state()) != null) throw invalid();
            if (row.state() != Eligibility.ELIGIBLE) warnings.add("EXCLUDED_" + row.state().name());
        }
        if (eligibility.size() != rooms.size() * nights.size()) throw invalid();
        List<RoomAssignmentCandidate> eligible = new ArrayList<>();
        for (RoomAssignmentCandidate room : rooms.values()) {
            boolean entireStay = nights.stream().allMatch(date -> eligibility.get(new NightKey(room.roomInstanceId(), date)) == Eligibility.ELIGIBLE);
            if (room.eligibleForEntireStay() != entireStay) throw invalid();
            if (entireStay) eligible.add(room);
        }
        eligible.sort(Comparator.comparingInt(RoomAssignmentCandidate::roomInstanceId));
        boolean floor = eligible.stream().allMatch(room -> room.floorNumber() != null);
        if (!floor) warnings.add("UNKNOWN_FLOOR_METADATA");
        Map<Block, Integer> limits = new HashMap<>();
        for (Block block : blocks) limits.put(block, nights.stream().mapToInt(date -> quota.get(new QuotaKey(block, date))).min().orElseThrow());
        return new Validated(request.demands().stream().sorted(Comparator.comparing(GroupRoomDemand::demandKey)).toList(),
                List.copyOf(eligible), Map.copyOf(limits), List.copyOf(warnings), floor);
    }

    private static IllegalArgumentException invalid() { return new IllegalArgumentException("INCOMPLETE_OR_INVALID_SNAPSHOT"); }
}
