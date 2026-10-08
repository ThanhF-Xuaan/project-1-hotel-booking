import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, BedDouble } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import roomTypeService from '../services/roomType.service'
import type { RoomTypeDto, RoomTypeCreateRequest, RoomTypeUpdateRequest } from '../types/inventory.types'

export const RoomTypeListPage: React.FC = () => {
  const [roomTypes, setRoomTypes] = useState<RoomTypeDto[]>([])
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
  const [editingRoomType, setEditingRoomType] = useState<RoomTypeDto | null>(null)
  const [formCode, setFormCode] = useState('')
  const [formName, setFormName] = useState('')
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
      const response = await roomTypeService.filter({
        page,
        pageSize,
        keyword: keyword.trim(),
        status: status || undefined,
      })
      if (response && response.result) {
        setRoomTypes(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách loại phòng:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, status])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadData()
  }

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(roomTypes.map((rt) => rt.id))
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
    setEditingRoomType(null)
    setFormCode('')
    setFormName('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (roomType: RoomTypeDto) => {
    setEditingRoomType(roomType)
    setFormCode(roomType.code)
    setFormName(roomType.name)
    setFormStatus(roomType.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!formName.trim()) {
      setFormError('Tên loại phòng không được để trống')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingRoomType) {
        const updatePayload: RoomTypeUpdateRequest = {
          name: formName.trim(),
          status: formStatus,
        }
        await roomTypeService.update(editingRoomType.id, updatePayload)
      } else {
        if (!formCode.trim()) {
          setFormError('Mã loại phòng không được để trống')
          setIsSubmitting(false)
          return
        }
        const createPayload: RoomTypeCreateRequest = {
          code: formCode.trim().toUpperCase(),
          name: formName.trim(),
          status: formStatus,
        }
        await roomTypeService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const errorMsg =
        err instanceof Error ? err.message : 'Có lỗi xảy ra khi lưu loại phòng'
      setFormError(errorMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  const promptDeleteSingle = (id: number) => {
    setDeletingIds([id])
    setDeleteConfirmOpen(true)
  }

  const promptDeleteBatch = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteConfirmOpen(true)
  }

  const handleConfirmDelete = async () => {
    setIsDeleting(true)
    try {
      await roomTypeService.deleteBatch(deletingIds)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      setDeleteConfirmOpen(false)
      loadData()
    } catch (err) {
      console.error('Lỗi khi xóa loại phòng:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header Title */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <BedDouble className="w-7 h-7 text-red-600" />
            Danh Mục Loại Phòng
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý các loại phòng tiêu chuẩn áp dụng thống nhất toàn chuỗi khách sạn
          </p>
        </div>
        <Button onClick={openCreateModal} className="shrink-0">
          <Plus className="w-5 h-5 mr-1" />
          Thêm Loại Phòng
        </Button>
      </div>

      {/* 3-Row Layout: Row 1 - Search & Filter */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1">
            <Input
              placeholder="Tìm theo mã hoặc tên loại phòng..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="h-10 sm:h-12"
            />
          </div>
          <div className="w-full sm:w-48">
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full h-10 sm:h-12 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Tạm dừng (INACTIVE)</option>
            </select>
          </div>
          <Button type="submit" variant="secondary" className="h-10 sm:h-12">
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
            className="h-10 sm:h-12"
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
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> loại phòng
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
                    checked={
                      roomTypes.length > 0 && selectedIds.length === roomTypes.length
                    }
                    onChange={handleSelectAll}
                    aria-label="Chọn tất cả loại phòng"
                    className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                  />
                </th>
                <th className="py-3.5 px-4 font-semibold">Mã Loại Phòng</th>
                <th className="py-3.5 px-4 font-semibold">Tên Loại Phòng</th>
                <th className="py-3.5 px-4 font-semibold">Trạng Thái</th>
                <th className="py-3.5 px-4 font-semibold">Ngày Cập Nhật</th>
                <th className="py-3.5 px-4 font-semibold text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200/70">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-500">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải dữ liệu...
                    </div>
                  </td>
                </tr>
              ) : roomTypes.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-500">
                    Không tìm thấy loại phòng nào phù hợp.
                  </td>
                </tr>
              ) : (
                roomTypes.map((rt) => {
                  const isSelected = selectedIds.includes(rt.id)
                  return (
                    <tr
                      key={rt.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/20' : ''
                      }`}
                    >
                      <td className="py-3 px-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(rt.id)}
                          aria-label={`Chọn loại phòng ${rt.name}`}
                          className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                        />
                      </td>
                      <td className="py-3 px-4 font-mono font-medium text-neutral-900">
                        {rt.code}
                      </td>
                      <td className="py-3 px-4 font-medium text-neutral-900">
                        {rt.name}
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                            rt.status === 'ACTIVE'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                          }`}
                        >
                          {rt.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-neutral-500 text-xs">
                        {rt.updatedAt
                          ? new Date(rt.updatedAt).toLocaleString('vi-VN')
                          : '—'}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEditModal(rt)}
                            aria-label={`Chỉnh sửa ${rt.name}`}
                          >
                            <Edit2 className="w-4 h-4 text-neutral-600" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => promptDeleteSingle(rt.id)}
                            aria-label={`Xóa ${rt.name}`}
                          >
                            <Trash2 className="w-4 h-4 text-red-600" />
                          </Button>
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
        <div className="p-4 border-t border-neutral-200/80">
          <Pagination
            pageNumber={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={(newPage) => setPage(newPage)}
          />
        </div>
      </div>

      {/* Form Modal (Create / Update) */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingRoomType ? 'Chỉnh Sửa Loại Phòng' : 'Thêm Mới Loại Phòng'}
      >
        <form onSubmit={handleSubmitForm} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 text-red-600 text-sm rounded-xl border border-red-200">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Mã Loại Phòng <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: DELUXE, SUITE, STANDARD..."
              value={formCode}
              onChange={(e) => setFormCode(e.target.value.toUpperCase())}
              disabled={!!editingRoomType}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Tên Loại Phòng <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: Phòng Deluxe Hướng Biển..."
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Trạng Thái
            </label>
            <select
              value={formStatus}
              onChange={(e) => setFormStatus(e.target.value)}
              className="w-full h-11 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Tạm dừng (INACTIVE)</option>
            </select>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-neutral-200">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              {editingRoomType ? 'Cập Nhật' : 'Tạo Mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa loại phòng"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} loại phòng đã chọn? Thao tác này sẽ chuyển dữ liệu vào thùng rác.`}
        confirmText="Xác nhận xóa"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default RoomTypeListPage
