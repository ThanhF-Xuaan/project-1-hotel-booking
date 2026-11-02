import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, UserCheck, Phone, Mail, CreditCard, History, Calendar, DollarSign, Hotel } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import { guestService } from '../services/identity.service'
import type { GuestDto, GuestStayHistoryDto } from '../types/identity.types'

export const GuestListPage: React.FC = () => {
  const [guests, setGuests] = useState<GuestDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)

  // Filters
  const [fullNameFilter, setFullNameFilter] = useState('')
  const [phoneFilter, setPhoneFilter] = useState('')
  const [identityFilter, setIdentityFilter] = useState('')
  const [emailFilter, setEmailFilter] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal Create / Edit
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingGuest, setEditingGuest] = useState<GuestDto | null>(null)
  const [formFullName, setFormFullName] = useState('')
  const [formPhone, setFormPhone] = useState('')
  const [formEmail, setFormEmail] = useState('')
  const [formBirthDate, setFormBirthDate] = useState('')
  const [formIdentityType, setFormIdentityType] = useState('CCCD')
  const [formIdentityNumber, setFormIdentityNumber] = useState('')
  const [formNationality, setFormNationality] = useState('Việt Nam')
  const [formStatus, setFormStatus] = useState('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Modal Stay History
  const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false)
  const [historyGuest, setHistoryGuest] = useState<GuestDto | null>(null)
  const [stayHistory, setStayHistory] = useState<GuestStayHistoryDto | null>(null)
  const [isLoadingHistory, setIsLoadingHistory] = useState(false)

  // Modal Delete
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const res = await guestService.filter({
        page,
        pageSize,
        fullName: fullNameFilter.trim() || undefined,
        phone: phoneFilter.trim() || undefined,
        identityNumber: identityFilter.trim() || undefined,
        email: emailFilter.trim() || undefined,
      })
      if (res.result) {
        setGuests(res.result.content || [])
        setTotalElements(res.result.totalElements || 0)
        setTotalPages(res.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách khách hàng:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, fullNameFilter, phoneFilter, identityFilter, emailFilter])

  useEffect(() => {
    const timer = setTimeout(() => { void loadData() }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadData()
  }

  const handleResetFilter = () => {
    setFullNameFilter('')
    setPhoneFilter('')
    setIdentityFilter('')
    setEmailFilter('')
    setPage(0)
  }

  // Row selection
  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(guests.map((g) => g.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  // Open Create Modal
  const handleOpenCreate = () => {
    setEditingGuest(null)
    setFormFullName('')
    setFormPhone('')
    setFormEmail('')
    setFormBirthDate('')
    setFormIdentityType('CCCD')
    setFormIdentityNumber('')
    setFormNationality('Việt Nam')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  // Open Edit Modal
  const handleOpenEdit = (guest: GuestDto) => {
    setEditingGuest(guest)
    setFormFullName(guest.fullName)
    setFormPhone(guest.phone)
    setFormEmail(guest.email || '')
    setFormBirthDate(guest.birthDate || '')
    setFormIdentityType(guest.identityType || 'CCCD')
    setFormIdentityNumber(guest.identityNumber || '')
    setFormNationality(guest.nationality || 'Việt Nam')
    setFormStatus(guest.status || 'ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  // Open History Modal
  const handleOpenHistory = async (guest: GuestDto) => {
    setHistoryGuest(guest)
    setIsHistoryModalOpen(true)
    setIsLoadingHistory(true)
    try {
      const res = await guestService.getStayHistory(guest.id)
      if (res.result) {
        setStayHistory(res.result)
      }
    } catch (err) {
      console.error('Không thể tải lịch sử lưu trú:', err)
    } finally {
      setIsLoadingHistory(false)
    }
  }

  // Submit Create / Edit
  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formFullName.trim()) {
      setFormError('Vui lòng nhập họ và tên khách hàng')
      return
    }
    if (!formPhone.trim()) {
      setFormError('Vui lòng nhập số điện thoại')
      return
    }

    setIsSubmitting(true)
    setFormError(null)

    try {
      if (editingGuest) {
        await guestService.update(editingGuest.id, {
          fullName: formFullName.trim(),
          phone: formPhone.trim(),
          email: formEmail.trim() || undefined,
          birthDate: formBirthDate || undefined,
          identityType: formIdentityType,
          identityNumber: formIdentityNumber.trim() || undefined,
          nationality: formNationality.trim() || undefined,
          status: formStatus,
        })
      } else {
        await guestService.create({
          fullName: formFullName.trim(),
          phone: formPhone.trim(),
          email: formEmail.trim() || undefined,
          birthDate: formBirthDate || undefined,
          identityType: formIdentityType,
          identityNumber: formIdentityNumber.trim() || undefined,
          nationality: formNationality.trim() || undefined,
        })
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const errorMsg = (err as { message?: string })?.message || 'Có lỗi xảy ra khi lưu thông tin khách hàng'
      setFormError(errorMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  // Delete Action
  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return
    setIsDeleting(true)
    try {
      await guestService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setDeletingIds([])
      setSelectedIds([])
      loadData()
    } catch (err: unknown) {
      const errorMsg = (err as { message?: string })?.message || 'Không thể xóa hồ sơ khách hàng đã chọn'
      alert(errorMsg)
    } finally {
      setIsDeleting(false)
    }
  }

  const formatCurrency = (val?: number) => {
    if (!val) return '0 ₫'
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <UserCheck className="w-7 h-7 text-red-600" />
            Hồ sơ Khách hàng (CRM)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý thông tin định danh, liên hệ và lịch sử lưu trú của khách lưu trú trong toàn chuỗi
          </p>
        </div>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 sm:p-5 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 items-end">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Họ và tên
            </label>
            <div className="relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
              <input
                type="text"
                value={fullNameFilter}
                onChange={(e) => setFullNameFilter(e.target.value)}
                placeholder="VD: Nguyễn Văn A..."
                className="w-full h-10 sm:h-12 pl-10 pr-4 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Số điện thoại
            </label>
            <div className="relative">
              <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
              <input
                type="text"
                value={phoneFilter}
                onChange={(e) => setPhoneFilter(e.target.value)}
                placeholder="VD: 0987654321..."
                className="w-full h-10 sm:h-12 pl-10 pr-4 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Số CCCD / Hộ chiếu
            </label>
            <div className="relative">
              <CreditCard className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
              <input
                type="text"
                value={identityFilter}
                onChange={(e) => setIdentityFilter(e.target.value)}
                placeholder="VD: 001200001234..."
                className="w-full h-10 sm:h-12 pl-10 pr-4 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
              />
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button
              type="submit"
              variant="primary"
              className="flex-1 h-10 sm:h-12 rounded-xl flex items-center justify-center gap-2"
              disabled={isLoading}
            >
              <Search className="w-4 h-4" />
              <span>Tìm kiếm</span>
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleResetFilter}
              className="h-10 sm:h-12 px-4 rounded-xl flex items-center justify-center gap-1.5 text-neutral-600 hover:text-neutral-900"
              title="Đặt lại bộ lọc"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </div>

      {/* HÀNG 2: ACTION TOOLBAR */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-white px-4 sm:px-5 py-3 rounded-2xl border border-neutral-200/80 shadow-xs">
        <div className="flex items-center gap-3">
          <Button
            variant="primary"
            onClick={handleOpenCreate}
            className="h-10 sm:h-12 px-4 rounded-xl flex items-center gap-2 text-sm font-semibold"
          >
            <Plus className="w-4 h-4" />
            <span>Thêm hồ sơ khách</span>
          </Button>
          {selectedIds.length > 0 && (
            <Button
              variant="danger"
              onClick={() => {
                setDeletingIds(selectedIds)
                setDeleteConfirmOpen(true)
              }}
              className="h-10 sm:h-12 px-4 rounded-xl flex items-center gap-2 text-sm font-semibold"
            >
              <Trash2 className="w-4 h-4" />
              <span>Xóa đã chọn ({selectedIds.length})</span>
            </Button>
          )}
        </div>
        <div className="text-xs text-neutral-500 font-medium">
          Tổng cộng: <span className="font-bold text-neutral-900">{totalElements}</span> hồ sơ
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE & PAGINATION */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-neutral-700">
            <thead className="bg-neutral-50/80 border-b border-neutral-200 text-xs uppercase font-bold text-neutral-600">
              <tr>
                <th className="p-4 w-12 text-center">
                  <input
                    type="checkbox"
                    checked={guests.length > 0 && selectedIds.length === guests.length}
                    onChange={handleSelectAll}
                    className="w-4 h-4 text-red-600 border-neutral-300 rounded focus:ring-red-600 cursor-pointer"
                  />
                </th>
                <th className="p-4">Họ và tên</th>
                <th className="p-4">Liên hệ</th>
                <th className="p-4">Giấy tờ tùy thân</th>
                <th className="p-4">Quốc tịch</th>
                <th className="p-4 text-center">Trạng thái</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="p-12 text-center">
                    <div className="inline-flex items-center gap-2 text-sm text-neutral-500">
                      <RefreshCw className="w-4 h-4 animate-spin text-red-600" />
                      <span>Đang tải danh sách khách hàng...</span>
                    </div>
                  </td>
                </tr>
              ) : guests.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-12 text-center text-neutral-400">
                    Không tìm thấy hồ sơ khách hàng nào phù hợp
                  </td>
                </tr>
              ) : (
                guests.map((guest) => {
                  const isSelected = selectedIds.includes(guest.id)
                  return (
                    <tr
                      key={guest.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(guest.id)}
                          className="w-4 h-4 text-red-600 border-neutral-300 rounded focus:ring-red-600 cursor-pointer"
                        />
                      </td>
                      <td className="p-4">
                        <div className="font-semibold text-neutral-900">{guest.fullName}</div>
                        {guest.birthDate && (
                          <div className="text-xs text-neutral-400 flex items-center gap-1 mt-0.5">
                            <Calendar className="w-3 h-3" />
                            {guest.birthDate}
                          </div>
                        )}
                      </td>
                      <td className="p-4">
                        <div className="flex items-center gap-1.5 text-xs text-neutral-800 font-medium">
                          <Phone className="w-3.5 h-3.5 text-neutral-400" />
                          {guest.phone}
                        </div>
                        {guest.email && (
                          <div className="flex items-center gap-1.5 text-xs text-neutral-500 mt-0.5">
                            <Mail className="w-3.5 h-3.5 text-neutral-400" />
                            {guest.email}
                          </div>
                        )}
                      </td>
                      <td className="p-4">
                        {guest.identityNumber ? (
                          <div className="text-xs">
                            <span className="font-mono font-semibold text-neutral-800">
                              {guest.identityNumber}
                            </span>
                            <span className="ml-1.5 px-1.5 py-0.5 bg-neutral-100 text-[10px] text-neutral-600 rounded">
                              {guest.identityType || 'CCCD'}
                            </span>
                          </div>
                        ) : (
                          <span className="text-xs text-neutral-400 italic">Chưa có</span>
                        )}
                      </td>
                      <td className="p-4 text-xs text-neutral-700">
                        {guest.nationality || 'Việt Nam'}
                      </td>
                      <td className="p-4 text-center">
                        {guest.status === 'ACTIVE' ? (
                          <span className="inline-block text-xs font-semibold px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                            Hoạt động
                          </span>
                        ) : (
                          <span className="inline-block text-xs font-semibold px-2.5 py-1 rounded-full bg-neutral-100 text-neutral-600 border border-neutral-200">
                            Ngừng
                          </span>
                        )}
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => handleOpenHistory(guest)}
                            className="p-2 text-neutral-500 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors"
                            title="Lịch sử lưu trú CRM"
                          >
                            <History className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleOpenEdit(guest)}
                            className="p-2 text-neutral-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="Chỉnh sửa hồ sơ"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setDeletingIds([guest.id])
                              setDeleteConfirmOpen(true)
                            }}
                            className="p-2 text-neutral-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Xóa hồ sơ"
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

        {/* Phân trang */}
        {totalPages > 0 && (
          <Pagination
            pageNumber={page}
            pageSize={pageSize}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={(p) => setPage(p)}
          />
        )}
      </div>

      {/* MODAL: CREATE / EDIT GUEST */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingGuest ? `Cập nhật hồ sơ: ${editingGuest.fullName}` : 'Thêm mới hồ sơ khách hàng'}
        maxWidth="2xl"
      >
        <form onSubmit={handleSubmitForm} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 text-red-700 text-sm rounded-xl border border-red-200 font-medium">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Họ và tên khách hàng"
              value={formFullName}
              onChange={(e) => setFormFullName(e.target.value)}
              placeholder="VD: Nguyễn Văn A"
              required
            />
            <Input
              label="Số điện thoại"
              value={formPhone}
              onChange={(e) => setFormPhone(e.target.value)}
              placeholder="VD: 0987654321"
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Địa chỉ Email"
              type="email"
              value={formEmail}
              onChange={(e) => setFormEmail(e.target.value)}
              placeholder="VD: guest@example.com"
            />
            <Input
              label="Ngày sinh"
              type="date"
              value={formBirthDate}
              onChange={(e) => setFormBirthDate(e.target.value)}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
                Loại giấy tờ
              </label>
              <select
                value={formIdentityType}
                onChange={(e) => setFormIdentityType(e.target.value)}
                className="w-full h-11 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              >
                <option value="CCCD">CCCD / CMND</option>
                <option value="PASSPORT">Hộ chiếu (Passport)</option>
                <option value="OTHER">Khác</option>
              </select>
            </div>
            <div>
              <Input
                label="Số định danh / Hộ chiếu"
                value={formIdentityNumber}
                onChange={(e) => setFormIdentityNumber(e.target.value)}
                placeholder="VD: 001200001234"
              />
            </div>
            <div>
              <Input
                label="Quốc tịch"
                value={formNationality}
                onChange={(e) => setFormNationality(e.target.value)}
                placeholder="VD: Việt Nam"
              />
            </div>
          </div>

          {editingGuest && (
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
                Trạng thái hồ sơ
              </label>
              <select
                value={formStatus}
                onChange={(e) => setFormStatus(e.target.value)}
                className="w-full h-11 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              >
                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
              </select>
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy bỏ
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={isSubmitting}
              className="min-w-[120px]"
            >
              {isSubmitting ? 'Đang lưu...' : editingGuest ? 'Cập nhật' : 'Tạo mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* MODAL: GUEST STAY HISTORY (CRM) */}
      <Modal
        isOpen={isHistoryModalOpen}
        onClose={() => setIsHistoryModalOpen(false)}
        title={`Lịch sử lưu trú CRM: ${historyGuest?.fullName || ''}`}
        maxWidth="4xl"
      >
        <div className="space-y-5">
          {isLoadingHistory ? (
            <div className="py-12 text-center">
              <div className="inline-flex items-center gap-2 text-sm text-neutral-500">
                <RefreshCw className="w-4 h-4 animate-spin text-red-600" />
                <span>Đang tải thông tin lưu trú...</span>
              </div>
            </div>
          ) : (
            <>
              {/* Thẻ thống kê CRM */}
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 bg-red-50/50 border border-red-100 rounded-2xl flex items-center gap-3.5">
                  <div className="w-11 h-11 rounded-xl bg-red-600 text-white flex items-center justify-center font-bold">
                    <Hotel className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs text-neutral-500 font-medium">Tổng số lượt lưu trú</div>
                    <div className="text-xl font-bold text-neutral-900 mt-0.5">
                      {stayHistory?.totalStays || 0} lần
                    </div>
                  </div>
                </div>

                <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-2xl flex items-center gap-3.5">
                  <div className="w-11 h-11 rounded-xl bg-emerald-600 text-white flex items-center justify-center font-bold">
                    <DollarSign className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs text-neutral-500 font-medium">Tổng chi tiêu tích lũy</div>
                    <div className="text-xl font-bold text-neutral-900 mt-0.5">
                      {formatCurrency(stayHistory?.totalSpent)}
                    </div>
                  </div>
                </div>
              </div>

              {/* Danh sách đơn đặt phòng gần nhất */}
              <div>
                <h4 className="text-xs font-bold uppercase text-neutral-600 tracking-wider mb-2.5">
                  Các đơn đặt phòng gần đây
                </h4>
                {(!stayHistory?.bookings || stayHistory.bookings.length === 0) ? (
                  <div className="p-8 text-center text-sm text-neutral-400 bg-neutral-50 rounded-xl border border-neutral-100">
                    Khách hàng chưa có lịch sử đặt phòng nào trong hệ thống
                  </div>
                ) : (
                  <div className="border border-neutral-200 rounded-xl overflow-hidden">
                    <table className="w-full text-left text-xs text-neutral-700">
                      <thead className="bg-neutral-50 font-bold text-neutral-600 border-b border-neutral-200">
                        <tr>
                          <th className="p-3">Mã đơn phòng</th>
                          <th className="p-3">Khách sạn</th>
                          <th className="p-3">Ngày đặt</th>
                          <th className="p-3 text-right">Tổng tiền</th>
                          <th className="p-3 text-center">Trạng thái</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-neutral-100">
                        {stayHistory.bookings.map((b) => (
                          <tr key={b.bookingId} className="hover:bg-neutral-50/50">
                            <td className="p-3 font-mono font-bold text-neutral-900">
                              {b.bookingNumber}
                            </td>
                            <td className="p-3 font-medium text-neutral-800">
                              {b.hotelName || 'Khách sạn UTC'}
                            </td>
                            <td className="p-3 text-neutral-500">
                              {b.bookingDate || '---'}
                            </td>
                            <td className="p-3 text-right font-semibold text-neutral-900">
                              {formatCurrency(b.totalPrice)}
                            </td>
                            <td className="p-3 text-center">
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-neutral-100 text-neutral-700">
                                {b.status}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </>
          )}

          <div className="flex justify-end pt-3 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsHistoryModalOpen(false)}
            >
              Đóng
            </Button>
          </div>
        </div>
      </Modal>

      {/* MODAL: CONFIRM DELETE */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa hồ sơ khách hàng"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} hồ sơ khách hàng đã chọn?`}
        confirmText="Xác nhận xóa"
        cancelText="Hủy"
        isLoading={isDeleting}
        variant="danger"
      />
    </div>
  )
}

export default GuestListPage
