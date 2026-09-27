package vn.edu.utc.hotel_booking.modules.organization.service;

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
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionCreateRequest;
import vn.edu.utc.hotel_booking.modules.organization.dto.request.RegionSearchDto;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.RegionResponse;
import vn.edu.utc.hotel_booking.modules.organization.entity.Region;
import vn.edu.utc.hotel_booking.modules.organization.mapper.RegionMapper;
import vn.edu.utc.hotel_booking.modules.organization.repository.RegionRepository;
import vn.edu.utc.hotel_booking.modules.organization.service.impl.RegionServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private RegionMapper regionMapper;

    @InjectMocks
    private RegionServiceImpl regionService;

    private Region region;
    private RegionResponse regionResponse;

    @BeforeEach
    void setUp() {
        region = Region.builder()
                .id((short) 1)
                .code("NORTH")
                .name("Miền Bắc")
                .description("Khu vực phía Bắc")
                .status("ACTIVE")
                .build();

        regionResponse = RegionResponse.builder()
                .id((short) 1)
                .code("NORTH")
                .name("Miền Bắc")
                .description("Khu vực phía Bắc")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Happy path: Lấy thông tin khu vực theo ID thành công")
    void getById_Success() {
        when(regionRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(region));
        when(regionMapper.toResponse(region)).thenReturn(regionResponse);

        RegionResponse result = regionService.getById((short) 1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 1);
        assertThat(result.getCode()).isEqualTo("NORTH");
        verify(regionRepository).findByIdAndIsDeletedFalse((short) 1);
    }

    @Test
    @DisplayName("Error path: Ném ngoại lệ REGION_NOT_FOUND khi không tìm thấy khu vực")
    void getById_NotFound_ThrowsException() {
        when(regionRepository.findByIdAndIsDeletedFalse((short) 99)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> regionService.getById((short) 99));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND);
        verify(regionRepository).findByIdAndIsDeletedFalse((short) 99);
    }

    @Test
    @DisplayName("Happy path: Tạo mới khu vực thành công khi mã code chưa tồn tại")
    void create_Success() {
        RegionCreateRequest request = RegionCreateRequest.builder()
                .code("CENTRAL")
                .name("Miền Trung")
                .description("Khu vực miền Trung")
                .status("ACTIVE")
                .build();

        Region centralRegion = Region.builder()
                .code("CENTRAL")
                .name("Miền Trung")
                .description("Khu vực miền Trung")
                .status("ACTIVE")
                .build();

        Region savedRegion = Region.builder()
                .id((short) 2)
                .code("CENTRAL")
                .name("Miền Trung")
                .description("Khu vực miền Trung")
                .status("ACTIVE")
                .build();

        when(regionRepository.existsByCodeAndIsDeletedFalse("CENTRAL")).thenReturn(false);
        when(regionMapper.toEntity(request)).thenReturn(centralRegion);
        when(regionRepository.save(centralRegion)).thenReturn(savedRegion);
        when(regionMapper.toResponse(savedRegion)).thenReturn(
                RegionResponse.builder().id((short) 2).code("CENTRAL").name("Miền Trung").build()
        );

        RegionResponse result = regionService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 2);
        assertThat(result.getCode()).isEqualTo("CENTRAL");
        verify(regionRepository).save(centralRegion);
    }

    @Test
    @DisplayName("Error path: Ném ngoại lệ REGION_CODE_ALREADY_EXISTS khi trùng mã code")
    void create_DuplicateCode_ThrowsException() {
        RegionCreateRequest request = RegionCreateRequest.builder()
                .code("NORTH")
                .name("Miền Bắc 2")
                .build();

        when(regionRepository.existsByCodeAndIsDeletedFalse("NORTH")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> regionService.create(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_CODE_ALREADY_EXISTS);
        verify(regionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Edge case: Phân trang tìm kiếm khi không có bản ghi nào")
    void filter_EmptyList() {
        RegionSearchDto searchDto = RegionSearchDto.builder()
                .keyword("NonExisting")
                .page(0)
                .pageSize(10)
                .build();

        Page<Region> emptyPage = new PageImpl<>(Collections.emptyList());
        when(regionRepository.searchRegions(eq("NonExisting"), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponse<RegionResponse> result = regionService.filter(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Batch Delete: Xóa mềm danh sách khu vực")
    void deleteBatch_Success() {
        List<Short> ids = List.of((short) 1, (short) 2);
        when(regionRepository.softDeleteBatch(ids)).thenReturn(2);

        regionService.deleteBatch(ids);

        verify(regionRepository).softDeleteBatch(ids);
    }
}
