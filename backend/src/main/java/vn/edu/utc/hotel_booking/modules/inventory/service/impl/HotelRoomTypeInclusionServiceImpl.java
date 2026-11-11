package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomInclusionDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeInclusionRequest;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeInclusion;
import vn.edu.utc.hotel_booking.modules.inventory.entity.InclusionFrequency;
import vn.edu.utc.hotel_booking.modules.inventory.entity.InclusionTargetType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeInclusionRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeInclusionService;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MasterItemResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.service.MasterItemService;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelRoomTypeInclusionServiceImpl implements HotelRoomTypeInclusionService {

    private final HotelRoomTypeInclusionRepository inclusionRepository;
    private final HotelRoomTypeRepository hotelRoomTypeRepository;
    private final MenuService menuService;
    private final MasterItemService masterItemService;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<RoomInclusionDto> getInclusionsByHotelRoomTypeId(Integer hotelRoomTypeId) {
        if (hotelRoomTypeId == null) {
            return Collections.emptyList();
        }

        List<HotelRoomTypeInclusion> inclusions = inclusionRepository.findActiveWithDetailsByHotelRoomTypeId(hotelRoomTypeId);
        if (inclusions.isEmpty()) {
            return Collections.emptyList();
        }

        return inclusions.stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional
    public RoomInclusionDto createInclusion(Integer hotelRoomTypeId, HotelRoomTypeInclusionRequest request) {
        HotelRoomType roomType = hotelRoomTypeRepository.findById(hotelRoomTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_NOT_FOUND));

        if (request.getMenuId() == null && request.getMasterItemId() == null) {
            throw new AppException(ErrorCode.INVALID_INCLUSION_CONFIGURATION);
        }

        Menu menu = null;
        if (request.getMenuId() != null) {
            MenuResponse menuDto = menuService.getById(request.getMenuId());
            if (menuDto == null) {
                throw new AppException(ErrorCode.MENU_ITEM_NOT_FOUND);
            }
            if (menuDto.getHotelId() != null && !menuDto.getHotelId().equals(roomType.getHotel().getId())) {
                throw new AppException(ErrorCode.INVALID_INCLUSION_CONFIGURATION);
            }
            menu = entityManager.getReference(Menu.class, request.getMenuId());
        }

        MasterItem masterItem = null;
        if (request.getMasterItemId() != null) {
            MasterItemResponse masterItemDto = masterItemService.getById(request.getMasterItemId());
            if (masterItemDto == null) {
                throw new AppException(ErrorCode.MASTER_ITEM_NOT_FOUND);
            }
            if (masterItemDto.getCode() != null && masterItemDto.getCode().startsWith("LOCAL_")) {
                throw new AppException(ErrorCode.INVALID_INCLUSION_CONFIGURATION,
                        "Danh mục đặc sản/dịch vụ địa phương phải được khai báo cụ thể trong Thực đơn cơ sở trước khi thêm vào phòng.");
            }
            Short hotelId = roomType.getHotel().getId();
            if (menuService.isMasterItemOverriddenByHotel(hotelId, request.getMasterItemId())) {
                throw new AppException(ErrorCode.MASTER_ITEM_OVERRIDDEN_BY_HOTEL);
            }
            masterItem = entityManager.getReference(MasterItem.class, request.getMasterItemId());
        }

        HotelRoomTypeInclusion inclusion = HotelRoomTypeInclusion.builder()
                .hotelRoomType(roomType)
                .menu(menu)
                .masterItem(masterItem)
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .frequency(request.getFrequency() != null ? request.getFrequency() : InclusionFrequency.PER_NIGHT)
                .targetType(request.getTargetType() != null ? request.getTargetType() : InclusionTargetType.PER_ROOM)
                .customDisplayName(request.getCustomDisplayName() != null && !request.getCustomDisplayName().isBlank() ? request.getCustomDisplayName().trim() : null)
                .description(request.getDescription() != null && !request.getDescription().isBlank() ? request.getDescription().trim() : null)
                .build();

        inclusion = inclusionRepository.save(inclusion);
        return mapToDto(inclusion);
    }

    @Override
    @Transactional
    public RoomInclusionDto updateInclusion(Long inclusionId, HotelRoomTypeInclusionRequest request) {
        HotelRoomTypeInclusion inclusion = inclusionRepository.findById(inclusionId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_INCLUSION_NOT_FOUND));

        if (request.getMenuId() == null && request.getMasterItemId() == null) {
            throw new AppException(ErrorCode.INVALID_INCLUSION_CONFIGURATION);
        }

        if (request.getMenuId() != null) {
            MenuResponse menuDto = menuService.getById(request.getMenuId());
            if (menuDto == null) {
                throw new AppException(ErrorCode.MENU_ITEM_NOT_FOUND);
            }
            if (menuDto.getHotelId() != null && !menuDto.getHotelId().equals(inclusion.getHotelRoomType().getHotel().getId())) {
                throw new AppException(ErrorCode.INVALID_INCLUSION_CONFIGURATION);
            }
            inclusion.setMenu(entityManager.getReference(Menu.class, request.getMenuId()));
            inclusion.setMasterItem(null);
        } else if (request.getMasterItemId() != null) {
            MasterItemResponse masterItemDto = masterItemService.getById(request.getMasterItemId());
            if (masterItemDto == null) {
                throw new AppException(ErrorCode.MASTER_ITEM_NOT_FOUND);
            }
            if (masterItemDto.getCode() != null && masterItemDto.getCode().startsWith("LOCAL_")) {
                throw new AppException(ErrorCode.INVALID_INCLUSION_CONFIGURATION,
                        "Danh mục đặc sản/dịch vụ địa phương phải được khai báo cụ thể trong Thực đơn cơ sở trước khi thêm vào phòng.");
            }
            Short hotelId = inclusion.getHotelRoomType().getHotel().getId();
            if (menuService.isMasterItemOverriddenByHotel(hotelId, request.getMasterItemId())) {
                throw new AppException(ErrorCode.MASTER_ITEM_OVERRIDDEN_BY_HOTEL);
            }
            inclusion.setMasterItem(entityManager.getReference(MasterItem.class, request.getMasterItemId()));
            inclusion.setMenu(null);
        }

        inclusion.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        inclusion.setFrequency(request.getFrequency() != null ? request.getFrequency() : InclusionFrequency.PER_NIGHT);
        inclusion.setTargetType(request.getTargetType() != null ? request.getTargetType() : InclusionTargetType.PER_ROOM);
        inclusion.setCustomDisplayName(request.getCustomDisplayName() != null && !request.getCustomDisplayName().isBlank() ? request.getCustomDisplayName().trim() : null);
        inclusion.setDescription(request.getDescription() != null && !request.getDescription().isBlank() ? request.getDescription().trim() : null);

        inclusion = inclusionRepository.save(inclusion);
        return mapToDto(inclusion);
    }

    @Override
    @Transactional
    public void deleteInclusion(Long inclusionId) {
        HotelRoomTypeInclusion inclusion = inclusionRepository.findById(inclusionId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_ROOM_TYPE_INCLUSION_NOT_FOUND));
        inclusion.setIsDeleted(true);
        inclusionRepository.save(inclusion);
    }

    public RoomInclusionDto mapToDto(HotelRoomTypeInclusion inc) {
        String name = "Quà tặng dịch vụ kèm phòng #" + (inc.getId() != null ? inc.getId() : "");
        String description = "Bao gồm " + inc.getQuantity() + " suất/phần";
        String unit = null;
        String sourceType = "HOTEL_MENU";
        Integer menuId = null;
        Integer masterItemId = null;

        if (inc.getCustomDisplayName() != null && !inc.getCustomDisplayName().isBlank()) {
            name = inc.getCustomDisplayName();
        }

        if (inc.getMenu() != null) {
            sourceType = "HOTEL_MENU";
            menuId = inc.getMenu().getId();
            if (inc.getCustomDisplayName() == null || inc.getCustomDisplayName().isBlank()) {
                name = (inc.getMenu().getCustomName() != null && !inc.getMenu().getCustomName().isBlank())
                        ? inc.getMenu().getCustomName()
                        : inc.getMenu().getName();
            }
            if (inc.getMenu().getDescription() != null && !inc.getMenu().getDescription().isBlank()) {
                description = inc.getMenu().getDescription();
            }
            if (inc.getMenu().getMasterItem() != null) {
                unit = inc.getMenu().getMasterItem().getUnit();
            }
        } else if (inc.getMasterItem() != null) {
            sourceType = "CHAIN_MASTER";
            masterItemId = inc.getMasterItem().getId();
            if (inc.getCustomDisplayName() == null || inc.getCustomDisplayName().isBlank()) {
                name = inc.getMasterItem().getName();
            }
            if (inc.getMasterItem().getDescription() != null && !inc.getMasterItem().getDescription().isBlank()) {
                description = inc.getMasterItem().getDescription();
            }
            unit = inc.getMasterItem().getUnit();
        } else if (inc.getReferenceId() != null) {
            // Fallback tương thích ngược cho dữ liệu cũ chưa migrate
            menuId = inc.getReferenceId();
            try {
                if (menuService != null) {
                    MenuResponse menuResp = menuService.getById(inc.getReferenceId());
                    if (menuResp != null) {
                        if (inc.getCustomDisplayName() == null || inc.getCustomDisplayName().isBlank()) {
                            name = menuResp.getName();
                        }
                        if (menuResp.getDescription() != null && !menuResp.getDescription().isBlank()) {
                            description = menuResp.getDescription();
                        }
                        unit = menuResp.getUnit();
                    }
                }
            } catch (Exception ex) {
                log.warn("Không thể lấy thông tin món tặng kèm legacy referenceId={}: {}", inc.getReferenceId(), ex.getMessage());
            }
        }

        if (inc.getDescription() != null && !inc.getDescription().isBlank()) {
            description = inc.getDescription();
        }

        return RoomInclusionDto.builder()
                .id(inc.getId())
                .menuId(menuId)
                .masterItemId(masterItemId)
                .sourceType(sourceType)
                .referenceType(inc.getReferenceType())
                .referenceId(inc.getReferenceId() != null ? inc.getReferenceId() : menuId)
                .name(name)
                .unit(unit)
                .quantity(inc.getQuantity())
                .frequency(inc.getFrequency() != null ? inc.getFrequency().name() : InclusionFrequency.PER_NIGHT.name())
                .targetType(inc.getTargetType() != null ? inc.getTargetType().name() : InclusionTargetType.PER_ROOM.name())
                .description(description)
                .build();
    }
}
