package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.utc.hotel_booking.modules.finance.dto.response.ErrorRowDto;
import vn.edu.utc.hotel_booking.modules.finance.entity.PayrollSummary;
import vn.edu.utc.hotel_booking.modules.finance.repository.PayrollSummaryRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.PayrollSummaryServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PayrollSummaryServiceTest {

    @Mock
    private PayrollSummaryRepository payrollSummaryRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private PayrollSummaryServiceImpl payrollSummaryService;

    @Test
    public void importCsv_emptyFile_returnsError() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);
        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);
        assertEquals(1, errors.size());
        assertEquals(0, errors.get(0).getRow());
    }

    @Test
    public void importCsv_validData_success() {
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "1,10,2026,1000,200,50,1150\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));
        
        Staff mockStaff = new Staff();
        mockStaff.setId(1);
        when(staffRepository.findAllById(any())).thenReturn(List.of(mockStaff));
        
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
        String csvData = "staffId,periodMonth,periodYear,baseSalary,bonus,deductions,netSalary\n" +
                         "99,10,2026,1000,200,50,1150\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));
        
        when(staffRepository.findAllById(any())).thenReturn(List.of());
        
        List<ErrorRowDto> errors = payrollSummaryService.importCsv(file);
        
        assertEquals(1, errors.size());
        assertEquals(2, errors.get(0).getRow());
        
        verify(payrollSummaryRepository, never()).saveAll(any());
    }
}
