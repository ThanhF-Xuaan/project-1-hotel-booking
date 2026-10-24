package vn.edu.utc.hotel_booking.modules.ai.advisor.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Standalone JACKSON tests for the advisor-local error DTO.
 *
 * <p>The read-only frozen R1-00 schema is loaded from the repository so this suite proves the real
 * serialized tree, not a duplicated hand-written expectation. The 429/503 examples are serialization
 * fixtures only: TASK-002 does not implement those HTTP handlers or their future collaborators.</p>
 */
class RoomAdvisorErrorResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SCHEMA_RELATIVE_PATH =
            "docs/t12/r1-00/v2.0.0-draft.1/schemas/advisor.schema.json";

    @Test
    @DisplayName("serialized 404 body matches every frozen ErrorResponse constraint")
    void serializesExactlyFrozenErrorShape() throws Exception {
        JsonNode schema = loadErrorResponseSchema();
        JsonNode serialized = serialize(RoomAdvisorErrorResponse.forReason(
                404, RoomAdvisorErrorResponse.Reason.SESSION_NOT_FOUND, false));

        assertExactKeys(serialized, schema);
        assertThat(serialized.path("code").isInt()).isTrue();
        assertThat(serialized.path("code").asInt())
                .isBetween(schema.path("properties").path("code").path("minimum").asInt(),
                        schema.path("properties").path("code").path("maximum").asInt());
        assertThat(serialized.path("message").isTextual()).isTrue();
        assertThat(serialized.path("message").asText())
                .isNotBlank()
                .hasSizeLessThanOrEqualTo(schema.path("properties").path("message").path("maxLength").asInt());

        JsonNode error = serialized.path("error");
        assertThat(error.path("contractVersion").asText())
                .isEqualTo(schema.path("properties").path("error").path("properties")
                        .path("contractVersion").path("const").asText());
        assertThat(UUID.fromString(error.path("requestId").asText())).isNotNull();
        assertThat(error.path("retryable").isBoolean()).isTrue();

        assertThat(serialized.has("data")).isFalse();
        assertThat(serialized.has("result")).isFalse();
    }

    @Test
    @DisplayName("DTO exposes exactly the seven frozen reasons and no extra value")
    void usesOnlyFrozenReasonEnumValues() throws Exception {
        JsonNode schema = loadErrorResponseSchema();
        Set<String> schemaReasons = new LinkedHashSet<>();
        schema.path("properties").path("error").path("properties").path("reason").path("enum")
                .forEach(node -> schemaReasons.add(node.asText()));

        Set<String> dtoReasons = new LinkedHashSet<>();
        for (RoomAdvisorErrorResponse.Reason reason : RoomAdvisorErrorResponse.Reason.values()) {
            dtoReasons.add(reason.name());
        }

        assertThat(dtoReasons).containsExactlyInAnyOrderElementsOf(schemaReasons);
        assertThat(dtoReasons).hasSize(7);
    }

    @Test
    @DisplayName("all seven approved reasons serialize with the frozen reason name")
    void serializesAllApprovedReasons() throws Exception {
        Map<RoomAdvisorErrorResponse.Reason, Integer> statusByReason = new LinkedHashMap<>();
        statusByReason.put(RoomAdvisorErrorResponse.Reason.INVALID_REQUEST, 400);
        statusByReason.put(RoomAdvisorErrorResponse.Reason.SESSION_NOT_FOUND, 404);
        statusByReason.put(RoomAdvisorErrorResponse.Reason.SESSION_VERSION_CONFLICT, 409);
        statusByReason.put(RoomAdvisorErrorResponse.Reason.SCOPE_DENIED, 403);
        statusByReason.put(RoomAdvisorErrorResponse.Reason.RATE_LIMITED, 429);
        statusByReason.put(RoomAdvisorErrorResponse.Reason.UPSTREAM_UNAVAILABLE, 503);
        statusByReason.put(RoomAdvisorErrorResponse.Reason.AUTH_REQUIRED, 401);

        JsonNode schema = loadErrorResponseSchema();
        Set<String> schemaReasons = new LinkedHashSet<>();
        schema.path("properties").path("error").path("properties").path("reason").path("enum")
                .forEach(node -> schemaReasons.add(node.asText()));

        for (Map.Entry<RoomAdvisorErrorResponse.Reason, Integer> entry : statusByReason.entrySet()) {
            JsonNode serialized = serialize(RoomAdvisorErrorResponse.forReason(
                    entry.getValue(), entry.getKey(), false));
            String reason = serialized.path("error").path("reason").asText();
            assertThat(reason).isEqualTo(entry.getKey().name());
            assertThat(schemaReasons).contains(reason);
            assertThat(serialized.path("code").asInt()).isEqualTo(entry.getValue());
        }
    }

    @Test
    @DisplayName("synthetic 429 and 503 fixtures serialize without future HTTP collaborators")
    void serializesSyntheticRateLimitAndStorageFailureBodies() throws Exception {
        JsonNode limited = serialize(RoomAdvisorErrorResponse.forReason(
                429, RoomAdvisorErrorResponse.Reason.RATE_LIMITED, true));
        JsonNode unavailable = serialize(RoomAdvisorErrorResponse.forReason(
                503, RoomAdvisorErrorResponse.Reason.UPSTREAM_UNAVAILABLE, true));

        assertThat(limited.path("code").asInt()).isEqualTo(429);
        assertThat(limited.path("error").path("reason").asText()).isEqualTo("RATE_LIMITED");
        assertThat(limited.path("error").path("retryable").asBoolean()).isTrue();

        assertThat(unavailable.path("code").asInt()).isEqualTo(503);
        assertThat(unavailable.path("error").path("reason").asText()).isEqualTo("UPSTREAM_UNAVAILABLE");
        assertThat(unavailable.path("error").path("retryable").asBoolean()).isTrue();

        assertExactKeys(limited, loadErrorResponseSchema());
        assertExactKeys(unavailable, loadErrorResponseSchema());
    }

    @Test
    @DisplayName("DTO rejects values that the frozen schema would reject")
    void rejectsOutOfSchemaValues() {
        assertThatThrownBy(() -> new RoomAdvisorErrorResponse(
                399,
                "invalid",
                new RoomAdvisorErrorResponse.ErrorDetail(
                        "2.0", UUID.randomUUID(), RoomAdvisorErrorResponse.Reason.INVALID_REQUEST, false)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RoomAdvisorErrorResponse(
                400,
                "x".repeat(201),
                new RoomAdvisorErrorResponse.ErrorDetail(
                        "2.0", UUID.randomUUID(), RoomAdvisorErrorResponse.Reason.INVALID_REQUEST, false)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RoomAdvisorErrorResponse.ErrorDetail(
                "1.0", UUID.randomUUID(), RoomAdvisorErrorResponse.Reason.INVALID_REQUEST, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static JsonNode serialize(RoomAdvisorErrorResponse response) throws IOException {
        return OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(response));
    }

    private static void assertExactKeys(JsonNode body, JsonNode schema) {
        assertThat(fieldNames(body))
                .containsExactlyInAnyOrderElementsOf(fieldNames(schema.path("properties")));
        assertThat(fieldNames(body.path("error")))
                .containsExactlyInAnyOrderElementsOf(
                        fieldNames(schema.path("properties").path("error").path("properties")));
        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("properties").path("error").path("additionalProperties").asBoolean()).isFalse();
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static JsonNode loadErrorResponseSchema() throws IOException {
        Path schemaPath = repositoryRoot().resolve(SCHEMA_RELATIVE_PATH);
        assertThat(Files.isRegularFile(schemaPath))
                .as("frozen R1-00 advisor schema must exist at %s", schemaPath)
                .isTrue();
        JsonNode schema = OBJECT_MAPPER.readTree(Files.readString(schemaPath));
        JsonNode errorResponse = schema.path("$defs").path("ErrorResponse");
        assertThat(errorResponse.isObject())
                .as("$defs.ErrorResponse must exist in the frozen schema")
                .isTrue();
        return errorResponse;
    }

    private static Path repositoryRoot() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            if (Files.isRegularFile(directory.resolve(SCHEMA_RELATIVE_PATH))
                    && Files.isDirectory(directory.resolve("backend"))) {
                return directory;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("Unable to locate repository root from " + Path.of("").toAbsolutePath());
    }
}
