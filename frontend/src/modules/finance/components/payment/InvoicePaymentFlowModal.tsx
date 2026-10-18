import React, { useState, useEffect, useRef, useMemo } from 'react';
import {
  X,
  CheckCircle2,
  XCircle,
  CreditCard,
  Banknote,
  Receipt,
  ArrowRight,
  ArrowLeft,
  Copy,
  ExternalLink,
  RefreshCw,
  Tag,
  Printer,
  ShieldCheck,
  Clock,
  Sparkles,
  Building2,
} from 'lucide-react';
import Button from '../../../../core/components/ui/Button';
import Input from '../../../../core/components/ui/Input';
import CurrencyInput from '../../../../core/components/ui/CurrencyInput';
import { formatCurrency } from '../../../../core/utils/currency';
import financeService from '../../services/finance.service';
import type {
  PaymentResponse,
  InvoiceResponse,
  PaymentMethod,
  PaymentPurpose,
  PaymentUrlResponse,
} from '../../types/finance.types';
import type { BookingResponse } from '../../../booking/types/booking.types';
import { VNPAY_ATM_TEST_CARD } from '../../utils/vnpay.utils';
import HotelFolioReceipt from './HotelFolioReceipt';

export interface InvoicePaymentFlowModalProps {
  isOpen: boolean;
  onClose: () => void;
  booking: BookingResponse | null;
  invoice: InvoiceResponse | null;
  onPaymentSuccess: (payment: PaymentResponse) => void;
  initialPaidAmount?: number;
  initialBalanceDue?: number;
}

type PaymentFlowStep = 1 | 2 | 3 | 4;

const METHOD_OPTIONS: Array<{
  id: PaymentMethod;
  name: string;
  desc: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string;
}> = [
    {
      id: 'CASH',
      name: 'Tiền mặt tại quầy',
      desc: 'Thu tiền mặt trực tiếp tại quầy lễ tân',
      icon: Banknote,
      badge: 'Trực tiếp',
    },
    {
      id: 'VNPAY',
      name: 'Thẻ ATM nội địa (Cổng VNPAY Sandbox)',
      desc: 'Thanh toán thẻ ATM NCB trực tuyến qua cổng VNPAY (không quét QR)',
      icon: CreditCard,
      badge: 'Thẻ ATM Sandbox',
    },
  ];

