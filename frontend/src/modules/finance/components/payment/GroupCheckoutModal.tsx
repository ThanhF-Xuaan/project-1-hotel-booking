import React, { useState, useEffect } from 'react';
import {
  CreditCard,
  AlertTriangle,
  CheckCircle2,
  Receipt,
  FileText,
  DoorOpen,
  AlertCircle,
  Check,
} from 'lucide-react';
import Modal from '../../../../core/components/ui/Modal';
import { formatCurrency } from '../../../../core/utils/currency';
import { masterFolioService } from '../../services/masterFolio.service';
import type {
  MasterFolioResponse,
  GroupCheckoutRequest,
  GroupCheckoutResponse,
} from '../../types/masterFolio.types';
import type { PaymentMethod } from '../../types/finance.types';

interface GroupCheckoutModalProps {
  isOpen: boolean;
  onClose: () => void;
  contractId: number;
  contractNumber: string;
  groupName: string;
  onSuccess?: (result: GroupCheckoutResponse) => void;
}

const PAYMENT_METHODS: { value: PaymentMethod; label: string; icon: string }[] = [
  { value: 'BANK_TRANSFER', label: 'Chuyển khoản Ngân hàng (VCB, BIDV...)', icon: '🏦' },
  { value: 'CREDIT_CARD', label: 'Thẻ tín dụng / POS (Visa/Mastercard)', icon: '💳' },
  { value: 'CASH', label: 'Tiền mặt tại Quầy Lễ tân', icon: '💵' },
  { value: 'OTHER', label: 'Bảo lãnh Công nợ Doanh nghiệp (AR)', icon: '📑' },
];

