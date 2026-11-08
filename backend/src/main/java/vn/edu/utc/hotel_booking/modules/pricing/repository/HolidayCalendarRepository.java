package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            "AND hc.date = :date AND hc.status = 'ACTIVE'")
    Optional<HolidayCalendar> findHolidayByDate(@Param("date") LocalDate date);

    Optional<HolidayCalendar> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByNameAndDateAndIsDeletedFalse(String name, LocalDate date);

    @Query("SELECT hc FROM HolidayCalendar hc WHERE hc.isDeleted = false " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "     OR LOWER(hc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(hc.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:year IS NULL OR EXTRACT(YEAR FROM hc.date) = :year) " +
            "AND (:month IS NULL OR EXTRACT(MONTH FROM hc.date) = :month) " +
            "AND (:status IS NULL OR :status = '' OR hc.status = :status)")
    Page<HolidayCalendar> filter(
            @Param("keyword") String keyword,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("status") String status,
            Pageable pageable
    );
}