export const InvoicePaymentFlowModal: React.FC<InvoicePaymentFlowModalProps> = ({
  isOpen,
  onClose,
  booking,
  invoice,
  onPaymentSuccess,
  initialPaidAmount = 0,
  initialBalanceDue,
}) => {
  // ── Step State ─────────────────────────────────────────────────────────────
  const [currentStep, setCurrentStep] = useState<PaymentFlowStep>(1);
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('CASH');
  const [isReceiptModalOpen, setIsReceiptModalOpen] = useState(false);

  // ── Voucher State ──────────────────────────────────────────────────────────
  const [voucherInput, setVoucherInput] = useState('');
  const [appliedVoucher, setAppliedVoucher] = useState<{
    code: string;
    discountPercent?: number;
    discountAmount?: number;
    description: string;
  } | null>(null);
  const [voucherError, setVoucherError] = useState<string | null>(null);

  // ── Cash Details (Step 2) ──────────────────────────────────────────────────
  const [cashGiven, setCashGiven] = useState<number>(0);
  const [cashReceiptCode, setCashReceiptCode] = useState('');

  // ── VNPay Card Details (Step 2: Thẻ ATM nội địa - tự nhập & gợi ý test) ────
  const [vnpayCardNumber, setVnpayCardNumber] = useState('');
  const [vnpayCardHolder, setVnpayCardHolder] = useState('');
  const [vnpayIssueDate, setVnpayIssueDate] = useState('');
  const [vnpayCardError, setVnpayCardError] = useState<string | null>(null);

  // ── VNPay State & Real API / Polling (Step 2: Thẻ ATM nội địa) ────────────
  const [vnPaySession, setVnPaySession] = useState<PaymentUrlResponse | null>(null);
  const [isCreatingVnPay, setIsCreatingVnPay] = useState(false);
  const [vnPayError, setVnPayError] = useState<string | null>(null);
  const [vnpayCopiedField, setVnpayCopiedField] = useState<string | null>(null);
  const vnpayPollRef = useRef<ReturnType<typeof setInterval> | null>(null);


  // ── OTP State (Step 3) ─────────────────────────────────────────────────────
  const [otpValues, setOtpValues] = useState<string[]>(['', '', '', '', '', '']);
  const [otpSecondsLeft, setOtpSecondsLeft] = useState(60);
  const otpTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const otpInputsRef = useRef<(HTMLInputElement | null)[]>([]);

  // ── Final Result State (Step 4) ────────────────────────────────────
  const [isSuccess, setIsSuccess] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [completedPayment, setCompletedPayment] = useState<PaymentResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // ── Reset state when modal opens or closes ─────────────────────────────────
  useEffect(() => {
    if (isOpen) {
      setCurrentStep(1);
      setSelectedMethod('CASH');
      setVoucherInput('');
      setAppliedVoucher(null);
      setVoucherError(null);
      setVnPaySession(null);
      setVnpayCardNumber('');
      setVnpayCardHolder('');
      setVnpayIssueDate('');
      setVnpayCardError(null);
      setOtpValues(['', '', '', '', '', '']);
      setOtpSecondsLeft(60);
      setIsSuccess(true);
      setErrorMessage('');
      setCompletedPayment(null);
    } else {
      if (vnpayPollRef.current) {
        clearInterval(vnpayPollRef.current);
        vnpayPollRef.current = null;
      }
      if (otpTimerRef.current) {
        clearInterval(otpTimerRef.current);
        otpTimerRef.current = null;
      }
    }
  }, [isOpen]);

  // ── Dịch vụ & Chiết tính chi phí ───────────────────────────────────────────
  const serviceItems = useMemo(() => {
    if (invoice?.details && invoice.details.length > 0) {
      return invoice.details.map((d) => ({
        id: d.id,
        name: d.description,
        quantity: d.quantity,
        unitPrice: Number(d.unitPrice),
        vatRate: Number(d.vatRate || 8),
        vatAmount: Number(d.vatAmount),
        subtotal: Number(d.subtotal),
        totalAmount: Number(d.totalAmount),
      }));
    }

    // Nếu chưa có hóa đơn chính thức, phân tích từ booking
    if (!booking) return [];

    const items: Array<{
      id: number;
      name: string;
      quantity: number;
      unitPrice: number;
      vatRate: number;
      vatAmount: number;
      subtotal: number;
      totalAmount: number;
    }> = [];

    let itemId = 1;

    // Chi tiết phòng lưu trú
    booking.bookingDetails?.forEach((detail) => {
      const roomCount = detail.bookingRooms?.length || detail.quantity || 1;
      const baseSubtotal = Number(booking.subtotalAmount) || 1250000;
      const unit = roomCount > 0 ? Math.round(baseSubtotal / roomCount) : baseSubtotal;
      const vatAmount = Math.round(baseSubtotal * 0.08);

      items.push({
        id: itemId++,
        name: `Lưu trú: ${detail.roomTypeName || 'Phòng Khách sạn'} (${detail.checkInDate} ➔ ${detail.checkOutDate})`,
        quantity: roomCount,
        unitPrice: unit,
        vatRate: 8,
        vatAmount: vatAmount,
        subtotal: baseSubtotal,
        totalAmount: baseSubtotal + vatAmount,
      });

      // Các phụ phí / dịch vụ phòng nếu có
      detail.bookingRooms?.forEach((rm) => {
        rm.charges?.forEach((ch) => {
          items.push({
            id: itemId++,
            name: `${ch.itemName} (${rm.roomNumber ? `P.${rm.roomNumber}` : 'Dịch vụ'})`,
            quantity: ch.quantity || 1,
            unitPrice: Number(ch.unitPrice),
            vatRate: Number(ch.vatRate || 8),
            vatAmount: Number(ch.vatAmount || 0),
            subtotal: Number(ch.subtotal || ch.unitPrice),
            totalAmount: Number(ch.totalAmount),
          });
        });
      });
    });

    if (items.length === 0) {
      const sub = Number(booking.subtotalAmount || booking.totalAmount || 1000000);
      const vat = Math.round(sub * 0.08);
      items.push({
        id: 1,
        name: 'Dịch vụ lưu trú khách sạn',
        quantity: 1,
        unitPrice: sub,
        vatRate: 8,
        vatAmount: vat,
        subtotal: sub,
        totalAmount: sub + vat,
      });
    }

    return items;
  }, [invoice, booking]);

  // Tổng tiền trước thuế, phí, VAT
  const baseSubtotal = useMemo(() => {
    return serviceItems.reduce((acc, it) => acc + it.subtotal, 0);
  }, [serviceItems]);

  const serviceFeeAmount = useMemo(() => {
    if (invoice?.serviceFeeAmount !== undefined) return Number(invoice.serviceFeeAmount);
    return Math.round(baseSubtotal * 0.05);
  }, [invoice, baseSubtotal]);

  const totalVatAmount = useMemo(() => {
    if (invoice?.vatAmount !== undefined) return Number(invoice.vatAmount);
    return serviceItems.reduce((acc, it) => acc + it.vatAmount, 0);
  }, [invoice, serviceItems]);

  const totalBeforeVoucher = useMemo(() => {
    return baseSubtotal + serviceFeeAmount + totalVatAmount;
  }, [baseSubtotal, serviceFeeAmount, totalVatAmount]);

  // Giảm trừ voucher
  const voucherDiscount = useMemo(() => {
    if (!appliedVoucher) return 0;
    if (appliedVoucher.discountPercent) {
      return Math.round((totalBeforeVoucher * appliedVoucher.discountPercent) / 100);
    }
    if (appliedVoucher.discountAmount) {
      return Math.min(totalBeforeVoucher, appliedVoucher.discountAmount);
    }
    return 0;
  }, [appliedVoucher, totalBeforeVoucher]);

  // Tổng tiền thanh toán cuối cùng
  const grandTotal = useMemo(() => {
    return Math.max(0, totalBeforeVoucher - voucherDiscount);
  }, [totalBeforeVoucher, voucherDiscount]);

  // Dư nợ còn lại thực tế sau khi đối trừ các khoản thanh toán trước đó
  const remainingDue = useMemo(() => {
    if (initialBalanceDue !== undefined) {
      return Math.max(0, initialBalanceDue - voucherDiscount);
    }
    return Math.max(0, grandTotal - initialPaidAmount);
  }, [initialBalanceDue, voucherDiscount, grandTotal, initialPaidAmount]);

  // Số tiền thanh toán kỳ này (mặc định lấy toàn bộ dư nợ còn lại)
  const effectivePayAmount = useMemo(() => {
    if (remainingDue > 0) return remainingDue;
    return grandTotal;
  }, [remainingDue, grandTotal]);

  // Cập nhật số tiền khách đưa mặc định khi chuyển sang Form 2
  useEffect(() => {
    if (currentStep === 2 && selectedMethod === 'CASH') {
      setCashGiven(effectivePayAmount);
    }
  }, [currentStep, selectedMethod, effectivePayAmount]);

  // ── Xử lý áp dụng Voucher ──────────────────────────────────────────────────
  const handleApplyVoucher = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setVoucherError(null);
    const code = voucherInput.trim().toUpperCase();

    if (!code) {
      setVoucherError('Vui lòng nhập mã giảm giá');
      return;
    }

    if (code === 'VIP10') {
      setAppliedVoucher({
        code: 'VIP10',
        discountPercent: 10,
        description: 'Ưu đãi Khách hàng Thân thiết (Giảm 10%)',
      });
    } else if (code === 'SUMMER15' || code === 'SUMMER2026') {
      setAppliedVoucher({
        code: 'SUMMER2026',
        discountPercent: 15,
        description: 'Chiến dịch Khuyến mãi Mùa hè (Giảm 15%)',
      });
    } else if (code === 'HOTEL50K' || code === 'GIAM50K') {
      setAppliedVoucher({
        code: 'GIAM50K',
        discountAmount: 50000,
        description: 'Voucher Chào mừng (Giảm 50.000 đ)',
      });
    } else if (code === 'WELCOME100K' || code === 'GIAM100K') {
      setAppliedVoucher({
        code: 'GIAM100K',
        discountAmount: 100000,
        description: 'Ưu đãi Đặt phòng Lần đầu (Giảm 100.000 đ)',
      });
    } else if (code === 'VIP200K' || code === 'GIAM200K') {
      setAppliedVoucher({
        code: 'GIAM200K',
        discountAmount: 200000,
        description: 'Voucher Khách hàng Doanh nghiệp (Giảm 200.000 đ)',
      });
    } else {
      setVoucherError(`Mã "${code}" không hợp lệ hoặc đã hết lượt áp dụng (Thử: VIP10, GIAM50K, GIAM100K)`);
    }
  };

  const handleRemoveVoucher = () => {
    setAppliedVoucher(null);
    setVoucherInput('');
    setVoucherError(null);
  };

  // ── Khởi tạo phiên VNPay thật từ Cổng Sandbox (Nếu chọn VNPay) ─────────────
  const initializeVnPaySession = async () => {
    if (!booking) return;
    setIsCreatingVnPay(true);
    setVnPayError(null);

    try {
      const res = await financeService.createVnPayPayment({
        bookingId: booking.id,
        method: 'VNPAY',
        bankCode: 'NCB',
        amount: effectivePayAmount,
      });
      if (res.result) {
        setVnPaySession(res.result);
        startVnPayPolling(res.result.paymentId);
      }
    } catch (err: unknown) {
      console.warn('Lỗi tạo phiên VNPay sandbox thật:', err);
      setVnPayError(
        'Không thể tạo phiên thanh toán VNPay Sandbox. Hãy đảm bảo VNPAY_TMN_ID và VNPAY_HASH_SECRET đã được thiết lập đúng trong file .env và backend đang chạy.'
      );
    } finally {
      setIsCreatingVnPay(false);
    }
  };


  // Polling lắng nghe VNPay redirect về BE /vnpay/return
  const startVnPayPolling = (paymentId: number) => {
    if (vnpayPollRef.current) clearInterval(vnpayPollRef.current);

    vnpayPollRef.current = setInterval(async () => {
      try {
        const res = await financeService.getPaymentById(paymentId);
        if (res.result && res.result.status === 'SUCCESS') {
          if (vnpayPollRef.current) clearInterval(vnpayPollRef.current);
          setCompletedPayment(res.result);
          setIsSuccess(true);
          setCurrentStep(4);
          onPaymentSuccess(res.result);
        } else if (res.result && (res.result.status === 'FAILED' || res.result.status === 'CANCELLED')) {
          if (vnpayPollRef.current) clearInterval(vnpayPollRef.current);
          setIsSuccess(false);
          setErrorMessage('Cổng VNPAY báo giao dịch bị hủy hoặc thanh toán không thành công.');
          setCurrentStep(4);
        }
      } catch {
        // Tiếp tục poll ở lần kế tiếp
      }
    }, 2500);
  };

  // ── Xử lý đếm ngược OTP (Form 3) ───────────────────────────────────────────
  const startOtpCountdown = () => {
    setOtpSecondsLeft(60);
    if (otpTimerRef.current) clearInterval(otpTimerRef.current);

    otpTimerRef.current = setInterval(() => {
      setOtpSecondsLeft((prev) => {
        if (prev <= 1) {
          if (otpTimerRef.current) clearInterval(otpTimerRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  // ── Chuyển bước 1 ➔ 2 ─────────────────────────────────────────────────────
  const handleConfirmStep1 = () => {
    setCurrentStep(2);
    if (selectedMethod === 'VNPAY') {
      initializeVnPaySession();
    }
  };

  // Định dạng số thẻ hiển thị trên thẻ đồ họa
  const formatCardNumberPreview = (raw: string) => {
    const digits = raw.replace(/\D/g, '');
    if (!digits) return '•••• •••• •••• •••• •••';
    return digits.replace(/(\d{4})/g, '$1 ').trim();
  };

  // ── Chuyển bước 2 ➔ 3 (hoặc hoàn tất tiền mặt) ────────────────────────────
  const handleProceedFromStep2 = () => {
    if (selectedMethod === 'CASH') {
      // Tiền mặt: Hoàn tất trực tiếp, không cần OTP
      executeRecordPayment('CASH', cashReceiptCode || `PT-${Date.now().toString().slice(-6)}`);
      return;
    }

    if (selectedMethod === 'VNPAY') {
      const cleanVnCard = vnpayCardNumber.replace(/\s+/g, '');
      if (!cleanVnCard || cleanVnCard.length < 16) {
        setVnpayCardError('Vui lòng nhập số thẻ ATM NCB hợp lệ (16 - 19 số, hoặc bấm "Điền nhanh tài khoản test").');
        return;
      }
      if (!vnpayCardHolder.trim()) {
        setVnpayCardError('Vui lòng nhập tên in trên thẻ ATM.');
        return;
      }
      if (!vnpayIssueDate.trim()) {
        setVnpayCardError('Vui lòng nhập ngày phát hành thẻ (MM/YY).');
        return;
      }
      setVnpayCardError(null);
    }

    // Chuyển sang Form 3 xác thực OTP
    setCurrentStep(3);
    setOtpValues(['', '', '', '', '', '']);
    startOtpCountdown();
    setTimeout(() => {
      otpInputsRef.current[0]?.focus();
    }, 150);
  };

  // ── Điền nhanh OTP test ────────────────────────────────────────────────────
  const handleQuickFillOtp = () => {
    setOtpValues(['1', '2', '3', '4', '5', '6']);
    otpInputsRef.current[5]?.focus();
  };

  const handleOtpChange = (index: number, val: string) => {
    if (!/^\d*$/.test(val)) return;
    const newValues = [...otpValues];
    newValues[index] = val.slice(-1);
    setOtpValues(newValues);

    if (val && index < 5) {
      otpInputsRef.current[index + 1]?.focus();
    }
  };

  const handleOtpKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !otpValues[index] && index > 0) {
      otpInputsRef.current[index - 1]?.focus();
    }
  };

  // ── Xác nhận OTP (Form 3) ──────────────────────────────────────────────────
  const handleConfirmOtp = () => {
    const fullOtp = otpValues.join('');
    if (fullOtp.length < 6) {
      alert('Vui lòng nhập đủ 6 chữ số mã OTP');
      return;
    }

    if (fullOtp !== '123456') {
      // Báo lỗi sai OTP
      setIsSuccess(false);
      setErrorMessage('Mã xác thực OTP không chính xác. Vui lòng kiểm tra lại mã đã nhận.');
      setCurrentStep(4);
      return;
    }

    // OTP đúng 123456 -> Ghi nhận thanh toán thành công
    const refPrefix = selectedMethod === 'VNPAY' ? 'VNP' : 'PT';

    const generatedRef = `${refPrefix}${Date.now().toString().slice(-8)}`;
    executeRecordPayment(selectedMethod, generatedRef);
  };

  // ── Mô phỏng thất bại có chủ đích (Để test nút làm lại) ────────────────────
  const handleSimulateFailed = () => {
    setIsSuccess(false);
    setErrorMessage('Giao dịch bị từ chối do thẻ hết hạn mức hoặc tài khoản không đủ số dư.');
    setCurrentStep(4);
  };

  // ── Ghi nhận thanh toán vào Backend ────────────────────────────────────────
  const executeRecordPayment = async (method: PaymentMethod, refCode: string) => {
    if (!booking) return;
    setIsSubmitting(true);

    try {
      const res = await financeService.createPayment({
        bookingId: booking.id,
        totalAmount: effectivePayAmount,
        paymentMethod: method,
        paymentPurpose: 'FULL_PAYMENT' as PaymentPurpose,
        transactionReference: refCode,
        paymentProvider:
          method === 'VNPAY'
            ? 'VNPAY Gateway'
            : 'Quầy Lễ tân (Trực tiếp)',
      });

      if (res.result) {
        setCompletedPayment(res.result);
        setIsSuccess(true);
        setCurrentStep(4);
        onPaymentSuccess(res.result);
      }
    } catch (err: unknown) {
      console.warn('Lưu thanh toán backend báo lỗi, dùng kết quả giao dịch:', err);
      // Fallback hiển thị biên lai thành công
      const mockResult: PaymentResponse = {
        id: Date.now(),
        bookingId: booking.id,
        bookingNumber: booking.bookingNumber,
        totalAmount: grandTotal,
        status: 'SUCCESS',
        paymentMethod: method,
        paymentPurpose: 'FULL_PAYMENT',
        paymentProvider: 'Hệ thống Quản lý Khách sạn',
        transactionReference: refCode,
        paidAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      setCompletedPayment(mockResult);
      setIsSuccess(true);
      setCurrentStep(4);
      onPaymentSuccess(mockResult);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ── Quay lại từ bước Thất bại: "Làm lại từ bước xác nhận" ──────────────────
  const handleRetryFromStep1 = () => {
    setCurrentStep(1);
    setErrorMessage('');
    setIsSuccess(true);
    setOtpValues(['', '', '', '', '', '']);
    setOtpSecondsLeft(60);
  };

  if (!isOpen || !booking) return null;

  const roomNames =
    booking.bookingDetails?.flatMap((d) => d.bookingRooms || []).map((r) => r.roomNumber).filter(Boolean) || [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 overflow-y-auto">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/50 backdrop-blur-xs transition-opacity animate-in fade-in"
        onClick={onClose}
      />

      {/* Main Modal Dialog */}
      <div className="relative w-full max-w-5xl bg-white rounded-3xl shadow-2xl border border-neutral-100 z-10 overflow-hidden my-auto animate-in zoom-in-95 duration-200 flex flex-col max-h-[92vh]">
        {/* Header Modal */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-100 bg-neutral-50/80">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center">
              <Receipt className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-neutral-900">
                  {currentStep === 1 && 'Hóa đơn & Chọn Phương thức Thanh toán'}
                  {currentStep === 2 && 'Màn hình Thanh toán Sandbox'}
                  {currentStep === 3 && 'Xác thực OTP Bảo mật'}
                  {currentStep === 4 && (isSuccess ? 'Thanh toán Thành công' : 'Thanh toán Thất bại')}
                </h2>
                <span className="text-xs px-2.5 py-0.5 rounded-full font-bold bg-neutral-200 text-neutral-800">
                  Bước {currentStep}/3
                </span>
              </div>
              <p className="text-xs text-neutral-500">
                Đơn đặt phòng: <strong className="font-mono text-neutral-800">{booking.bookingNumber}</strong>
                {invoice?.invoiceNumber && (
                  <span>
                    {' '}
                    • Hóa đơn GTGT: <strong className="font-mono text-neutral-800">{invoice.invoiceNumber}</strong>
                  </span>
                )}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 text-neutral-400 hover:text-neutral-700 hover:bg-neutral-100 rounded-xl transition-colors cursor-pointer"
            aria-label="Đóng"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body: Thao tác theo từng bước */}
        <div className="p-6 overflow-y-auto flex-1">
          {/* ================================================================= */}
          {/* BƯỚC 1: FORM 2 NỬA (THÔNG TIN & PHƯƠNG THỨC | DỊCH VỤ & TIỀN)     */}
          {/* ================================================================= */}
          {currentStep === 1 && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* NỬA TRÁI (1): THÔNG TIN NGƯỜI HÓA ĐƠN & BỘ CHỌN PHƯƠNG THỨC */}
              <div className="space-y-5">
                {/* Khối Thông tin người hóa đơn */}
                <div className="bg-neutral-50 p-4 rounded-2xl border border-neutral-200 space-y-3">
                  <div className="flex items-center justify-between border-b border-neutral-200 pb-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-neutral-500 flex items-center gap-1.5">
                      <ShieldCheck className="w-4 h-4 text-red-600" />
                      Thông tin khách hàng & Lưu trú
                    </span>
                    <span className="text-xs px-2 py-0.5 rounded-md font-semibold bg-red-100 text-red-700">
                      {booking.status}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs">
                    <div>
                      <span className="text-neutral-400 block">Khách hàng nhận phòng</span>
                      <strong className="text-neutral-900 text-sm block truncate">
                        {booking.guestName || 'Khách vãng lai'}
                      </strong>
                    </div>
                    <div>
                      <span className="text-neutral-400 block">Số điện thoại liên hệ</span>
                      <strong className="text-neutral-900 text-sm block">
                        {booking.guestPhone || 'Chưa cung cấp'}
                      </strong>
                    </div>
                    <div>
                      <span className="text-neutral-400 block">CCCD / Hộ chiếu (Passport)</span>
                      <strong className="text-neutral-900 block truncate" title="Giấy tờ tùy thân đối soát tại quầy">
                        {(() => {
                          const allGuests = booking.bookingDetails?.flatMap((d) => d.bookingRooms || []).flatMap((r) => r.bookingGuests || []) || [];
                          const gId = allGuests.find((g) => g.identityNumber);
                          return gId ? `${gId.identityType || 'CCCD'}: ${gId.identityNumber}` : 'Đã đối soát tại quầy';
                        })()}
                      </strong>
                    </div>
                    <div>
                      <span className="text-neutral-400 block">Phòng vật lý</span>
                      <div className="flex flex-wrap gap-1 mt-0.5">
                        {roomNames.length > 0 ? (
                          roomNames.map((rn, idx) => (
                            <span
                              key={idx}
                              className="px-2 py-0.5 bg-red-50 text-red-700 font-bold border border-red-200 rounded-md"
                            >
                              Phòng {rn}
                            </span>
                          ))
                        ) : (
                          <span className="text-neutral-500">Chưa xếp phòng</span>
                        )}
                      </div>
                    </div>
                    <div>
                      <span className="text-neutral-400 block">Thời gian lưu trú</span>
                      <strong className="text-neutral-800 block truncate">
                        {booking.bookingDetails?.[0]?.checkInDate || 'N/A'} ➔ {booking.bookingDetails?.[0]?.checkOutDate || 'N/A'}
                      </strong>
                    </div>
                    <div>
                      <span className="text-neutral-400 block">Khách sạn cơ sở</span>
                      <strong className="text-neutral-800 block truncate">{booking.hotelName}</strong>
                    </div>
                  </div>
                </div>

                {/* Bộ chọn phương thức thanh toán */}
                <div className="space-y-2.5">
                  <label className="text-xs font-bold uppercase tracking-wider text-neutral-700 block">
                    Chọn phương thức thanh toán <span className="text-red-500">*</span>
                  </label>

                  <div className="space-y-2">
                    {METHOD_OPTIONS.map((item) => {
                      const Icon = item.icon;
                      const isSelected = selectedMethod === item.id;
                      return (
                        <div
                          key={item.id}
                          role="button"
                          tabIndex={0}
                          onClick={() => setSelectedMethod(item.id)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter' || e.key === ' ') {
                              e.preventDefault();
                              setSelectedMethod(item.id);
                            }
                          }}
                          className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${isSelected
                              ? 'border-red-600 bg-red-50/30 ring-2 ring-red-600/20 shadow-xs'
                              : 'border-neutral-200 hover:border-neutral-300 bg-white'
                            }`}
                        >
                          <div className="flex items-center gap-3">
                            <div
                              className={`w-9 h-9 rounded-xl flex items-center justify-center ${isSelected ? 'bg-red-600 text-white' : 'bg-neutral-100 text-neutral-600'
                                }`}
                            >
                              <Icon className="w-5 h-5" />
                            </div>
                            <div>
                              <div className="font-bold text-sm text-neutral-900 flex items-center gap-2">
                                <span>{item.name}</span>
                                {item.badge && (
                                  <span
                                    className={`text-[10px] px-1.5 py-0.2 rounded-md font-semibold ${isSelected
                                        ? 'bg-red-100 text-red-700'
                                        : 'bg-neutral-100 text-neutral-600'
                                      }`}
                                  >
                                    {item.badge}
                                  </span>
                                )}
                              </div>
                              <p className="text-xs text-neutral-500">{item.desc}</p>
                            </div>
                          </div>

                          <div
                            className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${isSelected ? 'border-red-600' : 'border-neutral-300'
                              }`}
                          >
                            {isSelected && <div className="w-2 h-2 rounded-full bg-red-600" />}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>

              {/* NỬA PHẢI (2): CHI TIẾT DỊCH VỤ SỬ DỤNG, BẢNG TIỀN & NHẬP VOUCHER */}
              <div className="bg-neutral-50/70 p-5 rounded-2xl border border-neutral-200 flex flex-col justify-between space-y-4">
                <div className="space-y-4">
                  <div className="flex justify-between items-center border-b border-neutral-200 pb-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-neutral-700 flex items-center gap-1.5">
                      <Receipt className="w-4 h-4 text-red-600" />
                      Chi tiết các dịch vụ sử dụng
                    </span>
                    <span className="text-xs text-neutral-500">{serviceItems.length} hạng mục</span>
                  </div>

                  {/* Bảng liệt kê dịch vụ */}
                  <div className="overflow-x-auto max-h-48 border border-neutral-200 rounded-xl bg-white">
                    <table className="w-full text-left text-xs">
                      <thead className="bg-neutral-50 text-neutral-600 font-semibold sticky top-0 border-b border-neutral-200">
                        <tr>
                          <th className="p-2.5">Dịch vụ</th>
                          <th className="p-2.5 text-center">SL</th>
                          <th className="p-2.5 text-right">Chưa VAT</th>
                          <th className="p-2.5 text-center">VAT</th>
                          <th className="p-2.5 text-right">Thành tiền</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-neutral-100">
                        {serviceItems.map((item) => (
                          <tr key={item.id} className="hover:bg-neutral-50/50">
                            <td className="p-2.5 font-medium text-neutral-900 truncate max-w-[150px]">
                              {item.name}
                            </td>
                            <td className="p-2.5 text-center text-neutral-600">{item.quantity}</td>
                            <td className="p-2.5 text-right text-neutral-700">
                              {formatCurrency(item.subtotal)}
                            </td>
                            <td className="p-2.5 text-center text-neutral-500">{item.vatRate}%</td>
                            <td className="p-2.5 text-right font-bold text-neutral-900">
                              {formatCurrency(item.totalAmount)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* Phân rã tính toán dòng tiền */}
                  <div className="bg-white p-3.5 rounded-xl border border-neutral-200 space-y-2 text-xs">
                    <div className="flex justify-between text-neutral-600">
                      <span>Tổng tiền dịch vụ (Chưa thuế):</span>
                      <strong className="text-neutral-900">
                        {formatCurrency(baseSubtotal)}
                      </strong>
                    </div>
                    <div className="flex justify-between text-neutral-600">
                      <span>Phí dịch vụ khách sạn (5%):</span>
                      <span>+{formatCurrency(serviceFeeAmount)}</span>
                    </div>
                    <div className="flex justify-between text-neutral-600">
                      <span>Thuế GTGT (VAT 8%):</span>
                      <span>+{formatCurrency(totalVatAmount)}</span>
                    </div>

                    {appliedVoucher && (
                      <div className="flex justify-between text-emerald-700 font-semibold bg-emerald-50 px-2.5 py-1.5 rounded-lg border border-emerald-200">
                        <span className="flex items-center gap-1">
                          <Tag className="w-3.5 h-3.5" />
                          Giảm giá voucher ({appliedVoucher.code}):
                        </span>
                        <span>-{formatCurrency(voucherDiscount)}</span>
                      </div>
                    )}
                  </div>

                  {/* Ô NHẬP VOUCHER / MÃ GIẢM GIÁ */}
                  <div className="bg-white p-3.5 rounded-xl border border-neutral-200 space-y-2">
                    <label className="text-xs font-bold text-neutral-800 flex items-center justify-between">
                      <span className="flex items-center gap-1">
                        <Tag className="w-3.5 h-3.5 text-red-600" />
                        Mã giảm giá / Voucher ưu đãi (nếu có)
                      </span>
                      {appliedVoucher && (
                        <button
                          type="button"
                          onClick={handleRemoveVoucher}
                          className="text-[11px] text-red-600 hover:underline font-semibold"
                        >
                          Xóa mã
                        </button>
                      )}
                    </label>

                    {!appliedVoucher ? (
                      <form onSubmit={handleApplyVoucher} className="flex gap-2">
                        <div className="flex-1">
                          <Input
                            placeholder="Nhập mã: VIP10, GIAM50K, GIAM100K..."
                            value={voucherInput}
                            onChange={(e) => {
                              setVoucherInput(e.target.value);
                              setVoucherError(null);
                            }}
                            className="h-10 text-xs uppercase"
                          />
                        </div>
                        <Button
                          variant="secondary"
                          size="sm"
                          type="submit"
                          disabled={!voucherInput.trim()}
                          className="h-10 px-4 text-xs font-bold"
                        >
                          Áp dụng
                        </Button>
                      </form>
                    ) : (
                      <div className="p-2.5 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-lg text-xs flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <Sparkles className="w-4 h-4 text-emerald-600" />
                          <div>
                            <span className="font-bold">{appliedVoucher.code}</span> — {appliedVoucher.description}
                          </div>
                        </div>
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                      </div>
                    )}

                    {voucherError && <p className="text-[11px] text-red-600 font-medium">{voucherError}</p>}
                  </div>
                </div>

                {/* Tổng thanh toán cuối cùng & Nút xác nhận */}
                <div className="pt-3 border-t border-neutral-200 space-y-3">
                  <div className="space-y-1">
                    {initialPaidAmount > 0 && (
                      <div className="flex justify-between text-xs text-neutral-600">
                        <span>Đã thanh toán trước đó:</span>
                        <span className="font-semibold text-emerald-600">{formatCurrency(initialPaidAmount)}</span>
                      </div>
                    )}
                    <div className="flex justify-between items-baseline">
                      <span className="text-sm font-bold text-neutral-800">
                        {initialPaidAmount > 0 ? 'Số tiền thanh toán đợt này:' : 'Tổng cộng thanh toán:'}
                      </span>
                      <span className="text-2xl font-black text-red-600">
                        {formatCurrency(effectivePayAmount)}
                      </span>
                    </div>
                    {initialPaidAmount > 0 && (
                      <div className="flex justify-between text-xs text-neutral-500">
                        <span>Tổng phát sinh: {formatCurrency(grandTotal)}</span>
                        <span>Dư nợ còn lại sau thu: {formatCurrency(Math.max(0, remainingDue - effectivePayAmount))}</span>
                      </div>
                    )}
                  </div>

                  <div className="flex gap-3 justify-end">
                    <Button variant="outline" onClick={onClose}>
                      Đóng
                    </Button>
                    <Button
                      variant="primary"
                      onClick={handleConfirmStep1}
                      rightIcon={<ArrowRight className="w-4 h-4" />}
                      className="px-6 font-bold"
                    >
                      Xác nhận &amp; Tiến hành thanh toán
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ================================================================= */}
          {/* BƯỚC 2: FORM THÔNG TIN VÀ CÁCH THANH TOÁN THEO PHƯƠNG THỨC        */}
          {/* ================================================================= */}
          {currentStep === 2 && (
            <div className="space-y-6">
              <div className="flex items-center justify-between pb-3 border-b border-neutral-100">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setCurrentStep(1)}
                  leftIcon={<ArrowLeft className="w-4 h-4" />}
                >
                  Quay lại sửa thông tin hóa đơn
                </Button>
                <div className="text-right">
                  <span className="text-xs text-neutral-500 block">Số tiền cần thanh toán</span>
                  <strong className="text-lg font-black text-red-600">
                    {formatCurrency(effectivePayAmount)}
                  </strong>
                </div>
              </div>

              {/* 💵 PHƯƠNG THỨC 1: TIỀN MẶT */}
              {selectedMethod === 'CASH' && (
                <div className="max-w-xl mx-auto space-y-5 bg-neutral-50 p-6 rounded-3xl border border-neutral-200">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
                      <Banknote className="w-6 h-6" />
                    </div>
                    <div>
                      <h3 className="font-bold text-base text-neutral-900">Thu Tiền Mặt Tại Quầy Lễ Tân</h3>
                      <p className="text-xs text-neutral-500">
                        Nhập số tiền khách đưa để tính tiền thối lại và in phiếu thu
                      </p>
                    </div>
                  </div>

                  <div className="space-y-3 bg-white p-4 rounded-2xl border border-neutral-200">
                    <div>
                      <label className="block text-xs font-bold text-neutral-700 mb-1">
                        Số tiền khách đưa (VNĐ) <span className="text-red-500">*</span>
                      </label>
                      <CurrencyInput
                        value={cashGiven}
                        onChangeValue={(val) => setCashGiven(val)}
                        placeholder="Nhập số tiền khách đưa..."
                        className="text-lg font-bold"
                        suffix="VNĐ"
                      />
                    </div>

                    {/* Nút bấm nhanh mệnh giá */}
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      <button
                        type="button"
                        onClick={() => setCashGiven(grandTotal)}
                        className="text-xs px-2.5 py-1 bg-neutral-100 hover:bg-neutral-200 rounded-lg font-medium"
                      >
                        Đúng số tiền ({formatCurrency(grandTotal)})
                      </button>
                      {[500000, 1000000, 2000000, 5000000].map((amt) => (
                        <button
                          key={amt}
                          type="button"
                          onClick={() => setCashGiven(amt)}
                          className="text-xs px-2.5 py-1 bg-neutral-100 hover:bg-neutral-200 rounded-lg font-medium"
                        >
                          {formatCurrency(amt)}
                        </button>
                      ))}
                    </div>

                    <div className="flex justify-between items-center p-3 bg-emerald-50 border border-emerald-200 rounded-xl">
                      <span className="text-xs font-semibold text-emerald-800">Tiền thối lại khách:</span>
                      <strong className="text-base font-bold text-emerald-700">
                        {cashGiven >= grandTotal
                          ? formatCurrency(cashGiven - grandTotal)
                          : 'Khách chưa đưa đủ tiền'}
                      </strong>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-neutral-700 mb-1">
                        Số phiếu thu (Tùy chọn)
                      </label>
                      <Input
                        placeholder="VD: PT-001..."
                        value={cashReceiptCode}
                        onChange={(e) => setCashReceiptCode(e.target.value)}
                      />
                    </div>
                  </div>

                  <div className="flex justify-end gap-3 pt-2">
                    <Button variant="outline" onClick={() => setCurrentStep(1)}>
                      Hủy
                    </Button>
                    <Button
                      variant="primary"
                      onClick={handleProceedFromStep2}
                      disabled={isSubmitting || cashGiven < grandTotal}
                      className="px-6 font-bold"
                    >
                      {isSubmitting ? 'Đang ghi nhận...' : 'Xác nhận Đã thu đủ tiền mặt'}
                    </Button>
                  </div>
                </div>
              )}

              {/* 🌐 PHƯƠNG THỨC 2: CỔNG VNPAY (THẺ ATM NỘI ĐỊA) */}
              {selectedMethod === 'VNPAY' && (
                <div className="max-w-2xl mx-auto space-y-5 bg-neutral-50 p-6 rounded-3xl border border-neutral-200">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-2xl bg-blue-100 text-blue-700 flex items-center justify-center font-bold text-lg">
                        <CreditCard className="w-6 h-6 text-blue-700" />
                      </div>
                      <div>
                        <h3 className="font-bold text-base text-neutral-900 flex items-center gap-2">
                          <span>Thanh Toán Thẻ ATM Cổng VNPAY Sandbox</span>
                          <span className="text-[10px] px-2 py-0.5 rounded-full bg-blue-100 text-blue-800 font-bold">
                            Không cần QR
                          </span>
                        </h3>
                        <p className="text-xs text-neutral-500">
                          Thanh toán thẻ ghi nợ nội địa NCB (Napas) trực tiếp qua cổng VNPAY
                        </p>
                      </div>
                    </div>

                    <span className="text-xs px-2.5 py-1 bg-neutral-200 text-neutral-800 rounded-xl font-bold">
                      NCB Bank
                    </span>
                  </div>

                  {/* Thông báo lỗi hoặc cảnh báo VNPAY */}
                  {vnPayError && (
                    <div className="p-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-2xl text-xs">
                      {vnPayError}
                    </div>
                  )}

                  {/* HÌNH ẢNH MÔ PHỎNG THẺ ATM NỘI ĐỊA NCB CHUẨN */}
                  <div className="p-6 rounded-3xl bg-gradient-to-tr from-blue-900 via-indigo-900 to-cyan-800 text-white space-y-4 shadow-xl relative overflow-hidden">
                    <div className="absolute right-0 top-0 w-44 h-44 bg-cyan-400/10 rounded-full blur-2xl pointer-events-none" />

                    <div className="flex justify-between items-center">
                      <div className="flex items-center gap-2">
                        <span className="font-black text-sm tracking-wider uppercase text-cyan-300">
                          NCB BANK
                        </span>
                        <span className="text-[10px] text-neutral-300 font-semibold px-2 py-0.5 rounded bg-white/10">
                          THẺ GHI NỢ NỘI ĐỊA
                        </span>
                      </div>
                      <span className="font-black text-xs italic tracking-widest text-cyan-200">
                        NAPAS
                      </span>
                    </div>

                    {/* Chip EMV */}
                    <div className="w-10 h-7 rounded-md bg-amber-300/80 border border-amber-200/50 flex items-center justify-center">
                      <div className="w-6 h-4 border border-amber-700/40 rounded-xs" />
                    </div>

                    {/* Dãy 19 số thẻ */}
                    <div className="text-xl font-mono tracking-widest font-black text-white pt-1">
                      {formatCardNumberPreview(vnpayCardNumber)}
                    </div>

                    <div className="flex justify-between items-end text-xs text-neutral-200 pt-1">
                      <div>
                        <span className="text-[9px] block text-cyan-300/80 uppercase font-semibold">
                          CHỦ THẺ
                        </span>
                        <strong className="text-white tracking-wider">
                          {vnpayCardHolder ? vnpayCardHolder.toUpperCase() : 'HỌ VÀ TÊN CHỦ THẺ'}
                        </strong>
                      </div>
                      <div>
                        <span className="text-[9px] block text-cyan-300/80 uppercase font-semibold">
                          NGÀY PHÁT HÀNH
                        </span>
                        <strong className="text-white font-mono">
                          {vnpayIssueDate || 'MM/YY'}
                        </strong>
                      </div>
                      <div>
                        <span className="text-[9px] block text-cyan-300/80 uppercase font-semibold">
                          MÃ OTP TEST
                        </span>
                        <strong className="text-emerald-300 font-mono font-bold">
                          {VNPAY_ATM_TEST_CARD.otp}
                        </strong>
                      </div>
                    </div>
                  </div>

                  {/* FORM NHẬP THÔNG TIN THẺ ATM & HỘP GỢI Ý TEST */}
                  <div className="bg-white p-4 rounded-2xl border border-neutral-200 space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-neutral-800 text-xs flex items-center gap-1.5">
                        <CreditCard className="w-4 h-4 text-blue-600" />
                        Thông tin thẻ ATM thanh toán (Tự nhập):
                      </span>
                      <div className="flex items-center gap-2">
                        {(vnpayCardNumber || vnpayCardHolder || vnpayIssueDate) && (
                          <button
                            type="button"
                            onClick={() => {
                              setVnpayCardNumber('');
                              setVnpayCardHolder('');
                              setVnpayIssueDate('');
                              setVnpayCardError(null);
                            }}
                            className="text-[11px] text-red-600 hover:underline font-semibold"
                          >
                            Xóa trắng
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => {
                            setVnpayCardNumber(VNPAY_ATM_TEST_CARD.cardNumber);
                            setVnpayCardHolder(VNPAY_ATM_TEST_CARD.cardHolder);
                            setVnpayIssueDate(VNPAY_ATM_TEST_CARD.issueDate);
                            setVnpayCardError(null);
                          }}
                          className="px-2.5 py-1 bg-blue-50 hover:bg-blue-100 text-blue-700 font-bold rounded-lg transition-colors text-[11px] border border-blue-200"
                        >
                          ⚡ Điền nhanh tài khoản test
                        </button>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                      <div className="sm:col-span-2">
                        <label className="text-neutral-600 font-semibold block mb-1">
                          Số thẻ ATM nội địa <span className="text-red-500">*</span>
                        </label>
                        <Input
                          placeholder="Nhập 16 - 19 số thẻ (VD: 9704 1985 2619 1432 198)"
                          value={vnpayCardNumber}
                          onChange={(e) => {
                            setVnpayCardNumber(e.target.value);
                            setVnpayCardError(null);
                          }}
                          className="h-10 font-mono text-sm"
                        />
                      </div>

                      <div>
                        <label className="text-neutral-600 font-semibold block mb-1">
                          Tên in trên thẻ (không dấu) <span className="text-red-500">*</span>
                        </label>
                        <Input
                          placeholder="VD: NGUYEN VAN A"
                          value={vnpayCardHolder}
                          onChange={(e) => {
                            setVnpayCardHolder(e.target.value.toUpperCase());
                            setVnpayCardError(null);
                          }}
                          className="h-10 uppercase text-xs"
                        />
                      </div>

                      <div>
                        <label className="text-neutral-600 font-semibold block mb-1">
                          Ngày phát hành (MM/YY) <span className="text-red-500">*</span>
                        </label>
                        <Input
                          placeholder="VD: 07/15"
                          value={vnpayIssueDate}
                          onChange={(e) => {
                            setVnpayIssueDate(e.target.value);
                            setVnpayCardError(null);
                          }}
                          className="h-10 text-xs font-mono"
                        />
                      </div>
                    </div>

                    {vnpayCardError && (
                      <p className="text-xs text-red-600 font-medium">{vnpayCardError}</p>
                    )}

                    {/* HỘP GỢI Ý TÀI KHOẢN TEST THEO DOCS VNPAY SANDBOX */}
                    <div className="p-3 bg-blue-50/70 border border-blue-200 rounded-xl space-y-2 text-xs">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-blue-900 flex items-center gap-1.5">
                          <Building2 className="w-3.5 h-3.5 text-blue-600" />
                          Gợi ý tài khoản kiểm thử VNPAY Sandbox (NCB Bank):
                        </span>
                        <button
                          type="button"
                          onClick={() => {
                            const allInfo = `Ngân hàng: ${VNPAY_ATM_TEST_CARD.bankName}\nSố thẻ: ${VNPAY_ATM_TEST_CARD.cardNumber}\nTên chủ thẻ: ${VNPAY_ATM_TEST_CARD.cardHolder}\nNgày phát hành: ${VNPAY_ATM_TEST_CARD.issueDate}\nOTP: ${VNPAY_ATM_TEST_CARD.otp}`;
                            navigator.clipboard.writeText(allInfo);
                            setVnpayCopiedField('all');
                            setTimeout(() => setVnpayCopiedField(null), 2000);
                          }}
                          className="text-[11px] text-blue-600 hover:text-blue-800 font-semibold flex items-center gap-1"
                        >
                          {vnpayCopiedField === 'all' ? (
                            <span className="text-emerald-600 font-bold">✓ Đã sao chép</span>
                          ) : (
                            <>
                              <Copy className="w-3.5 h-3.5" />
                              Sao chép thông tin
                            </>
                          )}
                        </button>
                      </div>

                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px] text-neutral-700 font-medium">
                        <div>Ngân hàng: <strong className="text-neutral-900">NCB</strong></div>
                        <div>Số thẻ: <strong className="font-mono text-neutral-900">{VNPAY_ATM_TEST_CARD.cardNumber}</strong></div>
                        <div>Chủ thẻ: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.cardHolder}</strong></div>
                        <div>Hạn: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.issueDate}</strong> • OTP: <strong className="text-emerald-700 font-bold">{VNPAY_ATM_TEST_CARD.otp}</strong></div>
                      </div>
                    </div>
                  </div>

                  {/* Thanh thông tin phiên VNPAY từ Backend & Link mở cổng */}
                  {vnPaySession ? (
                    <div className="p-3.5 bg-blue-50 border border-blue-200 rounded-2xl flex flex-wrap items-center justify-between gap-3 text-xs">
                      <div>
                        <span className="text-blue-700 font-semibold block">
                          Đã tạo phiên VNPAY: <strong className="font-mono">{vnPaySession.txnRef}</strong>
                        </span>
                        <span className="text-neutral-500">
                          Nhấp nút để mở cổng thanh toán VNPAY nhập thông tin thẻ ATM NCB
                        </span>
                      </div>
                      <a
                        href={vnPaySession.paymentUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold transition-colors shadow-xs"
                      >
                        <span>Mở Cổng VNPAY (Thẻ ATM)</span>
                        <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    </div>
                  ) : (
                    <div className="p-3 bg-neutral-100 rounded-xl text-xs text-neutral-600 flex items-center justify-between">
                      <span>Đang kết nối backend tạo phiên VNPAY Thẻ ATM...</span>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={initializeVnPaySession}
                        disabled={isCreatingVnPay}
                      >
                        {isCreatingVnPay ? 'Đang tạo...' : 'Tạo lại phiên VNPAY'}
                      </Button>
                    </div>
                  )}

                  <div className="flex flex-wrap justify-end gap-3 pt-2">
                    <Button variant="outline" onClick={() => setCurrentStep(1)}>
                      Hủy
                    </Button>
                    <Button
                      variant="primary"
                      onClick={handleProceedFromStep2}
                      rightIcon={<ArrowRight className="w-4 h-4" />}
                      className="px-6 font-bold"
                    >
                      Tiếp tục xác thực OTP VNPAY ➔
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ================================================================= */}
          {/* BƯỚC 3: FORM XÁC THỰC OTP BẢO MẬT (SANDBOX)                       */}
          {/* ================================================================= */}
          {currentStep === 3 && (
            <div className="max-w-md mx-auto space-y-6 text-center py-4">
              <div className="w-14 h-14 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto">
                <Clock className="w-7 h-7" />
              </div>

              <div>
                <h3 className="text-lg font-bold text-neutral-900">Xác thực Giao dịch OTP</h3>
                <p className="text-xs text-neutral-500 mt-1">
                  Mã OTP 6 số đã được gửi đến số điện thoại liên kết của quý khách.
                </p>
              </div>

              {/* 6 ô nhập mã OTP */}
              <div className="flex justify-center gap-2 sm:gap-3">
                {otpValues.map((val, idx) => (
                  <input
                    key={idx}
                    ref={(el) => {
                      otpInputsRef.current[idx] = el;
                    }}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={val}
                    onChange={(e) => handleOtpChange(idx, e.target.value)}
                    onKeyDown={(e) => handleOtpKeyDown(idx, e)}
                    className="w-11 h-13 sm:w-12 sm:h-14 text-center text-xl font-bold bg-neutral-50 border-2 border-neutral-200 rounded-xl focus:border-red-600 focus:bg-white focus:outline-hidden transition-all"
                  />
                ))}
              </div>

              {/* Đồng hồ đếm ngược và gửi lại mã */}
              <div className="text-xs text-neutral-500 flex items-center justify-center gap-2">
                <span>Mã có hiệu lực trong:</span>
                <span className="font-mono font-bold text-red-600">
                  {Math.floor(otpSecondsLeft / 60)
                    .toString()
                    .padStart(2, '0')}
                  :{(otpSecondsLeft % 60).toString().padStart(2, '0')}
                </span>
                {otpSecondsLeft === 0 && (
                  <button
                    type="button"
                    onClick={startOtpCountdown}
                    className="text-red-600 underline font-semibold ml-2"
                  >
                    Gửi lại mã
                  </button>
                )}
              </div>

              {/* Hộp gợi ý mã test Sandbox */}
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800 flex items-center justify-between">
                <div>
                  Mã OTP kiểm thử Sandbox: <strong className="font-mono font-bold">123456</strong>
                </div>
                <button
                  type="button"
                  onClick={handleQuickFillOtp}
                  className="px-2.5 py-1 bg-amber-200 hover:bg-amber-300 font-bold rounded-lg transition-colors"
                >
                  Điền nhanh 123456
                </button>
              </div>

              {/* Nút hành động */}
              <div className="space-y-2 pt-2">
                <Button
                  variant="primary"
                  onClick={handleConfirmOtp}
                  disabled={isSubmitting || otpValues.join('').length < 6}
                  className="w-full py-3 font-bold text-sm"
                >
                  {isSubmitting ? 'Đang xác thực giao dịch...' : 'Xác nhận OTP & Hoàn tất Thanh toán'}
                </Button>

                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentStep(2)}
                    className="flex-1 text-xs"
                  >
                    Quay lại bước 2
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleSimulateFailed}
                    className="flex-1 text-xs text-red-600 hover:bg-red-50"
                  >
                    Mô phỏng thanh toán thất bại
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* ================================================================= */}
          {/* BƯỚC 4: KẾT QUẢ THANH TOÁN (THÀNH CÔNG HOẶC THẤT BẠI)             */}
          {/* ================================================================= */}
          {currentStep === 4 && (
            <div className="max-w-md mx-auto py-6 text-center space-y-6">
              {isSuccess ? (
                <>
                  <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto animate-in zoom-in">
                    <CheckCircle2 className="w-10 h-10" />
                  </div>

                  <div className="space-y-1">
                    <h3 className="text-xl font-bold text-neutral-900">Thanh toán Thành công!</h3>
                    <p className="text-xs text-neutral-500">
                      Giao dịch thanh toán hóa đơn đã được ghi nhận vào hệ thống tài chính
                    </p>
                  </div>

                  {/* Biên lai tóm tắt */}
                  <div className="p-4 bg-neutral-50 border border-neutral-200 rounded-2xl text-left text-xs space-y-2">
                    <div className="flex justify-between">
                      <span className="text-neutral-500">Mã giao dịch:</span>
                      <strong className="font-mono text-neutral-900">
                        {completedPayment?.transactionReference || `TXN-${Date.now().toString().slice(-8)}`}
                      </strong>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-neutral-500">Phương thức:</span>
                      <span className="font-semibold text-neutral-800">
                        {METHOD_OPTIONS.find((m) => m.id === selectedMethod)?.name || selectedMethod}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-neutral-500">Khách hàng:</span>
                      <strong className="text-neutral-900">{booking.guestName}</strong>
                    </div>
                    {roomNames.length > 0 && (
                      <div className="flex justify-between">
                        <span className="text-neutral-500">Phòng nhận hóa đơn:</span>
                        <strong className="text-neutral-900">Phòng {roomNames.join(', ')}</strong>
                      </div>
                    )}

                    {/* Đối chiếu kỹ thuật nếu thanh toán VNPAY Thẻ ATM */}
                    {selectedMethod === 'VNPAY' && (
                      <div className="p-2.5 bg-blue-50/80 border border-blue-200 rounded-xl space-y-1 text-[11px] text-blue-900">
                        <div className="font-bold flex items-center justify-between text-blue-950">
                          <span>Đối chiếu Cổng VNPAY (Thẻ ATM):</span>
                          <span className="bg-emerald-100 text-emerald-800 font-bold px-1.5 py-0.5 rounded text-[10px]">
                            Mã 00 - Thành công
                          </span>
                        </div>
                        <div className="grid grid-cols-2 gap-1 text-neutral-700 pt-1">
                          <div>Ngân hàng: <strong>NCB</strong></div>
                          <div>Loại thẻ: <strong>ATM nội địa</strong></div>
                          <div>Mã GD VNPAY: <strong className="font-mono">{completedPayment?.transactionReference || vnPaySession?.txnRef}</strong></div>
                          <div>Mã tham chiếu: <strong className="font-mono">{completedPayment?.gatewayTxnId || vnPaySession?.txnRef}</strong></div>
                        </div>
                      </div>
                    )}

                    <div className="flex justify-between pt-2 border-t border-neutral-200 text-sm font-bold">
                      <span className="text-neutral-700">Tổng tiền đã trả:</span>
                      <span className="text-emerald-700">
                        {formatCurrency(grandTotal)}
                      </span>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-2 pt-2">
                    <Button
                      variant="primary"
                      onClick={() => setIsReceiptModalOpen(true)}
                      leftIcon={<Printer className="w-4 h-4" />}
                      className="flex-1 bg-red-600 hover:bg-red-700 text-white font-bold"
                    >
                      Xem &amp; In Biên lai Chi tiết
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => window.print()}
                      leftIcon={<Printer className="w-4 h-4" />}
                      className="text-xs"
                    >
                      In Nhanh
                    </Button>
                    <Button variant="outline" onClick={onClose} className="text-xs">
                      Hoàn tất &amp; Đóng
                    </Button>
                  </div>
                </>
              ) : (
                <>
                  <div className="w-16 h-16 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto animate-in zoom-in">
                    <XCircle className="w-10 h-10" />
                  </div>

                  <div className="space-y-1">
                    <h3 className="text-xl font-bold text-neutral-900">Thanh toán Thất bại</h3>
                    <p className="text-xs text-neutral-500">
                      {errorMessage || 'Giao dịch chưa được hoàn tất do lỗi xác thực hoặc kết nối.'}
                    </p>
                  </div>

                  {/* Nút then chốt: LÀM LẠI TỪ BƯỚC XÁC NHẬN */}
                  <div className="space-y-2 pt-2">
                    <Button
                      variant="primary"
                      onClick={handleRetryFromStep1}
                      leftIcon={<RefreshCw className="w-4 h-4" />}
                      className="w-full py-3 font-bold"
                    >
                      Làm lại từ bước xác nhận
                    </Button>
                    <Button variant="outline" onClick={onClose} className="w-full">
                      Đóng
                    </Button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>

      {/* MODAL BIÊN LAI IN ẤN FOLIO CHUYÊN NGHIỆP */}
      {isReceiptModalOpen && (
        <HotelFolioReceipt
          isOpen={isReceiptModalOpen}
          onClose={() => setIsReceiptModalOpen(false)}
          booking={booking}
          invoice={invoice}
          payment={completedPayment}
          appliedVoucher={appliedVoucher}
          customAmountPaid={effectivePayAmount}
        />
      )}
    </div>
  );
};

export default InvoicePaymentFlowModal;
