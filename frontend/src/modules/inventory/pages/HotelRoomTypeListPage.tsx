import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Trash2, RefreshCw, Search, Eye, Building2, BedDouble, Users, Image as ImageIcon } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Select, { type SelectOption } from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import Input from '../../../core/components/ui/Input'
import CurrencyInput from '../../../core/components/ui/CurrencyInput'
import { formatCurrency } from '../../../core/utils/currency'
import hotelRoomTypeService from '../services/hotelRoomType.service'
import roomTypeService from '../services/roomType.service'
import hotelService from '../../organization/services/hotel.service'
import taxCategoryService from '../../pricing/services/taxCategory.service'
import { RoomTypeMediaGalleryModal } from '../components/RoomTypeMediaGalleryModal'
import type {
  HotelRoomTypeDto,
  HotelRoomTypeCreateRequest,
  RoomTypeDto,
} from '../types/inventory.types'
import type { HotelDto } from '../../organization/types/organization.types'
import type { TaxCategoryDto } from '../../pricing/types/pricing.types'

export const HotelRoomTypeListPage: React.FC = () => {
  const navigate = useNavigate()

  const [hotelRoomTypes, setHotelRoomTypes] = useState<HotelRoomTypeDto[]>([])
  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [roomTypes, setRoomTypes] = useState<RoomTypeDto[]>([])
  const [taxCategories, setTaxCategories] = useState<TaxCategoryDto[]>([])

  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)

  // Filters
  const [selectedHotelId, setSelectedHotelId] = useState<number | ''>('')
  const [selectedRoomTypeId, setSelectedRoomTypeId] = useState<number | ''>('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Configured summary for dynamic real-data filter options
  const [configuredHotels, setConfiguredHotels] = useState<
    { id: number; name: string; count: number }[]
  >([])
  const [configuredRoomTypes, setConfiguredRoomTypes] = useState<
    { id: number; name: string; code: string; count: number }[]
  >([])

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal Create State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [formHotelId, setFormHotelId] = useState<number | ''>('')
  const [formRoomTypeId, setFormRoomTypeId] = useState<number | ''>('')
  const [formTaxCategoryId, setFormTaxCategoryId] = useState<number | ''>('')
  const [formBasePrice, setFormBasePrice] = useState<number>(1000000)
  const [formTotalQuantity, setFormTotalQuantity] = useState<number>(5)
  const [formMaxTotalGuests, setFormMaxTotalGuests] = useState<number>(3)
  const [formMaxBeds, setFormMaxBeds] = useState<number>(1)
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Media Gallery Modal
  const [galleryRoomType, setGalleryRoomType] = useState<{
    id: number
    name: string
    hotelName: string
  } | null>(null)

  // Tải danh sách khách sạn & hạng phòng thực tế đang có cấu hình (để bộ lọc đồng bộ 100% với dữ liệu)
  const refreshFilterSummary = useCallback(async () => {
    try {
      const res = await hotelRoomTypeService.filter({
        page: 0,
        pageSize: 1000,
      })
      if (res && res.result && res.result.content) {
        const hotelMap = new Map<number, { id: number; name: string; count: number }>()
        const rtMap = new Map<number, { id: number; name: string; code: string; count: number }>()

        res.result.content.forEach((item) => {
          // Khách sạn đang có cấu hình
          const existingHotel = hotelMap.get(item.hotelId)
          if (existingHotel) {
            existingHotel.count += 1
          } else {
            hotelMap.set(item.hotelId, {
              id: item.hotelId,
              name: item.hotelName,
              count: 1,
            })
          }

          // Hạng phòng đang có cấu hình
          const existingRt = rtMap.get(item.roomTypeId)
          if (existingRt) {
            existingRt.count += 1
          } else {
            rtMap.set(item.roomTypeId, {
              id: item.roomTypeId,
              name: item.roomTypeName,
              code: item.roomTypeCode,
              count: 1,
            })
          }
        })

        setConfiguredHotels(Array.from(hotelMap.values()))
        setConfiguredRoomTypes(Array.from(rtMap.values()))

        // Nếu khách sạn đang chọn không còn cấu hình nào (vừa bị xóa hết), tự động reset về Tất cả
        setSelectedHotelId((prev) => (prev !== '' && !hotelMap.has(Number(prev)) ? '' : prev))
        setSelectedRoomTypeId((prev) => (prev !== '' && !rtMap.has(Number(prev)) ? '' : prev))
      }
    } catch (err: unknown) {
      console.error('Lỗi khi tải danh sách bộ lọc cấu hình phòng:', err)
    }
  }, [])

  // Load Reference Data (Hotels, RoomTypes, TaxCategories) & Filter Summary
  useEffect(() => {
    hotelService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setHotels(res.result.content || [])
      })
      .catch((err: unknown) => console.error('Lỗi khi tải danh sách khách sạn:', err))

    roomTypeService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setRoomTypes(res.result.content || [])
      })
      .catch((err: unknown) => console.error('Lỗi khi tải danh sách loại phòng:', err))

    taxCategoryService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setTaxCategories(res.result.content || [])
      })
      .catch((err: unknown) => console.error('Lỗi khi tải danh mục thuế:', err))

    const timer = setTimeout(() => {
      void refreshFilterSummary()
    }, 0)
    return () => clearTimeout(timer)
  }, [refreshFilterSummary])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await hotelRoomTypeService.filter({
        page,
        pageSize,
        hotelId: selectedHotelId !== '' ? Number(selectedHotelId) : undefined,
        roomTypeId: selectedRoomTypeId !== '' ? Number(selectedRoomTypeId) : undefined,
        status: status || undefined,
      })
      if (response && response.result) {
        setHotelRoomTypes(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err: unknown) {
      console.error('Lỗi khi tải danh sách cấu hình loại phòng:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, selectedHotelId, selectedRoomTypeId, status])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadData()
  }

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(hotelRoomTypes.map((hrt) => hrt.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  const openCreateModal = () => {
    hotelService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result && res.result.content) setHotels(res.result.content)
      })
      .catch(() => {})

    setFormHotelId(hotels.length > 0 ? hotels[0].id : '')
    setFormRoomTypeId(roomTypes.length > 0 ? roomTypes[0].id : '')
    setFormTaxCategoryId(taxCategories.length > 0 ? taxCategories[0].id : '')
    setFormBasePrice(1000000)
    setFormTotalQuantity(5)
    setFormMaxTotalGuests(3)
    setFormMaxBeds(1)
    setFormError(null)
    setIsCreateModalOpen(true)
  }

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (formHotelId === '' || formRoomTypeId === '' || formTaxCategoryId === '') {
      setFormError('Vui lòng chọn đầy đủ Khách sạn, Loại phòng và Nhóm thuế')
      return
    }

    if (formBasePrice <= 0) {
      setFormError('Giá gốc mỗi đêm phải lớn hơn 0 VNĐ')
      return
    }

    if (formTotalQuantity < 0) {
      setFormError('Tổng số phòng cơ sở không được âm')
      return
    }

    if (formMaxTotalGuests < 1) {
      setFormError('Trần tổng khách tối đa tối thiểu là 1 người')
      return
    }

    if (formMaxBeds < 1) {
      setFormError('Số lượng giường tối đa tối thiểu là 1 giường')
      return
    }

    setIsSubmitting(true)
    try {
      const payload: HotelRoomTypeCreateRequest = {
        hotelId: Number(formHotelId),
        roomTypeId: Number(formRoomTypeId),
        taxCategoryId: Number(formTaxCategoryId),
        basePrice: formBasePrice,
        totalQuantity: formTotalQuantity,
        maxTotalGuests: formMaxTotalGuests,
        maxBeds: formMaxBeds,
        status: 'ACTIVE',
      }
      const res = await hotelRoomTypeService.create(payload)
      setIsCreateModalOpen(false)
      void refreshFilterSummary()
      if (res.result) {
        // Chuyển hướng ngay tới trang chi tiết để cấu hình Giường và Tiện ích
        navigate(`/inventory/hotel-room-types/${res.result.id}`)
      } else {
        loadData()
      }
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } }; message?: string }
      setFormError(axiosError.response?.data?.message || axiosError.message || 'Lỗi khi tạo cấu hình phòng')
    } finally {
      setIsSubmitting(false)
    }
  }

  const promptDeleteBatch = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteConfirmOpen(true)
  }

  const promptDeleteSingle = (id: number) => {
    setDeletingIds([id])
    setDeleteConfirmOpen(true)
  }

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return
    setIsDeleting(true)
    try {
      await hotelRoomTypeService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      loadData()
      void refreshFilterSummary()
    } catch (err: unknown) {
      console.error('Lỗi khi xóa cấu hình loại phòng:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  // Options for Filter Selects (calculated dynamically from actual data)
  const hotelOptions: SelectOption<number | ''>[] = useMemo(() => {
    return [
      {
        value: '',
        label: 'Tất cả khách sạn',
      },
      ...configuredHotels.map((h) => ({
        value: h.id,
        label: h.name,
      })),
    ]
  }, [configuredHotels])

  const roomTypeOptions: SelectOption<number | ''>[] = useMemo(() => {
    return [
      {
        value: '',
        label: 'Tất cả loại phòng',
      },
      ...configuredRoomTypes.map((rt) => ({
        value: rt.id,
        label: `${rt.name} (${rt.code})`,
      })),
    ]
  }, [configuredRoomTypes])

  const statusOptions = [
    { value: '', label: 'Tất cả trạng thái' },
    { value: 'ACTIVE', label: 'Đang mở bán (ACTIVE)' },
    { value: 'INACTIVE', label: 'Tạm dừng (INACTIVE)' },
  ]

  const modalHotelOptions = hotels.map((h) => ({ value: h.id, label: h.name }))
  const modalRoomTypeOptions = roomTypes.map((rt) => ({ value: rt.id, label: `${rt.name} (${rt.code})` }))
  const modalTaxOptions = taxCategories.map((t) => ({ value: t.id, label: `${t.categoryName} (${t.categoryCode})` }))

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-red-50 text-red-600 rounded-xl">
              <Building2 className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-neutral-900 tracking-tight">
                Cấu Hình Hạng Phòng Khách Sạn
              </h1>
              <p className="text-sm text-neutral-500">
                Quản lý phân bổ giá, số lượng phòng vật lý và thiết lập giường/tiện ích cho từng cơ sở
              </p>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-2.5">
          <Button
            variant="secondary"
            onClick={() => navigate('/inventory/room-type-matrix')}
            className="shrink-0"
          >
            Ma Trận Phân Bổ Cơ Sở
          </Button>
          <Button onClick={openCreateModal} className="shrink-0">
            <Plus className="w-4 h-4 mr-1.5" />
            Thêm Cấu Hình Phòng
          </Button>
        </div>
      </div>

      {/* 3-Row Layout: Row 1 - Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="flex flex-col md:flex-row gap-3">
          <div className="flex-1">
            <Select
              value={selectedHotelId}
              onChange={(val) => {
                setSelectedHotelId(val !== '' ? Number(val) : '')
                setPage(0)
              }}
              options={hotelOptions}
              placeholder="Chọn khách sạn cơ sở..."
            />
          </div>
          <div className="flex-1">
            <Select
              value={selectedRoomTypeId}
              onChange={(val) => {
                setSelectedRoomTypeId(val !== '' ? Number(val) : '')
                setPage(0)
              }}
              options={roomTypeOptions}
              placeholder="Chọn loại phòng..."
            />
          </div>
          <div className="w-full md:w-52">
            <Select
              value={status}
              onChange={(val) => {
                setStatus(String(val))
                setPage(0)
              }}
              options={statusOptions}
            />
          </div>
          <Button type="submit" variant="secondary" className="shrink-0">
            <Search className="w-4 h-4 mr-1.5" />
            Lọc Dữ Liệu
          </Button>
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              setSelectedHotelId('')
              setSelectedRoomTypeId('')
              setStatus('')
              setPage(0)
              void refreshFilterSummary()
            }}
            className="shrink-0"
            title="Làm mới bộ lọc"
            aria-label="Làm mới bộ lọc"
          >
            <RefreshCw className="w-4 h-4" />
          </Button>
        </form>
      </div>

      {/* 3-Row Layout: Row 2 - Action Toolbar */}
      <div className="flex items-center justify-between px-1">
        <div className="flex items-center gap-3">
          {selectedIds.length > 0 && (
            <Button
              variant="danger"
              size="sm"
              onClick={promptDeleteBatch}
              className="animate-fade-in"
            >
              <Trash2 className="w-4 h-4 mr-1.5" />
              Xóa {selectedIds.length} cấu hình đã chọn
            </Button>
          )}
        </div>
        <div className="text-sm text-neutral-500">
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> cấu hình hạng phòng
        </div>
      </div>

      {/* 3-Row Layout: Row 3 - Data Table */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs xl:text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-50/50 text-neutral-600">
                <th className="py-3 px-2 w-10 text-center whitespace-nowrap">
                  <input
                    type="checkbox"
                    checked={
                      hotelRoomTypes.length > 0 && selectedIds.length === hotelRoomTypes.length
                    }
                    onChange={handleSelectAll}
                    aria-label="Chọn tất cả cấu hình"
                    className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                  />
                </th>
                <th className="py-3 px-2 font-semibold">Khách Sạn</th>
                <th className="py-3 px-2 font-semibold">Hạng Phòng</th>
                <th className="py-3 px-2 font-semibold text-right whitespace-nowrap">Giá Gốc / Đêm</th>
                <th className="py-3 px-1.5 font-semibold text-center whitespace-nowrap">Số Phòng</th>
                <th className="py-3 px-2 font-semibold">Sức Chứa & Giường</th>
                <th className="py-3 px-1.5 font-semibold text-center whitespace-nowrap">Trạng Thái</th>
                <th className="py-3 px-2 font-semibold text-center whitespace-nowrap">Chi Tiết</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200/70">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-neutral-500">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải dữ liệu...
                    </div>
                  </td>
                </tr>
              ) : hotelRoomTypes.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-neutral-500">
                    Không tìm thấy cấu hình hạng phòng nào.
                  </td>
                </tr>
              ) : (
                hotelRoomTypes.map((hrt) => {
                  const isSelected = selectedIds.includes(hrt.id)
                  return (
                    <tr
                      key={hrt.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/20' : ''
                      }`}
                    >
                      <td className="py-2.5 px-2 text-center whitespace-nowrap">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(hrt.id)}
                          aria-label={`Chọn cấu hình ${hrt.roomTypeName}`}
                          className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                        />
                      </td>
                      <td className="py-2.5 px-2 font-medium text-neutral-900 leading-snug">
                        {hrt.hotelName}
                      </td>
                      <td className="py-2.5 px-2 leading-snug">
                        <div className="flex items-center gap-2.5">
                          {hrt.coverImageUrl ? (
                            <img
                              src={hrt.coverImageUrl}
                              alt={hrt.roomTypeName}
                              className="w-10 h-8 object-cover rounded-md border border-neutral-200 shrink-0 shadow-2xs"
                            />
                          ) : (
                            <div className="w-10 h-8 rounded-md bg-neutral-100 border border-neutral-200/80 flex items-center justify-center text-neutral-400 shrink-0">
                              <ImageIcon className="w-4 h-4" />
                            </div>
                          )}
                          <div className="min-w-0">
                            <div className="font-semibold text-neutral-900 leading-tight">{hrt.roomTypeName}</div>
                            <div className="text-[11px] text-neutral-400 font-mono mt-0.5">{hrt.roomTypeCode}</div>
                          </div>
                        </div>
                      </td>
                      <td className="py-2.5 px-2 text-right font-bold text-red-600 whitespace-nowrap">
                        {formatCurrency(hrt.basePrice)}
                      </td>
                      <td className="py-2.5 px-1.5 text-center font-bold text-neutral-800 whitespace-nowrap">
                        {hrt.totalQuantity}
                      </td>
                      <td className="py-2.5 px-2 text-neutral-600">
                        <div className="flex items-center gap-1.5 leading-tight mb-0.5 whitespace-nowrap">
                          <Users className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
                          <span>{hrt.maxTotalGuests} khách ({hrt.standardAdults}L)</span>
                        </div>
                        <div className="flex items-center gap-1.5 leading-tight text-neutral-500 text-[11px] whitespace-nowrap">
                          <BedDouble className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
                          <span>{hrt.maxBeds} giường{hrt.extraBeds > 0 ? ` (+${hrt.extraBeds} phụ)` : ''}</span>
                        </div>
                      </td>
                      <td className="py-2.5 px-1.5 text-center whitespace-nowrap">
                        <span
                          className={`inline-flex items-center justify-center px-2 py-0.5 rounded-full text-[11px] font-semibold whitespace-nowrap ${
                            hrt.status === 'ACTIVE'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                          }`}
                        >
                          {hrt.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                        </span>
                      </td>
                      <td className="py-2.5 px-2 text-center whitespace-nowrap">
                        <div className="inline-flex items-center justify-center gap-1 whitespace-nowrap">
                          <button
                            type="button"
                            onClick={() =>
                              setGalleryRoomType({
                                id: hrt.id,
                                name: hrt.roomTypeName,
                                hotelName: hrt.hotelName,
                              })
                            }
                            className="p-1 text-neutral-400 hover:text-red-600 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer shrink-0"
                            title="Quản lý ảnh hạng phòng"
                            aria-label={`Quản lý ảnh ${hrt.roomTypeName}`}
                          >
                            <ImageIcon className="w-3.5 h-3.5" />
                          </button>
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => navigate(`/inventory/hotel-room-types/${hrt.id}`)}
                            title="Xem chi tiết & Chỉnh sửa Giường, Tiện ích"
                            className="h-7 px-2 text-xs whitespace-nowrap shrink-0"
                          >
                            <Eye className="w-3.5 h-3.5 mr-1 shrink-0" />
                            <span>Cấu Hình</span>
                          </Button>
                          <button
                            type="button"
                            onClick={() => promptDeleteSingle(hrt.id)}
                            className="p-1 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer shrink-0"
                            title="Xóa cấu hình"
                            aria-label={`Xóa cấu hình ${hrt.roomTypeName}`}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
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
        <div className="p-4 border-t border-neutral-200 bg-neutral-50/30">
          <Pagination
            pageNumber={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={setPage}
          />
        </div>
      </div>

      {/* Modal Tạo Mới Nhanh Cấu Hình Phòng */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Thêm Mới Cấu Hình Hạng Phòng Cơ Sở"
        maxWidth="lg"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          {formError && (
            <div className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
                Khách Sạn Cơ Sở <span className="text-red-600">*</span>
              </label>
              <Select
                value={formHotelId}
                onChange={(val) => setFormHotelId(val !== '' ? Number(val) : '')}
                options={modalHotelOptions}
                placeholder="Chọn khách sạn..."
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
                Hạng Phòng Danh Mục <span className="text-red-600">*</span>
              </label>
              <Select
                value={formRoomTypeId}
                onChange={(val) => setFormRoomTypeId(val !== '' ? Number(val) : '')}
                options={modalRoomTypeOptions}
                placeholder="Chọn loại phòng..."
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
                Nhóm Thuế & VAT <span className="text-red-600">*</span>
              </label>
              <Select
                value={formTaxCategoryId}
                onChange={(val) => setFormTaxCategoryId(val !== '' ? Number(val) : '')}
                options={modalTaxOptions}
                placeholder="Chọn nhóm thuế..."
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5">
                Giá Gốc Mỗi Đêm (VNĐ) <span className="text-red-600">*</span>
              </label>
              <CurrencyInput
                value={formBasePrice}
                onChangeValue={(val) => setFormBasePrice(val)}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="flex flex-col">
              <label className="text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5 min-h-[32px] flex items-end">
                Tổng Phòng Cơ Sở
              </label>
              <Input
                type="number"
                min="0"
                value={formTotalQuantity}
                onChange={(e) => setFormTotalQuantity(Number(e.target.value))}
                required
              />
            </div>
            <div className="flex flex-col">
              <label className="text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5 min-h-[32px] flex items-end">
                Tổng Khách Tối Đa
              </label>
              <Input
                type="number"
                min="1"
                value={formMaxTotalGuests}
                onChange={(e) => setFormMaxTotalGuests(Number(e.target.value))}
                required
              />
            </div>
            <div className="flex flex-col">
              <label className="text-xs font-semibold text-neutral-700 uppercase tracking-wider mb-1.5 min-h-[32px] flex items-end">
                Số Giường Tối Đa
              </label>
              <Input
                type="number"
                min="1"
                value={formMaxBeds}
                onChange={(e) => setFormMaxBeds(Number(e.target.value))}
                required
              />
            </div>
          </div>

          <div className="p-3 bg-blue-50/60 rounded-xl border border-blue-100 text-xs text-blue-800">
            💡 <strong>Mẹo:</strong> Sau khi tạo cấu hình cơ bản, hệ thống sẽ tự động chuyển bạn đến màn hình xem chi tiết để phân bổ các loại giường (`HotelRoomTypeBed`) và gắn tiện ích (`HotelRoomTypeFeature`).
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-neutral-200">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsCreateModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy Bỏ
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang tạo...' : 'Tạo & Cấu Hình Chi Tiết'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa cấu hình loại phòng"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} cấu hình hạng phòng đã chọn? Thao tác này sẽ chuyển dữ liệu sang trạng thái đã xóa mềm.`}
        confirmText={isDeleting ? 'Đang xóa...' : 'Xác Nhận Xóa'}
        isLoading={isDeleting}
        variant="danger"
      />

      {/* Media Gallery Modal */}
      {galleryRoomType && (
        <RoomTypeMediaGalleryModal
          isOpen={!!galleryRoomType}
          onClose={() => setGalleryRoomType(null)}
          hotelRoomTypeId={galleryRoomType.id}
          roomTypeName={galleryRoomType.name}
          hotelName={galleryRoomType.hotelName}
          onCoverImageChanged={() => {
            loadData()
            void refreshFilterSummary()
          }}
        />
      )}
    </div>
  )
}

export default HotelRoomTypeListPage
