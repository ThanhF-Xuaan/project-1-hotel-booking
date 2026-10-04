package vn.edu.utc.hotel_booking.modules.finance.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.hotel_booking.modules.finance.entity.PayrollSummary;

@Repository
public interface PayrollSummaryRepository extends JpaRepository<PayrollSummary, Long> {
}
