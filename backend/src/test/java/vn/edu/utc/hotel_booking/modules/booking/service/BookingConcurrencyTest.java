package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.BookingResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingStatus;
import vn.edu.utc.hotel_booking.modules.booking.mapper.BookingMapper;
import vn.edu.utc.hotel_booking.modules.booking.repository.*;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomAvailability;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomAvailabilityRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomInstanceRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomMaintenanceBlockRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomSlotService;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingRoomService;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.RoomAvailabilityServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Sprint 4 — Task 4: Concurrency & Double Booking Stress Test")
class BookingConcurrencyTest {

    // --- Mock dependencies cho RoomAvailabilityService ---
    @Mock private RoomAvailabilityRepository roomAvailabilityRepository;
    @Mock private HotelRoomTypeService hotelRoomTypeService;
    @InjectMocks private RoomAvailabilityServiceImpl roomAvailabilityService;

    // --- Mock dependencies cho BookingService assignRoom ---
    @Mock private BookingRoomService bookingRoomService;
    @Mock private RoomInstanceService roomInstanceService;
    @Mock private RoomSlotService roomSlotService;
    @Mock private BookingRoomRepository bookingRoomRepository;
    @Mock private RoomInstanceRepository roomInstanceRepository;
    @Mock private RoomSlotRepository roomSlotRepository;
    @Mock private RoomMaintenanceBlockRepository roomMaintenanceBlockRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingMapper bookingMapper;
    @InjectMocks private BookingServiceImpl bookingService;

    private Hotel sampleHotel;
    private RoomType sampleRoomType;
    private HotelRoomType sampleHotelRoomType;
    private RoomInstance sampleRoomInstance;
    private LocalDate checkIn;
    private LocalDate checkOut;

    @BeforeEach
    void setUp() {
        sampleHotel = Hotel.builder().id((short) 1).name("Khách sạn UTC Hà Nội").build();
        sampleRoomType = RoomType.builder().id((short) 1).code("STD").name("Standard Room").build();
        sampleHotelRoomType = HotelRoomType.builder()
                .id(1)
                .hotel(sampleHotel)
                .roomType(sampleRoomType)
                .totalQuantity(1) // Chỉ có đúng 1 phòng duy nhất trong kho
                .basePrice(BigDecimal.valueOf(1000000))
                .build();

        sampleRoomInstance = RoomInstance.builder()
                .id(101)
                .hotel(sampleHotel)
                .hotelRoomType(sampleHotelRoomType)
                .roomNumber("101A")
                .currentStatus("READY")
                .build();

        checkIn = LocalDate.now().plusDays(10);
        checkOut = checkIn.plusDays(1); // 1 đêm
    }

