import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { Plus, Trash2, RefreshCw, Search, Edit2, Shield, CheckSquare, Square } from 'lucide-react'
import Button from '../../../core/components/ui/Button'
import Input from '../../../core/components/ui/Input'
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal'
import Pagination from '../../../core/components/ui/Pagination'
import { roleService } from '../services/identity.service'
import type { RoleDto, PermissionDto } from '../types/identity.types'

export const RoleListPage: React.FC = () => {
  const [roles, setRoles] = useState<RoleDto[]>([])
  const [allPermissions, setAllPermissions] = useState<PermissionDto[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  // Modal Create/Edit
  const [isFormModalOpen, setIsFormModalOpen] = useState(false)
  const [editingRole, setEditingRole] = useState<RoleDto | null>(null)
  const [formName, setFormName] = useState('')
  const [formCode, setFormCode] = useState('')
  const [formStatus, setFormStatus] = useState<string>('ACTIVE')
  const [formPermissionIds, setFormPermissionIds] = useState<number[]>([])
  const [formError, setFormError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [deletingIds, setDeletingIds] = useState<number[]>([])
  const [isDeleting, setIsDeleting] = useState(false)

  // Load all system permissions
  useEffect(() => {
    let isMounted = true
    roleService.getAllPermissions().then((res) => {
      if (isMounted && res.result) {
        setAllPermissions(res.result)
      }
    }).catch((err) => {
      console.error('Không thể tải danh sách quyền hạn:', err)
    })
    return () => { isMounted = false }
  }, [])

  // Group permissions by module
  const permissionsByModule = useMemo(() => {
    const map: Record<string, PermissionDto[]> = {}
    allPermissions.forEach((p) => {
      const mod = p.module || 'HỆ THỐNG'
      if (!map[mod]) map[mod] = []
      map[mod].push(p)
    })
    return map
  }, [allPermissions])

  const loadData = useCallback(async () => {
    setIsLoading(true)
    try {
      const res = await roleService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        status: statusFilter || undefined,
      })
      if (res.result) {
        setRoles(res.result.content || [])
        setTotalElements(res.result.totalElements || 0)
        setTotalPages(res.result.totalPages || 0)
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách vai trò:', err)
    } finally {
      setIsLoading(false)
    }
  }, [page, pageSize, keyword, statusFilter])

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
    setKeyword('')
    setStatusFilter('')
    setPage(0)
  }

  // Row selection
  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(roles.map((r) => r.id))
    } else {
      setSelectedIds([])
    }
  }

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    )
  }

  // Open Create Form
  const handleOpenCreate = () => {
    setEditingRole(null)
    setFormName('')
    setFormCode('')
    setFormStatus('ACTIVE')
    setFormPermissionIds([])
    setFormError(null)
    setIsFormModalOpen(true)
  }

  // Open Edit Form
  const handleOpenEdit = async (role: RoleDto) => {
    setEditingRole(role)
    setFormName(role.name)
    setFormCode(role.code)
    setFormStatus(role.status)
    setFormError(null)
    setIsFormModalOpen(true)

    // Fetch detail role to get exact permission IDs
    try {
      const detailRes = await roleService.getById(role.id)
      if (detailRes.result?.permissions) {
        setFormPermissionIds(detailRes.result.permissions.map((p) => p.id))
      } else {
        setFormPermissionIds([])
      }
    } catch {
      setFormPermissionIds(role.permissions?.map((p) => p.id) || [])
    }
  }

  // Toggle single permission
  const handleTogglePermission = (pId: number) => {
    setFormPermissionIds((prev) =>
      prev.includes(pId) ? prev.filter((id) => id !== pId) : [...prev, pId]
    )
  }

  // Toggle all permissions in a module
  const handleToggleModulePermissions = (moduleName: string) => {
    const modulePerms = permissionsByModule[moduleName] || []
    const modulePermIds = modulePerms.map((p) => p.id)
    const allSelected = modulePermIds.every((id) => formPermissionIds.includes(id))

    if (allSelected) {
      setFormPermissionIds((prev) => prev.filter((id) => !modulePermIds.includes(id)))
    } else {
      setFormPermissionIds((prev) => Array.from(new Set([...prev, ...modulePermIds])))
    }
  }

  // Submit Create / Edit
  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!formName.trim()) {
      setFormError('Vui lòng nhập tên vai trò')
      return
    }

    if (!editingRole && !formCode.trim()) {
      setFormError('Vui lòng nhập mã vai trò')
      return
    }

    setIsSubmitting(true)
    setFormError(null)

    try {
      if (editingRole) {
        await roleService.update(editingRole.id, {
          name: formName.trim(),
          status: formStatus,
          permissionIds: formPermissionIds,
        })
      } else {
        await roleService.create({
          name: formName.trim(),
          code: formCode.trim().toUpperCase(),
          permissionIds: formPermissionIds,
        })
      }
      setIsFormModalOpen(false)
      loadData()
    } catch (err: unknown) {
      const errorMsg = (err as { message?: string })?.message || 'Có lỗi xảy ra khi lưu vai trò'
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
      await roleService.deleteBatch(deletingIds)
      setDeleteConfirmOpen(false)
      setDeletingIds([])
      setSelectedIds([])
      loadData()
    } catch (err: unknown) {
      const errorMsg = (err as { message?: string })?.message || 'Không thể xóa vai trò đã chọn'
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
            <Shield className="w-7 h-7 text-red-600" />
            Vai trò & Phân quyền
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quản lý vai trò người dùng (Roles) và ma trận phân quyền chi tiết (Permissions) trong hệ thống
          </p>
        </div>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 sm:p-5 rounded-2xl border border-neutral-200/80 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-12 gap-3 sm:gap-4 items-end">
          <div className="sm:col-span-6 lg:col-span-5">
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Từ khóa tìm kiếm
            </label>
            <div className="relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
              <input
                type="text"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="Tìm theo tên hoặc mã vai trò..."
                className="w-full h-10 sm:h-12 pl-10 pr-4 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
              />
            </div>
          </div>

          <div className="sm:col-span-3 lg:col-span-3">
            <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
              Trạng thái
            </label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full h-10 sm:h-12 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-red-600 transition-all bg-white"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang kích hoạt</option>
              <option value="INACTIVE">Ngừng kích hoạt</option>
            </select>
          </div>

          <div className="sm:col-span-3 lg:col-span-4 flex items-center gap-2">
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
            <span>Thêm vai trò mới</span>
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
          Tổng cộng: <span className="font-bold text-neutral-900">{totalElements}</span> vai trò
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
                    checked={roles.length > 0 && selectedIds.length === roles.length}
                    onChange={handleSelectAll}
                    className="w-4 h-4 text-red-600 border-neutral-300 rounded focus:ring-red-600 cursor-pointer"
                  />
                </th>
                <th className="p-4">Mã vai trò (Code)</th>
                <th className="p-4">Tên vai trò</th>
                <th className="p-4 text-center">Số quyền hạn</th>
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
                      <span>Đang tải dữ liệu...</span>
                    </div>
                  </td>
                </tr>
              ) : roles.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-12 text-center text-neutral-400">
                    Không tìm thấy vai trò nào phù hợp
                  </td>
                </tr>
              ) : (
                roles.map((role) => {
                  const isSelected = selectedIds.includes(role.id)
                  const isProtected = ['ROLE_CHAIN_ADMIN', 'ROLE_CHAIN_EXECUTIVE'].includes(role.code)
                  return (
                    <tr
                      key={role.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isSelected ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-4 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(role.id)}
                          disabled={isProtected}
                          className="w-4 h-4 text-red-600 border-neutral-300 rounded focus:ring-red-600 cursor-pointer disabled:opacity-30"
                        />
                      </td>
                      <td className="p-4">
                        <span className="font-mono text-xs font-bold px-2 py-1 bg-neutral-100 text-neutral-800 rounded-md border border-neutral-200">
                          {role.code}
                        </span>
                      </td>
                      <td className="p-4 font-semibold text-neutral-900">
                        {role.name}
                      </td>
                      <td className="p-4 text-center">
                        <span className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-0.5 rounded-full bg-blue-50 text-blue-700 border border-blue-200">
                          <Shield className="w-3 h-3" />
                          {role.permissions?.length || 0} quyền
                        </span>
                      </td>
                      <td className="p-4 text-center">
                        {role.status === 'ACTIVE' ? (
                          <span className="inline-block text-xs font-semibold px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                            Đang kích hoạt
                          </span>
                        ) : (
                          <span className="inline-block text-xs font-semibold px-2.5 py-1 rounded-full bg-neutral-100 text-neutral-600 border border-neutral-200">
                            Ngừng kích hoạt
                          </span>
                        )}
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => handleOpenEdit(role)}
                            className="p-2 text-neutral-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="Chỉnh sửa vai trò"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          {!isProtected && (
                            <button
                              type="button"
                              onClick={() => {
                                setDeletingIds([role.id])
                                setDeleteConfirmOpen(true)
                              }}
                              className="p-2 text-neutral-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                              title="Xóa vai trò"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          )}
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

      {/* MODAL: CREATE / EDIT ROLE & PERMISSIONS */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingRole ? `Cập nhật vai trò: ${editingRole.name}` : 'Thêm mới vai trò'}
        maxWidth="4xl"
      >
        <form onSubmit={handleSubmitForm} className="space-y-5">
          {formError && (
            <div className="p-3 bg-red-50 text-red-700 text-sm rounded-xl border border-red-200 font-medium">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Tên vai trò"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              placeholder="VD: Quản lý buồng phòng cơ sở"
              required
            />
            <Input
              label="Mã vai trò (ROLE_...)"
              value={formCode}
              onChange={(e) => setFormCode(e.target.value.toUpperCase())}
              placeholder="VD: ROLE_HOUSEKEEPING_SUPERVISOR"
              required={!editingRole}
              disabled={!!editingRole}
            />
          </div>

          {editingRole && (
            <div>
              <label className="block text-xs font-semibold text-neutral-700 uppercase mb-1.5">
                Trạng thái hoạt động
              </label>
              <select
                value={formStatus}
                onChange={(e) => setFormStatus(e.target.value)}
                className="w-full h-11 px-3 text-sm rounded-xl border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-red-600 bg-white"
              >
                <option value="ACTIVE">Đang kích hoạt (ACTIVE)</option>
                <option value="INACTIVE">Ngừng kích hoạt (INACTIVE)</option>
              </select>
            </div>
          )}

          {/* PERMISSION SELECTOR */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="block text-xs font-semibold text-neutral-700 uppercase">
                Phân quyền chức năng ({formPermissionIds.length} / {allPermissions.length} quyền đã chọn)
              </label>
            </div>

            <div className="border border-neutral-200 rounded-xl p-3 max-h-80 overflow-y-auto space-y-4 bg-neutral-50/50">
              {Object.keys(permissionsByModule).length === 0 ? (
                <div className="text-center py-6 text-sm text-neutral-400">
                  Đang tải danh sách quyền hệ thống...
                </div>
              ) : (
                Object.entries(permissionsByModule).map(([moduleName, perms]) => {
                  const modulePermIds = perms.map((p) => p.id)
                  const allInModuleSelected = modulePermIds.every((id) =>
                    formPermissionIds.includes(id)
                  )

                  return (
                    <div key={moduleName} className="bg-white p-3 rounded-lg border border-neutral-200/80 shadow-2xs">
                      <div className="flex items-center justify-between pb-2 mb-2 border-b border-neutral-100">
                        <span className="font-bold text-xs uppercase text-neutral-900 tracking-wider">
                          Mô-đun: {moduleName} ({perms.length})
                        </span>
                        <button
                          type="button"
                          onClick={() => handleToggleModulePermissions(moduleName)}
                          className="text-xs font-semibold text-red-600 hover:text-red-700 flex items-center gap-1 cursor-pointer"
                        >
                          {allInModuleSelected ? (
                            <>
                              <CheckSquare className="w-3.5 h-3.5" />
                              Bỏ chọn nhóm
                            </>
                          ) : (
                            <>
                              <Square className="w-3.5 h-3.5" />
                              Chọn tất cả
                            </>
                          )}
                        </button>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        {perms.map((p) => {
                          const isChecked = formPermissionIds.includes(p.id)
                          return (
                            <label
                              key={p.id}
                              className={`flex items-start gap-2 p-2 rounded-lg border cursor-pointer transition-colors text-xs ${
                                isChecked
                                  ? 'bg-red-50/40 border-red-200 text-neutral-900'
                                  : 'bg-white border-neutral-200 text-neutral-600 hover:bg-neutral-50'
                              }`}
                            >
                              <input
                                type="checkbox"
                                checked={isChecked}
                                onChange={() => handleTogglePermission(p.id)}
                                className="mt-0.5 w-3.5 h-3.5 text-red-600 border-neutral-300 rounded focus:ring-red-600"
                              />
                              <div>
                                <div className="font-semibold">{p.name}</div>
                                <div className="text-[10px] font-mono text-neutral-400">{p.code}</div>
                              </div>
                            </label>
                          )
                        })}
                      </div>
                    </div>
                  )
                })
              )}
            </div>
          </div>

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
              {isSubmitting ? 'Đang lưu...' : editingRole ? 'Cập nhật' : 'Tạo mới'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* MODAL: CONFIRM DELETE */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa vai trò"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} vai trò đã chọn? Thao tác này sẽ thu hồi quyền của các người dùng liên kết.`}
        confirmText="Xác nhận xóa"
        cancelText="Hủy"
        isLoading={isDeleting}
        variant="danger"
      />
    </div>
  )
}

export default RoleListPage
