package vn.edu.utc.hotel_booking.modules.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.finance.entity.FolioTransaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolioTransactionRepository extends JpaRepository<FolioTransaction, Long> {

    List<FolioTransaction> findByFolioIdOrderByPostedAtAsc(Long folioId);

    List<FolioTransaction> findByFolioIdAndTransactionType(Long folioId, String transactionType);

    Optional<FolioTransaction> findBySourceTypeAndSourceId(String sourceType, Long sourceId);
}

