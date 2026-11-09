package vn.edu.utc.hotel_booking.modules.booking.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContractStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlock;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Xuất dữ liệu danh sách hợp đồng khách đoàn và phân bổ block phòng ra định dạng Excel (.xlsx).
 */
@Component
public class GroupContractWorkbookWriter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneOffset REPORT_OFFSET = ZoneOffset.ofHours(7);

    private static final String[] HEADERS_CONTRACTS = {
            "STT",
            "Mã hợp đồng",
            "Tên đoàn",
            "Công ty / Doanh nghiệp",
            "Mã số thuế",
            "Khách sạn",
            "Nhân viên phụ trách",
            "Ngày nhận phòng",
            "Ngày trả phòng",
            "Số đêm",
            "Số phòng",
            "Số khách",
            "Tiền phòng (VNĐ)",
            "Phí dịch vụ (VNĐ)",
            "Thuế VAT (VNĐ)",
            "Tổng giá trị HĐ (VNĐ)",
            "Tỷ lệ cọc (%)",
            "Tiền cọc yêu cầu (VNĐ)",
            "Tiền cọc thực nhận (VNĐ)",
            "Hạn đặt cọc",
            "Ngày nộp cọc",
            "Hạn chốt (Cut-off)",
            "Hạn thanh toán",
            "Trạng thái",
            "Ngày tạo"
    };

    private static final String[] HEADERS_ROOM_BLOCKS = {
            "STT",
            "Mã hợp đồng",
            "Tên đoàn",
            "Khách sạn",
            "Loại phòng",
            "Số phòng block",
            "Số phòng đã gán khách",
            "Giá thỏa thuận / đêm (VNĐ)",
            "Trạng thái block"
    };

    public void write(List<GroupContract> contracts, OutputStream output) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            DataFormat dataFormat = workbook.createDataFormat();
            short currencyFormat = dataFormat.getFormat("#,##0");
            short integerFormat = dataFormat.getFormat("#,##0");
            short percentFormat = dataFormat.getFormat("0.00\"%\"");

            // Styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subTitleStyle = createSubTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createBorderedStyle(workbook, HorizontalAlignment.LEFT);
            CellStyle centerStyle = createBorderedStyle(workbook, HorizontalAlignment.CENTER);

            CellStyle numberStyle = createBorderedStyle(workbook, HorizontalAlignment.RIGHT);
            numberStyle.setDataFormat(integerFormat);

            CellStyle currencyStyle = createBorderedStyle(workbook, HorizontalAlignment.RIGHT);
            currencyStyle.setDataFormat(currencyFormat);

            CellStyle percentStyle = createBorderedStyle(workbook, HorizontalAlignment.RIGHT);
            percentStyle.setDataFormat(percentFormat);

            CellStyle summaryLabelStyle = createSummaryStyle(workbook, HorizontalAlignment.CENTER);
            CellStyle summaryCurrencyStyle = createSummaryStyle(workbook, HorizontalAlignment.RIGHT);
            summaryCurrencyStyle.setDataFormat(currencyFormat);
            CellStyle summaryNumberStyle = createSummaryStyle(workbook, HorizontalAlignment.RIGHT);
            summaryNumberStyle.setDataFormat(integerFormat);

            // --- Sheet 1: Danh sách hợp đồng ---
            SXSSFSheet sheet1 = workbook.createSheet("Danh_Sach_Hop_Dong");
            sheet1.trackAllColumnsForAutoSizing();

            // Row 0: Title
            Row titleRow = sheet1.createRow(0);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DANH SÁCH HỢP ĐỒNG KHÁCH ĐOÀN");
            titleCell.setCellStyle(titleStyle);
            sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS_CONTRACTS.length - 1));

            // Row 1: Subtitle / Export Time
            Row subTitleRow = sheet1.createRow(1);
            Cell subTitleCell = subTitleRow.createCell(0);
            subTitleCell.setCellValue("Thời gian xuất báo cáo: " + OffsetDateTime.now(REPORT_OFFSET).format(DATE_TIME_FORMATTER)
                    + "  |  Tổng số hợp đồng: " + (contracts != null ? contracts.size() : 0));
            subTitleCell.setCellStyle(subTitleStyle);
            sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, HEADERS_CONTRACTS.length - 1));

            // Row 3: Headers
            Row headerRow = sheet1.createRow(3);
            headerRow.setHeightInPoints(25);
            for (int i = 0; i < HEADERS_CONTRACTS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS_CONTRACTS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIndex = 4;
            int stt = 1;

            BigDecimal totalSubtotal = BigDecimal.ZERO;
            BigDecimal totalServiceFee = BigDecimal.ZERO;
            BigDecimal totalVat = BigDecimal.ZERO;
            BigDecimal grandTotal = BigDecimal.ZERO;
            BigDecimal totalDepositRequired = BigDecimal.ZERO;
            BigDecimal totalDepositPaid = BigDecimal.ZERO;
            long totalRooms = 0;
            long totalGuests = 0;

            if (contracts != null && !contracts.isEmpty()) {
                for (GroupContract contract : contracts) {
                    Row row = sheet1.createRow(rowIndex++);
                    long nights = 1;
                    if (contract.getCheckInDate() != null && contract.getCheckOutDate() != null) {
                        nights = ChronoUnit.DAYS.between(contract.getCheckInDate(), contract.getCheckOutDate());
                        if (nights <= 0) nights = 1;
                    }

                    // 0. STT
                    createCell(row, 0, stt++, centerStyle);
                    // 1. Mã HĐ
                    createCell(row, 1, contract.getContractNumber() != null ? contract.getContractNumber() : "", centerStyle);
                    // 2. Tên đoàn
                    createCell(row, 2, contract.getGroupName() != null ? contract.getGroupName() : "", textStyle);
                    // 3. Tên công ty
                    createCell(row, 3, contract.getCompany() != null ? contract.getCompany().getName() : "", textStyle);
                    // 4. Mã số thuế
                    createCell(row, 4, contract.getCompanyTaxCode() != null ? contract.getCompanyTaxCode() : "", centerStyle);
                    // 5. Khách sạn
                    createCell(row, 5, contract.getHotel() != null ? contract.getHotel().getName() : "", textStyle);
                    // 6. Nhân viên Sales
                    createCell(row, 6, contract.getSalesStaff() != null ? contract.getSalesStaff().getFullName() : "", textStyle);
                    // 7. Check-in
                    createCell(row, 7, formatDate(contract.getCheckInDate()), centerStyle);
                    // 8. Check-out
                    createCell(row, 8, formatDate(contract.getCheckOutDate()), centerStyle);
                    // 9. Số đêm
                    createCell(row, 9, nights, numberStyle);
                    // 10. Số phòng
                    int rooms = contract.getTotalRooms() != null ? contract.getTotalRooms() : 0;
                    totalRooms += rooms;
                    createCell(row, 10, rooms, numberStyle);
                    // 11. Số khách
                    int guests = contract.getTotalGuests() != null ? contract.getTotalGuests() : 0;
                    totalGuests += guests;
                    createCell(row, 11, guests, numberStyle);
                    // 12. Tiền phòng
                    BigDecimal subtotal = contract.getSubtotalAmount() != null ? contract.getSubtotalAmount() : BigDecimal.ZERO;
                    totalSubtotal = totalSubtotal.add(subtotal);
                    createCell(row, 12, subtotal.doubleValue(), currencyStyle);
                    // 13. Phí dịch vụ
                    BigDecimal serviceFee = contract.getServiceFeeAmount() != null ? contract.getServiceFeeAmount() : BigDecimal.ZERO;
                    totalServiceFee = totalServiceFee.add(serviceFee);
                    createCell(row, 13, serviceFee.doubleValue(), currencyStyle);
                    // 14. Thuế VAT
                    BigDecimal vat = contract.getVatAmount() != null ? contract.getVatAmount() : BigDecimal.ZERO;
                    totalVat = totalVat.add(vat);
                    createCell(row, 14, vat.doubleValue(), currencyStyle);
                    // 15. Tổng giá trị HĐ
                    BigDecimal contractVal = contract.getTotalContractValue() != null ? contract.getTotalContractValue() : BigDecimal.ZERO;
                    grandTotal = grandTotal.add(contractVal);
                    createCell(row, 15, contractVal.doubleValue(), currencyStyle);
                    // 16. Tỷ lệ cọc
                    double depRate = contract.getDepositRate() != null ? contract.getDepositRate().doubleValue() / 100.0 : 0.50;
                    createCell(row, 16, depRate, percentStyle);
                    // 17. Cọc yêu cầu
                    BigDecimal depAmount = contract.getDepositAmount() != null ? contract.getDepositAmount() : BigDecimal.ZERO;
                    totalDepositRequired = totalDepositRequired.add(depAmount);
                    createCell(row, 17, depAmount.doubleValue(), currencyStyle);
                    // 18. Cọc thực nhận
                    BigDecimal depPaid = contract.getDepositPaidAmount() != null ? contract.getDepositPaidAmount() : BigDecimal.ZERO;
                    totalDepositPaid = totalDepositPaid.add(depPaid);
                    createCell(row, 18, depPaid.doubleValue(), currencyStyle);
                    // 19. Hạn đặt cọc
                    createCell(row, 19, formatDateTime(contract.getDepositDueDate()), centerStyle);
                    // 20. Ngày nộp cọc
                    createCell(row, 20, formatDateTime(contract.getDepositPaidAt()), centerStyle);
                    // 21. Hạn Cut-off
                    createCell(row, 21, formatDateTime(contract.getCutoffDate()), centerStyle);
                    // 22. Hạn thanh toán
                    createCell(row, 22, formatDateTime(contract.getFinalPaymentDueDate()), centerStyle);
                    // 23. Trạng thái
                    createCell(row, 23, formatStatus(contract.getStatus()), centerStyle);
                    // 24. Ngày tạo
                    createCell(row, 24, formatDateTime(contract.getCreatedAt()), centerStyle);
                }
            }

            // Summary Row
            Row summaryRow = sheet1.createRow(rowIndex);
            summaryRow.setHeightInPoints(22);
            for (int i = 0; i < HEADERS_CONTRACTS.length; i++) {
                Cell cell = summaryRow.createCell(i);
                cell.setCellStyle(summaryLabelStyle);
            }
            Cell sumLabel = summaryRow.getCell(0);
            sumLabel.setCellValue("TỔNG CỘNG");
            sheet1.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 9));

            summaryRow.getCell(10).setCellValue(totalRooms);
            summaryRow.getCell(10).setCellStyle(summaryNumberStyle);

            summaryRow.getCell(11).setCellValue(totalGuests);
            summaryRow.getCell(11).setCellStyle(summaryNumberStyle);

            summaryRow.getCell(12).setCellValue(totalSubtotal.doubleValue());
            summaryRow.getCell(12).setCellStyle(summaryCurrencyStyle);

            summaryRow.getCell(13).setCellValue(totalServiceFee.doubleValue());
            summaryRow.getCell(13).setCellStyle(summaryCurrencyStyle);

            summaryRow.getCell(14).setCellValue(totalVat.doubleValue());
            summaryRow.getCell(14).setCellStyle(summaryCurrencyStyle);

            summaryRow.getCell(15).setCellValue(grandTotal.doubleValue());
            summaryRow.getCell(15).setCellStyle(summaryCurrencyStyle);

            summaryRow.getCell(17).setCellValue(totalDepositRequired.doubleValue());
            summaryRow.getCell(17).setCellStyle(summaryCurrencyStyle);

            summaryRow.getCell(18).setCellValue(totalDepositPaid.doubleValue());
            summaryRow.getCell(18).setCellStyle(summaryCurrencyStyle);

            // Auto-size columns with padding
            for (int i = 0; i < HEADERS_CONTRACTS.length; i++) {
                sheet1.autoSizeColumn(i);
                int currentWidth = sheet1.getColumnWidth(i);
                sheet1.setColumnWidth(i, Math.max(currentWidth + 1200, 3200));
            }

            // --- Sheet 2: Chi tiết Block phòng ---
            SXSSFSheet sheet2 = workbook.createSheet("Chi_Tiet_Block_Phong");
            sheet2.trackAllColumnsForAutoSizing();

            Row sheet2TitleRow = sheet2.createRow(0);
            sheet2TitleRow.setHeightInPoints(26);
            Cell s2TitleCell = sheet2TitleRow.createCell(0);
            s2TitleCell.setCellValue("CHI TIẾT PHÂN BỔ QUỸ PHÒNG ĐOÀN (ROOM BLOCKS)");
            s2TitleCell.setCellStyle(titleStyle);
            sheet2.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS_ROOM_BLOCKS.length - 1));

            Row sheet2HeaderRow = sheet2.createRow(2);
            sheet2HeaderRow.setHeightInPoints(25);
            for (int i = 0; i < HEADERS_ROOM_BLOCKS.length; i++) {
                Cell cell = sheet2HeaderRow.createCell(i);
                cell.setCellValue(HEADERS_ROOM_BLOCKS[i]);
                cell.setCellStyle(headerStyle);
            }

            int blockRowIndex = 3;
            int blockStt = 1;
            if (contracts != null) {
                for (GroupContract contract : contracts) {
                    if (contract.getRoomBlocks() == null || contract.getRoomBlocks().isEmpty()) continue;
                    for (GroupRoomBlock block : contract.getRoomBlocks()) {
                        Row row = sheet2.createRow(blockRowIndex++);
                        createCell(row, 0, blockStt++, centerStyle);
                        createCell(row, 1, contract.getContractNumber() != null ? contract.getContractNumber() : "", centerStyle);
                        createCell(row, 2, contract.getGroupName() != null ? contract.getGroupName() : "", textStyle);
                        createCell(row, 3, contract.getHotel() != null ? contract.getHotel().getName() : "", textStyle);
                        String roomTypeName = (block.getHotelRoomType() != null && block.getHotelRoomType().getRoomType() != null)
                                ? block.getHotelRoomType().getRoomType().getName() : "Phòng #" + block.getId();
                        createCell(row, 4, roomTypeName, textStyle);
                        createCell(row, 5, block.getTotalBlockedRooms() != null ? block.getTotalBlockedRooms() : 0, numberStyle);
                        createCell(row, 6, block.getAllocatedRooms() != null ? block.getAllocatedRooms() : 0, numberStyle);
                        createCell(row, 7, block.getAgreedRate() != null ? block.getAgreedRate().doubleValue() : 0, currencyStyle);
                        createCell(row, 8, block.getStatus() != null ? block.getStatus().name() : "", centerStyle);
                    }
                }
            }

            for (int i = 0; i < HEADERS_ROOM_BLOCKS.length; i++) {
                sheet2.autoSizeColumn(i);
                int currentWidth = sheet2.getColumnWidth(i);
                sheet2.setColumnWidth(i, Math.max(currentWidth + 1200, 3200));
            }

            workbook.write(output);
            workbook.dispose();
        }
    }

    private void createCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        if (style != null) cell.setCellStyle(style);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMATTER);
    }

    private String formatDateTime(OffsetDateTime dt) {
        return dt == null ? "" : dt.withOffsetSameInstant(REPORT_OFFSET).format(DATE_TIME_FORMATTER);
    }

    private String formatStatus(GroupContractStatus status) {
        if (status == null) return "";
        return switch (status) {
            case DRAFT -> "Nháp / Báo giá";
            case TENTATIVE -> "Giữ chỗ 3 ngày";
            case DEPOSIT_PAID -> "Đã cọc 50%";
            case ROOMING_LIST_ATTACHED -> "Đã nộp danh sách";
            case IN_HOUSE -> "Đang ở";
            case SETTLED -> "Đã quyết toán";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED_NO_DEPOSIT -> "Hủy (Quá hạn cọc)";
            case CANCELLED -> "Đã hủy";
        };
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 15);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private CellStyle createBorderedStyle(Workbook workbook, HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private CellStyle createSummaryStyle(Workbook workbook, HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void setBorders(CellStyle style, BorderStyle borderStyle) {
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
    }
}
