import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Briefcase } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import departmentService from '../services/department.service'
import type { DepartmentDto } from '../types/department.types'

export const DepartmentListPage: React.FC = () => {
  const [departments, setDepartments] = useState<DepartmentDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingDept, setEditingDept] = useState<DepartmentDto | null>(null)
  const [formCode, setFormCode] = useState('')
  const [formName, setFormName] = useState('')
  const [formDescription, setFormDescription] = useState('')
  const [formStatus, setFormStatus] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await departmentService.filter({
        page,
        pageSize,
        keyword: keyword || undefined,
        status: status || undefined,
      })
      if (response && response.result) {
        setDepartments(response.result.content || [])
        setTotalElements(response.result.totalElements || 0)
        setTotalPages(response.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách phòng ban:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, status])

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData()
    }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    void loadData()
  }

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(departments.map((d) => d.id))
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
    setEditingDept(null)
    setFormCode('')
    setFormName('')
    setFormDescription('')
    setFormStatus('ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (dept: DepartmentDto) => {
    setEditingDept(dept)
    setFormCode(dept.code)
    setFormName(dept.name)
    setFormDescription('')
    setFormStatus((dept.status as 'ACTIVE' | 'INACTIVE') || 'ACTIVE')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formName.trim()) {
      setFormError('Tên phòng ban không được để trống')
      return
    }

    setIsSubmitting(true)
    setFormError(null)
    try {
      if (editingDept) {
        await departmentService.update(editingDept.id, {
          name: formName.trim(),
          description: formDescription.trim() || undefined,
          status: formStatus,
        })
      } else {
        if (!formCode.trim()) {
          setFormError('Mã phòng ban không được để trống')
          setIsSubmitting(false)
          return
        }
        await departmentService.create({
          code: formCode.trim().toUpperCase(),
          name: formName.trim(),
          description: formDescription.trim() || undefined,
        })
      }
      setIsFormModalOpen(false)
      void loadData()
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } }
      setFormError(error.response?.data?.message || 'Có lỗi xảy ra khi lưu phòng ban')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDelete = (ids: number[]) => {
    setDeletingIds(ids)
    setDeleteConfirmOpen(true)
  }

  const confirmDelete = async () => {
    setIsDeleting(true)
    try {
      await departmentService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds([])
      void loadData()
    } catch (err) {
      console.error('Lỗi khi xóa phòng ban:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2.5">
            <Briefcase className="w-7 h-7 text-red-600" />
            Quản Lý Phòng Ban Tiêu Chuẩn
          </h2>
          <p className="text-sm text-neutral-500 mt-1">
            Thiết lập danh mục phòng ban thống nhất trên toàn chuỗi khách sạn
          </p>
        </div>
      </div>

      {/* Standard 3-Row Layout */}
      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleSearch} className="flex flex-wrap items-center gap-3">
          <div className="flex-1 min-w-[240px]">
            <Input
              placeholder="Tìm theo mã hoặc tên phòng ban..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
              className="h-11"
              aria-label="Tìm kiếm phòng ban"
            />
          </div>

          <div className="w-48">
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              aria-label="Lọc theo trạng thái"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="INACTIVE">Ngừng hoạt động</option>
            </select>
          </div>

          <Button type="submit" variant="primary" className="h-11 px-5">
            Tìm kiếm
          </Button>

          <Button
            type="button"
            variant="secondary"
            onClick={() => {
              setKeyword('')
              setStatus('')
              setPage(0)
            }}
            className="h-11 px-4"
          >
            <RefreshCw className="w-4 h-4" />
          </Button>
        </form>
      </div>

      {/* HÀNG 2: ACTION TOOLBAR */}
      <div className="flex justify-between items-center bg-white p-3.5 px-4 rounded-2xl border border-neutral-200 shadow-xs">
        <div className="flex items-center gap-2">
          <Button
            variant="primary"
            onClick={openCreateModal}
            className="h-10 text-xs font-semibold flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" /> Thêm Phòng Ban Mới
          </Button>

          {selectedIds.length > 0 && (
            <Button
              variant="danger"
              onClick={() => handleDelete(selectedIds)}
              className="h-10 text-xs font-semibold flex items-center gap-1.5"
            >
              <Trash2 className="w-4 h-4" /> Xóa ({selectedIds.length}) mục đã chọn
            </Button>
          )}
        </div>

        <div className="text-xs text-neutral-500 font-medium">
          Tổng số: <span className="font-bold text-neutral-900">{totalElements}</span> phòng ban
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE & PAGINATION */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-neutral-600">
            <thead className="bg-neutral-100/70 text-neutral-900 font-semibold text-xs uppercase tracking-wider border-b border-neutral-200">
              <tr>
                <th className="p-4 w-12 text-center">
                  <input
                    type="checkbox"
                    onChange={handleSelectAll}
                    checked={departments.length > 0 && selectedIds.length === departments.length}
                    className="w-4 h-4 text-red-600 rounded border-neutral-300 focus:ring-red-500"
                    aria-label="Chọn tất cả"
                  />
                </th>
                <th className="p-4">Mã phòng ban</th>
                <th className="p-4">Tên phòng ban</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4">Ngày tạo</th>
                <th className="p-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-neutral-500">
                    <div className="flex items-center justify-center gap-2">
                      <RefreshCw className="w-5 h-5 animate-spin text-red-600" />
                      Đang tải dữ liệu...
                    </div>
                  </td>
                </tr>
              ) : departments.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-neutral-500">
                    Không tìm thấy phòng ban nào.
                  </td>
                </tr>
              ) : (
                departments.map((dept) => (
                  <tr key={dept.id} className="hover:bg-neutral-50/70 transition-colors">
                    <td className="p-4 text-center">
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(dept.id)}
                        onChange={() => handleSelectOne(dept.id)}
                        className="w-4 h-4 text-red-600 rounded border-neutral-300 focus:ring-red-500"
                        aria-label={`Chọn phòng ban ${dept.name}`}
                      />
                    </td>
                    <td className="p-4 font-mono font-bold text-neutral-900">{dept.code}</td>
                    <td className="p-4 font-semibold text-neutral-900">{dept.name}</td>
                    <td className="p-4">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                          dept.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-neutral-100 text-neutral-600 border border-neutral-300'
                        }`}
                      >
                        {dept.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                      </span>
                    </td>
                    <td className="p-4 text-xs text-neutral-500">
                      {dept.createdAt ? new Date(dept.createdAt).toLocaleDateString('vi-VN') : '—'}
                    </td>
                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="ghost"
                          onClick={() => openEditModal(dept)}
                          className="h-8 w-8 p-0 text-blue-600 hover:bg-blue-50"
                          aria-label={`Sửa ${dept.name}`}
                        >
                          <Edit2 className="w-4 h-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          onClick={() => handleDelete([dept.id])}
                          className="h-8 w-8 p-0 text-red-600 hover:bg-red-50"
                          aria-label={`Xóa ${dept.name}`}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

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

      {/* Form Modal */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingDept ? 'Cập Nhật Phòng Ban' : 'Thêm Phòng Ban Mới'}
      >
        <form onSubmit={handleSave} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-600">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
              Mã phòng ban <span className="text-red-500">*</span>
            </label>
            <Input
              value={formCode}
              onChange={(e) => setFormCode(e.target.value.toUpperCase())}
              disabled={!!editingDept}
              placeholder="VD: FRONT_OFFICE, HOUSEKEEPING"
              className="h-11 font-mono uppercase"
              aria-label="Mã phòng ban"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
              Tên phòng ban <span className="text-red-500">*</span>
            </label>
            <Input
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              placeholder="VD: Lễ tân Tiền sảnh, Buồng phòng"
              className="h-11"
              aria-label="Tên phòng ban"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
              Mô tả chi tiết
            </label>
            <textarea
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
              placeholder="Mô tả chức năng nhiệm vụ của phòng ban..."
              rows={3}
              className="w-full p-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600"
              aria-label="Mô tả chi tiết"
            />
          </div>

          {editingDept && (
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Trạng thái
              </label>
              <select
                value={formStatus}
                onChange={(e) => setFormStatus(e.target.value as 'ACTIVE' | 'INACTIVE')}
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
                aria-label="Trạng thái phòng ban"
              >
                <option value="ACTIVE">Đang hoạt động</option>
                <option value="INACTIVE">Ngừng hoạt động</option>
              </select>
            </div>
          )}

          <div className="flex justify-end gap-2.5 pt-4 border-t border-neutral-200">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsFormModalOpen(false)}
              className="h-11 px-5"
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={isSubmitting}
              className="h-11 px-5"
            >
              {isSubmitting ? 'Đang lưu...' : editingDept ? 'Lưu thay đổi' : 'Tạo mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={confirmDelete}
        title="Xác nhận xóa phòng ban?"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} phòng ban đã chọn? Hành động này không thể hoàn tác.`}
        confirmText="Xóa vĩnh viễn"
        cancelText="Hủy"
        isLoading={isDeleting}
        variant="danger"
      />
    </div>
  )
}

export default DepartmentListPage
