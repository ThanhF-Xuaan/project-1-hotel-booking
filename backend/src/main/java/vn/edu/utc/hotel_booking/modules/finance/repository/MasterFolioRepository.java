package vn.edu.utc.hotel_booking.modules.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolio;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolioStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface MasterFolioRepository extends JpaRepository<MasterFolio, Long>, JpaSpecificationExecutor<MasterFolio> {

    Optional<MasterFolio> findByFolioNumber(String folioNumber);

    Optional<MasterFolio> findByContractId(Long contractId);

    List<MasterFolio> findByHotelIdAndStatus(Short hotelId, MasterFolioStatus status);

    List<MasterFolio> findByCompanyId(Long companyId);
}
