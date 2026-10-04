package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityReadingResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityMeter;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityReading;
import vn.edu.utc.hotel_booking.modules.operation.mapper.UtilityReadingMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityMeterRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityReadingRepository;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.UtilityReadingServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilityReadingServiceTest {

    @Mock
    private UtilityReadingRepository readingRepository;

    @Mock
    private UtilityMeterRepository meterRepository;

    @Mock
    private UtilityReadingMapper readingMapper;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private UtilityReadingServiceImpl readingService;

    private UtilityMeter meter;
    private UtilityReading reading;
    private UtilityReadingResponse readingResponse;

    @BeforeEach
    void setUp() {
        meter = UtilityMeter.builder()
                .id(1)
                .meterCode("EL-01")
                .build();
        meter.setIsDeleted(false);

        reading = UtilityReading.builder()
                .id(1L)
                .meter(meter)
                .readingDate(LocalDate.of(2023, 10, 1))
                .readingValue(new BigDecimal("100.5"))
                .isMeterReset(false)
                .build();
        reading.setIsDeleted(false);

        readingResponse = new UtilityReadingResponse();
        readingResponse.setId(1L);
        readingResponse.setMeterId(1);
        readingResponse.setReadingDate(LocalDate.of(2023, 10, 1));
        readingResponse.setReadingValue(new BigDecimal("100.5"));
        readingResponse.setIsMeterReset(false);
    }

    @Test
    @DisplayName("Happy path: Tạo chỉ số đọc thành công và tính toán usage khi không có previous reading")
    void create_Success_NoPreviousReading() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 1));
        request.setReadingValue(new BigDecimal("100.5"));
        request.setIsMeterReset(false);

        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 1))).thenReturn(false);
        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingMapper.toEntity(request)).thenReturn(reading);
        when(readingRepository.save(reading)).thenReturn(reading);
        when(readingMapper.toResponse(reading)).thenReturn(readingResponse);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());

        UtilityReadingResponse result = readingService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsage()).isEqualTo(new BigDecimal("100.5")); // Fallback to current value
        verify(readingRepository).save(reading);
    }

    @Test
    @DisplayName("Happy path: Tạo chỉ số đọc thành công và tính toán usage với previous reading")
    void create_Success_WithPreviousReading() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 2));
        request.setReadingValue(new BigDecimal("150.5"));
        request.setIsMeterReset(false);

        UtilityReading newReading = UtilityReading.builder()
                .id(2L)
                .meter(meter)
                .readingDate(LocalDate.of(2023, 10, 2))
                .readingValue(new BigDecimal("150.5"))
                .isMeterReset(false)
                .build();
        newReading.setIsDeleted(false);

        UtilityReadingResponse newResponse = new UtilityReadingResponse();
        newResponse.setId(2L);
        newResponse.setReadingValue(new BigDecimal("150.5"));

        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 2))).thenReturn(false);
        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingMapper.toEntity(request)).thenReturn(newReading);
        when(readingRepository.save(newReading)).thenReturn(newReading);
        when(readingMapper.toResponse(newReading)).thenReturn(newResponse);
        
        // Mock previous reading
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 2))).thenReturn(Optional.of(reading));

        UtilityReadingResponse result = readingService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsage()).isEqualTo(new BigDecimal("50.0")); // 150.5 - 100.5
        verify(readingRepository).save(newReading);
    }

    @Test
    @DisplayName("Validation path: Lỗi khi trùng chỉ số trong ngày")
    void create_ThrowsWhenReadingAlreadyExists() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 1));
        request.setReadingValue(new BigDecimal("100.5"));
        request.setIsMeterReset(false);

        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 1))).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> readingService.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UTILITY_READING_ALREADY_EXISTS);
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validation path: Lỗi khi đồng hồ không tồn tại")
    void create_ThrowsWhenMeterNotFound() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(99);
        request.setReadingDate(LocalDate.of(2023, 10, 1));

        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(anyInt(), any(LocalDate.class))).thenReturn(false);
        when(meterRepository.findById(99)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> readingService.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UTILITY_METER_NOT_FOUND);
    }

    @Test
    @DisplayName("Edge case: Phân trang trả về rỗng")
    void search_Empty() {
        UtilityReadingSearchDto searchDto = UtilityReadingSearchDto.builder()
                .page(0)
                .pageSize(10)
                .build();

        Page<UtilityReading> emptyPage = new PageImpl<>(Collections.emptyList());
        when(readingRepository.search(isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<UtilityReadingResponse> result = readingService.search(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Validation path: Lỗi khi chỉ số đọc nhỏ hơn chỉ số trước đó mà không có cờ reset")
    void create_ThrowsWhenReadingValueDecreasedWithoutReset() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 2));
        request.setReadingValue(new BigDecimal("80.0")); // Smaller than previous reading (100.5)
        request.setIsMeterReset(false);

        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 2))).thenReturn(false);
        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 2))).thenReturn(Optional.of(reading));

        AppException ex = assertThrows(AppException.class, () -> readingService.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_UTILITY_READING_VALUE);
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Happy path: Chỉ số đọc nhỏ hơn chỉ số trước đó nhưng có cờ reset thì hợp lệ")
    void create_Success_WhenReadingValueDecreasedWithReset() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 2));
        request.setReadingValue(new BigDecimal("20.0"));
        request.setIsMeterReset(true);

        UtilityReading resetReading = UtilityReading.builder()
                .id(3L)
                .meter(meter)
                .readingDate(LocalDate.of(2023, 10, 2))
                .readingValue(new BigDecimal("20.0"))
                .isMeterReset(true)
                .build();
        resetReading.setIsDeleted(false);

        UtilityReadingResponse resetResponse = new UtilityReadingResponse();
        resetResponse.setId(3L);
        resetResponse.setReadingValue(new BigDecimal("20.0"));
        resetResponse.setIsMeterReset(true);

        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 2))).thenReturn(false);
        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 2))).thenReturn(Optional.of(reading));
        when(readingMapper.toEntity(request)).thenReturn(resetReading);
        when(readingRepository.save(resetReading)).thenReturn(resetReading);
        when(readingMapper.toResponse(resetReading)).thenReturn(resetResponse);

        UtilityReadingResponse result = readingService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsage()).isEqualTo(new BigDecimal("20.0"));
        verify(readingRepository).save(resetReading);
    }

    @Test
    @DisplayName("Happy path: Xóa mềm hàng loạt bản ghi đọc chỉ số")
    void delete_Success_SoftDelete() {
        when(readingRepository.findAllById(List.of(1L))).thenReturn(List.of(reading));

        readingService.delete(List.of(1L));

        assertThat(reading.getIsDeleted()).isTrue();
        verify(readingRepository).saveAll(List.of(reading));
    }
}
