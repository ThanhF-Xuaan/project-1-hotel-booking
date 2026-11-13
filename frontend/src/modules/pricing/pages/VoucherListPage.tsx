import React, { useState, useEffect, useCallback } from 'react'
import {
  Plus,
  Trash2,
  RefreshCw,
  Search,
  Edit2,
  Ticket,
  Calendar,
  AlertCircle,
  Building,
  CheckCircle2,
  Clock,
  History,
  X,
} from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select, { type SelectOption } from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import voucherService from '../services/voucher.service'
import hotelService from '../../organization/services/hotel.service'
import type {
  VoucherDto,
  VoucherCreateRequest,
  VoucherUpdateRequest,
  VoucherUsageDto,
} from '../types/voucher.types'
import type { HotelDto } from '../../organization/types/organization.types'
import { formatCurrency } from '../../../core/utils/currency'

export const VoucherListPage: React.FC = () => {
  const [vouchers, setVouchers] = useState<VoucherDto[]>([])
  const [hotels, setHotels] = useState<HotelDto[]>([])

  // Pagination & Filter State
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const pageSize = 10
  const [keyword, setKeyword] = useState('')
  const [filterHotelId, setFilterHotelId] = useState<number | undefined>(undefined)
  const [filterDiscountType, setFilterDiscountType] = useState<string>('')
  const [filterApplyScope, setFilterApplyScope] = useState<string>('')
  const [filterStatus, setFilterStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingVoucher, setEditingVoucher] = useState<VoucherDto | null>(null)
  const [formHotelId, setFormHotelId] = useState<number | ''>('')
  const [formVoucherCode, setFormVoucherCode] = useState('')
  const [formTitle, setFormTitle] = useState('')
  const [formDescription, setFormDescription] = useState('')
  const [formDiscountType, setFormDiscountType] = useState<'PERCENT' | 'FIXED'>('PERCENT')
  const [formDiscountValue, setFormDiscountValue] = useState<number>(10)
  const [formApplyScope, setFormApplyScope] = useState<'ROOM_ONLY' | 'TOTAL_BOOKING' | 'SERVICE_ONLY'>('ROOM_ONLY')
  const [formMinOrderAmount, setFormMinOrderAmount] = useState<number>(0)
  const [formMaxDiscountAmount, setFormMaxDiscountAmount] = useState<number | ''>('')
  const [formTotalQuantity, setFormTotalQuantity] = useState<number>(100)
  const [formMaxUsagePerUser, setFormMaxUsagePerUser] = useState<number>(1)
  const [formValidFrom, setFormValidFrom] = useState('')
  const [formValidTo, setFormValidTo] = useState('')
  const [formStatus, setFormStatus] = useState<'ACTIVE' | 'INACTIVE' | 'EXPIRED'>('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Usage History Drawer State
  const [selectedUsageVoucher, setSelectedUsageVoucher] = useState<VoucherDto | null>(null)
  const [usages, setUsages] = useState<VoucherUsageDto[]>([])
  const [isLoadingUsages, setIsLoadingUsages] = useState(false)

  // Load hotels on mount
  useEffect(() => {
    let active = true
    hotelService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (active && res.result) setHotels(res.result.content || [])
      })
      .catch((err: unknown) => console.error('Lỗi tải danh mục khách sạn:', err))
    return () => {
      active = false
    }
  }, [])

  // Load vouchers callback
  const loadVouchers = useCallback(async () => {
    setIsLoading(true)
    setLoadError(null)
    try {
      const res = await voucherService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        hotelId: filterHotelId,
        discountType: filterDiscountType || undefined,
        applyScope: filterApplyScope || undefined,
        status: filterStatus || undefined,
        sortBy: 'id',
        sortDirection: 'DESC',
      })

      if (res.result) {
        setVouchers(res.result.content || [])
        setTotalElements(res.result.totalElements || 0)
        setTotalPages(res.result.totalPages || 0)
      }
    } catch (err: unknown) {
      console.error('Lỗi tải danh sách voucher:', err)
      const axiosErr = err as { response?: { data?: { message?: string } }; message?: string }
      const msg =
        axiosErr?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Không thể kết nối đến máy chủ để tải danh sách voucher.')
      setLoadError(msg)
      setVouchers([])
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, filterHotelId, filterDiscountType, filterApplyScope, filterStatus])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadVouchers()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadVouchers])

  // Open modal for create
  const handleOpenCreateModal = () => {
    setEditingVoucher(null)
    setFormHotelId('')
    setFormVoucherCode('')
    setFormTitle('')
    setFormDescription('')
    setFormDiscountType('PERCENT')
    setFormDiscountValue(10)
    setFormApplyScope('ROOM_ONLY')
    setFormMinOrderAmount(0)
    setFormMaxDiscountAmount('')
    setFormTotalQuantity(100)
    setFormMaxUsagePerUser(1)

    const now = new Date()
    const nextMonth = new Date()
    nextMonth.setMonth(nextMonth.getMonth() + 1)

    const formatToLocalDatetime = (d: Date) => {
      const pad = (n: number) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
    }

    setFormValidFrom(formatToLocalDatetime(now))
    setFormValidTo(formatToLocalDatetime(nextMonth))
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  // Open modal for edit
  const handleOpenEditModal = (voucher: VoucherDto) => {
    setEditingVoucher(voucher)
    setFormHotelId(voucher.hotelId || '')
    setFormVoucherCode(voucher.voucherCode)
    setFormTitle(voucher.title)
    setFormDescription(voucher.description || '')
    setFormDiscountType(voucher.discountType)
    setFormDiscountValue(voucher.discountValue)
    setFormApplyScope(voucher.applyScope)
    setFormMinOrderAmount(voucher.minOrderAmount || 0)
    setFormMaxDiscountAmount(voucher.maxDiscountAmount || '')
    setFormTotalQuantity(voucher.totalQuantity)
    setFormMaxUsagePerUser(voucher.maxUsagePerUser || 1)

    const formatISOToLocalDatetime = (isoStr: string) => {
      if (!isoStr) return ''
      const d = new Date(isoStr)
      const pad = (n: number) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
    }

    setFormValidFrom(formatISOToLocalDatetime(voucher.validFrom))
    setFormValidTo(formatISOToLocalDatetime(voucher.validTo))
    setFormStatus(voucher.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  // Open usage history drawer
  const handleOpenUsageDrawer = async (voucher: VoucherDto) => {
    setSelectedUsageVoucher(voucher)
    setIsLoadingUsages(true)
    try {
      const res = await voucherService.getUsages(voucher.id)
      if (res.result) {
        setUsages(res.result)
      } else {
        setUsages([])
      }
    } catch {
      setUsages([])
    } finally {
      setIsLoadingUsages(false)
    }
  }

  // Submit Form
  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!formTitle.trim()) {
      setFormError('Vui lòng nhập tiêu đề voucher')
      return
    }

    if (!editingVoucher && !formVoucherCode.trim()) {
      setFormError('Vui lòng nhập mã code voucher')
      return
    }

    if (!formValidFrom || !formValidTo) {
      setFormError('Vui lòng chọn thời gian bắt đầu và kết thúc')
      return
    }

    const start = new Date(formValidFrom)
    const end = new Date(formValidTo)
    if (start >= end) {
      setFormError('Thời gian bắt đầu phải trước thời gian kết thúc')
      return
    }

    if (formDiscountValue <= 0) {
      setFormError('Giá trị chiết khấu phải lớn hơn 0')
      return
    }

    if (formDiscountType === 'PERCENT' && formDiscountValue > 100) {
      setFormError('Chiết khấu phần trăm không được vượt quá 100%')
      return
    }

    setIsSubmitting(true)
    try {
      const validFromISO = new Date(formValidFrom).toISOString()
      const validToISO = new Date(formValidTo).toISOString()

      if (editingVoucher) {
        const updatePayload: VoucherUpdateRequest = {
          hotelId: formHotelId !== '' ? Number(formHotelId) : null,
          title: formTitle.trim(),
          description: formDescription.trim() || undefined,
          discountType: formDiscountType,
          discountValue: Number(formDiscountValue),
          applyScope: formApplyScope,
          minOrderAmount: Number(formMinOrderAmount) || 0,
          maxDiscountAmount:
            formDiscountType === 'PERCENT' && formMaxDiscountAmount !== ''
              ? Number(formMaxDiscountAmount)
              : null,
          totalQuantity: Number(formTotalQuantity),
          maxUsagePerUser: Number(formMaxUsagePerUser),
          validFrom: validFromISO,
          validTo: validToISO,
          status: formStatus,
        }
        await voucherService.update(editingVoucher.id, updatePayload)
      } else {
        const createPayload: VoucherCreateRequest = {
          hotelId: formHotelId !== '' ? Number(formHotelId) : null,
          voucherCode: formVoucherCode.trim().toUpperCase(),
          title: formTitle.trim(),
          description: formDescription.trim() || undefined,
          discountType: formDiscountType,
          discountValue: Number(formDiscountValue),
          applyScope: formApplyScope,
          minOrderAmount: Number(formMinOrderAmount) || 0,
          maxDiscountAmount:
            formDiscountType === 'PERCENT' && formMaxDiscountAmount !== ''
              ? Number(formMaxDiscountAmount)
              : null,
          totalQuantity: Number(formTotalQuantity),
          maxUsagePerUser: Number(formMaxUsagePerUser),
          validFrom: validFromISO,
          validTo: validToISO,
          status: formStatus,
        }
        await voucherService.create(createPayload)
      }

      setIsFormModalOpen(false)
      void loadVouchers()
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } }; message?: string }
      setFormError(axiosErr.response?.data?.message || axiosErr.message || 'Có lỗi xảy ra khi lưu voucher')
    } finally {
      setIsSubmitting(false)
    }
  }

  // Selection handlers
  const handleToggleSelectAll = () => {
    if (selectedIds.length === vouchers.length) {
      setSelectedIds([])
    } else {
      setSelectedIds(vouchers.map((v) => v.id))
    }
  }

  const handleToggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  // Delete handlers
  const handleOpenDeleteSingle = (id: number) => {
    setDeletingIds([id])
    setDeleteConfirmOpen(true)
  }

  const handleOpenDeleteBatch = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteConfirmOpen(true)
  }

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return
    setIsDeleting(true)
    try {
      await voucherService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds([])
      void loadVouchers()
    } catch (err: unknown) {
      console.error('Lỗi xóa voucher:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  const formatDateDisplay = (isoStr: string) => {
    if (!isoStr) return '-'
    const d = new Date(isoStr)
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
  }

  // Filter Select Options
  const hotelFilterOptions: SelectOption<number | ''>[] = [
    { value: '', label: 'Tất cả cơ sở (Toàn chuỗi & Khách sạn)' },
    ...hotels.map((h) => ({ value: h.id, label: h.name })),
  ]

  const discountTypeFilterOptions: SelectOption<string>[] = [
    { value: '', label: 'Tất cả hình thức giảm' },
    { value: 'PERCENT', label: 'Giảm theo phần trăm (%)' },
    { value: 'FIXED', label: 'Giảm số tiền cố định (VNĐ)' },
  ]

  const applyScopeFilterOptions: SelectOption<string>[] = [
    { value: '', label: 'Tất cả phạm vi áp dụng' },
    { value: 'ROOM_ONLY', label: 'Chỉ tiền phòng (ROOM_ONLY)' },
    { value: 'TOTAL_BOOKING', label: 'Toàn bộ hóa đơn (TOTAL_BOOKING)' },
    { value: 'SERVICE_ONLY', label: 'Dịch vụ ăn uống/spa (SERVICE_ONLY)' },
  ]

  const statusFilterOptions: SelectOption<string>[] = [
    { value: '', label: 'Tất cả trạng thái' },
    { value: 'ACTIVE', label: 'Đang kích hoạt (ACTIVE)' },
    { value: 'INACTIVE', label: 'Tạm ngưng (INACTIVE)' },
    { value: 'EXPIRED', label: 'Đã hết hạn/hết lượt (EXPIRED)' },
  ]

  // Form Select Options
  const formHotelOptions: SelectOption<number | ''>[] = [
    { value: '', label: 'Toàn chuỗi (Mọi cơ sở đều áp dụng)' },
    ...hotels.map((h) => ({ value: h.id, label: h.name })),
  ]

  const formScopeOptions: SelectOption<'ROOM_ONLY' | 'TOTAL_BOOKING' | 'SERVICE_ONLY'>[] = [
    { value: 'ROOM_ONLY', label: 'Chỉ giảm tiền phòng (ROOM_ONLY)' },
    { value: 'TOTAL_BOOKING', label: 'Giảm toàn bộ hóa đơn (TOTAL_BOOKING)' },
    { value: 'SERVICE_ONLY', label: 'Chỉ giảm dịch vụ ăn uống/spa (SERVICE_ONLY)' },
  ]

  const formTypeOptions: SelectOption<'PERCENT' | 'FIXED'>[] = [
    { value: 'PERCENT', label: 'Phần trăm (%)' },
    { value: 'FIXED', label: 'Số tiền cố định (VNĐ)' },
  ]

  const formStatusOptions: SelectOption<'ACTIVE' | 'INACTIVE' | 'EXPIRED'>[] = [
    { value: 'ACTIVE', label: 'Đang kích hoạt (ACTIVE)' },
    { value: 'INACTIVE', label: 'Tạm ngưng (INACTIVE)' },
    { value: 'EXPIRED', label: 'Hết hạn (EXPIRED)' },
  ]

  return (
    <div className="space-y-6">
      {/* HEADER TIÊU ĐỀ */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-red-50 text-red-600 rounded-xl">
              <Ticket className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-neutral-900">Quản Lý Mã Giảm Giá & Voucher</h1>
              <p className="text-sm text-neutral-500 mt-0.5">
                Thiết lập các mã khuyến mại phát hành cho khách hàng nhập áp dụng khi đặt phòng hoặc thanh toán
              </p>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            onClick={() => void loadVouchers()}
            disabled={isLoading}
            className="flex items-center gap-2 border-neutral-200 hover:bg-neutral-50 h-10 px-4 rounded-xl text-neutral-700"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
          <Button
            onClick={handleOpenCreateModal}
            className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white shadow-xs h-10 px-5 rounded-xl font-medium"
          >
            <Plus className="w-4 h-4" />
            Thêm Voucher Mới
          </Button>
        </div>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {/* Keyword Search */}
          <div className="relative">
            <Input
              placeholder="Tìm mã code, tiêu đề..."
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value)
                setPage(0)
                setSelectedIds([])
              }}
              className="pl-9 h-11 rounded-xl"
            />
            <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-3.5 pointer-events-none" />
          </div>

          {/* Hotel Filter */}
          <Select<number | ''>
            value={filterHotelId !== undefined ? filterHotelId : ''}
            onChange={(val) => {
              setFilterHotelId(val !== '' ? Number(val) : undefined)
              setPage(0)
              setSelectedIds([])
            }}
            options={hotelFilterOptions}
            size="md"
          />

          {/* Discount Type */}
          <Select<string>
            value={filterDiscountType}
            onChange={(val) => {
              setFilterDiscountType(val)
              setPage(0)
              setSelectedIds([])
            }}
            options={discountTypeFilterOptions}
            size="md"
          />

          {/* Apply Scope */}
          <Select<string>
            value={filterApplyScope}
            onChange={(val) => {
              setFilterApplyScope(val)
              setPage(0)
              setSelectedIds([])
            }}
            options={applyScopeFilterOptions}
            size="md"
          />

          {/* Status */}
          <Select<string>
            value={filterStatus}
            onChange={(val) => {
              setFilterStatus(val)
              setPage(0)
              setSelectedIds([])
            }}
            options={statusFilterOptions}
            size="md"
          />
        </div>
      </div>

      {/* HÀNG 2: ACTION TOOLBAR */}
      {selectedIds.length > 0 && (
        <div className="flex items-center justify-between bg-red-50/80 border border-red-200 px-5 py-3 rounded-xl animate-in fade-in duration-200">
          <div className="flex items-center gap-2 text-sm text-red-900 font-medium">
            <span className="w-2 h-2 rounded-full bg-red-600 animate-pulse"></span>
            Đã chọn <strong className="text-red-700">{selectedIds.length}</strong> voucher
          </div>
          <Button
            variant="danger"
            onClick={handleOpenDeleteBatch}
            className="flex items-center gap-2 h-9 px-4 rounded-lg text-sm bg-red-600 hover:bg-red-700 text-white"
          >
            <Trash2 className="w-4 h-4" />
            Xóa {selectedIds.length} bản ghi
          </Button>
        </div>
      )}

      {/* HÀNG 3: DATA TABLE & PAGINATION */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-neutral-50/75 border-b border-neutral-200 text-xs font-semibold text-neutral-600 uppercase tracking-wider">
                <th className="py-4 px-4 w-12 text-center">
                  <input
                    type="checkbox"
                    checked={vouchers.length > 0 && selectedIds.length === vouchers.length}
                    onChange={handleToggleSelectAll}
                    className="w-4 h-4 rounded-sm border-neutral-300 text-red-600 focus:ring-red-500"
                  />
                </th>
                <th className="py-4 px-4">Mã Voucher & Tiêu Đề</th>
                <th className="py-4 px-4">Mức Giảm & Điều Kiện</th>
                <th className="py-4 px-4">Phạm Vi & Cơ Sở</th>
                <th className="py-4 px-4 w-48">Tiến Độ Phát Hành</th>
                <th className="py-4 px-4">Hạn Sử Dụng</th>
                <th className="py-4 px-4 text-center">Trạng Thái</th>
                <th className="py-4 px-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200/75 text-sm text-neutral-700">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-500" />
                    Đang tải dữ liệu vouchers...
                  </td>
                </tr>
              ) : loadError ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-red-600 space-y-3">
                    <AlertCircle className="w-10 h-10 mx-auto text-red-500" />
                    <div className="font-semibold text-sm">{loadError}</div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => void loadVouchers()}
                      className="inline-flex items-center gap-1.5 border-neutral-300 hover:bg-neutral-50 text-xs"
                    >
                      <RefreshCw className="w-3.5 h-3.5" />
                      Thử lại
                    </Button>
                  </td>
                </tr>
              ) : vouchers.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-neutral-400">
                    <Ticket className="w-10 h-10 mx-auto mb-2 text-neutral-300" />
                    Không tìm thấy mã giảm giá nào phù hợp bộ lọc
                  </td>
                </tr>
              ) : (
                vouchers.map((v) => {
                  const usedPercent =
                    v.totalQuantity > 0
                      ? Math.min(100, Math.round((v.usedQuantity / v.totalQuantity) * 100))
                      : 0

                  return (
                    <tr
                      key={v.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        selectedIds.includes(v.id) ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="py-4 px-4 text-center">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(v.id)}
                          onChange={() => handleToggleSelect(v.id)}
                          className="w-4 h-4 rounded-sm border-neutral-300 text-red-600 focus:ring-red-500"
                        />
                      </td>

                      {/* Mã Voucher & Tiêu Đề */}
                      <td className="py-4 px-4">
                        <div className="flex items-start gap-2.5">
                          <span className="font-mono font-bold text-red-700 bg-red-50 border border-red-200/80 px-2 py-0.5 rounded-md text-xs tracking-wide">
                            {v.voucherCode}
                          </span>
                        </div>
                        <div className="font-semibold text-neutral-900 mt-1 line-clamp-1">
                          {v.title}
                        </div>
                        {v.description && (
                          <div className="text-xs text-neutral-500 line-clamp-1 mt-0.5">
                            {v.description}
                          </div>
                        )}
                      </td>

                      {/* Mức Giảm & Điều Kiện */}
                      <td className="py-4 px-4">
                        <div className="font-bold text-neutral-900">
                          {v.discountType === 'PERCENT' ? (
                            <span className="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200 text-xs">
                              Giảm {v.discountValue}%
                            </span>
                          ) : (
                            <span className="text-blue-700 bg-blue-50 px-2 py-0.5 rounded-md border border-blue-200 text-xs">
                              Giảm {formatCurrency(v.discountValue)}
                            </span>
                          )}
                        </div>
                        {v.maxDiscountAmount && v.discountType === 'PERCENT' && (
                          <div className="text-xs text-neutral-500 mt-1">
                            Tối đa: {formatCurrency(v.maxDiscountAmount)}
                          </div>
                        )}
                        {v.minOrderAmount && v.minOrderAmount > 0 ? (
                          <div className="text-xs text-neutral-500 mt-0.5">
                            Đơn tối thiểu: {formatCurrency(v.minOrderAmount)}
                          </div>
                        ) : null}
                      </td>

                      {/* Phạm Vi & Cơ Sở */}
                      <td className="py-4 px-4">
                        <div className="flex flex-col gap-1">
                          {v.hotelId ? (
                            <span className="inline-flex items-center gap-1 text-xs text-purple-700 bg-purple-50 px-2 py-0.5 rounded-md border border-purple-200 w-fit">
                              <Building className="w-3 h-3" />
                              {v.hotelName || `Cơ sở #${v.hotelId}`}
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-xs text-blue-700 bg-blue-50 px-2 py-0.5 rounded-md border border-blue-200 w-fit">
                              <Building className="w-3 h-3" />
                              Toàn chuỗi áp dụng
                            </span>
                          )}

                          <span className="text-[11px] text-neutral-500 font-mono">
                            {v.applyScope === 'ROOM_ONLY'
                              ? 'Chỉ trừ tiền phòng'
                              : v.applyScope === 'TOTAL_BOOKING'
                              ? 'Trừ toàn đơn đặt'
                              : 'Chỉ trừ dịch vụ'}
                          </span>
                        </div>
                      </td>

                      {/* Tiến Độ Phát Hành (Progress Bar) */}
                      <td className="py-4 px-4">
                        <div className="flex items-center justify-between text-xs font-medium mb-1 text-neutral-700">
                          <span>
                            {v.usedQuantity} / {v.totalQuantity}
                          </span>
                          <span className="text-neutral-500">{usedPercent}%</span>
                        </div>
                        <div className="w-full bg-neutral-100 rounded-full h-2 overflow-hidden border border-neutral-200/60">
                          <div
                            className={`h-full rounded-full transition-all duration-300 ${
                              usedPercent >= 100
                                ? 'bg-red-500'
                                : usedPercent >= 75
                                ? 'bg-amber-500'
                                : 'bg-emerald-500'
                            }`}
                            style={{ width: `${usedPercent}%` }}
                          />
                        </div>
                        <div className="text-[11px] text-neutral-400 mt-1">
                          Tối đa {v.maxUsagePerUser} lần/khách
                        </div>
                      </td>

                      {/* Hạn Sử Dụng */}
                      <td className="py-4 px-4 text-xs text-neutral-600">
                        <div className="flex items-center gap-1.5 text-neutral-700">
                          <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                          <span>
                            {formatDateDisplay(v.validFrom)} - {formatDateDisplay(v.validTo)}
                          </span>
                        </div>
                      </td>

                      {/* Trạng Thái */}
                      <td className="py-4 px-4 text-center">
                        {v.status === 'ACTIVE' ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3" />
                            Đang chạy
                          </span>
                        ) : v.status === 'EXPIRED' ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-neutral-100 text-neutral-600 border border-neutral-200">
                            <Clock className="w-3 h-3" />
                            Hết hạn
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                            <AlertCircle className="w-3 h-3" />
                            Tạm ngưng
                          </span>
                        )}
                      </td>

                      {/* Thao Tác */}
                      <td className="py-4 px-4 text-right space-x-1 whitespace-nowrap">
                        <button
                          onClick={() => void handleOpenUsageDrawer(v)}
                          className="p-1.5 text-neutral-500 hover:text-purple-600 hover:bg-purple-50 rounded-lg transition-colors"
                          title="Xem lịch sử sử dụng"
                        >
                          <History className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleOpenEditModal(v)}
                          className="p-1.5 text-neutral-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                          title="Sửa voucher"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleOpenDeleteSingle(v.id)}
                          className="p-1.5 text-neutral-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                          title="Xóa voucher"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Phân Trang */}
        <div className="p-4 border-t border-neutral-200 bg-neutral-50/50">
          <Pagination
            pageNumber={page}
            pageSize={pageSize}
            totalElements={totalElements}
            totalPages={totalPages}
            onPageChange={(newPage) => setPage(newPage)}
          />
        </div>
      </div>

      {/* MODAL THÊM / SỬA VOUCHER */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => !isSubmitting && setIsFormModalOpen(false)}
        title={editingVoucher ? `Chỉnh Sửa Voucher: ${editingVoucher.voucherCode}` : 'Tạo Mới Voucher Khuyến Mãi'}
        description="Cấu hình các điều kiện chiết khấu, hạn mức và thời gian phát hành voucher"
        maxWidth="2xl"
      >
        <form onSubmit={handleSubmitForm} className="space-y-3">
          {formError && (
            <div className="p-2.5 bg-red-50 border border-red-200 text-red-700 rounded-xl text-xs sm:text-sm flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          {/* Section 1: Thông tin cơ bản */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Mã Code Voucher <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="VD: SUMMER2026 (Viết hoa, chữ và số)"
                value={formVoucherCode}
                onChange={(e) => setFormVoucherCode(e.target.value.toUpperCase().replace(/\s+/g, ''))}
                disabled={Boolean(editingVoucher)}
                className="h-11 md:h-11 font-mono uppercase font-bold text-xs sm:text-sm px-3.5 bg-white"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Tiêu Đề Voucher <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="VD: Ưu đãi mùa hè 2026"
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
                className="h-11 md:h-11 text-xs sm:text-sm px-3.5 bg-white"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
              Mô Tả Chi Tiết
            </label>
            <Input
              placeholder="VD: Giảm 15% tiền phòng áp dụng cho đặt phòng từ 2 đêm..."
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
              className="h-11 md:h-11 text-xs sm:text-sm px-3.5 bg-white"
            />
          </div>

          {/* Section 2: Cơ sở & Phạm vi */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-neutral-100">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Cơ Sở Áp Dụng
              </label>
              <Select<number | ''>
                value={formHotelId}
                onChange={(val) => setFormHotelId(val !== '' ? Number(val) : '')}
                options={formHotelOptions}
                size="md"
                className="[&>button]:bg-white [&>button]:h-11 [&>button]:text-xs sm:[&>button]:text-sm [&>button]:font-normal [&>button]:px-3.5"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Phạm Vi Giảm Trừ
              </label>
              <Select<'ROOM_ONLY' | 'TOTAL_BOOKING' | 'SERVICE_ONLY'>
                value={formApplyScope}
                onChange={(val) => setFormApplyScope(val)}
                options={formScopeOptions}
                size="md"
                className="[&>button]:bg-white [&>button]:h-11 [&>button]:text-xs sm:[&>button]:text-sm [&>button]:font-normal [&>button]:px-3.5"
              />
            </div>
          </div>

          {/* Section 3: Mức giảm & Điều kiện */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-neutral-100">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Loại Giảm Giá
              </label>
              <Select<'PERCENT' | 'FIXED'>
                value={formDiscountType}
                onChange={(val) => setFormDiscountType(val)}
                options={formTypeOptions}
                size="md"
                className="[&>button]:bg-white [&>button]:h-11 [&>button]:text-xs sm:[&>button]:text-sm [&>button]:font-normal [&>button]:px-3.5"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Giá Trị Giảm <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min={1}
                max={formDiscountType === 'PERCENT' ? 100 : undefined}
                value={formDiscountValue}
                onChange={(e) => setFormDiscountValue(Number(e.target.value))}
                className="h-11 md:h-11 text-xs sm:text-sm px-3.5 bg-white"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Giảm Tối Đa (VNĐ)
              </label>
              <Input
                type="number"
                placeholder={formDiscountType === 'PERCENT' ? 'Vd: 200000' : 'Không áp dụng'}
                value={formMaxDiscountAmount}
                onChange={(e) =>
                  setFormMaxDiscountAmount(e.target.value ? Number(e.target.value) : '')
                }
                disabled={formDiscountType !== 'PERCENT'}
                className="h-11 md:h-11 text-xs sm:text-sm px-3.5 bg-white"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Đơn Hàng Tối Thiểu (VNĐ)
              </label>
              <Input
                type="number"
                min={0}
                value={formMinOrderAmount}
                onChange={(e) => setFormMinOrderAmount(Number(e.target.value))}
                className="h-11 md:h-11 text-xs sm:text-sm px-3.5 bg-white"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Tổng Lượt Phát Hành <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min={1}
                value={formTotalQuantity}
                onChange={(e) => setFormTotalQuantity(Number(e.target.value))}
                className="h-11 md:h-11 text-xs sm:text-sm px-3.5 bg-white"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Giới Hạn Lượt / Khách <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min={1}
                value={formMaxUsagePerUser}
                onChange={(e) => setFormMaxUsagePerUser(Number(e.target.value))}
                className="h-11 md:h-11 text-xs sm:text-sm px-3.5 bg-white"
                required
              />
            </div>
          </div>

          {/* Section 4: Thời gian hiệu lực & Trạng thái */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-neutral-100">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Ngày Bắt Đầu <span className="text-red-500">*</span>
              </label>
              <Input
                type="datetime-local"
                value={formValidFrom}
                onChange={(e) => setFormValidFrom(e.target.value)}
                className="h-11 md:h-11 text-xs sm:text-sm px-3.5 bg-white"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Ngày Kết Thúc <span className="text-red-500">*</span>
              </label>
              <Input
                type="datetime-local"
                value={formValidTo}
                onChange={(e) => setFormValidTo(e.target.value)}
                className="h-11 md:h-11 text-xs sm:text-sm px-3.5 bg-white"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1">
                Trạng Thái
              </label>
              <Select<'ACTIVE' | 'INACTIVE' | 'EXPIRED'>
                value={formStatus}
                onChange={(val) => setFormStatus(val)}
                options={formStatusOptions}
                size="md"
                className="[&>button]:bg-white [&>button]:h-11 [&>button]:text-xs sm:[&>button]:text-sm [&>button]:font-normal [&>button]:px-3.5"
              />
            </div>
          </div>

          {/* Footer actions */}
          <div className="flex justify-end gap-3 pt-3 border-t border-neutral-200">
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
              disabled={isSubmitting}
              className="bg-red-600 hover:bg-red-700 text-white px-5"
            >
              {isSubmitting ? (
                <RefreshCw className="w-4 h-4 animate-spin mr-2" />
              ) : null}
              {editingVoucher ? 'Cập Nhật Voucher' : 'Tạo Voucher'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* DRAWER LỊCH SỬ SỬ DỤNG VOUCHER */}
      {selectedUsageVoucher && (
        <div className="fixed inset-0 z-50 overflow-hidden">
          <div
            className="absolute inset-0 bg-neutral-900/40 backdrop-blur-xs transition-opacity"
            onClick={() => setSelectedUsageVoucher(null)}
          />
          <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
            <div className="w-screen max-w-md bg-white shadow-xl flex flex-col">
              <div className="p-5 border-b border-neutral-200 flex items-center justify-between bg-neutral-50">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 bg-purple-100 text-purple-700 rounded-lg">
                    <History className="w-5 h-5" />
                  </div>
                  <div>
                    <h2 className="text-base font-bold text-neutral-900">Lịch Sử Sử Dụng</h2>
                    <p className="text-xs text-neutral-500 font-mono">
                      Mã: {selectedUsageVoucher.voucherCode} ({selectedUsageVoucher.usedQuantity}/{selectedUsageVoucher.totalQuantity} lượt)
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedUsageVoucher(null)}
                  className="p-1.5 text-neutral-400 hover:text-neutral-700 rounded-lg hover:bg-neutral-100"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto p-5 space-y-3">
                {isLoadingUsages ? (
                  <div className="py-12 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-purple-600" />
                    Đang tải nhật ký lượt dùng...
                  </div>
                ) : usages.length === 0 ? (
                  <div className="py-12 text-center text-neutral-400">
                    <Ticket className="w-8 h-8 mx-auto mb-2 text-neutral-300" />
                    Chưa có lượt khách nào sử dụng mã này
                  </div>
                ) : (
                  usages.map((u) => (
                    <div
                      key={u.id}
                      className="p-3.5 bg-neutral-50 rounded-xl border border-neutral-200/80 space-y-1.5 text-xs"
                    >
                      <div className="flex items-center justify-between font-semibold text-neutral-900">
                        <span>{u.guestFullName || `Khách hàng #${u.bookingGuestId}`}</span>
                        <span className="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 font-bold">
                          -{formatCurrency(u.discountAmount)}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-neutral-500 text-[11px]">
                        <span>SĐT: {u.guestPhoneNumber || 'Không có'}</span>
                        <span>{new Date(u.usedAt).toLocaleString('vi-VN')}</span>
                      </div>
                    </div>
                  ))
                )}
              </div>

              <div className="p-4 border-t border-neutral-200 bg-neutral-50">
                <Button
                  variant="outline"
                  onClick={() => setSelectedUsageVoucher(null)}
                  className="w-full justify-center"
                >
                  Đóng
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL XÁC NHẬN XÓA */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => !isDeleting && setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa mã giảm giá?"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} mã voucher đã chọn? Thao tác này sẽ ẩn voucher và người dùng không thể áp dụng được nữa.`}
        confirmText="Xác nhận xóa"
        cancelText="Hủy bỏ"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default VoucherListPage
