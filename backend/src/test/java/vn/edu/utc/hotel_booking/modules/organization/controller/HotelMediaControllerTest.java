package vn.edu.utc.hotel_booking.modules.organization.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.modules.organization.dto.response.HotelImageResponse;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelMediaService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelMediaController — Unit Tests")
class HotelMediaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HotelMediaService hotelMediaService;

    @InjectMocks
    private HotelMediaController hotelMediaController;

    private final Short hotelId = 1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(hotelMediaController).build();
    }

    @Test
    @DisplayName("GET /api/v1/hotels/{hotelId}/images: Trả về danh sách ảnh 200 OK")
    void getImages_success() throws Exception {
        HotelImageResponse img1 = HotelImageResponse.builder()
                .id(101L)
                .url("http://cdn.local/img1.jpg")
                .originalFilename("img1.jpg")
                .sizeBytes(2048L)
                .isPrimary(true)
                .build();

        when(hotelMediaService.getHotelImages(hotelId)).thenReturn(List.of(img1));

        mockMvc.perform(get("/api/v1/hotels/{hotelId}/images", hotelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result[0].id").value(101))
                .andExpect(jsonPath("$.result[0].url").value("http://cdn.local/img1.jpg"))
                .andExpect(jsonPath("$.result[0].primary").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/hotels/{hotelId}/images: Tải lên ảnh thành công 201 Created")
    void uploadImages_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "image-bytes".getBytes());

        HotelImageResponse img = HotelImageResponse.builder()
                .id(102L)
                .url("http://cdn.local/test.jpg")
                .originalFilename("test.jpg")
                .sizeBytes(11L)
                .isPrimary(true)
                .build();

        when(hotelMediaService.uploadImages(eq(hotelId), any(), any())).thenReturn(List.of(img));

        mockMvc.perform(multipart("/api/v1/hotels/{hotelId}/images", hotelId)
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.result[0].id").value(102));
    }

    @Test
    @DisplayName("PATCH /api/v1/hotels/{hotelId}/images/{imageId}/primary: Đặt ảnh chính thành công 200 OK")
    void setPrimaryImage_success() throws Exception {
        mockMvc.perform(patch("/api/v1/hotels/{hotelId}/images/{imageId}/primary", hotelId, 105L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Đã đặt làm ảnh đại diện chính"));

        verify(hotelMediaService).setPrimaryImage(hotelId, 105L);
    }

    @Test
    @DisplayName("DELETE /api/v1/hotels/{hotelId}/images/{imageId}: Xóa ảnh thành công 200 OK")
    void deleteImage_success() throws Exception {
        mockMvc.perform(delete("/api/v1/hotels/{hotelId}/images/{imageId}", hotelId, 105L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Xóa ảnh khách sạn thành công"));

        verify(hotelMediaService).deleteImage(hotelId, 105L);
    }
}

