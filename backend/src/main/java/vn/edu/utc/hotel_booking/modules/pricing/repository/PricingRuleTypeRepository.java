package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRuleType;

import java.util.Optional;

@Repository
public interface PricingRuleTypeRepository extends JpaRepository<PricingRuleType, String> {

    Optional<PricingRuleType> findByCode(String code);
}
