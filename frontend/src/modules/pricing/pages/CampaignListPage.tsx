import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Megaphone } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import campaignService from '../services/campaign.service'
import hotelService from '../../organization/services/hotel.service'
import type {
  CampaignDto,
  CampaignCreateRequest,
  CampaignUpdateRequest,
} from '../types/pricing.types'
import type { HotelDto } from '../../organization/types/organization.types'

export const CampaignListPage: React.FC = () => {
  const [campaigns, setCampaigns] = useState<CampaignDto[]>([])
  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [selectedHotelId, setSelectedHotelId] = useState<number | undefined>(undefined)
  const [name, setName] = useState('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingCampaign, setEditingCampaign] = useState<CampaignDto | null>(null)
  const [formHotelId, setFormHotelId] = useState<number | ''>('')
  const [formName, setFormName] = useState('')
  const [formDescription, setFormDescription] = useState('')
  const [formStartDate, setFormStartDate] = useState('')
  const [formEndDate, setFormEndDate] = useState('')
  const [formStatus, setFormStatus] = useState('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  useEffect(() => {
    hotelService
      .filter({ pageSize: 100, status: 'ACTIVE' })
      .then((res) => {
        if (res.result) setHotels(res.result.content || [])
      })
      .catch((err) => console.error('Lỗi tải danh sách khách sạn:', err))
  }, [])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await campaignService.filter({
        page,
        pageSize,
        hotelId: selectedHotelId || undefined,
        name: name.trim(),
        status: status || undefined,
      })
      if (response && response.result) {
        setCampaigns(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách chiến dịch khuyến mại:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, selectedHotelId, name, status])

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
      setSelectedIds(campaigns.map((c) => c.id))
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
    setEditingCampaign(null)
    setFormHotelId(hotels.length > 0 ? hotels[0].id : '')
    setFormName('')
    setFormDescription('')
    setFormStartDate('')
    setFormEndDate('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (campaign: CampaignDto) => {
    setEditingCampaign(campaign)
    setFormHotelId(campaign.hotelId)
    setFormName(campaign.name)
    setFormDescription(campaign.description || '')
    setFormStartDate(campaign.startDate)
    setFormEndDate(campaign.endDate)
    setFormStatus(campaign.status)
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!formHotelId || !formName.trim() || !formStartDate || !formEndDate) {
      setFormError('Vui lòng điền đầy đủ các thông tin bắt buộc')
      return
    }

    if (formStartDate > formEndDate) {
      setFormError('Ngày kết thúc phải sau ngày bắt đầu')
      return
    }

    setIsSubmitting(true)
    try {
      if (editingCampaign) {
        const updatePayload: CampaignUpdateRequest = {
          name: formName.trim(),
          description: formDescription.trim() || undefined,
          startDate: formStartDate,
          endDate: formEndDate,
          status: formStatus,
        }
        await campaignService.update(editingCampaign.id, updatePayload)
      } else {
        const createPayload: CampaignCreateRequest = {
          hotelId: Number(formHotelId),
          name: formName.trim(),
          description: formDescription.trim() || undefined,
          startDate: formStartDate,
          endDate: formEndDate,
          status: formStatus,
        }
        await campaignService.create(createPayload)
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const errorMsg =
        err instanceof Error ? err.message : 'Có lỗi xảy ra khi lưu chiến dịch'
      setFormError(errorMsg)
    } finally {
      setIsSubmitting(false)
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
      await campaignService.deleteBatch(deletingIds)
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)))
      setDeleteConfirmOpen(false)
      loadData()
    } catch (err) {
      console.error('Lỗi khi xóa chiến dịch:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header Title */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <Megaphone className="w-7 h-7 text-red-600" />
            Chiến Dịch Khuyến Mại
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý các chương trình ưu đãi, giảm giá mùa vụ và sự kiện tại từng cơ sở
          </p>
        </div>
        <Button onClick={openCreateModal} className="shrink-0">
          <Plus className="w-5 h-5 mr-1" />
          Tạo Chiến Dịch
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
              placeholder="Tên chiến dịch khuyến mại..."
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="h-10 sm:h-12"
            />
          </div>

          <div>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full h-10 sm:h-12 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Tạm dừng (INACTIVE)</option>
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
                setName('')
                setStatus('')
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
              Xóa {selectedIds.length} chiến dịch đã chọn
            </Button>
          )}
        </div>
        <div className="text-sm text-neutral-500">
          Tổng cộng: <span className="font-semibold text-neutral-900">{totalElements}</span> chiến dịch
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
                    checked={
                      campaigns.length > 0 && selectedIds.length === campaigns.length
                    }
                    onChange={handleSelectAll}
                    aria-label="Chọn tất cả chiến dịch"
                    className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                  />
                </th>
                <th className="py-3.5 px-4 font-semibold">Tên Chiến Dịch</th>
                <th className="py-3.5 px-4 font-semibold">Khách Sạn Áp Dụng</th>
                <th className="py-3.5 px-4 font-semibold">Thời Gian Diễn Ra</th>
                <th className="py-3.5 px-4 font-semibold">Trạng Thái</th>
                <th className="py-3.5 px-4 font-semibold text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200/70">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-500">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải danh sách chiến dịch...
                    </div>
                  </td>
                </tr>
              ) : campaigns.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-500">
                    Không tìm thấy chiến dịch khuyến mại nào.
                  </td>
                </tr>
              ) : (
                campaigns.map((c) => {
                  const isSelected = selectedIds.includes(c.id)
                  return (
                    <tr
                      key={c.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/20' : ''
                      }`}
                    >
                      <td className="py-3 px-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(c.id)}
                          aria-label={`Chọn chiến dịch ${c.name}`}
                          className="rounded-md border-neutral-300 text-red-600 focus:ring-red-500 cursor-pointer"
                        />
                      </td>
                      <td className="py-3 px-4">
                        <div className="font-semibold text-neutral-900">{c.name}</div>
                        {c.description && (
                          <div className="text-xs text-neutral-500 line-clamp-1">
                            {c.description}
                          </div>
                        )}
                      </td>
                      <td className="py-3 px-4 font-medium text-neutral-800">
                        {c.hotelName}
                      </td>
                      <td className="py-3 px-4 text-neutral-600 text-xs">
                        {c.startDate} ➔ {c.endDate}
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                            c.status === 'ACTIVE'
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-neutral-100 text-neutral-600 border border-neutral-200'
                          }`}
                        >
                          {c.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEditModal(c)}
                            aria-label={`Chỉnh sửa ${c.name}`}
                          >
                            <Edit2 className="w-4 h-4 text-neutral-600" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => promptDeleteSingle(c.id)}
                            aria-label={`Xóa ${c.name}`}
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
        title={editingCampaign ? 'Chỉnh Sửa Chiến Dịch' : 'Tạo Chiến Dịch Khuyến Mại'}
      >
        <form onSubmit={handleSubmitForm} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 text-red-600 text-sm rounded-xl border border-red-200">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Khách Sạn Áp Dụng <span className="text-red-500">*</span>
            </label>
            <select
              value={formHotelId}
              onChange={(e) => setFormHotelId(Number(e.target.value))}
              disabled={!!editingCampaign}
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
              Tên Chiến Dịch <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: Chào Hè Sôi Động 2026..."
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Mô Tả Chương Trình
            </label>
            <textarea
              rows={3}
              placeholder="Chi tiết ưu đãi hoặc điều kiện áp dụng..."
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
              className="w-full p-3 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600 text-sm"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Ngày Bắt Đầu <span className="text-red-500">*</span>
              </label>
              <Input
                type="date"
                value={formStartDate}
                onChange={(e) => setFormStartDate(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Ngày Kết Thúc <span className="text-red-500">*</span>
              </label>
              <Input
                type="date"
                value={formEndDate}
                onChange={(e) => setFormEndDate(e.target.value)}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Trạng Thái
            </label>
            <select
              value={formStatus}
              onChange={(e) => setFormStatus(e.target.value)}
              className="w-full h-11 px-4 rounded-xl border border-neutral-200 bg-white text-neutral-900 focus:outline-none focus:ring-2 focus:ring-red-600"
            >
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Tạm dừng (INACTIVE)</option>
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
            <Button type="submit" isLoading={isSubmitting}>
              {editingCampaign ? 'Cập Nhật' : 'Tạo Mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Delete Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa chiến dịch"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} chiến dịch đã chọn? Thao tác này sẽ chuyển dữ liệu vào thùng rác.`}
        confirmText="Xác nhận xóa"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default CampaignListPage
