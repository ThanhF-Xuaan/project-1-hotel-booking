import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  ArrowLeft,
  CheckCircle2,
  Loader2,
  XCircle,
  ShieldCheck,
  CreditCard,
  Building2,
  Calendar,
  Hash,
  FileCode,
  Copy,
  Check,
  ExternalLink,
  Search,
  FileText,
  Clock,
  RotateCcw,
  List,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import { formatCurrency } from '../../../core/utils/currency';
import { getErrorStatus } from '../../../core/api/client';
import financeService from '../services/finance.service';
import PaymentStatusBadge from '../components/payment/PaymentStatusBadge';
import type { PaymentResponse, PaymentStatus } from '../types/finance.types';
import {
  getVnPayResponseInfo,
  formatVnPayDate,
  parseVnPayRawPayload,
  VNPAY_ATM_TEST_CARD,
} from '../utils/vnpay.utils';

/**
 * Trang Kết quả & Đối chiếu Dữ liệu Giao dịch Cổng VNPAY (Thẻ ATM nội địa):
 * - Giao diện thân thiện cho khách đặt phòng online (Portal UX)
 * - Tự động đếm ngược 10 giây chuyển hướng về trang chủ tìm kiếm
 * - Nút tiếp tục đặt phòng và tra cứu đơn đặt phòng
 * - Phần đối chiếu kỹ thuật VNPAY được ẩn mặc định (có thể mở rộng khi cần kiểm tra đối soát)
 */
