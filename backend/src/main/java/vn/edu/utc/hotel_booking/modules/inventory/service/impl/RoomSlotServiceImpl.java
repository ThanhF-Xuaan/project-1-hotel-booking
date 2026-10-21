package vn.edu.utc.hotel_booking.modules.inventory.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomInstance;
import vn.edu.utc.hotel_booking.modules.inventory.entity.RoomSlot;
import vn.edu.utc.hotel_booking.modules.inventory.repository.RoomSlotRepository;
import vn.edu.utc.hotel_booking.modules.inventory.service.RoomSlotService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomSlotServiceImpl implements RoomSlotService {

    RoomSlotRepository roomSlotRepository;

    @Override
    @Transactional
    public void createOrReserveSlots(RoomInstance roomInstance, LocalDate checkIn, LocalDate checkOut, Long bookingRoomId) {
        if (roomInstance == null || checkIn == null || checkOut == null) {
            return;
        }
        LocalDate cur = checkIn;
        while (cur.isBefore(checkOut)) {
            LocalDate slotDate = cur;
            Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDate(roomInstance.getId(), slotDate);
            RoomSlot slot = optSlot.orElseGet(() -> RoomSlot.builder()
                    .roomInstance(roomInstance)
                    .slotDate(slotDate)
                    .build());
            slot.setStatus("RESERVED");
            slot.setBookingRoomId(bookingRoomId);
            slot.setReservedAt(OffsetDateTime.now());
            roomSlotRepository.save(slot);
            cur = cur.plusDays(1);
        }
    }

    @Override
    @Transactional
    public void releaseSlots(Integer roomInstanceId, LocalDate checkIn, LocalDate checkOut) {
        if (roomInstanceId == null || checkIn == null || checkOut == null) {
            return;
        }
        LocalDate cur = checkIn;
        while (cur.isBefore(checkOut)) {
            Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDate(roomInstanceId, cur);
            if (optSlot.isPresent()) {
                RoomSlot slot = optSlot.get();
                slot.setStatus("READY");
                slot.setBookingRoomId(null);
                slot.setReservedAt(null);
                roomSlotRepository.save(slot);
            }
            cur = cur.plusDays(1);
        }
    }

    @Override
    @Transactional
    public void occupySlots(Integer roomInstanceId, LocalDate checkIn, LocalDate checkOut) {
        if (roomInstanceId == null || checkIn == null || checkOut == null) {
            return;
        }
        LocalDate cur = checkIn;
        while (cur.isBefore(checkOut)) {
            Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDate(roomInstanceId, cur);
            if (optSlot.isPresent()) {
                RoomSlot slot = optSlot.get();
                slot.setStatus("OCCUPIED");
                roomSlotRepository.save(slot);
            }
            cur = cur.plusDays(1);
        }
    }

    @Override
    @Transactional
    public void markSlotsDirty(Integer roomInstanceId, LocalDate checkIn, LocalDate checkOut) {
        if (roomInstanceId == null || checkIn == null || checkOut == null) {
            return;
        }
        LocalDate cur = checkIn;
        while (cur.isBefore(checkOut)) {
            Optional<RoomSlot> optSlot = roomSlotRepository.findByRoomInstanceIdAndSlotDate(roomInstanceId, cur);
            if (optSlot.isPresent()) {
                RoomSlot slot = optSlot.get();
                slot.setStatus("DIRTY");
                roomSlotRepository.save(slot);
            }
            cur = cur.plusDays(1);
        }
    }

    @Override
    @Transactional
    public void updateTodaySlotStatus(Integer roomInstanceId, String status) {
        if (roomInstanceId == null || status == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        roomSlotRepository.findByRoomInstanceIdAndSlotDate(roomInstanceId, today).ifPresent(slot -> {
            slot.setStatus(status);
            roomSlotRepository.save(slot);
        });
    }
}
