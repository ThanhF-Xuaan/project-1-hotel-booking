package vn.edu.utc.hotel_booking.modules.dashboard.service;

import vn.edu.utc.hotel_booking.modules.dashboard.dto.request.ExecutiveReportFilterDto;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.ExecutiveBiReportResponse;
import vn.edu.utc.hotel_booking.modules.dashboard.dto.response.ExecutiveKpiSummaryDto;

import java.time.LocalDate;

public interface ExecutiveReportService {

    ExecutiveBiReportResponse generateExecutiveReport(ExecutiveReportFilterDto filter);

    ExecutiveKpiSummaryDto getQuickSummary(String scope, Short regionId, Short hotelId, LocalDate targetDate);
}
