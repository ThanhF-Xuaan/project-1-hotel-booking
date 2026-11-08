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
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicyCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.HotelAgePolicySearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.HotelAgePolicyResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.HotelAgePolicy;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.HotelAgePolicyMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.HotelAgePolicyRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.HotelAgePolicyServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelAgePolicyServiceImplTest {

    @Mock
    HotelAgePolicyRepository hotelAgePolicyRepository;

    @Mock
    HotelService hotelService;

    @Mock
    HotelAgePolicyMapper hotelAgePolicyMapper;

    @InjectMocks
    HotelAgePolicyServiceImpl hotelAgePolicyService;

    Hotel sampleHotel;
    HotelAgePolicy childPolicy;
    HotelAgePolicy infantPolicy;
    HotelAgePolicy adultPolicy;
    HotelAgePolicyResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleHotel = Hotel.builder().id((short) 1).name("SmartStay Hanoi").build();

        infantPolicy = HotelAgePolicy.builder()
                .id((short) 1)
                .hotel(sampleHotel)
                .guestType("INFANT")
                .minAge((short) 0)
                .maxAge((short) 5)
                .status("ACTIVE")
                .build();

        childPolicy = HotelAgePolicy.builder()
                .id((short) 2)
                .hotel(sampleHotel)
                .guestType("CHILD")
                .minAge((short) 6)
                .maxAge((short) 11)
                .status("ACTIVE")
                .build();

        adultPolicy = HotelAgePolicy.builder()
                .id((short) 3)
                .hotel(sampleHotel)
                .guestType("ADULT")
                .minAge((short) 12)
                .maxAge((short) 100)
                .status("ACTIVE")
                .build();

        sampleResponse = HotelAgePolicyResponse.builder()
                .id((short) 2)
                .hotelId((short) 1)
                .hotelName("SmartStay Hanoi")
                .guestType("CHILD")
                .minAge((short) 6)
                .maxAge((short) 11)
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("getById - Thành công khi ID hợp lệ")
    void getById_Success() {
        when(hotelAgePolicyRepository.findByIdAndIsDeletedFalse((short) 2)).thenReturn(Optional.of(childPolicy));
        when(hotelAgePolicyMapper.toResponse(childPolicy)).thenReturn(sampleResponse);

        HotelAgePolicyResponse response = hotelAgePolicyService.getById((short) 2);

        assertThat(response).isNotNull();
        assertThat(response.getGuestType()).isEqualTo("CHILD");
        verify(hotelAgePolicyRepository).findByIdAndIsDeletedFalse((short) 2);
    }

    @Test
    @DisplayName("create - Thành công khi thông tin hợp lệ")
    void create_Success() {
        HotelAgePolicyCreateRequest request = HotelAgePolicyCreateRequest.builder()
                .hotelId((short) 1)
                .guestType("CHILD")
                .minAge((short) 6)
                .maxAge((short) 11)
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(sampleHotel);
        when(hotelAgePolicyRepository.existsByHotelIdAndGuestTypeAndIsDeletedFalse((short) 1, "CHILD")).thenReturn(false);
        when(hotelAgePolicyRepository.findActivePoliciesByScope((short) 1, null)).thenReturn(List.of(infantPolicy, adultPolicy));
        when(hotelAgePolicyMapper.toEntity(request)).thenReturn(childPolicy);
        when(hotelAgePolicyRepository.save(any())).thenReturn(childPolicy);
        when(hotelAgePolicyMapper.toResponse(childPolicy)).thenReturn(sampleResponse);

        HotelAgePolicyResponse response = hotelAgePolicyService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getGuestType()).isEqualTo("CHILD");
        verify(hotelService).getEntityById((short) 1);
        verify(hotelAgePolicyRepository).save(any());
    }

    @Test
    @DisplayName("create - Ném lỗi khi minAge > maxAge")
    void create_InvalidMinMax_ThrowsException() {
        HotelAgePolicyCreateRequest request = HotelAgePolicyCreateRequest.builder()
                .hotelId((short) 1)
                .guestType("CHILD")
                .minAge((short) 12)
                .maxAge((short) 6)
                .build();

        assertThatThrownBy(() -> hotelAgePolicyService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOTEL_AGE_POLICY_RANGE_INVALID);
    }

    @Test
    @DisplayName("create - Ném lỗi khi dải tuổi bị chồng lấn với loại khách khác")
    void create_AgeOverlap_ThrowsException() {
        HotelAgePolicyCreateRequest request = HotelAgePolicyCreateRequest.builder()
                .hotelId((short) 1)
                .guestType("CHILD")
                .minAge((short) 5) // Chồng lấn với INFANT (0-5)
                .maxAge((short) 11)
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(sampleHotel);
        when(hotelAgePolicyRepository.existsByHotelIdAndGuestTypeAndIsDeletedFalse((short) 1, "CHILD")).thenReturn(false);
        when(hotelAgePolicyRepository.findActivePoliciesByScope((short) 1, null)).thenReturn(List.of(infantPolicy));

        assertThatThrownBy(() -> hotelAgePolicyService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOTEL_AGE_POLICY_RANGE_INVALID);
    }

    @Test
    @DisplayName("resolveGuestTypeByAge - Nhận diện đúng INFANT, CHILD, ADULT")
    void resolveGuestTypeByAge_Success() {
        when(hotelAgePolicyRepository.findCandidatesForHotel((short) 1)).thenReturn(List.of(infantPolicy, childPolicy, adultPolicy));
        when(hotelAgePolicyMapper.toResponseList(any())).thenReturn(List.of(
                HotelAgePolicyResponse.builder().guestType("INFANT").minAge((short) 0).maxAge((short) 5).build(),
                HotelAgePolicyResponse.builder().guestType("CHILD").minAge((short) 6).maxAge((short) 11).build(),
                HotelAgePolicyResponse.builder().guestType("ADULT").minAge((short) 12).maxAge((short) 100).build()
        ));

        assertThat(hotelAgePolicyService.resolveGuestTypeByAge((short) 1, 3)).isEqualTo("INFANT");
        assertThat(hotelAgePolicyService.resolveGuestTypeByAge((short) 1, 8)).isEqualTo("CHILD");
        assertThat(hotelAgePolicyService.resolveGuestTypeByAge((short) 1, 15)).isEqualTo("ADULT");
    }

    @Test
    @DisplayName("filter - Phân trang thành công")
    void filter_Success() {
        HotelAgePolicySearchDto searchDto = new HotelAgePolicySearchDto();
        searchDto.setPage(0);
        searchDto.setPageSize(10);

        Page<HotelAgePolicy> page = new PageImpl<>(List.of(childPolicy));
        when(hotelAgePolicyRepository.filter(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(hotelAgePolicyMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<HotelAgePolicyResponse> result = hotelAgePolicyService.filter(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
}
