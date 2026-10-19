import React, { useState, useEffect, useCallback } from 'react';
import {
  Wrench,
  Filter,
  RotateCcw,
  Plus,
  RefreshCw,
  CheckCircle,
  XCircle,
  Calendar,
  AlertCircle,
  Info,
} from 'lucide-react';
import Button from '../../../../core/components/ui/Button';
import Pagination from '../../../../core/components/ui/Pagination';
import { ConfirmModal } from '../../../../core/components/ui/Modal';
import { operationService } from '../../services/operation.service';
import type {
  MaintenanceBlockDto,
  MaintenanceBlockSearchDto,
} from '../../types/operation.types';

export interface MaintenanceBlockListProps {
  hotelId: number;
  onOpenCreateModal: () => void;
  onError: (err: unknown) => void;
  onSuccess: (message: string) => void;
  refreshTrigger?: number;
}

export const MaintenanceBlockList: React.FC<MaintenanceBlockListProps> = ({
  hotelId,
  onOpenCreateModal,
  onError,
  onSuccess,
  refreshTrigger,
}) => {
  const [blocks, setBlocks] = useState<MaintenanceBlockDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [page, setPage] = useState<number>(0);
  const [pageSize] = useState<number>(10);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Filters
  const [filterBlockType, setFilterBlockType] = useState<string>('ALL');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');

  // Action Confirmation state
  const [confirmAction, setConfirmAction] = useState<{
    type: 'complete' | 'cancel';
    block: MaintenanceBlockDto;
  } | null>(null);
  const [actionLoading, setActionLoading] = useState<boolean>(false);

  const fetchBlocks = useCallback(async () => {
    setIsLoading(true);
    try {
      const searchDto: MaintenanceBlockSearchDto = {
        hotelId,
        blockType: filterBlockType === 'ALL' ? undefined : filterBlockType,
        status: filterStatus === 'ALL' ? undefined : filterStatus,
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        page,
        pageSize,
      };

      const res = await operationService.filterMaintenanceBlocks(searchDto);
      setBlocks(res.data || []);
      setTotalPages(res.totalPages || 0);
      setTotalElements(res.totalElements || 0);
    } catch (err) {
      onError(err);
    } finally {
      setIsLoading(false);
    }
  }, [hotelId, filterBlockType, filterStatus, fromDate, toDate, page, pageSize, onError]);

  useEffect(() => {
    fetchBlocks();
  }, [fetchBlocks, refreshTrigger]);

  const handleResetFilters = () => {
    setFilterBlockType('ALL');
    setFilterStatus('ALL');
    setFromDate('');
    setToDate('');
    setPage(0);
  };

  const handleConfirmAction = async () => {
    if (!confirmAction) return;
    setActionLoading(true);
    try {
      if (confirmAction.type === 'complete') {
        await operationService.completeMaintenanceBlock(confirmAction.block.id);
        onSuccess(`Đã hoàn tất bảo trì cho phòng P.${confirmAction.block.roomNumber}.`);
      } else {
        await operationService.cancelMaintenanceBlock(confirmAction.block.id);
        onSuccess(`Đã hủy lệnh khóa phòng P.${confirmAction.block.roomNumber}.`);
      }
      setConfirmAction(null);
      fetchBlocks();
    } catch (err) {
      onError(err);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      {/* ────────────────────────────────────────────────────────
          HÀNG 1: SEARCH & FILTER BAR
          ──────────────────────────────────────────────────────── */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          {/* Block Type */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs font-semibold text-neutral-600">Loại:</label>
            <select
              value={filterBlockType}
              onChange={(e) => {
                setFilterBlockType(e.target.value);
                setPage(0);
              }}
              className="h-9 px-3 border border-neutral-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
            >
              <option value="ALL">Tất cả loại</option>
              <option value="OOO">OOO (Out of Order)</option>
              <option value="OOS">OOS (Out of Service)</option>
            </select>
          </div>

          {/* Status */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs font-semibold text-neutral-600">Trạng thái:</label>
            <select
              value={filterStatus}
              onChange={(e) => {
                setFilterStatus(e.target.value);
                setPage(0);
              }}
              className="h-9 px-3 border border-neutral-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang khóa (ACTIVE)</option>
              <option value="COMPLETED">Đã hoàn tất (COMPLETED)</option>
              <option value="CANCELLED">Đã hủy (CANCELLED)</option>
            </select>
          </div>

          {/* Date Range */}
          <div className="flex items-center gap-1.5 text-xs text-neutral-600">
            <span className="font-semibold">Từ:</span>
            <input
              type="date"
              value={fromDate}
              onChange={(e) => {
                setFromDate(e.target.value);
                setPage(0);
              }}
              className="h-9 px-2.5 border border-neutral-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
            />
            <span className="font-semibold">Đến:</span>
            <input
              type="date"
              value={toDate}
              onChange={(e) => {
                setToDate(e.target.value);
                setPage(0);
              }}
              className="h-9 px-2.5 border border-neutral-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
            />
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handleResetFilters}
            className="h-9 text-xs flex items-center gap-1"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Đặt lại
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={fetchBlocks}
            className="h-9 text-xs flex items-center gap-1"
          >
            <Filter className="w-3.5 h-3.5" />
            Lọc kết quả
          </Button>
        </div>
      </div>

      {/* ────────────────────────────────────────────────────────
          HÀNG 2: ACTION TOOLBAR
          ──────────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Button
            variant="primary"
            size="md"
            onClick={onOpenCreateModal}
            className="bg-red-600 hover:bg-red-700 text-white font-semibold text-xs md:text-sm h-10 px-4 flex items-center gap-1.5 shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Tạo lệnh khóa phòng mới
          </Button>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchBlocks}
            disabled={isLoading}
            className="h-9 text-xs flex items-center gap-1.5"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>
      </div>

      {/* ────────────────────────────────────────────────────────
          HÀNG 3: DATA TABLE & PAGINATION
          ──────────────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        {isLoading ? (
          <div className="py-20 text-center text-neutral-400 text-sm">
            Đang tải danh sách khóa bảo trì...
          </div>
        ) : blocks.length === 0 ? (
          <div className="py-20 text-center text-neutral-400 text-sm">
            Không tìm thấy lệnh khóa bảo trì nào phù hợp.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs md:text-sm">
              <thead>
                <tr className="bg-neutral-50/80 border-b border-neutral-200 text-neutral-700 font-semibold text-xs uppercase tracking-wider">
                  <th className="py-3.5 px-4">Mã</th>
                  <th className="py-3.5 px-4">Phòng</th>
                  <th className="py-3.5 px-4">Loại khóa</th>
                  <th className="py-3.5 px-4">Thời gian hiệu lực</th>
                  <th className="py-3.5 px-4">Lý do & Ghi chú</th>
                  <th className="py-3.5 px-4">Trạng thái</th>
                  <th className="py-3.5 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {blocks.map((b) => {
                  const isActive = b.status === 'ACTIVE';
                  const isCompleted = b.status === 'COMPLETED';
                  const isCancelled = b.status === 'CANCELLED';

                  return (
                    <tr
                      key={b.id}
                      className="hover:bg-neutral-50/60 transition-colors text-neutral-800"
                    >
                      <td className="py-3.5 px-4 font-mono text-neutral-500 font-semibold">
                        #{b.id}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-neutral-900 text-sm">P.{b.roomNumber}</div>
                        <div className="text-[11px] text-neutral-500">
                          {b.roomTypeName || 'Tiêu chuẩn'}
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        {b.blockType === 'OOO' ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-red-50 text-red-700 border border-red-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-red-600" />
                            OOO (Khóa bán)
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-50 text-amber-800 border border-amber-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-amber-600" />
                            OOS (Nội bộ)
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-1 font-medium text-neutral-900">
                          <Calendar className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
                          <span>
                            {b.startDate} → {b.endDate}
                          </span>
                        </div>
                        {b.createdAt && (
                          <div className="text-[11px] text-neutral-400 mt-0.5">
                            Tạo: {b.createdAt.substring(0, 16).replace('T', ' ')}
                          </div>
                        )}
                      </td>
                      <td className="py-3.5 px-4 max-w-xs">
                        <div className="font-medium text-neutral-900 truncate" title={b.reason}>
                          {b.reason}
                        </div>
                        {b.notes && (
                          <div
                            className="text-[11px] text-neutral-500 italic truncate mt-0.5"
                            title={b.notes}
                          >
                            {b.notes}
                          </div>
                        )}
                      </td>
                      <td className="py-3.5 px-4">
                        {isActive && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle className="w-3 h-3 text-emerald-600" />
                            Đang hiệu lực
                          </span>
                        )}
                        {isCompleted && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                            <Info className="w-3 h-3 text-blue-600" />
                            Đã hoàn tất
                          </span>
                        )}
                        {isCancelled && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-neutral-100 text-neutral-600 border border-neutral-300">
                            <XCircle className="w-3 h-3 text-neutral-500" />
                            Đã hủy
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        {isActive ? (
                          <div className="flex items-center justify-end gap-1.5">
                            <button
                              onClick={() => setConfirmAction({ type: 'complete', block: b })}
                              className="px-2.5 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-semibold rounded-lg text-xs transition-colors cursor-pointer border border-emerald-200"
                              title="Hoàn tất bảo trì & mở khóa phòng"
                            >
                              Hoàn tất
                            </button>
                            <button
                              onClick={() => setConfirmAction({ type: 'cancel', block: b })}
                              className="px-2.5 py-1 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 font-semibold rounded-lg text-xs transition-colors cursor-pointer border border-neutral-200"
                              title="Hủy bỏ lệnh khóa"
                            >
                              Hủy bỏ
                            </button>
                          </div>
                        ) : (
                          <span className="text-xs text-neutral-400 italic">Đã đóng</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        <Pagination
          pageNumber={page}
          pageSize={pageSize}
          totalElements={totalElements}
          totalPages={totalPages}
          onPageChange={(newPage) => setPage(newPage)}
        />
      </div>

      {/* Confirmation Modal */}
      {confirmAction && (
        <ConfirmModal
          isOpen={!!confirmAction}
          onClose={() => setConfirmAction(null)}
          onConfirm={handleConfirmAction}
          isLoading={actionLoading}
          variant={confirmAction.type === 'complete' ? 'primary' : 'danger'}
          title={
            confirmAction.type === 'complete'
              ? 'Xác nhận hoàn tất bảo trì?'
              : 'Xác nhận hủy lệnh khóa bảo trì?'
          }
          description={
            confirmAction.type === 'complete'
              ? `Bạn có chắc muốn hoàn tất lệnh bảo trì cho phòng P.${confirmAction.block.roomNumber}? Hệ thống sẽ giải phóng slot và tính lại trạng thái buồng phòng.`
              : `Bạn có chắc muốn hủy bỏ lệnh khóa cho phòng P.${confirmAction.block.roomNumber}? Lệnh này sẽ chuyển sang CANCELLED và phòng sẽ được phục hồi trạng thái.`
          }
          confirmText={confirmAction.type === 'complete' ? 'Hoàn tất bảo trì' : 'Hủy bỏ lệnh khóa'}
          cancelText="Đóng"
        />
      )}
    </div>
  );
};

export default MaintenanceBlockList;
