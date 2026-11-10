import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Percent, Tag, Calendar, AlertCircle } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import discountRuleService from '../services/discountRule.service'
import discountRuleTypeService from '../services/discountRuleType.service'
import campaignService from '../services/campaign.service'
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service'
import hotelService from '../../organization/services/hotel.service'
import type {
  DiscountRuleDto,
  DiscountRuleCreateRequest,
  DiscountRuleUpdateRequest,
  DiscountRuleTypeDto,
  CampaignDto,
} from '../types/pricing.types'
import type { HotelRoomTypeDto } from '../../inventory/types/inventory.types'
import type { HotelDto } from '../../organization/types/organization.types'

export const DiscountRuleListPage: React.FC = () => {
  const [rules, setRules] = useState<DiscountRuleDto[]>([])
  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [hotelRoomTypes, setHotelRoomTypes] = useState<HotelRoomTypeDto[]>([])
  const [ruleTypes, setRuleTypes] = useState<DiscountRuleTypeDto[]>([])
  const [campaigns, setCampaigns] = useState<CampaignDto[]>([])

  // Pagination & Filter State
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [filterHotelId, setFilterHotelId] = useState<number | undefined>(undefined)
  const [filterRoomTypeId, setFilterRoomTypeId] = useState<number | undefined>(undefined)
  const [filterCampaignId, setFilterCampaignId] = useState<number | undefined>(undefined)
  const [filterRuleTypeCode, setFilterRuleTypeCode] = useState<string>('')
  const [filterStatus, setFilterStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingRule, setEditingRule] = useState<DiscountRuleDto | null>(null)
  const [formRoomTypeId, setFormRoomTypeId] = useState<number | ''>('')
  const [formRuleTypeCode, setFormRuleTypeCode] = useState('')
  const [formCampaignId, setFormCampaignId] = useState<number | ''>('')
  const [formDiscountType, setFormDiscountType] = useState<'PERCENT' | 'FIXED'>('PERCENT')
  const [formDiscountValue, setFormDiscountValue] = useState<number>(10)
  const [formStartDate, setFormStartDate] = useState('')
  const [formEndDate, setFormEndDate] = useState('')
  const [formStatus, setFormStatus] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Load auxiliary data on mount
  useEffect(() => {
    hotelService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setHotels(res.result.content || [])
      })
      .catch((err) => console.error('Lỗi tải danh mục khách sạn:', err))

    hotelRoomTypeService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setHotelRoomTypes(res.result.content || [])
      })
      .catch((err) => console.error('Lỗi tải danh mục loại phòng:', err))

    discountRuleTypeService
      .getActive()
      .then((res) => {
        if (res.result) setRuleTypes(res.result)
      })
      .catch((err) => console.error('Lỗi tải loại quy tắc giảm giá:', err))

    campaignService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setCampaigns(res.result.content || [])
      })
      .catch((err) => console.error('Lỗi tải danh sách chiến dịch:', err))
  }, [])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await discountRuleService.filter({
        page,
        pageSize,
        hotelId: filterHotelId || undefined,
        hotelRoomTypeId: filterRoomTypeId || undefined,
        campaignId: filterCampaignId || undefined,
        ruleTypeCode: filterRuleTypeCode || undefined,
        status: filterStatus || undefined,
      })
      if (response && response.result) {
        setRules(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách quy tắc giảm giá:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, filterHotelId, filterRoomTypeId, filterCampaignId, filterRuleTypeCode, filterStatus])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    void loadData()
  }

  const handleResetFilter = () => {
    setFilterHotelId(undefined)
    setFilterRoomTypeId(undefined)
    setFilterCampaignId(undefined)
    setFilterRuleTypeCode('')
    setFilterStatus('')
    setPage(0)
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

  // Find currently selected campaign in modal to enforce date range constraint
  const selectedCampaignInForm = campaigns.find(
    (c) => c.id === (formCampaignId ? Number(formCampaignId) : undefined)
  )

  const isCampaignRequired =
    formRuleTypeCode === 'SPECIAL_CAMPAIGN' || Boolean(formCampaignId)

  const openCreateModal = () => {
    setEditingRule(null)
    setFormRoomTypeId(hotelRoomTypes.length > 0 ? hotelRoomTypes[0].id : '')
    setFormRuleTypeCode(ruleTypes.length > 0 ? ruleTypes[0].code : 'SPECIAL_CAMPAIGN')
    setFormCampaignId('')
    setFormDiscountType('PERCENT')
    setFormDiscountValue(10)
    setFormStartDate('')
    setFormEndDate('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (rule: DiscountRuleDto) => {
    setEditingRule(rule)
    setFormRoomTypeId(rule.hotelRoomTypeId)
    setFormRuleTypeCode(rule.ruleTypeCode)
    setFormCampaignId(rule.campaignId || '')
    setFormDiscountType(rule.discountType)
    setFormDiscountValue(rule.discountValue)
    setFormStartDate(rule.startDate)
    setFormEndDate(rule.endDate)
    setFormStatus(rule.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!formRoomTypeId || !formRuleTypeCode || !formStartDate || !formEndDate) {
      setFormError('Vui lòng điền đầy đủ các thông tin bắt buộc')
      return
    }

    if (formStartDate > formEndDate) {
      setFormError('Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu')
      return
    }

    if (formDiscountType === 'PERCENT') {
      if (formDiscountValue <= 0 || formDiscountValue > 100) {
        setFormError('Giá trị giảm giá theo phần trăm phải nằm trong khoảng từ 0.01% đến 100%')
        return
      }
    } else {
      if (formDiscountValue <= 0) {
        setFormError('Số tiền giảm giá cố định phải lớn hơn 0 VNĐ')
        return
      }
    }

    // Strict Campaign Date Scope Validation
    if (formRuleTypeCode === 'SPECIAL_CAMPAIGN' && !formCampaignId) {
      setFormError('Loại quy tắc SPECIAL_CAMPAIGN bắt buộc phải chọn một Chiến dịch khuyến mại liên kết')
      return
    }

    if (selectedCampaignInForm) {
      if (
        formStartDate < selectedCampaignInForm.startDate ||
        formEndDate > selectedCampaignInForm.endDate
      ) {
        setFormError(
          `Thời gian hiệu lực của quy tắc giảm giá phải nằm trong khoảng thời gian diễn ra của chiến dịch [${selectedCampaignInForm.name}] (${selectedCampaignInForm.startDate} đến ${selectedCampaignInForm.endDate})`
        )
        return
      }
    }

    setIsSubmitting(true)
    try {
      if (editingRule) {
        const updatePayload: DiscountRuleUpdateRequest = {
          campaignId: formCampaignId ? Number(formCampaignId) : undefined,
          discountType: formDiscountType,
          discountValue: Number(formDiscountValue),
          startDate: formStartDate,
          endDate: formEndDate,
          status: formStatus,
        }
        await discountRuleService.update(editingRule.id, updatePayload)
      } else {
        const createPayload: DiscountRuleCreateRequest = {
          hotelRoomTypeId: Number(formRoomTypeId),
          ruleTypeCode: formRuleTypeCode,
          campaignId: formCampaignId ? Number(formCampaignId) : undefined,
          discountType: formDiscountType,
          discountValue: Number(formDiscountValue),
          startDate: formStartDate,
          endDate: formEndDate,
          status: formStatus,
        }
        await discountRuleService.create(createPayload)
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
      setFormError(errorMsg || 'Không thể lưu quy tắc giảm giá. Vui lòng thử lại.')
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
    if (deletingIds.length === 0) return
    setIsDeleting(true)
    try {
      await discountRuleService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      loadData()
    } catch (err) {
      console.error('Lỗi khi xóa quy tắc giảm giá:', err)
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
            <Percent className="w-7 h-7 text-red-600" />
            Quy Tắc Giảm Giá (Discount Rules)
          </h1>
          <p className="text-neutral-500 text-sm mt-1">
            Quản lý các chương trình chiết khấu, giảm giá phòng theo chiến dịch hoặc loại ưu đãi
          </p>
        </div>
      </div>

      {/* Standard 3-Row Layout Container */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-sm overflow-hidden">
        {/* ROW 1: SEARCH & FILTER BAR */}
        <div className="p-4 border-b border-neutral-100 bg-neutral-50/50">
          <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
            {/* Filter Hotel */}
            <div>
              <Select<number | ''>
                value={filterHotelId || ''}
                onChange={(val) => {
                  setFilterHotelId(val ? Number(val) : undefined)
                  setFilterRoomTypeId(undefined)
                }}
                options={[
                  { value: '', label: 'Tất cả khách sạn' },
                  ...hotels.map((h) => ({ value: h.id, label: h.name })),
                ]}
                size="md"
              />
            </div>

            {/* Filter Room Type */}
            <div>
              <Select<number | ''>
                value={filterRoomTypeId || ''}
                onChange={(val) => setFilterRoomTypeId(val ? Number(val) : undefined)}
                options={[
                  { value: '', label: 'Tất cả loại phòng' },
                  ...hotelRoomTypes
                    .filter((rt) => !filterHotelId || rt.hotelId === filterHotelId)
                    .map((rt) => ({
                      value: rt.id,
                      label: `${rt.hotelName} - ${rt.roomTypeName}`,
                    })),
                ]}
                size="md"
              />
            </div>

            {/* Filter Rule Type */}
            <div>
              <Select<string>
                value={filterRuleTypeCode}
                onChange={(val) => setFilterRuleTypeCode(val)}
                options={[
                  { value: '', label: 'Tất cả loại ưu đãi' },
                  ...ruleTypes.map((rt) => ({
                    value: rt.code,
                    label: `${rt.displayName} (${rt.code})`,
                  })),
                ]}
                size="md"
              />
            </div>

            {/* Filter Campaign */}
            <div>
              <Select<number | ''>
                value={filterCampaignId || ''}
                onChange={(val) => setFilterCampaignId(val ? Number(val) : undefined)}
                options={[
                  { value: '', label: 'Tất cả chiến dịch' },
                  ...campaigns.map((c) => ({
                    value: c.id,
                    label: c.name,
                  })),
                ]}
                size="md"
              />
            </div>

            {/* Filter Status & Buttons */}
            <div className="flex items-center gap-2">
              <Select<string>
                value={filterStatus}
                onChange={(val) => setFilterStatus(val)}
                options={[
                  { value: '', label: 'Tất cả trạng thái' },
                  { value: 'ACTIVE', label: 'Hoạt động' },
                  { value: 'INACTIVE', label: 'Tạm dừng' },
                ]}
                size="md"
              />

              <Button type="submit" variant="primary" size="md" className="shrink-0">
                <Search className="w-4 h-4 mr-1" />
                Lọc
              </Button>
              <Button
                type="button"
                variant="outline"
                size="md"
                onClick={handleResetFilter}
                className="shrink-0"
              >
                Đặt lại
              </Button>
            </div>
          </form>
        </div>

        {/* ROW 2: ACTION TOOLBAR */}
        <div className="px-4 py-3 border-b border-neutral-100 flex flex-col sm:flex-row gap-3 items-center justify-between bg-white">
          <div className="flex items-center gap-3">
            <span className="text-sm text-neutral-600 font-medium">
              Tổng số: <span className="text-neutral-900 font-semibold">{totalElements}</span> quy tắc
            </span>
            {selectedIds.length > 0 && (
              <Button
                variant="danger"
                size="sm"
                onClick={promptDeleteBatch}
                className="flex items-center gap-1.5"
              >
                <Trash2 className="w-4 h-4" />
                Xóa {selectedIds.length} mục đã chọn
              </Button>
            )}
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="md"
              onClick={() => void loadData()}
              disabled={isLoading}
            >
              <RefreshCw className={`w-4 h-4 mr-1.5 ${isLoading ? 'animate-spin' : ''}`} />
              Làm mới
            </Button>
            <Button variant="primary" size="md" onClick={openCreateModal}>
              <Plus className="w-4 h-4 mr-1.5" />
              Thêm Quy Tắc Giảm Giá
            </Button>
          </div>
        </div>

        {/* ROW 3: DATA TABLE */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-50/75 text-neutral-600 font-semibold text-xs uppercase tracking-wider">
                <th className="py-3 px-4 w-10">
                  <input
                    type="checkbox"
                    className="rounded border-neutral-300 text-red-600 focus:ring-red-500"
                    onChange={handleSelectAll}
                    checked={rules.length > 0 && selectedIds.length === rules.length}
                    aria-label="Chọn tất cả"
                  />
                </th>
                <th className="py-3 px-4">Loại Phòng & Khách Sạn</th>
                <th className="py-3 px-4">Loại Ưu Đãi</th>
                <th className="py-3 px-4">Chiến Dịch Liên Kết</th>
                <th className="py-3 px-4">Mức Giảm Giá</th>
                <th className="py-3 px-4">Thời Gian Hiệu Lực</th>
                <th className="py-3 px-4">Trạng Thái</th>
                <th className="py-3 px-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-neutral-500">
                    <div className="flex items-center justify-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải danh sách...
                    </div>
                  </td>
                </tr>
              ) : rules.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-neutral-500">
                    Không tìm thấy quy tắc giảm giá nào phù hợp.
                  </td>
                </tr>
              ) : (
                rules.map((rule) => {
                  const isSelected = selectedIds.includes(rule.id)
                  return (
                    <tr
                      key={rule.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="py-3 px-4">
                        <input
                          type="checkbox"
                          className="rounded border-neutral-300 text-red-600 focus:ring-red-500"
                          checked={isSelected}
                          onChange={() => handleSelectOne(rule.id)}
                          aria-label={`Chọn quy tắc ${rule.id}`}
                        />
                      </td>
                      <td className="py-3 px-4">
                        <div className="font-medium text-neutral-900">{rule.roomTypeName}</div>
                        <div className="text-xs text-neutral-500">{rule.hotelName}</div>
                      </td>
                      <td className="py-3 px-4">
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-medium bg-neutral-100 text-neutral-800">
                          <Tag className="w-3 h-3 text-neutral-500" />
                          {rule.ruleTypeName || rule.ruleTypeCode}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        {rule.campaignName ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-red-50 text-red-700 border border-red-100">
                            {rule.campaignName}
                          </span>
                        ) : (
                          <span className="text-neutral-400 text-xs">—</span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <span className="font-semibold text-emerald-600">
                          {rule.discountType === 'PERCENT'
                            ? `-${rule.discountValue}%`
                            : `-${Number(rule.discountValue).toLocaleString('vi-VN')} đ`}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-neutral-600 text-xs">
                        <div className="flex items-center gap-1">
                          <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                          <span>{rule.startDate}</span>
                          <span>➔</span>
                          <span>{rule.endDate}</span>
                        </div>
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
                            aria-label={`Sửa quy tắc ${rule.id}`}
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

      {/* Form Modal (Create / Edit) */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingRule ? 'Chỉnh Sửa Quy Tắc Giảm Giá' : 'Thêm Mới Quy Tắc Giảm Giá'}
      >
        <form onSubmit={handleSubmitForm} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 text-red-600 text-sm rounded-xl border border-red-200 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          {/* Room Type */}
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Hạng Phòng Áp Dụng <span className="text-red-500">*</span>
            </label>
            <Select<number | ''>
              value={formRoomTypeId || ''}
              onChange={(val) => setFormRoomTypeId(Number(val))}
              disabled={!!editingRule}
              options={[
                { value: '', label: 'Chọn hạng phòng', disabled: true },
                ...hotelRoomTypes.map((hrt) => ({
                  value: hrt.id,
                  label: `${hrt.hotelName} - ${hrt.roomTypeName}`,
                })),
              ]}
              size="md"
            />
          </div>

          {/* Rule Type */}
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Loại Ưu Đãi (Discount Rule Type) <span className="text-red-500">*</span>
            </label>
            <Select<string>
              value={formRuleTypeCode}
              onChange={(val) => {
                setFormRuleTypeCode(val)
                // If switching to non-campaign and user hasn't selected campaign, keep clean
              }}
              disabled={!!editingRule}
              options={
                ruleTypes.length > 0
                  ? ruleTypes.map((rt) => ({
                      value: rt.code,
                      label: `${rt.displayName} (${rt.code})`,
                    }))
                  : [
                      { value: 'SPECIAL_CAMPAIGN', label: 'Chiến dịch khuyến mại (SPECIAL_CAMPAIGN)' },
                      { value: 'LOYALTY', label: 'Khách hàng thân thiết (LOYALTY)' },
                      { value: 'EARLY_BIRD', label: 'Đặt sớm (EARLY_BIRD)' },
                      { value: 'LAST_MINUTE', label: 'Giờ chót (LAST_MINUTE)' },
                    ]
              }
              size="md"
            />
          </div>

          {/* Campaign Selection */}
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Chiến Dịch Khuyến Mại Liên Kết{' '}
              {isCampaignRequired ? (
                <span className="text-red-500">* (Bắt buộc cho loại ưu đãi này)</span>
              ) : (
                <span className="text-neutral-400 font-normal">(Tùy chọn)</span>
              )}
            </label>
            <Select<number | ''>
              value={formCampaignId || ''}
              onChange={(val) => {
                const cId = val ? Number(val) : ''
                setFormCampaignId(cId)
                if (cId) {
                  const camp = campaigns.find((c) => c.id === cId)
                  if (camp) {
                    // Set default dates to campaign dates if empty or outside
                    if (!formStartDate || formStartDate < camp.startDate) setFormStartDate(camp.startDate)
                    if (!formEndDate || formEndDate > camp.endDate) setFormEndDate(camp.endDate)
                  }
                }
              }}
              options={[
                { value: '', label: 'Không liên kết chiến dịch' },
                ...campaigns.map((c) => ({
                  value: c.id,
                  label: `${c.hotelName ? c.hotelName + ' - ' : ''}${c.name} (${c.startDate} đến ${c.endDate})`,
                })),
              ]}
              size="md"
            />
            {selectedCampaignInForm && (
              <div className="mt-1.5 p-2.5 bg-neutral-50 rounded-lg border border-neutral-200 text-xs text-neutral-600 flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5 text-red-600 shrink-0" />
                <span>
                  Thời hạn chiến dịch: <strong>{selectedCampaignInForm.startDate}</strong> đến{' '}
                  <strong>{selectedCampaignInForm.endDate}</strong>. Ngày áp dụng quy tắc giảm giá bắt buộc phải nằm trong khoảng này.
                </span>
              </div>
            )}
          </div>

          {/* Discount Type & Value */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Kiểu Giảm Giá <span className="text-red-500">*</span>
              </label>
              <Select<'PERCENT' | 'FIXED'>
                value={formDiscountType}
                onChange={(val) => setFormDiscountType(val)}
                options={[
                  { value: 'PERCENT', label: 'Phần trăm (%)' },
                  { value: 'FIXED', label: 'Số tiền cố định (VNĐ)' },
                ]}
                size="md"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Giá Trị Giảm Giá ({formDiscountType === 'PERCENT' ? '%' : 'VNĐ'}) <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                step="any"
                min="0.01"
                max={formDiscountType === 'PERCENT' ? '100' : undefined}
                placeholder={formDiscountType === 'PERCENT' ? 'VD: 15' : 'VD: 200000'}
                value={formDiscountValue}
                onChange={(e) => setFormDiscountValue(Number(e.target.value))}
                required
              />
            </div>
          </div>

          {/* Date Range with Campaign Scope Constraints */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Ngày Bắt Đầu <span className="text-red-500">*</span>
              </label>
              <Input
                type="date"
                value={formStartDate}
                min={selectedCampaignInForm?.startDate}
                max={selectedCampaignInForm?.endDate}
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
                min={formStartDate || selectedCampaignInForm?.startDate}
                max={selectedCampaignInForm?.endDate}
                onChange={(e) => setFormEndDate(e.target.value)}
                required
              />
            </div>
          </div>

          {/* Status */}
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

          {/* Modal Actions */}
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
              {isSubmitting ? 'Đang lưu...' : editingRule ? 'Cập nhật' : 'Tạo mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa quy tắc giảm giá?"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} quy tắc giảm giá đã chọn? Thao tác này sẽ chuyển quy tắc vào thùng rác.`}
        confirmText="Xác nhận xóa"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default DiscountRuleListPage
