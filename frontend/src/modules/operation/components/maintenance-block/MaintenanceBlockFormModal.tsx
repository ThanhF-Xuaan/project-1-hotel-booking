import React, { useState, useEffect } from 'react';
import { AlertTriangle, Wrench, ShieldAlert, Calendar, FileText } from 'lucide-react';
import Modal from '../../../../core/components/ui/Modal';
import Button from '../../../../core/components/ui/Button';
import { operationService } from '../../services/operation.service';
import type {
  RoomHousekeepingStatusResponse,
  MaintenanceBlockType,
  CreateMaintenanceBlockRequest,
} from '../../types/operation.types';

export interface MaintenanceBlockFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (message: string) => void;
  onError: (err: unknown) => void;
  rooms: RoomHousekeepingStatusResponse[];
  initialRoomInstanceId?: number;
}

export const MaintenanceBlockFormModal: React.FC<MaintenanceBlockFormModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  onError,
  rooms,
  initialRoomInstanceId,
}) => {
  const today = new Date().toISOString().split('T')[0];

  const [roomInstanceId, setRoomInstanceId] = useState<number | ''>('');
  const [blockType, setBlockType] = useState<MaintenanceBlockType>('OOO');
  const [startDate, setStartDate] = useState<string>(today);
  const [endDate, setEndDate] = useState<string>(today);
  const [reason, setReason] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setRoomInstanceId(initialRoomInstanceId || (rooms.length > 0 ? rooms[0].roomInstanceId : ''));
      setBlockType('OOO');
      setStartDate(today);
      setEndDate(today);
      setReason('');
      setNotes('');
      setFormError(null);
    }
  }, [isOpen, initialRoomInstanceId, rooms, today]);

  const selectedRoom = rooms.find((r) => r.roomInstanceId === Number(roomInstanceId));
  const isOccupied = selectedRoom?.currentStatus === 'OCCUPIED';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!roomInstanceId) {
      setFormError('Vui lòng chọn phòng cần khóa bảo trì.');
      return;
    }
    if (!reason.trim()) {
      setFormError('Vui lòng nhập lý do khóa bảo trì.');
      return;
    }
    if (!startDate || !endDate) {
      setFormError('Vui lòng chọn ngày bắt đầu và kết thúc.');
      return;
    }
    if (endDate < startDate) {
      setFormError('Ngày kết thúc không được trước ngày bắt đầu.');
      return;
    }

    const payload: CreateMaintenanceBlockRequest = {
      roomInstanceId: Number(roomInstanceId),
      blockType,
      startDate,
      endDate,
      reason: reason.trim(),
      notes: notes.trim() ? notes.trim() : undefined,
    };

    setIsLoading(true);
    try {
      await operationService.createMaintenanceBlock(payload);
      const roomNumber = selectedRoom ? selectedRoom.roomNumber : roomInstanceId;
      onSuccess(`Đã tạo lệnh khóa phòng P.${roomNumber} (${blockType}) thành công.`);
      onClose();
    } catch (err) {
      onError(err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Khóa phòng bảo trì (OOO / OOS)"
      description="Tạo lệnh tạm dừng khai thác phòng để phục vụ sửa chữa kỹ thuật hoặc vệ sinh chuyên sâu"
      maxWidth="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {formError && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-xs flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0 text-red-600" />
            <span>{formError}</span>
          </div>
        )}

        {/* Room selector */}
        <div>
          <label className="block text-xs font-semibold text-neutral-700 mb-1">
            Chọn phòng cần khóa <span className="text-red-500">*</span>
          </label>
          <select
            value={roomInstanceId}
            onChange={(e) => setRoomInstanceId(e.target.value ? Number(e.target.value) : '')}
            className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
            disabled={isLoading}
          >
            <option value="">-- Chọn phòng --</option>
            {rooms.map((r) => (
              <option key={r.roomInstanceId} value={r.roomInstanceId}>
                Phòng {r.roomNumber} - {r.roomTypeName || 'Tiêu chuẩn'} ({r.currentStatus})
              </option>
            ))}
          </select>
        </div>

        {/* Warning if room is occupied */}
        {isOccupied && (
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-xs flex items-start gap-2.5">
            <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Cảnh báo: Phòng đang có khách lưu trú!</p>
              <p className="mt-0.5 text-neutral-600">
                Khách {selectedRoom?.guestName ? `"${selectedRoom.guestName}"` : ''} đang ở phòng này. Nếu bạn khóa OOO trong thời gian khách ở, hệ thống sẽ từ chối để tránh tranh chấp slot.
              </p>
            </div>
          </div>
        )}

        {/* Block Type Selection */}
        <div>
          <label className="block text-xs font-semibold text-neutral-700 mb-1.5">
            Loại khóa bảo trì <span className="text-red-500">*</span>
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div
              onClick={() => setBlockType('OOO')}
              className={`cursor-pointer p-3.5 rounded-xl border transition-all ${
                blockType === 'OOO'
                  ? 'border-red-600 bg-red-50/50 shadow-xs ring-1 ring-red-600'
                  : 'border-neutral-200 bg-white hover:border-neutral-300'
              }`}
            >
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-red-600" />
                <span className="text-sm font-bold text-neutral-900">OOO (Out of Order)</span>
              </div>
              <p className="text-xs text-neutral-600 mt-1 leading-relaxed">
                Trừ phòng khỏi quỹ bán online. Chặn gán phòng và chuyển slot sang BLOCKED. Dành cho sửa chữa lớn.
              </p>
            </div>

            <div
              onClick={() => setBlockType('OOS')}
              className={`cursor-pointer p-3.5 rounded-xl border transition-all ${
                blockType === 'OOS'
                  ? 'border-amber-600 bg-amber-50/50 shadow-xs ring-1 ring-amber-600'
                  : 'border-neutral-200 bg-white hover:border-neutral-300'
              }`}
            >
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-amber-600" />
                <span className="text-sm font-bold text-neutral-900">OOS (Out of Service)</span>
              </div>
              <p className="text-xs text-neutral-600 mt-1 leading-relaxed">
                Giữ nguyên quỹ phòng bán. Chỉ chặn gán phòng và check-in vật lý. Dành cho bảo dưỡng nhỏ, xử lý nhanh.
              </p>
            </div>
          </div>
        </div>

        {/* Date Range */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1 flex items-center gap-1">
              <Calendar className="w-3.5 h-3.5 text-neutral-500" />
              Ngày bắt đầu <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              disabled={isLoading}
              required
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1 flex items-center gap-1">
              <Calendar className="w-3.5 h-3.5 text-neutral-500" />
              Ngày kết thúc <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              min={startDate}
              className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              disabled={isLoading}
              required
            />
          </div>
        </div>

        {/* Reason */}
        <div>
          <label className="block text-xs font-semibold text-neutral-700 mb-1 flex items-center gap-1">
            <Wrench className="w-3.5 h-3.5 text-neutral-500" />
            Lý do khóa phòng <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Ví dụ: Hỏng điều hòa nhiệt độ, rò rỉ nước phòng tắm..."
            className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
            disabled={isLoading}
            required
          />
        </div>

        {/* Notes */}
        <div>
          <label className="block text-xs font-semibold text-neutral-700 mb-1 flex items-center gap-1">
            <FileText className="w-3.5 h-3.5 text-neutral-500" />
            Ghi chú kỹ thuật bổ sung
          </label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Ghi chú thêm về đơn vị phụ trách, linh kiện thay thế (nếu có)..."
            rows={2}
            className="w-full p-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white resize-none"
            disabled={isLoading}
          />
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-3 pt-3 border-t border-neutral-100">
          <Button type="button" variant="secondary" onClick={onClose} disabled={isLoading}>
            Hủy bỏ
          </Button>
          <Button type="submit" variant="primary" isLoading={isLoading} className="bg-red-600 hover:bg-red-700">
            Xác nhận khóa phòng
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default MaintenanceBlockFormModal;
