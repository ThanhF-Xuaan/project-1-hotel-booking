import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Calendar, CalendarDays } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import holidayService from '../services/holiday.service'
import type {
  HolidayCalendarDto,
  HolidayCalendarCreateRequest,
  HolidayCalendarUpdateRequest,
} from '../types/pricing.types'

export const HolidayCalendarListPage: React.FC = () => {
  const currentYear = new Date().getFullYear()

  const [holidays, setHolidays] = useState<HolidayCalendarDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [year, setYear] = useState<number | ''>(currentYear)
  const [month, setMonth] = useState<number | ''>('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingHoliday, setEditingHoliday] = useState<HolidayCalendarDto | null>(null)
  const [formName, setFormName] = useState('')
  const [formDate, setFormDate] = useState('')
  const [formDescription, setFormDescription] = useState('')
  const [formStatus, setFormStatus] = useState('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const res = await holidayService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        year: year ? Number(year) : undefined,
        month: month ? Number(month) : undefined,
        status: status || undefined,
        sortBy: 'date',
        sortDirection: 'ASC',
      })
      if (res.result) {
        setHolidays(res.result.content || [])
        setTotalElements(res.result.totalElements)
        setTotalPages(res.result.totalPages)
      }
    } catch (err) {
      console.error('Lỗi tải danh mục ngày lễ tết:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, year, month, status])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadData()
  }

  const handleReset = () => {
    setKeyword('')
    setYear(currentYear)
    setMonth('')
    setStatus('')
    setPage(0)
  }

  const handleOpenCreateModal = () => {
    setEditingHoliday(null)
    setFormName('')
    setFormDate(new Date().toISOString().split('T')[0])
    setFormDescription('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleOpenEditModal = (h: HolidayCalendarDto) => {
    setEditingHoliday(h)
    setFormName(h.name)
    setFormDate(h.date)
    setFormDescription(h.description || '')
    setFormStatus(h.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!formName.trim()) {
      setFormError('Tên ngày lễ tết không được để trống')
      return
    }
    if (!formDate) {
      setFormError('Ngày diễn ra không được để trống')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingHoliday) {
        const updatePayload: HolidayCalendarUpdateRequest = {
          name: formName.trim(),
          date: formDate,
          description: formDescription.trim() || undefined,
          status: formStatus,
        }
        await holidayService.update(editingHoliday.id, updatePayload)
      } else {
        const createPayload: HolidayCalendarCreateRequest = {
          name: formName.trim(),
          date: formDate,
          description: formDescription.trim() || undefined,
        }
        await holidayService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const axiosError = err as {
        response?: {
          data?: {
            message?: string
          }
        }
        message?: string
      }
      setFormError(axiosError.response?.data?.message || axiosError.message || 'Thao tác thất bại')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(holidays.map((h) => h.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  const handleDeleteSelected = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteConfirmOpen(true)
  }

  const handleDeleteOne = (id: number) => {
    setDeletingIds([id])
    setDeleteConfirmOpen(true)
  }

  const handleConfirmDelete = async () => {
    setIsDeleting(true)
    try {
      await holidayService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds([])
      loadData()
    } catch (err: unknown) {
      const axiosError = err as {
        response?: {
          data?: {
            message?: string
          }
        }
        message?: string
      }
      alert(axiosError.response?.data?.message || axiosError.message || 'Lỗi khi xóa ngày lễ tết')
    } finally {
      setIsDeleting(false)
    }
  }

  const formatDateDisplay = (dateStr: string) => {
    if (!dateStr) return '-'
    try {
      const parts = dateStr.split('-')
      if (parts.length === 3) {
        return `${parts[2]}/${parts[1]}/${parts[0]}`
      }
      return dateStr
    } catch {
      return dateStr
    }
  }

  const getDayOfWeek = (dateStr: string) => {
    if (!dateStr) return ''
    try {
      const d = new Date(dateStr)
      const day = d.getDay()
      const days = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
      return days[day]
    } catch {
      return ''
    }
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <Calendar className="w-7 h-7 text-red-600" />
            Lịch Ngày Lễ Tết & Phụ Thu Giá
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý danh mục các ngày lễ tết theo Điều 112 Bộ luật Lao động và sự kiện đặc biệt dùng để cấu hình phụ thu trong Pricing Engine
          </p>
        </div>
        <Button onClick={handleOpenCreateModal} className="shrink-0">
          <Plus className="w-5 h-5 mr-1" />
          Thêm Ngày Lễ Tết
        </Button>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleFilter} className="grid grid-cols-1 sm:grid-cols-5 gap-3">
          <div className="sm:col-span-2">
            <Input
              type="text"
              placeholder="Nhập tên ngày lễ (Tết, Quốc khánh...)..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>

          <div>
            <Select<number | ''>
              value={year}
              onChange={(val) => setYear(val ? Number(val) : '')}
              options={[
                { value: '', label: 'Tất cả các năm' },
                { value: currentYear - 1, label: `${currentYear - 1}` },
                { value: currentYear, label: `${currentYear} (Năm nay)` },
                { value: currentYear + 1, label: `${currentYear + 1}` },
                { value: currentYear + 2, label: `${currentYear + 2}` },
              ]}
              size="lg"
            />
          </div>

          <div>
            <Select<number | ''>
              value={month}
              onChange={(val) => setMonth(val ? Number(val) : '')}
              options={[
                { value: '', label: 'Tất cả các tháng' },
                ...Array.from({ length: 12 }, (_, i) => ({
                  value: i + 1,
                  label: `Tháng ${i + 1}`,
                })),
              ]}
              size="lg"
            />
          </div>

          <div className="flex gap-2">
            <Button type="submit" variant="secondary" className="flex-1 h-10 sm:h-12" isLoading={isLoading}>
              <Search className="w-4 h-4 mr-1.5" />
              Lọc
            </Button>
            <Button type="button" variant="outline" onClick={handleReset} className="h-10 sm:h-12 px-3">
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </div>

      {/* HÀNG 2: ACTION TOOLBAR */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-neutral-50 p-3 rounded-xl border border-neutral-200">
        <div className="flex items-center gap-2">
          {selectedIds.length > 0 && (
            <Button
              type="button"
              variant="danger"
              size="sm"
              onClick={handleDeleteSelected}
              className="flex items-center gap-1.5"
            >
              <Trash2 className="w-4 h-4" />
              Xóa ({selectedIds.length})
            </Button>
          )}
        </div>
        <div className="text-xs text-neutral-500 font-medium">
          Tổng số: <span className="font-semibold text-neutral-800">{totalElements}</span> ngày lễ
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE & PAGINATION */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-neutral-200 text-sm">
            <thead className="bg-neutral-50">
              <tr>
                <th className="px-4 py-3 text-left w-10">
                  <input
                    type="checkbox"
                    className="rounded border-neutral-300 text-red-600 focus:ring-red-500"
                    onChange={handleSelectAll}
                    checked={holidays.length > 0 && selectedIds.length === holidays.length}
                  />
                </th>
                <th className="px-4 py-3 text-left font-semibold text-neutral-700">Tên Ngày Lễ Tết</th>
                <th className="px-4 py-3 text-left font-semibold text-neutral-700">Ngày diễn ra</th>
                <th className="px-4 py-3 text-left font-semibold text-neutral-700">Mô tả / Ý nghĩa</th>
                <th className="px-4 py-3 text-left font-semibold text-neutral-700">Trạng thái</th>
                <th className="px-4 py-3 text-right font-semibold text-neutral-700 w-28">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-neutral-500">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-neutral-400" />
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : holidays.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-neutral-500">
                    Không có ngày lễ tết nào trong kỳ tìm kiếm
                  </td>
                </tr>
              ) : (
                holidays.map((h) => (
                  <tr key={h.id} className="hover:bg-neutral-50 transition-colors">
                    <td className="px-4 py-3">
                      <input
                        type="checkbox"
                        className="rounded border-neutral-300 text-red-600 focus:ring-red-500"
                        checked={selectedIds.includes(h.id)}
                        onChange={() => handleSelectOne(h.id)}
                      />
                    </td>
                    <td className="px-4 py-3 font-semibold text-neutral-900">
                      <div className="flex items-center gap-2">
                        <CalendarDays className="w-4 h-4 text-red-500 shrink-0" />
                        <span>{h.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="font-medium text-neutral-900">{formatDateDisplay(h.date)}</div>
                      <div className="text-xs text-neutral-500">{getDayOfWeek(h.date)}</div>
                    </td>
                    <td className="px-4 py-3 text-neutral-600 max-w-xs truncate">
                      {h.description || '-'}
                    </td>
                    <td className="px-4 py-3">
                      {h.status === 'ACTIVE' ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">
                          Hoạt động
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-neutral-100 text-neutral-600">
                          Ngừng áp dụng
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          onClick={() => handleOpenEditModal(h)}
                          className="p-1.5 text-neutral-500 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                          title="Chỉnh sửa"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteOne(h.id)}
                          className="p-1.5 text-neutral-500 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                          title="Xóa"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
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

      {/* Form Modal (Create / Update) */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingHoliday ? 'Chỉnh Sửa Ngày Lễ Tết' : 'Thêm Mới Ngày Lễ Tết'}
      >
        <form onSubmit={handleFormSubmit} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-xs">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-sm font-semibold text-neutral-700 mb-1">
              Tên ngày lễ tết <span className="text-red-500">*</span>
            </label>
            <Input
              type="text"
              placeholder="VD: Tết Dương Lịch, Quốc Khánh 2/9, Giỗ Tổ Hùng Vương..."
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-neutral-700 mb-1">
              Ngày diễn ra <span className="text-red-500">*</span>
            </label>
            <Input
              type="date"
              value={formDate}
              onChange={(e) => setFormDate(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-neutral-700 mb-1">
              Mô tả / Ghi chú
            </label>
            <textarea
              className="w-full px-3 py-2 border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500 text-sm"
              rows={3}
              placeholder="Ghi chú chi tiết về ngày lễ hoặc căn cứ áp dụng phụ thu giá phòng..."
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
            />
          </div>

          {editingHoliday && (
            <div>
              <label className="block text-sm font-semibold text-neutral-700 mb-1">
                Trạng thái áp dụng
              </label>
              <Select<string>
                value={formStatus}
                onChange={(val) => setFormStatus(val)}
                options={[
                  { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
                  { value: 'INACTIVE', label: 'Ngừng áp dụng (INACTIVE)' },
                ]}
                size="md"
              />
            </div>
          )}

          <div className="p-3 bg-blue-50 rounded-lg border border-blue-200 text-xs text-blue-800">
            📌 <strong>Quy tắc phụ thu ngày lễ:</strong> Khi ngày nhận phòng/lưu trú trùng với ngày trong lịch lễ tết này, Pricing Engine sẽ tự động kích hoạt các quy tắc giá (Pricing Rule) loại HOLIDAY_SURCHARGE tương ứng đã được gắn với ngày lễ này.
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              {editingHoliday ? 'Cập Nhật' : 'Tạo Mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa ngày lễ tết"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} ngày lễ tết đã chọn? Hệ thống sẽ ngăn chặn việc xóa nếu ngày lễ này đang được sử dụng trong các quy tắc phụ thu giá đang hoạt động.`}
        confirmText="Xác nhận xóa"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default HolidayCalendarListPage
