package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Campaign;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Integer> {

    @Query("SELECT c FROM Campaign c " +
            "JOIN FETCH c.hotel h " +
            "WHERE c.id = :id AND c.isDeleted = false")
    Optional<Campaign> findByIdWithDetails(@Param("id") Integer id);

    Optional<Campaign> findByIdAndIsDeletedFalse(Integer id);

    Optional<Campaign> findByHotelIdAndNameAndIsDeletedFalse(Short hotelId, String name);

    @Query("SELECT c FROM Campaign c " +
            "WHERE c.hotel.id = :hotelId " +
            "AND c.status = 'ACTIVE' " +
            "AND c.isDeleted = false " +
            "AND c.startDate <= :date AND c.endDate >= :date")
    List<Campaign> findActiveCampaignsByHotelIdAndDate(
            @Param("hotelId") Short hotelId,
            @Param("date") LocalDate date
    );

    @Query("SELECT c FROM Campaign c " +
            "JOIN FETCH c.hotel h " +
            "WHERE c.isDeleted = false " +
            "AND (:hotelId IS NULL OR c.hotel.id = :hotelId) " +
            "AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:activeOnDate IS NULL OR (c.startDate <= :activeOnDate AND c.endDate >= :activeOnDate)) " +
            "AND (:status IS NULL OR c.status = :status)")
    Page<Campaign> filterCampaigns(
            @Param("hotelId") Short hotelId,
            @Param("name") String name,
            @Param("activeOnDate") LocalDate activeOnDate,
            @Param("status") String status,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Campaign c SET c.isDeleted = true WHERE c.id IN :ids")
    int softDeleteByIds(@Param("ids") List<Integer> ids);
}
