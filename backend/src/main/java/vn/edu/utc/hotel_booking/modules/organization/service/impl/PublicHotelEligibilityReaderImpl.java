package vn.edu.utc.hotel_booking.modules.organization.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.PublicHotelEligibilityReader;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicHotelEligibilityReaderImpl implements PublicHotelEligibilityReader {
    private final HotelRepository repository;
    @Override public boolean isActivePublicHotel(short hotelId) {
        return hotelId > 0 && repository.existsByIdAndStatusAndIsDeletedFalse(hotelId, "ACTIVE");
    }
}
