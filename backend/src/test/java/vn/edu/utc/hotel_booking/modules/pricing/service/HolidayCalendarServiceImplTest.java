package vn.edu.utc.hotel_booking.modules.pricing.service;

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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HolidayCalendarSearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HolidayCalendarResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HolidayCalendar;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.HolidayCalendarMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.HolidayCalendarRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.HolidayCalendarServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HolidayCalendarServiceImplTest {

    @Mock
    HolidayCalendarRepository holidayCalendarRepository;

    @Mock
    PricingRuleRepository pricingRuleRepository;

    @Mock
    HolidayCalendarMapper holidayCalendarMapper;

    @InjectMocks
    HolidayCalendarServiceImpl holidayCalendarService;

    HolidayCalendar sampleHoliday;
    HolidayCalendarResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleHoliday = HolidayCalendar.builder()
                .id(1)
                .name("Tết Dương Lịch 2026")
                .date(LocalDate.of(2026, 1, 1))
                .status("ACTIVE")
                .build();

        sampleResponse = HolidayCalendarResponse.builder()
                .id(1)
                .name("Tết Dương Lịch 2026")
                .date(LocalDate.of(2026, 1, 1))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("getById - Thành công khi ID hợp lệ")
    void getById_Success() {
        when(holidayCalendarRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleHoliday));
        when(holidayCalendarMapper.toResponse(sampleHoliday)).thenReturn(sampleResponse);

        HolidayCalendarResponse response = holidayCalendarService.getById(1);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Tết Dương Lịch 2026");
        verify(holidayCalendarRepository).findByIdAndIsDeletedFalse(1);
    }

    @Test
    @DisplayName("create - Thành công khi ngày lễ chưa tồn tại")
    void create_Success() {
        HolidayCalendarCreateRequest request = HolidayCalendarCreateRequest.builder()
                .name("Giỗ Tổ Hùng Vương 2026")
                .date(LocalDate.of(2026, 4, 26))
                .build();

        when(holidayCalendarRepository.existsByNameAndDateAndIsDeletedFalse("Giỗ Tổ Hùng Vương 2026", LocalDate.of(2026, 4, 26)))
                .thenReturn(false);
        when(holidayCalendarMapper.toEntity(request)).thenReturn(sampleHoliday);
        when(holidayCalendarRepository.save(any())).thenReturn(sampleHoliday);
        when(holidayCalendarMapper.toResponse(sampleHoliday)).thenReturn(sampleResponse);

        HolidayCalendarResponse response = holidayCalendarService.create(request);

        assertThat(response).isNotNull();
        verify(holidayCalendarRepository).save(any());
    }

    @Test
    @DisplayName("create - Ném lỗi HOLIDAY_CALENDAR_ALREADY_EXISTS khi trùng tên và ngày")
    void create_Duplicate_ThrowsException() {
        HolidayCalendarCreateRequest request = HolidayCalendarCreateRequest.builder()
                .name("Tết Dương Lịch 2026")
                .date(LocalDate.of(2026, 1, 1))
                .build();

        when(holidayCalendarRepository.existsByNameAndDateAndIsDeletedFalse("Tết Dương Lịch 2026", LocalDate.of(2026, 1, 1)))
                .thenReturn(true);

        assertThatThrownBy(() -> holidayCalendarService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOLIDAY_CALENDAR_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("deleteBatch - Ném lỗi HOLIDAY_CALENDAR_IN_USE khi ngày lễ đang được dùng bởi PricingRule")
    void deleteBatch_InUse_ThrowsException() {
        when(holidayCalendarRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleHoliday));
        when(pricingRuleRepository.existsByHolidayCalendarIdAndIsDeletedFalse(1)).thenReturn(true);

        assertThatThrownBy(() -> holidayCalendarService.deleteBatch(List.of(1)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOLIDAY_CALENDAR_IN_USE);
    }

    @Test
    @DisplayName("deleteBatch - Thành công khi không có ràng buộc")
    void deleteBatch_Success() {
        when(holidayCalendarRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleHoliday));
        when(pricingRuleRepository.existsByHolidayCalendarIdAndIsDeletedFalse(1)).thenReturn(false);

        holidayCalendarService.deleteBatch(List.of(1));

        assertThat(sampleHoliday.getIsDeleted()).isTrue();
        verify(holidayCalendarRepository).save(sampleHoliday);
    }

    @Test
    @DisplayName("filter - Phân trang thành công")
    void filter_Success() {
        HolidayCalendarSearchDto searchDto = new HolidayCalendarSearchDto();
        searchDto.setPage(0);
        searchDto.setPageSize(10);

        Page<HolidayCalendar> page = new PageImpl<>(List.of(sampleHoliday));
        when(holidayCalendarRepository.filter(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(holidayCalendarMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<HolidayCalendarResponse> result = holidayCalendarService.filter(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
}
