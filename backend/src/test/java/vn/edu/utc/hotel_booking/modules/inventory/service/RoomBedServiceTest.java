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
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomBedUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomBedResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomBed;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomBedMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomBedRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomBedServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomBedServiceTest {

    @Mock
    RoomBedRepository roomBedRepository;

    @Mock
    RoomBedMapper roomBedMapper;

    @InjectMocks
    RoomBedServiceImpl roomBedService;

    @Test
    @DisplayName("create: Thành công khi tên chưa tồn tại")
    void testCreate_Success() {
        RoomBedCreateRequest request = RoomBedCreateRequest.builder()
                .name("King Bed")
                .size("1.8m x 2.0m")
                .status("ACTIVE")
                .build();

        RoomBed entity = RoomBed.builder().id((short) 1).name("King Bed").build();
        RoomBedResponse response = RoomBedResponse.builder().id((short) 1).name("King Bed").build();

        when(roomBedRepository.existsByNameAndIsDeletedFalse("King Bed")).thenReturn(false);
        when(roomBedMapper.toEntity(request)).thenReturn(entity);
        when(roomBedRepository.save(entity)).thenReturn(entity);
        when(roomBedMapper.toResponse(entity)).thenReturn(response);

        RoomBedResponse result = roomBedService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 1);
        assertThat(result.getName()).isEqualTo("King Bed");
        verify(roomBedRepository).save(entity);
    }

    @Test
    @DisplayName("create: Ném ngoại lệ khi tên đã tồn tại")
    void testCreate_Conflict_ThrowsException() {
        RoomBedCreateRequest request = RoomBedCreateRequest.builder().name("King Bed").build();
        when(roomBedRepository.existsByNameAndIsDeletedFalse("King Bed")).thenReturn(true);

        assertThatThrownBy(() -> roomBedService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_BED_NAME_ALREADY_EXISTS);

        verify(roomBedRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: Thành công khi ID tồn tại")
    void testUpdate_Success() {
        RoomBedUpdateRequest request = RoomBedUpdateRequest.builder()
                .name("King Bed Updated")
                .size("2.0m x 2.0m")
                .status("ACTIVE")
                .build();

        RoomBed entity = RoomBed.builder().id((short) 1).name("King Bed").build();
        RoomBedResponse response = RoomBedResponse.builder().id((short) 1).name("King Bed Updated").build();

        when(roomBedRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(entity));
        when(roomBedRepository.existsByNameAndIdNotAndIsDeletedFalse("King Bed Updated", (short) 1)).thenReturn(false);
        when(roomBedRepository.save(entity)).thenReturn(entity);
        when(roomBedMapper.toResponse(entity)).thenReturn(response);

        RoomBedResponse result = roomBedService.update((short) 1, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("King Bed Updated");
        verify(roomBedMapper).updateEntity(entity, request);
    }

    @Test
    @DisplayName("update: Ném ngoại lệ khi không tìm thấy ID")
    void testUpdate_NotFound_ThrowsException() {
        RoomBedUpdateRequest request = RoomBedUpdateRequest.builder().name("King Bed").build();
        when(roomBedRepository.findByIdAndIsDeletedFalse((short) 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomBedService.update((short) 99, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_BED_NOT_FOUND);
    }

    @Test
    @DisplayName("getById: Thành công khi tồn tại")
    void testGetById_Success() {
        RoomBed entity = RoomBed.builder().id((short) 1).name("King Bed").build();
        RoomBedResponse response = RoomBedResponse.builder().id((short) 1).name("King Bed").build();

        when(roomBedRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(entity));
        when(roomBedMapper.toResponse(entity)).thenReturn(response);

        RoomBedResponse result = roomBedService.getById((short) 1);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("getEntityById: Trả về Entity trực tiếp cho Aggregate Root")
    void testGetEntityById_Success() {
        RoomBed entity = RoomBed.builder().id((short) 1).name("King Bed").build();
        when(roomBedRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(entity));

        RoomBed result = roomBedService.getEntityById((short) 1);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("filter: Phân trang và trả về danh sách")
    void testFilter_Success() {
        RoomBedSearchDto searchDto = new RoomBedSearchDto();
        searchDto.setPage(0);
        searchDto.setPageSize(10);
        searchDto.setSortBy("id");
        searchDto.setSortDirection("ASC");

        RoomBed entity = RoomBed.builder().id((short) 1).name("King Bed").build();
        Page<RoomBed> page = new PageImpl<>(List.of(entity));
        RoomBedResponse response = RoomBedResponse.builder().id((short) 1).name("King Bed").build();

        when(roomBedRepository.filterRoomBeds(isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(roomBedMapper.toResponseList(anyList())).thenReturn(List.of(response));

        PageResponse<RoomBedResponse> result = roomBedService.filter(searchDto);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("deleteBatch: Gọi soft delete theo danh sách IDs")
    void testDeleteBatch_Success() {
        roomBedService.deleteBatch(List.of((short) 1, (short) 2));
        verify(roomBedRepository).softDeleteByIds(List.of((short) 1, (short) 2));
    }
}

