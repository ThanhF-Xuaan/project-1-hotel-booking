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
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.repository.TaxCategoryRepository;
import vn.edu.utc.hotel_booking.modules.pricing.service.impl.TaxCategoryServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxCategoryServiceImplTest {

    @Mock
    TaxCategoryRepository taxCategoryRepository;

    @InjectMocks
    TaxCategoryServiceImpl taxCategoryService;

    TaxCategory sampleTaxCategory;

    @BeforeEach
    void setUp() {
        sampleTaxCategory = TaxCategory.builder()
                .id(1)
                .categoryCode("VAT_10")
                .categoryName("Thuế GTGT 10%")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("getEntityById - Thành công khi ID hợp lệ")
    void getEntityById_Success() {
        when(taxCategoryRepository.findById(1)).thenReturn(Optional.of(sampleTaxCategory));

        TaxCategory result = taxCategoryService.getEntityById(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getCategoryCode()).isEqualTo("VAT_10");
        verify(taxCategoryRepository).findById(1);
    }

    @Test
    @DisplayName("getEntityById - Ném ngoại lệ TAX_CATEGORY_NOT_FOUND khi không tìm thấy")
    void getEntityById_NotFound_ThrowsException() {
        when(taxCategoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taxCategoryService.getEntityById(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TAX_CATEGORY_NOT_FOUND);
        verify(taxCategoryRepository).findById(999);
    }

    @Test
    @DisplayName("getEntityById - Ném ngoại lệ INVALID_REQUEST_DATA khi ID là null")
    void getEntityById_NullId_ThrowsException() {
        assertThatThrownBy(() -> taxCategoryService.getEntityById(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
    }
}
