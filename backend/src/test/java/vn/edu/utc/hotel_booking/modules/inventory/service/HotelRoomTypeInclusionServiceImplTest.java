package vn.edu.utc.hotel_booking.modules.inventory.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomInclusionDto;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeInclusion;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeInclusionRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.HotelRoomTypeInclusionServiceImpl;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotelRoomTypeInclusionServiceImplTest {

    @Mock
    private HotelRoomTypeInclusionRepository inclusionRepository;

    @Mock
    private MenuService menuService;

    @InjectMocks
    private HotelRoomTypeInclusionServiceImpl inclusionService;

    @Test
    @DisplayName("Lấy danh sách quà tặng kèm thành công và map tên qua MenuService")
    void getInclusionsByHotelRoomTypeId_Success_MapsMenuName() {
        HotelRoomTypeInclusion inc = HotelRoomTypeInclusion.builder()
                .id(1L)
                .referenceType("POS_PRODUCT")
                .referenceId(5)
                .quantity(2)
                .build();

        when(inclusionRepository.findByHotelRoomTypeIdAndIsDeletedFalse(101)).thenReturn(List.of(inc));

        MenuResponse menu = MenuResponse.builder()
                .id(5)
                .name("Nước Suối Lavie 500ml")
                .description("Nước uống miễn phí mỗi ngày")
                .build();
        when(menuService.getById(5)).thenReturn(menu);

        List<RoomInclusionDto> result = inclusionService.getInclusionsByHotelRoomTypeId(101);

        assertThat(result).hasSize(1);
        RoomInclusionDto item = result.get(0);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getReferenceId()).isEqualTo(5);
        assertThat(item.getName()).isEqualTo("Nước Suối Lavie 500ml");
        assertThat(item.getDescription()).isEqualTo("Nước uống miễn phí mỗi ngày");
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Fallback an toàn nếu MenuService ném exception")
    void getInclusionsByHotelRoomTypeId_MenuServiceException_FallbackGracefully() {
        HotelRoomTypeInclusion inc = HotelRoomTypeInclusion.builder()
                .id(2L)
                .referenceType("SERVICE")
                .referenceId(99)
                .quantity(1)
                .build();

        when(inclusionRepository.findByHotelRoomTypeIdAndIsDeletedFalse(101)).thenReturn(List.of(inc));
        when(menuService.getById(99)).thenThrow(new RuntimeException("Menu not found"));

        List<RoomInclusionDto> result = inclusionService.getInclusionsByHotelRoomTypeId(101);

        assertThat(result).hasSize(1);
        RoomInclusionDto item = result.get(0);
        assertThat(item.getName()).contains("#99");
    }
}
