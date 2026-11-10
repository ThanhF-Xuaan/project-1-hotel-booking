package vn.edu.utc.hotel_booking.modules.pricing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.CampaignCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.CampaignResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.Campaign;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.CampaignMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.CampaignRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.CampaignServiceImpl;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    @Mock
    CampaignRepository campaignRepository;

    @Mock
    HotelService hotelService;

    @Mock
    CampaignMapper campaignMapper;

    @InjectMocks
    CampaignServiceImpl campaignService;

    Hotel sampleHotel;
    Campaign sampleCampaign;

    @BeforeEach
    void setUp() {
        sampleHotel = Hotel.builder().id((short) 1).name("Khách sạn Grand Hà Nội").build();
        sampleCampaign = Campaign.builder()
                .id(10)
                .hotel(sampleHotel)
                .name("Hè Sôi Động 2026")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Tạo chiến dịch thành công khi ngày hợp lệ và gọi qua HotelService")
    void createCampaign_Success() {
        CampaignCreateRequest request = CampaignCreateRequest.builder()
                .hotelId((short) 1)
                .name("Hè Sôi Động 2026")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .status("ACTIVE")
                .build();

        CampaignResponse response = CampaignResponse.builder()
                .id(10)
                .hotelId((short) 1)
                .name("Hè Sôi Động 2026")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .status("ACTIVE")
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(sampleHotel);
        when(campaignRepository.findByHotelIdAndNameAndIsDeletedFalse((short) 1, "Hè Sôi Động 2026"))
                .thenReturn(Optional.empty());
        when(campaignMapper.toEntity(request)).thenReturn(sampleCampaign);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(sampleCampaign);
        when(campaignMapper.toResponse(sampleCampaign)).thenReturn(response);

        CampaignResponse result = campaignService.createCampaign(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Hè Sôi Động 2026");
        verify(hotelService).getEntityById((short) 1);
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi ngày kết thúc trước ngày bắt đầu")
    void createCampaign_EndDateBeforeStartDate_ThrowsException() {
        CampaignCreateRequest request = CampaignCreateRequest.builder()
                .hotelId((short) 1)
                .name("Chiến dịch lỗi ngày")
                .startDate(LocalDate.of(2026, 8, 31))
                .endDate(LocalDate.of(2026, 6, 1))
                .build();

        AppException ex = assertThrows(AppException.class, () -> campaignService.createCampaign(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        verifyNoInteractions(hotelService);
        verify(campaignRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tên chiến dịch đã tồn tại trong khách sạn")
    void createCampaign_DuplicateName_ThrowsException() {
        CampaignCreateRequest request = CampaignCreateRequest.builder()
                .hotelId((short) 1)
                .name("Hè Sôi Động 2026")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(sampleHotel);
        when(campaignRepository.findByHotelIdAndNameAndIsDeletedFalse((short) 1, "Hè Sôi Động 2026"))
                .thenReturn(Optional.of(sampleCampaign));

        AppException ex = assertThrows(AppException.class, () -> campaignService.createCampaign(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
    }
}
