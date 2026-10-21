package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationPolicy;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Integer>, JpaSpecificationExecutor<CancellationPolicy> {

    Optional<CancellationPolicy> findByPolicyCodeAndIsDeletedFalse(String policyCode);

    Optional<CancellationPolicy> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByPolicyCodeAndIsDeletedFalse(String policyCode);

    boolean existsByPolicyCodeAndIdNotAndIsDeletedFalse(String policyCode, Integer id);

    Optional<CancellationPolicy> findFirstByBookingTypeInAndIsActiveTrueAndIsDeletedFalseOrderByIdAsc(Collection<BookingType> bookingTypes);
}
