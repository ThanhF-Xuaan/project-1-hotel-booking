package vn.edu.utc.hotel_booking.modules.finance.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.utc.hotel_booking.modules.finance.entity.PayrollSummary;

import java.util.Optional;

@Repository
public interface PayrollSummaryRepository extends JpaRepository<PayrollSummary, Long> {

    Optional<PayrollSummary> findByStaffIdAndPeriodMonthAndPeriodYearAndIsDeletedFalse(
            Integer staffId, Integer periodMonth, Integer periodYear);

    Optional<PayrollSummary> findFirstByStaffIdAndPeriodMonthAndPeriodYearAndIsDeletedFalseOrderByIdDesc(
            Integer staffId, Integer periodMonth, Integer periodYear);
}
