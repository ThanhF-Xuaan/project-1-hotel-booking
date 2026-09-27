package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoomFeatureRepository extends JpaRepository<RoomFeature, Short> {

    Optional<RoomFeature> findByCode(String code);

    Set<RoomFeature> findByIdIn(Set<Short> ids);
}
