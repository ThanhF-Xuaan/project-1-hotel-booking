package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingExportRequest;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.export.LodgingWorkbookWriter;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingGuestCategory;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.LodgingExportService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LodgingExportServiceImpl implements LodgingExportService {

    private final LodgingQueueRepository lodgingQueueRepository;
    private final StaffRepository staffRepository;
    private final LodgingWorkbookWriter workbookWriter;

    private static final ZoneOffset REPORT_OFFSET = ZoneOffset.ofHours(7);

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public byte[] exportBcaLodgingReport(LodgingExportRequest request) {
        validateStaffScope(request.getHotelId());

        int cutOffHour = request.getCutOffHour() != null ? request.getCutOffHour() : 23;
        if (cutOffHour < 0 || cutOffHour > 23) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Giờ chốt dữ liệu phải từ 0 đến 23");
        }
        OffsetDateTime cutoffExclusive = request.getDate().atTime(cutOffHour, 0).plusHours(1).atOffset(REPORT_OFFSET);

        boolean isVietnamese = (request.getCategory() == LodgingGuestCategory.VIETNAMESE);
        List<LodgingQueue> queueItems = lodgingQueueRepository.findPendingForExport(
                request.getHotelId(),
                cutoffExclusive,
                isVietnamese
        );

        if (queueItems.isEmpty()) {
            throw new AppException(ErrorCode.LODGING_EXPORT_EMPTY, "Không có dữ liệu khách lưu trú phù hợp để xuất file khai báo");
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbookWriter.write(queueItems, isVietnamese, bos);

            // Cập nhật trạng thái các hàng chờ sang EXPORTED
            OffsetDateTime now = OffsetDateTime.now();
            String batchRef = "BCA-" + request.getDate() + "-" + UUID.randomUUID().toString().substring(0, 8);
            for (LodgingQueue item : queueItems) {
                item.setStatus(LodgingQueueStatus.EXPORTED);
                item.setExportedAt(now);
                item.setBatchReference(batchRef);
            }
            lodgingQueueRepository.saveAll(queueItems);

            return bos.toByteArray();

        } catch (IOException e) {
            log.error("Lỗi khi sinh file Excel khai báo lưu trú BCA", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi khi tạo file Excel xuất báo cáo");
        }
    }

    private void validateStaffScope(Short hotelId) {
        if (hotelId == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "hotelId không được để trống");
        }

        if (SecurityUtils.hasRole("ROLE_CHAIN_ADMIN")) {
            return;
        }

        if (SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER") || SecurityUtils.hasRole("ROLE_RECEPTIONIST")) {
            Staff currentStaff = resolveCurrentStaff();
            Integer scopeId = currentStaff.getScopeEntityId();
            if (!"PROPERTY".equalsIgnoreCase(currentStaff.getScopeType())
                    || scopeId == null || scopeId <= 0 || scopeId > Short.MAX_VALUE
                    || !scopeId.equals(hotelId.intValue())) {
                String roleTitle = SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER") ? "Quản lý khách sạn" : "Lễ tân";
                throw new AppException(ErrorCode.UNAUTHORIZED, roleTitle + " không có quyền xuất báo cáo trên khách sạn khác");
            }
            return;
        }

        throw new AppException(ErrorCode.UNAUTHORIZED, "Bạn không có quyền xuất báo cáo trên khách sạn này");
    }

    private Staff resolveCurrentStaff() {
        return SecurityUtils.getCurrentUserKeycloakId()
                .flatMap(staffRepository::findByKeycloakIdAndIsDeletedFalse)
                .or(() -> SecurityUtils.getCurrentUsername()
                        .flatMap(staffRepository::findByUsernameAndIsDeletedFalse))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
    }
}
