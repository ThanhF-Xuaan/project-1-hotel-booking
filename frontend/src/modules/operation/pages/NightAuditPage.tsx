import React, { useState, useEffect, useCallback } from 'react';
import {
  Moon,
  AlertOctagon,
  CheckCircle2,
  Calendar,
  Building2,
  BedDouble,
  DollarSign,
  Utensils,
  PlayCircle,
  FileText,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import { ConfirmModal } from '../../../core/components/ui/Modal';
import { operationService } from '../services/operation.service';
import type { NightAuditResponse } from '../types/operation.types';

export const NightAuditPage: React.FC = () => {
  const [hotelId, setHotelId] = useState<number>(1);
  const [auditDate, setAuditDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [auditSummary, setAuditSummary] = useState<NightAuditResponse | null>(null);
  const [isLoadingSummary, setIsLoadingSummary] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [executionResult, setExecutionResult] = useState<NightAuditResponse | null>(null);

  const fetchSummary = useCallback(async () => {
    setIsLoadingSummary(true);
    setExecutionResult(null);
    try {
      const data = await operationService.getAuditSummary(hotelId, auditDate);
      setAuditSummary(data);
    } catch (err) {
      console.error('Failed to load night audit summary:', err);
    } finally {
      setIsLoadingSummary(false);
    }
  }, [hotelId, auditDate]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  const handleExecuteNightAudit = async () => {
    setIsExecuting(true);
    try {
      const result = await operationService.executeNightAudit({
        hotelId,
        auditDate,
      });
      setExecutionResult(result);
      setAuditSummary(result);
      setIsConfirmModalOpen(false);
    } catch (err) {
      console.error('Execute night audit failed:', err);
    } finally {
      setIsExecuting(false);
    }
  };

  const formatVND = (amount: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <Moon className="w-7 h-7 text-red-600" />
            Quy trình Đóng Ngày & Đối Soát Đêm (Night Audit)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quy trình kết sổ cuối ngày, tự động xử lý đơn No-show và chốt doanh thu kế toán
          </p>
        </div>
      </div>

      {/* Date & Hotel Selector */}
      <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center gap-4">
        <div className="w-64">
          <label className="block text-xs font-semibold text-neutral-700 mb-1 flex items-center gap-1.5">
            <Building2 className="w-3.5 h-3.5 text-neutral-500" />
            Khách sạn cơ sở
          </label>
          <select
            value={hotelId}
            onChange={(e) => setHotelId(Number(e.target.value))}
            className="w-full h-12 px-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
          >
            <option value={1}>Khách sạn Grand Hà Nội</option>
            <option value={2}>Khách sạn Premier Đà Nẵng</option>
            <option value={3}>Khách sạn Boutique Sài Gòn</option>
          </select>
        </div>

        <div className="w-56">
          <Input
            label="Ngày đối soát (Audit Date)"
            type="date"
            value={auditDate}
            onChange={(e) => setAuditDate(e.target.value)}
            leftIcon={<Calendar className="w-4 h-4 text-neutral-400" />}
          />
        </div>

        <div className="flex items-end pt-5">
          <Button variant="secondary" onClick={fetchSummary} disabled={isLoadingSummary} className="h-12">
            {isLoadingSummary ? 'Đang tính toán...' : 'Xem trước số liệu'}
          </Button>
        </div>
      </div>

      {/* Success Notification upon execution */}
      {executionResult && (
        <div className="p-5 bg-emerald-50 border border-emerald-300 rounded-2xl text-emerald-900 shadow-xs space-y-2">
          <div className="flex items-center gap-2 font-bold text-base text-emerald-800">
            <CheckCircle2 className="w-6 h-6 text-emerald-600 shrink-0" />
            Quy trình Night Audit đã hoàn tất thành công!
          </div>
          <div className="text-sm pl-8 space-y-1">
            <div>
              • <strong>Số đơn No-show đã xử lý hủy & giải phóng phòng:</strong> {executionResult.noShowBookingsCount} đơn
            </div>
            <div>
              • <strong>Tổng doanh thu chốt sổ ngày {executionResult.auditDate}:</strong> {formatVND(executionResult.totalDailyRevenue)}
            </div>
            <div>
              • <strong>Thời điểm chốt sổ:</strong> {new Date(executionResult.auditTimestamp).toLocaleString('vi-VN')}
            </div>
          </div>
        </div>
      )}

      {/* Executive Financial Dashboard Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center gap-2 text-xs font-bold text-blue-600 uppercase tracking-wider">
            <BedDouble className="w-4 h-4" />
            Phòng đang có khách
          </div>
          <div className="text-3xl font-extrabold text-neutral-900 mt-2">
            {auditSummary?.occupiedRoomsCount ?? 0}
          </div>
          <div className="text-xs text-neutral-400 mt-1">Phòng đang có khách lưu trú</div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center gap-2 text-xs font-bold text-emerald-600 uppercase tracking-wider">
            <DollarSign className="w-4 h-4" />
            Doanh thu tiền phòng
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 mt-2">
            {formatVND(auditSummary?.totalDailyRoomRevenue ?? 0)}
          </div>
          <div className="text-xs text-neutral-400 mt-1">Tiền phòng tích lũy trong ngày</div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center gap-2 text-xs font-bold text-purple-600 uppercase tracking-wider">
            <Utensils className="w-4 h-4" />
            Doanh thu Dịch vụ & F&B
          </div>
          <div className="text-2xl font-extrabold text-neutral-900 mt-2">
            {formatVND(auditSummary?.totalDailyServiceRevenue ?? 0)}
          </div>
          <div className="text-xs text-neutral-400 mt-1">Minibar, F&B và dịch vụ phòng</div>
        </div>

        <div className="bg-white p-5 rounded-2xl border-2 border-red-500 shadow-xs bg-red-50/20">
          <div className="flex items-center gap-2 text-xs font-bold text-red-600 uppercase tracking-wider">
            <FileText className="w-4 h-4" />
            Tổng doanh thu trong ngày
          </div>
          <div className="text-2xl font-extrabold text-red-600 mt-2">
            {formatVND(auditSummary?.totalDailyRevenue ?? 0)}
          </div>
          <div className="text-xs text-neutral-500 mt-1">Chốt sổ kế toán ngày đối soát</div>
        </div>
      </div>

      {/* Audit Checklist & Danger Zone */}
      <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
        <h2 className="text-lg font-bold text-neutral-900 flex items-center gap-2">
          <AlertOctagon className="w-5 h-5 text-amber-500" />
          Nghiệp vụ tự động khi thực hiện Đóng Ngày
        </h2>

        <div className="space-y-3 text-sm text-neutral-600 bg-neutral-50 p-4 rounded-xl border border-neutral-200">
          <div className="flex items-start gap-2.5">
            <span className="font-bold text-red-600">1.</span>
            <span>
              <strong>Quét tự động No-Show:</strong> Mọi đơn đặt phòng có ngày nhận phòng trong hoặc trước ngày {auditDate} nhưng khách chưa tới check-in sẽ được chuyển sang trạng thái <code>NO_SHOW</code>.
            </span>
          </div>
          <div className="flex items-start gap-2.5">
            <span className="font-bold text-red-600">2.</span>
            <span>
              <strong>Giải phóng phòng trống:</strong> Toàn bộ các slot phòng vật lý được giữ cho khách No-show sẽ tự động được thu hồi và đưa về trạng thái <code>READY</code>.
            </span>
          </div>
          <div className="flex items-start gap-2.5">
            <span className="font-bold text-red-600">3.</span>
            <span>
              <strong>Đóng kỳ sổ sách kế toán:</strong> Lưu trữ bản ghi kiểm toán doanh thu hàng ngày cho ban giám đốc và bộ phận tài chính.
            </span>
          </div>
        </div>

        <div className="pt-2 flex justify-end">
          <Button
            onClick={() => setIsConfirmModalOpen(true)}
            disabled={isExecuting}
            className="flex items-center gap-2 px-6 py-3.5 text-base font-bold"
          >
            <PlayCircle className="w-5 h-5" />
            {isExecuting ? 'Đang thực hiện đóng ngày...' : 'Thực hiện Đóng Ngày (Night Audit)'}
          </Button>
        </div>
      </div>

      {/* Confirmation Modal */}
      <ConfirmModal
        isOpen={isConfirmModalOpen}
        onClose={() => setIsConfirmModalOpen(false)}
        onConfirm={handleExecuteNightAudit}
        title="Xác nhận Đóng Ngày & Khóa Sổ Doanh Thu"
        description={`Bạn có chắc chắn muốn thực hiện quy trình Night Audit cho ngày ${auditDate}? Hành động này sẽ tự động chuyển các đơn chưa check-in thành No-show và chốt sổ doanh thu ngày.`}
      />
    </div>
  );
};
