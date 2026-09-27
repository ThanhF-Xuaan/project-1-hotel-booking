package vn.edu.utc.hotel_booking.modules.dashboard.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoomStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDailyRateRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDetailRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.DashboardKpiResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.OccupancyTrendDto;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.RevenueTrendDto;
import vn.edu.utc.hotel_booking.modules.dashboard.service.DashboardService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrder;
import vn.edu.utc.hotel_booking.modules.operation.repository.ServiceOrderRepository;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    HotelRepository hotelRepository;
    RoomInstanceRepository roomInstanceRepository;
    BookingRepository bookingRepository;
    BookingDetailRepository bookingDetailRepository;
    BookingDailyRateRepository bookingDailyRateRepository;
    ServiceOrderRepository serviceOrderRepository;

    @Override
    public DashboardKpiResponse getExecutiveKpis(Short hotelId, LocalDate targetDate) {
        if (targetDate == null) {
            targetDate = LocalDate.now();
        }

        Hotel hotel = hotelRepository.findByIdAndIsDeletedFalse(hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn ID: " + hotelId));

        List<RoomInstance> allRooms = roomInstanceRepository.findAll().stream()
                .filter(r -> !r.getIsDeleted() && r.getHotel().getId().equals(hotelId))
                .toList();

        int totalRooms = allRooms.size();
        int occupiedCount = (int) allRooms.stream().filter(r -> "OCCUPIED".equalsIgnoreCase(r.getCurrentStatus())).count();
        int outOfServiceCount = (int) allRooms.stream()
                .filter(r -> List.of("CLEANING", "DIRTY", "MAINTENANCE").contains(r.getCurrentStatus().toUpperCase()))
                .count();
        int availableCount = Math.max(0, totalRooms - occupiedCount - outOfServiceCount);

        double occupancyRate = totalRooms > 0
                ? Math.round((occupiedCount * 100.0 / totalRooms) * 10.0) / 10.0
                : 0.0;

        // Doanh thu phòng
        List<BookingDailyRate> dailyRates = bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate(hotelId, targetDate);
        BigDecimal totalRoomRevenue = dailyRates.stream()
                .map(BookingDailyRate::getNetPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Doanh thu dịch vụ
        OffsetDateTime startOfDay = targetDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = targetDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        List<ServiceOrder> dailyOrders = serviceOrderRepository.findActiveOrdersForHotelAndDate(hotelId, startOfDay, endOfDay);
        BigDecimal totalServiceRevenue = dailyOrders.stream()
                .map(ServiceOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenueToday = totalRoomRevenue.add(totalServiceRevenue);

        // Tính ADR & RevPAR chuẩn ngành khách sạn
        BigDecimal adr = occupiedCount > 0
                ? totalRoomRevenue.divide(BigDecimal.valueOf(occupiedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal revPar = totalRooms > 0
                ? totalRoomRevenue.divide(BigDecimal.valueOf(totalRooms), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Thống kê luồng vận hành lễ tân hôm nay
        LocalDate finalTargetDate = targetDate;
        List<BookingDetail> arrivalsToday = bookingDetailRepository.findAll().stream()
                .filter(bd -> bd.getBooking().getHotel().getId().equals(hotelId))
                .filter(bd -> bd.getBooking().getStatus() == BookingStatus.CONFIRMED)
                .filter(bd -> bd.getCheckInDate().isEqual(finalTargetDate))
                .toList();

        List<BookingDetail> departuresToday = bookingDetailRepository.findAll().stream()
                .filter(bd -> bd.getBooking().getHotel().getId().equals(hotelId))
                .filter(bd -> bd.getBooking().getStatus() == BookingStatus.CONFIRMED)
                .filter(bd -> bd.getCheckOutDate().isEqual(finalTargetDate))
                .toList();

        int inHouseGuests = allRooms.stream()
                .filter(r -> "OCCUPIED".equalsIgnoreCase(r.getCurrentStatus()))
                .mapToInt(r -> 2) // Tiêu chuẩn ước tính trung bình 2 khách / phòng occupied
                .sum();

        return DashboardKpiResponse.builder()
                .hotelId(hotelId)
                .hotelName(hotel.getName())
                .targetDate(targetDate)
                .occupancyRate(occupancyRate)
                .averageDailyRate(adr)
                .revPar(revPar)
                .totalActiveRooms(totalRooms)
                .occupiedRooms(occupiedCount)
                .availableRooms(availableCount)
                .outOfServiceRooms(outOfServiceCount)
                .totalRoomRevenue(totalRoomRevenue)
                .totalServiceRevenue(totalServiceRevenue)
                .totalRevenueToday(totalRevenueToday)
                .arrivalsTodayCount(arrivalsToday.size())
                .departuresTodayCount(departuresToday.size())
                .inHouseGuestsCount(inHouseGuests)
                .build();
    }

    @Override
    public List<RevenueTrendDto> getRevenueTrends(Short hotelId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();

        List<RevenueTrendDto> list = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            List<BookingDailyRate> rates = bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate(hotelId, cur);
            BigDecimal roomRev = rates.stream().map(BookingDailyRate::getNetPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

            OffsetDateTime startOfDay = cur.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime endOfDay = cur.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
            List<ServiceOrder> orders = serviceOrderRepository.findActiveOrdersForHotelAndDate(hotelId, startOfDay, endOfDay);
            BigDecimal srvRev = orders.stream().map(ServiceOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            list.add(RevenueTrendDto.builder()
                    .date(cur)
                    .roomRevenue(roomRev)
                    .serviceRevenue(srvRev)
                    .totalRevenue(roomRev.add(srvRev))
                    .build());

            cur = cur.plusDays(1);
        }

        return list;
    }

    @Override
    public List<OccupancyTrendDto> getOccupancyTrends(Short hotelId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();

        int totalRooms = (int) roomInstanceRepository.findAll().stream()
                .filter(r -> !r.getIsDeleted() && r.getHotel().getId().equals(hotelId))
                .count();

        List<OccupancyTrendDto> list = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            List<BookingDailyRate> rates = bookingDailyRateRepository.findActiveDailyRatesForHotelAndDate(hotelId, cur);
            int occupiedCount = (int) rates.stream()
                    .filter(r -> r.getBookingRoom() != null && r.getBookingRoom().getStatus() == BookingRoomStatus.CHECKED_IN)
                    .count();

            // Nếu ngày hôm nay/quá khứ mà chưa có checkin chi tiết, lấy theo số phòng có daily rate active
            if (occupiedCount == 0 && !rates.isEmpty()) {
                occupiedCount = rates.size();
            }

            double occRate = totalRooms > 0
                    ? Math.round((occupiedCount * 100.0 / totalRooms) * 10.0) / 10.0
                    : 0.0;

            list.add(OccupancyTrendDto.builder()
                    .date(cur)
                    .totalRooms(totalRooms)
                    .occupiedRooms(occupiedCount)
                    .occupancyRate(occRate)
                    .build());

            cur = cur.plusDays(1);
        }

        return list;
    }
}
