import React, { useState, useEffect, useMemo } from 'react';
import {
  BedDouble,
  Search,
  CheckCircle2,
  Calendar,
  User,
  AlertCircle,
  DoorClosed,
  Check,
  Building,
  Ban,
  Clock,
} from 'lucide-react';
import { Modal } from '../../../core/components/ui/Modal';
import { Button } from '../../../core/components/ui/Button';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import bookingService from '../services/booking.service';
import type { RoomAssignmentCandidateDto } from '../../inventory/types/inventory.types';

export interface RoomAssignTarget {
  bookingId: number;
  bookingNumber: string;
  bookingRoomId: number;
  hotelId: number;
  hotelName?: string;
  guestName?: string;
  roomTypeName?: string;
  hotelRoomTypeId?: number;
  checkInDate?: string;
  checkOutDate?: string;
}

export interface RoomAssignModalProps {
  isOpen: boolean;
  onClose: () => void;
  target: RoomAssignTarget | null;
  onSuccess: (assignedRoomNumber: string) => void;
}

export const RoomAssignModal: React.FC<RoomAssignModalProps> = ({
  isOpen,
  onClose,
  target,
  onSuccess,
}) => {
  const [rooms, setRooms] = useState<RoomAssignmentCandidateDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'AVAILABLE' | 'UNAVAILABLE'>('ALL');
  const [onlyMatchingType, setOnlyMatchingType] = useState(true);

  const [isAssigning, setIsAssigning] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Load candidate rooms based on stay dates and RoomSlot sparse data
  useEffect(() => {
    let active = true;

    if (!isOpen || !target?.hotelId || !target?.checkInDate || !target?.checkOutDate) {
      return;
    }

    const timer = setTimeout(() => {
      setIsLoading(true);
      setErrorMessage(null);
      setSelectedRoomId(null);
      setSearchQuery('');
      setStatusFilter('ALL');
      setOnlyMatchingType(Boolean(target.hotelRoomTypeId));

      roomInstanceService
        .getAvailableRoomsForAssignment({
          hotelId: target.hotelId,
          checkInDate: target.checkInDate!,
          checkOutDate: target.checkOutDate!,
          targetHotelRoomTypeId: target.hotelRoomTypeId,
          excludeBookingRoomId: target.bookingRoomId,
        })
        .then((res) => {
          if (active && res.result) {
            setRooms(res.result || []);
          }
        })
        .catch((err) => {
          if (active) {
            console.error('Lỗi tải danh sách phòng khả dụng (RoomSlot):', err);
            setErrorMessage('Không thể tải danh sách phòng khả dụng theo lịch lưu trú. Vui lòng thử lại.');
          }
        })
        .finally(() => {
          if (active) setIsLoading(false);
        });
    }, 0);

    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [isOpen, target]);

  // Filtered rooms
  const filteredRooms = useMemo(() => {
    return rooms.filter((room) => {
      // 1. Lọc theo search input
      if (searchQuery.trim()) {
        const query = searchQuery.trim().toLowerCase();
        const matchNumber = room.roomNumber.toLowerCase().includes(query);
        const matchType = room.roomTypeName?.toLowerCase().includes(query);
        if (!matchNumber && !matchType) return false;
      }

      // 2. Lọc theo tính khả dụng (RoomSlot Sparse Data)
      const isAvailable = Boolean(room.isAvailable ?? room.available);
      if (statusFilter === 'AVAILABLE' && !isAvailable) {
        return false;
      }
      if (statusFilter === 'UNAVAILABLE' && isAvailable) {
        return false;
      }

      // 3. Lọc theo đúng hạng phòng đã đặt
      const isSameType = Boolean(room.isSameRoomType ?? room.sameRoomType);
      if (onlyMatchingType && target?.hotelRoomTypeId) {
        if (!isSameType) {
          return false;
        }
      }

      return true;
    });
  }, [rooms, searchQuery, statusFilter, onlyMatchingType, target]);

  const selectedRoom = useMemo(() => {
    return rooms.find((r) => r.id === selectedRoomId) || null;
  }, [rooms, selectedRoomId]);

  const availableCount = useMemo(
    () => rooms.filter((r) => Boolean(r.isAvailable ?? r.available)).length,
    [rooms]
  );
  const busyCount = useMemo(
    () => rooms.filter((r) => !(r.isAvailable ?? r.available)).length,
    [rooms]
  );

  const handleConfirm = async () => {
    if (!target || !selectedRoomId || !selectedRoom) return;

    const isAvailable = selectedRoom.isAvailable ?? selectedRoom.available;
    if (!isAvailable) {
      setErrorMessage(`Phòng ${selectedRoom.roomNumber} không khả dụng: ${selectedRoom.conflictReason || 'Trùng lịch'}`);
      return;
    }

    setIsAssigning(true);
    setErrorMessage(null);
    try {
      await bookingService.assignRoom(target.bookingRoomId, selectedRoomId);
      onSuccess(selectedRoom.roomNumber);
      onClose();
    } catch (err: unknown) {
      console.error('Lỗi khi xếp phòng:', err);
      const errObj = err as { response?: { data?: { message?: string } }; message?: string };
      setErrorMessage(
        errObj.response?.data?.message ||
          errObj.message ||
          'Không thể xếp phòng. Có thể phòng đã có khách đặt hoặc trùng lịch.'
      );
    } finally {
      setIsAssigning(false);
    }
  };

  if (!isOpen || !target) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Xếp phòng vật lý (Room Assignment)"
      maxWidth="4xl"
    >
      <div className="space-y-5">
        {/* CONTEXT BANNER */}
        <div className="bg-neutral-50/80 border border-neutral-200/90 rounded-2xl p-4 shadow-2xs">
          <div className="flex items-center justify-between mb-2.5">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-red-600"></span>
              <span className="text-xs font-bold text-neutral-500 uppercase tracking-wider">
                Thông tin lượt lưu trú cần gán phòng
              </span>
            </div>
            <span className="text-[11px] font-semibold text-neutral-400 bg-neutral-200/60 px-2 py-0.5 rounded-md">
              Kiểm tra theo RoomSlot (Sparse Data)
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
            <div>
              <span className="text-xs text-neutral-500 flex items-center gap-1">
                <Building className="w-3.5 h-3.5 text-neutral-400" />
                Mã đơn đặt
              </span>
              <p className="font-mono font-bold text-neutral-900 mt-0.5">{target.bookingNumber}</p>
            </div>

            <div>
              <span className="text-xs text-neutral-500 flex items-center gap-1">
                <User className="w-3.5 h-3.5 text-neutral-400" />
                Khách hàng
              </span>
              <p className="font-semibold text-neutral-900 mt-0.5 truncate">{target.guestName || 'Khách vãng lai'}</p>
            </div>

            <div>
              <span className="text-xs text-neutral-500 flex items-center gap-1">
                <BedDouble className="w-3.5 h-3.5 text-neutral-400" />
                Hạng phòng đặt
              </span>
              <p className="font-semibold text-red-600 mt-0.5 truncate" title={target.roomTypeName}>
                {target.roomTypeName || 'Tiêu chuẩn'}
              </p>
            </div>

            <div>
              <span className="text-xs text-neutral-500 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                Dải ngày lưu trú
              </span>
              <p className="font-medium text-neutral-800 text-xs mt-1">
                {target.checkInDate} <span className="text-neutral-400">➔</span> {target.checkOutDate}
              </p>
            </div>
          </div>
        </div>

        {/* ERROR MESSAGE IF ANY */}
        {errorMessage && (
          <div className="flex items-start gap-2.5 p-3.5 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 animate-in fade-in">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-600 mt-0.5" />
            <div className="flex-1 font-medium">{errorMessage}</div>
          </div>
        )}

        {/* SEARCH & FILTERS BAR */}
        <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
          {/* Ô tìm kiếm */}
          <div className="relative flex-1 max-w-sm">
            <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo số phòng (VD: 101, 102B)..."
              className="w-full pl-9 pr-3 py-2 text-xs md:text-sm bg-white border border-neutral-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition"
            />
          </div>

          {/* Bộ lọc trạng thái & Toggle cùng hạng phòng */}
          <div className="flex items-center gap-2 flex-wrap">
            <div className="inline-flex rounded-xl p-1 bg-neutral-100 border border-neutral-200 text-xs">
              <button
                type="button"
                onClick={() => setStatusFilter('ALL')}
                className={`px-2.5 py-1 rounded-lg font-medium transition cursor-pointer ${
                  statusFilter === 'ALL'
                    ? 'bg-white text-neutral-900 shadow-2xs font-semibold'
                    : 'text-neutral-600 hover:text-neutral-900'
                }`}
              >
                Tất cả ({rooms.length})
              </button>
              <button
                type="button"
                onClick={() => setStatusFilter('AVAILABLE')}
                className={`px-2.5 py-1 rounded-lg font-medium transition cursor-pointer flex items-center gap-1 ${
                  statusFilter === 'AVAILABLE'
                    ? 'bg-white text-emerald-700 shadow-2xs font-semibold'
                    : 'text-neutral-600 hover:text-emerald-700'
                }`}
              >
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                Sẵn sàng ({availableCount})
              </button>
              <button
                type="button"
                onClick={() => setStatusFilter('UNAVAILABLE')}
                className={`px-2.5 py-1 rounded-lg font-medium transition cursor-pointer flex items-center gap-1 ${
                  statusFilter === 'UNAVAILABLE'
                    ? 'bg-white text-neutral-700 shadow-2xs font-semibold'
                    : 'text-neutral-600 hover:text-neutral-800'
                }`}
              >
                <span className="w-1.5 h-1.5 rounded-full bg-neutral-400"></span>
                Đã bận ({busyCount})
              </button>
            </div>

            {target.hotelRoomTypeId && (
              <label className="flex items-center gap-1.5 text-xs text-neutral-700 cursor-pointer select-none pl-1">
                <input
                  type="checkbox"
                  checked={onlyMatchingType}
                  onChange={(e) => setOnlyMatchingType(e.target.checked)}
                  className="rounded text-red-600 focus:ring-red-500 border-neutral-300 w-3.5 h-3.5"
                />
                <span>Cùng hạng phòng</span>
              </label>
            )}
          </div>
        </div>

        {/* VISUAL ROOM GRID PICKER */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-neutral-600">
              Chọn phòng vật lý khả dụng (Tìm thấy {filteredRooms.length} phòng):
            </span>
            {selectedRoom && (
              <span className="text-xs font-bold text-red-600 flex items-center gap-1">
                <Check className="w-3.5 h-3.5" /> Đã chọn: Phòng {selectedRoom.roomNumber} ({selectedRoom.roomTypeName})
              </span>
            )}
          </div>

          {isLoading ? (
            <div className="py-12 text-center text-neutral-400 text-xs">
              <Clock className="w-6 h-6 animate-spin mx-auto mb-2 text-neutral-300" />
              Đang phân tích khả dụng theo dải ngày lưu trú (RoomSlot Sparse Data)...
            </div>
          ) : filteredRooms.length === 0 ? (
            <div className="py-10 text-center bg-neutral-50 rounded-2xl border border-dashed border-neutral-300">
              <DoorClosed className="w-8 h-8 text-neutral-400 mx-auto mb-2" />
              <p className="text-xs text-neutral-600 font-medium">
                Không tìm thấy phòng vật lý nào phù hợp với bộ lọc.
              </p>
              {onlyMatchingType && (
                <button
                  type="button"
                  onClick={() => setOnlyMatchingType(false)}
                  className="mt-2 text-xs text-red-600 hover:underline font-semibold"
                >
                  Bỏ chọn 'Cùng hạng phòng' để xem các phòng hạng khác
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2.5 max-h-72 overflow-y-auto p-1 scrollbar-thin">
              {filteredRooms.map((room) => {
                const isSelected = selectedRoomId === room.id;
                const isAvailable = Boolean(room.isAvailable ?? room.available);
                const isSameRoomType = Boolean(room.isSameRoomType ?? room.sameRoomType);

                return (
                  <button
                    key={room.id}
                    type="button"
                    disabled={!isAvailable}
                    onClick={() => setSelectedRoomId(room.id)}
                    className={`relative p-3 rounded-xl border text-left transition-all select-none flex flex-col justify-between min-h-[96px] ${
                      isSelected
                        ? 'border-red-600 bg-red-50/60 ring-2 ring-red-600 shadow-xs cursor-pointer'
                        : !isAvailable
                        ? 'border-neutral-200 bg-neutral-100/70 opacity-60 cursor-not-allowed'
                        : 'border-neutral-200 bg-white hover:border-emerald-300 hover:bg-neutral-50/70 hover:shadow-2xs cursor-pointer'
                    }`}
                  >
                    {/* Header: Room Number + Status Icon */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono font-bold text-base text-neutral-900">
                          {room.roomNumber}
                        </span>
                        {isSameRoomType && (
                          <span className="text-[10px] font-bold text-red-600 bg-red-50 border border-red-200 px-1 py-0.2 rounded">
                            Chuẩn
                          </span>
                        )}
                      </div>
                      {isSelected ? (
                        <span className="w-5 h-5 rounded-full bg-red-600 text-white flex items-center justify-center">
                          <Check className="w-3 h-3 stroke-[3]" />
                        </span>
                      ) : !isAvailable ? (
                        <Ban className="w-4 h-4 text-neutral-400" />
                      ) : (
                        <span className="w-4 h-4 rounded-full border border-neutral-300"></span>
                      )}
                    </div>

                    {/* Room Type Label */}
                    <div className="mt-1">
                      <p className="text-[11px] text-neutral-500 font-medium truncate" title={room.roomTypeName}>
                        {room.roomTypeName}
                      </p>
                    </div>

                    {/* Footer Status Badge */}
                    <div className="mt-2 pt-1 border-t border-neutral-100 flex items-center justify-between text-[11px]">
                      {isAvailable ? (
                        <span className="inline-flex items-center gap-1 text-emerald-700 font-semibold">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                          Sẵn sàng
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-neutral-500 font-medium truncate" title={room.conflictReason || 'Đã có khách'}>
                          <span className="w-1.5 h-1.5 rounded-full bg-neutral-400"></span>
                          {room.conflictReason || 'Trùng lịch'}
                        </span>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* MODAL FOOTER */}
        <div className="flex items-center justify-between pt-4 border-t border-neutral-200">
          <div className="text-xs text-neutral-500 hidden sm:block">
            {selectedRoom ? (
              <span className="text-neutral-800">
                Chuẩn bị gán phòng <strong>{selectedRoom.roomNumber}</strong> cho đơn đặt{' '}
                <strong>{target.bookingNumber}</strong>
              </span>
            ) : (
              <span>Vui lòng chọn 1 phòng vật lý sẵn sàng từ danh sách trên</span>
            )}
          </div>

          <div className="flex items-center gap-2 ml-auto">
            <Button
              type="button"
              variant="outline"
              size="md"
              onClick={onClose}
              disabled={isAssigning}
            >
              Hủy
            </Button>
            <Button
              type="button"
              variant="primary"
              size="md"
              disabled={!selectedRoomId || isAssigning || !(selectedRoom?.isAvailable ?? selectedRoom?.available)}
              onClick={handleConfirm}
              className="min-w-[150px]"
            >
              {isAssigning ? (
                'Đang xếp phòng...'
              ) : (
                <>
                  <CheckCircle2 className="w-4 h-4 mr-1.5" />
                  Xác nhận xếp phòng
                </>
              )}
            </Button>
          </div>
        </div>
      </div>
    </Modal>
  );
};

export default RoomAssignModal;
