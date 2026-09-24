package vn.edu.utc.hotel_booking.iam;

import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService;
import vn.edu.utc.hotel_booking.iam.service.StaffAccessService.StaffActor;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaffAccessServiceTest {
    private final StaffAccessService policy = new StaffAccessService(null);

    @Test
    void regionScopeOnlyAllowsHotelsInRegion() {
        var actor = new StaffActor(1, "REGION", 2, "REGION_MANAGER", Set.of("VIEW:INVENTORY"));
        assertDoesNotThrow(() -> policy.requireHotel(actor, "VIEW:INVENTORY", 17, 2));
        assertThrows(AppException.class, () -> policy.requireHotel(actor, "VIEW:INVENTORY", 17, 3));
    }

    @Test
    void permissionIsRequiredEvenForChainScope() {
        var actor = new StaffActor(1, "CHAIN", null, "CHAIN_ADMIN", Set.of());
        assertThrows(AppException.class, () -> policy.requireHotel(actor, "VIEW:INVENTORY", 17, 3));
    }
}
