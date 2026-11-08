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
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryCreateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategorySearchDto;
import vn.edu.utc.hotel_booking.modules.pricing.dto.request.TaxCategoryUpdateRequest;
import vn.edu.utc.hotel_booking.modules.pricing.dto.response.TaxCategoryResponse;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.mapper.TaxCategoryMapper;
import vn.edu.utc.hotel_booking.modules.pricing.repository.TaxCategoryRepository;
import vn.edu.utc.hotel_booking.modules.pricing.repository.VatRuleRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.TaxCategoryServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxCategoryServiceImplTest {

    @Mock
    TaxCategoryRepository taxCategoryRepository;

    @Mock
    VatRuleRepository vatRuleRepository;

    @Mock
    TaxCategoryMapper taxCategoryMapper;

    @InjectMocks
    TaxCategoryServiceImpl taxCategoryService;

    TaxCategory sampleTaxCategory;
    TaxCategoryResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleTaxCategory = TaxCategory.builder()
                .id(1)
                .categoryCode("ROOM_TAX")
                .categoryName("Thuế dịch vụ lưu trú")
                .status("ACTIVE")
                .build();

        sampleResponse = TaxCategoryResponse.builder()
                .id(1)
                .categoryCode("ROOM_TAX")
                .categoryName("Thuế dịch vụ lưu trú")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("getEntityById - Thành công khi ID hợp lệ")
    void getEntityById_Success() {
        when(taxCategoryRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleTaxCategory));

        TaxCategory result = taxCategoryService.getEntityById(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getCategoryCode()).isEqualTo("ROOM_TAX");
        verify(taxCategoryRepository).findByIdAndIsDeletedFalse(1);
    }

    @Test
    @DisplayName("getEntityById - Ném ngoại lệ TAX_CATEGORY_NOT_FOUND khi không tìm thấy")
    void getEntityById_NotFound_ThrowsException() {
        when(taxCategoryRepository.findByIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxCategoryService.getEntityById(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAX_CATEGORY_NOT_FOUND);
        verify(taxCategoryRepository).findByIdAndIsDeletedFalse(999);
    }

    @Test
    @DisplayName("create - Thành công khi mã chưa tồn tại")
    void create_Success() {
        TaxCategoryCreateRequest request = TaxCategoryCreateRequest.builder()
                .categoryCode("FB_TAX")
                .categoryName("Thuế ăn uống F&B")
                .build();

        when(taxCategoryRepository.existsByCategoryCodeAndIsDeletedFalse("FB_TAX")).thenReturn(false);
        when(taxCategoryMapper.toEntity(request)).thenReturn(sampleTaxCategory);
        when(taxCategoryRepository.save(any(TaxCategory.class))).thenReturn(sampleTaxCategory);
        when(taxCategoryMapper.toResponse(sampleTaxCategory)).thenReturn(sampleResponse);

        TaxCategoryResponse response = taxCategoryService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        verify(taxCategoryRepository).save(any(TaxCategory.class));
    }

    @Test
    @DisplayName("create - Ném lỗi TAX_CATEGORY_CODE_ALREADY_EXISTS khi mã đã tồn tại")
    void create_DuplicateCode_ThrowsException() {
        TaxCategoryCreateRequest request = TaxCategoryCreateRequest.builder()
                .categoryCode("ROOM_TAX")
                .categoryName("Thuế phòng")
                .build();

        when(taxCategoryRepository.existsByCategoryCodeAndIsDeletedFalse("ROOM_TAX")).thenReturn(true);

        assertThatThrownBy(() -> taxCategoryService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAX_CATEGORY_CODE_ALREADY_EXISTS);
        verify(taxCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteBatch - Ném lỗi TAX_CATEGORY_IN_USE khi nhóm thuế có quy tắc VAT liên kết")
    void deleteBatch_InUse_ThrowsException() {
        when(taxCategoryRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleTaxCategory));
        when(vatRuleRepository.existsByTaxCategoryIdAndIsDeletedFalse(1)).thenReturn(true);

        assertThatThrownBy(() -> taxCategoryService.deleteBatch(List.of(1)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAX_CATEGORY_IN_USE);
    }

    @Test
    @DisplayName("deleteBatch - Thành công khi nhóm thuế không có ràng buộc")
    void deleteBatch_Success() {
        when(taxCategoryRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleTaxCategory));
        when(vatRuleRepository.existsByTaxCategoryIdAndIsDeletedFalse(1)).thenReturn(false);

        taxCategoryService.deleteBatch(List.of(1));

        assertThat(sampleTaxCategory.getIsDeleted()).isTrue();
        verify(taxCategoryRepository).save(sampleTaxCategory);
    }

    @Test
    @DisplayName("filter - Phân trang tìm kiếm thành công")
    void filter_Success() {
        TaxCategorySearchDto searchDto = new TaxCategorySearchDto();
        searchDto.setPage(0);
        searchDto.setPageSize(10);
        searchDto.setKeyword("ROOM");

        Page<TaxCategory> page = new PageImpl<>(List.of(sampleTaxCategory));
        when(taxCategoryRepository.filter(eq("ROOM"), any(), any(Pageable.class))).thenReturn(page);
        when(taxCategoryMapper.toResponseList(any())).thenReturn(List.of(sampleResponse));

        PageResponse<TaxCategoryResponse> result = taxCategoryService.filter(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
