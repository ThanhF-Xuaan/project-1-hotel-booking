import React, { useState, useEffect, useCallback } from 'react';
import {
  Plus,
  Trash2,
  RefreshCw,
  Search,
  Edit2,
  KeyRound,
  Shield,
  UserCheck,
  Building,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import staffService from '../services/staff.service';
import roleService from '../services/role.service';
import departmentService from '../../organization/services/department.service';
import type {
  StaffDto,
  StaffScopeType,
  StaffStatus,
} from '../types/staff.types';
import type { RoleDto } from '../types/role.types';
import type { DepartmentDto } from '../../organization/types/department.types';

export const StaffListPage: React.FC = () => {
  const [staffList, setStaffList] = useState<StaffDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('');
  const [deptFilter, setDeptFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);

  // Reference lists for dropdowns
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // Create / Edit Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState<StaffDto | null>(null);
  const [formUsername, setFormUsername] = useState('');
  const [formFirstName, setFormFirstName] = useState('');
  const [formLastName, setFormLastName] = useState('');
  const [formEmail, setFormEmail] = useState('');
  const [formPhone, setFormPhone] = useState('');
  const [formRoleId, setFormRoleId] = useState<number | ''>('');
  const [formDeptId, setFormDeptId] = useState<number | ''>('');
  const [formScopeType, setFormScopeType] = useState<StaffScopeType>('PROPERTY');
  const [formScopeEntityId, setFormScopeEntityId] = useState<number | ''>(1);
  const [formPassword, setFormPassword] = useState('');
  const [formStatus, setFormStatus] = useState<StaffStatus>('ACTIVE');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Reset Password Modal
  const [resetPasswordModalOpen, setResetPasswordModalOpen] = useState(false);
  const [targetStaff, setTargetStaff] = useState<StaffDto | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [resetError, setResetError] = useState<string | null>(null);
  const [isResetting, setIsResetting] = useState(false);
  const [resetSuccessMessage, setResetSuccessMessage] = useState<string | null>(null);

  // Status Change Modal
  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [statusTargetStaff, setStatusTargetStaff] = useState<StaffDto | null>(null);
  const [newStatus, setNewStatus] = useState<StaffStatus>('ACTIVE');
  const [statusReason, setStatusReason] = useState('');
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deletingIds, setDeletingIds] = useState<number[]>([]);
  const [isDeleting, setIsDeleting] = useState(false);

  // Load reference data (roles & departments)
  useEffect(() => {
    let ignore = false;
    const fetchRefs = async () => {
      try {
        const [rolesRes, deptsRes] = await Promise.all([
          roleService.filter({ page: 0, pageSize: 50 }),
          departmentService.getAllActive(),
        ]);
        if (!ignore) {
          if (rolesRes?.result?.content) setRoles(rolesRes.result.content);
          if (deptsRes?.result) setDepartments(deptsRes.result);
        }
      } catch (err) {
        console.error('Lỗi tải danh mục vai trò / phòng ban:', err);
      }
    };
    void fetchRefs();
    return () => {
      ignore = true;
    };
  }, []);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await staffService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        roleId: roleFilter ? Number(roleFilter) : undefined,
        departmentId: deptFilter ? Number(deptFilter) : undefined,
        status: statusFilter || undefined,
      });
      if (response?.result) {
        setStaffList(response.result.content || []);
        setTotalElements(response.result.totalElements || 0);
        setTotalPages(response.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách nhân sự:', err);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, keyword, roleFilter, deptFilter, statusFilter]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadData();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadData]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    void loadData();
  };

  const handleReset = () => {
    setKeyword('');
    setRoleFilter('');
    setDeptFilter('');
    setStatusFilter('');
    setPage(0);
  };

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(staffList.map((s) => s.id));
    } else {
      setSelectedIds([]);
    }
  };

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const openCreateModal = () => {
    setEditingStaff(null);
    setFormUsername('');
    setFormFirstName('');
    setFormLastName('');
    setFormEmail('');
    setFormPhone('');
    setFormRoleId(roles.length > 0 ? roles[0].id : '');
    setFormDeptId(departments.length > 0 ? departments[0].id : '');
    setFormScopeType('PROPERTY');
    setFormScopeEntityId(1);
    setFormPassword('');
    setFormStatus('ACTIVE');
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const openEditModal = (staff: StaffDto) => {
    setEditingStaff(staff);
    setFormUsername(staff.username);
    setFormFirstName(staff.firstName || '');
    setFormLastName(staff.lastName || '');
    setFormEmail(staff.email || '');
    setFormPhone(staff.phone || '');
    setFormRoleId(staff.roleId);
    setFormDeptId(staff.departmentId || '');
    setFormScopeType(staff.scopeType || 'PROPERTY');
    setFormScopeEntityId(staff.scopeEntityId || 1);
    setFormPassword('');
    setFormStatus(staff.status);
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formRoleId) {
      setFormError('Vui lòng chọn vai trò cho nhân sự.');
      return;
    }
    setFormError(null);
    setIsSubmitting(true);
    try {
      if (editingStaff) {
        await staffService.update(editingStaff.id, {
          firstName: formFirstName.trim(),
          lastName: formLastName.trim(),
          email: formEmail.trim() || undefined,
          phone: formPhone.trim() || undefined,
          roleId: Number(formRoleId),
          departmentId: formDeptId ? Number(formDeptId) : undefined,
          scopeType: formScopeType,
          scopeEntityId: formScopeEntityId ? Number(formScopeEntityId) : undefined,
          status: formStatus,
          password: formPassword.trim() || undefined,
        });
      } else {
        if (!formUsername.trim()) {
          setFormError('Tên đăng nhập không được để trống.');
          setIsSubmitting(false);
          return;
        }
        await staffService.create({
          username: formUsername.trim(),
          firstName: formFirstName.trim(),
          lastName: formLastName.trim(),
          email: formEmail.trim(),
          phone: formPhone.trim(),
          roleId: Number(formRoleId),
          departmentId: formDeptId ? Number(formDeptId) : undefined,
          scopeType: formScopeType,
          scopeEntityId: formScopeEntityId ? Number(formScopeEntityId) : undefined,
          password: formPassword.trim() || undefined,
          status: formStatus,
        });
      }
      setIsFormModalOpen(false);
      void loadData();
    } catch (err: unknown) {
      const errorMsg =
        err instanceof Error ? err.message : 'Đã có lỗi xảy ra khi lưu nhân sự.';
      setFormError(errorMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Reset Password
  const openResetPasswordModal = (staff: StaffDto) => {
    setTargetStaff(staff);
    setNewPassword('');
    setResetError(null);
    setResetSuccessMessage(null);
    setResetPasswordModalOpen(true);
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetStaff) return;
    if (newPassword.trim().length < 6) {
      setResetError('Mật khẩu mới phải có tối thiểu 6 ký tự.');
      return;
    }
    setIsResetting(true);
    setResetError(null);
    try {
      await staffService.resetPassword(targetStaff.id, {
        newPassword: newPassword.trim(),
      });
      setResetSuccessMessage(`Đã đặt lại mật khẩu cho nhân viên ${targetStaff.username} thành công!`);
      setTimeout(() => {
        setResetPasswordModalOpen(false);
      }, 1500);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể đặt lại mật khẩu.';
      setResetError(msg);
    } finally {
      setIsResetting(false);
    }
  };

  // Status Lifecycle Update
  const openStatusModal = (staff: StaffDto) => {
    setStatusTargetStaff(staff);
    setNewStatus(staff.status);
    setStatusReason('');
    setStatusModalOpen(true);
  };

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!statusTargetStaff) return;
    setIsUpdatingStatus(true);
    try {
      await staffService.updateStatus(statusTargetStaff.id, {
        status: newStatus,
        reason: statusReason.trim() || undefined,
      });
      setStatusModalOpen(false);
      void loadData();
    } catch (err) {
      console.error('Lỗi đổi trạng thái nhân sự:', err);
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  // Delete Handlers
  const handleDeleteSingle = (id: number) => {
    setDeletingIds([id]);
    setDeleteConfirmOpen(true);
  };

  const handleDeleteBatch = () => {
    if (selectedIds.length === 0) return;
    setDeletingIds(selectedIds);
    setDeleteConfirmOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return;
    setIsDeleting(true);
    try {
      await staffService.deleteBatch(deletingIds);
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)));
      setDeleteConfirmOpen(false);
      void loadData();
    } catch (err) {
      console.error('Lỗi khi xóa nhân sự:', err);
    } finally {
      setIsDeleting(false);
    }
  };

  const getStatusBadge = (st: StaffStatus) => {
    switch (st) {
      case 'ACTIVE':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
            Đang hoạt động
          </span>
        );
      case 'SUSPENDED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-600"></span>
            Tạm đình chỉ
          </span>
        );
      case 'RESIGNED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800">
            <span className="w-1.5 h-1.5 rounded-full bg-red-600"></span>
            Đã nghỉ việc
          </span>
        );
      default:
        return <span className="text-xs text-neutral-500">{st}</span>;
    }
  };

  return (
    <div className="space-y-4">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-neutral-900 tracking-tight">
          Quản lý Nhân sự (Staff Accounts)
        </h1>
        <p className="text-sm text-neutral-500 mt-0.5">
          Quản lý danh sách nhân viên, gán vai trò IAM, phân cấp phạm vi và điều phối vòng đời tài khoản.
        </p>
      </div>

      {/* HÀNG 1: SEARCH & FILTER BAR */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-3 items-center">
          <div className="lg:col-span-4">
            <Input
              placeholder="Tìm theo Username, Họ tên, Email, SĐT..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>

          <div className="lg:col-span-2">
            <select
              className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
            >
              <option value="">-- Tất cả vai trò --</option>
              {roles.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </div>

          <div className="lg:col-span-2">
            <select
              className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
              value={deptFilter}
              onChange={(e) => setDeptFilter(e.target.value)}
            >
              <option value="">-- Tất cả phòng ban --</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>

          <div className="lg:col-span-2">
            <select
              className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">-- Trạng thái --</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="SUSPENDED">Tạm đình chỉ</option>
              <option value="RESIGNED">Đã nghỉ việc</option>
            </select>
          </div>

          <div className="lg:col-span-2 flex gap-2 justify-end">
            <Button
              type="submit"
              variant="primary"
              className="bg-red-600 hover:bg-red-700 text-white font-medium flex-1 h-10 md:h-12"
            >
              <Search className="w-4 h-4 mr-1.5" />
              Lọc
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleReset}
              className="h-10 md:h-12 px-3"
              title="Đặt lại bộ lọc"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </div>

      {/* HÀNG 2: ACTION TOOLBAR */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <div className="flex items-center gap-2.5">
          <Button
            type="button"
            variant="primary"
            onClick={openCreateModal}
            className="bg-red-600 hover:bg-red-700 text-white font-bold h-10 px-4 rounded-xl shadow-xs"
          >
            <Plus className="w-4 h-4 mr-1.5" />
            Thêm nhân viên
          </Button>

          {selectedIds.length > 0 && (
            <Button
              type="button"
              variant="danger"
              onClick={handleDeleteBatch}
              className="h-10 px-3.5 rounded-xl text-xs font-semibold"
            >
              <Trash2 className="w-3.5 h-3.5 mr-1.5" />
              Xóa đã chọn ({selectedIds.length})
            </Button>
          )}
        </div>

        <div className="text-xs text-neutral-500 font-medium">
          Tổng số: <strong className="text-neutral-900 font-mono">{totalElements}</strong> nhân viên
        </div>
      </div>

      {/* HÀNG 3: DATA TABLE */}
      <div className="bg-white rounded-2xl border border-neutral-200 overflow-hidden shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-neutral-50/80 border-b border-neutral-200 text-neutral-700 font-bold text-xs">
                <th className="py-3.5 px-4 w-10">
                  <input
                    type="checkbox"
                    checked={staffList.length > 0 && selectedIds.length === staffList.length}
                    onChange={handleSelectAll}
                    className="rounded text-red-600 focus:ring-red-500 cursor-pointer"
                  />
                </th>
                <th className="py-3.5 px-4">Nhân sự</th>
                <th className="py-3.5 px-4">Liên hệ</th>
                <th className="py-3.5 px-4">Vai trò & Phòng ban</th>
                <th className="py-3.5 px-4">Phạm vi (Scope)</th>
                <th className="py-3.5 px-4">Trạng thái</th>
                <th className="py-3.5 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
                    Đang tải dữ liệu nhân sự...
                  </td>
                </tr>
              ) : staffList.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    Không tìm thấy nhân viên nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                staffList.map((staff) => (
                  <tr
                    key={staff.id}
                    className={`hover:bg-neutral-50/60 transition-colors ${
                      selectedIds.includes(staff.id) ? 'bg-red-50/30' : ''
                    }`}
                  >
                    <td className="py-3.5 px-4">
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(staff.id)}
                        onChange={() => handleSelectOne(staff.id)}
                        className="rounded text-red-600 focus:ring-red-500 cursor-pointer"
                      />
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-bold text-neutral-900">{staff.fullName}</div>
                      <div className="text-xs text-neutral-500 font-mono">@{staff.username}</div>
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="text-xs text-neutral-800">{staff.email}</div>
                      <div className="text-xs text-neutral-500 font-mono">{staff.phone}</div>
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-1 font-semibold text-neutral-900 text-xs">
                        <Shield className="w-3.5 h-3.5 text-red-600" />
                        {staff.roleName || staff.roleCode}
                      </div>
                      <div className="flex items-center gap-1 text-xs text-neutral-500 mt-0.5">
                        <Building className="w-3 h-3 text-neutral-400" />
                        {staff.departmentName || 'Chưa gán'}
                      </div>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-flex px-2 py-0.5 rounded-md text-[11px] font-mono font-bold bg-neutral-100 text-neutral-700">
                        {staff.scopeType} {staff.scopeEntityId ? `#${staff.scopeEntityId}` : ''}
                      </span>
                    </td>
                    <td className="py-3.5 px-4">{getStatusBadge(staff.status)}</td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          type="button"
                          onClick={() => openResetPasswordModal(staff)}
                          className="p-1.5 text-neutral-500 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-colors cursor-pointer"
                          title="Đặt lại mật khẩu"
                        >
                          <KeyRound className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => openStatusModal(staff)}
                          className="p-1.5 text-neutral-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
                          title="Đổi trạng thái công tác"
                        >
                          <UserCheck className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => openEditModal(staff)}
                          className="p-1.5 text-neutral-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                          title="Chỉnh sửa thông tin"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteSingle(staff.id)}
                          className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                          title="Xóa nhân sự"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Phân trang */}
        {totalPages > 1 && (
          <div className="p-4 border-t border-neutral-100">
            <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
          </div>
        )}
      </div>

      {/* CREATE / EDIT STAFF MODAL */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingStaff ? `Chỉnh sửa nhân viên: ${editingStaff.username}` : 'Thêm mới nhân viên'}
        description="Thông tin nhân sự được đồng bộ tự động vào Keycloak IAM và kiểm soát quyền truy cập."
        maxWidth="3xl"
      >
        <form onSubmit={handleSubmitForm} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Tên đăng nhập (Username) <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="receptionist_hn"
                value={formUsername}
                onChange={(e) => setFormUsername(e.target.value)}
                disabled={!!editingStaff}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Vai trò (Role) <span className="text-red-500">*</span>
              </label>
              <select
                className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={formRoleId}
                onChange={(e) => setFormRoleId(Number(e.target.value))}
                required
              >
                <option value="">-- Chọn vai trò --</option>
                {roles.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name} ({r.code})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Họ và tên đệm <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="Nguyễn"
                value={formLastName}
                onChange={(e) => setFormLastName(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Tên <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="Văn A"
                value={formFirstName}
                onChange={(e) => setFormFirstName(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Email liên hệ <span className="text-red-500">*</span>
              </label>
              <Input
                type="email"
                placeholder="staff@utc.edu.vn"
                value={formEmail}
                onChange={(e) => setFormEmail(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Số điện thoại
              </label>
              <Input
                placeholder="0988889999"
                value={formPhone}
                onChange={(e) => setFormPhone(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Phòng ban trực thuộc
              </label>
              <select
                className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={formDeptId}
                onChange={(e) => setFormDeptId(e.target.value ? Number(e.target.value) : '')}
              >
                <option value="">-- Không phân phòng ban --</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Phạm vi dữ liệu (Scope Type)
              </label>
              <select
                className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
                value={formScopeType}
                onChange={(e) => setFormScopeType(e.target.value as StaffScopeType)}
              >
                <option value="CHAIN">CHAIN (Toàn bộ chuỗi)</option>
                <option value="REGION">REGION (Theo khu vực)</option>
                <option value="PROPERTY">PROPERTY (Khách sạn cơ sở)</option>
              </select>
            </div>

            {!editingStaff && (
              <div className="md:col-span-2">
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Mật khẩu khởi tạo ban đầu (Tùy chọn, mặc định sinh tự động)
                </label>
                <Input
                  type="password"
                  placeholder="Nhập tối thiểu 6 ký tự..."
                  value={formPassword}
                  onChange={(e) => setFormPassword(e.target.value)}
                />
              </div>
            )}
          </div>

          <div className="flex justify-end gap-2.5 pt-4 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={isSubmitting}
              className="bg-red-600 hover:bg-red-700 text-white font-bold"
            >
              {isSubmitting ? 'Đang lưu...' : editingStaff ? 'Cập nhật' : 'Thêm nhân sự'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* RESET PASSWORD MODAL */}
      <Modal
        isOpen={resetPasswordModalOpen}
        onClose={() => setResetPasswordModalOpen(false)}
        title={`Đặt lại mật khẩu: @${targetStaff?.username}`}
        description="Mật khẩu mới sẽ được mã hóa và đồng bộ trực tiếp vào máy chủ xác thực Keycloak."
        maxWidth="md"
      >
        <form onSubmit={handleResetPassword} className="space-y-4">
          {resetSuccessMessage && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl font-medium">
              {resetSuccessMessage}
            </div>
          )}
          {resetError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl">
              {resetError}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Mật khẩu mới <span className="text-red-500">*</span>
            </label>
            <Input
              type="password"
              placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)..."
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setResetPasswordModalOpen(false)}
              disabled={isResetting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={isResetting || !newPassword.trim()}
              className="bg-red-600 hover:bg-red-700 text-white font-bold"
            >
              {isResetting ? 'Đang cập nhật...' : 'Xác nhận đặt lại'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* UPDATE STATUS MODAL */}
      <Modal
        isOpen={statusModalOpen}
        onClose={() => setStatusModalOpen(false)}
        title={`Vòng đời công tác: ${statusTargetStaff?.fullName}`}
        description="Thay đổi trạng thái nhân sự. Nếu tạm đình chỉ hoặc thôi việc, toàn bộ phiên đăng nhập sẽ bị thu hồi."
        maxWidth="md"
      >
        <form onSubmit={handleUpdateStatus} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Trạng thái nhân sự mới
            </label>
            <select
              className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value as StaffStatus)}
            >
              <option value="ACTIVE">ACTIVE - Đang làm việc</option>
              <option value="SUSPENDED">SUSPENDED - Tạm đình chỉ công tác</option>
              <option value="RESIGNED">RESIGNED - Đã chấm dứt hợp đồng / Nghỉ việc</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Lý do thay đổi (Phục vụ kiểm toán nội bộ)
            </label>
            <Input
              placeholder="Ví dụ: Nghỉ thai sản, Chuyển công tác, Nghỉ việc..."
              value={statusReason}
              onChange={(e) => setStatusReason(e.target.value)}
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setStatusModalOpen(false)}
              disabled={isUpdatingStatus}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              disabled={isUpdatingStatus}
              className="bg-red-600 hover:bg-red-700 text-white font-bold"
            >
              {isUpdatingStatus ? 'Đang lưu...' : 'Lưu trạng thái'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* CONFIRM DELETE MODAL */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa tài khoản nhân viên"
        message={`Bạn có chắc chắn muốn xóa mềm ${deletingIds.length} tài khoản nhân viên đã chọn? Tài khoản sẽ không thể đăng nhập hệ thống sau khi xóa.`}
        confirmText="Xóa nhân viên"
        cancelText="Hủy"
        type="danger"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default StaffListPage;
