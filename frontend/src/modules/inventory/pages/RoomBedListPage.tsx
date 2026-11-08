import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Bed } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import roomBedService from '../services/roomBed.service'
import type { RoomBedDto, RoomBedCreateRequest, RoomBedUpdateRequest } from '../types/inventory.types'

export const RoomBedListPage: React.FC = () => {
  const [beds, setBeds] = useState<RoomBedDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingBed, setEditingBed] = useState<RoomBedDto | null>(null)
  const [formName, setFormName] = useState('')
  const [formSize, setFormSize] = useState('')
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
      const response = await roomBedService.filter({
        page,
        pageSize,
        keyword: keyword.trim(),
        status: status || undefined,
      })
      if (response && response.result) {
        setBeds(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err: unknown) {
      console.error('Lỗi khi tải danh sách loại giường:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, status])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadData()
  }

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(beds.map((b) => b.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  const openCreateModal = () => {
    setEditingBed(null)
    setFormName('')
    setFormSize('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (bed: RoomBedDto) => {
    setEditingBed(bed)
    setFormName(bed.name)
    setFormSize(bed.size || '')
    setFormStatus(bed.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!formName.trim()) {
      setFormError('Tên loại giường không được để trống')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingBed) {
        const updatePayload: RoomBedUpdateRequest = {
          name: formName.trim(),
          size: formSize.trim() || undefined,
          status: formStatus,
        }
        await roomBedService.update(editingBed.id, updatePayload)
      } else {
        const createPayload: RoomBedCreateRequest = {
          name: formName.trim(),
          size: formSize.trim() || undefined,
          status: formStatus,
        }
        await roomBedService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } }; message?: string }
      setFormError(axiosError.response?.data?.message || axiosError.message || 'Lỗi khi lưu dữ liệu loại giường')
    } finally {
      setIsSubmitting(false)
    }
  }

  const promptDeleteBatch = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteConfirmOpen(true)
  }

  const promptDeleteSingle = (id: number) => {
    setDeletingIds([id])
    setDeleteConfirmOpen(true)
  }

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return
    setIsDeleting(true)
    try {
      await roomBedService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      loadData()
    } catch (err: unknown) {
      console.error('Lỗi khi xóa loại giường:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  const statusOptions = [
    { value: '', label: 'Tất cả trạng thái' },
    { value: 'ACTIVE', label: 'Đang hoạt động' },
    { value: 'INACTIVE', label: 'Tạm dừng' },
  ]

  const formStatusOptions = [
    { value: 'ACTIVE', label: 'Đang hoạt động' },
    { value: 'INACTIVE', label: 'Tạm dừng' },
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-red-50 text-red-600 rounded-xl">
              <Bed className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-neutral-900 tracking-tight">
                Danh Mục Tiêu Chuẩn Giường
              </h1>
              <p className="text-sm text-neutral-500">
                Quản lý các loại kích thước và tiêu chuẩn giường toàn hệ thống
              </p>
            </div>
          </div>
        </div>
        <Button onClick={openCreateModal} className="shrink-0">
          <Plus className="w-4 h-4 mr-1.5" />
          Thêm Loại Giường
        </Button>
      </div>

      {/* 3-Row Layout: Row 1 - Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1">
            <Input
              placeholder="Tìm theo tên hoặc kích thước giường..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>
          <div className="w-full sm:w-56">
            <Select
              value={status}
              onChange={(val) => setStatus(String(val))}
              options={statusOptions}
            />
          </div>
          <Button type="submit" variant="secondary" className="shrink-0">
            <Search className="w-4 h-4 mr-1.5" />
            Tìm Kiếm
          </Button>
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              setKeyword('')
              setStatus('')
              setPage(0)
            }}
            className="shrink-0"
            title="Làm mới bộ lọc"
            aria-label="Làm mới bộ lọc"
          >
            <RefreshCw className="w-4 h-4" />
          </Button>
        </form>
      </div>

      {/* 3-Row Layout: Row 2 - Action Toolbar */}
      <div className="flex items-center justify-between px-1">
        <div className="flex items-center gap-3">
          {selectedIds.length > 0 && (
            <Button
              variant="danger"
              size="sm"
              onClick={promptDeleteBatch}
              className="animate-fade-in"
            >
              <Trash2 className="w-4 h-4 mr-1.5" />
              Xóa {selectedIds.length} mục đã chọn
            </Button>
          )}
        </div>
        <div className="text-sm text-neutral-500">
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> loại giường
        </div>
      </div>

      {/* 3-Row Layout: Row 3 - Data Table */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-50/50 text-neutral-600">
                <th className="py-3.5 px-4 w-12 text-center">
                  <input
                    type="checkbox"
                    checked={beds.length > 0 && selectedIds.length === beds.length}
                    onChange={handleSelectAll}
                    aria-label="Chọn tất cả loại giường"
                    className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                  />
                </th>
                <th className="py-3.5 px-4 font-semibold">Tên Loại Giường</th>
                <th className="py-3.5 px-4 font-semibold">Kích Thước Tiêu Chuẩn</th>
                <th className="py-3.5 px-4 font-semibold">Trạng Thái</th>
                <th className="py-3.5 px-4 font-semibold text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200/70">
              {isLoading ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-neutral-500">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải dữ liệu...
                    </div>
                  </td>
                </tr>
              ) : beds.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-neutral-500">
                    Không tìm thấy loại giường nào phù hợp.
                  </td>
                </tr>
              ) : (
                beds.map((bed) => {
                  const isSelected = selectedIds.includes(bed.id)
                  return (
                    <tr
                      key={bed.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/20' : ''
                      }`}
                    >
                      <td className="py-3 px-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(bed.id)}
                          aria-label={`Chọn loại giường ${bed.name}`}
                          className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                        />
                      </td>
                      <td className="py-3 px-4 font-medium text-neutral-900">
                        {bed.name}
                      </td>
                      <td className="py-3 px-4 text-neutral-600 font-mono text-xs">
                        {bed.size || '—'}
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                            bed.status === 'ACTIVE'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                          }`}
                        >
                          {bed.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="inline-flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => openEditModal(bed)}
                            className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                            title="Chỉnh sửa loại giường"
                            aria-label={`Chỉnh sửa ${bed.name}`}
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => promptDeleteSingle(bed.id)}
                            className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                            title="Xóa loại giường"
                            aria-label={`Xóa ${bed.name}`}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="p-4 border-t border-neutral-200 bg-neutral-50/30">
          <Pagination
            pageNumber={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={setPage}
          />
        </div>
      </div>

      {/* Form Modal (Create / Edit) */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingBed ? 'Cập Nhật Loại Giường' : 'Thêm Loại Giường Mới'}
        maxWidth="md"
      >
        <form onSubmit={handleSubmitForm} className="space-y-4">
          {formError && (
            <div className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Tên Loại Giường <span className="text-red-600">*</span>
            </label>
            <Input
              placeholder="VD: King Size Bed (Giường cỡ đại)"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Kích Thước Tiêu Chuẩn
            </label>
            <Input
              placeholder="VD: 1.8m x 2.0m"
              value={formSize}
              onChange={(e) => setFormSize(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Trạng Thái
            </label>
            <Select
              value={formStatus}
              onChange={(val) => setFormStatus(String(val))}
              options={formStatusOptions}
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-neutral-200">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy Bỏ
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : editingBed ? 'Lưu Thay Đổi' : 'Tạo Loại Giường'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa loại giường"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} loại giường đã chọn? Thao tác này sẽ chuyển dữ liệu sang trạng thái đã xóa mềm.`}
        confirmText={isDeleting ? 'Đang xóa...' : 'Xác Nhận Xóa'}
        isLoading={isDeleting}
        variant="danger"
      />
    </div>
  )
}

export default RoomBedListPage
