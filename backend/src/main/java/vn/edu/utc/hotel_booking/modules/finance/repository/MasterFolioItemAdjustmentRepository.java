package vn.edu.utc.hotel_booking.modules.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolioItemAdjustment;

import java.util.List;
import java.util.Optional;

@Repository
public interface MasterFolioItemAdjustmentRepository extends JpaRepository<MasterFolioItemAdjustment, Long> {

    List<MasterFolioItemAdjustment> findByMasterFolioId(Long masterFolioId);

    Optional<MasterFolioItemAdjustment> findFirstByItemTypeAndReferenceIdOrderByIdDesc(String itemType, Long referenceId);

    List<MasterFolioItemAdjustment> findByMasterFolioIdAndBookingRoomId(Long masterFolioId, Long bookingRoomId);
}
