package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HolidayCalendar;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, Integer> {

    @Query("SELECT hc FROM HolidayCalendar hc " +
            "WHERE hc.isDeleted = false " +
            "AND hc.startDate <= :date AND hc.endDate >= :date")
    Optional<HolidayCalendar> findHolidayByDate(@Param("date") LocalDate date);
}
