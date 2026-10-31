package vn.edu.utc.hotel_booking.modules.ai.groupassignment.model;

import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RoomAssignmentPreview(Status status, String reasonCode, String dataMode, Integer hotelId, Long contractId,
                                    LocalDate checkInDate, LocalDate checkOutDate, Instant observedAt,
                                    List<Assignment> assignments, List<Unmet> unmet, List<String> warnings,
                                    Objective objective, boolean previewOnly) {
    public enum Status { COMPLETE, INVALID_REQUEST, ACCESS_DENIED, TEMP_UNAVAILABLE }
    public record Assignment(String demandKey, int roomInstanceId) { }
    public record Unmet(String demandKey, String reason) { }
    public record Objective(int assignedCount, long bedWaste, Long floorSpan, boolean floorObjectiveEnabled) { }

    public RoomAssignmentPreview {
        assignments = List.copyOf(assignments); unmet = List.copyOf(unmet); warnings = List.copyOf(warnings);
        if (!previewOnly || (status != Status.COMPLETE && (!assignments.isEmpty() || !unmet.isEmpty() || objective != null))) {
            throw new IllegalArgumentException("Invalid preview-only result");
        }
    }

    public static RoomAssignmentPreview failure(Status status, GroupRoomAssignmentPreviewRequest request, String reason) {
        return new RoomAssignmentPreview(status, reason, null, request == null ? null : request.hotelId(),
                request == null ? null : request.contractId(), request == null ? null : request.checkInDate(),
                request == null ? null : request.checkOutDate(), null, List.of(), List.of(), List.of(), null, true);
    }
}
