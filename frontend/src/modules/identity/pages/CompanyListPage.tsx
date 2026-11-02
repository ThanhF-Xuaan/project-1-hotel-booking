import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Building2, Phone, Mail, MapPin, FileText } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import { companyService } from '../services/identity.service'
import type { CompanyDto } from '../types/identity.types'

export const CompanyListPage: React.FC = () => {
  const [companies, setCompanies] = useState<CompanyDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)

  // Filters
  const [companyNameFilter, setCompanyNameFilter] = useState('')
  const [taxCodeFilter, setTaxCodeFilter] = useState('')
  const [contactPersonFilter, setContactPersonFilter] = useState('')
  const [contactPhoneFilter, setContactPhoneFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal Create / Edit
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingCompany, setEditingCompany] = useState<CompanyDto | null>(null)
  const [formName, setFormName] = useState('')
  const [formTaxCode, setFormTaxCode] = useState('')
  const [formAddress, setFormAddress] = useState('')
  const [formContactName, setFormContactName] = useState('')
  const [formContactPhone, setFormContactPhone] = useState('')
  const [formContactEmail, setFormContactEmail] = useState('')
  const [formStatus, setFormStatus] = useState('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Modal Delete
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const res = await companyService.filter({
        page,
        pageSize,
        companyName: companyNameFilter.trim() || undefined,
        taxCode: taxCodeFilter.trim() || undefined,
        contactPerson: contactPersonFilter.trim() || undefined,
        contactPhone: contactPhoneFilter.trim() || undefined,
        status: statusFilter || undefined,
      })
      if (res.result) {
        setCompanies(res.result.content || [])
        setTotalElements(res.result.totalElements || 0)
        setTotalPages(res.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách doanh nghiệp đối tác:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, companyNameFilter, taxCodeFilter, contactPersonFilter, contactPhoneFilter, statusFilter])

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
    setCompanyNameFilter('')
    setTaxCodeFilter('')
    setContactPersonFilter('')
    setContactPhoneFilter('')
    setStatusFilter('')
    setPage(0)
  }

  // Row selection
  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(companies.map((c) => c.id))
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
    setEditingCompany(null)
    setFormName('')
    setFormTaxCode('')
    setFormAddress('')
    setFormContactName('')
    setFormContactPhone('')
    setFormContactEmail('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  // Open Edit Modal
  const handleOpenEdit = (comp: CompanyDto) => {
    setEditingCompany(comp)
    setFormName(comp.name)
    setFormTaxCode(comp.taxCode)
    setFormAddress(comp.address || '')
    setFormContactName(comp.contactName || '')
    setFormContactPhone(comp.contactPhone || '')
    setFormContactEmail(comp.contactEmail || '')
    setFormStatus(comp.status || 'ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  // Submit Create / Edit
  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formName.trim()) {
      setFormError('Vui lòng nhập tên doanh nghiệp đối tác')
      return
    }
    if (!formTaxCode.trim()) {
      setFormError('Vui lòng nhập mã số thuế')
      return
    }

    setIsSubmitting(true)
    setFormError(null)

    try {
      if (editingCompany) {
        await companyService.update(editingCompany.id, {
          name: formName.trim(),
          taxCode: formTaxCode.trim(),
          address: formAddress.trim() || undefined,
          contactName: formContactName.trim() || undefined,
          contactPhone: formContactPhone.trim() || undefined,
          contactEmail: formContactEmail.trim() || undefined,
          status: formStatus,
        })
      } else {
        await companyService.create({
          name: formName.trim(),
          taxCode: formTaxCode.trim(),
          address: formAddress.trim() || undefined,
          contactName: formContactName.trim() || undefined,
          contactPhone: formContactPhone.trim() || undefined,
          contactEmail: formContactEmail.trim() || undefined,
        })
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const errorMsg = (err as { message?: string })?.message || 'Có lỗi xảy ra khi lưu thông tin doanh nghiệp'
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
      await companyService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setDeletingIds([])
      setSelectedIds([])
      loadData()
    } catch (err: unknown) {
      const errorMsg = (err as { message?: string })?.message || 'Không thể xóa doanh nghiệp đã chọn'
      alert(errorMsg)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2">
            <Building2 className="w-7 h-7 text-red-600" />
            Doanh nghiệp Đối tác (B2B)
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý danh sách các công ty, doanh nghiệp hợp tác đặt phòng và dịch vụ sự kiện đoàn
          </p>
        </div>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 sm:p-5 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-4 items-end">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Tên doanh nghiệp
            </label>
            <div className="relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
              <input
                type="text"
                value={companyNameFilter}
                onChange={(e) => setCompanyNameFilter(e.target.value)}
                placeholder="VD: Tập đoàn FPT..."
                className="w-full h-10 sm:h-12 pl-10 pr-4 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Mã số thuế
            </label>
            <div className="relative">
              <FileText className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
              <input
                type="text"
                value={taxCodeFilter}
                onChange={(e) => setTaxCodeFilter(e.target.value)}
                placeholder="VD: 0101234567..."
                className="w-full h-10 sm:h-12 pl-10 pr-4 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Người đại diện
            </label>
            <input
              type="text"
              value={contactPersonFilter}
              onChange={(e) => setContactPersonFilter(e.target.value)}
              placeholder="VD: Trần Thị B..."
              className="w-full h-10 sm:h-12 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Trạng thái
            </label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full h-10 sm:h-12 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hợp tác</option>
              <option value="INACTIVE">Ngừng hợp tác</option>
            </select>
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
            <span>Thêm doanh nghiệp đối tác</span>
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
          Tổng cộng: <span className="font-bold text-neutral-900">{totalElements}</span> doanh nghiệp
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
                    checked={companies.length > 0 && selectedIds.length === companies.length}
                    onChange={handleSelectAll}
                    className="w-4 h-4 text-red-600 border-neutral-300 rounded focus:ring-red-600 cursor-pointer"
                  />
                </th>
                <th className="p-4">Doanh nghiệp / MST</th>
                <th className="p-4">Địa chỉ trụ sở</th>
                <th className="p-4">Đại diện liên hệ</th>
                <th className="p-4 text-center">Trạng thái</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-12 text-center">
                    <div className="inline-flex items-center gap-2 text-sm text-neutral-500">
                      <RefreshCw className="w-4 h-4 animate-spin text-red-600" />
                      <span>Đang tải dữ liệu doanh nghiệp...</span>
                    </div>
                  </td>
                </tr>
              ) : companies.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-12 text-center text-neutral-400">
                    Không tìm thấy doanh nghiệp nào phù hợp
                  </td>
                </tr>
              ) : (
                companies.map((comp) => {
                  const isSelected = selectedIds.includes(comp.id)
                  return (
                    <tr
                      key={comp.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(comp.id)}
                          className="w-4 h-4 text-red-600 border-neutral-300 rounded focus:ring-red-600 cursor-pointer"
                        />
                      </td>
                      <td className="p-4">
                        <div className="font-semibold text-neutral-900">{comp.name}</div>
                        <div className="text-xs text-neutral-500 font-mono mt-0.5">
                          MST: {comp.taxCode}
                        </div>
                      </td>
                      <td className="p-4">
                        {comp.address ? (
                          <div className="flex items-center gap-1.5 text-xs text-neutral-700 max-w-xs truncate">
                            <MapPin className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
                            <span>{comp.address}</span>
                          </div>
                        ) : (
                          <span className="text-xs text-neutral-400 italic">Chưa cập nhật</span>
                        )}
                      </td>
                      <td className="p-4">
                        {comp.contactName ? (
                          <div>
                            <div className="font-medium text-neutral-900 text-xs">
                              {comp.contactName}
                            </div>
                            <div className="flex items-center gap-3 text-xs text-neutral-500 mt-0.5">
                              {comp.contactPhone && (
                                <span className="flex items-center gap-1">
                                  <Phone className="w-3 h-3 text-neutral-400" />
                                  {comp.contactPhone}
                                </span>
                              )}
                              {comp.contactEmail && (
                                <span className="flex items-center gap-1">
                                  <Mail className="w-3 h-3 text-neutral-400" />
                                  {comp.contactEmail}
                                </span>
                              )}
                            </div>
                          </div>
                        ) : (
                          <span className="text-xs text-neutral-400 italic">Chưa cập nhật</span>
                        )}
                      </td>
                      <td className="p-4 text-center">
                        {comp.status === 'ACTIVE' ? (
                          <span className="inline-block text-xs font-semibold px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                            Đang hợp tác
                          </span>
                        ) : (
                          <span className="inline-block text-xs font-semibold px-2.5 py-1 rounded-full bg-neutral-100 text-neutral-600 border border-neutral-200">
                            Ngừng hợp tác
                          </span>
                        )}
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => handleOpenEdit(comp)}
                            className="p-2 text-neutral-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="Chỉnh sửa doanh nghiệp"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setDeletingIds([comp.id])
                              setDeleteConfirmOpen(true)
                            }}
                            className="p-2 text-neutral-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Xóa doanh nghiệp"
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

      {/* MODAL: CREATE / EDIT COMPANY */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingCompany ? `Cập nhật: ${editingCompany.name}` : 'Thêm mới doanh nghiệp đối tác'}
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
              label="Tên doanh nghiệp"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              placeholder="VD: Tập đoàn Bưu chính Viễn thông VN"
              required
            />
            <Input
              label="Mã số thuế"
              value={formTaxCode}
              onChange={(e) => setFormTaxCode(e.target.value)}
              placeholder="VD: 0100684378"
              required
            />
          </div>

          <Input
            label="Địa chỉ trụ sở doanh nghiệp"
            value={formAddress}
            onChange={(e) => setFormAddress(e.target.value)}
            placeholder="VD: 57 Huỳnh Thúc Kháng, Đống Đa, Hà Nội"
          />

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Input
              label="Người đại diện"
              value={formContactName}
              onChange={(e) => setFormContactName(e.target.value)}
              placeholder="VD: Nguyễn Văn B"
            />
            <Input
              label="Số điện thoại"
              value={formContactPhone}
              onChange={(e) => setFormContactPhone(e.target.value)}
              placeholder="VD: 0912345678"
            />
            <Input
              label="Email liên hệ"
              type="email"
              value={formContactEmail}
              onChange={(e) => setFormContactEmail(e.target.value)}
              placeholder="VD: contact@company.vn"
            />
          </div>

          {editingCompany && (
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
                Trạng thái hợp tác
              </label>
              <select
                value={formStatus}
                onChange={(e) => setFormStatus(e.target.value)}
                className="w-full h-11 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              >
                <option value="ACTIVE">Đang hợp tác (ACTIVE)</option>
                <option value="INACTIVE">Ngừng hợp tác (INACTIVE)</option>
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
              {isSubmitting ? 'Đang lưu...' : editingCompany ? 'Cập nhật' : 'Tạo mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* MODAL: CONFIRM DELETE */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa doanh nghiệp đối tác"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} doanh nghiệp đã chọn?`}
        confirmText="Xác nhận xóa"
        cancelText="Hủy"
        isLoading={isDeleting}
        variant="danger"
      />
    </div>
  )
}

export default CompanyListPage
