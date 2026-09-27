package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.HotelServiceItem;

@Repository
public interface HotelServiceItemRepository extends JpaRepository<HotelServiceItem, Integer> {
}
