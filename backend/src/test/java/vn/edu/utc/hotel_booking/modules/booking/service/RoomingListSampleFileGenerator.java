package vn.edu.utc.hotel_booking.modules.booking.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomingListSampleFileGenerator {

    private static final String TARGET_DIR = "../docs/test-data/U12/";

    private static final String[] HEADERS = {
            "Full Name", "Gender", "Date of Birth", "Nationality", "ID Number", "Phone", "Email", "Room Number", "Note"
    };

    @Test
    void generateAllSampleFiles() throws IOException {
        File dir = new File(TARGET_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            assertThat(created || dir.exists()).isTrue();
        }

        generateValid20File(new File(dir, "rooming-list-valid-20.xlsx"));
        generateVacantRoomsFile(new File(dir, "rooming-list-vacant-rooms.xlsx"));
        generateVacantRoomsFile(new File(dir, "rooming-list-3-phong-trong.xlsx"));
        generateSuiteVacantRoomsFile(new File(dir, "rooming-list-suite-rooms.xlsx"));
        generateTemplateFile(new File(dir, "rooming-list-template.xlsx"));
        generateInvalidRowsFile(new File(dir, "rooming-list-invalid-rows.xlsx"));
        generateInvalidStructureFile(new File(dir, "rooming-list-invalid-structure.xlsx"));
    }

    private void generateTemplateFile(File targetFile) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            Sheet sheet = wb.createSheet("RoomingList");
            CellStyle headerStyle = createHeaderStyle(wb);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dòng 1 mẫu gợi ý
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("Nguyễn Văn Mẫu");
            sampleRow.createCell(1).setCellValue("Nam");
            sampleRow.createCell(2).setCellValue("15/08/1990");
            sampleRow.createCell(3).setCellValue("Việt Nam");
            sampleRow.createCell(4).setCellValue("001090001234");
            sampleRow.createCell(5).setCellValue("0912345678");
            sampleRow.createCell(6).setCellValue("mau.nguyen@example.com");
            sampleRow.createCell(7).setCellValue("101 - STD");
            sampleRow.createCell(8).setCellValue("Khách mẫu (có thể xóa dòng này để nhập khách của bạn)");

            // Tạo các dòng trống để người dùng nhập tiếp
            for (int r = 2; r <= 15; r++) {
                Row emptyRow = sheet.createRow(r);
                for (int c = 0; c < HEADERS.length; c++) {
                    emptyRow.createCell(c).setCellValue("");
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(fos);
        }
    }

    private void generateVacantRoomsFile(File targetFile) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            Sheet sheet = wb.createSheet("RoomingList");
            CellStyle headerStyle = createHeaderStyle(wb);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Gán vào 3 phòng trống (102, 103, 105), mỗi phòng đúng 2 người (đúng theo sức chứa tối đa của phòng Deluxe)
            List<String[]> data = List.<String[]>of(
                    // Phòng 102 (Trống, tối đa 2 người lớn)
                    new String[]{"Nguyễn Văn An", "Nam", "12/03/1988", "Việt Nam", "001088001234", "0912345601", "an.nguyen@example.com", "102", "Khách đại diện phòng 102"},
                    new String[]{"Trần Thị Bích", "Nữ", "25/07/1990", "Việt Nam", "001190002345", "0912345602", "bich.tran@example.com", "102", "Thành viên đoàn"},
                    // Phòng 103 (Trống, tối đa 2 người lớn)
                    new String[]{"Lê Hoàng Cường", "Nam", "05/11/1985", "Việt Nam", "001085003456", "0912345603", "cuong.le@example.com", "103", "Khách phòng 103"},
                    new String[]{"Phạm Thu Dung", "Nữ", "19/09/1992", "Việt Nam", "001192004567", "0912345604", "dung.pham@example.com", "103", "Thành viên đoàn"},
                    // Phòng 105 (Trống, tối đa 2 người lớn)
                    new String[]{"Hoàng Minh Đức", "Nam", "30/01/1987", "Việt Nam", "001087005678", "0912345605", "duc.hoang@example.com", "105", "Trưởng nhóm phòng 105"},
                    new String[]{"Vũ Mai Hương", "Nữ", "14/04/1994", "Việt Nam", "001194006789", "0912345606", "huong.vu@example.com", "105", "Thành viên đoàn"}
            );

            int rowIdx = 1;
            for (String[] rowData : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < rowData.length; c++) {
                    row.createCell(c).setCellValue(rowData[c]);
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(fos);
        }
    }

    private void generateSuiteVacantRoomsFile(File targetFile) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            Sheet sheet = wb.createSheet("RoomingList");
            CellStyle headerStyle = createHeaderStyle(wb);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Gán vào 3 phòng Suite trống (202, 203, 204), mỗi phòng 2 người
            List<String[]> data = List.<String[]>of(
                    // Phòng 202 (Trống, tối đa 3 người lớn)
                    new String[]{"Johnathan Smith", "Nam", "15/02/1984", "Hoa Kỳ", "B12345678", "0912345611", "john.smith@example.com", "202", "Chuyên gia nước ngoài"},
                    new String[]{"Emily Johnson", "Nữ", "20/09/1986", "Hoa Kỳ", "B23456789", "0912345612", "emily.j@example.com", "202", "Khách tham quan"},
                    // Phòng 203 (Trống, tối đa 3 người lớn)
                    new String[]{"Đỗ Hải Nam", "Nam", "17/06/1991", "Việt Nam", "001091009012", "0912345609", "nam.do@example.com", "203", "Đoàn công tác"},
                    new String[]{"Ngô Phương Nga", "Nữ", "03/05/1996", "Việt Nam", "001196010123", "0912345610", "nga.ngo@example.com", "203", "Đoàn công tác"},
                    // Phòng 204 (Trống, tối đa 3 người lớn)
                    new String[]{"Tanaka Kenji", "Nam", "10/12/1980", "Nhật Bản", "TZ8901234", "0912345619", "kenji.t@example.com", "204", "Khách đối tác"},
                    new String[]{"Sato Yuka", "Nữ", "05/06/1985", "Nhật Bản", "TZ9012345", "0912345620", "yuka.s@example.com", "204", "Khách đối tác"}
            );

            int rowIdx = 1;
            for (String[] rowData : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < rowData.length; c++) {
                    row.createCell(c).setCellValue(rowData[c]);
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(fos);
        }
    }

    private void generateValid20File(File targetFile) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            Sheet sheet = wb.createSheet("RoomingList");
            CellStyle headerStyle = createHeaderStyle(wb);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            List<String[]> data = List.<String[]>of(
                    new String[]{"Nguyễn Văn An", "Nam", "12/03/1988", "Việt Nam", "001088001234", "0912345601", "an.nguyen@example.com", "101", "Trưởng đoàn du lịch"},
                    new String[]{"Trần Thị Bích", "Nữ", "25/07/1990", "Việt Nam", "001190002345", "0912345602", "bich.tran@example.com", "101", "Thành viên đoàn"},
                    new String[]{"Lê Hoàng Cường", "Nam", "05/11/1985", "Việt Nam", "001085003456", "0912345603", "cuong.le@example.com", "102", "Khách VIP"},
                    new String[]{"Phạm Thu Dung", "Nữ", "19/09/1992", "Việt Nam", "001192004567", "0912345604", "dung.pham@example.com", "102", "Thành viên đoàn"},
                    new String[]{"Hoàng Minh Đức", "Nam", "30/01/1987", "Việt Nam", "001087005678", "0912345605", "duc.hoang@example.com", "103", "Phó đoàn"},
                    new String[]{"Vũ Mai Hương", "Nữ", "14/04/1994", "Việt Nam", "001194006789", "0912345606", "huong.vu@example.com", "103", "Thành viên đoàn"},
                    new String[]{"Đặng Quốc Khánh", "Nam", "22/08/1989", "Việt Nam", "001089007890", "0912345607", "khanh.dang@example.com", "201", "Khách hội nghị"},
                    new String[]{"Bùi Lan Linh", "Nữ", "08/12/1993", "Việt Nam", "001193008901", "0912345608", "linh.bui@example.com", "201", "Khách hội nghị"},
                    new String[]{"Đỗ Hải Nam", "Nam", "17/06/1991", "Việt Nam", "001091009012", "0912345609", "nam.do@example.com", "202", "Đoàn công tác"},
                    new String[]{"Ngô Phương Nga", "Nữ", "03/05/1996", "Việt Nam", "001196010123", "0912345610", "nga.ngo@example.com", "202", "Đoàn công tác"},
                    new String[]{"Johnathan Smith", "Nam", "15/02/1984", "Hoa Kỳ", "B12345678", "0912345611", "john.smith@example.com", "203", "Chuyên gia nước ngoài"},
                    new String[]{"Emily Johnson", "Nữ", "20/09/1986", "Hoa Kỳ", "B23456789", "0912345612", "emily.j@example.com", "203", "Khách tham quan"},
                    new String[]{"Trịnh Văn Phong", "Nam", "11/10/1983", "Việt Nam", "001083011234", "0912345613", "phong.trinh@example.com", "301", "Thành viên đoàn"},
                    new String[]{"Mai Thị Quyên", "Nữ", "28/03/1995", "Việt Nam", "001195012345", "0912345614", "quyen.mai@example.com", "301", "Thành viên đoàn"},
                    new String[]{"Phan Trọng Sơn", "Nam", "09/07/1989", "Việt Nam", "001089013456", "0912345615", "son.phan@example.com", "302", "Thành viên đoàn"},
                    new String[]{"Đinh Thanh Thảo", "Nữ", "16/11/1997", "Việt Nam", "001197014567", "0912345616", "thao.dinh@example.com", "302", "Thành viên đoàn"},
                    new String[]{"Lưu Quang Tuấn", "Nam", "04/04/1986", "Việt Nam", "001086015678", "0912345617", "tuan.luu@example.com", "303", "Thành viên đoàn"},
                    new String[]{"Dương Kim Uyên", "Nữ", "21/08/1992", "Việt Nam", "001192016789", "0912345618", "uyen.duong@example.com", "303", "Thành viên đoàn"},
                    new String[]{"Tanaka Kenji", "Nam", "10/12/1980", "Nhật Bản", "TZ8901234", "0912345619", "kenji.t@example.com", "304", "Khách đối tác"},
                    new String[]{"Sato Yuka", "Nữ", "05/06/1985", "Nhật Bản", "TZ9012345", "0912345620", "yuka.s@example.com", "304", "Khách đối tác"}
            );

            int rowIdx = 1;
            for (String[] rowData : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < rowData.length; c++) {
                    row.createCell(c).setCellValue(rowData[c]);
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(fos);
        }
    }

    private void generateInvalidRowsFile(File targetFile) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            Sheet sheet = wb.createSheet("RoomingList");
            CellStyle headerStyle = createHeaderStyle(wb);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            List<String[]> data = List.<String[]>of(
                    // Dòng 2: Hợp lệ
                    new String[]{"Nguyễn Văn Hợp Lệ", "Nam", "12/03/1988", "Việt Nam", "001088099001", "0912345601", "hople1@example.com", "101", "Dòng hợp lệ"},
                    // Dòng 3: Thiếu họ tên
                    new String[]{"", "Nam", "25/07/1990", "Việt Nam", "001190099002", "0912345602", "thieuten@example.com", "101", "Lỗi: thiếu họ tên"},
                    // Dòng 4: Email sai định dạng
                    new String[]{"Trần Văn Ba", "Nam", "05/11/1985", "Việt Nam", "001085099003", "0912345603", "email_sai_dinh_dang", "102", "Lỗi: email sai format"},
                    // Dòng 5: Ngày sinh không hợp lệ (31/02)
                    new String[]{"Lê Thị Bốn", "Nữ", "31/02/1995", "Việt Nam", "001195099004", "0912345604", "bon.le@example.com", "102", "Lỗi: ngày sinh không tồn tại"},
                    // Dòng 6: Hợp lệ
                    new String[]{"Phạm Văn Năm", "Nam", "30/01/1987", "Việt Nam", "001087099005", "0912345605", "nam.pham@example.com", "103", "Dòng hợp lệ"},
                    // Dòng 7: Giới tính không được hỗ trợ
                    new String[]{"Hoàng Thị Sáu", "KhongXacDinh", "14/04/1994", "Việt Nam", "001194099006", "0912345606", "sau.hoang@example.com", "103", "Lỗi: giới tính không hỗ trợ"},
                    // Dòng 8: Số điện thoại sai định dạng
                    new String[]{"Vũ Văn Bảy", "Nam", "22/08/1989", "Việt Nam", "001089099007", "012345", "bay.vu@example.com", "201", "Lỗi: SĐT quá ngắn"},
                    // Dòng 9: Ngày sinh ở tương lai
                    new String[]{"Đỗ Thị Tám", "Nữ", "15/05/2099", "Việt Nam", "001199099008", "0912345608", "tam.do@example.com", "201", "Lỗi: ngày sinh tương lai"},
                    // Dòng 10: Hợp lệ
                    new String[]{"Bùi Văn Chín", "Nam", "17/06/1991", "Việt Nam", "001091099009", "0912345609", "chin.bui@example.com", "202", "Dòng hợp lệ (CCCD gốc)"},
                    // Dòng 11: Trùng số giấy tờ với dòng 10 trong cùng file
                    new String[]{"Ngô Thị Mười", "Nữ", "03/05/1996", "Việt Nam", "001091099009", "0912345610", "muoi.ngo@example.com", "202", "Lỗi: trùng số giấy tờ với dòng 10"},
                    // Dòng 12: Hợp lệ
                    new String[]{"Đinh Văn Mười Một", "Nam", "10/10/1992", "Việt Nam", "001092099011", "0912345611", "muoimot@example.com", "203", "Dòng hợp lệ"}
            );

            int rowIdx = 1;
            for (String[] rowData : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < rowData.length; c++) {
                    row.createCell(c).setCellValue(rowData[c]);
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(fos);
        }
    }

    private void generateInvalidStructureFile(File targetFile) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            // Cố ý đặt sai tên sheet thành "DanhSachKhach" thay vì "RoomingList"
            Sheet sheet = wb.createSheet("DanhSachKhach");
            CellStyle headerStyle = createHeaderStyle(wb);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Nguyễn Văn A");
            row1.createCell(1).setCellValue("Nam");
            row1.createCell(2).setCellValue("10/10/1990");
            row1.createCell(3).setCellValue("Việt Nam");

            wb.write(fos);
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}
