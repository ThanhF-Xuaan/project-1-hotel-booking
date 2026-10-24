package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.ContractAttachment;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractAttachmentRepository extends JpaRepository<ContractAttachment, Long> {

    List<ContractAttachment> findByContractIdAndIsDeletedFalse(Long contractId);

    Optional<ContractAttachment> findByContractIdAndSha256ChecksumAndIsDeletedFalse(Long contractId, String sha256Checksum);
}
