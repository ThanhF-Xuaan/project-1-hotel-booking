package vn.edu.utc.hotel_booking.modules.inventory.service;

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
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;
import vn.edu.utc.hotel_booking.modules.identity.repository.StaffRepository;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockCreateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockSearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomMaintenanceBlockUpdateRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomMaintenanceBlockResponse;
import vn.edu.utc.hotel_booking.modules.inventory.entity.*;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomMaintenanceBlockMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomMaintenanceBlockRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomMaintenanceBlockServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomMaintenanceBlockServiceTest {

    @Mock
    RoomMaintenanceBlockRepository repository;

    @Mock
    RoomInstanceRepository roomInstanceRepository;

    @Mock
    RoomSlotRepository roomSlotRepository;

    @Mock
    RoomAvailabilityService roomAvailabilityService;

    @Mock
    BookingService bookingService;

    @Mock
    StaffRepository staffRepository;

    @Mock
    RoomMaintenanceBlockMapper mapper;

    @InjectMocks
    RoomMaintenanceBlockServiceImpl service;

    RoomInstance room;
    HotelRoomType hotelRoomType;
    Hotel hotel;
    RoomType roomType;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder().id((short) 1).name("Grand Hotel").build();
        roomType = RoomType.builder().id((short) 1).code("DLX").name("Deluxe").build();
        hotelRoomType = HotelRoomType.builder().id(10).hotel(hotel).roomType(roomType).totalQuantity(5).build();
        room = RoomInstance.builder()
                .id(101)
                .hotel(hotel)
                .hotelRoomType(hotelRoomType)
                .roomNumber("P301")
                .currentStatus("READY")
                .build();
    }

    @Test
    @DisplayName("Case 1: Happy path - Tạo block OOO thành công, trừ quỹ phòng, cập nhật trạng thái phòng")
    void create_Ooo_Success() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);
        RoomMaintenanceBlockCreateRequest req = RoomMaintenanceBlockCreateRequest.builder()
                .roomInstanceId(101)
                .blockType("OOO")
                .startDate(start)
                .endDate(end)
                .reason("Bảo trì điều hòa")
                .notes("Thợ vào sáng sớm")
                .build();

        RoomMaintenanceBlock entity = RoomMaintenanceBlock.builder()
                .id(1L)
                .roomInstance(room)
                .blockType("OOO")
                .startDate(start)
                .endDate(end)
                .reason(req.getReason())
                .status("ACTIVE")
                .build();

        RoomMaintenanceBlockResponse resp = RoomMaintenanceBlockResponse.builder()
                .id(1L)
                .roomInstanceId(101)
                .roomNumber("P301")
                .blockType("OOO")
                .status("ACTIVE")
                .build();

        when(roomInstanceRepository.findByIdWithLock(101)).thenReturn(Optional.of(room));
        when(repository.findActiveOverlap(101, start, end)).thenReturn(Collections.emptyList());
        when(bookingService.isRoomBookedBetween(101, start, end)).thenReturn(false);
        when(mapper.toEntity(req)).thenReturn(entity);
        when(repository.save(any(RoomMaintenanceBlock.class))).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        RoomMaintenanceBlockResponse result = service.create(req);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(room.getCurrentStatus()).isEqualTo("MAINTENANCE");

        verify(roomAvailabilityService).applyOooDelta(10, start, end, 1);
        verify(roomInstanceRepository).save(room);
        verify(repository).save(any(RoomMaintenanceBlock.class));
    }

    @Test
    @DisplayName("Case 2: Happy path - Tạo block OOS thành công, không gọi applyOooDelta")
    void create_Oos_Success() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        RoomMaintenanceBlockCreateRequest req = RoomMaintenanceBlockCreateRequest.builder()
                .roomInstanceId(101)
                .blockType("OOS")
                .startDate(start)
                .endDate(end)
                .reason("Sơn lại chân tường")
                .build();

        RoomMaintenanceBlock entity = RoomMaintenanceBlock.builder()
                .id(2L)
                .roomInstance(room)
                .blockType("OOS")
                .startDate(start)
                .endDate(end)
                .reason(req.getReason())
                .status("ACTIVE")
                .build();

        RoomMaintenanceBlockResponse resp = RoomMaintenanceBlockResponse.builder()
                .id(2L)
                .blockType("OOS")
                .status("ACTIVE")
                .build();

        when(roomInstanceRepository.findByIdWithLock(101)).thenReturn(Optional.of(room));
        when(repository.findActiveOverlap(101, start, end)).thenReturn(Collections.emptyList());
        when(bookingService.isRoomBookedBetween(101, start, end)).thenReturn(false);
        when(mapper.toEntity(req)).thenReturn(entity);
        when(repository.save(any(RoomMaintenanceBlock.class))).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        RoomMaintenanceBlockResponse result = service.create(req);

        assertThat(result).isNotNull();
        assertThat(result.getBlockType()).isEqualTo("OOS");
        verify(roomAvailabilityService, never()).applyOooDelta(any(), any(), any(), anyInt());
        verify(roomInstanceRepository).save(room);
    }

    @Test
    @DisplayName("Case 3: Validation - startDate sau endDate ném lỗi MAINTENANCE_BLOCK_TIME_INVALID")
    void create_StartAfterEnd_Throws() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(2);
        RoomMaintenanceBlockCreateRequest req = RoomMaintenanceBlockCreateRequest.builder()
                .roomInstanceId(101)
                .blockType("OOO")
                .startDate(start)
                .endDate(end)
                .reason("Test")
                .build();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAINTENANCE_BLOCK_TIME_INVALID);

        verifyNoInteractions(roomAvailabilityService);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Case 4: Validation - Phòng đang có block ACTIVE trùng lịch ném ROOM_ALREADY_BLOCKED")
    void create_OverlapActiveBlock_Throws() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);
        RoomMaintenanceBlockCreateRequest req = RoomMaintenanceBlockCreateRequest.builder()
                .roomInstanceId(101)
                .blockType("OOO")
                .startDate(start)
                .endDate(end)
                .reason("Test")
                .build();

        RoomMaintenanceBlock existing = RoomMaintenanceBlock.builder().id(99L).build();

        when(roomInstanceRepository.findByIdWithLock(101)).thenReturn(Optional.of(room));
        when(repository.findActiveOverlap(101, start, end)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_ALREADY_BLOCKED);

        verifyNoInteractions(roomAvailabilityService);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Case 5: Validation - Phòng đang OCCUPIED hoặc có booking gán ném ROOM_ALREADY_BLOCKED")
    void create_RoomOccupiedOrBooked_Throws() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);
        RoomMaintenanceBlockCreateRequest req = RoomMaintenanceBlockCreateRequest.builder()
                .roomInstanceId(101)
                .blockType("OOO")
                .startDate(start)
                .endDate(end)
                .reason("Test")
                .build();

        room.setCurrentStatus("OCCUPIED");
        when(roomInstanceRepository.findByIdWithLock(101)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_ALREADY_BLOCKED);

        // Trường hợp phòng READY nhưng có đơn đặt
        room.setCurrentStatus("READY");
        when(repository.findActiveOverlap(101, start, end)).thenReturn(Collections.emptyList());
        when(bookingService.isRoomBookedBetween(101, start, end)).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_ALREADY_BLOCKED);
    }

    @Test
    @DisplayName("Case 6: Happy path - Hoàn tất block OOO giải phóng quỹ phòng và đưa phòng về READY")
    void complete_ReleasesEverything() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(1);
        RoomMaintenanceBlock block = RoomMaintenanceBlock.builder()
                .id(1L)
                .roomInstance(room)
                .blockType("OOO")
                .startDate(start)
                .endDate(end)
                .status("ACTIVE")
                .build();

        RoomSlot slot = RoomSlot.builder()
                .id(10L)
                .roomInstance(room)
                .slotDate(LocalDate.now())
                .status("BLOCKED")
                .maintenanceBlock(block)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(block));
        when(roomInstanceRepository.findByIdWithLock(101)).thenReturn(Optional.of(room));
        when(roomSlotRepository.findByMaintenanceBlockIdWithLock(1L)).thenReturn(List.of(slot));
        when(repository.existsByRoomInstanceIdAndStatus(101, "ACTIVE")).thenReturn(false);
        when(roomSlotRepository.findByRoomInstanceIdAndSlotDate(101, LocalDate.now())).thenReturn(Optional.of(slot));
        when(repository.save(any(RoomMaintenanceBlock.class))).thenReturn(block);
        when(mapper.toResponse(block)).thenReturn(RoomMaintenanceBlockResponse.builder().id(1L).status("COMPLETED").build());

        RoomMaintenanceBlockResponse resp = service.complete(1L);

        assertThat(resp.getStatus()).isEqualTo("COMPLETED");
        assertThat(block.getStatus()).isEqualTo("COMPLETED");
        assertThat(slot.getStatus()).isEqualTo("READY");
        assertThat(slot.getMaintenanceBlock()).isNull();
        assertThat(room.getCurrentStatus()).isEqualTo("READY");

        verify(roomAvailabilityService).applyOooDelta(10, start, end, -1);
        verify(roomInstanceRepository).save(room);
    }

    @Test
    @DisplayName("Case 7: Edge case - Khi hoàn tất block, nếu slot hôm nay đang DIRTY thì phòng chuyển về CLEANING chứ không tự về READY")
    void complete_RoomStillDirty_StaysCleaning() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now();
        RoomMaintenanceBlock block = RoomMaintenanceBlock.builder()
                .id(1L)
                .roomInstance(room)
                .blockType("OOO")
                .startDate(start)
                .endDate(end)
                .status("ACTIVE")
                .build();

        RoomSlot todaySlot = RoomSlot.builder()
                .id(11L)
                .roomInstance(room)
                .slotDate(LocalDate.now())
                .status("DIRTY")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(block));
        when(roomInstanceRepository.findByIdWithLock(101)).thenReturn(Optional.of(room));
        when(roomSlotRepository.findByMaintenanceBlockIdWithLock(1L)).thenReturn(Collections.emptyList());
        when(repository.existsByRoomInstanceIdAndStatus(101, "ACTIVE")).thenReturn(false);
        when(roomSlotRepository.findByRoomInstanceIdAndSlotDate(101, LocalDate.now())).thenReturn(Optional.of(todaySlot));
        when(repository.save(any(RoomMaintenanceBlock.class))).thenReturn(block);
        when(mapper.toResponse(block)).thenReturn(RoomMaintenanceBlockResponse.builder().id(1L).status("COMPLETED").build());

        service.complete(1L);

        assertThat(room.getCurrentStatus()).isEqualTo("CLEANING");
        verify(roomInstanceRepository).save(room);
    }

    @Test
    @DisplayName("Case 8: Validation - Cập nhật block đã COMPLETED/CANCELLED ném lỗi")
    void update_NotActive_Throws() {
        RoomMaintenanceBlock block = RoomMaintenanceBlock.builder()
                .id(1L)
                .roomInstance(room)
                .status("COMPLETED")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(block));

        RoomMaintenanceBlockUpdateRequest req = RoomMaintenanceBlockUpdateRequest.builder()
                .reason("Đổi lý do")
                .build();

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAINTENANCE_BLOCK_TIME_INVALID);
    }

    @Test
    @DisplayName("Case 9: Happy path - Hủy block CANCELLED thành công")
    void cancel_Success() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(1);
        RoomMaintenanceBlock block = RoomMaintenanceBlock.builder()
                .id(5L)
                .roomInstance(room)
                .blockType("OOO")
                .startDate(start)
                .endDate(end)
                .status("ACTIVE")
                .build();

        when(repository.findById(5L)).thenReturn(Optional.of(block));
        when(roomInstanceRepository.findByIdWithLock(101)).thenReturn(Optional.of(room));
        when(roomSlotRepository.findByMaintenanceBlockIdWithLock(5L)).thenReturn(Collections.emptyList());
        when(repository.existsByRoomInstanceIdAndStatus(101, "ACTIVE")).thenReturn(false);
        when(repository.save(any())).thenReturn(block);
        when(mapper.toResponse(block)).thenReturn(RoomMaintenanceBlockResponse.builder().id(5L).status("CANCELLED").build());

        RoomMaintenanceBlockResponse res = service.cancel(5L);

        assertThat(res.getStatus()).isEqualTo("CANCELLED");
        assertThat(block.getStatus()).isEqualTo("CANCELLED");
        verify(roomAvailabilityService).applyOooDelta(10, start, end, -1);
    }

    @Test
    @DisplayName("Case 10: Happy path & Edge case - Filter danh sách block có dữ liệu và danh sách rỗng")
    void filter_Scenarios() {
        RoomMaintenanceBlockSearchDto searchDto = RoomMaintenanceBlockSearchDto.builder()
                .hotelId((short) 1)
                .status("ACTIVE")
                .build();
        searchDto.setPage(0);
        searchDto.setPageSize(10);

        Page<RoomMaintenanceBlock> page = new PageImpl<>(List.of(
                RoomMaintenanceBlock.builder().id(1L).status("ACTIVE").build()
        ));

        when(repository.filter(isNull(), eq((short) 1), isNull(), eq("ACTIVE"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(mapper.toResponse(any())).thenReturn(RoomMaintenanceBlockResponse.builder().id(1L).build());

        PageResponse<RoomMaintenanceBlockResponse> res = service.filter(searchDto);
        assertThat(res.getContent()).hasSize(1);

        // Edge case: rỗng
        when(repository.filter(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        PageResponse<RoomMaintenanceBlockResponse> emptyRes = service.filter(searchDto);
        assertThat(emptyRes.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Case 11: Error path - getById không tìm thấy ném MAINTENANCE_BLOCK_NOT_FOUND")
    void getById_NotFound_Throws() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAINTENANCE_BLOCK_NOT_FOUND);
    }
}
