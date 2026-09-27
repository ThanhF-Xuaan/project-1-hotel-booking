package vn.edu.utc.hotel_booking.modules.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomBed;

import java.util.Optional;

@Repository
public interface RoomBedRepository extends JpaRepository<RoomBed, Short> {

    Optional<RoomBed> findByBedCode(String bedCode);
}
