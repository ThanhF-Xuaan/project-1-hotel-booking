package vn.edu.utc.hotel_booking.common.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.GlobalHandlerException;
import vn.edu.utc.hotel_booking.common.storage.domain.StorageCategory;
import vn.edu.utc.hotel_booking.common.storage.dto.CategoryMetaResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaItemResponse;
import vn.edu.utc.hotel_booking.common.storage.dto.MediaSearchDto;
import vn.edu.utc.hotel_booking.common.storage.service.MediaManagementService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaManagementController — Unit Tests")
class MediaManagementControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private MediaManagementService mediaManagementService;

    @InjectMocks
    private MediaManagementController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandlerException())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/media/filter - Trả về danh sách hình ảnh phân trang")
    void filterMedia_returnsPage() throws Exception {
        MediaItemResponse item = MediaItemResponse.builder()
                .id(1L)
                .originalFilename("banner.jpg")
                .category(StorageCategory.MARKETING_BANNER)
                .url("http://localhost:9000/download/banner.jpg")
                .isPrimary(true)
                .build();

        PageResponse<MediaItemResponse> pageResponse = PageResponse.<MediaItemResponse>builder()
                .content(List.of(item))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(mediaManagementService.filterMedia(any(MediaSearchDto.class))).thenReturn(pageResponse);

        mockMvc.perform(post("/api/v1/media/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MediaSearchDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.content[0].id").value(1))
                .andExpect(jsonPath("$.result.content[0].originalFilename").value("banner.jpg"));
    }

    @Test
    @DisplayName("GET /api/v1/media/categories - Trả về danh sách danh mục hỗ trợ")
    void getCategories_returnsList() throws Exception {
        CategoryMetaResponse cat = CategoryMetaResponse.builder()
                .category(StorageCategory.HOTEL_PROPERTY_PHOTO)
                .label("Khuôn viên / Khách sạn cơ sở")
                .folderPrefix("properties")
                .isPublic(true)
                .build();

        when(mediaManagementService.getAvailableCategories()).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/v1/media/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result[0].category").value("HOTEL_PROPERTY_PHOTO"));
    }

    @Test
    @DisplayName("PATCH /api/v1/media/{id}/primary - Đặt làm ảnh đại diện chính")
    void setPrimary_success() throws Exception {
        mockMvc.perform(patch("/api/v1/media/10/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(mediaManagementService).setPrimary(10L);
    }

    @Test
    @DisplayName("DELETE /api/v1/media/{id} - Xóa ảnh thành công")
    void deleteMedia_success() throws Exception {
        mockMvc.perform(delete("/api/v1/media/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(mediaManagementService).deleteMedia(10L);
    }
}

