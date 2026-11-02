package vn.edu.utc.hotel_booking.modules.ai.demo.group;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview.Status;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.Snapshot;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.DeterministicGroupRoomAssignmentEngine;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.GroupRoomAssignmentPreviewService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Fixed-clock synthetic preview. Never reserves, assigns or releases a real room. */
public final class GroupDemoService {
    private static final int MAX_BYTES = 128_000;
    private static final Instant REFERENCE = Instant.parse("2026-10-31T02:00:00Z");
    private final vn.edu.utc.hotel_booking.modules.ai.demo.data.DemoDataProvider provider;
    private enum Scenario {
        FIT_AND_TIE("P01", "Đủ sức chứa, ưu tiên giường và tầng"),
        QUOTA_ONE("P11_QUOTA_ONE", "Quota chỉ đủ một phòng"),
        MIDDLE_NIGHT_LIMIT("P12_MIDDLE_BOTTLENECK", "Giới hạn ở đêm giữa"),
        ZERO_QUOTA("P14_ZERO", "Quota bằng 0"),
        QUOTA_CONTROL("P14_CONTROL", "Quota đủ cho hai yêu cầu"),
        NO_TRANSFER_BLOCK("P14_NO_TRANSFER_roomBlockId", "Không chuyển quota giữa block"),
        NO_TRANSFER_ROOM_TYPE("P14_NO_TRANSFER_hotelRoomTypeId", "Không chuyển quota giữa loại phòng");
        final String sourceId, label;
        Scenario(String sourceId, String label) { this.sourceId = sourceId; this.label = label; }
    }

    public GroupDemoService() { this(new vn.edu.utc.hotel_booking.modules.ai.demo.data.FixtureDemoDataProvider()); }
    public GroupDemoService(vn.edu.utc.hotel_booking.modules.ai.demo.data.DemoDataProvider provider) { this.provider=provider; }
    GroupDemoService(byte[] raw) { this(parseFixture(raw)); }
    private static vn.edu.utc.hotel_booking.modules.ai.demo.data.DemoDataProvider parseFixture(byte[] raw) {
        try { return new vn.edu.utc.hotel_booking.modules.ai.demo.data.FixtureDemoDataProvider(raw); }
        catch (RuntimeException e) { throw new IllegalStateException("Synthetic group demo corpus unavailable"); }
    }

    public DemoResponse replay(String id) {
        requireAdmin();
        Scenario scenario;
        try { scenario = Scenario.valueOf(id == null ? "" : id); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Invalid demo scenario"); }
        return provider.group(scenario.sourceId, data -> replayData(scenario,data));
    }
    private DemoResponse replayData(Scenario scenario, vn.edu.utc.hotel_booking.modules.ai.demo.data.DemoDataProvider.GroupData data) {
        var scope = data.snapshot().scope();
        var service = new GroupRoomAssignmentPreviewService((hotel, contract) -> isAdmin() && hotel == scope.hotelId() && contract == scope.contractId()
                ? Optional.of(scope) : Optional.empty(), (requestedScope, from, to, asOf) -> {
                    if (!isAdmin() || !requestedScope.equals(scope) || !from.equals(data.request().checkInDate())
                            || !to.equals(data.request().checkOutDate()) || !REFERENCE.equals(asOf)) throw new IllegalStateException("Invalid demo scope");
                    return data.snapshot();
                }, new DeterministicGroupRoomAssignmentEngine(), Clock.fixed(REFERENCE, ZoneOffset.UTC));
        RoomAssignmentPreview result = service.preview(data.request());
        requireAdmin();
        if (result.status() != Status.COMPLETE) throw new IllegalStateException("Group demo preview unavailable");
        List<ScenarioView> choices = Arrays.stream(Scenario.values()).map(value -> new ScenarioView(value.name(), value.label)).toList();
        return new DemoResponse("SYNTHETIC", false, true, scenario.name(),
                "Phương án mô phỏng theo mốc cố định; không phải phòng trống thật, không giữ phòng hoặc phân phòng nghiệp vụ.",
                REFERENCE, choices, data.request(), data.snapshot(), result, result.unmet().isEmpty());
    }
    private boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CHAIN_ADMIN".equals(authority.getAuthority()));
    }
    private void requireAdmin() { if (!isAdmin()) throw new AccessDeniedException("Demo requires chain administrator"); }
    public record ScenarioView(String id, String label) { }
    public record DemoResponse(String dataMode, boolean businessData, boolean previewOnly, String scenario, String notice,
                               Instant referenceInstant, List<ScenarioView> scenarios, GroupRoomAssignmentPreviewRequest request,
                               Snapshot snapshot, RoomAssignmentPreview preview, boolean allDemandsAssigned) { }
}
