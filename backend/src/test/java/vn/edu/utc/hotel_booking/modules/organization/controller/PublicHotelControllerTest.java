package vn.edu.utc.hotel_booking.modules.organization.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelMediaService;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicHotelController — Unit Tests")
class PublicHotelControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HotelService hotelService;

    @Mock
    private HotelMediaService hotelMediaService;

    @InjectMocks
    private PublicHotelController publicHotelController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicHotelController).build();
    }

    @Test
    @DisplayName("GET /api/v1/public/hotels/{id}/images: Trả về danh sách ảnh công khai cho khách hàng")
    void getPublicHotelImages_success() throws Exception {
        HotelImageResponse img = HotelImageResponse.builder()
                .id(1L)
                .url("http://cdn.local/hotel_front.jpg")
                .originalFilename("hotel_front.jpg")
                .sizeBytes(1024L)
                .isPrimary(true)
                .build();

        when(hotelMediaService.getHotelImages((short) 1)).thenReturn(List.of(img));

        mockMvc.perform(get("/api/v1/public/hotels/{id}/images", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result[0].id").value(1))
                .andExpect(jsonPath("$.result[0].url").value("http://cdn.local/hotel_front.jpg"))
                .andExpect(jsonPath("$.result[0].primary").value(true));
    }
}

