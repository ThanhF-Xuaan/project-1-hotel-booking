package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.entity.Guest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    Optional<Guest> findByIdAndIsDeletedFalse(Long id);

    Optional<Guest> findByPublicIdAndIsDeletedFalse(UUID publicId);

    Optional<Guest> findByPhoneAndIsDeletedFalse(String phone);

    boolean existsByPhoneAndIsDeletedFalse(String phone);

    @Query("SELECT g FROM Guest g WHERE g.isDeleted = false " +
            "AND (:identityType IS NULL OR g.identityType = :identityType) " +
            "AND (:status IS NULL OR g.status = :status) " +
            "AND (:keyword IS NULL OR g.phone LIKE CONCAT('%', :keyword, '%') " +
            "    OR LOWER(g.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR g.identityNumber LIKE CONCAT('%', :keyword, '%'))")
    Page<Guest> searchGuests(@Param("keyword") String keyword,
                             @Param("identityType") String identityType,
                             @Param("status") String status,
                             Pageable pageable);

    @Modifying
    @Query("UPDATE Guest g SET g.isDeleted = true, g.updatedAt = CURRENT_TIMESTAMP WHERE g.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Long> ids);
}
