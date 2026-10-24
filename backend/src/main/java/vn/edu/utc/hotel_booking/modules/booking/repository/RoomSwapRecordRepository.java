package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.RoomSwapRecord;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomSwapRecordRepository extends JpaRepository<RoomSwapRecord, Long> {

    List<RoomSwapRecord> findByBookingRoomId(Long bookingRoomId);

    List<RoomSwapRecord> findByBookingRoomBookingDetailBookingId(Long bookingId);

    List<RoomSwapRecord> findBySwapDateBetween(LocalDate startDate, LocalDate endDate);
}
