import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Building } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import hotelService from '../services/hotel.service'
import regionService from '../services/region.service'
import type { HotelDto, HotelCreateRequest, HotelUpdateRequest, RegionDto } from '../types/organization.types'

export const HotelListPage: React.FC = () => {
  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [regions, setRegions] = useState<RegionDto[]>([])
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

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Load Regions for Dropdown
  useEffect(() => {
    regionService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setRegions(res.result.content || [])
      })
      .catch((err) => console.error('Lỗi tải danh sách vùng:', err))
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
    loadData()
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
    setIsFormModalOpen(true)
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

    setIsSubmitting(true)
    setFormError(null)
    try {
      if (editingHotel) {
        const updatePayload: HotelUpdateRequest = {
          regionId: Number(formRegionId),
          name: formName.trim(),
          address: formAddress.trim(),
          phone: formPhone.trim() || undefined,
          checkInTime: formCheckInTime,
          checkOutTime: formCheckOutTime,
          serviceFeePercent: Number(formServiceFee),
          status: formStatus,
        }
        await hotelService.update(editingHotel.id, updatePayload)
      } else {
        const createPayload: HotelCreateRequest = {
          regionId: Number(formRegionId),
          name: formName.trim(),
          address: formAddress.trim(),
          phone: formPhone.trim() || undefined,
          checkInTime: formCheckInTime,
          checkOutTime: formCheckOutTime,
          serviceFeePercent: Number(formServiceFee),
          status: formStatus,
        }
        await hotelService.create(createPayload)
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
            Danh sách các khách sạn thành viên trong chuỗi, cấu hình phí dịch vụ và thời gian nhận/trả phòng
          </p>
        </div>
      </div>

      {/* STANDARD 3-ROW LAYOUT */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        {/* ROW 1: SEARCH & FILTER BAR */}
        <form
          onSubmit={handleSearch}
          className="p-4 md:p-5 border-b border-neutral-200 bg-neutral-50/50 flex flex-col md:flex-row items-center gap-3"
        >
          <div className="w-full md:w-56">
            <select
              value={regionId || ''}
              onChange={(e) => setRegionId(e.target.value ? Number(e.target.value) : undefined)}
              className="w-full h-10 md:h-12 px-3 text-sm text-neutral-900 bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả khu vực</option>
              {regions.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name} ({r.code})
                </option>
              ))}
            </select>
          </div>

          <div className="flex-1 w-full">
            <Input
              placeholder="Tìm theo tên khách sạn, địa chỉ, số điện thoại..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>

          <div className="w-full md:w-44">
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full h-10 md:h-12 px-3 text-sm text-neutral-900 bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
            </select>
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
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : hotels.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-neutral-500">
                    Không tìm thấy khách sạn nào.
                  </td>
                </tr>
              ) : (
                hotels.map((hotel) => {
                  const isChecked = selectedIds.includes(hotel.id)
                  return (
                    <tr
                      key={hotel.id}
                      className={`hover:bg-neutral-50/80 transition-colors ${
                        isChecked ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-4 text-center">
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => handleSelectOne(hotel.id)}
                          className="w-4 h-4 rounded text-red-600 focus:ring-red-600 cursor-pointer"
                        />
                      </td>
                      <td className="p-4 font-bold text-neutral-900">
                        {hotel.name}
                        <span className="block text-xs font-normal text-neutral-400">ID #{hotel.id}</span>
                      </td>
                      <td className="p-4">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-neutral-100 text-neutral-800">
                          {hotel.regionName}
                        </span>
                      </td>
                      <td className="p-4 max-w-xs">
                        <div className="truncate text-neutral-800">{hotel.address}</div>
                        {hotel.phone && (
                          <div className="text-xs text-neutral-500 font-mono mt-0.5">{hotel.phone}</div>
                        )}
                      </td>
                      <td className="p-4 text-xs font-mono text-neutral-600">
                        <div>In: {hotel.checkInTime}</div>
                        <div>Out: {hotel.checkOutTime}</div>
                      </td>
                      <td className="p-4 font-semibold text-neutral-900">{hotel.serviceFeePercent}%</td>
                      <td className="p-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                            hotel.status === 'ACTIVE'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-300'
                          }`}
                        >
                          {hotel.status === 'ACTIVE' ? 'Hoạt động' : 'Ngừng'}
                        </span>
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => openEditModal(hotel)}
                            className="p-2 text-neutral-600 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                            title="Sửa khách sạn"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => confirmDeleteOne(hotel.id)}
                            className="p-2 text-red-600 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                            title="Xóa khách sạn"
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
        <Pagination
          pageNumber={page}
          pageSize={pageSize}
          totalElements={totalElements}
          totalPages={totalPages}
          onPageChange={(newPage) => setPage(newPage)}
        />
      </div>

      {/* FORM MODAL (ADD / EDIT) */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingHotel ? 'Cập nhật Khách sạn' : 'Thêm mới Khách sạn Cơ sở'}
        maxWidth="lg"
      >
        <form onSubmit={handleSave} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-xl text-sm">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-neutral-800">Khu vực trực thuộc *</label>
              <select
                value={formRegionId}
                onChange={(e) => setFormRegionId(Number(e.target.value))}
                className="w-full h-10 md:h-12 px-3 text-sm text-neutral-900 bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-600"
                required
              >
                <option value="">-- Chọn khu vực --</option>
                {regions.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name} ({r.code})
                  </option>
                ))}
              </select>
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

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-neutral-800">Trạng thái</label>
              <select
                value={formStatus}
                onChange={(e) => setFormStatus(e.target.value)}
                className="w-full h-10 md:h-12 px-3 text-sm text-neutral-900 bg-white border border-neutral-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-600"
              >
                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
              </select>
            </div>
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
        description={`Bạn có chắc chắn muốn xóa ${
          deletingIds.length === 1 ? 'khách sạn này' : `${deletingIds.length} khách sạn đã chọn`
        }? Dữ liệu sẽ được chuyển sang trạng thái xóa mềm.`}
        confirmText="Xác nhận xóa"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default HotelListPage
