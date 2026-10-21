package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.entity.CancellationPolicy;
import vn.edu.utc.hotel_booking.modules.booking.entity.NoShowPolicy;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.PolicyLookupServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyLookupServiceImplTest {

    @Mock
    PropertyPolicyOverrideQueryService propertyPolicyOverrideQueryService;

    @Mock
    CancellationPolicyService cancellationPolicyService;

    @Mock
    NoShowPolicyService noShowPolicyService;

    @InjectMocks
    PolicyLookupServiceImpl policyLookupService;

    @Test
    @DisplayName("existsOverrideByCancellationPolicyId - Ủy quyền cho PropertyPolicyOverrideQueryService")
    void existsOverrideByCancellationPolicyId_Success() {
        when(propertyPolicyOverrideQueryService.existsByCancellationPolicyId(1)).thenReturn(true);

        boolean exists = policyLookupService.existsOverrideByCancellationPolicyId(1);

        assertThat(exists).isTrue();
        verify(propertyPolicyOverrideQueryService).existsByCancellationPolicyId(1);
    }

    @Test
    @DisplayName("existsOverrideByCancellationPolicyId - Trả về false khi id là null")
    void existsOverrideByCancellationPolicyId_NullId_ReturnsFalse() {
        boolean exists = policyLookupService.existsOverrideByCancellationPolicyId(null);

        assertThat(exists).isFalse();
        verifyNoInteractions(propertyPolicyOverrideQueryService);
    }

    @Test
    @DisplayName("existsOverrideByNoShowPolicyId - Ủy quyền cho PropertyPolicyOverrideQueryService")
    void existsOverrideByNoShowPolicyId_Success() {
        when(propertyPolicyOverrideQueryService.existsByNoShowPolicyId(2)).thenReturn(true);

        boolean exists = policyLookupService.existsOverrideByNoShowPolicyId(2);

        assertThat(exists).isTrue();
        verify(propertyPolicyOverrideQueryService).existsByNoShowPolicyId(2);
    }

    @Test
    @DisplayName("existsOverrideByNoShowPolicyId - Trả về false khi id là null")
    void existsOverrideByNoShowPolicyId_NullId_ReturnsFalse() {
        boolean exists = policyLookupService.existsOverrideByNoShowPolicyId(null);

        assertThat(exists).isFalse();
        verifyNoInteractions(propertyPolicyOverrideQueryService);
    }

    @Test
    @DisplayName("getCancellationPolicyEntityById - Ủy quyền cho CancellationPolicyService")
    void getCancellationPolicyEntityById_Success() {
        CancellationPolicy policy = CancellationPolicy.builder().id(10).policyCode("CANCEL_FIT").build();
        when(cancellationPolicyService.getEntityById(10)).thenReturn(policy);

        CancellationPolicy result = policyLookupService.getCancellationPolicyEntityById(10);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
        verify(cancellationPolicyService).getEntityById(10);
    }

    @Test
    @DisplayName("getNoShowPolicyEntityById - Ủy quyền cho NoShowPolicyService")
    void getNoShowPolicyEntityById_Success() {
        NoShowPolicy policy = NoShowPolicy.builder().id(20).policyCode("NOSHOW_FIT").build();
        when(noShowPolicyService.getEntityById(20)).thenReturn(policy);

        NoShowPolicy result = policyLookupService.getNoShowPolicyEntityById(20);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20);
        verify(noShowPolicyService).getEntityById(20);
    }
}
