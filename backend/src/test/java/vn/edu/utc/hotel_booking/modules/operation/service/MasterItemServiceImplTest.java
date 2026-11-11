package vn.edu.utc.hotel_booking.modules.operation.service;

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
import org.springframework.data.jpa.domain.Specification;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MasterItemUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MasterItemResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.MasterItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.MasterItemServiceImpl;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.service.TaxCategoryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterItemServiceImplTest {

    @Mock
    MasterItemRepository masterItemRepository;

    @Mock
    TaxCategoryService taxCategoryService;

    @Mock
    OperationMapper operationMapper;

    @InjectMocks
    MasterItemServiceImpl masterItemService;

    MasterItem sampleMasterItem;
    TaxCategory sampleTaxCategory;
    MasterItemResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleTaxCategory = TaxCategory.builder()
                .categoryName("VAT 8%")
                .build();
        sampleTaxCategory.setId(1);

        sampleMasterItem = MasterItem.builder()
                .id(10)
                .code("SPA_VIP")
                .name("Spa VIP Massage")
                .itemType(MenuType.SERVICE)
                .defaultBasePrice(new BigDecimal("300000"))
                .taxCategory(sampleTaxCategory)
                .pricingType(ServicePricingType.PER_USE)
                .unit("Lượt")
                .status("ACTIVE")
                .build();

        sampleResponse = MasterItemResponse.builder()
                .id(10)
                .code("SPA_VIP")
                .name("Spa VIP Massage")
                .itemType(MenuType.SERVICE)
                .defaultBasePrice(new BigDecimal("300000"))
                .taxCategoryId(1)
                .taxCategoryName("VAT 8%")
                .pricingType(ServicePricingType.PER_USE)
                .unit("Lượt")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("getEntityById - Thành công khi ID hợp lệ")
    void getEntityById_Success() {
        when(masterItemRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(sampleMasterItem));

        MasterItem result = masterItemService.getEntityById(10);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
        assertThat(result.getCode()).isEqualTo("SPA_VIP");
        verify(masterItemRepository).findByIdAndIsDeletedFalse(10);
    }

    @Test
    @DisplayName("getEntityById - Trả về null khi ID là null")
    void getEntityById_NullId_ReturnsNull() {
        MasterItem result = masterItemService.getEntityById(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getEntityById - Ném ngoại lệ MASTER_ITEM_NOT_FOUND khi không tìm thấy")
    void getEntityById_NotFound_ThrowsException() {
        when(masterItemRepository.findByIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> masterItemService.getEntityById(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MASTER_ITEM_NOT_FOUND);
        verify(masterItemRepository).findByIdAndIsDeletedFalse(999);
    }

    @Test
    @DisplayName("create - Thành công tạo mới MasterItem và gọi qua TaxCategoryService")
    void create_Success() {
        MasterItemCreateRequest request = MasterItemCreateRequest.builder()
                .code("BEER_HN")
                .name("Bia Hà Nội")
                .itemType(MenuType.PRODUCT)
                .defaultBasePrice(new BigDecimal("25000"))
                .taxCategoryId(1)
                .unit("Lon")
                .status("ACTIVE")
                .build();

        when(masterItemRepository.existsByCodeAndIsDeletedFalse("BEER_HN")).thenReturn(false);
        when(taxCategoryService.getEntityById(1)).thenReturn(sampleTaxCategory);
        when(masterItemRepository.save(any(MasterItem.class))).thenAnswer(inv -> {
            MasterItem item = inv.getArgument(0);
            item.setId(20);
            return item;
        });
        when(operationMapper.toResponse(any(MasterItem.class))).thenReturn(sampleResponse);

        MasterItemResponse response = masterItemService.create(request);

        assertThat(response).isNotNull();
        verify(masterItemRepository).existsByCodeAndIsDeletedFalse("BEER_HN");
        verify(taxCategoryService).getEntityById(1);
        verify(masterItemRepository).save(any(MasterItem.class));
    }

    @Test
    @DisplayName("create - Ném ngoại lệ khi trùng mã code")
    void create_DuplicateCode_ThrowsException() {
        MasterItemCreateRequest request = MasterItemCreateRequest.builder()
                .code("SPA_VIP")
                .name("Spa VIP Massage")
                .itemType(MenuType.SERVICE)
                .defaultBasePrice(new BigDecimal("300000"))
                .taxCategoryId(1)
                .build();

        when(masterItemRepository.existsByCodeAndIsDeletedFalse("SPA_VIP")).thenReturn(true);

        assertThatThrownBy(() -> masterItemService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MASTER_ITEM_CODE_ALREADY_EXISTS);

        verify(masterItemRepository, never()).save(any(MasterItem.class));
    }

    @Test
    @DisplayName("update - Thành công cập nhật thông tin MasterItem")
    void update_Success() {
        MasterItemUpdateRequest request = MasterItemUpdateRequest.builder()
                .name("Spa VIP Cập nhật")
                .defaultBasePrice(new BigDecimal("350000"))
                .build();

        when(masterItemRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(sampleMasterItem));
        when(masterItemRepository.save(any(MasterItem.class))).thenReturn(sampleMasterItem);
        when(operationMapper.toResponse(any(MasterItem.class))).thenReturn(sampleResponse);

        MasterItemResponse response = masterItemService.update(10, request);

        assertThat(response).isNotNull();
        verify(masterItemRepository).save(sampleMasterItem);
    }

    @Test
    @DisplayName("filter - Phân trang và tìm kiếm thành công")
    void filter_Success() {
        MasterItemSearchDto searchDto = MasterItemSearchDto.builder()
                .name("Spa")
                .itemType(MenuType.SERVICE)
                .page(0)
                .pageSize(10)
                .build();

        Page<MasterItem> page = new PageImpl<>(List.of(sampleMasterItem));
        when(masterItemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(operationMapper.toResponse(sampleMasterItem)).thenReturn(sampleResponse);

        PageResponse<MasterItemResponse> result = masterItemService.filter(searchDto);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("deleteBatch - Soft delete các bản ghi được chọn")
    void deleteBatch_Success() {
        when(masterItemRepository.findAllById(List.of(10))).thenReturn(List.of(sampleMasterItem));

        masterItemService.deleteBatch(List.of(10));

        assertThat(sampleMasterItem.getIsDeleted()).isTrue();
        verify(masterItemRepository).saveAll(List.of(sampleMasterItem));
    }

    @Test
    @DisplayName("getAllActive - Lấy danh sách MasterItem hoạt động")
    void getAllActive_Success() {
        when(masterItemRepository.findAllByStatusAndIsDeletedFalse("ACTIVE")).thenReturn(List.of(sampleMasterItem));
        when(operationMapper.toResponse(sampleMasterItem)).thenReturn(sampleResponse);

        List<MasterItemResponse> activeItems = masterItemService.getAllActive();

        assertThat(activeItems).hasSize(1);
    }
}
