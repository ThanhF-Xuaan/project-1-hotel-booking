package vn.edu.utc.hotel_booking.modules.operation.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;

import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Generates the existing technical XLSX; owns workbook resources, not the caller's stream. */
@Component
public class LodgingWorkbookWriter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneOffset REPORT_OFFSET = ZoneOffset.ofHours(7);
    private static final String[] HEADERS = {
            "STT", "Họ và tên", "Ngày tháng năm sinh", "Giới tính", "Quốc tịch", "Loại giấy tờ", "Số giấy tờ",
            "Nơi ĐKTT / Nơi cấp", "Chỗ ở hiện nay", "Ngày đến", "Ngày đi (dự kiến)", "Số phòng", "Lý do lưu trú"
    };

    public void write(List<LodgingQueue> items, boolean vietnamese, OutputStream output) throws IOException {
        // POI 5.3 close() also disposes SXSSF spill files, including on write failure.
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            SXSSFSheet sheet = workbook.createSheet(vietnamese ? "Thong_Bao_Luu_Tru_VN" : "Khai_Bao_Tam_Tru_NN");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            Row header = sheet.createRow(0);
            for (int index = 0; index < HEADERS.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(HEADERS[index]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (LodgingQueue item : items) {
                StayGuest guest = item.getStayGuest();
                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(rowIndex++);
                row.createCell(1).setCellValue(guest.getFullName() != null ? guest.getFullName().toUpperCase(Locale.ROOT) : "");
                row.createCell(2).setCellValue(guest.getDateOfBirth() != null ? guest.getDateOfBirth().format(DATE_FORMATTER) : "");
                row.createCell(3).setCellValue(formatGender(guest.getGender()));
                row.createCell(4).setCellValue(guest.getNationality() != null ? guest.getNationality() : "");
                row.createCell(5).setCellValue(guest.getDocumentType() != null ? guest.getDocumentType().name() : "");
                row.createCell(6).setCellValue(guest.getDocumentNumber() != null ? guest.getDocumentNumber() : "");
                row.createCell(7).setCellValue(guest.getPermanentAddress() != null ? guest.getPermanentAddress() : "");
                row.createCell(8).setCellValue(guest.getCurrentAddress() != null ? guest.getCurrentAddress() : "");
                row.createCell(9).setCellValue(formatTime(guest.getCheckInTime()));
                row.createCell(10).setCellValue(formatTime(guest.getExpectedCheckOutTime()));
                row.createCell(11).setCellValue(guest.getRoomNumber() != null ? guest.getRoomNumber() : "");
                row.createCell(12).setCellValue(guest.getReasonForStay() != null ? guest.getReasonForStay() : "");
            }
            workbook.write(output);
        }
    }

    private String formatTime(OffsetDateTime time) {
        return time == null ? "" : time.withOffsetSameInstant(REPORT_OFFSET).format(DATE_TIME_FORMATTER);
    }

    private String formatGender(Gender gender) {
        if (gender == null) return "Khác";
        return switch (gender) {
            case MALE -> "Nam";
            case FEMALE -> "Nữ";
            default -> "Khác";
        };
    }
}
