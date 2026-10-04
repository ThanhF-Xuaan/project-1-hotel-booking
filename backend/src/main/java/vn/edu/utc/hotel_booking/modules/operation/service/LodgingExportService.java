package vn.edu.utc.hotel_booking.modules.operation.service;

import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingExportRequest;

public interface LodgingExportService {

    byte[] exportBcaLodgingReport(LodgingExportRequest request);
}
