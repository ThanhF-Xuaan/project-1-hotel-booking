package vn.edu.utc.hotel_booking.modules.ai.groupassignment;

import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.GroupRoomDemand;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentCandidate;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentAuthorizationPort.Scope;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class GroupPreviewTestData {
    static final Instant NOW = Instant.parse("2026-10-31T02:00:00Z");
    static final Scope SCOPE = new Scope(1, 10, 501);
    static GroupRoomAssignmentPreviewRequest request(int count, int nights) {
        List<GroupRoomDemand> demands = new ArrayList<>();
        for (int i = 0; i < count; i++) demands.add(new GroupRoomDemand(String.format("D%03d", i), 2, 1));
        LocalDate start = LocalDate.of(2026, 11, 1);
        return new GroupRoomAssignmentPreviewRequest(1, 501L, start, start.plusDays(nights), demands);
    }
    static RoomAssignmentCandidate room(int id, int persons, int beds, Integer floor) {
        return new RoomAssignmentCandidate(id, 1, 601, 101, persons, beds, floor, true);
    }
    static Snapshot snapshot(GroupRoomAssignmentPreviewRequest request, List<RoomAssignmentCandidate> rooms, int... quota) {
        List<QuotaRow> quotas = new ArrayList<>();
        List<NightEligibility> evidence = new ArrayList<>();
        var groups = rooms.stream().map(room -> new Key(room.roomBlockId(), room.hotelRoomTypeId())).distinct().toList();
        for (int i = 0; i < quota.length; i++) {
            LocalDate date = request.checkInDate().plusDays(i);
            for (Key key : groups) quotas.add(new QuotaRow(1, 501, key.block, key.type, date, quota[i]));
            for (var room : rooms) evidence.add(new NightEligibility(room.roomInstanceId(), room.hotelId(), 501,
                    room.roomBlockId(), room.hotelRoomTypeId(), date, room.eligibleForEntireStay() ? Eligibility.ELIGIBLE : Eligibility.ROOM_UNAVAILABLE));
        }
        return new Snapshot(SCOPE, "synthetic-preview-v1", NOW.minusSeconds(5), NOW.plusSeconds(55),
                "SYNTHETIC_PREVIEWABLE_ROOMS_V1", "SYNTHETIC", Coverage.COMPLETE, rooms, quotas, evidence);
    }
    static Snapshot copy(Snapshot source, List<RoomAssignmentCandidate> rooms, List<QuotaRow> quota, List<NightEligibility> evidence) {
        return new Snapshot(source.scope(), source.snapshotId(), source.observedAt(), source.validUntil(), source.basis(),
                source.dataMode(), source.coverage(), rooms, quota, evidence);
    }
    private record Key(long block, int type) { }
}
