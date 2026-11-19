import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  DollarSign,
  AlertTriangle,
  Clock,
  Sparkles,
  Layers,
  Loader2,
  Building2,
  Tag,
  Info,
  AlertCircle,
} from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select, { type SelectOption } from '../../../core/components/ui/Select';
import { bookingService } from '../services/booking.service';
import { surchargeRuleService } from '../../pricing/services/surchargeRule.service';
import type {
  BookingChargeType,
  BookingRoomResponse,
  BookingDetailResponse,
  SurchargeProposalResponse,
} from '../types/booking.types';
import type { SurchargeRuleDto } from '../../pricing/types/pricing.types';

export interface BookingAddChargeModalProps {
  isOpen: boolean;
  onClose: () => void;
  bookingId?: number;
  bookingNumber?: string;
  hotelId?: number;
  room: BookingRoomResponse | null;
  detail?: BookingDetailResponse | null;
  guestName?: string;
  initialChargeType?: BookingChargeType;
  initialProposal?: SurchargeProposalResponse | null;
  onChargeSuccess?: (msg?: string) => void;
}

const PENALTY_PRESETS = [
  { name: 'Vỡ ly / cốc sứ', price: 50000 },
  { name: 'Mất thẻ từ khóa phòng', price: 100000 },
  { name: 'Hỏng điều khiển TV / Điều hòa', price: 150000 },
  { name: 'Làm ố bẩn ga / drap trải giường', price: 200000 },
  { name: 'Hút thuốc trong phòng (Phạt nội quy)', price: 500000 },
];

