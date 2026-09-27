package vn.edu.utc.hotel_booking.modules.organization.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.organization.entity.Department;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Short> {

    Optional<Department> findByIdAndIsDeletedFalse(Short id);

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIdNotAndIsDeletedFalse(String code, Short id);

    @Query("SELECT d FROM Department d WHERE d.isDeleted = false " +
            "AND (:status IS NULL OR d.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(d.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Department> searchDepartments(@Param("keyword") String keyword,
                                       @Param("status") String status,
                                       Pageable pageable);

    @Modifying
    @Query("UPDATE Department d SET d.isDeleted = true, d.updatedAt = CURRENT_TIMESTAMP WHERE d.id IN :ids")
    int softDeleteBatch(@Param("ids") List<Short> ids);
}
