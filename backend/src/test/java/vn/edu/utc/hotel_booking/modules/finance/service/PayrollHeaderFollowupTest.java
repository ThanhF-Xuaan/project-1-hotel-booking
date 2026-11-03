package vn.edu.utc.hotel_booking.modules.finance.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.finance.repository.PayrollSummaryRepository;
import vn.edu.utc.hotel_booking.modules.finance.service.impl.PayrollSummaryServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollHeaderFollowupTest {
    static final String HEADER="staff_id,period_month,period_year,base_salary,bonus,deductions,net_salary";
    final PayrollSummaryRepository payroll=mock(PayrollSummaryRepository.class);
    final StaffRepository staff=mock(StaffRepository.class);
    final PayrollSummaryService service=new PayrollSummaryServiceImpl(payroll,staff);
    @BeforeEach void authenticate() {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "synthetic-admin","",AuthorityUtils.createAuthorityList("ROLE_CHAIN_ADMIN")));
        when(staff.findAllById(any())).thenReturn(List.of(Staff.builder().id(7).build()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    MockMultipartFile file(String csv) { return new MockMultipartFile("file","synthetic.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8)); }
    @ParameterizedTest @ValueSource(strings={
        "staff_id,period_month,period_year,bonus,base_salary,deductions,net_salary",
        "staff,month,year,base,bonus,deductions,net",
        "staff_id,period_month,period_year,base_salary,bonus,deductions",
        "staff_id,period_month,period_year,base_salary,bonus,deductions,net_salary,extra",
        "staff_id,period_month,period_year,base_salary,bonus,bonus,net_salary",
        "STAFF_ID,period_month,period_year,base_salary,bonus,deductions,net_salary",
        "7,11,2026,10,0,0,10"})
    void invalidHeaderCannotWrite(String header) {
        var errors=service.importCsv(file(header+"\n7,11,2026,10,0,0,10\n"));
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().getRow()).isEqualTo(1);
        assertThat(errors.getFirst().getError()).isEqualTo("INVALID_CSV_HEADER");
        verifyNoInteractions(payroll); verify(staff,never()).findAllById(any());
    }
    @Test void bomIsAcceptedAndExtraDataColumnsAreRejectedPerRow() {
        var errors=service.importCsv(file("\uFEFF"+HEADER+"\n7,11,2026,10,0,0,10,ignored\n7,12,2026,10,0,0,10\n"));
        assertThat(errors).hasSize(1); assertThat(errors.getFirst().getRow()).isEqualTo(2);
        verify(payroll).saveAll(argThat(rows -> java.util.stream.StreamSupport.stream(rows.spliterator(),false).count()==1));
    }
    @Test void validHeaderOnlyDoesNotWrite() {
        assertThat(service.importCsv(file(HEADER+"\n"))).isEmpty(); verifyNoInteractions(payroll);
    }
}
