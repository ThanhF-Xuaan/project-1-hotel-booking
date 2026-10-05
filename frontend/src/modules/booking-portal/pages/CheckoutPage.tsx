import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Clock,
  ShieldCheck,
  CreditCard,
  Building2,
  Calendar,
  CheckCircle2,
  Lock,
  ArrowLeft,
  QrCode,
  Banknote,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { bookingService } from '../../booking/services/booking.service';
import { financeService } from '../../finance/services/finance.service';
import { getApiErrorMessage, type ApiErrorInfo } from '../../../core/api/error';
import PaymentErrorModal from '../../finance/components/payment/PaymentErrorModal';
import type { AvailableRoomOffer, CustomerSearchCriteria } from '../types/portal.types';

export const CheckoutPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const state = location.state as {
    offer?: AvailableRoomOffer;
    criteria?: CustomerSearchCriteria;
  } | undefined;

  const offer = state?.offer;
  const criteria = state?.criteria;

  // 15-Minute Countdown Timer (900 seconds)
  const [secondsRemaining, setSecondsRemaining] = useState<number>(900);

  // Guest details form
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [identityNumber, setIdentityNumber] = useState('');
  const [specialRequest, setSpecialRequest] = useState('');

  // Payment method
  const [paymentMethod, setPaymentMethod] = useState<'VNPAY' | 'CREDIT_CARD' | 'PAY_AT_HOTEL'>('VNPAY');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [bookingConfirmed, setBookingConfirmed] = useState<{
    id: number;
    bookingNumber: string;
    totalAmount: number;
  } | null>(null);

  // Giai đoạn D — gọi API VNPay thật + popup lỗi §6
  const [gatewayError, setGatewayError] = useState<ApiErrorInfo | null>(null);
  const [pendingGatewayBookingId, setPendingGatewayBookingId] = useState<number | null>(null);
  const [isRetryingGateway, setIsRetryingGateway] = useState(false);

  // Tạo phiên thanh toán VNPay cho booking vừa tạo → redirect ra cổng (tree T06: gọi API thật + redirect)
  const handlePayWithGateway = async (bookingId: number): Promise<boolean> => {
    try {
      const res = await financeService.createVnPayPayment({ bookingId, method: 'VNPAY' });
      if (res.result) {
        setPendingGatewayBookingId(null);
        setGatewayError(null);
        window.location.href = res.result.paymentUrl;
        return true;
      }
      return false;
    } catch (err) {
      // §6 bước ① — popup (8008 GATEWAY_ERROR khi sandbox keys trống / gateway sập)
      console.error('Lỗi tạo phiên VNPay:', err);
      setGatewayError(getApiErrorMessage(err, 'Không thể tạo phiên thanh toán VNPay'));
      setPendingGatewayBookingId(bookingId);
      return false;
    }
  };

  // §6 bước ②③④ — Thử lại = tạo lại phiên cho booking đã có (booking đã tạo, không tạo lại)
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

  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const formatTimer = (totalSecs: number) => {
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const formatVND = (amount: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  const nightsCount = criteria
    ? Math.max(1, Math.round((new Date(criteria.checkOutDate).getTime() - new Date(criteria.checkInDate).getTime()) / 86400000))
    : 2;

  const basePriceTotal = (offer?.basePrice ?? 1250000) * nightsCount;
  const serviceFee = Math.round(basePriceTotal * 0.05);
  const vat = Math.round((basePriceTotal + serviceFee) * 0.08);
  const grandTotal = basePriceTotal + serviceFee + vat;

  const handleConfirmBooking = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!phone || !firstName || !lastName) return;

    setIsSubmitting(true);
    try {
      const res = await bookingService.create({
        hotelId: criteria?.hotelId || 1,
        guestId: 1, // Default or resolved guest ID
        bookingType: 'FIT',
        serviceFeeRate: 5,
        rooms: [
          {
            hotelRoomTypeId: offer?.hotelRoomTypeId || 1,
            checkInDate: criteria?.checkInDate || new Date().toISOString().split('T')[0],
            checkOutDate: criteria?.checkOutDate || new Date(Date.now() + 86400000).toISOString().split('T')[0],
            quantity: 1,
            adultCount: criteria?.adultCount || 2,
            childCount: criteria?.childCount || 0,
            guests: [
              {
                firstName: firstName.trim(),
                lastName: lastName.trim(),
                fullName: `${lastName.trim()} ${firstName.trim()}`,
                identityNumber: identityNumber.trim(),
              },
            ],
          },
        ],
      });

      if (res && res.result) {
        setBookingConfirmed({
          id: res.result.id,
          bookingNumber: res.result.bookingNumber,
          totalAmount: grandTotal,
        });
        // VNPAY — thay option giả thành gọi API thật + redirect (tree T06 D3)
        if (paymentMethod === 'VNPAY') {
          await handlePayWithGateway(res.result.id);
        }
      }
    } catch (err) {
      console.error('Booking submission failed:', err);
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
              <span className="font-semibold text-neutral-900">{lastName} {firstName}</span>
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
            <Button onClick={() => navigate('/portal/my-bookings')}>
              Tra cứu đơn đặt phòng của tôi
            </Button>
            <button
              onClick={() => navigate('/portal/search')}
              className="text-xs text-neutral-500 hover:text-neutral-800 py-2"
            >
              Về trang chủ tìm kiếm
            </button>
          </div>
        </div>

        {/* Popup lỗi tạo phiên VNPay — booking đã tạo nên "Thử lại" = tạo lại phiên cho booking này */}
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

  return (
    <div className="min-h-screen bg-neutral-50 pb-16">
      {/* Top Header */}
      <header className="bg-white border-b border-neutral-200 sticky top-0 z-30 shadow-2xs">
        <div className="max-w-5xl mx-auto px-4 h-16 flex items-center justify-between">
          <button
            onClick={() => navigate(-1)}
            className="flex items-center gap-1.5 text-xs font-semibold text-neutral-600 hover:text-neutral-900"
          >
            <ArrowLeft className="w-4 h-4" />
            Quay lại chọn phòng
          </button>
          <div className="flex items-center gap-2">
            <Lock className="w-4 h-4 text-emerald-600" />
            <span className="text-xs font-semibold text-neutral-700">Thanh toán an toàn 256-bit SSL</span>
          </div>
        </div>
      </header>

      {/* 15-Minute Countdown Sticky Banner */}
      <div className="bg-amber-500 text-white py-3 px-4 shadow-sm sticky top-16 z-20">
        <div className="max-w-5xl mx-auto flex items-center justify-between text-xs sm:text-sm font-semibold">
          <div className="flex items-center gap-2">
            <Clock className="w-5 h-5 animate-pulse" />
            <span>Phòng đang được khóa giữ chỗ độc quyền cho bạn trong:</span>
          </div>
          <div className="text-lg font-mono font-extrabold bg-black/20 px-3 py-0.5 rounded-lg">
            {formatTimer(secondsRemaining)}
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-5xl mx-auto px-4 mt-6">
        <form onSubmit={handleConfirmBooking} className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left 2 Cols: Guest Form & Payment Selector */}
          <div className="lg:col-span-2 space-y-6">
            {/* Guest Form Card */}
            <div className="bg-white p-6 rounded-3xl border border-neutral-200 shadow-xs space-y-4">
              <h2 className="text-lg font-bold text-neutral-900 flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-red-600" />
                Thông tin người nhận phòng
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-neutral-700 mb-1">
                    Họ đệm <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    placeholder="VD: Nguyễn Văn"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-neutral-700 mb-1">
                    Tên <span className="text-red-500">*</span>
                  </label>
                  <Input
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    placeholder="VD: An"
                    required
                  />
                </div>
              </div>

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
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-neutral-700 mb-1">Email nhận xác nhận</label>
                  <Input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="VD: guest@example.com"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-neutral-700 mb-1">Số CCCD / Hộ chiếu (Passport)</label>
                <Input
                  value={identityNumber}
                  onChange={(e) => setIdentityNumber(e.target.value)}
                  placeholder="VD: 001201000123"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-neutral-700 mb-1">Yêu cầu đặc biệt (Không bắt buộc)</label>
                <textarea
                  value={specialRequest}
                  onChange={(e) => setSpecialRequest(e.target.value)}
                  rows={2}
                  className="w-full p-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600"
                  placeholder="Tầng cao, phòng không hút thuốc, nhận phòng muộn..."
                />
              </div>
            </div>

            {/* Payment Method Card */}
            <div className="bg-white p-6 rounded-3xl border border-neutral-200 shadow-xs space-y-4">
              <h2 className="text-lg font-bold text-neutral-900 flex items-center gap-2">
                <CreditCard className="w-5 h-5 text-red-600" />
                Phương thức thanh toán
              </h2>

              <div className="space-y-3">
                <label
                  onClick={() => setPaymentMethod('VNPAY')}
                  className={`flex items-center justify-between p-4 rounded-2xl border cursor-pointer transition-all ${
                    paymentMethod === 'VNPAY'
                      ? 'border-red-600 bg-red-50/20 shadow-xs'
                      : 'border-neutral-200 hover:border-neutral-300'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="radio"
                      name="paymentMethod"
                      checked={paymentMethod === 'VNPAY'}
                      onChange={() => setPaymentMethod('VNPAY')}
                      className="text-red-600 focus:ring-red-600"
                    />
                    <div>
                      <div className="font-bold text-neutral-900 text-sm">Cổng VNPay QR / Thẻ nội địa</div>
                      <div className="text-xs text-neutral-500">Quét mã QR qua tất cả ứng dụng ngân hàng & ví điện tử</div>
                    </div>
                  </div>
                  <QrCode className="w-6 h-6 text-red-600" />
                </label>

                <label
                  onClick={() => setPaymentMethod('CREDIT_CARD')}
                  className={`flex items-center justify-between p-4 rounded-2xl border cursor-pointer transition-all ${
                    paymentMethod === 'CREDIT_CARD'
                      ? 'border-red-600 bg-red-50/20 shadow-xs'
                      : 'border-neutral-200 hover:border-neutral-300'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="radio"
                      name="paymentMethod"
                      checked={paymentMethod === 'CREDIT_CARD'}
                      onChange={() => setPaymentMethod('CREDIT_CARD')}
                      className="text-red-600 focus:ring-red-600"
                    />
                    <div>
                      <div className="font-bold text-neutral-900 text-sm">Thẻ Quốc Tế (Visa / Mastercard / JCB)</div>
                      <div className="text-xs text-neutral-500">Thanh toán bảo mật tức thì</div>
                    </div>
                  </div>
                  <CreditCard className="w-6 h-6 text-blue-600" />
                </label>

                <label
                  onClick={() => setPaymentMethod('PAY_AT_HOTEL')}
                  className={`flex items-center justify-between p-4 rounded-2xl border cursor-pointer transition-all ${
                    paymentMethod === 'PAY_AT_HOTEL'
                      ? 'border-red-600 bg-red-50/20 shadow-xs'
                      : 'border-neutral-200 hover:border-neutral-300'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="radio"
                      name="paymentMethod"
                      checked={paymentMethod === 'PAY_AT_HOTEL'}
                      onChange={() => setPaymentMethod('PAY_AT_HOTEL')}
                      className="text-red-600 focus:ring-red-600"
                    />
                    <div>
                      <div className="font-bold text-neutral-900 text-sm">Thanh toán khi nhận phòng (Pay at Hotel)</div>
                      <div className="text-xs text-neutral-500">Thanh toán trực tiếp bằng tiền mặt hoặc thẻ tại quầy lễ tân</div>
                    </div>
                  </div>
                  <Banknote className="w-6 h-6 text-emerald-600" />
                </label>
              </div>
            </div>
          </div>

          {/* Right Col: Booking Summary & Final Button */}
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-3xl border border-neutral-200 shadow-xs space-y-4">
              <h3 className="font-bold text-neutral-900 text-base flex items-center gap-2">
                <Building2 className="w-4 h-4 text-red-600" />
                Tóm tắt kỳ nghỉ
              </h3>

              <div className="space-y-3 text-xs text-neutral-600 pb-4 border-b border-neutral-200">
                <div className="font-bold text-neutral-900 text-sm">
                  {offer?.roomTypeName || 'Phòng Deluxe Hướng Phố'}
                </div>
                <div className="flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-neutral-400" />
                  <span>
                    {criteria?.checkInDate || '2026-09-28'} ➔ {criteria?.checkOutDate || '2026-09-30'} ({nightsCount} đêm)
                  </span>
                </div>
                <div>{offer?.bedDescription || '1 Giường đôi King Size'}</div>
              </div>

              {/* Breakdown */}
              <div className="space-y-2 text-xs text-neutral-600">
                <div className="flex justify-between">
                  <span>Tiền phòng ({nightsCount} đêm):</span>
                  <span>{formatVND(basePriceTotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Phí dịch vụ khách sạn (5%):</span>
                  <span>{formatVND(serviceFee)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Thuế GTGT (VAT 8%):</span>
                  <span>{formatVND(vat)}</span>
                </div>
                <div className="flex justify-between text-base font-extrabold text-neutral-900 pt-3 border-t border-neutral-200">
                  <span>Tổng tiền thanh toán:</span>
                  <span className="text-red-600">{formatVND(grandTotal)}</span>
                </div>
              </div>

              <Button
                type="submit"
                disabled={isSubmitting || secondsRemaining <= 0}
                className="w-full py-3.5 font-bold text-base mt-4"
              >
                {isSubmitting ? 'Đang xác nhận...' : 'Xác Nhận Đặt Phòng'}
              </Button>
            </div>
          </div>
        </form>
      </div>

      {/* Popup lỗi tạo phiên VNPay (§6) — nút "Đóng" để khách xem lại xác nhận đơn, "Thử lại" tạo lại phiên */}
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
};

export default CheckoutPage;
