package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;

import java.util.Optional;

@Repository
public interface MasterItemRepository extends JpaRepository<MasterItem, Integer>, JpaSpecificationExecutor<MasterItem> {

    Optional<MasterItem> findByIdAndIsDeletedFalse(Integer id);

    Optional<MasterItem> findByCodeAndIsDeletedFalse(String code);
}
