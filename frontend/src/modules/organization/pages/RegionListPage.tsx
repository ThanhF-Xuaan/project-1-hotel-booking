import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Globe } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import regionService from '../services/region.service'
import type { RegionDto, RegionCreateRequest, RegionUpdateRequest } from '../types/organization.types'

export const RegionListPage: React.FC = () => {
  const [regions, setRegions] = useState<RegionDto[]>([])
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
  const [editingRegion, setEditingRegion] = useState<RegionDto | null>(null)
  const [formCode, setFormCode] = useState('')
  const [formName, setFormName] = useState('')
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
      const response = await regionService.filter({
        page,
        pageSize,
        keyword: keyword || undefined,
        status: status || undefined,
      })
      if (response && response.result) {
        setRegions(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách khu vực:', err)
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
      setSelectedIds(regions.map((r) => r.id))
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
    setEditingRegion(null)
    setFormCode('')
    setFormName('')
    setFormDescription('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (region: RegionDto) => {
    setEditingRegion(region)
    setFormCode(region.code)
    setFormName(region.name)
    setFormDescription(region.description || '')
    setFormStatus(region.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formName.trim()) {
      setFormError('Vui lòng nhập tên khu vực')
      return
    }
    if (!editingRegion && !formCode.trim()) {
      setFormError('Vui lòng nhập mã khu vực')
      return
    }

    setIsSubmitting(true)
    setFormError(null)
    try {
      if (editingRegion) {
        const updatePayload: RegionUpdateRequest = {
          name: formName.trim(),
          description: formDescription.trim() || undefined,
          status: formStatus,
        }
        await regionService.update(editingRegion.id, updatePayload)
      } else {
        const createPayload: RegionCreateRequest = {
          code: formCode.trim().toUpperCase(),
          name: formName.trim(),
          description: formDescription.trim() || undefined,
          status: formStatus,
        }
        await regionService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const errorMsg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Có lỗi xảy ra, vui lòng thử lại!'
      setFormError(errorMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  const confirmDeleteOne = (id: number) => {
    setDeletingIds([id])
    setDeleteConfirmOpen(true)
  }

  const confirmDeleteBatch = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteConfirmOpen(true)
  }

  const executeDelete = async () => {
    setIsDeleting(true)
    try {
      await regionService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds([])
      loadData()
    } catch (err) {
      console.error('Lỗi xóa khu vực:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Title & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2.5">
            <Globe className="w-6 h-6 text-red-600" />
            Quản lý Khu vực & Vùng
          </h2>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý cơ cấu phân vùng hoạt động của chuỗi khách sạn UTC (Miền Bắc, Miền Trung, Miền Nam)
          </p>
        </div>
      </div>

      {/* STANDARD 3-ROW LAYOUT */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        {/* ROW 1: SEARCH & FILTER BAR */}
        <form
          onSubmit={handleSearch}
          className="p-4 md:p-5 border-b border-neutral-200 bg-neutral-50/50 flex flex-col md:flex-row items-center gap-3"
        >
          <div className="flex-1 w-full">
            <Input
              placeholder="Tìm kiếm theo mã hoặc tên vùng..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>

          <div className="w-full md:w-48">
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full h-10 md:h-12 px-3 text-sm text-neutral-900 bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
            </select>
          </div>

          <div className="flex items-center gap-2 w-full md:w-auto">
            <Button type="submit" variant="primary" size="md" className="w-full md:w-auto">
              Tìm kiếm
            </Button>
            <Button
              type="button"
              variant="outline"
              size="md"
              onClick={() => {
                setKeyword('')
                setStatus('')
                setPage(0)
              }}
              title="Làm mới bộ lọc"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>

        {/* ROW 2: ACTION TOOLBAR */}
        <div className="p-4 border-b border-neutral-200 flex items-center justify-between gap-4">
          <div className="flex items-center gap-2.5">
            <Button variant="primary" size="md" onClick={openCreateModal} leftIcon={<Plus className="w-4 h-4" />}>
              Thêm mới Khu vực
            </Button>

            {selectedIds.length > 0 && (
              <Button
                variant="danger"
                size="md"
                onClick={confirmDeleteBatch}
                leftIcon={<Trash2 className="w-4 h-4" />}
              >
                Xóa đã chọn ({selectedIds.length})
              </Button>
            )}
          </div>

          <span className="text-xs md:text-sm text-neutral-500 font-medium">
            Tổng cộng: <strong className="text-neutral-900">{totalElements}</strong> khu vực
          </span>
        </div>

        {/* ROW 3: DATA TABLE & PAGINATION */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-neutral-700">
            <thead className="bg-neutral-100/70 text-neutral-900 font-semibold text-xs uppercase tracking-wider border-b border-neutral-200">
              <tr>
                <th className="p-4 w-12 text-center">
                  <input
                    type="checkbox"
                    checked={regions.length > 0 && selectedIds.length === regions.length}
                    onChange={handleSelectAll}
                    className="w-4 h-4 rounded text-red-600 focus:ring-red-600 cursor-pointer"
                  />
                </th>
                <th className="p-4">Mã Khu vực</th>
                <th className="p-4">Tên Khu vực</th>
                <th className="p-4">Mô tả</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-neutral-500">
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : regions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-neutral-500">
                    Không tìm thấy khu vực nào.
                  </td>
                </tr>
              ) : (
                regions.map((region) => {
                  const isChecked = selectedIds.includes(region.id)
                  return (
                    <tr
                      key={region.id}
                      className={`hover:bg-neutral-50/80 transition-colors ${
                        isChecked ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-4 text-center">
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => handleSelectOne(region.id)}
                          className="w-4 h-4 rounded text-red-600 focus:ring-red-600 cursor-pointer"
                        />
                      </td>
                      <td className="p-4 font-bold text-neutral-900">{region.code}</td>
                      <td className="p-4 font-medium text-neutral-800">{region.name}</td>
                      <td className="p-4 text-neutral-500 max-w-xs truncate">{region.description || '—'}</td>
                      <td className="p-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                            region.status === 'ACTIVE'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-300'
                          }`}
                        >
                          {region.status === 'ACTIVE' ? 'Hoạt động' : 'Ngừng'}
                        </span>
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => openEditModal(region)}
                            className="p-2 text-neutral-600 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                            title="Sửa khu vực"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => confirmDeleteOne(region.id)}
                            className="p-2 text-red-600 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                            title="Xóa khu vực"
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

        {/* PAGINATION */}
        <Pagination
          pageNumber={page}
          pageSize={pageSize}
          totalElements={totalElements}
          totalPages={totalPages}
          onPageChange={(newPage) => setPage(newPage)}
        />
      </div>

      {/* FORM MODAL (ADD / EDIT) */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingRegion ? 'Cập nhật Khu vực' : 'Thêm mới Khu vực'}
        description={
          editingRegion
            ? `Chỉnh sửa thông tin khu vực ${editingRegion.code}`
            : 'Nhập thông tin khu vực mới vào hệ thống'
        }
      >
        <form onSubmit={handleSave} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-sm">
              {formError}
            </div>
          )}

          <Input
            label="Mã Khu vực"
            placeholder="VD: NORTH, CENTRAL, SOUTH..."
            value={formCode}
            onChange={(e) => setFormCode(e.target.value)}
            disabled={!!editingRegion}
            required
          />

          <Input
            label="Tên Khu vực"
            placeholder="VD: Miền Bắc, Miền Trung..."
            value={formName}
            onChange={(e) => setFormName(e.target.value)}
            required
          />

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-neutral-800">Mô tả chi tiết</label>
            <textarea
              rows={3}
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
              className="w-full p-3 text-sm text-neutral-900 bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-600"
              placeholder="Nhập mô tả về khu vực quản lý..."
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-neutral-800">Trạng thái</label>
            <select
              value={formStatus}
              onChange={(e) => setFormStatus(e.target.value)}
              className="w-full h-10 md:h-12 px-3 text-sm text-neutral-900 bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
            </select>
          </div>

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-neutral-100">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy bỏ
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              {editingRegion ? 'Lưu thay đổi' : 'Tạo mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* CONFIRM DELETE MODAL */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={executeDelete}
        title="Xác nhận xóa khu vực?"
        description={`Bạn có chắc chắn muốn xóa ${
          deletingIds.length === 1 ? 'khu vực này' : `${deletingIds.length} khu vực đã chọn`
        }? Dữ liệu sẽ được chuyển vào trạng thái xóa mềm.`}
        confirmText="Xác nhận xóa"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default RegionListPage
