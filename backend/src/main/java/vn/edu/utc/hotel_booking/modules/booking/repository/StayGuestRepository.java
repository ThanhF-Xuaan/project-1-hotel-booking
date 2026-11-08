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

    @org.springframework.data.jpa.repository.QueryHints(@jakarta.persistence.QueryHint(name="org.hibernate.flushMode",value="COMMIT"))
    @Query(value = "SELECT public.m1_active_stay_key(CAST(:hotel AS smallint),CAST(:booking AS bigint),CAST(:bookingRoom AS bigint),CAST(:room AS integer),"
            + "CAST(:roomNumber AS text),CAST(:document AS text),CAST(:checkIn AS timestamptz),CAST(:checkout AS timestamptz),CAST(:deleted AS boolean))", nativeQuery = true)
    byte[] computeActiveKey(@Param("hotel") Short hotel, @Param("booking") Long booking,
            @Param("bookingRoom") Long bookingRoom, @Param("room") Integer room,
            @Param("roomNumber") String roomNumber, @Param("document") String document,
            @Param("checkIn") OffsetDateTime checkIn, @Param("checkout") OffsetDateTime checkout, @Param("deleted") Boolean deleted);

    @org.springframework.data.jpa.repository.QueryHints(@jakarta.persistence.QueryHint(name="org.hibernate.flushMode",value="COMMIT"))
    @Query(value = "SELECT set_config('lock_timeout','5s',true)", nativeQuery = true)
    String configureDedupLockTimeout();

    @org.springframework.data.jpa.repository.QueryHints(@jakarta.persistence.QueryHint(name="org.hibernate.flushMode",value="COMMIT"))
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(encode(CAST(:key AS bytea),'hex'),0))", nativeQuery = true)
    Integer lockActiveKey(@Param("key") byte[] key);

    @Query(value = "SELECT * FROM public.stay_guests WHERE active_dedup_key=:key AND check_in_time IS NOT NULL "
            + "AND actual_check_out_time IS NULL AND NOT is_deleted", nativeQuery = true)
    Optional<StayGuest> findActiveByDedupKey(@Param("key") byte[] key);

    List<StayGuest> findByBookingRoomId(Long bookingRoomId);

    List<StayGuest> findByBookingRoomIdAndHotelIdAndIsDeletedFalse(Long bookingRoomId, Short hotelId);

    Optional<StayGuest> findByHotelIdAndDocumentNumberAndIsDeletedFalse(Short hotelId, String documentNumber);

    Optional<StayGuest> findFirstByHotelIdAndBookingIdAndDocumentNumberAndIsDeletedFalse(Short hotelId, Long bookingId, String documentNumber);

    Optional<StayGuest> findFirstByHotelIdAndBookingIdAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(Short hotelId, Long bookingId, String documentNumber);

    Optional<StayGuest> findFirstByHotelIdAndRoomNumberAndDocumentNumberAndActualCheckOutTimeIsNullAndIsDeletedFalse(Short hotelId, String roomNumber, String documentNumber);

    @Query("SELECT s FROM StayGuest s WHERE s.isDeleted = false " +
           "AND (:hotelId IS NULL OR s.hotel.id = :hotelId) " +
           "AND (:bookingId IS NULL OR s.booking.id = :bookingId) " +
           "AND (:roomNumber IS NULL OR s.roomNumber = :roomNumber) " +
           "AND (:documentNumber IS NULL OR s.documentNumber = :documentNumber) " +
           "AND (CAST(:keyword AS string) IS NULL OR :keyword = '' " +
           "     OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "     OR s.documentNumber LIKE CONCAT('%', CAST(:keyword AS string), '%')) " +
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
