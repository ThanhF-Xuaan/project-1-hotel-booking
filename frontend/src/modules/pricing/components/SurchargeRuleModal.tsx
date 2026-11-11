import React, { useState, useEffect } from 'react'
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

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)

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
      setErrors({})
      setServerError(null)
    }
  }, [editingRule, isOpen])

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
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return

    setIsSubmitting(true)
    setServerError(null)

    try {
      const payload: SurchargeRuleCreateRequest = {
        hotelRoomTypeId: Number(hotelRoomTypeId),
        agePolicyId: agePolicyId ? Number(agePolicyId) : undefined,
        ruleType: ruleType as any,
        pricingType: pricingType as any,
        adjustmentType: adjustmentType as any,
        adjustmentValue,
        startDate,
        endDate,
        conditions,
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

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={editingRule ? 'Cập nhật Quy tắc Phụ thu' : 'Thêm mới Quy tắc Phụ thu'}
      maxWidth="2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {serverError && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm">
            {serverError}
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
                { label: 'Theo đêm (PER_NIGHT)', value: 'PER_NIGHT' },
                { label: 'Theo kỳ lưu trú (PER_STAY)', value: 'PER_STAY' },
              ]}
            />
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Select
            label="Kiểu điều chỉnh *"
            value={adjustmentType}
            onChange={(val) => setAdjustmentType(String(val))}
            options={[
              { label: 'Số tiền cố định (VNĐ)', value: 'FIXED' },
              { label: 'Phần trăm (%) giá phòng', value: 'PERCENT' },
            ]}
          />

          {adjustmentType === 'FIXED' ? (
            <CurrencyInput
              label="Giá trị phụ thu (VNĐ) *"
              value={adjustmentValue}
              onChangeValue={(val) => setAdjustmentValue(val)}
              error={errors.adjustmentValue}
            />
          ) : (
            <Input
              label="Tỷ lệ phụ thu (%) *"
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

          <Input
            label="Điều kiện bổ sung (JSON)"
            value={conditions}
            onChange={(e) => setConditions(e.target.value)}
            placeholder='{"checkInBefore": "12:00"}'
          />
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
