import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Percent } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import vatRuleService from '../services/vatRule.service'
import taxCategoryService from '../services/taxCategory.service'
import type {
  VatRuleDto,
  VatRuleCreateRequest,
  VatRuleUpdateRequest,
  TaxCategoryDto,
} from '../types/pricing.types'

export const VatRuleListPage: React.FC = () => {
  const [vatRules, setVatRules] = useState<VatRuleDto[]>([])
  const [taxCategories, setTaxCategories] = useState<TaxCategoryDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [selectedTaxCategoryId, setSelectedTaxCategoryId] = useState<number | ''>('')
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingVatRule, setEditingVatRule] = useState<VatRuleDto | null>(null)
  const [formTaxCategoryId, setFormTaxCategoryId] = useState<number | ''>('')
  const [formVatCode, setFormVatCode] = useState('')
  const [formVatName, setFormVatName] = useState('')
  const [formVatPercent, setFormVatPercent] = useState<number | ''>(10)
  const [formStartDate, setFormStartDate] = useState('')
  const [formEndDate, setFormEndDate] = useState('')
  const [formStatus, setFormStatus] = useState('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => {
      taxCategoryService.getAllActive().then(setTaxCategories).catch(console.error)
    }, 0)
    return () => clearTimeout(timer)
  }, [])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const res = await vatRuleService.filter({
        page,
        pageSize,
        taxCategoryId: selectedTaxCategoryId ? Number(selectedTaxCategoryId) : undefined,
        keyword: keyword.trim() || undefined,
        status: status || undefined,
        sortBy: 'startDate',
        sortDirection: 'DESC',
      })
      if (res.result) {
        setVatRules(res.result.content || [])
        setTotalElements(res.result.totalElements)
        setTotalPages(res.result.totalPages)
      }
    } catch (err) {
      console.error('Lỗi tải danh sách quy tắc VAT:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, selectedTaxCategoryId, keyword, status])

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
    setSelectedTaxCategoryId('')
    setKeyword('')
    setStatus('')
    setPage(0)
  }

  const handleOpenCreateModal = () => {
    setEditingVatRule(null)
    setFormTaxCategoryId(taxCategories.length > 0 ? taxCategories[0].id : '')
    setFormVatCode('')
    setFormVatName('')
    setFormVatPercent(10)
    setFormStartDate(new Date().toISOString().split('T')[0])
    setFormEndDate('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleOpenEditModal = (rule: VatRuleDto) => {
    setEditingVatRule(rule)
    setFormTaxCategoryId(rule.taxCategoryId)
    setFormVatCode(rule.vatCode)
    setFormVatName(rule.vatName)
    setFormVatPercent(rule.vatPercent)
    setFormStartDate(rule.startDate)
    setFormEndDate(rule.endDate || '')
    setFormStatus(rule.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!formTaxCategoryId) {
      setFormError('Vui lòng chọn nhóm thuế')
      return
    }
    if (!editingVatRule && !formVatCode.trim()) {
      setFormError('Mã quy tắc VAT không được để trống')
      return
    }
    if (!formVatName.trim()) {
      setFormError('Tên quy tắc VAT không được để trống')
      return
    }
    if (formVatPercent === '' || formVatPercent < 0 || formVatPercent > 100) {
      setFormError('Thuế suất VAT phải nằm trong khoảng từ 0% đến 100%')
      return
    }
    if (!formStartDate) {
      setFormError('Ngày bắt đầu không được để trống')
      return
    }
    if (formEndDate && formEndDate < formStartDate) {
      setFormError('Ngày kết thúc không được trước ngày bắt đầu')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingVatRule) {
        const updatePayload: VatRuleUpdateRequest = {
          vatName: formVatName.trim(),
          vatPercent: Number(formVatPercent),
          startDate: formStartDate,
          endDate: formEndDate || undefined,
          status: formStatus,
        }
        await vatRuleService.update(editingVatRule.id, updatePayload)
      } else {
        const createPayload: VatRuleCreateRequest = {
          taxCategoryId: Number(formTaxCategoryId),
          vatCode: formVatCode.trim().toUpperCase(),
          vatName: formVatName.trim(),
          vatPercent: Number(formVatPercent),
          startDate: formStartDate,
          endDate: formEndDate || undefined,
        }
        await vatRuleService.create(createPayload)
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
      setSelectedIds(vatRules.map((r) => r.id))
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
      await vatRuleService.deleteBatch(deletingIds)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      setDeleteConfirmOpen(false)
      setDeletingIds([])
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
      alert(axiosError.response?.data?.message || axiosError.message || 'Xóa quy tắc VAT thất bại')
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
            <Percent className="w-7 h-7 text-red-600" />
            Quy Tắc Thuế Suất VAT (VAT Rules)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Cấu hình tỷ lệ thuế GTGT theo mốc thời gian và nhóm hàng hóa/dịch vụ theo quy định pháp luật Việt Nam.
          </p>
        </div>
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleFilter} className="grid grid-cols-1 md:grid-cols-5 gap-3">
          <div>
            <Select<string | number>
              value={selectedTaxCategoryId}
              onChange={(val) => setSelectedTaxCategoryId(val ? Number(val) : '')}
              options={[
                { value: '', label: 'Tất cả nhóm thuế' },
                ...taxCategories.map((t) => ({ value: t.id, label: `${t.categoryCode} - ${t.categoryName}` })),
              ]}
              size="md"
            />
          </div>

          <div className="md:col-span-2">
            <Input
              placeholder="Tìm theo mã hoặc tên quy tắc VAT..."
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
            Thêm Quy Tắc VAT
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
          Tổng số: <span className="font-semibold text-neutral-900">{totalElements}</span> quy tắc VAT
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
                    checked={vatRules.length > 0 && selectedIds.length === vatRules.length}
                    onChange={handleSelectAll}
                  />
                </th>
                <th className="py-3.5 px-4">Nhóm Thuế</th>
                <th className="py-3.5 px-4">Mã Quy Tắc</th>
                <th className="py-3.5 px-4">Tên Quy Tắc VAT</th>
                <th className="py-3.5 px-4 text-center">Thuế Suất</th>
                <th className="py-3.5 px-4">Thời Gian Hiệu Lực</th>
                <th className="py-3.5 px-4">Trạng Thái</th>
                <th className="py-3.5 px-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200 text-sm">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-neutral-400">
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : vatRules.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-neutral-400">
                    Không tìm thấy quy tắc thuế VAT nào phù hợp
                  </td>
                </tr>
              ) : (
                vatRules.map((rule) => (
                  <tr key={rule.id} className="hover:bg-neutral-50 transition-colors">
                    <td className="py-3.5 px-4 text-center">
                      <input
                        type="checkbox"
                        className="rounded border-neutral-300 text-red-600 focus:ring-red-600"
                        checked={selectedIds.includes(rule.id)}
                        onChange={() => handleSelectOne(rule.id)}
                      />
                    </td>
                    <td className="py-3.5 px-4 font-medium text-neutral-900">
                      {rule.taxCategoryName || rule.taxCategoryCode || `ID #${rule.taxCategoryId}`}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-medium text-neutral-900">
                      {rule.vatCode}
                    </td>
                    <td className="py-3.5 px-4 font-medium text-neutral-900">
                      {rule.vatName}
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-50 text-red-600 border border-red-200">
                        {rule.vatPercent}%
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-xs font-mono text-neutral-600">
                      {rule.startDate} {rule.endDate ? `→ ${rule.endDate}` : '→ Không thời hạn'}
                    </td>
                    <td className="py-3.5 px-4">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          rule.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                        }`}
                      >
                        {rule.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleOpenEditModal(rule)}
                          title="Chỉnh sửa"
                        >
                          <Edit2 className="w-4 h-4 text-neutral-600" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setDeletingIds([rule.id])
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
        title={editingVatRule ? 'Chỉnh Sửa Quy Tắc Thuế VAT' : 'Tạo Mới Quy Tắc Thuế VAT'}
      >
        <form onSubmit={handleFormSubmit} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Nhóm Thuế Liên Kết <span className="text-red-500">*</span>
            </label>
            <Select<number | string>
              value={formTaxCategoryId}
              onChange={(val) => setFormTaxCategoryId(Number(val))}
              options={taxCategories.map((t) => ({
                value: t.id,
                label: `${t.categoryCode} - ${t.categoryName}`,
              }))}
              disabled={!!editingVatRule}
              size="md"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Mã Quy Tắc VAT <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: VAT_ROOM_8, VAT_FB_10..."
              value={formVatCode}
              onChange={(e) => setFormVatCode(e.target.value.toUpperCase())}
              disabled={!!editingVatRule}
              required
            />
            {editingVatRule && (
              <p className="text-xs text-neutral-400 mt-1">Mã quy tắc VAT không thể sửa đổi sau khi tạo.</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Tên Quy Tắc VAT <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: Thuế GTGT 8% theo Nghị định 72/2024..."
              value={formVatName}
              onChange={(e) => setFormVatName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Thuế Suất VAT (%) <span className="text-red-500">*</span>
            </label>
            <Input
              type="number"
              step="0.01"
              min="0"
              max="100"
              placeholder="10.00"
              value={formVatPercent}
              onChange={(e) => setFormVatPercent(e.target.value === '' ? '' : Number(e.target.value))}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Ngày Bắt Đầu <span className="text-red-500">*</span>
              </label>
              <Input
                type="date"
                value={formStartDate}
                onChange={(e) => setFormStartDate(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Ngày Kết Thúc (Tùy chọn)
              </label>
              <Input
                type="date"
                value={formEndDate}
                onChange={(e) => setFormEndDate(e.target.value)}
              />
            </div>
          </div>

          {editingVatRule && (
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
              {editingVatRule ? 'Cập Nhật' : 'Tạo Mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa quy tắc VAT"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} quy tắc VAT đã chọn?`}
        confirmText="Xác nhận xóa"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default VatRuleListPage
