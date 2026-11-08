package vn.edu.utc.hotel_booking.modules.identity.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import vn.edu.utc.hotel_booking.modules.identity.dto.response.AuditLogResponse;
import vn.edu.utc.hotel_booking.modules.identity.entity.AuditLog;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogMapperTest {

    private final AuditLogMapper mapper = Mappers.getMapper(AuditLogMapper.class);

    @Test
    @DisplayName("Ánh xạ AuditLog của Staff: trích xuất targetResource và category STAFF")
    void toResponse_StaffEntity_Success() {
        AuditLog log = AuditLog.builder()
                .id(1L)
                .actionType("CREATE")
                .entityName("Staff")
                .entityId("5")
                .newValues("{\"targetAccount\":\"Nguyen Van B (@vanb)\",\"actionDescription\":\"Tạo mới tài khoản nhân sự\"}")
                .createdAt(OffsetDateTime.now())
                .build();

        AuditLogResponse res = mapper.toResponse(log);

        assertThat(res).isNotNull();
        assertThat(res.getTargetResource()).isEqualTo("Nguyen Van B (@vanb)");
        assertThat(res.getResourceCategory()).isEqualTo("STAFF");
        assertThat(res.getDescription()).isEqualTo("Tạo mới tài khoản nhân sự");
    }

    @Test
    @DisplayName("Ánh xạ AuditLog của PricingRule: trích xuất tên quy tắc giá và category PRICING")
    void toResponse_PricingRuleEntity_Success() {
        AuditLog log = AuditLog.builder()
                .id(2L)
                .actionType("PRICING_RULE_CREATE")
                .entityName("PricingRule")
                .entityId("12")
                .newValues("{\"actionDescription\":\"Tạo mới quy tắc giá: Mùa hè 2026\",\"request\":{\"name\":\"Mùa hè 2026\"}}")
                .createdAt(OffsetDateTime.now())
                .build();

        AuditLogResponse res = mapper.toResponse(log);

        assertThat(res).isNotNull();
        assertThat(res.getTargetResource()).isEqualTo("Mùa hè 2026 (#12)");
        assertThat(res.getResourceCategory()).isEqualTo("PRICING");
        assertThat(res.getDescription()).isEqualTo("Tạo mới quy tắc giá: Mùa hè 2026");
    }

    @Test
    @DisplayName("Ánh xạ AuditLog của Booking: trích xuất mã đơn và category BOOKING")
    void toResponse_BookingEntity_Success() {
        AuditLog log = AuditLog.builder()
                .id(3L)
                .actionType("BOOKING_CANCELLED")
                .entityName("Booking")
                .entityId("99")
                .newValues("{\"actionDescription\":\"Hủy đơn đặt phòng. Lý do: Khách đổi lịch\",\"request\":{\"bookingCode\":\"BK-20261108-001\"}}")
                .createdAt(OffsetDateTime.now())
                .build();

        AuditLogResponse res = mapper.toResponse(log);

        assertThat(res).isNotNull();
        assertThat(res.getTargetResource()).isEqualTo("Đơn BK-20261108-001");
        assertThat(res.getResourceCategory()).isEqualTo("BOOKING");
        assertThat(res.getDescription()).isEqualTo("Hủy đơn đặt phòng. Lý do: Khách đổi lịch");
    }

    @Test
    @DisplayName("Ánh xạ AuditLog của Payment: trích xuất mã giao dịch và category FINANCE")
    void toResponse_PaymentEntity_Success() {
        AuditLog log = AuditLog.builder()
                .id(4L)
                .actionType("PAYMENT_REFUND")
                .entityName("Payment")
                .entityId("77")
                .newValues("{\"actionDescription\":\"Hoàn tiền giao dịch ID 77: 500.000 đ\"}")
                .createdAt(OffsetDateTime.now())
                .build();

        AuditLogResponse res = mapper.toResponse(log);

        assertThat(res).isNotNull();
        assertThat(res.getTargetResource()).isEqualTo("Giao dịch #77");
        assertThat(res.getResourceCategory()).isEqualTo("FINANCE");
        assertThat(res.getDescription()).isEqualTo("Hoàn tiền giao dịch ID 77: 500.000 đ");
    }
}
