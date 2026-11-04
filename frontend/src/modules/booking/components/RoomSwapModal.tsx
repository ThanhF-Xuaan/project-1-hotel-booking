import React, { useState, useEffect, useMemo } from 'react';
import { ArrowLeftRight, AlertCircle, CheckCircle2, BedDouble, Building, Calendar, User, Info } from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import bookingService from '../services/booking.service';
import roomInstanceService from '../../inventory/services/roomInstance.service';
import type { RoomInstanceDto } from '../../inventory/types/inventory.types';
import type { RoomSwapResponse, RoomSwapRequest } from '../types/booking.types';

export interface RoomSwapTarget {
  bookingId: number;
  bookingNumber: string;
  bookingRoomId: number;
  currentRoomInstanceId?: number;
  currentRoomNumber?: string;
  currentRoomTypeName?: string;
  guestName?: string;
  checkInDate?: string;
  checkOutDate?: string;
  hotelId?: number;
  currentPrice?: number;
}

export interface RoomSwapModalProps {
  isOpen: boolean;
  onClose: () => void;
  target: RoomSwapTarget | null;
  onSuccess: (response: RoomSwapResponse) => void;
}

const COMMON_SWAP_REASONS = [
  'Khách yêu cầu nâng hạng phòng (Upgrade)',
  'Sự cố kỹ thuật / Thiết bị phòng hỏng (Hỗ trợ đổi phòng)',
  'Khách yêu cầu phòng yên tĩnh hơn / Hướng phòng khác',
  'Khách muốn ở gần phòng người thân / đồng nghiệp',
  'Yêu cầu buồng phòng / Vệ sinh cần xử lý gấp',
  'Lý do khác (Nhập chi tiết bên dưới)',
];

