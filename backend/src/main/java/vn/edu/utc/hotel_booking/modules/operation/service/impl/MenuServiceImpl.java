package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.dto.PageResponse;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuCreateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuSearchDto;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.MenuUpdateRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.CatalogItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.HotelServiceItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.CatalogItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.HotelServiceItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.repository.HotelRepository;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.repository.TaxCategoryRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {

    MenuRepository menuRepository;
    CatalogItemRepository catalogItemRepository;
    HotelServiceItemRepository hotelServiceItemRepository;
    HotelRepository hotelRepository;
    TaxCategoryRepository taxCategoryRepository;
    OperationMapper operationMapper;

    @Override
    @Transactional
    public MenuResponse create(MenuCreateRequest request) {
        Hotel hotel = null;
        if (request.getHotelId() != null) {
            hotel = hotelRepository.findByIdAndIsDeletedFalse(request.getHotelId())
                    .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND, "Không tìm thấy khách sạn ID: " + request.getHotelId()));
        }

        TaxCategory taxCategory = taxCategoryRepository.findById(request.getTaxCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.TAX_CATEGORY_NOT_FOUND, "Không tìm thấy nhóm thuế ID: " + request.getTaxCategoryId()));

        Menu menu = Menu.builder()
                .hotel(hotel)
                .taxCategory(taxCategory)
                .menuType(request.getMenuType())
                .name(request.getName().trim())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .status("ACTIVE")
                .build();

        Menu savedMenu = menuRepository.save(menu);

        if (request.getMenuType() == MenuType.PRODUCT) {
            CatalogItem catalogItem = CatalogItem.builder()
                    .menu(savedMenu)
                    .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                    .build();
            catalogItemRepository.save(catalogItem);
            savedMenu.setCatalogItem(catalogItem);
        } else if (request.getMenuType() == MenuType.SERVICE) {
            HotelServiceItem serviceItem = HotelServiceItem.builder()
                    .menu(savedMenu)
                    .pricingType(request.getPricingType())
                    .build();
            hotelServiceItemRepository.save(serviceItem);
            savedMenu.setHotelServiceItem(serviceItem);
        }

        return operationMapper.toResponse(savedMenu);
    }

    @Override
    @Transactional
    public MenuResponse update(Integer id, MenuUpdateRequest request) {
        Menu menu = menuRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_ITEM_NOT_FOUND, "Không tìm thấy món ăn/dịch vụ ID: " + id));

        if (request.getTaxCategoryId() != null) {
            TaxCategory taxCategory = taxCategoryRepository.findById(request.getTaxCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.TAX_CATEGORY_NOT_FOUND, "Không tìm thấy nhóm thuế ID: " + request.getTaxCategoryId()));
            menu.setTaxCategory(taxCategory);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            menu.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            menu.setDescription(request.getDescription());
        }
        if (request.getBasePrice() != null) {
            menu.setBasePrice(request.getBasePrice());
        }
        if (request.getStatus() != null) {
            menu.setStatus(request.getStatus());
        }

        if (menu.getMenuType() == MenuType.PRODUCT && request.getStockQuantity() != null) {
            CatalogItem catalogItem = catalogItemRepository.findById(id).orElseGet(() -> CatalogItem.builder().menu(menu).build());
            catalogItem.setStockQuantity(request.getStockQuantity());
            catalogItemRepository.save(catalogItem);
            menu.setCatalogItem(catalogItem);
        } else if (menu.getMenuType() == MenuType.SERVICE && request.getPricingType() != null) {
            HotelServiceItem serviceItem = hotelServiceItemRepository.findById(id).orElseGet(() -> HotelServiceItem.builder().menu(menu).build());
            serviceItem.setPricingType(request.getPricingType());
            hotelServiceItemRepository.save(serviceItem);
            menu.setHotelServiceItem(serviceItem);
        }

        Menu saved = menuRepository.save(menu);
        return operationMapper.toResponse(saved);
    }

    @Override
    public MenuResponse getById(Integer id) {
        Menu menu = menuRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_ITEM_NOT_FOUND, "Không tìm thấy món ăn/dịch vụ ID: " + id));
        return operationMapper.toResponse(menu);
    }

    @Override
    public PageResponse<MenuResponse> filter(MenuSearchDto searchDto) {
        int page = searchDto.getPage() != null && searchDto.getPage() > 0 ? searchDto.getPage() - 1 : 0;
        int size = searchDto.getPageSize() != null && searchDto.getPageSize() > 0 ? searchDto.getPageSize() : 10;

        Specification<Menu> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (searchDto.getHotelId() != null) {
                predicates.add(cb.equal(root.get("hotel").get("id"), searchDto.getHotelId()));
            }
            if (searchDto.getMenuType() != null) {
                predicates.add(cb.equal(root.get("menuType"), searchDto.getMenuType()));
            }
            if (searchDto.getName() != null && !searchDto.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + searchDto.getName().trim().toLowerCase() + "%"));
            }
            if (searchDto.getStatus() != null && !searchDto.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Menu> pageResult = menuRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(pageResult.map(operationMapper::toResponse));
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Menu> menus = menuRepository.findAllById(ids);
        menus.forEach(m -> m.setIsDeleted(true));
        menuRepository.saveAll(menus);
    }
}
