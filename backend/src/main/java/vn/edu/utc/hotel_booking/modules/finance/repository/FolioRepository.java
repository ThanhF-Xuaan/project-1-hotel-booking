package vn.edu.utc.hotel_booking.modules.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.finance.entity.Folio;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolioRepository extends JpaRepository<Folio, Long>, JpaSpecificationExecutor<Folio> {

    Optional<Folio> findByFolioNumber(String folioNumber);

    List<Folio> findByBookingId(Long bookingId);

    Optional<Folio> findByBookingRoomId(Long bookingRoomId);

    Optional<Folio> findByBookingIdAndBookingRoomId(Long bookingId, Long bookingRoomId);

    Optional<Folio> findByBookingIdAndBookingRoomIdIsNull(Long bookingId);
}

