import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Sparkles } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import roomFeatureService from '../services/roomFeature.service'
import type { RoomFeatureDto, RoomFeatureCreateRequest, RoomFeatureUpdateRequest } from '../types/inventory.types'

export const RoomFeatureListPage: React.FC = () => {
  const [features, setFeatures] = useState<RoomFeatureDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState<string>('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingFeature, setEditingFeature] = useState<RoomFeatureDto | null>(null)
  const [formCode, setFormCode] = useState('')
  const [formName, setFormName] = useState('')
  const [formCategory, setFormCategory] = useState('AMENITY')
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
      const response = await roomFeatureService.filter({
        page,
        pageSize,
        keyword: keyword.trim(),
        category: category || undefined,
        status: status || undefined,
      })
      if (response && response.result) {
        const filtered = (response.result.content || []).filter((f) => f.category !== 'VIEW')
        setFeatures(filtered)
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err: unknown) {
      console.error('Lỗi khi tải danh sách tiện ích phòng:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, category, status])

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
      setSelectedIds(features.map((f) => f.id))
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
    setEditingFeature(null)
    setFormCode('')
    setFormName('')
    setFormCategory('AMENITY')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (feature: RoomFeatureDto) => {
    setEditingFeature(feature)
    setFormCode(feature.code)
    setFormName(feature.name)
    setFormCategory(feature.category || 'AMENITY')
    setFormStatus(feature.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!editingFeature) {
      if (!formCode.trim()) {
        setFormError('Mã tiện ích không được để trống')
        return
      }
      if (!/^[A-Z0-9_]+$/.test(formCode.trim().toUpperCase())) {
        setFormError('Mã tiện ích chỉ được chứa chữ hoa không dấu, số và dấu gạch dưới (VD: BALCONY, MINIBAR)')
        return
      }
      if (formCode.trim().length > 50) {
        setFormError('Mã tiện ích tối đa 50 ký tự')
        return
      }
    }

    if (!formName.trim()) {
      setFormError('Tên tiện ích không được để trống')
      return
    }

    if (formName.trim().length > 150) {
      setFormError('Tên tiện ích tối đa 150 ký tự')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingFeature) {
        const updatePayload: RoomFeatureUpdateRequest = {
          name: formName.trim(),
          category: formCategory,
          status: formStatus,
        }
        await roomFeatureService.update(editingFeature.id, updatePayload)
      } else {
        const createPayload: RoomFeatureCreateRequest = {
          code: formCode.trim().toUpperCase(),
          name: formName.trim(),
          category: formCategory,
          status: formStatus,
        }
        await roomFeatureService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } }; message?: string }
      setFormError(axiosError.response?.data?.message || axiosError.message || 'Lỗi khi lưu dữ liệu tiện ích')
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
      await roomFeatureService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      loadData()
    } catch (err: unknown) {
      console.error('Lỗi khi xóa tiện ích phòng:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  const categoryOptions = [
    { value: '', label: 'Tất cả danh mục' },
    { value: 'BATHROOM', label: 'Phòng tắm (Bathroom)' },
    { value: 'BEDROOM', label: 'Phòng ngủ (Bedroom)' },
    { value: 'MEDIA', label: 'Truyền thông & Công nghệ (Media)' },
    { value: 'ENTERTAINMENT', label: 'Giải trí (Entertainment)' },
    { value: 'AMENITY', label: 'Tiện nghi sinh hoạt (Amenity)' },
    { value: 'COMFORT', label: 'Nội thất & Tiện ích (Comfort)' },
    { value: 'INTERNET', label: 'Mạng & Kết nối (Internet)' },
    { value: 'OTHER', label: 'Khác (Other)' },
  ]

  const formCategoryOptions = [
    { value: 'BATHROOM', label: 'Phòng tắm (Bathroom)' },
    { value: 'BEDROOM', label: 'Phòng ngủ (Bedroom)' },
    { value: 'MEDIA', label: 'Truyền thông & Công nghệ (Media)' },
    { value: 'ENTERTAINMENT', label: 'Giải trí (Entertainment)' },
    { value: 'AMENITY', label: 'Tiện nghi sinh hoạt (Amenity)' },
    { value: 'COMFORT', label: 'Nội thất & Tiện ích (Comfort)' },
    { value: 'INTERNET', label: 'Mạng & Kết nối (Internet)' },
    { value: 'OTHER', label: 'Khác (Other)' },
  ]

  const statusOptions = [
    { value: '', label: 'Tất cả trạng thái' },
    { value: 'ACTIVE', label: 'Đang hoạt động' },
    { value: 'INACTIVE', label: 'Tạm dừng' },
  ]

  const formStatusOptions = [
    { value: 'ACTIVE', label: 'Đang hoạt động' },
    { value: 'INACTIVE', label: 'Tạm dừng' },
  ]

  const getCategoryBadgeClass = (cat?: string) => {
    switch (cat) {
      case 'VIEW':
        return 'bg-blue-50 text-blue-700 border-blue-200'
      case 'BATHROOM':
        return 'bg-cyan-50 text-cyan-700 border-cyan-200'
      case 'BEDROOM':
        return 'bg-purple-50 text-purple-700 border-purple-200'
      case 'MEDIA':
      case 'INTERNET':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200'
      case 'ENTERTAINMENT':
        return 'bg-amber-50 text-amber-700 border-amber-200'
      case 'AMENITY':
      case 'COMFORT':
        return 'bg-teal-50 text-teal-700 border-teal-200'
      default:
        return 'bg-neutral-100 text-neutral-600 border-neutral-200'
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-red-50 text-red-600 rounded-xl">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-neutral-900 tracking-tight">
                Danh Mục Tiện Ích Phòng
              </h1>
              <p className="text-sm text-neutral-500">
                Quản lý các cơ sở vật chất và tiện ích phòng lưu trú toàn chuỗi
              </p>
            </div>
          </div>
        </div>
        <Button onClick={openCreateModal} className="shrink-0">
          <Plus className="w-4 h-4 mr-1.5" />
          Thêm Tiện Ích
        </Button>
      </div>

      {/* 3-Row Layout: Row 1 - Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1">
            <Input
              placeholder="Tìm theo mã hoặc tên tiện ích..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>
          <div className="w-full sm:w-60">
            <Select
              value={category}
              onChange={(val) => setCategory(String(val))}
              options={categoryOptions}
            />
          </div>
          <div className="w-full sm:w-48">
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
              setCategory('')
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
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> tiện ích
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
                    checked={features.length > 0 && selectedIds.length === features.length}
                    onChange={handleSelectAll}
                    aria-label="Chọn tất cả tiện ích phòng"
                    className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                  />
                </th>
                <th className="py-3.5 px-4 font-semibold">Mã Tiện Ích</th>
                <th className="py-3.5 px-4 font-semibold">Tên Tiện Ích</th>
                <th className="py-3.5 px-4 font-semibold">Danh Mục</th>
                <th className="py-3.5 px-4 font-semibold">Trạng Thái</th>
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
              ) : features.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-500">
                    Không tìm thấy tiện ích nào phù hợp.
                  </td>
                </tr>
              ) : (
                features.map((feature) => {
                  const isSelected = selectedIds.includes(feature.id)
                  return (
                    <tr
                      key={feature.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/20' : ''
                      }`}
                    >
                      <td className="py-3 px-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(feature.id)}
                          aria-label={`Chọn tiện ích ${feature.name}`}
                          className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                        />
                      </td>
                      <td className="py-3 px-4 font-mono font-medium text-neutral-900">
                        {feature.code}
                      </td>
                      <td className="py-3 px-4 font-medium text-neutral-900">
                        {feature.name}
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${getCategoryBadgeClass(
                            feature.category
                          )}`}
                        >
                          {feature.category || 'OTHER'}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                            feature.status === 'ACTIVE'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                          }`}
                        >
                          {feature.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="inline-flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => openEditModal(feature)}
                            className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                            title="Chỉnh sửa tiện ích"
                            aria-label={`Chỉnh sửa ${feature.name}`}
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => promptDeleteSingle(feature.id)}
                            className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                            title="Xóa tiện ích"
                            aria-label={`Xóa ${feature.name}`}
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
        title={editingFeature ? 'Cập Nhật Tiện Ích Phòng' : 'Thêm Tiện Ích Mới'}
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
              Mã Tiện Ích <span className="text-red-600">*</span>
            </label>
            <Input
              placeholder="VD: BALCONY"
              value={formCode}
              onChange={(e) => setFormCode(e.target.value.toUpperCase())}
              disabled={!!editingFeature}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Tên Tiện Ích <span className="text-red-600">*</span>
            </label>
            <Input
              placeholder="VD: Ban công ngắm biển"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Danh Mục Tiện Ích
            </label>
            <Select
              value={formCategory}
              onChange={(val) => setFormCategory(String(val))}
              options={formCategoryOptions}
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
              {isSubmitting ? 'Đang lưu...' : editingFeature ? 'Lưu Thay Đổi' : 'Tạo Tiện Ích'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa tiện ích"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} tiện ích phòng đã chọn? Thao tác này sẽ chuyển dữ liệu sang trạng thái đã xóa mềm.`}
        confirmText={isDeleting ? 'Đang xóa...' : 'Xác Nhận Xóa'}
        isLoading={isDeleting}
        variant="danger"
      />
    </div>
  )
}

export default RoomFeatureListPage
