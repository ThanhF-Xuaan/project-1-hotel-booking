import React, { useState } from 'react';
import { AlertTriangle, Check, ShieldAlert, Edit3 } from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import type { SurchargeProposalResponse } from '../types/booking.types';

export interface SurchargeProposalModalProps {
  isOpen: boolean;
  onClose: () => void;
  proposal: SurchargeProposalResponse | null;
  actionType: 'CHECK_IN' | 'CHECK_OUT';
  roomNumber?: string;
  onApplyProposal: () => Promise<void>;
  onWaiveSurcharge: (reason: string) => Promise<void>;
  onCustomAdjust: () => void;
  isProcessing: boolean;
}

export const SurchargeProposalModal: React.FC<SurchargeProposalModalProps> = ({
  isOpen,
  onClose,
  proposal,
  actionType,
  roomNumber,
  onApplyProposal,
  onWaiveSurcharge,
  onCustomAdjust,
  isProcessing,
}) => {
  const [isWaiveMode, setIsWaiveMode] = useState(false);
  const [waiveReason, setWaiveReason] = useState('Khách VIP / Phê duyệt lãnh đạo');

  if (!proposal) return null;

  const actionTitle = actionType === 'CHECK_IN' ? 'Nhận phòng sớm (Early Check-in)' : 'Trả phòng muộn (Late Check-out)';
  const formatVND = (amount?: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Đề xuất phụ thu: ${actionTitle}`}
      maxWidth="lg"
    >
      <div className="space-y-4">
        {/* Banner notification */}
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="text-sm">
            <p className="font-semibold text-amber-900">
              Phát hiện mốc thời gian phát sinh phụ thu {roomNumber ? `cho Phòng ${roomNumber}` : ''}
            </p>
            <p className="text-amber-800 text-xs mt-1">
              Hệ thống tự động đối chiếu chính sách phụ thu của khách sạn và đề xuất mức thu dưới đây. Lễ tân có toàn quyền xác nhận áp dụng, miễn trừ hoặc điều chỉnh mức thu theo thẩm quyền.
            </p>
          </div>
        </div>

        {/* Breakdown Card */}
        <div className="bg-neutral-50 rounded-xl border border-neutral-200 p-4 space-y-2.5 text-sm">
          <div className="flex justify-between items-center py-1 border-b border-neutral-200">
            <span className="text-neutral-500 text-xs">Chính sách áp dụng:</span>
            <span className="font-medium text-neutral-800 text-xs px-2 py-0.5 bg-neutral-200 rounded">
              {proposal.ruleCode || 'SURCHARGE_RULE'}
            </span>
          </div>

          <div className="flex justify-between items-start py-1 border-b border-neutral-200">
            <span className="text-neutral-500 text-xs">Mô tả quy định:</span>
            <span className="font-medium text-neutral-900 text-right text-xs max-w-[260px]">
              {proposal.description || 'Theo biểu phí phụ thu của khách sạn'}
            </span>
          </div>

          <div className="flex justify-between items-center py-1 border-b border-neutral-200">
            <span className="text-neutral-500 text-xs">Tỷ lệ phụ thu:</span>
            <span className="font-semibold text-neutral-900 text-xs">
              {proposal.percentage}% giá phòng 1 đêm
            </span>
          </div>

          <div className="flex justify-between items-center py-1 border-b border-neutral-200">
            <span className="text-neutral-500 text-xs">Giá phòng 1 đêm căn cứ:</span>
            <span className="font-medium text-neutral-800 text-xs">
              {formatVND(proposal.baseAmount)}
            </span>
          </div>

          <div className="flex justify-between items-center pt-2">
            <span className="text-sm font-bold text-neutral-900">Phụ thu đề xuất:</span>
            <span className="text-lg font-extrabold text-red-600">
              {formatVND(proposal.suggestedAmount)}
            </span>
          </div>
        </div>

        {/* Waive reason input if in waive mode */}
        {isWaiveMode ? (
          <div className="space-y-3 pt-2 border-t border-neutral-100">
            <label className="block text-xs font-semibold text-neutral-700">
              Lý do miễn phụ thu (0đ) / Ghi chú thẩm quyền:
            </label>
            <Input
              value={waiveReason}
              onChange={(e) => setWaiveReason(e.target.value)}
              placeholder="Nhập lý do miễn (VD: Khách VIP, Giám đốc ký duyệt...)"
            />
            <div className="flex flex-wrap gap-1.5 text-xs">
              {['Khách VIP chuỗi', 'Ban giám đốc duyệt', 'Lỗi khách quan khách sạn', 'Bù trừ khuyến mãi'].map((preset) => (
                <button
                  key={preset}
                  type="button"
                  onClick={() => setWaiveReason(preset)}
                  className="px-2.5 py-1 bg-neutral-100 hover:bg-neutral-200 rounded-lg text-neutral-700 transition"
                >
                  {preset}
                </button>
              ))}
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setIsWaiveMode(false)}
                disabled={isProcessing}
              >
                Quay lại
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={() => onWaiveSurcharge(waiveReason)}
                disabled={isProcessing}
                className="bg-neutral-800 hover:bg-neutral-900"
              >
                {isProcessing ? 'Đang xử lý...' : 'Xác nhận miễn phụ thu & Tiếp tục'}
              </Button>
            </div>
          </div>
        ) : (
          /* Normal Action Buttons */
          <div className="space-y-2 pt-2">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <Button
                variant="primary"
                onClick={onApplyProposal}
                disabled={isProcessing}
                className="w-full justify-center"
              >
                <Check className="w-4 h-4 mr-1.5" />
                {isProcessing ? 'Đang ghi nhận...' : 'Áp dụng đề xuất'}
              </Button>

              <Button
                variant="outline"
                onClick={() => setIsWaiveMode(true)}
                disabled={isProcessing}
                className="w-full justify-center text-amber-700 border-amber-300 hover:bg-amber-50"
              >
                <ShieldAlert className="w-4 h-4 mr-1.5 text-amber-600" />
                Miễn phụ thu (0đ)
              </Button>
            </div>

            <div className="flex justify-between items-center pt-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={onCustomAdjust}
                disabled={isProcessing}
                className="text-neutral-600 hover:text-neutral-900"
              >
                <Edit3 className="w-3.5 h-3.5 mr-1 text-neutral-500" />
                Tự điều chỉnh số tiền...
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={onClose}
                disabled={isProcessing}
              >
                Đóng / Hủy
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default SurchargeProposalModal;
