import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Receipt } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import taxCategoryService from '../services/taxCategory.service'
import type {
  TaxCategoryDto,
  TaxCategoryCreateRequest,
  TaxCategoryUpdateRequest,
} from '../types/pricing.types'

export const TaxCategoryListPage: React.FC = () => {
  const [categories, setCategories] = useState<TaxCategoryDto[]>([])
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
  const [editingCategory, setEditingCategory] = useState<TaxCategoryDto | null>(null)
  const [formCategoryCode, setFormCategoryCode] = useState('')
  const [formCategoryName, setFormCategoryName] = useState('')
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
      const res = await taxCategoryService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        status: status || undefined,
        sortBy: 'id',
        sortDirection: 'ASC',
      })
      if (res.result) {
        setCategories(res.result.content || [])
        setTotalElements(res.result.totalElements)
        setTotalPages(res.result.totalPages)
      }
    } catch (err) {
      console.error('Lỗi tải danh mục nhóm thuế:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, status])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadData()
  }

  const handleReset = () => {
    setKeyword('')
    setStatus('')
    setPage(0)
  }

  const handleOpenCreateModal = () => {
    setEditingCategory(null)
    setFormCategoryCode('')
    setFormCategoryName('')
    setFormDescription('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleOpenEditModal = (cat: TaxCategoryDto) => {
    setEditingCategory(cat)
    setFormCategoryCode(cat.categoryCode)
    setFormCategoryName(cat.categoryName)
    setFormDescription(cat.description || '')
    setFormStatus(cat.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!editingCategory && !formCategoryCode.trim()) {
      setFormError('Mã nhóm thuế không được để trống')
      return
    }
    if (!formCategoryName.trim()) {
      setFormError('Tên nhóm thuế không được để trống')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingCategory) {
        const updatePayload: TaxCategoryUpdateRequest = {
          categoryName: formCategoryName.trim(),
          description: formDescription.trim() || undefined,
          status: formStatus as 'ACTIVE' | 'INACTIVE',
        }
        await taxCategoryService.update(editingCategory.id, updatePayload)
      } else {
        const createPayload: TaxCategoryCreateRequest = {
          categoryCode: formCategoryCode.trim().toUpperCase(),
          categoryName: formCategoryName.trim(),
          description: formDescription.trim() || undefined,
        }
        await taxCategoryService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: any) {
      setFormError(err.response?.data?.message || err.message || 'Thao tác thất bại')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(categories.map((c) => c.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    )
  }

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return
    setIsDeleting(true)
    try {
      await taxCategoryService.deleteBatch(deletingIds)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      setDeleteConfirmOpen(false)
      setDeletingIds([])
      loadData()
    } catch (err: any) {
      alert(err.response?.data?.message || err.message || 'Xóa nhóm thuế thất bại')
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 flex items-center gap-2">
            <Receipt className="w-7 h-7 text-red-600" />
            Danh Mục Nhóm Thuế (Tax Categories)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý quy chuẩn nhóm thuế toàn chuỗi (Lưu trú, Ăn uống F&B, Spa, Dịch vụ). Chỉ Chain Admin có quyền chỉnh sửa.
          </p>
        </div>
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleFilter} className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <div className="md:col-span-2">
            <Input
              placeholder="Tìm theo mã hoặc tên nhóm thuế..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>

          <div>
            <Select<string>
              value={status}
              onChange={(val) => setStatus(val)}
              options={[
                { value: '', label: 'Tất cả trạng thái' },
                { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
                { value: 'INACTIVE', label: 'Ngừng hoạt động (INACTIVE)' },
              ]}
              size="md"
            />
          </div>

          <div className="flex items-center gap-2">
            <Button type="submit" variant="primary" className="flex-1" isLoading={isLoading}>
              <Search className="w-4 h-4 mr-1.5" />
              Lọc
            </Button>
            <Button type="button" variant="secondary" onClick={handleReset}>
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </div>

      {/* Row 2: Action Toolbar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <div className="flex items-center gap-2">
          <Button onClick={handleOpenCreateModal} variant="primary">
            <Plus className="w-4 h-4 mr-1.5" />
            Thêm Nhóm Thuế
          </Button>

          {selectedIds.length > 0 && (
            <Button
              variant="danger"
              onClick={() => {
                setDeletingIds(selectedIds)
                setDeleteConfirmOpen(true)
              }}
            >
              <Trash2 className="w-4 h-4 mr-1.5" />
              Xóa Đã Chọn ({selectedIds.length})
            </Button>
          )}
        </div>

        <div className="text-sm text-neutral-500">
          Tổng số: <span className="font-semibold text-neutral-900">{totalElements}</span> nhóm thuế
        </div>
      </div>

      {/* Row 3: Data Table & Pagination */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-neutral-50 border-b border-neutral-200 text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                <th className="py-3.5 px-4 w-12 text-center">
                  <input
                    type="checkbox"
                    className="rounded border-neutral-300 text-red-600 focus:ring-red-600"
                    checked={categories.length > 0 && selectedIds.length === categories.length}
                    onChange={handleSelectAll}
                  />
                </th>
                <th className="py-3.5 px-4">Mã Nhóm Thuế</th>
                <th className="py-3.5 px-4">Tên Nhóm Thuế</th>
                <th className="py-3.5 px-4">Mô Tả</th>
                <th className="py-3.5 px-4">Trạng Thái</th>
                <th className="py-3.5 px-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200 text-sm">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-neutral-400">
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : categories.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-neutral-400">
                    Không tìm thấy nhóm thuế nào phù hợp
                  </td>
                </tr>
              ) : (
                categories.map((cat) => (
                  <tr key={cat.id} className="hover:bg-neutral-50 transition-colors">
                    <td className="py-3.5 px-4 text-center">
                      <input
                        type="checkbox"
                        className="rounded border-neutral-300 text-red-600 focus:ring-red-600"
                        checked={selectedIds.includes(cat.id)}
                        onChange={() => handleSelectOne(cat.id)}
                      />
                    </td>
                    <td className="py-3.5 px-4 font-mono font-medium text-neutral-900">
                      {cat.categoryCode}
                    </td>
                    <td className="py-3.5 px-4 font-medium text-neutral-900">
                      {cat.categoryName}
                    </td>
                    <td className="py-3.5 px-4 text-neutral-500 max-w-xs truncate">
                      {cat.description || '—'}
                    </td>
                    <td className="py-3.5 px-4">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          cat.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                        }`}
                      >
                        {cat.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleOpenEditModal(cat)}
                          title="Chỉnh sửa"
                        >
                          <Edit2 className="w-4 h-4 text-neutral-600" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setDeletingIds([cat.id])
                            setDeleteConfirmOpen(true)
                          }}
                          title="Xóa"
                        >
                          <Trash2 className="w-4 h-4 text-red-600" />
                        </Button>
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

      {/* Form Modal (Create / Edit) */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingCategory ? 'Chỉnh Sửa Nhóm Thuế' : 'Tạo Mới Nhóm Thuế'}
      >
        <form onSubmit={handleFormSubmit} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Mã Nhóm Thuế <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: ROOM_TAX, FB_TAX, SPA_TAX..."
              value={formCategoryCode}
              onChange={(e) => setFormCategoryCode(e.target.value.toUpperCase())}
              disabled={!!editingCategory}
              required
            />
            {editingCategory && (
              <p className="text-xs text-neutral-400 mt-1">Mã nhóm thuế không thể sửa đổi sau khi tạo.</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Tên Nhóm Thuế <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: Thuế dịch vụ lưu trú phòng..."
              value={formCategoryName}
              onChange={(e) => setFormCategoryName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Mô Tả Chi Tiết</label>
            <textarea
              rows={3}
              placeholder="Mô tả phạm vi áp dụng của nhóm thuế..."
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
              className="w-full p-3 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600 text-sm"
            />
          </div>

          {editingCategory && (
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Trạng Thái</label>
              <Select<string>
                value={formStatus}
                onChange={(val) => setFormStatus(val)}
                options={[
                  { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
                  { value: 'INACTIVE', label: 'Tạm dừng (INACTIVE)' },
                ]}
                size="md"
              />
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-neutral-200">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              {editingCategory ? 'Cập Nhật' : 'Tạo Mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa nhóm thuế"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} nhóm thuế đã chọn? Thao tác này sẽ chuyển trạng thái bản ghi sang đã xóa.`}
        confirmText="Xác nhận xóa"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default TaxCategoryListPage
