import React, { useState, useEffect } from 'react';
import { AlertCircle } from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import policyService from '../services/policy.service';
import type {
  NoShowPolicyResponse,
  NoShowPenaltyType,
  BookingType,
} from '../types/policy.types';

interface NoShowPolicyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  policy: NoShowPolicyResponse | null;
}

export const NoShowPolicyModal: React.FC<NoShowPolicyModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  policy,
}) => {
  const isEdit = !!policy;
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [policyCode, setPolicyCode] = useState('');
  const [name, setName] = useState('');
  const [bookingType, setBookingType] = useState<BookingType>('ALL');
  const [releaseRoomTime, setReleaseRoomTime] = useState('23:59');
  const [penaltyType, setPenaltyType] = useState<NoShowPenaltyType>('PERCENT_FIRST_NIGHT_RATE');
  const [penaltyValue, setPenaltyValue] = useState(100);
  const [description, setDescription] = useState('');
  const [isActive, setIsActive] = useState(true);

  useEffect(() => {
    if (policy) {
      setPolicyCode(policy.policyCode);
      setName(policy.name);
      setBookingType(policy.bookingType);
      setReleaseRoomTime(policy.releaseRoomTime ? policy.releaseRoomTime.substring(0, 5) : '23:59');
      setPenaltyType(policy.penaltyType);
      setPenaltyValue(policy.penaltyValue);
      setDescription(policy.description || '');
      setIsActive(policy.isActive);
    } else {
      setPolicyCode('');
      setName('');
      setBookingType('ALL');
      setReleaseRoomTime('23:59');
      setPenaltyType('PERCENT_FIRST_NIGHT_RATE');
      setPenaltyValue(100);
      setDescription('');
      setIsActive(true);
    }
    setErrorMsg(null);
  }, [policy, isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setErrorMsg('Vui lòng nhập tên chính sách');
      return;
    }
    if (!isEdit && !policyCode.trim()) {
      setErrorMsg('Vui lòng nhập mã chính sách');
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMsg(null);

      const formattedTime = releaseRoomTime.length === 5 ? `${releaseRoomTime}:00` : releaseRoomTime;

      if (isEdit && policy) {
        await policyService.updateNoShowPolicy(policy.id, {
          name,
          bookingType,
          releaseRoomTime: formattedTime,
          penaltyType,
          penaltyValue: Number(penaltyValue),
          description,
          isActive,
        });
      } else {
        await policyService.createNoShowPolicy({
          policyCode: policyCode.trim().toUpperCase(),
          name,
          bookingType,
          releaseRoomTime: formattedTime,
          penaltyType,
          penaltyValue: Number(penaltyValue),
          description,
          isActive,
        });
      }

      onSuccess();
      onClose();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Có lỗi xảy ra khi lưu chính sách';
      setErrorMsg(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEdit ? `Cập nhật chính sách: ${policy?.policyCode}` : 'Tạo mới Chính sách Vắng mặt (No-Show)'}
      description="Chính sách quy định giờ giải phóng phòng và mức phạt khi khách vắng mặt đêm đầu"
      maxWidth="2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {errorMsg && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">
              Mã Chính Sách <span className="text-red-500">*</span>
            </label>
            <Input
              value={policyCode}
              onChange={(e) => setPolicyCode(e.target.value)}
              placeholder="VD: NOSHOW_STANDARD"
              disabled={isEdit}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">
              Tên Chính Sách <span className="text-red-500">*</span>
            </label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="VD: Chính sách vắng mặt tiêu chuẩn toàn chuỗi"
              required
            />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">Loại Đối Tượng</label>
            <select
              value={bookingType}
              onChange={(e) => setBookingType(e.target.value as BookingType)}
              className="w-full h-10 md:h-12 px-3 border border-neutral-300 rounded-xl bg-white text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
            >
              <option value="ALL">Toàn bộ khách (ALL)</option>
              <option value="FIT">Khách lẻ (FIT)</option>
              <option value="GIT">Khách đoàn (GIT)</option>
              <option value="CORPORATE">Doanh nghiệp (CORPORATE)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">Hình Thức Phạt No-Show</label>
            <select
              value={penaltyType}
              onChange={(e) => setPenaltyType(e.target.value as NoShowPenaltyType)}
              className="w-full h-10 md:h-12 px-3 border border-neutral-300 rounded-xl bg-white text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
            >
              <option value="PERCENT_FIRST_NIGHT_RATE">% Giá phòng đêm đầu (Mặc định)</option>
              <option value="PERCENT_TOTAL_BOOKING">% Tổng giá trị đơn đặt</option>
              <option value="FIXED_AMOUNT">Số tiền cố định (VND)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">
              {penaltyType === 'FIXED_AMOUNT' ? 'Số tiền phạt (VNĐ)' : 'Mức phạt (%)'}
            </label>
            <Input
              type="number"
              min={0}
              max={penaltyType !== 'FIXED_AMOUNT' ? 100 : undefined}
              step={penaltyType !== 'FIXED_AMOUNT' ? '0.01' : '1000'}
              value={penaltyValue}
              onChange={(e) => setPenaltyValue(Number(e.target.value))}
              required
            />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">Giờ Hủy / Release Phòng</label>
            <Input
              type="time"
              value={releaseRoomTime}
              onChange={(e) => setReleaseRoomTime(e.target.value)}
              required
            />
            <p className="text-xs text-neutral-500 mt-1">
              Thời điểm hệ thống Night Audit tự động hủy và tính phạt nếu khách chưa nhận phòng.
            </p>
          </div>

          <div className="flex items-center gap-2 pt-6">
            <input
              type="checkbox"
              id="noshow-active"
              checked={isActive}
              onChange={(e) => setIsActive(e.target.checked)}
              className="w-4 h-4 text-red-600 rounded border-neutral-300 focus:ring-red-500"
            />
            <label htmlFor="noshow-active" className="text-sm font-medium text-neutral-700 cursor-pointer">
              Kích hoạt chính sách này
            </label>
          </div>
        </div>

        <div>
          <label className="block text-sm font-semibold text-neutral-800 mb-1">Mô Tả Chi Tiết</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
            className="w-full p-3 border border-neutral-300 rounded-xl text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
            placeholder="Ghi chú chi tiết về quy định vắng mặt không nhận phòng..."
          />
        </div>

        <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
          <Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" isLoading={isSubmitting}>
            {isEdit ? 'Lưu thay đổi' : 'Tạo chính sách'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default NoShowPolicyModal;
