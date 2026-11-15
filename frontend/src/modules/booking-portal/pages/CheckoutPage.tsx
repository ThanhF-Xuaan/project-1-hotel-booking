import React, { useState, useEffect, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Clock,
  ShieldCheck,
  CreditCard,
  Building2,
  Calendar,
  CheckCircle2,
  ArrowLeft,
  Sparkles,
  Copy,
  Check,
  Ticket,
  Tag,
  X,
  ChevronDown,
  ChevronUp,
  FileText,
  AlertCircle,
  Plus,
  Minus,
  Loader2,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import { isValidVietnamesePhone } from '../../../core/utils/phone';
import { bookingService } from '../../booking/services/booking.service';
import { financeService } from '../../finance/services/finance.service';
import { voucherService } from '../../pricing/services/voucher.service';
import { bookingSessionService } from '../services/bookingSessionService';
import { publicMenuService } from '../services/publicMenuService';
import { bookingSearchService } from '../services/bookingSearchService';
import type { VoucherValidationResponse } from '../../pricing/types/voucher.types';
import { getApiErrorMessage, type ApiErrorInfo } from '../../../core/api/error';
import PaymentErrorModal from '../../finance/components/payment/PaymentErrorModal';
import SessionExpiredModal from '../components/SessionExpiredModal';
import { VNPAY_ATM_TEST_CARD } from '../../finance/utils/vnpay.utils';
import type {
  AvailableRoomOffer,
  CustomerSearchCriteria,
  SelectedRoomCartItem,
  AddonServiceItem,
  CartPriceQuoteResponse,
} from '../types/portal.types';

const HONORIFIC_OPTIONS = [
  { value: 'Ông', label: 'Ông' },
  { value: 'Bà', label: 'Bà' },
  { value: 'Anh', label: 'Anh' },
  { value: 'Chị', label: 'Chị' },
];

const COUNTRY_OPTIONS = [
  { value: 'Việt Nam', label: 'Việt Nam' },
  { value: 'United States', label: 'United States' },
  { value: 'Korea', label: 'Korea' },
  { value: 'Japan', label: 'Japan' },
  { value: 'China', label: 'China' },
  { value: 'Khác', label: 'Quốc gia khác' },
];

const DEFAULT_FALLBACK_CHECK_IN = new Date().toISOString().split('T')[0];
const DEFAULT_FALLBACK_CHECK_OUT = new Date(Date.now() + 86400000 * 2).toISOString().split('T')[0];

export const CheckoutPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const state = location.state as {
    sessionId?: string;
    remainingSeconds?: number;
    hotelName?: string;
    hotelId?: number;
    offer?: AvailableRoomOffer;
    criteria?: CustomerSearchCriteria;
    selectedCartItems?: SelectedRoomCartItem[];
    quote?: CartPriceQuoteResponse;
  } | undefined;

  const sessionId = state?.sessionId;
  const initialSeconds = state?.remainingSeconds ?? 900;
  const hotelName = state?.hotelName || 'Khách sạn Viettel Boutique Sapa';
  const offer = state?.offer;
  const criteria = state?.criteria;
  const selectedCartItems = useMemo(() => state?.selectedCartItems || [], [state?.selectedCartItems]);

  // 15-Minute Countdown Timer (đồng bộ từ Backend remainingSeconds hoặc fallback 900s)
  const [secondsRemaining, setSecondsRemaining] = useState<number>(initialSeconds);

  // Form Thông tin người đặt phòng (Chuẩn Mường Thanh)
  const [honorific, setHonorific] = useState<string>('Ông');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [confirmEmail, setConfirmEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [country, setCountry] = useState<string>('Việt Nam');
  const [identityNumber, setIdentityNumber] = useState('');
  const [specialRequest, setSpecialRequest] = useState('');

  // UI state
  const [isRoomsCollapsed, setIsRoomsCollapsed] = useState(false);
  const [isCancellingSession, setIsCancellingSession] = useState(false);
  const [isExpiredModalOpen, setIsExpiredModalOpen] = useState(false);

  // Payment method (cố định cổng thanh toán trực tuyến VNPAY)
  const paymentMethod = 'VNPAY';
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [bookingConfirmed, setBookingConfirmed] = useState<{
    id: number;
    bookingNumber: string;
    totalAmount: number;
  } | null>(null);

  // Tính số đêm & ngày
  const checkInDate = criteria?.checkInDate || DEFAULT_FALLBACK_CHECK_IN;
  const checkOutDate = criteria?.checkOutDate || DEFAULT_FALLBACK_CHECK_OUT;

  const nightsCount = Math.max(
    1,
    Math.round((new Date(checkOutDate).getTime() - new Date(checkInDate).getTime()) / 86400000)
  );
  const daysCount = nightsCount + 1;

  // Danh sách dịch vụ gia tăng / Add-on services & Báo giá giỏ hàng
  const [availableServices, setAvailableServices] = useState<AddonServiceItem[]>([]);
  const [isLoadingServices, setIsLoadingServices] = useState<boolean>(true);
  const [selectedServices, setSelectedServices] = useState<Array<{ menuId: number; quantity: number }>>([]);
  const [cartQuote, setCartQuote] = useState<CartPriceQuoteResponse | null>(state?.quote || null);
  const [isRecalculatingPrice, setIsRecalculatingPrice] = useState(false);

  // Tổng tiền phòng gốc thuần (Net) trước thuế phí và voucher
  const basePriceTotal = useMemo(() => {
    if (cartQuote?.totalBasePrice != null && cartQuote.totalBasePrice > 0) {
      return cartQuote.totalBasePrice;
    }
    if (selectedCartItems.length > 0) {
      return selectedCartItems.reduce((sum, item) => {
        const itemBase =
          item.basePriceTotal ??
          item.quote?.priceBreakdown?.totalBasePrice ??
          Math.round(item.estimatedTotal / 1.134);
        return sum + itemBase;
      }, 0);
    }
    const estimated = offer?.estimatedTotal ?? (offer?.basePrice ?? 600000) * nightsCount;
    return Math.round(estimated / 1.134);
  }, [cartQuote?.totalBasePrice, selectedCartItems, nightsCount, offer?.estimatedTotal, offer?.basePrice]);

  // Voucher state
  const [voucherCodeInput, setVoucherCodeInput] = useState(criteria?.voucherCode || '');
  const [appliedVoucher, setAppliedVoucher] = useState<VoucherValidationResponse | null>(null);
  const [voucherError, setVoucherError] = useState<string | null>(null);
  const [isValidatingVoucher, setIsValidatingVoucher] = useState(false);

  const effectiveHotelId = criteria?.hotelId || state?.hotelId || 1;

  // Helper chuyển đổi đơn vị tính chuẩn hóa sang tiếng Việt
  const formatServiceUnit = (unit?: string, pricingType?: string) => {
    if (pricingType === 'PER_STAY') return 'kỳ nghỉ';
    if (pricingType === 'PER_NIGHT') return 'đêm';
    if (pricingType === 'PER_PERSON') return 'người';
    if (pricingType === 'PER_PERSON_PER_NIGHT') return 'người/đêm';
    if (pricingType === 'PER_USE') return 'lượt';
    switch (unit?.toUpperCase()) {
      case 'CHUYEN':
        return 'chuyến';
      case 'KG':
        return 'kg';
      case 'NGAY':
        return 'ngày';
      case 'GOI':
        return 'gói';
      case 'PHAN':
        return 'suất';
      case 'LUOT':
        return 'lượt';
      default:
        return unit?.toLowerCase() || 'lượt';
    }
  };

  // Tính toán lại báo giá giỏ hàng đa hạng phòng + dịch vụ thời gian thực
  const recalculateCartPricing = async (
    services: Array<{ menuId: number; quantity: number }>,
    vCode?: string
  ) => {
    setIsRecalculatingPrice(true);
    try {
      const itemsPayload =
        selectedCartItems.length > 0
          ? selectedCartItems.map((cartItem) => {
              const firstRoom = cartItem.rooms[0];
              return {
                hotelRoomTypeId: cartItem.hotelRoomTypeId,
                quantity: cartItem.quantity as number,
                adults: firstRoom?.adults ?? 2,
                children: firstRoom?.children ?? 0,
                infants: firstRoom?.infants ?? 0,
                extraBeds: 0,
              };
            })
          : [
              {
                hotelRoomTypeId: offer?.hotelRoomTypeId || 1,
                quantity: 1,
                adults: criteria?.adultCount || 2,
                children: criteria?.childCount || 0,
                infants: 0,
                extraBeds: 0,
              },
            ];

      const res = await bookingSearchService.calculateCartQuote({
        hotelId: effectiveHotelId,
        checkInDate,
        checkOutDate,
        voucherCode: vCode || undefined,
        items: itemsPayload,
        selectedServices: services.length > 0 ? services : undefined,
      });

      if (res && res.result) {
        setCartQuote(res.result);
      }
    } catch (err) {
      console.warn('Lỗi tính toán lại báo giá giỏ hàng:', err);
    } finally {
      setIsRecalculatingPrice(false);
    }
  };

  // Tự động tải báo giá ban đầu từ backend nếu chưa có từ router state (hoặc khi F5 tải lại trang)
  useEffect(() => {
    if (state?.quote || !effectiveHotelId) return;

    let isSubscribed = true;
    const itemsPayload =
      selectedCartItems.length > 0
        ? selectedCartItems.map((cartItem) => {
            const firstRoom = cartItem.rooms[0];
            return {
              hotelRoomTypeId: cartItem.hotelRoomTypeId,
              quantity: cartItem.quantity as number,
              adults: firstRoom?.adults ?? 2,
              children: firstRoom?.children ?? 0,
              infants: firstRoom?.infants ?? 0,
              extraBeds: 0,
            };
          })
        : [
            {
              hotelRoomTypeId: offer?.hotelRoomTypeId || 1,
              quantity: 1,
              adults: criteria?.adultCount || 2,
              children: criteria?.childCount || 0,
              infants: 0,
              extraBeds: 0,
            },
          ];

    bookingSearchService
      .calculateCartQuote({
        hotelId: effectiveHotelId,
        checkInDate,
        checkOutDate,
        voucherCode: criteria?.voucherCode || undefined,
        items: itemsPayload,
        selectedServices: undefined,
      })
      .then((res) => {
        if (!isSubscribed) return;
        if (res && res.result) {
          setCartQuote(res.result);
        }
      })
      .catch((err) => {
        if (!isSubscribed) return;
        console.warn('Lỗi tính toán lại báo giá ban đầu:', err);
      });

    return () => {
      isSubscribed = false;
    };
  }, [
    state?.quote,
    effectiveHotelId,
    checkInDate,
    checkOutDate,
    criteria?.voucherCode,
    criteria?.adultCount,
    criteria?.childCount,
    offer?.hotelRoomTypeId,
    selectedCartItems,
  ]);

  // Tải danh mục dịch vụ bổ sung chuẩn chuỗi & cơ sở override từ Backend
  useEffect(() => {
    let isSubscribed = true;
    publicMenuService
      .getAddonServicesByHotel(effectiveHotelId)
      .then((res) => {
        if (!isSubscribed) return;
        if (res && res.result && Array.isArray(res.result.content)) {
          setAvailableServices(res.result.content);
        }
      })
      .catch((err) => {
        if (!isSubscribed) return;
        console.warn('Không thể tải danh sách dịch vụ bổ sung:', err);
      })
      .finally(() => {
        if (isSubscribed) {
          setIsLoadingServices(false);
        }
      });

    return () => {
      isSubscribed = false;
    };
  }, [effectiveHotelId]);

  // Tự động kiểm tra và áp dụng voucher nếu được truyền từ trang tìm kiếm
  useEffect(() => {
    if (criteria?.voucherCode && basePriceTotal > 0) {
      const code = criteria.voucherCode.trim().toUpperCase();
      voucherService
        .validateVoucher({
          voucherCode: code,
          hotelId: effectiveHotelId,
          orderAmount: basePriceTotal,
        })
        .then((res) => {
          if (res?.result?.valid) {
            setAppliedVoucher(res.result);
            setVoucherError(null);
          }
        })
        .catch((err) => {
          console.warn('Lỗi tự động kích hoạt voucher từ trang tìm kiếm:', err);
        });
    }
  }, [criteria?.voucherCode, effectiveHotelId, basePriceTotal]);

  // Bật/tắt dịch vụ
  const handleToggleService = (menuId: number) => {
    const existing = selectedServices.find((s) => s.menuId === menuId);
    let next: Array<{ menuId: number; quantity: number }>;
    if (existing) {
      next = selectedServices.filter((s) => s.menuId !== menuId);
    } else {
      next = [...selectedServices, { menuId, quantity: 1 }];
    }
    setSelectedServices(next);
    recalculateCartPricing(next, appliedVoucher?.voucherCode);
  };

  // Tăng/giảm số lượng dịch vụ [ - 1 + ]
  const handleUpdateServiceQuantity = (menuId: number, delta: number) => {
    const next = selectedServices
      .map((s) => {
        if (s.menuId === menuId) {
          const newQty = s.quantity + delta;
          return newQty > 0 ? { ...s, quantity: newQty } : null;
        }
        return s;
      })
      .filter((s): s is { menuId: number; quantity: number } => s !== null);

    setSelectedServices(next);
    recalculateCartPricing(next, appliedVoucher?.voucherCode);
  };

  // VNPay test account copy state
  const [copiedVnpayCard, setCopiedVnpayCard] = useState(false);

  // Giai đoạn D — gọi API VNPay thật + popup lỗi §6
  const [gatewayError, setGatewayError] = useState<ApiErrorInfo | null>(null);
  const [pendingGatewayBookingId, setPendingGatewayBookingId] = useState<number | null>(null);
  const [isRetryingGateway, setIsRetryingGateway] = useState(false);

  // Tạo phiên thanh toán VNPay cho booking vừa tạo → redirect ra cổng (tree T06: gọi API thật + redirect)
  const handlePayWithGateway = async (bookingId: number): Promise<boolean> => {
    try {
      const res = await financeService.createPublicVnPayPayment({ bookingId, method: 'VNPAY', bankCode: 'NCB' });
      if (res.result) {
        setPendingGatewayBookingId(null);
        setGatewayError(null);
        window.location.href = res.result.paymentUrl;
        return true;
      }
      return false;
    } catch (err) {
      console.error('Lỗi tạo phiên VNPay:', err);
      setGatewayError(getApiErrorMessage(err, 'Không thể tạo phiên thanh toán VNPay'));
      setPendingGatewayBookingId(bookingId);
      return false;
    }
  };

  // §6 bước ②③④ — Thử lại = tạo lại phiên cho booking đã có
  const handleRetryGateway = async () => {
    if (!pendingGatewayBookingId) {
      setGatewayError(null);
      return;
    }
    setIsRetryingGateway(true);
    try {
      await handlePayWithGateway(pendingGatewayBookingId);
    } finally {
      setIsRetryingGateway(false);
    }
  };

  // Đếm ngược 15 phút (giữ phòng thật)
  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          setIsExpiredModalOpen(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const handleRedirectToSearch = () => {
    setIsExpiredModalOpen(false);
    navigate('/portal/search', {
      state: {
        criteria,
      },
    });
  };

  const formatTimer = (totalSecs: number) => {
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const formatVND = (amount: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  const formatDayOfWeekAndDate = (dateStr: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const days = ['Chủ Nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
    const dayName = days[date.getDay()];
    const dd = String(date.getDate()).padStart(2, '0');
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const yyyy = date.getFullYear();
    return `${dayName}, ${dd}/${mm}/${yyyy}`;
  };

  const voucherDiscount = cartQuote?.totalDiscountAmount ?? appliedVoucher?.discountAmount ?? 0;
  const fallbackNetRoomPrice = Math.max(0, basePriceTotal - voucherDiscount);
  const fallbackRoomServiceFee = Math.round(fallbackNetRoomPrice * 0.05);
  const fallbackRoomVat = Math.round((fallbackNetRoomPrice + fallbackRoomServiceFee) * 0.08);
  const fallbackRoomGross = fallbackNetRoomPrice + fallbackRoomServiceFee + fallbackRoomVat;

  const fallbackServiceSubtotal = selectedServices.reduce((sum, s) => {
    const item = availableServices.find((a) => a.id === s.menuId);
    return sum + (item ? item.basePrice * s.quantity : 0);
  }, 0);
  const fallbackServiceFee = Math.round(fallbackServiceSubtotal * 0.05);
  const fallbackServiceVat = Math.round((fallbackServiceSubtotal + fallbackServiceFee) * 0.10);
  const fallbackServiceGross = fallbackServiceSubtotal + fallbackServiceFee + fallbackServiceVat;

  const displayBasePriceTotal = cartQuote?.totalBasePrice ?? basePriceTotal;
  const displayServiceSubtotal = cartQuote?.totalServiceSubtotal ?? fallbackServiceSubtotal;
  const displayHotelServiceFee = (cartQuote?.totalRoomServiceFee ?? fallbackRoomServiceFee) + (cartQuote?.totalServiceFee ?? fallbackServiceFee);
  const displayTotalVat = (cartQuote?.totalRoomVat ?? fallbackRoomVat) + (cartQuote?.totalServiceVat ?? fallbackServiceVat);
  const displayGrandTotal = cartQuote?.grandTotal ?? (fallbackRoomGross + fallbackServiceGross);

  // Xử lý khi khách bấm quay lại chọn phòng -> Giải phóng hold phòng lập tức
  const handleBackToSearch = async () => {
    if (sessionId && !isCancellingSession) {
      setIsCancellingSession(true);
      try {
        await bookingSessionService.cancelSession(sessionId, 'USER_CANCELLED');
      } catch (err) {
        console.warn('Không thể gửi yêu cầu hủy session:', err);
      } finally {
        setIsCancellingSession(false);
      }
    }
    navigate(-1);
  };

  const handleApplyVoucher = async () => {
    const code = voucherCodeInput.trim().toUpperCase();
    if (!code) return;
    setVoucherError(null);
    setIsValidatingVoucher(true);
    try {
      const res = await voucherService.validateVoucher({
        voucherCode: code,
        hotelId: effectiveHotelId,
        orderAmount: basePriceTotal,
      });

      if (res && res.result) {
        if (res.result.valid) {
          setAppliedVoucher(res.result);
          setVoucherError(null);
          recalculateCartPricing(selectedServices, code);
        } else {
          setAppliedVoucher(null);
          setVoucherError(res.result.message || 'Mã giảm giá không hợp lệ');
          recalculateCartPricing(selectedServices, undefined);
        }
      } else {
        setAppliedVoucher(null);
        setVoucherError('Không thể kiểm tra mã giảm giá');
        recalculateCartPricing(selectedServices, undefined);
      }
    } catch (err: unknown) {
      setAppliedVoucher(null);
      const axiosErr = err as { response?: { data?: { message?: string } }; message?: string };
      setVoucherError(axiosErr.response?.data?.message || axiosErr.message || 'Lỗi khi kiểm tra mã voucher');
      recalculateCartPricing(selectedServices, undefined);
    } finally {
      setIsValidatingVoucher(false);
    }
  };

  const handleRemoveVoucher = () => {
    setAppliedVoucher(null);
    setVoucherCodeInput('');
    setVoucherError(null);
    recalculateCartPricing(selectedServices, undefined);
  };

  const handleConfirmBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!phone || !firstName || !lastName || !email) return;

    if (!isValidVietnamesePhone(phone)) {
      alert('Số điện thoại không đúng định dạng (Ví dụ: 0912345678 hoặc 02466668888)');
      return;
    }

    if (confirmEmail && confirmEmail.trim().toLowerCase() !== email.trim().toLowerCase()) {
      alert('Email và Nhập lại email không khớp nhau. Vui lòng kiểm tra lại!');
      return;
    }

    // Xây dựng danh sách phòng đặt: hỗ trợ cả giỏ hàng đa phòng
    const roomsPayload =
      selectedCartItems.length > 0
        ? selectedCartItems.map((cartItem) => {
            const firstRoom = cartItem.rooms[0];
            return {
              hotelRoomTypeId: cartItem.hotelRoomTypeId,
              checkInDate,
              checkOutDate,
              quantity: cartItem.quantity,
              adultCount: firstRoom?.adults ?? 2,
              childCount: firstRoom?.children ?? 0,
              infantCount: firstRoom?.infants ?? 0,
              extraBeds: 0,
              guests: [
                {
                  firstName: firstName.trim(),
                  lastName: lastName.trim(),
                  fullName: `${lastName.trim()} ${firstName.trim()}`,
                  identityNumber: identityNumber.trim(),
                },
              ],
            };
          })
        : [
            {
              hotelRoomTypeId: offer?.hotelRoomTypeId || 1,
              checkInDate,
              checkOutDate,
              quantity: 1,
              adultCount: criteria?.adultCount || 2,
              childCount: criteria?.childCount || 0,
              infantCount: 0,
              extraBeds: 0,
              guests: [
                {
                  firstName: firstName.trim(),
                  lastName: lastName.trim(),
                  fullName: `${lastName.trim()} ${firstName.trim()}`,
                  identityNumber: identityNumber.trim(),
                },
              ],
            },
          ];

    setIsSubmitting(true);
    try {
      const res = await bookingService.createPublicBooking({
        hotelId: effectiveHotelId,
        customerName: `${lastName.trim()} ${firstName.trim()}`,
        customerPhone: phone.trim(),
        customerEmail: email.trim(),
        customerNationality: country,
        customerIdentityNumber: identityNumber.trim() || undefined,
        sessionId: sessionId || undefined,
        bookingType: 'FIT',
        serviceFeeRate: 5,
        voucherCode: appliedVoucher?.voucherCode || undefined,
        rooms: roomsPayload,
        selectedServices: selectedServices.length > 0 ? selectedServices : undefined,
      });

      if (res && res.result) {
        setBookingConfirmed({
          id: res.result.id,
          bookingNumber: res.result.bookingNumber,
          totalAmount: displayGrandTotal,
        });

        // VNPAY: gọi API tạo phiên thanh toán thật + redirect sang ngân hàng
        if (paymentMethod === 'VNPAY') {
          await handlePayWithGateway(res.result.id);
        }
      }
    } catch (err: unknown) {
      console.error('Booking submission failed:', err);
      const axiosErr = err as { response?: { data?: { message?: string } }; message?: string };
      alert(axiosErr.response?.data?.message || axiosErr.message || 'Không thể tạo đơn đặt phòng. Vui lòng thử lại!');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (bookingConfirmed) {
    return (
      <div className="min-h-screen bg-neutral-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-3xl border border-neutral-200 p-8 max-w-lg w-full text-center space-y-6 shadow-xl">
          <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
            <CheckCircle2 className="w-10 h-10" />
          </div>

          <div className="space-y-2">
            <h1 className="text-2xl font-bold text-neutral-900">Đặt Phòng Thành Công!</h1>
            <p className="text-sm text-neutral-500">
              Cảm ơn quý khách. Chúng tôi đã nhận được đơn đặt phòng và gửi email xác nhận.
            </p>
          </div>

          <div className="bg-neutral-50 p-4 rounded-2xl border border-neutral-200 space-y-2 text-left text-sm">
            <div className="flex justify-between">
              <span className="text-neutral-500">Mã đơn đặt phòng:</span>
              <strong className="text-red-600 font-mono text-base">{bookingConfirmed.bookingNumber}</strong>
            </div>
            <div className="flex justify-between">
              <span className="text-neutral-500">Người đặt:</span>
              <span className="font-semibold text-neutral-900">
                {honorific} {lastName} {firstName}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-neutral-500">Số điện thoại:</span>
              <span className="font-semibold text-neutral-900">{phone}</span>
            </div>
            <div className="flex justify-between border-t border-neutral-200 pt-2">
              <span className="text-neutral-500">Tổng thanh toán:</span>
              <strong className="text-neutral-900 font-bold">{formatVND(bookingConfirmed.totalAmount)}</strong>
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <Button onClick={() => navigate('/portal/my-bookings')}>Tra cứu đơn đặt phòng của tôi</Button>
            <button
              onClick={() => navigate('/portal/search')}
              className="text-xs text-neutral-500 hover:text-neutral-800 py-2 cursor-pointer"
            >
              Về trang chủ tìm kiếm
            </button>
          </div>
        </div>

        {/* Popup lỗi tạo phiên VNPay */}
        <PaymentErrorModal
          isOpen={gatewayError !== null}
          code={gatewayError?.code}
          message={gatewayError?.message ?? ''}
          onRetry={handleRetryGateway}
          onClose={() => setGatewayError(null)}
          isRetrying={isRetryingGateway}
        />
      </div>
    );
  }

  // Danh sách phòng tóm tắt hiển thị
  const displayRooms =
    selectedCartItems.length > 0
      ? selectedCartItems
      : [
          {
            hotelRoomTypeId: offer?.hotelRoomTypeId || 1,
            roomTypeName: offer?.roomTypeName || 'Phòng Deluxe Hướng Phố',
            roomTypeCode: offer?.roomTypeCode || 'DLX',
            quantity: 1,
            rooms: [
              {
                roomIndex: 1,
                adults: criteria?.adultCount || 2,
                children: criteria?.childCount || 0,
                infants: 0,
              },
            ],
            estimatedTotal: basePriceTotal,
            perNightPrice: offer?.basePrice || 1250000,
          },
        ];

  return (
    <div className="min-h-screen bg-neutral-50 pb-16">
      {/* Top Header — Chuẩn Mường Thanh (Không có logo SSL) */}
      <header className="bg-white border-b border-neutral-200 sticky top-0 z-30 shadow-2xs">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <button
            type="button"
            onClick={handleBackToSearch}
            className="flex items-center gap-1.5 text-xs font-semibold text-neutral-700 hover:text-red-600 cursor-pointer transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Quay lại chọn phòng
          </button>
          <div className="text-xs font-bold text-neutral-800 tracking-wider uppercase">
            THÔNG TIN ĐẶT PHÒNG
          </div>
        </div>
      </header>

      {/* 15-Minute Countdown Sticky Banner */}
      <div
        className={`${
          secondsRemaining > 0 ? 'bg-amber-500 text-white' : 'bg-red-600 text-white'
        } py-3 px-4 shadow-sm sticky top-16 z-20`}
      >
        <div className="max-w-7xl mx-auto flex items-center justify-between text-xs sm:text-sm font-semibold">
          <div className="flex items-center gap-2">
            {secondsRemaining > 0 ? (
              <>
                <Clock className="w-5 h-5 animate-pulse shrink-0" />
                <span>Phòng đang được khóa giữ chỗ độc quyền cho bạn trong:</span>
              </>
            ) : (
              <>
                <AlertCircle className="w-5 h-5 shrink-0" />
                <span>Phiên giữ chỗ của bạn đã hết hạn. Vui lòng quay lại để cập nhật giá và tồn kho!</span>
              </>
            )}
          </div>
          <div className="text-lg font-mono font-extrabold bg-black/20 px-3 py-0.5 rounded-lg shrink-0">
            {formatTimer(secondsRemaining)}
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 mt-6">
        <form onSubmit={handleConfirmBooking} className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* CỘT TRÁI (7 Cột): Thông tin người đặt phòng & Phương thức thanh toán & Chính sách */}
          <div className="lg:col-span-7 space-y-6">
            {/* Khối Thông tin người đặt phòng */}
            <div className="bg-white p-6 rounded-3xl border border-neutral-200 shadow-xs space-y-4">
              <h2 className="text-lg font-bold text-neutral-900 flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-red-600" />
                Thông tin người đặt phòng
              </h2>

              {/* Hàng 1: Tiêu đề (Danh xưng) - Tên - Họ */}
              <div className="grid grid-cols-1 sm:grid-cols-5 gap-4">
                <div className="sm:col-span-1">
                  <label className="block text-xs font-medium text-neutral-700 mb-1">Tiêu đề</label>
                  <Select
                    options={HONORIFIC_OPTIONS}
                    value={honorific}
                    onChange={(val) => setHonorific(val as string)}
                    className="w-full h-11"
                  />
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-xs font-medium text-neutral-700 mb-1">
                    Tên <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    placeholder="VD: An"
                    required
                    className="h-11"
                  />
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-xs font-medium text-neutral-700 mb-1">
                    Họ <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    placeholder="VD: Nguyễn Văn"
                    required
                    className="h-11"
                  />
                </div>
              </div>

              {/* Hàng 2: Email & Nhập lại email */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-neutral-700 mb-1">
                    Email <span className="text-red-500">*</span>
                  </label>
                  <Input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="VD: guest@example.com"
                    required
                    className="h-11"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-neutral-700 mb-1">
                    Nhập lại email <span className="text-red-500">*</span>
                  </label>
                  <Input
                    type="email"
                    value={confirmEmail}
                    onChange={(e) => setConfirmEmail(e.target.value)}
                    placeholder="VD: guest@example.com"
                    required
                    className="h-11"
                    error={
                      confirmEmail && confirmEmail.trim().toLowerCase() !== email.trim().toLowerCase()
                        ? 'Email xác nhận không khớp'
                        : undefined
                    }
                  />
                </div>
              </div>

              {/* Hàng 3: Số điện thoại & Quốc gia */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-neutral-700 mb-1">
                    Số điện thoại <span className="text-red-500">*</span>
                  </label>
                  <Input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="VD: 0987654321"
                    error={
                      phone && !isValidVietnamesePhone(phone)
                        ? 'Số điện thoại không đúng định dạng (VD: 0987654321)'
                        : undefined
                    }
                    required
                    className="h-11"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-neutral-700 mb-1">Quốc gia</label>
                  <Select
                    options={COUNTRY_OPTIONS}
                    value={country}
                    onChange={(val) => setCountry(val as string)}
                    className="w-full h-11"
                  />
                </div>
              </div>

              {/* Hàng 4: Số CCCD / Hộ chiếu */}
              <div>
                <label className="block text-xs font-medium text-neutral-700 mb-1">
                  Số CCCD / Hộ chiếu (Passport)
                </label>
                <Input
                  value={identityNumber}
                  onChange={(e) => setIdentityNumber(e.target.value)}
                  placeholder="VD: 001201000123"
                  className="h-11"
                />
              </div>

              {/* Hàng 5: Yêu cầu thêm */}
              <div>
                <label className="block text-xs font-medium text-neutral-700 mb-1">
                  Yêu cầu thêm
                </label>
                <textarea
                  value={specialRequest}
                  onChange={(e) => setSpecialRequest(e.target.value)}
                  rows={3}
                  className="w-full p-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 transition-all"
                  placeholder="Ví dụ: Sở thích về Giường, địa điểm đón hoặc trả khách, nhận phòng muộn..."
                />
              </div>
            </div>

            {/* Khối Phương thức thanh toán */}
            <div className="bg-white p-6 rounded-3xl border border-neutral-200 shadow-xs space-y-4">
              <h2 className="text-lg font-bold text-neutral-900 flex items-center gap-2">
                <CreditCard className="w-5 h-5 text-red-600" />
                Phương thức thanh toán
              </h2>

              <div className="space-y-3">
                <div className="flex items-center justify-between p-4 rounded-2xl border border-red-600 bg-red-50/20 shadow-xs">
                  <div className="flex items-center gap-3">
                    <input
                      type="radio"
                      name="paymentMethod"
                      checked={true}
                      readOnly
                      className="text-red-600 focus:ring-red-600 cursor-default"
                    />
                    <div>
                      <div className="font-bold text-neutral-900 text-sm">Thẻ ATM nội địa (Cổng VNPAY Sandbox)</div>
                      <div className="text-xs text-neutral-500">
                        Thanh toán bằng thẻ ATM NCB (Napas) trực tuyến qua VNPAY
                      </div>
                    </div>
                  </div>
                  <CreditCard className="w-6 h-6 text-red-600 shrink-0" />
                </div>

                {/* Hộp gợi ý tài khoản VNPAY Sandbox */}
                <div className="p-4 bg-blue-50/70 border border-blue-200 rounded-2xl space-y-2.5 text-xs animate-in fade-in">
                  <div className="flex items-center justify-between text-blue-900 font-bold">
                    <span className="flex items-center gap-1.5">
                      <Sparkles className="w-4 h-4 text-blue-600" />
                      Gợi ý tài khoản kiểm thử Cổng VNPAY Sandbox:
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        const text = `Ngân hàng: ${VNPAY_ATM_TEST_CARD.bankName}\nSố thẻ: ${VNPAY_ATM_TEST_CARD.cardNumber}\nTên chủ thẻ: ${VNPAY_ATM_TEST_CARD.cardHolder}\nNgày phát hành: ${VNPAY_ATM_TEST_CARD.issueDate}\nOTP: ${VNPAY_ATM_TEST_CARD.otp}`;
                        navigator.clipboard.writeText(text);
                        setCopiedVnpayCard(true);
                        setTimeout(() => setCopiedVnpayCard(false), 2000);
                      }}
                      className="text-[11px] text-blue-700 hover:underline font-semibold flex items-center gap-1 cursor-pointer"
                    >
                      {copiedVnpayCard ? (
                        <span className="text-emerald-600 font-bold flex items-center gap-1">
                          <Check className="w-3.5 h-3.5" />
                          Đã sao chép thông tin!
                        </span>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5" />
                          Sao chép thông tin thẻ test
                        </>
                      )}
                    </button>
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-neutral-700 font-medium">
                    <div>
                      Ngân hàng: <strong className="text-neutral-900 font-bold">NCB</strong>
                    </div>
                    <div>
                      Số thẻ test:{' '}
                      <strong className="font-mono text-neutral-900 font-bold">
                        {VNPAY_ATM_TEST_CARD.cardNumber}
                      </strong>
                    </div>
                    <div>
                      Chủ thẻ: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.cardHolder}</strong>
                    </div>
                    <div>
                      Hạn: <strong className="text-neutral-900">{VNPAY_ATM_TEST_CARD.issueDate}</strong> • OTP:{' '}
                      <strong className="text-emerald-700 font-bold">{VNPAY_ATM_TEST_CARD.otp}</strong>
                    </div>
                  </div>
                  <p className="text-[11px] text-neutral-500 pt-1 border-t border-blue-100">
                    * Sau khi bấm <strong>"Xác Nhận Đặt Phòng"</strong>, bạn sẽ được chuyển hướng sang Cổng VNPAY
                    Sandbox để nhập các thông tin thẻ ATM NCB này.
                  </p>
                </div>
              </div>
            </div>

            {/* Khối Chính sách đặt phòng (Theo mẫu Mường Thanh) */}
            <div className="bg-white p-6 rounded-3xl border border-neutral-200 shadow-xs space-y-4">
              <h2 className="text-base font-bold text-neutral-900 flex items-center gap-2">
                <FileText className="w-4 h-4 text-red-600" />
                Chính sách đặt phòng
              </h2>

              <div className="p-4 bg-neutral-50 rounded-2xl border border-neutral-200 space-y-2 text-xs text-neutral-600 leading-relaxed">
                <div className="font-bold text-neutral-900 text-sm">
                  {displayRooms[0]?.roomTypeName}
                </div>
                <div>
                  <strong className="text-neutral-800">Hủy: </strong>
                  Hủy miễn phí trước 48 giờ so với ngày nhận phòng. Sau thời gian này hoặc không đến (No-show), quý
                  khách sẽ chịu phí hủy theo quy định của khách sạn.
                </div>
                <div>
                  <strong className="text-neutral-800">Thanh toán: </strong>
                  Thanh toán trực tuyến 100% tổng giá trị đơn đặt phòng qua Cổng VNPAY an toàn.
                </div>
                <div>
                  <strong className="text-neutral-800">Bữa ăn: </strong>
                  {offer?.freeBreakfast ? 'Đã bao gồm ăn sáng buffet hàng ngày' : 'Chưa bao gồm ăn sáng'}
                </div>
              </div>
            </div>
          </div>

          {/* CỘT PHẢI (5 Cột): Dịch vụ gia tăng & Yêu cầu đặt phòng (Mở rộng ~510px) */}
          <div className="lg:col-span-5 space-y-6">
            {/* Card 1: Khối Dịch vụ bổ sung / Dịch vụ gia tăng (Add-on Services) */}
            <div className="bg-white p-6 rounded-3xl border border-neutral-200 shadow-xs space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-neutral-200">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-xl bg-red-50 text-red-600 flex items-center justify-center shrink-0">
                    <Sparkles className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-bold text-neutral-900 text-sm">Dịch vụ bổ sung (Add-on Services)</h3>
                    <p className="text-[11px] text-neutral-500">Tùy chọn nâng tầm trải nghiệm lưu trú</p>
                  </div>
                </div>
                {selectedServices.length > 0 && (
                  <span className="text-[11px] bg-red-100 text-red-700 font-bold px-2.5 py-0.5 rounded-full shrink-0">
                    Đã chọn {selectedServices.length}
                  </span>
                )}
              </div>

              {isLoadingServices ? (
                <div className="py-6 flex items-center justify-center text-neutral-400 gap-2 text-xs">
                  <Loader2 className="w-4 h-4 animate-spin text-red-500" />
                  Đang tải danh mục dịch vụ...
                </div>
              ) : availableServices.length === 0 ? (
                <p className="text-xs text-neutral-500 italic py-2">Khách sạn chưa có dịch vụ bổ sung khả dụng.</p>
              ) : (
                <div className="space-y-3 max-h-[380px] overflow-y-auto pr-1">
                  {availableServices.map((service) => {
                    const selectedItem = selectedServices.find((s) => s.menuId === service.id);
                    const isSelected = !!selectedItem;
                    const unitStr = formatServiceUnit(service.unit, service.pricingType);

                    return (
                      <div
                        key={service.id}
                        className={`p-3.5 rounded-2xl border transition-all ${
                          isSelected
                            ? 'border-red-500 bg-red-50/20 shadow-xs'
                            : 'border-neutral-200/80 bg-neutral-50/40 hover:border-neutral-300'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <label className="flex items-start gap-3 cursor-pointer flex-1 select-none">
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={() => handleToggleService(service.id)}
                              className="mt-1 h-4 w-4 rounded text-red-600 focus:ring-red-500 border-neutral-300 cursor-pointer"
                            />
                            <div className="space-y-1">
                              <div className="font-bold text-xs text-neutral-900 leading-snug">
                                {service.name}
                              </div>
                              {service.description && (
                                <div className="text-[11px] text-neutral-500 line-clamp-2 leading-relaxed">
                                  {service.description}
                                </div>
                              )}
                              <div className="text-xs font-bold text-red-600 pt-0.5">
                                {formatVND(service.basePrice)}{' '}
                                <span className="text-[11px] text-neutral-500 font-normal">
                                  /{unitStr}
                                </span>
                              </div>
                            </div>
                          </label>

                          {/* Bộ tăng/giảm số lượng [ - 1 + ] khi được chọn */}
                          {isSelected && (
                            <div className="flex items-center gap-1.5 bg-white border border-neutral-200 rounded-xl p-1 shrink-0 self-center shadow-2xs">
                              <button
                                type="button"
                                onClick={() => handleUpdateServiceQuantity(service.id, -1)}
                                className="w-6 h-6 flex items-center justify-center rounded-lg hover:bg-neutral-100 text-neutral-700 cursor-pointer transition-colors"
                                title="Giảm số lượng"
                              >
                                <Minus className="w-3 h-3" />
                              </button>
                              <span className="w-6 text-center text-xs font-bold text-neutral-900 font-mono">
                                {selectedItem.quantity}
                              </span>
                              <button
                                type="button"
                                onClick={() => handleUpdateServiceQuantity(service.id, 1)}
                                className="w-6 h-6 flex items-center justify-center rounded-lg hover:bg-neutral-100 text-neutral-700 cursor-pointer transition-colors"
                                title="Tăng số lượng"
                              >
                                <Plus className="w-3 h-3" />
                              </button>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Card 2: Khối "Yêu cầu đặt phòng của bạn" & Bảng tổng kết chi phí (Chuẩn Mường Thanh) */}
            <div className="bg-white p-6 rounded-3xl border border-neutral-200 shadow-xs space-y-5">
              <h3 className="font-bold text-neutral-900 text-lg flex items-center gap-2">
                <Building2 className="w-5 h-5 text-red-600" />
                Yêu cầu đặt phòng của bạn
              </h3>

              {/* Thông tin khách sạn & Ngày giờ nhận/trả */}
              <div className="space-y-2.5 text-xs text-neutral-700 pb-4 border-b border-neutral-200">
                <div className="font-extrabold text-neutral-900 text-sm">
                  {hotelName}
                </div>
                <div className="flex items-start gap-2 text-neutral-600">
                  <Calendar className="w-4 h-4 text-red-500 mt-0.5 shrink-0" />
                  <div className="space-y-1">
                    <div>
                      <strong className="text-neutral-900">Nhận phòng:</strong>{' '}
                      {formatDayOfWeekAndDate(checkInDate)} từ 14:00
                    </div>
                    <div>
                      <strong className="text-neutral-900">Trả phòng:</strong>{' '}
                      {formatDayOfWeekAndDate(checkOutDate)} cho đến 12:00
                    </div>
                    <div className="text-neutral-500 font-medium">
                      ({daysCount} ngày {nightsCount} đêm)
                    </div>
                  </div>
                </div>
              </div>

              {/* Khối Thông tin phòng chi tiết (hỗ trợ thu gọn/mở rộng như Mường Thanh) */}
              <div className="space-y-3 pb-4 border-b border-neutral-200">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-neutral-800">Thông tin phòng</span>
                  <button
                    type="button"
                    onClick={() => setIsRoomsCollapsed(!isRoomsCollapsed)}
                    className="text-neutral-500 hover:text-neutral-800 flex items-center gap-1 font-medium cursor-pointer"
                  >
                    <span>{isRoomsCollapsed ? 'Mở rộng' : 'Thu gọn'}</span>
                    {isRoomsCollapsed ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronUp className="w-3.5 h-3.5" />}
                  </button>
                </div>

                {!isRoomsCollapsed ? (
                  <div className="space-y-3 animate-in fade-in">
                    {displayRooms.map((room, idx) => {
                      const firstRoomConfig = room.rooms[0];
                      const adults = firstRoomConfig?.adults ?? 2;
                      const children = firstRoomConfig?.children ?? 0;
                      const infants = firstRoomConfig?.infants ?? 0;

                      return (
                        <div
                          key={`${room.hotelRoomTypeId}-${idx}`}
                          className="bg-neutral-50/70 p-3 rounded-2xl border border-neutral-200/80 space-y-1 text-xs"
                        >
                          <div className="font-bold text-neutral-900">
                            Phòng {idx + 1}: {room.roomTypeName}
                          </div>
                          <div className="text-[11px] text-neutral-500 font-medium">
                            Gói: Giá tiêu chuẩn công khai
                          </div>
                          <div className="text-[11px] text-neutral-600">
                            Dành cho: {adults} Người lớn - {children} Trẻ em - {infants} Em bé
                          </div>
                          <div className="flex justify-between items-start pt-1.5 border-t border-neutral-200/60 text-xs">
                            <div>
                              <span className="font-semibold text-neutral-800">Tiền phòng ({nightsCount} đêm):</span>
                              <div className="text-[10px] text-neutral-500 font-normal">
                                (Trọn gói gồm thuế & phí: {formatVND(room.estimatedTotal || Math.round((room.basePriceTotal ?? 1200000) * 1.134))})
                              </div>
                            </div>
                            <span className="text-neutral-900 font-bold">
                              {formatVND(room.basePriceTotal ?? Math.round((room.estimatedTotal ?? 0) / 1.134))}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="text-xs text-neutral-500 italic bg-neutral-50 p-2.5 rounded-xl border border-neutral-200">
                    {displayRooms.length} phòng ({displayRooms.reduce((sum, r) => sum + r.quantity, 0)} hạng phòng đã chọn)
                  </div>
                )}
              </div>

              {/* Mã ưu đãi / Voucher */}
              <div className="space-y-2 pb-4 border-b border-neutral-200">
                <label className="text-xs font-bold text-neutral-800 uppercase tracking-wide flex items-center gap-1.5">
                  <Ticket className="w-4 h-4 text-red-600" />
                  Mã Giảm Giá / Voucher
                </label>

                {appliedVoucher ? (
                  <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-between text-xs animate-in fade-in">
                    <div className="flex items-center gap-2">
                      <Tag className="w-4 h-4 text-emerald-600 shrink-0" />
                      <div>
                        <div className="font-bold text-emerald-900 font-mono tracking-wider">
                          {appliedVoucher.voucherCode}
                        </div>
                        <div className="text-emerald-700 text-[11px] font-medium">
                          {appliedVoucher.title} (-{formatVND(voucherDiscount)})
                        </div>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={handleRemoveVoucher}
                      className="p-1 text-emerald-700 hover:text-red-600 hover:bg-emerald-100 rounded-lg transition-colors cursor-pointer"
                      title="Hủy áp dụng mã"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <div className="space-y-1.5">
                    <div className="flex gap-2">
                      <Input
                        placeholder="Nhập mã ưu đãi..."
                        value={voucherCodeInput}
                        onChange={(e) => {
                          setVoucherCodeInput(e.target.value.toUpperCase().replace(/\s+/g, ''));
                          setVoucherError(null);
                        }}
                        className="h-10 text-xs font-mono uppercase font-semibold"
                      />
                      <Button
                        type="button"
                        variant="outline"
                        onClick={handleApplyVoucher}
                        disabled={!voucherCodeInput.trim() || isValidatingVoucher}
                        className="h-10 px-3 text-xs shrink-0 border-neutral-300 hover:border-red-500 hover:text-red-600 cursor-pointer"
                      >
                        {isValidatingVoucher ? 'Đang kiểm tra...' : 'Áp dụng'}
                      </Button>
                    </div>
                    {voucherError && <p className="text-[11px] text-red-600 font-medium">{voucherError}</p>}
                  </div>
                )}
              </div>

              {/* Bảng tổng kết chi phí & Lưu ý chuẩn Mường Thanh */}
              <div className="space-y-2 text-xs text-neutral-600">
                <div className="flex justify-between">
                  <span>Tiền phòng ({nightsCount} đêm):</span>
                  <span className="font-medium text-neutral-800">{formatVND(displayBasePriceTotal)}</span>
                </div>
                {voucherDiscount > 0 && (
                  <div className="flex justify-between text-emerald-700 font-medium">
                    <span>Ưu đãi voucher:</span>
                    <span>-{formatVND(voucherDiscount)}</span>
                  </div>
                )}
                {displayServiceSubtotal > 0 && (
                  <div className="flex justify-between text-neutral-800">
                    <span>Dịch vụ bổ sung ({selectedServices.length} món):</span>
                    <span className="font-medium text-neutral-900">{formatVND(displayServiceSubtotal)}</span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span>Phí dịch vụ khách sạn (5%):</span>
                  <span>{formatVND(displayHotelServiceFee)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Thuế GTGT (VAT):</span>
                  <span>{formatVND(displayTotalVat)}</span>
                </div>
                <div className="flex justify-between items-center text-base font-extrabold text-neutral-900 pt-3 border-t border-neutral-200">
                  <span className="flex items-center gap-1.5">
                    Tổng giá:
                    {isRecalculatingPrice && (
                      <Loader2 className="w-3.5 h-3.5 animate-spin text-red-600" />
                    )}
                  </span>
                  <span className={`text-red-600 ${isRecalculatingPrice ? 'opacity-60' : ''}`}>
                    {formatVND(displayGrandTotal)}
                  </span>
                </div>

                {/* Dòng lưu ý pháp lý theo đúng quy định Mường Thanh */}
                <div className="text-[11px] text-neutral-500 pt-2 leading-relaxed">
                  <p>Bao gồm tất cả các loại thuế và phí dịch vụ</p>
                  <p className="text-red-600 font-medium">
                    (Theo quy định của Ngân hàng Nhà nước Việt Nam, Quý khách vui lòng thanh toán bằng VNĐ)
                  </p>
                </div>
              </div>

              <Button
                type="submit"
                disabled={isSubmitting || secondsRemaining <= 0}
                className="w-full py-3.5 font-bold text-base mt-4 cursor-pointer"
              >
                {isSubmitting ? 'Đang xác nhận...' : 'Xác Nhận Đặt Phòng'}
              </Button>
            </div>
          </div>
        </form>
      </div>

      {/* Popup lỗi tạo phiên VNPay */}
      <PaymentErrorModal
        isOpen={gatewayError !== null}
        code={gatewayError?.code}
        message={gatewayError?.message ?? ''}
        onRetry={handleRetryGateway}
        onClose={() => setGatewayError(null)}
        isRetrying={isRetryingGateway}
      />

      {/* Modal cảnh báo hết hạn giữ phòng 15 phút & tự động redirect */}
      <SessionExpiredModal
        isOpen={isExpiredModalOpen}
        onRedirect={handleRedirectToSearch}
        autoRedirectSeconds={5}
      />
    </div>
  );
};

export default CheckoutPage;
