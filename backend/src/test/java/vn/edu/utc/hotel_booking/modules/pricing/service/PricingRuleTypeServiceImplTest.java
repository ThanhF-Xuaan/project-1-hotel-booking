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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.PricingRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.PricingRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.PricingRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.PricingRuleTypeMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.PricingRuleTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.PricingRuleTypeServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingRuleTypeServiceImplTest {

    @Mock
    PricingRuleTypeRepository pricingRuleTypeRepository;

    @Mock
    PricingRuleRepository pricingRuleRepository;

    @Mock
    PricingRuleTypeMapper pricingRuleTypeMapper;

    @InjectMocks
    PricingRuleTypeServiceImpl pricingRuleTypeService;

    PricingRuleType sampleType;

    @BeforeEach
    void setUp() {
        sampleType = PricingRuleType.builder()
                .code("PEAK_SEASON")
                .displayName("Mùa cao điểm")
                .priority((short) 80)
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Tạo loại quy tắc giá thành công")
    void create_Success() {
        PricingRuleTypeCreateRequest request = PricingRuleTypeCreateRequest.builder()
                .code("PEAK_SEASON")
                .displayName("Mùa cao điểm")
                .priority((short) 80)
                .status("ACTIVE")
                .build();

        PricingRuleTypeResponse response = PricingRuleTypeResponse.builder()
                .code("PEAK_SEASON")
                .displayName("Mùa cao điểm")
                .priority((short) 80)
                .status("ACTIVE")
                .build();

        when(pricingRuleTypeRepository.existsByCodeAndIsDeletedFalse("PEAK_SEASON")).thenReturn(false);
        when(pricingRuleTypeMapper.toEntity(request)).thenReturn(sampleType);
        when(pricingRuleTypeRepository.save(any())).thenReturn(sampleType);
        when(pricingRuleTypeMapper.toResponse(sampleType)).thenReturn(response);

        PricingRuleTypeResponse result = pricingRuleTypeService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("PEAK_SEASON");
        verify(pricingRuleTypeRepository).save(sampleType);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi mã loại quy tắc giá đã tồn tại")
    void create_DuplicateCode_ThrowsException() {
        PricingRuleTypeCreateRequest request = PricingRuleTypeCreateRequest.builder()
                .code("PEAK_SEASON")
                .displayName("Mùa cao điểm")
                .priority((short) 80)
                .build();

        when(pricingRuleTypeRepository.existsByCodeAndIsDeletedFalse("PEAK_SEASON")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> pricingRuleTypeService.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        verify(pricingRuleTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật loại quy tắc giá thành công")
    void update_Success() {
        PricingRuleTypeUpdateRequest updateReq = PricingRuleTypeUpdateRequest.builder()
                .displayName("Mùa cao điểm mới")
                .priority((short) 85)
                .status("ACTIVE")
                .build();

        when(pricingRuleTypeRepository.findByCodeAndIsDeletedFalse("PEAK_SEASON")).thenReturn(java.util.Optional.of(sampleType));
        when(pricingRuleTypeRepository.save(sampleType)).thenReturn(sampleType);
        when(pricingRuleTypeMapper.toResponse(sampleType)).thenAnswer(invocation -> {
            PricingRuleType entity = invocation.getArgument(0);
            return PricingRuleTypeResponse.builder()
                    .code(entity.getCode())
                    .displayName(entity.getDisplayName())
                    .priority(entity.getPriority())
                    .status(entity.getStatus())
                    .build();
        });

        PricingRuleTypeResponse updated = pricingRuleTypeService.update("PEAK_SEASON", updateReq);
        assertThat(updated.getDisplayName()).isEqualTo("Mùa cao điểm");
        verify(pricingRuleTypeRepository).save(sampleType);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi xóa loại quy tắc đang được sử dụng trong pricing_rules")
    void delete_InUse_ThrowsException() {
        when(pricingRuleTypeRepository.findByCodeAndIsDeletedFalse("PEAK_SEASON")).thenReturn(java.util.Optional.of(sampleType));
        when(pricingRuleRepository.existsByRuleTypeCodeAndIsDeletedFalse("PEAK_SEASON")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> pricingRuleTypeService.delete("PEAK_SEASON"));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        assertThat(sampleType.getIsDeleted()).isFalse();
    }
}