export const BookingAddChargeModal: React.FC<BookingAddChargeModalProps> = ({
  isOpen,
  onClose,
  bookingId,
  bookingNumber,
  room,
  detail,
  guestName,
  initialChargeType,
  initialProposal,
  onChargeSuccess,
}) => {
  // Form states
  const initialType: BookingChargeType =
    initialChargeType ||
    initialProposal?.chargeType ||
    (room?.status === 'CHECKED_IN' ? 'LATE_CHECKOUT' : 'EARLY_CHECKIN');

  const [chargeType, setChargeType] = useState<BookingChargeType>(initialType);
  const [itemName, setItemName] = useState(
    () => initialProposal?.suggestedItemName || initialProposal?.policyRule || ''
  );
  const [quantity, setQuantity] = useState(1);
  const [unitPrice, setUnitPrice] = useState(() => initialProposal?.suggestedAmount || 0);
  const [serviceFeeRate, setServiceFeeRate] = useState(
    () => Number(initialProposal?.defaultServiceFeeRate) || 5
  );
  const [vatRate, setVatRate] = useState(
    () => Number(initialProposal?.defaultVatRate) || 8
  );
  const [note, setNote] = useState('');

  // Surcharge policy previews from backend
  const [isLoadingPreviews, setIsLoadingPreviews] = useState(false);
  const [earlyProposal, setEarlyProposal] = useState<SurchargeProposalResponse | null>(() =>
    initialProposal?.chargeType === 'EARLY_CHECKIN' ? initialProposal : null
  );
  const [lateProposal, setLateProposal] = useState<SurchargeProposalResponse | null>(() =>
    initialProposal?.chargeType === 'LATE_CHECKOUT' ? initialProposal : null
  );
  const [configuredRules, setConfiguredRules] = useState<SurchargeRuleDto[]>([]);

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const formatVND = (amount?: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  // 1. Thẩm định điều kiện và tải chính sách phụ thu từ backend
  const evaluateSurchargePolicies = useCallback(async () => {
    if (!room || !isOpen) return;

    try {
      // 1.1 Tải song song đề xuất Nhận phòng sớm và Trả phòng muộn từ previewSurcharge API
      const [earlyRes, lateRes] = await Promise.allSettled([
        bookingService.previewSurcharge(room.id, { chargeType: 'EARLY_CHECKIN' }),
        bookingService.previewSurcharge(room.id, { chargeType: 'LATE_CHECKOUT' }),
      ]);

      const earlyData = earlyRes.status === 'fulfilled' && earlyRes.value.result ? earlyRes.value.result : null;
      const lateData = lateRes.status === 'fulfilled' && lateRes.value.result ? lateRes.value.result : null;

      setEarlyProposal(earlyData);
      setLateProposal(lateData);

      // Điền giá trị gợi ý nếu người dùng chưa nhập hoặc đang khớp loại phụ thu
      if (chargeType === 'EARLY_CHECKIN' && earlyData) {
        setItemName(earlyData.suggestedItemName || earlyData.policyRule || 'Phụ thu nhận phòng sớm');
        setUnitPrice(earlyData.suggestedAmount || 0);
        setServiceFeeRate(Number(earlyData.defaultServiceFeeRate) || 5);
        setVatRate(Number(earlyData.defaultVatRate) || 8);
      } else if (chargeType === 'LATE_CHECKOUT' && lateData) {
        setItemName(lateData.suggestedItemName || lateData.policyRule || 'Phụ thu trả phòng muộn');
        setUnitPrice(lateData.suggestedAmount || 0);
        setServiceFeeRate(Number(lateData.defaultServiceFeeRate) || 5);
        setVatRate(Number(lateData.defaultVatRate) || 8);
      }

      // 1.2 Tải các cấu hình quy tắc phụ thu hạng phòng từ pricing module
      if (detail?.hotelRoomTypeId) {
        try {
          const ruleRes = await surchargeRuleService.filter({
            hotelRoomTypeId: detail.hotelRoomTypeId,
            status: 'ACTIVE',
            pageSize: 50,
          });
          if (ruleRes.result && ruleRes.result.content) {
            setConfiguredRules(ruleRes.result.content);
          }
        } catch (e) {
          console.warn('Lỗi tải cấu hình quy tắc phụ thu từ surchargeRuleService:', e);
        }
      }
    } catch (err) {
      console.error('Lỗi khi thẩm định phụ thu:', err);
    } finally {
      setIsLoadingPreviews(false);
    }
  }, [room, isOpen, detail, chargeType]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (isOpen && room) {
        void evaluateSurchargePolicies();
      }
    }, 0);
    return () => clearTimeout(timer);
  }, [isOpen, room, evaluateSurchargePolicies]);

  // 2. Tính toán tính hợp lệ (Eligibility) của từng loại phụ thu
  const isRoomCheckedIn = room?.status === 'CHECKED_IN';
  const isRoomCheckedOut = room?.status === 'CHECKED_OUT';

  // Điều kiện EARLY_CHECKIN:
  // - Phòng PHẢI là chưa Check-in (không phải CHECKED_IN hay CHECKED_OUT)
  // - Phải thỏa mãn mốc thời gian trước giờ check-in chuẩn (applicable = true)
  const isEarlyCheckInEligible = useMemo(() => {
    if (isRoomCheckedIn || isRoomCheckedOut) return false;
    if (initialProposal && initialProposal.chargeType === 'EARLY_CHECKIN') return true;
    return Boolean(earlyProposal && earlyProposal.applicable);
  }, [isRoomCheckedIn, isRoomCheckedOut, initialProposal, earlyProposal]);

  const earlyCheckInReason = useMemo(() => {
    if (isRoomCheckedIn) return 'Phòng đã check-in đang ở. Không thể phụ thu nhận phòng sớm.';
    if (isRoomCheckedOut) return 'Phòng đã trả. Không thể áp dụng.';
    if (earlyProposal && !earlyProposal.applicable) {
      return earlyProposal.policyRule || 'Khách nhận phòng đúng hoặc sau 14:00 (Không phát sinh phụ thu).';
    }
    return undefined;
  }, [isRoomCheckedIn, isRoomCheckedOut, earlyProposal]);

  // Điều kiện LATE_CHECKOUT:
  // - Phòng PHẢI là đang ở (CHECKED_IN)
  // - Phải thỏa mãn mốc thời gian quá giờ check-out chuẩn (applicable = true)
  const isLateCheckOutEligible = useMemo(() => {
    if (!isRoomCheckedIn) return false;
    if (initialProposal && initialProposal.chargeType === 'LATE_CHECKOUT') return true;
    return Boolean(lateProposal && lateProposal.applicable);
  }, [isRoomCheckedIn, initialProposal, lateProposal]);

  const lateCheckOutReason = useMemo(() => {
    if (!isRoomCheckedIn) return 'Khách chưa nhận phòng. Chỉ áp dụng trả phòng muộn khi phòng đang ở.';
    if (lateProposal && !lateProposal.applicable) {
      return lateProposal.policyRule || 'Khách trả phòng đúng hoặc trước 12:00 (Không phát sinh phụ thu).';
    }
    return undefined;
  }, [isRoomCheckedIn, lateProposal]);

  // Kiểm tra rule Thêm người / Giường phụ
  const extraPersonRule = useMemo(
    () => configuredRules.find((r) => r.ruleType === 'EXTRA_PERSON'),
    [configuredRules]
  );
  const extraBedRule = useMemo(
    () => configuredRules.find((r) => r.ruleType === 'EXTRA_BED'),
    [configuredRules]
  );

  // 3. Xây dựng danh sách tùy chọn SelectOption với cờ disabled & badge
  const selectOptions: SelectOption<BookingChargeType>[] = useMemo(() => {
    return [
      {
        value: 'EARLY_CHECKIN',
        label: 'Nhận phòng sớm (Early Check-in)',
        icon: <Clock className="w-4 h-4 text-amber-600" />,
        disabled: !isEarlyCheckInEligible,
        badge: isEarlyCheckInEligible
          ? `Đề xuất: ${formatVND(earlyProposal?.suggestedAmount)}`
          : 'Không khả dụng',
        description: isEarlyCheckInEligible
          ? earlyProposal?.policyRule || 'Áp dụng biểu phí nhận phòng sớm của khách sạn'
          : earlyCheckInReason || 'Không thỏa mốc thời gian nhận phòng sớm',
      },
      {
        value: 'LATE_CHECKOUT',
        label: 'Trả phòng muộn (Late Check-out)',
        icon: <Clock className="w-4 h-4 text-purple-600" />,
        disabled: !isLateCheckOutEligible,
        badge: isLateCheckOutEligible
          ? `Đề xuất: ${formatVND(lateProposal?.suggestedAmount)}`
          : 'Không khả dụng',
        description: isLateCheckOutEligible
          ? lateProposal?.policyRule || 'Áp dụng biểu phí trả phòng muộn của khách sạn'
          : lateCheckOutReason || 'Không thỏa mốc thời gian trả phòng muộn',
      },
      {
        value: 'EXTRA_PERSON',
        label: 'Phụ thu thêm người (Extra Person)',
        icon: <Building2 className="w-4 h-4 text-blue-600" />,
        disabled: false,
        badge: extraPersonRule
          ? `Biểu phí: ${formatVND(extraPersonRule.adjustmentValue)}`
          : 'Linh hoạt',
        description: extraPersonRule
          ? `Phụ thu thêm người theo cấu hình hạng phòng (${extraPersonRule.adjustmentType === 'PERCENT' ? `${extraPersonRule.adjustmentValue}%` : formatVND(extraPersonRule.adjustmentValue)})`
          : 'Phụ thu người lớn / trẻ em phát sinh thêm',
      },
      {
        value: 'EXTRA_BED',
        label: 'Kê thêm giường phụ (Extra Bed)',
        icon: <Layers className="w-4 h-4 text-teal-600" />,
        disabled: false,
        badge: extraBedRule
          ? `Biểu phí: ${formatVND(extraBedRule.adjustmentValue)}`
          : 'Linh hoạt',
        description: extraBedRule
          ? `Biểu phí giường phụ: ${formatVND(extraBedRule.adjustmentValue)}`
          : 'Kê giường phụ theo yêu cầu lưu trú',
      },
      {
        value: 'PENALTY',
        label: 'Bồi thường hư hỏng / Phạt vi phạm',
        icon: <AlertTriangle className="w-4 h-4 text-rose-600" />,
        disabled: false,
        badge: 'Linh hoạt',
        description: 'Hỏng hóc đồ đạc, mất chìa khóa, phạt nội quy',
      },
      {
        value: 'OTHER',
        label: 'Dịch vụ & Phụ phí khác',
        icon: <Sparkles className="w-4 h-4 text-neutral-600" />,
        disabled: false,
        badge: 'Tùy chỉnh',
        description: 'Các chi phí phát sinh đặc biệt khác',
      },
    ];
  }, [
    isEarlyCheckInEligible,
    earlyProposal,
    earlyCheckInReason,
    isLateCheckOutEligible,
    lateProposal,
    lateCheckOutReason,
    extraPersonRule,
    extraBedRule,
  ]);

  // 4. Xử lý chuyển loại phụ thu và tự động điền biểu phí
  const handleSelectChargeType = (newType: BookingChargeType) => {
    setChargeType(newType);
    if (newType === 'EARLY_CHECKIN' && earlyProposal) {
      setItemName(
        earlyProposal.suggestedItemName ||
          earlyProposal.policyRule ||
          'Phụ thu nhận phòng sớm'
      );
      setUnitPrice(earlyProposal.suggestedAmount || 0);
      setServiceFeeRate(Number(earlyProposal.defaultServiceFeeRate) || 5);
      setVatRate(Number(earlyProposal.defaultVatRate) || 8);
      setQuantity(1);
    } else if (newType === 'LATE_CHECKOUT' && lateProposal) {
      setItemName(
        lateProposal.suggestedItemName ||
          lateProposal.policyRule ||
          'Phụ thu trả phòng muộn'
      );
      setUnitPrice(lateProposal.suggestedAmount || 0);
      setServiceFeeRate(Number(lateProposal.defaultServiceFeeRate) || 5);
      setVatRate(Number(lateProposal.defaultVatRate) || 8);
      setQuantity(1);
    } else if (newType === 'EXTRA_PERSON') {
      setItemName('Phụ thu thêm người lưu trú');
      const val = extraPersonRule?.adjustmentValue || 200000;
      setUnitPrice(val);
      setServiceFeeRate(5);
      setVatRate(8);
      setQuantity(1);
    } else if (newType === 'EXTRA_BED') {
      setItemName('Phụ thu giường phụ (Extra Bed)');
      const val = extraBedRule?.adjustmentValue || 300000;
      setUnitPrice(val);
      setServiceFeeRate(5);
      setVatRate(8);
      setQuantity(1);
    } else if (newType === 'PENALTY') {
      setItemName('Bồi thường hư hỏng trang thiết bị');
      setUnitPrice(100000);
      setServiceFeeRate(0); // Bồi thường thường không tính phí phục vụ
      setVatRate(8);
      setQuantity(1);
    } else if (newType === 'OTHER') {
      setItemName('Phụ phí phát sinh');
      setUnitPrice(100000);
      setServiceFeeRate(5);
      setVatRate(8);
      setQuantity(1);
    }
  };

  // 6. Tính toán tài chính trực tiếp (Live Financial Summary)
  const subTotal = useMemo(() => {
    return Math.max(0, quantity * unitPrice);
  }, [quantity, unitPrice]);

  const serviceFeeAmount = useMemo(() => {
    return Math.round((subTotal * (serviceFeeRate || 0)) / 100);
  }, [subTotal, serviceFeeRate]);

  const vatAmount = useMemo(() => {
    return Math.round(((subTotal + serviceFeeAmount) * (vatRate || 0)) / 100);
  }, [subTotal, serviceFeeAmount, vatRate]);

  const grandTotal = useMemo(() => {
    return subTotal + serviceFeeAmount + vatAmount;
  }, [subTotal, serviceFeeAmount, vatAmount]);

  // 7. Xử lý lưu phụ phí vào đơn phòng
  const handleSubmit = async () => {
    if (!room) {
      setErrorMessage('Không xác định được thông tin phòng.');
      return;
    }
    if (!itemName.trim()) {
      setErrorMessage('Vui lòng nhập tên khoản phụ thu.');
      return;
    }
    if (quantity <= 0) {
      setErrorMessage('Số lượng phải lớn hơn 0.');
      return;
    }
    if (unitPrice < 0) {
      setErrorMessage('Đơn giá không được âm.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      await bookingService.addCharge(room.id, {
        chargeType,
        itemName: itemName.trim(),
        quantity,
        unitPrice,
        serviceFeeRate,
        vatRate,
        description: note.trim() || undefined,
      });

      const successMsg = `Đã thêm thành công khoản "${itemName.trim()}" cho Phòng ${
        room.roomNumber || room.id
      }! Tổng cộng: ${formatVND(grandTotal)}`;

      onClose();
      if (onChargeSuccess) {
        onChargeSuccess(successMsg);
      }
    } catch (err: unknown) {
      console.error('Lỗi thêm phụ phí:', err);
      const apiErr = err as { response?: { data?: { message?: string } }; message?: string };
      setErrorMessage(
        apiErr.response?.data?.message || apiErr.message || 'Không thể thêm phụ phí. Vui lòng thử lại.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const activeProposal = chargeType === 'EARLY_CHECKIN' ? earlyProposal : chargeType === 'LATE_CHECKOUT' ? lateProposal : null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Thêm Phụ Phí Đơn Phòng"
      maxWidth="lg"
    >
      <div className="space-y-4">
        {/* ROOM & BOOKING CONTEXT HEADER */}
        <div className="bg-neutral-50 border border-neutral-200/90 rounded-2xl p-3.5 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-red-50 border border-red-200 flex items-center justify-center font-black text-red-600 text-sm">
              {room?.roomNumber || 'P.?'}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-neutral-900 text-sm">
                  Phòng {room?.roomNumber || 'Chưa xếp'}
                </span>
                <span
                  className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                    isRoomCheckedIn
                      ? 'bg-blue-100 text-blue-800'
                      : isRoomCheckedOut
                      ? 'bg-neutral-200 text-neutral-700'
                      : 'bg-emerald-100 text-emerald-800'
                  }`}
                >
                  {isRoomCheckedIn ? 'Đang ở' : isRoomCheckedOut ? 'Đã trả phòng' : 'Sẵn sàng nhận'}
                </span>
              </div>
              <div className="text-neutral-500 text-[11px] mt-0.5 flex items-center gap-2">
                <span>{detail?.roomTypeName || 'Hạng tiêu chuẩn'}</span>
                <span>•</span>
                <span>Đơn #{bookingNumber || bookingId}</span>
                {guestName && (
                  <>
                    <span>•</span>
                    <span className="font-medium text-neutral-700">{guestName}</span>
                  </>
                )}
              </div>
            </div>
          </div>

          <div className="text-right text-[11px] text-neutral-500">
            <div>
              Khung giờ chuẩn: <strong>Nhận 14:00</strong> | <strong>Trả 12:00</strong>
            </div>
            {detail?.checkInDate && detail?.checkOutDate && (
              <div className="text-neutral-400 mt-0.5">
                Lưu trú: {detail.checkInDate} ➔ {detail.checkOutDate}
              </div>
            )}
          </div>
        </div>

        {/* ERROR MESSAGE BANNER */}
        {errorMessage && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-xl flex items-start gap-2.5 text-xs text-red-700">
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* LOADING STATE FOR POLICY PREVIEWS */}
        {isLoadingPreviews && (
          <div className="flex items-center gap-2 px-3 py-2 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800">
            <Loader2 className="w-3.5 h-3.5 animate-spin text-amber-600" />
            <span>Đang đối chiếu chính sách phụ thu động của khách sạn và hạng phòng...</span>
          </div>
        )}

        {/* SELECT LOẠI PHỤ PHÍ */}
        <div>
          <div className="flex justify-between items-center mb-1">
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider">
              Loại phụ phí (Surcharge Rule)
            </label>
            <span className="text-[11px] text-neutral-500">
              Quy tắc không thỏa mốc thời gian sẽ tự động bị khóa
            </span>
          </div>

          <Select<BookingChargeType>
            value={chargeType}
            onChange={(val) => handleSelectChargeType(val)}
            options={selectOptions}
            size="lg"
          />
        </div>

        {/* POLICY EVALUATION CARD (NẾU CÓ ĐỀ XUẤT TỪ CHÍNH SÁCH) */}
        {activeProposal && activeProposal.applicable && (
          <div className="bg-amber-50/90 border border-amber-200/90 rounded-2xl p-3.5 space-y-2 text-xs">
            <div className="flex items-center justify-between border-b border-amber-200/60 pb-2">
              <span className="font-bold text-amber-900 flex items-center gap-1.5">
                <Info className="w-4 h-4 text-amber-600" />
                Căn cứ chính sách biểu phí phụ thu khách sạn
              </span>
              <span className="text-[11px] font-semibold text-amber-800 bg-amber-200/70 px-2 py-0.5 rounded-md">
                Chuẩn: {activeProposal.standardTime || (chargeType === 'EARLY_CHECKIN' ? '14:00' : '12:00')}
                {activeProposal.timeDifferenceMinutes
                  ? ` (Lệch ${Math.round((activeProposal.timeDifferenceMinutes / 60) * 10) / 10}h)`
                  : ''}
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] text-amber-900">
              <div>
                <span className="text-neutral-500">Quy định áp dụng: </span>
                <span className="font-semibold">{activeProposal.policyRule}</span>
              </div>
              <div>
                <span className="text-neutral-500">Mức đề xuất: </span>
                <span className="font-bold text-red-600">
                  {activeProposal.suggestedPercentage ? `${activeProposal.suggestedPercentage}% x ` : ''}
                  {formatVND(activeProposal.baseRoomRate)} = {formatVND(activeProposal.suggestedAmount)}
                </span>
              </div>
            </div>
          </div>
        )}

        {/* PRESETS NHANH CHO PENALTY */}
        {chargeType === 'PENALTY' && (
          <div className="space-y-1.5">
            <div className="flex items-center gap-1.5 text-neutral-500 text-[11px] font-medium">
              <Tag className="w-3.5 h-3.5 text-neutral-400" />
              <span>Gợi ý khoản bồi thường thường gặp:</span>
            </div>
            <div className="flex flex-wrap gap-1.5">
              {PENALTY_PRESETS.map((preset) => (
                <button
                  key={preset.name}
                  type="button"
                  onClick={() => {
                    setItemName(preset.name);
                    setUnitPrice(preset.price);
                    setServiceFeeRate(0);
                  }}
                  className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-neutral-100 hover:bg-neutral-200 text-neutral-700 border border-neutral-200 cursor-pointer transition-colors"
                >
                  + {preset.name} ({formatVND(preset.price)})
                </button>
              ))}
            </div>
          </div>
        )}

        {/* INPUT TÊN KHOẢN PHỤ THU */}
        <div>
          <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1">
            Tên khoản phụ thu <span className="text-red-500">*</span>
          </label>
          <Input
            value={itemName}
            onChange={(e) => setItemName(e.target.value)}
            placeholder="VD: Phụ thu nhận phòng sớm 09:00 hoặc Đền bù vỡ cốc sứ"
            className="rounded-xl"
          />
        </div>

        {/* GRID SỐ LƯỢNG & ĐƠN GIÁ */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1">
              Số lượng <span className="text-red-500">*</span>
            </label>
            <Input
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value, 10) || 1))}
              className="rounded-xl"
            />
          </div>
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1">
              Đơn giá (VNĐ) <span className="text-red-500">*</span>
            </label>
            <Input
              type="number"
              min={0}
              step={10000}
              value={unitPrice}
              onChange={(e) => setUnitPrice(Math.max(0, parseInt(e.target.value, 10) || 0))}
              className="rounded-xl font-bold text-neutral-900"
            />
          </div>
        </div>

        {/* GRID PHÍ DỊCH VỤ & THUẾ VAT */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1">
              Phí dịch vụ khách sạn (%)
            </label>
            <Input
              type="number"
              min={0}
              max={100}
              value={serviceFeeRate}
              onChange={(e) => setServiceFeeRate(Math.max(0, parseFloat(e.target.value) || 0))}
              className="rounded-xl"
            />
          </div>
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1">
              Thuế VAT (%)
            </label>
            <Input
              type="number"
              min={0}
              max={100}
              value={vatRate}
              onChange={(e) => setVatRate(Math.max(0, parseFloat(e.target.value) || 0))}
              className="rounded-xl"
            />
          </div>
        </div>

        {/* GHI CHÚ BỔ SUNG */}
        <div>
          <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1">
            Ghi chú thẩm quyền / Lý do phát sinh
          </label>
          <Input
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="VD: Căn cứ biên bản kiểm phòng hoặc phê duyệt của Quản lý ca..."
            className="rounded-xl text-xs"
          />
        </div>

        {/* BẢNG TÍNH TÀI CHÍNH CHI TIẾT (LIVE FINANCIAL SUMMARY) */}
        <div className="bg-neutral-50 rounded-2xl p-3.5 space-y-1.5 text-xs border border-neutral-200">
          <div className="flex justify-between text-neutral-600 text-[11px]">
            <span>Tiền hàng / Dịch vụ gốc:</span>
            <span className="font-semibold text-neutral-800">{formatVND(subTotal)}</span>
          </div>
          <div className="flex justify-between text-neutral-600 text-[11px]">
            <span>Phí phục vụ ({serviceFeeRate}%):</span>
            <span className="font-semibold text-neutral-800">{formatVND(serviceFeeAmount)}</span>
          </div>
          <div className="flex justify-between text-neutral-600 text-[11px]">
            <span>Thuế VAT ({vatRate}%):</span>
            <span className="font-semibold text-neutral-800">{formatVND(vatAmount)}</span>
          </div>
          <div className="flex justify-between font-bold text-neutral-900 border-t border-neutral-200 pt-2 text-xs">
            <span>Tổng khoản phụ thu phát sinh:</span>
            <span className="text-red-600 font-black text-sm">{formatVND(grandTotal)}</span>
          </div>
        </div>

        {/* MODAL ACTIONS */}
        <div className="flex justify-end gap-2.5 pt-3 border-t border-neutral-100">
          <Button variant="outline" onClick={onClose} disabled={isSubmitting} className="rounded-xl">
            Hủy
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            disabled={isSubmitting || !itemName.trim()}
            className="bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl px-5 h-10 shadow-sm cursor-pointer"
          >
            {isSubmitting ? (
              <span className="flex items-center gap-1.5">
                <Loader2 className="w-4 h-4 animate-spin" />
                Đang ghi nhận...
              </span>
            ) : (
              <span className="flex items-center gap-1.5">
                <DollarSign className="w-4 h-4" />
                Xác Nhận Thêm Phụ Phí
              </span>
            )}
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export default BookingAddChargeModal;
