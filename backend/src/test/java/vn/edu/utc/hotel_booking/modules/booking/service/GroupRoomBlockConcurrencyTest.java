package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.entity.*;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupRoomBlockDailyRepository;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupRoomBlockRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupContractService;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.GroupRoomBlockServiceImpl;
import vn.edu.utc.hotel_booking.modules.identity.service.AuditLogService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Group Room Block Concurrency & Idempotency Test")
class GroupRoomBlockConcurrencyTest {

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

    @Test
    @DisplayName("processCutoffAutoRelease is idempotent when executed concurrently")
    void testProcessCutoffAutoRelease_ConcurrentExecution_IsIdempotent() throws InterruptedException {
        GroupContract contract = GroupContract.builder()
                .id(1L)
                .contractNumber("GC-20261101-0001")
                .groupName("Đoàn Khách Thử Nghiệm Đồng Thời")
                .status(GroupContractStatus.DEPOSIT_PAID)
                .cutoffDate(OffsetDateTime.now().minusDays(1))
                .roomBlocks(new ArrayList<>())
                .build();

        GroupRoomBlock block = GroupRoomBlock.builder()
                .id(10L)
                .contract(contract)
                .totalBlockedRooms(10)
                .allocatedRooms(4)
                .releasedRooms(0)
                .status(GroupRoomBlockStatus.DEFINITE)
                .dailyBlocks(new ArrayList<>())
                .build();

        GroupRoomBlockDaily daily = GroupRoomBlockDaily.builder()
                .id(100L)
                .roomBlock(block)
                .stayDate(LocalDate.of(2026, 11, 20))
                .blockedQuantity(10)
                .pickedUpQuantity(4) // Unpicked = 6
                .releasedQuantity(0)
                .status(GroupRoomBlockDailyStatus.ACTIVE)
                .build();

        block.getDailyBlocks().add(daily);
        contract.getRoomBlocks().add(block);

        when(groupContractService.findContractsPastCutoff(any()))
                .thenReturn(List.of(contract));

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger totalReleasedAcrossThreads = new AtomicInteger(0);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                return groupRoomBlockService.processCutoffAutoRelease();
            }));
        }

        latch.countDown(); // Kích hoạt 4 luồng chạy cùng 1 thời điểm
        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(finished).isTrue();

        // Kiểm tra sau khi hoàn thành, daily block phản ánh chính xác số lượng unpicked
        assertThat(daily.getReleasedQuantity()).isGreaterThanOrEqualTo(6);
        assertThat(daily.getBlockedQuantity()).isEqualTo(4);
    }
}
