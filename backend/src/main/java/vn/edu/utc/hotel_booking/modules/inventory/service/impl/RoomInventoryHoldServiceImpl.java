package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HoldReleaseReason;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHold;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHoldStatus;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInventoryHoldRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInventoryHoldService;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomInventoryHoldServiceImpl implements RoomInventoryHoldService {

    RoomInventoryHoldRepository roomInventoryHoldRepository;

    @Override
    @Transactional
    public List<RoomInventoryHold> saveAll(List<RoomInventoryHold> holds) {
        if (holds == null || holds.isEmpty()) {
            return Collections.emptyList();
        }
        return roomInventoryHoldRepository.saveAll(holds);
    }

    @Override
    public List<RoomInventoryHold> findActiveHoldsBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Collections.emptyList();
        }
        return roomInventoryHoldRepository.findBySessionIdAndStatusAndIsDeletedFalse(
                sessionId.trim(), RoomInventoryHoldStatus.HELD);
    }

    @Override
    public List<RoomInventoryHold> findExpiredHolds(OffsetDateTime expiresAt) {
        OffsetDateTime cutoff = expiresAt != null ? expiresAt : OffsetDateTime.now();
        return roomInventoryHoldRepository.findByStatusAndExpiresAtBeforeAndIsDeletedFalse(
                RoomInventoryHoldStatus.HELD, cutoff);
    }

    @Override
    @Transactional
    public void markHoldsReleased(List<RoomInventoryHold> holds, HoldReleaseReason reason) {
        if (holds == null || holds.isEmpty()) {
            return;
        }
        HoldReleaseReason actualReason = reason != null ? reason : HoldReleaseReason.USER_CANCELLED;
        for (RoomInventoryHold hold : holds) {
            hold.setStatus(RoomInventoryHoldStatus.RELEASED);
            hold.setReleaseReason(actualReason);
        }
        roomInventoryHoldRepository.saveAll(holds);
    }

    @Override
    @Transactional
    public void markHoldsCommitted(List<RoomInventoryHold> holds) {
        if (holds == null || holds.isEmpty()) {
            return;
        }
        for (RoomInventoryHold hold : holds) {
            hold.setStatus(RoomInventoryHoldStatus.COMMITTED);
        }
        roomInventoryHoldRepository.saveAll(holds);
    }
}
