package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityReadingUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityReadingResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityMeter;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityReading;
import vn.edu.utc.hotel_booking.modules.operation.mapper.UtilityReadingMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityMeterRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityReadingRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.UtilityReadingServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

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

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private UtilityReadingServiceImpl readingService;

    private Region region;
    private Hotel hotel;
    private UtilityMeter meter;
    private UtilityReading reading;
    private UtilityReadingResponse readingResponse;

    @BeforeEach
    void setUp() {
        setSecurityContext("staff_test", "ROLE_CHAIN_ADMIN");

        Staff staff = Staff.builder().id(1).username("staff_test").scopeType("CHAIN").build();
        lenient().when(staffRepository.findByUsernameAndIsDeletedFalse("staff_test"))
                .thenReturn(Optional.of(staff));

        region = Region.builder().id((short) 1).name("Region 1").build();

        hotel = Hotel.builder()
                .id((short) 1)
                .region(region)
                .name("Hotel 1")
                .build();
        hotel.setIsDeleted(false);

        meter = UtilityMeter.builder()
                .id(1)
                .hotel(hotel)
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(String username, String... roles) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(username, "pwd", authorities));
        SecurityContextHolder.setContext(context);
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
        when(readingRepository.saveAndFlush(reading)).thenReturn(reading);
        when(readingMapper.toResponse(reading)).thenReturn(readingResponse);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());

        UtilityReadingResponse result = readingService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsage()).isEqualTo(new BigDecimal("100.5"));
        verify(readingRepository).saveAndFlush(reading);
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

        UtilityReadingResponse newResponse = new UtilityReadingResponse();
        newResponse.setId(2L);
        newResponse.setReadingValue(new BigDecimal("150.5"));

        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 2))).thenReturn(false);
        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 2))).thenReturn(Optional.of(reading));
        when(readingMapper.toEntity(request)).thenReturn(newReading);
        when(readingRepository.saveAndFlush(newReading)).thenReturn(newReading);
        when(readingMapper.toResponse(newReading)).thenReturn(newResponse);

        UtilityReadingResponse result = readingService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsage()).isEqualTo(new BigDecimal("50.0"));
        verify(readingRepository).saveAndFlush(newReading);
    }

    @Test
    @DisplayName("Happy path: Lấy thông tin chỉ số đọc qua ID")
    void getById_Success() {
        when(readingRepository.findByIdWithMeterAndHotelAndRegion(1L)).thenReturn(Optional.of(reading));
        when(readingMapper.toResponse(reading)).thenReturn(readingResponse);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());

        UtilityReadingResponse result = readingService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(readingRepository).findByIdWithMeterAndHotelAndRegion(1L);
    }

    @Test
    @DisplayName("Error path: Ném ngoại lệ UTILITY_READING_NOT_FOUND khi bản ghi không tồn tại")
    void getById_NotFound_ThrowsException() {
        when(readingRepository.findByIdWithMeterAndHotelAndRegion(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> readingService.getById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UTILITY_READING_NOT_FOUND);
    }

    @Test
    @DisplayName("Validation path: Lỗi khi chỉ số đọc đã tồn tại trong ngày")
    void create_ThrowsWhenReadingAlreadyExists() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 1));

        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
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
        when(readingRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

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
        request.setReadingValue(new BigDecimal("80.0"));
        request.setIsMeterReset(false);

        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 2))).thenReturn(false);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 2))).thenReturn(Optional.of(reading));

        AppException ex = assertThrows(AppException.class, () -> readingService.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_UTILITY_READING_VALUE);
    }

    @Test
    @DisplayName("Happy path: Chỉ số đọc nhỏ hơn chỉ số trước đó nhưng có cờ reset thành công")
    void create_Success_WhenReadingValueDecreasedWithReset() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 2));
        request.setReadingValue(new BigDecimal("20.0"));
        request.setIsMeterReset(true);

        UtilityReading resetReading = UtilityReading.builder()
                .id(2L)
                .meter(meter)
                .readingDate(LocalDate.of(2023, 10, 2))
                .readingValue(new BigDecimal("20.0"))
                .isMeterReset(true)
                .build();

        UtilityReadingResponse resetResponse = new UtilityReadingResponse();
        resetResponse.setId(2L);
        resetResponse.setReadingValue(new BigDecimal("20.0"));

        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 2))).thenReturn(false);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 2))).thenReturn(Optional.of(reading));
        when(readingMapper.toEntity(request)).thenReturn(resetReading);
        when(readingRepository.saveAndFlush(resetReading)).thenReturn(resetReading);
        when(readingMapper.toResponse(resetReading)).thenReturn(resetResponse);

        UtilityReadingResponse result = readingService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getUsage()).isEqualTo(new BigDecimal("20.0"));
        verify(readingRepository).saveAndFlush(resetReading);
    }

    @Test
    @DisplayName("Happy path: Xóa mềm hàng loạt bản ghi đọc chỉ số")
    void delete_Success_SoftDelete() {
        when(readingRepository.findAllByIdInWithMeterAndHotel(List.of(1L))).thenReturn(List.of(reading));

        readingService.delete(List.of(1L));

        assertThat(reading.getIsDeleted()).isTrue();
        verify(readingRepository).saveAll(List.of(reading));
    }

    @Test
    @DisplayName("Validation path: Lỗi khi chỉ số đọc lớn hơn chỉ số ngày tiếp theo mà không có cờ reset ngày tiếp theo")
    void create_ThrowsWhenReadingValueGreaterThanNextReadingWithoutReset() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 1));
        request.setReadingValue(new BigDecimal("120.0"));
        request.setIsMeterReset(false);

        UtilityReading nextReading = UtilityReading.builder()
                .id(2L)
                .meter(meter)
                .readingDate(LocalDate.of(2023, 10, 2))
                .readingValue(new BigDecimal("110.0"))
                .isMeterReset(false)
                .build();

        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 1))).thenReturn(false);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());
        when(readingRepository.findNextReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.of(nextReading));

        AppException ex = assertThrows(AppException.class, () -> readingService.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_UTILITY_READING_VALUE);
        verify(readingRepository, never()).save(any());
        verify(readingRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Happy path: Chỉ số đọc lớn hơn chỉ số tiếp theo nhưng ngày tiếp theo có cờ reset")
    void create_Success_WhenReadingValueGreaterThanNextReadingWithReset() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 1));
        request.setReadingValue(new BigDecimal("120.0"));
        request.setIsMeterReset(false);

        UtilityReading nextReading = UtilityReading.builder()
                .id(2L)
                .meter(meter)
                .readingDate(LocalDate.of(2023, 10, 2))
                .readingValue(new BigDecimal("10.0"))
                .isMeterReset(true)
                .build();

        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 1))).thenReturn(false);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());
        when(readingRepository.findNextReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.of(nextReading));
        when(readingMapper.toEntity(request)).thenReturn(reading);
        when(readingRepository.saveAndFlush(reading)).thenReturn(reading);
        when(readingMapper.toResponse(reading)).thenReturn(readingResponse);

        UtilityReadingResponse result = readingService.create(request);

        assertThat(result).isNotNull();
        verify(readingRepository).saveAndFlush(reading);
    }

    @Test
    @DisplayName("Concurrency: Bắt lỗi trùng lặp duy nhất từ DB khi 2 luồng đồng thời tạo chỉ số và ném UTILITY_READING_ALREADY_EXISTS")
    void create_ConcurrentDuplicate_ThrowsUtilityReadingAlreadyExists() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 1));
        request.setReadingValue(new BigDecimal("100.5"));
        request.setIsMeterReset(false);

        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 1))).thenReturn(false);
        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingMapper.toEntity(request)).thenReturn(reading);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());
        when(readingRepository.saveAndFlush(reading))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_utility_reading_active_per_day\""));

        AppException ex = assertThrows(AppException.class, () -> readingService.create(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UTILITY_READING_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Concurrency: Ngoại lệ DataIntegrityViolation không liên quan đến unique reading được ném lại, không bị nuốt")
    void create_OtherDataIntegrityViolation_RethrowsException() {
        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 1));
        request.setReadingValue(new BigDecimal("100.5"));
        request.setIsMeterReset(false);

        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 1))).thenReturn(false);
        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingMapper.toEntity(request)).thenReturn(reading);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());
        when(readingRepository.saveAndFlush(reading))
                .thenThrow(new DataIntegrityViolationException("insert or update on table \"utility_readings\" violates foreign key constraint \"fk_utility_readings_staff_id\""));

        assertThrows(DataIntegrityViolationException.class, () -> readingService.create(request));
    }

    @Test
    @DisplayName("Happy path: Cập nhật chỉ số đọc thành công")
    void update_Success() {
        UtilityReadingUpdateRequest request = new UtilityReadingUpdateRequest();
        request.setReadingValue(new BigDecimal("105.0"));
        request.setIsMeterReset(false);

        when(readingRepository.findByIdWithMeterAndHotelAndRegion(1L)).thenReturn(Optional.of(reading));
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());
        when(readingRepository.findNextReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());
        when(readingRepository.save(reading)).thenReturn(reading);
        when(readingMapper.toResponse(reading)).thenReturn(readingResponse);

        UtilityReadingResponse result = readingService.update(1L, request);

        assertThat(result).isNotNull();
        assertThat(reading.getReadingValue()).isEqualTo(new BigDecimal("105.0"));
        verify(readingRepository).save(reading);
    }

    @Test
    @DisplayName("Validation path: Cập nhật chỉ số đọc lớn hơn ngày tiếp theo không có cờ reset")
    void update_ThrowsWhenReadingValueGreaterThanNextReadingWithoutReset() {
        UtilityReadingUpdateRequest request = new UtilityReadingUpdateRequest();
        request.setReadingValue(new BigDecimal("150.0"));
        request.setIsMeterReset(false);

        UtilityReading nextReading = UtilityReading.builder()
                .id(2L)
                .meter(meter)
                .readingDate(LocalDate.of(2023, 10, 2))
                .readingValue(new BigDecimal("120.0"))
                .isMeterReset(false)
                .build();

        when(readingRepository.findByIdWithMeterAndHotelAndRegion(1L)).thenReturn(Optional.of(reading));
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());
        when(readingRepository.findNextReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.of(nextReading));

        AppException ex = assertThrows(AppException.class, () -> readingService.update(1L, request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_UTILITY_READING_VALUE);
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validation path: Lỗi STAFF_NOT_FOUND khi không xác định được nhân viên thực hiện")
    void create_ThrowsWhenStaffNotFound() {
        setSecurityContext("unknown_staff", "ROLE_CHAIN_ADMIN");
        when(staffRepository.findByUsernameAndIsDeletedFalse("unknown_staff")).thenReturn(Optional.empty());

        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 1));
        request.setReadingValue(new BigDecimal("100.5"));
        request.setIsMeterReset(false);

        when(meterRepository.findById(1)).thenReturn(Optional.of(meter));
        when(readingRepository.existsByMeterIdAndReadingDateAndIsDeletedFalse(1, LocalDate.of(2023, 10, 1))).thenReturn(false);
        when(readingRepository.findPreviousReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());
        when(readingRepository.findNextReading(1, LocalDate.of(2023, 10, 1))).thenReturn(Optional.empty());
        when(readingMapper.toEntity(request)).thenReturn(reading);

        AppException ex = assertThrows(AppException.class, () -> readingService.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STAFF_NOT_FOUND);
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER tự động giới hạn tìm kiếm theo khách sạn của mình")
    void search_PropertyManager_AutoRestrictsToStaffHotel() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(1).build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));

        Page<UtilityReading> emptyPage = new PageImpl<>(Collections.emptyList());
        when(readingRepository.search(isNull(), eq((short) 1), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        UtilityReadingSearchDto request = UtilityReadingSearchDto.builder().build();
        readingService.search(request);

        verify(readingRepository).search(isNull(), eq((short) 1), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER tìm kiếm chỉ số khách sạn khác bị từ chối")
    void search_PropertyManager_OtherHotel_ThrowsUnauthorized() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(1).build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));

        UtilityReadingSearchDto request = UtilityReadingSearchDto.builder().hotelId((short) 2).build();

        AppException ex = assertThrows(AppException.class, () -> readingService.search(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Scope: REGION_MANAGER tự động giới hạn tìm kiếm theo vùng")
    void search_RegionManager_RestrictsToRegion() {
        setSecurityContext("rm_user", "ROLE_REGION_MANAGER");
        Staff staff = Staff.builder().id(20).username("rm_user").scopeType("REGION").scopeEntityId(1).build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("rm_user")).thenReturn(Optional.of(staff));

        Page<UtilityReading> emptyPage = new PageImpl<>(Collections.emptyList());
        when(readingRepository.search(isNull(), isNull(), eq((short) 1), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        UtilityReadingSearchDto request = UtilityReadingSearchDto.builder().build();
        readingService.search(request);

        verify(readingRepository).search(isNull(), isNull(), eq((short) 1), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER xem chi tiết chỉ số thuộc khách sạn khác bị từ chối")
    void getById_PropertyManager_OtherHotel_ThrowsUnauthorized() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(2).build(); // hotel 2
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));
        when(readingRepository.findByIdWithMeterAndHotelAndRegion(1L)).thenReturn(Optional.of(reading)); // reading in hotel 1

        AppException ex = assertThrows(AppException.class, () -> readingService.getById(1L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER ghi chỉ số cho đồng hồ thuộc khách sạn khác bị từ chối")
    void create_PropertyManager_OtherHotel_ThrowsUnauthorized() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(2).build(); // hotel 2
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));
        when(meterRepository.findById(1)).thenReturn(Optional.of(meter)); // meter in hotel 1

        UtilityReadingCreateRequest request = new UtilityReadingCreateRequest();
        request.setMeterId(1);
        request.setReadingDate(LocalDate.of(2023, 10, 1));
        request.setReadingValue(new BigDecimal("100.5"));

        AppException ex = assertThrows(AppException.class, () -> readingService.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Scope: REGION_MANAGER gọi xóa bị từ chối UNAUTHORIZED")
    void delete_RegionManager_ThrowsUnauthorized() {
        setSecurityContext("rm_user", "ROLE_REGION_MANAGER");

        AppException ex = assertThrows(AppException.class, () -> readingService.delete(List.of(1L)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER xóa danh sách chứa chỉ số khách sạn khác bị từ chối")
    void delete_PropertyManager_MixedHotels_ThrowsUnauthorized() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(1).build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));

        Hotel hotel2 = Hotel.builder().id((short) 2).region(region).name("Hotel 2").build();
        UtilityMeter meter2 = UtilityMeter.builder().id(2).hotel(hotel2).meterCode("EL-02").build();
        UtilityReading reading2 = UtilityReading.builder().id(2L).meter(meter2).readingDate(LocalDate.of(2023, 10, 1)).build();

        when(readingRepository.findAllByIdInWithMeterAndHotel(List.of(1L, 2L))).thenReturn(List.of(reading, reading2));

        AppException ex = assertThrows(AppException.class, () -> readingService.delete(List.of(1L, 2L)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(reading.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("Sorting: Trường sắp xếp hợp lệ kèm tie-breaker id DESC")
    void search_SortingWhitelist_AddsTieBreaker() {
        UtilityReadingSearchDto request = UtilityReadingSearchDto.builder()
                .sortBy("readingDate")
                .sortDirection("ASC")
                .build();

        Page<UtilityReading> emptyPage = new PageImpl<>(Collections.emptyList());
        when(readingRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        readingService.search(request);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(readingRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(), captor.capture());

        Pageable pageable = captor.getValue();
        Sort.Order firstOrder = pageable.getSort().getOrderFor("readingDate");
        Sort.Order secondOrder = pageable.getSort().getOrderFor("id");

        assertThat(firstOrder).isNotNull();
        assertThat(firstOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(secondOrder).isNotNull();
        assertThat(secondOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
