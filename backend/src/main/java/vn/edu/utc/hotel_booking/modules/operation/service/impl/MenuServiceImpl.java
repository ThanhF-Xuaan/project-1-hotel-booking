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
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.mapper.OperationMapper;
import vn.edu.utc.hotel_booking.modules.operation.repository.MenuRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.MasterItemService;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;
import vn.edu.utc.hotel_booking.modules.pricing.entity.TaxCategory;
import vn.edu.utc.hotel_booking.modules.pricing.service.TaxCategoryService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {

    MenuRepository menuRepository;
    MasterItemService masterItemService;
    HotelService hotelService;
    TaxCategoryService taxCategoryService;
    OperationMapper operationMapper;

    @Override
    @Transactional
    public MenuResponse create(MenuCreateRequest request) {
        Hotel hotel = hotelService.getEntityById(request.getHotelId());
        TaxCategory taxCategory = taxCategoryService.getEntityById(request.getTaxCategoryId());
        MasterItem masterItem = masterItemService.getEntityById(request.getMasterItemId());

        Menu menu = Menu.builder()
                .hotel(hotel)
                .masterItem(masterItem)
                .taxCategory(taxCategory)
                .menuType(request.getMenuType())
                .name(request.getName().trim())
                .customName(request.getCustomName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .status("ACTIVE")
                .build();

        Menu savedMenu = menuRepository.save(menu);
        return operationMapper.toResponse(savedMenu);
    }

    @Override
    @Transactional
    public MenuResponse update(Integer id, MenuUpdateRequest request) {
        Menu menu = getEntityById(id);

        if (request.getTaxCategoryId() != null) {
            TaxCategory taxCategory = taxCategoryService.getEntityById(request.getTaxCategoryId());
            menu.setTaxCategory(taxCategory);
        }

        if (request.getMasterItemId() != null) {
            MasterItem masterItem = masterItemService.getEntityById(request.getMasterItemId());
            menu.setMasterItem(masterItem);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            menu.setName(request.getName().trim());
        }
        if (request.getCustomName() != null) {
            menu.setCustomName(request.getCustomName());
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
        if (request.getStockQuantity() != null) {
            menu.setStockQuantity(request.getStockQuantity());
        }

        if (menu.getMenuType() == MenuType.SERVICE && request.getPricingType() != null && menu.getMasterItem() != null) {
            menu.getMasterItem().setPricingType(request.getPricingType());
            masterItemService.saveEntity(menu.getMasterItem());
        }

        Menu saved = menuRepository.save(menu);
        return operationMapper.toResponse(saved);
    }

    @Override
    public MenuResponse getById(Integer id) {
        Menu menu = getEntityById(id);
        return operationMapper.toResponse(menu);
    }

    @Override
    public Menu getEntityById(Integer id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "ID món ăn/dịch vụ không được để trống");
        }
        return menuRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_ITEM_NOT_FOUND, "Không tìm thấy món ăn/dịch vụ ID: " + id));
    }

    @Override
    @Transactional
    public void deductStock(Integer menuId, int quantity) {
        Menu menu = getEntityById(menuId);
        if (menu.getMenuType() == MenuType.PRODUCT) {
            int currentStock = menu.getStockQuantity() != null ? menu.getStockQuantity() : 0;
            if (currentStock < quantity) {
                throw new AppException(ErrorCode.OUT_OF_STOCK, "Sản phẩm " + menu.getName() + " không đủ tồn kho (còn " + currentStock + ")");
            }
            menu.setStockQuantity(currentStock - quantity);
            menuRepository.save(menu);
        }
    }

    @Override
    @Transactional
    public void restoreStock(Integer menuId, int quantity) {
        Menu menu = getEntityById(menuId);
        if (menu.getMenuType() == MenuType.PRODUCT) {
            int currentStock = menu.getStockQuantity() != null ? menu.getStockQuantity() : 0;
            menu.setStockQuantity(currentStock + quantity);
            menuRepository.save(menu);
        }
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
