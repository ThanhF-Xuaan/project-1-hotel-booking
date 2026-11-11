package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRuleType;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleTypeRepository extends JpaRepository<PricingRuleType, String> {

    Optional<PricingRuleType> findByCode(String code);

    Optional<PricingRuleType> findByCodeAndIsDeletedFalse(String code);

    List<PricingRuleType> findByStatusAndIsDeletedFalseOrderByPriorityDesc(String status);

    List<PricingRuleType> findByIsDeletedFalseOrderByPriorityDesc();

    boolean existsByCodeAndIsDeletedFalse(String code);
}
