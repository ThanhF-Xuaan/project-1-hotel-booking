import React, { useState, useEffect, useCallback } from 'react'
import { RefreshCw, Search, Eye, User, ShieldCheck, Globe } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Modal from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import { auditLogService } from '../services/identity.service'
import type { AuditLogDto } from '../types/identity.types'

export const AuditLogPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(15)

  // Filters
  const [actionTypeFilter, setActionTypeFilter] = useState('')
  const [entityNameFilter, setEntityNameFilter] = useState('')
  const [actorFilter, setActorFilter] = useState('')
  const [fromDateFilter, setFromDateFilter] = useState('')
  const [toDateFilter, setToDateFilter] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  // Modal Detail Diff
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false)
  const [selectedLog, setSelectedLog] = useState<AuditLogDto | null>(null)

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const res = await auditLogService.filter({
        page,
        pageSize,
        actionType: actionTypeFilter || undefined,
        entityName: entityNameFilter.trim() || undefined,
        actorUsername: actorFilter.trim() || undefined,
        fromDate: fromDateFilter || undefined,
        toDate: toDateFilter || undefined,
      })
      if (res.result) {
        setLogs(res.result.content || [])
        setTotalElements(res.result.totalElements || 0)
        setTotalPages(res.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải nhật ký kiểm toán:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, actionTypeFilter, entityNameFilter, actorFilter, fromDateFilter, toDateFilter])

  useEffect(() => {
    const timer = setTimeout(() => { void loadData() }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadData()
  }

  const handleResetFilter = () => {
    setActionTypeFilter('')
    setEntityNameFilter('')
    setActorFilter('')
    setFromDateFilter('')
    setToDateFilter('')
    setPage(0)
  }

  const handleOpenDetail = (log: AuditLogDto) => {
    setSelectedLog(log)
    setIsDetailModalOpen(true)
  }

  const formatJson = (raw?: string) => {
    if (!raw) return 'Không có dữ liệu'
    try {
      const parsed = JSON.parse(raw)
      return JSON.stringify(parsed, null, 2)
    } catch {
      return raw
    }
  }

  const getActionBadge = (action: string) => {
    const act = action.toUpperCase()
    if (act.includes('CREATE') || act.includes('INSERT')) {
      return (
        <span className="inline-block text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
          {action}
        </span>
      )
    }
    if (act.includes('UPDATE') || act.includes('MODIFY') || act.includes('STATUS')) {
      return (
        <span className="inline-block text-xs font-semibold px-2.5 py-0.5 rounded-full bg-blue-50 text-blue-700 border border-blue-200">
          {action}
        </span>
      )
    }
    if (act.includes('DELETE') || act.includes('REMOVE')) {
      return (
        <span className="inline-block text-xs font-semibold px-2.5 py-0.5 rounded-full bg-red-50 text-red-700 border border-red-200">
          {action}
        </span>
      )
    }
    return (
      <span className="inline-block text-xs font-semibold px-2.5 py-0.5 rounded-full bg-neutral-100 text-neutral-700 border border-neutral-200">
        {action}
      </span>
    )
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <ShieldCheck className="w-7 h-7 text-red-600" />
            Nhật ký Kiểm toán (Audit Logs)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Ghi nhận và truy vết toàn bộ biến động dữ liệu, phiên đăng nhập và thao tác bảo mật của nhân sự
          </p>
        </div>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 sm:p-5 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3 sm:gap-4 items-end">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Loại hành động
            </label>
            <select
              value={actionTypeFilter}
              onChange={(e) => setActionTypeFilter(e.target.value)}
              className="w-full h-10 sm:h-12 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
            >
              <option value="">Tất cả hành động</option>
              <option value="CREATE">CREATE (Tạo mới)</option>
              <option value="UPDATE">UPDATE (Cập nhật)</option>
              <option value="DELETE">DELETE (Xóa dữ liệu)</option>
              <option value="STATUS_CHANGE">STATUS_CHANGE (Đổi trạng thái)</option>
              <option value="LOGIN">LOGIN (Đăng nhập)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Tên thực thể (Entity)
            </label>
            <input
              type="text"
              value={entityNameFilter}
              onChange={(e) => setEntityNameFilter(e.target.value)}
              placeholder="VD: STAFF, ROLE, HOTEL..."
              className="w-full h-10 sm:h-12 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Người thực hiện
            </label>
            <div className="relative">
              <User className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
              <input
                type="text"
                value={actorFilter}
                onChange={(e) => setActorFilter(e.target.value)}
                placeholder="VD: admin..."
                className="w-full h-10 sm:h-12 pl-10 pr-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Từ ngày
            </label>
            <input
              type="date"
              value={fromDateFilter}
              onChange={(e) => setFromDateFilter(e.target.value)}
              className="w-full h-10 sm:h-12 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Đến ngày
            </label>
            <input
              type="date"
              value={toDateFilter}
              onChange={(e) => setToDateFilter(e.target.value)}
              className="w-full h-10 sm:h-12 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
            />
          </div>

          <div className="flex items-center gap-2">
            <Button
              type="submit"
              variant="primary"
              className="flex-1 h-10 sm:h-12 rounded-xl flex items-center justify-center gap-2"
              disabled={isLoading}
            >
              <Search className="w-4 h-4" />
              <span>Tìm kiếm</span>
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleResetFilter}
              className="h-10 sm:h-12 px-4 rounded-xl flex items-center justify-center gap-1.5 text-neutral-600 hover:text-neutral-900"
              title="Đặt lại bộ lọc"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </div>

      {/* HÀNG 2: ACTION TOOLBAR */}
      <div className="flex items-center justify-between bg-white px-4 sm:px-5 py-3 rounded-2xl border border-neutral-200/80 shadow-xs">
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={loadData}
            className="h-10 sm:h-12 px-4 rounded-xl flex items-center gap-2 text-sm font-semibold"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-red-600' : ''}`} />
            <span>Làm mới nhật ký</span>
          </Button>
        </div>
        <div className="text-xs text-neutral-500 font-medium">
          Tổng cộng: <span className="font-bold text-neutral-900">{totalElements}</span> bản ghi kiểm toán
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE & PAGINATION */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-neutral-700">
            <thead className="bg-neutral-50/80 border-b border-neutral-200 text-xs uppercase font-bold text-neutral-600">
              <tr>
                <th className="p-4">Thời gian</th>
                <th className="p-4">Người thực hiện</th>
                <th className="p-4">Hành động</th>
                <th className="p-4">Thực thể (Entity)</th>
                <th className="p-4">IP & Thiết bị</th>
                <th className="p-4 text-right">Chi tiết</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-12 text-center">
                    <div className="inline-flex items-center gap-2 text-sm text-neutral-500">
                      <RefreshCw className="w-4 h-4 animate-spin text-red-600" />
                      <span>Đang tải nhật ký kiểm toán...</span>
                    </div>
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-12 text-center text-neutral-400">
                    Không tìm thấy bản ghi kiểm toán nào phù hợp
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-neutral-50/60 transition-colors">
                    <td className="p-4 whitespace-nowrap">
                      <div className="text-xs font-semibold text-neutral-900">
                        {log.createdAt ? new Date(log.createdAt).toLocaleString('vi-VN') : '---'}
                      </div>
                    </td>
                    <td className="p-4">
                      <div className="font-semibold text-neutral-900 text-xs">
                        {log.staffUsername || 'system'}
                      </div>
                      {log.staffFullName && (
                        <div className="text-[11px] text-neutral-400">
                          {log.staffFullName}
                        </div>
                      )}
                    </td>
                    <td className="p-4">
                      {getActionBadge(log.actionType)}
                    </td>
                    <td className="p-4">
                      <div className="text-xs font-mono font-semibold text-neutral-800">
                        {log.entityName}
                      </div>
                      {log.entityId && (
                        <div className="text-[10px] text-neutral-500 font-mono">
                          ID: #{log.entityId}
                        </div>
                      )}
                    </td>
                    <td className="p-4">
                      <div className="flex items-center gap-1.5 text-xs text-neutral-600 font-mono">
                        <Globe className="w-3.5 h-3.5 text-neutral-400" />
                        <span>{log.ipAddress || '127.0.0.1'}</span>
                      </div>
                      {log.userAgent && (
                        <div className="text-[10px] text-neutral-400 max-w-xs truncate mt-0.5">
                          {log.userAgent}
                        </div>
                      )}
                    </td>
                    <td className="p-4 text-right">
                      <button
                        type="button"
                        onClick={() => handleOpenDetail(log)}
                        className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-semibold text-neutral-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors border border-neutral-200"
                        title="Xem biến động chi tiết"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>Xem diff</span>
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Phân trang */}
        {totalPages > 0 && (
          <Pagination
            pageNumber={page}
            pageSize={pageSize}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={(p) => setPage(p)}
          />
        )}
      </div>

      {/* MODAL: AUDIT DETAIL DIFF */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        title={`Chi tiết biến động kiểm toán #${selectedLog?.id || ''}`}
        maxWidth="4xl"
      >
        <div className="space-y-4">
          {/* Metadata banner */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-3.5 bg-neutral-50 rounded-xl border border-neutral-200 text-xs">
            <div>
              <span className="text-neutral-400 block">Thực thể:</span>
              <span className="font-mono font-bold text-neutral-900">
                {selectedLog?.entityName} #{selectedLog?.entityId}
              </span>
            </div>
            <div>
              <span className="text-neutral-400 block">Hành động:</span>
              <span className="font-semibold text-neutral-900">{selectedLog?.actionType}</span>
            </div>
            <div>
              <span className="text-neutral-400 block">Người thực hiện:</span>
              <span className="font-semibold text-neutral-900">{selectedLog?.staffUsername || 'system'}</span>
            </div>
            <div>
              <span className="text-neutral-400 block">Địa chỉ IP:</span>
              <span className="font-mono text-neutral-900">{selectedLog?.ipAddress || '127.0.0.1'}</span>
            </div>
          </div>

          {/* Side by side JSON Diff */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <div className="text-xs font-bold text-neutral-700 uppercase mb-1.5 flex items-center justify-between">
                <span>Dữ liệu trước thay đổi (Old Values)</span>
              </div>
              <pre className="p-3 bg-neutral-900 text-neutral-200 rounded-xl text-xs font-mono overflow-x-auto max-h-80 border border-neutral-800">
                {formatJson(selectedLog?.oldValues)}
              </pre>
            </div>

            <div>
              <div className="text-xs font-bold text-neutral-700 uppercase mb-1.5 flex items-center justify-between">
                <span>Dữ liệu sau thay đổi (New Values)</span>
              </div>
              <pre className="p-3 bg-neutral-900 text-emerald-400 rounded-xl text-xs font-mono overflow-x-auto max-h-80 border border-neutral-800">
                {formatJson(selectedLog?.newValues)}
              </pre>
            </div>
          </div>

          <div className="flex justify-end pt-3 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsDetailModalOpen(false)}
            >
              Đóng
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

export default AuditLogPage
