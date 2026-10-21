package vn.edu.utc.hotel_booking.modules.booking.service;

import vn.edu.utc.hotel_booking.modules.booking.dto.policy.ResolvedBookingPolicies;
import vn.edu.utc.hotel_booking.modules.booking.entity.Booking;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingDetail;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingRoom;
import vn.edu.utc.hotel_booking.modules.booking.entity.BookingType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BookingPolicyResolverService {

    /**
     * Hòa trộn cấu hình chuỗi và cơ sở để resolve chính sách cho booking mới và sinh snapshot bất biến
     */
    ResolvedBookingPolicies resolvePolicyForBooking(Short hotelId, Integer hotelRoomTypeId, BookingType bookingType);

    /**
     * Tính tỷ lệ phạt hủy (0.0 đến 1.0) dựa trên snapshot (hoặc fallback cho booking cũ)
     */
    BigDecimal calculateRoomCancellationPenaltyRate(BookingDetail detail, LocalDate cancelDate);

    /**
     * Mô tả chi tiết lý do phạt hủy hiển thị cho khách và lễ tân
     */
    String getCancellationPolicyDescription(BookingDetail detail, LocalDate cancelDate);

    /**
     * Tính toán số tiền phạt No-Show cho 1 phòng dựa trên snapshot (hoặc fallback cho booking cũ)
     */
    BigDecimal calculateSingleRoomNoShowPenalty(BookingRoom room, BookingDetail detail, Booking booking, BigDecimal firstNightRate);
}
