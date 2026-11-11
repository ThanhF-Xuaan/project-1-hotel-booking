import React, { useState, useEffect } from 'react'
import Modal from '../../../core/components/ui/Modal'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import { operationService } from '../../operation/services/operation.service'
import hotelRoomTypeService from '../services/hotelRoomType.service'
import type {
  HotelRoomTypeInclusionDto,
  HotelRoomTypeInclusionRequest,
} from '../types/inventory.types'
import type { MasterItemResponse, MenuResponse } from '../../operation/types/operation.types'
import { Gift, Sparkles, UtensilsCrossed } from 'lucide-react'

interface HotelRoomTypeInclusionModalProps {
  isOpen: boolean
  onClose: () => void
  hotelRoomTypeId: number
  hotelId: number
  item?: HotelRoomTypeInclusionDto | null
  onSuccess: () => void
}

export const HotelRoomTypeInclusionModal: React.FC<HotelRoomTypeInclusionModalProps> = ({
  isOpen,
  onClose,
  hotelRoomTypeId,
  hotelId,
  item,
  onSuccess,
}) => {
  const [sourceType, setSourceType] = useState<'CHAIN_MASTER' | 'HOTEL_MENU'>('CHAIN_MASTER')
  const [masterItemId, setMasterItemId] = useState<number>(0)
  const [menuId, setMenuId] = useState<number>(0)
  const [quantity, setQuantity] = useState<number>(1)
  const [frequency, setFrequency] = useState<'PER_NIGHT' | 'PER_STAY'>('PER_NIGHT')
  const [targetType, setTargetType] = useState<'PER_ROOM' | 'PER_ADULT' | 'PER_GUEST'>('PER_ROOM')
  const [customDisplayName, setCustomDisplayName] = useState<string>('')
  const [description, setDescription] = useState<string>('')

  const [masterItems, setMasterItems] = useState<MasterItemResponse[]>([])
  const [menus, setMenus] = useState<MenuResponse[]>([])
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Reset and load dependencies
  useEffect(() => {
    if (!isOpen) return

    const timer = setTimeout(() => {
      setErrorMessage(null)
      setIsSubmitting(false)

      if (item) {
        setSourceType(item.sourceType || (item.masterItemId ? 'CHAIN_MASTER' : 'HOTEL_MENU'))
        setMasterItemId(item.masterItemId || 0)
        setMenuId(item.menuId || 0)
        setQuantity(item.quantity || 1)
        setFrequency(item.frequency || 'PER_NIGHT')
        setTargetType(item.targetType || 'PER_ROOM')
        setCustomDisplayName(item.name || '')
        setDescription(item.description || '')
      } else {
        setSourceType('CHAIN_MASTER')
        setMasterItemId(0)
        setMenuId(0)
        setQuantity(1)
        setFrequency('PER_NIGHT')
        setTargetType('PER_ROOM')
        setCustomDisplayName('')
        setDescription('')
      }

      void (async () => {
        try {
          const [masterRes, menuRes] = await Promise.all([
            operationService.getMasterItems({ pageSize: 100, status: 'ACTIVE' }),
            operationService.getMenus({ hotelId, pageSize: 100, status: 'ACTIVE' }),
          ])
          setMasterItems(masterRes.content || [])
          setMenus(menuRes.content || [])
        } catch (err) {
          console.error('Lỗi khi tải danh mục quà tặng:', err)
        }
      })()
    }, 0)

    return () => clearTimeout(timer)
  }, [isOpen, item, hotelId])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (sourceType === 'CHAIN_MASTER' && (!masterItemId || masterItemId <= 0)) {
      setErrorMessage('Vui lòng chọn mục quà tặng từ danh mục chuỗi (Master Item)')
      return
    }

    if (sourceType === 'HOTEL_MENU' && (!menuId || menuId <= 0)) {
      setErrorMessage('Vui lòng chọn món / dịch vụ từ thực đơn khách sạn cơ sở')
      return
    }

    if (quantity < 1) {
      setErrorMessage('Số lượng quà tặng tối thiểu là 1')
      return
    }

    setIsSubmitting(true)
    try {
      const payload: HotelRoomTypeInclusionRequest = {
        masterItemId: sourceType === 'CHAIN_MASTER' ? masterItemId : undefined,
        menuId: sourceType === 'HOTEL_MENU' ? menuId : undefined,
        quantity,
        frequency,
        targetType,
        customDisplayName: customDisplayName.trim() || undefined,
        description: description.trim() || undefined,
      }

      if (item) {
        await hotelRoomTypeService.updateInclusion(hotelRoomTypeId, item.id, payload)
      } else {
        await hotelRoomTypeService.createInclusion(hotelRoomTypeId, payload)
      }

      onSuccess()
      onClose()
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } }; message?: string }
      setErrorMessage(
        errorObj.response?.data?.message || errorObj.message || 'Lưu quà tặng kèm phòng thất bại'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const masterItemOptions = masterItems.map((m) => ({
    value: m.id,
    label: `${m.name} [${m.code}] (${m.unit || 'phần'})`,
    description: m.description,
  }))

  const menuOptions = menus.map((m) => ({
    value: m.id,
    label: `${m.customName ? `${m.customName} (${m.name})` : m.name} — ${new Intl.NumberFormat('vi-VN').format(m.basePrice)} đ`,
    description: m.description,
  }))

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={item ? 'Cập nhật quà tặng kèm hạng phòng' : 'Thêm quà tặng / Dịch vụ kèm hạng phòng'}
      description="Cấu hình quyền lợi buffet sáng, voucher spa, đồ uống chào mừng tặng kèm cho khách lưu trú"
      maxWidth="2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {errorMessage && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-xs">
            {errorMessage}
          </div>
        )}

        {/* Nguồn quà tặng: Tab chuyển đổi */}
        <div>
          <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
            Nguồn quà tặng / Dịch vụ <span className="text-red-500 ml-1">*</span>
          </label>
          <div className="grid grid-cols-2 gap-2 p-1 bg-neutral-100 rounded-xl">
            <button
              type="button"
              onClick={() => setSourceType('CHAIN_MASTER')}
              className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                sourceType === 'CHAIN_MASTER'
                  ? 'bg-white text-red-600 shadow-xs'
                  : 'text-neutral-600 hover:text-neutral-900'
              }`}
            >
              <Sparkles className="w-4 h-4" />
              <span>Tiêu chuẩn Chuỗi (Master Item)</span>
            </button>
            <button
              type="button"
              onClick={() => setSourceType('HOTEL_MENU')}
              className={`flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                sourceType === 'HOTEL_MENU'
                  ? 'bg-white text-red-600 shadow-xs'
                  : 'text-neutral-600 hover:text-neutral-900'
              }`}
            >
              <UtensilsCrossed className="w-4 h-4" />
              <span>Thực đơn Cơ sở (Menu)</span>
            </button>
          </div>
          <p className="text-[11px] text-neutral-500 mt-1">
            {sourceType === 'CHAIN_MASTER'
              ? 'Áp dụng cho các quyền lợi chuỗi chuẩn hóa: Buffet sáng, Voucher Spa, Đưa đón sân bay...'
              : 'Áp dụng cho món ăn, đồ uống hoặc đặc sản có quản lý tồn kho tại khách sạn cơ sở này.'}
          </p>
        </div>

        {/* Chọn món / Master Item */}
        <div>
          <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
            {sourceType === 'CHAIN_MASTER' ? 'Chọn từ Danh mục chuỗi (Master Item)' : 'Chọn từ Thực đơn cơ sở (Menu)'}{' '}
            <span className="text-red-500 ml-1">*</span>
          </label>
          {sourceType === 'CHAIN_MASTER' ? (
            <Select<number>
              value={masterItemId}
              onChange={(val) => setMasterItemId(val)}
              options={masterItemOptions}
              placeholder="-- Chọn quyền lợi chuỗi chuẩn hóa --"
              size="md"
            />
          ) : (
            <Select<number>
              value={menuId}
              onChange={(val) => setMenuId(val)}
              options={menuOptions}
              placeholder="-- Chọn món từ thực đơn cơ sở --"
              size="md"
            />
          )}
        </div>

        {/* Hàng 2: Tần suất + Đối tượng */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Tần suất tặng kèm <span className="text-red-500 ml-1">*</span>
            </label>
            <Select<'PER_NIGHT' | 'PER_STAY'>
              value={frequency}
              onChange={(val) => setFrequency(val)}
              options={[
                {
                  value: 'PER_NIGHT',
                  label: 'Hàng ngày / Mỗi đêm nghỉ (PER_NIGHT)',
                  description: 'Vd: Buffet sáng, 2 chai nước suối minibar/ngày',
                },
                {
                  value: 'PER_STAY',
                  label: 'Một lần cho cả kỳ nghỉ (PER_STAY)',
                  description: 'Vd: 1 chai rượu vang chào mừng, 1 voucher spa',
                },
              ]}
              size="md"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Đối tượng áp dụng <span className="text-red-500 ml-1">*</span>
            </label>
            <Select<'PER_ROOM' | 'PER_ADULT' | 'PER_GUEST'>
              value={targetType}
              onChange={(val) => setTargetType(val)}
              options={[
                {
                  value: 'PER_ROOM',
                  label: 'Theo phòng (PER_ROOM)',
                  description: '1 phần cho cả phòng (Vd: 1 đĩa hoa quả)',
                },
                {
                  value: 'PER_ADULT',
                  label: 'Mỗi người lớn (PER_ADULT)',
                  description: 'Theo số lượng người lớn (Vd: Buffet sáng)',
                },
                {
                  value: 'PER_GUEST',
                  label: 'Tất cả khách (PER_GUEST)',
                  description: 'Cho mọi khách lưu trú kể cả trẻ em',
                },
              ]}
              size="md"
            />
          </div>
        </div>

        {/* Hàng 3: Số lượng + Tên tiếp thị hiển thị */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="sm:col-span-1">
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Số lượng tặng <span className="text-red-500 ml-1">*</span>
            </label>
            <Input
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value, 10) || 1))}
              className="h-11 text-sm font-semibold"
              required
            />
          </div>

          <div className="sm:col-span-2">
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
              Tên hiển thị tiếp thị (Tùy chọn)
            </label>
            <Input
              value={customDisplayName}
              onChange={(e) => setCustomDisplayName(e.target.value)}
              placeholder="Vd: Miễn phí Buffet sáng hàng ngày cho 02 người lớn..."
              className="h-11 text-sm"
            />
          </div>
        </div>

        {/* Ghi chú / Điều kiện */}
        <div>
          <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider h-5 flex items-center mb-1.5">
            Ghi chú / Điều kiện phục vụ
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
            className="w-full p-3 border border-neutral-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 transition-colors placeholder:text-neutral-400"
            placeholder="Vd: Phục vụ từ 06:00 đến 10:00 tại Nhà hàng tầng 2..."
          />
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 pt-4 border-t border-neutral-200">
          <Button variant="secondary" type="button" onClick={onClose} className="h-11 px-5">
            Hủy
          </Button>
          <Button type="submit" disabled={isSubmitting} className="h-11 px-6 flex items-center gap-2">
            <Gift className="w-4 h-4" />
            {isSubmitting ? 'Đang lưu...' : item ? 'Lưu thay đổi' : 'Thêm quà tặng kèm'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}

export default HotelRoomTypeInclusionModal

