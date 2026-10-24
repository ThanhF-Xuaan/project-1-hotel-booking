package vn.edu.utc.hotel_booking.modules.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.finance.entity.BillingRoutingRule;
import vn.edu.utc.hotel_booking.modules.finance.entity.ChargeCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRoutingRuleRepository extends JpaRepository<BillingRoutingRule, Long> {

    List<BillingRoutingRule> findByMasterFolioIdAndIsActiveTrue(Long masterFolioId);

    Optional<BillingRoutingRule> findByMasterFolioIdAndChargeCategoryAndIsActiveTrue(
            Long masterFolioId,
            ChargeCategory chargeCategory
    );
}
