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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.DiscountRuleTypeUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.DiscountRuleTypeResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.DiscountRuleType;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.DiscountRuleTypeMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.DiscountRuleTypeRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.DiscountRuleTypeServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountRuleTypeServiceImplTest {

    @Mock
    DiscountRuleTypeRepository discountRuleTypeRepository;

    @Mock
    DiscountRuleRepository discountRuleRepository;

    @Mock
    DiscountRuleTypeMapper discountRuleTypeMapper;

    @InjectMocks
    DiscountRuleTypeServiceImpl discountRuleTypeService;

    DiscountRuleType sampleType;

    @BeforeEach
    void setUp() {
        sampleType = DiscountRuleType.builder()
                .code("SPECIAL_CAMPAIGN")
                .displayName("Chiến dịch đặc biệt (Flash Sale)")
                .priority((short) 90)
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Tạo loại quy tắc giảm giá thành công")
    void create_Success() {
        DiscountRuleTypeCreateRequest request = DiscountRuleTypeCreateRequest.builder()
                .code("SPECIAL_CAMPAIGN")
                .displayName("Chiến dịch đặc biệt (Flash Sale)")
                .description("  Ưu đãi giờ vàng  ")
                .priority((short) 90)
                .status("ACTIVE")
                .build();

        DiscountRuleTypeResponse response = DiscountRuleTypeResponse.builder()
                .code("SPECIAL_CAMPAIGN")
                .displayName("Chiến dịch đặc biệt (Flash Sale)")
                .description("Ưu đãi giờ vàng")
                .priority((short) 90)
                .status("ACTIVE")
                .build();

        when(discountRuleTypeRepository.existsByCodeAndIsDeletedFalse("SPECIAL_CAMPAIGN")).thenReturn(false);
        when(discountRuleTypeMapper.toEntity(request)).thenReturn(sampleType);
        when(discountRuleTypeRepository.save(any())).thenReturn(sampleType);
        when(discountRuleTypeMapper.toResponse(sampleType)).thenReturn(response);

        DiscountRuleTypeResponse result = discountRuleTypeService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("SPECIAL_CAMPAIGN");
        assertThat(sampleType.getDescription()).isEqualTo("Ưu đãi giờ vàng");
        assertThat(result.getDescription()).isEqualTo("Ưu đãi giờ vàng");
        verify(discountRuleTypeRepository).save(sampleType);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi mã loại quy tắc giảm giá đã tồn tại")
    void create_DuplicateCode_ThrowsException() {
        DiscountRuleTypeCreateRequest request = DiscountRuleTypeCreateRequest.builder()
                .code("SPECIAL_CAMPAIGN")
                .displayName("Chiến dịch đặc biệt (Flash Sale)")
                .priority((short) 90)
                .build();

        when(discountRuleTypeRepository.existsByCodeAndIsDeletedFalse("SPECIAL_CAMPAIGN")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> discountRuleTypeService.create(request));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
        verify(discountRuleTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật và xóa trắng mô tả loại quy tắc giảm giá thành công")
    void update_UpdatesAndClearsDescription() {
        sampleType.setDescription("Mô tả cũ");
        DiscountRuleTypeUpdateRequest updateWithDesc = DiscountRuleTypeUpdateRequest.builder()
                .displayName("Chiến dịch mới")
                .description("  Mô tả khuyến mãi mới  ")
                .priority((short) 95)
                .status("ACTIVE")
                .build();

        when(discountRuleTypeRepository.findByCodeAndIsDeletedFalse("SPECIAL_CAMPAIGN")).thenReturn(java.util.Optional.of(sampleType));
        when(discountRuleTypeRepository.save(sampleType)).thenReturn(sampleType);
        when(discountRuleTypeMapper.toResponse(sampleType)).thenAnswer(invocation -> {
            DiscountRuleType entity = invocation.getArgument(0);
            return DiscountRuleTypeResponse.builder()
                    .code(entity.getCode())
                    .displayName(entity.getDisplayName())
                    .description(entity.getDescription())
                    .priority(entity.getPriority())
                    .status(entity.getStatus())
                    .build();
        });

        DiscountRuleTypeResponse updated = discountRuleTypeService.update("SPECIAL_CAMPAIGN", updateWithDesc);
        assertThat(updated.getDescription()).isEqualTo("Mô tả khuyến mãi mới");

        DiscountRuleTypeUpdateRequest clearDesc = DiscountRuleTypeUpdateRequest.builder()
                .displayName("Chiến dịch mới")
                .description("   ")
                .priority((short) 95)
                .status("ACTIVE")
                .build();

        DiscountRuleTypeResponse cleared = discountRuleTypeService.update("SPECIAL_CAMPAIGN", clearDesc);
        assertThat(cleared.getDescription()).isNull();
    }

    @Test
    @DisplayName("Ném ngoại lệ khi xóa loại quy tắc đang được sử dụng trong discount_rules")
    void delete_InUse_ThrowsException() {
        when(discountRuleTypeRepository.findByCodeAndIsDeletedFalse("SPECIAL_CAMPAIGN")).thenReturn(java.util.Optional.of(sampleType));
        when(discountRuleRepository.existsByRuleTypeCodeAndIsDeletedFalse("SPECIAL_CAMPAIGN")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> discountRuleTypeService.delete("SPECIAL_CAMPAIGN"));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
    }
}
