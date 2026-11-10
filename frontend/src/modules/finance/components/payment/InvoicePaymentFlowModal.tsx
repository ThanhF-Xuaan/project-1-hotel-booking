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
  Sparkles,
  Building2,
  Loader2,
  AlertTriangle,
} from 'lucide-react';
import Button from '../../../../core/components/ui/Button';
import Input from '../../../../core/components/ui/Input';
import CurrencyInput from '../../../../core/components/ui/CurrencyInput';
import { formatCurrency } from '../../../../core/utils/currency';
import { getApiErrorMessage } from '../../../../core/api/error';
import financeService from '../../services/finance.service';
import CountdownBadge from './CountdownBadge';
import PaymentStatusBadge from './PaymentStatusBadge';
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

const generatePaymentRef = (prefix: string, length = 8) =>
  `${prefix}${Date.now().toString().slice(-length)}`;

export interface InvoicePaymentFlowModalProps {
  isOpen: boolean;
  onClose: () => void;
  booking: BookingResponse | null;
  invoice: InvoiceResponse | null;
  onPaymentSuccess: (payment: PaymentResponse) => void;
  initialPaidAmount?: number;
  initialBalanceDue?: number;
}

type PaymentFlowStep = 1 | 2 | 4;

const METHOD_OPTIONS: Array<{
  id: PaymentMethod;
  name: string;
  desc: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string;
}> = [
  {
    id: 'VNPAY',
    name: 'Cổng thanh toán VNPay Sandbox (Thẻ ATM NCB)',
    desc: 'Chuyển hướng trực tiếp sang cổng sandbox.vnpayment.vn & tự cập nhật trạng thái khi quay về',
    icon: CreditCard,
    badge: 'Redirect Sandbox',
  },
  {
    id: 'CASH',
    name: 'Tiền mặt tại quầy',
    desc: 'Thu tiền mặt trực tiếp tại quầy lễ tân (xác nhận SUCCESS ngay)',
    icon: Banknote,
    badge: 'Trực tiếp',
  },
  {
    id: 'BANK_TRANSFER',
    name: 'Chuyển khoản ngân hàng',
    desc: 'Ghi nhận chờ đối soát (PENDING) → Kế toán xác nhận hoàn tất hoặc hủy',
    icon: Building2,
    badge: 'Chờ xác nhận',
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
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('VNPAY');
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

  // ── Amount to collect this session ─────────────────────────────────────────
  const [customPayAmount, setCustomPayAmount] = useState<number>(0);

  // ── Cash & Bank Transfer Details (Step 2) ──────────────────────────────────
  const [cashGiven, setCashGiven] = useState<number>(0);
  const [cashReceiptCode, setCashReceiptCode] = useState('');
  const [bankRefCode, setBankRefCode] = useState('');
  const [bankProviderName, setBankProviderName] = useState('');
  const [bankFormError, setBankFormError] = useState<string | null>(null);

  // ── VNPay State & Real API Redirect / Polling ──────────────────────────────
  const [vnPaySession, setVnPaySession] = useState<PaymentUrlResponse | null>(null);
  const [isCreatingVnPay, setIsCreatingVnPay] = useState(false);
  const [vnPayError, setVnPayError] = useState<{ code?: number; message: string } | null>(null);
  const [blockingPendingId, setBlockingPendingId] = useState<number | null>(null);
  const [isCancellingSession, setIsCancellingSession] = useState(false);
  const [vnpayCopiedField, setVnpayCopiedField] = useState<string | null>(null);
  const vnpayPollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // ── Final Result State (Step 4) ────────────────────────────────────────────
  const [isSuccess, setIsSuccess] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [completedPayment, setCompletedPayment] = useState<PaymentResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // ── Reset state when modal opens or closes ─────────────────────────────────
  useEffect(() => {
    if (!isOpen) {
      if (vnpayPollRef.current) {
        clearInterval(vnpayPollRef.current);
        vnpayPollRef.current = null;
      }
      return;
    }

    let ignore = false;
    const resetOnOpen = async () => {
      await Promise.resolve();
      if (!ignore) {
        setCurrentStep(1);
        setSelectedMethod('VNPAY');
        setVoucherInput('');
        setAppliedVoucher(null);
        setVoucherError(null);
        setCashReceiptCode('');
        setBankRefCode('');
        setBankProviderName('');
        setBankFormError(null);
        setVnPaySession(null);
        setVnPayError(null);
        setBlockingPendingId(null);
        setIsSuccess(true);
        setErrorMessage('');
        setCompletedPayment(null);
      }
    };
    resetOnOpen();
    return () => {
      ignore = true;
    };
  }, [isOpen]);

  // ── Dịch vụ & Chiết tính chi phí hiển thị theo từng phòng ─────────────────
  interface ModalFolioItem {
    id: number | string;
    roomNumber?: string;
    roomTypeName?: string;
    name: string;
    quantity: number;
    unitPrice: number;
    vatRate: number;
    vatAmount: number;
    subtotal: number;
    totalAmount: number;
  }

  const serviceItems: ModalFolioItem[] = useMemo(() => {
    if (invoice?.details && invoice.details.length > 0) {
      return invoice.details.map((d): ModalFolioItem => {
        const match = d.description.match(/(?:Phòng|P\.)\s*([A-Za-z0-9-]+)/i);
        const extractedRoom = match ? match[1] : undefined;

        return {
          id: d.id,
          roomNumber: extractedRoom,
          roomTypeName: undefined,
          name: d.description,
          quantity: d.quantity,
          unitPrice: Number(d.unitPrice),
          vatRate: Number(d.vatRate || 8),
          vatAmount: Number(d.vatAmount),
          subtotal: Number(d.subtotal),
          totalAmount: Number(d.totalAmount),
        };
      });
    }

    // Nếu chưa có hóa đơn chính thức, phân tích từ booking theo từng phòng
    if (!booking) return [];

    const items: ModalFolioItem[] = [];

    let itemId = 1;

    // Chi tiết phòng lưu trú theo từng phòng
    booking.bookingDetails?.forEach((detail) => {
      const roomTypeName = detail.roomTypeName || 'Phòng Khách sạn';
      const assignedRoomsInDetail = detail.bookingRooms || [];

      if (assignedRoomsInDetail.length > 0) {
        assignedRoomsInDetail.forEach((rm) => {
          const roomNum = rm.roomNumber;

          // Tiền phòng cho phòng vật lý này
          if (rm.dailyRates && rm.dailyRates.length > 0) {
            rm.dailyRates.forEach((dr) => {
              const sub = Number(dr.netPrice) || 0;
              const vat = Math.round(sub * (Number(dr.vatPercent || 8) / 100));
              items.push({
                id: itemId++,
                roomNumber: roomNum,
                roomTypeName: roomTypeName,
                name: `Lưu trú: ${roomTypeName} (${dr.stayDate})`,
                quantity: 1,
                unitPrice: sub,
                vatRate: Number(dr.vatPercent || 8),
                vatAmount: vat,
                subtotal: sub,
                totalAmount: sub + vat,
              });
            });
          } else {
            const roomCount = assignedRoomsInDetail.length || 1;
            const baseSubtotal = Number(booking.subtotalAmount) || 1250000;
            const unit = Math.round(baseSubtotal / roomCount);
            const vat = Math.round(unit * 0.08);

            items.push({
              id: itemId++,
              roomNumber: roomNum,
              roomTypeName: roomTypeName,
              name: `Lưu trú: ${roomTypeName} (${detail.checkInDate} ➔ ${detail.checkOutDate})`,
              quantity: 1,
              unitPrice: unit,
              vatRate: 8,
              vatAmount: vat,
              subtotal: unit,
              totalAmount: unit + vat,
            });
          }

          // Các phụ phí / ẩm thực / minibar gán theo phòng này
          rm.charges?.forEach((ch) => {
            items.push({
              id: itemId++,
              roomNumber: roomNum,
              roomTypeName: roomTypeName,
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
      } else {
        const roomCount = detail.quantity || 1;
        const baseSubtotal = Number(booking.subtotalAmount) || 1250000;
        const unit = roomCount > 0 ? Math.round(baseSubtotal / roomCount) : baseSubtotal;
        const vatAmount = Math.round(baseSubtotal * 0.08);

        items.push({
          id: itemId++,
          name: `Lưu trú: ${roomTypeName} (${detail.checkInDate} ➔ ${detail.checkOutDate})`,
          quantity: roomCount,
          unitPrice: unit,
          vatRate: 8,
          vatAmount: vatAmount,
          subtotal: baseSubtotal,
          totalAmount: baseSubtotal + vatAmount,
        });
      }
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

  // Gom các dịch vụ theo từng phòng để hiển thị chi tiết
  const roomGroups = useMemo(() => {
    const map = new Map<string, {
      roomKey: string;
      roomNumber?: string;
      roomTypeName?: string;
      items: ModalFolioItem[];
      subtotal: number;
      totalAmount: number;
    }>();

    serviceItems.forEach((item) => {
      const rKey = item.roomNumber ? `Phòng ${item.roomNumber}` : 'Dịch vụ chung đoàn / Khác';
      if (!map.has(rKey)) {
        const matchedRoomTypeName = item.roomTypeName || (item.roomNumber
          ? booking?.bookingDetails
              ?.find((d) => d.bookingRooms?.some((r) => r.roomNumber === item.roomNumber))
              ?.roomTypeName
          : undefined);

        map.set(rKey, {
          roomKey: rKey,
          roomNumber: item.roomNumber,
          roomTypeName: matchedRoomTypeName,
          items: [],
          subtotal: 0,
          totalAmount: 0,
        });
      }
      const g = map.get(rKey)!;
      g.items.push(item);
      g.subtotal += item.subtotal;
      g.totalAmount += item.totalAmount;
    });

    return Array.from(map.values());
  }, [serviceItems, booking]);

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

  // Đồng bộ số tiền thanh toán mặc định khi mở modal hoặc khi áp dụng voucher
  useEffect(() => {
    if (!isOpen) return;
    let ignore = false;
    const syncPayAmount = async () => {
      await Promise.resolve();
      if (!ignore) {
        setCustomPayAmount(effectivePayAmount);
        setCashGiven(effectivePayAmount);
      }
    };
    syncPayAmount();
    return () => {
      ignore = true;
    };
  }, [isOpen, effectivePayAmount]);

  const activePayAmount = customPayAmount > 0 ? customPayAmount : effectivePayAmount;

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

  // ── Khởi tạo phiên VNPay thật & redirect thẳng sang Cổng Sandbox ──────────
  const initializeVnPaySession = async (autoRedirect = true) => {
    if (!booking) return;
    if (activePayAmount <= 0) {
      setVnPayError({ message: 'Số tiền thanh toán phải lớn hơn 0 đ.' });
      return;
    }
    setIsCreatingVnPay(true);
    setVnPayError(null);
    setBlockingPendingId(null);

    try {
      const res = await financeService.createVnPayPayment({
        bookingId: booking.id,
        method: 'VNPAY',
        bankCode: 'NCB',
        amount: activePayAmount,
      });
      if (res.result?.paymentUrl) {
        setVnPaySession(res.result);
        setCurrentStep(2);
        startVnPayPolling(res.result.paymentId);
        if (autoRedirect) {
          window.location.assign(res.result.paymentUrl);
        }
      }
    } catch (err: unknown) {
      const apiErr = getApiErrorMessage(err, 'Không thể tạo phiên thanh toán VNPay Sandbox');
      setVnPayError(apiErr);

      // Nếu bị chặn lock 8005 (đã có phiên PENDING trước đó) → tìm payment PENDING để hỗ trợ hủy & tạo lại
      try {
        const listRes = await financeService.getPaymentsByBookingId(booking.id);
        const pendingGateway = (listRes.result || []).find(
          (p) => p.status === 'PENDING' && (p.paymentMethod === 'VNPAY' || p.paymentMethod === 'MOMO')
        );
        if (pendingGateway) {
          setBlockingPendingId(pendingGateway.id);
        }
      } catch {
        // Bỏ qua nếu không tải được danh sách
      }
    } finally {
      setIsCreatingVnPay(false);
    }
  };

  // Hủy phiên PENDING cũ đang giữ lock Redis rồi tạo ngay phiên VNPay mới & redirect
  const handleCancelBlockingAndRetry = async () => {
    if (!blockingPendingId) return;
    setIsCancellingSession(true);
    try {
      await financeService.cancelPayment(blockingPendingId);
      setBlockingPendingId(null);
      setVnPayError(null);
      await initializeVnPaySession(true);
    } catch (err: unknown) {
      const apiErr = getApiErrorMessage(err, 'Không thể hủy phiên thanh toán cũ');
      setVnPayError(apiErr);
    } finally {
      setIsCancellingSession(false);
    }
  };

  // Hủy phiên VNPay hiện tại ở Step 2 (PENDING → CANCELLED + mở lock Redis)
  const handleCancelCurrentVnPaySession = async () => {
    if (!vnPaySession) {
      setCurrentStep(1);
      return;
    }
    setIsCancellingSession(true);
    try {
      await financeService.cancelPayment(vnPaySession.paymentId);
      if (vnpayPollRef.current) {
        clearInterval(vnpayPollRef.current);
        vnpayPollRef.current = null;
      }
      setVnPaySession(null);
      setCurrentStep(1);
    } catch (err: unknown) {
      const apiErr = getApiErrorMessage(err, 'Không thể hủy phiên VNPay');
      setVnPayError(apiErr);
    } finally {
      setIsCancellingSession(false);
    }
  };

  // Polling lắng nghe VNPay redirect về BE /vnpay/return (phòng trường hợp mở tab mới)
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
          setCompletedPayment(res.result);
          setIsSuccess(false);
          setErrorMessage(
            res.result.gatewayResponseDescription ||
              'Cổng VNPAY báo giao dịch bị hủy hoặc thanh toán không thành công.'
          );
          setCurrentStep(4);
        }
      } catch {
        // Tiếp tục poll ở lần kế tiếp
      }
    }, 2500);
  };

  // ── Chuyển bước 1 ➔ Bước tiếp theo theo từng phương thức ─────────────────
  const handleConfirmStep1 = () => {
    setVnPayError(null);
    if (selectedMethod === 'VNPAY') {
      void initializeVnPaySession(true);
      return;
    }
    setCashGiven(activePayAmount);
    setCurrentStep(2);
  };

  // ── Xử lý submit ở Bước 2 (Tiền mặt hoặc Chuyển khoản NH) ────────────────
  const handleProceedFromStep2 = () => {
    if (selectedMethod === 'CASH') {
      void executeRecordPayment(
        'CASH',
        cashReceiptCode.trim() || generatePaymentRef('PT-', 6),
        'Quầy Lễ tân (Tiền mặt)'
      );
      return;
    }

    if (selectedMethod === 'BANK_TRANSFER') {
      if (!bankRefCode.trim()) {
        setBankFormError('Vui lòng nhập mã giao dịch ngân hàng (VD: FT260930001).');
        return;
      }
      setBankFormError(null);
      void executeRecordPayment(
        'BANK_TRANSFER',
        bankRefCode.trim(),
        bankProviderName.trim() || 'Chuyển khoản Ngân hàng'
      );
    }
  };

  // ── Tương tác chuyển trạng thái trực tiếp trên thanh toán PENDING ở Step 4 ─
  const handleCompletePendingPayment = async () => {
    if (!completedPayment) return;
    setIsSubmitting(true);
    try {
      const res = await financeService.completePayment(completedPayment.id);
      if (res.result) {
        setCompletedPayment(res.result);
        setIsSuccess(true);
        onPaymentSuccess(res.result);
      }
    } catch (err: unknown) {
      const apiErr = getApiErrorMessage(err, 'Không thể xác nhận hoàn tất thanh toán');
      setIsSuccess(false);
      setErrorMessage(apiErr.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancelPendingPayment = async () => {
    if (!completedPayment) return;
    setIsSubmitting(true);
    try {
      const res = await financeService.cancelPayment(completedPayment.id);
      if (res.result) {
        setCompletedPayment(res.result);
        setIsSuccess(false);
        setErrorMessage('Giao dịch đã được hủy (PENDING → CANCELLED). Bạn có thể tạo thanh toán mới.');
        onPaymentSuccess(res.result);
      }
    } catch (err: unknown) {
      const apiErr = getApiErrorMessage(err, 'Không thể hủy giao dịch');
      setErrorMessage(apiErr.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ── Ghi nhận thanh toán vào Backend ────────────────────────────────────────
  const executeRecordPayment = async (method: PaymentMethod, refCode: string, provider: string) => {
    if (!booking) return;
    setIsSubmitting(true);
    setErrorMessage('');

    try {
      const res = await financeService.createPayment({
        bookingId: booking.id,
        totalAmount: activePayAmount,
        paymentMethod: method,
        paymentPurpose: (activePayAmount >= remainingDue ? 'FULL_PAYMENT' : 'DEPOSIT') as PaymentPurpose,
        transactionReference: refCode,
        paymentProvider: provider,
      });

      if (res.result) {
        setCompletedPayment(res.result);
        setIsSuccess(true);
        setCurrentStep(4);
        onPaymentSuccess(res.result);
      }
    } catch (err: unknown) {
      const apiErr = getApiErrorMessage(err, 'Không thể ghi nhận thanh toán vào hệ thống');
      setCompletedPayment(null);
      setIsSuccess(false);
      setErrorMessage(apiErr.code ? `[Mã lỗi ${apiErr.code}] ${apiErr.message}` : apiErr.message);
      setCurrentStep(4);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ── Quay lại từ bước Thất bại: "Làm lại từ bước xác nhận" ──────────────────
  const handleRetryFromStep1 = () => {
    setCurrentStep(1);
    setErrorMessage('');
    setVnPayError(null);
    setBlockingPendingId(null);
    setIsSuccess(true);
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
                  {currentStep === 2 &&
                    (selectedMethod === 'VNPAY'
                      ? 'Đang chuyển hướng sang Cổng VNPay Sandbox'
                      : selectedMethod === 'BANK_TRANSFER'
                        ? 'Ghi nhận Chuyển khoản Ngân hàng'
                        : 'Thu Tiền Mặt Tại Quầy Lễ Tân')}
                  {currentStep === 4 &&
                    (completedPayment?.status === 'PENDING'
                      ? 'Giao dịch Đang chờ xác nhận (PENDING)'
                      : isSuccess
                        ? 'Thanh toán Thành công'
                        : 'Thanh toán Chưa hoàn tất')}
                </h2>
                <span className="text-xs px-2.5 py-0.5 rounded-full font-bold bg-neutral-200 text-neutral-800">
                  {currentStep === 4 ? 'Kết quả' : `Bước ${currentStep}/2`}
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

                  {/* Thông tin thẻ test VNPay Sandbox khi chọn VNPAY */}
                  {selectedMethod === 'VNPAY' && (
                    <div className="p-3.5 bg-blue-50/70 border border-blue-200 rounded-2xl space-y-2 text-xs">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-blue-900 flex items-center gap-1.5">
                          <CreditCard className="w-4 h-4 text-blue-600" />
                          Thẻ ATM test trên cổng VNPay Sandbox (Ngân hàng NCB):
                        </span>
                        <button
                          type="button"
                          onClick={() => {
                            const text = `Ngân hàng: ${VNPAY_ATM_TEST_CARD.bankName}\nSố thẻ: ${VNPAY_ATM_TEST_CARD.cardNumber}\nChủ thẻ: ${VNPAY_ATM_TEST_CARD.cardHolder}\nNgày phát hành: ${VNPAY_ATM_TEST_CARD.issueDate}\nOTP: ${VNPAY_ATM_TEST_CARD.otp}`;
                            navigator.clipboard.writeText(text);
                            setVnpayCopiedField('all');
                            setTimeout(() => setVnpayCopiedField(null), 2000);
                          }}
                          className="text-[11px] text-blue-700 hover:underline font-semibold flex items-center gap-1 cursor-pointer"
                        >
                          {vnpayCopiedField === 'all' ? (
                            <span className="text-emerald-600 font-bold">✓ Đã sao chép</span>
                          ) : (
                            <>
                              <Copy className="w-3.5 h-3.5" />
                              Sao chép thẻ test
                            </>
                          )}
                        </button>
                      </div>
                      <div className="grid grid-cols-2 gap-1.5 text-[11px] text-neutral-700">
                        <div>Ngân hàng: <strong className="text-neutral-900">NCB</strong></div>
                        <div>Ngày phát hành: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.issueDate}</strong></div>
                        <div className="col-span-2">
                          Số thẻ: <strong className="font-mono text-neutral-900">{VNPAY_ATM_TEST_CARD.cardNumber}</strong>
                        </div>
                        <div className="col-span-2">
                          Chủ thẻ: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.cardHolder}</strong> • OTP: <strong className="text-emerald-700 font-bold">{VNPAY_ATM_TEST_CARD.otp}</strong>
                        </div>
                      </div>
                      <p className="text-[11px] text-blue-800/80 pt-1 border-t border-blue-200/70">
                        Khi bấm <strong>"Thanh toán qua VNPay Sandbox"</strong>, trình duyệt sẽ chuyển hướng trực tiếp sang trang <strong>sandbox.vnpayment.vn</strong> và tự động quay về hệ thống cập nhật trạng thái.
                      </p>
                    </div>
                  )}

                  {/* Hiển thị lỗi khi tạo phiên VNPay (kèm nút hủy phiên PENDING cũ nếu vướng lock 8005) */}
                  {vnPayError && (
                    <div className="p-3.5 bg-red-50 border border-red-200 rounded-2xl space-y-2 text-xs text-red-800">
                      <div className="flex items-start gap-2">
                        <AlertTriangle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                        <div>
                          <div className="font-bold">
                            Không thể tạo phiên VNPay {vnPayError.code ? `(Mã lỗi: ${vnPayError.code})` : ''}
                          </div>
                          <p className="mt-0.5">{vnPayError.message}</p>
                        </div>
                      </div>
                      {blockingPendingId && (
                        <div className="pt-1 flex justify-end">
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={handleCancelBlockingAndRetry}
                            isLoading={isCancellingSession}
                            className="bg-red-600 hover:bg-red-700 text-white text-xs"
                          >
                            Hủy phiên PENDING cũ (#{blockingPendingId}) &amp; Thanh toán lại
                          </Button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>

              {/* NỬA PHẢI (2): CHI TIẾT DỊCH VỤ SỬ DỤNG, BẢNG TIỀN & NHẬP VOUCHER */}
              <div className="bg-neutral-50/70 p-5 rounded-2xl border border-neutral-200 flex flex-col justify-between space-y-4">
                <div className="space-y-4">
                  <div className="flex justify-between items-center border-b border-neutral-200 pb-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-neutral-700 flex items-center gap-1.5">
                      <Receipt className="w-4 h-4 text-red-600" />
                      Chi tiết chi phí theo từng phòng
                    </span>
                    <span className="text-xs text-neutral-500 font-semibold">{roomGroups.length} nhóm phòng • {serviceItems.length} hạng mục</span>
                  </div>

                  {/* Bảng liệt kê dịch vụ gom theo từng phòng */}
                  <div className="overflow-y-auto max-h-52 space-y-2.5 pr-1">
                    {roomGroups.map((group) => (
                      <div key={group.roomKey} className="border border-neutral-200 rounded-xl overflow-hidden bg-white shadow-2xs">
                        <div className="bg-neutral-100/90 px-3 py-1.5 border-b border-neutral-200 flex items-center justify-between text-xs">
                          <span className="font-bold text-neutral-900 flex items-center gap-1.5 font-mono">
                            <span className="px-1.5 py-0.5 rounded bg-neutral-900 text-white text-[10px]">
                              {group.roomNumber ? `P.${group.roomNumber}` : 'Chung'}
                            </span>
                            {group.roomTypeName && (
                              <span className="font-sans font-medium text-neutral-600 text-[11px]">
                                {group.roomTypeName}
                              </span>
                            )}
                          </span>
                          <span className="font-bold text-red-600 text-xs">
                            {formatCurrency(group.totalAmount)}
                          </span>
                        </div>
                        <table className="w-full text-left text-xs">
                          <tbody className="divide-y divide-neutral-100 text-[11px]">
                            {group.items.map((item) => (
                              <tr key={item.id} className="hover:bg-neutral-50/50">
                                <td className="p-2 font-medium text-neutral-800">
                                  {item.name}
                                </td>
                                <td className="p-2 text-center text-neutral-500 w-10">x{item.quantity}</td>
                                <td className="p-2 text-right font-bold text-neutral-900 w-24">
                                  {formatCurrency(item.totalAmount)}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ))}
                  </div>

                  {/* Phân rã tính toán dòng tiền (Gộp 1 hóa đơn cho người đại diện) */}
                  <div className="bg-white p-3.5 rounded-xl border border-neutral-200 space-y-2 text-xs">
                    <div className="flex justify-between items-center pb-1 mb-1 border-b border-neutral-100 text-neutral-500 text-[11px]">
                      <span>Khách đại diện thanh toán:</span>
                      <strong className="text-neutral-900">{booking.guestName}</strong>
                    </div>
                    <div className="flex justify-between text-neutral-600">
                      <span>Tổng tiền phòng &amp; dịch vụ (Chưa thuế):</span>
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

                {/* Tổng thanh toán cuối cùng & Điều chỉnh số tiền thu */}
                <div className="pt-3 border-t border-neutral-200 space-y-3">
                  <div className="space-y-2">
                    {initialPaidAmount > 0 && (
                      <div className="flex justify-between text-xs text-neutral-600">
                        <span>Đã thanh toán trước đó:</span>
                        <span className="font-semibold text-emerald-600">{formatCurrency(initialPaidAmount)}</span>
                      </div>
                    )}

                    <div className="bg-white p-3 rounded-xl border border-neutral-200 space-y-1.5">
                      <div className="flex items-center justify-between text-xs">
                        <label htmlFor="custom-pay-amount" className="font-bold text-neutral-700">
                          Số tiền thanh toán đợt này (VNĐ)
                        </label>
                        {customPayAmount !== effectivePayAmount && effectivePayAmount > 0 && (
                          <button
                            type="button"
                            onClick={() => setCustomPayAmount(effectivePayAmount)}
                            className="text-[11px] text-red-600 hover:underline font-semibold cursor-pointer"
                          >
                            Thu đủ ({formatCurrency(effectivePayAmount)})
                          </button>
                        )}
                      </div>
                      <CurrencyInput
                        id="custom-pay-amount"
                        value={customPayAmount}
                        onChangeValue={(val) => setCustomPayAmount(val)}
                        suffix="VNĐ"
                        className="h-10 font-bold text-neutral-900"
                      />
                      {activePayAmount > effectivePayAmount && effectivePayAmount > 0 && (
                        <p className="text-[11px] text-amber-700 font-medium">
                          Lưu ý: Số tiền vượt dư nợ còn lại ({formatCurrency(effectivePayAmount)}) có thể bị backend từ chối (mã 8003).
                        </p>
                      )}
                      {selectedMethod === 'VNPAY' && activePayAmount > 0 && activePayAmount < 5000 && (
                        <p className="text-[11px] text-amber-700 font-medium">
                          VNPay Sandbox yêu cầu giao dịch tối thiểu từ 5.000 đ trở lên.
                        </p>
                      )}
                    </div>

                    <div className="flex justify-between items-baseline">
                      <span className="text-sm font-bold text-neutral-800">
                        {initialPaidAmount > 0 ? 'Thanh toán đợt này:' : 'Tổng cộng thanh toán:'}
                      </span>
                      <span className="text-2xl font-black text-red-600">
                        {formatCurrency(activePayAmount)}
                      </span>
                    </div>
                    {initialPaidAmount > 0 && (
                      <div className="flex justify-between text-xs text-neutral-500">
                        <span>Tổng phát sinh: {formatCurrency(grandTotal)}</span>
                        <span>Dư nợ còn lại sau thu: {formatCurrency(Math.max(0, remainingDue - activePayAmount))}</span>
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
                      isLoading={isCreatingVnPay}
                      disabled={isCreatingVnPay || activePayAmount <= 0}
                      rightIcon={!isCreatingVnPay ? <ArrowRight className="w-4 h-4" /> : undefined}
                      className="px-6 font-bold"
                    >
                      {selectedMethod === 'VNPAY'
                        ? isCreatingVnPay
                          ? 'Đang chuyển sang VNPay Sandbox...'
                          : 'Thanh toán qua VNPay Sandbox'
                        : 'Xác nhận & Tiếp tục'}
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ================================================================= */}
          {/* BƯỚC 2: XỬ LÝ THANH TOÁN THEO PHƯƠNG THỨC                        */}
          {/* ================================================================= */}
          {currentStep === 2 && (
            <div className="space-y-6">
              <div className="flex items-center justify-between pb-3 border-b border-neutral-100">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    if (selectedMethod === 'VNPAY' && vnPaySession) {
                      void handleCancelCurrentVnPaySession();
                    } else {
                      setCurrentStep(1);
                    }
                  }}
                  disabled={isCancellingSession}
                  leftIcon={<ArrowLeft className="w-4 h-4" />}
                >
                  {selectedMethod === 'VNPAY' && vnPaySession
                    ? 'Hủy phiên & Quay lại'
                    : 'Quay lại sửa thông tin'}
                </Button>
                <div className="text-right">
                  <span className="text-xs text-neutral-500 block">Số tiền thanh toán đợt này</span>
                  <strong className="text-lg font-black text-red-600">
                    {formatCurrency(activePayAmount)}
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
                        Nhập số tiền khách đưa để tính tiền thối lại và ghi nhận SUCCESS ngay
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
                        onClick={() => setCashGiven(activePayAmount)}
                        className="text-xs px-2.5 py-1 bg-neutral-100 hover:bg-neutral-200 rounded-lg font-medium cursor-pointer"
                      >
                        Đúng số tiền ({formatCurrency(activePayAmount)})
                      </button>
                      {[500000, 1000000, 2000000, 5000000].map((amt) => (
                        <button
                          key={amt}
                          type="button"
                          onClick={() => setCashGiven(amt)}
                          className="text-xs px-2.5 py-1 bg-neutral-100 hover:bg-neutral-200 rounded-lg font-medium cursor-pointer"
                        >
                          {formatCurrency(amt)}
                        </button>
                      ))}
                    </div>

                    <div className="flex justify-between items-center p-3 bg-emerald-50 border border-emerald-200 rounded-xl">
                      <span className="text-xs font-semibold text-emerald-800">Tiền thối lại khách:</span>
                      <strong className="text-base font-bold text-emerald-700">
                        {cashGiven >= activePayAmount
                          ? formatCurrency(cashGiven - activePayAmount)
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
                      isLoading={isSubmitting}
                      disabled={isSubmitting || cashGiven < activePayAmount || activePayAmount <= 0}
                      className="px-6 font-bold"
                    >
                      {isSubmitting ? 'Đang ghi nhận...' : 'Xác nhận Đã thu đủ tiền mặt'}
                    </Button>
                  </div>
                </div>
              )}

              {/* 🏦 PHƯƠNG THỨC 2: CHUYỂN KHOẢN NGÂN HÀNG (TẠO PENDING ĐỂ TƯƠNG TÁC) */}
              {selectedMethod === 'BANK_TRANSFER' && (
                <div className="max-w-xl mx-auto space-y-5 bg-neutral-50 p-6 rounded-3xl border border-neutral-200">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center">
                      <Building2 className="w-6 h-6" />
                    </div>
                    <div>
                      <h3 className="font-bold text-base text-neutral-900">
                        Ghi nhận Chuyển khoản Ngân hàng
                      </h3>
                      <p className="text-xs text-neutral-500">
                        Giao dịch tạo ở trạng thái PENDING để kế toán đối soát ➔ xác nhận SUCCESS hoặc CANCELLED
                      </p>
                    </div>
                  </div>

                  <div className="space-y-3 bg-white p-4 rounded-2xl border border-neutral-200">
                    <div>
                      <label className="block text-xs font-bold text-neutral-700 mb-1">
                        Ngân hàng nhận / chuyển (Tùy chọn)
                      </label>
                      <Input
                        placeholder="VD: Vietcombank, BIDV, MB Bank..."
                        value={bankProviderName}
                        onChange={(e) => setBankProviderName(e.target.value)}
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-neutral-700 mb-1">
                        Mã giao dịch ngân hàng (FT Code) <span className="text-red-500">*</span>
                      </label>
                      <Input
                        placeholder="VD: FT26111000123"
                        value={bankRefCode}
                        onChange={(e) => {
                          setBankRefCode(e.target.value);
                          setBankFormError(null);
                        }}
                      />
                      {bankFormError && (
                        <p className="text-xs text-red-600 font-medium mt-1">{bankFormError}</p>
                      )}
                    </div>

                    <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800">
                      Sau khi tạo, bạn có thể nhấn <strong>"Xác nhận đã nhận tiền (PENDING → SUCCESS)"</strong> ngay ở bước tiếp theo hoặc trong bảng lịch sử Folio.
                    </div>
                  </div>

                  <div className="flex justify-end gap-3 pt-2">
                    <Button variant="outline" onClick={() => setCurrentStep(1)}>
                      Quay lại
                    </Button>
                    <Button
                      variant="primary"
                      onClick={handleProceedFromStep2}
                      isLoading={isSubmitting}
                      disabled={isSubmitting || activePayAmount <= 0}
                      className="px-6 font-bold"
                    >
                      {isSubmitting ? 'Đang tạo giao dịch...' : 'Ghi nhận chuyển khoản (PENDING)'}
                    </Button>
                  </div>
                </div>
              )}

              {/* 🌐 PHƯƠNG THỨC 3: CỔNG VNPAY SANDBOX THẬT */}
              {selectedMethod === 'VNPAY' && (
                <div className="max-w-2xl mx-auto space-y-5 bg-neutral-50 p-6 rounded-3xl border border-neutral-200">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-2xl bg-blue-100 text-blue-700 flex items-center justify-center">
                        <CreditCard className="w-6 h-6" />
                      </div>
                      <div>
                        <h3 className="font-bold text-base text-neutral-900">
                          Cổng thanh toán VNPay Sandbox
                        </h3>
                        <p className="text-xs text-neutral-500">
                          Nhập thông tin thẻ NCB trực tiếp trên trang sandbox.vnpayment.vn
                        </p>
                      </div>
                    </div>
                    <PaymentStatusBadge status="PENDING" />
                  </div>

                  {vnPayError && (
                    <div className="p-3.5 bg-red-50 border border-red-200 text-red-800 rounded-2xl text-xs space-y-1">
                      <div className="font-bold">
                        Lỗi tạo phiên VNPay {vnPayError.code ? `(Mã ${vnPayError.code})` : ''}
                      </div>
                      <p>{vnPayError.message}</p>
                    </div>
                  )}

                  {vnPaySession ? (
                    <div className="bg-white p-5 rounded-2xl border border-blue-200 space-y-4">
                      <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-neutral-100">
                        <div>
                          <span className="text-xs text-neutral-500 block">Mã tham chiếu phiên (vnp_TxnRef)</span>
                          <strong className="font-mono text-sm text-neutral-900">{vnPaySession.txnRef}</strong>
                        </div>
                        <CountdownBadge expiresAt={vnPaySession.expiresAt} />
                      </div>

                      <div className="flex items-center gap-3 p-3 bg-blue-50/70 border border-blue-200 rounded-xl text-xs text-blue-900">
                        <Loader2 className="w-4 h-4 text-blue-600 animate-spin shrink-0" />
                        <span>
                          Đang chờ bạn hoàn tất thanh toán trên trang <strong>sandbox.vnpayment.vn</strong>. Nếu trình duyệt chưa tự chuyển trang, hãy bấm nút bên dưới.
                        </span>
                      </div>

                      <div className="p-3.5 bg-neutral-50 border border-neutral-200 rounded-xl space-y-2 text-xs">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-neutral-800">Thẻ ATM test NCB trên VNPay Sandbox:</span>
                          <button
                            type="button"
                            onClick={() => {
                              const text = `Ngân hàng: ${VNPAY_ATM_TEST_CARD.bankName}\nSố thẻ: ${VNPAY_ATM_TEST_CARD.cardNumber}\nChủ thẻ: ${VNPAY_ATM_TEST_CARD.cardHolder}\nNgày phát hành: ${VNPAY_ATM_TEST_CARD.issueDate}\nOTP: ${VNPAY_ATM_TEST_CARD.otp}`;
                              navigator.clipboard.writeText(text);
                              setVnpayCopiedField('all');
                              setTimeout(() => setVnpayCopiedField(null), 2000);
                            }}
                            className="text-[11px] text-blue-700 hover:underline font-semibold flex items-center gap-1 cursor-pointer"
                          >
                            <Copy className="w-3.5 h-3.5" />
                            {vnpayCopiedField === 'all' ? 'Đã sao chép!' : 'Sao chép thông tin thẻ'}
                          </button>
                        </div>
                        <div className="grid grid-cols-2 gap-1.5 text-[11px] text-neutral-700">
                          <div>Ngân hàng: <strong className="text-neutral-900">NCB</strong></div>
                          <div>Ngày phát hành: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.issueDate}</strong></div>
                          <div className="col-span-2">
                            Số thẻ: <strong className="font-mono text-neutral-900">{VNPAY_ATM_TEST_CARD.cardNumber}</strong>
                          </div>
                          <div className="col-span-2">
                            Tên chủ thẻ: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.cardHolder}</strong> • OTP: <strong className="text-emerald-700 font-bold">{VNPAY_ATM_TEST_CARD.otp}</strong>
                          </div>
                        </div>
                      </div>

                      <div className="flex flex-wrap gap-2.5 pt-1">
                        <Button
                          variant="primary"
                          onClick={() => window.location.assign(vnPaySession.paymentUrl)}
                          rightIcon={<ExternalLink className="w-4 h-4" />}
                          className="flex-1 font-bold"
                        >
                          Mở Cổng VNPay Sandbox ngay
                        </Button>
                        <a
                          href={vnPaySession.paymentUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 text-xs font-semibold border border-neutral-300 bg-white hover:bg-neutral-50 text-neutral-800 rounded-xl transition-colors"
                        >
                          Mở tab mới
                          <ExternalLink className="w-3.5 h-3.5" />
                        </a>
                        <Button
                          variant="outline"
                          onClick={() => void handleCancelCurrentVnPaySession()}
                          isLoading={isCancellingSession}
                          disabled={isCancellingSession}
                          className="text-red-600 border-red-200 hover:bg-red-50"
                        >
                          Hủy phiên (#{vnPaySession.paymentId})
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div className="p-4 bg-white border border-neutral-200 rounded-2xl text-xs text-neutral-600 flex items-center justify-between gap-3">
                      <span className="flex items-center gap-2">
                        <Loader2 className="w-4 h-4 animate-spin text-red-600" />
                        Chưa có phiên VNPay hoặc phiên đã hủy...
                      </span>
                      <Button
                        size="sm"
                        variant="primary"
                        onClick={() => void initializeVnPaySession(true)}
                        isLoading={isCreatingVnPay}
                        disabled={isCreatingVnPay}
                      >
                        {isCreatingVnPay ? 'Đang tạo...' : 'Tạo phiên VNPay mới'}
                      </Button>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* ================================================================= */}
          {/* BƯỚC 4: KẾT QUẢ & TƯƠNG TÁC CHUYỂN TRẠNG THÁI GIAO DỊCH           */}
          {/* ================================================================= */}
          {currentStep === 4 && (
            <div className="max-w-md mx-auto py-6 text-center space-y-6">
              {completedPayment?.status === 'PENDING' ? (
                <>
                  <div className="w-16 h-16 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center mx-auto animate-in zoom-in">
                    <Loader2 className="w-9 h-9" />
                  </div>

                  <div className="space-y-1.5">
                    <div className="flex items-center justify-center gap-2">
                      <h3 className="text-xl font-bold text-neutral-900">Giao dịch Đang chờ xác nhận</h3>
                      <PaymentStatusBadge status="PENDING" />
                    </div>
                    <p className="text-xs text-neutral-500">
                      Thanh toán #{completedPayment.id} đang ở trạng thái <strong>PENDING</strong>. Bạn có thể xác nhận hoàn tất hoặc hủy bỏ giao dịch này.
                    </p>
                  </div>

                  <div className="p-4 bg-neutral-50 border border-neutral-200 rounded-2xl text-left text-xs space-y-2">
                    <div className="flex justify-between">
                      <span className="text-neutral-500">Mã giao dịch:</span>
                      <strong className="font-mono text-neutral-900">
                        {completedPayment.transactionReference || `PAY-${completedPayment.id}`}
                      </strong>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-neutral-500">Phương thức:</span>
                      <span className="font-semibold text-neutral-800">{completedPayment.paymentMethod}</span>
                    </div>
                    {completedPayment.paymentProvider && (
                      <div className="flex justify-between">
                        <span className="text-neutral-500">Ngân hàng / Nhà CC:</span>
                        <span className="font-semibold text-neutral-800">{completedPayment.paymentProvider}</span>
                      </div>
                    )}
                    <div className="flex justify-between pt-2 border-t border-neutral-200 text-sm font-bold">
                      <span className="text-neutral-700">Số tiền:</span>
                      <span className="text-amber-700">
                        {formatCurrency(completedPayment.totalAmount)}
                      </span>
                    </div>
                  </div>

                  {errorMessage && (
                    <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-xs">
                      {errorMessage}
                    </div>
                  )}

                  <div className="space-y-2.5 pt-1">
                    <Button
                      variant="primary"
                      onClick={() => void handleCompletePendingPayment()}
                      isLoading={isSubmitting}
                      disabled={isSubmitting}
                      leftIcon={<CheckCircle2 className="w-4 h-4" />}
                      className="w-full py-3 font-bold bg-emerald-600 hover:bg-emerald-700"
                    >
                      Xác nhận đã nhận tiền (PENDING → SUCCESS)
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => void handleCancelPendingPayment()}
                      disabled={isSubmitting}
                      leftIcon={<XCircle className="w-4 h-4" />}
                      className="w-full text-red-600 border-red-200 hover:bg-red-50 font-semibold"
                    >
                      Hủy giao dịch (PENDING → CANCELLED)
                    </Button>
                    <Button variant="ghost" onClick={onClose} className="w-full text-xs">
                      Đóng (xử lý sau trong Folio)
                    </Button>
                  </div>
                </>
              ) : isSuccess && completedPayment?.status === 'SUCCESS' ? (
                <>
                  <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto animate-in zoom-in">
                    <CheckCircle2 className="w-10 h-10" />
                  </div>

                  <div className="space-y-1.5">
                    <div className="flex items-center justify-center gap-2">
                      <h3 className="text-xl font-bold text-neutral-900">Thanh toán Thành công!</h3>
                      <PaymentStatusBadge status="SUCCESS" />
                    </div>
                    <p className="text-xs text-neutral-500">
                      Giao dịch thanh toán hóa đơn đã được ghi nhận vào hệ thống tài chính
                    </p>
                  </div>

                  {/* Biên lai tóm tắt */}
                  <div className="p-4 bg-neutral-50 border border-neutral-200 rounded-2xl text-left text-xs space-y-2">
                    <div className="flex justify-between">
                      <span className="text-neutral-500">Mã giao dịch:</span>
                      <strong className="font-mono text-neutral-900">
                        {completedPayment?.transactionReference || `PAY-${completedPayment?.id || ''}`}
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

                    {/* Đối chiếu kỹ thuật nếu thanh toán VNPAY */}
                    {completedPayment?.paymentMethod === 'VNPAY' && (
                      <div className="p-2.5 bg-blue-50/80 border border-blue-200 rounded-xl space-y-1 text-[11px] text-blue-900">
                        <div className="font-bold flex items-center justify-between text-blue-950">
                          <span>Đối chiếu Cổng VNPay Sandbox:</span>
                          <span className="bg-emerald-100 text-emerald-800 font-bold px-1.5 py-0.5 rounded text-[10px]">
                            Mã {completedPayment.gatewayResponseCode || '00'} - Thành công
                          </span>
                        </div>
                        <div className="grid grid-cols-2 gap-1 text-neutral-700 pt-1">
                          <div>Ngân hàng: <strong>{completedPayment.bankCode || 'NCB'}</strong></div>
                          <div>Loại thẻ: <strong>{completedPayment.cardType || 'ATM'}</strong></div>
                          <div>vnp_TxnRef: <strong className="font-mono">{completedPayment.transactionReference || vnPaySession?.txnRef}</strong></div>
                          <div>vnp_TransactionNo: <strong className="font-mono">{completedPayment.gatewayTxnId || 'N/A'}</strong></div>
                        </div>
                      </div>
                    )}

                    <div className="flex justify-between pt-2 border-t border-neutral-200 text-sm font-bold">
                      <span className="text-neutral-700">Tổng tiền đã trả:</span>
                      <span className="text-emerald-700">
                        {formatCurrency(completedPayment?.totalAmount ?? activePayAmount)}
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

                  <div className="space-y-1.5">
                    <div className="flex items-center justify-center gap-2">
                      <h3 className="text-xl font-bold text-neutral-900">
                        {completedPayment?.status === 'CANCELLED' ? 'Giao dịch Đã hủy' : 'Thanh toán Thất bại'}
                      </h3>
                      {completedPayment?.status && <PaymentStatusBadge status={completedPayment.status} />}
                    </div>
                    <p className="text-xs text-neutral-500">
                      {errorMessage ||
                        completedPayment?.gatewayResponseDescription ||
                        'Giao dịch chưa được hoàn tất do lỗi xác thực hoặc kết nối.'}
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
          customAmountPaid={completedPayment?.totalAmount ?? activePayAmount}
        />
      )}
    </div>
  );
};

export default InvoicePaymentFlowModal;
