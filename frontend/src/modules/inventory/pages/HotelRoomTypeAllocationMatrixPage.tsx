import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { LayoutGrid, Building2, BedDouble, Users, Sparkles, Edit2, Plus, ArrowLeft } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Select from '../../../core/components/ui/Select'
import { formatCurrency } from '../../../core/utils/currency'
import hotelRoomTypeService from '../services/hotelRoomType.service'
import hotelService from '../../organization/services/hotel.service'
import type { HotelRoomTypeDto } from '../types/inventory.types'
import type { HotelDto } from '../../organization/types/organization.types'

export const HotelRoomTypeAllocationMatrixPage: React.FC = () => {
  const navigate = useNavigate()

  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [selectedHotelId, setSelectedHotelId] = useState<number | ''>('')
  const [roomTypes, setRoomTypes] = useState<HotelRoomTypeDto[]>([])
  const [isLoading, setIsLoading] = useState(false)

  // Load Hotels
  useEffect(() => {
    hotelService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result && res.result.content && res.result.content.length > 0) {
          setHotels(res.result.content)
          setSelectedHotelId(res.result.content[0].id)
        }
      })
      .catch((err: unknown) => console.error('Lỗi tải danh sách khách sạn:', err))
  }, [])

  const loadData = useCallback(async () => {
    if (!selectedHotelId) return
    setIsLoading(true)
    try {
      const res = await hotelRoomTypeService.filter({
        pageSize: 100,
        hotelId: Number(selectedHotelId),
      })
      if (res.result) {
        setRoomTypes(res.result.content || [])
      }
    } catch (err: unknown) {
      console.error('Lỗi khi tải ma trận phân bổ hạng phòng:', err)
    } finally {
      setIsLoading(false)
    }
  }, [selectedHotelId])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const hotelOptions = hotels.map((h) => ({
    value: h.id,
    label: `${h.name} (${h.regionName})`,
  }))

  const selectedHotel = hotels.find((h) => h.id === selectedHotelId)

  // Calculated Stats
  const totalPhysicalRooms = roomTypes.reduce((acc, rt) => acc + (rt.totalQuantity || 0), 0)
  const totalMaxGuestCapacity = roomTypes.reduce(
    (acc, rt) => acc + (rt.maxTotalGuests || 0) * (rt.totalQuantity || 0),
    0
  )

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/inventory/hotel-room-types')}
            className="shrink-0"
            title="Quay lại danh sách"
            aria-label="Quay lại danh sách"
          >
            <ArrowLeft className="w-5 h-5" />
          </Button>
          <div>
            <div className="flex items-center gap-2">
              <div className="p-2 bg-red-50 text-red-600 rounded-xl">
                <LayoutGrid className="w-5 h-5" />
              </div>
              <h1 className="text-xl font-bold text-neutral-900 tracking-tight">
                Ma Trận Phân Bổ Hạng Phòng & Giường Cơ Sở
              </h1>
            </div>
            <p className="text-xs text-neutral-500 mt-1">
              Bảng ma trận tổng hợp cấu hình phòng, phân bổ giường và danh sách tiện ích của từng khách sạn
            </p>
          </div>
        </div>

        <div className="w-full sm:w-72">
          <Select
            value={selectedHotelId}
            onChange={(val) => setSelectedHotelId(val !== '' ? Number(val) : '')}
            options={hotelOptions}
            placeholder="Chọn khách sạn cơ sở..."
          />
        </div>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs flex items-center gap-3.5">
          <div className="p-3 bg-red-50 text-red-600 rounded-xl">
            <Building2 className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-medium text-neutral-500 uppercase tracking-wider">
              Hạng Phòng Đã Cấu Hình
            </div>
            <div className="text-xl font-bold text-neutral-900 mt-0.5">
              {roomTypes.length} hạng phòng
            </div>
            <div className="text-[11px] text-neutral-400">
              Tại cơ sở {selectedHotel?.name || '—'}
            </div>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs flex items-center gap-3.5">
          <div className="p-3 bg-blue-50 text-blue-600 rounded-xl">
            <BedDouble className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-medium text-neutral-500 uppercase tracking-wider">
              Tổng Quỹ Phòng Vật Lý
            </div>
            <div className="text-xl font-bold text-neutral-900 mt-0.5">
              {totalPhysicalRooms} phòng
            </div>
            <div className="text-[11px] text-neutral-400">
              Tổng số phòng sẵn sàng vận hành
            </div>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs flex items-center gap-3.5">
          <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-medium text-neutral-500 uppercase tracking-wider">
              Tổng Công Suất Khách Tối Đa
            </div>
            <div className="text-xl font-bold text-neutral-900 mt-0.5">
              {totalMaxGuestCapacity} khách
            </div>
            <div className="text-[11px] text-neutral-400">
              Công suất phục vụ tối đa đồng thời
            </div>
          </div>
        </div>
      </div>

      {/* Matrix Table */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-xs overflow-hidden">
        <div className="p-4 border-b border-neutral-200/80 flex items-center justify-between bg-neutral-50/40">
          <div>
            <h2 className="text-sm font-bold text-neutral-900">
              Chi Tiết Phân Bổ Hạng Phòng: {selectedHotel?.name}
            </h2>
            <p className="text-xs text-neutral-500">
              Đối chiếu số lượng, giá bán, cơ cấu giường ngủ và tiện ích đính kèm
            </p>
          </div>
          <Button
            size="sm"
            onClick={() => navigate('/inventory/hotel-room-types')}
          >
            <Plus className="w-4 h-4 mr-1" />
            Thêm Hạng Phòng Mới
          </Button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-50/80 text-neutral-600 text-xs uppercase tracking-wider">
                <th className="py-3.5 px-4 font-semibold w-52">Hạng Phòng</th>
                <th className="py-3.5 px-4 font-semibold text-right w-32">Giá / Đêm</th>
                <th className="py-3.5 px-4 font-semibold text-center w-24">Số Phòng</th>
                <th className="py-3.5 px-4 font-semibold w-40">Sức Chứa Khách</th>
                <th className="py-3.5 px-4 font-semibold w-64">Cơ Cấu Giường (Beds)</th>
                <th className="py-3.5 px-4 font-semibold">Tiện Ích Đi Kèm (Features)</th>
                <th className="py-3.5 px-4 font-semibold text-right w-24">Chi Tiết</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200/70">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-500">
                    Đang tải dữ liệu ma trận phân bổ...
                  </td>
                </tr>
              ) : roomTypes.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-500">
                    Khách sạn này chưa có hạng phòng nào được cấu hình.
                  </td>
                </tr>
              ) : (
                roomTypes.map((rt) => (
                  <tr key={rt.id} className="hover:bg-neutral-50/60 transition-colors">
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-neutral-900">{rt.roomTypeName}</div>
                      <div className="text-xs text-neutral-400 font-mono">{rt.roomTypeCode}</div>
                    </td>
                    <td className="py-3.5 px-4 text-right font-bold text-red-600">
                      {formatCurrency(rt.basePrice)}
                    </td>
                    <td className="py-3.5 px-4 text-center font-bold text-neutral-900">
                      {rt.totalQuantity}
                    </td>
                    <td className="py-3.5 px-4 text-xs text-neutral-600">
                      <div>Tối đa: <strong className="text-neutral-900">{rt.maxTotalGuests} khách</strong></div>
                      <div className="text-neutral-400">Tiêu chuẩn: {rt.standardAdults} lớn, {rt.standardChildren} trẻ</div>
                    </td>
                    <td className="py-3.5 px-4 text-xs">
                      {rt.beds && rt.beds.length > 0 ? (
                        <div className="space-y-1">
                          {rt.beds.map((b) => (
                            <div key={b.roomBedId} className="flex items-center gap-1.5 text-neutral-800">
                              <span className="w-5 h-5 bg-neutral-100 rounded text-center text-[11px] font-bold text-red-600 leading-5">
                                {b.baseQuantity}x
                              </span>
                              <span className="font-medium">{b.roomBedName}</span>
                              <span className="text-[10px] text-neutral-400 font-mono">({b.roomBedSize || 'Tiêu chuẩn'})</span>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <span className="text-neutral-400 italic text-xs">Chưa cấu hình giường</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4">
                      {rt.features && rt.features.length > 0 ? (
                        <div className="flex flex-wrap gap-1 max-w-md">
                          {rt.features.slice(0, 4).map((f) => (
                            <span
                              key={f.id}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium bg-neutral-100 text-neutral-700 border border-neutral-200"
                            >
                              <Sparkles className="w-2.5 h-2.5 text-amber-500" />
                              {f.name}
                            </span>
                          ))}
                          {rt.features.length > 4 && (
                            <span className="inline-flex items-center px-1.5 py-0.5 rounded-md text-[10px] font-bold bg-neutral-200 text-neutral-700">
                              +{rt.features.length - 4} khác
                            </span>
                          )}
                        </div>
                      ) : (
                        <span className="text-neutral-400 italic text-xs">Chưa gắn tiện ích</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => navigate(`/inventory/hotel-room-types/${rt.id}`)}
                        title="Chỉnh sửa cấu hình chi tiết"
                      >
                        <Edit2 className="w-3.5 h-3.5 mr-1" />
                        Sửa
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

export default HotelRoomTypeAllocationMatrixPage
