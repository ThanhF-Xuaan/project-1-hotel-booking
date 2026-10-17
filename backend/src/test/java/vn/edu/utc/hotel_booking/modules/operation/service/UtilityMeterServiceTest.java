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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.UtilityMeterUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.UtilityMeterResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.UtilityMeter;
import vn.edu.utc.hotel_booking.modules.operation.entity.enums.MeterType;
import vn.edu.utc.hotel_booking.modules.operation.mapper.UtilityMeterMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.UtilityMeterRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.UtilityMeterServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;
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

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private UtilityMeterServiceImpl service;

    private Region region;
    private Hotel hotel;
    private UtilityMeter meter;
    private UtilityMeterResponse meterResponse;

    @BeforeEach
    void setUp() {
        setSecurityContext("admin_user", "ROLE_CHAIN_ADMIN");

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
                .meterType(MeterType.ELECTRICITY)
                .locationLabel("Lobby")
                .build();
        meter.setIsDeleted(false);

        meterResponse = new UtilityMeterResponse();
        meterResponse.setId(1);
        meterResponse.setMeterCode("EL-01");
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
    @DisplayName("Happy path: Lấy thông tin đồng hồ thành công")
    void getById_Success() {
        when(repository.findByIdWithHotelAndRegion(1)).thenReturn(Optional.of(meter));
        when(mapper.toResponse(meter)).thenReturn(meterResponse);

        UtilityMeterResponse result = service.getById(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        verify(repository).findByIdWithHotelAndRegion(1);
    }

    @Test
    @DisplayName("Error path: Ném ngoại lệ UTILITY_METER_NOT_FOUND khi đồng hồ không tồn tại")
    void getById_NotFound_ThrowsException() {
        when(repository.findByIdWithHotelAndRegion(99)).thenReturn(Optional.empty());

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
        when(repository.search(isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<UtilityMeterResponse> result = service.search(request);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Happy path: Cập nhật đồng hồ thành công")
    void update_Success() {
        UtilityMeterUpdateRequest updateRequest = new UtilityMeterUpdateRequest();
        updateRequest.setMeterCode("EL-02");
        updateRequest.setLocationLabel("Floor 2");

        when(repository.findByIdWithHotelAndRegion(1)).thenReturn(Optional.of(meter));
        when(repository.existsByHotelIdAndMeterCodeAndIsDeletedFalse((short) 1, "EL-02")).thenReturn(false);
        when(repository.save(meter)).thenReturn(meter);
        when(mapper.toResponse(meter)).thenReturn(meterResponse);

        UtilityMeterResponse result = service.update(1, updateRequest);

        assertThat(result).isNotNull();
        assertThat(meter.getMeterCode()).isEqualTo("EL-02");
        assertThat(meter.getLocationLabel()).isEqualTo("Floor 2");
        verify(repository).save(meter);
    }

    @Test
    @DisplayName("Validation path: Cập nhật lỗi khi đổi sang mã đồng hồ đã tồn tại")
    void update_DuplicateCode_ThrowsException() {
        UtilityMeterUpdateRequest updateRequest = new UtilityMeterUpdateRequest();
        updateRequest.setMeterCode("EL-DUPLICATE");

        when(repository.findByIdWithHotelAndRegion(1)).thenReturn(Optional.of(meter));
        when(repository.existsByHotelIdAndMeterCodeAndIsDeletedFalse((short) 1, "EL-DUPLICATE")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.update(1, updateRequest));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UTILITY_METER_ALREADY_EXISTS);
        verify(repository, never()).save(meter);
    }

    @Test
    @DisplayName("Happy path: Xóa mềm hàng loạt đồng hồ")
    void delete_Success_SoftDelete() {
        when(repository.findAllByIdInWithHotel(List.of(1))).thenReturn(List.of(meter));

        service.delete(List.of(1));

        assertThat(meter.getIsDeleted()).isTrue();
        verify(repository).saveAll(List.of(meter));
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER tự động lọc theo khách sạn của nhân viên")
    void search_PropertyManager_AutoRestrictsToStaffHotel() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(1).build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));

        Page<UtilityMeter> emptyPage = new PageImpl<>(Collections.emptyList());
        when(repository.search(eq((short) 1), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        UtilityMeterSearchDto request = UtilityMeterSearchDto.builder().build();
        service.search(request);

        verify(repository).search(eq((short) 1), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER tìm kiếm khách sạn khác bị từ chối UNAUTHORIZED")
    void search_PropertyManager_OtherHotel_ThrowsUnauthorized() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(1).build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));

        UtilityMeterSearchDto request = UtilityMeterSearchDto.builder().hotelId((short) 2).build();

        AppException ex = assertThrows(AppException.class, () -> service.search(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Scope: REGION_MANAGER tự động giới hạn vùng quản lý")
    void search_RegionManager_RestrictsToRegion() {
        setSecurityContext("rm_user", "ROLE_REGION_MANAGER");
        Staff staff = Staff.builder().id(20).username("rm_user").scopeType("REGION").scopeEntityId(1).build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("rm_user")).thenReturn(Optional.of(staff));

        Page<UtilityMeter> emptyPage = new PageImpl<>(Collections.emptyList());
        when(repository.search(isNull(), eq((short) 1), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        UtilityMeterSearchDto request = UtilityMeterSearchDto.builder().build();
        service.search(request);

        verify(repository).search(isNull(), eq((short) 1), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER xem chi tiết đồng hồ khách sạn khác bị từ chối")
    void getById_PropertyManager_OtherHotel_ThrowsUnauthorized() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(2).build(); // Belongs to hotel 2
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));
        when(repository.findByIdWithHotelAndRegion(1)).thenReturn(Optional.of(meter)); // Meter belongs to hotel 1

        AppException ex = assertThrows(AppException.class, () -> service.getById(1));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER tạo đồng hồ cho khách sạn khác bị từ chối")
    void create_PropertyManager_OtherHotel_ThrowsUnauthorized() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(2).build(); // Belongs to hotel 2
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));

        UtilityMeterCreateRequest request = new UtilityMeterCreateRequest();
        request.setHotelId((short) 1);
        request.setMeterCode("EL-01");

        AppException ex = assertThrows(AppException.class, () -> service.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Scope: REGION_MANAGER gọi xóa bị từ chối UNAUTHORIZED")
    void delete_RegionManager_ThrowsUnauthorized() {
        setSecurityContext("rm_user", "ROLE_REGION_MANAGER");

        AppException ex = assertThrows(AppException.class, () -> service.delete(List.of(1)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Scope: PROPERTY_MANAGER xóa danh sách chứa đồng hồ khách sạn khác bị từ chối")
    void delete_PropertyManager_MixedHotels_ThrowsUnauthorized() {
        setSecurityContext("pm_user", "ROLE_PROPERTY_MANAGER");
        Staff staff = Staff.builder().id(10).username("pm_user").scopeType("PROPERTY").scopeEntityId(1).build();
        when(staffRepository.findByUsernameAndIsDeletedFalse("pm_user")).thenReturn(Optional.of(staff));

        Hotel hotel2 = Hotel.builder().id((short) 2).region(region).name("Hotel 2").build();
        UtilityMeter meter2 = UtilityMeter.builder().id(2).hotel(hotel2).meterCode("EL-02").build();

        when(repository.findAllByIdInWithHotel(List.of(1, 2))).thenReturn(List.of(meter, meter2));

        AppException ex = assertThrows(AppException.class, () -> service.delete(List.of(1, 2)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(meter.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("Sorting: Trường sắp xếp hợp lệ kèm tie-breaker id DESC")
    void search_SortingWhitelist_AddsTieBreaker() {
        UtilityMeterSearchDto request = UtilityMeterSearchDto.builder()
                .sortBy("meterCode")
                .sortDirection("ASC")
                .build();

        Page<UtilityMeter> emptyPage = new PageImpl<>(Collections.emptyList());
        when(repository.search(isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        service.search(request);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(isNull(), isNull(), isNull(), isNull(), captor.capture());

        Pageable pageable = captor.getValue();
        Sort.Order firstOrder = pageable.getSort().getOrderFor("meterCode");
        Sort.Order secondOrder = pageable.getSort().getOrderFor("id");

        assertThat(firstOrder).isNotNull();
        assertThat(firstOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(secondOrder).isNotNull();
        assertThat(secondOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
