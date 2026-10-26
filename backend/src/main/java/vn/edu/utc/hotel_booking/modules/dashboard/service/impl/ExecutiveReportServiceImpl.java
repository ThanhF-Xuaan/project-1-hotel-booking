package vn.edu.utc.hotel_booking.modules.dashboard.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDailyRate;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.request.ExecutiveReportFilterDto;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.*;
import vn.edu.utc.hotel_booking.modules.dashboard.service.ExecutiveReportService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.operation.entity.NightAudit;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrder;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServiceOrderDetail;
import vn.edu.utc.hotel_booking.modules.operation.service.NightAuditService;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.organization.service.RegionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class ExecutiveReportServiceImpl implements ExecutiveReportService {

    HotelService hotelService;
    RegionService regionService;
    RoomInstanceService roomInstanceService;
    BookingLookupService bookingLookupService;
    ServiceOrderService serviceOrderService;
    NightAuditService nightAuditService;

    @Override
    public ExecutiveBiReportResponse generateExecutiveReport(ExecutiveReportFilterDto filter) {
        if (filter == null) {
            filter = new ExecutiveReportFilterDto();
        }

        LocalDate startDate = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.now().minusDays(29);
        LocalDate endDate = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now();

        if (startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "startDate không được sau endDate");
        }

        String rawScope = filter.getScope() != null ? filter.getScope().trim().toUpperCase() : "CHAIN";
        List<Hotel> targetHotels = resolveTargetHotels(rawScope, filter.getRegionId(), filter.getHotelId());
        String scopeName = resolveScopeName(rawScope, filter.getRegionId(), filter.getHotelId(), targetHotels);

        if (targetHotels.isEmpty()) {
            return buildEmptyResponse(rawScope, scopeName, startDate, endDate);
        }

        List<Short> hotelIds = targetHotels.stream().map(Hotel::getId).toList();

        // 1. Kiểm tra Night Audit chốt sổ (Ngoại lệ 3a SRS)
        boolean isProvisional = false;
        for (Hotel hotel : targetHotels) {
            if (!nightAuditService.isNightAuditCompleted(hotel.getId(), endDate)) {
                isProvisional = true;
                break;
            }
        }
        String provisionalNotice = isProvisional
                ? "Cảnh báo: Dữ liệu doanh thu & chỉ số đang ở trạng thái TẠM TÍNH do chưa hoàn tất Night Audit (Chốt kiểm toán đêm) cho toàn bộ cơ sở trong kỳ báo cáo."
                : null;

        // 2. Lấy dữ liệu phòng & công suất
        List<RoomInstance> allRooms = roomInstanceService.getActiveRoomsByHotelIds(hotelIds);
        Map<Short, List<RoomInstance>> roomsByHotel = allRooms.stream()
                .collect(Collectors.groupingBy(r -> r.getHotel().getId()));

        long daysCount = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int totalPhysicalRooms = allRooms.size();
        long totalAvailableRoomNights = totalPhysicalRooms * daysCount;

        // 3. Lấy dữ liệu doanh thu phòng (BookingDailyRate)
        List<BookingDailyRate> dailyRates = bookingLookupService.getActiveDailyRatesForHotelsAndDateRange(hotelIds, startDate, endDate);

        BigDecimal totalRoomRevenue = dailyRates.stream()
                .map(BookingDailyRate::getNetPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOccupiedRoomNights = dailyRates.size();

        // 4. Lấy dữ liệu doanh thu dịch vụ (ServiceOrder)
        OffsetDateTime startDateTime = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endDateTime = endDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        List<ServiceOrder> serviceOrders = serviceOrderService.getActiveOrdersForHotelsAndDateRange(hotelIds, startDateTime, endDateTime);

        // 5. Phân tách Doanh thu theo Phòng ban (Department Revenue Breakdown)
        DepartmentRevenueBreakdownDto departmentRevenue = calculateDepartmentRevenue(totalRoomRevenue, serviceOrders);

        // 6. Phân tách Doanh thu theo Phân khúc khách hàng (FIT vs GIT Segment)
        SegmentRevenueBreakdownDto segmentRevenue = calculateSegmentRevenue(dailyRates, serviceOrders);

        // 7. Tổng hợp KPI Cấp cao (USALI)
        BigDecimal totalRevenue = totalRoomRevenue.add(departmentRevenue.getTotalServiceRevenue());
        ExecutiveKpiSummaryDto summary = calculateKpiSummary(
                totalPhysicalRooms, totalAvailableRoomNights, totalOccupiedRoomNights,
                totalRoomRevenue, departmentRevenue.getTotalFnbRevenue(), departmentRevenue.getTotalOtherRevenue(), totalRevenue
        );

        // 8. Phân tách theo Vùng (Region Breakdown)
        List<RegionPerformanceDto> regionBreakdowns = calculateRegionPerformance(targetHotels, roomsByHotel, dailyRates, serviceOrders, totalRevenue, daysCount);

        // 9. Phân tách và Xếp hạng theo Khách sạn (Hotel Breakdown & Ranking)
        List<HotelPerformanceDto> hotelBreakdowns = calculateHotelPerformance(targetHotels, roomsByHotel, dailyRates, serviceOrders, daysCount);

        // 10. Xu hướng theo Ngày (Time-series Trends)
        List<BiTrendPointDto> trends = calculateTrends(targetHotels, roomsByHotel, dailyRates, serviceOrders, startDate, endDate);

        return ExecutiveBiReportResponse.builder()
                .scope(rawScope)
                .scopeName(scopeName)
                .startDate(startDate)
                .endDate(endDate)
                .isProvisional(isProvisional)
                .provisionalNotice(provisionalNotice)
                .summary(summary)
                .departmentRevenue(departmentRevenue)
                .segmentRevenue(segmentRevenue)
                .regionBreakdowns(regionBreakdowns)
                .hotelBreakdowns(hotelBreakdowns)
                .trends(trends)
                .build();
    }

    @Override
    public ExecutiveKpiSummaryDto getQuickSummary(String scope, Short regionId, Short hotelId, LocalDate targetDate) {
        if (targetDate == null) {
            targetDate = LocalDate.now();
        }
        ExecutiveReportFilterDto filter = ExecutiveReportFilterDto.builder()
                .scope(scope)
                .regionId(regionId)
                .hotelId(hotelId)
                .startDate(targetDate)
                .endDate(targetDate)
                .build();

        ExecutiveBiReportResponse response = generateExecutiveReport(filter);
        return response.getSummary();
    }

    // --- Private Helper Methods ---

    private List<Hotel> resolveTargetHotels(String scope, Short regionId, Short hotelId) {
        return switch (scope) {
            case "HOTEL" -> {
                if (hotelId == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Cần cung cấp hotelId khi chọn scope HOTEL");
                }
                Hotel h = hotelService.getEntityById(hotelId);
                yield List.of(h);
            }
            case "REGION" -> {
                if (regionId == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Cần cung cấp regionId khi chọn scope REGION");
                }
                regionService.getEntityById(regionId);
                yield hotelService.getHotelsByRegionId(regionId);
            }
            default -> hotelService.getAllActiveHotels();
        };
    }

    private String resolveScopeName(String scope, Short regionId, Short hotelId, List<Hotel> targetHotels) {
        return switch (scope) {
            case "HOTEL" -> targetHotels.isEmpty() ? "Khách sạn không xác định" : targetHotels.getFirst().getName();
            case "REGION" -> {
                if (regionId != null) {
                    try {
                        Region r = regionService.getEntityById(regionId);
                        if (r != null) {
                            yield "Vùng " + r.getName();
                        }
                    } catch (Exception ignored) {
                    }
                }
                yield "Vùng chỉ định";
            }
            default -> "Toàn hệ thống Chuỗi Khách sạn";
        };
    }

    private DepartmentRevenueBreakdownDto calculateDepartmentRevenue(BigDecimal roomRevenue, List<ServiceOrder> serviceOrders) {
        BigDecimal restaurantRevenue = BigDecimal.ZERO;
        BigDecimal banquetRevenue = BigDecimal.ZERO;
        BigDecimal breakfastRevenue = BigDecimal.ZERO;

        BigDecimal spaRevenue = BigDecimal.ZERO;
        BigDecimal laundryRevenue = BigDecimal.ZERO;
        BigDecimal minibarRevenue = BigDecimal.ZERO;
        BigDecimal transportRevenue = BigDecimal.ZERO;
        BigDecimal miscellaneousRevenue = BigDecimal.ZERO;

        for (ServiceOrder order : serviceOrders) {
            if (order.getDetails() != null && !order.getDetails().isEmpty()) {
                for (ServiceOrderDetail d : order.getDetails()) {
                    BigDecimal itemAmount = d.getTotalAmount() != null ? d.getTotalAmount() : BigDecimal.ZERO;
                    String type = d.getItemType() != null ? d.getItemType().toUpperCase() : "";
                    String name = d.getItemName() != null ? d.getItemName().toLowerCase() : "";

                    if (type.contains("BANQUET") || name.contains("tiệc") || name.contains("hội nghị") || name.contains("banquet") || name.contains("event")) {
                        banquetRevenue = banquetRevenue.add(itemAmount);
                    } else if (type.contains("BREAKFAST") || name.contains("buffet") || name.contains("ăn sáng") || name.contains("breakfast")) {
                        breakfastRevenue = breakfastRevenue.add(itemAmount);
                    } else if (type.contains("RESTAURANT") || type.contains("FNB") || type.contains("FOOD") || name.contains("nhà hàng") || name.contains("restaurant") || name.contains("món") || name.contains("uống") || name.contains("cơm") || name.contains("bar")) {
                        restaurantRevenue = restaurantRevenue.add(itemAmount);
                    } else if (type.contains("SPA") || name.contains("spa") || name.contains("massage")) {
                        spaRevenue = spaRevenue.add(itemAmount);
                    } else if (type.contains("LAUNDRY") || name.contains("giặt") || name.contains("laundry")) {
                        laundryRevenue = laundryRevenue.add(itemAmount);
                    } else if (type.contains("MINIBAR") || name.contains("minibar")) {
                        minibarRevenue = minibarRevenue.add(itemAmount);
                    } else if (type.contains("TRANSPORT") || name.contains("xe") || name.contains("đưa đón") || name.contains("transport")) {
                        transportRevenue = transportRevenue.add(itemAmount);
                    } else {
                        miscellaneousRevenue = miscellaneousRevenue.add(itemAmount);
                    }
                }
            } else {
                BigDecimal orderAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                miscellaneousRevenue = miscellaneousRevenue.add(orderAmount);
            }
        }

        BigDecimal totalFnb = restaurantRevenue.add(banquetRevenue).add(breakfastRevenue);
        BigDecimal totalOther = spaRevenue.add(laundryRevenue).add(minibarRevenue).add(transportRevenue).add(miscellaneousRevenue);
        BigDecimal totalService = totalFnb.add(totalOther);
        BigDecimal grandTotal = roomRevenue.add(totalService);

        double roomPct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                ? roomRevenue.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double fnbPct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                ? totalFnb.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double otherPct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                ? totalOther.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return DepartmentRevenueBreakdownDto.builder()
                .roomRevenue(roomRevenue)
                .restaurantRevenue(restaurantRevenue)
                .banquetRevenue(banquetRevenue)
                .breakfastRevenue(breakfastRevenue)
                .totalFnbRevenue(totalFnb)
                .spaRevenue(spaRevenue)
                .laundryRevenue(laundryRevenue)
                .minibarRevenue(minibarRevenue)
                .transportRevenue(transportRevenue)
                .miscellaneousRevenue(miscellaneousRevenue)
                .totalOtherRevenue(totalOther)
                .totalServiceRevenue(totalService)
                .totalDepartmentRevenue(grandTotal)
                .fnbPercentage(fnbPct)
                .fnbRevenuePercentage(fnbPct)
                .roomPercentage(roomPct)
                .roomRevenuePercentage(roomPct)
                .otherPercentage(otherPct)
                .otherRevenuePercentage(otherPct)
                .build();
    }

    private SegmentRevenueBreakdownDto calculateSegmentRevenue(List<BookingDailyRate> dailyRates, List<ServiceOrder> serviceOrders) {
        BigDecimal fitRoomRevenue = BigDecimal.ZERO;
        BigDecimal gitRoomRevenue = BigDecimal.ZERO;
        int fitRoomNights = 0;
        int gitRoomNights = 0;

        for (BookingDailyRate rate : dailyRates) {
            BigDecimal price = rate.getNetPrice() != null ? rate.getNetPrice() : BigDecimal.ZERO;
            boolean isGit = false;
            try {
                if (rate.getBookingRoom() != null && rate.getBookingRoom().getBookingDetail() != null) {
                    Booking b = rate.getBookingRoom().getBookingDetail().getBooking();
                    if (b != null && (b.getGroupContract() != null || b.getCompany() != null)) {
                        isGit = true;
                    }
                }
            } catch (Exception ignored) {
            }

            if (isGit) {
                gitRoomRevenue = gitRoomRevenue.add(price);
                gitRoomNights++;
            } else {
                fitRoomRevenue = fitRoomRevenue.add(price);
                fitRoomNights++;
            }
        }

        // Với ServiceOrder: Banquet và đơn số lượng lớn quy về GIT, còn lại phân bổ
        BigDecimal fitServiceRev = BigDecimal.ZERO;
        BigDecimal gitServiceRev = BigDecimal.ZERO;

        for (ServiceOrder order : serviceOrders) {
            BigDecimal amt = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            boolean hasBanquet = false;
            if (order.getDetails() != null) {
                for (ServiceOrderDetail d : order.getDetails()) {
                    String type = d.getItemType() != null ? d.getItemType().toUpperCase() : "";
                    String name = d.getItemName() != null ? d.getItemName().toLowerCase() : "";
                    if (type.contains("BANQUET") || name.contains("tiệc") || name.contains("hội nghị")) {
                        hasBanquet = true;
                        break;
                    }
                }
            }
            if (hasBanquet) {
                gitServiceRev = gitServiceRev.add(amt);
            } else {
                fitServiceRev = fitServiceRev.add(amt);
            }
        }

        BigDecimal fitTotal = fitRoomRevenue.add(fitServiceRev);
        BigDecimal gitTotal = gitRoomRevenue.add(gitServiceRev);
        BigDecimal segmentGrandTotal = fitTotal.add(gitTotal);

        double fitShare = segmentGrandTotal.compareTo(BigDecimal.ZERO) > 0
                ? fitTotal.multiply(BigDecimal.valueOf(100)).divide(segmentGrandTotal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double gitShare = segmentGrandTotal.compareTo(BigDecimal.ZERO) > 0
                ? gitTotal.multiply(BigDecimal.valueOf(100)).divide(segmentGrandTotal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return SegmentRevenueBreakdownDto.builder()
                .fitRevenue(fitTotal)
                .gitRevenue(gitTotal)
                .fitRoomNights(fitRoomNights)
                .gitRoomNights(gitRoomNights)
                .fitSharePercentage(fitShare)
                .gitSharePercentage(gitShare)
                .build();
    }

    private ExecutiveKpiSummaryDto calculateKpiSummary(
            int totalRooms, long totalAvailableRoomNights, int totalOccupiedRoomNights,
            BigDecimal roomRevenue, BigDecimal fnbRevenue, BigDecimal otherRevenue, BigDecimal totalRevenue
    ) {
        double occupancyRate = totalAvailableRoomNights > 0
                ? Math.round((totalOccupiedRoomNights * 100.0 / totalAvailableRoomNights) * 10.0) / 10.0
                : 0.0;

        BigDecimal adr = totalOccupiedRoomNights > 0
                ? roomRevenue.divide(BigDecimal.valueOf(totalOccupiedRoomNights), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal revPar = totalAvailableRoomNights > 0
                ? roomRevenue.divide(BigDecimal.valueOf(totalAvailableRoomNights), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ExecutiveKpiSummaryDto.builder()
                .occupancyRate(occupancyRate)
                .averageDailyRate(adr)
                .revPar(revPar)
                .totalActiveRooms(totalRooms)
                .occupiedRoomNights(totalOccupiedRoomNights)
                .availableRoomNights(totalAvailableRoomNights)
                .totalRoomRevenue(roomRevenue)
                .totalFnbRevenue(fnbRevenue)
                .totalOtherRevenue(otherRevenue)
                .totalRevenue(totalRevenue)
                .build();
    }

    private List<RegionPerformanceDto> calculateRegionPerformance(
            List<Hotel> targetHotels, Map<Short, List<RoomInstance>> roomsByHotel,
            List<BookingDailyRate> dailyRates, List<ServiceOrder> serviceOrders,
            BigDecimal overallTotalRevenue, long daysCount
    ) {
        Map<Short, List<Hotel>> hotelsByRegion = targetHotels.stream()
                .filter(h -> h.getRegion() != null)
                .collect(Collectors.groupingBy(h -> h.getRegion().getId()));

        List<RegionPerformanceDto> result = new ArrayList<>();

        for (Map.Entry<Short, List<Hotel>> entry : hotelsByRegion.entrySet()) {
            Short regionId = entry.getKey();
            List<Hotel> regionHotels = entry.getValue();
            String regionName = regionHotels.getFirst().getRegion().getName();
            Set<Short> regionHotelIds = regionHotels.stream().map(Hotel::getId).collect(Collectors.toSet());

            int regionRooms = regionHotels.stream()
                    .mapToInt(h -> roomsByHotel.getOrDefault(h.getId(), Collections.emptyList()).size())
                    .sum();
            long regionAvailableRoomNights = (long) regionRooms * daysCount;

            List<BookingDailyRate> regionRates = dailyRates.stream()
                    .filter(r -> r.getBookingRoom() != null && r.getBookingRoom().getBookingDetail() != null
                            && regionHotelIds.contains(r.getBookingRoom().getBookingDetail().getBooking().getHotel().getId()))
                    .toList();

            BigDecimal regionRoomRevenue = regionRates.stream()
                    .map(BookingDailyRate::getNetPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int regionOccupiedRoomNights = regionRates.size();

            BigDecimal regionFnbRevenue = serviceOrders.stream()
                    .filter(so -> regionHotelIds.contains(so.getHotel().getId()))
                    .map(ServiceOrder::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal regionTotalRevenue = regionRoomRevenue.add(regionFnbRevenue);

            double occRate = regionAvailableRoomNights > 0
                    ? Math.round((regionOccupiedRoomNights * 100.0 / regionAvailableRoomNights) * 10.0) / 10.0
                    : 0.0;

            BigDecimal adr = regionOccupiedRoomNights > 0
                    ? regionRoomRevenue.divide(BigDecimal.valueOf(regionOccupiedRoomNights), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal revPar = regionAvailableRoomNights > 0
                    ? regionRoomRevenue.divide(BigDecimal.valueOf(regionAvailableRoomNights), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            double contributePct = overallTotalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? regionTotalRevenue.multiply(BigDecimal.valueOf(100)).divide(overallTotalRevenue, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(RegionPerformanceDto.builder()
                    .regionId(regionId)
                    .regionName(regionName)
                    .occupancyRate(occRate)
                    .averageDailyRate(adr)
                    .revPar(revPar)
                    .roomRevenue(regionRoomRevenue)
                    .fnbRevenue(regionFnbRevenue)
                    .totalRevenue(regionTotalRevenue)
                    .contributePercentage(contributePct)
                    .build());
        }

        return result;
    }

    private List<HotelPerformanceDto> calculateHotelPerformance(
            List<Hotel> targetHotels, Map<Short, List<RoomInstance>> roomsByHotel,
            List<BookingDailyRate> dailyRates, List<ServiceOrder> serviceOrders, long daysCount
    ) {
        List<HotelPerformanceDto> list = new ArrayList<>();

        for (Hotel hotel : targetHotels) {
            Short hotelId = hotel.getId();
            int hotelRooms = roomsByHotel.getOrDefault(hotelId, Collections.emptyList()).size();
            long hotelAvailableRoomNights = (long) hotelRooms * daysCount;

            List<BookingDailyRate> hotelRates = dailyRates.stream()
                    .filter(r -> r.getBookingRoom() != null && r.getBookingRoom().getBookingDetail() != null
                            && hotelId.equals(r.getBookingRoom().getBookingDetail().getBooking().getHotel().getId()))
                    .toList();

            BigDecimal hotelRoomRevenue = hotelRates.stream()
                    .map(BookingDailyRate::getNetPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int hotelOccupiedRoomNights = hotelRates.size();

            BigDecimal hotelServiceRevenue = serviceOrders.stream()
                    .filter(so -> hotelId.equals(so.getHotel().getId()))
                    .map(ServiceOrder::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal hotelTotalRevenue = hotelRoomRevenue.add(hotelServiceRevenue);

            double occRate = hotelAvailableRoomNights > 0
                    ? Math.round((hotelOccupiedRoomNights * 100.0 / hotelAvailableRoomNights) * 10.0) / 10.0
                    : 0.0;

            BigDecimal adr = hotelOccupiedRoomNights > 0
                    ? hotelRoomRevenue.divide(BigDecimal.valueOf(hotelOccupiedRoomNights), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal revPar = hotelAvailableRoomNights > 0
                    ? hotelRoomRevenue.divide(BigDecimal.valueOf(hotelAvailableRoomNights), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            list.add(HotelPerformanceDto.builder()
                    .hotelId(hotel.getId().intValue())
                    .hotelName(hotel.getName())
                    .regionId(hotel.getRegion() != null ? hotel.getRegion().getId() : null)
                    .regionName(hotel.getRegion() != null ? hotel.getRegion().getName() : null)
                    .occupancyRate(occRate)
                    .averageDailyRate(adr)
                    .revPar(revPar)
                    .roomRevenue(hotelRoomRevenue)
                    .fnbRevenue(hotelServiceRevenue)
                    .otherRevenue(BigDecimal.ZERO)
                    .totalRevenue(hotelTotalRevenue)
                    .build());
        }

        // Sắp xếp giảm dần theo totalRevenue để gán rank 1, 2, 3...
        list.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRank(i + 1);
        }

        return list;
    }

    private List<BiTrendPointDto> calculateTrends(
            List<Hotel> targetHotels, Map<Short, List<RoomInstance>> roomsByHotel,
            List<BookingDailyRate> dailyRates, List<ServiceOrder> serviceOrders,
            LocalDate startDate, LocalDate endDate
    ) {
        int totalRooms = targetHotels.stream()
                .mapToInt(h -> roomsByHotel.getOrDefault(h.getId(), Collections.emptyList()).size())
                .sum();

        List<BiTrendPointDto> list = new ArrayList<>();
        LocalDate cur = startDate;

        while (!cur.isAfter(endDate)) {
            LocalDate d = cur;
            List<BookingDailyRate> dayRates = dailyRates.stream()
                    .filter(r -> r.getStayDate().isEqual(d))
                    .toList();

            BigDecimal dayRoomRev = dayRates.stream()
                    .map(BookingDailyRate::getNetPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int dayOccupied = dayRates.size();

            OffsetDateTime dayStart = d.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime dayEnd = d.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

            List<ServiceOrder> dayOrders = serviceOrders.stream()
                    .filter(so -> so.getIssuedAt() != null && !so.getIssuedAt().isBefore(dayStart) && !so.getIssuedAt().isAfter(dayEnd))
                    .toList();

            BigDecimal dayFnbRev = dayOrders.stream()
                    .map(ServiceOrder::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal dayTotal = dayRoomRev.add(dayFnbRev);

            double occRate = totalRooms > 0
                    ? Math.round((dayOccupied * 100.0 / totalRooms) * 10.0) / 10.0
                    : 0.0;

            BigDecimal adr = dayOccupied > 0
                    ? dayRoomRev.divide(BigDecimal.valueOf(dayOccupied), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal revPar = totalRooms > 0
                    ? dayRoomRev.divide(BigDecimal.valueOf(totalRooms), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            list.add(BiTrendPointDto.builder()
                    .date(d)
                    .periodLabel(d.toString())
                    .occupancyRate(occRate)
                    .adr(adr)
                    .revPar(revPar)
                    .roomRevenue(dayRoomRev)
                    .fnbRevenue(dayFnbRev)
                    .otherRevenue(BigDecimal.ZERO)
                    .totalRevenue(dayTotal)
                    .build());

            cur = cur.plusDays(1);
        }

        return list;
    }

    private ExecutiveBiReportResponse buildEmptyResponse(String scope, String scopeName, LocalDate startDate, LocalDate endDate) {
        return ExecutiveBiReportResponse.builder()
                .scope(scope)
                .scopeName(scopeName)
                .startDate(startDate)
                .endDate(endDate)
                .isProvisional(false)
                .summary(ExecutiveKpiSummaryDto.builder()
                        .occupancyRate(0.0)
                        .averageDailyRate(BigDecimal.ZERO)
                        .revPar(BigDecimal.ZERO)
                        .totalActiveRooms(0)
                        .occupiedRoomNights(0)
                        .availableRoomNights(0L)
                        .totalRoomRevenue(BigDecimal.ZERO)
                        .totalFnbRevenue(BigDecimal.ZERO)
                        .totalOtherRevenue(BigDecimal.ZERO)
                        .totalRevenue(BigDecimal.ZERO)
                        .build())
                .departmentRevenue(DepartmentRevenueBreakdownDto.builder().build())
                .segmentRevenue(SegmentRevenueBreakdownDto.builder().build())
                .regionBreakdowns(Collections.emptyList())
                .hotelBreakdowns(Collections.emptyList())
                .trends(Collections.emptyList())
                .build();
    }
}
