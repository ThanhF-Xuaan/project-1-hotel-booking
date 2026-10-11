package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHold;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInventoryHoldStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface RoomInventoryHoldRepository extends JpaRepository<RoomInventoryHold, Long> {

    List<RoomInventoryHold> findBySessionIdAndStatusAndIsDeletedFalse(String sessionId, RoomInventoryHoldStatus status);

    List<RoomInventoryHold> findBySessionIdAndIsDeletedFalse(String sessionId);

    List<RoomInventoryHold> findByStatusAndExpiresAtBeforeAndIsDeletedFalse(RoomInventoryHoldStatus status, OffsetDateTime expiresAt);

    boolean existsBySessionIdAndStatusAndIsDeletedFalse(String sessionId, RoomInventoryHoldStatus status);
}
