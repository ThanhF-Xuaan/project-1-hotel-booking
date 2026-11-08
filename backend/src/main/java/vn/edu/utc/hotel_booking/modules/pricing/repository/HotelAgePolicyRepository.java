package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HotelAgePolicy;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelAgePolicyRepository extends JpaRepository<HotelAgePolicy, Short> {

    Optional<HotelAgePolicy> findByIdAndIsDeletedFalse(Short id);

    @Query("SELECT p FROM HotelAgePolicy p LEFT JOIN FETCH p.hotel " +
            "WHERE p.hotel.id = :hotelId AND p.guestType = :guestType " +
            "AND p.status = 'ACTIVE' AND p.isDeleted = false")
    Optional<HotelAgePolicy> findActiveByHotelIdAndGuestType(
            @Param("hotelId") Short hotelId,
            @Param("guestType") String guestType
    );

    @Query("SELECT p FROM HotelAgePolicy p " +
            "WHERE p.hotel IS NULL AND p.guestType = :guestType " +
            "AND p.status = 'ACTIVE' AND p.isDeleted = false")
    Optional<HotelAgePolicy> findActiveChainPolicyByGuestType(
            @Param("guestType") String guestType
    );

    @Query("SELECT p FROM HotelAgePolicy p LEFT JOIN FETCH p.hotel " +
            "WHERE p.isDeleted = false AND p.status = 'ACTIVE' " +
            "AND (:hotelId IS NULL AND p.hotel IS NULL OR p.hotel.id = :hotelId OR p.hotel IS NULL) " +
            "ORDER BY p.hotel.id NULLS LAST, p.minAge ASC")
    List<HotelAgePolicy> findCandidatesForHotel(@Param("hotelId") Short hotelId);

    @Query("SELECT p FROM HotelAgePolicy p LEFT JOIN FETCH p.hotel WHERE p.isDeleted = false " +
            "AND (:guestType IS NULL OR :guestType = '' OR p.guestType = :guestType) " +
            "AND (:status IS NULL OR :status = '' OR p.status = :status) " +
            "AND (" +
            "     (:chainOnly = true AND p.hotel IS NULL) OR " +
            "     (:chainOnly = false AND (:hotelId IS NULL OR p.hotel.id = :hotelId)) OR " +
            "     (:chainOnly IS NULL AND (:hotelId IS NULL OR p.hotel.id = :hotelId))" +
            ")")
    Page<HotelAgePolicy> filter(
            @Param("hotelId") Short hotelId,
            @Param("guestType") String guestType,
            @Param("status") String status,
            @Param("chainOnly") Boolean chainOnly,
            Pageable pageable
    );

    @Query("SELECT COUNT(p) > 0 FROM HotelAgePolicy p WHERE p.hotel.id = :hotelId " +
            "AND p.guestType = :guestType AND p.isDeleted = false")
    boolean existsByHotelIdAndGuestTypeAndIsDeletedFalse(
            @Param("hotelId") Short hotelId,
            @Param("guestType") String guestType
    );

    @Query("SELECT COUNT(p) > 0 FROM HotelAgePolicy p WHERE p.hotel IS NULL " +
            "AND p.guestType = :guestType AND p.isDeleted = false")
    boolean existsByHotelIsNullAndGuestTypeAndIsDeletedFalse(
            @Param("guestType") String guestType
    );

    @Query("SELECT p FROM HotelAgePolicy p WHERE p.isDeleted = false AND p.status = 'ACTIVE' " +
            "AND (:hotelId IS NULL AND p.hotel IS NULL OR p.hotel.id = :hotelId) " +
            "AND (:excludeId IS NULL OR p.id != :excludeId)")
    List<HotelAgePolicy> findActivePoliciesByScope(
            @Param("hotelId") Short hotelId,
            @Param("excludeId") Short excludeId
    );
}

