package vn.edu.utc.hotel_booking.modules.inventory.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomAvailability;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, Long> {

    Optional<RoomAvailability> findByHotelRoomTypeIdAndDate(Integer hotelRoomTypeId, LocalDate date);

    List<RoomAvailability> findByHotelRoomTypeIdAndDateBetweenOrderByDateAsc(
            Integer hotelRoomTypeId, LocalDate startDate, LocalDate endDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ra FROM RoomAvailability ra WHERE ra.hotelRoomType.id = :hotelRoomTypeId AND ra.date = :date")
    Optional<RoomAvailability> findByHotelRoomTypeIdAndDateWithLock(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("date") LocalDate date
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ra FROM RoomAvailability ra WHERE ra.hotelRoomType.id = :hotelRoomTypeId AND ra.date >= :startDate AND ra.date <= :endDate ORDER BY ra.date ASC")
    List<RoomAvailability> findByHotelRoomTypeIdAndDateBetweenWithLock(
            @Param("hotelRoomTypeId") Integer hotelRoomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
