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
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.MasterItemService;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.MenuServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.service.TaxCategoryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock MenuRepository menuRepository;
    @Mock MasterItemService masterItemService;
    @Mock HotelService hotelService;
    @Mock TaxCategoryService taxCategoryService;
    @Mock OperationMapper operationMapper;

    @InjectMocks
    MenuServiceImpl menuService;

    private Hotel testHotel;
    private TaxCategory testTaxCategory;
    private Menu testProductMenu;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder().name("Khách sạn Grand Hà Nội").build();
        testHotel.setId((short) 1);

        testTaxCategory = TaxCategory.builder()
                .categoryName("Đồ uống VAT 8%")
                .build();
        testTaxCategory.setId(1);

        testProductMenu = Menu.builder()
                .id(10)
                .hotel(testHotel)
                .taxCategory(testTaxCategory)
                .menuType(MenuType.PRODUCT)
                .name("Bia Heineken lon")
                .basePrice(BigDecimal.valueOf(35000))
                .stockQuantity(100)
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Tạo món ăn/sản phẩm vật lý thành công và lưu số lượng tồn kho trực tiếp trên menu")
    void createProduct_Success() {
        MenuCreateRequest request = MenuCreateRequest.builder()
                .hotelId((short) 1)
                .taxCategoryId(1)
                .menuType(MenuType.PRODUCT)
                .name("Bia Heineken lon")
                .basePrice(BigDecimal.valueOf(35000))
                .stockQuantity(100)
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(taxCategoryService.getEntityById(1)).thenReturn(testTaxCategory);
        when(menuRepository.save(any(Menu.class))).thenReturn(testProductMenu);
        when(operationMapper.toResponse(any(Menu.class))).thenReturn(MenuResponse.builder()
                .id(10)
                .name("Bia Heineken lon")
                .menuType(MenuType.PRODUCT)
                .stockQuantity(100)
                .build());

        MenuResponse response = menuService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getStockQuantity()).isEqualTo(100);
        verify(menuRepository, times(1)).save(any(Menu.class));
    }

    @Test
    @DisplayName("Tạo dịch vụ khách sạn thành công liên kết MasterItem")
    void createService_Success() {
        MasterItem masterLaundry = MasterItem.builder()
                .id(5)
                .code("SRV_LAUNDRY")
                .name("Giặt ủi áo sơ mi")
                .pricingType(ServicePricingType.PER_UNIT)
                .defaultBasePrice(BigDecimal.valueOf(50000))
                .build();

        MenuCreateRequest request = MenuCreateRequest.builder()
                .hotelId((short) 1)
                .masterItemId(5)
                .taxCategoryId(1)
                .menuType(MenuType.SERVICE)
                .name("Giặt ủi áo sơ mi")
                .basePrice(BigDecimal.valueOf(50000))
                .pricingType(ServicePricingType.PER_UNIT)
                .build();

        Menu serviceMenu = Menu.builder()
                .id(11)
                .hotel(testHotel)
                .masterItem(masterLaundry)
                .taxCategory(testTaxCategory)
                .menuType(MenuType.SERVICE)
                .name("Giặt ủi áo sơ mi")
                .basePrice(BigDecimal.valueOf(50000))
                .status("ACTIVE")
                .build();

        when(hotelService.getEntityById((short) 1)).thenReturn(testHotel);
        when(taxCategoryService.getEntityById(1)).thenReturn(testTaxCategory);
        when(masterItemService.getEntityById(5)).thenReturn(masterLaundry);
        when(menuRepository.save(any(Menu.class))).thenReturn(serviceMenu);
        when(operationMapper.toResponse(any(Menu.class))).thenReturn(MenuResponse.builder()
                .id(11)
                .masterItemId(5)
                .name("Giặt ủi áo sơ mi")
                .menuType(MenuType.SERVICE)
                .pricingType(ServicePricingType.PER_UNIT)
                .build());

        MenuResponse response = menuService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(11);
        assertThat(response.getPricingType()).isEqualTo(ServicePricingType.PER_UNIT);
        verify(menuRepository, times(1)).save(any(Menu.class));
    }

    @Test
    @DisplayName("Cập nhật thông tin món và số lượng tồn kho thành công")
    void update_Success() {
        MenuUpdateRequest request = MenuUpdateRequest.builder()
                .name("Bia Heineken lon 330ml")
                .basePrice(BigDecimal.valueOf(40000))
                .stockQuantity(150)
                .build();

        when(menuRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(testProductMenu));
        when(menuRepository.save(any(Menu.class))).thenReturn(testProductMenu);
        when(operationMapper.toResponse(any(Menu.class))).thenReturn(MenuResponse.builder()
                .id(10)
                .name("Bia Heineken lon 330ml")
                .basePrice(BigDecimal.valueOf(40000))
                .stockQuantity(150)
                .build());

        MenuResponse response = menuService.update(10, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Bia Heineken lon 330ml");
        assertThat(testProductMenu.getStockQuantity()).isEqualTo(150);
        verify(menuRepository, times(1)).save(testProductMenu);
    }

    @Test
    @DisplayName("Tìm kiếm và phân trang món/dịch vụ thành công")
    void filter_Success() {
        MenuSearchDto searchDto = MenuSearchDto.builder()
                .hotelId((short) 1)
                .name("Heineken")
                .page(1)
                .pageSize(10)
                .build();

        Page<Menu> page = new PageImpl<>(List.of(testProductMenu));
        when(menuRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(operationMapper.toResponse(any(Menu.class))).thenReturn(MenuResponse.builder()
                .id(10)
                .name("Bia Heineken lon")
                .build());

        PageResponse<MenuResponse> response = menuService.filter(searchDto);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getEntityById - Thành công khi ID hợp lệ")
    void getEntityById_Success() {
        when(menuRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(testProductMenu));

        Menu result = menuService.getEntityById(10);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
        assertThat(result.getName()).isEqualTo("Bia Heineken lon");
        verify(menuRepository).findByIdAndIsDeletedFalse(10);
    }

    @Test
    @DisplayName("deductStock - Trừ tồn kho thành công cho sản phẩm Product")
    void deductStock_Success() {
        when(menuRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(testProductMenu));

        menuService.deductStock(10, 5);

        assertThat(testProductMenu.getStockQuantity()).isEqualTo(95);
        verify(menuRepository).save(testProductMenu);
    }

    @Test
    @DisplayName("deductStock - Báo lỗi OUT_OF_STOCK khi tồn kho không đủ")
    void deductStock_OutOfStock_ThrowsException() {
        when(menuRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(testProductMenu));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> menuService.deductStock(10, 999))
                .isInstanceOf(vn.edu.utc.hotel_booking.common.exception.AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", vn.edu.utc.hotel_booking.common.exception.ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("restoreStock - Hoàn trả tồn kho thành công cho sản phẩm Product")
    void restoreStock_Success() {
        when(menuRepository.findByIdAndIsDeletedFalse(10)).thenReturn(Optional.of(testProductMenu));

        menuService.restoreStock(10, 10);

        assertThat(testProductMenu.getStockQuantity()).isEqualTo(110);
        verify(menuRepository).save(testProductMenu);
    }
}
