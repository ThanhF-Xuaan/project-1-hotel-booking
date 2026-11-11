import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Clock } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Select from '../../../core/components/ui/Select'
import { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import surchargeRuleService from '../services/surchargeRule.service'
import hotelService from '../../organization/services/hotel.service'
import hotelRoomTypeService from '../../inventory/services/hotelRoomType.service'
import SurchargeRuleModal from '../components/SurchargeRuleModal'
import type { SurchargeRuleDto } from '../types/pricing.types'
import type { HotelDto } from '../../organization/types/organization.types'
import type { HotelRoomTypeDto } from '../../inventory/types/inventory.types'

export const SurchargeRuleListPage: React.FC = () => {
  const [rules, setRules] = useState<SurchargeRuleDto[]>([])
  const [hotels, setHotels] = useState<HotelDto[]>([])
  const [roomTypes, setRoomTypes] = useState<HotelRoomTypeDto[]>([])

  // Pagination & Filters
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const [filterHotelId, setFilterHotelId] = useState<number | ''>('')
  const [filterRoomTypeId, setFilterRoomTypeId] = useState<number | ''>('')
  const [filterRuleType, setFilterRuleType] = useState<string>('')
  const [filterStatus, setFilterStatus] = useState<string>('')
  const [filterActiveOnDate, setFilterActiveOnDate] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingRule, setEditingRule] = useState<SurchargeRuleDto | null>(null)

  // Delete Confirm
  const [deleteModalOpen, setDeleteModalOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Load auxiliary data
  useEffect(() => {
    hotelService
      .filter({ page: 0, pageSize: 100, status: 'ACTIVE' })
      .then((res) => setHotels(res.result?.content || []))
      .catch((err) => console.error('Lỗi tải danh mục khách sạn:', err))
  }, [])

  useEffect(() => {
    if (filterHotelId) {
      hotelRoomTypeService
        .filter({ hotelId: Number(filterHotelId), pageSize: 100 })
        .then((res) => setRoomTypes(res.result?.content || []))
        .catch((err) => console.error('Lỗi tải danh mục loại phòng:', err))
    } else {
      setRoomTypes([])
      setFilterRoomTypeId('')
    }
  }, [filterHotelId])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const res = await surchargeRuleService.filter({
        page,
        pageSize,
        hotelId: filterHotelId ? Number(filterHotelId) : undefined,
        hotelRoomTypeId: filterRoomTypeId ? Number(filterRoomTypeId) : undefined,
        ruleType: filterRuleType || undefined,
        status: filterStatus || undefined,
        activeOnDate: filterActiveOnDate || undefined,
        sortBy: 'id',
        sortDirection: 'DESC',
      })
      if (res.result) {
        setRules(res.result.content || [])
        setTotalElements(res.result.totalElements)
        setTotalPages(res.result.totalPages)
      }
    } catch (err) {
      console.error('Lỗi tải danh sách quy tắc phụ thu:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, filterHotelId, filterRoomTypeId, filterRuleType, filterStatus, filterActiveOnDate])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedIds(rules.map((r) => r.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelectRow = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  const handleDeleteSelected = () => {
    if (selectedIds.length === 0) return
    setDeletingIds(selectedIds)
    setDeleteModalOpen(true)
  }

  const handleDeleteSingle = (id: number) => {
    setDeletingIds([id])
    setDeleteModalOpen(true)
  }

  const confirmDelete = async () => {
    setIsDeleting(true)
    try {
      await surchargeRuleService.deleteBatch(deletingIds)
      setSelectedIds([])
      setDeleteModalOpen(false)
      void loadData()
    } catch (err) {
      console.error('Lỗi khi xóa quy tắc phụ thu:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  const formatRuleType = (type: string) => {
    switch (type) {
      case 'EXTRA_PERSON':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">Thêm người</span>
      case 'EXTRA_BED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800">Kê thêm giường</span>
      case 'EARLY_CHECKIN':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-800">Nhận phòng sớm</span>
      case 'LATE_CHECKOUT':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800">Trả phòng muộn</span>
      default:
        return type
    }
  }

  return (
    <div className="space-y-6">
      {/* Tiêu đề */}
      <div>
        <h1 className="text-2xl font-bold text-neutral-900">Quy tắc Phụ thu (Surcharge Rules)</h1>
        <p className="text-sm text-neutral-500 mt-1">
          Quản lý chính sách phụ thu cho thêm người, kê giường phụ, nhận phòng sớm và trả phòng muộn.
        </p>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-sm space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          <Select
            label="Khách sạn"
            value={filterHotelId}
            onChange={(val) => setFilterHotelId(val)}
            options={[
              { label: '-- Tất cả khách sạn --', value: '' },
              ...hotels.map((h) => ({ label: h.name, value: h.id })),
            ]}
          />

          <Select
            label="Hạng phòng"
            value={filterRoomTypeId}
            onChange={(val) => setFilterRoomTypeId(val)}
            options={[
              { label: '-- Tất cả hạng phòng --', value: '' },
              ...roomTypes.map((rt) => ({ label: rt.roomTypeName || `Loại ${rt.id}`, value: rt.id })),
            ]}
            disabled={!filterHotelId}
          />

          <Select
            label="Loại phụ thu"
            value={filterRuleType}
            onChange={(val) => setFilterRuleType(String(val))}
            options={[
              { label: '-- Tất cả loại --', value: '' },
              { label: 'Thêm người (EXTRA_PERSON)', value: 'EXTRA_PERSON' },
              { label: 'Kê giường phụ (EXTRA_BED)', value: 'EXTRA_BED' },
              { label: 'Nhận phòng sớm (EARLY_CHECKIN)', value: 'EARLY_CHECKIN' },
              { label: 'Trả phòng muộn (LATE_CHECKOUT)', value: 'LATE_CHECKOUT' },
            ]}
          />

          <Select
            label="Trạng thái"
            value={filterStatus}
            onChange={(val) => setFilterStatus(String(val))}
            options={[
              { label: '-- Tất cả trạng thái --', value: '' },
              { label: 'Hoạt động (ACTIVE)', value: 'ACTIVE' },
              { label: 'Tạm ngừng (INACTIVE)', value: 'INACTIVE' },
            ]}
          />

          <Input
            label="Hiệu lực vào ngày"
            type="date"
            value={filterActiveOnDate}
            onChange={(e) => setFilterActiveOnDate(e.target.value)}
          />
        </div>

        <div className="flex justify-end gap-3">
          <Button
            variant="secondary"
            onClick={() => {
              setFilterHotelId('')
              setFilterRoomTypeId('')
              setFilterRuleType('')
              setFilterStatus('')
              setFilterActiveOnDate('')
              setPage(0)
            }}
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Làm mới bộ lọc
          </Button>
          <Button variant="primary" onClick={() => setPage(0)}>
            <Search className="w-4 h-4 mr-2" />
            Tìm kiếm
          </Button>
        </div>
      </div>

      {/* HÀNG 2: ACTION TOOLBAR */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-neutral-600">
            {selectedIds.length > 0
              ? `Đã chọn ${selectedIds.length} bản ghi`
              : `Tổng số: ${totalElements} quy tắc`}
          </span>
        </div>

        <div className="flex items-center gap-3">
          {selectedIds.length > 0 && (
            <Button variant="danger" onClick={handleDeleteSelected}>
              <Trash2 className="w-4 h-4 mr-2" />
              Xóa ({selectedIds.length})
            </Button>
          )}

          <Button
            variant="primary"
            onClick={() => {
              setEditingRule(null)
              setIsModalOpen(true)
            }}
          >
            <Plus className="w-4 h-4 mr-2" />
            Thêm mới Phụ thu
          </Button>
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE & PAGINATION */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-neutral-50 border-b border-neutral-200 text-xs font-semibold text-neutral-600 uppercase tracking-wider">
                <th className="p-4 w-12 text-center">
                  <input
                    type="checkbox"
                    className="rounded border-neutral-300 text-red-600 focus:ring-red-500 w-4 h-4"
                    checked={rules.length > 0 && selectedIds.length === rules.length}
                    onChange={(e) => handleSelectAll(e.target.checked)}
                  />
                </th>
                <th className="p-4">Cơ sở / Hạng phòng</th>
                <th className="p-4">Loại phụ thu</th>
                <th className="p-4">Áp dụng độ tuổi</th>
                <th className="p-4">Hình thức & Mức thu</th>
                <th className="p-4">Thời gian hiệu lực</th>
                <th className="p-4 text-center">Trạng thái</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200 text-sm">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-neutral-500">
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : rules.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-neutral-500">
                    Không tìm thấy quy tắc phụ thu nào phù hợp.
                  </td>
                </tr>
              ) : (
                rules.map((rule) => {
                  const isSelected = selectedIds.includes(rule.id)
                  return (
                    <tr
                      key={rule.id}
                      className={`hover:bg-neutral-50 transition-colors ${
                        isSelected ? 'bg-red-50/40' : ''
                      }`}
                    >
                      <td className="p-4 text-center">
                        <input
                          type="checkbox"
                          className="rounded border-neutral-300 text-red-600 focus:ring-red-500 w-4 h-4"
                          checked={isSelected}
                          onChange={() => handleSelectRow(rule.id)}
                        />
                      </td>
                      <td className="p-4 font-medium text-neutral-900">
                        <div>{rule.roomTypeName || `Hạng phòng #${rule.hotelRoomTypeId}`}</div>
                        <div className="text-xs text-neutral-500">{rule.hotelName || 'Toàn chuỗi'}</div>
                      </td>
                      <td className="p-4">{formatRuleType(rule.ruleType)}</td>
                      <td className="p-4">
                        {rule.agePolicyGuestType ? (
                          <span className="text-xs bg-neutral-100 text-neutral-700 px-2 py-1 rounded">
                            {rule.agePolicyGuestType} ({rule.agePolicyMinAge} - {rule.agePolicyMaxAge}t)
                          </span>
                        ) : (
                          <span className="text-xs text-neutral-400">Tất cả độ tuổi</span>
                        )}
                      </td>
                      <td className="p-4">
                        {(() => {
                          let parsedTiers: any[] = []
                          let standardTime: string | null = null
                          try {
                            if (rule.conditions) {
                              const parsed = JSON.parse(rule.conditions)
                              if (Array.isArray(parsed.tiers) && parsed.tiers.length > 0) {
                                parsedTiers = parsed.tiers
                                standardTime = parsed.standard_time || null
                              }
                            }
                          } catch {
                            // ignore JSON parse error
                          }

                          if (parsedTiers.length > 0) {
                            return (
                              <div className="space-y-1.5">
                                <div className="flex items-center gap-1.5 flex-wrap">
                                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
                                    <Clock className="w-3 h-3" />
                                    Theo bậc ({parsedTiers.length} khung giờ)
                                  </span>
                                  {standardTime && (
                                    <span className="text-[11px] text-neutral-500 bg-neutral-100 px-1.5 py-0.5 rounded font-mono">
                                      Chuẩn: {standardTime}
                                    </span>
                                  )}
                                </div>
                                <div className="text-xs text-neutral-600 space-y-0.5">
                                  {parsedTiers.slice(0, 3).map((tier: any, i: number) => (
                                    <div key={i} className="flex items-center gap-1">
                                      <span className="font-mono text-neutral-700 font-medium">
                                        {tier.from_time} - {tier.to_time}:
                                      </span>
                                      <span className="font-semibold text-neutral-900">
                                        {tier.adjustment_type === 'PERCENT'
                                          ? `${tier.adjustment_value}%`
                                          : `${Number(tier.adjustment_value).toLocaleString('vi-VN')} đ`}
                                      </span>
                                    </div>
                                  ))}
                                  {parsedTiers.length > 3 && (
                                    <div className="text-[11px] text-neutral-400 italic">
                                      + {parsedTiers.length - 3} bậc khác...
                                    </div>
                                  )}
                                </div>
                              </div>
                            )
                          }

                          return (
                            <div>
                              <span className="font-semibold text-neutral-900">
                                {rule.adjustmentType === 'FIXED'
                                  ? `${Number(rule.adjustmentValue).toLocaleString('vi-VN')} đ`
                                  : `${rule.adjustmentValue}% giá phòng`}
                              </span>
                              <span className="text-xs font-normal text-neutral-500 ml-1">
                                ({rule.pricingType === 'PER_NIGHT' ? '/ đêm' : '/ kỳ'})
                              </span>
                            </div>
                          )
                        })()}
                      </td>
                      <td className="p-4 text-neutral-600 text-xs">
                        <div>{rule.startDate}</div>
                        <div>đến {rule.endDate}</div>
                      </td>
                      <td className="p-4 text-center">
                        <span
                          className={`inline-flex px-2 py-1 rounded-full text-xs font-medium ${
                            rule.status === 'ACTIVE'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-neutral-100 text-neutral-600'
                          }`}
                        >
                          {rule.status === 'ACTIVE' ? 'Đang áp dụng' : 'Tạm dừng'}
                        </span>
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() => {
                              setEditingRule(rule)
                              setIsModalOpen(true)
                            }}
                            className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors"
                            title="Chỉnh sửa"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDeleteSingle(rule.id)}
                            className="p-1.5 text-neutral-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
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

        {/* Phân trang */}
        <div className="p-4 border-t border-neutral-200">
          <Pagination
            pageNumber={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={(p) => setPage(p)}
          />
        </div>
      </div>

      {/* Modal Thêm/Sửa */}
      <SurchargeRuleModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false)
          setEditingRule(null)
        }}
        onSuccess={() => void loadData()}
        editingRule={editingRule}
      />

      {/* Modal Xác nhận Xóa */}
      <ConfirmModal
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={confirmDelete}
        title="Xác nhận xóa quy tắc phụ thu"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} quy tắc phụ thu đã chọn? Hành động này sẽ chuyển trạng thái quy tắc sang đã xóa.`}
        confirmText="Xác nhận xóa"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  )
}

export default SurchargeRuleListPage
