package vn.edu.utc.hotel_booking.modules.inventory.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.RoomInclusionDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HotelRoomTypeInclusionRequest;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomTypeInclusion;
import vn.edu.utc.hotel_booking.modules.inventory.entity.InclusionFrequency;
import vn.edu.utc.hotel_booking.modules.inventory.entity.InclusionTargetType;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeInclusionRepository;
import vn.edu.utc.hotel_booking.modules.inventory.repository.HotelRoomTypeRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.impl.HotelRoomTypeInclusionServiceImpl;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MasterItemResponse;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.MenuResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.Menu;
import vn.edu.utc.hotel_booking.modules.operation.service.MasterItemService;
import vn.edu.utc.hotel_booking.modules.operation.service.MenuService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelRoomTypeInclusionServiceImplTest {

    @Mock
    private HotelRoomTypeInclusionRepository inclusionRepository;

    @Mock
    private HotelRoomTypeRepository hotelRoomTypeRepository;

    @Mock
    private MenuService menuService;

    @Mock
    private MasterItemService masterItemService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private HotelRoomTypeInclusionServiceImpl inclusionService;

    @Test
    @DisplayName("Lấy danh sách quà tặng kèm thành công khi gán theo Menu cơ sở")
    void getInclusionsByHotelRoomTypeId_Success_MapsMenu() {
        MasterItem mi = MasterItem.builder().id(1).code("WATER").name("Nước Suối").unit("CHAI").build();
        Menu menu = Menu.builder().id(5).name("Nước Suối Lavie 500ml").customName("Lavie ướp lạnh").masterItem(mi).build();

        HotelRoomTypeInclusion inc = HotelRoomTypeInclusion.builder()
                .id(1L)
                .menu(menu)
                .quantity(2)
                .frequency(InclusionFrequency.PER_NIGHT)
                .targetType(InclusionTargetType.PER_ROOM)
                .build();

        when(inclusionRepository.findActiveWithDetailsByHotelRoomTypeId(101)).thenReturn(List.of(inc));

        List<RoomInclusionDto> result = inclusionService.getInclusionsByHotelRoomTypeId(101);

        assertThat(result).hasSize(1);
        RoomInclusionDto item = result.get(0);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getMenuId()).isEqualTo(5);
        assertThat(item.getSourceType()).isEqualTo("HOTEL_MENU");
        assertThat(item.getName()).isEqualTo("Lavie ướp lạnh");
        assertThat(item.getUnit()).isEqualTo("CHAI");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getFrequency()).isEqualTo("PER_NIGHT");
    }

    @Test
    @DisplayName("Lấy danh sách quà tặng kèm thành công khi gán theo MasterItem chuỗi")
    void getInclusionsByHotelRoomTypeId_Success_MapsMasterItem() {
        MasterItem mi = MasterItem.builder()
                .id(2)
                .code("BUFFET")
                .name("Buffet Sáng Tiêu Chuẩn Chuỗi")
                .description("Miễn phí buffet sáng mỗi ngày")
                .unit("PHAN")
                .build();

        HotelRoomTypeInclusion inc = HotelRoomTypeInclusion.builder()
                .id(2L)
                .masterItem(mi)
                .quantity(1)
                .frequency(InclusionFrequency.PER_NIGHT)
                .targetType(InclusionTargetType.PER_ADULT)
                .build();

        when(inclusionRepository.findActiveWithDetailsByHotelRoomTypeId(101)).thenReturn(List.of(inc));

        List<RoomInclusionDto> result = inclusionService.getInclusionsByHotelRoomTypeId(101);

        assertThat(result).hasSize(1);
        RoomInclusionDto item = result.get(0);
        assertThat(item.getMasterItemId()).isEqualTo(2);
        assertThat(item.getSourceType()).isEqualTo("CHAIN_MASTER");
        assertThat(item.getName()).isEqualTo("Buffet Sáng Tiêu Chuẩn Chuỗi");
        assertThat(item.getUnit()).isEqualTo("PHẦN");
        assertThat(item.getTargetType()).isEqualTo("PER_ADULT");
    }

    @Test
    @DisplayName("Ưu tiên tên tùy biến tiếp thị customDisplayName nếu được cấu hình")
    void getInclusionsByHotelRoomTypeId_Success_UsesCustomDisplayName() {
        MasterItem mi = MasterItem.builder().id(2).name("Buffet").build();
        HotelRoomTypeInclusion inc = HotelRoomTypeInclusion.builder()
                .id(3L)
                .masterItem(mi)
                .customDisplayName("Miễn phí Buffet sáng hàng ngày cho 02 người lớn")
                .quantity(2)
                .build();

        when(inclusionRepository.findActiveWithDetailsByHotelRoomTypeId(101)).thenReturn(List.of(inc));

        List<RoomInclusionDto> result = inclusionService.getInclusionsByHotelRoomTypeId(101);

        assertThat(result.get(0).getName()).isEqualTo("Miễn phí Buffet sáng hàng ngày cho 02 người lớn");
    }

    @Test
    @DisplayName("Tạo quà tặng kèm phòng thành công với Menu cơ sở")
    void createInclusion_Success_WithMenu() {
        Hotel hotel = Hotel.builder().id((short) 1).name("Khách sạn Hà Nội").build();
        HotelRoomType roomType = HotelRoomType.builder().id(101).hotel(hotel).build();
        when(hotelRoomTypeRepository.findById(101)).thenReturn(Optional.of(roomType));

        MenuResponse menuDto = MenuResponse.builder().id(5).hotelId((short) 1).name("Rượu vang").build();
        when(menuService.getById(5)).thenReturn(menuDto);

        Menu proxyMenu = Menu.builder().id(5).name("Rượu vang").build();
        when(entityManager.getReference(Menu.class, 5)).thenReturn(proxyMenu);

        when(inclusionRepository.save(any(HotelRoomTypeInclusion.class))).thenAnswer(inv -> {
            HotelRoomTypeInclusion saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        HotelRoomTypeInclusionRequest req = HotelRoomTypeInclusionRequest.builder()
                .menuId(5)
                .quantity(1)
                .frequency(InclusionFrequency.PER_STAY)
                .targetType(InclusionTargetType.PER_ROOM)
                .build();

        RoomInclusionDto result = inclusionService.createInclusion(101, req);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getMenuId()).isEqualTo(5);
        assertThat(result.getFrequency()).isEqualTo("PER_STAY");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo inclusion mà không chọn cả menu lẫn masterItem")
    void createInclusion_ThrowsException_WhenNoItemProvided() {
        Hotel hotel = Hotel.builder().id((short) 1).build();
        HotelRoomType roomType = HotelRoomType.builder().id(101).hotel(hotel).build();
        when(hotelRoomTypeRepository.findById(101)).thenReturn(Optional.of(roomType));

        HotelRoomTypeInclusionRequest req = HotelRoomTypeInclusionRequest.builder().build();

        assertThatThrownBy(() -> inclusionService.createInclusion(101, req))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo inclusion với MasterItem đã bị khách sạn cơ sở override trong Menu")
    void createInclusion_ThrowsException_WhenMasterItemOverriddenByHotel() {
        Hotel hotel = Hotel.builder().id((short) 1).build();
        HotelRoomType roomType = HotelRoomType.builder().id(101).hotel(hotel).build();
        when(hotelRoomTypeRepository.findById(101)).thenReturn(Optional.of(roomType));

        MasterItemResponse miDto = MasterItemResponse.builder().id(2).code("BUFFET").name("Buffet Sáng").build();
        when(masterItemService.getById(2)).thenReturn(miDto);
        when(menuService.isMasterItemOverriddenByHotel((short) 1, 2, "Buffet Sáng")).thenReturn(true);

        HotelRoomTypeInclusionRequest req = HotelRoomTypeInclusionRequest.builder()
                .masterItemId(2)
                .quantity(1)
                .build();

        assertThatThrownBy(() -> inclusionService.createInclusion(101, req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("đã được tùy biến");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo inclusion trực tiếp với MasterItem nhóm đặc thù địa phương LOCAL_*")
    void createInclusion_ThrowsException_WhenMasterItemIsLocalCategory() {
        Hotel hotel = Hotel.builder().id((short) 1).build();
        HotelRoomType roomType = HotelRoomType.builder().id(101).hotel(hotel).build();
        when(hotelRoomTypeRepository.findById(101)).thenReturn(Optional.of(roomType));

        MasterItemResponse miDto = MasterItemResponse.builder()
                .id(9)
                .code("LOCAL_FOOD_SPECIALTY")
                .name("Đặc sản ẩm thực địa phương")
                .build();
        when(masterItemService.getById(9)).thenReturn(miDto);

        HotelRoomTypeInclusionRequest req = HotelRoomTypeInclusionRequest.builder()
                .masterItemId(9)
                .quantity(1)
                .build();

        assertThatThrownBy(() -> inclusionService.createInclusion(101, req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Thực đơn cơ sở");
    }

    @Test
    @DisplayName("Tạo quà tặng kèm phòng thành công với MasterItem chuỗi khi chưa bị override")
    void createInclusion_Success_WithMasterItem_WhenNotOverridden() {
        Hotel hotel = Hotel.builder().id((short) 1).build();
        HotelRoomType roomType = HotelRoomType.builder().id(101).hotel(hotel).build();
        when(hotelRoomTypeRepository.findById(101)).thenReturn(Optional.of(roomType));

        MasterItemResponse miDto = MasterItemResponse.builder().id(3).code("AIRPORT_SHUTTLE").name("Đưa đón sân bay").build();
        when(masterItemService.getById(3)).thenReturn(miDto);
        when(menuService.isMasterItemOverriddenByHotel((short) 1, 3, "Đưa đón sân bay")).thenReturn(false);

        MasterItem proxyItem = MasterItem.builder().id(3).name("Đưa đón sân bay").build();
        when(entityManager.getReference(MasterItem.class, 3)).thenReturn(proxyItem);

        when(inclusionRepository.save(any(HotelRoomTypeInclusion.class))).thenAnswer(inv -> {
            HotelRoomTypeInclusion saved = inv.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        HotelRoomTypeInclusionRequest req = HotelRoomTypeInclusionRequest.builder()
                .masterItemId(3)
                .quantity(1)
                .frequency(InclusionFrequency.PER_STAY)
                .targetType(InclusionTargetType.PER_ROOM)
                .build();

        RoomInclusionDto result = inclusionService.createInclusion(101, req);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getMasterItemId()).isEqualTo(3);
        assertThat(result.getSourceType()).isEqualTo("CHAIN_MASTER");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo inclusion trùng lặp Menu trên cùng hạng phòng")
    void createInclusion_ThrowsException_WhenMenuAlreadyExistsOnRoomType() {
        Hotel hotel = Hotel.builder().id((short) 1).build();
        HotelRoomType roomType = HotelRoomType.builder().id(101).hotel(hotel).build();
        when(hotelRoomTypeRepository.findById(101)).thenReturn(Optional.of(roomType));
        when(inclusionRepository.existsByRoomTypeAndMenu(101, 5, null)).thenReturn(true);

        HotelRoomTypeInclusionRequest req = HotelRoomTypeInclusionRequest.builder()
                .menuId(5)
                .quantity(1)
                .build();

        assertThatThrownBy(() -> inclusionService.createInclusion(101, req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo inclusion trùng lặp MasterItem trên cùng hạng phòng")
    void createInclusion_ThrowsException_WhenMasterItemAlreadyExistsOnRoomType() {
        Hotel hotel = Hotel.builder().id((short) 1).build();
        HotelRoomType roomType = HotelRoomType.builder().id(101).hotel(hotel).build();
        when(hotelRoomTypeRepository.findById(101)).thenReturn(Optional.of(roomType));
        when(inclusionRepository.existsByRoomTypeAndMasterItem(101, 3, null)).thenReturn(true);

        HotelRoomTypeInclusionRequest req = HotelRoomTypeInclusionRequest.builder()
                .masterItemId(3)
                .quantity(1)
                .build();

        assertThatThrownBy(() -> inclusionService.createInclusion(101, req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("Xóa mềm quà tặng kèm phòng thành công")
    void deleteInclusion_Success() {
        HotelRoomTypeInclusion inc = HotelRoomTypeInclusion.builder().id(15L).build();
        when(inclusionRepository.findById(15L)).thenReturn(Optional.of(inc));

        inclusionService.deleteInclusion(15L);

        assertThat(inc.getIsDeleted()).isTrue();
        verify(inclusionRepository).save(inc);
    }

    @Test
    @DisplayName("Chuẩn hóa đơn vị tính formatUnit trả về tiếng Việt in hoa có dấu chuẩn xác")
    void formatUnit_StandardizesToUppercaseVietnamese() {
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit(null)).isEqualTo("PHẦN");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("")).isEqualTo("PHẦN");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("PHAN")).isEqualTo("PHẦN");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("chai")).isEqualTo("CHAI");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("can")).isEqualTo("LON");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("kg")).isEqualTo("KG");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("luot")).isEqualTo("LƯỢT");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("chuyen")).isEqualTo("CHUYẾN");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("ngay")).isEqualTo("NGÀY");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("goi")).isEqualTo("GÓI");
        assertThat(HotelRoomTypeInclusionServiceImpl.formatUnit("hop")).isEqualTo("HỘP");
    }
}
