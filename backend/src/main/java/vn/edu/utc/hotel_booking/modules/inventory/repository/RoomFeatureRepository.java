package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoomFeatureRepository extends JpaRepository<RoomFeature, Short> {

    Optional<RoomFeature> findByCode(String code);

    Optional<RoomFeature> findByIdAndIsDeletedFalse(Short id);

    Optional<RoomFeature> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIdNotAndIsDeletedFalse(String code, Short id);

    Set<RoomFeature> findByIdIn(Set<Short> ids);

    Set<RoomFeature> findByIdInAndIsDeletedFalse(Set<Short> ids);

    @Query("SELECT rf FROM RoomFeature rf WHERE rf.isDeleted = false " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(rf.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(rf.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:category IS NULL OR :category = '' OR rf.category = :category) " +
            "AND (:status IS NULL OR :status = '' OR rf.status = :status)")
    Page<RoomFeature> filterRoomFeatures(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("status") String status,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE RoomFeature rf SET rf.isDeleted = true WHERE rf.id IN :ids")
    int softDeleteByIds(@Param("ids") List<Short> ids);
}
