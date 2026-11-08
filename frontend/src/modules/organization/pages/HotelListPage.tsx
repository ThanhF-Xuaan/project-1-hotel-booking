import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Building, Globe, Filter, BedDouble, Check } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import Select from '../../../core/components/ui/Select'
import hotelService from '../services/hotel.service'
import regionService from '../services/region.service'
import roomTypeService from '../../inventory/services/roomType.service'
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service'
import { isValidVietnamesePhone, cleanPhoneNumber } from '../../../core/utils/phone'
import type { HotelDto, HotelCreateRequest, HotelUpdateRequest, RegionDto } from '../types/organization.types'
import type { RoomTypeDto, HotelRoomTypeDto } from '../../inventory/types/inventory.types'

const DEFAULT_ROOM_PRICES: Record<string, number> = {
  STD: 1200000,
  SUP: 1500000,
  DLX: 2200000,
  STE: 3500000,
  EXE: 5000000,
}

export const HotelListPage: React.FC = () => {
  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [regions, setRegions] = useState<RegionDto[]>([])
  const [systemRoomTypes, setSystemRoomTypes] = useState<RoomTypeDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [regionId, setRegionId] = useState<number | undefined>(undefined)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingHotel, setEditingHotel] = useState<HotelDto | null>(null)
  const [formRegionId, setFormRegionId] = useState<number | ''>('')
  const [formName, setFormName] = useState('')
  const [formAddress, setFormAddress] = useState('')
  const [formPhone, setFormPhone] = useState('')
  const [formCheckInTime, setFormCheckInTime] = useState('14:00:00')
  const [formCheckOutTime, setFormCheckOutTime] = useState('12:00:00')
  const [formServiceFee, setFormServiceFee] = useState<number>(5.0)
  const [formStatus, setFormStatus] = useState('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Room Types Configuration inside Hotel Modal
  const [selectedRoomTypeIds, setSelectedRoomTypeIds] = useState<number[]>([])
  const [roomTypePrices, setRoomTypePrices] = useState<Record<number, number>>({})
  const [existingHotelRoomTypes, setExistingHotelRoomTypes] = useState<HotelRoomTypeDto[]>([])
  const [existingHotelRoomTypeIds, setExistingHotelRoomTypeIds] = useState<Set<number>>(new Set())
  const [isLoadingHotelRoomTypes, setIsLoadingHotelRoomTypes] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Load Regions and System Room Types on mount
  useEffect(() => {
    regionService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setRegions(res.result.content || [])
      })
      .catch((err) => console.error('Lỗi tải danh sách vùng:', err))

    roomTypeService
      .filter({ pageSize: 50, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setSystemRoomTypes(res.result.content || [])
      })
      .catch((err) => console.error('Lỗi tải danh mục loại phòng:', err))
  }, [])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await hotelService.filter({
        page,
        pageSize,
        regionId: regionId || undefined,
        keyword: keyword || undefined,
        status: status || undefined,
      })
      if (response && response.result) {
        setHotels(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách khách sạn:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, regionId, keyword, status])

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
      setSelectedIds(hotels.map((h) => h.id))
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
    setEditingHotel(null)
    setFormRegionId(regions[0]?.id || '')
    setFormName('')
    setFormAddress('')
    setFormPhone('')
    setFormCheckInTime('14:00:00')
    setFormCheckOutTime('12:00:00')
    setFormServiceFee(5.0)
    setFormStatus('ACTIVE')
    setFormError(null)

    // Pre-select all room types for new hotel
    setSelectedRoomTypeIds(systemRoomTypes.map((rt) => rt.id))
    const initialPrices: Record<number, number> = {}
    systemRoomTypes.forEach((rt) => {
      initialPrices[rt.id] = DEFAULT_ROOM_PRICES[rt.code] || 1500000
    })
    setRoomTypePrices(initialPrices)
    setExistingHotelRoomTypes([])
    setExistingHotelRoomTypeIds(new Set())
    setIsFormModalOpen(true)
  }

  const openEditModal = (hotel: HotelDto) => {
    setEditingHotel(hotel)
    setFormRegionId(hotel.regionId)
    setFormName(hotel.name)
    setFormAddress(hotel.address)
    setFormPhone(hotel.phone || '')
    setFormCheckInTime(hotel.checkInTime)
    setFormCheckOutTime(hotel.checkOutTime)
    setFormServiceFee(hotel.serviceFeePercent)
    setFormStatus(hotel.status)
    setFormError(null)

    // Fetch existing room types of this hotel
    setIsLoadingHotelRoomTypes(true)
    hotelRoomTypeService
      .filter({ hotelId: hotel.id, pageSize: 50 })
      .then((res) => {
        const existing = res.result?.content || []
        setExistingHotelRoomTypes(existing)
        const existingIds = new Set(existing.map((h) => Number(h.roomTypeId)))
        setExistingHotelRoomTypeIds(existingIds)
        setSelectedRoomTypeIds(Array.from(existingIds))

        const prices: Record<number, number> = {}
        systemRoomTypes.forEach((rt) => {
          const found = existing.find((h) => Number(h.roomTypeId) === rt.id)
          prices[rt.id] = found
            ? Number(found.basePrice)
            : DEFAULT_ROOM_PRICES[rt.code] || 1500000
        })
        setRoomTypePrices(prices)
      })
      .catch((err) => {
        console.warn('Lỗi tải danh mục loại phòng của khách sạn:', err)
        setExistingHotelRoomTypeIds(new Set())
      })
      .finally(() => {
        setIsLoadingHotelRoomTypes(false)
      })

    setIsFormModalOpen(true)
  }

  const toggleRoomTypeSelection = (rtId: number) => {
    setSelectedRoomTypeIds((prev) =>
      prev.includes(rtId) ? prev.filter((id) => id !== rtId) : [...prev, rtId]
    )
  }

  const handlePriceChange = (rtId: number, price: number) => {
    setRoomTypePrices((prev) => ({
      ...prev,
      [rtId]: price,
    }))
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formRegionId) {
      setFormError('Vui lòng chọn khu vực trực thuộc')
      return
    }
    if (!formName.trim()) {
      setFormError('Vui lòng nhập tên khách sạn')
      return
    }
    if (!formAddress.trim()) {
      setFormError('Vui lòng nhập địa chỉ')
      return
    }
    if (formPhone.trim() && !isValidVietnamesePhone(formPhone.trim())) {
      setFormError('Số điện thoại không hợp lệ (Ví dụ: 02466668888 đối với máy bàn hoặc 0912345678 đối với di động)')
      return
    }
    if (selectedRoomTypeIds.length === 0) {
      setFormError('Vui lòng chọn ít nhất 1 loại phòng kinh doanh cho cơ sở')
      return
    }

    setIsSubmitting(true)
    setFormError(null)
    try {
      let hotelId: number
      const sanitizedPhone = formPhone.trim() ? cleanPhoneNumber(formPhone.trim()) : undefined
      if (editingHotel) {
        const updatePayload: HotelUpdateRequest = {
          regionId: Number(formRegionId),
          name: formName.trim(),
          address: formAddress.trim(),
          phone: sanitizedPhone,
          checkInTime: formCheckInTime,
          checkOutTime: formCheckOutTime,
          serviceFeePercent: Number(formServiceFee),
          status: formStatus,
        }
        await hotelService.update(editingHotel.id, updatePayload)
        hotelId = editingHotel.id
      } else {
        const createPayload: HotelCreateRequest = {
          regionId: Number(formRegionId),
          name: formName.trim(),
          address: formAddress.trim(),
          phone: sanitizedPhone,
          checkInTime: formCheckInTime,
          checkOutTime: formCheckOutTime,
          serviceFeePercent: Number(formServiceFee),
          status: formStatus,
        }
        const createdRes = await hotelService.create(createPayload)
        hotelId = Number(createdRes.result.id)
      }

      // 1. Soft-delete hotel_room_types for room types that were unchecked
      if (editingHotel) {
        const toDeleteIds = existingHotelRoomTypes
          .filter((hrt) => !selectedRoomTypeIds.includes(Number(hrt.roomTypeId)))
          .map((hrt) => hrt.id)
        if (toDeleteIds.length > 0) {
          await hotelRoomTypeService.deleteBatch(toDeleteIds)
        }
      }

      // 2. Automatically create hotel_room_types for newly selected room types
      for (const rtId of selectedRoomTypeIds) {
        if (!existingHotelRoomTypeIds.has(rtId)) {
          await hotelRoomTypeService.create({
            hotelId: Number(hotelId),
            roomTypeId: Number(rtId),
            taxCategoryId: 1,
            standardAdults: 2,
            standardChildren: 0,
            maxAdults: 2,
            maxChildren: 1,
            maxInfants: 1,
            maxTotalGuests: 3,
            maxBeds: 1,
            extraBeds: 0,
            basePrice: roomTypePrices[rtId] || 1500000,
            totalQuantity: 20,
            status: 'ACTIVE',
          })
        }
      }

      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const errorMsg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Có lỗi xảy ra, vui lòng thử lại!'
      setFormError(errorMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  const confirmDeleteOne = (id: number) => {
    setDeletingIds([id])
    setDeleteConfirmOpen(true)
  }

  const confirmDeleteBatch = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteConfirmOpen(true)
  }

  const executeDelete = async () => {
    setIsDeleting(true)
    try {
      await hotelService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds([])
      loadData()
    } catch (err) {
      console.error('Lỗi xóa khách sạn:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Title & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2.5">
            <Building className="w-6 h-6 text-red-600" />
            Quản lý Khách sạn Cơ sở
          </h2>
          <p className="text-sm text-neutral-500 mt-1">
            Thiết lập danh sách khách sạn trực thuộc chuỗi, giờ nhận/trả phòng và phí dịch vụ
          </p>
        </div>
      </div>

      {/* 3-ROW CRUD LAYOUT */}
      <div className="bg-white rounded-2xl border border-neutral-200 overflow-hidden shadow-xs">
        {/* ROW 1: SEARCH & FILTER BAR */}
        <form
          onSubmit={handleSearch}
          className="p-4 md:p-5 border-b border-neutral-200 bg-neutral-50/50 flex flex-col md:flex-row items-center gap-3"
        >
          <div className="w-full md:w-56">
            <Select<number>
              value={regionId || 0}
              onChange={(val) => setRegionId(val ? Number(val) : undefined)}
              options={[
                { value: 0, label: 'Tất cả khu vực' },
                ...regions.map((r) => ({ value: r.id, label: `${r.name} (${r.code})` })),
              ]}
              prefixIcon={<Globe className="w-4 h-4 text-neutral-400" />}
            />
          </div>

          <div className="flex-1 w-full">
            <Input
              placeholder="Tìm theo tên khách sạn, địa chỉ, số điện thoại..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>

          <div className="w-full md:w-48">
            <Select<string>
              value={status}
              onChange={(val) => setStatus(val)}
              options={[
                { value: '', label: 'Tất cả trạng thái' },
                { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
                { value: 'INACTIVE', label: 'Ngừng hoạt động' },
              ]}
              prefixIcon={<Filter className="w-4 h-4 text-neutral-400" />}
            />
          </div>

          <div className="flex items-center gap-2 w-full md:w-auto">
            <Button type="submit" variant="primary" size="md" className="w-full md:w-auto">
              Tìm kiếm
            </Button>
            <Button
              type="button"
              variant="outline"
              size="md"
              onClick={() => {
                setKeyword('')
                setRegionId(undefined)
                setStatus('')
                setPage(0)
              }}
              title="Làm mới bộ lọc"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>

        {/* ROW 2: ACTION TOOLBAR */}
        <div className="p-4 border-b border-neutral-200 flex items-center justify-between gap-4">
          <div className="flex items-center gap-2.5">
            <Button variant="primary" size="md" onClick={openCreateModal} leftIcon={<Plus className="w-4 h-4" />}>
              Thêm mới Khách sạn
            </Button>

            {selectedIds.length > 0 && (
              <Button
                variant="danger"
                size="md"
                onClick={confirmDeleteBatch}
                leftIcon={<Trash2 className="w-4 h-4" />}
              >
                Xóa đã chọn ({selectedIds.length})
              </Button>
            )}
          </div>

          <span className="text-xs md:text-sm text-neutral-500 font-medium">
            Tổng cộng: <strong className="text-neutral-900">{totalElements}</strong> khách sạn
          </span>
        </div>

        {/* ROW 3: DATA TABLE & PAGINATION */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-neutral-700">
            <thead className="bg-neutral-100/70 text-neutral-900 font-semibold text-xs uppercase tracking-wider border-b border-neutral-200">
              <tr>
                <th className="p-4 w-12 text-center">
                  <input
                    type="checkbox"
                    checked={hotels.length > 0 && selectedIds.length === hotels.length}
                    onChange={handleSelectAll}
                    aria-label="Chọn tất cả khách sạn"
                    className="w-4 h-4 rounded text-red-600 focus:ring-red-600 cursor-pointer"
                  />
                </th>
                <th className="p-4">Tên Khách sạn</th>
                <th className="p-4">Khu vực</th>
                <th className="p-4">Địa chỉ & Liên hệ</th>
                <th className="p-4">Nhận / Trả phòng</th>
                <th className="p-4">Phí DV</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-neutral-500">
                    <div className="flex items-center justify-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải danh sách khách sạn...
                    </div>
                  </td>
                </tr>
              ) : hotels.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-neutral-500">
                    Chưa có khách sạn cơ sở nào được tạo.
                  </td>
                </tr>
              ) : (
                hotels.map((item) => {
                  const isSelected = selectedIds.includes(item.id)
                  return (
                    <tr
                      key={item.id}
                      className={`hover:bg-neutral-50 transition-colors ${isSelected ? 'bg-red-50/30' : ''
                        }`}
                    >
                      <td className="p-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(item.id)}
                          aria-label={`Chọn khách sạn ${item.name}`}
                          className="w-4 h-4 rounded text-red-600 focus:ring-red-600 cursor-pointer"
                        />
                      </td>
                      <td className="p-4 font-bold text-neutral-900">{item.name}</td>
                      <td className="p-4">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-700">
                          {item.regionName}
                        </span>
                      </td>
                      <td className="p-4">
                        <div className="font-medium text-neutral-900">{item.address}</div>
                        {item.phone && (
                          <div className="text-xs text-neutral-500 mt-0.5 font-mono">{item.phone}</div>
                        )}
                      </td>
                      <td className="p-4">
                        <span className="text-xs text-neutral-600">
                          {item.checkInTime} / {item.checkOutTime}
                        </span>
                      </td>
                      <td className="p-4">
                        <span className="font-semibold text-neutral-900">{item.serviceFeePercent}%</span>
                      </td>
                      <td className="p-4">
                        {item.status === 'ACTIVE' ? (
                          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            Hoạt động
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-600 border border-neutral-200">
                            Ngừng hoạt động
                          </span>
                        )}
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => openEditModal(item)}
                            className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                            title="Chỉnh sửa"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => confirmDeleteOne(item.id)}
                            className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                            title="Xóa"
                          >
                            <Trash2 className="w-4 h-4" />
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

        {/* PAGINATION */}
        <div className="p-4 border-t border-neutral-200">
          <Pagination
            pageNumber={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={(newPage) => setPage(newPage)}
          />
        </div>
      </div>

      {/* CREATE & EDIT FORM MODAL */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingHotel ? 'Cập nhật Khách sạn' : 'Thêm mới Khách sạn Cơ sở'}
        maxWidth="2xl"
      >
        <form onSubmit={handleSave} className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-sm font-semibold">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Khu vực trực thuộc *
              </label>
              <Select<number>
                value={Number(formRegionId) || 0}
                onChange={(val) => setFormRegionId(val ? Number(val) : '')}
                options={[
                  { value: 0, label: 'Chọn khu vực', disabled: true },
                  ...regions.map((r) => ({ value: r.id, label: `${r.name} (${r.code})` })),
                ]}
                prefixIcon={<Globe className="w-4 h-4 text-neutral-400" />}
              />
            </div>

            <Input
              label="Tên khách sạn *"
              placeholder="VD: Viettel Luxury Hà Nội"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              required
            />
          </div>

          <Input
            label="Địa chỉ chi tiết *"
            placeholder="VD: Tòa nhà Viettel, Nam Từ Liêm, Hà Nội"
            value={formAddress}
            onChange={(e) => setFormAddress(e.target.value)}
            required
          />

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input
              label="Số điện thoại"
              placeholder="VD: 02466668888"
              value={formPhone}
              onChange={(e) => setFormPhone(e.target.value)}
              error={
                formPhone.trim() && !isValidVietnamesePhone(formPhone.trim())
                  ? 'Số điện thoại không hợp lệ (VD: 02466668888 hoặc 0912345678)'
                  : undefined
              }
            />

            <Input
              label="Giờ nhận phòng"
              type="time"
              value={formCheckInTime}
              onChange={(e) => setFormCheckInTime(e.target.value)}
              required
            />

            <Input
              label="Giờ trả phòng"
              type="time"
              value={formCheckOutTime}
              onChange={(e) => setFormCheckOutTime(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Phí dịch vụ khách sạn (%)"
              type="number"
              step="0.01"
              min="0"
              max="100"
              value={formServiceFee}
              onChange={(e) => setFormServiceFee(parseFloat(e.target.value) || 0)}
              required
            />

            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Trạng thái
              </label>
              <Select<string>
                value={formStatus}
                onChange={(val) => setFormStatus(val)}
                options={[
                  { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
                  { value: 'INACTIVE', label: 'Ngừng hoạt động (INACTIVE)' },
                ]}
              />
            </div>
          </div>

          {/* CẤU HÌNH LOẠI PHÒNG KINH DOANH CHO KHÁCH SẠN (CÁCH 2 - BULK SELECTION) */}
          <div className="pt-3 border-t border-neutral-200 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-xs font-bold text-neutral-700 tracking-wider flex items-center gap-2">
                  <BedDouble className="w-4 h-4 text-red-600" />
                  Cấu Hình Hạng Phòng Kinh Doanh *
                </h4>
              </div>

              <button
                type="button"
                onClick={() => {
                  if (selectedRoomTypeIds.length === systemRoomTypes.length) {
                    setSelectedRoomTypeIds([])
                  } else {
                    setSelectedRoomTypeIds(systemRoomTypes.map((rt) => rt.id))
                  }
                }}
                className="text-xs font-bold text-red-600 hover:text-red-700 cursor-pointer"
              >
                {selectedRoomTypeIds.length === systemRoomTypes.length
                  ? 'Bỏ chọn tất cả'
                  : 'Chọn tất cả'}
              </button>
            </div>

            {isLoadingHotelRoomTypes ? (
              <div className="p-4 text-center text-xs text-neutral-400">
                Đang nạp cấu hình loại phòng hiện tại của khách sạn...
              </div>
            ) : systemRoomTypes.length === 0 ? (
              <div className="p-4 text-center text-xs text-neutral-400 border border-dashed border-neutral-300 rounded-xl">
                Chưa có danh mục loại phòng toàn chuỗi. Vui lòng tạo tại menu Quản lý Loại phòng.
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {systemRoomTypes.map((rt) => {
                  const isChecked = selectedRoomTypeIds.includes(rt.id)
                  const isExisting = existingHotelRoomTypeIds.has(rt.id)
                  const price = roomTypePrices[rt.id] || DEFAULT_ROOM_PRICES[rt.code] || 1500000

                  return (
                    <div
                      key={rt.id}
                      onClick={() => toggleRoomTypeSelection(rt.id)}
                      className={`p-3 rounded-xl border transition-all cursor-pointer flex flex-col justify-between gap-2 ${isChecked
                        ? 'border-red-500 bg-red-50/30'
                        : 'border-neutral-200 bg-white hover:border-neutral-300'
                        }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-2.5">
                          <div
                            className={`w-4 h-4 rounded flex items-center justify-center border transition-colors ${isChecked
                              ? 'bg-red-600 border-red-600 text-white'
                              : 'border-neutral-300 bg-white'
                              }`}
                          >
                            {isChecked && <Check className="w-3 h-3 stroke-[3]" />}
                          </div>
                          <div>
                            <div className="text-xs font-bold text-neutral-900">{rt.name}</div>
                            <span className="text-[10px] font-mono text-neutral-500 font-bold">
                              Mã: {rt.code}
                            </span>
                          </div>
                        </div>

                        {isExisting && (
                          <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-green-100 text-green-700">
                            Đang kinh doanh
                          </span>
                        )}
                      </div>

                      {isChecked && (
                        <div
                          className="pt-2 border-t border-neutral-200 flex items-center justify-between gap-2"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <span className="text-[11px] font-bold text-neutral-600">Đơn giá/đêm:</span>
                          <div className="flex items-center gap-1">
                            <input
                              type="number"
                              min="100000"
                              step="50000"
                              value={price}
                              onChange={(e) =>
                                handlePriceChange(rt.id, Math.max(0, Number(e.target.value) || 0))
                              }
                              className="w-28 h-8 px-2 text-right text-xs font-bold border border-neutral-300 rounded-lg bg-white focus:outline-none focus:ring-1 focus:ring-red-500"
                            />
                            <span className="text-[11px] font-bold text-neutral-500">đ</span>
                          </div>
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy bỏ
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              {editingHotel ? 'Lưu thay đổi' : 'Tạo mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* CONFIRM DELETE MODAL */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={executeDelete}
        title="Xác nhận xóa khách sạn?"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length === 1 ? 'khách sạn này' : `${deletingIds.length} khách sạn đã chọn`
          }? Dữ liệu sẽ được chuyển sang trạng thái xóa mềm.`}
        confirmText="Xác nhận xóa"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default HotelListPage
