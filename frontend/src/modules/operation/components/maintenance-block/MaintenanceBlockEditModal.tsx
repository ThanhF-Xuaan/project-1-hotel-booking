import React, { useState, useEffect } from 'react';
import { Calendar, Wrench, FileText, AlertTriangle, CalendarPlus } from 'lucide-react';
import Modal from '../../../../core/components/ui/Modal';
import Button from '../../../../core/components/ui/Button';
import { operationService } from '../../services/operation.service';
import type {
  MaintenanceBlockDto,
  MaintenanceBlockType,
  UpdateMaintenanceBlockRequest,
} from '../../types/operation.types';

export interface MaintenanceBlockEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (message: string) => void;
  onError: (err: unknown) => void;
  block: MaintenanceBlockDto | null;
}

export const MaintenanceBlockEditModal: React.FC<MaintenanceBlockEditModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  onError,
  block,
}) => {
  const [blockType, setBlockType] = useState<MaintenanceBlockType>('OOO');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [reason, setReason] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (block && isOpen) {
      setBlockType(block.blockType);
      setStartDate(block.startDate || '');
      setEndDate(block.endDate || '');
      setReason(block.reason || '');
      setNotes(block.notes || '');
      setFormError(null);
    }
  }, [block, isOpen]);

  if (!block) return null;

  // Tính năng gia hạn nhanh (+1 ngày, +3 ngày, +7 ngày)
  const handleExtendDays = (days: number) => {
    const base = endDate ? new Date(endDate) : new Date();
    base.setDate(base.getDate() + days);
    const newEnd = base.toISOString().split('T')[0];
    setEndDate(newEnd);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!startDate || !endDate) {
      setFormError('Vui lòng chọn ngày bắt đầu và ngày kết thúc');
      return;
    }

    if (startDate > endDate) {
      setFormError('Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu');
      return;
    }

    if (!reason.trim()) {
      setFormError('Vui lòng nhập lý do khóa phòng');
      return;
    }

    setIsLoading(true);
    try {
      const payload: UpdateMaintenanceBlockRequest = {
        blockType,
        startDate,
        endDate,
        reason: reason.trim(),
        notes: notes.trim() || undefined,
      };

      await operationService.updateMaintenanceBlock(block.id, payload);
      onSuccess(`Đã cập nhật & gia hạn lệnh khóa phòng P.${block.roomNumber} thành công!`);
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
      title="Gia hạn & Chỉnh sửa lệnh khóa phòng"
      description="Điều chỉnh loại khóa, cập nhật thời gian hiệu lực hoặc gia hạn ngày kết thúc bảo trì"
      maxWidth="2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Banner thông tin phòng gọn trên 1 dòng */}
        <div className="px-3.5 py-2 bg-neutral-50 rounded-xl border border-neutral-200 flex flex-wrap items-center justify-between gap-2 text-xs">
          <div className="flex items-center gap-2">
            <span className="font-bold text-neutral-900 text-sm">
              Phòng P.{block.roomNumber}
            </span>
            {block.roomTypeName && (
              <span className="text-neutral-500 font-medium">
                • {block.roomTypeName}
              </span>
            )}
            <span className="px-2 py-0.5 rounded-md text-[10px] font-mono font-bold bg-neutral-200 text-neutral-700">
              #{block.id}
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-neutral-500 text-[11px]">Trạng thái:</span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
              Đang hiệu lực
            </span>
          </div>
        </div>

        {formError && (
          <div className="p-2.5 bg-red-50 text-red-700 text-xs rounded-xl flex items-center gap-2 border border-red-200">
            <AlertTriangle className="w-4 h-4 shrink-0 text-red-600" />
            <span>{formError}</span>
          </div>
        )}

        {/* Bố cục 2 cột cân đối trên 1 khung hình */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-start">
          {/* CỘT TRÁI: Loại khóa & Thời gian hiệu lực */}
          <div className="space-y-3">
            {/* Loại khóa (Nút toggle dạng thẻ nhỏ gọn) */}
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1.5">
                Loại khóa bảo trì <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setBlockType('OOO')}
                  className={`p-2.5 rounded-xl border text-left transition-all cursor-pointer ${
                    blockType === 'OOO'
                      ? 'border-red-600 bg-red-50/60 ring-1 ring-red-600 text-red-900 shadow-xs'
                      : 'border-neutral-200 bg-white hover:border-neutral-300 text-neutral-700'
                  }`}
                >
                  <div className="flex items-center gap-1.5 font-bold text-xs">
                    <span className="w-2 h-2 rounded-full bg-red-600 shrink-0" />
                    OOO (Khóa bán)
                  </div>
                  <p className="text-[10px] text-neutral-500 mt-1 line-clamp-1">
                    Trừ khỏi quỹ phòng bán
                  </p>
                </button>

                <button
                  type="button"
                  onClick={() => setBlockType('OOS')}
                  className={`p-2.5 rounded-xl border text-left transition-all cursor-pointer ${
                    blockType === 'OOS'
                      ? 'border-amber-600 bg-amber-50/60 ring-1 ring-amber-600 text-amber-900 shadow-xs'
                      : 'border-neutral-200 bg-white hover:border-neutral-300 text-neutral-700'
                  }`}
                >
                  <div className="flex items-center gap-1.5 font-bold text-xs">
                    <span className="w-2 h-2 rounded-full bg-amber-600 shrink-0" />
                    OOS (Nội bộ)
                  </div>
                  <p className="text-[10px] text-neutral-500 mt-1 line-clamp-1">
                    Giữ nguyên quỹ phòng bán
                  </p>
                </button>
              </div>
            </div>

            {/* Thời gian hiệu lực: 2 ô ngày song song */}
            <div className="grid grid-cols-2 gap-2.5">
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1 flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                  Bắt đầu <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full h-9 px-2.5 border border-neutral-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white text-neutral-900"
                  disabled={isLoading}
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1 flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                  Kết thúc <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  min={startDate}
                  className="w-full h-9 px-2.5 border border-neutral-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white text-neutral-900"
                  disabled={isLoading}
                  required
                />
              </div>
            </div>

            {/* Nút gia hạn nhanh 1 chạm */}
            <div className="flex items-center justify-between p-2 bg-neutral-50 rounded-xl border border-neutral-200 text-xs">
              <span className="text-neutral-600 flex items-center gap-1 text-[11px] font-medium">
                <CalendarPlus className="w-3.5 h-3.5 text-blue-600" />
                Gia hạn thêm:
              </span>
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => handleExtendDays(1)}
                  className="px-2.5 py-1 text-[11px] font-semibold bg-white hover:bg-neutral-100 text-neutral-700 rounded-lg border border-neutral-200 transition-colors cursor-pointer shadow-xs"
                >
                  +1 ngày
                </button>
                <button
                  type="button"
                  onClick={() => handleExtendDays(3)}
                  className="px-2.5 py-1 text-[11px] font-semibold bg-white hover:bg-neutral-100 text-neutral-700 rounded-lg border border-neutral-200 transition-colors cursor-pointer shadow-xs"
                >
                  +3 ngày
                </button>
                <button
                  type="button"
                  onClick={() => handleExtendDays(7)}
                  className="px-2.5 py-1 text-[11px] font-semibold bg-white hover:bg-neutral-100 text-neutral-700 rounded-lg border border-neutral-200 transition-colors cursor-pointer shadow-xs"
                >
                  +7 ngày
                </button>
              </div>
            </div>
          </div>

          {/* CỘT PHẢI: Lý do & Ghi chú */}
          <div className="space-y-3">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1 flex items-center gap-1">
                <Wrench className="w-3.5 h-3.5 text-neutral-400" />
                Lý do khóa / bảo trì <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Nhập lý do khóa phòng..."
                className="w-full h-9 px-2.5 border border-neutral-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white text-neutral-900"
                disabled={isLoading}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1 flex items-center gap-1">
                <FileText className="w-3.5 h-3.5 text-neutral-400" />
                Ghi chú kỹ thuật bổ sung
              </label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Ghi chú thêm về đơn vị sửa chữa, tiến độ, linh kiện..."
                rows={4}
                className="w-full p-2.5 border border-neutral-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white resize-none text-neutral-900"
                disabled={isLoading}
              />
            </div>
          </div>
        </div>

        {/* Thanh nút hành động cuối form */}
        <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-neutral-100">
          <Button type="button" variant="secondary" onClick={onClose} disabled={isLoading} className="text-xs h-9 px-4">
            Hủy bỏ
          </Button>
          <Button type="submit" variant="primary" isLoading={isLoading} className="bg-red-600 hover:bg-red-700 text-white font-semibold text-xs h-9 px-5 shadow-sm">
            Lưu thay đổi / Gia hạn
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default MaintenanceBlockEditModal;
