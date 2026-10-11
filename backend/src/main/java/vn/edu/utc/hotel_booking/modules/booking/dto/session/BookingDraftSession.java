package vn.edu.utc.hotel_booking.modules.booking.dto.session;

import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.LeadGuestRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CartPriceQuoteResponse;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDraftSession implements Serializable {

    private static final long serialVersionUID = 1L;

    String sessionId;
    Short hotelId;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    String voucherCode;

    @Builder.Default
    List<CartPriceQuoteRequest.CartItemQuoteRequest> items = new ArrayList<>();

    @Builder.Default
    List<CartPriceQuoteRequest.SelectedServiceRequest> selectedServices = new ArrayList<>();

    LeadGuestRequest leadGuest;
    CartPriceQuoteResponse quoteResponse;

    OffsetDateTime createdAt;
    OffsetDateTime expiresAt;
}
