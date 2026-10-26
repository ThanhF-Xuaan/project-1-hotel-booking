package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupRoomBlockResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupRoomBlockDailyRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupRoomBlockRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupContractService;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.GroupRoomBlockServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupRoomBlockServiceTest {

    @Mock
    GroupRoomBlockRepository groupRoomBlockRepository;

    @Mock
    GroupRoomBlockDailyRepository groupRoomBlockDailyRepository;

    @Mock
    GroupContractService groupContractService;

    @Mock
    AuditLogService auditLogService;

    @InjectMocks
    GroupRoomBlockServiceImpl groupRoomBlockService;

    HotelRoomType hotelRoomType;
    GroupContract contract;
    GroupRoomBlock block;
    GroupRoomBlockDaily daily1;
    GroupRoomBlockDaily daily2;

    @BeforeEach
    void setUp() {
        RoomType roomType = RoomType.builder()
                .id((short) 1)
                .code("DELUXE")
                .name("Phòng Deluxe Hướng Biển")
                .build();

        hotelRoomType = HotelRoomType.builder()
                .id(10)
                .roomType(roomType)
                .totalQuantity(20)
                .build();

        contract = GroupContract.builder()
                .id(100L)
                .contractNumber("GC-20261026-0001")
                .groupName("Đoàn Hội Thảo FPT")
                .checkInDate(LocalDate.of(2026, 11, 10))
                .checkOutDate(LocalDate.of(2026, 11, 12))
                .totalRooms(6)
                .status(GroupContractStatus.DEPOSIT_PAID)
                .cutoffDate(OffsetDateTime.now().minusHours(1)) // Đã quá hạn cut-off
                .depositDueDate(OffsetDateTime.now().plusDays(2))
                .roomBlocks(new ArrayList<>())
                .build();

        block = GroupRoomBlock.builder()
                .id(200L)
                .contract(contract)
                .hotelRoomType(hotelRoomType)
                .totalBlockedRooms(6)
                .allocatedRooms(2)
                .releasedRooms(0)
                .agreedRate(BigDecimal.valueOf(1500000))
                .status(GroupRoomBlockStatus.DEFINITE)
                .dailyBlocks(new ArrayList<>())
                .build();

        daily1 = GroupRoomBlockDaily.builder()
                .id(301L)
                .roomBlock(block)
                .hotelRoomType(hotelRoomType)
                .stayDate(LocalDate.of(2026, 11, 10))
                .blockedQuantity(6)
                .pickedUpQuantity(2) // Đã gán 2 phòng, còn thừa 4 phòng
                .releasedQuantity(0)
                .status(GroupRoomBlockDailyStatus.ACTIVE)
                .build();

        daily2 = GroupRoomBlockDaily.builder()
                .id(302L)
                .roomBlock(block)
                .hotelRoomType(hotelRoomType)
                .stayDate(LocalDate.of(2026, 11, 11))
                .blockedQuantity(6)
                .pickedUpQuantity(2) // Đã gán 2 phòng, còn thừa 4 phòng
                .releasedQuantity(0)
                .status(GroupRoomBlockDailyStatus.ACTIVE)
                .build();

        block.getDailyBlocks().add(daily1);
        block.getDailyBlocks().add(daily2);
        contract.getRoomBlocks().add(block);
    }

    @Test
    @DisplayName("getActiveBlockedRooms returns sum of unpicked-up rooms from repository")
    void testGetActiveBlockedRooms_Success() {
        LocalDate stayDate = LocalDate.of(2026, 11, 10);
        when(groupRoomBlockDailyRepository.sumActiveBlockedRooms(10, stayDate))
                .thenReturn(4);

        int count = groupRoomBlockService.getActiveBlockedRooms(10, stayDate);

        assertThat(count).isEqualTo(4);
        verify(groupRoomBlockDailyRepository, times(1)).sumActiveBlockedRooms(10, stayDate);
    }

    @Test
    @DisplayName("processCutoffAutoRelease releases unpicked-up rooms and updates block status")
    void testProcessCutoffAutoRelease_Success() {
        when(groupContractService.findContractsPastCutoff(any()))
                .thenReturn(List.of(contract));

        int totalReleased = groupRoomBlockService.processCutoffAutoRelease();

        // 4 phòng unpicked ngày 1 + 4 phòng unpicked ngày 2 = 8 lượt phòng
        assertThat(totalReleased).isEqualTo(8);
        assertThat(daily1.getReleasedQuantity()).isEqualTo(4);
        assertThat(daily1.getBlockedQuantity()).isEqualTo(2); // Giảm về đúng bằng picked-up
        assertThat(daily1.getStatus()).isEqualTo(GroupRoomBlockDailyStatus.ACTIVE);

        assertThat(block.getReleasedRooms()).isEqualTo(8);
        verify(groupRoomBlockDailyRepository, times(2)).save(any(GroupRoomBlockDaily.class));
        verify(groupRoomBlockRepository, times(1)).save(block);
        verify(groupContractService, times(1)).saveEntity(contract);
        verify(auditLogService, times(1)).recordLog(
                isNull(),
                eq("GROUP_CUTOFF_AUTO_RELEASE"),
                eq("GROUP_CONTRACT"),
                eq("100"),
                any(),
                contains("Auto-released 8 unpicked rooms"),
                eq("SYSTEM_SCHEDULER"),
                eq("GroupCutoffScheduler")
        );
    }

    @Test
    @DisplayName("processDepositExpiredAutoCancel delegates to groupContractService")
    void testProcessDepositExpiredAutoCancel_Success() {
        when(groupContractService.cancelExpiredTentativeContracts(any())).thenReturn(1);

        int cancelled = groupRoomBlockService.processDepositExpiredAutoCancel();

        assertThat(cancelled).isEqualTo(1);
        verify(groupContractService, times(1)).cancelExpiredTentativeContracts(any());
        verify(auditLogService, times(1)).recordLog(
                isNull(),
                eq("GROUP_CONTRACT_DEPOSIT_EXPIRED_AUTO_CANCEL"),
                eq("GROUP_CONTRACT"),
                eq("BATCH"),
                eq("TENTATIVE"),
                eq("CANCELLED_NO_DEPOSIT"),
                eq("SYSTEM_SCHEDULER"),
                eq("GroupCutoffScheduler")
        );
    }

    @Test
    @DisplayName("releaseUnpickedUpRoomsManually releases unpicked rooms for specific block")
    void testReleaseUnpickedUpRoomsManually_Success() {
        when(groupContractService.getEntityById(100L)).thenReturn(contract);
        when(groupRoomBlockRepository.findById(200L)).thenReturn(Optional.of(block));

        GroupRoomBlockResponse response = groupRoomBlockService.releaseUnpickedUpRoomsManually(100L, 200L);

        assertThat(response).isNotNull();
        assertThat(response.getReleasedRooms()).isEqualTo(8);
        assertThat(daily1.getBlockedQuantity()).isEqualTo(2);
        assertThat(daily1.getReleasedQuantity()).isEqualTo(4);

        verify(auditLogService, times(1)).recordLog(
                isNull(),
                eq("GROUP_CUTOFF_MANUAL_RELEASE"),
                eq("GROUP_ROOM_BLOCK"),
                eq("200"),
                any(),
                contains("Manually released 8 unpicked rooms"),
                eq("MANUAL_STAFF_ACTION"),
                eq("ManagementConsole")
        );
    }
}
