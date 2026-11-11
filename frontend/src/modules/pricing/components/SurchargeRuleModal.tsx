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

/**
 * Component bọc nhãn đồng nhất cho toàn bộ các control trong Form
 * Đảm bảo 100% các ô (Input, Select, CurrencyInput, Date) có cùng font chữ, cùng cỡ chữ và thẳng hàng
 */
const FormField: React.FC<{
  label: string
  required?: boolean
  children: React.ReactNode
}> = ({ label, required, children }) => (
  <div className="w-full flex flex-col">
    <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1.5">
      {label} {required && <span className="text-red-600">*</span>}
    </label>
    {children}
  </div>
)

const INPUT_CONTROL_CLASS = '!h-11 text-sm bg-neutral-50 hover:bg-white focus:bg-white rounded-xl'

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
  const [ruleType, setRuleType] = useState<SurchargeRuleCreateRequest['ruleType']>('EXTRA_PERSON')
  const [pricingType, setPricingType] = useState<SurchargeRuleCreateRequest['pricingType']>('PER_NIGHT')
  const [adjustmentType, setAdjustmentType] = useState<SurchargeRuleCreateRequest['adjustmentType']>('FIXED')
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
    if (!selectedHotelId) {
      return
    }
    hotelRoomTypeService
      .filter({ hotelId: Number(selectedHotelId), pageSize: 100 })
      .then((res) => setRoomTypes(res.result?.content || []))
      .catch((err) => console.error('Lỗi tải danh mục loại phòng:', err))

    agePolicyService
      .getEffective(Number(selectedHotelId))
      .then((res) => setAgePolicies(res.result || []))
      .catch((err) => console.error('Lỗi tải chính sách độ tuổi:', err))
  }, [selectedHotelId])

  // Đổ dữ liệu khi Edit hoặc Reset khi Thêm mới
  useEffect(() => {
    const timer = setTimeout(() => {
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
        setRoomTypes([])
        setAgePolicies([])
        setErrors({})
        setTierError(null)
        setServerError(null)
      }
    }, 0)
    return () => clearTimeout(timer)
  }, [editingRule, isOpen])

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
  const handleTierChange = <K extends keyof SurchargeTierItem>(
    index: number,
    field: K,
    value: SurchargeTierItem[K]
  ) => {
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
      newErrors.adjustmentValue = 'Giá trị phụ thu phải lớn hơn 0'
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
      // Tự động đóng gói conditions ở backend/frontend ngầm, người dùng không cần nhập JSON thô
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
      } else {
        finalConditions = '{}'
      }

      const payload: SurchargeRuleCreateRequest = {
        hotelRoomTypeId: Number(hotelRoomTypeId),
        agePolicyId: agePolicyId ? Number(agePolicyId) : undefined,
        ruleType,
        pricingType,
        adjustmentType,
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
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } }; message?: string }
      const msg = axiosError.response?.data?.message || axiosError.message || 'Lưu thất bại. Vui lòng kiểm tra dải ngày hiệu lực.'
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

        {/* ROW 1: Cơ sở & Hạng phòng */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField label="Khách sạn Cơ sở">
            <Select
              value={selectedHotelId}
              onChange={(val) => {
                setSelectedHotelId(val)
                setHotelRoomTypeId('')
                setAgePolicyId('')
                if (!val) {
                  setRoomTypes([])
                  setAgePolicies([])
                }
              }}
              options={[
                { label: '-- Chọn cơ sở --', value: '' },
                ...hotels.map((h) => ({ label: h.name, value: h.id })),
              ]}
              size="md"
            />
          </FormField>

          <FormField label="Hạng phòng áp dụng" required>
            <Select
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
              size="md"
            />
          </FormField>
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

        {/* ROW 2: Loại phụ thu & (Chính sách độ tuổi hoặc Hình thức tính giá) */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField label="Loại phụ thu" required>
            <Select
              value={ruleType}
              onChange={(val) => {
                const newType = String(val) as SurchargeRuleCreateRequest['ruleType']
                setRuleType(newType)
                if (newType === 'EARLY_CHECKIN' || newType === 'LATE_CHECKOUT') {
                  setPricingType('PER_STAY')
                }
              }}
              options={[
                { label: 'Thêm người (EXTRA_PERSON)', value: 'EXTRA_PERSON' },
                { label: 'Kê thêm giường (EXTRA_BED)', value: 'EXTRA_BED' },
                { label: 'Nhận phòng sớm (EARLY_CHECKIN)', value: 'EARLY_CHECKIN' },
                { label: 'Trả phòng muộn (LATE_CHECKOUT)', value: 'LATE_CHECKOUT' },
              ]}
              size="md"
            />
          </FormField>

          {ruleType === 'EXTRA_PERSON' ? (
            <FormField label="Chính sách Độ tuổi (Tùy chọn)">
              <Select
                value={agePolicyId}
                onChange={(val) => setAgePolicyId(val)}
                options={[
                  { label: '-- Áp dụng chung cho mọi độ tuổi --', value: '' },
                  ...agePolicies.map((ap) => ({
                    label: `${ap.guestType} (${ap.minAge} - ${ap.maxAge} tuổi)`,
                    value: ap.id,
                  })),
                ]}
                size="md"
              />
            </FormField>
          ) : (
            <FormField label="Hình thức tính giá" required>
              <Select<'PER_NIGHT' | 'PER_STAY'>
                value={pricingType}
                onChange={(val) => setPricingType(val)}
                options={[
                  { label: 'Theo kỳ lưu trú (PER_STAY)', value: 'PER_STAY' },
                  { label: 'Theo đêm (PER_NIGHT)', value: 'PER_NIGHT' },
                ]}
                size="md"
              />
            </FormField>
          )}
        </div>

        {/* Trường hợp EXTRA_PERSON: thêm hàng cấu hình Hình thức tính giá & Trạng thái để các hàng luôn cân đối */}
        {ruleType === 'EXTRA_PERSON' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FormField label="Hình thức tính giá" required>
              <Select<'PER_NIGHT' | 'PER_STAY'>
                value={pricingType}
                onChange={(val) => setPricingType(val)}
                options={[
                  { label: 'Theo đêm (PER_NIGHT)', value: 'PER_NIGHT' },
                  { label: 'Theo kỳ lưu trú (PER_STAY)', value: 'PER_STAY' },
                ]}
                size="md"
              />
            </FormField>

            <FormField label="Trạng thái">
              <Select
                value={status}
                onChange={(val) => setStatus(String(val))}
                options={[
                  { label: 'Hoạt động (ACTIVE)', value: 'ACTIVE' },
                  { label: 'Tạm ngừng (INACTIVE)', value: 'INACTIVE' },
                ]}
                size="md"
              />
            </FormField>
          </div>
        )}

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
              <div className="space-y-3">
                {tiers.map((tier, idx) => (
                  <div
                    key={idx}
                    className="p-3.5 bg-white rounded-xl border border-neutral-200 shadow-2xs space-y-3 hover:border-neutral-300 transition-colors"
                  >
                    <div className="flex items-center justify-between border-b border-neutral-100 pb-2">
                      <span className="text-xs font-bold text-neutral-800 bg-neutral-100 px-2.5 py-0.5 rounded-md">
                        BẬC KHUNG GIỜ #{idx + 1}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleRemoveTier(idx)}
                        className="text-neutral-400 hover:text-red-600 p-1 rounded-md hover:bg-red-50 transition-colors cursor-pointer"
                        title="Xóa bậc này"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    {/* 4 ô nhập trong bậc - Đồng bộ chiều cao và nhãn */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 items-start">
                      <FormField label="Từ giờ" required>
                        <Input
                          type="time"
                          value={tier.from_time}
                          onChange={(e) => handleTierChange(idx, 'from_time', e.target.value)}
                          className={INPUT_CONTROL_CLASS}
                        />
                      </FormField>

                      <FormField label="Đến giờ" required>
                        <Input
                          type="time"
                          value={tier.to_time}
                          onChange={(e) => handleTierChange(idx, 'to_time', e.target.value)}
                          className={INPUT_CONTROL_CLASS}
                        />
                      </FormField>

                      <FormField label="Kiểu tính" required>
                        <Select
                          value={tier.adjustment_type}
                          onChange={(val) => handleTierChange(idx, 'adjustment_type', val)}
                          options={[
                            { label: 'Phần trăm (%)', value: 'PERCENT' },
                            { label: 'Cố định (VNĐ)', value: 'FIXED' },
                          ]}
                          size="md"
                        />
                      </FormField>

                      <FormField
                        label={tier.adjustment_type === 'PERCENT' ? 'Tỷ lệ (%)' : 'Số tiền (VNĐ)'}
                        required
                      >
                        {tier.adjustment_type === 'FIXED' ? (
                          <CurrencyInput
                            value={tier.adjustment_value}
                            onChangeValue={(val) => handleTierChange(idx, 'adjustment_value', val)}
                            className={INPUT_CONTROL_CLASS}
                          />
                        ) : (
                          <Input
                            type="number"
                            min={1}
                            max={100}
                            value={tier.adjustment_value}
                            onChange={(e) =>
                              handleTierChange(idx, 'adjustment_value', Number(e.target.value))
                            }
                            className={INPUT_CONTROL_CLASS}
                          />
                        )}
                      </FormField>
                    </div>

                    <FormField label="Mô tả bậc (Hiển thị trên hóa đơn / Folio)">
                      <Input
                        placeholder="Ví dụ: Nhận phòng từ 06:00 đến 10:00 (Phụ thu 50% giá phòng)"
                        value={tier.description || ''}
                        onChange={(e) => handleTierChange(idx, 'description', e.target.value)}
                        className={INPUT_CONTROL_CLASS}
                      />
                    </FormField>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Mức phụ thu mặc định / dự phòng */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField
            label={isTierApplicable ? 'Kiểu điều chỉnh mặc định (Dự phòng)' : 'Kiểu điều chỉnh'}
            required
          >
            <Select<'PERCENT' | 'FIXED'>
              value={adjustmentType}
              onChange={(val) => setAdjustmentType(val)}
              options={[
                { label: 'Số tiền cố định (VNĐ)', value: 'FIXED' },
                { label: 'Phần trăm (%) giá phòng', value: 'PERCENT' },
              ]}
              size="md"
            />
          </FormField>

          <FormField
            label={
              isTierApplicable
                ? adjustmentType === 'FIXED'
                  ? 'Mức thu mặc định (VNĐ)'
                  : 'Tỷ lệ mặc định (%)'
                : adjustmentType === 'FIXED'
                ? 'Giá trị phụ thu (VNĐ)'
                : 'Tỷ lệ phụ thu (%)'
            }
            required
          >
            {adjustmentType === 'FIXED' ? (
              <CurrencyInput
                value={adjustmentValue}
                onChangeValue={(val) => setAdjustmentValue(val)}
                error={errors.adjustmentValue}
                className={INPUT_CONTROL_CLASS}
              />
            ) : (
              <Input
                type="number"
                min={1}
                max={100}
                value={adjustmentValue}
                onChange={(e) => setAdjustmentValue(Number(e.target.value))}
                error={errors.adjustmentValue}
                className={INPUT_CONTROL_CLASS}
              />
            )}
          </FormField>
        </div>

        {/* Ngày bắt đầu & Ngày kết thúc */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormField label="Ngày bắt đầu" required>
            <Input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              error={errors.startDate}
              className={INPUT_CONTROL_CLASS}
            />
          </FormField>

          <FormField label="Ngày kết thúc" required>
            <Input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              error={errors.endDate}
              className={INPUT_CONTROL_CLASS}
            />
          </FormField>
        </div>

        {/* Trạng thái (đối với các loại phụ thu không phải EXTRA_PERSON - do EXTRA_PERSON đã ghép ở trên) */}
        {ruleType !== 'EXTRA_PERSON' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FormField label="Trạng thái">
              <Select
                value={status}
                onChange={(val) => setStatus(String(val))}
                options={[
                  { label: 'Hoạt động (ACTIVE)', value: 'ACTIVE' },
                  { label: 'Tạm ngừng (INACTIVE)', value: 'INACTIVE' },
                ]}
                size="md"
              />
            </FormField>
          </div>
        )}

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
