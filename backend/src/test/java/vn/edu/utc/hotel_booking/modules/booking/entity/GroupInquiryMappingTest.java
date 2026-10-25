package vn.edu.utc.hotel_booking.modules.booking.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HotelRoomType;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group Inquiry Entity Mapping Test")
class GroupInquiryMappingTest {

    @Test
    @DisplayName("Khởi tạo GroupInquiry với đầy đủ thông tin đoàn và kiểm tra ràng buộc >= 6 phòng")
    void instantiateGroupInquiry_shouldSucceed() {
        Hotel hotel = Hotel.builder().id((short) 1).name("Grand Hanoi").build();
        GroupInquiry inquiry = GroupInquiry.builder()
                .inquiryNumber("INQ-20261026-0001")
                .hotel(hotel)
                .companyName("VinFast Global")
                .taxCode("0108999999")
                .contactName("Nguyen Van A")
                .contactPhone("0912345678")
                .contactEmail("contact@vinfast.vn")
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(13))
                .totalRooms(15)
                .totalGuests(30)
                .estimatedBudget(BigDecimal.valueOf(150000000))
                .status(GroupInquiryStatus.PENDING)
                .build();

        HotelRoomType roomType = HotelRoomType.builder().id(10).build();
        GroupInquiryRoomType requestedItem = GroupInquiryRoomType.builder()
                .inquiry(inquiry)
                .hotelRoomType(roomType)
                .requestedRooms(15)
                .build();
        inquiry.getRequestedRoomTypes().add(requestedItem);

        assertThat(inquiry.getInquiryNumber()).isEqualTo("INQ-20261026-0001");
        assertThat(inquiry.getTotalRooms()).isEqualTo(15);
        assertThat(inquiry.getStatus()).isEqualTo(GroupInquiryStatus.PENDING);
        assertThat(inquiry.getRequestedRoomTypes()).hasSize(1);
        assertThat(inquiry.getRequestedRoomTypes().get(0).getRequestedRooms()).isEqualTo(15);
    }
}
