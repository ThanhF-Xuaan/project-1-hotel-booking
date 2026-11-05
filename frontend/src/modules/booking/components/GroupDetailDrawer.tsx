import React, { useState, useEffect } from 'react';
import {
  X,
  Building2,
  Calendar,
  DollarSign,
  AlertTriangle,
  BedDouble,
  ShieldAlert,
  FileText,
  CreditCard,
  Ban,
  Receipt,
} from 'lucide-react';
import type {
  GroupContractResponse,
  GroupRoomBlockResponse,
} from '../types/groupBooking.types';
import { groupBookingService } from '../services/groupBooking.service';
import { ConfirmModal } from '../../../core/components/ui/ConfirmModal';

interface GroupDetailDrawerProps {
  contract: GroupContractResponse | null;
  isOpen: boolean;
  onClose: () => void;
  onRefresh: () => Promise<void>;
}

export const GroupDetailDrawer: React.FC<GroupDetailDrawerProps> = ({
  contract,
  isOpen,
  onClose,
  onRefresh,
}) => {
  const [activeTab, setActiveTab] = useState<'INFO' | 'BLOCKS'>('INFO');
  const [roomBlocks, setRoomBlocks] = useState<GroupRoomBlockResponse[]>([]);
  const [isLoadingBlocks, setIsLoadingBlocks] = useState(false);

  // Deposit Form state
  const [depositAmount, setDepositAmount] = useState<string>('');
  const [transactionRef, setTransactionRef] = useState<string>('');
  const [depositNotes, setDepositNotes] = useState<string>('');
  const [isDepositing, setIsDepositing] = useState(false);
  const [depositError, setDepositError] = useState<string | null>(null);

  // Cancel form
  const [cancelReason, setCancelReason] = useState('');
  const [isCancelling, setIsCancelling] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);

  // Cut-off Release confirm modal
  const [releaseBlockId, setReleaseBlockId] = useState<number | null>(null);
  const [isReleasing, setIsReleasing] = useState(false);

  useEffect(() => {
    if (!isOpen || !contract?.id) return;
    let ignore = false;
    Promise.resolve().then(() => {
      if (!ignore) setIsLoadingBlocks(true);
    });
    groupBookingService
      .getRoomBlocks(contract.id)
      .then((res) => {
        if (!ignore) {
          setRoomBlocks(res.result || []);
        }
      })
      .catch((err: unknown) => {
        if (!ignore) {
          console.warn('Lỗi khi tải room blocks:', err);
        }
      })
      .finally(() => {
        if (!ignore) {
          setIsLoadingBlocks(false);
        }
      });

    if (contract.depositAmount && contract.depositPaidAmount < contract.depositAmount) {
      Promise.resolve().then(() => {
        if (!ignore) {
          setDepositAmount(String(contract.depositAmount - contract.depositPaidAmount));
        }
      });
    }

    return () => {
      ignore = true;
    };
  }, [isOpen, contract?.id, contract?.depositAmount, contract?.depositPaidAmount]);

  if (!isOpen || !contract) return null;

  const depositPercent =
    contract.depositAmount > 0
      ? Math.min(100, Math.round((contract.depositPaidAmount / contract.depositAmount) * 100))
      : 0;

  const isCutoffPassed = contract.cutoffDate
    ? new Date(contract.cutoffDate) < new Date()
    : false;

  const handleDepositSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setDepositError(null);
    const amt = Number(depositAmount);
    if (!amt || amt <= 0) {
      setDepositError('Vui lòng nhập số tiền cọc hợp lệ');
      return;
    }
    if (!transactionRef.trim()) {
      setDepositError('Vui lòng nhập mã giao dịch ngân hàng / số phiếu thu');
      return;
    }

    try {
      setIsDepositing(true);
      await groupBookingService.confirmDeposit(contract.id, {
        amount: amt,
        paymentMethodId: 1, // Bank Transfer
        transactionReference: transactionRef.trim(),
        notes: depositNotes.trim() || undefined,
      });
      alert('Xác nhận đặt cọc 50% thành công! Hệ thống đã kích hoạt Room Block DEFINITE và tạo MasterFolio.');
      await onRefresh();
      setTransactionRef('');
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message ||
        (err as { message?: string })?.message ||
        'Lỗi khi xác nhận cọc';
      setDepositError(msg);
    } finally {
      setIsDepositing(false);
    }
  };

  const handleReleaseBlock = (blockId: number) => {
    setReleaseBlockId(blockId);
  };

  const handleConfirmReleaseBlock = async () => {
    if (!releaseBlockId || !contract?.id) return;
    try {
      setIsReleasing(true);
      await groupBookingService.releaseCutoff(contract.id, releaseBlockId);
      const updated = await groupBookingService.getRoomBlocks(contract.id);
      setRoomBlocks(updated.result || []);
      await onRefresh();
      setReleaseBlockId(null);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message ||
        (err as { message?: string })?.message ||
        'Lỗi khi giải phóng phòng';
      alert(msg);
    } finally {
      setIsReleasing(false);
    }
  };

  const handleCancelSubmit = async () => {
    if (!cancelReason.trim()) {
      alert('Vui lòng nhập lý do hủy hợp đồng');
      return;
    }
    try {
      setIsCancelling(true);
      await groupBookingService.cancel(contract.id, { reason: cancelReason.trim() });
      setShowCancelModal(false);
      await onRefresh();
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message ||
        (err as { message?: string })?.message ||
        'Lỗi khi hủy hợp đồng';
      alert(msg);
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/50 backdrop-blur-xs flex justify-end">
      <div className="w-full max-w-3xl bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-300">
        {/* Drawer Header */}
        <div className="p-6 bg-neutral-900 text-white flex items-center justify-between border-b border-neutral-800">
          <div>
            <div className="flex items-center gap-3">
              <span className="px-2.5 py-1 bg-red-600 text-white font-mono text-xs font-bold rounded-lg tracking-wider">
                {contract.contractNumber}
              </span>
              <span
                className={`px-2.5 py-1 text-xs font-bold rounded-lg uppercase ${contract.status === 'DEPOSIT_PAID' || contract.status === 'ROOMING_LIST_ATTACHED'
                  ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                  : contract.status === 'TENTATIVE'
                    ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                    : contract.status === 'IN_HOUSE'
                      ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
                      : 'bg-neutral-700 text-neutral-300'
                  }`}
              >
                {contract.status === 'DEPOSIT_PAID'
                  ? 'Đã đặt cọc 50%'
                  : contract.status === 'TENTATIVE'
                    ? 'Giữ chỗ 3 ngày (Chờ cọc)'
                    : contract.status === 'ROOMING_LIST_ATTACHED'
                      ? 'Đã nộp danh sách'
                      : contract.status === 'IN_HOUSE'
                        ? 'Đang lưu trú'
                        : contract.status}
              </span>
            </div>
            <h2 className="text-xl font-black text-white mt-2 tracking-tight">
              {contract.groupName}
            </h2>
            <p className="text-xs text-neutral-400 mt-0.5">
              {contract.companyName || 'Khách đoàn tự do'} • MST: {contract.companyTaxCode} • Người ký: {contract.signerName || '---'}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 text-neutral-400 hover:text-white rounded-xl hover:bg-neutral-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Controls */}
        <div className="flex border-b border-neutral-200 bg-neutral-50 px-6 pt-3 gap-6 text-xs font-bold uppercase tracking-wider">
          <button
            type="button"
            onClick={() => setActiveTab('INFO')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer flex items-center gap-2 ${activeTab === 'INFO'
              ? 'border-red-600 text-red-600'
              : 'border-transparent text-neutral-500 hover:text-neutral-900'
              }`}
          >
            <Building2 className="w-4 h-4" />
            Hợp Đồng & Quản Lý Cọc
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('BLOCKS')}
            className={`pb-3 border-b-2 transition-colors cursor-pointer flex items-center gap-2 ${activeTab === 'BLOCKS'
              ? 'border-red-600 text-red-600'
              : 'border-transparent text-neutral-500 hover:text-neutral-900'
              }`}
          >
            <BedDouble className="w-4 h-4" />
            Quỹ Phòng Đoàn ({roomBlocks.length})
          </button>
        </div>

        {/* Drawer Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {activeTab === 'INFO' ? (
            <div className="space-y-6">
              {/* Alert: Cutoff status */}
              {contract.status === 'TENTATIVE' && (
                <div
                  className={`p-4 rounded-xl border flex items-start gap-3 ${isCutoffPassed
                    ? 'bg-red-50 border-red-300 text-red-800'
                    : 'bg-amber-50 border-amber-300 text-amber-800'
                    }`}
                >
                  {isCutoffPassed ? (
                    <ShieldAlert className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
                  ) : (
                    <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                  )}
                  <div>
                    <h4 className="text-xs font-black uppercase tracking-wider">
                      {isCutoffPassed
                        ? 'Cảnh báo: Đã quá hạn giữ chỗ 3 ngày!'
                        : 'Lưu ý thời hạn đặt cọc 50% theo Điều 328 BLDS'}
                    </h4>
                    <p className="text-xs mt-1 leading-relaxed">
                      Hạn chót đặt cọc: <strong>{contract.depositDueDate ? new Date(contract.depositDueDate).toLocaleDateString('vi-VN') : '3 ngày kể từ ngày tạo'}</strong>.
                      {isCutoffPassed
                        ? ' Hợp đồng chưa nhận được tiền cọc, vui lòng liên hệ khách hoặc hủy hợp đồng để trả phòng về Public Inventory.'
                        : ' Vui lòng đôn đốc khách thanh toán cọc tối thiểu 50% để chuyển sang trạng thái DEFINITE.'}
                    </p>
                  </div>
                </div>
              )}

              {/* Master Folio Linked Badge */}
              {contract.masterFolioNumber && (
                <div className="p-4 bg-blue-50 border border-blue-200 rounded-xl flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <Receipt className="w-5 h-5 text-blue-600" />
                    <div>
                      <span className="text-xs font-bold text-blue-900 block">
                        Đã kích hoạt Master Folio Quyết toán đoàn:
                      </span>
                      <span className="font-mono text-sm font-black text-blue-700">
                        {contract.masterFolioNumber}
                      </span>
                    </div>
                  </div>
                  <span className="text-xs font-semibold text-blue-600 bg-white px-2.5 py-1 rounded-lg border border-blue-200">
                    Sổ cái công ty
                  </span>
                </div>
              )}

              {/* Financial & Deposit Status Card */}
              <div className="bg-neutral-50 border border-neutral-200 rounded-2xl p-5 space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-black uppercase text-neutral-800 tracking-wider flex items-center gap-2">
                    <DollarSign className="w-4 h-4 text-red-600" />
                    Tiến Độ Thanh Toán Tiền Cọc (Tối thiểu 50%)
                  </h3>
                  <span className="text-xs font-bold text-neutral-600">
                    {depositPercent}% hoàn thành
                  </span>
                </div>

                {/* Progress bar */}
                <div className="w-full bg-neutral-200 h-3 rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all duration-500 rounded-full ${depositPercent >= 100 ? 'bg-green-600' : 'bg-red-600'
                      }`}
                    style={{ width: `${depositPercent}%` }}
                  />
                </div>

                <div className="grid grid-cols-3 gap-3 text-center pt-2">
                  <div className="p-3 bg-white rounded-xl border border-neutral-200">
                    <span className="block text-[11px] font-bold text-neutral-500 uppercase">
                      Tổng giá trị hợp đồng
                    </span>
                    <span className="text-sm font-black text-neutral-900 mt-1 block">
                      {Number(contract.totalContractValue).toLocaleString('vi-VN')} đ
                    </span>
                  </div>
                  <div className="p-3 bg-white rounded-xl border border-neutral-200">
                    <span className="block text-[11px] font-bold text-neutral-500 uppercase">
                      Yêu cầu cọc ({contract.depositRate}%)
                    </span>
                    <span className="text-sm font-black text-red-600 mt-1 block">
                      {Number(contract.depositAmount).toLocaleString('vi-VN')} đ
                    </span>
                  </div>
                  <div className="p-3 bg-white rounded-xl border border-neutral-200">
                    <span className="block text-[11px] font-bold text-neutral-500 uppercase">
                      Đã thanh toán
                    </span>
                    <span className="text-sm font-black text-green-600 mt-1 block">
                      {Number(contract.depositPaidAmount).toLocaleString('vi-VN')} đ
                    </span>
                  </div>
                </div>

                {/* Form xác nhận cọc */}
                {contract.status === 'TENTATIVE' && (
                  <form onSubmit={handleDepositSubmit} className="pt-3 border-t border-neutral-200 space-y-3">
                    <h4 className="text-xs font-black uppercase text-neutral-700 tracking-wider flex items-center gap-1.5">
                      <CreditCard className="w-4 h-4 text-green-600" />
                      Ghi nhận đặt cọc 50%
                    </h4>

                    {depositError && (
                      <p className="text-xs font-bold text-red-600 bg-red-50 p-2 rounded-lg">
                        {depositError}
                      </p>
                    )}

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      <div>
                        <label className="block text-[11px] font-bold text-neutral-600 uppercase mb-1">
                          Số tiền cọc thực nhận (VNĐ) *
                        </label>
                        <input
                          type="number"
                          step="100000"
                          value={depositAmount}
                          onChange={(e) => setDepositAmount(e.target.value)}
                          required
                          className="w-full h-10 px-3 bg-white border border-neutral-300 rounded-xl text-xs font-bold focus:ring-2 focus:ring-red-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block text-[11px] font-bold text-neutral-600 uppercase mb-1">
                          Mã giao dịch ngân hàng / Số phiếu thu *
                        </label>
                        <input
                          type="text"
                          value={transactionRef}
                          onChange={(e) => setTransactionRef(e.target.value)}
                          placeholder="VD: FT2611049928..."
                          required
                          className="w-full h-10 px-3 bg-white border border-neutral-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-red-500 focus:outline-none"
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-[11px] font-bold text-neutral-600 uppercase mb-1">
                        Ghi chú phiếu thu cọc
                      </label>
                      <input
                        type="text"
                        value={depositNotes}
                        onChange={(e) => setDepositNotes(e.target.value)}
                        placeholder="VD: Thu cọc 50% qua chuyển khoản Vietcombank..."
                        className="w-full h-10 px-3 bg-white border border-neutral-300 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-red-500 focus:outline-none"
                      />
                    </div>

                    <div className="flex items-center justify-end gap-3 pt-1">
                      <button
                        type="submit"
                        disabled={isDepositing}
                        className="h-10 px-5 bg-green-600 hover:bg-green-700 text-white rounded-xl text-xs font-bold uppercase transition-colors cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
                      >
                        {isDepositing ? 'Đang xử lý...' : 'Xác Nhận Thu Cọc 50%'}
                      </button>
                    </div>
                  </form>
                )}
              </div>

              {/* Bóc tách Thuế phí chi tiết */}
              <div className="p-4 bg-white border border-neutral-200 rounded-xl space-y-2 text-xs">
                <h4 className="font-black uppercase text-neutral-800 tracking-wider mb-2 flex items-center gap-1.5">
                  <FileText className="w-4 h-4 text-neutral-600" />
                  Bóc Tách Thuế Phí Hợp Đồng
                </h4>
                <div className="flex justify-between py-1 border-b border-neutral-100">
                  <span className="text-neutral-500">Tiền phòng thuần:</span>
                  <span className="font-semibold text-neutral-800">
                    {Number(contract.subtotalAmount).toLocaleString('vi-VN')} đ
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-neutral-100">
                  <span className="text-neutral-500">Phí phục vụ ({contract.serviceFeeRate}%):</span>
                  <span className="font-semibold text-neutral-800">
                    {Number(contract.serviceFeeAmount).toLocaleString('vi-VN')} đ
                  </span>
                </div>
                <div className="flex justify-between py-1 border-b border-neutral-100">
                  <span className="text-neutral-500">Thuế GTGT / VAT ({contract.vatRate}%):</span>
                  <span className="font-semibold text-neutral-800">
                    {Number(contract.vatAmount).toLocaleString('vi-VN')} đ
                  </span>
                </div>
                <div className="flex justify-between pt-1 font-bold">
                  <span className="text-neutral-900">Tổng giá trị thanh toán:</span>
                  <span className="text-red-600 text-sm">
                    {Number(contract.totalContractValue).toLocaleString('vi-VN')} đ
                  </span>
                </div>
              </div>

              {/* Lịch trình & Ghi chú */}
              <div className="p-4 bg-neutral-100 rounded-xl border border-neutral-200 text-xs text-neutral-600 space-y-1">
                <div className="flex items-center gap-2 font-medium">
                  <Calendar className="w-4 h-4 text-neutral-500" />
                  <span>
                    Check-in: <strong>{contract.checkInDate}</strong> | Check-out:{' '}
                    <strong>{contract.checkOutDate}</strong> ({contract.totalRooms} phòng, {contract.totalGuests} khách)
                  </span>
                </div>
                {contract.specialRequests && (
                  <p className="pt-2 text-neutral-500 italic">
                    Yêu cầu đặc biệt: "{contract.specialRequests}"
                  </p>
                )}
              </div>
            </div>
          ) : (
            /* ROOM BLOCKS TAB */
            <div className="space-y-4">
              <div className="bg-neutral-50 p-4 rounded-xl border border-neutral-200">
                <h4 className="text-xs font-black uppercase text-neutral-900 tracking-wider">
                  Quỹ Phòng Đoàn (Room Blocks - API Realtime)
                </h4>
                <p className="text-[11px] text-neutral-500 mt-0.5">
                  Theo dõi số lượng phòng đã block, đã gán khách và thao tác giải phóng phòng quá hạn Cut-off về Public Inventory.
                </p>
              </div>

              {isLoadingBlocks ? (
                <div className="p-12 text-center text-neutral-400 font-medium">
                  Đang tải danh sách Room Blocks từ server...
                </div>
              ) : roomBlocks.length === 0 ? (
                <div className="p-12 text-center border-2 border-dashed border-neutral-200 rounded-2xl text-xs font-medium text-neutral-500">
                  Chưa có Room Block nào được tạo cho hợp đồng này.
                </div>
              ) : (
                <div className="border border-neutral-200 rounded-xl overflow-hidden bg-white shadow-2xs">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-neutral-900 text-white font-black uppercase tracking-wider text-[11px]">
                      <tr>
                        <th className="p-3">Hạng phòng</th>
                        <th className="p-3 text-center">Phòng Block</th>
                        <th className="p-3 text-center">Đã Pick-up</th>
                        <th className="p-3 text-center">Đã Nhả</th>
                        <th className="p-3 text-right">Đơn giá/đêm</th>
                        <th className="p-3 text-right">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-200">
                      {roomBlocks.map((b) => (
                        <tr key={b.id} className="hover:bg-neutral-50/70">
                          <td className="p-3 font-bold text-neutral-900">
                            {b.roomTypeName || `Hạng phòng #${b.hotelRoomTypeId}`}
                          </td>
                          <td className="p-3 text-center font-black text-neutral-900">
                            {b.blockedRooms}
                          </td>
                          <td className="p-3 text-center font-bold text-green-600">
                            {b.pickedUpRooms}
                          </td>
                          <td className="p-3 text-center font-bold text-neutral-400">
                            {b.releasedRooms}
                          </td>
                          <td className="p-3 text-right font-semibold text-neutral-800">
                            {Number(b.negotiatedRatePerNight).toLocaleString('vi-VN')} đ
                          </td>
                          <td className="p-3 text-right">
                            {b.blockedRooms - b.pickedUpRooms - b.releasedRooms > 0 && (
                              <button
                                type="button"
                                onClick={() => handleReleaseBlock(b.id)}
                                className="px-2.5 py-1 bg-red-50 hover:bg-red-100 text-red-600 rounded-lg text-[11px] font-bold uppercase transition-colors cursor-pointer"
                              >
                                Nhả Cut-off
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Drawer Footer */}
        <div className="p-4 bg-neutral-50 border-t border-neutral-200 flex items-center justify-between">
          <div>
            {contract.status === 'TENTATIVE' && (
              <button
                type="button"
                onClick={() => setShowCancelModal(true)}
                className="h-10 px-4 rounded-xl border border-red-300 text-red-600 hover:bg-red-50 font-bold text-xs uppercase transition-colors cursor-pointer flex items-center gap-1.5"
              >
                <Ban className="w-4 h-4" />
                Hủy Hợp Đồng
              </button>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="h-10 px-5 rounded-xl bg-neutral-200 hover:bg-neutral-300 font-bold text-xs text-neutral-700 transition-colors cursor-pointer"
          >
            Đóng lại
          </button>
        </div>
      </div>

      {/* Modal Hủy Hợp đồng (Áp dụng ConfirmModal đa biến thể danger) */}
      <ConfirmModal
        isOpen={showCancelModal}
        onClose={() => setShowCancelModal(false)}
        onConfirm={handleCancelSubmit}
        title="Xác Nhận Hủy Hợp Đồng Khách Đoàn"
        description="Hành động này sẽ hủy hợp đồng đoàn và giải phóng toàn bộ Room Block về Public Inventory."
        confirmText="Đồng Ý Hủy HĐ"
        cancelText="Đóng lại"
        variant="danger"
        isLoading={isCancelling}
      >
        <div>
          <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
            Lý do hủy hợp đồng *
          </label>
          <textarea
            rows={3}
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            placeholder="VD: Khách đổi lịch trình, không duyệt thanh toán cọc..."
            className="w-full p-3 border border-neutral-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-red-500 focus:outline-none"
          />
        </div>
      </ConfirmModal>

      {/* Modal Nhả Quỹ Phòng Cut-off (Áp dụng ConfirmModal biến thể warning) */}
      <ConfirmModal
        isOpen={releaseBlockId !== null}
        onClose={() => setReleaseBlockId(null)}
        onConfirm={handleConfirmReleaseBlock}
        title="Giải Phóng Quỹ Phòng Cut-off"
        description="Bạn có chắc chắn muốn giải phóng các phòng chưa gán về lại quỹ phòng bán công khai (Public Inventory)? Thao tác này sẽ cập nhật số lượng phòng khả dụng của khách sạn ngay lập tức."
        confirmText="Giải Phóng Phòng"
        cancelText="Hủy bỏ"
        variant="warning"
        isLoading={isReleasing}
      />
    </div>
  );
};
