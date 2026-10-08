import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, TrendingUp } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import pricingService from '../services/pricing.service'
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service'
import type {
  PricingRuleDto,
  PricingRuleCreateRequest,
  PricingRuleUpdateRequest,
} from '../types/pricing.types'
import type { HotelRoomTypeDto } from '../../inventory/types/inventory.types'

export const PricingRuleListPage: React.FC = () => {
  const [rules, setRules] = useState<PricingRuleDto[]>([])
  const [hotelRoomTypes, setHotelRoomTypes] = useState<HotelRoomTypeDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState<number | undefined>(undefined)
  const [ruleTypeCode, setRuleTypeCode] = useState<string>('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingRule, setEditingRule] = useState<PricingRuleDto | null>(null)
  const [formRoomTypeId, setFormRoomTypeId] = useState<number | ''>('')
  const [formRuleTypeCode, setFormRuleTypeCode] = useState('PEAK_SEASON')
  const [formAdjustmentType, setFormAdjustmentType] = useState('PERCENT')
  const [formAdjustmentValue, setFormAdjustmentValue] = useState<number>(10)
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
    hotelRoomTypeService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setHotelRoomTypes(res.result.content || [])
      })
      .catch((err) => console.error('Lỗi tải danh mục loại phòng:', err))
  }, [])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await pricingService.filterRules({
        page,
        pageSize,
        hotelRoomTypeId: selectedRoomTypeId || undefined,
        ruleTypeCode: ruleTypeCode || undefined,
        status: status || undefined,
      })
      if (response && response.result) {
        setRules(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách quy tắc giá:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, selectedRoomTypeId, ruleTypeCode, status])

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
      setSelectedIds(rules.map((r) => r.id))
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
    setEditingRule(null)
    setFormRoomTypeId(hotelRoomTypes.length > 0 ? hotelRoomTypes[0].id : '')
    setFormRuleTypeCode('PEAK_SEASON')
    setFormAdjustmentType('PERCENT')
    setFormAdjustmentValue(10)
    setFormStartDate('')
    setFormEndDate('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (rule: PricingRuleDto) => {
    setEditingRule(rule)
    setFormRoomTypeId(rule.hotelRoomTypeId)
    setFormRuleTypeCode(rule.ruleTypeCode)
    setFormAdjustmentType(rule.adjustmentType)
    setFormAdjustmentValue(rule.adjustmentValue)
    setFormStartDate(rule.startDate)
    setFormEndDate(rule.endDate)
    setFormStatus(rule.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!formRoomTypeId || !formStartDate || !formEndDate) {
      setFormError('Vui lòng điền đầy đủ ngày bắt đầu và ngày kết thúc')
      return
    }

    if (formStartDate > formEndDate) {
      setFormError('Ngày kết thúc phải sau ngày bắt đầu')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingRule) {
        const updatePayload: PricingRuleUpdateRequest = {
          adjustmentType: formAdjustmentType,
          adjustmentValue: Number(formAdjustmentValue),
          startDate: formStartDate,
          endDate: formEndDate,
          status: formStatus,
        }
        await pricingService.updateRule(editingRule.id, updatePayload)
      } else {
        const createPayload: PricingRuleCreateRequest = {
          hotelRoomTypeId: Number(formRoomTypeId),
          ruleTypeCode: formRuleTypeCode,
          adjustmentType: formAdjustmentType,
          adjustmentValue: Number(formAdjustmentValue),
          startDate: formStartDate,
          endDate: formEndDate,
          status: formStatus,
        }
        await pricingService.createRule(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const axiosError = err as {
        response?: {
          status?: number
          data?: {
            message?: string
            error?: string
            errors?: Record<string, string>
          }
        }
        message?: string
      }
      const respData = axiosError?.response?.data
      let errorMsg = respData?.message
      if (respData?.errors && Object.keys(respData.errors).length > 0) {
        errorMsg = Object.values(respData.errors).join(', ')
      }

      if (errorMsg && !errorMsg.toLowerCase().includes('status code')) {
        setFormError(errorMsg)
      } else {
        setFormError(
          'Không thể lưu quy tắc giá. Khoảng thời gian của loại quy tắc này có thể đang bị trùng lặp với quy tắc đã có trên cùng loại phòng, hoặc dữ liệu không hợp lệ.'
        )
      }
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
      await pricingService.deleteRulesBatch(deletingIds)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      setDeleteConfirmOpen(false)
      loadData()
    } catch (err) {
      console.error('Lỗi khi xóa quy tắc giá:', err)
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
            <TrendingUp className="w-7 h-7 text-red-600" />
            Cấu Hình Quy Tắc Giá Động
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Thiết lập điều chỉnh giá theo mùa vụ, ngày lễ tết, cuối tuần cho từng loại phòng
          </p>
        </div>
        <Button onClick={openCreateModal} className="shrink-0">
          <Plus className="w-5 h-5 mr-1" />
          Thêm Quy Tắc Giá
        </Button>
      </div>

      {/* 3-Row Layout: Row 1 - Search & Filter */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-4 gap-3">
          <div>
            <select
              value={selectedRoomTypeId || ''}
              onChange={(e) =>
                setSelectedRoomTypeId(e.target.value ? Number(e.target.value) : undefined)
              }
              className="w-full h-10 sm:h-12 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả loại phòng</option>
              {hotelRoomTypes.map((hrt) => (
                <option key={hrt.id} value={hrt.id}>
                  {hrt.hotelName} - {hrt.roomTypeName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <select
              value={ruleTypeCode}
              onChange={(e) => setRuleTypeCode(e.target.value)}
              className="w-full h-10 sm:h-12 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả loại quy tắc</option>
              <option value="PEAK_SEASON">Mùa cao điểm (PEAK_SEASON)</option>
              <option value="HOLIDAY">Lễ tết (HOLIDAY)</option>
              <option value="WEEKEND">Cuối tuần (WEEKEND)</option>
            </select>
          </div>

          <div>
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

          <div className="flex gap-2">
            <Button type="submit" variant="secondary" className="flex-1 h-10 sm:h-12">
              <Search className="w-4 h-4 mr-1.5" />
              Lọc
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setSelectedRoomTypeId(undefined)
                setRuleTypeCode('')
                setStatus('')
                setPage(0)
              }}
              className="h-10 sm:h-12"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
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
              Xóa {selectedIds.length} quy tắc đã chọn
            </Button>
          )}
        </div>
        <div className="text-sm text-neutral-500">
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> quy tắc
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
                    checked={rules.length > 0 && selectedIds.length === rules.length}
                    onChange={handleSelectAll}
                    aria-label="Chọn tất cả quy tắc"
                    className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                  />
                </th>
                <th className="py-3.5 px-4 font-semibold">Loại Phòng Áp Dụng</th>
                <th className="py-3.5 px-4 font-semibold">Loại Quy Tắc</th>
                <th className="py-3.5 px-4 font-semibold">Mức Điều Chỉnh</th>
                <th className="py-3.5 px-4 font-semibold">Thời Gian Hiệu Lực</th>
                <th className="py-3.5 px-4 font-semibold">Trạng Thái</th>
                <th className="py-3.5 px-4 font-semibold text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200/70">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-500">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải danh sách quy tắc...
                    </div>
                  </td>
                </tr>
              ) : rules.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-500">
                    Không tìm thấy quy tắc giá nào phù hợp.
                  </td>
                </tr>
              ) : (
                rules.map((rule) => {
                  const isSelected = selectedIds.includes(rule.id)
                  const isPositive = rule.adjustmentValue >= 0
                  return (
                    <tr
                      key={rule.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/20' : ''
                      }`}
                    >
                      <td className="py-3 px-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(rule.id)}
                          aria-label={`Chọn quy tắc ${rule.id}`}
                          className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                        />
                      </td>
                      <td className="py-3 px-4 font-medium text-neutral-900">
                        {rule.roomTypeName}
                      </td>
                      <td className="py-3 px-4">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-neutral-100 text-neutral-800">
                          {rule.ruleTypeName || rule.ruleTypeCode}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`font-semibold ${
                            isPositive ? 'text-red-600' : 'text-emerald-600'
                          }`}
                        >
                          {isPositive ? '+' : ''}
                          {rule.adjustmentValue}
                          {rule.adjustmentType === 'PERCENT' ? '%' : ' đ'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-neutral-600 text-xs">
                        {rule.startDate} ➔ {rule.endDate}
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                            rule.status === 'ACTIVE'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                          }`}
                        >
                          {rule.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEditModal(rule)}
                            aria-label={`Chỉnh sửa quy tắc ${rule.id}`}
                          >
                            <Edit2 className="w-4 h-4 text-neutral-600" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => promptDeleteSingle(rule.id)}
                            aria-label={`Xóa quy tắc ${rule.id}`}
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

      {/* Form Modal */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingRule ? 'Chỉnh Sửa Quy Tắc Giá' : 'Thêm Mới Quy Tắc Giá'}
      >
        <form onSubmit={handleSubmitForm} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 text-red-600 text-sm rounded-xl border border-red-200">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Loại Phòng Áp Dụng <span className="text-red-500">*</span>
            </label>
            <select
              value={formRoomTypeId}
              onChange={(e) => setFormRoomTypeId(Number(e.target.value))}
              disabled={!!editingRule}
              className="w-full h-11 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
              required
            >
              <option value="">Chọn loại phòng</option>
              {hotelRoomTypes.map((hrt) => (
                <option key={hrt.id} value={hrt.id}>
                  {hrt.hotelName} - {hrt.roomTypeName}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Loại Quy Tắc
              </label>
              <select
                value={formRuleTypeCode}
                onChange={(e) => setFormRuleTypeCode(e.target.value)}
                disabled={!!editingRule}
                className="w-full h-11 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
              >
                <option value="PEAK_SEASON">Mùa cao điểm (PEAK_SEASON)</option>
                <option value="HOLIDAY">Lễ tết (HOLIDAY)</option>
                <option value="WEEKEND">Cuối tuần (WEEKEND)</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Kiểu Điều Chỉnh
              </label>
              <select
                value={formAdjustmentType}
                onChange={(e) => setFormAdjustmentType(e.target.value)}
                className="w-full h-11 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
              >
                <option value="PERCENT">Phần trăm (%)</option>
                <option value="FIXED">Số tiền cố định (VNĐ)</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Giá Trị Điều Chỉnh <span className="text-red-500">*</span>
            </label>
            <Input
              type="number"
              step="any"
              placeholder="VD: 15 hoặc -10..."
              value={formAdjustmentValue}
              onChange={(e) => setFormAdjustmentValue(Number(e.target.value))}
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
                Ngày Kết Thúc <span className="text-red-500">*</span>
              </label>
              <Input
                type="date"
                value={formEndDate}
                onChange={(e) => setFormEndDate(e.target.value)}
                required
              />
            </div>
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
              {editingRule ? 'Cập Nhật' : 'Tạo Mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa quy tắc giá"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} quy tắc giá đã chọn? Thao tác này sẽ chuyển dữ liệu vào thùng rác.`}
        confirmText="Xác nhận xóa"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default PricingRuleListPage
