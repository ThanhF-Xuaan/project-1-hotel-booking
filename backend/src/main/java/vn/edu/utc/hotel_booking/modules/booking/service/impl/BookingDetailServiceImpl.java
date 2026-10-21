package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDetailRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingDetailService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class BookingDetailServiceImpl implements BookingDetailService {

    BookingDetailRepository bookingDetailRepository;

    @Override
    public BookingDetail getEntityById(Long id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID chi tiết đặt phòng không được để trống");
        }
        return bookingDetailRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND, "Không tìm thấy chi tiết đặt phòng ID: " + id));
    }

    @Override
    public List<BookingDetail> findByBookingId(Long bookingId) {
        if (bookingId == null) {
            return List.of();
        }
        return bookingDetailRepository.findByBookingId(bookingId);
    }

    @Override
    public boolean existsByCancellationPolicyId(Integer cancellationPolicyId) {
        if (cancellationPolicyId == null) {
            return false;
        }
        return bookingDetailRepository.existsByCancellationPolicyId(cancellationPolicyId);
    }

    @Override
    public boolean existsByNoShowPolicyId(Integer noShowPolicyId) {
        if (noShowPolicyId == null) {
            return false;
        }
        return bookingDetailRepository.existsByNoShowPolicyId(noShowPolicyId);
    }
}
