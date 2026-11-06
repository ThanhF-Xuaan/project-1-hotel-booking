import React, { useState, useEffect, useCallback } from 'react';
import {
  X,
  Receipt,
  Building2,
  DollarSign,
  ShieldCheck,
  Lock,
  Unlock,
  CheckCircle2,
  AlertCircle,
  ArrowRightLeft,
  Plus,
  RefreshCw,
  Printer,
  BedDouble,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import { ConfirmModal } from '../../../core/components/ui/Modal';
import { masterFolioService } from '../services/masterFolio.service';
import type {
  MasterFolioResponse,
  MasterFolioLineItemDto,
  ChargeCategory,
  RoutingTarget,
  BillingRoutingRuleRequest,
} from '../types/masterFolio.types';
import { RoutingAdjustmentModal } from './RoutingAdjustmentModal';

interface MasterFolioModalProps {
  isOpen: boolean;
  onClose: () => void;
  contractId: number;
}

export const MasterFolioModal: React.FC<MasterFolioModalProps> = ({
  isOpen,
  onClose,
  contractId,
}) => {
  const [folio, setFolio] = useState<MasterFolioResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'DEBITS' | 'SUB_FOLIOS' | 'ROUTING_RULES'>('DEBITS');

  // Adjustment Modal
  const [adjustingItem, setAdjustingItem] = useState<MasterFolioLineItemDto | null>(null);
  const [isAdjustmentOpen, setIsAdjustmentOpen] = useState<boolean>(false);

  // Status Action Modals
  const [isLockConfirmOpen, setIsLockConfirmOpen] = useState<boolean>(false);
  const [isSettleConfirmOpen, setIsSettleConfirmOpen] = useState<boolean>(false);
  const [isActionLoading, setIsActionLoading] = useState<boolean>(false);

  // Add Routing Rule form state
  const [newCategory, setNewCategory] = useState<ChargeCategory>('ROOM_RATE');
  const [newTarget, setNewTarget] = useState<RoutingTarget>('MASTER');
  const [newMaxAmount, setNewMaxAmount] = useState<number | undefined>(undefined);
  const [isAddingRule, setIsAddingRule] = useState<boolean>(false);
  const [ruleError, setRuleError] = useState<string | null>(null);

  const loadFolio = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await masterFolioService.getByContractId(contractId);
      if (res.result) {
        setFolio(res.result);
      } else {
        setError('Hợp đồng đoàn này chưa phát sinh Master Folio (Yêu cầu xác nhận cọc tối thiểu 50% để kích hoạt).');
      }
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data
          ?.message ||
        (err as { message?: string })?.message ||
        'Chưa thể tải Sổ cái Master Folio. Vui lòng kiểm tra trạng thái đặt cọc của đoàn.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [contractId]);

  useEffect(() => {
    if (!isOpen || !contractId) return;
    let ignore = false;
    Promise.resolve().then(() => {
      if (!ignore) {
        void loadFolio();
      }
    });
    return () => {
      ignore = true;
    };
  }, [isOpen, contractId, loadFolio]);

  if (!isOpen) return null;

  const formatVND = (amt?: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amt || 0);

  const handleOpenAdjustment = (item: MasterFolioLineItemDto) => {
    setAdjustingItem(item);
    setIsAdjustmentOpen(true);
  };

  const handleAdjustmentSuccess = (updatedFolio: MasterFolioResponse) => {
    setFolio(updatedFolio);
  };

  const handleConfirmLock = async () => {
    if (!folio) return;
    try {
      setIsActionLoading(true);
      const res = await masterFolioService.updateStatus(folio.id, {
        status: 'LOCKED_FOR_SETTLEMENT',
        note: 'Khóa sổ chuẩn bị quyết toán xuất hóa đơn GTGT',
      });
      if (res.result) setFolio(res.result);
      setIsLockConfirmOpen(false);
    } catch (err: unknown) {
      alert((err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Lỗi khi khóa sổ');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleConfirmSettle = async () => {
    if (!folio) return;
    try {
      setIsActionLoading(true);
      const res = await masterFolioService.updateStatus(folio.id, {
        status: 'SETTLED',
        note: 'Đã hoàn tất thanh toán quyết toán đoàn và xuất hóa đơn tổng hợp',
      });
      if (res.result) setFolio(res.result);
      setIsSettleConfirmOpen(false);
    } catch (err: unknown) {
      alert((err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Lỗi khi quyết toán');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleAddRoutingRule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!folio) return;
    setRuleError(null);
    try {
      setIsAddingRule(true);
      const payload: BillingRoutingRuleRequest = {
        chargeCategory: newCategory,
        routeTarget: newTarget,
        maxCoveredAmount: newMaxAmount,
        isActive: true,
      };
      await masterFolioService.addRoutingRule(folio.id, payload);
      await loadFolio();
      setNewMaxAmount(undefined);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } }; message?: string })?.response?.data
          ?.message || 'Lỗi khi thêm quy tắc định tuyến';
      setRuleError(msg);
    } finally {
      setIsAddingRule(false);
    }
  };

  const handleDeleteRule = async (ruleId: number) => {
    if (!folio) return;
    if (!confirm('Bạn có chắc chắn muốn vô hiệu hóa quy tắc định tuyến này?')) return;
    try {
      await masterFolioService.deleteRoutingRule(folio.id, ruleId);
      await loadFolio();
    } catch (err: unknown) {
      alert((err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Lỗi khi xóa quy tắc');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-3 md:p-6 overflow-y-auto scrollbar-none">
      <div className="relative w-full max-w-5xl bg-white rounded-2xl shadow-2xl border border-neutral-200 overflow-hidden my-auto flex flex-col max-h-[92vh] animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="bg-neutral-900 text-white p-5 flex items-center justify-between border-b border-neutral-800 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl bg-red-600 flex items-center justify-center text-white shadow-md">
              <Receipt className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2.5">
                <h2 className="text-lg font-black tracking-tight">Sổ Cái Master Folio & Quyết Toán Đoàn</h2>
                {folio && (
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold tracking-wide uppercase border ${
                      folio.status === 'SETTLED'
                        ? 'bg-blue-500/20 text-blue-400 border-blue-500/30'
                        : folio.status === 'LOCKED_FOR_SETTLEMENT'
                        ? 'bg-amber-500/20 text-amber-400 border-amber-500/30'
                        : folio.status === 'CLOSED'
                        ? 'bg-neutral-500/20 text-neutral-400 border-neutral-500/30'
                        : 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'
                    }`}
                  >
                    {folio.status === 'OPEN' && 'Đang Mở (Ghi Nhận)'}
                    {folio.status === 'LOCKED_FOR_SETTLEMENT' && 'Đã Khóa Chờ Quyết Toán'}
                    {folio.status === 'SETTLED' && 'Đã Quyết Toán Xong'}
                    {folio.status === 'CLOSED' && 'Đã Đóng Sổ'}
                  </span>
                )}
              </div>
              <p className="text-xs text-neutral-400 mt-0.5 flex items-center gap-2">
                <span>Mã Folio: <strong className="text-white font-mono">{folio?.folioNumber || 'Đang tải...'}</strong></span>
                <span>•</span>
                <span>Hợp đồng: <strong className="text-white font-mono">{folio?.contractNumber || `#${contractId}`}</strong></span>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={loadFolio} title="Tải lại Sổ cái">
              <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            </Button>
            <button
              type="button"
              onClick={onClose}
              aria-label="Đóng modal"
              className="text-neutral-400 hover:text-white p-1 rounded-lg transition-colors"
            >
              <X className="w-6 h-6" />
            </button>
          </div>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto scrollbar-none p-6 space-y-6">
          {isLoading && !folio ? (
            <div className="py-20 text-center text-neutral-500 space-y-3">
              <RefreshCw className="w-8 h-8 animate-spin text-red-600 mx-auto" />
              <p className="text-sm font-medium">Đang nạp dữ liệu Sổ cái Master Folio...</p>
            </div>
          ) : error && !folio ? (
            <div className="py-16 text-center max-w-md mx-auto space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mx-auto">
                <AlertCircle className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-neutral-900">Chưa Khởi Tạo Sổ Cái</h3>
              <p className="text-xs text-neutral-600 leading-relaxed">{error}</p>
              <Button variant="primary" size="md" onClick={onClose}>
                Quay lại chi tiết hợp đồng
              </Button>
            </div>
          ) : folio ? (
            <>
              {/* Buyer / Corporate Info Banner */}
              <div className="bg-neutral-50 rounded-2xl p-4 border border-neutral-200 grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                <div>
                  <span className="text-neutral-400 font-bold uppercase text-[10px]">Đơn vị thụ hưởng xuất hóa đơn GTGT:</span>
                  <div className="text-sm font-bold text-neutral-900 mt-0.5 flex items-center gap-1.5">
                    <Building2 className="w-4 h-4 text-red-600" />
                    {folio.buyerLegalName || folio.companyName || 'Doanh nghiệp đối tác B2B'}
                  </div>
                  <div className="text-neutral-600 mt-1">
                    Mã số thuế: <strong className="font-mono text-neutral-900">{folio.buyerTaxCode || 'Chưa cập nhật'}</strong>
                  </div>
                </div>
                <div>
                  <span className="text-neutral-400 font-bold uppercase text-[10px]">Khách sạn phục vụ:</span>
                  <div className="text-sm font-bold text-neutral-900 mt-0.5">
                    {folio.hotelName || 'Khách sạn cơ sở'}
                  </div>
                  <div className="text-neutral-600 mt-1 truncate">
                    Địa chỉ: {folio.buyerAddress || 'Địa chỉ doanh nghiệp ghi nhận theo hợp đồng'}
                  </div>
                </div>
              </div>

              {/* 4 Financial Cards */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="p-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
                  <span className="text-[11px] font-bold text-neutral-500 uppercase tracking-wider block">
                    Tổng Chi Phí Đoàn
                  </span>
                  <div className="text-lg font-black text-neutral-900 mt-1 font-mono">
                    {formatVND(folio.totalAmount)}
                  </div>
                  <span className="text-[10px] text-neutral-400 mt-0.5 block">Đã gồm VAT 10% & Phí DV 5%</span>
                </div>

                <div className="p-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
                  <span className="text-[11px] font-bold text-emerald-600 uppercase tracking-wider block">
                    Tiền Cọc 50% Đã Trừ
                  </span>
                  <div className="text-lg font-black text-emerald-600 mt-1 font-mono">
                    {formatVND(folio.depositCredited)}
                  </div>
                  <span className="text-[10px] text-neutral-400 mt-0.5 block">Cấn trừ cọc hợp đồng đoàn</span>
                </div>

                <div className="p-4 bg-white rounded-2xl border border-neutral-200 shadow-xs">
                  <span className="text-[11px] font-bold text-blue-600 uppercase tracking-wider block">
                    Tổng Tiền Đã Thu
                  </span>
                  <div className="text-lg font-black text-blue-600 mt-1 font-mono">
                    {formatVND(folio.totalPaid)}
                  </div>
                  <span className="text-[10px] text-neutral-400 mt-0.5 block">Đã ghi nhận qua ngân hàng</span>
                </div>

                <div className="p-4 bg-red-50/60 rounded-2xl border border-red-200 shadow-xs">
                  <span className="text-[11px] font-bold text-red-700 uppercase tracking-wider block">
                    Dư Nợ Còn Phải Thu
                  </span>
                  <div className="text-xl font-black text-red-600 mt-1 font-mono">
                    {formatVND(folio.balance)}
                  </div>
                  <span className="text-[10px] text-red-600/70 font-semibold mt-0.5 block">
                    Quyết toán xuất hóa đơn tổng
                  </span>
                </div>
              </div>

              {/* Navigation Tabs */}
              <div className="flex items-center gap-2 border-b border-neutral-200 pb-2">
                <button
                  type="button"
                  onClick={() => setActiveTab('DEBITS')}
                  className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-2 ${
                    activeTab === 'DEBITS'
                      ? 'bg-neutral-900 text-white shadow-xs'
                      : 'text-neutral-600 hover:bg-neutral-100'
                  }`}
                >
                  <DollarSign className="w-4 h-4" />
                  Chi Phí Công Ty Chịu ({folio.debits?.length || 0})
                </button>

                <button
                  type="button"
                  onClick={() => setActiveTab('SUB_FOLIOS')}
                  className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-2 ${
                    activeTab === 'SUB_FOLIOS'
                      ? 'bg-neutral-900 text-white shadow-xs'
                      : 'text-neutral-600 hover:bg-neutral-100'
                  }`}
                >
                  <BedDouble className="w-4 h-4" />
                  Hóa Đơn Phụ Khách Lẻ ({folio.subFolios?.length || 0})
                </button>

                <button
                  type="button"
                  onClick={() => setActiveTab('ROUTING_RULES')}
                  className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-2 ${
                    activeTab === 'ROUTING_RULES'
                      ? 'bg-neutral-900 text-white shadow-xs'
                      : 'text-neutral-600 hover:bg-neutral-100'
                  }`}
                >
                  <ShieldCheck className="w-4 h-4" />
                  Quy Tắc Định Tuyến ({folio.routingRules?.length || 0})
                </button>
              </div>

              {/* TAB 1: DEBITS (Chi phí công ty chịu) */}
              {activeTab === 'DEBITS' && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <p className="text-xs text-neutral-500">
                      Bao gồm toàn bộ các khoản mục tiền phòng thỏa thuận, tiệc và các dịch vụ mà hợp đồng quy định công ty chịu.
                    </p>
                    <span className="text-xs font-bold text-neutral-700">
                      Tổng mục ghi nợ: <strong className="text-neutral-900">{folio.debits?.length || 0}</strong>
                    </span>
                  </div>

                  <div className="border border-neutral-200 rounded-2xl overflow-hidden">
                    <table className="w-full text-left text-xs text-neutral-700">
                      <thead className="bg-neutral-100 text-neutral-900 font-bold uppercase text-[10px] tracking-wider border-b border-neutral-200">
                        <tr>
                          <th className="p-3">Khoản mục / Diễn giải</th>
                          <th className="p-3">Ngày</th>
                          <th className="p-3">Phòng / Khách</th>
                          <th className="p-3 text-right">Đơn giá</th>
                          <th className="p-3 text-right">SL</th>
                          <th className="p-3 text-right">Thuế + Phí DV</th>
                          <th className="p-3 text-right">Tổng thành tiền</th>
                          <th className="p-3 text-center">Thao tác</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-neutral-200">
                        {(!folio.debits || folio.debits.length === 0) ? (
                          <tr>
                            <td colSpan={8} className="p-8 text-center text-neutral-400">
                              Chưa có chi phí ghi nợ nào phát sinh cho Master Folio.
                            </td>
                          </tr>
                        ) : (
                          folio.debits.map((item, idx) => (
                            <tr key={`${item.referenceId}-${idx}`} className="hover:bg-neutral-50/60 transition-colors">
                              <td className="p-3 font-semibold text-neutral-900">
                                <div>{item.description}</div>
                                <span className="text-[10px] text-neutral-400 font-mono">
                                  Loại: {item.chargeCategory}
                                </span>
                              </td>
                              <td className="p-3 text-neutral-600 whitespace-nowrap">
                                {item.itemDate || 'Trong kỳ lưu trú'}
                              </td>
                              <td className="p-3 text-neutral-600 whitespace-nowrap">
                                <div className="font-bold text-neutral-800">{item.roomNumber || 'Toàn đoàn'}</div>
                                <div className="text-[10px] text-neutral-400">{item.guestName || 'Đại diện đoàn'}</div>
                              </td>
                              <td className="p-3 text-right font-mono text-neutral-600">
                                {formatVND(item.unitPrice)}
                              </td>
                              <td className="p-3 text-right font-bold text-neutral-800">
                                {item.quantity || 1}
                              </td>
                              <td className="p-3 text-right text-neutral-500 font-mono text-[11px]">
                                {formatVND((item.vatAmount || 0) + (item.serviceFeeAmount || 0))}
                              </td>
                              <td className="p-3 text-right font-bold text-neutral-900 font-mono">
                                {formatVND(item.totalAmount)}
                              </td>
                              <td className="p-3 text-center whitespace-nowrap">
                                {folio.status !== 'SETTLED' && folio.status !== 'CLOSED' && (
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => handleOpenAdjustment(item)}
                                    leftIcon={<ArrowRightLeft className="w-3.5 h-3.5" />}
                                    className="text-[11px] py-1 px-2.5"
                                  >
                                    Điều chuyển
                                  </Button>
                                )}
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* TAB 2: SUB_FOLIOS (Hóa đơn phụ khách lẻ) */}
              {activeTab === 'SUB_FOLIOS' && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <p className="text-xs text-neutral-500">
                      Theo dõi riêng các chi phí phát sinh cá nhân (minibar, giặt là...) của từng phòng để thu tiền khi khách lẻ check-out.
                    </p>
                    <span className="text-xs font-bold text-neutral-700">
                      Tổng số phòng đoàn: <strong className="text-neutral-900">{folio.subFolios?.length || 0}</strong>
                    </span>
                  </div>

                  <div className="border border-neutral-200 rounded-2xl overflow-hidden">
                    <table className="w-full text-left text-xs text-neutral-700">
                      <thead className="bg-neutral-100 text-neutral-900 font-bold uppercase text-[10px] tracking-wider border-b border-neutral-200">
                        <tr>
                          <th className="p-3">Số Phòng</th>
                          <th className="p-3">Khách Lưu Trú</th>
                          <th className="p-3">Hạng Phòng</th>
                          <th className="p-3 text-right">Công Ty Gánh (Master)</th>
                          <th className="p-3 text-right">Phát Sinh Cá Nhân</th>
                          <th className="p-3 text-right">Khách Đã Trả</th>
                          <th className="p-3 text-right">Dư Nợ Khách</th>
                          <th className="p-3 text-center">Trạng Thái</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-neutral-200">
                        {(!folio.subFolios || folio.subFolios.length === 0) ? (
                          <tr>
                            <td colSpan={8} className="p-8 text-center text-neutral-400">
                              Chưa có phòng nào được gán danh sách khách (Rooming List).
                            </td>
                          </tr>
                        ) : (
                          folio.subFolios.map((sub, idx) => (
                            <tr key={`${sub.bookingId}-${idx}`} className="hover:bg-neutral-50/60 transition-colors">
                              <td className="p-3 font-black text-neutral-900 font-mono">
                                {sub.roomNumber || 'Chưa gán'}
                              </td>
                              <td className="p-3 font-semibold text-neutral-900">
                                {sub.guestName || 'Chưa nhập tên khách'}
                              </td>
                              <td className="p-3 text-neutral-600">
                                {sub.roomTypeName || 'Hạng phòng đoàn'}
                              </td>
                              <td className="p-3 text-right font-mono text-emerald-700 font-bold">
                                {formatVND(sub.coveredByMasterAmount)}
                              </td>
                              <td className="p-3 text-right font-mono text-amber-700 font-bold">
                                {formatVND(sub.personalDebitAmount)}
                              </td>
                              <td className="p-3 text-right font-mono text-blue-700">
                                {formatVND(sub.personalPaidAmount)}
                              </td>
                              <td className="p-3 text-right font-mono font-black">
                                <span
                                  className={
                                    sub.personalBalance > 0
                                      ? 'text-red-600'
                                      : 'text-neutral-500'
                                  }
                                >
                                  {formatVND(sub.personalBalance)}
                                </span>
                              </td>
                              <td className="p-3 text-center whitespace-nowrap">
                                <span
                                  className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                    sub.personalBalance <= 0
                                      ? 'bg-green-100 text-green-700'
                                      : 'bg-red-100 text-red-700'
                                  }`}
                                >
                                  {sub.personalBalance <= 0 ? 'Đã Thanh Toán' : 'Chưa Quyết Toán'}
                                </span>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* TAB 3: ROUTING RULES (Quy tắc định tuyến) */}
              {activeTab === 'ROUTING_RULES' && (
                <div className="space-y-5">
                  <div className="p-4 bg-neutral-50 rounded-2xl border border-neutral-200">
                    <h4 className="text-xs font-bold text-neutral-900 uppercase tracking-wider mb-2">
                      + Thêm Quy Tắc Định Tuyến Mới
                    </h4>
                    {ruleError && (
                      <div className="p-2.5 mb-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-xs">
                        {ruleError}
                      </div>
                    )}
                    <form onSubmit={handleAddRoutingRule} className="grid grid-cols-1 sm:grid-cols-4 gap-3 items-end">
                      <div>
                        <label className="block text-[11px] font-bold text-neutral-700 mb-1">
                          Danh mục chi phí *
                        </label>
                        <Select<ChargeCategory>
                          value={newCategory}
                          onChange={(val) => setNewCategory(val)}
                          options={[
                            { value: 'ROOM_RATE', label: 'Tiền phòng (ROOM_RATE)' },
                            { value: 'BREAKFAST', label: 'Bữa sáng (BREAKFAST)' },
                            { value: 'BANQUET', label: 'Tiệc sự kiện (BANQUET)' },
                            { value: 'MINIBAR', label: 'Đồ uống Minibar (MINIBAR)' },
                            { value: 'LAUNDRY', label: 'Giặt ủi (LAUNDRY)' },
                            { value: 'CUSTOM', label: 'Tùy chỉnh khác (CUSTOM)' },
                          ]}
                        />
                      </div>

                      <div>
                        <label className="block text-[11px] font-bold text-neutral-700 mb-1">
                          Mục tiêu gánh nợ *
                        </label>
                        <Select<RoutingTarget>
                          value={newTarget}
                          onChange={(val) => setNewTarget(val)}
                          options={[
                            { value: 'MASTER', label: 'MASTER (Doanh nghiệp chịu)' },
                            { value: 'SUB_FOLIO', label: 'SUB_FOLIO (Khách tự trả)' },
                          ]}
                        />
                      </div>

                      <div>
                        <label className="block text-[11px] font-bold text-neutral-700 mb-1">
                          Hạn mức tối đa (Để trống = Vô hạn)
                        </label>
                        <Input
                          type="number"
                          placeholder="Không giới hạn"
                          value={newMaxAmount ?? ''}
                          onChange={(e) =>
                            setNewMaxAmount(e.target.value ? Number(e.target.value) : undefined)
                          }
                          min="0"
                          step="100000"
                        />
                      </div>

                      <Button
                        type="submit"
                        variant="primary"
                        isLoading={isAddingRule}
                        leftIcon={<Plus className="w-4 h-4" />}
                      >
                        Thêm quy tắc
                      </Button>
                    </form>
                  </div>

                  <div className="border border-neutral-200 rounded-2xl overflow-hidden">
                    <table className="w-full text-left text-xs text-neutral-700">
                      <thead className="bg-neutral-100 text-neutral-900 font-bold uppercase text-[10px] tracking-wider border-b border-neutral-200">
                        <tr>
                          <th className="p-3">Mã Rule</th>
                          <th className="p-3">Danh Mục Chi Phí</th>
                          <th className="p-3">Bên Chi Trả (Routing Target)</th>
                          <th className="p-3">Hạn Mức Chi Trả</th>
                          <th className="p-3 text-center">Trạng Thái</th>
                          <th className="p-3 text-center">Thao Tác</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-neutral-200">
                        {(!folio.routingRules || folio.routingRules.length === 0) ? (
                          <tr>
                            <td colSpan={6} className="p-8 text-center text-neutral-400">
                              Chưa có quy tắc định tuyến nào được thiết lập.
                            </td>
                          </tr>
                        ) : (
                          folio.routingRules.map((rule) => (
                            <tr key={rule.id} className="hover:bg-neutral-50/60 transition-colors">
                              <td className="p-3 font-mono text-neutral-500 font-bold">#{rule.id}</td>
                              <td className="p-3 font-bold text-neutral-900">{rule.chargeCategory}</td>
                              <td className="p-3">
                                <span
                                  className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                                    rule.routeTarget === 'MASTER'
                                      ? 'bg-blue-100 text-blue-800'
                                      : 'bg-amber-100 text-amber-800'
                                  }`}
                                >
                                  {rule.routeTarget === 'MASTER'
                                    ? 'MASTER (Doanh nghiệp chịu)'
                                    : 'SUB_FOLIO (Khách cá nhân)'}
                                </span>
                              </td>
                              <td className="p-3 font-mono text-neutral-700">
                                {rule.maxCoveredAmount
                                  ? formatVND(rule.maxCoveredAmount)
                                  : 'Không giới hạn hạn mức'}
                              </td>
                              <td className="p-3 text-center">
                                <span
                                  className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                                    rule.isActive ? 'bg-green-100 text-green-700' : 'bg-neutral-200 text-neutral-600'
                                  }`}
                                >
                                  {rule.isActive ? 'Đang hiệu lực' : 'Đã vô hiệu'}
                                </span>
                              </td>
                              <td className="p-3 text-center">
                                {rule.isActive && (
                                  <button
                                    type="button"
                                    onClick={() => handleDeleteRule(rule.id)}
                                    className="text-xs font-bold text-red-600 hover:text-red-700 cursor-pointer"
                                  >
                                    Vô hiệu hóa
                                  </button>
                                )}
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </>
          ) : null}
        </div>

        {/* Footer Actions */}
        {folio && (
          <div className="bg-neutral-50 px-6 py-4 border-t border-neutral-200 flex items-center justify-between gap-4 shrink-0">
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="md"
                onClick={() => window.print()}
                leftIcon={<Printer className="w-4 h-4" />}
              >
                In Sổ Cái Master Folio
              </Button>
            </div>

            <div className="flex items-center gap-3">
              {folio.status === 'OPEN' && (
                <Button
                  variant="outline"
                  size="md"
                  onClick={() => setIsLockConfirmOpen(true)}
                  leftIcon={<Lock className="w-4 h-4 text-amber-600" />}
                >
                  Khóa Sổ Chờ Quyết Toán
                </Button>
              )}

              {folio.status === 'LOCKED_FOR_SETTLEMENT' && (
                <>
                  <Button
                    variant="outline"
                    size="md"
                    onClick={async () => {
                      if (!confirm('Mở lại Sổ cái cho phép tiếp tục ghi nhận chi phí phát sinh?')) return;
                      await masterFolioService.updateStatus(folio.id, {
                        status: 'OPEN',
                        note: 'Mở lại sổ cái ghi nhận chi phí',
                      });
                      loadFolio();
                    }}
                    leftIcon={<Unlock className="w-4 h-4 text-neutral-600" />}
                  >
                    Mở lại Sổ cái
                  </Button>
                  <Button
                    variant="primary"
                    size="md"
                    onClick={() => setIsSettleConfirmOpen(true)}
                    leftIcon={<CheckCircle2 className="w-4 h-4" />}
                  >
                    Xác Nhận Quyết Toán & Xuất Hóa Đơn
                  </Button>
                </>
              )}

              <Button variant="secondary" size="md" onClick={onClose}>
                Đóng
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Routing Adjustment Modal */}
      {folio && (
        <RoutingAdjustmentModal
          isOpen={isAdjustmentOpen}
          onClose={() => setIsAdjustmentOpen(false)}
          masterFolioId={folio.id}
          item={adjustingItem}
          onSuccess={handleAdjustmentSuccess}
        />
      )}

      {/* Confirm Lock Modal */}
      <ConfirmModal
        isOpen={isLockConfirmOpen}
        onClose={() => setIsLockConfirmOpen(false)}
        onConfirm={handleConfirmLock}
        title="Khóa Sổ Cái Master Folio?"
        description="Khi đã khóa sổ, hệ thống sẽ chốt toàn bộ số liệu phát sinh để Lễ tân / Kế toán tiến hành quyết toán công nợ và xuất hóa đơn điện tử GTGT. Bạn có muốn tiếp tục?"
        confirmText="Xác nhận Khóa sổ"
        variant="warning"
        isLoading={isActionLoading}
      />

      {/* Confirm Settle Modal */}
      <ConfirmModal
        isOpen={isSettleConfirmOpen}
        onClose={() => setIsSettleConfirmOpen(false)}
        onConfirm={handleConfirmSettle}
        title="Xác Nhận Quyết Toán Đoàn & Đóng Sổ?"
        description={`Bạn đang xác nhận doanh nghiệp đối tác đã thanh toán đủ số dư nợ còn lại ${formatVND(
          folio?.balance
        )}. Hệ thống sẽ hoàn tất quyết toán hợp đồng đoàn.`}
        confirmText="Xác nhận Quyết toán"
        variant="primary"
        isLoading={isActionLoading}
      />
    </div>
  );
};

export default MasterFolioModal;
