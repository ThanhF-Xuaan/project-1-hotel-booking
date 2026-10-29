package vn.edu.utc.hotel_booking.modules.organization.service;

/** Public catalog eligibility only; exposes no guest, booking or write operation. */
public interface PublicHotelEligibilityReader {
    boolean isActivePublicHotel(short hotelId);
}
