package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {

    List<BookingRoom> findByBookingDetailId(Long bookingDetailId);

    @Query("SELECT br FROM BookingRoom br JOIN br.bookingDetail bd WHERE bd.booking.id = :bookingId")
    List<BookingRoom> findByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT br FROM BookingRoom br WHERE br.roomInstance.id = :roomInstanceId AND br.status IN :statuses")
    List<BookingRoom> findByRoomInstanceIdAndStatusIn(
            @Param("roomInstanceId") Integer roomInstanceId,
            @Param("statuses") Collection<BookingRoomStatus> statuses
    );

    @Query("SELECT br FROM BookingRoom br " +
            "JOIN br.bookingDetail bd " +
            "WHERE br.roomInstance.id = :roomInstanceId " +
            "AND br.status NOT IN ('CANCELLED', 'CHECKED_OUT') " +
            "AND bd.checkInDate < :checkOutDate AND bd.checkOutDate > :checkInDate")
    List<BookingRoom> findOverlappingAssignedRooms(
            @Param("roomInstanceId") Integer roomInstanceId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    @Query("SELECT br FROM BookingRoom br JOIN br.bookingDetail bd WHERE bd.booking.id = :bookingId AND br.roomInstance.id = :roomInstanceId")
    java.util.Optional<BookingRoom> findByBookingIdAndRoomInstanceId(
            @Param("bookingId") Long bookingId,
            @Param("roomInstanceId") Integer roomInstanceId
    );

    @Query("SELECT br FROM BookingRoom br " +
            "JOIN FETCH br.bookingDetail bd " +
            "JOIN FETCH bd.booking b " +
            "LEFT JOIN FETCH br.stayGuests sg " +
            "LEFT JOIN FETCH b.bookingGuest bg " +
            "WHERE br.roomInstance.id IN :roomInstanceIds " +
            "AND br.status IN :statuses")
    List<BookingRoom> findActiveByRoomInstanceIdInWithDetails(
            @Param("roomInstanceIds") Collection<Integer> roomInstanceIds,
            @Param("statuses") Collection<BookingRoomStatus> statuses
    );
}
