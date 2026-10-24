package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContractStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupContractRepository extends JpaRepository<GroupContract, Long>, JpaSpecificationExecutor<GroupContract> {

    Optional<GroupContract> findByContractNumber(String contractNumber);

    boolean existsByContractNumber(String contractNumber);

    List<GroupContract> findByHotelIdAndStatus(Short hotelId, GroupContractStatus status);

    List<GroupContract> findByCompanyId(Long companyId);

    List<GroupContract> findByStatusAndDepositDueDateBefore(GroupContractStatus status, OffsetDateTime deadline);

    List<GroupContract> findByStatusAndCutoffDateBefore(GroupContractStatus status, OffsetDateTime cutoff);
}
