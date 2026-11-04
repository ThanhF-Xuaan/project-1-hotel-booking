import React, { useState, useEffect, useCallback } from 'react';
import { LayoutDashboard } from 'lucide-react';
import TimelineRackView from '../components/TimelineRackView';
import RoomSwapModal, { type RoomSwapTarget } from '../components/RoomSwapModal';
import bookingService from '../services/booking.service';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import hotelService from '../../organization/services/hotel.service';
import type { BookingResponse, RoomSwapResponse } from '../types/booking.types';
import type { RoomInstanceDto } from '../../inventory/types/inventory.types';

export const TimelineMatrixPage: React.FC = () => {
  const [hotels, setHotels] = useState<{ id: number; name: string }[]>([]);
  const [selectedHotelId, setSelectedHotelId] = useState<number | undefined>(undefined);
  const [rooms, setRooms] = useState<RoomInstanceDto[]>([]);
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Room swap modal state
  const [isSwapModalOpen, setIsSwapModalOpen] = useState(false);
  const [swapTarget, setSwapTarget] = useState<RoomSwapTarget | null>(null);

  // 1. Fetch Hotels
  useEffect(() => {
    hotelService
      .filter({ page: 0, pageSize: 100 })
      .then((res) => {
        const hotelData = (res.result?.content || []).map((h) => ({
          id: h.id,
          name: h.name,
        }));
        setHotels(hotelData);
        if (hotelData.length > 0) {
          setSelectedHotelId(hotelData[0].id);
        }
      })
      .catch((err) => {
        console.error('Failed to load hotels for Timeline:', err);
      });
  }, []);

  // 2. Fetch Rooms & Bookings for Selected Hotel
  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [roomsRes, bookingsRes] = await Promise.all([
        roomInstanceService.filter({
          page: 0,
          pageSize: 500,
          hotelId: selectedHotelId,
        }),
        bookingService.filter({
          page: 0,
          pageSize: 100,
          size: 100,
          hotelId: selectedHotelId,
        }),
      ]);

      setRooms(roomsRes.result?.content || []);
      setBookings(bookingsRes.result?.content || []);
    } catch (err) {
      console.error('Failed to load timeline data:', err);
    } finally {
      setIsLoading(false);
    }
  }, [selectedHotelId]);

  useEffect(() => {
    if (selectedHotelId) {
      const timer = setTimeout(() => {
        void loadData();
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [selectedHotelId, loadData]);

  // Handler for opening swap modal
  const handleOpenSwapModal = (target: RoomSwapTarget) => {
    setSwapTarget(target);
    setIsSwapModalOpen(true);
  };

  const handleSwapSuccess = (res: RoomSwapResponse) => {
    alert(
      `Đổi phòng thành công! Đã chuyển khách sang phòng ${res.newRoomNumber} (${res.newRoomTypeName}).`
    );
    loadData();
  };

  return (
    <div className="space-y-4">
      {/* Breadcrumb matching Screenshot */}
      <div className="flex items-center gap-2 text-sm text-neutral-500">
        <span className="p-1 rounded-md bg-neutral-200 text-neutral-600">
          <LayoutDashboard className="w-3.5 h-3.5" />
        </span>
        <span>Admin</span>
        <span>/</span>
        <span className="font-bold text-neutral-900">Timeline Matrix</span>
      </div>

      {/* Main Timeline Rack Component */}
      <TimelineRackView
        rooms={rooms}
        bookings={bookings}
        isLoading={isLoading}
        onRefresh={loadData}
        onOpenSwapModal={handleOpenSwapModal}
        hotelList={hotels}
        selectedHotelId={selectedHotelId}
        onSelectHotelId={(id) => setSelectedHotelId(id)}
      />

      {/* Room Swap Modal */}
      <RoomSwapModal
        isOpen={isSwapModalOpen}
        onClose={() => {
          setIsSwapModalOpen(false);
          setSwapTarget(null);
        }}
        target={swapTarget}
        onSuccess={handleSwapSuccess}
      />
    </div>
  );
};

export default TimelineMatrixPage;
