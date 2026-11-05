package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListErrorDto;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListPreviewResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListRowDto;
import vn.edu.utc.hotel_booking.modules.booking.service.ExcelParserService;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ExcelParserServiceImpl implements ExcelParserService {

    public static final String EXPECTED_SHEET_NAME = "RoomingList";
    public static final int MAX_ALLOWED_ROWS = 500;
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)[0-9]{9,10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    @Override
    public RoomingListPreviewResponse parseRoomingList(MultipartFile file) {
        validateFilePreconditions(file);

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = findRoomingListSheet(workbook);
            if (sheet == null) {
                throw new AppException(ErrorCode.EXCEL_INVALID_STRUCTURE,
                        "Cấu trúc file không hợp lệ: Không tìm thấy sheet dữ liệu có tên '" + EXPECTED_SHEET_NAME + "'");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null || headerRow.getPhysicalNumberOfCells() == 0) {
                throw new AppException(ErrorCode.EXCEL_INVALID_STRUCTURE,
                        "Cấu trúc file không hợp lệ: Không tìm thấy hàng tiêu đề (Header row)");
            }

            Map<String, Integer> colMap = buildColumnMapping(headerRow);
            if (!colMap.containsKey("fullName")) {
                throw new AppException(ErrorCode.EXCEL_INVALID_STRUCTURE,
                        "Cấu trúc file không hợp lệ: Thiếu cột bắt buộc 'Full Name' (Họ và tên)");
            }

            List<RoomingListRowDto> rowDtos = new ArrayList<>();
            List<RoomingListErrorDto> allErrors = new ArrayList<>();
            Map<String, Integer> seenIdNumbers = new HashMap<>();

            int lastRow = sheet.getLastRowNum();
            int totalDataRows = 0;

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowCompletelyEmpty(row)) {
                    continue;
                }
                totalDataRows++;

                if (totalDataRows > MAX_ALLOWED_ROWS) {
                    throw new AppException(ErrorCode.EXCEL_MAX_ROWS_EXCEEDED,
                            "Số lượng dòng dữ liệu vượt quá giới hạn tối đa cho phép là " + MAX_ALLOWED_ROWS + " dòng");
                }

                int excelRowNum = r + 1; // 1-based index (header is row 1)
                RoomingListRowDto rowDto = parseAndValidateRow(row, excelRowNum, colMap, seenIdNumbers, allErrors);
                rowDtos.add(rowDto);
            }

            if (totalDataRows == 0) {
                throw new AppException(ErrorCode.EXCEL_EMPTY_FILE, "File Excel không chứa dòng dữ liệu nào");
            }

            int validCount = (int) rowDtos.stream().filter(RoomingListRowDto::getIsValid).count();
            int errorCount = rowDtos.size() - validCount;

            return RoomingListPreviewResponse.builder()
                    .totalRows(rowDtos.size())
                    .validCount(validCount)
                    .errorCount(errorCount)
                    .rows(rowDtos)
                    .errors(allErrors)
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi đọc file Excel: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.EXCEL_INVALID_FILE, "Không thể đọc nội dung file Excel: " + e.getMessage());
        }
    }

    private void validateFilePreconditions(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.EXCEL_EMPTY_FILE, "File tải lên rỗng");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(ErrorCode.EXCEL_FILE_SIZE_EXCEEDED,
                    "Dung lượng file (" + (file.getSize() / 1024 / 1024) + "MB) vượt quá giới hạn tối đa 10MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            throw new AppException(ErrorCode.EXCEL_INVALID_FILE,
                    "File không đúng định dạng. Hệ thống chỉ hỗ trợ tệp Excel (.xlsx)");
        }
    }

    private Sheet findRoomingListSheet(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (EXPECTED_SHEET_NAME.equalsIgnoreCase(sheet.getSheetName().trim())) {
                return sheet;
            }
        }
        return null;
    }

    private Map<String, Integer> buildColumnMapping(Row headerRow) {
        Map<String, Integer> colMap = new HashMap<>();
        DataFormatter formatter = new DataFormatter();

        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String headerText = formatter.formatCellValue(cell).trim().toLowerCase();
            String normalized = normalizeHeader(headerText);

            if (matchesFullName(normalized)) {
                colMap.put("fullName", c);
            } else if (matchesGender(normalized)) {
                colMap.put("gender", c);
            } else if (matchesDob(normalized)) {
                colMap.put("dateOfBirth", c);
            } else if (matchesNationality(normalized)) {
                colMap.put("nationality", c);
            } else if (matchesIdNumber(normalized)) {
                colMap.put("idNumber", c);
            } else if (matchesPhone(normalized)) {
                colMap.put("phone", c);
            } else if (matchesEmail(normalized)) {
                colMap.put("email", c);
            } else if (matchesRoomNumber(normalized)) {
                colMap.put("roomNumber", c);
            } else if (matchesNote(normalized)) {
                colMap.put("note", c);
            }
        }
        return colMap;
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        String temp = Normalizer.normalize(header, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private boolean matchesFullName(String s) {
        return s.contains("fullname") || s.contains("hovaten") || s.contains("hoten") || s.contains("name") || s.contains("tenkhach");
    }

    private boolean matchesGender(String s) {
        return s.contains("gender") || s.contains("gioitinh") || s.contains("sex");
    }

    private boolean matchesDob(String s) {
        return s.contains("dateofbirth") || s.contains("ngaysinh") || s.contains("birthdate") || s.contains("dob") || s.contains("namsinh");
    }

    private boolean matchesNationality(String s) {
        return s.contains("nationality") || s.contains("quoctich");
    }

    private boolean matchesIdNumber(String s) {
        return s.contains("idnumber") || s.contains("sogiayto") || s.contains("cccd") || s.contains("cmnd") || s.contains("passport") || s.contains("dinhdanh");
    }

    private boolean matchesPhone(String s) {
        return s.contains("phone") || s.contains("sodienthoai") || s.contains("dienthoai") || s.contains("sdt");
    }

    private boolean matchesEmail(String s) {
        return s.contains("email") || s.contains("thudientu");
    }

    private boolean matchesRoomNumber(String s) {
        return s.contains("roomnumber") || s.contains("sophong") || s.contains("phong") || s.contains("room");
    }

    private boolean matchesNote(String s) {
        return s.contains("note") || s.contains("ghichu") || s.contains("lydoluutru") || s.contains("notes");
    }

    private boolean isRowCompletelyEmpty(Row row) {
        DataFormatter formatter = new DataFormatter();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private RoomingListRowDto parseAndValidateRow(Row row,
                                                  int excelRowNum,
                                                  Map<String, Integer> colMap,
                                                  Map<String, Integer> seenIdNumbers,
                                                  List<RoomingListErrorDto> allErrors) {
        List<String> rowErrors = new ArrayList<>();

        // 1. Full Name (Bắt buộc)
        String fullName = getCellString(row, colMap.get("fullName"));
        if (fullName == null || fullName.trim().isEmpty()) {
            addError(excelRowNum, "Full Name", "Họ và tên khách không được để trống", rowErrors, allErrors);
        } else if (fullName.length() > 250) {
            addError(excelRowNum, "Full Name", "Họ và tên không được vượt quá 250 ký tự", rowErrors, allErrors);
        }

        // 2. Gender
        String genderRaw = getCellString(row, colMap.get("gender"));
        Gender gender = null;
        if (genderRaw != null && !genderRaw.trim().isEmpty()) {
            gender = parseGender(genderRaw.trim());
            if (gender == null) {
                addError(excelRowNum, "Gender", "Giới tính '" + genderRaw + "' không hợp lệ (hỗ trợ: Nam, Nữ, Khác hoặc MALE, FEMALE, OTHER)", rowErrors, allErrors);
            }
        }

        // 3. Date of Birth
        Cell dobCell = colMap.containsKey("dateOfBirth") ? row.getCell(colMap.get("dateOfBirth")) : null;
        LocalDate dateOfBirth = null;
        String dateOfBirthRaw = null;
        if (dobCell != null) {
            dateOfBirthRaw = getCellString(row, colMap.get("dateOfBirth"));
            dateOfBirth = parseDateOfBirth(dobCell, dateOfBirthRaw);
            if (dateOfBirthRaw != null && !dateOfBirthRaw.trim().isEmpty() && dateOfBirth == null) {
                addError(excelRowNum, "Date of Birth", "Ngày sinh '" + dateOfBirthRaw + "' không đúng định dạng (hợp lệ: dd/MM/yyyy hoặc yyyy-MM-dd)", rowErrors, allErrors);
            } else if (dateOfBirth != null) {
                if (dateOfBirth.isAfter(LocalDate.now())) {
                    addError(excelRowNum, "Date of Birth", "Ngày sinh không được ở tương lai (" + dateOfBirth + ")", rowErrors, allErrors);
                } else if (dateOfBirth.getYear() < 1900) {
                    addError(excelRowNum, "Date of Birth", "Năm sinh không hợp lệ (" + dateOfBirth.getYear() + ")", rowErrors, allErrors);
                }
            }
        }

        // 4. Nationality
        String nationality = getCellString(row, colMap.get("nationality"));
        if (nationality == null || nationality.trim().isEmpty()) {
            nationality = "Việt Nam";
        } else if (nationality.length() > 100) {
            addError(excelRowNum, "Nationality", "Quốc tịch không được vượt quá 100 ký tự", rowErrors, allErrors);
        }

        // 5. ID Number
        String idNumber = getCellString(row, colMap.get("idNumber"));
        DocumentType documentType = DocumentType.CCCD;
        if (idNumber != null && !idNumber.trim().isEmpty()) {
            idNumber = idNumber.replaceAll("\\s+", "");
            if (idNumber.length() < 6 || idNumber.length() > 50) {
                addError(excelRowNum, "ID Number", "Số giấy tờ phải từ 6 đến 50 ký tự", rowErrors, allErrors);
            } else {
                // Check duplicate within the file
                if (seenIdNumbers.containsKey(idNumber)) {
                    addError(excelRowNum, "ID Number", "Số giấy tờ '" + idNumber + "' bị trùng lặp trong file (đã xuất hiện tại dòng " + seenIdNumbers.get(idNumber) + ")", rowErrors, allErrors);
                } else {
                    seenIdNumbers.put(idNumber, excelRowNum);
                }
            }
            documentType = inferDocumentType(idNumber);
        }

        // 6. Phone
        String phone = getCellString(row, colMap.get("phone"));
        if (phone != null && !phone.trim().isEmpty()) {
            phone = phone.replaceAll("[\\s.-]", "");
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                addError(excelRowNum, "Phone", "Số điện thoại '" + phone + "' không đúng định dạng (VD: 0912345678)", rowErrors, allErrors);
            }
        }

        // 7. Email
        String email = getCellString(row, colMap.get("email"));
        if (email != null && !email.trim().isEmpty()) {
            email = email.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                addError(excelRowNum, "Email", "Email '" + email + "' không đúng định dạng (VD: example@domain.com)", rowErrors, allErrors);
            }
        }

        // 8. Room Number
        String roomNumber = getCellString(row, colMap.get("roomNumber"));
        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            roomNumber = roomNumber.trim();
            if (roomNumber.length() > 20) {
                addError(excelRowNum, "Room Number", "Số phòng không được vượt quá 20 ký tự", rowErrors, allErrors);
            }
        }

        // 9. Note
        String note = getCellString(row, colMap.get("note"));
        if (note != null && note.length() > 250) {
            addError(excelRowNum, "Note", "Ghi chú không được vượt quá 250 ký tự", rowErrors, allErrors);
        }

        boolean isValid = rowErrors.isEmpty();

        return RoomingListRowDto.builder()
                .rowNumber(excelRowNum)
                .fullName(fullName != null ? fullName.trim() : null)
                .gender(gender)
                .genderRaw(genderRaw)
                .dateOfBirth(dateOfBirth)
                .dateOfBirthRaw(dateOfBirthRaw)
                .nationality(nationality != null ? nationality.trim() : "Việt Nam")
                .documentType(documentType)
                .idNumber(idNumber)
                .phone(phone)
                .email(email)
                .roomNumber(roomNumber)
                .note(note != null ? note.trim() : null)
                .isValid(isValid)
                .errors(rowErrors)
                .build();
    }

    private String getCellString(Row row, Integer colIdx) {
        if (colIdx == null || colIdx < 0) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;

        DataFormatter formatter = new DataFormatter();
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        if (type == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return formatter.formatCellValue(cell).trim();
            }
            // Format number without scientific notation (e.g. 0912345678 or 001200001234)
            double val = cell.getNumericCellValue();
            if (val == Math.floor(val)) {
                return BigDecimal.valueOf(val).toBigInteger().toString();
            }
            return BigDecimal.valueOf(val).toPlainString();
        }

        String formatted = formatter.formatCellValue(cell);
        return formatted != null && !formatted.trim().isEmpty() ? formatted.trim() : null;
    }

    private Gender parseGender(String val) {
        String s = normalizeHeader(val);
        if (s.equals("nam") || s.equals("male") || s.equals("m")) {
            return Gender.MALE;
        }
        if (s.equals("nu") || s.equals("female") || s.equals("f")) {
            return Gender.FEMALE;
        }
        if (s.equals("khac") || s.equals("other") || s.equals("o")) {
            return Gender.OTHER;
        }
        return null;
    }

    private LocalDate parseDateOfBirth(Cell cell, String rawStr) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (rawStr == null || rawStr.trim().isEmpty()) {
            return null;
        }
        String clean = rawStr.trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(clean, fmt);
                // Round-trip check để phát hiện ngày không tồn tại (như 31/02 bị SMART resolver tự sửa)
                String formatted = date.format(fmt);
                if (clean.equals(formatted) || clean.replaceFirst("^0", "").equals(formatted.replaceFirst("^0", ""))) {
                    return date;
                }
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private DocumentType inferDocumentType(String idNumber) {
        if (idNumber == null || idNumber.isEmpty()) return DocumentType.CCCD;
        if (idNumber.matches("^\\d{9}$") || idNumber.matches("^\\d{12}$")) {
            return DocumentType.CCCD;
        }
        if (idNumber.matches("^[A-Za-z][0-9A-Za-z]{6,11}$")) {
            return DocumentType.PASSPORT;
        }
        return DocumentType.OTHER;
    }

    private void addError(int rowNum, String col, String msg, List<String> rowErrors, List<RoomingListErrorDto> allErrors) {
        rowErrors.add(msg);
        allErrors.add(RoomingListErrorDto.builder()
                .rowNumber(rowNum)
                .column(col)
                .error(msg)
                .build());
    }
}
