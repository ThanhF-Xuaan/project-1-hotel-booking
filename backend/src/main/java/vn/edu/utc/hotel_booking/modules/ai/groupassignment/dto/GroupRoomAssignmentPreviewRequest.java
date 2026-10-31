package vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto;

import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.GroupRoomDemand;

import java.time.LocalDate;
import java.util.List;

/** Internal selector/demand contract; no client-supplied rooms, quota, scope grants or write action. */
public record GroupRoomAssignmentPreviewRequest(Integer hotelId, Long contractId, LocalDate checkInDate,
                                                LocalDate checkOutDate, List<GroupRoomDemand> demands) {
    public GroupRoomAssignmentPreviewRequest { if (demands != null) demands = List.copyOf(demands); }
}
