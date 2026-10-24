package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomOccupancyDto;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingDailyRateService;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingDetailService;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingRoomService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class BookingLookupServiceImpl implements BookingLookupService {

    BookingRepository bookingRepository;
    BookingRoomService bookingRoomService;
    BookingDetailService bookingDetailService;
    BookingDailyRateService bookingDailyRateService;

    @Override
    public Booking getBookingEntityById(Long id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID đơn đặt phòng không được để trống");
        }
        return bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng ID: " + id));
    }

    @Override
    public Booking getBookingEntityByNumber(String bookingNumber) {
        if (bookingNumber == null || bookingNumber.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mã đơn đặt phòng không được để trống");
        }
        return bookingRepository.findByBookingNumber(bookingNumber.trim())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn đặt phòng với mã: " + bookingNumber));
    }

    @Override
    public BookingRoom getBookingRoomEntityById(Long id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID phòng đặt (BookingRoom) không được để trống");
        }
        return bookingRoomService.getEntityById(id);
    }

    @Override
    public java.util.List<BookingRoom> getBookingRoomsByBookingId(Long bookingId) {
        if (bookingId == null) {
            return java.util.List.of();
        }
        return bookingRoomService.findByBookingId(bookingId);
    }

    @Override
    public Booking getBookingByRoomId(Long bookingRoomId) {
        BookingRoom room = getBookingRoomEntityById(bookingRoomId);
        if (room.getBookingDetail() == null || room.getBookingDetail().getBooking() == null) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy thông tin đơn đặt phòng tương ứng với phòng đặt ID: " + bookingRoomId);
        }
        return room.getBookingDetail().getBooking();
    }

    @Override
    public Optional<BookingRoom> findBookingRoomByBookingIdAndRoomInstanceId(Long bookingId, Integer roomInstanceId) {
        if (bookingId == null || roomInstanceId == null) {
            return Optional.empty();
        }
        return bookingRoomService.findByBookingIdAndRoomInstanceId(bookingId, roomInstanceId);
    }

    @Override
    public RoomOccupancyDto getActiveOccupancyByRoomInstanceId(Integer roomInstanceId) {
        if (roomInstanceId == null) {
            return null;
        }
        Map<Integer, RoomOccupancyDto> map = getActiveOccupanciesByRoomInstanceIds(List.of(roomInstanceId));
        return map.get(roomInstanceId);
    }

    @Override
    public Map<Integer, RoomOccupancyDto> getActiveOccupanciesByRoomInstanceIds(Collection<Integer> roomInstanceIds) {
        if (roomInstanceIds == null || roomInstanceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<BookingRoom> activeRooms = bookingRoomService.findActiveByRoomInstanceIdInWithDetails(
                roomInstanceIds,
                List.of(BookingRoomStatus.CHECKED_IN)
        );

        Map<Integer, RoomOccupancyDto> result = new HashMap<>();
        for (BookingRoom br : activeRooms) {
            if (br.getRoomInstance() == null) continue;
            Integer roomInstanceId = br.getRoomInstance().getId();

            String bookingNumber = null;
            LocalDate checkOutDate = null;
            String guestName = null;

            if (br.getBookingDetail() != null) {
                checkOutDate = br.getBookingDetail().getCheckOutDate();
                if (br.getBookingDetail().getBooking() != null) {
                    var booking = br.getBookingDetail().getBooking();
                    bookingNumber = booking.getBookingNumber();
                    if (booking.getBookingGuest() != null) {
                        guestName = org.springframework.util.StringUtils.hasText(booking.getBookingGuest().getFullName())
                                ? booking.getBookingGuest().getFullName()
                                : booking.getBookingGuest().getPhone();
                    }
                }
            }

            if (br.getStayGuests() != null && !br.getStayGuests().isEmpty()) {
                guestName = br.getStayGuests().get(0).getFullName();
            }

            result.put(roomInstanceId, RoomOccupancyDto.builder()
                    .roomInstanceId(roomInstanceId)
                    .bookingNumber(bookingNumber)
                    .checkOutDate(checkOutDate)
                    .guestName(guestName)
                    .build());
        }
        return result;
    }

    @Override
    public BigDecimal getDailyRoomRevenue(Short hotelId, LocalDate date) {
        if (hotelId == null || date == null) {
            return BigDecimal.ZERO;
        }
        return bookingDailyRateService.calculateDailyRoomRevenue(hotelId, date);
    }

    @Override
    public boolean existsBookingDetailByCancellationPolicyId(Integer cancellationPolicyId) {
        if (cancellationPolicyId == null) {
            return false;
        }
        return bookingDetailService.existsByCancellationPolicyId(cancellationPolicyId);
    }

    @Override
    public boolean existsBookingDetailByNoShowPolicyId(Integer noShowPolicyId) {
        if (noShowPolicyId == null) {
            return false;
        }
        return bookingDetailService.existsByNoShowPolicyId(noShowPolicyId);
    }
}
