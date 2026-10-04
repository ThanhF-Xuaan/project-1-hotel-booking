package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StayGuestRepository extends JpaRepository<StayGuest, Long> {

    List<StayGuest> findByBookingRoomId(Long bookingRoomId);

    Optional<StayGuest> findByHotelIdAndDocumentNumberAndIsDeletedFalse(Short hotelId, String documentNumber);

    @Query("SELECT s FROM StayGuest s WHERE s.isDeleted = false " +
           "AND (:hotelId IS NULL OR s.hotel.id = :hotelId) " +
           "AND (:bookingId IS NULL OR s.booking.id = :bookingId) " +
           "AND (:roomNumber IS NULL OR s.roomNumber = :roomNumber) " +
           "AND (:documentNumber IS NULL OR s.documentNumber = :documentNumber) " +
           "AND (:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR s.documentNumber LIKE CONCAT('%', :keyword, '%')) " +
           "AND (CAST(:fromDate AS java.time.OffsetDateTime) IS NULL OR s.checkInTime >= :fromDate) " +
           "AND (CAST(:toDate AS java.time.OffsetDateTime) IS NULL OR s.checkInTime <= :toDate)")
    Page<StayGuest> search(@Param("hotelId") Short hotelId,
                           @Param("bookingId") Long bookingId,
                           @Param("roomNumber") String roomNumber,
                           @Param("documentNumber") String documentNumber,
                           @Param("keyword") String keyword,
                           @Param("fromDate") OffsetDateTime fromDate,
                           @Param("toDate") OffsetDateTime toDate,
                           Pageable pageable);

    List<StayGuest> findAllByIdInAndIsDeletedFalse(List<Long> ids);
}
