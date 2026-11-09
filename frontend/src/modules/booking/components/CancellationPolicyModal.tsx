import React, { useState, useEffect } from 'react';
import { Plus, Trash2, AlertCircle } from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { Select } from '../../../core/components/ui/Select';
import policyService from '../services/policy.service';
import type {
  CancellationPolicyResponse,
  CancellationFeeType,
  BookingType,
  CancellationTierDto,
} from '../types/policy.types';

interface CancellationPolicyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  policy: CancellationPolicyResponse | null;
}

export const CancellationPolicyModal: React.FC<CancellationPolicyModalProps> = ({
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
  const [bookingType, setBookingType] = useState<BookingType>('FIT');
  const [cancellationFeeType, setCancellationFeeType] = useState<CancellationFeeType>('TIERED');
  const [freeCancellationBeforeDays, setFreeCancellationBeforeDays] = useState(7);
  const [defaultPenaltyPercent, setDefaultPenaltyPercent] = useState(100);
  const [description, setDescription] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [tiers, setTiers] = useState<CancellationTierDto[]>([]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (policy) {
        setPolicyCode(policy.policyCode);
        setName(policy.name);
        setBookingType(policy.bookingType);
        setCancellationFeeType(policy.cancellationFeeType);
        setFreeCancellationBeforeDays(policy.freeCancellationBeforeDays);
        setDefaultPenaltyPercent(policy.defaultPenaltyPercent);
        setDescription(policy.description || '');
        setIsActive(policy.isActive);
        setTiers(policy.tiers && policy.tiers.length > 0 ? [...policy.tiers].sort((a, b) => Number(a.minDays) - Number(b.minDays)) : []);
      } else {
        setPolicyCode('');
        setName('');
        setBookingType('FIT');
        setCancellationFeeType('TIERED');
        setFreeCancellationBeforeDays(7);
        setDefaultPenaltyPercent(100);
        setDescription('');
        setIsActive(true);
        setTiers([
          { minDays: 0, maxDays: 3, penaltyPercent: 100, description: 'Hủy trong vòng 3 ngày: Phạt 100% tiền phòng' },
          { minDays: 4, maxDays: 6, penaltyPercent: 50, description: 'Hủy từ 4-6 ngày: Phạt 50% tiền phòng' },
          { minDays: 7, maxDays: null, penaltyPercent: 0, description: 'Hủy trước >= 7 ngày: Miễn phí hủy (0%)' },
        ]);
      }
      setErrorMsg(null);
    }, 0);
    return () => clearTimeout(timer);
  }, [policy, isOpen]);

  const handleAddTier = () => {
    if (tiers.length === 0) {
      setTiers([{ minDays: 0, maxDays: null, penaltyPercent: 100, description: '' }]);
      return;
    }
    const updated = [...tiers];
    const last = updated[updated.length - 1];
    if (last.maxDays === null || last.maxDays === undefined || last.maxDays === ('' as unknown)) {
      last.maxDays = Number(last.minDays) + 3;
    }
    const nextMin = Number(last.maxDays) + 1;
    updated.push({ minDays: nextMin, maxDays: null, penaltyPercent: 0, description: '' });
    setTiers(updated);
  };

  const handleRemoveTier = (index: number) => {
    setTiers(tiers.filter((_, idx) => idx !== index));
  };

  const handleUpdateTier = (index: number, field: keyof CancellationTierDto, value: unknown) => {
    const updated = [...tiers];
    updated[index] = { ...updated[index], [field]: value };
    setTiers(updated);
  };

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

    // Client-side validation cho bậc phạt nếu là TIERED hoặc FULL_DEPOSIT
    if (cancellationFeeType === 'TIERED' || cancellationFeeType === 'FULL_DEPOSIT') {
      if (!tiers || tiers.length === 0) {
        setErrorMsg('Vui lòng thiết lập ít nhất 1 bậc phạt');
        return;
      }
      const sorted = [...tiers].sort((a, b) => Number(a.minDays) - Number(b.minDays));
      if (Number(sorted[0].minDays) !== 0) {
        setErrorMsg('Bậc phạt đầu tiên bắt buộc phải bắt đầu từ ngày 0');
        return;
      }
      for (let i = 0; i < sorted.length; i++) {
        const cur = sorted[i];
        if (
          cur.penaltyPercent === undefined ||
          cur.penaltyPercent === null ||
          Number(cur.penaltyPercent) < 0 ||
          Number(cur.penaltyPercent) > 100
        ) {
          setErrorMsg('Tỷ lệ phạt phải nằm trong khoảng từ 0% đến 100%');
          return;
        }
        if (i < sorted.length - 1) {
          if (
            cur.maxDays === null ||
            cur.maxDays === undefined ||
            cur.maxDays === ('' as unknown) ||
            Number(cur.maxDays) < Number(cur.minDays)
          ) {
            setErrorMsg('Các bậc phạt ở giữa bắt buộc phải có mốc Đến (<= ngày) và lớn hơn hoặc bằng mốc Từ');
            return;
          }
          const next = sorted[i + 1];
          if (Number(next.minDays) !== Number(cur.maxDays) + 1) {
            setErrorMsg(`Các bậc phạt không liên tục: bậc tiếp theo (${next.minDays}) phải bằng bậc trước (${cur.maxDays}) + 1`);
            return;
          }
          if (Number(next.penaltyPercent) > Number(cur.penaltyPercent)) {
            setErrorMsg('Mức phạt phải giảm dần hoặc bằng khi thời gian hủy cách xa ngày check-in hơn');
            return;
          }
        } else {
          if (
            cur.maxDays !== null &&
            cur.maxDays !== undefined &&
            cur.maxDays !== ('' as unknown) &&
            Number(cur.maxDays) < Number(cur.minDays)
          ) {
            setErrorMsg('Mốc Đến (<= ngày) của bậc cuối cùng phải lớn hơn hoặc bằng mốc Từ');
            return;
          }
        }
      }
    }

    try {
      setIsSubmitting(true);
      setErrorMsg(null);

      const formattedTiers = tiers.map((t) => ({
        ...t,
        minDays: Number(t.minDays),
        maxDays: t.maxDays !== null && t.maxDays !== undefined && t.maxDays !== ('' as unknown) ? Number(t.maxDays) : null,
        penaltyPercent: Number(t.penaltyPercent),
      }));

      if (isEdit && policy) {
        await policyService.updateCancellationPolicy(policy.id, {
          name,
          bookingType,
          cancellationFeeType,
          freeCancellationBeforeDays: Number(freeCancellationBeforeDays),
          defaultPenaltyPercent: Number(defaultPenaltyPercent),
          description,
          isActive,
          tiers: cancellationFeeType === 'TIERED' || cancellationFeeType === 'FULL_DEPOSIT' ? formattedTiers : [],
        });
      } else {
        await policyService.createCancellationPolicy({
          policyCode: policyCode.trim().toUpperCase(),
          name,
          bookingType,
          cancellationFeeType,
          freeCancellationBeforeDays: Number(freeCancellationBeforeDays),
          defaultPenaltyPercent: Number(defaultPenaltyPercent),
          description,
          isActive,
          tiers: cancellationFeeType === 'TIERED' || cancellationFeeType === 'FULL_DEPOSIT' ? formattedTiers : [],
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
      title={isEdit ? `Cập nhật chính sách: ${policy?.policyCode}` : 'Tạo mới Chính sách Hủy phòng'}
      description="Chính sách hủy phòng cấp chuỗi quy định các mốc ngày và tỷ lệ phạt áp dụng toàn hệ thống"
      maxWidth="4xl"
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
              placeholder="VD: CANCEL_FIT_STANDARD"
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
              placeholder="VD: Chính sách hủy tiêu chuẩn khách lẻ FIT"
              required
            />
          </div>
        </div>

        <div className={`grid grid-cols-1 ${cancellationFeeType !== 'TIERED' ? 'md:grid-cols-4' : 'md:grid-cols-3'} gap-4`}>
          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">Loại Đối Tượng</label>
            <Select<BookingType>
              value={bookingType}
              onChange={(val) => setBookingType(val)}
              options={[
                { value: 'FIT', label: 'Khách lẻ (FIT)' },
                { value: 'GIT', label: 'Khách đoàn (GIT)' },
                { value: 'CORPORATE', label: 'Doanh nghiệp (CORPORATE)' },
                { value: 'ALL', label: 'Toàn bộ khách (ALL)' },
              ]}
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">Loại Cơ Chế Phạt</label>
            <Select<CancellationFeeType>
              value={cancellationFeeType}
              onChange={(val) => setCancellationFeeType(val)}
              options={[
                { value: 'TIERED', label: 'Bậc thang thời gian (TIERED)' },
                { value: 'FULL_DEPOSIT', label: 'Tỷ lệ trên tiền cọc thực tế (FULL_DEPOSIT)' },
                { value: 'PERCENT', label: 'Tỷ lệ % cố định (PERCENT)' },
                { value: 'FIXED_AMOUNT', label: 'Số tiền cố định (FIXED_AMOUNT)' },
                { value: 'FIRST_NIGHT', label: 'Phạt đêm đầu tiên (FIRST_NIGHT)' },
              ]}
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-neutral-800 mb-1">Miễn phí hủy trước (ngày)</label>
            <Input
              type="number"
              min={0}
              value={freeCancellationBeforeDays}
              onChange={(e) => setFreeCancellationBeforeDays(Number(e.target.value))}
              disabled={cancellationFeeType === 'TIERED'}
              title={cancellationFeeType === 'TIERED' ? 'TIERED sử dụng các bậc bên dưới làm nguồn sự thật' : ''}
            />
          </div>

          {cancellationFeeType !== 'TIERED' && (
            <div>
              <label className="block text-sm font-semibold text-neutral-800 mb-1">
                Tỷ lệ phạt mặc định (%) <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min={0}
                max={100}
                step="0.01"
                value={defaultPenaltyPercent}
                onChange={(e) => setDefaultPenaltyPercent(Number(e.target.value))}
                required
              />
            </div>
          )}
        </div>

        {(cancellationFeeType === 'TIERED' || cancellationFeeType === 'FULL_DEPOSIT') && (
          <div className="mt-4 border border-neutral-200 rounded-xl p-4 bg-neutral-50/50">
            <div className="flex items-center justify-between mb-3">
              <div>
                <h4 className="text-sm font-bold text-neutral-900">
                  {cancellationFeeType === 'FULL_DEPOSIT'
                    ? 'Bậc phạt theo timeline đặt cọc (0% = hoàn cọc, 100% = mất toàn bộ tiền cọc)'
                    : 'Cấu hình các bậc phạt theo ngày (Tiers)'}
                </h4>
                <p className="text-xs text-neutral-500">
                  Quy định mốc số ngày trước check-in và % phạt tương ứng.
                </p>
              </div>
              <Button type="button" size="sm" variant="outline" leftIcon={<Plus className="w-4 h-4" />} onClick={handleAddTier}>
                Thêm bậc
              </Button>
            </div>

            <div className="space-y-2">
              {tiers.map((tier, idx) => (
                <div key={idx} className="flex flex-wrap items-center gap-2 p-2 bg-white rounded-lg border border-neutral-200">
                  <div className="w-24">
                    <label className="text-xs text-neutral-500">Từ (≥ ngày)</label>
                    <input
                      type="number"
                      min={0}
                      value={tier.minDays}
                      onChange={(e) => handleUpdateTier(idx, 'minDays', Number(e.target.value))}
                      className="w-full h-9 px-2 border border-neutral-300 rounded-lg text-sm"
                      required
                    />
                  </div>

                  <div className="w-24">
                    <label className="text-xs text-neutral-500">Đến (≤ ngày)</label>
                    <input
                      type="number"
                      min={0}
                      value={tier.maxDays ?? ''}
                      placeholder="∞ (Không giới hạn)"
                      onChange={(e) => handleUpdateTier(idx, 'maxDays', e.target.value === '' ? null : Number(e.target.value))}
                      className="w-full h-9 px-2 border border-neutral-300 rounded-lg text-sm"
                    />
                  </div>

                  <div className="w-28">
                    <label className="text-xs text-neutral-500">Phạt (% tiền)</label>
                    <input
                      type="number"
                      min={0}
                      max={100}
                      step="0.01"
                      value={tier.penaltyPercent}
                      onChange={(e) => handleUpdateTier(idx, 'penaltyPercent', Number(e.target.value))}
                      className="w-full h-9 px-2 border border-neutral-300 rounded-lg text-sm font-semibold text-red-600"
                      required
                    />
                  </div>

                  <div className="flex-1 min-w-[140px]">
                    <label className="text-xs text-neutral-500">Mô tả hiển thị</label>
                    <input
                      type="text"
                      value={tier.description || ''}
                      placeholder="VD: Miễn phí phạt"
                      onChange={(e) => handleUpdateTier(idx, 'description', e.target.value)}
                      className="w-full h-9 px-2 border border-neutral-300 rounded-lg text-sm"
                    />
                  </div>

                  <button
                    type="button"
                    onClick={() => handleRemoveTier(idx)}
                    className="p-2 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg mt-4 cursor-pointer"
                    title="Xóa bậc này"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-neutral-800 mb-1">Mô Tả Chi Tiết</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
            className="w-full p-3 border border-neutral-300 rounded-xl text-sm focus:ring-2 focus:ring-red-600 focus:outline-none"
            placeholder="Ghi chú chi tiết về các điều khoản phạt hủy..."
          />
        </div>

        <div className="flex items-center gap-2 pt-2">
          <input
            type="checkbox"
            id="cancel-active"
            checked={isActive}
            onChange={(e) => setIsActive(e.target.checked)}
            className="w-4 h-4 text-red-600 rounded border-neutral-300 focus:ring-red-500"
          />
          <label htmlFor="cancel-active" className="text-sm font-medium text-neutral-700 cursor-pointer">
            Kích hoạt chính sách này
          </label>
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

export default CancellationPolicyModal;
