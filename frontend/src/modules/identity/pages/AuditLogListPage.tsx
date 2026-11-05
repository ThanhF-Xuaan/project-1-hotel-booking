import React, { useState, useEffect, useCallback } from 'react';
import {
  Search,
  RefreshCw,
  Eye,
  Calendar,
  FileCode,
  ShieldAlert,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Modal from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import auditLogService from '../services/auditLog.service';
import type { AuditLogDto } from '../types/auditLog.types';

export const AuditLogListPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [actionType, setActionType] = useState('');
  const [entityName, setEntityName] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Detail Modal
  const [selectedLog, setSelectedLog] = useState<AuditLogDto | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await auditLogService.filter({
        page,
        pageSize,
        actionType: actionType || undefined,
        entityName: entityName.trim() || undefined,
        fromDate: fromDate ? `${fromDate}T00:00:00Z` : undefined,
        toDate: toDate ? `${toDate}T23:59:59Z` : undefined,
      });
      if (response?.result) {
        setLogs(response.result.content || []);
        setTotalElements(response.result.totalElements || 0);
        setTotalPages(response.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi khi tải nhật ký kiểm toán:', err);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, actionType, entityName, fromDate, toDate]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadData]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    void loadData();
  };

  const handleReset = () => {
    setActionType('');
    setEntityName('');
    setFromDate('');
    setToDate('');
    setPage(0);
  };

  const openDetailModal = (log: AuditLogDto) => {
    setSelectedLog(log);
    setIsDetailModalOpen(true);
  };

  const formatJson = (jsonStr?: string) => {
    if (!jsonStr) return 'Không có dữ liệu';
    try {
      const parsed = JSON.parse(jsonStr);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return jsonStr;
    }
  };

  const getActionBadge = (action: string) => {
    switch (action?.toUpperCase()) {
      case 'CREATE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
            CREATE
          </span>
        );
      case 'UPDATE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-100 text-blue-800">
            UPDATE
          </span>
        );
      case 'DELETE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-100 text-red-800">
            DELETE
          </span>
        );
      case 'STATUS_UPDATE':
      case 'STAFF_STATUS_UPDATE':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800">
            STATUS_CHANGE
          </span>
        );
      case 'PASSWORD_RESET':
      case 'STAFF_PASSWORD_RESET':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-100 text-purple-800">
            RESET_PASS
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-neutral-100 text-neutral-800">
            {action}
          </span>
        );
    }
  };

  return (
    <div className="space-y-4">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-neutral-900 tracking-tight">
          Nhật ký Kiểm toán Hệ thống (Audit Logs)
        </h1>
        <p className="text-sm text-neutral-500 mt-0.5">
          Theo dõi và truy vết toàn bộ thao tác thêm, sửa, xóa và thay đổi trạng thái của nhân sự trong hệ thống.
        </p>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-3 items-center">
          <div className="lg:col-span-3">
            <select
              className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
              value={actionType}
              onChange={(e) => setActionType(e.target.value)}
            >
              <option value="">-- Tất cả hành động --</option>
              <option value="CREATE">CREATE (Tạo mới)</option>
              <option value="UPDATE">UPDATE (Cập nhật)</option>
              <option value="DELETE">DELETE (Xóa)</option>
              <option value="STATUS_UPDATE">STATUS_UPDATE (Đổi trạng thái)</option>
              <option value="PASSWORD_RESET">PASSWORD_RESET (Đặt lại mật khẩu)</option>
              <option value="LOGIN">LOGIN (Đăng nhập)</option>
            </select>
          </div>

          <div className="lg:col-span-3">
            <Input
              placeholder="Thực thể (Staff, Booking, Role...)"
              value={entityName}
              onChange={(e) => setEntityName(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>

          <div className="lg:col-span-2">
            <Input
              type="date"
              placeholder="Từ ngày"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              leftIcon={<Calendar className="w-4 h-4" />}
            />
          </div>

          <div className="lg:col-span-2">
            <Input
              type="date"
              placeholder="Đến ngày"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              leftIcon={<Calendar className="w-4 h-4" />}
            />
          </div>

          <div className="lg:col-span-2 flex gap-2 justify-end">
            <Button
              type="submit"
              variant="primary"
              className="bg-red-600 hover:bg-red-700 text-white font-medium flex-1 h-10 md:h-12"
            >
              <Search className="w-4 h-4 mr-1.5" />
              Lọc
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleReset}
              className="h-10 md:h-12 px-3"
              title="Đặt lại bộ lọc"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </div>

      {/* HÀNG 2: TOOLBAR */}
      <div className="flex justify-between items-center text-xs text-neutral-500 font-medium px-1">
        <div className="flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 text-neutral-400" />
          <span>Dữ liệu kiểm toán được lưu trữ bất biến (Read-only) và không thể chỉnh sửa hay xóa.</span>
        </div>
        <div>
          Tổng số: <strong className="text-neutral-900 font-mono">{totalElements}</strong> sự kiện
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE */}
      <div className="bg-white rounded-2xl border border-neutral-200 overflow-hidden shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-neutral-50/80 border-b border-neutral-200 text-neutral-700 font-bold text-xs">
                <th className="py-3.5 px-4">Thời gian</th>
                <th className="py-3.5 px-4">Nhân sự thực hiện</th>
                <th className="py-3.5 px-4">Hành động</th>
                <th className="py-3.5 px-4">Thực thể (Entity)</th>
                <th className="py-3.5 px-4">Địa chỉ IP</th>
                <th className="py-3.5 px-4 text-right">Chi tiết</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
                    Đang tải nhật ký kiểm toán...
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-400">
                    Không có sự kiện kiểm toán nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-neutral-50/60 transition-colors">
                    <td className="py-3.5 px-4 text-xs font-mono text-neutral-700">
                      {log.createdAt ? new Date(log.createdAt).toLocaleString('vi-VN') : '—'}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-neutral-900">
                        {log.staffFullName || 'Hệ thống'}
                      </div>
                      {log.staffUsername && (
                        <div className="text-xs text-neutral-500 font-mono">@{log.staffUsername}</div>
                      )}
                    </td>
                    <td className="py-3.5 px-4">{getActionBadge(log.actionType)}</td>
                    <td className="py-3.5 px-4">
                      <span className="font-semibold text-neutral-800">{log.entityName}</span>
                      {log.entityId && (
                        <span className="ml-1.5 text-xs font-mono px-1.5 py-0.5 bg-neutral-100 rounded text-neutral-600">
                          #{log.entityId}
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-xs font-mono text-neutral-600">
                      {log.ipAddress || '—'}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        type="button"
                        onClick={() => openDetailModal(log)}
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold text-neutral-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                        title="Xem chi tiết dữ liệu thay đổi"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        Xem Diff
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Phân trang */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-neutral-100">
            <Pagination
              pageNumber={page}
              pageSize={pageSize}
              totalElements={totalElements}
              totalPages={totalPages}
              onPageChange={setPage}
            />
          </div>
        )}
      </div>

      {/* DETAIL DIFF MODAL */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        title={`Chi tiết Kiểm toán #${selectedLog?.id} — ${selectedLog?.actionType} [${selectedLog?.entityName} #${selectedLog?.entityId}]`}
        description={`Thực hiện lúc ${selectedLog?.createdAt ? new Date(selectedLog.createdAt).toLocaleString('vi-VN') : ''} bởi @${selectedLog?.staffUsername || 'System'}`}
        maxWidth="4xl"
      >
        <div className="space-y-4">
          {/* Metadata Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-3 bg-neutral-50 rounded-xl border border-neutral-200 text-xs">
            <div>
              <span className="text-neutral-500 block">Địa chỉ IP:</span>
              <strong className="font-mono text-neutral-900">{selectedLog?.ipAddress || 'N/A'}</strong>
            </div>
            <div className="sm:col-span-2">
              <span className="text-neutral-500 block">Trình duyệt / Thiết bị (User Agent):</span>
              <span className="font-mono text-[11px] text-neutral-800 truncate block">
                {selectedLog?.userAgent || 'N/A'}
              </span>
            </div>
          </div>

          {/* Side by side JSON Diff */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-700 mb-1.5">
                <FileCode className="w-4 h-4 text-amber-600" />
                <span>Dữ liệu trước thay đổi (Old Values)</span>
              </div>
              <pre className="p-3 bg-neutral-900 text-amber-300 text-xs font-mono rounded-xl overflow-x-auto max-h-72 leading-relaxed">
                {formatJson(selectedLog?.oldValues)}
              </pre>
            </div>

            <div>
              <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-700 mb-1.5">
                <FileCode className="w-4 h-4 text-emerald-600" />
                <span>Dữ liệu sau thay đổi (New Values)</span>
              </div>
              <pre className="p-3 bg-neutral-900 text-emerald-300 text-xs font-mono rounded-xl overflow-x-auto max-h-72 leading-relaxed">
                {formatJson(selectedLog?.newValues)}
              </pre>
            </div>
          </div>

          <div className="flex justify-end pt-3 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsDetailModalOpen(false)}
            >
              Đóng
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default AuditLogListPage;

