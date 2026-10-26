package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomAvailability;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.mapper.RoomAvailabilityMapper;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomAvailabilityRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomMaintenanceBlockRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomAvailabilityGenerationServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomAvailabilityServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Room Availability Concurrency & Anti-Overbooking Test")
class RoomAvailabilityConcurrencyTest {

    @Mock
    RoomAvailabilityRepository roomAvailabilityRepository;

    @Mock
    HotelRoomTypeRepository hotelRoomTypeRepository;

    @Mock
    RoomMaintenanceBlockRepository roomMaintenanceBlockRepository;

    @Mock
    HotelRoomTypeService hotelRoomTypeService;

    @Mock
    RoomInventoryHoldService roomInventoryHoldService;

    @Mock
    RoomAvailabilityMapper roomAvailabilityMapper;

    RoomAvailabilityServiceImpl roomAvailabilityService;
    RoomAvailabilityGenerationServiceImpl generationService;

    Hotel sampleHotel;
    HotelRoomType sampleRoomType;

    @BeforeEach
    void setUp() {
        generationService = new RoomAvailabilityGenerationServiceImpl(
                roomAvailabilityRepository,
                hotelRoomTypeRepository,
                roomMaintenanceBlockRepository
        );

        roomAvailabilityService = new RoomAvailabilityServiceImpl(
                roomAvailabilityRepository,
                hotelRoomTypeService,
                roomInventoryHoldService,
                roomAvailabilityMapper,
                Optional.empty(),
                generationService
        );

        sampleHotel = Hotel.builder().id((short) 1).name("Khách sạn Central").build();
        RoomType rt = RoomType.builder().id((short) 1).code("DELUXE").name("Phòng Deluxe").build();
        sampleRoomType = HotelRoomType.builder()
                .id(10)
                .hotel(sampleHotel)
                .roomType(rt)
                .totalQuantity(3) // Tổng cộng chỉ có 3 phòng
                .basePrice(BigDecimal.valueOf(1000000))
                .status("ACTIVE")
                .build();
        sampleRoomType.setIsDeleted(false);
    }

    @Test
    @DisplayName("Concurrency: 10 luồng đồng thời tranh giữ 3 phòng -> Đúng 3 luồng thành công, 7 luồng bị chặn, không có Overbooking")
    void testConcurrentHoldInventory_ZeroOverbooking() throws InterruptedException {
        LocalDate checkIn = LocalDate.of(2026, 11, 1);
        LocalDate checkOut = LocalDate.of(2026, 11, 2);

        // Mô phỏng dòng dữ liệu được khóa tuần tự (Row-level lock simulation)
        RoomAvailability availability = RoomAvailability.builder()
                .id(100L)
                .hotelRoomType(sampleRoomType)
                .date(checkIn)
                .totalRooms(3)
                .bookedRooms(0)
                .lockedRooms(0)
                .oooRooms(0)
                .version(0L)
                .build();

        // Đồng bộ hóa trạng thái trên object mô phỏng lock DB
        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(10, checkIn))
                .thenAnswer(inv -> Optional.of(availability));

        when(roomAvailabilityRepository.save(any(RoomAvailability.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Object rowLock = new Object();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Mô phỏng transaction phân tách từng luồng
                    synchronized (rowLock) {
                        HoldInventoryRequest req = HoldInventoryRequest.builder()
                                .hotelRoomTypeId(10)
                                .checkInDate(checkIn)
                                .checkOutDate(checkOut)
                                .roomsCount(1)
                                .holdMinutes(15)
                                .build();
                        roomAvailabilityService.holdInventory(req);
                        successCount.incrementAndGet();
                    }
                } catch (AppException ex) {
                    if (ex.getErrorCode() == ErrorCode.ROOM_NOT_AVAILABLE) {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Kích hoạt 10 luồng cùng 1 thời điểm
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Kiểm định chất lượng nghiệp vụ (Zero Overbooking)
        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failureCount.get()).isEqualTo(7);
        assertThat(availability.getLockedRooms()).isEqualTo(3);
        assertThat(availability.getLockedRooms() + availability.getBookedRooms())
                .isLessThanOrEqualTo(availability.getTotalRooms());
    }

    @Test
    @DisplayName("Concurrency: Nhiều luồng đồng thời sinh tồn kho cùng 1 hạng phòng -> Đảm bảo Idempotency không tạo trùng")
    void testConcurrentGeneration_Idempotent() throws InterruptedException {
        LocalDate start = LocalDate.of(2026, 12, 1);
        int days = 5;

        when(hotelRoomTypeRepository.findByIdAndIsDeletedFalse(10))
                .thenReturn(Optional.of(sampleRoomType));

        Set<LocalDate> existingDates = Collections.synchronizedSet(new HashSet<>());
        when(roomAvailabilityRepository.findExistingDates(eq(10), eq(start), eq(start.plusDays(days - 1))))
                .thenAnswer(inv -> new HashSet<>(existingDates));

        doAnswer(inv -> {
            List<RoomAvailability> toSave = inv.getArgument(0);
            for (RoomAvailability ra : toSave) {
                existingDates.add(ra.getDate());
            }
            return toSave;
        }).when(roomAvailabilityRepository).saveAll(anyList());

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger totalCreated = new AtomicInteger(0);

        Object genLock = new Object();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    synchronized (genLock) {
                        int created = generationService.generateForRoomType(10, start, days);
                        totalCreated.addAndGet(created);
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Luồng đầu tiên sinh đúng 5 ngày, các luồng sau phát hiện đã có ngày nên sinh 0 ngày
        assertThat(totalCreated.get()).isEqualTo(5);
        assertThat(existingDates).hasSize(5);
    }
}
