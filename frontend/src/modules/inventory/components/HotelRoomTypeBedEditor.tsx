import React, { useState, useEffect } from 'react'
import { Plus, Trash2, BedDouble, Save, Check } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Select from '../../../core/components/ui/Select'
import roomBedService from '../services/roomBed.service'
import hotelRoomTypeService from '../services/hotelRoomType.service'
import type {
  HotelRoomTypeBedDto,
  HotelRoomTypeBedItemRequest,
  RoomBedDto,
} from '../types/inventory.types'

interface HotelRoomTypeBedEditorProps {
  hotelRoomTypeId: number
  initialBeds: HotelRoomTypeBedDto[]
  onUpdated: () => void
}

export const HotelRoomTypeBedEditor: React.FC<HotelRoomTypeBedEditorProps> = ({
  hotelRoomTypeId,
  initialBeds,
  onUpdated,
}) => {
  const [allRoomBeds, setAllRoomBeds] = useState<RoomBedDto[]>([])
  const [currentBeds, setCurrentBeds] = useState<HotelRoomTypeBedDto[]>(initialBeds)
  const [selectedBedId, setSelectedBedId] = useState<number | ''>('')
  const [quantity, setQuantity] = useState<number>(1)
  const [isSaving, setIsSaving] = useState(false)
  const [saveSuccess, setSaveSuccess] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Sync when initialBeds changes
  useEffect(() => {
    const timer = setTimeout(() => {
      setCurrentBeds(initialBeds || [])
    }, 0)
    return () => clearTimeout(timer)
  }, [initialBeds])

  // Load available room beds
  useEffect(() => {
    roomBedService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setAllRoomBeds(res.result.content || [])
      })
      .catch((err: unknown) => console.error('Lỗi tải danh mục giường:', err))
  }, [])

  const handleAddBed = () => {
    setError(null)
    if (selectedBedId === '') {
      setError('Vui lòng chọn loại giường để thêm')
      return
    }

    const bedObj = allRoomBeds.find((b) => b.id === Number(selectedBedId))
    if (!bedObj) return

    // If bed already exists, increase quantity
    const existingIndex = currentBeds.findIndex((b) => b.roomBedId === bedObj.id)
    if (existingIndex >= 0) {
      const updated = [...currentBeds]
      updated[existingIndex] = {
        ...updated[existingIndex],
        baseQuantity: updated[existingIndex].baseQuantity + quantity,
      }
      setCurrentBeds(updated)
    } else {
      setCurrentBeds([
        ...currentBeds,
        {
          roomBedId: bedObj.id,
          roomBedName: bedObj.name,
          roomBedSize: bedObj.size,
          baseQuantity: quantity,
        },
      ])
    }

    setSelectedBedId('')
    setQuantity(1)
  }

  const handleRemoveBed = (roomBedId: number) => {
    setCurrentBeds((prev) => prev.filter((b) => b.roomBedId !== roomBedId))
  }

  const handleQuantityChange = (roomBedId: number, newQty: number) => {
    if (newQty < 1) return
    setCurrentBeds((prev) =>
      prev.map((b) => (b.roomBedId === roomBedId ? { ...b, baseQuantity: newQty } : b))
    )
  }

  const handleSave = async () => {
    setIsSaving(true)
    setError(null)
    setSaveSuccess(false)
    try {
      const payload: HotelRoomTypeBedItemRequest[] = currentBeds.map((b) => ({
        roomBedId: b.roomBedId,
        baseQuantity: b.baseQuantity,
      }))
      await hotelRoomTypeService.updateBeds(hotelRoomTypeId, payload)
      setSaveSuccess(true)
      onUpdated()
      setTimeout(() => setSaveSuccess(false), 3000)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } }; message?: string }
      setError(axiosError.response?.data?.message || axiosError.message || 'Lỗi khi lưu cấu hình giường')
    } finally {
      setIsSaving(false)
    }
  }

  const bedOptions = allRoomBeds.map((b) => ({
    value: b.id,
    label: `${b.name} (${b.size || 'Tiêu chuẩn'})`,
  }))

  return (
    <div className="bg-white rounded-2xl border border-neutral-200/80 p-6 shadow-xs space-y-5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="p-2 bg-red-50 text-red-600 rounded-xl">
            <BedDouble className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-neutral-900">
              Cấu Hình Giường Trong Phòng (Hotel Room Type Bed)
            </h2>
            <p className="text-xs text-neutral-500">
              Thiết lập các loại giường và số lượng mặc định có sẵn trong hạng phòng này
            </p>
          </div>
        </div>

        <Button onClick={handleSave} disabled={isSaving} size="sm">
          {saveSuccess ? (
            <>
              <Check className="w-4 h-4 mr-1 text-emerald-300" />
              Đã Lưu Thành Công
            </>
          ) : (
            <>
              <Save className="w-4 h-4 mr-1.5" />
              {isSaving ? 'Đang lưu...' : 'Lưu Cấu Hình Giường'}
            </>
          )}
        </Button>
      </div>

      {error && (
        <div className="p-3 text-xs text-red-600 bg-red-50 border border-red-200 rounded-xl">
          {error}
        </div>
      )}

      {/* Add Bed Bar */}
      <div className="flex flex-col sm:flex-row gap-3 p-4 bg-neutral-50/70 rounded-xl border border-neutral-200/60">
        <div className="flex-1">
          <Select
            value={selectedBedId}
            onChange={(val) => setSelectedBedId(val !== '' ? Number(val) : '')}
            options={bedOptions}
            placeholder="Chọn loại giường muốn thêm vào phòng..."
          />
        </div>
        <div className="w-full sm:w-32">
          <input
            type="number"
            min="1"
            max="10"
            value={quantity}
            onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))}
            className="w-full h-11 px-3 text-xs font-semibold rounded-xl border border-neutral-300 bg-white text-center text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            placeholder="Số lượng"
            title="Số lượng giường"
          />
        </div>
        <Button type="button" variant="secondary" onClick={handleAddBed} className="shrink-0">
          <Plus className="w-4 h-4 mr-1" />
          Thêm Giường
        </Button>
      </div>

      {/* Current Beds Table */}
      <div className="overflow-x-auto rounded-xl border border-neutral-200/80">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="border-b border-neutral-200 bg-neutral-50/50 text-neutral-600 text-xs uppercase tracking-wider">
              <th className="py-3 px-4 font-semibold">Tên Loại Giường</th>
              <th className="py-3 px-4 font-semibold">Kích Thước</th>
              <th className="py-3 px-4 font-semibold text-center w-36">Số Lượng Giường</th>
              <th className="py-3 px-4 font-semibold text-right w-20">Xóa</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-200/70">
            {currentBeds.length === 0 ? (
              <tr>
                <td colSpan={4} className="py-8 text-center text-neutral-500 text-xs">
                  Chưa cấu hình loại giường nào cho hạng phòng này. Hãy chọn giường ở trên và bấm "Thêm Giường".
                </td>
              </tr>
            ) : (
              currentBeds.map((bed) => (
                <tr key={bed.roomBedId} className="hover:bg-neutral-50/50 transition-colors">
                  <td className="py-3 px-4 font-medium text-neutral-900">
                    {bed.roomBedName}
                  </td>
                  <td className="py-3 px-4 text-xs font-mono text-neutral-600">
                    {bed.roomBedSize || 'Tiêu chuẩn'}
                  </td>
                  <td className="py-3 px-4 text-center">
                    <div className="inline-flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() => handleQuantityChange(bed.roomBedId, bed.baseQuantity - 1)}
                        className="w-7 h-7 flex items-center justify-center bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-lg text-sm font-bold cursor-pointer transition-colors"
                        title="Giảm số lượng"
                      >
                        -
                      </button>
                      <span className="w-10 text-center font-bold text-neutral-900">
                        {bed.baseQuantity}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleQuantityChange(bed.roomBedId, bed.baseQuantity + 1)}
                        className="w-7 h-7 flex items-center justify-center bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-lg text-sm font-bold cursor-pointer transition-colors"
                        title="Tăng số lượng"
                      >
                        +
                      </button>
                    </div>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <button
                      type="button"
                      onClick={() => handleRemoveBed(bed.roomBedId)}
                      className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                      title="Xóa giường khỏi phòng"
                      aria-label={`Xóa ${bed.roomBedName}`}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default HotelRoomTypeBedEditor
