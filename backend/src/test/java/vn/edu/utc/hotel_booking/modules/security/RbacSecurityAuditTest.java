package vn.edu.utc.hotel_booking.modules.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import vn.edu.utc.hotel_booking.modules.ai.rag.controller.AdminRagLifecycleController;
import vn.edu.utc.hotel_booking.modules.ai.rag.dto.RagDeleteRequestDto;
import vn.edu.utc.hotel_booking.modules.ai.rag.service.RagKnowledgeLifecycleService;
import vn.edu.utc.hotel_booking.modules.booking.controller.BookingController;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.BookingCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;
import vn.edu.utc.hotel_booking.modules.finance.controller.PaymentController;
import vn.edu.utc.hotel_booking.modules.finance.dto.request.PaymentCreateRequest;
import vn.edu.utc.hotel_booking.modules.finance.service.PaymentService;
import vn.edu.utc.hotel_booking.modules.identity.controller.AuthController;
import vn.edu.utc.hotel_booking.modules.identity.service.AuthService;
import vn.edu.utc.hotel_booking.modules.inventory.controller.RoomMaintenanceBlockController;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomMaintenanceBlockService;
import vn.edu.utc.hotel_booking.modules.operation.controller.ServiceOrderController;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.ServiceOrderCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RbacSecurityAuditTest.MethodSecurityConfig.class,
        PaymentController.class,
        BookingController.class,
        ServiceOrderController.class,
        RoomMaintenanceBlockController.class,
        AdminRagLifecycleController.class,
        AuthController.class
})
@DisplayName("Sprint 4 — Task 3: RBAC Security Audit Test")
class RbacSecurityAuditTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {

        @Bean
        public PaymentService paymentService() {
            return mock(PaymentService.class);
        }

        @Bean
        public BookingService bookingService() {
            return mock(BookingService.class);
        }

        @Bean
        public ServiceOrderService serviceOrderService() {
            return mock(ServiceOrderService.class);
        }

        @Bean
        public RoomMaintenanceBlockService roomMaintenanceBlockService() {
            return mock(RoomMaintenanceBlockService.class);
        }

        @Bean
        public RagKnowledgeLifecycleService ragKnowledgeLifecycleService() {
            return mock(RagKnowledgeLifecycleService.class);
        }

        @Bean
        public AuthService authService() {
            return mock(AuthService.class);
        }
    }

    @Autowired private PaymentController paymentController;
    @Autowired private BookingController bookingController;
    @Autowired private ServiceOrderController serviceOrderController;
    @Autowired private RoomMaintenanceBlockController maintenanceBlockController;
    @Autowired private AdminRagLifecycleController adminRagLifecycleController;
    @Autowired private AuthController authController;

    private final RagDeleteRequestDto sampleDeleteReq = RagDeleteRequestDto.builder()
            .sourceId("test-src")
            .version(1)
            .build();

    // --- 1. PAYMENT MODULE RBAC ---

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Payment create: Receptionist role is allowed")
    void paymentCreate_receptionist_allowed() {
        assertDoesNotThrow(() -> paymentController.createPayment(new PaymentCreateRequest()));
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPING")
    @DisplayName("Payment create: Housekeeping role is denied (403)")
    void paymentCreate_housekeeping_denied() {
        assertDenied(() -> paymentController.createPayment(new PaymentCreateRequest()));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Payment create: Anonymous/No Token is denied (401/403)")
    void paymentCreate_anonymous_denied() {
        assertDenied(() -> paymentController.createPayment(new PaymentCreateRequest()));
    }

    // --- 2. BOOKING MODULE RBAC ---

    @Test
    @WithMockUser(roles = "SALES_GROUP")
    @DisplayName("Booking create: Sales Group is allowed")
    void bookingCreate_salesGroup_allowed() {
        assertDoesNotThrow(() -> bookingController.createBooking(new BookingCreateRequest()));
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPING")
    @DisplayName("Booking create: Housekeeping role is denied (403)")
    void bookingCreate_housekeeping_denied() {
        assertDenied(() -> bookingController.createBooking(new BookingCreateRequest()));
    }

    // --- 3. SERVICE ORDER MODULE RBAC ---

    @Test
    @WithMockUser(roles = "F_AND_B")
    @DisplayName("Service order create: F&B staff is allowed")
    void serviceOrderCreate_fAndB_allowed() {
        assertDoesNotThrow(() -> serviceOrderController.createOrder(new ServiceOrderCreateRequest()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Service order create: Customer role is denied (403)")
    void serviceOrderCreate_customer_denied() {
        assertDenied(() -> serviceOrderController.createOrder(new ServiceOrderCreateRequest()));
    }

    // --- 4. ROOM MAINTENANCE BLOCK RBAC ---

    @Test
    @WithMockUser(roles = "ENGINEERING")
    @DisplayName("Maintenance block create: Engineering role is allowed")
    void maintenanceBlockCreate_engineering_allowed() {
        assertDoesNotThrow(() -> maintenanceBlockController.create(new RoomMaintenanceBlockCreateRequest()));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Maintenance block create: Receptionist cannot create maintenance block (403)")
    void maintenanceBlockCreate_receptionist_denied() {
        assertDenied(() -> maintenanceBlockController.create(new RoomMaintenanceBlockCreateRequest()));
    }

    // --- 5. ADMIN RAG LIFECYCLE RBAC ---

    @Test
    @WithMockUser(roles = "CHAIN_ADMIN")
    @DisplayName("Admin RAG: Chain Admin is allowed")
    void adminRag_chainAdmin_allowed() {
        assertDoesNotThrow(() -> adminRagLifecycleController.delete(sampleDeleteReq));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Admin RAG: Receptionist is denied (403)")
    void adminRag_receptionist_denied() {
        assertDenied(() -> adminRagLifecycleController.delete(sampleDeleteReq));
    }

    // --- 6. AUTH LOGOUT RBAC ---

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Auth logout: Authenticated user is allowed")
    void authLogout_authenticated_allowed() {
        assertDoesNotThrow(() -> authController.logout(null, null));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Auth logout: Anonymous/No Token is denied (401/403)")
    void authLogout_anonymous_denied() {
        assertDenied(() -> authController.logout(null, null));
    }

    private void assertDenied(org.junit.jupiter.api.function.Executable executable) {
        Exception ex = assertThrows(Exception.class, executable);
        assertTrue(ex instanceof AccessDeniedException || ex instanceof AuthorizationDeniedException,
                "Expected AccessDeniedException or AuthorizationDeniedException, got " + ex.getClass().getName());
    }
}
