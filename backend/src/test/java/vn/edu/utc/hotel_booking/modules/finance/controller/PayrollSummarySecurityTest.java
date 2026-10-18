package vn.edu.utc.hotel_booking.modules.finance.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import vn.edu.utc.hotel_booking.modules.finance.service.PayrollSummaryService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        PayrollSummaryController.class,
        PayrollSummarySecurityTest.MethodSecurityConfig.class
})
class PayrollSummarySecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
        @Bean
        public PayrollSummaryService payrollSummaryService() {
            PayrollSummaryService service = mock(PayrollSummaryService.class);
            when(service.importCsv(any())).thenReturn(Collections.emptyList());
            return service;
        }
    }

    @Autowired
    private PayrollSummaryController controller;

    private final MockMultipartFile sampleFile = new MockMultipartFile(
            "file", "payroll.csv", "text/csv", "dummy content".getBytes()
    );

    @Test
    @WithMockUser(roles = "CHAIN_ADMIN")
    void importCsv_chainAdminRole_allowed() {
        assertDoesNotThrow(() -> controller.importCsv(sampleFile));
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    void importCsv_propertyManagerRole_allowed() {
        assertDoesNotThrow(() -> controller.importCsv(sampleFile));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void importCsv_receptionistRole_denied() {
        Exception ex = assertThrows(Exception.class, () -> controller.importCsv(sampleFile));
        assertTrue(ex instanceof AccessDeniedException || ex instanceof AuthorizationDeniedException);
    }

    @Test
    @WithAnonymousUser
    void importCsv_unauthenticatedOrAnonymous_denied() {
        Exception ex = assertThrows(Exception.class, () -> controller.importCsv(sampleFile));
        assertTrue(ex instanceof AccessDeniedException || ex instanceof AuthorizationDeniedException);
    }
}
