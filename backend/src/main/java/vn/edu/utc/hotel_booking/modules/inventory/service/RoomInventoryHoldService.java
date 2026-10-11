package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.modules.inventory.entity.HoldReleaseReason;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHold;

import java.time.OffsetDateTime;
import java.util.List;

public interface RoomInventoryHoldService {

    List<RoomInventoryHold> saveAll(List<RoomInventoryHold> holds);

    List<RoomInventoryHold> findActiveHoldsBySessionId(String sessionId);

    List<RoomInventoryHold> findExpiredHolds(OffsetDateTime expiresAt);

    void markHoldsReleased(List<RoomInventoryHold> holds, HoldReleaseReason reason);

    void markHoldsCommitted(List<RoomInventoryHold> holds);
}
