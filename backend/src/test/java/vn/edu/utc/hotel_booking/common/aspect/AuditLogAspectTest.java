package vn.edu.utc.hotel_booking.common.aspect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import vn.edu.utc.hotel_booking.common.annotation.Auditable;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogAspect — Unit & Fail-Safe Tests")
class AuditLogAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private StaffRepository staffRepository;

    private TestTargetService proxyService;

    static class TestSwapRequest {
        private String fromRoomNumber;
        private String toRoomNumber;
        private String reason;

        public TestSwapRequest(String fromRoomNumber, String toRoomNumber, String reason) {
            this.fromRoomNumber = fromRoomNumber;
            this.toRoomNumber = toRoomNumber;
            this.reason = reason;
        }

        public String getFromRoomNumber() { return fromRoomNumber; }
        public String getToRoomNumber() { return toRoomNumber; }
        public String getReason() { return reason; }
    }

    static class TestSensitiveRequest {
        private String username;
        private String password;
        private String citizenId;

        public TestSensitiveRequest(String username, String password, String citizenId) {
            this.username = username;
            this.password = password;
            this.citizenId = citizenId;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getCitizenId() { return citizenId; }
    }

    static class TestTargetService {

        @Auditable(
                actionType = "BOOKING_ROOM_SWAP",
                entityName = "BookingRoom",
                entityId = "#bookingRoomId.toString()",
                description = "'Đổi từ phòng ' + #request.fromRoomNumber + ' sang ' + #request.toRoomNumber"
        )
        public String swapRoom(Long bookingRoomId, TestSwapRequest request) {
            return "SWAP_SUCCESS_" + bookingRoomId;
        }

        @Auditable(
                actionType = "PAYMENT_REFUND",
                entityName = "Payment",
                entityId = "#id.toString()",
                description = "'Hoàn tiền: ' + #refundAmount + ' đ'"
        )
        public String refund(Long id, BigDecimal refundAmount, String reason) {
            return "REFUNDED_" + id;
        }

        @Auditable(
                actionType = "USER_REGISTER",
                entityName = "User",
                entityId = "#request.username"
        )
        public String createUser(TestSensitiveRequest request) {
            return "USER_CREATED";
        }
    }

    @BeforeEach
    void setUp() {
        AuditLogAspect aspect = new AuditLogAspect(auditLogService, staffRepository);
        TestTargetService target = new TestTargetService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        proxyService = factory.getProxy();
    }

    @Test
    @DisplayName("Ghi log đổi phòng: SpEL đánh giá đúng entityId và description")
    void swapRoom_shouldEvaluateSpelAndRecordLog() {
        TestSwapRequest request = new TestSwapRequest("101", "102", "Điều hòa hỏng");
        String result = proxyService.swapRoom(500L, request);

        assertThat(result).isEqualTo("SWAP_SUCCESS_500");

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> entityNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> entityIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newValuesCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditLogService, times(1)).recordLog(
                isNull(),
                actionCaptor.capture(),
                entityNameCaptor.capture(),
                entityIdCaptor.capture(),
                isNull(),
                newValuesCaptor.capture(),
                anyString(),
                anyString()
        );

        assertThat(actionCaptor.getValue()).isEqualTo("BOOKING_ROOM_SWAP");
        assertThat(entityNameCaptor.getValue()).isEqualTo("BookingRoom");
        assertThat(entityIdCaptor.getValue()).isEqualTo("500");
        assertThat(newValuesCaptor.getValue()).contains("101").contains("102");
    }

    @Test
    @DisplayName("Ghi log hoàn tiền: SpEL đánh giá số tiền hoàn BigDecimal")
    void refund_shouldRecordLog() {
        String result = proxyService.refund(1001L, new BigDecimal("2500000"), "Hủy phòng trước 7 ngày");
        assertThat(result).isEqualTo("REFUNDED_1001");

        verify(auditLogService, times(1)).recordLog(
                isNull(),
                eq("PAYMENT_REFUND"),
                eq("Payment"),
                eq("1001"),
                isNull(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Dữ liệu nhạy cảm password và CCCD được mask tự động theo PDPD trong newValues")
    void sensitiveRequest_shouldMaskPasswordAndCitizenId() {
        TestSensitiveRequest request = new TestSensitiveRequest("staff_nam", "Secret123!", "001089123456");
        String result = proxyService.createUser(request);
        assertThat(result).isEqualTo("USER_CREATED");

        ArgumentCaptor<String> newValuesCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditLogService, times(1)).recordLog(
                isNull(),
                eq("USER_REGISTER"),
                eq("User"),
                eq("staff_nam"),
                isNull(),
                newValuesCaptor.capture(),
                anyString(),
                anyString()
        );

        String json = newValuesCaptor.getValue();
        assertThat(json).doesNotContain("Secret123!");
        assertThat(json).contains("[PROTECTED]");
        assertThat(json).doesNotContain("001089123456");
        assertThat(json).contains("001******456");
    }

    @Test
    @DisplayName("Fail-Safe: Nếu AuditLogService ném ngoại lệ, luồng nghiệp vụ chính vẫn thành công không bị gián đoạn")
    void failSafe_whenAuditServiceThrows_businessMethodStillSucceeds() {
        doThrow(new RuntimeException("Database disk full"))
                .when(auditLogService).recordLog(any(), any(), any(), any(), any(), any(), any(), any());

        TestSwapRequest request = new TestSwapRequest("201", "202", "Khách yêu cầu");
        String result = proxyService.swapRoom(700L, request);

        assertThat(result).isEqualTo("SWAP_SUCCESS_700");
    }
}
