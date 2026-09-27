package vn.edu.utc.hotel_booking.modules.inventory.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomSlotRepository extends JpaRepository<RoomSlot, Long> {

    Optional<RoomSlot> findByRoomInstanceIdAndSlotDate(Integer roomInstanceId, LocalDate slotDate);

    List<RoomSlot> findByRoomInstanceIdAndSlotDateBetweenOrderBySlotDateAsc(
            Integer roomInstanceId, LocalDate fromDate, LocalDate toDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rs FROM RoomSlot rs WHERE rs.roomInstance.id = :roomInstanceId AND rs.slotDate = :slotDate")
    Optional<RoomSlot> findByRoomInstanceIdAndSlotDateWithLock(
            @Param("roomInstanceId") Integer roomInstanceId,
            @Param("slotDate") LocalDate slotDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rs FROM RoomSlot rs WHERE rs.roomInstance.id = :roomInstanceId AND rs.slotDate >= :fromDate AND rs.slotDate <= :toDate")
    List<RoomSlot> findByRoomInstanceIdAndSlotDateBetweenWithLock(
            @Param("roomInstanceId") Integer roomInstanceId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
