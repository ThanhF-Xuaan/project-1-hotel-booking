package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;

import java.util.Optional;

@Repository
public interface TaxCategoryRepository extends JpaRepository<TaxCategory, Integer> {

    Optional<TaxCategory> findByCategoryCodeAndIsDeletedFalse(String categoryCode);

    Optional<TaxCategory> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByCategoryCodeAndIsDeletedFalse(String categoryCode);

    @Query("SELECT tc FROM TaxCategory tc WHERE tc.isDeleted = false " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "     OR LOWER(tc.categoryCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(tc.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR tc.status = :status)")
    Page<TaxCategory> filter(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}
