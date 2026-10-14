package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomInclusionDto;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeInclusion;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeInclusionRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeInclusionService;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelRoomTypeInclusionServiceImpl implements HotelRoomTypeInclusionService {

    private final HotelRoomTypeInclusionRepository inclusionRepository;
    private final MenuService menuService;

    @Override
    @Transactional(readOnly = true)
    public List<RoomInclusionDto> getInclusionsByHotelRoomTypeId(Integer hotelRoomTypeId) {
        if (hotelRoomTypeId == null) {
            return Collections.emptyList();
        }

        List<HotelRoomTypeInclusion> inclusions = inclusionRepository.findByHotelRoomTypeIdAndIsDeletedFalse(hotelRoomTypeId);
        if (inclusions.isEmpty()) {
            return Collections.emptyList();
        }

        return inclusions.stream().map(inc -> {
            String name = "Quà tặng dịch vụ kèm phòng #" + inc.getReferenceId();
            String description = "Bao gồm " + inc.getQuantity() + " suất/phần";

            try {
                if (menuService != null && inc.getReferenceId() != null) {
                    MenuResponse menu = menuService.getById(inc.getReferenceId());
                    if (menu != null) {
                        name = menu.getName();
                        if (menu.getDescription() != null && !menu.getDescription().isBlank()) {
                            description = menu.getDescription();
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Không thể lấy thông tin món tặng kèm referenceId={}: {}", inc.getReferenceId(), ex.getMessage());
            }

            return RoomInclusionDto.builder()
                    .id(inc.getId())
                    .referenceType(inc.getReferenceType())
                    .referenceId(inc.getReferenceId())
                    .name(name)
                    .quantity(inc.getQuantity())
                    .description(description)
                    .build();
        }).toList();
    }
}
