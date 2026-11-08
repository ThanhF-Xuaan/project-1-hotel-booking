import React, { useState, useEffect } from 'react'
import { Sparkles, Save, Check } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import roomFeatureService from '../services/roomFeature.service'
import hotelRoomTypeService from '../services/hotelRoomType.service'
import type { RoomFeatureDto } from '../types/inventory.types'

interface HotelRoomTypeFeatureEditorProps {
  hotelRoomTypeId: number
  initialFeatures: RoomFeatureDto[]
  onUpdated: () => void
}

export const HotelRoomTypeFeatureEditor: React.FC<HotelRoomTypeFeatureEditorProps> = ({
  hotelRoomTypeId,
  initialFeatures,
  onUpdated,
}) => {
  const [allFeatures, setAllFeatures] = useState<RoomFeatureDto[]>([])
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [isSaving, setIsSaving] = useState(false)
  const [saveSuccess, setSaveSuccess] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Sync selectedIds with initialFeatures
  useEffect(() => {
    const timer = setTimeout(() => {
      const ids = new Set((initialFeatures || []).map((f) => f.id))
      setSelectedIds(ids)
    }, 0)
    return () => clearTimeout(timer)
  }, [initialFeatures])

  // Load all available active features
  useEffect(() => {
    roomFeatureService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setAllFeatures(res.result.content || [])
      })
      .catch((err: unknown) => console.error('Lỗi tải danh mục tiện ích:', err))
  }, [])

  const toggleFeature = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const handleSave = async () => {
    setIsSaving(true)
    setError(null)
    setSaveSuccess(false)
    try {
      const featureIds = Array.from(selectedIds)
      await hotelRoomTypeService.updateFeatures(hotelRoomTypeId, featureIds)
      setSaveSuccess(true)
      onUpdated()
      setTimeout(() => setSaveSuccess(false), 3000)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } }; message?: string }
      setError(axiosError.response?.data?.message || axiosError.message || 'Lỗi khi lưu cấu hình tiện ích')
    } finally {
      setIsSaving(false)
    }
  }

  // Group features by category
  const categories = Array.from(new Set(allFeatures.map((f) => f.category || 'OTHER')))

  const getCategoryTitle = (cat: string) => {
    switch (cat) {
      case 'VIEW':
        return 'Tầm Nhìn & Cảnh Quan (View)'
      case 'BATHROOM':
        return 'Phòng Tắm & Vệ Sinh (Bathroom)'
      case 'BEDROOM':
        return 'Phòng Ngủ & Giường Nệm (Bedroom)'
      case 'MEDIA':
        return 'Truyền Thông & Thiết Bị (Media)'
      case 'ENTERTAINMENT':
        return 'Giải Trí & Âm Thanh (Entertainment)'
      case 'AMENITY':
        return 'Tiện Nghi Cơ Bản (Amenity)'
      case 'COMFORT':
        return 'Nội Thất & Thoải Mái (Comfort)'
      case 'INTERNET':
        return 'Mạng & Kết Nối (Internet)'
      default:
        return 'Tiện Ích Khác (Other)'
    }
  }

  return (
    <div className="bg-white rounded-2xl border border-neutral-200/80 p-6 shadow-xs space-y-5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="p-2 bg-red-50 text-red-600 rounded-xl">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-neutral-900">
              Tiện Ích & Cơ Sở Vật Chất (Hotel Room Type Feature)
            </h2>
            <p className="text-xs text-neutral-500">
              Chọn các tiện ích đi kèm với hạng phòng này (được lưu trực tiếp vào bảng hotel_room_type_features)
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <span className="text-xs text-neutral-500">
            Đã chọn: <strong className="text-red-600">{selectedIds.size}</strong> tiện ích
          </span>
          <Button onClick={handleSave} disabled={isSaving} size="sm">
            {saveSuccess ? (
              <>
                <Check className="w-4 h-4 mr-1 text-emerald-300" />
                Đã Lưu Thành Công
              </>
            ) : (
              <>
                <Save className="w-4 h-4 mr-1.5" />
                {isSaving ? 'Đang lưu...' : 'Lưu Cấu Hình Tiện Ích'}
              </>
            )}
          </Button>
        </div>
      </div>

      {error && (
        <div className="p-3 text-xs text-red-600 bg-red-50 border border-red-200 rounded-xl">
          {error}
        </div>
      )}

      {/* Grouped Feature Cards */}
      <div className="space-y-4">
        {categories.map((cat) => {
          const catFeatures = allFeatures.filter((f) => (f.category || 'OTHER') === cat)
          if (catFeatures.length === 0) return null

          return (
            <div key={cat} className="p-4 bg-neutral-50/60 rounded-xl border border-neutral-200/60 space-y-2.5">
              <h3 className="text-xs font-bold text-neutral-700 uppercase tracking-wider">
                {getCategoryTitle(cat)}
              </h3>
              <div className="flex flex-wrap gap-2">
                {catFeatures.map((feature) => {
                  const isChecked = selectedIds.has(feature.id)
                  return (
                    <button
                      key={feature.id}
                      type="button"
                      onClick={() => toggleFeature(feature.id)}
                      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium border transition-all cursor-pointer ${
                        isChecked
                          ? 'bg-red-50 text-red-700 border-red-300 shadow-2xs font-semibold ring-1 ring-red-300'
                          : 'bg-white text-neutral-700 border-neutral-200 hover:border-neutral-300 hover:bg-neutral-50'
                      }`}
                    >
                      <span
                        className={`w-3.5 h-3.5 rounded-full flex items-center justify-center text-[10px] ${
                          isChecked ? 'bg-red-600 text-white' : 'border border-neutral-300 bg-white'
                        }`}
                      >
                        {isChecked && '✓'}
                      </span>
                      <span>{feature.name}</span>
                      <span className="text-[10px] text-neutral-400 font-mono">({feature.code})</span>
                    </button>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default HotelRoomTypeFeatureEditor
