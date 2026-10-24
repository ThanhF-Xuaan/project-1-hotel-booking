package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.entity.RoomSwapRecord;
import vn.edu.utc.hotel_booking.modules.booking.repository.RoomSwapRecordRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.RoomSwapRecordService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RoomSwapRecordServiceImpl implements RoomSwapRecordService {

    RoomSwapRecordRepository roomSwapRecordRepository;

    @Override
    @Transactional
    public RoomSwapRecord save(RoomSwapRecord record) {
        return roomSwapRecordRepository.save(record);
    }

    @Override
    public List<RoomSwapRecord> findByBookingRoomId(Long bookingRoomId) {
        return roomSwapRecordRepository.findByBookingRoomId(bookingRoomId);
    }

    @Override
    public List<RoomSwapRecord> findByBookingId(Long bookingId) {
        return roomSwapRecordRepository.findByBookingRoomBookingDetailBookingId(bookingId);
    }

    @Override
    public List<RoomSwapRecord> findBySwapDateBetween(LocalDate startDate, LocalDate endDate) {
        return roomSwapRecordRepository.findBySwapDateBetween(startDate, endDate);
    }
}