export const GroupCheckoutModal: React.FC<GroupCheckoutModalProps> = ({
  isOpen,
  onClose,
  contractId,
  contractNumber,
  groupName,
  onSuccess,
}) => {
  const [masterFolio, setMasterFolio] = useState<MasterFolioResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Form states
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('BANK_TRANSFER');
  const [paymentReference, setPaymentReference] = useState('');
  const [waiveUnpaidSubFolios, setWaiveUnpaidSubFolios] = useState(false);
  const [notes, setNotes] = useState('');

  // VAT Invoice fields
  const [buyerLegalName, setBuyerLegalName] = useState('');
  const [buyerTaxCode, setBuyerTaxCode] = useState('');
  const [buyerAddress, setBuyerAddress] = useState('');
  const [buyerEmailForInvoice, setBuyerEmailForInvoice] = useState('');

  // Result state
  const [checkoutResult, setCheckoutResult] = useState<GroupCheckoutResponse | null>(null);

  useEffect(() => {
    if (!isOpen || !contractId) {
      setCheckoutResult(null);
      setErrorMessage(null);
      return;
    }

    let isMounted = true;
    setIsLoading(true);
    setErrorMessage(null);
    setCheckoutResult(null);

    masterFolioService
      .getMasterFolioByContractId(contractId)
      .then((res) => {
        if (!isMounted) return;
        const folio = res.result;
        setMasterFolio(folio);
        if (folio) {
          setBuyerLegalName(folio.buyerLegalName || '');
          setBuyerTaxCode(folio.buyerTaxCode || '');
          setBuyerAddress(folio.buyerAddress || '');
          setBuyerEmailForInvoice(folio.buyerEmailForInvoice || '');
        }
      })
      .catch((err: any) => {
        if (!isMounted) return;
        const msg = err.response?.data?.message || err.message || 'Không thể tải thông tin Master Folio';
        setErrorMessage(msg);
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isOpen, contractId]);

  if (!isOpen) return null;

  // Tính toán công nợ các phòng con
  const indebtedRooms =
    masterFolio?.subFolios?.filter((sf) => sf.personalBalance > 0) || [];

  const totalIndebtedAmount = indebtedRooms.reduce(
    (sum, sf) => sum + (sf.personalBalance || 0),
    0
  );

  // Tính số tiền cần thanh toán
  const baseBalance = masterFolio?.balance || 0;
  const calculatedBalanceToPay = waiveUnpaidSubFolios
    ? baseBalance + totalIndebtedAmount
    : baseBalance;

  const handleSubmitCheckout = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!masterFolio) return;

    if (!paymentReference.trim()) {
      setErrorMessage('Vui lòng nhập mã tham chiếu giao dịch hoặc số ủy nhiệm chi.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    const payload: GroupCheckoutRequest = {
      paymentMethod,
      paymentReference: paymentReference.trim(),
      waiveUnpaidSubFolios,
      notes: notes.trim() || undefined,
      buyerLegalName: buyerLegalName.trim() || undefined,
      buyerTaxCode: buyerTaxCode.trim() || undefined,
      buyerAddress: buyerAddress.trim() || undefined,
      buyerEmailForInvoice: buyerEmailForInvoice.trim() || undefined,
    };

    try {
      const res = await masterFolioService.checkoutGroup(masterFolio.id, payload);
      setCheckoutResult(res.result);
      if (onSuccess && res.result) {
        onSuccess(res.result);
      }
    } catch (err: any) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        'Quyết toán thất bại. Vui lòng kiểm tra lại công nợ các phòng con.';
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Quyết Toán & Check-out Toàn Đoàn (Master Folio)"
      description={`Quyết toán công nợ tổng hợp, cấn trừ tiền cọc và trả phòng đồng loạt cho hợp đồng ${contractNumber}`}
      maxWidth="4xl"
    >
      {isLoading ? (
        <div className="p-12 text-center text-neutral-500 font-medium">
          <div className="w-8 h-8 border-3 border-red-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
          Đang tải dữ liệu sổ cái Master Folio...
        </div>
      ) : checkoutResult ? (
        /* MÀN HÌNH THÀNH CÔNG */
        <div className="space-y-6 py-2">
          <div className="text-center bg-green-50 border border-green-200 p-6 rounded-2xl">
            <CheckCircle2 className="w-12 h-12 text-green-600 mx-auto mb-2" />
            <h3 className="text-lg font-black text-green-900 uppercase">
              Quyết Toán & Trả Phòng Toàn Đoàn Thành Công!
            </h3>
            <p className="text-xs text-green-700 mt-1 font-medium">
              Sổ cái Master Folio <strong>{checkoutResult.folioNumber}</strong> đã được đóng.
              Toàn bộ {checkoutResult.checkedOutRoomsCount} phòng đã chuyển sang trạng thái Trả
              phòng & Báo dọn phòng (DIRTY).
            </p>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 bg-neutral-50 p-4 rounded-xl border border-neutral-200 text-xs">
            <div>
              <span className="text-neutral-500 block">Tổng chi phí đoàn:</span>
              <span className="font-bold text-neutral-900 text-sm">
                {formatCurrency(checkoutResult.totalChargesAmount)}
              </span>
            </div>
            <div>
              <span className="text-neutral-500 block">Tiền cọc cấn trừ:</span>
              <span className="font-bold text-green-600 text-sm">
                - {formatCurrency(checkoutResult.depositCredited)}
              </span>
            </div>
            <div>
              <span className="text-neutral-500 block">Thanh toán cuối:</span>
              <span className="font-bold text-red-600 text-sm">
                {formatCurrency(checkoutResult.finalPaymentAmount)}
              </span>
            </div>
            <div>
              <span className="text-neutral-500 block">Số dư sổ cái:</span>
              <span className="font-bold text-neutral-900 text-sm">
                {formatCurrency(checkoutResult.remainingBalance)}
              </span>
            </div>
          </div>

          {checkoutResult.checkedOutRoomNumbers && checkoutResult.checkedOutRoomNumbers.length > 0 && (
            <div className="bg-white p-4 rounded-xl border border-neutral-200">
              <span className="text-xs font-black uppercase text-neutral-700 tracking-wider block mb-2">
                Các phòng đã check-out ({checkoutResult.checkedOutRoomsCount} phòng):
              </span>
              <div className="flex flex-wrap gap-2">
                {checkoutResult.checkedOutRoomNumbers.map((rn) => (
                  <span
                    key={rn}
                    className="px-2.5 py-1 bg-neutral-100 border border-neutral-300 rounded-lg text-xs font-black text-neutral-800"
                  >
                    Phòng {rn}
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-neutral-200">
            <button
              type="button"
              onClick={onClose}
              className="h-10 px-6 bg-neutral-900 hover:bg-neutral-800 text-white rounded-xl text-xs font-bold uppercase transition-colors cursor-pointer"
            >
              Hoàn tất & Đóng lại
            </button>
          </div>
        </div>
      ) : (
        /* MÀN HÌNH FORM QUYẾT TOÁN */
        <form onSubmit={handleSubmitCheckout} className="space-y-5">
          {errorMessage && (
            <div className="p-3.5 bg-red-50 border border-red-200 rounded-xl text-xs font-bold text-red-700 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <div>{errorMessage}</div>
            </div>
          )}

          {/* 1. Header Overview Card */}
          <div className="bg-neutral-900 text-white p-4 rounded-2xl flex flex-wrap items-center justify-between gap-3">
            <div>
              <span className="text-[10px] font-black uppercase text-neutral-400 tracking-wider block">
                Sổ cái Master Folio
              </span>
              <h3 className="text-base font-black text-white flex items-center gap-2">
                <Receipt className="w-4 h-4 text-red-500" />
                {masterFolio?.folioNumber || 'MF-PENDING'}
              </h3>
              <p className="text-xs text-neutral-300 mt-0.5 font-medium">
                Đoàn: <strong>{groupName}</strong> | Hợp đồng: <strong>{contractNumber}</strong>
              </p>
            </div>
            <div className="text-right">
              <span className="text-[10px] font-black uppercase text-neutral-400 tracking-wider block">
                Trạng thái
              </span>
              <span className="inline-block px-2.5 py-0.5 rounded-full text-xs font-black uppercase bg-green-500/20 text-green-400 border border-green-500/30">
                {masterFolio?.status || 'OPEN'}
              </span>
            </div>
          </div>

          {/* 2. Sub-Folio Checklist */}
          <div className="bg-white border border-neutral-200 rounded-2xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-black uppercase text-neutral-800 tracking-wider flex items-center gap-1.5">
                <DoorOpen className="w-4 h-4 text-neutral-600" />
                Kiểm soát công nợ phòng con (Sub-Folios)
              </h4>
              <span className="text-xs text-neutral-500 font-semibold">
                Tổng {masterFolio?.subFolios?.length || 0} phòng thành viên
              </span>
            </div>

            {indebtedRooms.length > 0 ? (
              <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl text-xs space-y-2">
                <div className="flex items-center gap-1.5 font-bold text-amber-900">
                  <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                  <span>
                    Phát hiện {indebtedRooms.length} phòng còn nợ chi phí cá nhân (Minibar, Giặt
                    là...):
                  </span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2 pt-1">
                  {indebtedRooms.map((sf) => (
                    <div
                      key={sf.bookingRoomId}
                      className="p-2 bg-white rounded-lg border border-amber-300 text-[11px]"
                    >
                      <span className="font-bold text-neutral-900 block">
                        Phòng {sf.roomNumber} ({sf.guestName || 'Khách đoàn'})
                      </span>
                      <span className="text-red-600 font-black">
                        Còn nợ: {formatCurrency(sf.personalBalance)}
                      </span>
                    </div>
                  ))}
                </div>

                <label className="flex items-center gap-2 pt-2 border-t border-amber-200 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={waiveUnpaidSubFolios}
                    onChange={(e) => setWaiveUnpaidSubFolios(e.target.checked)}
                    className="w-4 h-4 text-red-600 rounded focus:ring-red-500 border-neutral-300 cursor-pointer"
                  />
                  <span className="font-bold text-neutral-800 text-xs">
                    Gộp toàn bộ nợ cá nhân này ({formatCurrency(totalIndebtedAmount)}) vào Master
                    Folio để Trưởng đoàn/Công ty thanh toán thay.
                  </span>
                </label>
              </div>
            ) : (
              <div className="p-3 bg-green-50 border border-green-200 rounded-xl text-xs font-bold text-green-800 flex items-center gap-2">
                <Check className="w-4 h-4 text-green-600" />
                Toàn bộ các phòng con đã sạch nợ cá nhân (0 đ). Đủ điều kiện check-out ngay!
              </div>
            )}
          </div>

          {/* 3. Bảng Cấn Trừ Tiền Cọc & Sổ Cái */}
          <div className="bg-white border border-neutral-200 rounded-2xl p-4 space-y-2 text-xs">
            <h4 className="font-black uppercase text-neutral-800 tracking-wider mb-2 flex items-center gap-1.5">
              <FileText className="w-4 h-4 text-neutral-600" />
              Bảng Cấn Trừ Tiền Cọc & Quyết Toán (Deposit Netting)
            </h4>
            <div className="flex justify-between py-1 border-b border-neutral-100">
              <span className="text-neutral-500">Tiền phòng thuần & Hợp đồng:</span>
              <span className="font-semibold text-neutral-800">
                {formatCurrency(masterFolio?.subtotalAmount || 0)}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b border-neutral-100">
              <span className="text-neutral-500">Phí dịch vụ:</span>
              <span className="font-semibold text-neutral-800">
                {formatCurrency(masterFolio?.serviceFeeAmount || 0)}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b border-neutral-100">
              <span className="text-neutral-500">Thuế GTGT (VAT):</span>
              <span className="font-semibold text-neutral-800">
                {formatCurrency(masterFolio?.totalVatAmount || 0)}
              </span>
            </div>
            {waiveUnpaidSubFolios && totalIndebtedAmount > 0 && (
              <div className="flex justify-between py-1 border-b border-amber-200 bg-amber-50/50 px-2 rounded">
                <span className="font-bold text-amber-800">Nợ cá nhân gộp vào Master:</span>
                <span className="font-bold text-amber-900">
                  + {formatCurrency(totalIndebtedAmount)}
                </span>
              </div>
            )}
            <div className="flex justify-between py-1 border-b border-neutral-100 font-bold">
              <span className="text-neutral-700">Tổng chi phí đoàn:</span>
              <span className="text-neutral-900">
                {formatCurrency(
                  (masterFolio?.totalAmount || 0) +
                    (waiveUnpaidSubFolios ? totalIndebtedAmount : 0)
                )}
              </span>
            </div>
            <div className="flex justify-between py-1 border-b border-neutral-100 text-green-600 font-bold">
              <span>Đã thanh toán cọc 50%:</span>
              <span>- {formatCurrency(masterFolio?.depositCredited || 0)}</span>
            </div>
            <div className="flex justify-between pt-2 text-sm font-black text-red-600">
              <span>SỐ TIỀN CÒN PHẢI THANH TOÁN:</span>
              <span>{formatCurrency(calculatedBalanceToPay)}</span>
            </div>
          </div>

          {/* 4. Form Thanh toán & Xuất Hóa đơn VAT */}
          <div className="bg-neutral-50 border border-neutral-200 rounded-2xl p-4 space-y-4">
            <h4 className="text-xs font-black uppercase text-neutral-800 tracking-wider flex items-center gap-1.5">
              <CreditCard className="w-4 h-4 text-neutral-600" />
              Thanh toán khoản chênh lệch & Xuất hóa đơn VAT
            </h4>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-bold text-neutral-700 uppercase mb-1">
                  Phương thức thanh toán *
                </label>
                <select
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
                  className="w-full h-10 px-3 bg-white border border-neutral-300 rounded-xl text-xs font-bold text-neutral-900 focus:ring-2 focus:ring-red-500 focus:outline-none cursor-pointer"
                >
                  {PAYMENT_METHODS.map((m) => (
                    <option key={m.value} value={m.value}>
                      {m.icon} {m.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-neutral-700 uppercase mb-1">
                  Mã giao dịch / Số ủy nhiệm chi *
                </label>
                <input
                  type="text"
                  value={paymentReference}
                  onChange={(e) => setPaymentReference(e.target.value)}
                  placeholder="VD: FT26110688921 hoặc POS-TXN-101"
                  required
                  className="w-full h-10 px-3 bg-white border border-neutral-300 rounded-xl text-xs font-semibold text-neutral-900 focus:ring-2 focus:ring-red-500 focus:outline-none"
                />
              </div>
            </div>

            {/* Thông tin hóa đơn điện tử */}
            <div className="p-3 bg-white border border-neutral-200 rounded-xl space-y-2 text-xs">
              <span className="text-[11px] font-black uppercase text-neutral-600 tracking-wider block">
                Thông tin xuất hóa đơn điện tử GTGT:
              </span>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                <div>
                  <label className="block text-[10px] font-bold text-neutral-500 uppercase">
                    Tên pháp nhân công ty
                  </label>
                  <input
                    type="text"
                    value={buyerLegalName}
                    onChange={(e) => setBuyerLegalName(e.target.value)}
                    className="w-full h-8 px-2.5 bg-neutral-50 border border-neutral-200 rounded-lg text-xs font-medium"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-bold text-neutral-500 uppercase">
                    Mã số thuế (MST)
                  </label>
                  <input
                    type="text"
                    value={buyerTaxCode}
                    onChange={(e) => setBuyerTaxCode(e.target.value)}
                    className="w-full h-8 px-2.5 bg-neutral-50 border border-neutral-200 rounded-lg text-xs font-medium"
                  />
                </div>
              </div>
              <div>
                <label className="block text-[10px] font-bold text-neutral-500 uppercase">
                  Email nhận hóa đơn điện tử
                </label>
                <input
                  type="email"
                  value={buyerEmailForInvoice}
                  onChange={(e) => setBuyerEmailForInvoice(e.target.value)}
                  className="w-full h-8 px-2.5 bg-neutral-50 border border-neutral-200 rounded-lg text-xs font-medium"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-bold text-neutral-700 uppercase mb-1">
                Ghi chú quyết toán
              </label>
              <input
                type="text"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="VD: Khách trả phòng đúng hạn, cấn trừ cọc hợp đồng..."
                className="w-full h-10 px-3 bg-white border border-neutral-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-red-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Action Footer */}
          <div className="flex items-center justify-between pt-2 border-t border-neutral-200">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="h-10 px-5 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-xl text-xs font-bold uppercase transition-colors cursor-pointer"
            >
              Hủy bỏ
            </button>

            <button
              type="submit"
              disabled={isSubmitting}
              className="h-10 px-6 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-black uppercase transition-colors cursor-pointer disabled:opacity-50 flex items-center gap-2 shadow-sm"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Đang quyết toán...
                </>
              ) : (
                <>
                  <Receipt className="w-4 h-4" />
                  Xác Nhận Quyết Toán & Check-out Toàn Đoàn
                </>
              )}
            </button>
          </div>
        </form>
      )}
    </Modal>
  );
};

export default GroupCheckoutModal;
