package vn.edu.utc.hotel_booking.modules.ai.demo.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview.Status;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.Snapshot;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.DeterministicGroupRoomAssignmentEngine;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.GroupRoomAssignmentPreviewService;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fixed-clock synthetic preview. Never reserves, assigns or releases a real room. */
public final class GroupDemoService {
    private static final int MAX_BYTES = 128_000;
    private static final Instant REFERENCE = Instant.parse("2026-10-31T02:00:00Z");
    private record CaseData(GroupRoomAssignmentPreviewRequest request, Snapshot snapshot) { }
    private final Map<Scenario, CaseData> cases;
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

    public GroupDemoService() { this(readCorpus()); }
    GroupDemoService(byte[] raw) {
        try {
            if (raw == null || raw.length > MAX_BYTES || !HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw))
                    .equals("8d569695a769c55b9bc94ec465d47063555a87917eb47b415a1341405d1994cf")) throw invalidCorpus();
            var mapper = new ObjectMapper().findAndRegisterModules();
            JsonNode root = mapper.readTree(raw);
            if (!root.path("dataMode").asText().equals("SYNTHETIC") || !root.path("schemaVersion").asText().equals("group-preview-local-v1")
                    || !root.path("referenceInstant").asText().equals(REFERENCE.toString()) || !root.path("cases").isArray() || root.path("cases").size() != 7) throw invalidCorpus();
            Map<String, JsonNode> nodes = new HashMap<>();
            for (JsonNode node : root.path("cases")) {
                if (node.has("expected") || nodes.putIfAbsent(node.path("id").asText(), node) != null) throw invalidCorpus();
            }
            Map<Scenario, CaseData> parsed = new EnumMap<>(Scenario.class);
            for (Scenario scenario : Scenario.values()) {
                JsonNode node = nodes.get(scenario.sourceId);
                if (node == null) throw invalidCorpus();
                var request = mapper.treeToValue(node.path("request"), GroupRoomAssignmentPreviewRequest.class);
                var snapshot = mapper.treeToValue(node.path("snapshot"), Snapshot.class);
                if (request == null || snapshot == null || snapshot.scope() == null || !"SYNTHETIC".equals(snapshot.dataMode())
                        || !"SYNTHETIC_PREVIEWABLE_ROOMS_V1".equals(snapshot.basis())) throw invalidCorpus();
                parsed.put(scenario, new CaseData(request, snapshot));
            }
            cases = Map.copyOf(parsed);
        } catch (Exception exception) { throw invalidCorpus(); }
    }
    private static byte[] readCorpus() {
        try (var input = new ClassPathResource("ai/demo/group-preview-v1.json").getInputStream()) {
            return input.readNBytes(MAX_BYTES + 1);
        } catch (Exception exception) { throw invalidCorpus(); }
    }
    private static IllegalStateException invalidCorpus() { return new IllegalStateException("Synthetic group demo corpus unavailable"); }

    public DemoResponse replay(String id) {
        requireAdmin();
        Scenario scenario;
        try { scenario = Scenario.valueOf(id == null ? "" : id); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Invalid demo scenario"); }
        CaseData data = cases.get(scenario);
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
