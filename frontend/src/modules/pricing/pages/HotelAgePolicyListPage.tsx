import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Users, Building2, Globe } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import agePolicyService from '../services/agePolicy.service'
import hotelService from '../../organization/services/hotel.service'
import type {
  HotelAgePolicyDto,
  HotelAgePolicyCreateRequest,
  HotelAgePolicyUpdateRequest,
} from '../types/pricing.types'
import type { HotelDto } from '../../organization/types/organization.types'

export const HotelAgePolicyListPage: React.FC = () => {
  const [policies, setPolicies] = useState<HotelAgePolicyDto[]>([])
  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [filterHotelScope, setFilterHotelScope] = useState<string>('') // '' = all, 'CHAIN' = chain only, or hotelId
  const [guestType, setGuestType] = useState<string>('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingPolicy, setEditingPolicy] = useState<HotelAgePolicyDto | null>(null)
  const [isChainDefaultScope, setIsChainDefaultScope] = useState(true)
  const [formHotelId, setFormHotelId] = useState<number | ''>('')
  const [formGuestType, setFormGuestType] = useState<'INFANT' | 'CHILD' | 'ADULT'>('CHILD')
  const [formMinAge, setFormMinAge] = useState<number | ''>(6)
  const [formMaxAge, setFormMaxAge] = useState<number | ''>(11)
  const [formStatus, setFormStatus] = useState('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Load hotels for dropdown
  useEffect(() => {
    const timer = setTimeout(() => {
      hotelService
        .filter({ page: 0, pageSize: 100 })
        .then((res) => {
          if (res.result?.content) {
            setHotels(res.result.content)
          }
        })
        .catch((err) => console.error('Lỗi tải danh sách khách sạn:', err))
    }, 0)
    return () => clearTimeout(timer)
  }, [])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const isChainOnly = filterHotelScope === 'CHAIN'
      const hotelIdNum =
        filterHotelScope && filterHotelScope !== 'CHAIN' ? Number(filterHotelScope) : undefined

      const res = await agePolicyService.filter({
        page,
        pageSize,
        chainOnly: isChainOnly || undefined,
        hotelId: hotelIdNum,
        guestType: guestType || undefined,
        status: status || undefined,
        sortBy: 'guestType',
        sortDirection: 'ASC',
      })
      if (res.result) {
        setPolicies(res.result.content || [])
        setTotalElements(res.result.totalElements)
        setTotalPages(res.result.totalPages)
      }
    } catch (err) {
      console.error('Lỗi tải danh sách chính sách độ tuổi:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, filterHotelScope, guestType, status])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    loadData()
  }

  const handleReset = () => {
    setFilterHotelScope('')
    setGuestType('')
    setStatus('')
    setPage(0)
  }

  const handleOpenCreateModal = () => {
    setEditingPolicy(null)
    setIsChainDefaultScope(true)
    setFormHotelId(hotels.length > 0 ? hotels[0].id : '')
    setFormGuestType('CHILD')
    setFormMinAge(6)
    setFormMaxAge(11)
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleOpenEditModal = (p: HotelAgePolicyDto) => {
    setEditingPolicy(p)
    setIsChainDefaultScope(p.isChainDefault || !p.hotelId)
    setFormHotelId(p.hotelId || (hotels.length > 0 ? hotels[0].id : ''))
    setFormGuestType(p.guestType)
    setFormMinAge(p.minAge)
    setFormMaxAge(p.maxAge)
    setFormStatus(p.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleGuestTypePreset = (type: 'INFANT' | 'CHILD' | 'ADULT') => {
    setFormGuestType(type)
    if (!editingPolicy) {
      if (type === 'INFANT') {
        setFormMinAge(0)
        setFormMaxAge(5)
      } else if (type === 'CHILD') {
        setFormMinAge(6)
        setFormMaxAge(11)
      } else {
        setFormMinAge(12)
        setFormMaxAge(120)
      }
    }
  }

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (formMinAge === '' || formMinAge < 0) {
      setFormError('Độ tuổi tối thiểu không hợp lệ (>= 0)')
      return
    }
    if (formMaxAge === '' || formMaxAge < formMinAge) {
      setFormError('Độ tuổi tối đa phải lớn hơn hoặc bằng độ tuổi tối thiểu')
      return
    }
    if (!isChainDefaultScope && !formHotelId) {
      setFormError('Vui lòng chọn cơ sở khách sạn áp dụng')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingPolicy) {
        const updatePayload: HotelAgePolicyUpdateRequest = {
          minAge: Number(formMinAge),
          maxAge: Number(formMaxAge),
          status: formStatus,
        }
        await agePolicyService.update(editingPolicy.id, updatePayload)
      } else {
        const createPayload: HotelAgePolicyCreateRequest = {
          hotelId: isChainDefaultScope ? undefined : Number(formHotelId),
          guestType: formGuestType,
          minAge: Number(formMinAge),
          maxAge: Number(formMaxAge),
        }
        await agePolicyService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const axiosError = err as {
        response?: {
          data?: {
            message?: string
          }
        }
        message?: string
      }
      setFormError(axiosError.response?.data?.message || axiosError.message || 'Thao tác thất bại')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(policies.map((p) => p.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  const handleDeleteSelected = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteConfirmOpen(true)
  }

  const handleDeleteOne = (id: number) => {
    setDeletingIds([id])
    setDeleteConfirmOpen(true)
  }

  const handleConfirmDelete = async () => {
    setIsDeleting(true)
    try {
      await agePolicyService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds([])
      loadData()
    } catch (err: unknown) {
      const axiosError = err as {
        response?: {
          data?: {
            message?: string
          }
        }
        message?: string
      }
      alert(axiosError.response?.data?.message || axiosError.message || 'Lỗi khi xóa chính sách độ tuổi')
    } finally {
      setIsDeleting(false)
    }
  }

  const getGuestTypeBadge = (type: string) => {
    switch (type) {
      case 'INFANT':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800">
            🍼 Trẻ sơ sinh (Infant)
          </span>
        )
      case 'CHILD':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-sky-100 text-sky-800">
            🧒 Trẻ em (Child)
          </span>
        )
      case 'ADULT':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800">
            🧑 Người lớn (Adult)
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
            {type}
          </span>
        )
    }
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <Users className="w-7 h-7 text-red-600" />
            Chính Sách Độ Tuổi Khách Hàng
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý quy chuẩn phân loại độ tuổi (Sơ sinh, Trẻ em, Người lớn) toàn chuỗi và chính sách ghi đè (override) theo từng cơ sở khách sạn
          </p>
        </div>
        <Button onClick={handleOpenCreateModal} className="shrink-0">
          <Plus className="w-5 h-5 mr-1" />
          Thêm Chính Sách Độ Tuổi
        </Button>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleFilter} className="grid grid-cols-1 sm:grid-cols-4 gap-3">
          <div>
            <Select<string>
              value={filterHotelScope}
              onChange={(val) => setFilterHotelScope(val)}
              options={[
                { value: '', label: 'Tất cả (Toàn chuỗi & Cơ sở)' },
                { value: 'CHAIN', label: '🌐 Chỉ quy chuẩn toàn chuỗi' },
                ...hotels.map((h) => ({
                  value: String(h.id),
                  label: `🏨 ${h.name}`,
                })),
              ]}
              size="lg"
            />
          </div>

          <div>
            <Select<string>
              value={guestType}
              onChange={(val) => setGuestType(val)}
              options={[
                { value: '', label: 'Tất cả loại khách' },
                { value: 'INFANT', label: '🍼 Trẻ sơ sinh (Infant)' },
                { value: 'CHILD', label: '🧒 Trẻ em (Child)' },
                { value: 'ADULT', label: '🧑 Người lớn (Adult)' },
              ]}
              size="lg"
            />
          </div>

          <div>
            <Select<string>
              value={status}
              onChange={(val) => setStatus(val)}
              options={[
                { value: '', label: 'Tất cả trạng thái' },
                { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
                { value: 'INACTIVE', label: 'Ngừng áp dụng (INACTIVE)' },
              ]}
              size="lg"
            />
          </div>

          <div className="flex gap-2">
            <Button type="submit" variant="secondary" className="flex-1 h-10 sm:h-12" isLoading={isLoading}>
              <Search className="w-4 h-4 mr-1.5" />
              Lọc
            </Button>
            <Button type="button" variant="outline" onClick={handleReset} className="h-10 sm:h-12 px-3">
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </div>

      {/* HÀNG 2: ACTION TOOLBAR */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-neutral-50 p-3 rounded-xl border border-neutral-200">
        <div className="flex items-center gap-2">
          {selectedIds.length > 0 && (
            <Button
              type="button"
              variant="danger"
              size="sm"
              onClick={handleDeleteSelected}
              className="flex items-center gap-1.5"
            >
              <Trash2 className="w-4 h-4" />
              Xóa ({selectedIds.length})
            </Button>
          )}
        </div>
        <div className="text-xs text-neutral-500 font-medium">
          Tổng số: <span className="font-semibold text-neutral-800">{totalElements}</span> chính sách
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE & PAGINATION */}
      <div className="bg-white rounded-2xl border border-neutral-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-neutral-200 text-sm">
            <thead className="bg-neutral-50">
              <tr>
                <th className="px-4 py-3 text-left w-10">
                  <input
                    type="checkbox"
                    className="rounded border-neutral-300 text-red-600 focus:ring-red-500"
                    onChange={handleSelectAll}
                    checked={policies.length > 0 && selectedIds.length === policies.length}
                  />
                </th>
                <th className="px-4 py-3 text-left font-semibold text-neutral-700">Phạm vi áp dụng</th>
                <th className="px-4 py-3 text-left font-semibold text-neutral-700">Loại khách</th>
                <th className="px-4 py-3 text-left font-semibold text-neutral-700">Khoảng độ tuổi</th>
                <th className="px-4 py-3 text-left font-semibold text-neutral-700">Trạng thái</th>
                <th className="px-4 py-3 text-right font-semibold text-neutral-700 w-28">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-neutral-500">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-neutral-400" />
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : policies.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-neutral-500">
                    Không tìm thấy chính sách độ tuổi nào phù hợp
                  </td>
                </tr>
              ) : (
                policies.map((p) => {
                  const isChain = p.isChainDefault || !p.hotelId
                  return (
                    <tr key={p.id} className="hover:bg-neutral-50 transition-colors">
                      <td className="px-4 py-3">
                        <input
                          type="checkbox"
                          className="rounded border-neutral-300 text-red-600 focus:ring-red-500"
                          checked={selectedIds.includes(p.id)}
                          onChange={() => handleSelectOne(p.id)}
                        />
                      </td>
                      <td className="px-4 py-3">
                        {isChain ? (
                          <div className="flex items-center gap-1.5 text-xs font-semibold text-purple-700 bg-purple-50 px-2.5 py-1 rounded-md border border-purple-200 w-fit">
                            <Globe className="w-3.5 h-3.5" />
                            Quy chuẩn Toàn Chuỗi
                          </div>
                        ) : (
                          <div className="flex items-center gap-1.5 text-xs font-medium text-neutral-800 bg-neutral-100 px-2.5 py-1 rounded-md border border-neutral-200 w-fit">
                            <Building2 className="w-3.5 h-3.5 text-neutral-500" />
                            {p.hotelName || `Khách sạn #${p.hotelId}`}
                          </div>
                        )}
                      </td>
                      <td className="px-4 py-3">{getGuestTypeBadge(p.guestType)}</td>
                      <td className="px-4 py-3 font-semibold text-neutral-900">
                        {p.minAge} - {p.maxAge} <span className="text-xs font-normal text-neutral-500">tuổi</span>
                        {p.maxAge >= 100 && (
                          <span className="ml-1.5 text-xs text-indigo-600 font-normal">
                            (trở lên)
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        {p.status === 'ACTIVE' ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">
                            Hoạt động
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-neutral-100 text-neutral-600">
                            Ngừng áp dụng
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => handleOpenEditModal(p)}
                            className="p-1.5 text-neutral-500 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                            title="Chỉnh sửa"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDeleteOne(p.id)}
                            className="p-1.5 text-neutral-500 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
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

        {/* Pagination */}
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

      {/* Form Modal (Create / Update) */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingPolicy ? 'Chỉnh Sửa Chính Sách Độ Tuổi' : 'Thêm Mới Chính Sách Độ Tuổi'}
      >
        <form onSubmit={handleFormSubmit} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-xs">
              {formError}
            </div>
          )}

          {/* Scope selection */}
          {!editingPolicy ? (
            <div className="space-y-2">
              <label className="block text-sm font-semibold text-neutral-700">
                Phạm vi áp dụng <span className="text-red-500">*</span>
              </label>
              <div className="flex items-center gap-4 p-3 bg-neutral-50 rounded-lg border border-neutral-200">
                <label className="flex items-center gap-2 cursor-pointer text-sm font-medium text-neutral-700">
                  <input
                    type="radio"
                    name="scopeType"
                    checked={isChainDefaultScope}
                    onChange={() => setIsChainDefaultScope(true)}
                    className="text-red-600 focus:ring-red-500"
                  />
                  <span>🌐 Quy chuẩn toàn chuỗi (Chain Default)</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer text-sm font-medium text-neutral-700">
                  <input
                    type="radio"
                    name="scopeType"
                    checked={!isChainDefaultScope}
                    onChange={() => setIsChainDefaultScope(false)}
                    className="text-red-600 focus:ring-red-500"
                  />
                  <span>🏨 Từng cơ sở khách sạn</span>
                </label>
              </div>

              {!isChainDefaultScope && (
                <div className="mt-2">
                  <label className="block text-xs font-medium text-neutral-600 mb-1">
                    Chọn khách sạn cơ sở <span className="text-red-500">*</span>
                  </label>
                  <Select<number | ''>
                    value={formHotelId}
                    onChange={(val) => setFormHotelId(val ? Number(val) : '')}
                    options={[
                      { value: '', label: '-- Chọn khách sạn --' },
                      ...hotels.map((h) => ({
                        value: h.id,
                        label: h.name,
                      })),
                    ]}
                    size="md"
                  />
                </div>
              )}
            </div>
          ) : (
            <div className="p-3 bg-neutral-50 rounded-lg border border-neutral-200 text-xs text-neutral-600 space-y-1">
              <div>
                <strong>Phạm vi:</strong>{' '}
                {editingPolicy.isChainDefault || !editingPolicy.hotelId ? (
                  <span className="text-purple-700 font-semibold">Quy chuẩn Toàn Chuỗi</span>
                ) : (
                  <span>Khách sạn: {editingPolicy.hotelName || `#${editingPolicy.hotelId}`}</span>
                )}
              </div>
              <div>
                <strong>Loại khách:</strong> {editingPolicy.guestType}
              </div>
            </div>
          )}

          {/* Guest Type selection */}
          {!editingPolicy && (
            <div>
              <label className="block text-sm font-semibold text-neutral-700 mb-1">
                Loại khách hàng <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  onClick={() => handleGuestTypePreset('INFANT')}
                  className={`p-2.5 rounded-lg border text-xs font-semibold text-center transition-all ${
                    formGuestType === 'INFANT'
                      ? 'border-emerald-500 bg-emerald-50 text-emerald-800 ring-2 ring-emerald-400'
                      : 'border-neutral-200 hover:bg-neutral-50 text-neutral-700'
                  }`}
                >
                  🍼 Sơ sinh (Infant)
                  <div className="text-[10px] font-normal text-neutral-500 mt-0.5">0 - 5 tuổi</div>
                </button>
                <button
                  type="button"
                  onClick={() => handleGuestTypePreset('CHILD')}
                  className={`p-2.5 rounded-lg border text-xs font-semibold text-center transition-all ${
                    formGuestType === 'CHILD'
                      ? 'border-sky-500 bg-sky-50 text-sky-800 ring-2 ring-sky-400'
                      : 'border-neutral-200 hover:bg-neutral-50 text-neutral-700'
                  }`}
                >
                  🧒 Trẻ em (Child)
                  <div className="text-[10px] font-normal text-neutral-500 mt-0.5">6 - 11 tuổi</div>
                </button>
                <button
                  type="button"
                  onClick={() => handleGuestTypePreset('ADULT')}
                  className={`p-2.5 rounded-lg border text-xs font-semibold text-center transition-all ${
                    formGuestType === 'ADULT'
                      ? 'border-indigo-500 bg-indigo-50 text-indigo-800 ring-2 ring-indigo-400'
                      : 'border-neutral-200 hover:bg-neutral-50 text-neutral-700'
                  }`}
                >
                  🧑 Người lớn (Adult)
                  <div className="text-[10px] font-normal text-neutral-500 mt-0.5">12+ tuổi</div>
                </button>
              </div>
            </div>
          )}

          {/* Age range */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-neutral-700 mb-1">
                Độ tuổi tối thiểu (Min) <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min={0}
                max={120}
                value={formMinAge}
                onChange={(e) => setFormMinAge(e.target.value === '' ? '' : Number(e.target.value))}
                placeholder="VD: 0, 6, 12"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-neutral-700 mb-1">
                Độ tuổi tối đa (Max) <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min={0}
                max={120}
                value={formMaxAge}
                onChange={(e) => setFormMaxAge(e.target.value === '' ? '' : Number(e.target.value))}
                placeholder="VD: 5, 11, 120"
                required
              />
            </div>
          </div>

          {editingPolicy && (
            <div>
              <label className="block text-sm font-semibold text-neutral-700 mb-1">
                Trạng thái áp dụng
              </label>
              <Select<string>
                value={formStatus}
                onChange={(val) => setFormStatus(val)}
                options={[
                  { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
                  { value: 'INACTIVE', label: 'Ngừng áp dụng (INACTIVE)' },
                ]}
                size="md"
              />
            </div>
          )}

          <div className="p-3 bg-amber-50 rounded-lg border border-amber-200 text-xs text-amber-800">
            ℹ️ <strong>Cơ chế Fallback Hierarchy:</strong> Nếu cơ sở khách sạn không cấu hình chính sách riêng, hệ thống tính giá & phụ thu sẽ tự động áp dụng quy chuẩn toàn chuỗi (Mặc định: 0-5 tuổi là Sơ sinh miễn phí, 6-11 tuổi là Trẻ em, từ 12 tuổi là Người lớn theo Luật Trẻ em 2016).
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button type="submit" variant="primary" isLoading={isSubmitting}>
              {editingPolicy ? 'Cập Nhật' : 'Tạo Mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa chính sách độ tuổi"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} chính sách độ tuổi đã chọn? Khi xóa chính sách cơ sở, cơ sở đó sẽ tự động fallback về quy chuẩn toàn chuỗi.`}
        confirmText="Xác nhận xóa"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default HotelAgePolicyListPage
