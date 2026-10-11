package vn.edu.utc.hotel_booking.modules.inventory.service;

import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchHoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.BatchReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.HoldInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.ReleaseInventoryRequest;
import vn.edu.utc.hotel_booking.modules.inventory.dto.request.RoomAvailabilitySearchDto;
import vn.edu.utc.hotel_booking.modules.inventory.dto.response.RoomAvailabilityResponse;

import java.time.LocalDate;
import java.util.List;

public interface RoomAvailabilityService {

    List<RoomAvailabilityResponse> getAvailability(RoomAvailabilitySearchDto searchDto);

    void holdInventory(HoldInventoryRequest request);

    void releaseInventory(ReleaseInventoryRequest request);

    void batchHoldInventory(BatchHoldInventoryRequest request);

    void batchReleaseInventory(BatchReleaseInventoryRequest request);

    void confirmBookingInventory(Integer hotelRoomTypeId, LocalDate startDate, LocalDate endDate, Integer roomsCount);
}

