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
import vn.edu.utc.hotel_booking.modules.operation.entity.CatalogItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.HotelServiceItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.CatalogItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.HotelServiceItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.MenuServiceImpl;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.repository.TaxCategoryRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock MenuRepository menuRepository;
    @Mock CatalogItemRepository catalogItemRepository;
    @Mock HotelServiceItemRepository hotelServiceItemRepository;
    @Mock HotelRepository hotelRepository;
    @Mock TaxCategoryRepository taxCategoryRepository;
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
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Tạo món ăn/sản phẩm vật lý thành công và lưu số lượng tồn kho")
    void createProduct_Success() {
        MenuCreateRequest request = MenuCreateRequest.builder()
                .hotelId((short) 1)
                .taxCategoryId(1)
                .menuType(MenuType.PRODUCT)
                .name("Bia Heineken lon")
                .basePrice(BigDecimal.valueOf(35000))
                .stockQuantity(100)
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(testHotel));
        when(taxCategoryRepository.findById(1)).thenReturn(Optional.of(testTaxCategory));
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
        verify(catalogItemRepository, times(1)).save(any(CatalogItem.class));
    }

    @Test
    @DisplayName("Tạo dịch vụ khách sạn thành công và lưu hình thức tính giá")
    void createService_Success() {
        MenuCreateRequest request = MenuCreateRequest.builder()
                .hotelId((short) 1)
                .taxCategoryId(1)
                .menuType(MenuType.SERVICE)
                .name("Giặt ủi áo sơ mi")
                .basePrice(BigDecimal.valueOf(50000))
                .pricingType(ServicePricingType.PER_UNIT)
                .build();

        Menu serviceMenu = Menu.builder()
                .id(11)
                .hotel(testHotel)
                .taxCategory(testTaxCategory)
                .menuType(MenuType.SERVICE)
                .name("Giặt ủi áo sơ mi")
                .basePrice(BigDecimal.valueOf(50000))
                .status("ACTIVE")
                .build();

        when(hotelRepository.findByIdAndIsDeletedFalse((short) 1)).thenReturn(Optional.of(testHotel));
        when(taxCategoryRepository.findById(1)).thenReturn(Optional.of(testTaxCategory));
        when(menuRepository.save(any(Menu.class))).thenReturn(serviceMenu);
        when(operationMapper.toResponse(any(Menu.class))).thenReturn(MenuResponse.builder()
                .id(11)
                .name("Giặt ủi áo sơ mi")
                .menuType(MenuType.SERVICE)
                .pricingType(ServicePricingType.PER_UNIT)
                .build());

        MenuResponse response = menuService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(11);
        assertThat(response.getPricingType()).isEqualTo(ServicePricingType.PER_UNIT);
        verify(hotelServiceItemRepository, times(1)).save(any(HotelServiceItem.class));
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
        when(catalogItemRepository.findById(10)).thenReturn(Optional.of(CatalogItem.builder().menu(testProductMenu).stockQuantity(100).build()));
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
        verify(catalogItemRepository, times(1)).save(any(CatalogItem.class));
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
}
