import React, { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Users, ShieldAlert } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import { staffService, roleService } from '../services/identity.service'
import departmentService from '../../organization/services/department.service'
import hotelService from '../../organization/services/hotel.service'
import type { StaffDto, RoleDto } from '../types/identity.types'
import type { DepartmentDto } from '../../organization/types/department.types'
import type { HotelDto } from '../../organization/types/organization.types'

export const StaffListPage: React.FC = () => {
  const [staffs, setStaffs] = useState<StaffDto[]>([])
  const [roles, setRoles] = useState<RoleDto[]>([])
  const [departments, setDepartments] = useState<DepartmentDto[]>([])
  const [hotels, setHotels] = useState<HotelDto[]>([])

  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [roleFilter, setRoleFilter] = useState<string>('')
  const [deptFilter, setDeptFilter] = useState<string>('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal Create/Edit
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingStaff, setEditingStaff] = useState<StaffDto | null>(null)
  const [formUsername, setFormUsername] = useState('')
  const [formPassword, setFormPassword] = useState('')
  const [formFirstName, setFormFirstName] = useState('')
  const [formLastName, setFormLastName] = useState('')
  const [formEmail, setFormEmail] = useState('')
  const [formPhone, setFormPhone] = useState('')
  const [formRoleId, setFormRoleId] = useState<number | ''>('')
  const [formDeptId, setFormDeptId] = useState<number | ''>('')
  const [formScopeType, setFormScopeType] = useState<string>('PROPERTY')
  const [formScopeEntityId, setFormScopeEntityId] = useState<number | ''>('')
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Status Change Modal
  const [statusModalOpen, setStatusModalOpen] = useState(false)
  const [statusTargetStaff, setStatusTargetStaff] = useState<StaffDto | null>(null)
  const [newStatus, setNewStatus] = useState<string>('ACTIVE')
  const [statusReason, setStatusReason] = useState<string>('')

  // Load auxiliary data for dropdowns
  useEffect(() => {
    let isMounted = true
    roleService.filter({ pageSize: 50, status: 'ACTIVE' }).then((res) => {
      if (isMounted && res.result?.content) setRoles(res.result.content)
    }).catch(console.error)

    departmentService.getAllActive().then((res) => {
      if (isMounted && res.result) setDepartments(res.result)
    }).catch(console.error)

    hotelService.filter({ pageSize: 100, status: 'ACTIVE' }).then((res) => {
      if (isMounted && res.result?.content) setHotels(res.result.content)
    }).catch(console.error)

    return () => { isMounted = false }
  }, [])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const res = await staffService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        roleId: roleFilter ? Number(roleFilter) : undefined,
        departmentId: deptFilter ? Number(deptFilter) : undefined,
        status: statusFilter || undefined,
      })
      if (res.result) {
        setStaffs(res.result.content || [])
        setTotalElements(res.result.totalElements || 0)
        setTotalPages(res.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách nhân viên:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, roleFilter, deptFilter, statusFilter])

  useEffect(() => {
    const timer = setTimeout(() => { void loadData() }, 0)
    return () => clearTimeout(timer)
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    void loadData()
  }

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSelectedIds(e.target.checked ? staffs.map((s) => s.id) : [])
  }

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) => prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id])
  }

  const openCreateModal = () => {
    setEditingStaff(null)
    setFormUsername('')
    setFormPassword('')
    setFormFirstName('')
    setFormLastName('')
    setFormEmail('')
    setFormPhone('')
    setFormRoleId(roles[0]?.id || '')
    setFormDeptId(departments[0]?.id || '')
    setFormScopeType('PROPERTY')
    setFormScopeEntityId(hotels[0]?.id || '')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const openEditModal = (staff: StaffDto) => {
    setEditingStaff(staff)
    setFormUsername(staff.username)
    setFormPassword('')
    setFormFirstName(staff.firstName || '')
    setFormLastName(staff.lastName || '')
    setFormEmail(staff.email)
    setFormPhone(staff.phone || '')
    setFormRoleId(staff.roleId || '')
    setFormDeptId(staff.departmentId || '')
    setFormScopeType(staff.scopeType || 'PROPERTY')
    setFormScopeEntityId(staff.scopeEntityId || '')
    setFormError(null)
    setIsFormModalOpen(true)
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formEmail.trim() || !formFirstName.trim() || !formLastName.trim() || !formRoleId) {
      setFormError('Vui lòng điền đầy đủ các thông tin bắt buộc (*)')
      return
    }

    setIsSubmitting(true)
    setFormError(null)
    try {
      if (editingStaff) {
        await staffService.update(editingStaff.id, {
          email: formEmail.trim(),
          phone: formPhone.trim() || undefined,
          firstName: formFirstName.trim(),
          lastName: formLastName.trim(),
          roleId: Number(formRoleId),
          departmentId: formDeptId ? Number(formDeptId) : undefined,
          scopeType: formScopeType,
          scopeEntityId: formScopeEntityId ? Number(formScopeEntityId) : undefined,
        })
      } else {
        if (!formUsername.trim()) {
          setFormError('Tên đăng nhập không được để trống')
          setIsSubmitting(false)
          return
        }
        await staffService.create({
          username: formUsername.trim(),
          password: formPassword.trim() || undefined,
          email: formEmail.trim(),
          phone: formPhone.trim() || undefined,
          firstName: formFirstName.trim(),
          lastName: formLastName.trim(),
          roleId: Number(formRoleId),
          departmentId: formDeptId ? Number(formDeptId) : undefined,
          scopeType: formScopeType,
          scopeEntityId: formScopeEntityId ? Number(formScopeEntityId) : undefined,
        })
      }
      setIsFormModalOpen(false)
      void loadData()
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } }
      setFormError(error.response?.data?.message || 'Có lỗi xảy ra khi lưu nhân viên')
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
      await staffService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setSelectedIds([])
      void loadData()
    } catch (err) {
      console.error('Lỗi khi xóa nhân sự:', err)
    } finally {
      setIsDeleting(false)
    }
  }

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!statusTargetStaff) return
    setIsSubmitting(true)
    try {
      await staffService.updateStatus(statusTargetStaff.id, {
        status: newStatus,
        reason: statusReason.trim() || undefined,
      })
      setStatusModalOpen(false)
      void loadData()
    } catch (err) {
      console.error('Lỗi khi đổi trạng thái nhân viên:', err)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-neutral-900 tracking-tight flex items-center gap-2.5">
            <Users className="w-7 h-7 text-red-600" />
            Nhân Sự & Tài Khoản Nội Bộ
          </h2>
          <p className="text-sm text-neutral-500 mt-1">
            Quản trị danh sách nhân viên, gán vai trò RBAC, phạm vi chi nhánh và trạng thái truy cập
          </p>
        </div>
      </div>

      {/* Row 1: Search & Filter */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleSearch} className="flex flex-wrap items-center gap-3">
          <div className="flex-1 min-w-[200px]">
            <Input
              placeholder="Tìm theo username, họ tên, email, SĐT..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
              className="h-11"
              aria-label="Tìm kiếm nhân sự"
            />
          </div>

          <div className="w-40">
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              aria-label="Lọc theo vai trò"
            >
              <option value="">Tất cả vai trò</option>
              {roles.map((r) => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </select>
          </div>

          <div className="w-40">
            <select
              value={deptFilter}
              onChange={(e) => setDeptFilter(e.target.value)}
              className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              aria-label="Lọc theo phòng ban"
            >
              <option value="">Tất cả phòng ban</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </div>

          <div className="w-36">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              aria-label="Lọc theo trạng thái"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Hoạt động</option>
              <option value="SUSPENDED">Tạm khóa</option>
              <option value="RESIGNED">Đã nghỉ việc</option>
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
              setRoleFilter('')
              setDeptFilter('')
              setStatusFilter('')
              setPage(0)
            }}
            className="h-11 px-4"
          >
            <RefreshCw className="w-4 h-4" />
          </Button>
        </form>
      </div>

      {/* Row 2: Action Toolbar */}
      <div className="flex justify-between items-center bg-white p-3.5 px-4 rounded-2xl border border-neutral-200 shadow-xs">
        <div className="flex items-center gap-2">
          <Button
            variant="primary"
            onClick={openCreateModal}
            className="h-10 text-xs font-semibold flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" /> Thêm Nhân Viên Mới
          </Button>

          {selectedIds.length > 0 && (
            <Button
              variant="danger"
              onClick={() => handleDelete(selectedIds)}
              className="h-10 text-xs font-semibold flex items-center gap-1.5"
            >
              <Trash2 className="w-4 h-4" /> Xóa ({selectedIds.length}) nhân sự
            </Button>
          )}
        </div>

        <div className="text-xs text-neutral-500 font-medium">
          Tổng số: <span className="font-bold text-neutral-900">{totalElements}</span> tài khoản
        </div>
      </div>

      {/* Row 3: Data Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-neutral-600">
            <thead className="bg-neutral-100/70 text-neutral-900 font-semibold text-xs uppercase tracking-wider border-b border-neutral-200">
              <tr>
                <th className="p-4 w-12 text-center">
                  <input
                    type="checkbox"
                    onChange={handleSelectAll}
                    checked={staffs.length > 0 && selectedIds.length === staffs.length}
                    className="w-4 h-4 text-red-600 rounded border-neutral-300 focus:ring-red-500"
                    aria-label="Chọn tất cả"
                  />
                </th>
                <th className="p-4">Tài khoản / Họ tên</th>
                <th className="p-4">Email & SĐT</th>
                <th className="p-4">Vai trò</th>
                <th className="p-4">Phòng ban</th>
                <th className="p-4">Phạm vi</th>
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
                      Đang tải dữ liệu...
                    </div>
                  </td>
                </tr>
              ) : staffs.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-neutral-500">
                    Không tìm thấy nhân viên nào.
                  </td>
                </tr>
              ) : (
                staffs.map((staff) => (
                  <tr key={staff.id} className="hover:bg-neutral-50/70 transition-colors">
                    <td className="p-4 text-center">
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(staff.id)}
                        onChange={() => handleSelectOne(staff.id)}
                        className="w-4 h-4 text-red-600 rounded border-neutral-300 focus:ring-red-500"
                        aria-label={`Chọn nhân viên ${staff.fullName}`}
                      />
                    </td>
                    <td className="p-4">
                      <div className="font-bold text-neutral-900">{staff.fullName}</div>
                      <div className="text-xs text-neutral-400 font-mono">@{staff.username}</div>
                    </td>
                    <td className="p-4 text-xs">
                      <div className="text-neutral-800">{staff.email}</div>
                      <div className="text-neutral-400">{staff.phone || '—'}</div>
                    </td>
                    <td className="p-4">
                      <span className="px-2 py-0.5 bg-neutral-100 text-neutral-800 rounded-md text-xs font-semibold">
                        {staff.roleName || staff.roleCode || 'Chưa gán'}
                      </span>
                    </td>
                    <td className="p-4 text-xs font-medium text-neutral-700">
                      {staff.departmentName || '—'}
                    </td>
                    <td className="p-4 text-xs">
                      <span className="font-semibold text-neutral-700">{staff.scopeType}</span>
                      {staff.scopeEntityId ? ` (#${staff.scopeEntityId})` : ''}
                    </td>
                    <td className="p-4">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                          staff.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : staff.status === 'SUSPENDED'
                            ? 'bg-amber-50 text-amber-700 border border-amber-200'
                            : 'bg-red-50 text-red-700 border border-red-200'
                        }`}
                      >
                        {staff.status === 'ACTIVE'
                          ? 'Hoạt động'
                          : staff.status === 'SUSPENDED'
                          ? 'Tạm khóa'
                          : 'Đã nghỉ'}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="ghost"
                          onClick={() => {
                            setStatusTargetStaff(staff)
                            setNewStatus(staff.status)
                            setStatusReason('')
                            setStatusModalOpen(true)
                          }}
                          className="h-8 w-8 p-0 text-amber-600 hover:bg-amber-50"
                          title="Đổi trạng thái"
                          aria-label={`Đổi trạng thái ${staff.fullName}`}
                        >
                          <ShieldAlert className="w-4 h-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          onClick={() => openEditModal(staff)}
                          className="h-8 w-8 p-0 text-blue-600 hover:bg-blue-50"
                          title="Sửa thông tin"
                          aria-label={`Sửa ${staff.fullName}`}
                        >
                          <Edit2 className="w-4 h-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          onClick={() => handleDelete([staff.id])}
                          className="h-8 w-8 p-0 text-red-600 hover:bg-red-50"
                          title="Xóa tài khoản"
                          aria-label={`Xóa ${staff.fullName}`}
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

      {/* Form Modal (Create / Edit) */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingStaff ? 'Cập Nhật Hồ Sơ Nhân Viên' : 'Thêm Nhân Viên Mới'}
      >
        <form onSubmit={handleSave} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-600">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Tên đăng nhập <span className="text-red-500">*</span>
              </label>
              <Input
                value={formUsername}
                onChange={(e) => setFormUsername(e.target.value)}
                disabled={!!editingStaff}
                placeholder="VD: nv_letan_01"
                className="h-11"
                aria-label="Tên đăng nhập"
              />
            </div>
            {!editingStaff && (
              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Mật khẩu ban đầu
                </label>
                <Input
                  type="password"
                  value={formPassword}
                  onChange={(e) => setFormPassword(e.target.value)}
                  placeholder="Mặc định: 123456"
                  className="h-11"
                  aria-label="Mật khẩu"
                />
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Họ & tên đệm <span className="text-red-500">*</span>
              </label>
              <Input
                value={formLastName}
                onChange={(e) => setFormLastName(e.target.value)}
                placeholder="VD: Nguyễn Văn"
                className="h-11"
                aria-label="Họ và tên đệm"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Tên <span className="text-red-500">*</span>
              </label>
              <Input
                value={formFirstName}
                onChange={(e) => setFormFirstName(e.target.value)}
                placeholder="VD: An"
                className="h-11"
                aria-label="Tên"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Email liên hệ <span className="text-red-500">*</span>
              </label>
              <Input
                type="email"
                value={formEmail}
                onChange={(e) => setFormEmail(e.target.value)}
                placeholder="VD: an.nv@hotel.utc.edu.vn"
                className="h-11"
                aria-label="Email"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Số điện thoại
              </label>
              <Input
                value={formPhone}
                onChange={(e) => setFormPhone(e.target.value)}
                placeholder="VD: 0988889999"
                className="h-11"
                aria-label="Số điện thoại"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Vai trò (Role) <span className="text-red-500">*</span>
              </label>
              <select
                value={formRoleId}
                onChange={(e) => setFormRoleId(Number(e.target.value))}
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
                aria-label="Vai trò"
              >
                <option value="">-- Chọn vai trò --</option>
                {roles.map((r) => (
                  <option key={r.id} value={r.id}>{r.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Phòng ban
              </label>
              <select
                value={formDeptId}
                onChange={(e) => setFormDeptId(e.target.value ? Number(e.target.value) : '')}
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
                aria-label="Phòng ban"
              >
                <option value="">-- Không phân phòng ban --</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                Phạm vi quyền hạn (Scope)
              </label>
              <select
                value={formScopeType}
                onChange={(e) => setFormScopeType(e.target.value)}
                className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
                aria-label="Phạm vi"
              >
                <option value="PROPERTY">Khách sạn cơ sở (PROPERTY)</option>
                <option value="REGION">Khu vực / Vùng (REGION)</option>
                <option value="CHAIN">Toàn chuỗi (CHAIN)</option>
              </select>
            </div>
            {formScopeType === 'PROPERTY' && (
              <div>
                <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
                  Thuộc khách sạn cơ sở
                </label>
                <select
                  value={formScopeEntityId}
                  onChange={(e) => setFormScopeEntityId(Number(e.target.value))}
                  className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
                  aria-label="Khách sạn trực thuộc"
                >
                  <option value="">-- Chọn khách sạn --</option>
                  {hotels.map((h) => (
                    <option key={h.id} value={h.id}>{h.name}</option>
                  ))}
                </select>
              </div>
            )}
          </div>

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
              {isSubmitting ? 'Đang lưu...' : editingStaff ? 'Lưu thay đổi' : 'Tạo nhân viên'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Status Modal */}
      <Modal
        isOpen={statusModalOpen}
        onClose={() => setStatusModalOpen(false)}
        title="Cập Nhật Trạng Thái Nhân Viên"
      >
        <form onSubmit={handleUpdateStatus} className="space-y-4">
          <p className="text-sm text-neutral-600">
            Thay đổi trạng thái tài khoản cho: <strong className="text-neutral-900">{statusTargetStaff?.fullName}</strong> (@{statusTargetStaff?.username})
          </p>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
              Trạng thái mới
            </label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              className="w-full h-11 px-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              aria-label="Trạng thái mới"
            >
              <option value="ACTIVE">Hoạt động (ACTIVE)</option>
              <option value="SUSPENDED">Tạm khóa tài khoản (SUSPENDED)</option>
              <option value="RESIGNED">Đã thôi việc (RESIGNED)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase mb-1">
              Lý do thay đổi
            </label>
            <textarea
              value={statusReason}
              onChange={(e) => setStatusReason(e.target.value)}
              placeholder="VD: Nhân viên xin nghỉ việc / Khóa theo yêu cầu kiểm toán..."
              rows={2}
              className="w-full p-3 border border-neutral-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-600"
              aria-label="Lý do thay đổi"
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-3 border-t border-neutral-200">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setStatusModalOpen(false)}
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
              {isSubmitting ? 'Đang cập nhật...' : 'Xác nhận cập nhật'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={confirmDelete}
        title="Xác nhận xóa tài khoản nhân viên?"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} nhân viên đã chọn? Tài khoản sẽ bị vô hiệu hóa vĩnh viễn trên Keycloak và hệ thống.`}
        confirmText="Xóa vĩnh viễn"
        cancelText="Hủy"
        isLoading={isDeleting}
        variant="danger"
      />
    </div>
  )
}

export default StaffListPage
