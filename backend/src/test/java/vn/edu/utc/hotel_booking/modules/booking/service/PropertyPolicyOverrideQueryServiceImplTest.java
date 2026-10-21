package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.repository.PropertyPolicyOverrideRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.PropertyPolicyOverrideQueryServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyPolicyOverrideQueryServiceImplTest {

    @Mock
    PropertyPolicyOverrideRepository propertyPolicyOverrideRepository;

    @InjectMocks
    PropertyPolicyOverrideQueryServiceImpl queryService;

    @Test
    @DisplayName("existsByCancellationPolicyId - Trả về true khi repository tìm thấy override")
    void existsByCancellationPolicyId_Success() {
        when(propertyPolicyOverrideRepository.existsByCancellationPolicyIdAndIsDeletedFalse(100)).thenReturn(true);

        boolean result = queryService.existsByCancellationPolicyId(100);

        assertThat(result).isTrue();
        verify(propertyPolicyOverrideRepository).existsByCancellationPolicyIdAndIsDeletedFalse(100);
    }

    @Test
    @DisplayName("existsByCancellationPolicyId - Trả về false khi id là null")
    void existsByCancellationPolicyId_NullId_ReturnsFalse() {
        boolean result = queryService.existsByCancellationPolicyId(null);

        assertThat(result).isFalse();
        verifyNoInteractions(propertyPolicyOverrideRepository);
    }

    @Test
    @DisplayName("existsByNoShowPolicyId - Trả về true khi repository tìm thấy override")
    void existsByNoShowPolicyId_Success() {
        when(propertyPolicyOverrideRepository.existsByNoShowPolicyIdAndIsDeletedFalse(200)).thenReturn(true);

        boolean result = queryService.existsByNoShowPolicyId(200);

        assertThat(result).isTrue();
        verify(propertyPolicyOverrideRepository).existsByNoShowPolicyIdAndIsDeletedFalse(200);
    }

    @Test
    @DisplayName("existsByNoShowPolicyId - Trả về false khi id là null")
    void existsByNoShowPolicyId_NullId_ReturnsFalse() {
        boolean result = queryService.existsByNoShowPolicyId(null);

        assertThat(result).isFalse();
        verifyNoInteractions(propertyPolicyOverrideRepository);
    }
}
