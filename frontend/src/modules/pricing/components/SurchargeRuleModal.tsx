import React, { useState, useEffect } from 'react'
import { Plus, Trash2, Clock, Sparkles, AlertCircle, Info } from 'lucide-react'
import Modal from '../../../core/components/ui/Modal'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import CurrencyInput from '../../../core/components/ui/CurrencyInput'
import hotelService from '../../organization/services/hotel.service'
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service'
import agePolicyService from '../services/agePolicy.service'
import surchargeRuleService from '../services/surchargeRule.service'
import type {
  SurchargeRuleDto,
  SurchargeRuleCreateRequest,
  SurchargeRuleUpdateRequest,
  HotelAgePolicyDto,
  SurchargeTierItem,
  SurchargeConditions,
} from '../types/pricing.types'
import type { HotelDto } from '../../organization/types/organization.types'
import type { HotelRoomTypeDto } from '../../inventory/types/inventory.types'

interface SurchargeRuleModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
  editingRule?: SurchargeRuleDto | null
}

export const SurchargeRuleModal: React.FC<SurchargeRuleModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  editingRule,
}) => {
  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [roomTypes, setRoomTypes] = useState<HotelRoomTypeDto[]>([])
  const [agePolicies, setAgePolicies] = useState<HotelAgePolicyDto[]>([])

  const [selectedHotelId, setSelectedHotelId] = useState<number | ''>('')
  const [hotelRoomTypeId, setHotelRoomTypeId] = useState<number | ''>('')
  const [agePolicyId, setAgePolicyId] = useState<number | ''>('')
  const [ruleType, setRuleType] = useState<string>('EXTRA_PERSON')
  const [pricingType, setPricingType] = useState<string>('PER_NIGHT')
  const [adjustmentType, setAdjustmentType] = useState<string>('FIXED')
  const [adjustmentValue, setAdjustmentValue] = useState<number>(100000)
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')
  const [conditions, setConditions] = useState<string>('{}')
  const [status, setStatus] = useState<string>('ACTIVE')

  // Quản lý các bậc phụ thu thời gian (Tiers)
  const [tiers, setTiers] = useState<SurchargeTierItem[]>([])
  const [tierError, setTierError] = useState<string | null>(null)

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)

  // Lấy thông tin cơ sở hiện tại được chọn
  const currentHotel = hotels.find((h) => h.id === Number(selectedHotelId))
  const hotelCheckInTime =
    currentHotel?.checkInTime?.substring(0, 5) ||
    (editingRule?.hotelCheckInTime ? String(editingRule.hotelCheckInTime).substring(0, 5) : '14:00')
  const hotelCheckOutTime =
    currentHotel?.checkOutTime?.substring(0, 5) ||
    (editingRule?.hotelCheckOutTime ? String(editingRule.hotelCheckOutTime).substring(0, 5) : '12:00')

  const currentStandardTime = ruleType === 'EARLY_CHECKIN' ? hotelCheckInTime : hotelCheckOutTime

  // Tải danh sách khách sạn
  useEffect(() => {
    if (isOpen) {
      hotelService
        .filter({ page: 0, pageSize: 100, status: 'ACTIVE' })
        .then((res) => {
          if (res.result?.content) setHotels(res.result.content)
        })
        .catch((err) => console.error('Lỗi tải danh mục khách sạn:', err))
    }
  }, [isOpen])

  // Khi chọn khách sạn, lọc room types và age policies
  useEffect(() => {
    if (selectedHotelId) {
      hotelRoomTypeService
        .filter({ hotelId: Number(selectedHotelId), pageSize: 100 })
        .then((res) => setRoomTypes(res.result?.content || []))
        .catch((err) => console.error('Lỗi tải danh mục loại phòng:', err))

      agePolicyService
        .getEffective(Number(selectedHotelId))
        .then((res) => setAgePolicies(res.result || []))
        .catch((err) => console.error('Lỗi tải chính sách độ tuổi:', err))
    } else {
      setRoomTypes([])
      setAgePolicies([])
    }
  }, [selectedHotelId])

  // Đổ dữ liệu khi Edit hoặc Reset khi Thêm mới
  useEffect(() => {
    if (editingRule) {
      setSelectedHotelId(editingRule.hotelId || '')
      setHotelRoomTypeId(editingRule.hotelRoomTypeId)
      setAgePolicyId(editingRule.agePolicyId || '')
      setRuleType(editingRule.ruleType)
      setPricingType(editingRule.pricingType)
      setAdjustmentType(editingRule.adjustmentType)
      setAdjustmentValue(editingRule.adjustmentValue)
      setStartDate(editingRule.startDate)
      setEndDate(editingRule.endDate)
      setConditions(editingRule.conditions || '{}')
      setStatus(editingRule.status)

      // Trích xuất tiers từ conditions nếu có
      try {
        const parsed: SurchargeConditions = JSON.parse(editingRule.conditions || '{}')
        if (Array.isArray(parsed.tiers) && parsed.tiers.length > 0) {
          setTiers(parsed.tiers)
        } else {
          setTiers([])
        }
      } catch {
        setTiers([])
      }
    } else {
      setSelectedHotelId('')
      setHotelRoomTypeId('')
      setAgePolicyId('')
      setRuleType('EXTRA_PERSON')
      setPricingType('PER_NIGHT')
      setAdjustmentType('FIXED')
      setAdjustmentValue(100000)
      setStartDate(new Date().toISOString().split('T')[0])
      setEndDate(new Date(Date.now() + 90 * 86400000).toISOString().split('T')[0])
      setConditions('{}')
      setStatus('ACTIVE')
      setTiers([])
      setErrors({})
      setTierError(null)
      setServerError(null)
    }
  }, [editingRule, isOpen])

  // Khi chuyển sang EARLY_CHECKIN hoặc LATE_CHECKOUT, mặc định pricingType là PER_STAY
  useEffect(() => {
    if (ruleType === 'EARLY_CHECKIN' || ruleType === 'LATE_CHECKOUT') {
      setPricingType('PER_STAY')
    }
  }, [ruleType])

  // Thêm bậc mới
  const handleAddTier = () => {
    const lastTier = tiers[tiers.length - 1]
    const defaultFrom = lastTier ? lastTier.to_time : (ruleType === 'LATE_CHECKOUT' ? hotelCheckOutTime : '06:00')
    const defaultTo = ruleType === 'EARLY_CHECKIN' ? hotelCheckInTime : '18:00'

    setTiers([
      ...tiers,
      {
        from_time: defaultFrom,
        to_time: defaultTo,
        adjustment_type: 'PERCENT',
        adjustment_value: 30,
        description: '',
      },
    ])
    setTierError(null)
  }

  // Xóa bậc
  const handleRemoveTier = (index: number) => {
    setTiers(tiers.filter((_, i) => i !== index))
  }

  // Cập nhật giá trị trường trong bậc
  const handleTierChange = (index: number, field: keyof SurchargeTierItem, value: any) => {
    const updated = [...tiers]
    updated[index] = { ...updated[index], [field]: value }
    setTiers(updated)
    setTierError(null)
  }

  // Áp dụng bộ bậc mẫu gợi ý theo giờ quy chuẩn cơ sở
  const handleApplyPreset = () => {
    if (ruleType === 'EARLY_CHECKIN') {
      setTiers([
        {
          from_time: '00:00',
          to_time: '06:00',
          adjustment_type: 'PERCENT',
          adjustment_value: 100,
          description: 'Nhận phòng trước 06:00 (Phụ thu 100% giá phòng)',
        },
        {
          from_time: '06:00',
          to_time: '10:00',
          adjustment_type: 'PERCENT',
          adjustment_value: 50,
          description: 'Nhận phòng từ 06:00 đến 10:00 (Phụ thu 50% giá phòng)',
        },
        {
          from_time: '10:00',
          to_time: hotelCheckInTime,
          adjustment_type: 'PERCENT',
          adjustment_value: 30,
          description: `Nhận phòng từ 10:00 đến ${hotelCheckInTime} (Phụ thu 30% giá phòng)`,
        },
      ])
    } else if (ruleType === 'LATE_CHECKOUT') {
      setTiers([
        {
          from_time: hotelCheckOutTime,
          to_time: '15:00',
          adjustment_type: 'PERCENT',
          adjustment_value: 30,
          description: `Trả phòng từ ${hotelCheckOutTime} đến 15:00 (Phụ thu 30% giá phòng)`,
        },
        {
          from_time: '15:00',
          to_time: '18:00',
          adjustment_type: 'PERCENT',
          adjustment_value: 50,
          description: 'Trả phòng từ 15:00 đến 18:00 (Phụ thu 50% giá phòng)',
        },
        {
          from_time: '18:00',
          to_time: '23:59',
          adjustment_type: 'PERCENT',
          adjustment_value: 100,
          description: 'Trả phòng sau 18:00 (Phụ thu 100% giá phòng)',
        },
      ])
    }
    setTierError(null)
  }

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}
    if (!hotelRoomTypeId) newErrors.hotelRoomTypeId = 'Vui lòng chọn hạng phòng áp dụng'
    if (!startDate) newErrors.startDate = 'Vui lòng chọn ngày bắt đầu'
    if (!endDate) newErrors.endDate = 'Vui lòng chọn ngày kết thúc'
    if (startDate && endDate && startDate > endDate) {
      newErrors.endDate = 'Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu'
    }
    if (adjustmentValue <= 0) {
      newErrors.adjustmentValue = 'Giá trị phụ thu cơ sở phải lớn hơn 0'
    }
    if (adjustmentType === 'PERCENT' && adjustmentValue > 100) {
      newErrors.adjustmentValue = 'Tỷ lệ phụ thu phần trăm không được vượt quá 100%'
    }

    // Validate bậc thời gian nếu có
    if ((ruleType === 'EARLY_CHECKIN' || ruleType === 'LATE_CHECKOUT') && tiers.length > 0) {
      for (let i = 0; i < tiers.length; i++) {
        const t = tiers[i]
        if (!t.from_time || !t.to_time) {
          setTierError(`Bậc thứ ${i + 1}: Vui lòng nhập đầy đủ giờ bắt đầu và kết thúc`)
          return false
        }
        if (t.from_time >= t.to_time) {
          setTierError(`Bậc thứ ${i + 1}: Giờ bắt đầu (${t.from_time}) phải trước giờ kết thúc (${t.to_time})`)
          return false
        }
        if (t.adjustment_type === 'PERCENT' && (t.adjustment_value <= 0 || t.adjustment_value > 100)) {
          setTierError(`Bậc thứ ${i + 1}: Tỷ lệ phần trăm phải từ 1% đến 100%`)
          return false
        }
        if (t.adjustment_type === 'FIXED' && t.adjustment_value <= 0) {
          setTierError(`Bậc thứ ${i + 1}: Giá trị phụ thu cố định phải lớn hơn 0`)
          return false
        }

        // Chặn theo giờ quy chuẩn cơ sở
        if (ruleType === 'EARLY_CHECKIN' && t.to_time > hotelCheckInTime) {
          setTierError(
            `Bậc thứ ${i + 1}: Phụ thu nhận sớm không thể kết thúc sau giờ check-in của cơ sở (${hotelCheckInTime})`
          )
          return false
        }
        if (ruleType === 'LATE_CHECKOUT' && t.from_time < hotelCheckOutTime) {
          setTierError(
            `Bậc thứ ${i + 1}: Phụ thu trả muộn không thể bắt đầu trước giờ check-out của cơ sở (${hotelCheckOutTime})`
          )
          return false
        }
      }

      // Kiểm tra chống chồng lấn
      const sorted = [...tiers].sort((a, b) => a.from_time.localeCompare(b.from_time))
      for (let i = 0; i < sorted.length - 1; i++) {
        if (sorted[i].to_time > sorted[i + 1].from_time) {
          setTierError(
            `Khung giờ bậc phụ thu bị chồng lấn: [${sorted[i].from_time} - ${sorted[i].to_time}] và [${sorted[i + 1].from_time} - ${sorted[i + 1].to_time}]`
          )
          return false
        }
      }
    }

    setErrors(newErrors)
    setTierError(null)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return

    setIsSubmitting(true)
    setServerError(null)

    try {
      // Đóng gói conditions với standard_time và tiers
      let finalConditions = conditions
      if (ruleType === 'EARLY_CHECKIN' || ruleType === 'LATE_CHECKOUT') {
        const conditionsObj: SurchargeConditions = {
          standard_time: currentStandardTime,
          tiers: tiers.map((t) => ({
            from_time: t.from_time,
            to_time: t.to_time,
            adjustment_type: t.adjustment_type,
            adjustment_value: Number(t.adjustment_value),
            description: t.description?.trim() || undefined,
          })),
        }
        finalConditions = JSON.stringify(conditionsObj)
      }

      const payload: SurchargeRuleCreateRequest = {
        hotelRoomTypeId: Number(hotelRoomTypeId),
        agePolicyId: agePolicyId ? Number(agePolicyId) : undefined,
        ruleType: ruleType as any,
        pricingType: pricingType as any,
        adjustmentType: adjustmentType as any,
        adjustmentValue,
        startDate,
        endDate,
        conditions: finalConditions,
        status,
      }

      if (editingRule) {
        await surchargeRuleService.update(editingRule.id, payload as SurchargeRuleUpdateRequest)
      } else {
        await surchargeRuleService.create(payload)
      }
      onSuccess()
      onClose()
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Lưu thất bại. Vui lòng kiểm tra dải ngày hiệu lực.'
      setServerError(msg)
    } finally {
      setIsSubmitting(false)
    }
  }

  const isTierApplicable = ruleType === 'EARLY_CHECKIN' || ruleType === 'LATE_CHECKOUT'

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={editingRule ? 'Cập nhật Quy tắc Phụ thu' : 'Thêm mới Quy tắc Phụ thu'}
      maxWidth="4xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {serverError && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm flex items-start gap-2">
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
            <span>{serverError}</span>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Select
            label="Khách sạn Cơ sở"
            value={selectedHotelId}
            onChange={(val) => {
              setSelectedHotelId(val)
              setHotelRoomTypeId('')
            }}
            options={[
              { label: '-- Chọn cơ sở --', value: '' },
              ...hotels.map((h) => ({ label: h.name, value: h.id })),
            ]}
          />

          <Select
            label="Hạng phòng áp dụng *"
            value={hotelRoomTypeId}
            onChange={(val) => setHotelRoomTypeId(val)}
            options={[
              { label: '-- Chọn loại phòng --', value: '' },
              ...roomTypes.map((rt) => ({
                label: `${rt.roomTypeName || 'Loại phòng ' + rt.id} (Tối đa ${rt.maxTotalGuests} khách)`,
                value: rt.id,
              })),
            ]}
            error={errors.hotelRoomTypeId}
          />
        </div>

        {/* Khóa giờ chuẩn cấp Cơ sở (Read-only Policy Banner) */}
        {isTierApplicable && (
          <div className="bg-amber-50/70 p-3.5 rounded-xl border border-amber-200/80 text-xs text-amber-900 flex flex-wrap items-center justify-between gap-2 shadow-xs">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-amber-700 shrink-0" />
              <span>
                <strong>Chính sách Cơ sở:</strong> Giờ {ruleType === 'EARLY_CHECKIN' ? 'Check-in' : 'Check-out'} quy chuẩn:{' '}
                <span className="font-bold text-red-600 bg-white px-2.5 py-0.5 rounded-md border border-amber-300 text-sm shadow-xs">
                  {currentStandardTime}
                </span>
              </span>
            </div>
            <span className="text-amber-800 italic flex items-center gap-1">
              <Info className="w-3.5 h-3.5" />
              Cố định theo Cơ sở — Hạng phòng không được phép override
            </span>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Select
            label="Loại phụ thu *"
            value={ruleType}
            onChange={(val) => setRuleType(String(val))}
            options={[
              { label: 'Thêm người (EXTRA_PERSON)', value: 'EXTRA_PERSON' },
              { label: 'Kê thêm giường (EXTRA_BED)', value: 'EXTRA_BED' },
              { label: 'Nhận phòng sớm (EARLY_CHECKIN)', value: 'EARLY_CHECKIN' },
              { label: 'Trả phòng muộn (LATE_CHECKOUT)', value: 'LATE_CHECKOUT' },
            ]}
          />

          {ruleType === 'EXTRA_PERSON' ? (
            <Select
              label="Chính sách Độ tuổi (Tùy chọn)"
              value={agePolicyId}
              onChange={(val) => setAgePolicyId(val)}
              options={[
                { label: '-- Áp dụng chung cho mọi độ tuổi --', value: '' },
                ...agePolicies.map((ap) => ({
                  label: `${ap.guestType} (${ap.minAge} - ${ap.maxAge} tuổi)`,
                  value: ap.id,
                })),
              ]}
            />
          ) : (
            <Select
              label="Hình thức tính giá *"
              value={pricingType}
              onChange={(val) => setPricingType(String(val))}
              options={[
                { label: 'Theo kỳ lưu trú (PER_STAY)', value: 'PER_STAY' },
                { label: 'Theo đêm (PER_NIGHT)', value: 'PER_NIGHT' },
              ]}
            />
          )}
        </div>

        {/* Cấu hình các bậc thời gian (Tiers) cho EARLY_CHECKIN / LATE_CHECKOUT */}
        {isTierApplicable && (
          <div className="border border-neutral-200 rounded-2xl p-4 bg-neutral-50/50 space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h4 className="text-sm font-semibold text-neutral-900 flex items-center gap-1.5">
                  <Clock className="w-4 h-4 text-red-600" />
                  Cấu hình các bậc khung giờ phụ thu (Tiers)
                </h4>
                <p className="text-xs text-neutral-500 mt-0.5">
                  Tự động tính phụ thu linh hoạt theo thời gian khách check-in sớm hoặc check-out muộn thực tế.
                </p>
              </div>
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  type="button"
                  onClick={handleApplyPreset}
                  className="text-xs flex items-center gap-1 text-neutral-700 bg-white"
                >
                  <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                  Gợi ý bậc mẫu
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  type="button"
                  onClick={handleAddTier}
                  className="text-xs flex items-center gap-1"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Thêm bậc
                </Button>
              </div>
            </div>

            {tierError && (
              <div className="p-2.5 bg-red-50 border border-red-200 text-red-600 rounded-xl text-xs flex items-center gap-1.5">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{tierError}</span>
              </div>
            )}

            {tiers.length === 0 ? (
              <div className="text-center py-5 text-neutral-400 text-xs bg-white rounded-xl border border-dashed border-neutral-200">
                Chưa có bậc thời gian nào được cấu hình. Nhấn{' '}
                <strong className="text-neutral-700">"Gợi ý bậc mẫu"</strong> hoặc{' '}
                <strong className="text-neutral-700">"+ Thêm bậc"</strong> để bắt đầu.
              </div>
            ) : (
              <div className="space-y-2.5">
                {tiers.map((tier, idx) => (
                  <div
                    key={idx}
                    className="p-3 bg-white rounded-xl border border-neutral-200 shadow-2xs space-y-2 hover:border-neutral-300 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-neutral-800 bg-neutral-100 px-2 py-0.5 rounded-md">
                        Bậc #{idx + 1}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleRemoveTier(idx)}
                        className="text-neutral-400 hover:text-red-600 p-1 rounded-md hover:bg-red-50 transition-colors"
                        title="Xóa bậc này"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5">
                      <Input
                        label="Từ giờ *"
                        type="time"
                        value={tier.from_time}
                        onChange={(e) => handleTierChange(idx, 'from_time', e.target.value)}
                      />
                      <Input
                        label="Đến giờ *"
                        type="time"
                        value={tier.to_time}
                        onChange={(e) => handleTierChange(idx, 'to_time', e.target.value)}
                      />
                      <Select
                        label="Kiểu tính *"
                        value={tier.adjustment_type}
                        onChange={(val) => handleTierChange(idx, 'adjustment_type', val)}
                        options={[
                          { label: 'Phần trăm (%)', value: 'PERCENT' },
                          { label: 'Cố định (VNĐ)', value: 'FIXED' },
                        ]}
                      />
                      {tier.adjustment_type === 'FIXED' ? (
                        <CurrencyInput
                          label="Số tiền (VNĐ) *"
                          value={tier.adjustment_value}
                          onChangeValue={(val) => handleTierChange(idx, 'adjustment_value', val)}
                        />
                      ) : (
                        <Input
                          label="Tỷ lệ (%) *"
                          type="number"
                          min={1}
                          max={100}
                          value={tier.adjustment_value}
                          onChange={(e) => handleTierChange(idx, 'adjustment_value', Number(e.target.value))}
                        />
                      )}
                    </div>

                    <Input
                      label="Mô tả bậc (Hiển thị trên hóa đơn / Folio)"
                      placeholder="Ví dụ: Nhận phòng từ 06:00 đến 10:00 (Phụ thu 50% giá phòng)"
                      value={tier.description || ''}
                      onChange={(e) => handleTierChange(idx, 'description', e.target.value)}
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Mức phụ thu mặc định / dự phòng */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Select
            label={isTierApplicable ? 'Kiểu điều chỉnh mặc định (Dự phòng) *' : 'Kiểu điều chỉnh *'}
            value={adjustmentType}
            onChange={(val) => setAdjustmentType(String(val))}
            options={[
              { label: 'Số tiền cố định (VNĐ)', value: 'FIXED' },
              { label: 'Phần trăm (%) giá phòng', value: 'PERCENT' },
            ]}
          />

          {adjustmentType === 'FIXED' ? (
            <CurrencyInput
              label={isTierApplicable ? 'Mức thu mặc định (VNĐ) *' : 'Giá trị phụ thu (VNĐ) *'}
              value={adjustmentValue}
              onChangeValue={(val) => setAdjustmentValue(val)}
              error={errors.adjustmentValue}
            />
          ) : (
            <Input
              label={isTierApplicable ? 'Tỷ lệ mặc định (%) *' : 'Tỷ lệ phụ thu (%) *'}
              type="number"
              min={1}
              max={100}
              value={adjustmentValue}
              onChange={(e) => setAdjustmentValue(Number(e.target.value))}
              error={errors.adjustmentValue}
            />
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            label="Ngày bắt đầu *"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            error={errors.startDate}
          />

          <Input
            label="Ngày kết thúc *"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            error={errors.endDate}
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Select
            label="Trạng thái"
            value={status}
            onChange={(val) => setStatus(String(val))}
            options={[
              { label: 'Hoạt động (ACTIVE)', value: 'ACTIVE' },
              { label: 'Tạm ngừng (INACTIVE)', value: 'INACTIVE' },
            ]}
          />

          {!isTierApplicable && (
            <Input
              label="Điều kiện bổ sung (JSON)"
              value={conditions}
              onChange={(e) => setConditions(e.target.value)}
              placeholder='{"checkInBefore": "12:00"}'
            />
          )}
        </div>

        <div className="flex justify-end gap-3 pt-4 border-t border-neutral-200">
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Hủy
          </Button>
          <Button variant="primary" type="submit" isLoading={isSubmitting}>
            {editingRule ? 'Lưu thay đổi' : 'Tạo mới'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

export default SurchargeRuleModal
