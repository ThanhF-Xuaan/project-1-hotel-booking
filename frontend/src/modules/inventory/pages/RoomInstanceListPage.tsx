import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, DoorOpen } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import roomInstanceService from '../services/roomInstance.service'
import hotelRoomTypeService from '../services/hotelRoomType.service'
import hotelService from '../../organization/services/hotel.service'
import type {
  RoomInstanceDto,
  RoomInstanceCreateRequest,
  RoomInstanceUpdateRequest,
  HotelRoomTypeDto,
} from '../types/inventory.types'
import type { HotelDto } from '../../organization/types/organization.types'

export const RoomInstanceListPage: React.FC = () => {
  const [rooms, setRooms] = useState<RoomInstanceDto[]>([])
  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [hotelRoomTypes, setHotelRoomTypes] = useState<HotelRoomTypeDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [selectedHotelId, setSelectedHotelId] = useState<number | undefined>(undefined)
  const [roomNumber, setRoomNumber] = useState('')
  const [currentStatus, setCurrentStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingRoom, setEditingRoom] = useState<RoomInstanceDto | null>(null)
  const [formHotelId, setFormHotelId] = useState<number | ''>('')
  const [formHotelRoomTypeId, setFormHotelRoomTypeId] = useState<number | ''>('')
  const [formRoomNumber, setFormRoomNumber] = useState('')
  const [formStatus, setFormStatus] = useState<string>('READY')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [existingHotelRooms, setExistingHotelRooms] = useState<RoomInstanceDto[]>([])

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Load Hotels for Dropdown
  useEffect(() => {
    hotelService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setHotels(res.result.content || [])
      })
      .catch((err) => console.error('Lỗi tải danh sách khách sạn:', err))
  }, [])

  // Load Hotel Room Types & existing rooms when hotel selected in Form
  useEffect(() => {
    if (formHotelId) {
      hotelRoomTypeService
        .filter({ hotelId: Number(formHotelId), pageSize: 50, status: 'ACTIVE' })
        .then((res) => {
          if (res.result) setHotelRoomTypes(res.result.content || [])
        })
        .catch((err) => console.error('Lỗi tải danh mục loại phòng khách sạn:', err))

      // Tải danh sách các phòng hiện có của khách sạn này để kiểm tra tránh trùng tên phòng
      roomInstanceService
        .filter({ hotelId: Number(formHotelId), pageSize: 100 })
        .then((res) => {
          if (res.result) setExistingHotelRooms(res.result.content || [])
        })
        .catch((err) => console.error('Lỗi tải danh sách phòng hiện có:', err))
    } else {
      setHotelRoomTypes([])
      setExistingHotelRooms([])
    }
  }, [formHotelId])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await roomInstanceService.filter({
        page,
        pageSize,
        hotelId: selectedHotelId || undefined,
        roomNumber: roomNumber || undefined,
        currentStatus: currentStatus || undefined,
      })
      if (response && response.result) {
        setRooms(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách phòng vật lý:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, selectedHotelId, roomNumber, currentStatus])

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
      setSelectedIds(rooms.map((r) => r.id))
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
    setEditingRoom(null)
    const initialHotelId = selectedHotelId || (hotels.length > 0 ? hotels[0].id : '')
    setFormHotelId(initialHotelId)
    setFormHotelRoomTypeId('')
    setFormRoomNumber('')
    setFormStatus('READY')
    setFormError(null)
    setIsFormModalOpen(true)
    if (initialHotelId) {
      roomInstanceService
        .filter({ hotelId: Number(initialHotelId), pageSize: 100 })
        .then((res) => {
          if (res.result) setExistingHotelRooms(res.result.content || [])
        })
        .catch((err) => console.error('Lỗi tải danh sách phòng hiện có:', err))
    }
  }

  const openEditModal = (room: RoomInstanceDto) => {
    setEditingRoom(room)
    setFormHotelId(room.hotelId)
    setFormHotelRoomTypeId(room.hotelRoomTypeId)
    setFormRoomNumber(room.roomNumber)
    setFormStatus(room.currentStatus)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const isDuplicateRoomNumber = Boolean(
    formRoomNumber.trim() &&
    existingHotelRooms.some(
      (r) =>
        r.roomNumber.trim().toLowerCase() === formRoomNumber.trim().toLowerCase() &&
        (!editingRoom || r.id !== editingRoom.id)
    )
  )

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!formHotelId || !formHotelRoomTypeId || !formRoomNumber.trim()) {
      setFormError('Vui lòng điền đầy đủ các thông tin bắt buộc')
      return
    }

    if (isDuplicateRoomNumber) {
      setFormError(
        `Số phòng "${formRoomNumber.trim()}" đã tồn tại trong khách sạn này. Vui lòng chọn số phòng khác!`
      )
      return
    }

    setIsSubmitting(true)
    try {
      if (editingRoom) {
        const updatePayload: RoomInstanceUpdateRequest = {
          hotelRoomTypeId: Number(formHotelRoomTypeId),
          roomNumber: formRoomNumber.trim(),
          currentStatus: formStatus,
        }
        await roomInstanceService.update(editingRoom.id, updatePayload)
      } else {
        const createPayload: RoomInstanceCreateRequest = {
          hotelId: Number(formHotelId),
          hotelRoomTypeId: Number(formHotelRoomTypeId),
          roomNumber: formRoomNumber.trim(),
          currentStatus: formStatus,
        }
        await roomInstanceService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: any) {
      const errorMsg =
        err?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Có lỗi xảy ra khi lưu thông tin phòng')
      setFormError(errorMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleQuickStatusChange = async (id: number, newStatus: string) => {
    try {
      await roomInstanceService.updateStatus(id, newStatus)
      loadData()
    } catch (err) {
      console.error('Lỗi khi đổi trạng thái phòng:', err)
    }
  }

  const promptDeleteSingle = (id: number) => {
    setDeletingIds([id])
    setDeleteConfirmOpen(true)
  }

  const promptDeleteBatch = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteConfirmOpen(true)
  }

  const handleConfirmDelete = async () => {
    setIsDeleting(true)
    try {
      await roomInstanceService.deleteBatch(deletingIds)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      setDeleteConfirmOpen(false)
      loadData()
    } catch (err) {
      console.error('Lỗi khi xóa phòng:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'READY':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            Sẵn sàng
          </span>
        )
      case 'OCCUPIED':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-red-50 text-red-700 border border-red-200">
            Có khách
          </span>
        )
      case 'CLEANING':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            Đang dọn dẹp
          </span>
        )
      case 'MAINTENANCE':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-600 border border-neutral-300">
            Bảo trì
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-600 border border-neutral-200">
            {status}
          </span>
        )
    }
  }

  return (
    <div className="space-y-6">
      {/* Header Title */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <DoorOpen className="w-7 h-7 text-red-600" />
            Danh Sách Phòng Vật Lý
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý số phòng cụ thể và trạng thái dọn dẹp, bảo trì tại từng khách sạn
          </p>
        </div>
        <Button onClick={openCreateModal} className="shrink-0">
          <Plus className="w-5 h-5 mr-1" />
          Thêm Phòng Mới
        </Button>
      </div>

      {/* 3-Row Layout: Row 1 - Search & Filter */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-4 gap-3">
          <div>
            <select
              value={selectedHotelId || ''}
              onChange={(e) =>
                setSelectedHotelId(e.target.value ? Number(e.target.value) : undefined)
              }
              className="w-full h-10 sm:h-12 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả khách sạn</option>
              {hotels.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <Input
              placeholder="Số phòng (VD: 301, P402)..."
              value={roomNumber}
              onChange={(e) => setRoomNumber(e.target.value)}
              className="h-10 sm:h-12"
            />
          </div>

          <div>
            <select
              value={currentStatus}
              onChange={(e) => setCurrentStatus(e.target.value)}
              className="w-full h-10 sm:h-12 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="READY">Sẵn sàng (READY)</option>
              <option value="OCCUPIED">Có khách (OCCUPIED)</option>
              <option value="CLEANING">Đang dọn dẹp (CLEANING)</option>
              <option value="MAINTENANCE">Bảo trì (MAINTENANCE)</option>
            </select>
          </div>

          <div className="flex gap-2">
            <Button type="submit" variant="secondary" className="flex-1 h-10 sm:h-12">
              <Search className="w-4 h-4 mr-1.5" />
              Lọc
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setSelectedHotelId(undefined)
                setRoomNumber('')
                setCurrentStatus('')
                setPage(0)
              }}
              className="h-10 sm:h-12"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
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
              Xóa {selectedIds.length} phòng đã chọn
            </Button>
          )}
        </div>
        <div className="text-sm text-neutral-500">
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> phòng
        </div>
      </div>

      {/* 3-Row Layout: Row 3 - Data Table */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-50/50 text-neutral-600">
                <th className="py-3.5 px-4 w-12 text-center">
                  <input
                    type="checkbox"
                    checked={rooms.length > 0 && selectedIds.length === rooms.length}
                    onChange={handleSelectAll}
                    aria-label="Chọn tất cả phòng"
                    className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                  />
                </th>
                <th className="py-3.5 px-4 font-semibold">Số Phòng</th>
                <th className="py-3.5 px-4 font-semibold">Khách Sạn</th>
                <th className="py-3.5 px-4 font-semibold">Loại Phòng</th>
                <th className="py-3.5 px-4 font-semibold">Trạng Thái Hiện Tại</th>
                <th className="py-3.5 px-4 font-semibold text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200/70">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-500">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải danh sách phòng...
                    </div>
                  </td>
                </tr>
              ) : rooms.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-500">
                    Không tìm thấy phòng vật lý nào.
                  </td>
                </tr>
              ) : (
                rooms.map((room) => {
                  const isSelected = selectedIds.includes(room.id)
                  return (
                    <tr
                      key={room.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/20' : ''
                      }`}
                    >
                      <td className="py-3 px-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(room.id)}
                          aria-label={`Chọn phòng ${room.roomNumber}`}
                          className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                        />
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-neutral-900 text-base">
                        {room.roomNumber}
                      </td>
                      <td className="py-3 px-4 text-neutral-900">{room.hotelName}</td>
                      <td className="py-3 px-4">
                        <div className="font-medium text-neutral-900">
                          {room.roomTypeName}
                        </div>
                        <div className="text-xs text-neutral-500 font-mono">
                          {room.roomTypeCode}
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          {getStatusBadge(room.currentStatus)}
                          <select
                            value={room.currentStatus}
                            onChange={(e) =>
                              handleQuickStatusChange(room.id, e.target.value)
                            }
                            aria-label={`Chuyển trạng thái phòng ${room.roomNumber}`}
                            className="text-xs px-2 py-1 rounded-lg border border-neutral-200 bg-neutral-50 text-neutral-700 focus:outline-none cursor-pointer"
                          >
                            <option value="READY">Sẵn sàng</option>
                            <option value="OCCUPIED">Có khách</option>
                            <option value="CLEANING">Dọn dẹp</option>
                            <option value="MAINTENANCE">Bảo trì</option>
                          </select>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEditModal(room)}
                            aria-label={`Chỉnh sửa phòng ${room.roomNumber}`}
                          >
                            <Edit2 className="w-4 h-4 text-neutral-600" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => promptDeleteSingle(room.id)}
                            aria-label={`Xóa phòng ${room.roomNumber}`}
                          >
                            <Trash2 className="w-4 h-4 text-red-600" />
                          </Button>
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
        <div className="p-4 border-t border-neutral-200/80">
          <Pagination
            pageNumber={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={(newPage) => setPage(newPage)}
          />
        </div>
      </div>

      {/* Form Modal */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingRoom ? 'Chỉnh Sửa Phòng Vật Lý' : 'Thêm Mới Phòng Vật Lý'}
      >
        <form onSubmit={handleSubmitForm} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 text-red-600 text-sm rounded-xl border border-red-200">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Khách Sạn <span className="text-red-500">*</span>
            </label>
            <select
              value={formHotelId}
              onChange={(e) => setFormHotelId(Number(e.target.value))}
              disabled={!!editingRoom}
              className="w-full h-11 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
              required
            >
              <option value="">Chọn khách sạn</option>
              {hotels.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Cấu Hình Loại Phòng <span className="text-red-500">*</span>
            </label>
            <select
              value={formHotelRoomTypeId}
              onChange={(e) => setFormHotelRoomTypeId(Number(e.target.value))}
              className="w-full h-11 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
              required
            >
              <option value="">Chọn loại phòng</option>
              {hotelRoomTypes.map((hrt) => (
                <option key={hrt.id} value={hrt.id}>
                  {hrt.roomTypeName} ({hrt.roomTypeCode})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Số Phòng / Tên Phòng <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: 301, 302, VIP-01..."
              value={formRoomNumber}
              onChange={(e) => {
                setFormRoomNumber(e.target.value)
                if (formError) setFormError(null)
              }}
              className={isDuplicateRoomNumber ? 'border-red-500 focus:ring-red-500' : ''}
              required
            />
            {isDuplicateRoomNumber && (
              <p className="text-red-600 text-xs mt-1.5 font-medium flex items-center gap-1">
                <span>⚠️</span> Số phòng &quot;{formRoomNumber.trim()}&quot; đã tồn tại trong khách sạn này. Vui lòng chọn số khác!
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Trạng Thái Ban Đầu
            </label>
            <select
              value={formStatus}
              onChange={(e) => setFormStatus(e.target.value)}
              className="w-full h-11 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="READY">Sẵn sàng (READY)</option>
              <option value="OCCUPIED">Có khách (OCCUPIED)</option>
              <option value="CLEANING">Đang dọn dẹp (CLEANING)</option>
              <option value="MAINTENANCE">Bảo trì (MAINTENANCE)</option>
            </select>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-neutral-200">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              isLoading={isSubmitting}
              disabled={isSubmitting || isDuplicateRoomNumber}
            >
              {editingRoom ? 'Cập Nhật' : 'Tạo Mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa phòng vật lý"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} phòng đã chọn? Thao tác này sẽ chuyển dữ liệu vào thùng rác.`}
        confirmText="Xác nhận xóa"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default RoomInstanceListPage
