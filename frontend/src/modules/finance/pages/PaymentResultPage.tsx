import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, Loader2, XCircle } from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import financeService from '../services/finance.service';
import PaymentStatusBadge from '../components/payment/PaymentStatusBadge';
import type { PaymentResponse, PaymentStatus } from '../types/finance.types';

/**
 * Trang kết quả sau khi quay về từ cổng thanh toán:
 * - VNPay: BE /vnpay/return verify chữ ký → 302 về đây với ?status=&txnRef=&paymentId=&bookingId=
 * - MoMo: redirect kèm ?resultCode=&orderId=...
 * Poll trạng thái theo paymentId (3s/lần) — useEffect PHẢI có cleanup (Rules 03).
 */
export const PaymentResultPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const statusParam = searchParams.get('status'); // PAID | FAILED (qua BE /vnpay/return)
  const vnpResponseCode = searchParams.get('vnp_ResponseCode'); // fallback nếu redirect thẳng từ VNPay
  const momoResultCode = searchParams.get('resultCode'); // MoMo redirect
  const txnRef =
    searchParams.get('txnRef') || searchParams.get('vnp_TxnRef') || searchParams.get('orderId');
  const transactionNo = searchParams.get('vnp_TransactionNo');
  const paymentIdParam = searchParams.get('paymentId');
  const paymentId = paymentIdParam ? Number(paymentIdParam) : undefined;

  const isPaidByParams =
    statusParam === 'PAID' || vnpResponseCode === '00' || momoResultCode === '0';

  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [pollFailed, setPollFailed] = useState(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Poll trạng thái thật từ BE — dừng khi không còn PENDING; cleanup khi unmount (Rules 03)
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
        // Lần sau poll lại — không đốt UI bằng lỗi giữa chừng
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

  // Trạng thái hiển thị: có payment → status thật; chưa tải được → suy ra từ param redirect
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

  return (
    <main className="min-h-screen bg-neutral-50 flex items-center justify-center p-4">
      <section
        aria-live="polite"
        className="bg-white rounded-2xl border border-neutral-200 shadow-xl p-8 max-w-md w-full text-center space-y-6"
      >
        {isPaid && (
          <>
            <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-10 h-10" aria-hidden="true" />
            </div>
            <div className="space-y-2">
              <h1 className="text-xl font-bold text-neutral-900">Thanh toán thành công!</h1>
              <p className="text-sm text-neutral-500">
                Giao dịch của bạn đã được xác nhận. Cảm ơn quý khách!
              </p>
              <div className="flex justify-center">
                <PaymentStatusBadge status="SUCCESS" />
              </div>
            </div>
          </>
        )}

        {isConfirming && (
          <>
            <div className="w-16 h-16 bg-amber-100 text-amber-600 rounded-full flex items-center justify-center mx-auto">
              <Loader2 className="w-10 h-10 animate-spin" aria-hidden="true" />
            </div>
            <div className="space-y-2">
              <h1 className="text-xl font-bold text-neutral-900">Đang xác nhận thanh toán...</h1>
              <p className="text-sm text-neutral-500">
                Không đóng trang này — trạng thái sẽ tự cập nhật trong giây lát.
              </p>
              <div className="flex justify-center">
                <PaymentStatusBadge status="PENDING" />
              </div>
              {pollFailed && (
                <p className="text-xs text-amber-600">
                  Chưa kết nối được máy chủ — đang thử lại...
                </p>
              )}
            </div>
          </>
        )}

        {isFailed && (
          <>
            <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto">
              <XCircle className="w-10 h-10" aria-hidden="true" />
            </div>
            <div className="space-y-2">
              <h1 className="text-xl font-bold text-neutral-900">Thanh toán thất bại</h1>
              <p className="text-sm text-neutral-500">
                {searchParams.get('message') ||
                  'Giao dịch chưa được hoàn tất (khách hủy, hết hạn hoặc lỗi kết nối).'}
              </p>
              <div className="flex justify-center">
                <PaymentStatusBadge status={displayStatus === 'CANCELLED' ? 'CANCELLED' : 'FAILED'} />
              </div>
            </div>
          </>
        )}

        {(txnRef || transactionNo || payment) && (
          <div className="bg-neutral-50 border border-neutral-200 rounded-xl p-4 space-y-1 text-left text-xs text-neutral-600">
            {payment && (
              <div className="flex justify-between">
                <span>Số tiền:</span>
                <b className="text-neutral-900">
                  {new Intl.NumberFormat('vi-VN').format(Number(payment.totalAmount))} đ
                </b>
              </div>
            )}
            {(txnRef || payment?.transactionReference) && (
              <div className="flex justify-between gap-3">
                <span>Mã đơn (txnRef):</span>
                <b className="font-mono text-neutral-900 break-all">
                  {txnRef || payment?.transactionReference}
                </b>
              </div>
            )}
            {(transactionNo || payment?.transactionReference) && (
              <div className="flex justify-between gap-3">
                <span>Mã tham chiếu:</span>
                <b className="font-mono text-neutral-900 break-all">
                  {transactionNo || payment?.transactionReference}
                </b>
              </div>
            )}
            <div className="flex justify-between">
              <span>Trạng thái:</span>
              <PaymentStatusBadge status={displayStatus} />
            </div>
          </div>
        )}

        <div className="flex flex-col gap-3 pt-2">
          {isFailed && (
            <Button variant="primary" onClick={() => navigate(-1)}>
              Thử thanh toán lại
            </Button>
          )}
          <Button
            variant={isFailed ? 'outline' : 'primary'}
            onClick={() => navigate(-1)}
            leftIcon={<ArrowLeft className="w-4 h-4" />}
          >
            Quay lại trang trước
          </Button>
        </div>
      </section>
    </main>
  );
};

export default PaymentResultPage;