export const RoomSwapModal: React.FC<RoomSwapModalProps> = ({
  isOpen,
  onClose,
  target,
  onSuccess,
}) => {
  const [availableRooms, setAvailableRooms] = useState<RoomInstanceDto[]>([]);
  const [isLoadingRooms, setIsLoadingRooms] = useState(false);

  // Form states
  const [selectedTargetRoomId, setSelectedTargetRoomId] = useState<string>('');
  const [filterRoomType, setFilterRoomType] = useState<string>('ALL');
  const [selectedReasonOption, setSelectedReasonOption] = useState<string>(COMMON_SWAP_REASONS[0]);
  const [customReasonDetail, setCustomReasonDetail] = useState('');
  const [isComplimentary, setIsComplimentary] = useState<boolean>(false);
  const [effectiveDate, setEffectiveDate] = useState<string>(() => new Date().toISOString().split('T')[0]);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Load available rooms (READY / AVAILABLE)
  useEffect(() => {
    const timer = setTimeout(() => {
      if (!isOpen || !target) {
        setAvailableRooms([]);
        setSelectedTargetRoomId('');
        setSubmitError(null);
        return;
      }

      setIsLoadingRooms(true);
      setSubmitError(null);

      roomInstanceService
        .filter({
          hotelId: target.hotelId,
          currentStatus: 'READY',
          pageSize: 100,
        })
        .then((res) => {
          if (res.result) {
            // Exclude current room
            const rooms = (res.result.content || []).filter(
              (r) => r.id !== target.currentRoomInstanceId
            );
            setAvailableRooms(rooms);
          }
        })
        .catch((err) => {
          console.error('Lỗi tải danh sách phòng trống:', err);
          setSubmitError('Không thể tải danh sách phòng trống khả dụng.');
        })
        .finally(() => {
          setIsLoadingRooms(false);
        });
    }, 0);

    return () => clearTimeout(timer);
  }, [isOpen, target]);

  // Unique room types from available rooms
  const roomTypeOptions = useMemo(() => {
    const map = new Map<number, string>();
    availableRooms.forEach((r) => {
      if (r.hotelRoomTypeId && r.roomTypeName) {
        map.set(r.hotelRoomTypeId, r.roomTypeName);
      }
    });
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [availableRooms]);

  // Filtered rooms
  const filteredRooms = useMemo(() => {
    if (filterRoomType === 'ALL') return availableRooms;
    return availableRooms.filter((r) => String(r.hotelRoomTypeId) === filterRoomType);
  }, [availableRooms, filterRoomType]);

  // Selected room object
  const selectedRoomObj = useMemo(() => {
    if (!selectedTargetRoomId) return null;
    return availableRooms.find((r) => String(r.id) === selectedTargetRoomId) || null;
  }, [availableRooms, selectedTargetRoomId]);

  // Detect if changing room type
  const isDifferentRoomType = useMemo(() => {
    if (!selectedRoomObj || !target?.currentRoomTypeName) return false;
    return selectedRoomObj.roomTypeName !== target.currentRoomTypeName;
  }, [selectedRoomObj, target]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!target || !selectedTargetRoomId) {
      setSubmitError('Vui lòng chọn phòng đích để chuyển sang.');
      return;
    }

    const finalReason =
      selectedReasonOption === 'Lý do khác (Nhập chi tiết bên dưới)'
        ? customReasonDetail.trim()
        : selectedReasonOption;

    if (!finalReason) {
      setSubmitError('Vui lòng nhập lý do đổi phòng.');
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    const payload: RoomSwapRequest = {
      targetRoomInstanceId: Number(selectedTargetRoomId),
      reason: finalReason,
      isComplimentary,
      effectiveDate: effectiveDate || undefined,
    };

    try {
      const res = await bookingService.swapRoom(target.bookingRoomId, payload);
      if (res.result) {
        onSuccess(res.result);
        onClose();
      } else {
        setSubmitError(res.message || 'Đổi phòng không thành công.');
      }
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } }; message?: string };
      setSubmitError(
        errorObj.response?.data?.message || errorObj.message || 'Đã xảy ra lỗi khi thực hiện đổi phòng.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen || !target) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Thao Tác Đổi Phòng Lưu Trú (Room Swap)" maxWidth="2xl">
      <form onSubmit={handleSubmit} className="space-y-5">
        {/* Current Room Summary Card */}
        <div className="bg-neutral-50 border border-neutral-200 rounded-2xl p-4">
          <div className="flex items-center gap-2 mb-3">
            <span className="w-2 h-2 rounded-full bg-red-600"></span>
            <span className="text-xs font-bold text-neutral-500 uppercase tracking-wider">
              Phòng Hiện Tại Của Khách
            </span>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
            <div>
              <div className="text-xs text-neutral-500 flex items-center gap-1">
                <BedDouble className="w-3.5 h-3.5 text-neutral-400" />
                Số phòng
              </div>
              <div className="font-bold text-neutral-900 text-base mt-0.5">
                {target.currentRoomNumber ? `Phòng ${target.currentRoomNumber}` : 'Chưa gán số'}
              </div>
            </div>
            <div>
              <div className="text-xs text-neutral-500 flex items-center gap-1">
                <Building className="w-3.5 h-3.5 text-neutral-400" />
                Hạng phòng
              </div>
              <div className="font-semibold text-neutral-800 mt-0.5 truncate">
                {target.currentRoomTypeName || 'Tiêu chuẩn'}
              </div>
            </div>
            <div>
              <div className="text-xs text-neutral-500 flex items-center gap-1">
                <User className="w-3.5 h-3.5 text-neutral-400" />
                Khách lưu trú
              </div>
              <div className="font-semibold text-neutral-800 mt-0.5 truncate">
                {target.guestName || 'Khách vãng lai'}
              </div>
            </div>
            <div>
              <div className="text-xs text-neutral-500 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                Mã đơn đặt
              </div>
              <div className="font-mono font-bold text-red-600 mt-0.5">
                {target.bookingNumber}
              </div>
            </div>
          </div>
        </div>

        {/* Target Room Selection */}
        <div className="space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <label className="text-sm font-bold text-neutral-800 flex items-center gap-1.5">
              <ArrowLeftRight className="w-4 h-4 text-red-600" />
              Chọn Phòng Đích Chuyển Đến <span className="text-red-500">*</span>
            </label>
            {roomTypeOptions.length > 1 && (
              <div className="flex items-center gap-2 text-xs">
                <span className="text-neutral-500">Lọc theo hạng:</span>
                <select
                  aria-label="Lọc theo hạng phòng"
                  value={filterRoomType}
                  onChange={(e) => setFilterRoomType(e.target.value)}
                  className="px-2.5 py-1 bg-white border border-neutral-300 rounded-lg text-neutral-700 text-xs focus:ring-2 focus:ring-red-600 focus:outline-none"
                >
                  <option value="ALL">Tất cả hạng phòng ({availableRooms.length})</option>
                  {roomTypeOptions.map((opt) => (
                    <option key={opt.id} value={String(opt.id)}>
                      {opt.name}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {isLoadingRooms ? (
            <div className="py-6 text-center text-sm text-neutral-500 bg-neutral-50 border border-neutral-200 rounded-xl">
              Đang tải danh sách phòng trống khả dụng...
            </div>
          ) : filteredRooms.length === 0 ? (
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-sm flex items-start gap-2">
              <AlertCircle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">Không có phòng trống sẵn sàng (READY)</p>
                <p className="text-xs text-amber-700 mt-1">
                  Hiện tại không có phòng vật lý nào ở trạng thái sẵn sàng đón khách. Vui lòng kiểm tra lại sơ đồ buồng phòng hoặc hoàn tất dọn phòng trước khi đổi.
                </p>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 max-h-48 overflow-y-auto p-1 border border-neutral-200 rounded-xl bg-neutral-50/50">
              {filteredRooms.map((room) => {
                const isSelected = String(room.id) === selectedTargetRoomId;
                const isSameType = room.roomTypeName === target.currentRoomTypeName;
                return (
                  <button
                    key={room.id}
                    type="button"
                    onClick={() => setSelectedTargetRoomId(String(room.id))}
                    className={`p-3 rounded-xl border text-left transition-all relative flex flex-col justify-between cursor-pointer ${
                      isSelected
                        ? 'border-red-600 bg-red-50/60 ring-2 ring-red-600/30 shadow-xs'
                        : 'border-neutral-200 bg-white hover:border-neutral-300 hover:shadow-2xs'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-extrabold text-neutral-900 text-base">
                        P.{room.roomNumber}
                      </span>
                      {isSelected && <CheckCircle2 className="w-4 h-4 text-red-600" />}
                    </div>
                    <div className="mt-1.5">
                      <p className="text-xs font-medium text-neutral-700 truncate">
                        {room.roomTypeName}
                      </p>
                      <span
                        className={`inline-block text-[10px] font-semibold px-1.5 py-0.5 rounded mt-1 ${
                          isSameType
                            ? 'bg-neutral-100 text-neutral-600'
                            : 'bg-blue-100 text-blue-700 font-bold'
                        }`}
                      >
                        {isSameType ? 'Cùng hạng' : 'Hạng khác / Upgrade'}
                      </span>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Change of Room Type Notice */}
        {isDifferentRoomType && selectedRoomObj && (
          <div className="p-3.5 bg-blue-50 border border-blue-200 rounded-xl text-blue-900 text-xs space-y-2">
            <div className="flex items-start gap-2">
              <Info className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-bold">Đổi sang hạng phòng khác:</p>
                <p className="mt-0.5">
                  Từ <span className="font-semibold">{target.currentRoomTypeName}</span> sang{' '}
                  <span className="font-bold text-blue-800">{selectedRoomObj.roomTypeName}</span>. Hệ thống sẽ tính lại giá phòng và phụ thu theo quy tắc nâng hạng.
                </p>
              </div>
            </div>
            <label className="flex items-center gap-2 pt-1.5 border-t border-blue-200/80 cursor-pointer">
              <input
                type="checkbox"
                checked={isComplimentary}
                onChange={(e) => setIsComplimentary(e.target.checked)}
                className="w-4 h-4 text-red-600 rounded border-neutral-300 focus:ring-red-600"
              />
              <span className="font-semibold text-neutral-800">
                Miễn phí nâng hạng (Complimentary Upgrade — Khách VIP / Hỗ trợ sự cố kỹ thuật)
              </span>
            </label>
          </div>
        )}

        {/* Reason & Effective Date */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="sm:col-span-2 space-y-1.5">
            <label className="text-xs font-bold text-neutral-700">
              Lý do đổi phòng <span className="text-red-500">*</span>
            </label>
            <select
              aria-label="Lý do đổi phòng"
              value={selectedReasonOption}
              onChange={(e) => setSelectedReasonOption(e.target.value)}
              className="w-full h-11 px-3 bg-white border border-neutral-300 rounded-xl text-neutral-800 text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
            >
              {COMMON_SWAP_REASONS.map((reason, idx) => (
                <option key={idx} value={reason}>
                  {reason}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-neutral-700">Ngày hiệu lực</label>
            <Input
              type="date"
              value={effectiveDate}
              onChange={(e) => setEffectiveDate(e.target.value)}
              className="h-11"
            />
          </div>
        </div>

        {selectedReasonOption === 'Lý do khác (Nhập chi tiết bên dưới)' && (
          <div className="space-y-1">
            <label className="text-xs font-bold text-neutral-700">
              Chi tiết lý do đổi phòng <span className="text-red-500">*</span>
            </label>
            <textarea
              aria-label="Chi tiết lý do đổi phòng"
              value={customReasonDetail}
              onChange={(e) => setCustomReasonDetail(e.target.value)}
              placeholder="Nhập chi tiết lý do đổi phòng để ghi vết kiểm toán..."
              rows={2}
              className="w-full p-3 bg-white border border-neutral-300 rounded-xl text-sm text-neutral-800 focus:ring-2 focus:ring-red-600 focus:outline-none resize-none"
              required
            />
          </div>
        )}

        {/* Error message */}
        {submitError && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
            <span>{submitError}</span>
          </div>
        )}

        {/* Action buttons */}
        <div className="flex items-center justify-end gap-3 pt-3 border-t border-neutral-200">
          <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting}>
            Hủy Bỏ
          </Button>
          <Button
            type="submit"
            disabled={isSubmitting || !selectedTargetRoomId || filteredRooms.length === 0}
            className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white font-bold px-6"
          >
            {isSubmitting ? (
              <>
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                Đang xử lý...
              </>
            ) : (
              <>
                <ArrowLeftRight className="w-4 h-4" />
                Xác Nhận Đổi Phòng
              </>
            )}
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default RoomSwapModal;
