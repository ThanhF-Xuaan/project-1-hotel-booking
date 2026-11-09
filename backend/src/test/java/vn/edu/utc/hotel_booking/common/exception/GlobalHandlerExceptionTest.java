package vn.edu.utc.hotel_booking.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.edu.utc.hotel_booking.common.dto.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalHandlerExceptionTest {

    private GlobalHandlerException handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalHandlerException();
    }

    @Test
    @DisplayName("handlingDataIntegrityViolation: Nhận diện chính xác constraint regions_code_key và trả về message thân thiện")
    void handlingDataIntegrityViolation_regionsConstraint() {
        String rawDbError = "ERROR: duplicate key value violates unique constraint \"regions_code_key\"\n Detail: Key (code)=(NORTH) already exists.";
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Data error", new RuntimeException(rawDbError));

        ResponseEntity<ApiResponse<Void>> response = handler.handlingDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.REGION_CODE_ALREADY_EXISTS.getCode());
        assertThat(response.getBody().getMessage()).contains("Mã khu vực này đã tồn tại");
        assertThat(response.getBody().getMessage()).doesNotContain("ERROR");
    }

    @Test
    @DisplayName("handlingDataIntegrityViolation: Nhận diện constraint uq_regions_code_active mới")
    void handlingDataIntegrityViolation_newRegionsPartialIndex() {
        String rawDbError = "ERROR: duplicate key value violates unique constraint \"uq_regions_code_active\"\n Detail: Key (code)=(SOUTH) already exists.";
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Data error", new RuntimeException(rawDbError));

        ResponseEntity<ApiResponse<Void>> response = handler.handlingDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.REGION_CODE_ALREADY_EXISTS.getCode());
        assertThat(response.getBody().getMessage()).contains("Mã khu vực này đã tồn tại");
    }

    @Test
    @DisplayName("handlingDataIntegrityViolation: Nhận diện constraint phòng ban departments_code_key")
    void handlingDataIntegrityViolation_departmentsConstraint() {
        String rawDbError = "ERROR: duplicate key value violates unique constraint \"departments_code_key\"\n Detail: Key (code)=(HR) already exists.";
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Data error", new RuntimeException(rawDbError));

        ResponseEntity<ApiResponse<Void>> response = handler.handlingDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.DEPARTMENT_CODE_ALREADY_EXISTS.getCode());
        assertThat(response.getBody().getMessage()).contains("Mã phòng ban này đã tồn tại");
    }

    @Test
    @DisplayName("handlingDataIntegrityViolation: Nhận diện constraint số phòng uk_room_number")
    void handlingDataIntegrityViolation_roomNumberConstraint() {
        String rawDbError = "ERROR: duplicate key value violates unique constraint \"uk_room_number\"\n Detail: Key (hotel_id, room_number)=(1, 101) already exists.";
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Data error", new RuntimeException(rawDbError));

        ResponseEntity<ApiResponse<Void>> response = handler.handlingDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.ROOM_NUMBER_ALREADY_EXISTS.getCode());
        assertThat(response.getBody().getMessage()).contains("Số phòng này đã tồn tại");
    }

    @Test
    @DisplayName("handlingDataIntegrityViolation: Fallback trích xuất giá trị trường khi gặp constraint không định danh sẵn")
    void handlingDataIntegrityViolation_fallbackExtraction() {
        String rawDbError = "ERROR: duplicate key value violates unique constraint \"some_unknown_constraint\"\n Detail: Key (custom_field)=(VALUE_X) already exists.";
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Data error", new RuntimeException(rawDbError));

        ResponseEntity<ApiResponse<Void>> response = handler.handlingDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("VALUE_X");
        assertThat(response.getBody().getMessage()).contains("custom_field");
        assertThat(response.getBody().getMessage()).doesNotContain("ERROR:");
    }

    @Test
    @DisplayName("handlingDataIntegrityViolation: Fallback chung an toàn khi không có chi tiết cụ thể")
    void handlingDataIntegrityViolation_genericFallback() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unknown integrity error");

        ResponseEntity<ApiResponse<Void>> response = handler.handlingDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc");
    }
}

