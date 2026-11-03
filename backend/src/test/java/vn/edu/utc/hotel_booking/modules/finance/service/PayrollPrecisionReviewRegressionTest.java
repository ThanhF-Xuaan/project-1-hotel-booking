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
import vn.edu.utc.hotel_booking.modules.finance.entity.PayrollSummary;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollPrecisionReviewRegressionTest {
    final PayrollSummaryRepository payroll = mock(PayrollSummaryRepository.class);
    final StaffRepository staff = mock(StaffRepository.class);
    final PayrollSummaryService service = new PayrollSummaryServiceImpl(payroll, staff);
    @BeforeEach void authenticate() {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "synthetic-admin", "", AuthorityUtils.createAuthorityList("ROLE_CHAIN_ADMIN")));
        when(staff.findAllById(any())).thenReturn(List.of(Staff.builder().id(7).build()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    MockMultipartFile file(String salary) {
        return new MockMultipartFile("file", "synthetic.csv", "text/csv", ("staff,month,year,base,bonus,deductions,net\n"
                + "7,11,2026,"+salary+",0,0,"+salary+"\n").getBytes(StandardCharsets.UTF_8));
    }
    @ParameterizedTest @ValueSource(strings={"100.005","10000000000000","1E+1000000000","1E-1000000000","0.001"})
    void unrepresentableMoneyMustBeRejectedBeforePersistence(String salary) {
        var errors = service.importCsv(file(salary));
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().getRow()).isEqualTo(2);
        assertThat(errors.getFirst().getError()).contains("baseSalary").doesNotContain(salary);
        verifyNoInteractions(payroll);
    }

    @ParameterizedTest @ValueSource(strings={"9999999999999.99","1.2300","1e2","0"})
    void representableMoneyIsSavedWithoutLosingValue(String salary) {
        AtomicReference<PayrollSummary> saved = new AtomicReference<>();
        when(payroll.saveAll(any())).thenAnswer(call -> {
            List<PayrollSummary> rows = call.getArgument(0); saved.set(rows.getFirst()); return rows;
        });
        assertThat(service.importCsv(file(salary))).isEmpty();
        assertThat(saved.get().getBaseSalary()).isEqualByComparingTo(new java.math.BigDecimal(salary));
        assertThat(saved.get().getBaseSalary().scale()).isLessThanOrEqualTo(2);
        if (new java.math.BigDecimal(salary).scale() > 2) assertThat(saved.get().getBaseSalary().scale()).isEqualTo(2);
        assertThat(saved.get().getNetSalary()).isEqualByComparingTo(saved.get().getBaseSalary()
                .add(saved.get().getBonus()).subtract(saved.get().getDeductions()));
    }

    @Test void invalidNetIsRejectedBeforeEquationAndNoSalaryIsEchoed() {
        var csv = new MockMultipartFile("file", "synthetic.csv", "text/csv",
                "staff,month,year,base,bonus,deductions,net\n7,11,2026,1,0,0,100.005\n".getBytes(StandardCharsets.UTF_8));
        var errors = service.importCsv(csv);
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().getError()).contains("netSalary").doesNotContain("100.005");
        verifyNoInteractions(payroll);
    }

    @Test void mixedFileSavesOnlyRepresentableRowsAndKeepsRowNumbers() {
        when(payroll.saveAll(any())).thenAnswer(call -> call.getArgument(0));
        var csv = new MockMultipartFile("file", "synthetic.csv", "text/csv",
                "staff,month,year,base,bonus,deductions,net\n7,11,2026,1.23,0,0,1.23\n7,12,2026,100.005,0,0,100.005\n".getBytes(StandardCharsets.UTF_8));
        var errors = service.importCsv(csv);
        assertThat(errors).hasSize(1); assertThat(errors.getFirst().getRow()).isEqualTo(3);
        verify(payroll).saveAll(argThat(rows -> java.util.stream.StreamSupport.stream(rows.spliterator(),false).count()==1));
    }
}
