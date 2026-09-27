package vn.edu.utc.hotel_booking.modules.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRuleType;

import java.util.Optional;

@Repository
public interface DiscountRuleTypeRepository extends JpaRepository<DiscountRuleType, String> {

    Optional<DiscountRuleType> findByCode(String code);
}
