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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityMeterResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityMeter;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.MeterType;
import vn.edu.utc.hotel_booking.modules.operation.mapper.UtilityMeterMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityMeterRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.UtilityMeterServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilityMeterServiceTest {

    @Mock
    private UtilityMeterRepository repository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private UtilityMeterMapper mapper;

    @InjectMocks
    private UtilityMeterServiceImpl service;

    private Hotel hotel;
    private UtilityMeter meter;
    private UtilityMeterResponse meterResponse;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .id((short) 1)
                .name("Hotel 1")
                .build();
        hotel.setIsDeleted(false);

        meter = UtilityMeter.builder()
                .id(1)
                .hotel(hotel)
                .meterCode("EL-01")
                .meterType(MeterType.ELECTRICITY)
                .locationLabel("Lobby")
                .build();
        meter.setIsDeleted(false);

        meterResponse = new UtilityMeterResponse();
        meterResponse.setId(1);
        meterResponse.setMeterCode("EL-01");
    }

    @Test
    @DisplayName("Happy path: Lấy thông tin đồng hồ thành công")
    void getById_Success() {
        when(repository.findById(1)).thenReturn(Optional.of(meter));
        when(mapper.toResponse(meter)).thenReturn(meterResponse);

        UtilityMeterResponse result = service.getById(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        verify(repository).findById(1);
    }

    @Test
    @DisplayName("Error path: Ném ngoại lệ UTILITY_METER_NOT_FOUND khi đồng hồ không tồn tại")
    void getById_NotFound_ThrowsException() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getById(99));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UTILITY_METER_NOT_FOUND);
    }

    @Test
    @DisplayName("Happy path: Tạo mới đồng hồ thành công")
    void create_Success() {
        UtilityMeterCreateRequest request = new UtilityMeterCreateRequest();
        request.setHotelId((short) 1);
        request.setMeterCode("EL-01");
        request.setMeterType(MeterType.ELECTRICITY);
        request.setLocationLabel("Lobby");

        when(repository.existsByHotelIdAndMeterCodeAndIsDeletedFalse((short) 1, "EL-01")).thenReturn(false);
        when(hotelRepository.findById((short) 1)).thenReturn(Optional.of(hotel));
        when(mapper.toEntity(request)).thenReturn(meter);
        when(repository.save(meter)).thenReturn(meter);
        when(mapper.toResponse(meter)).thenReturn(meterResponse);

        UtilityMeterResponse result = service.create(request);

        assertThat(result).isNotNull();
        verify(repository).save(meter);
    }

    @Test
    @DisplayName("Validation path: Lỗi khi mã đồng hồ đã tồn tại")
    void create_DuplicateCode_ThrowsException() {
        UtilityMeterCreateRequest request = new UtilityMeterCreateRequest();
        request.setHotelId((short) 1);
        request.setMeterCode("EL-01");

        when(repository.existsByHotelIdAndMeterCodeAndIsDeletedFalse((short) 1, "EL-01")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.create(request));
        
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UTILITY_METER_ALREADY_EXISTS);
        verify(repository, never()).save(any());
    }
    
    @Test
    @DisplayName("Validation path: Lỗi khi khách sạn không tồn tại")
    void create_HotelNotFound_ThrowsException() {
        UtilityMeterCreateRequest request = new UtilityMeterCreateRequest();
        request.setHotelId((short) 99);
        request.setMeterCode("EL-01");

        when(repository.existsByHotelIdAndMeterCodeAndIsDeletedFalse((short) 99, "EL-01")).thenReturn(false);
        when(hotelRepository.findById((short) 99)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.create(request));
        
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.HOTEL_NOT_FOUND);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Edge case: Phân trang tìm kiếm rỗng")
    void search_Empty() {
        UtilityMeterSearchDto request = UtilityMeterSearchDto.builder()
                .page(0)
                .pageSize(10)
                .build();

        Page<UtilityMeter> emptyPage = new PageImpl<>(Collections.emptyList());
        when(repository.search(isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<UtilityMeterResponse> result = service.search(request);

        assertThat(result.getContent()).isEmpty();
    }
}
