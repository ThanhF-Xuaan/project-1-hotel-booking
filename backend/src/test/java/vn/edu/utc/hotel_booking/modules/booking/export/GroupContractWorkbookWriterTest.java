package vn.edu.utc.hotel_booking.modules.booking.export;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContractStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlock;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockStatus;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group Contract Workbook Writer Test")
class GroupContractWorkbookWriterTest {

    private final GroupContractWorkbookWriter writer = new GroupContractWorkbookWriter();

    @Test
    @DisplayName("Xuất danh sách hợp đồng hợp lệ ra Excel với 2 sheet và dữ liệu chính xác")
    void write_validContracts_shouldGenerateExcelWithTwoSheets() throws Exception {
        Hotel hotel = Hotel.builder().name("Khách sạn Grand Hà Nội").build();
        Company company = Company.builder().name("Tập đoàn Vingroup").build();
        Staff staff = Staff.builder().fullName("Nguyễn Văn A").build();
        RoomType rt = RoomType.builder().name("Deluxe City View").build();
        HotelRoomType hrt = HotelRoomType.builder().roomType(rt).build();

        GroupRoomBlock block = GroupRoomBlock.builder()
                .id(1L)
                .hotelRoomType(hrt)
                .totalBlockedRooms(10)
                .allocatedRooms(5)
                .agreedRate(BigDecimal.valueOf(1500000))
                .status(GroupRoomBlockStatus.DEFINITE)
                .build();

        GroupContract contract = GroupContract.builder()
                .id(100L)
                .contractNumber("GC-20261107-TEST")
                .groupName("Hội nghị khách hàng 2026")
                .hotel(hotel)
                .company(company)
                .salesStaff(staff)
                .companyTaxCode("0108999999")
                .checkInDate(LocalDate.of(2026, 11, 20))
                .checkOutDate(LocalDate.of(2026, 11, 23))
                .totalRooms(10)
                .totalGuests(20)
                .subtotalAmount(BigDecimal.valueOf(45000000))
                .serviceFeeAmount(BigDecimal.valueOf(2250000))
                .vatAmount(BigDecimal.valueOf(4725000))
                .totalContractValue(BigDecimal.valueOf(51975000))
                .depositRate(BigDecimal.valueOf(50.00))
                .depositAmount(BigDecimal.valueOf(25987500))
                .depositPaidAmount(BigDecimal.valueOf(25987500))
                .depositDueDate(OffsetDateTime.now().plusDays(3))
                .cutoffDate(OffsetDateTime.now().plusDays(7))
                .status(GroupContractStatus.DEPOSIT_PAID)
                .roomBlocks(List.of(block))
                .build();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writer.write(List.of(contract), bos);

        byte[] bytes = bos.toByteArray();
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);

            // Sheet 1: Danh_Sach_Hop_Dong
            Sheet sheet1 = workbook.getSheet("Danh_Sach_Hop_Dong");
            assertThat(sheet1).isNotNull();
            Row titleRow = sheet1.getRow(0);
            assertThat(titleRow.getCell(0).getStringCellValue()).contains("BÁO CÁO DANH SÁCH HỢP ĐỒNG KHÁCH ĐOÀN");

            Row headerRow = sheet1.getRow(3);
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Mã hợp đồng");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Tên đoàn");

            Row dataRow = sheet1.getRow(4);
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("GC-20261107-TEST");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("Hội nghị khách hàng 2026");
            assertThat(dataRow.getCell(3).getStringCellValue()).isEqualTo("Tập đoàn Vingroup");
            assertThat(dataRow.getCell(10).getNumericCellValue()).isEqualTo(10.0);
            assertThat(dataRow.getCell(15).getNumericCellValue()).isEqualTo(51975000.0);
            assertThat(dataRow.getCell(23).getStringCellValue()).isEqualTo("Đã cọc 50%");

            // Sheet 2: Chi_Tiet_Block_Phong
            Sheet sheet2 = workbook.getSheet("Chi_Tiet_Block_Phong");
            assertThat(sheet2).isNotNull();
            Row blockHeader = sheet2.getRow(2);
            assertThat(blockHeader.getCell(4).getStringCellValue()).isEqualTo("Loại phòng");

            Row blockData = sheet2.getRow(3);
            assertThat(blockData.getCell(1).getStringCellValue()).isEqualTo("GC-20261107-TEST");
            assertThat(blockData.getCell(4).getStringCellValue()).isEqualTo("Deluxe City View");
            assertThat(blockData.getCell(5).getNumericCellValue()).isEqualTo(10.0);
            assertThat(blockData.getCell(7).getNumericCellValue()).isEqualTo(1500000.0);
        }
    }

    @Test
    @DisplayName("Xuất danh sách rỗng vẫn tạo file Excel hoàn chỉnh có tiêu đề và dòng tổng cộng")
    void write_emptyContracts_shouldGenerateValidEmptyWorkbook() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writer.write(Collections.emptyList(), bos);

        byte[] bytes = bos.toByteArray();
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            Sheet sheet1 = workbook.getSheet("Danh_Sach_Hop_Dong");
            assertThat(sheet1).isNotNull();
            Row summaryRow = sheet1.getRow(4);
            assertThat(summaryRow.getCell(0).getStringCellValue()).isEqualTo("TỔNG CỘNG");
            assertThat(summaryRow.getCell(10).getNumericCellValue()).isEqualTo(0.0);
        }
    }
}
