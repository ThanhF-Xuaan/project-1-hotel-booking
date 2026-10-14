package vn.edu.utc.hotel_booking.modules.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancelSessionRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.InitiateDraftSessionRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CartPriceQuoteResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.DraftSessionResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.session.BookingDraftSession;
import vn.edu.utc.hotel_booking.modules.booking.service.impl.BookingSessionServiceImpl;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchHoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HoldReleaseReason;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSessionServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock ObjectMapper objectMapper;
    @Mock PublicBookingSearchService publicBookingSearchService;
    @Mock RoomAvailabilityService roomAvailabilityService;

    @InjectMocks
    BookingSessionServiceImpl bookingSessionService;

    @Test
    @DisplayName("initiateOrUpdateDraftSession: Lưu session vào Redis với TTL 30 phút và giữ phòng trong DB")
    void initiateOrUpdateDraftSession_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CartPriceQuoteResponse quoteResp = CartPriceQuoteResponse.builder()
                .finalTotalAmount(BigDecimal.valueOf(4000000))
                .grandTotal(BigDecimal.valueOf(4000000))
                .build();
        when(publicBookingSearchService.calculateCartQuote(any())).thenReturn(quoteResp);
        when(roomAvailabilityService.batchHoldInventory(any())).thenReturn("session-123");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"sessionId\":\"session-123\"}");

        InitiateDraftSessionRequest request = InitiateDraftSessionRequest.builder()
                .sessionId("session-123")
                .hotelId((short) 1)
                .checkInDate(LocalDate.now().plusDays(2))
                .checkOutDate(LocalDate.now().plusDays(4))
                .items(List.of(CartPriceQuoteRequest.CartItemQuoteRequest.builder()
                        .hotelRoomTypeId(10)
                        .quantity((short) 2)
                        .build()))
                .build();

        DraftSessionResponse response = bookingSessionService.initiateOrUpdateDraftSession(request);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("session-123");
        assertThat(response.getQuote().getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(4000000));

        verify(roomAvailabilityService, times(1)).batchHoldInventory(any(BatchHoldInventoryRequest.class));
        verify(valueOperations, times(1)).set(eq("booking:draft:session-123"), any(), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("getDraftSession: Lấy thông tin phiên draft hợp lệ và tính thời gian còn lại")
    void getDraftSession_Success() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        BookingDraftSession draftSession = BookingDraftSession.builder()
                .sessionId("draft-123")
                .hotelId((short) 1)
                .expiresAt(OffsetDateTime.now().plusMinutes(15))
                .quoteResponse(CartPriceQuoteResponse.builder()
                        .grandTotal(BigDecimal.valueOf(2500000))
                        .build())
                .build();

        when(valueOperations.get("booking:draft:draft-123")).thenReturn("mock-json");
        when(objectMapper.readValue("mock-json", BookingDraftSession.class)).thenReturn(draftSession);

        DraftSessionResponse response = bookingSessionService.getDraftSession("draft-123");

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("draft-123");
        assertThat(response.getRemainingSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("getDraftSession: Ném BOOKING_SESSION_EXPIRED khi phiên không tồn tại")
    void getDraftSession_Expired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("booking:draft:non-existent")).thenReturn(null);

        assertThatThrownBy(() -> bookingSessionService.getDraftSession("non-existent"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BOOKING_SESSION_EXPIRED);
    }

    @Test
    @DisplayName("cancelSession: Thu hồi kho phòng bằng sessionId và xóa session Redis")
    void cancelSession_Success() {
        CancelSessionRequest request = CancelSessionRequest.builder()
                .sessionId("session-123")
                .reason(HoldReleaseReason.USER_CANCELLED)
                .build();

        bookingSessionService.cancelSession(request);

        verify(roomAvailabilityService, times(1)).batchReleaseInventory(any(BatchReleaseInventoryRequest.class));
        verify(redisTemplate, times(1)).delete("booking:draft:session-123");
    }
}
