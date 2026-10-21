package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPolicy;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface NoShowPolicyRepository extends JpaRepository<NoShowPolicy, Integer>, JpaSpecificationExecutor<NoShowPolicy> {

    Optional<NoShowPolicy> findByPolicyCodeAndIsDeletedFalse(String policyCode);

    Optional<NoShowPolicy> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByPolicyCodeAndIsDeletedFalse(String policyCode);

    boolean existsByPolicyCodeAndIdNotAndIsDeletedFalse(String policyCode, Integer id);

    Optional<NoShowPolicy> findFirstByBookingTypeInAndIsActiveTrueAndIsDeletedFalseOrderByIdAsc(Collection<BookingType> bookingTypes);
}
