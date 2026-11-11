import React, { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Building2, BedDouble, Save, Check, RefreshCw, Image as ImageIcon } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import CurrencyInput from '../../../core/components/ui/CurrencyInput'
import hotelRoomTypeService from '../services/hotelRoomType.service'
import taxCategoryService from '../../pricing/services/taxCategory.service'
import { HotelRoomTypeBedEditor } from '../components/HotelRoomTypeBedEditor'
import { HotelRoomTypeFeatureEditor } from '../components/HotelRoomTypeFeatureEditor'
import { HotelRoomTypeInclusionEditor } from '../components/HotelRoomTypeInclusionEditor'
import { RoomTypeMediaGalleryModal } from '../components/RoomTypeMediaGalleryModal'
import type {
  HotelRoomTypeDto,
  HotelRoomTypeUpdateRequest,
} from '../types/inventory.types'
import type { TaxCategoryDto } from '../../pricing/types/pricing.types'

export const HotelRoomTypeDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [hotelRoomType, setHotelRoomType] = useState<HotelRoomTypeDto | null>(null)
  const [taxCategories, setTaxCategories] = useState<TaxCategoryDto[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSavingGeneral, setIsSavingGeneral] = useState(false)
  const [saveGeneralSuccess, setSaveGeneralSuccess] = useState(false)
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [isGalleryOpen, setIsGalleryOpen] = useState(false)

  // Form General & Capacity Fields
  const [basePrice, setBasePrice] = useState<number>(0)
  const [totalQuantity, setTotalQuantity] = useState<number>(0)
  const [taxCategoryId, setTaxCategoryId] = useState<number | ''>('')
  const [status, setStatus] = useState<string>('ACTIVE')

  const [standardAdults, setStandardAdults] = useState<number>(2)
  const [standardChildren, setStandardChildren] = useState<number>(0)
  const [maxAdults, setMaxAdults] = useState<number>(2)
  const [maxChildren, setMaxChildren] = useState<number>(1)
  const [maxInfants, setMaxInfants] = useState<number>(1)
  const [maxTotalGuests, setMaxTotalGuests] = useState<number>(3)
  const [maxBeds, setMaxBeds] = useState<number>(1)
  const [extraBeds, setExtraBeds] = useState<number>(0)

  // Load Tax Categories
  useEffect(() => {
    taxCategoryService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setTaxCategories(res.result.content || [])
      })
      .catch((err: unknown) => console.error('Lỗi khi tải nhóm thuế:', err))
  }, [])

  const loadData = useCallback(async () => {
    if (!id) return
    setIsLoading(true)
    try {
      const res = await hotelRoomTypeService.getById(Number(id))
      if (res && res.result) {
        const data = res.result
        setHotelRoomType(data)
        setBasePrice(data.basePrice)
        setTotalQuantity(data.totalQuantity)
        setTaxCategoryId(data.taxCategoryId || '')
        setStatus(data.status)
        setStandardAdults(data.standardAdults)
        setStandardChildren(data.standardChildren)
        setMaxAdults(data.maxAdults)
        setMaxChildren(data.maxChildren)
        setMaxInfants(data.maxInfants)
        setMaxTotalGuests(data.maxTotalGuests)
        setMaxBeds(data.maxBeds)
        setExtraBeds(data.extraBeds)
      }
    } catch (err: unknown) {
      console.error('Lỗi khi tải chi tiết cấu hình phòng:', err)
    } finally {
      setIsLoading(false)
    }
  }, [id])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleSaveGeneral = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!id) return

    setIsSavingGeneral(true)
    setGeneralError(null)
    setSaveGeneralSuccess(false)

    try {
      const payload: HotelRoomTypeUpdateRequest = {
        basePrice,
        totalQuantity,
        taxCategoryId: taxCategoryId !== '' ? Number(taxCategoryId) : undefined,
        status,
        standardAdults,
        standardChildren,
        maxAdults,
        maxChildren,
        maxInfants,
        maxTotalGuests,
        maxBeds,
        extraBeds,
      }
      await hotelRoomTypeService.update(Number(id), payload)
      setSaveGeneralSuccess(true)
      loadData()
      setTimeout(() => setSaveGeneralSuccess(false), 3000)
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } }; message?: string }
      setGeneralError(axiosError.response?.data?.message || axiosError.message || 'Lỗi khi lưu cấu hình chung')
    } finally {
      setIsSavingGeneral(false)
    }
  }

  const statusOptions = [
    { value: 'ACTIVE', label: 'Đang mở bán (ACTIVE)' },
    { value: 'INACTIVE', label: 'Tạm dừng mở bán (INACTIVE)' },
  ]

  const taxOptions = taxCategories.map((t) => ({
    value: t.id,
    label: `${t.categoryName} (${t.categoryCode})`,
  }))

  if (isLoading && !hotelRoomType) {
    return (
      <div className="py-20 text-center text-neutral-500">
        <div className="inline-flex items-center gap-2">
          <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
          Đang tải thông tin cấu hình hạng phòng...
        </div>
      </div>
    )
  }

  if (!hotelRoomType) {
    return (
      <div className="bg-white p-8 rounded-2xl border border-neutral-200 text-center space-y-4">
        <p className="text-neutral-600">Không tìm thấy thông tin cấu hình hạng phòng này.</p>
        <Button variant="secondary" onClick={() => navigate('/inventory/hotel-room-types')}>
          <ArrowLeft className="w-4 h-4 mr-1.5" />
          Quay Lại Danh Sách
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* Top Bar with Navigation & Title */}
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
              <span className="text-xs font-semibold px-2 py-0.5 rounded-md bg-neutral-100 text-neutral-600 uppercase font-mono">
                {hotelRoomType.roomTypeCode}
              </span>
              <h1 className="text-xl font-bold text-neutral-900">
                {hotelRoomType.roomTypeName}
              </h1>
              <span
                className={`text-xs px-2.5 py-0.5 rounded-full font-semibold ${
                  hotelRoomType.status === 'ACTIVE'
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                }`}
              >
                {hotelRoomType.status === 'ACTIVE' ? 'Đang mở bán' : 'Tạm dừng'}
              </span>
            </div>
            <p className="text-xs text-neutral-500 flex items-center gap-1.5 mt-0.5">
              <Building2 className="w-3.5 h-3.5 text-neutral-400" />
              <span>Cơ sở: <strong>{hotelRoomType.hotelName}</strong></span>
            </p>
          </div>
        </div>
      </div>

      {/* Block 1: Thông tin cấu hình chung & Sức chứa */}
      <form onSubmit={handleSaveGeneral} className="bg-white rounded-2xl border border-neutral-200/80 p-6 shadow-xs space-y-6">
        <div className="flex items-center justify-between border-b border-neutral-200/80 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-red-50 text-red-600 rounded-xl">
              <BedDouble className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-neutral-900">
                Thông Tin Cơ Bản, Giá & Sức Chứa
              </h2>
              <p className="text-xs text-neutral-500">
                Cập nhật các hạn mức sức chứa tối đa, số lượng phòng cơ sở và giá cơ bản
              </p>
            </div>
          </div>
          <Button type="submit" disabled={isSavingGeneral} size="sm">
            {saveGeneralSuccess ? (
              <>
                <Check className="w-4 h-4 mr-1 text-emerald-300" />
                Đã Lưu
              </>
            ) : (
              <>
                <Save className="w-4 h-4 mr-1.5" />
                {isSavingGeneral ? 'Đang lưu...' : 'Lưu Thông Tin Chung'}
              </>
            )}
          </Button>
        </div>

        {generalError && (
          <div className="p-3 text-xs text-red-600 bg-red-50 border border-red-200 rounded-xl">
            {generalError}
          </div>
        )}

        {/* 2 Cột: Giá & Thuế */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Giá Gốc Mỗi Đêm (VNĐ) <span className="text-red-600">*</span>
            </label>
            <CurrencyInput
              value={basePrice}
              onChangeValue={(val) => setBasePrice(val)}
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Tổng Số Phòng Vật Lý <span className="text-red-600">*</span>
            </label>
            <Input
              type="number"
              min="0"
              value={totalQuantity}
              onChange={(e) => setTotalQuantity(Number(e.target.value))}
              required
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Nhóm Thuế & VAT
            </label>
            <Select
              value={taxCategoryId}
              onChange={(val) => setTaxCategoryId(val !== '' ? Number(val) : '')}
              options={taxOptions}
              placeholder="Chọn nhóm thuế..."
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
              Trạng Thái Mở Bán
            </label>
            <Select
              value={status}
              onChange={(val) => setStatus(String(val))}
              options={statusOptions}
            />
          </div>
        </div>

        {/* Grid Sức chứa */}
        <div className="pt-2 border-t border-neutral-100">
          <h3 className="text-xs font-bold text-neutral-800 uppercase tracking-wider mb-3">
            Quy Định Sức Chứa & Giường
          </h3>
          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-8 gap-3">
            <div>
              <label className="block text-[11px] font-medium text-neutral-600 mb-1">
                Tiêu Chuẩn NL
              </label>
              <Input
                type="number"
                min="1"
                value={standardAdults}
                onChange={(e) => setStandardAdults(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-[11px] font-medium text-neutral-600 mb-1">
                Tiêu Chuẩn TE
              </label>
              <Input
                type="number"
                min="0"
                value={standardChildren}
                onChange={(e) => setStandardChildren(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-[11px] font-medium text-neutral-600 mb-1">
                Tối Đa NL
              </label>
              <Input
                type="number"
                min="1"
                value={maxAdults}
                onChange={(e) => setMaxAdults(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-[11px] font-medium text-neutral-600 mb-1">
                Tối Đa TE
              </label>
              <Input
                type="number"
                min="0"
                value={maxChildren}
                onChange={(e) => setMaxChildren(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-[11px] font-medium text-neutral-600 mb-1">
                Tối Đa Sơ Sinh
              </label>
              <Input
                type="number"
                min="0"
                value={maxInfants}
                onChange={(e) => setMaxInfants(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-[11px] font-bold text-red-600 mb-1">
                Trần Tổng Khách
              </label>
              <Input
                type="number"
                min="1"
                value={maxTotalGuests}
                onChange={(e) => setMaxTotalGuests(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-[11px] font-medium text-neutral-600 mb-1">
                Giường Tối Đa
              </label>
              <Input
                type="number"
                min="1"
                value={maxBeds}
                onChange={(e) => setMaxBeds(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="block text-[11px] font-medium text-neutral-600 mb-1">
                Giường Phụ
              </label>
              <Input
                type="number"
                min="0"
                value={extraBeds}
                onChange={(e) => setExtraBeds(Number(e.target.value))}
              />
            </div>
          </div>
        </div>
      </form>

      {/* Block 2: Cấu hình Giường HotelRoomTypeBed */}
      <HotelRoomTypeBedEditor
        hotelRoomTypeId={Number(id)}
        initialBeds={hotelRoomType.beds || []}
        maxBeds={hotelRoomType.maxBeds}
        onUpdated={loadData}
      />

      {/* Block 3: Cấu hình Tiện ích HotelRoomTypeFeature */}
      <HotelRoomTypeFeatureEditor
        hotelRoomTypeId={Number(id)}
        initialFeatures={hotelRoomType.features || []}
        onUpdated={loadData}
      />

      {/* Block 4: Quà tặng & Dịch vụ kèm phòng HotelRoomTypeInclusion */}
      <HotelRoomTypeInclusionEditor
        hotelRoomTypeId={Number(id)}
        hotelId={hotelRoomType.hotelId}
        onUpdated={loadData}
      />

      {/* Block 5: Bộ Sưu Tập Hình Ảnh Hạng Phòng (Media Gallery) */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 p-6 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-red-50 text-red-600 rounded-xl">
              <ImageIcon className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-neutral-900">
                Hình Ảnh Hạng Phòng (Room Type Media Gallery)
              </h2>
              <p className="text-xs text-neutral-500">
                Quản lý ảnh đại diện và bộ sưu tập ảnh giới thiệu hạng phòng cho khách đặt trực tuyến
              </p>
            </div>
          </div>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => setIsGalleryOpen(true)}
          >
            <ImageIcon className="w-4 h-4 mr-1.5" />
            Quản Lý Thư Viện Ảnh
          </Button>
        </div>

        {hotelRoomType.coverImageUrl ? (
          <div className="flex flex-col sm:flex-row items-center gap-4 p-3 bg-neutral-50/70 rounded-xl border border-neutral-200/60">
            <img
              src={hotelRoomType.coverImageUrl}
              alt={hotelRoomType.roomTypeName}
              className="w-36 h-24 object-cover rounded-lg border border-neutral-200 shadow-xs shrink-0"
            />
            <div className="space-y-1 text-center sm:text-left">
              <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold bg-red-100 text-red-700">
                ★ Ảnh đại diện chính
              </span>
              <p className="text-xs text-neutral-600">
                Ảnh này hiển thị trên cổng tìm kiếm & trang chi tiết đặt phòng của khách.
              </p>
              <button
                type="button"
                onClick={() => setIsGalleryOpen(true)}
                className="text-xs font-semibold text-red-600 hover:text-red-700 hover:underline inline-block pt-1 cursor-pointer"
              >
                Tải lên thêm ảnh hoặc thay đổi ảnh đại diện →
              </button>
            </div>
          </div>
        ) : (
          <div className="p-6 border-2 border-dashed border-neutral-200 rounded-xl text-center space-y-2">
            <p className="text-xs text-neutral-500">
              Hạng phòng này chưa có ảnh nào. Hãy thêm ảnh để tăng tỷ lệ chuyển đổi khi khách tìm phòng!
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsGalleryOpen(true)}
            >
              <ImageIcon className="w-4 h-4 mr-1.5" />
              Tải Lên Ảnh Ngay
            </Button>
          </div>
        )}
      </div>

      {isGalleryOpen && (
        <RoomTypeMediaGalleryModal
          isOpen={isGalleryOpen}
          onClose={() => setIsGalleryOpen(false)}
          hotelRoomTypeId={Number(id)}
          roomTypeName={hotelRoomType.roomTypeName}
          hotelName={hotelRoomType.hotelName}
          onCoverImageChanged={loadData}
        />
      )}
    </div>
  )
}

export default HotelRoomTypeDetailPage
