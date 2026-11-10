import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Percent, Tag, Calendar, AlertCircle, Layers, Zap } from 'lucide-react'
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
  DiscountTierItem,
  DiscountConditions,
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

  // Tiers State for LONG_STAY / Tiered Discount
  const [isTieredMode, setIsTieredMode] = useState(false)
  const [formTiers, setFormTiers] = useState<DiscountTierItem[]>([
    { minNights: 3, maxNights: 6, discountType: 'PERCENT', discountValue: 10 },
    { minNights: 7, maxNights: 13, discountType: 'PERCENT', discountValue: 15 },
    { minNights: 14, maxNights: null, discountType: 'PERCENT', discountValue: 20 },
  ])

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

  const STANDARD_TIERS: DiscountTierItem[] = [
    { minNights: 3, maxNights: 6, discountType: 'PERCENT', discountValue: 10 },
    { minNights: 7, maxNights: 13, discountType: 'PERCENT', discountValue: 15 },
    { minNights: 14, maxNights: null, discountType: 'PERCENT', discountValue: 20 },
  ]

  const WEEKLY_TIERS: DiscountTierItem[] = [
    { minNights: 7, maxNights: 13, discountType: 'PERCENT', discountValue: 15 },
    { minNights: 14, maxNights: 29, discountType: 'PERCENT', discountValue: 20 },
    { minNights: 30, maxNights: null, discountType: 'PERCENT', discountValue: 25 },
  ]

  const parseTiers = (conditionsJson?: string): DiscountTierItem[] | null => {
    if (!conditionsJson || conditionsJson.trim() === '' || conditionsJson === '{}') return null
    try {
      const parsed = JSON.parse(conditionsJson)
      if (parsed.tiers && Array.isArray(parsed.tiers) && parsed.tiers.length > 0) {
        return parsed.tiers
      }
    } catch {
      // ignore
    }
    return null
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
    const defaultRuleType = ruleTypes.length > 0 ? ruleTypes[0].code : 'SPECIAL_CAMPAIGN'
    setFormRuleTypeCode(defaultRuleType)
    setIsTieredMode(defaultRuleType === 'LONG_STAY')
    setFormTiers(STANDARD_TIERS)
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

    const existingTiers = parseTiers(rule.conditions)
    if (existingTiers && existingTiers.length > 0) {
      setFormTiers(existingTiers)
      setIsTieredMode(true)
    } else if (rule.ruleTypeCode === 'LONG_STAY') {
      setIsTieredMode(true)
      setFormTiers(STANDARD_TIERS)
    } else {
      setIsTieredMode(false)
      setFormTiers(STANDARD_TIERS)
    }

    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleApplyTemplate = (type: 'standard' | 'weekly') => {
    setFormTiers(type === 'standard' ? [...STANDARD_TIERS] : [...WEEKLY_TIERS])
  }

  const handleAddTier = () => {
    setFormTiers((prev) => {
      if (prev.length === 0) {
        return [{ minNights: 3, maxNights: 6, discountType: 'PERCENT', discountValue: 10 }]
      }
      const last = prev[prev.length - 1]
      const nextMin = (last.maxNights ?? last.minNights) + 1
      const updatedPrev = prev.map((t, idx) => {
        if (idx === prev.length - 1 && t.maxNights == null) {
          return { ...t, maxNights: nextMin - 1 }
        }
        return t
      })
      return [
        ...updatedPrev,
        {
          minNights: nextMin,
          maxNights: null,
          discountType: last.discountType,
          discountValue: last.discountValue + 5,
        },
      ]
    })
  }

  const handleUpdateTier = (
    index: number,
    field: keyof DiscountTierItem,
    value: string | number | null
  ) => {
    setFormTiers((prev) =>
      prev.map((t, i) => (i === index ? { ...t, [field]: value } : t))
    )
  }

  const handleRemoveTier = (index: number) => {
    setFormTiers((prev) => prev.filter((_, i) => i !== index))
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

    let conditionsStr = '{}'
    let effectiveDiscountType = formDiscountType
    let effectiveDiscountValue = Number(formDiscountValue)

    if (isTieredMode) {
      if (formTiers.length === 0) {
        setFormError('Vui lòng thêm ít nhất 1 bậc lưu trú')
        return
      }

      for (let i = 0; i < formTiers.length; i++) {
        const tier = formTiers[i]
        if (!tier.minNights || tier.minNights < 1) {
          setFormError(`Bậc ${i + 1}: Số đêm tối thiểu phải từ 1 đêm trở lên`)
          return
        }
        if (tier.discountValue == null || tier.discountValue <= 0) {
          setFormError(`Bậc ${i + 1}: Mức giảm giá phải lớn hơn 0`)
          return
        }
        if (tier.discountType === 'PERCENT' && tier.discountValue > 100) {
          setFormError(`Bậc ${i + 1}: Mức giảm giá theo phần trăm không được vượt quá 100%`)
          return
        }
        if (tier.maxNights != null && tier.maxNights < tier.minNights) {
          setFormError(`Bậc ${i + 1}: Số đêm tối đa (${tier.maxNights}) không được nhỏ hơn số đêm tối thiểu (${tier.minNights})`)
          return
        }
        if (i < formTiers.length - 1 && tier.maxNights == null) {
          setFormError(`Bậc ${i + 1}: Chỉ bậc cuối cùng mới được phép để trống số đêm tối đa`)
          return
        }
        if (i > 0) {
          const prev = formTiers[i - 1]
          if (prev.maxNights != null && tier.minNights <= prev.maxNights) {
            setFormError(`Khoảng đêm bị chồng lấn: Bậc ${i} kết thúc ở ${prev.maxNights} đêm, bậc ${i + 1} lại bắt đầu từ ${tier.minNights} đêm`)
            return
          }
        }
      }

      const conditionsObj: DiscountConditions = {
        minNights: formTiers[0].minNights,
        tiers: formTiers,
      }
      conditionsStr = JSON.stringify(conditionsObj)
      effectiveDiscountType = formTiers[0].discountType
      effectiveDiscountValue = Number(formTiers[0].discountValue)
    } else {
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
          conditions: conditionsStr,
          discountType: effectiveDiscountType,
          discountValue: effectiveDiscountValue,
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
          conditions: conditionsStr,
          discountType: effectiveDiscountType,
          discountValue: effectiveDiscountValue,
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
                        {(() => {
                          const tiers = parseTiers(rule.conditions)
                          if (tiers && tiers.length > 0) {
                            return (
                              <div className="space-y-1">
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                                  <Layers className="w-3 h-3" />
                                  Phân tầng ({tiers.length} bậc)
                                </span>
                                <div className="flex flex-wrap gap-1 max-w-xs">
                                  {tiers.map((t, idx) => (
                                    <span
                                      key={idx}
                                      className="inline-flex items-center text-[11px] bg-neutral-100 text-neutral-700 px-1.5 py-0.5 rounded font-mono font-medium"
                                    >
                                      {t.minNights}
                                      {t.maxNights ? `-${t.maxNights}` : '+'}đ:{' '}
                                      <strong className="ml-0.5 text-emerald-700">
                                        {t.discountType === 'PERCENT'
                                          ? `-${t.discountValue}%`
                                          : `-${Number(t.discountValue).toLocaleString('vi-VN')}đ`}
                                      </strong>
                                    </span>
                                  ))}
                                </div>
                              </div>
                            )
                          }
                          return (
                            <span className="font-semibold text-emerald-600">
                              {rule.discountType === 'PERCENT'
                                ? `-${rule.discountValue}%`
                                : `-${Number(rule.discountValue).toLocaleString('vi-VN')} đ`}
                            </span>
                          )
                        })()}
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
                if (val === 'LONG_STAY') {
                  setIsTieredMode(true)
                  if (formTiers.length === 0) setFormTiers(STANDARD_TIERS)
                }
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
                      { value: 'LONG_STAY', label: 'Giảm giá lưu trú dài ngày (LONG_STAY)' },
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

          {/* Chế độ phân tầng Tiers cho LONG_STAY hoặc tùy chọn */}
          <div className="pt-2 border-t border-neutral-100">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Layers className="w-4 h-4 text-red-600" />
                <span className="text-sm font-semibold text-neutral-800">
                  Cấu Hình Bậc Lưu Trú (Stay Duration Tiers)
                </span>
                {formRuleTypeCode === 'LONG_STAY' && (
                  <span className="text-xs bg-red-50 text-red-700 px-2 py-0.5 rounded-full font-medium">
                    Khuyến nghị cho LONG_STAY
                  </span>
                )}
              </div>
              <label className="inline-flex items-center gap-2 cursor-pointer text-xs font-medium text-neutral-600">
                <input
                  type="checkbox"
                  className="rounded border-neutral-300 text-red-600 focus:ring-red-500"
                  checked={isTieredMode}
                  onChange={(e) => {
                    setIsTieredMode(e.target.checked)
                    if (e.target.checked && formTiers.length === 0) {
                      setFormTiers(STANDARD_TIERS)
                    }
                  }}
                />
                Áp dụng giảm theo bậc số đêm
              </label>
            </div>

            {isTieredMode ? (
              <div className="space-y-3 bg-neutral-50/70 p-3.5 rounded-xl border border-neutral-200/80">
                {/* Presets bar */}
                <div className="flex flex-wrap items-center gap-2 pb-2 border-b border-neutral-200/60">
                  <span className="text-xs text-neutral-500 flex items-center gap-1 font-medium">
                    <Zap className="w-3.5 h-3.5 text-amber-500" /> Mẫu nhanh:
                  </span>
                  <button
                    type="button"
                    onClick={() => handleApplyTemplate('standard')}
                    className="text-xs px-2.5 py-1 bg-white hover:bg-neutral-100 border border-neutral-200 rounded-lg text-neutral-700 transition-colors"
                  >
                    Chuẩn (3-6đ: 10%, 7-13đ: 15%, ≥14đ: 20%)
                  </button>
                  <button
                    type="button"
                    onClick={() => handleApplyTemplate('weekly')}
                    className="text-xs px-2.5 py-1 bg-white hover:bg-neutral-100 border border-neutral-200 rounded-lg text-neutral-700 transition-colors"
                  >
                    Tuần/Tháng (7-13đ: 15%, 14-29đ: 20%, ≥30đ: 25%)
                  </button>
                </div>

                {/* Tiers List */}
                <div className="space-y-2">
                  {formTiers.map((tier, idx) => (
                    <div
                      key={idx}
                      className="flex flex-col sm:flex-row items-start sm:items-center gap-2 bg-white p-2.5 rounded-lg border border-neutral-200/70 text-xs shadow-2xs"
                    >
                      <span className="font-semibold text-neutral-700 w-12 shrink-0">
                        Bậc {idx + 1}:
                      </span>
                      <div className="flex items-center gap-1.5 flex-1">
                        <span className="text-neutral-500">Từ</span>
                        <input
                          type="number"
                          min="1"
                          className="w-16 h-8 px-2 border border-neutral-200 rounded-lg text-xs text-center focus:ring-1 focus:ring-red-500"
                          value={tier.minNights}
                          onChange={(e) =>
                            handleUpdateTier(idx, 'minNights', Number(e.target.value))
                          }
                          required
                        />
                        <span className="text-neutral-500">đến</span>
                        <input
                          type="number"
                          min={tier.minNights}
                          placeholder="Trở lên"
                          className="w-20 h-8 px-2 border border-neutral-200 rounded-lg text-xs text-center focus:ring-1 focus:ring-red-500"
                          value={tier.maxNights ?? ''}
                          onChange={(e) =>
                            handleUpdateTier(
                              idx,
                              'maxNights',
                              e.target.value === '' ? null : Number(e.target.value)
                            )
                          }
                        />
                        <span className="text-neutral-500">đêm</span>
                      </div>

                      <div className="flex items-center gap-2">
                        <select
                          className="h-8 px-2 border border-neutral-200 rounded-lg text-xs bg-white focus:ring-1 focus:ring-red-500"
                          value={tier.discountType}
                          onChange={(e) =>
                            handleUpdateTier(idx, 'discountType', e.target.value as 'PERCENT' | 'FIXED')
                          }
                        >
                          <option value="PERCENT">Phần trăm (%)</option>
                          <option value="FIXED">Cố định (VNĐ)</option>
                        </select>

                        <div className="flex items-center gap-1">
                          <input
                            type="number"
                            step="any"
                            min="0.01"
                            max={tier.discountType === 'PERCENT' ? '100' : undefined}
                            className="w-20 h-8 px-2 border border-neutral-200 rounded-lg text-xs text-right font-medium text-emerald-700 focus:ring-1 focus:ring-red-500"
                            value={tier.discountValue}
                            onChange={(e) =>
                              handleUpdateTier(idx, 'discountValue', Number(e.target.value))
                            }
                            required
                          />
                          <span className="text-neutral-500">
                            {tier.discountType === 'PERCENT' ? '%' : 'đ'}
                          </span>
                        </div>

                        {formTiers.length > 1 && (
                          <button
                            type="button"
                            onClick={() => handleRemoveTier(idx)}
                            aria-label={`Xóa bậc ${idx + 1}`}
                            className="p-1.5 text-neutral-400 hover:text-red-600 rounded-md transition-colors"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>

                {/* Add Tier button */}
                <div className="pt-1">
                  <button
                    type="button"
                    onClick={handleAddTier}
                    className="inline-flex items-center gap-1 text-xs font-semibold text-red-600 hover:text-red-700 hover:bg-red-50 px-2.5 py-1.5 rounded-lg border border-dashed border-red-300 transition-colors"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Thêm Bậc Mới
                  </button>
                </div>
              </div>
            ) : (
              /* Single Discount Type & Value */
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
            )}
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
