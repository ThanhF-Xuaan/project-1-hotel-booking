package vn.edu.utc.hotel_booking.modules.ai.groupassignment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.model.RoomAssignmentPreview.Status;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.Snapshot;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.service.DeterministicGroupRoomAssignmentEngine;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class GroupPreviewFixtureTest {
    static Stream<Arguments> cases() throws Exception {
        byte[] bytes;
        try (var input = GroupPreviewFixtureTest.class.getResourceAsStream("/ai/group-assignment/preview-cases.json")) {
            assertThat(input).isNotNull(); bytes = input.readAllBytes();
        }
        assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
                .isEqualTo("2dbd7da729831b0f21cc53c21412bb744fd8e620aaa050c6fe9a2b2a89ddc476");
        JsonNode root = new ObjectMapper().readTree(bytes);
        assertThat(root.get("dataMode").asText()).isEqualTo("SYNTHETIC");
        assertThat(root.get("cases")).hasSize(7);
        return StreamSupport.stream(root.get("cases").spliterator(), false)
                .map(node -> Arguments.of(node.get("id").asText(), root.get("asOf").asText(), node));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void reviewedC01OraclesRemainTrue(String id, String asOf, JsonNode testCase) throws Exception {
        var mapper = new ObjectMapper().findAndRegisterModules();
        for (JsonNode quota : testCase.get("snapshot").get("quotaRows")) assertThat(quota.get("previewableRoomCount").isIntegralNumber()).isTrue();
        var request = mapper.treeToValue(testCase.get("request"), GroupRoomAssignmentPreviewRequest.class);
        var snapshot = mapper.treeToValue(testCase.get("snapshot"), Snapshot.class);
        var expected = testCase.get("expected");
        var result = new DeterministicGroupRoomAssignmentEngine().preview(request, snapshot, Instant.parse(asOf));
        assertThat(result.status()).isEqualTo(Status.COMPLETE);
        assertThat(result.previewOnly()).isTrue(); assertThat(result.dataMode()).isEqualTo("SYNTHETIC");
        JsonNode assignments = mapper.valueToTree(result.assignments());
        JsonNode unmetKeys = mapper.valueToTree(result.unmet().stream().map(unmet -> unmet.demandKey()).toList());
        assertThat(assignments).isEqualTo(expected.get("assignments"));
        assertThat(unmetKeys).isEqualTo(expected.get("unmetKeys"));
        assertThat(result.objective().assignedCount()).isEqualTo(expected.get("assignedCount").asInt());
        assertThat(result.objective().bedWaste()).isEqualTo(expected.get("bedWaste").asLong());
        assertThat(result.objective().floorSpan()).isEqualTo(expected.get("floorSpan").asLong());
    }
}
