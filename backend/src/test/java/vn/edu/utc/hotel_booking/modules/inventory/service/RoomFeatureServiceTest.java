package vn.edu.utc.hotel_booking.modules.inventory.service;

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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomFeatureUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomFeatureResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomFeature;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomFeatureMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomFeatureRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomFeatureServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomFeatureServiceTest {

    @Mock
    RoomFeatureRepository roomFeatureRepository;

    @Mock
    RoomFeatureMapper roomFeatureMapper;

    @InjectMocks
    RoomFeatureServiceImpl roomFeatureService;

    @Test
    @DisplayName("create: Thành công khi mã chưa tồn tại")
    void testCreate_Success() {
        RoomFeatureCreateRequest request = RoomFeatureCreateRequest.builder()
                .code("BALCONY")
                .name("Ban công")
                .category("VIEW")
                .status("ACTIVE")
                .build();

        RoomFeature entity = RoomFeature.builder().id((short) 1).code("BALCONY").name("Ban công").build();
        RoomFeatureResponse response = RoomFeatureResponse.builder().id((short) 1).code("BALCONY").build();

        when(roomFeatureRepository.existsByCodeAndIsDeletedFalse("BALCONY")).thenReturn(false);
        when(roomFeatureMapper.toEntity(request)).thenReturn(entity);
        when(roomFeatureRepository.save(entity)).thenReturn(entity);
        when(roomFeatureMapper.toResponse(entity)).thenReturn(response);

        RoomFeatureResponse result = roomFeatureService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("BALCONY");
        verify(roomFeatureRepository).save(entity);
    }

    @Test
    @DisplayName("create: Ném ngoại lệ khi mã đã tồn tại")
    void testCreate_Conflict_ThrowsException() {
        RoomFeatureCreateRequest request = RoomFeatureCreateRequest.builder().code("BALCONY").build();
        when(roomFeatureRepository.existsByCodeAndIsDeletedFalse("BALCONY")).thenReturn(true);

        assertThatThrownBy(() -> roomFeatureService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_FEATURE_CODE_ALREADY_EXISTS);

        verify(roomFeatureRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: Thành công khi ID tồn tại")
    void testUpdate_Success() {
        RoomFeatureUpdateRequest request = RoomFeatureUpdateRequest.builder()
                .name("Ban công ngắm biển cập nhật")
                .category("VIEW")
                .status("ACTIVE")
                .build();

        RoomFeature entity = RoomFeature.builder().id((short) 1).code("BALCONY").name("Ban công").build();
        RoomFeatureResponse response = RoomFeatureResponse.builder().id((short) 1).name("Ban công ngắm biển cập nhật").build();

        when(roomFeatureRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(entity));
        when(roomFeatureRepository.save(entity)).thenReturn(entity);
        when(roomFeatureMapper.toResponse(entity)).thenReturn(response);

        RoomFeatureResponse result = roomFeatureService.update((short) 1, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Ban công ngắm biển cập nhật");
        verify(roomFeatureMapper).updateEntity(entity, request);
    }

    @Test
    @DisplayName("getById: Ném ngoại lệ khi không tìm thấy")
    void testGetById_NotFound() {
        when(roomFeatureRepository.findByIdAndIsDeletedFalse((short) 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomFeatureService.getById((short) 99))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_FEATURE_NOT_FOUND);
    }

    @Test
    @DisplayName("getEntitiesByIds: Trả về tập hợp thực thể theo danh sách IDs")
    void testGetEntitiesByIds() {
        RoomFeature f1 = RoomFeature.builder().id((short) 1).code("BALCONY").build();
        RoomFeature f2 = RoomFeature.builder().id((short) 2).code("BATHTUB").build();

        when(roomFeatureRepository.findByIdInAndIsDeletedFalse(Set.of((short) 1, (short) 2)))
                .thenReturn(Set.of(f1, f2));

        Set<RoomFeature> results = roomFeatureService.getEntitiesByIds(Set.of((short) 1, (short) 2));
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("filter: Phân trang danh mục tiện ích")
    void testFilter_Success() {
        RoomFeatureSearchDto searchDto = new RoomFeatureSearchDto();
        searchDto.setPage(0);
        searchDto.setPageSize(10);
        searchDto.setSortBy("id");
        searchDto.setSortDirection("ASC");

        RoomFeature entity = RoomFeature.builder().id((short) 1).code("BALCONY").build();
        Page<RoomFeature> page = new PageImpl<>(List.of(entity));
        RoomFeatureResponse response = RoomFeatureResponse.builder().id((short) 1).code("BALCONY").build();

        when(roomFeatureRepository.filterRoomFeatures(eq(""), isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(roomFeatureMapper.toResponseList(anyList())).thenReturn(List.of(response));

        PageResponse<RoomFeatureResponse> result = roomFeatureService.filter(searchDto);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("deleteBatch: Gọi soft delete theo danh sách IDs")
    void testDeleteBatch() {
        roomFeatureService.deleteBatch(List.of((short) 1, (short) 2));
        verify(roomFeatureRepository).softDeleteByIds(List.of((short) 1, (short) 2));
    }
}

