package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.NightAudit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface NightAuditRepository extends JpaRepository<NightAudit, Long> {

    boolean existsByHotelIdAndAuditDate(Short hotelId, LocalDate auditDate);

    Optional<NightAudit> findByHotelIdAndAuditDate(Short hotelId, LocalDate auditDate);

    List<NightAudit> findByHotelIdAndAuditDateBetweenOrderByAuditDateDesc(Short hotelId, LocalDate fromDate, LocalDate toDate);
}
