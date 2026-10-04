package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingExportRequest;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingGuestCategory;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.LodgingExportService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LodgingExportServiceImpl implements LodgingExportService {

    private final LodgingQueueRepository lodgingQueueRepository;
    private final StaffRepository staffRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] HEADERS = {
            "STT",
            "Họ và tên",
            "Ngày tháng năm sinh",
            "Giới tính",
            "Quốc tịch",
            "Loại giấy tờ",
            "Số giấy tờ",
            "Nơi ĐKTT / Nơi cấp",
            "Chỗ ở hiện nay",
            "Ngày đến",
            "Ngày đi (dự kiến)",
            "Số phòng",
            "Lý do lưu trú"
    };

    @Override
    @Transactional
    public byte[] exportBcaLodgingReport(LodgingExportRequest request) {
        validateStaffScope(request.getHotelId());

        int cutOffHour = request.getCutOffHour() != null ? request.getCutOffHour() : 23;
        OffsetDateTime cutoffTime = request.getDate().atTime(cutOffHour, 59, 59).atOffset(ZoneOffset.ofHours(7));

        boolean isVietnamese = (request.getCategory() == LodgingGuestCategory.VIETNAMESE);
        List<LodgingQueue> queueItems = lodgingQueueRepository.findPendingForExport(
                request.getHotelId(),
                cutoffTime,
                isVietnamese
        );

        if (queueItems.isEmpty()) {
            throw new AppException(ErrorCode.LODGING_EXPORT_EMPTY, "Không có dữ liệu khách lưu trú phù hợp để xuất file khai báo");
        }

        // Tạo Excel dạng Streaming để tránh tràn bộ nhớ với tệp lớn (OOM Prevention)
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            String sheetName = isVietnamese ? "Thong_Bao_Luu_Tru_VN" : "Khai_Bao_Tam_Tru_NN";
            SXSSFSheet sheet = workbook.createSheet(sheetName);

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Dòng tiêu đề
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            int stt = 1;
            for (LodgingQueue item : queueItems) {
                StayGuest g = item.getStayGuest();
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(g.getFullName() != null ? g.getFullName().toUpperCase() : "");
                row.createCell(2).setCellValue(g.getDateOfBirth() != null ? g.getDateOfBirth().format(DATE_FORMATTER) : "");
                row.createCell(3).setCellValue(formatGender(g.getGender()));
                row.createCell(4).setCellValue(g.getNationality() != null ? g.getNationality() : "");
                row.createCell(5).setCellValue(g.getDocumentType() != null ? g.getDocumentType().name() : "");
                row.createCell(6).setCellValue(g.getDocumentNumber() != null ? g.getDocumentNumber() : "");
                row.createCell(7).setCellValue(g.getPermanentAddress() != null ? g.getPermanentAddress() : "");
                row.createCell(8).setCellValue(g.getCurrentAddress() != null ? g.getCurrentAddress() : "");
                row.createCell(9).setCellValue(g.getCheckInTime() != null ? g.getCheckInTime().format(DATE_TIME_FORMATTER) : "");
                row.createCell(10).setCellValue(g.getExpectedCheckOutTime() != null ? g.getExpectedCheckOutTime().format(DATE_TIME_FORMATTER) : "");
                row.createCell(11).setCellValue(g.getRoomNumber() != null ? g.getRoomNumber() : "");
                row.createCell(12).setCellValue(g.getReasonForStay() != null ? g.getReasonForStay() : "");
            }

            workbook.write(bos);
            workbook.dispose(); // Dọn dẹp tệp tạm thời trên đĩa

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

    private String formatGender(Gender gender) {
        if (gender == null) return "Khác";
        return switch (gender) {
            case MALE -> "Nam";
            case FEMALE -> "Nữ";
            default -> "Khác";
        };
    }

    private void validateStaffScope(Short hotelId) {
        if (SecurityUtils.hasRole("ROLE_CHAIN_ADMIN") || SecurityUtils.hasRole("ROLE_PROPERTY_MANAGER")) {
            return;
        }

        if (SecurityUtils.hasRole("ROLE_RECEPTIONIST")) {
            Staff currentStaff = resolveCurrentStaff();
            if ("PROPERTY".equalsIgnoreCase(currentStaff.getScopeType())
                    && currentStaff.getScopeEntityId() != null
                    && !currentStaff.getScopeEntityId().equals(hotelId.intValue())) {
                throw new AppException(ErrorCode.UNAUTHORIZED, "Lễ tân không có quyền xuất báo cáo trên khách sạn khác");
            }
        }
    }

    private Staff resolveCurrentStaff() {
        return SecurityUtils.getCurrentUserKeycloakId()
                .flatMap(staffRepository::findByKeycloakIdAndIsDeletedFalse)
                .or(() -> SecurityUtils.getCurrentUsername()
                        .flatMap(staffRepository::findByUsernameAndIsDeletedFalse))
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
    }
}
