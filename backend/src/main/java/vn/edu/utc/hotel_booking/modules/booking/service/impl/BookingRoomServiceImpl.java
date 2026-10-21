package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingRoomService;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingRoomServiceImpl implements BookingRoomService {

    BookingRoomRepository bookingRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public BookingRoom getEntityById(Long id) {
        return bookingRoomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_ROOM_NOT_FOUND, "Không tìm thấy phòng đặt ID: " + id));
    }

    @Override
    @Transactional
    public BookingRoom save(BookingRoom bookingRoom) {
        return bookingRoomRepository.save(bookingRoom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingRoom> findOverlappingAssignedRooms(Integer roomInstanceId, LocalDate checkInDate, LocalDate checkOutDate) {
        return bookingRoomRepository.findOverlappingAssignedRooms(roomInstanceId, checkInDate, checkOutDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingRoom> findByBookingId(Long bookingId) {
        if (bookingId == null) {
            return List.of();
        }
        return bookingRoomRepository.findByBookingId(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingRoom> findByBookingDetailId(Long bookingDetailId) {
        if (bookingDetailId == null) {
            return List.of();
        }
        return bookingRoomRepository.findByBookingDetailId(bookingDetailId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<BookingRoom> findByBookingIdAndRoomInstanceId(Long bookingId, Integer roomInstanceId) {
        if (bookingId == null || roomInstanceId == null) {
            return java.util.Optional.empty();
        }
        return bookingRoomRepository.findByBookingIdAndRoomInstanceId(bookingId, roomInstanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingRoom> findByRoomInstanceIdAndStatusIn(Integer roomInstanceId, java.util.Collection<vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus> statuses) {
        if (roomInstanceId == null || statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return bookingRoomRepository.findByRoomInstanceIdAndStatusIn(roomInstanceId, statuses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingRoom> findActiveByRoomInstanceIdInWithDetails(
            Collection<Integer> roomInstanceIds,
            Collection<vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus> statuses) {
        if (roomInstanceIds == null || roomInstanceIds.isEmpty() || statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return bookingRoomRepository.findActiveByRoomInstanceIdInWithDetails(roomInstanceIds, statuses);
    }
}
