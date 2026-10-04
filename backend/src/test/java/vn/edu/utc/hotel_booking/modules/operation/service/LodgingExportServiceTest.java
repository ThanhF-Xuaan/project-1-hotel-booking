package vn.edu.utc.hotel_booking.modules.operation.service;

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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.LodgingExportRequest;
import vn.edu.utc.hotel_booking.modules.operation.entity.LodgingQueue;
import vn.edu.utc.hotel_booking.modules.booking.entity.StayGuest;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Test
    void exportBcaLodgingReport_validVietnameseList_generatesExcelAndMarksExported() throws IOException {
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
    void exportBcaLodgingReport_noPendingRecords_throwsLodgingExportEmpty() {
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
