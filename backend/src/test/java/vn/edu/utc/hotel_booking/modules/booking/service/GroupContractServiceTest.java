package vn.edu.utc.hotel_booking.modules.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCancelRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractCreateRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.GroupContractDepositRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.GroupContractResponse;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContract;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupContractStatus;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlock;
import vn.edu.utc.hotel_booking.modules.booking.entity.GroupRoomBlockStatus;
import vn.edu.utc.hotel_booking.modules.booking.repository.GroupContractRepository;
import vn.edu.utc.hotel_booking.modules.booking.service.GroupInquiryService;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.GroupContractServiceImpl;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolio;
import vn.edu.utc.hotel_booking.modules.finance.service.MasterFolioService;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.identity.service.CompanyService;
import vn.edu.utc.hotel_booking.modules.identity.service.StaffLookupService;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.inventory.service.HotelRoomTypeService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Group Contract & Deposit 50% Service Unit Test")
class GroupContractServiceTest {

    @Mock
    private GroupContractRepository groupContractRepository;

    @Mock
    private GroupInquiryService groupInquiryService;

    @Mock
    private HotelService hotelService;

    @Mock
    private HotelRoomTypeService hotelRoomTypeService;

    @Mock
    private CompanyService companyService;

    @Mock
    private StaffLookupService staffLookupService;

    @Mock
    private MasterFolioService masterFolioService;

    @InjectMocks
    private GroupContractServiceImpl groupContractService;

    @Test
    @DisplayName("Tạo hợp đồng thành công: Tính toán đúng 5% phí phục vụ, 10% VAT, 50% cọc và thời hạn 3 ngày")
    void createContract_whenValid_shouldCalculateTaxesAndDepositCorrectly() {
        Short hotelId = 1;
        Long companyId = 5L;
        Integer staffId = 2;

        Hotel hotel = Hotel.builder().id(hotelId).name("InterContinental Hanoi").build();
        Company company = Company.builder().id(companyId).name("FPT Software").build();
        Staff staff = Staff.builder().id(staffId).fullName("Le Van B").build();
        HotelRoomType roomType = HotelRoomType.builder().id(10).build();

        // 10 phòng * 1,000,000 VND * 2 đêm = 20,000,000 VND subtotal
        // Phí phục vụ 5% = 1,000,000 VND -> Cơ sở VAT = 21,000,000 VND
        // VAT 10% = 2,100,000 VND -> Tổng HĐ = 23,100,000 VND
        // Cọc 50% = 11,550,000 VND
        GroupContractCreateRequest request = GroupContractCreateRequest.builder()
                .hotelId(hotelId)
                .companyId(companyId)
                .groupName("FPT Tech Summit")
                .companyTaxCode("0101234567")
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(12)) // 2 đêm
                .totalRooms(10)
                .totalGuests(20)
                .serviceFeeRate(BigDecimal.valueOf(5.00))
                .vatRate(BigDecimal.valueOf(10.00))
                .depositRate(BigDecimal.valueOf(50.00))
                .roomBlocks(List.of(
                        GroupContractCreateRequest.ContractRoomBlockItem.builder()
                                .hotelRoomTypeId(10)
                                .blockedRooms(10)
                                .negotiatedRatePerNight(BigDecimal.valueOf(1000000))
                                .build()
                ))
                .build();

        when(hotelService.getEntityById(hotelId)).thenReturn(hotel);
        when(companyService.getEntityById(companyId)).thenReturn(company);
        when(staffLookupService.getStaffEntityById(staffId)).thenReturn(staff);
        when(hotelRoomTypeService.getEntityById(10)).thenReturn(roomType);
        when(groupContractRepository.save(any(GroupContract.class))).thenAnswer(invocation -> {
            GroupContract gc = invocation.getArgument(0);
            gc.setId(200L);
            return gc;
        });

