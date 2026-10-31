package vn.edu.utc.hotel_booking.modules.ai.groupassignment.port;

import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentCandidate;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentAuthorizationPort.Scope;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Bounded read-only snapshot; no production provider or synthetic fallback is registered. */
public interface GroupAssignmentDataPort {
    Snapshot read(Scope scope, LocalDate checkInDate, LocalDate checkOutDate, Instant asOf);

    enum Coverage { COMPLETE, PARTIAL, UNKNOWN }
    enum Eligibility { ELIGIBLE, ROOM_UNAVAILABLE, BLOCK_INACTIVE, CUTOFF_EXPIRED }
    record QuotaRow(int hotelId, long contractId, long roomBlockId, int hotelRoomTypeId,
                    LocalDate stayDate, Integer previewableRoomCount) { }
    record NightEligibility(int roomInstanceId, int hotelId, long contractId, long roomBlockId,
                            int hotelRoomTypeId, LocalDate stayDate, Eligibility state) { }
    record Snapshot(Scope scope, String snapshotId, Instant observedAt, Instant validUntil,
                    String basis, String dataMode, Coverage coverage, List<RoomAssignmentCandidate> candidates,
                    List<QuotaRow> quotaRows, List<NightEligibility> nightEligibility) {
        public Snapshot {
            if (candidates != null) candidates = List.copyOf(candidates);
            if (quotaRows != null) quotaRows = List.copyOf(quotaRows);
            if (nightEligibility != null) nightEligibility = List.copyOf(nightEligibility);
        }
    }
}
