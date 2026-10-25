package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupInquiryCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupInquiryResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupInquiry;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupInquiryStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupInquiryRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.GroupInquiryServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Group Inquiry Service Unit Test")
class GroupInquiryServiceTest {

    @Mock
    private GroupInquiryRepository groupInquiryRepository;

    @Mock
    private HotelService hotelService;

    @Mock
    private HotelRoomTypeService hotelRoomTypeService;

    @InjectMocks
    private GroupInquiryServiceImpl groupInquiryService;

    @Test
    @DisplayName("Tạo Inquiry thành công khi dữ liệu hợp lệ và >= 6 phòng")
    void createInquiry_whenValid_shouldSucceed() {
        Short hotelId = 1;
        Hotel hotel = Hotel.builder().id(hotelId).name("InterContinental Hanoi").build();
        HotelRoomType roomType = HotelRoomType.builder().id(10).build();

        GroupInquiryCreateRequest request = GroupInquiryCreateRequest.builder()
                .hotelId(hotelId)
                .companyName("Viettel Group")
                .contactName("Nguyen Van A")
                .contactPhone("0987654321")
                .contactEmail("contact@viettel.com.vn")
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(13))
                .totalRooms(12)
                .totalGuests(24)
                .estimatedBudget(BigDecimal.valueOf(200000000))
                .requestedRoomTypes(List.of(
                        GroupInquiryCreateRequest.InquiryRoomTypeItem.builder()
                                .hotelRoomTypeId(10)
                                .requestedRooms(12)
                                .build()
                ))
                .build();

        when(hotelService.getEntityById(hotelId)).thenReturn(hotel);
        when(hotelRoomTypeService.getEntityById(10)).thenReturn(roomType);
        when(groupInquiryRepository.save(any(GroupInquiry.class))).thenAnswer(invocation -> {
            GroupInquiry inq = invocation.getArgument(0);
            inq.setId(100L);
            return inq;
        });

        GroupInquiryResponse response = groupInquiryService.createInquiry(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getInquiryNumber()).startsWith("INQ-");
        assertThat(response.getStatus()).isEqualTo(GroupInquiryStatus.PENDING);
        assertThat(response.getTotalRooms()).isEqualTo(12);
        assertThat(response.getCompanyName()).isEqualTo("Viettel Group");

        verify(groupInquiryRepository, times(1)).save(any(GroupInquiry.class));
    }

    @Test
    @DisplayName("Ném lỗi khi ngày check-out trước ngày check-in")
    void createInquiry_whenCheckOutBeforeCheckIn_shouldThrowException() {
        GroupInquiryCreateRequest request = GroupInquiryCreateRequest.builder()
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(8))
                .totalRooms(10)
                .build();

        assertThatThrownBy(() -> groupInquiryService.createInquiry(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_BOOKING_DATES);

        verifyNoInteractions(groupInquiryRepository);
    }

    @Test
    @DisplayName("Ném lỗi khi tổng số phòng < 6")
    void createInquiry_whenTotalRoomsLessThan6_shouldThrowException() {
        GroupInquiryCreateRequest request = GroupInquiryCreateRequest.builder()
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(8))
                .totalRooms(5)
                .build();

        assertThatThrownBy(() -> groupInquiryService.createInquiry(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_INQUIRY_MIN_ROOMS_INVALID);

        verifyNoInteractions(groupInquiryRepository);
    }

    @Test
    @DisplayName("Từ chối Inquiry thành công khi chưa convert")
    void rejectInquiry_whenStatusPending_shouldSucceed() {
        GroupInquiry inquiry = GroupInquiry.builder()
                .id(1L)
                .status(GroupInquiryStatus.PENDING)
                .build();

        when(groupInquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));
        when(groupInquiryRepository.save(any(GroupInquiry.class))).thenAnswer(i -> i.getArgument(0));

        GroupInquiryResponse response = groupInquiryService.rejectInquiry(1L, "Hết phòng giai đoạn cao điểm");

        assertThat(response.getStatus()).isEqualTo(GroupInquiryStatus.REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("Hết phòng giai đoạn cao điểm");
    }

    @Test
    @DisplayName("Báo lỗi khi từ chối Inquiry đã convert sang hợp đồng")
    void rejectInquiry_whenAlreadyConverted_shouldThrowException() {
        GroupInquiry inquiry = GroupInquiry.builder()
                .id(1L)
                .status(GroupInquiryStatus.CONVERTED)
                .build();

        when(groupInquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> groupInquiryService.rejectInquiry(1L, "Từ chối"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_INQUIRY_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("markQuoted: Cập nhật trạng thái Inquiry thành QUOTED và trỏ hợp đồng")
    void markQuoted_whenValid_shouldUpdateStatusAndContract() {
        GroupInquiry inquiry = GroupInquiry.builder()
                .id(10L)
                .status(GroupInquiryStatus.PENDING)
                .build();

        vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract contract = vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract.builder().id(200L).build();

        when(groupInquiryRepository.findById(10L)).thenReturn(Optional.of(inquiry));
        when(groupInquiryRepository.save(any(GroupInquiry.class))).thenAnswer(i -> i.getArgument(0));

        groupInquiryService.markQuoted(10L, contract);

        assertThat(inquiry.getStatus()).isEqualTo(GroupInquiryStatus.QUOTED);
        assertThat(inquiry.getConvertedContract()).isEqualTo(contract);
        verify(groupInquiryRepository, times(1)).save(inquiry);
    }

    @Test
    @DisplayName("markConvertedByContractId: Cập nhật trạng thái Inquiry thành CONVERTED")
    void markConvertedByContractId_whenFound_shouldUpdateStatusToConverted() {
        GroupInquiry inquiry = GroupInquiry.builder()
                .id(10L)
                .status(GroupInquiryStatus.QUOTED)
                .build();

        when(groupInquiryRepository.findByConvertedContractId(200L)).thenReturn(Optional.of(inquiry));
        when(groupInquiryRepository.save(any(GroupInquiry.class))).thenAnswer(i -> i.getArgument(0));

        groupInquiryService.markConvertedByContractId(200L);

        assertThat(inquiry.getStatus()).isEqualTo(GroupInquiryStatus.CONVERTED);
        verify(groupInquiryRepository, times(1)).save(inquiry);
    }
}
