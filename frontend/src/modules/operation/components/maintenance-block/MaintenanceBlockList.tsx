import React, { useState, useEffect, useCallback } from 'react';
import {
  RotateCcw,
  Plus,
  RefreshCw,
  CheckCircle,
  XCircle,
  Calendar,
  Info,
  CalendarClock,
} from 'lucide-react';
import Button from '../../../../core/components/ui/Button';
import Select from '../../../../core/components/ui/Select';
import Pagination from '../../../../core/components/ui/Pagination';
import { ConfirmModal } from '../../../../core/components/ui/Modal';
import { operationService } from '../../services/operation.service';
import MaintenanceBlockEditModal from './MaintenanceBlockEditModal';
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

  // Edit / Extend block state
  const [editingBlock, setEditingBlock] = useState<MaintenanceBlockDto | null>(null);

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
      setBlocks(res.content || (res as unknown as { data?: MaintenanceBlockDto[] }).data || []);
      setTotalPages(res.totalPages || 0);
      setTotalElements(res.totalElements || 0);
    } catch (err) {
      onError(err);
    } finally {
      setIsLoading(false);
    }
  }, [hotelId, filterBlockType, filterStatus, fromDate, toDate, page, pageSize, onError]);

  useEffect(() => {
    let ignore = false;
    const run = async () => {
      await Promise.resolve();
      if (!ignore) {
        fetchBlocks();
      }
    };
    run();
    return () => {
      ignore = true;
    };
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
    <div className="space-y-3.5">
      {/* ────────────────────────────────────────────────────────
          HÀNG 1: INTEGRATED ACTION & FILTER TOOLBAR
          ──────────────────────────────────────────────────────── */}
      <div className="bg-white p-3.5 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center justify-between gap-3">
        {/* Left: Primary CTA */}
        <div className="flex items-center gap-2">
          <Button
            variant="primary"
            size="sm"
            onClick={onOpenCreateModal}
            className="bg-red-600 hover:bg-red-700 text-white font-semibold text-xs h-9 px-3.5 flex items-center gap-1.5 shadow-sm shrink-0"
          >
            <Plus className="w-4 h-4" />
            Tạo lệnh khóa phòng mới
          </Button>
        </div>

        {/* Right: Filters & Utilities */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Block Type */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs font-semibold text-neutral-600 whitespace-nowrap">Loại:</label>
            <div className="w-36">
              <Select<string>
                value={filterBlockType}
                onChange={(val) => {
                  setFilterBlockType(val);
                  setPage(0);
                }}
                options={[
                  { value: 'ALL', label: 'Tất cả loại' },
                  { value: 'OOO', label: 'OOO (Khóa bán)' },
                  { value: 'OOS', label: 'OOS (Nội bộ)' },
                ]}
                size="sm"
              />
            </div>
          </div>

          {/* Status */}
          <div className="flex items-center gap-1.5">
            <label className="text-xs font-semibold text-neutral-600 whitespace-nowrap">Trạng thái:</label>
            <div className="w-40">
              <Select<string>
                value={filterStatus}
                onChange={(val) => {
                  setFilterStatus(val);
                  setPage(0);
                }}
                options={[
                  { value: 'ALL', label: 'Tất cả trạng thái' },
                  { value: 'ACTIVE', label: 'Đang hiệu lực' },
                  { value: 'COMPLETED', label: 'Đã hoàn tất' },
                  { value: 'CANCELLED', label: 'Đã hủy' },
                ]}
                size="sm"
              />
            </div>
          </div>

          {/* Date Range */}
          <div className="flex items-center gap-1 text-xs text-neutral-600">
            <span className="font-semibold text-neutral-500">Từ:</span>
            <input
              type="date"
              value={fromDate}
              onChange={(e) => {
                setFromDate(e.target.value);
                setPage(0);
              }}
              className="h-9 px-2 border border-neutral-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white text-neutral-900"
            />
            <span className="font-semibold text-neutral-500">Đến:</span>
            <input
              type="date"
              value={toDate}
              onChange={(e) => {
                setToDate(e.target.value);
                setPage(0);
              }}
              className="h-9 px-2 border border-neutral-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-red-600 bg-white text-neutral-900"
            />
          </div>

          {/* Reset & Refresh Buttons */}
          <div className="flex items-center gap-1.5">
            <Button
              variant="outline"
              size="sm"
              onClick={handleResetFilters}
              className="h-9 px-2.5 text-xs flex items-center gap-1"
              title="Đặt lại bộ lọc"
            >
              <RotateCcw className="w-3.5 h-3.5 text-neutral-500" />
              Đặt lại
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={fetchBlocks}
              disabled={isLoading}
              className="h-9 px-2.5 text-xs flex items-center gap-1"
              title="Làm mới danh sách"
            >
              <RefreshCw className={`w-3.5 h-3.5 text-neutral-600 ${isLoading ? 'animate-spin' : ''}`} />
              Làm mới
            </Button>
          </div>
        </div>
      </div>

      {/* ────────────────────────────────────────────────────────
          HÀNG 2: DATA TABLE & PAGINATION
          ──────────────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        {isLoading ? (
          <div className="py-16 text-center text-neutral-400 text-sm">
            Đang tải danh sách khóa bảo trì...
          </div>
        ) : blocks.length === 0 ? (
          <div className="py-16 text-center text-neutral-400 text-sm">
            Không tìm thấy lệnh khóa bảo trì nào phù hợp.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-neutral-50/80 border-b border-neutral-200 text-neutral-700 font-semibold text-[11px] uppercase tracking-wider">
                  <th className="py-2.5 px-3 w-14 text-center whitespace-nowrap">Mã</th>
                  <th className="py-2.5 px-3 w-36 whitespace-nowrap">Phòng</th>
                  <th className="py-2.5 px-3 w-32 whitespace-nowrap">Loại khóa</th>
                  <th className="py-2.5 px-3 w-44 whitespace-nowrap">Thời gian hiệu lực</th>
                  <th className="py-2.5 px-3 min-w-[140px] whitespace-nowrap">Lý do & Ghi chú</th>
                  <th className="py-2.5 px-3 w-32 whitespace-nowrap">Trạng thái</th>
                  <th className="py-2.5 px-3 w-56 text-center whitespace-nowrap">Thao tác</th>
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
                      <td className="py-2.5 px-3 text-center font-mono text-neutral-500 font-semibold whitespace-nowrap">
                        #{b.id}
                      </td>
                      <td className="py-2.5 px-3 whitespace-nowrap">
                        <div className="font-bold text-neutral-900 text-xs sm:text-sm">P.{b.roomNumber}</div>
                        <div className="text-[11px] text-neutral-500 truncate" title={b.roomTypeName || 'Tiêu chuẩn'}>
                          {b.roomTypeName || 'Tiêu chuẩn'}
                        </div>
                      </td>
                      <td className="py-2.5 px-3 whitespace-nowrap">
                        {b.blockType === 'OOO' ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-red-50 text-red-700 border border-red-200 whitespace-nowrap">
                            <span className="w-1.5 h-1.5 rounded-full bg-red-600" />
                            OOO (Khóa bán)
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-800 border border-amber-200 whitespace-nowrap">
                            <span className="w-1.5 h-1.5 rounded-full bg-amber-600" />
                            OOS (Nội bộ)
                          </span>
                        )}
                      </td>
                      <td className="py-2.5 px-3 whitespace-nowrap">
                        <div className="flex items-center gap-1 font-medium text-neutral-900 text-xs">
                          <Calendar className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
                          <span>
                            {b.startDate} → {b.endDate}
                          </span>
                        </div>
                        {b.createdAt && (
                          <div className="text-[10px] text-neutral-400 mt-0.5">
                            Tạo: {b.createdAt.substring(0, 16).replace('T', ' ')}
                          </div>
                        )}
                      </td>
                      <td className="py-2.5 px-3 max-w-[220px]">
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
                      <td className="py-2.5 px-3 whitespace-nowrap">
                        {isActive && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 whitespace-nowrap">
                            <CheckCircle className="w-3 h-3 text-emerald-600" />
                            Đang hiệu lực
                          </span>
                        )}
                        {isCompleted && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200 whitespace-nowrap">
                            <Info className="w-3 h-3 text-blue-600" />
                            Đã hoàn tất
                          </span>
                        )}
                        {isCancelled && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-neutral-100 text-neutral-600 border border-neutral-300 whitespace-nowrap">
                            <XCircle className="w-3 h-3 text-neutral-500" />
                            Đã hủy
                          </span>
                        )}
                      </td>
                      <td className="py-2.5 px-3 text-center whitespace-nowrap">
                        {isActive ? (
                          <div className="flex items-center justify-center gap-1.5">
                            <button
                              onClick={() => setEditingBlock(b)}
                              className="px-2 py-1 bg-blue-50 hover:bg-blue-100 text-blue-700 font-semibold rounded-md text-[11px] transition-colors cursor-pointer border border-blue-200 flex items-center gap-1 shrink-0"
                              title="Gia hạn thời gian hoặc chỉnh sửa ngày khóa"
                            >
                              <CalendarClock className="w-3 h-3" />
                              Gia hạn
                            </button>
                            <button
                              onClick={() => setConfirmAction({ type: 'complete', block: b })}
                              className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-semibold rounded-md text-[11px] transition-colors cursor-pointer border border-emerald-200 flex items-center gap-1 shrink-0"
                              title="Hoàn tất bảo trì & mở khóa phòng"
                            >
                              <CheckCircle className="w-3 h-3" />
                              Hoàn tất
                            </button>
                            <button
                              onClick={() => setConfirmAction({ type: 'cancel', block: b })}
                              className="px-2 py-1 bg-neutral-100 hover:bg-neutral-200 text-neutral-600 font-semibold rounded-md text-[11px] transition-colors cursor-pointer border border-neutral-200 flex items-center gap-1 shrink-0"
                              title="Hủy bỏ lệnh khóa"
                            >
                              <XCircle className="w-3 h-3" />
                              Hủy
                            </button>
                          </div>
                        ) : (
                          <span className="text-xs text-neutral-400 italic inline-block text-center">Đã đóng</span>
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

      {/* Edit / Extend Modal */}
      <MaintenanceBlockEditModal
        isOpen={!!editingBlock}
        block={editingBlock}
        onClose={() => setEditingBlock(null)}
        onError={onError}
        onSuccess={(msg) => {
          onSuccess(msg);
          fetchBlocks();
        }}
      />

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
