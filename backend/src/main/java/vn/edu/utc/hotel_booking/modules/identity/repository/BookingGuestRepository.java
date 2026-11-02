package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.entity.BookingGuest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingGuestRepository extends JpaRepository<BookingGuest, Long> {

    Optional<BookingGuest> findByIdAndIsDeletedFalse(Long id);

    Optional<BookingGuest> findByPublicIdAndIsDeletedFalse(UUID publicId);

    Optional<BookingGuest> findByPhoneAndIsDeletedFalse(String phone);

    Optional<BookingGuest> findByIdentityNumberAndIsDeletedFalse(String identityNumber);

    boolean existsByPhoneAndIsDeletedFalse(String phone);

    @Query("SELECT g FROM BookingGuest g WHERE g.isDeleted = false " +
            "AND (CAST(:identityType AS string) IS NULL OR g.identityType = :identityType) " +
            "AND (CAST(:status AS string) IS NULL OR g.status = :status) " +
            "AND (CAST(:keyword AS string) IS NULL OR g.phone LIKE CONCAT('%', CAST(:keyword AS string), '%') " +
            "    OR LOWER(g.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
            "    OR g.identityNumber LIKE CONCAT('%', CAST(:keyword AS string), '%'))")
    Page<BookingGuest> searchGuests(@Param("keyword") String keyword,
                                    @Param("identityType") String identityType,
                                    @Param("status") String status,
                                    Pageable pageable);

    @Modifying
    @Query("UPDATE BookingGuest g SET g.isDeleted = true, g.updatedAt = CURRENT_TIMESTAMP WHERE g.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Long> ids);
}
