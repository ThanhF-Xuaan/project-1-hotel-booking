package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrder;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderReferenceType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long>, JpaSpecificationExecutor<ServiceOrder> {

    Optional<ServiceOrder> findByIdAndIsDeletedFalse(Long id);

    Optional<ServiceOrder> findByOrderNumberAndIsDeletedFalse(String orderNumber);

    List<ServiceOrder> findByReferenceTypeAndReferenceIdAndIsDeletedFalse(ServiceOrderReferenceType referenceType, Long referenceId);

    @Query("SELECT so FROM ServiceOrder so WHERE so.referenceType = vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderReferenceType.BOOKING AND so.referenceId = :bookingId AND so.isDeleted = false")
    List<ServiceOrder> findByBookingIdAndIsDeletedFalse(@Param("bookingId") Long bookingId);

    @Query("SELECT so FROM ServiceOrder so WHERE so.isDeleted = false AND (" +
            "(so.referenceType = vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderReferenceType.BOOKING AND so.referenceId = :bookingId) OR " +
            "(so.referenceType = vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderReferenceType.BOOKING_ROOM AND so.referenceId IN :bookingRoomIds))")
    List<ServiceOrder> findAllOrdersForBookingAndRooms(
            @Param("bookingId") Long bookingId,
            @Param("bookingRoomIds") List<Long> bookingRoomIds
    );

    @Query("SELECT so FROM ServiceOrder so " +
            "WHERE so.hotel.id = :hotelId " +
            "AND so.isDeleted = false " +
            "AND so.status NOT IN ('CANCELLED') " +
            "AND so.issuedAt >= :startOfDay AND so.issuedAt <= :endOfDay")
    List<ServiceOrder> findActiveOrdersForHotelAndDate(
            @Param("hotelId") Short hotelId,
            @Param("startOfDay") OffsetDateTime startOfDay,
            @Param("endOfDay") OffsetDateTime endOfDay
    );

    @Query("SELECT DISTINCT so FROM ServiceOrder so " +
            "LEFT JOIN FETCH so.details d " +
            "WHERE so.hotel.id IN :hotelIds " +
            "AND so.isDeleted = false " +
            "AND so.status NOT IN ('CANCELLED') " +
            "AND so.issuedAt >= :startDateTime AND so.issuedAt <= :endDateTime")
    List<ServiceOrder> findActiveOrdersForHotelsAndDateRange(
            @Param("hotelIds") List<Short> hotelIds,
            @Param("startDateTime") OffsetDateTime startDateTime,
            @Param("endDateTime") OffsetDateTime endDateTime
    );
}