export const PaymentResultPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // Tham số URL từ BE redirect hoặc VNPay return
  const statusParam = searchParams.get('status'); // PAID | FAILED
  const vnpResponseCode = searchParams.get('vnp_ResponseCode');
  const momoResultCode = searchParams.get('resultCode');
  const txnRef =
    searchParams.get('txnRef') || searchParams.get('vnp_TxnRef') || searchParams.get('orderId');
  const transactionNo = searchParams.get('vnp_TransactionNo');
  const bankCode = searchParams.get('vnp_BankCode');
  const cardType = searchParams.get('vnp_CardType');
  const payDate = searchParams.get('vnp_PayDate');
  const bankTranNo = searchParams.get('vnp_BankTranNo');
  const orderInfo = searchParams.get('vnp_OrderInfo');
  const transactionStatus = searchParams.get('vnp_TransactionStatus');
  const amountParam = searchParams.get('vnp_Amount');

  const paymentIdParam = searchParams.get('paymentId');
  const paymentId = paymentIdParam ? Number(paymentIdParam) : undefined;
  const bookingIdParam = searchParams.get('bookingId');

  const isPaidByParams =
    statusParam === 'PAID' ||
    (!statusParam && (vnpResponseCode === '00' || momoResultCode === '0'));

  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [pollFailed, setPollFailed] = useState(false);
  const [authPollStopped, setAuthPollStopped] = useState(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [showTechnicalDetails, setShowTechnicalDetails] = useState(false);
  const [showRawPayload, setShowRawPayload] = useState(false);

  // Đếm ngược tự động chuyển hướng về trang chủ tìm kiếm (10 giây)
  const [countdown, setCountdown] = useState<number>(10);
  const [isCountdownPaused, setIsCountdownPaused] = useState<boolean>(false);

  // Phát hiện xem trang có được mở trong tab/cửa sổ con từ giao diện quản trị hay không
  const [isAdminPopup] = useState<boolean>(() => {
    try {
      const activeSession = localStorage.getItem('vnpay_active_session');
      return Boolean(window.opener || activeSession);
    } catch {
      return Boolean(window.opener);
    }
  });
  const [closeCountdown, setCloseCountdown] = useState<number>(2);

  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Poll trạng thái thật từ BE theo paymentId (Rules 03: cleanup khi unmount)
  useEffect(() => {
    if (!paymentId) return;
    let active = true;

    const check = async () => {
      try {
        let res;
        try {
          res = await financeService.getPaymentById(paymentId);
        } catch (innerErr) {
          const errStatus = getErrorStatus(innerErr);
          if (errStatus === 401 || errStatus === 403) {
            res = await financeService.getPublicPaymentStatus(paymentId);
          } else {
            throw innerErr;
          }
        }
        if (!active || !res.result) return;
        setPollFailed(false);
        setAuthPollStopped(false);
        setPayment(res.result);
        if (res.result.status !== 'PENDING' && intervalRef.current) {
          clearInterval(intervalRef.current);
          intervalRef.current = null;
        }
      } catch (err: unknown) {
        if (!active) return;
        const status = getErrorStatus(err);
        if (status === 401 || status === 403) {
          // Người dùng không có token ở origin hiện tại và public status cũng dừng
          setAuthPollStopped(true);
          if (intervalRef.current) {
            clearInterval(intervalRef.current);
            intervalRef.current = null;
          }
          return;
        }
        setPollFailed(true);
      }
    };

    void check();
    intervalRef.current = setInterval(() => {
      void check();
    }, 3000);

    return () => {
      active = false;
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [paymentId]);

  // Trạng thái hiển thị: ưu tiên trạng thái DB nếu poll được; fallback sang URL params từ BE redirect nếu 401/403 hoặc thiếu paymentId
  const displayStatus: PaymentStatus = payment
    ? payment.status
    : !paymentId || authPollStopped
    ? isPaidByParams
      ? 'SUCCESS'
      : 'FAILED'
    : 'PENDING';

  const isSignatureInvalid =
    statusParam === 'INVALID_SIGNATURE' ||
    vnpResponseCode === '97' ||
    (statusParam === 'FAILED' && vnpResponseCode === '00');

  const isPaid = displayStatus === 'SUCCESS';
  const isConfirming = displayStatus === 'PENDING';
  const isFailed = displayStatus === 'FAILED' || displayStatus === 'CANCELLED';

  // Countdown timer tự động redirect về trang tìm kiếm portal
  useEffect(() => {
    if (isAdminPopup || !isPaid || isCountdownPaused) return;

    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          navigate('/portal/search');
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isAdminPopup, isPaid, isCountdownPaused, navigate]);

  // Đồng bộ kết quả thanh toán sang tab quản trị viên (qua BroadcastChannel, postMessage, localStorage)
  useEffect(() => {
    const isSuccess = isPaidByParams || displayStatus === 'SUCCESS';
    const statusStr = isSuccess ? 'PAID' : displayStatus === 'PENDING' ? 'PENDING' : 'FAILED';

    // 1. Gửi qua postMessage nếu có window.opener
    try {
      if (window.opener && !window.opener.closed) {
        window.opener.postMessage(
          {
            type: 'VNPAY_PAYMENT_COMPLETED',
            status: statusStr,
            paymentId,
            bookingId: bookingIdParam,
            txnRef,
          },
          window.location.origin
        );
      }
    } catch (e) {
      console.warn('postMessage to opener failed:', e);
    }

    // 2. Gửi qua BroadcastChannel đồng bộ liên tab
    try {
      const bc = new BroadcastChannel('vnpay_payment_sync');
      bc.postMessage({
        type: 'VNPAY_PAYMENT_COMPLETED',
        status: statusStr,
        paymentId,
        bookingId: bookingIdParam,
        txnRef,
      });
      bc.close();
    } catch (e) {
      console.warn('BroadcastChannel error:', e);
    }

    // 3. Fallback qua localStorage
    try {
      localStorage.setItem(
        'vnpay_payment_sync_event',
        JSON.stringify({
          type: 'VNPAY_PAYMENT_COMPLETED',
          status: statusStr,
          paymentId,
          time: Date.now(),
        })
      );
    } catch {
      void 0;
    }

    // 4. Nếu là popup mở từ Admin modal: tự động đếm ngược và đóng tab sau 2s
    if (isAdminPopup && (isPaid || isFailed)) {
      const timer = setInterval(() => {
        setCloseCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            try {
              localStorage.removeItem('vnpay_active_session');
              window.close();
            } catch {
              void 0;
            }
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(timer);
    }
  }, [isAdminPopup, isPaid, isFailed, isPaidByParams, displayStatus, paymentId, bookingIdParam, txnRef]);

  // Dữ liệu đối chiếu từ backend hoặc params
  const rawPayloadMap = payment?.rawCallbackPayload
    ? parseVnPayRawPayload(payment.rawCallbackPayload)
    : null;

  const resolvedTxnRef =
    payment?.gatewayTxnId || txnRef || rawPayloadMap?.vnp_TxnRef || '—';
  const resolvedTransactionNo =
    payment?.transactionReference ||
    transactionNo ||
    rawPayloadMap?.vnp_TransactionNo ||
    '—';
  const resolvedBankCode =
    payment?.bankCode ||
    bankCode ||
    rawPayloadMap?.vnp_BankCode ||
    payment?.paymentProvider ||
    'NCB';
  const resolvedCardType =
    payment?.cardType || cardType || rawPayloadMap?.vnp_CardType || 'ATM';
  const resolvedBankTranNo =
    payment?.bankTranNo || bankTranNo || rawPayloadMap?.vnp_BankTranNo || '—';
  const resolvedPayDate =
    payment?.payDate || payDate || rawPayloadMap?.vnp_PayDate || null;
  const resolvedResponseCode =
    payment?.gatewayResponseCode || vnpResponseCode || rawPayloadMap?.vnp_ResponseCode || (isPaid ? '00' : '24');
  const resolvedOrderInfo =
    orderInfo || rawPayloadMap?.vnp_OrderInfo || `Thanh toan don phong ${payment?.bookingNumber || ''}`;
  const resolvedTransactionStatus =
    transactionStatus || rawPayloadMap?.vnp_TransactionStatus || (isPaid ? '00' : '02');

  const responseInfo = getVnPayResponseInfo(resolvedResponseCode);

  const displayAmount = payment?.totalAmount
    ? payment.totalAmount
    : amountParam
    ? Number(amountParam) / 100
    : 0;

  const [isRetrying, setIsRetrying] = useState(false);
  const [retryError, setRetryError] = useState<string | null>(null);

  const handleRetryPayment = async () => {
    const bId = payment?.bookingId || (bookingIdParam ? Number(bookingIdParam) : null);
    if (!bId) {
      navigate('/finance/folios');
      return;
    }
    setIsRetrying(true);
    setRetryError(null);
    try {
      const res = await financeService.createVnPayPayment({
        bookingId: bId,
        method: 'VNPAY',
        bankCode: 'NCB',
        amount: displayAmount > 0 ? displayAmount : undefined,
      });
      if (res.result?.paymentUrl) {
        window.location.assign(res.result.paymentUrl);
        return;
      }
      throw new Error('Không nhận được liên kết thanh toán từ cổng VNPay Sandbox.');
    } catch (err: unknown) {
      console.error('Lỗi thử lại thanh toán VNPay:', err);
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Không thể khởi tạo lại phiên VNPay. Đang chuyển hướng về trang Quản lý Folio...';
      setRetryError(msg);
      setTimeout(() => {
        navigate(`/finance/folios?bookingId=${bId}`);
      }, 1500);
    } finally {
      setIsRetrying(false);
    }
  };

  const copyToClipboard = (text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  return (
    <main className="min-h-screen bg-neutral-100/70 py-10 px-4 flex items-center justify-center">
      <div className="max-w-2xl w-full space-y-6">
        {/* ========================================================================= */}
        {/* PANEL TRẠNG THÁI & BIÊN LAI XÁC NHẬN CHÍNH CHO KHÁCH HÀNG                   */}
        {/* ========================================================================= */}
        <section
          aria-live="polite"
          className="bg-white rounded-3xl border border-neutral-200 shadow-xl p-8 text-center space-y-6"
        >
          {isPaid && (
            <>
              <div className="w-20 h-20 bg-emerald-50 text-emerald-600 rounded-full flex items-center justify-center mx-auto shadow-inner ring-8 ring-emerald-50/50">
                <CheckCircle2 className="w-12 h-12" aria-hidden="true" />
              </div>
              <div className="space-y-2">
                <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-100 text-emerald-800 text-xs font-bold rounded-full">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  Đã đặt phòng thành công
                </div>
                <h1 className="text-2xl sm:text-3xl font-black text-neutral-900 tracking-tight">
                  Đặt Phòng & Thanh Toán Thành Công!
                </h1>
                <p className="text-sm text-neutral-600 max-w-md mx-auto">
                  {isAdminPopup ? (
                    'Giao dịch thanh toán dịch vụ phòng qua VNPay đã được hoàn tất và ghi nhận vào hệ thống.'
                  ) : (
                    <>
                      Cảm ơn quý khách. Đơn đặt phòng{' '}
                      {payment?.bookingNumber ? (
                        <strong className="text-neutral-900 font-mono">{payment.bookingNumber}</strong>
                      ) : bookingIdParam ? (
                        <strong className="text-neutral-900 font-mono">#{bookingIdParam}</strong>
                      ) : (
                        ''
                      )}{' '}
                      đã được thanh toán và xác nhận thành công qua cổng VNPAY. Hóa đơn GTGT và biên lai đã được gửi về Gmail của quý khách.
                    </>
                  )}
                </p>
                <div className="flex justify-center pt-1">
                  <PaymentStatusBadge status="SUCCESS" />
                </div>
              </div>
            </>
          )}

          {isConfirming && (
            <>
              <div className="w-20 h-20 bg-amber-50 text-amber-600 rounded-full flex items-center justify-center mx-auto shadow-inner ring-8 ring-amber-50/50">
                <Loader2 className="w-12 h-12 animate-spin" aria-hidden="true" />
              </div>
              <div className="space-y-2">
                <h1 className="text-2xl sm:text-3xl font-black text-neutral-900 tracking-tight">
                  Đang Xác Nhận Dữ Liệu Thanh Toán...
                </h1>
                <p className="text-sm text-neutral-600 max-w-md mx-auto">
                  Đang đối soát dữ liệu với cổng VNPAY. Trạng thái sẽ tự động cập nhật trong giây lát.
                </p>
                <div className="flex justify-center pt-1">
                  <PaymentStatusBadge status="PENDING" />
                </div>
                {pollFailed && (
                  <p className="text-xs text-amber-600 pt-1">
                    Đang duy trì kết nối máy chủ để nhận kết quả IPN...
                  </p>
                )}
              </div>
            </>
          )}

          {isFailed && (
            <>
              <div className="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto shadow-inner ring-8 ring-red-50/50">
                <XCircle className="w-12 h-12" aria-hidden="true" />
              </div>
              <div className="space-y-2">
                <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-red-100 text-red-800 text-xs font-bold rounded-full">
                  <XCircle className="w-4 h-4 text-red-600" />
                  Chưa đặt phòng thành công
                </div>
                <h1 className="text-2xl sm:text-3xl font-black text-neutral-900 tracking-tight">
                  Thanh Toán Chưa Hoàn Tất
                </h1>
                <p className="text-sm text-neutral-600 max-w-md mx-auto">
                  {responseInfo.message || 'Giao dịch chưa được hoàn tất (khách hủy, sai OTP hoặc phiên thanh toán hết hạn). Đơn đặt phòng chưa được xác nhận thanh toán.'}
                </p>
                <div className="flex justify-center pt-1">
                  <PaymentStatusBadge status={displayStatus === 'CANCELLED' ? 'CANCELLED' : 'FAILED'} />
                </div>
              </div>
            </>
          )}

          {/* CHI TIẾT BIÊN LAI GIAO DỊCH GỌN GÀNG DÀNH CHO KHÁCH HÀNG */}
          <div className="bg-neutral-50 rounded-2xl border border-neutral-200/80 p-5 text-left space-y-3.5">
            <div className="flex justify-between items-center pb-3 border-b border-neutral-200">
              <span className="text-xs text-neutral-500 font-medium">Tổng số tiền giao dịch</span>
              <span className="text-xl sm:text-2xl font-black text-red-600">
                {formatCurrency(displayAmount)}
              </span>
            </div>

            {(payment?.bookingNumber || bookingIdParam) && (
              <div className="flex justify-between items-center text-xs">
                <span className="text-neutral-500 font-medium">Mã đơn đặt phòng</span>
                <span className="font-bold text-neutral-900 font-mono">
                  {payment?.bookingNumber || `Đơn hàng #${bookingIdParam}`}
                </span>
              </div>
            )}

            <div className="flex justify-between items-center text-xs">
              <span className="text-neutral-500 font-medium">Mã tham chiếu đơn (TxnRef)</span>
              <span className="font-mono text-neutral-800 font-medium">{resolvedTxnRef}</span>
            </div>

            {resolvedTransactionNo !== '—' && (
              <div className="flex justify-between items-center text-xs">
                <span className="text-neutral-500 font-medium">Mã giao dịch VNPAY</span>
                <span className="font-mono font-semibold text-neutral-900">{resolvedTransactionNo}</span>
              </div>
            )}

            <div className="flex justify-between items-center text-xs">
              <span className="text-neutral-500 font-medium">Phương thức thanh toán</span>
              <span className="font-medium text-neutral-800">
                Cổng VNPAY ({resolvedBankCode} - Thẻ ATM nội địa)
              </span>
            </div>

            <div className="flex justify-between items-center text-xs">
              <span className="text-neutral-500 font-medium">Thời gian giao dịch</span>
              <span className="text-neutral-800">
                {resolvedPayDate ? formatVnPayDate(resolvedPayDate) : new Date().toLocaleString('vi-VN')}
              </span>
            </div>
          </div>

          {/* CỤM ĐIỀU HƯỚNG TÙY THEO NGỮ CẢNH: ADMIN MODAL HAY PORTAL KHÁCH */}
          {isAdminPopup ? (
            <div className="space-y-3 pt-2">
              <div className="bg-blue-50 border border-blue-200 text-blue-800 rounded-xl p-3 text-xs flex items-center justify-between">
                <span className="flex items-center gap-2">
                  <Clock className="w-4 h-4 text-blue-600 animate-spin" />
                  <span>
                    Đang tự động đóng cửa sổ sau{' '}
                    <strong className="text-blue-700 font-bold text-sm">{closeCountdown}s</strong>{' '}
                    để đồng bộ về Sơ đồ đặt phòng...
                  </span>
                </span>
              </div>
              <Button
                variant="primary"
                className="w-full h-11 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-md cursor-pointer justify-center"
                onClick={() => {
                  try {
                    localStorage.removeItem('vnpay_active_session');
                    window.close();
                  } catch {
                    void 0;
                  }
                }}
              >
                Đóng Cửa Sổ Ngay Để Quay Lại
              </Button>
            </div>
          ) : (
            <>
              {/* BANNER TỰ ĐỘNG CHUYỂN HƯỚNG ĐẾM NGƯỢC (10 GIÂY) */}
              {isPaid && (
                <div className="bg-red-50/80 border border-red-200/60 rounded-xl p-3 flex flex-wrap items-center justify-between gap-2 text-xs">
                  <span className="text-neutral-700 flex items-center gap-1.5">
                    <Clock className="w-4 h-4 text-red-600 animate-pulse" />
                    <span>
                      Tự động chuyển về trang tìm kiếm sau{' '}
                      <strong className="text-red-600 font-bold text-sm">{countdown}s</strong>
                    </span>
                  </span>
                  <button
                    type="button"
                    onClick={() => setIsCountdownPaused(!isCountdownPaused)}
                    className="text-xs font-semibold text-red-600 hover:text-red-800 underline px-2 py-0.5 rounded cursor-pointer"
                  >
                    {isCountdownPaused ? 'Tiếp tục đếm ngược' : 'Dừng tự chuyển'}
                  </button>
                </div>
              )}

              {/* CỤM NÚT ĐIỀU HƯỚNG CHÍNH DÀNH CHO KHÁCH HÀNG */}
              {isFailed ? (
                <div className="space-y-3 pt-2">
                  <div className="bg-red-50 border border-red-200 rounded-xl p-3.5 text-xs text-red-700 font-medium text-left">
                    ⚠️ Giao dịch chưa được ghi nhận thanh toán. Đơn đặt phòng chưa được quyết toán thanh toán. Vui lòng bấm <strong>Thực hiện thanh toán lại</strong> bên dưới để tiếp tục giao dịch qua VNPAY, hoặc quay lại trang Quản lý Folio.
                  </div>
                  {retryError && (
                    <div className="bg-amber-50 border border-amber-200 rounded-xl p-2.5 text-xs text-amber-700 font-medium text-left">
                      {retryError}
                    </div>
                  )}
                  <div className="flex flex-col sm:flex-row gap-3">
                    <Button
                      variant="primary"
                      className="w-full sm:flex-1 h-12 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-md"
                      onClick={handleRetryPayment}
                      isLoading={isRetrying}
                      leftIcon={<RotateCcw className="w-5 h-5" />}
                    >
                      Thực Hiện Thanh Toán Lại (VNPAY)
                    </Button>

                    <Button
                      variant="outline"
                      className="w-full sm:flex-1 h-12 border-neutral-300 text-neutral-800 hover:bg-neutral-50 font-semibold rounded-xl"
                      onClick={() => {
                        const bNum = payment?.bookingNumber;
                        if (bNum) {
                          navigate(`/portal/my-bookings?bookingNumber=${encodeURIComponent(bNum)}`);
                        } else {
                          navigate('/portal/my-bookings');
                        }
                      }}
                      leftIcon={<List className="w-5 h-5" />}
                    >
                      Danh Sách Đơn Của Tôi
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col sm:flex-row gap-3 pt-2">
                  <Button
                    variant="primary"
                    className="w-full sm:flex-1 h-12 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-md hover:shadow-lg transition-all"
                    onClick={() => navigate('/portal/search')}
                    leftIcon={<Search className="w-5 h-5" />}
                  >
                    Tiếp Tục Đặt Phòng
                  </Button>

                  <Button
                    variant="outline"
                    className="w-full sm:flex-1 h-12 border-neutral-300 text-neutral-800 hover:bg-neutral-50 font-semibold rounded-xl"
                    onClick={() => {
                      const bNum = payment?.bookingNumber;
                      if (bNum) {
                        navigate(`/portal/my-bookings?bookingNumber=${encodeURIComponent(bNum)}`);
                      } else {
                        navigate('/portal/my-bookings');
                      }
                    }}
                    leftIcon={<FileText className="w-5 h-5" />}
                  >
                    Tra Cứu Đơn Đặt Phòng
                  </Button>
                </div>
              )}
            </>
          )}
        </section>

        {/* ========================================================================= */}
        {/* NÚT THU GỌN / MỞ RỘNG ĐỐI CHIẾU DỮ LIỆU KỸ THUẬT VNPAY (MẶC ĐỊNH ẨN)      */}
        {/* ========================================================================= */}
        <div className="text-center">
          <button
            type="button"
            onClick={() => setShowTechnicalDetails(!showTechnicalDetails)}
            className="text-xs text-neutral-400 hover:text-neutral-600 transition-colors inline-flex items-center gap-1.5 py-1.5 px-3 rounded-lg hover:bg-neutral-200/50 cursor-pointer"
          >
            <span>{showTechnicalDetails ? 'Thu gọn dữ liệu đối soát kỹ thuật VNPAY' : 'Xem thông số kỹ thuật đối soát VNPAY'}</span>
            <span>{showTechnicalDetails ? '▲' : '▼'}</span>
          </button>
        </div>

        {/* BẢNG ĐỐI CHIẾU DỮ LIỆU KỸ THUẬT (CHỈ HIỂN THỊ KHI CLICK MỞ RỘNG) */}
        {showTechnicalDetails && (
          <section className="bg-white rounded-3xl border border-neutral-200 shadow-md overflow-hidden animate-fadeIn">
            {/* Header Bảng Đối Chiếu */}
            <div className="bg-gradient-to-r from-neutral-900 to-neutral-800 text-white p-5 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-red-600 flex items-center justify-center text-white font-bold text-sm">
                  VNP
                </div>
                <div>
                  <h2 className="text-base font-bold flex items-center gap-2">
                    <span>Đối Chiếu Dữ Liệu Cổng VNPAY</span>
                    <span className="text-[11px] font-normal px-2 py-0.5 rounded-full bg-white/20 text-neutral-200">
                      Docs v2.1.0
                    </span>
                  </h2>
                  <p className="text-xs text-neutral-400">
                    Đối soát kỹ thuật các tham số do VNPAY Sandbox phản hồi
                  </p>
                </div>
              </div>

              {isSignatureInvalid ? (
                <div className="flex items-center gap-1.5 px-3 py-1.5 bg-red-500/20 border border-red-400/40 text-red-200 rounded-xl text-xs font-semibold">
                  <XCircle className="w-4 h-4 text-red-400" />
                  <span>Chữ ký / Phản hồi Không Hợp Lệ</span>
                </div>
              ) : (
                <div className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-500/20 border border-emerald-400/40 text-emerald-300 rounded-xl text-xs font-semibold">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <span>{isConfirming ? 'Đang đối soát chữ ký...' : 'Chữ ký HMAC-SHA512 Hợp Lệ'}</span>
                </div>
              )}
            </div>

            <div className="p-6 space-y-6">
              {/* NHÓM 1: THÔNG TIN THẺ ATM KIỂM THỬ ĐÃ SỬ DỤNG (THEO DOCS SANDBOX) */}
              <div className="bg-blue-50/60 border border-blue-200 rounded-2xl p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-blue-900 flex items-center gap-2 uppercase tracking-wide">
                    <CreditCard className="w-4 h-4 text-blue-600" />
                    Thông tin Thẻ ATM nội địa (NCB Sandbox)
                  </span>
                  <span className="text-[11px] px-2 py-0.5 rounded-md bg-blue-200 text-blue-900 font-semibold">
                    Thẻ Test Docs VNPay
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                  <div className="bg-white p-2.5 rounded-xl border border-blue-100">
                    <span className="text-[10px] text-neutral-400 block font-medium">NGÂN HÀNG</span>
                    <strong className="text-neutral-900 block font-semibold">NCB (Quốc Dân)</strong>
                  </div>

                  <div className="bg-white p-2.5 rounded-xl border border-blue-100">
                    <span className="text-[10px] text-neutral-400 block font-medium">SỐ THẺ TEST</span>
                    <strong className="text-neutral-900 font-mono block">
                      {VNPAY_ATM_TEST_CARD.formattedCardNumber}
                    </strong>
                  </div>

                  <div className="bg-white p-2.5 rounded-xl border border-blue-100">
                    <span className="text-[10px] text-neutral-400 block font-medium">CHỦ THẺ</span>
                    <strong className="text-neutral-900 block font-semibold">
                      {VNPAY_ATM_TEST_CARD.cardHolder}
                    </strong>
                  </div>

                  <div className="bg-white p-2.5 rounded-xl border border-blue-100">
                    <span className="text-[10px] text-neutral-400 block font-medium">NGÀY PHÁT HÀNH</span>
                    <strong className="text-neutral-900 block font-semibold">
                      {VNPAY_ATM_TEST_CARD.issueDate} (OTP: 123456)
                    </strong>
                  </div>
                </div>
              </div>

              {/* NHÓM 2: BẢNG SO KHỚP CÁC THAM SỐ VNPAY TRẢ VỀ */}
              <div className="border border-neutral-200 rounded-2xl overflow-hidden">
                <div className="bg-neutral-50 px-4 py-2.5 border-b border-neutral-200 font-bold text-xs text-neutral-700 uppercase tracking-wider flex items-center justify-between">
                  <span>Tham số đối chiếu kỹ thuật</span>
                  <span className="text-[11px] text-neutral-500 font-normal">Giá trị phản hồi</span>
                </div>

                <div className="divide-y divide-neutral-100 text-xs">
                  {/* 1. Mã đơn Merchant */}
                  <div className="p-3.5 flex flex-wrap items-center justify-between gap-2 hover:bg-neutral-50/60">
                    <div className="flex items-center gap-2 text-neutral-600">
                      <Hash className="w-4 h-4 text-neutral-400" />
                      <span>Mã tham chiếu đơn hàng (<code>vnp_TxnRef</code>):</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-neutral-900">{resolvedTxnRef}</span>
                      <button
                        type="button"
                        onClick={() => copyToClipboard(resolvedTxnRef, 'txnRef')}
                        className="p-1 text-neutral-400 hover:text-neutral-700"
                        title="Sao chép"
                      >
                        {copiedKey === 'txnRef' ? (
                          <Check className="w-3.5 h-3.5 text-emerald-600" />
                        ) : (
                          <Copy className="w-3.5 h-3.5" />
                        )}
                      </button>
                    </div>
                  </div>

                  {/* 2. Mã giao dịch VNPAY */}
                  <div className="p-3.5 flex flex-wrap items-center justify-between gap-2 hover:bg-neutral-50/60 bg-neutral-50/30">
                    <div className="flex items-center gap-2 text-neutral-600">
                      <Hash className="w-4 h-4 text-red-500" />
                      <span>Mã giao dịch tại VNPAY (<code>vnp_TransactionNo</code>):</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-red-600">{resolvedTransactionNo}</span>
                      <button
                        type="button"
                        onClick={() => copyToClipboard(resolvedTransactionNo, 'transNo')}
                        className="p-1 text-neutral-400 hover:text-neutral-700"
                        title="Sao chép"
                      >
                        {copiedKey === 'transNo' ? (
                          <Check className="w-3.5 h-3.5 text-emerald-600" />
                        ) : (
                          <Copy className="w-3.5 h-3.5" />
                        )}
                      </button>
                    </div>
                  </div>

                  {/* 3. Mã giao dịch ngân hàng */}
                  <div className="p-3.5 flex flex-wrap items-center justify-between gap-2 hover:bg-neutral-50/60">
                    <div className="flex items-center gap-2 text-neutral-600">
                      <Building2 className="w-4 h-4 text-neutral-400" />
                      <span>Mã GD tại Ngân hàng (<code>vnp_BankTranNo</code>):</span>
                    </div>
                    <span className="font-mono font-semibold text-neutral-900">{resolvedBankTranNo}</span>
                  </div>

                  {/* 4. Ngân hàng & Loại thẻ */}
                  <div className="p-3.5 flex flex-wrap items-center justify-between gap-2 hover:bg-neutral-50/60 bg-neutral-50/30">
                    <div className="flex items-center gap-2 text-neutral-600">
                      <CreditCard className="w-4 h-4 text-neutral-400" />
                      <span>Ngân hàng &amp; Loại thẻ (<code>vnp_BankCode</code> / <code>vnp_CardType</code>):</span>
                    </div>
                    <span className="font-semibold text-neutral-900">
                      {resolvedBankCode} • {resolvedCardType} (Thẻ ghi nợ nội địa ATM)
                    </span>
                  </div>

                  {/* 5. Thời gian thanh toán */}
                  <div className="p-3.5 flex flex-wrap items-center justify-between gap-2 hover:bg-neutral-50/60">
                    <div className="flex items-center gap-2 text-neutral-600">
                      <Calendar className="w-4 h-4 text-neutral-400" />
                      <span>Thời gian thanh toán (<code>vnp_PayDate</code>):</span>
                    </div>
                    <span className="font-semibold text-neutral-900">
                      {resolvedPayDate ? formatVnPayDate(resolvedPayDate) : new Date().toLocaleString('vi-VN')}
                    </span>
                  </div>

                  {/* 6. Mã phản hồi & Ý nghĩa */}
                  <div className="p-3.5 flex flex-wrap items-center justify-between gap-2 hover:bg-neutral-50/60 bg-neutral-50/30">
                    <div className="flex items-center gap-2 text-neutral-600">
                      <CheckCircle2 className="w-4 h-4 text-neutral-400" />
                      <span>Mã phản hồi kết quả (<code>vnp_ResponseCode</code>):</span>
                    </div>
                    <div className="text-right">
                      <span
                        className={`inline-block font-mono font-bold px-2 py-0.5 rounded text-xs ${
                          responseInfo.isSuccess
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {responseInfo.code} — {responseInfo.title}
                      </span>
                    </div>
                  </div>

                  {/* 7. Trạng thái giao dịch */}
                  <div className="p-3.5 flex flex-wrap items-center justify-between gap-2 hover:bg-neutral-50/60">
                    <div className="flex items-center gap-2 text-neutral-600">
                      <span>Trạng thái giao dịch (<code>vnp_TransactionStatus</code>):</span>
                    </div>
                    <span className="font-semibold text-neutral-900">
                      {resolvedTransactionStatus === '00' ? '00 (Giao dịch hoàn tất thành công)' : resolvedTransactionStatus}
                    </span>
                  </div>

                  {/* 8. Nội dung đơn hàng */}
                  <div className="p-3.5 flex flex-wrap items-center justify-between gap-2 hover:bg-neutral-50/60 bg-neutral-50/30">
                    <div className="flex items-center gap-2 text-neutral-600">
                      <span>Nội dung đơn hàng (<code>vnp_OrderInfo</code>):</span>
                    </div>
                    <span className="font-medium text-neutral-800">{resolvedOrderInfo}</span>
                  </div>
                </div>
              </div>

              {/* RAW JSON PAYLOAD ACCORDION (DÀNH CHO AUDIT KỸ THUẬT) */}
              <div className="border border-neutral-200 rounded-2xl overflow-hidden">
                <button
                  type="button"
                  onClick={() => setShowRawPayload(!showRawPayload)}
                  className="w-full px-4 py-3 bg-neutral-50 hover:bg-neutral-100/80 transition-colors flex items-center justify-between text-xs font-bold text-neutral-700 cursor-pointer"
                >
                  <span className="flex items-center gap-2">
                    <FileCode className="w-4 h-4 text-neutral-500" />
                    Xem Payload Raw JSON Đối Soát Kỹ Thuật (Audit VNPAY Webhook)
                  </span>
                  <span className="text-neutral-400 text-xs">
                    {showRawPayload ? 'Thu gọn ▲' : 'Mở rộng ▼'}
                  </span>
                </button>

                {showRawPayload && (
                  <div className="p-4 bg-neutral-900 text-emerald-400 font-mono text-[11px] overflow-x-auto space-y-2">
                    <div className="flex justify-between items-center text-neutral-400 border-b border-neutral-800 pb-1">
                      <span>raw_callback_payload (jsonb):</span>
                      <button
                        type="button"
                        onClick={() =>
                          copyToClipboard(
                            payment?.rawCallbackPayload ||
                              JSON.stringify(Object.fromEntries(searchParams.entries()), null, 2),
                            'raw'
                          )
                        }
                        className="hover:text-white flex items-center gap-1 cursor-pointer"
                      >
                        {copiedKey === 'raw' ? 'Đã chép!' : 'Sao chép JSON'}
                      </button>
                    </div>
                    <pre className="whitespace-pre-wrap leading-relaxed">
                      {payment?.rawCallbackPayload
                        ? JSON.stringify(JSON.parse(payment.rawCallbackPayload), null, 2)
                        : JSON.stringify(Object.fromEntries(searchParams.entries()), null, 2)}
                    </pre>
                  </div>
                )}
              </div>

              {/* LIÊN KẾT NHANH CHO NỘI BỘ QUẢN LÝ TÀI CHÍNH */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-neutral-100 text-xs text-neutral-500">
                <button
                  type="button"
                  onClick={() => navigate('/finance/folios')}
                  className="hover:text-neutral-800 flex items-center gap-1"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  Quay lại Quản lý Folio (Kế toán)
                </button>
                {bookingIdParam && (
                  <button
                    type="button"
                    onClick={() => navigate(`/finance/folios?bookingId=${bookingIdParam}`)}
                    className="hover:text-neutral-800 flex items-center gap-1"
                  >
                    Xem Folio Đơn #{bookingIdParam}
                    <ExternalLink className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
            </div>
          </section>
        )}
      </div>
    </main>
  );
};

export default PaymentResultPage;
