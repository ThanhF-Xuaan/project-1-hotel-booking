package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.PropertyPolicyOverride;

import java.util.Optional;

@Repository
public interface PropertyPolicyOverrideRepository extends JpaRepository<PropertyPolicyOverride, Integer>, JpaSpecificationExecutor<PropertyPolicyOverride> {

    Optional<PropertyPolicyOverride> findByIdAndIsDeletedFalse(Integer id);

    Optional<PropertyPolicyOverride> findByHotelIdAndHotelRoomTypeIdAndIsActiveTrueAndIsDeletedFalse(Short hotelId, Integer hotelRoomTypeId);

    Optional<PropertyPolicyOverride> findByHotelIdAndHotelRoomTypeIsNullAndIsActiveTrueAndIsDeletedFalse(Short hotelId);

    boolean existsByHotelIdAndHotelRoomTypeIdAndIsActiveTrueAndIsDeletedFalse(Short hotelId, Integer hotelRoomTypeId);

    boolean existsByHotelIdAndHotelRoomTypeIsNullAndIsActiveTrueAndIsDeletedFalse(Short hotelId);

    boolean existsByHotelIdAndHotelRoomTypeIdAndIsActiveTrueAndIsDeletedFalseAndIdNot(Short hotelId, Integer hotelRoomTypeId, Integer id);

    boolean existsByHotelIdAndHotelRoomTypeIsNullAndIsActiveTrueAndIsDeletedFalseAndIdNot(Short hotelId, Integer id);

    boolean existsByCancellationPolicyIdAndIsDeletedFalse(Integer cancellationPolicyId);

    boolean existsByNoShowPolicyIdAndIsDeletedFalse(Integer noShowPolicyId);
}
