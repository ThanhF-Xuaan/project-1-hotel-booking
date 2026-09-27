package vn.edu.utc.hotel_booking.modules.operation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderDetail;

import java.util.List;

@Repository
public interface ServiceOrderDetailRepository extends JpaRepository<ServiceOrderDetail, Long> {

    List<ServiceOrderDetail> findByServiceOrderId(Long serviceOrderId);
}