    @Test
    @DisplayName("Kịch bản 1: 5 Request song song đặt cùng 1 phòng tồn kho cuối cùng → Đúng 1 thành công, 4 conflict (409)")
    void concurrentBookingInventory_onlyOneSucceeds_othersConflict() throws InterruptedException {
        int concurrency = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        when(hotelRoomTypeService.getEntityById(1)).thenReturn(sampleHotelRoomType);

        // Mô phỏng atomic update: chỉ đúng 1 luồng được cấp inventory phòng cuối cùng
        AtomicInteger remainingRooms = new AtomicInteger(1);

        doAnswer(invocation -> {
            synchronized (remainingRooms) {
                if (remainingRooms.get() <= 0) {
                    throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE, "Không đủ phòng trống trong khoảng thời gian yêu cầu");
                }
                remainingRooms.decrementAndGet();
                return null;
            }
        }).when(roomAvailabilityRepository).save(any(RoomAvailability.class));

        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDateWithLock(eq(1), eq(checkIn)))
                .thenAnswer(invocation -> Optional.of(RoomAvailability.builder()
                        .id(1L)
                        .hotelRoomType(sampleHotelRoomType)
                        .date(checkIn)
                        .totalRooms(1)
                        .bookedRooms(1 - remainingRooms.get())
                        .lockedRooms(0)
                        .oooRooms(0)
                        .version(0L)
                        .build()));

        when(roomAvailabilityRepository.findByHotelRoomTypeIdAndDate(eq(1), eq(checkIn)))
                .thenAnswer(invocation -> Optional.of(RoomAvailability.builder()
                        .id(1L)
                        .hotelRoomType(sampleHotelRoomType)
                        .date(checkIn)
                        .totalRooms(1)
                        .bookedRooms(1 - remainingRooms.get())
                        .lockedRooms(0)
                        .oooRooms(0)
                        .version(0L)
                        .build()));

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Giải phóng đồng loạt tất cả các luồng cùng 1 thời điểm
                    roomAvailabilityService.confirmBookingInventory(1, checkIn, checkOut, 1);
                    successCount.incrementAndGet();
                } catch (AppException e) {
                    if (e.getErrorCode() == ErrorCode.ROOM_NOT_AVAILABLE
                            || e.getErrorCode().getStatusCode().value() == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (ObjectOptimisticLockingFailureException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Bắn 5 request song song

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // Kiểm chứng bất biến:
        // 1. Chỉ duy nhất 1 request thành công
        assertThat(successCount.get()).isEqualTo(1);
        // 2. Toàn bộ 4 request tranh chấp bị ném lỗi Conflict 409
        assertThat(conflictCount.get()).isEqualTo(concurrency - 1);
        // 3. Tồn kho không bị âm (Không xảy ra Overbooking / Double Booking)
        assertThat(remainingRooms.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("Kịch bản 2: 5 Request song song xếp cùng 1 phòng vật lý (101A) cho cùng ngày → Đúng 1 thành công, 4 conflict 409")
    void concurrentPhysicalRoomAssignment_noDoubleBooking() throws InterruptedException {
        int concurrency = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        Booking booking = Booking.builder()
                .id(1L)
                .hotel(sampleHotel)
                .status(BookingStatus.CONFIRMED)
                .build();

        BookingDetail detail = BookingDetail.builder()
                .id(1L)
                .booking(booking)
                .hotelRoomType(sampleHotelRoomType)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .build();

        when(roomInstanceService.getEntityById(101)).thenReturn(sampleRoomInstance);
        when(bookingMapper.toResponse(any())).thenReturn(new BookingResponse());

        // Mô phỏng atomic pessimistic serialization: luồng đầu tiên chiếm phòng 101A
        AtomicReference<BookingRoom> assignedOwner = new AtomicReference<>(null);

        when(bookingRoomService.findOverlappingAssignedRooms(eq(101), any(), any()))
                .thenAnswer(inv -> {
                    synchronized (assignedOwner) {
                        BookingRoom owner = assignedOwner.get();
                        if (owner != null) {
                            return List.of(owner);
                        }
                        // Luồng đầu tiên chiếm phòng thành công
                        BookingRoom claimer = BookingRoom.builder().id(999L).build();
                        assignedOwner.set(claimer);
                        return Collections.emptyList();
                    }
                });

        // Setup 5 booking room ID 1..5
        for (long id = 1; id <= concurrency; id++) {
            BookingRoom br = BookingRoom.builder()
                    .id(id)
                    .bookingDetail(detail)
                    .build();
            when(bookingRoomService.getEntityById(id)).thenReturn(br);
        }

        for (int i = 0; i < concurrency; i++) {
            final long bookingRoomId = (long) (i + 1);

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Bắn 5 request cùng lúc
                    bookingService.assignRoom(bookingRoomId, 101);
                    successCount.incrementAndGet();
                } catch (AppException e) {
                    if (e.getErrorCode() == ErrorCode.ROOM_ALREADY_ASSIGNED
                            || e.getErrorCode().getStatusCode().value() == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // unexpected
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // Verify kết quả:
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(concurrency - 1);
        assertThat(assignedOwner.get()).isNotNull();
    }
}
