package vn.edu.utc.hotel_booking.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakErrorCodeTest {

    @Test
    @DisplayName("Verify Keycloak Error Codes and HTTP status mappings")
    void testKeycloakErrorCodes() {
        assertThat(ErrorCode.KEYCLOAK_USER_CREATION_FAILED.getCode()).isEqualTo(3026);
        assertThat(ErrorCode.KEYCLOAK_USER_CREATION_FAILED.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);

        assertThat(ErrorCode.KEYCLOAK_USER_UPDATE_FAILED.getCode()).isEqualTo(3027);
        assertThat(ErrorCode.KEYCLOAK_USER_UPDATE_FAILED.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);

        assertThat(ErrorCode.KEYCLOAK_USER_DELETE_FAILED.getCode()).isEqualTo(3028);
        assertThat(ErrorCode.KEYCLOAK_USER_DELETE_FAILED.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);

        assertThat(ErrorCode.KEYCLOAK_COMMUNICATION_ERROR.getCode()).isEqualTo(3029);
        assertThat(ErrorCode.KEYCLOAK_COMMUNICATION_ERROR.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        assertThat(ErrorCode.KEYCLOAK_USER_ALREADY_EXISTS.getCode()).isEqualTo(3030);
        assertThat(ErrorCode.KEYCLOAK_USER_ALREADY_EXISTS.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
