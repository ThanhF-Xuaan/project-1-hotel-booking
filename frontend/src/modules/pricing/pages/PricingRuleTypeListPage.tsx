import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Sliders } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import pricingRuleTypeService from '../services/pricingRuleType.service'
import type {
  PricingRuleTypeDto,
  PricingRuleTypeCreateRequest,
  PricingRuleTypeUpdateRequest,
} from '../types/pricing.types'

export const PricingRuleTypeListPage: React.FC = () => {
  const [ruleTypes, setRuleTypes] = useState<PricingRuleTypeDto[]>([])
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingType, setEditingType] = useState<PricingRuleTypeDto | null>(null)
  const [formCode, setFormCode] = useState('')
  const [formDisplayName, setFormDisplayName] = useState('')
  const [formPriority, setFormPriority] = useState<number>(10)
  const [formStatus, setFormStatus] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingCode, setDeletingCode] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await pricingRuleTypeService.getAll()
      if (response && response.result) {
        setRuleTypes(response.result)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách loại quy tắc giá:', err)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const filteredList = ruleTypes.filter((item) => {
    const keyword = searchTerm.toLowerCase().trim()
    const matchKeyword =
      !keyword ||
      item.code.toLowerCase().includes(keyword) ||
      item.displayName.toLowerCase().includes(keyword)
    const matchStatus = !statusFilter || item.status === statusFilter
    return matchKeyword && matchStatus
  })

  const openCreateModal = () => {
    setEditingType(null)
    setFormCode('')
    setFormDisplayName('')
    setFormPriority(10)
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (item: PricingRuleTypeDto) => {
    setEditingType(item)
    setFormCode(item.code)
    setFormDisplayName(item.displayName)
    setFormPriority(item.priority)
    setFormStatus(item.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!editingType && !formCode.trim()) {
      setFormError('Mã loại quy tắc không được để trống')
      return
    }
    if (!formDisplayName.trim()) {
      setFormError('Tên hiển thị không được để trống')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingType) {
        const updatePayload: PricingRuleTypeUpdateRequest = {
          displayName: formDisplayName.trim(),
          priority: Number(formPriority),
          status: formStatus,
        }
        await pricingRuleTypeService.update(editingType.code, updatePayload)
      } else {
        const createPayload: PricingRuleTypeCreateRequest = {
          code: formCode.trim().toUpperCase(),
          displayName: formDisplayName.trim(),
          priority: Number(formPriority),
          status: formStatus,
        }
        await pricingRuleTypeService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const axiosError = err as {
        response?: {
          data?: {
            message?: string
            errors?: Record<string, string>
          }
        }
      }
      const respData = axiosError?.response?.data
      let errorMsg = respData?.message
      if (respData?.errors && Object.keys(respData.errors).length > 0) {
        errorMsg = Object.values(respData.errors).join(', ')
      }
      setFormError(errorMsg || 'Không thể lưu loại quy tắc giá. Vui lòng thử lại.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const promptDelete = (code: string) => {
    setDeletingCode(code)
    setDeleteError(null)
    setDeleteConfirmOpen(true)
  }

  const handleConfirmDelete = async () => {
    if (!deletingCode) return
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await pricingRuleTypeService.delete(deletingCode)
      setDeleteConfirmOpen(false)
      loadData()
    } catch (err: unknown) {
      const axiosError = err as {
        response?: {
          data?: {
            message?: string
          }
        }
      }
      setDeleteError(
        axiosError?.response?.data?.message ||
          'Không thể xóa loại quy tắc này. Có thể đang có quy tắc giá sử dụng.'
      )
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <Sliders className="w-7 h-7 text-red-600" />
            Loại Quy Tắc Giá (Pricing Rule Types)
          </h1>
          <p className="text-neutral-500 text-sm mt-1">
            Quản lý danh mục các loại quy tắc phụ thu giá phòng (Mùa cao điểm, Cuối tuần, Ngày lễ...)
          </p>
        </div>
      </div>

      {/* Standard 3-Row Layout Container */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-sm overflow-hidden">
        {/* ROW 1: SEARCH & FILTER BAR */}
        <div className="p-4 border-b border-neutral-100 bg-neutral-50/50">
          <div className="flex flex-col md:flex-row gap-3 items-center justify-between">
            <div className="flex flex-1 flex-col sm:flex-row gap-3 w-full">
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <Input
                  type="text"
                  placeholder="Tìm theo mã hoặc tên loại quy tắc..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-9 h-10 w-full"
                />
              </div>

              <div className="w-full sm:w-48">
                <Select<string>
                  value={statusFilter}
                  onChange={(val) => setStatusFilter(val)}
                  options={[
                    { value: '', label: 'Tất cả trạng thái' },
                    { value: 'ACTIVE', label: 'Hoạt động' },
                    { value: 'INACTIVE', label: 'Tạm dừng' },
                  ]}
                  size="md"
                />
              </div>
            </div>

            <div className="flex items-center gap-2 w-full md:w-auto justify-end">
              <Button
                variant="outline"
                size="md"
                onClick={() => {
                  setSearchTerm('')
                  setStatusFilter('')
                  loadData()
                }}
                disabled={isLoading}
              >
                <RefreshCw className={`w-4 h-4 mr-1.5 ${isLoading ? 'animate-spin' : ''}`} />
                Làm mới
              </Button>
            </div>
          </div>
        </div>

        {/* ROW 2: ACTION TOOLBAR */}
        <div className="px-4 py-3 border-b border-neutral-100 flex items-center justify-between bg-white">
          <div className="text-sm text-neutral-600 font-medium">
            Tổng cộng: <span className="text-neutral-900 font-semibold">{filteredList.length}</span> loại quy tắc
          </div>

          <Button variant="primary" size="md" onClick={openCreateModal}>
            <Plus className="w-4 h-4 mr-1.5" />
            Thêm Loại Quy Tắc
          </Button>
        </div>

        {/* ROW 3: DATA TABLE */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-50/75 text-neutral-600 font-semibold text-xs uppercase tracking-wider">
                <th className="py-3 px-4">Mã Loại (Code)</th>
                <th className="py-3 px-4">Tên Hiển Thị</th>
                <th className="py-3 px-4 text-center">Độ Ưu Tiên</th>
                <th className="py-3 px-4">Trạng Thái</th>
                <th className="py-3 px-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoading ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-neutral-500">
                    <div className="flex items-center justify-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải danh sách...
                    </div>
                  </td>
                </tr>
              ) : filteredList.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-neutral-500">
                    Không tìm thấy loại quy tắc giá nào phù hợp.
                  </td>
                </tr>
              ) : (
                filteredList.map((item) => (
                  <tr key={item.code} className="hover:bg-neutral-50/60 transition-colors">
                    <td className="py-3 px-4 font-mono font-semibold text-neutral-900 whitespace-nowrap">
                      {item.code}
                    </td>
                    <td className="py-3 px-4 font-medium text-neutral-900 min-w-[200px] max-w-xs whitespace-normal break-words leading-snug">
                      {item.displayName}
                    </td>
                    <td className="py-3 px-4 text-center whitespace-nowrap">
                      <span className="inline-flex items-center justify-center px-2 py-0.5 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-800">
                        {item.priority}
                      </span>
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <span
                        className={`inline-flex items-center whitespace-nowrap px-2.5 py-1 rounded-full text-xs font-semibold ${
                          item.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                        }`}
                      >
                        {item.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => openEditModal(item)}
                          aria-label={`Sửa loại quy tắc ${item.code}`}
                        >
                          <Edit2 className="w-4 h-4 text-neutral-600" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => promptDelete(item.code)}
                          aria-label={`Xóa loại quy tắc ${item.code}`}
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
      </div>

      {/* Form Modal */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingType ? 'Chỉnh Sửa Loại Quy Tắc Giá' : 'Thêm Mới Loại Quy Tắc Giá'}
      >
        <form onSubmit={handleSubmitForm} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 text-red-600 text-sm rounded-xl border border-red-200">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Mã Loại Quy Tắc (Code) <span className="text-red-500">*</span>
            </label>
            <Input
              type="text"
              placeholder="VD: PEAK_SEASON, HOLIDAY, WEEKEND..."
              value={formCode}
              onChange={(e) => setFormCode(e.target.value)}
              disabled={!!editingType}
              required
            />
            <span className="text-xs text-neutral-400 mt-1 block">
              Mã viết hoa, không dấu, phân cách bằng dấu gạch dưới. Không thể đổi sau khi tạo.
            </span>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Tên Hiển Thị <span className="text-red-500">*</span>
            </label>
            <Input
              type="text"
              placeholder="VD: Mùa cao điểm, Ngày lễ tết..."
              value={formDisplayName}
              onChange={(e) => setFormDisplayName(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Độ Ưu Tiên <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                placeholder="10"
                value={formPriority}
                onChange={(e) => setFormPriority(Number(e.target.value))}
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Trạng Thái
              </label>
              <Select<'ACTIVE' | 'INACTIVE'>
                value={formStatus}
                onChange={(val) => setFormStatus(val)}
                options={[
                  { value: 'ACTIVE', label: 'Hoạt động' },
                  { value: 'INACTIVE', label: 'Tạm dừng' },
                ]}
                size="md"
              />
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              size="md"
              onClick={() => setIsFormModalOpen(false)}
            >
              Hủy
            </Button>
            <Button type="submit" variant="primary" size="md" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : editingType ? 'Cập nhật' : 'Tạo mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa loại quy tắc giá?"
        description={
          deleteError
            ? deleteError
            : `Bạn có chắc chắn muốn xóa loại quy tắc [${deletingCode}]? Thao tác này sẽ không thể hoàn tác nếu đã được lưu.`
        }
        confirmText="Xác nhận xóa"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default PricingRuleTypeListPage
