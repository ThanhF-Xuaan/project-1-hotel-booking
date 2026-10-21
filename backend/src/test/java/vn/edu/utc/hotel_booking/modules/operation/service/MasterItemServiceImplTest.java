package vn.edu.utc.hotel_booking.modules.operation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.operation.entity.MasterItem;
import vn.edu.utc.hotel_booking.modules.operation.entity.MenuType;
import vn.edu.utc.hotel_booking.modules.operation.entity.ServicePricingType;
import vn.edu.utc.hotel_booking.modules.operation.repository.MasterItemRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.impl.MasterItemServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterItemServiceImplTest {

    @Mock
    MasterItemRepository masterItemRepository;

    @InjectMocks
    MasterItemServiceImpl masterItemService;

    MasterItem sampleMasterItem;

    @BeforeEach
    void setUp() {
        sampleMasterItem = MasterItem.builder()
                .id(10)
                .code("SPA_VIP")
                .name("Spa VIP Massage")
                .itemType(MenuType.SERVICE)
                .defaultBasePrice(new BigDecimal("300000"))
                .pricingType(ServicePricingType.PER_USE)
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
    @DisplayName("getEntityById - Ném ngoại lệ INVALID_REQUEST_DATA khi không tìm thấy")
    void getEntityById_NotFound_ThrowsException() {
        when(masterItemRepository.findByIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> masterItemService.getEntityById(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
        verify(masterItemRepository).findByIdAndIsDeletedFalse(999);
    }

    @Test
    @DisplayName("saveEntity - Thành công lưu bản ghi")
    void saveEntity_Success() {
        when(masterItemRepository.save(sampleMasterItem)).thenReturn(sampleMasterItem);

        MasterItem result = masterItemService.saveEntity(sampleMasterItem);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("SPA_VIP");
        verify(masterItemRepository).save(sampleMasterItem);
    }
}
