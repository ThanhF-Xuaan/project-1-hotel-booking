import React, { useState, useEffect, useCallback } from 'react'
import { Gift, Plus, Trash2, Edit3, Sparkles, UtensilsCrossed, Calendar, Users } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import hotelRoomTypeService from '../services/hotelRoomType.service'
import { HotelRoomTypeInclusionModal } from './HotelRoomTypeInclusionModal'
import type { HotelRoomTypeInclusionDto } from '../types/inventory.types'

interface HotelRoomTypeInclusionEditorProps {
  hotelRoomTypeId: number
  hotelId: number
  onUpdated?: () => void
}

export const HotelRoomTypeInclusionEditor: React.FC<HotelRoomTypeInclusionEditorProps> = ({
  hotelRoomTypeId,
  hotelId,
  onUpdated,
}) => {
  const [inclusions, setInclusions] = useState<HotelRoomTypeInclusionDto[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<HotelRoomTypeInclusionDto | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const loadInclusions = useCallback(async () => {
    setIsLoading(true)
    try {
      const res = await hotelRoomTypeService.getInclusions(hotelRoomTypeId)
      if (res && res.result) {
        setInclusions(res.result)
      }
    } catch (err) {
      console.error('Lỗi khi tải quà tặng kèm phòng:', err)
    } finally {
      setIsLoading(false)
    }
  }, [hotelRoomTypeId])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadInclusions()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadInclusions])

  const handleOpenAdd = () => {
    setEditingItem(null)
    setIsModalOpen(true)
  }

  const handleOpenEdit = (item: HotelRoomTypeInclusionDto) => {
    setEditingItem(item)
    setIsModalOpen(true)
  }

  const handleDelete = async (inclusionId: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa quà tặng / dịch vụ kèm phòng này?')) return
    setDeletingId(inclusionId)
    try {
      await hotelRoomTypeService.deleteInclusion(hotelRoomTypeId, inclusionId)
      await loadInclusions()
      onUpdated?.()
    } catch (err) {
      console.error('Lỗi khi xóa quà tặng kèm phòng:', err)
      alert('Không thể xóa quà tặng kèm phòng. Vui lòng thử lại.')
    } finally {
      setDeletingId(null)
    }
  }

  const handleSuccess = async () => {
    await loadInclusions()
    onUpdated?.()
  }

  return (
    <div className="bg-white rounded-2xl border border-neutral-200/80 p-6 shadow-xs space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2 bg-red-50 text-red-600 rounded-xl">
            <Gift className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-neutral-900">
              Quà Tặng & Dịch Vụ Kèm Phòng (Room Inclusions)
            </h2>
            <p className="text-xs text-neutral-500">
              Quản lý các gói buffet sáng, voucher spa, đồ uống chào mừng được tặng kèm theo hạng phòng này
            </p>
          </div>
        </div>
        <Button
          type="button"
          size="sm"
          onClick={handleOpenAdd}
          className="flex items-center gap-1.5"
        >
          <Plus className="w-4 h-4" />
          <span>Thêm Quà Tặng Kèm</span>
        </Button>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="p-6 text-center text-xs text-neutral-400">
          Đang tải danh sách quà tặng kèm...
        </div>
      ) : inclusions.length === 0 ? (
        <div className="p-8 text-center bg-neutral-50/70 border border-dashed border-neutral-200 rounded-xl space-y-2">
          <Gift className="w-8 h-8 mx-auto text-neutral-300" />
          <p className="text-sm font-semibold text-neutral-600">
            Chưa có quà tặng hoặc dịch vụ kèm theo nào
          </p>
          <p className="text-xs text-neutral-400 max-w-md mx-auto">
            Hạng phòng này hiện chưa được cấu hình gói quyền lợi tặng kèm (như Buffet sáng, Rượu vang chào mừng, Voucher Spa...).
          </p>
          <Button variant="secondary" size="sm" onClick={handleOpenAdd} className="mt-2">
            <Plus className="w-4 h-4 mr-1" />
            Thêm gói quyền lợi đầu tiên
          </Button>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-neutral-200 text-[11px] font-bold text-neutral-500 uppercase tracking-wider bg-neutral-50/50">
                <th className="py-2.5 px-3">Nguồn</th>
                <th className="py-2.5 px-3">Tên Quà / Dịch Vụ</th>
                <th className="py-2.5 px-3">Tần Suất</th>
                <th className="py-2.5 px-3">Đối Tượng</th>
                <th className="py-2.5 px-3 text-center">Số Lượng</th>
                <th className="py-2.5 px-3 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 text-xs">
              {inclusions.map((inc) => (
                <tr key={inc.id} className="hover:bg-neutral-50/60 transition-colors">
                  {/* Nguồn */}
                  <td className="py-3 px-3 whitespace-nowrap">
                    {inc.sourceType === 'CHAIN_MASTER' ? (
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                        <Sparkles className="w-3 h-3" />
                        Chuẩn Chuỗi
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
                        <UtensilsCrossed className="w-3 h-3" />
                        Thực Đơn Cơ Sở
                      </span>
                    )}
                  </td>

                  {/* Tên & mô tả */}
                  <td className="py-3 px-3">
                    <div className="font-semibold text-neutral-900">{inc.name}</div>
                    {inc.description && (
                      <div className="text-[11px] text-neutral-400 line-clamp-1">{inc.description}</div>
                    )}
                  </td>

                  {/* Tần suất */}
                  <td className="py-3 px-3 whitespace-nowrap">
                    <span className="inline-flex items-center gap-1 text-neutral-700 font-medium">
                      <Calendar className="w-3 h-3 text-neutral-400" />
                      {inc.frequency === 'PER_NIGHT' ? 'Hàng ngày' : '1 lần / Kỳ nghỉ'}
                    </span>
                  </td>

                  {/* Đối tượng */}
                  <td className="py-3 px-3 whitespace-nowrap">
                    <span className="inline-flex items-center gap-1 text-neutral-700 font-medium">
                      <Users className="w-3 h-3 text-neutral-400" />
                      {inc.targetType === 'PER_ROOM'
                        ? 'Theo phòng'
                        : inc.targetType === 'PER_ADULT'
                        ? 'Mỗi người lớn'
                        : 'Tất cả khách'}
                    </span>
                  </td>

                  {/* Số lượng */}
                  <td className="py-3 px-3 text-center whitespace-nowrap font-bold text-neutral-900">
                    {inc.quantity} {inc.unit || 'phần'}
                  </td>

                  {/* Thao tác */}
                  <td className="py-3 px-3 text-right whitespace-nowrap">
                    <div className="inline-flex items-center gap-1">
                      <button
                        type="button"
                        onClick={() => handleOpenEdit(inc)}
                        className="p-1.5 text-neutral-500 hover:text-neutral-800 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                        title="Chỉnh sửa"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        disabled={deletingId === inc.id}
                        onClick={() => handleDelete(inc.id)}
                        className="p-1.5 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors cursor-pointer disabled:opacity-50"
                        title="Xóa quà tặng"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal Thêm / Sửa */}
      {isModalOpen && (
        <HotelRoomTypeInclusionModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          hotelRoomTypeId={hotelRoomTypeId}
          hotelId={hotelId}
          item={editingItem}
          existingInclusions={inclusions}
          onSuccess={handleSuccess}
        />
      )}
    </div>
  )
}

export default HotelRoomTypeInclusionEditor

