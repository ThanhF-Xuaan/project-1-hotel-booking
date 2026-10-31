package vn.edu.utc.hotel_booking.modules.ai.groupassignment.service;

import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview.Status;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentAuthorizationPort;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentAuthorizationPort.Scope;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.Snapshot;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Internal preview only. No automatic bean/API, Core write dependency or production provider. */
public class GroupRoomAssignmentPreviewService {
    private final GroupAssignmentAuthorizationPort authorization;
    private final GroupAssignmentDataPort data;
    private final DeterministicGroupRoomAssignmentEngine engine;
    private final Clock clock;

    public GroupRoomAssignmentPreviewService(GroupAssignmentAuthorizationPort authorization, GroupAssignmentDataPort data,
                                             DeterministicGroupRoomAssignmentEngine engine, Clock clock) {
        this.authorization = Objects.requireNonNull(authorization); this.data = Objects.requireNonNull(data);
        this.engine = Objects.requireNonNull(engine); this.clock = Objects.requireNonNull(clock);
    }

    public RoomAssignmentPreview preview(GroupRoomAssignmentPreviewRequest request) {
        if (!GroupAssignmentValidation.validRequest(request)) return RoomAssignmentPreview.failure(Status.INVALID_REQUEST, request, "INVALID_REQUEST");
        try {
            Optional<Scope> initial = authorization.authorize(request.hotelId(), request.contractId());
            if (initial == null) return unavailable(request);
            if (initial.isEmpty()) return RoomAssignmentPreview.failure(Status.ACCESS_DENIED, request, "ACCESS_DENIED");
            if (!GroupAssignmentValidation.validScope(initial.get(), request)) return unavailable(request);
            Instant asOf = clock.instant();
            Snapshot snapshot = data.read(initial.get(), request.checkInDate(), request.checkOutDate(), asOf);
            if (snapshot == null || !initial.get().equals(snapshot.scope())) return unavailable(request);
            RoomAssignmentPreview result = engine.preview(request, snapshot, clock.instant());
            if (result.status() != Status.COMPLETE) return result;
            Optional<Scope> current = authorization.authorize(request.hotelId(), request.contractId());
            if (current == null) return unavailable(request);
            if (!initial.equals(current)) return RoomAssignmentPreview.failure(Status.ACCESS_DENIED, request, "ACCESS_DENIED");
            if (!GroupAssignmentValidation.fresh(snapshot, clock.instant())) return unavailable(request);
            return result;
        } catch (RuntimeException exception) {
            return unavailable(request); // Stable code; do not echo sensitive dependency exceptions.
        }
    }

    private RoomAssignmentPreview unavailable(GroupRoomAssignmentPreviewRequest request) {
        return RoomAssignmentPreview.failure(Status.TEMP_UNAVAILABLE, request, "DEPENDENCY_UNAVAILABLE");
    }
}
