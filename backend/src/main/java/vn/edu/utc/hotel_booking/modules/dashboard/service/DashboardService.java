package vn.edu.utc.hotel_booking.modules.dashboard.service;

import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.DashboardKpiResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.OccupancyTrendDto;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.RevenueTrendDto;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

    DashboardKpiResponse getExecutiveKpis(Short hotelId, LocalDate targetDate);

    List<RevenueTrendDto> getRevenueTrends(Short hotelId, LocalDate startDate, LocalDate endDate);

    List<OccupancyTrendDto> getOccupancyTrends(Short hotelId, LocalDate startDate, LocalDate endDate);
}
