package vn.edu.utc.hotel_booking.modules.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupInquiry;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupInquiryStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInquiryRepository extends JpaRepository<GroupInquiry, Long>, JpaSpecificationExecutor<GroupInquiry> {

    Optional<GroupInquiry> findByInquiryNumber(String inquiryNumber);

    boolean existsByInquiryNumber(String inquiryNumber);

    List<GroupInquiry> findByHotelIdAndStatus(Short hotelId, GroupInquiryStatus status);
}
