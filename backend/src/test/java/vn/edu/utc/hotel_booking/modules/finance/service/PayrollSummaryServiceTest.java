package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.ErrorRowDto;
import vn.edu.utc.hotel_booking.modules.finance.entity.PayrollSummary;
import vn.edu.utc.hotel_booking.modules.finance.repository.PayrollSummaryRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.PayrollSummaryServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PayrollSummaryServiceTest {

    @Mock
    private PayrollSummaryRepository payrollSummaryRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PayrollSummaryServiceImpl payrollSummaryService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockChainAdminAuth() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_CHAIN_ADMIN"))).when(authentication).getAuthorities();
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    public void importCsv_emptyFile_returnsError() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);
        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);
        assertEquals(1, errors.size());
        assertEquals(0, errors.get(0).getRow());
    }

    @Test
    public void importCsv_validData_success() {
        mockChainAdminAuth();
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "1,10,2026,1000,200,50,1150\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));

        Staff mockStaff = new Staff();
        mockStaff.setId(1);
        when(staffRepository.findAllById(anyCollection())).thenReturn(List.of(mockStaff));
        when(payrollSummaryRepository.findFirstByStaffIdAndPeriodMonthAndPeriodYearAndIsDeletedFalseOrderByIdDesc(1, 10, 2026))
                .thenReturn(Optional.empty());

        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);

        assertEquals(0, errors.size());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PayrollSummary>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(payrollSummaryRepository, times(1)).saveAll(listCaptor.capture());

        List<PayrollSummary> savedList = listCaptor.getValue();
        assertEquals(1, savedList.size());
        assertEquals(1, savedList.get(0).getStaffId());
        assertEquals(10, savedList.get(0).getPeriodMonth());
        assertEquals(new BigDecimal("1000"), savedList.get(0).getBaseSalary());
    }

    @Test
    public void importCsv_invalidStaff_returnsErrorRow() {
        mockChainAdminAuth();
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "99,10,2026,1000,200,50,1150\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));

        when(staffRepository.findAllById(anyCollection())).thenReturn(List.of());

        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);

        assertEquals(1, errors.size());
        assertEquals(2, errors.get(0).getRow());

        verify(payrollSummaryRepository, never()).saveAll(any());
    }

    @Test
    public void importCsv_interleavedRows_reportsExactPhysicalRowNumbers() {
        mockChainAdminAuth();
        // Line 1: Header
        // Line 2: Valid
        // Line 3: Format error (missing columns)
        // Line 4: Valid
        // Line 5: Negative baseSalary
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "1,10,2026,1000,200,50,1150\n" +
                         "2,10,2026,1000\n" +
                         "3,10,2026,2000,100,50,2050\n" +
                         "4,10,2026,-500,0,0,-500\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));

        Staff staff1 = Staff.builder().id(1).build();
        Staff staff3 = Staff.builder().id(3).build();
        when(staffRepository.findAllById(anyCollection())).thenReturn(List.of(staff1, staff3));

        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);

        assertEquals(2, errors.size());
        // Exact physical row numbers preserved
        assertEquals(3, errors.get(0).getRow());
        assertTrue(errors.get(0).getError().contains("Missing columns"));

        assertEquals(5, errors.get(1).getRow());
        assertTrue(errors.get(1).getError().contains("Salary components"));
    }

    @Test
    public void importCsv_arithmeticMismatch_reportsError() {
        mockChainAdminAuth();
        // 1000 + 200 - 50 = 1150 != 1200
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "1,10,2026,1000,200,50,1200\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));

        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);

        assertEquals(1, errors.size());
        assertEquals(2, errors.get(0).getRow());
        assertTrue(errors.get(0).getError().contains("Net salary mismatch"));
    }

    @Test
    public void importCsv_invalidMonthOrYear_reportsError() {
        mockChainAdminAuth();
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "1,13,2026,1000,0,0,1000\n" +
                         "2,10,1999,1000,0,0,1000\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));

        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);

        assertEquals(2, errors.size());
        assertEquals(2, errors.get(0).getRow());
        assertTrue(errors.get(0).getError().contains("Invalid period month"));
        assertEquals(3, errors.get(1).getRow());
        assertTrue(errors.get(1).getError().contains("Invalid period year"));
    }

    @Test
    public void importCsv_duplicateInFile_reportsError() {
        mockChainAdminAuth();
        // Two rows for same staffId 1, period 10/2026
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "1,10,2026,1000,200,50,1150\n" +
                         "1,10,2026,1200,100,50,1250\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));

        Staff staff1 = Staff.builder().id(1).build();
        when(staffRepository.findAllById(anyCollection())).thenReturn(List.of(staff1));

        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);

        assertEquals(1, errors.size());
        assertEquals(3, errors.get(0).getRow());
        assertTrue(errors.get(0).getError().contains("Duplicate staff ID"));
    }

    @Test
    public void importCsv_idempotentUpdateExistingDbRecord_updatesInPlace() {
        mockChainAdminAuth();
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "1,10,2026,1500,300,100,1700\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));

        Staff mockStaff = new Staff();
        mockStaff.setId(1);
        when(staffRepository.findAllById(any())).thenReturn(List.of(mockStaff));

        PayrollSummary existingRecord = PayrollSummary.builder()
                .id(999L)
                .staffId(1)
                .periodMonth(10)
                .periodYear(2026)
                .baseSalary(new BigDecimal("1000"))
                .netSalary(new BigDecimal("1000"))
                .build();

        when(payrollSummaryRepository.findFirstByStaffIdAndPeriodMonthAndPeriodYearAndIsDeletedFalseOrderByIdDesc(1, 10, 2026))
                .thenReturn(Optional.of(existingRecord));

        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);

        assertEquals(0, errors.size());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PayrollSummary>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(payrollSummaryRepository, times(1)).saveAll(listCaptor.capture());

        List<PayrollSummary> savedList = listCaptor.getValue();
        assertEquals(1, savedList.size());
        assertEquals(999L, savedList.get(0).getId());
        assertEquals(new BigDecimal("1500"), savedList.get(0).getBaseSalary());
        assertEquals(new BigDecimal("1700"), savedList.get(0).getNetSalary());
    }

    @Test
    public void importCsv_existingDuplicateRowsInDatabase_updatesLatestRecordGracefullyWithoutException() {
        mockChainAdminAuth();
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "1,10,2026,2000,500,100,2400\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));

        Staff mockStaff = new Staff();
        mockStaff.setId(1);
        when(staffRepository.findAllById(anyCollection())).thenReturn(List.of(mockStaff));

        // When DB contains pre-existing duplicates (e.g. ID 10 and ID 20), findFirst...OrderByIdDesc returns latest ID 20
        PayrollSummary latestDuplicateRecord = PayrollSummary.builder()
                .id(20L)
                .staffId(1)
                .periodMonth(10)
                .periodYear(2026)
                .baseSalary(new BigDecimal("1800"))
                .netSalary(new BigDecimal("1800"))
                .build();

        when(payrollSummaryRepository.findFirstByStaffIdAndPeriodMonthAndPeriodYearAndIsDeletedFalseOrderByIdDesc(1, 10, 2026))
                .thenReturn(Optional.of(latestDuplicateRecord));

        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);

        assertEquals(0, errors.size());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PayrollSummary>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(payrollSummaryRepository, times(1)).saveAll(listCaptor.capture());

        List<PayrollSummary> savedList = listCaptor.getValue();
        assertEquals(1, savedList.size());
        assertEquals(20L, savedList.get(0).getId());
        assertEquals(new BigDecimal("2000"), savedList.get(0).getBaseSalary());
        assertEquals(new BigDecimal("2400"), savedList.get(0).getNetSalary());
    }

    @Test
    public void importCsv_propertyManager_crossPropertyStaff_reportsError() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_PROPERTY_MANAGER"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("pm_hotel1");
        when(securityContext.getAuthentication()).thenReturn(authentication);

        Staff pmStaff = Staff.builder()
                .username("pm_hotel1")
                .scopeType("PROPERTY")
                .scopeEntityId(1) // Hotel 1
                .build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_hotel1")).thenReturn(Optional.of(pmStaff));

        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "5,10,2026,1000,200,50,1150\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));

        Staff staffOtherHotel = Staff.builder()
                .id(5)
                .scopeType("PROPERTY")
                .scopeEntityId(2) // Hotel 2
                .build();
        when(staffRepository.findAllById(anyCollection())).thenReturn(List.of(staffOtherHotel));

        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);

        assertEquals(1, errors.size());
        assertEquals(2, errors.get(0).getRow());
        assertTrue(errors.get(0).getError().contains("does not belong to your authorized hotel"));
    }
}
