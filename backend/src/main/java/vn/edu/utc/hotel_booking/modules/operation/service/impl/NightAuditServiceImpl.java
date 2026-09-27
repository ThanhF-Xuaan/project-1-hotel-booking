package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDailyRateRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.NightAuditRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.NightAuditResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrder;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.NightAuditService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class NightAuditServiceImpl implements NightAuditService {

    HotelRepository hotelRepository;
    BookingRepository bookingRepository;
    BookingRoomRepository bookingRoomRepository;
    BookingDailyRateRepository bookingDailyRateRepository;
    RoomInstanceRepository roomInstanceRepository;
    RoomSlotRepository roomSlotRepository;
    ServiceOrderRepository serviceOrderRepository;

    @Override
    @Transactional
    public NightAuditResponse executeNightAudit(NightAuditRequest request) {
        Short hotelId = request.getHotelId();
        LocalDate auditDate = request.getAuditDate();

        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn ID: " + hotelId));

        // 1. Quét và xử lý tự động No-Show: các booking ngày check-in <= auditDate nhưng khách không tới
        List<Booking> pendingBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getHotel().getId().equals(hotelId))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList();

        int noShowCount = 0;
        for (Booking booking : pendingBookings) {
            boolean isNoShow = false;
            for (BookingDetail detail : booking.getBookingDetails()) {
                if (!detail.getCheckInDate().isAfter(auditDate)) {
                    // Kiểm tra xem tất cả các phòng đã check-in chưa
                    boolean anyCheckedIn = detail.getBookingRooms().stream()
                            .anyMatch(br -> br.getStatus() == BookingRoomStatus.CHECKED_IN || br.getActualCheckInAt() != null);
                    if (!anyCheckedIn) {
                        isNoShow = true;
                        break;
                    }
                }
            }

            if (isNoShow) {
                booking.setStatus(BookingStatus.NO_SHOW);
                bookingRepository.save(booking);

                for (BookingDetail detail : booking.getBookingDetails()) {
                    for (BookingRoom br : detail.getBookingRooms()) {
                        br.setStatus(BookingRoomStatus.NO_SHOW);
                        bookingRoomRepository.save(br);

                        // Giải phóng RoomSlot nếu phòng vật lý đã được gán trước đó
                        if (br.getRoomInstance() != null) {
                            List<RoomSlot> slots = roomSlotRepository.findByRoomInstanceIdAndSlotDateBetweenOrderBySlotDateAsc(
                                    br.getRoomInstance().getId(),
                                    detail.getCheckInDate(),
                                    detail.getCheckOutDate().minusDays(1)
                            );
                            for (RoomSlot slot : slots) {
                                if (br.getId().equals(slot.getBookingRoomId())) {
                                    slot.setStatus("READY");
                                    slot.setBookingRoomId(null);
                                    roomSlotRepository.save(slot);
                                }
                            }
                        }
                    }
                }
                noShowCount++;
            }
        }

        // 2. Tính số lượng phòng đang có khách ở (Occupied Rooms)
        int occupiedRooms = (int) roomInstanceRepository.findAll().stream()
                .filter(r -> !r.getIsDeleted() && r.getHotel().getId().equals(hotelId))
                .filter(r -> "OCCUPIED".equalsIgnoreCase(r.getCurrentStatus()))
                .count();

        // 3. Tính tổng doanh thu tiền phòng trong ngày (Daily Room Revenue)
        List<BookingDailyRate> dailyRates = bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate(hotelId, auditDate);
        BigDecimal totalRoomRevenue = dailyRates.stream()
                .map(BookingDailyRate::getNetPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Tính tổng doanh thu dịch vụ trong ngày (Daily Service Revenue)
        OffsetDateTime startOfDay = auditDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = auditDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        List<ServiceOrder> dailyOrders = serviceOrderRepository.findActiveOrdersForHotelAndDate(hotelId, startOfDay, endOfDay);
        BigDecimal totalServiceRevenue = dailyOrders.stream()
                .map(ServiceOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Tổng doanh thu trong ngày
        BigDecimal grandTotalRevenue = totalRoomRevenue.add(totalServiceRevenue);

        return NightAuditResponse.builder()
                .hotelId(hotelId)
                .auditDate(auditDate)
                .noShowBookingsCount(noShowCount)
                .occupiedRoomsCount(occupiedRooms)
                .totalDailyRoomRevenue(totalRoomRevenue)
                .totalDailyServiceRevenue(totalServiceRevenue)
                .totalDailyRevenue(grandTotalRevenue)
                .auditTimestamp(OffsetDateTime.now())
                .status("COMPLETED")
                .build();
    }

    @Override
    public NightAuditResponse getAuditSummary(Short hotelId, LocalDate auditDate) {
        NightAuditRequest request = NightAuditRequest.builder()
                .hotelId(hotelId)
                .auditDate(auditDate)
                .build();

        // Tính toán số liệu thống kê không làm thay đổi trạng thái (preview mode)
        int occupiedRooms = (int) roomInstanceRepository.findAll().stream()
                .filter(r -> !r.getIsDeleted() && r.getHotel().getId().equals(hotelId))
                .filter(r -> "OCCUPIED".equalsIgnoreCase(r.getCurrentStatus()))
                .count();

        List<BookingDailyRate> dailyRates = bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate(hotelId, auditDate);
        BigDecimal totalRoomRevenue = dailyRates.stream()
                .map(BookingDailyRate::getNetPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OffsetDateTime startOfDay = auditDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = auditDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        List<ServiceOrder> dailyOrders = serviceOrderRepository.findActiveOrdersForHotelAndDate(hotelId, startOfDay, endOfDay);
        BigDecimal totalServiceRevenue = dailyOrders.stream()
                .map(ServiceOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotalRevenue = totalRoomRevenue.add(totalServiceRevenue);

        return NightAuditResponse.builder()
                .hotelId(hotelId)
                .auditDate(auditDate)
                .noShowBookingsCount(0)
                .occupiedRoomsCount(occupiedRooms)
                .totalDailyRoomRevenue(totalRoomRevenue)
                .totalDailyServiceRevenue(totalServiceRevenue)
                .totalDailyRevenue(grandTotalRevenue)
                .auditTimestamp(OffsetDateTime.now())
                .status("PREVIEW")
                .build();
    }
}
