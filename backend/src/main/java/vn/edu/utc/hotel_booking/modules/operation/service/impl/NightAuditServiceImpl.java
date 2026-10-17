package vn.edu.utc.hotel_booking.modules.operation.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.common.util.SecurityUtils;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingLookupService;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingService;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomInstanceService;
import vn.edu.utc.hotel_booking.modules.operation.dto.request.NightAuditRequest;
import vn.edu.utc.hotel_booking.modules.operation.dto.response.NightAuditResponse;
import vn.edu.utc.hotel_booking.modules.operation.entity.NightAudit;
import vn.edu.utc.hotel_booking.modules.operation.repository.NightAuditRepository;
import vn.edu.utc.hotel_booking.modules.operation.service.NightAuditService;
import vn.edu.utc.hotel_booking.modules.operation.service.ServiceOrderService;
import vn.edu.utc.hotel_booking.modules.organization.entity.Hotel;
import vn.edu.utc.hotel_booking.modules.organization.service.HotelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class NightAuditServiceImpl implements NightAuditService {

    HotelService hotelService;
    BookingService bookingService;
    BookingLookupService bookingLookupService;
    RoomInstanceService roomInstanceService;
    ServiceOrderService serviceOrderService;
    NightAuditRepository nightAuditRepository;

    @Override
    @Transactional
    public NightAuditResponse executeNightAudit(NightAuditRequest request) {
        Short hotelId = request.getHotelId();
        LocalDate auditDate = request.getAuditDate();

        // 1. Kiểm tra chống chạy trùng đối soát đêm cho cùng một ngày kinh doanh
        if (nightAuditRepository.existsByHotelIdAndAuditDate(hotelId, auditDate)) {
            throw new AppException(ErrorCode.NIGHT_AUDIT_ALREADY_EXECUTED,
                    "Quy trình đối soát đêm ngày " + auditDate + " của khách sạn này đã được thực hiện");
        }

        // 2. Xác thực khách sạn qua HotelService
        Hotel hotel = hotelService.getEntityById(hotelId);

        // 3. Ủy quyền xử lý No-Show sang BookingService (Domain Expert)
        int noShowCount = bookingService.processNoShowBookings(hotelId, auditDate);

        // 4. Đếm số lượng phòng có khách đang ở qua RoomInstanceService (SQL count index)
        int occupiedRooms = roomInstanceService.countOccupiedRooms(hotelId);

        // 5. Tính doanh thu tiền phòng trong ngày qua BookingLookupService
        BigDecimal totalRoomRevenue = bookingLookupService.getDailyRoomRevenue(hotelId, auditDate);

        // 6. Tính doanh thu dịch vụ trong ngày qua ServiceOrderService (Domain Service)
        BigDecimal totalServiceRevenue = serviceOrderService.getDailyServiceRevenue(hotelId, auditDate);

        // 7. Tổng doanh thu trong ngày
        BigDecimal grandTotalRevenue = totalRoomRevenue.add(totalServiceRevenue);

        // 8. Lấy thông tin người thực hiện từ SecurityContext
        String executedBy = SecurityUtils.getCurrentUsername().orElse("SYSTEM");

        // 9. Lưu biên bản chốt ca kiểm toán đêm vào bảng night_audits
        NightAudit audit = NightAudit.builder()
                .hotel(hotel)
                .auditDate(auditDate)
                .occupiedRoomsCount(occupiedRooms)
                .noShowBookingsCount(noShowCount)
                .totalDailyRoomRevenue(totalRoomRevenue)
                .totalDailyServiceRevenue(totalServiceRevenue)
                .totalDailyRevenue(grandTotalRevenue)
                .status("COMPLETED")
                .executedBy(executedBy)
                .executedAt(OffsetDateTime.now())
                .build();

        NightAudit savedAudit = nightAuditRepository.save(audit);

        return NightAuditResponse.builder()
                .hotelId(hotelId)
                .auditDate(auditDate)
                .noShowBookingsCount(savedAudit.getNoShowBookingsCount())
                .occupiedRoomsCount(savedAudit.getOccupiedRoomsCount())
                .totalDailyRoomRevenue(savedAudit.getTotalDailyRoomRevenue())
                .totalDailyServiceRevenue(savedAudit.getTotalDailyServiceRevenue())
                .totalDailyRevenue(savedAudit.getTotalDailyRevenue())
                .auditTimestamp(savedAudit.getExecutedAt())
                .status(savedAudit.getStatus())
                .build();
    }

    @Override
    public NightAuditResponse getAuditSummary(Short hotelId, LocalDate auditDate) {
        // Xác thực khách sạn
        hotelService.getEntityById(hotelId);

        // Nếu ngày này đã được chốt sổ, trả về dữ liệu đã lưu trong bảng night_audits
        Optional<NightAudit> existingAudit = nightAuditRepository.findByHotelIdAndAuditDate(hotelId, auditDate);
        if (existingAudit.isPresent()) {
            NightAudit audit = existingAudit.get();
            return NightAuditResponse.builder()
                    .hotelId(hotelId)
                    .auditDate(auditDate)
                    .noShowBookingsCount(audit.getNoShowBookingsCount())
                    .occupiedRoomsCount(audit.getOccupiedRoomsCount())
                    .totalDailyRoomRevenue(audit.getTotalDailyRoomRevenue())
                    .totalDailyServiceRevenue(audit.getTotalDailyServiceRevenue())
                    .totalDailyRevenue(audit.getTotalDailyRevenue())
                    .auditTimestamp(audit.getExecutedAt())
                    .status(audit.getStatus())
                    .build();
        }

        // Nếu chưa chốt sổ, tính toán số liệu thống kê tạm thời (preview mode)
        int occupiedRooms = roomInstanceService.countOccupiedRooms(hotelId);
        BigDecimal totalRoomRevenue = bookingLookupService.getDailyRoomRevenue(hotelId, auditDate);
        BigDecimal totalServiceRevenue = serviceOrderService.getDailyServiceRevenue(hotelId, auditDate);
        BigDecimal grandTotalRevenue = totalRoomRevenue.add(totalServiceRevenue);

        return NightAuditResponse.builder()
                .hotelId(hotelId)
                .auditDate(auditDate)
                .noShowBookingsCount(0)
                .occupiedRoomsCount(occupiedRooms)
                .totalDailyRoomRevenue(totalRoomRevenue)
                .totalDailyServiceRevenue(totalServiceRevenue)
                .totalDailyRevenue(grandTotalRevenue)
                .auditTimestamp(OffsetDateTime.now())
                .status("PREVIEW")
                .build();
    }
}
