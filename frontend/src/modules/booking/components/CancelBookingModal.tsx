import React, { useState, useEffect } from 'react';
import { AlertTriangle, Ban, CheckCircle, CreditCard } from 'lucide-react';
import Modal from '../../../core/components/ui/Modal';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import bookingService from '../services/booking.service';
import type {
  BookingCancelPreviewResponse,
  BookingCancelRequest,
  BookingCancelResponse,
} from '../types/booking.types';

export interface CancelBookingModalProps {
  isOpen: boolean;
  onClose: () => void;
  target: {
    bookingId: number;
    bookingNumber: string;
    bookingRoomId?: number;
    roomNumber?: string;
    roomTypeName?: string;
  } | null;
  onSuccess: (response: BookingCancelResponse) => void;
}

const COMMON_REASONS = [
  'Khách đổi lịch trình / kế hoạch đột xuất',
  'Khách đặt nhầm ngày hoặc thông tin phòng',
  'Lý do bất khả kháng (Sức khỏe / Thời tiết)',
  'Khách yêu cầu hoàn hủy qua điện thoại / email',
  'Lý do khác (Nhập chi tiết bên dưới)',
];

export const CancelBookingModal: React.FC<CancelBookingModalProps> = ({
  isOpen,
  onClose,
  target,
  onSuccess,
}) => {
  const [preview, setPreview] = useState<BookingCancelPreviewResponse | null>(null);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [previewError, setPreviewError] = useState<string | null>(null);

  // Form states
  const [selectedReasonOption, setSelectedReasonOption] = useState<string>(COMMON_REASONS[0]);
  const [customReasonDetail, setCustomReasonDetail] = useState('');
  const [bankAccountNumber, setBankAccountNumber] = useState('');
  const [bankName, setBankName] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen || !target) {
      setPreview(null);
      setPreviewError(null);
      setSubmitError(null);
      return;
    }

    const fetchPreview = async () => {
      setIsLoadingPreview(true);
      setPreviewError(null);
      setSubmitError(null);
      try {
        let res;
        if (target.bookingRoomId) {
          res = await bookingService.previewCancelBookingRoom(target.bookingRoomId);
        } else {
          res = await bookingService.previewCancelBooking(target.bookingId);
        }
        if (res.result) {
          setPreview(res.result);
        } else {
          setPreviewError('Không thể lấy thông tin tính toán hoàn hủy từ máy chủ.');
        }
      } catch (err: unknown) {
        console.error('Lỗi khi tính toán phí hủy:', err);
        const errorMsg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          'Không thể tính toán chi phí hủy phòng. Vui lòng kiểm tra lại trạng thái đơn hàng.';
        setPreviewError(errorMsg);
      } finally {
        setIsLoadingPreview(false);
      }
    };

    fetchPreview();
  }, [isOpen, target]);

  if (!target) return null;

  const formatVND = (amount?: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  const isRoomLevel = Boolean(target.bookingRoomId);
  const title = isRoomLevel
    ? `Hủy phòng ${target.roomNumber ? `Phòng ${target.roomNumber}` : (target.roomTypeName || '')} (Đơn ${target.bookingNumber})`
    : `Hủy đơn đặt phòng ${target.bookingNumber}`;

  const finalReason =
    selectedReasonOption === COMMON_REASONS[COMMON_REASONS.length - 1]
      ? customReasonDetail.trim()
      : customReasonDetail.trim()
      ? `${selectedReasonOption} - ${customReasonDetail.trim()}`
      : selectedReasonOption;

  const handleConfirmCancel = async () => {
    if (!finalReason) {
      setSubmitError('Vui lòng nhập lý do hủy phòng.');
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    const payload: BookingCancelRequest = {
      reason: finalReason,
      bankAccountNumber: bankAccountNumber.trim() || undefined,
      bankName: bankName.trim() || undefined,
      accountHolderName: accountHolderName.trim() || undefined,
    };

    try {
      let res;
      if (target.bookingRoomId) {
        res = await bookingService.cancelBookingRoom(target.bookingRoomId, payload);
      } else {
        res = await bookingService.cancelBooking(target.bookingId, payload);
      }

      if (res.result) {
        onSuccess(res.result);
        onClose();
      } else {
        setSubmitError('Không nhận được phản hồi xác nhận từ hệ thống.');
      }
    } catch (err: unknown) {
      console.error('Lỗi thực hiện hủy:', err);
      const errorMsg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Hủy phòng không thành công. Vui lòng kiểm tra lại điều kiện hủy.';
      setSubmitError(errorMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} maxWidth="lg">
      <div className="space-y-4 text-sm">
        {/* Loading Preview */}
        {isLoadingPreview && (
          <div className="p-8 text-center text-neutral-500">
            <div className="inline-block animate-spin w-6 h-6 border-2 border-red-600 border-t-transparent rounded-full mb-2" />
            <p className="text-xs">Đang đối chiếu chính sách hủy FIT và tính toán hoàn tiền...</p>
          </div>
        )}

        {/* Preview Error */}
        {previewError && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-xl flex items-start gap-3 text-red-700 text-xs">
            <AlertTriangle className="w-5 h-5 shrink-0 text-red-600 mt-0.5" />
            <div>
              <p className="font-semibold text-sm">Không thể thực hiện hủy</p>
              <p className="mt-1">{previewError}</p>
            </div>
          </div>
        )}

        {/* Calculation Summary Card */}
        {preview && !isLoadingPreview && (
          <>
            <div className="bg-neutral-50 border border-neutral-200 rounded-xl p-4 space-y-3">
              <div className="flex items-center justify-between pb-2 border-b border-neutral-200">
                <span className="text-xs font-semibold text-neutral-600 uppercase tracking-wider">
                  Chính sách phạt hủy FIT:
                </span>
                <span className="font-bold text-xs px-2.5 py-1 bg-red-100 text-red-800 rounded-md border border-red-200">
                  Phạt {preview.penaltyRate}% ({preview.policyTier})
                </span>
              </div>

              {/* Scope notice */}
              <div className="text-xs text-neutral-600 flex items-center justify-between">
                <span>Phạm vi tác động:</span>
                <span className="font-semibold text-neutral-900">
                  {preview.willCancelWholeBooking
                    ? `Hủy toàn bộ đơn (${preview.cancelledRoomsCount} phòng)`
                    : `Hủy ${preview.cancelledRoomsCount} phòng (Còn lại ${preview.remainingRoomsCount} phòng tiếp tục lưu trú)`}
                </span>
              </div>

              {/* Financial Table */}
              <div className="space-y-1.5 pt-2 border-t border-neutral-200 text-xs">
                <div className="flex justify-between items-center text-neutral-600">
                  <span>Tiền phòng gốc (Dự kiến):</span>
                  <span className="font-medium text-neutral-800">{formatVND(preview.roomOriginalAmount)}</span>
                </div>

                <div className="flex justify-between items-center text-neutral-600">
                  <span>Tổng tiền khách đã thanh toán:</span>
                  <span className="font-semibold text-neutral-900">{formatVND(preview.totalPaid)}</span>
                </div>

                <div className="flex justify-between items-center text-red-600 font-semibold">
                  <span>Phí phạt hủy phòng ({preview.penaltyRate}%):</span>
                  <span>- {formatVND(preview.cancellationPenalty)}</span>
                </div>

                {preview.preorderedServicesAmount > 0 && (
                  <div className="flex justify-between items-center text-amber-700">
                    <span>Dịch vụ đặt trước không hoàn lại (SQ-03):</span>
                    <span>- {formatVND(preview.preorderedServicesAmount)}</span>
                  </div>
                )}

                {preview.retainedAddonsAmount > 0 && (
                  <div className="flex justify-between items-center text-neutral-600">
                    <span>Phụ thu / Gói tiện ích không hoàn lại:</span>
                    <span>- {formatVND(preview.retainedAddonsAmount)}</span>
                  </div>
                )}

                {/* Net Refund Calculation */}
                <div className="flex justify-between items-center pt-2 border-t border-neutral-300 text-sm font-bold">
                  <span className="text-emerald-700">Số tiền hoàn trả khách:</span>
                  <span className="text-emerald-700 text-base">{formatVND(preview.refundAmount)}</span>
                </div>
              </div>

              {/* Refund Method Banner */}
              <div
                className={`p-3 rounded-lg border text-xs flex items-center gap-2.5 ${
                  preview.refundMethod === 'AUTO_GATEWAY_REFUND'
                    ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                    : 'bg-amber-50 border-amber-200 text-amber-800'
                }`}
              >
                {preview.refundMethod === 'AUTO_GATEWAY_REFUND' ? (
                  <>
                    <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" />
                    <div>
                      <p className="font-semibold">Hoàn tiền tự động qua Cổng thanh toán (Auto-Refund)</p>
                      <p className="text-[11px] text-emerald-700 mt-0.5">
                        Số tiền hoàn ≤ 5.000.000 VNĐ. Tiền sẽ được hoàn trả tự động vào nguồn tiền gốc của khách hàng.
                      </p>
                    </div>
                  </>
                ) : (
                  <>
                    <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
                    <div>
                      <p className="font-semibold">Chờ Bộ phận Kế toán duyệt hoàn tiền (Waiting Finance Approval)</p>
                      <p className="text-[11px] text-amber-700 mt-0.5">
                        Số tiền hoàn &gt; 5.000.000 VNĐ cần duyệt theo quy trình kế toán tài chính. Vui lòng cung cấp số tài khoản nhận tiền.
                      </p>
                    </div>
                  </>
                )}
              </div>
            </div>

            {/* Bank info form if waiting finance approval and refund > 0 */}
            {preview.refundMethod === 'WAITING_FINANCE_APPROVAL' && preview.refundAmount > 0 && (
              <div className="p-3 bg-neutral-50 border border-neutral-200 rounded-xl space-y-3">
                <p className="text-xs font-semibold text-neutral-800 flex items-center gap-1.5">
                  <CreditCard className="w-4 h-4 text-blue-600" />
                  Thông tin tài khoản nhận hoàn tiền (Tùy chọn ghi chú cho Kế toán):
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  <div>
                    <label className="block text-[11px] text-neutral-600 mb-1">Tên ngân hàng</label>
                    <Input
                      placeholder="VD: Vietcombank"
                      value={bankName}
                      onChange={(e) => setBankName(e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] text-neutral-600 mb-1">Số tài khoản</label>
                    <Input
                      placeholder="VD: 1012345678"
                      value={bankAccountNumber}
                      onChange={(e) => setBankAccountNumber(e.target.value)}
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] text-neutral-600 mb-1">Chủ tài khoản</label>
                    <Input
                      placeholder="VD: NGUYEN VAN A"
                      value={accountHolderName}
                      onChange={(e) => setAccountHolderName(e.target.value)}
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Cancellation Reason Selector */}
            <div className="space-y-2">
              <label className="block text-xs font-semibold text-neutral-700">
                Lý do hủy phòng <span className="text-red-500">*</span>
              </label>
              <select
                className="w-full text-xs p-2.5 border border-neutral-300 rounded-xl bg-white focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={selectedReasonOption}
                onChange={(e) => setSelectedReasonOption(e.target.value)}
              >
                {COMMON_REASONS.map((reason) => (
                  <option key={reason} value={reason}>
                    {reason}
                  </option>
                ))}
              </select>

              <textarea
                className="w-full text-xs p-2.5 border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden min-h-[70px]"
                placeholder="Ghi chú chi tiết thêm lý do hủy (nếu có)..."
                value={customReasonDetail}
                onChange={(e) => setCustomReasonDetail(e.target.value)}
              />
            </div>

            {/* Asset release note */}
            <div className="p-2.5 bg-blue-50 border border-blue-200 rounded-xl text-[11px] text-blue-800 space-y-1">
              <p className="font-semibold">Tự động thu hồi tài nguyên sau khi hủy:</p>
              <ul className="list-disc list-inside space-y-0.5 text-blue-700">
                <li>Kho phòng còn trống (Room Availability) sẽ được hoàn trả cho ngày lưu trú.</li>
                <li>Lịch giữ phòng (RoomSlot) sẽ được giải phóng lập tức.</li>
                <li>Hàng chờ lưu trú BCA / Công an tự động hủy hồ sơ chưa đăng ký.</li>
              </ul>
            </div>
          </>
        )}

        {/* Submit Error */}
        {submitError && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700">
            <span className="font-semibold">Lỗi: </span>
            {submitError}
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex justify-end gap-2 pt-3 border-t border-neutral-200">
          <Button variant="outline" size="sm" onClick={onClose} disabled={isSubmitting}>
            Đóng
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={handleConfirmCancel}
            isLoading={isSubmitting}
            disabled={!preview || isLoadingPreview || Boolean(previewError)}
            className="bg-red-600 hover:bg-red-700 text-white font-semibold rounded-xl"
          >
            <Ban className="w-4 h-4 mr-1.5" />
            {isRoomLevel ? 'Xác nhận hủy phòng' : 'Xác nhận hủy đơn'}
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export default CancelBookingModal;
