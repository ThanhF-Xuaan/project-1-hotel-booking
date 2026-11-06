import React, { useState } from 'react';
import { X, ArrowRightLeft, DollarSign, FileText, AlertCircle, ShieldAlert } from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import { masterFolioService } from '../services/masterFolio.service';
import type {
  MasterFolioResponse,
  MasterFolioLineItemDto,
  ChargeCategory,
  RoutingTarget,
} from '../types/masterFolio.types';

interface RoutingAdjustmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  masterFolioId: number;
  item: MasterFolioLineItemDto | null;
  onSuccess: (updated: MasterFolioResponse) => void;
}

export const RoutingAdjustmentModal: React.FC<RoutingAdjustmentModalProps> = ({
  isOpen,
  onClose,
  masterFolioId,
  item,
  onSuccess,
}) => {
  const currentTarget: RoutingTarget = item?.routingTarget || 'MASTER';
  const nextTarget: RoutingTarget = currentTarget === 'MASTER' ? 'SUB_FOLIO' : 'MASTER';

  const [adjustedTarget, setAdjustedTarget] = useState<RoutingTarget>(nextTarget);
  const [chargeCategory, setChargeCategory] = useState<ChargeCategory>(
    item?.chargeCategory || 'ROOM_RATE'
  );
  const [amount, setAmount] = useState<number>(item?.totalAmount || 0);
  const [reason, setReason] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !item) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!reason.trim()) {
      setError('Bắt buộc phải nhập lý do giải trình điều chuyển theo quy định Luật Kế toán 2015');
      return;
    }

    if (!amount || amount <= 0) {
      setError('Số tiền điều chuyển phải lớn hơn 0');
      return;
    }

    try {
      setIsSubmitting(true);
      const res = await masterFolioService.adjustRouting(masterFolioId, {
        referenceId: item.referenceId,
        itemType: item.itemType,
        chargeCategory,
        itemDescription: item.description,
        adjustedTarget,
        amount,
        adjustmentReason: reason.trim(),
      });

      if (res.result) {
        onSuccess(res.result);
      }
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data
          ?.message ||
        (err as { message?: string })?.message ||
        'Có lỗi xảy ra khi điều chuyển khoản mục chi phí';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
      <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-neutral-200 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="bg-neutral-900 text-white px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-red-600 flex items-center justify-center text-white">
              <ArrowRightLeft className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold">Điều Chuyển Khoản Mục Chi Phí</h3>
              <p className="text-xs text-neutral-400">
                Chuyển định tuyến giữa Master Folio và Sub-Folio khách lẻ
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng modal"
            className="text-neutral-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Item Summary Card */}
          <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 space-y-1 text-xs">
            <div className="font-bold text-neutral-900">{item.description}</div>
            <div className="flex items-center justify-between text-neutral-500">
              <span>Phòng: <strong className="text-neutral-800">{item.roomNumber || 'Chưa gán'}</strong></span>
              <span>Khách: <strong className="text-neutral-800">{item.guestName || 'Khách đoàn'}</strong></span>
              <span>
                Thành tiền:{' '}
                <strong className="text-red-600">
                  {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(
                    item.totalAmount
                  )}
                </strong>
              </span>
            </div>
          </div>

          {/* Routing Target Selector using Custom Select */}
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
              Chuyển mục tiêu sang *
            </label>
            <Select<RoutingTarget>
              value={adjustedTarget}
              onChange={(val) => setAdjustedTarget(val)}
              options={[
                {
                  value: 'SUB_FOLIO',
                  label: 'SUB_FOLIO (Khách cá nhân tự chi trả khi Check-out)',
                  description: 'Chi phí ngoài tiêu chuẩn hoặc vượt mức định mức hợp đồng đoàn',
                },
                {
                  value: 'MASTER',
                  label: 'MASTER (Doanh nghiệp đối tác chi trả toàn bộ)',
                  description: 'Gộp vào Sổ cái tổng hợp quyết toán hóa đơn GTGT',
                },
              ]}
            />
          </div>

          {/* Charge Category Selector using Custom Select */}
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
              Phân loại danh mục chi phí *
            </label>
            <Select<ChargeCategory>
              value={chargeCategory}
              onChange={(val) => setChargeCategory(val)}
              options={[
                { value: 'ROOM_RATE', label: 'Tiền phòng theo hợp đồng (ROOM_RATE)' },
                { value: 'BREAKFAST', label: 'Bữa sáng & Buffet (BREAKFAST)' },
                { value: 'BANQUET', label: 'Tiệc & Sự kiện hội nghị (BANQUET)' },
                { value: 'MINIBAR', label: 'Đồ uống Minibar (MINIBAR)' },
                { value: 'LAUNDRY', label: 'Dịch vụ giặt ủi (LAUNDRY)' },
                { value: 'CUSTOM', label: 'Dịch vụ phụ trợ tùy chỉnh (CUSTOM)' },
              ]}
            />
          </div>

          {/* Amount */}
          <Input
            label="Số tiền điều chuyển (VNĐ) *"
            type="number"
            min="1000"
            step="1000"
            value={amount}
            onChange={(e) => setAmount(Math.max(0, Number(e.target.value) || 0))}
            leftIcon={<DollarSign className="w-4 h-4 text-neutral-400" />}
            required
          />

          {/* Adjustment Reason (Mandatory per Accounting Law 2015) */}
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase mb-1 flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5 text-neutral-400" />
              Lý do điều chuyển (Bắt buộc theo Luật Kế toán 2015) *
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="VD: Khách sử dụng dịch vụ vượt hạn mức công ty / Công ty duyệt gộp chi phí tiệc vào Master Folio..."
              rows={3}
              required
              className="w-full text-xs p-3 rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-600 transition-all"
            />
          </div>

          <div className="p-2.5 bg-amber-50 rounded-xl border border-amber-200 text-[11px] text-amber-800 flex items-start gap-2">
            <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
            <span>
              Mọi thao tác điều chuyển chi phí sẽ được lưu vết kiểm toán (Audit Trail) cùng tên nhân viên thực hiện và lý do giải trình.
            </span>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-neutral-100">
            <Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Hủy bỏ
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              Xác nhận điều chuyển
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RoutingAdjustmentModal;

