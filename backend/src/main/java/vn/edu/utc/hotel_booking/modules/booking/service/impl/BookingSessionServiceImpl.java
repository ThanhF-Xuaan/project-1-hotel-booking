package vn.edu.utc.hotel_booking.modules.booking.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.common.exception.AppException;
import vn.edu.utc.hotel_booking.common.exception.ErrorCode;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CancelSessionRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.CartPriceQuoteRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.request.InitiateDraftSessionRequest;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.CartPriceQuoteResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.response.DraftSessionResponse;
import vn.edu.utc.hotel_booking.modules.booking.dto.session.BookingDraftSession;
import vn.edu.utc.hotel_booking.modules.booking.service.BookingSessionService;
import vn.edu.utc.hotel_booking.modules.booking.service.PublicBookingSearchService;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchHoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.entity.HoldReleaseReason;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomAvailabilityService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingSessionServiceImpl implements BookingSessionService {

    private static final String DRAFT_KEY_PREFIX = "booking:draft:";
    private static final Duration DRAFT_TTL = Duration.ofMinutes(30);

    StringRedisTemplate redisTemplate;
    ObjectMapper objectMapper;

    PublicBookingSearchService publicBookingSearchService;
    RoomAvailabilityService roomAvailabilityService;

    @Override
    @Transactional
    public DraftSessionResponse initiateOrUpdateDraftSession(InitiateDraftSessionRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Dữ liệu yêu cầu không được để trống");
        }

        String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId().trim()
                : UUID.randomUUID().toString();

        // Nếu session cũ đã tồn tại, giải phóng hold cũ trước khi thiết lập hold mới
        String existingJson = redisTemplate.opsForValue().get(DRAFT_KEY_PREFIX + sessionId);
        if (existingJson != null) {
            roomAvailabilityService.batchReleaseInventory(BatchReleaseInventoryRequest.builder()
                    .sessionId(sessionId)
                    .releaseReason(HoldReleaseReason.SESSION_SUPERSEDED)
                    .build());
        }

        // 1. Tính toán báo giá thời gian thực
        CartPriceQuoteRequest quoteReq = CartPriceQuoteRequest.builder()
                .hotelId(request.getHotelId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .voucherCode(request.getVoucherCode())
                .items(request.getItems())
                .selectedServices(request.getSelectedServices())
                .build();
        CartPriceQuoteResponse quoteResp = publicBookingSearchService.calculateCartQuote(quoteReq);

        // 2. Khóa tạm giữ phòng (Optimistic Locking & lưu vết granular hold)
        List<BatchHoldInventoryRequest.RoomTypeHoldItem> holdItems = request.getItems().stream()
                .map(item -> BatchHoldInventoryRequest.RoomTypeHoldItem.builder()
                        .hotelRoomTypeId(item.getHotelRoomTypeId())
                        .roomsCount(item.getQuantity() != null ? item.getQuantity().intValue() : 1)
                        .build())
                .toList();

        BatchHoldInventoryRequest holdReq = BatchHoldInventoryRequest.builder()
                .sessionId(sessionId)
                .hotelId(request.getHotelId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .holdMinutes(30)
                .items(holdItems)
                .build();
        roomAvailabilityService.batchHoldInventory(holdReq);

        // 3. Lưu phiên Draft vào Redis với TTL 30 phút
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(DRAFT_TTL);

        BookingDraftSession draftSession = BookingDraftSession.builder()
                .sessionId(sessionId)
                .hotelId(request.getHotelId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .voucherCode(request.getVoucherCode())
                .items(request.getItems())
                .selectedServices(request.getSelectedServices() != null ? request.getSelectedServices() : new ArrayList<>())
                .leadGuest(request.getLeadGuest())
                .quoteResponse(quoteResp)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        saveToRedis(DRAFT_KEY_PREFIX + sessionId, draftSession, DRAFT_TTL);

        return DraftSessionResponse.builder()
                .sessionId(sessionId)
                .expiresAt(expiresAt)
                .remainingSeconds(DRAFT_TTL.getSeconds())
                .quote(quoteResp)
                .leadGuest(request.getLeadGuest())
                .build();
    }

    @Override
    public DraftSessionResponse getDraftSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_DATA, "Mã sessionId không được để trống");
        }

        BookingDraftSession session = getFromRedis(DRAFT_KEY_PREFIX + sessionId.trim(), BookingDraftSession.class);
        if (session == null) {
            throw new AppException(ErrorCode.BOOKING_SESSION_EXPIRED, "Phiên đặt phòng đã hết hạn hoặc không tồn tại");
        }

        long remainingSec = Math.max(0, Duration.between(OffsetDateTime.now(), session.getExpiresAt()).getSeconds());

        return DraftSessionResponse.builder()
                .sessionId(session.getSessionId())
                .expiresAt(session.getExpiresAt())
                .remainingSeconds(remainingSec)
                .quote(session.getQuoteResponse())
                .leadGuest(session.getLeadGuest())
                .build();
    }

    @Override
    @Transactional
    public void cancelSession(CancelSessionRequest request) {
        if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()) {
            return;
        }

        String draftSessionId = request.getSessionId().trim();

        // Giải phóng phòng trong PostgreSQL
        roomAvailabilityService.batchReleaseInventory(BatchReleaseInventoryRequest.builder()
                .sessionId(draftSessionId)
                .releaseReason(request.getReason() != null ? request.getReason() : HoldReleaseReason.USER_CANCELLED)
                .build());

        // Xóa draft session trong Redis
        redisTemplate.delete(DRAFT_KEY_PREFIX + draftSessionId);
    }

    private void saveToRedis(String key, Object data, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.error("Lỗi tuần tự hóa JSON khi lưu Redis key {}: {}", key, e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi hệ thống khi lưu phiên đặt phòng");
        }
    }

    private <T> T getFromRedis(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Lỗi giải mã JSON từ Redis key {}: {}", key, e.getMessage());
            return null;
        }
    }
}
