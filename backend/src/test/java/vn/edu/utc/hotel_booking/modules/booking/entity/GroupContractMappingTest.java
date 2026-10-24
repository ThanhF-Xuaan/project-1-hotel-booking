package vn.edu.utc.hotel_booking.modules.booking.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.finance.entity.BillingRoutingRule;
import vn.edu.utc.hotel_booking.modules.finance.entity.ChargeCategory;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolio;
import vn.edu.utc.hotel_booking.modules.finance.entity.MasterFolioStatus;
import vn.edu.utc.hotel_booking.modules.finance.entity.RoutingTarget;
import vn.edu.utc.hotel_booking.modules.identity.entity.Company;
import vn.edu.utc.hotel_booking.modules.identity.entity.Staff;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group Booking JPA Entities — Mapping & Invariant Tests")
class GroupContractMappingTest {

    @Test
    @DisplayName("Khởi tạo và liên kết các thực thể GroupContract, GroupRoomBlock, MasterFolio thành công")
    void instantiateAndLinkEntities_shouldSucceed() {
        Hotel hotel = Hotel.builder().name("Viettel Luxury Hanoi").build();
        Company company = Company.builder().name("FPT Software").taxCode("0101234567").build();
        Staff staff = Staff.builder().build();

        GroupContract contract = GroupContract.builder()
                .contractNumber("GCT-20261025-0001")
                .hotel(hotel)
                .company(company)
                .salesStaff(staff)
                .groupName("Doan Teambuilding FPT 2026")
                .companyTaxCode("0101234567")
                .checkInDate(LocalDate.of(2026, 11, 10))
                .checkOutDate(LocalDate.of(2026, 11, 15))
                .totalRooms(20)
                .depositRate(BigDecimal.valueOf(50.00))
                .depositDueDate(OffsetDateTime.now().plusDays(3))
                .cutoffDate(OffsetDateTime.now().plusDays(10))
                .finalPaymentDueDate(OffsetDateTime.now().plusDays(14))
                .status(GroupContractStatus.DEPOSIT_PAID)
                .build();

        assertThat(contract.getContractNumber()).isEqualTo("GCT-20261025-0001");
        assertThat(contract.getTotalRooms()).isEqualTo(20);
        assertThat(contract.getDepositRate()).isEqualByComparingTo("50.00");
        assertThat(contract.getGoverningLaw()).isEqualTo("VIETNAMESE_LAW");

        // Quỹ phòng đoàn (GroupRoomBlock)
        HotelRoomType roomType = HotelRoomType.builder().id(1).build();
        GroupRoomBlock roomBlock = GroupRoomBlock.builder()
                .contract(contract)
                .hotelRoomType(roomType)
                .agreedRate(BigDecimal.valueOf(1500000.00))
                .totalBlockedRooms(20)
                .status(GroupRoomBlockStatus.DEFINITE)
                .build();
        contract.getRoomBlocks().add(roomBlock);

        assertThat(contract.getRoomBlocks()).hasSize(1);
        assertThat(contract.getRoomBlocks().get(0).getStatus()).isEqualTo(GroupRoomBlockStatus.DEFINITE);

        // Chi tiết ngày (GroupRoomBlockDaily)
        GroupRoomBlockDaily dailyBlock = GroupRoomBlockDaily.builder()
                .roomBlock(roomBlock)
                .hotelRoomType(roomType)
                .stayDate(LocalDate.of(2026, 11, 10))
                .blockedQuantity(20)
                .status(GroupRoomBlockDailyStatus.ACTIVE)
                .build();
        roomBlock.getDailyBlocks().add(dailyBlock);

        assertThat(roomBlock.getDailyBlocks()).hasSize(1);

        // Master Folio & Định tuyến chi phí (BillingRoutingRule)
        MasterFolio masterFolio = MasterFolio.builder()
                .folioNumber("MF-20261025-0001")
                .contract(contract)
                .hotel(hotel)
                .company(company)
                .buyerLegalName("Công ty Cổ phần Phần mềm FPT")
                .buyerTaxCode("0101234567")
                .buyerAddress("Tòa nhà FPT, Cầu Giấy, Hà Nội")
                .status(MasterFolioStatus.OPEN)
                .build();
        contract.getMasterFolios().add(masterFolio);

        BillingRoutingRule routingRule = BillingRoutingRule.builder()
                .masterFolio(masterFolio)
                .chargeCategory(ChargeCategory.ROOM_RATE)
                .routeTarget(RoutingTarget.MASTER)
                .isActive(true)
                .build();
        masterFolio.getRoutingRules().add(routingRule);

        assertThat(contract.getMasterFolios()).hasSize(1);
        assertThat(masterFolio.getRoutingRules()).hasSize(1);
        assertThat(masterFolio.getRoutingRules().get(0).getChargeCategory()).isEqualTo(ChargeCategory.ROOM_RATE);

        // Liên kết mở rộng trong Booking (FIT vs GIT compatibility)
        Booking booking = Booking.builder()
                .bookingNumber("BKG-20261025-0001")
                .hotel(hotel)
                .groupContract(contract)
                .build();

        assertThat(booking.getGroupContract()).isNotNull();
        assertThat(booking.getGroupContract().getContractNumber()).isEqualTo("GCT-20261025-0001");
    }
}
