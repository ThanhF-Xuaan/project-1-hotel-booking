package vn.edu.utc.hotel_booking.modules.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByIdAndIsDeletedFalse(Long id);

    boolean existsByTaxCodeAndIsDeletedFalse(String taxCode);

    boolean existsByTaxCodeAndIdNotAndIsDeletedFalse(String taxCode, Long id);

    @Query("SELECT c FROM Company c WHERE c.isDeleted = false " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR c.taxCode LIKE CONCAT('%', :keyword, '%') " +
            "    OR LOWER(c.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR c.contactPhone LIKE CONCAT('%', :keyword, '%'))")
    Page<Company> searchCompanies(@Param("keyword") String keyword,
                                 @Param("status") String status,
                                 Pageable pageable);

    @Modifying
    @Query("UPDATE Company c SET c.isDeleted = true, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Long> ids);
}
