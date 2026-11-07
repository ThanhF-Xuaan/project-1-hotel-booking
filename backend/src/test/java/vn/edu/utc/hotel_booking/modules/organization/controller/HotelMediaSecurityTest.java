package vn.edu.utc.hotel_booking.modules.organization.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelMediaService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        HotelMediaController.class,
        HotelMediaSecurityTest.MethodSecurityConfig.class
})
@DisplayName("HotelMediaController — Method Security Tests")
class HotelMediaSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
        @Bean
        public HotelMediaService hotelMediaService() {
            HotelMediaService service = mock(HotelMediaService.class);
            when(service.uploadImages(any(), any(), any())).thenReturn(Collections.emptyList());
            when(service.getHotelImages(any())).thenReturn(Collections.emptyList());
            return service;
        }
    }

    @Autowired
    private HotelMediaController controller;

    private final MockMultipartFile sampleFile = new MockMultipartFile(
            "files", "photo.jpg", "image/jpeg", "dummy".getBytes()
    );

    @Test
    @WithMockUser(roles = "CHAIN_ADMIN")
    @DisplayName("CHAIN_ADMIN được phép upload ảnh")
    void uploadImages_chainAdminRole_allowed() {
        assertDoesNotThrow(() -> controller.uploadImages((short) 1, List.of(sampleFile)));
    }

    @Test
    @WithMockUser(roles = "PROPERTY_MANAGER")
    @DisplayName("PROPERTY_MANAGER được phép đặt ảnh chính và xóa ảnh")
    void setPrimaryAndDelete_propertyManagerRole_allowed() {
        assertDoesNotThrow(() -> controller.setPrimaryImage((short) 1, 100L));
        assertDoesNotThrow(() -> controller.deleteImage((short) 1, 100L));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("RECEPTIONIST được phép xem ảnh nhưng KHÔNG được phép upload ảnh")
    void uploadImages_receptionistRole_denied() {
        assertDoesNotThrow(() -> controller.getImages((short) 1));
        assertThrows(Exception.class, () -> controller.uploadImages((short) 1, List.of(sampleFile)));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("CUSTOMER bị từ chối khi gọi API quản lý ảnh")
    void uploadImages_customerRole_denied() {
        assertThrows(Exception.class, () -> controller.uploadImages((short) 1, List.of(sampleFile)));
        assertThrows(Exception.class, () -> controller.setPrimaryImage((short) 1, 100L));
        assertThrows(Exception.class, () -> controller.deleteImage((short) 1, 100L));
    }
}

