package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrder;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long>, JpaSpecificationExecutor<ServiceOrder> {

    Optional<ServiceOrder> findByIdAndIsDeletedFalse(Long id);

    Optional<ServiceOrder> findByOrderNumberAndIsDeletedFalse(String orderNumber);

    List<ServiceOrder> findByBookingIdAndIsDeletedFalse(Long bookingId);

    List<ServiceOrder> findByRoomInstanceIdAndIsDeletedFalse(Integer roomInstanceId);

    @org.springframework.data.jpa.repository.Query("SELECT so FROM ServiceOrder so " +
            "WHERE so.roomInstance.hotel.id = :hotelId " +
            "AND so.isDeleted = false " +
            "AND so.status NOT IN ('CANCELLED') " +
            "AND so.issuedAt >= :startOfDay AND so.issuedAt <= :endOfDay")
    List<ServiceOrder> findActiveOrdersForHotelAndDate(
            @org.springframework.data.repository.query.Param("hotelId") Short hotelId,
            @org.springframework.data.repository.query.Param("startOfDay") java.time.OffsetDateTime startOfDay,
            @org.springframework.data.repository.query.Param("endOfDay") java.time.OffsetDateTime endOfDay
    );
}
