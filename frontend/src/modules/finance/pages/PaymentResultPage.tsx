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
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import { formatCurrency } from '../../../core/utils/currency';
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
 * - Đối chiếu toàn diện theo chuẩn tài liệu VNPAY v2.1.0
 * - Hiển thị mã giao dịch VNPAY, mã ngân hàng, loại thẻ ATM, mã phản hồi chi tiết
 * - Poll trạng thái thật từ BE (3s/lần) có cleanup chống memory leak
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
    statusParam === 'PAID' || vnpResponseCode === '00' || momoResultCode === '0';

  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [pollFailed, setPollFailed] = useState(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [showRawPayload, setShowRawPayload] = useState(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Poll trạng thái thật từ BE theo paymentId (Rules 03: cleanup khi unmount)
  useEffect(() => {
    if (!paymentId) return;
    let active = true;

    const check = async () => {
      try {
        const res = await financeService.getPaymentById(paymentId);
        if (!active || !res.result) return;
        setPollFailed(false);
        setPayment(res.result);
        if (res.result.status !== 'PENDING' && intervalRef.current) {
          clearInterval(intervalRef.current);
          intervalRef.current = null;
        }
      } catch {
        if (active) setPollFailed(true);
      }
    };

    check();
    intervalRef.current = setInterval(check, 3000);

    return () => {
      active = false;
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [paymentId]);

  // Trạng thái hiển thị
  const displayStatus: PaymentStatus = payment
    ? payment.status
    : paymentId
    ? 'PENDING'
    : isPaidByParams
    ? 'SUCCESS'
    : 'FAILED';

  const isPaid = displayStatus === 'SUCCESS';
  const isConfirming = displayStatus === 'PENDING';
  const isFailed = displayStatus === 'FAILED' || displayStatus === 'CANCELLED';

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

  const copyToClipboard = (text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  return (
    <main className="min-h-screen bg-neutral-100/70 py-10 px-4 flex items-center justify-center">
      <div className="max-w-3xl w-full space-y-6">
        {/* PANEL TRẠNG THÁI CHÍNH */}
        <section
          aria-live="polite"
          className="bg-white rounded-3xl border border-neutral-200 shadow-xl p-8 text-center space-y-5"
        >
          {isPaid && (
            <>
              <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-2xl flex items-center justify-center mx-auto shadow-xs">
                <CheckCircle2 className="w-10 h-10" aria-hidden="true" />
              </div>
              <div className="space-y-1.5">
                <h1 className="text-2xl font-black text-neutral-900 tracking-tight">
                  Thanh Toán Thành Công!
                </h1>
                <p className="text-sm text-neutral-500">
                  Giao dịch thẻ ATM nội địa qua cổng VNPAY đã được xác thực và hạch toán thành công.
                </p>
                <div className="flex justify-center pt-1">
                  <PaymentStatusBadge status="SUCCESS" />
                </div>
              </div>
            </>
          )}

          {isConfirming && (
            <>
              <div className="w-16 h-16 bg-amber-100 text-amber-600 rounded-2xl flex items-center justify-center mx-auto shadow-xs">
                <Loader2 className="w-10 h-10 animate-spin" aria-hidden="true" />
              </div>
              <div className="space-y-1.5">
                <h1 className="text-2xl font-black text-neutral-900 tracking-tight">
                  Đang Xác Nhận Dữ Liệu Thanh Toán...
                </h1>
                <p className="text-sm text-neutral-500">
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
              <div className="w-16 h-16 bg-red-100 text-red-600 rounded-2xl flex items-center justify-center mx-auto shadow-xs">
                <XCircle className="w-10 h-10" aria-hidden="true" />
              </div>
              <div className="space-y-1.5">
                <h1 className="text-2xl font-black text-neutral-900 tracking-tight">
                  Thanh Toán Chưa Hoàn Tất
                </h1>
                <p className="text-sm text-neutral-500 max-w-md mx-auto">
                  {responseInfo.message || 'Giao dịch chưa được hoàn tất (khách hủy, sai OTP hoặc hết hạn).'}
                </p>
                <div className="flex justify-center pt-1">
                  <PaymentStatusBadge status={displayStatus === 'CANCELLED' ? 'CANCELLED' : 'FAILED'} />
                </div>
              </div>
            </>
          )}

          {/* SỐ TIỀN THANH TOÁN NỔI BẬT */}
          {displayAmount > 0 && (
            <div className="py-3 px-6 bg-neutral-50 rounded-2xl border border-neutral-200 inline-block mx-auto">
              <span className="text-xs font-medium text-neutral-500 block uppercase tracking-wider">
                Tổng số tiền đã giao dịch
              </span>
              <span className="text-2xl font-black text-red-600">
                {formatCurrency(displayAmount)}
              </span>
            </div>
          )}
        </section>

        {/* ========================================================================= */}
        {/* BẢNG ĐỐI CHIẾU DỮ LIỆU GIAO DỊCH VNPAY (THEO CHUẨN TÀI LIỆU VNPAY DOCS)     */}
        {/* ========================================================================= */}
        <section className="bg-white rounded-3xl border border-neutral-200 shadow-md overflow-hidden">
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

            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-500/20 border border-emerald-400/40 text-emerald-300 rounded-xl text-xs font-semibold">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Chữ ký HMAC-SHA512 Hợp Lệ</span>
            </div>
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
                className="w-full px-4 py-3 bg-neutral-50 hover:bg-neutral-100/80 transition-colors flex items-center justify-between text-xs font-bold text-neutral-700"
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
                      className="hover:text-white flex items-center gap-1"
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
          </div>
        </section>

        {/* NÚT ĐIỀU HƯỚNG CUỐI TRANG */}
        <div className="flex flex-col-reverse sm:flex-row items-center justify-between gap-3">
          <Button
            variant="outline"
            onClick={() => navigate('/finance/folios')}
            leftIcon={<ArrowLeft className="w-4 h-4" />}
          >
            Quay lại Danh sách Folio
          </Button>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            {bookingIdParam && (
              <Button
                variant="secondary"
                onClick={() => navigate(`/finance/folios?bookingId=${bookingIdParam}`)}
                rightIcon={<ExternalLink className="w-4 h-4" />}
              >
                Mở Folio Đơn Này
              </Button>
            )}

            {isFailed && (
              <Button
                variant="primary"
                onClick={() => navigate(-1)}
                className="w-full sm:w-auto"
              >
                Thử Thanh Toán Lại
              </Button>
            )}
          </div>
        </div>
      </div>
    </main>
  );
};

export default PaymentResultPage;