        GroupContractResponse response = groupContractService.createContract(request, staffId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getStatus()).isEqualTo(GroupContractStatus.TENTATIVE);
        assertThat(response.getSubtotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000000));
        assertThat(response.getServiceFeeAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(response.getVatAmount()).isEqualByComparingTo(BigDecimal.valueOf(2100000));
        assertThat(response.getTotalContractValue()).isEqualByComparingTo(BigDecimal.valueOf(23100000));
        assertThat(response.getDepositAmount()).isEqualByComparingTo(BigDecimal.valueOf(11550000));
        assertThat(response.getDepositDueDate()).isAfter(OffsetDateTime.now().plusDays(2));

        verify(groupContractRepository, times(1)).save(any(GroupContract.class));
    }

    @Test
    @DisplayName("Ném lỗi khi depositRate < 50% theo Điều 328 BLDS 2015")
    void createContract_whenDepositRateLessThan50_shouldThrowException() {
        GroupContractCreateRequest request = GroupContractCreateRequest.builder()
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(12))
                .totalRooms(10)
                .depositRate(BigDecimal.valueOf(40.00)) // Vi phạm < 50%
                .build();

        assertThatThrownBy(() -> groupContractService.createContract(request, 1))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_CONTRACT_DEPOSIT_INSUFFICIENT);
    }

    @Test
    @DisplayName("Xác nhận cọc 50% thành công: Hợp đồng sang DEPOSIT_PAID, kích hoạt block phòng DEFINITE và tự động sinh MasterFolio")
    void confirmDeposit_whenValid_shouldActivateRoomBlocksAndGenerateMasterFolio() {
        GroupRoomBlock block = GroupRoomBlock.builder()
                .id(1L)
                .status(GroupRoomBlockStatus.TENTATIVE)
                .build();

        List<GroupRoomBlock> blocks = new ArrayList<>();
        blocks.add(block);

        GroupContract contract = GroupContract.builder()
                .id(100L)
                .status(GroupContractStatus.TENTATIVE)
                .depositAmount(BigDecimal.valueOf(50000000))
                .depositDueDate(OffsetDateTime.now().plusDays(2)) // Chưa hết hạn 3 ngày
                .roomBlocks(blocks)
                .build();

        GroupContractDepositRequest request = GroupContractDepositRequest.builder()
                .amount(BigDecimal.valueOf(50000000))
                .transactionReference("TXN-VNPAY-998877")
                .notes("Đã chuyển khoản cọc 50%")
                .build();

        MasterFolio masterFolio = MasterFolio.builder()
                .id(999L)
                .folioNumber("MF-20261026-0001")
                .build();

        when(groupContractRepository.findById(100L)).thenReturn(Optional.of(contract));
        when(masterFolioService.createMasterFolioForContract(eq(contract), any(BigDecimal.class)))
                .thenReturn(masterFolio);
        when(groupContractRepository.save(any(GroupContract.class))).thenAnswer(i -> i.getArgument(0));

        GroupContractResponse response = groupContractService.confirmDeposit(100L, request);

        assertThat(response.getStatus()).isEqualTo(GroupContractStatus.DEPOSIT_PAID);
        assertThat(response.getDepositPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000000));
        assertThat(block.getStatus()).isEqualTo(GroupRoomBlockStatus.DEFINITE);
        assertThat(response.getMasterFolioId()).isEqualTo(999L);
        assertThat(response.getMasterFolioNumber()).isEqualTo("MF-20261026-0001");

        verify(masterFolioService, times(1)).createMasterFolioForContract(eq(contract), any(BigDecimal.class));
        verify(groupInquiryService, times(1)).markConvertedByContractId(100L);
    }

    @Test
    @DisplayName("Ném lỗi khi quá thời hạn giữ chỗ 3 ngày đặt cọc (Quá depositDueDate)")
    void confirmDeposit_whenDepositDueDateExpired_shouldThrowExceptionAndReleaseRooms() {
        GroupRoomBlock block = GroupRoomBlock.builder()
                .id(1L)
                .status(GroupRoomBlockStatus.TENTATIVE)
                .build();

        List<GroupRoomBlock> blocks = new ArrayList<>();
        blocks.add(block);

        GroupContract contract = GroupContract.builder()
                .id(100L)
                .status(GroupContractStatus.TENTATIVE)
                .depositAmount(BigDecimal.valueOf(50000000))
                .depositDueDate(OffsetDateTime.now().minusHours(1)) // Đã quá hạn 3 ngày
                .roomBlocks(blocks)
                .build();

        GroupContractDepositRequest request = GroupContractDepositRequest.builder()
                .amount(BigDecimal.valueOf(50000000))
                .transactionReference("TXN-LATE")
                .build();

        when(groupContractRepository.findById(100L)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> groupContractService.confirmDeposit(100L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_CONTRACT_DEPOSIT_EXPIRED);

        assertThat(contract.getStatus()).isEqualTo(GroupContractStatus.CANCELLED_NO_DEPOSIT);
        assertThat(block.getStatus()).isEqualTo(GroupRoomBlockStatus.RELEASED);
        verify(groupContractRepository, times(1)).save(contract);
        verifyNoInteractions(masterFolioService);
    }

    @Test
    @DisplayName("Ném lỗi khi số tiền thanh toán không đủ 50% giá trị cọc theo Điều 328 BLDS 2015")
    void confirmDeposit_whenAmountInsufficient_shouldThrowException() {
        GroupContract contract = GroupContract.builder()
                .id(100L)
                .status(GroupContractStatus.TENTATIVE)
                .depositAmount(BigDecimal.valueOf(50000000))
                .depositDueDate(OffsetDateTime.now().plusDays(1))
                .build();

        GroupContractDepositRequest request = GroupContractDepositRequest.builder()
                .amount(BigDecimal.valueOf(30000000)) // Chỉ nộp 30 triệu, thiếu 20 triệu
                .transactionReference("TXN-INSUFFICIENT")
                .build();

        when(groupContractRepository.findById(100L)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> groupContractService.confirmDeposit(100L, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_CONTRACT_DEPOSIT_INSUFFICIENT);

        verifyNoInteractions(masterFolioService);
    }

    @Test
    @DisplayName("Hủy hợp đồng thành công và hủy các Room Blocks")
    void cancelContract_whenValid_shouldCancelContractAndBlocks() {
        GroupRoomBlock block = GroupRoomBlock.builder()
                .id(1L)
                .status(GroupRoomBlockStatus.TENTATIVE)
                .build();

        List<GroupRoomBlock> blocks = new ArrayList<>();
        blocks.add(block);

        GroupContract contract = GroupContract.builder()
                .id(100L)
                .status(GroupContractStatus.TENTATIVE)
                .roomBlocks(blocks)
                .build();

        GroupContractCancelRequest request = GroupContractCancelRequest.builder()
                .cancellationReason("Doanh nghiệp thay đổi lịch công tác")
                .build();

        when(groupContractRepository.findById(100L)).thenReturn(Optional.of(contract));
        when(groupContractRepository.save(any(GroupContract.class))).thenAnswer(i -> i.getArgument(0));

        GroupContractResponse response = groupContractService.cancelContract(100L, request);

        assertThat(response.getStatus()).isEqualTo(GroupContractStatus.CANCELLED_NO_DEPOSIT);
        assertThat(response.getCancellationReason()).isEqualTo("Doanh nghiệp thay đổi lịch công tác");
        assertThat(block.getStatus()).isEqualTo(GroupRoomBlockStatus.CANCELLED);
    }
}
