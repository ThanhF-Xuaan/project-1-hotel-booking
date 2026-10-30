package vn.edu.utc.hotel_booking.modules.booking.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.RoomingListConfirmRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListImportResultResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomingListPreviewResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.booking.mapper.BookingMapper;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingDetailRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.BookingRoomRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.StayGuestRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingServiceImpl;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.ExcelParserServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;
import vn.edu.utc.hotel_booking.modules.operation.mapper.StayGuestMapper;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomingListImportBvaTest {

    private final ExcelParserService excelParserService = new ExcelParserServiceImpl();

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    @Mock
    private BookingRoomRepository bookingRoomRepository;
    @Mock
    private StayGuestRepository stayGuestRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private StayGuestMapper stayGuestMapper;

    private BookingServiceImpl bookingService;

    private Hotel sampleHotel;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository,
                bookingDetailRepository,
                bookingRoomRepository,
                stayGuestRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                bookingMapper,
                null,
                null,
                excelParserService,
                stayGuestMapper,
                null
        );

        sampleHotel = Hotel.builder().id((short) 1).name("Khách sạn Grand Hotel").build();
        sampleBooking = Booking.builder()
                .id(100L)
                .bookingNumber("BK-20261030-001")
                .hotel(sampleHotel)
                .bookingDetails(new ArrayList<>())
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(10L)
                .booking(sampleBooking)
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .bookingRooms(new ArrayList<>())
                .build();

        RoomInstance room101 = RoomInstance.builder().id(5).roomNumber("101").build();
        BookingRoom br1 = BookingRoom.builder().id(50L).bookingDetail(detail).roomInstance(room101).build();
        detail.getBookingRooms().add(br1);
        sampleBooking.getBookingDetails().add(detail);
    }

    private byte[] createWorkbookBytes(String sheetName, String[] headers, List<String[]> dataRows) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            if (headers != null) {
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
            }

            if (dataRows != null) {
                int rowIdx = 1;
                for (String[] rowData : dataRows) {
                    Row row = sheet.createRow(rowIdx++);
                    for (int i = 0; i < rowData.length; i++) {
                        row.createCell(i).setCellValue(rowData[i]);
                    }
                }
            }

            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private static final String[] STANDARD_HEADERS = {
            "Full Name", "Gender", "Date of Birth", "Nationality", "ID Number", "Phone", "Email", "Room Number", "Note"
    };

    @Test
    @DisplayName("BVA-01: File không có dòng dữ liệu (chỉ có header) -> Báo lỗi file rỗng")
    void testBva01_emptyDataRows() throws IOException {
        byte[] bytes = createWorkbookBytes("RoomingList", STANDARD_HEADERS, List.of());
        MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        assertThatThrownBy(() -> excelParserService.parseRoomingList(file))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_EMPTY_FILE);
    }

    @Test
    @DisplayName("BVA-02: File không có cột (header rỗng) -> Báo cấu trúc không hợp lệ")
    void testBva02_noColumns() throws IOException {
        byte[] bytes = createWorkbookBytes("RoomingList", new String[]{}, List.of());
        MockMultipartFile file = new MockMultipartFile("file", "no-columns.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        assertThatThrownBy(() -> excelParserService.parseRoomingList(file))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_INVALID_STRUCTURE);
    }

    @Test
    @DisplayName("BVA-03: File có 1 dòng hợp lệ -> Đọc và preview đúng 1 dòng")
    void testBva03_oneValidRow() throws IOException {
        List<String[]> data = List.<String[]>of(
                new String[]{"Nguyễn Văn A", "Nam", "15/05/1990", "Việt Nam", "001200001234", "0912345678", "nguyenvana@gmail.com", "101", "Ghi chú"}
        );
        byte[] bytes = createWorkbookBytes("RoomingList", STANDARD_HEADERS, data);
        MockMultipartFile file = new MockMultipartFile("file", "one-row.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        RoomingListPreviewResponse res = excelParserService.parseRoomingList(file);

        assertThat(res.getTotalRows()).isEqualTo(1);
        assertThat(res.getValidCount()).isEqualTo(1);
        assertThat(res.getErrorCount()).isEqualTo(0);
        assertThat(res.getRows().get(0).getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(res.getRows().get(0).getGender()).isEqualTo(Gender.MALE);
        assertThat(res.getRows().get(0).getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(res.getRows().get(0).getRoomNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("BVA-04: File có đúng 20 dòng hợp lệ -> Đọc đúng 20 dòng hợp lệ")
    void testBva04_twentyValidRows() throws IOException {
        List<String[]> data = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            data.add(new String[]{
                    "Khách Hàng " + i,
                    i % 2 == 0 ? "Nam" : "Nữ",
                    "01/01/199" + (i % 10),
                    "Việt Nam",
                    String.format("00120000%04d", i),
                    String.format("091234%04d", i),
                    "guest" + i + "@example.com",
                    "101",
                    "Đoàn du lịch"
            });
        }
        byte[] bytes = createWorkbookBytes("RoomingList", STANDARD_HEADERS, data);
        MockMultipartFile file = new MockMultipartFile("file", "valid-20.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        RoomingListPreviewResponse res = excelParserService.parseRoomingList(file);

        assertThat(res.getTotalRows()).isEqualTo(20);
        assertThat(res.getValidCount()).isEqualTo(20);
        assertThat(res.getErrorCount()).isEqualTo(0);
        assertThat(res.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("BVA-05: File có đúng 500 dòng dữ liệu -> Xử lý thành công đúng 500 dòng")
    void testBva05_boundary500Rows() throws IOException {
        List<String[]> data = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            data.add(new String[]{
                    "Khách " + i,
                    "Nam",
                    "01/01/1990",
                    "Việt Nam",
                    String.format("001999%06d", i),
                    "0900000000",
                    "g" + i + "@test.com",
                    "101",
                    "Note"
            });
        }
        byte[] bytes = createWorkbookBytes("RoomingList", STANDARD_HEADERS, data);
        MockMultipartFile file = new MockMultipartFile("file", "rows-500.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        RoomingListPreviewResponse res = excelParserService.parseRoomingList(file);

        assertThat(res.getTotalRows()).isEqualTo(500);
        assertThat(res.getValidCount()).isEqualTo(500);
    }

    @Test
    @DisplayName("BVA-06: File có 501 dòng dữ liệu -> Từ chối do vượt quá giới hạn 500 dòng")
    void testBva06_boundary501Rows() throws IOException {
        List<String[]> data = new ArrayList<>();
        for (int i = 1; i <= 501; i++) {
            data.add(new String[]{
                    "Khách " + i,
                    "Nam",
                    "01/01/1990",
                    "Việt Nam",
                    String.format("001999%06d", i),
                    "0900000000",
                    "g" + i + "@test.com",
                    "101",
                    "Note"
            });
        }
        byte[] bytes = createWorkbookBytes("RoomingList", STANDARD_HEADERS, data);
        MockMultipartFile file = new MockMultipartFile("file", "rows-501.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        assertThatThrownBy(() -> excelParserService.parseRoomingList(file))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_MAX_ROWS_EXCEEDED);
    }

    @Test
    @DisplayName("BVA-07: File có dòng lỗi xen kẽ -> Báo chính xác số dòng lỗi và nguyên nhân, giữ dòng hợp lệ")
    void testBva07_mixedInvalidRows() throws IOException {
        List<String[]> data = List.of(
                new String[]{"Trần Văn Hợp Lệ", "Nam", "10/10/1990", "Việt Nam", "001200001111", "0901234567", "tran@gmail.com", "101", "OK"},
                new String[]{"", "Nam", "10/10/1990", "Việt Nam", "001200002222", "0901234567", "tran@gmail.com", "101", "Thiếu tên"}, // Dòng 3 Excel: thiếu tên
                new String[]{"Lê Thị B", "Nữ", "10/10/1995", "Việt Nam", "001200003333", "0901234567", "sai-email", "101", "Sai email"}, // Dòng 4 Excel: sai email
                new String[]{"Phạm Văn C", "KhôngXácĐịnh", "10/10/1995", "Việt Nam", "001200004444", "0901234567", "c@gmail.com", "101", "Sai giới tính"}, // Dòng 5 Excel: sai giới tính
                new String[]{"Hoàng Văn D", "Nam", "31/02/2020", "Việt Nam", "001200005555", "0901234567", "d@gmail.com", "101", "Sai ngày sinh"} // Dòng 6 Excel: sai ngày sinh
        );
        byte[] bytes = createWorkbookBytes("RoomingList", STANDARD_HEADERS, data);
        MockMultipartFile file = new MockMultipartFile("file", "invalid-rows.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        RoomingListPreviewResponse res = excelParserService.parseRoomingList(file);

        assertThat(res.getTotalRows()).isEqualTo(5);
        assertThat(res.getValidCount()).isEqualTo(1);
        assertThat(res.getErrorCount()).isEqualTo(4);

        // Kiểm tra đúng số dòng Excel (header là dòng 1, data bắt đầu từ dòng 2)
        assertThat(res.getRows().get(0).getRowNumber()).isEqualTo(2);
        assertThat(res.getRows().get(0).getIsValid()).isTrue();

        assertThat(res.getRows().get(1).getRowNumber()).isEqualTo(3);
        assertThat(res.getRows().get(1).getIsValid()).isFalse();
        assertThat(res.getRows().get(1).getErrors().get(0)).contains("Họ và tên khách không được để trống");

        assertThat(res.getRows().get(2).getRowNumber()).isEqualTo(4);
        assertThat(res.getRows().get(2).getIsValid()).isFalse();
        assertThat(res.getRows().get(2).getErrors().get(0)).contains("Email");

        assertThat(res.getRows().get(3).getRowNumber()).isEqualTo(5);
        assertThat(res.getRows().get(3).getIsValid()).isFalse();
        assertThat(res.getRows().get(3).getErrors().get(0)).contains("Giới tính");

        assertThat(res.getRows().get(4).getRowNumber()).isEqualTo(6);
        assertThat(res.getRows().get(4).getIsValid()).isFalse();
        assertThat(res.getRows().get(4).getErrors().get(0)).contains("Ngày sinh");
    }

    @Test
    @DisplayName("BVA-08: File sai cấu trúc (sai tên sheet hoặc thiếu cột bắt buộc) -> Báo lỗi cấu trúc")
    void testBva08_invalidStructure() throws IOException {
        // Sai tên sheet: WrongSheetName thay vì RoomingList
        byte[] bytes = createWorkbookBytes("WrongSheetName", STANDARD_HEADERS, List.<String[]>of(
                new String[]{"Nguyễn Văn A", "Nam", "10/10/1990", "Việt Nam", "001200001234", "0901234567", "a@test.com", "101", ""}
        ));
        MockMultipartFile file = new MockMultipartFile("file", "wrong-sheet.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        assertThatThrownBy(() -> excelParserService.parseRoomingList(file))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_INVALID_STRUCTURE)
                .hasMessageContaining("RoomingList");

        // Thiếu cột bắt buộc Full Name
        String[] headersWithoutName = {"Gender", "Date of Birth", "Nationality"};
        byte[] bytes2 = createWorkbookBytes("RoomingList", headersWithoutName, List.<String[]>of(
                new String[]{"Nam", "10/10/1990", "Việt Nam"}
        ));
        MockMultipartFile file2 = new MockMultipartFile("file", "missing-col.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes2);

        assertThatThrownBy(() -> excelParserService.parseRoomingList(file2))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_INVALID_STRUCTURE)
                .hasMessageContaining("Full Name");
    }

    @Test
    @DisplayName("BVA-09: File không đúng định dạng (không phải xlsx) -> Từ chối an toàn")
    void testBva09_invalidFormat() {
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());

        assertThatThrownBy(() -> excelParserService.parseRoomingList(txtFile))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_INVALID_FILE);
    }

    @Test
    @DisplayName("BVA-10: File vượt quá dung lượng giới hạn (> 10MB) -> Từ chối trước khi xử lý")
    void testBva10_fileSizeExceeded() {
        // Mock file > 10MB
        MockMultipartFile hugeFile = new MockMultipartFile("file", "huge.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[11 * 1024 * 1024]);

        assertThatThrownBy(() -> excelParserService.parseRoomingList(hugeFile))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEL_FILE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("BVA-11: Booking không tồn tại -> Trả lỗi BOOKING_NOT_FOUND")
    void testBva11_bookingNotFound() throws IOException {
        when(bookingRepository.findById(9999L)).thenReturn(Optional.empty());

        byte[] bytes = createWorkbookBytes("RoomingList", STANDARD_HEADERS, List.<String[]>of(
                new String[]{"Nguyễn Văn A", "Nam", "10/10/1990", "Việt Nam", "001200001234", "0901234567", "a@test.com", "101", ""}
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        assertThatThrownBy(() -> bookingService.previewRoomingList(9999L, file))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_NOT_FOUND);

        assertThatThrownBy(() -> bookingService.importRoomingList(9999L, file, true))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_NOT_FOUND);
    }

    @Test
    @DisplayName("BVA-12: Import lặp lại -> Không tạo bản ghi trùng ngoài ý muốn (Idempotency)")
    void testBva12_duplicateImportPrevention() throws IOException {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));

        // Lần đầu: chưa có khách này trong database
        when(stayGuestRepository.findFirstByHotelIdAndBookingIdAndDocumentNumberAndIsDeletedFalse(
                sampleHotel.getId(), 100L, "001200001234"))
                .thenReturn(Optional.empty());

        when(stayGuestRepository.save(any(StayGuest.class))).thenAnswer(inv -> {
            StayGuest g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        List<String[]> data = List.<String[]>of(
                new String[]{"Nguyễn Văn A", "Nam", "10/10/1990", "Việt Nam", "001200001234", "0901234567", "a@test.com", "101", ""}
        );
        byte[] bytes = createWorkbookBytes("RoomingList", STANDARD_HEADERS, data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        // Lần 1: Import thành công 1 khách
        RoomingListImportResultResponse result1 = bookingService.importRoomingList(100L, file, true);
        assertThat(result1.getImportedCount()).isEqualTo(1);
        assertThat(result1.getSkippedCount()).isEqualTo(0);

        // Giả lập lần 2: khách "001200001234" ĐÃ tồn tại trong DB cho booking này
        StayGuest existingGuest = StayGuest.builder().id(1L).documentNumber("001200001234").fullName("Nguyễn Văn A").build();
        when(stayGuestRepository.findFirstByHotelIdAndBookingIdAndDocumentNumberAndIsDeletedFalse(
                sampleHotel.getId(), 100L, "001200001234"))
                .thenReturn(Optional.of(existingGuest));

        // Lần 2: Import lại cùng file -> Bỏ qua không insert trùng, skippedCount = 1
        RoomingListImportResultResponse result2 = bookingService.importRoomingList(100L, file, true);
        assertThat(result2.getImportedCount()).isEqualTo(0);
        assertThat(result2.getSkippedCount()).isEqualTo(1);
    }
}
