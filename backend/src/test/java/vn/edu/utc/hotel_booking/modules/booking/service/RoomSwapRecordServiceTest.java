package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.entity.RoomSwapRecord;
import vn.edu.utc.hotel_booking.modules.booking.repository.RoomSwapRecordRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.RoomSwapRecordServiceImpl;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomSwapRecordServiceTest {

    @Mock
    private RoomSwapRecordRepository roomSwapRecordRepository;

    @InjectMocks
    private RoomSwapRecordServiceImpl roomSwapRecordService;

    @Test
    @DisplayName("Lưu bản ghi đổi phòng thành công")
    void save_Success() {
        RoomSwapRecord record = RoomSwapRecord.builder()
                .id(1L)
                .reason("Khách yêu cầu tầng cao")
                .swapDate(LocalDate.now())
                .effectiveDate(LocalDate.now())
                .isUpgrade(false)
                .isComplimentary(true)
                .build();

        when(roomSwapRecordRepository.save(record)).thenReturn(record);

        RoomSwapRecord saved = roomSwapRecordService.save(record);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getReason()).isEqualTo("Khách yêu cầu tầng cao");
        verify(roomSwapRecordRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Tìm danh sách đổi phòng theo BookingRoomId")
    void findByBookingRoomId_Success() {
        RoomSwapRecord record = RoomSwapRecord.builder().id(10L).build();
        when(roomSwapRecordRepository.findByBookingRoomId(100L)).thenReturn(List.of(record));

        List<RoomSwapRecord> results = roomSwapRecordService.findByBookingRoomId(100L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(10L);
        verify(roomSwapRecordRepository, times(1)).findByBookingRoomId(100L);
    }

    @Test
    @DisplayName("Tìm danh sách đổi phòng theo BookingId")
    void findByBookingId_Success() {
        RoomSwapRecord record = RoomSwapRecord.builder().id(20L).build();
        when(roomSwapRecordRepository.findByBookingRoomBookingDetailBookingId(200L)).thenReturn(List.of(record));

        List<RoomSwapRecord> results = roomSwapRecordService.findByBookingId(200L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(20L);
        verify(roomSwapRecordRepository, times(1)).findByBookingRoomBookingDetailBookingId(200L);
    }

    @Test
    @DisplayName("Tìm danh sách đổi phòng theo khoảng ngày swapDate")
    void findBySwapDateBetween_Success() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        RoomSwapRecord record = RoomSwapRecord.builder().id(30L).build();
        when(roomSwapRecordRepository.findBySwapDateBetween(start, end)).thenReturn(List.of(record));

        List<RoomSwapRecord> results = roomSwapRecordService.findBySwapDateBetween(start, end);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(30L);
        verify(roomSwapRecordRepository, times(1)).findBySwapDateBetween(start, end);
    }
}
