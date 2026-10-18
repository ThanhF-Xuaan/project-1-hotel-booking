package vn.edu.utc.hotel_booking.modules.operation.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingExportRequest;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.DocumentType;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.Gender;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingGuestCategory;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.LodgingQueueStatus;
import vn.edu.utc.hotel_booking.modules.operation.repository.LodgingQueueRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.LodgingExportServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LodgingExportServiceTest {

    @Mock
    private LodgingQueueRepository lodgingQueueRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LodgingExportServiceImpl lodgingExportService;

    private Hotel sampleHotel;

    @BeforeEach
    void setUp() {
        sampleHotel = Hotel.builder().id((short) 1).name("Test Hotel").build();
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockChainAdminAuth() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CHAIN_ADMIN"))).when(authentication).getAuthorities();
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void exportBcaLodgingReport_validVietnameseList_generatesExcelAndMarksExported() throws IOException {
        mockChainAdminAuth();

        LodgingExportRequest request = LodgingExportRequest.builder()
                .hotelId((short) 1)
                .date(LocalDate.of(2026, 10, 4))
                .cutOffHour(23)
                .category(LodgingGuestCategory.VIETNAMESE)
                .build();

        StayGuest guest = StayGuest.builder()
                .id(1L)
                .hotel(sampleHotel)
                .fullName("NGUYỄN VĂN A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200001234")
                .permanentAddress("Hà Nội")
                .currentAddress("Cầu Giấy, Hà Nội")
                .checkInTime(OffsetDateTime.now())
                .expectedCheckOutTime(OffsetDateTime.now().plusDays(2))
                .roomNumber("101")
                .reasonForStay("Du lịch")
                .build();

        LodgingQueue queueItem = LodgingQueue.builder()
                .id(100L)
                .stayGuest(guest)
                .hotel(sampleHotel)
                .status(LodgingQueueStatus.PENDING)
                .build();

        when(lodgingQueueRepository.findPendingForExport(eq((short) 1), any(), eq(true)))
                .thenReturn(List.of(queueItem));

        byte[] result = lodgingExportService.exportBcaLodgingReport(request);

        assertNotNull(result);
        assertTrue(result.length > 0);

        // Verify that the Excel workbook can be read and has 13 headers
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Thong_Bao_Luu_Tru_VN");
            assertNotNull(sheet);
            assertEquals(13, sheet.getRow(0).getPhysicalNumberOfCells());
            assertEquals("NGUYỄN VĂN A", sheet.getRow(1).getCell(1).getStringCellValue());
        }

        // Verify status transitioned to EXPORTED
        assertEquals(LodgingQueueStatus.EXPORTED, queueItem.getStatus());
        assertNotNull(queueItem.getExportedAt());
        assertNotNull(queueItem.getBatchReference());
        verify(lodgingQueueRepository, times(1)).saveAll(any());
    }

    @Test
    void exportBcaLodgingReport_formulaInjectionPayload_persistsAsStringLiteralWithoutExecution() throws IOException {
        mockChainAdminAuth();

        LodgingExportRequest request = LodgingExportRequest.builder()
                .hotelId((short) 1)
                .date(LocalDate.of(2026, 10, 4))
                .cutOffHour(23)
                .category(LodgingGuestCategory.VIETNAMESE)
                .build();

        // Formula payload strings
        String formulaPayload = "=1+2";
        StayGuest guest = StayGuest.builder()
                .id(2L)
                .hotel(sampleHotel)
                .fullName(formulaPayload)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .nationality("Việt Nam")
                .documentType(DocumentType.CCCD)
                .documentNumber("001200001234")
                .permanentAddress("@SUM(A1:A10)")
                .currentAddress("+cmd.exe")
                .checkInTime(OffsetDateTime.now())
                .expectedCheckOutTime(OffsetDateTime.now().plusDays(2))
                .roomNumber("-calc.exe")
                .reasonForStay("Du lịch")
                .build();

        LodgingQueue queueItem = LodgingQueue.builder()
                .id(101L)
                .stayGuest(guest)
                .hotel(sampleHotel)
                .status(LodgingQueueStatus.PENDING)
                .build();

        when(lodgingQueueRepository.findPendingForExport(eq((short) 1), any(), eq(true)))
                .thenReturn(List.of(queueItem));

        byte[] result = lodgingExportService.exportBcaLodgingReport(request);
        assertNotNull(result);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Thong_Bao_Luu_Tru_VN");
            assertNotNull(sheet);
            Cell nameCell = sheet.getRow(1).getCell(1);

            // Verified: cellType is STRING, not FORMULA
            assertEquals(CellType.STRING, nameCell.getCellType());
            assertEquals("=1+2", nameCell.getStringCellValue());

            Cell addressCell = sheet.getRow(1).getCell(7);
            assertEquals(CellType.STRING, addressCell.getCellType());
            assertEquals("@SUM(A1:A10)", addressCell.getStringCellValue());
        }
    }

    @Test
    void exportBcaLodgingReport_crossPropertyStaff_throwsUnauthorized() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_PROPERTY_MANAGER"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("pm_hotel2");
        when(securityContext.getAuthentication()).thenReturn(authentication);

        Staff staff = Staff.builder()
                .username("pm_hotel2")
                .scopeType("PROPERTY")
                .scopeEntityId(2) // Belongs to Hotel 2
                .build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_hotel2")).thenReturn(Optional.of(staff));

        LodgingExportRequest request = LodgingExportRequest.builder()
                .hotelId((short) 1) // Attempting export on Hotel 1
                .date(LocalDate.of(2026, 10, 4))
                .cutOffHour(23)
                .category(LodgingGuestCategory.VIETNAMESE)
                .build();

        AppException ex = assertThrows(AppException.class, () -> lodgingExportService.exportBcaLodgingReport(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("không có quyền xuất báo cáo"));
    }

    @Test
    void exportBcaLodgingReport_noPendingRecords_throwsLodgingExportEmpty() {
        mockChainAdminAuth();

        LodgingExportRequest request = LodgingExportRequest.builder()
                .hotelId((short) 1)
                .date(LocalDate.of(2026, 10, 4))
                .cutOffHour(23)
                .category(LodgingGuestCategory.VIETNAMESE)
                .build();

        when(lodgingQueueRepository.findPendingForExport(eq((short) 1), any(), eq(true)))
                .thenReturn(Collections.emptyList());

        AppException ex = assertThrows(AppException.class, () -> lodgingExportService.exportBcaLodgingReport(request));
        assertEquals(ErrorCode.LODGING_EXPORT_EMPTY, ex.getErrorCode());
    }
}
