package vn.edu.utc.hotel_booking.modules.organization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.HotelCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;
import vn.edu.utc.hotel_booking.modules.organization.mapper.HotelMapper;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.organization.repository.RegionRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.impl.HotelServiceImpl;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private HotelServiceImpl hotelService;

    private Region region;
    private Hotel hotel;
    private HotelResponse hotelResponse;

    @BeforeEach
    void setUp() {
        region = Region.builder()
                .id((short) 1)
                .code("NORTH")
                .name("Miền Bắc")
                .build();

        hotel = Hotel.builder()
                .id((short) 10)
                .region(region)
                .name("Viettel Luxury Hà Nội")
                .address("Tòa nhà Viettel, Hà Nội")
                .phone("02466668888")
                .checkInTime(LocalTime.of(14, 0))
                .checkOutTime(LocalTime.of(12, 0))
                .serviceFeePercent(new BigDecimal("5.00"))
                .status("ACTIVE")
                .build();

        hotelResponse = HotelResponse.builder()
                .id((short) 10)
                .regionId((short) 1)
                .regionName("Miền Bắc")
                .regionCode("NORTH")
                .name("Viettel Luxury Hà Nội")
                .address("Tòa nhà Viettel, Hà Nội")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Happy path: Lấy thông tin khách sạn cơ sở theo ID")
    void getById_Success() {
        when(hotelRepository.findByIdWithRegion((short) 10)).thenReturn(Optional.of(hotel));
        when(hotelMapper.toResponse(hotel)).thenReturn(hotelResponse);

        HotelResponse result = hotelService.getById((short) 10);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 10);
        assertThat(result.getName()).isEqualTo("Viettel Luxury Hà Nội");
        verify(hotelRepository).findByIdWithRegion((short) 10);
    }

    @Test
    @DisplayName("Error path: Ném ngoại lệ HOTEL_NOT_FOUND khi không tìm thấy khách sạn")
    void getById_NotFound_ThrowsException() {
        when(hotelRepository.findByIdWithRegion((short) 99)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> hotelService.getById((short) 99));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.HOTEL_NOT_FOUND);
    }

    @Test
    @DisplayName("Happy path: Tạo mới khách sạn thành công")
    void create_Success() {
        HotelCreateRequest request = HotelCreateRequest.builder()
                .regionId((short) 1)
                .name("Viettel Boutique Sapa")
                .address("Thị trấn Sapa, Lào Cai")
                .phone("02146668888")
                .build();

        Hotel newHotel = Hotel.builder()
                .name("Viettel Boutique Sapa")
                .address("Thị trấn Sapa, Lào Cai")
                .build();

        Hotel savedHotel = Hotel.builder()
                .id((short) 11)
                .region(region)
                .name("Viettel Boutique Sapa")
                .address("Thị trấn Sapa, Lào Cai")
                .build();

        when(regionRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(region));
        when(hotelRepository.existsByNameAndRegionIdAndIsDeletedFalse("Viettel Boutique Sapa", (short) 1)).thenReturn(false);
        when(hotelMapper.toEntity(request)).thenReturn(newHotel);
        when(hotelRepository.save(newHotel)).thenReturn(savedHotel);
        when(hotelMapper.toResponse(savedHotel)).thenReturn(
                HotelResponse.builder().id((short) 11).name("Viettel Boutique Sapa").build()
        );

        HotelResponse result = hotelService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 11);
        verify(hotelRepository).save(newHotel);
    }

    @Test
    @DisplayName("Error path: Báo lỗi REGION_NOT_FOUND khi khu vực trực thuộc không tồn tại")
    void create_RegionNotFound_ThrowsException() {
        HotelCreateRequest request = HotelCreateRequest.builder()
                .regionId((short) 999)
                .name("Viettel Hotel")
                .address("123 Street")
                .build();

        when(regionRepository.findByIdAndIsDeletedFalse((short) 999)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> hotelService.create(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND);
        verify(hotelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Error path: Báo lỗi HOTEL_NAME_ALREADY_EXISTS khi tên khách sạn đã tồn tại trong vùng")
    void create_DuplicateName_ThrowsException() {
        HotelCreateRequest request = HotelCreateRequest.builder()
                .regionId((short) 1)
                .name("Viettel Luxury Hà Nội")
                .address("Hà Nội")
                .build();

        when(regionRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(region));
        when(hotelRepository.existsByNameAndRegionIdAndIsDeletedFalse("Viettel Luxury Hà Nội", (short) 1)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> hotelService.create(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.HOTEL_NAME_ALREADY_EXISTS);
        verify(hotelRepository, never()).save(any());
    }

    @Test
    @DisplayName("Batch Delete: Xóa mềm danh sách khách sạn")
    void deleteBatch_Success() {
        List<Short> ids = List.of((short) 10, (short) 11);
        when(hotelRepository.softDeleteBatch(ids)).thenReturn(2);

        hotelService.deleteBatch(ids);

        verify(hotelRepository).softDeleteBatch(ids);
    }
}
