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
  Eye,
  EyeOff,
  Sparkles,
  Copy,
  Check,
  AlertCircle,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import staffService from '../services/staff.service';
import roleService from '../services/role.service';
import departmentService from '../../organization/services/department.service';
import hotelService from '../../organization/services/hotel.service';
import regionService from '../../organization/services/region.service';
import type {
  StaffDto,
  StaffScopeType,
  StaffStatus,
} from '../types/staff.types';
import type { RoleDto } from '../types/role.types';
import type { DepartmentDto } from '../../organization/types/department.types';
import type { HotelDto, RegionDto } from '../../organization/types/organization.types';
import {
  validateStaffForm,
  type StaffFormFieldErrors,
} from '../types/staff.schema';

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
  const [hotels, setHotels] = useState<HotelDto[]>([]);
  const [regions, setRegions] = useState<RegionDto[]>([]);

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
  const [formConfirmPassword, setFormConfirmPassword] = useState('');
  const [showFormPassword, setShowFormPassword] = useState(false);
  const [formStatus, setFormStatus] = useState<StaffStatus>('ACTIVE');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formFieldErrors, setFormFieldErrors] = useState<StaffFormFieldErrors>({});

  const clearFieldError = (field: keyof StaffFormFieldErrors) => {
    setFormFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  // Success Modal State for Created Credentials
  const [createdCredentials, setCreatedCredentials] = useState<{
    username: string;
    password: string;
    fullName: string;
    roleName: string;
    scope: string;
  } | null>(null);
  const [copiedField, setCopiedField] = useState<'username' | 'password' | null>(null);

  // Reset Password Modal
  const [resetPasswordModalOpen, setResetPasswordModalOpen] = useState(false);
  const [targetStaff, setTargetStaff] = useState<StaffDto | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showResetPassword, setShowResetPassword] = useState(false);
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

  // Load reference data (roles, departments, hotels, regions)
  useEffect(() => {
    let ignore = false;
    const fetchRefs = async () => {
      try {
        const [rolesRes, deptsRes, hotelsRes, regionsRes] = await Promise.all([
          roleService.filter({ page: 0, pageSize: 50 }),
          departmentService.getAllActive(),
          hotelService.filter({ page: 0, pageSize: 100 }),
          regionService.filter({ page: 0, pageSize: 100 }),
        ]);
        if (!ignore) {
          if (rolesRes?.result?.content) setRoles(rolesRes.result.content);
          if (deptsRes?.result) setDepartments(deptsRes.result);
          if (hotelsRes?.result?.content) setHotels(hotelsRes.result.content);
          if (regionsRes?.result?.content) setRegions(regionsRes.result.content);
        }
      } catch (err) {
        console.error('Lỗi tải danh mục vai trò / phòng ban / cơ sở:', err);
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
    setFormDeptId('');
    setFormScopeType('PROPERTY');
    setFormScopeEntityId(hotels.length > 0 ? hotels[0].id : 1);
    setFormPassword('');
    setFormConfirmPassword('');
    setShowFormPassword(false);
    setFormStatus('ACTIVE');
    setFormError(null);
    setFormFieldErrors({});
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
    setFormScopeEntityId(staff.scopeEntityId || '');
    setFormPassword('');
    setFormConfirmPassword('');
    setShowFormPassword(false);
    setFormStatus(staff.status);
    setFormError(null);
    setFormFieldErrors({});
    setIsFormModalOpen(true);
  };

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const formData = {
      username: formUsername,
      firstName: formFirstName,
      lastName: formLastName,
      email: formEmail,
      phone: formPhone,
      roleId: formRoleId,
      scopeType: formScopeType,
      scopeEntityId: formScopeEntityId,
      password: formPassword,
      confirmPassword: formConfirmPassword,
    };

    const validation = validateStaffForm(formData, Boolean(editingStaff));

    if (!validation.isValid) {
      setFormFieldErrors(validation.errors);
      setFormError(validation.firstError || 'Vui lòng kiểm tra và sửa các thông tin chưa hợp lệ bên dưới.');
      return;
    }

    setFormFieldErrors({});
    setIsSubmitting(true);
    try {
      if (editingStaff) {
        const scopeEntityId = formScopeType === 'CHAIN'
          ? undefined
          : formScopeEntityId ? Number(formScopeEntityId) : undefined;
        const departmentId = formScopeType === 'PROPERTY' && formDeptId
          ? Number(formDeptId)
          : undefined;

        await staffService.update(editingStaff.id, {
          firstName: formFirstName.trim(),
          lastName: formLastName.trim(),
          email: formEmail.trim(),
          phone: formPhone.trim(),
          roleId: Number(formRoleId),
          departmentId,
          scopeType: formScopeType,
          scopeEntityId,
          status: formStatus,
          password: formPassword.trim() || undefined,
        });
        setIsFormModalOpen(false);
      } else {
        const scopeEntityId = formScopeType === 'CHAIN'
          ? undefined
          : formScopeEntityId ? Number(formScopeEntityId) : undefined;
        const departmentId = formScopeType === 'PROPERTY' && formDeptId
          ? Number(formDeptId)
          : undefined;

        await staffService.create({
          username: formUsername.trim(),
          firstName: formFirstName.trim(),
          lastName: formLastName.trim(),
          email: formEmail.trim(),
          phone: formPhone.trim(),
          roleId: Number(formRoleId),
          departmentId,
          scopeType: formScopeType,
          scopeEntityId,
          password: formPassword.trim(),
          status: formStatus,
        });

        const roleObj = roles.find((r) => r.id === Number(formRoleId));
        const hotelObj = hotels.find((h) => h.id === Number(formScopeEntityId));
        const regionObj = regions.find((r) => r.id === Number(formScopeEntityId));

        setCreatedCredentials({
          username: formUsername.trim(),
          password: formPassword.trim(),
          fullName: `${formLastName.trim()} ${formFirstName.trim()}`,
          roleName: roleObj ? `${roleObj.name} (${roleObj.code})` : 'Nhân sự',
          scope: formScopeType === 'CHAIN'
            ? 'CHAIN (Toàn bộ chuỗi)'
            : formScopeType === 'REGION'
              ? `REGION: ${regionObj?.name || formScopeEntityId}`
              : `PROPERTY: ${hotelObj?.name || formScopeEntityId}`,
        });
        setIsFormModalOpen(false);
      }
      void loadData();
    } catch (err: unknown) {
      console.error('Lỗi khi lưu nhân sự:', err);
      const axiosErr = err as {
        response?: {
          status?: number;
          data?: {
            code?: number;
            message?: string;
            error?: string;
            errors?: Record<string, string>;
          };
        };
        message?: string;
      };

      const resData = axiosErr?.response?.data;
      const apiCode = resData?.code;
      const apiMessage = resData?.message || resData?.error;

      // Ưu tiên trích xuất message nghiệp vụ từ backend thay vì chỉ hiển thị mã lỗi HTTP
      const displayMsg =
        apiMessage && typeof apiMessage === 'string' && apiMessage.trim().length > 0
          ? apiMessage
          : err instanceof Error && !err.message.includes('status code')
            ? err.message
            : 'Đã có lỗi xảy ra khi lưu thông tin nhân viên.';

      setFormError(displayMsg);

      // Nếu là lỗi trường cụ thể từ backend, gán trực tiếp thông báo lỗi vào trường tương ứng
      if (apiCode === 3022 || (apiMessage && /tài khoản|đăng nhập|username/i.test(apiMessage))) {
        setFormFieldErrors((prev) => ({
          ...prev,
          username: apiMessage || 'Tên đăng nhập đã được sử dụng.',
        }));
      } else if (apiCode === 3023 || (apiMessage && /email/i.test(apiMessage))) {
        setFormFieldErrors((prev) => ({
          ...prev,
          email: apiMessage || 'Địa chỉ email đã được sử dụng bởi nhân viên khác.',
        }));
      } else if (apiCode === 3024 || (apiMessage && /thoại|phone/i.test(apiMessage))) {
        setFormFieldErrors((prev) => ({
          ...prev,
          phone: apiMessage || 'Số điện thoại đã được sử dụng bởi nhân viên khác.',
        }));
      } else if (apiCode === 3025 || (apiMessage && /scope|phạm vi/i.test(apiMessage))) {
        setFormFieldErrors((prev) => ({
          ...prev,
          scopeEntityId: apiMessage || 'Cấu hình phạm vi quản lý không hợp lệ.',
        }));
      } else if (resData?.errors && typeof resData.errors === 'object') {
        setFormFieldErrors((prev) => ({
          ...prev,
          ...resData.errors,
        }));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Reset Password
  const openResetPasswordModal = (staff: StaffDto) => {
    setTargetStaff(staff);
    setNewPassword('');
    setConfirmPassword('');
    setShowResetPassword(false);
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
    if (newPassword.trim() !== confirmPassword.trim()) {
      setResetError('Mật khẩu xác nhận không khớp.');
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
      const axiosErr = err as {
        response?: { data?: { message?: string; error?: string } };
        message?: string;
      };
      const apiMessage = axiosErr?.response?.data?.message || axiosErr?.response?.data?.error;
      const msg =
        apiMessage && typeof apiMessage === 'string' && apiMessage.trim().length > 0
          ? apiMessage
          : err instanceof Error && !err.message.includes('status code')
            ? err.message
            : 'Không thể đặt lại mật khẩu.';
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
            <Select<string>
              value={roleFilter}
              onChange={(val) => setRoleFilter(val)}
              options={[
                { value: '', label: 'Tất cả vai trò' },
                ...roles.map((r) => ({ value: String(r.id), label: r.name })),
              ]}
              size="lg"
            />
          </div>

          <div className="lg:col-span-2">
            <Select<string>
              value={deptFilter}
              onChange={(val) => setDeptFilter(val)}
              options={[
                { value: '', label: 'Tất cả phòng ban' },
                ...departments.map((d) => ({ value: String(d.id), label: d.name })),
              ]}
              size="lg"
            />
          </div>

          <div className="lg:col-span-2">
            <Select<string>
              value={statusFilter}
              onChange={(val) => setStatusFilter(val)}
              options={[
                { value: '', label: 'Trạng thái' },
                { value: 'ACTIVE', label: 'Đang hoạt động' },
                { value: 'SUSPENDED', label: 'Tạm đình chỉ' },
                { value: 'RESIGNED', label: 'Đã nghỉ việc' },
              ]}
              size="lg"
            />
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
                    className={`hover:bg-neutral-50/60 transition-colors ${selectedIds.includes(staff.id) ? 'bg-red-50/30' : ''
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
            <Pagination
              pageNumber={page}
              pageSize={pageSize}
              totalElements={totalElements}
              totalPages={totalPages}
              onPageChange={setPage}
            />
          </div>
        )}
      </div>

      {/* CREATE / EDIT STAFF MODAL */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingStaff ? `Chỉnh sửa nhân viên: ${editingStaff.username}` : 'Thêm mới nhân viên'}
        description="Thông tin nhân sự được đồng bộ tự động vào Keycloak IAM và kiểm soát quyền truy cập."
        maxWidth="4xl"
      >
        <form onSubmit={handleSubmitForm} noValidate className="space-y-4" autoComplete="off">
          {/* Ngăn chặn trình duyệt tự động điền (autofill) thông tin đăng nhập đã lưu */}
          <input type="text" name="prevent_autofill_user" className="hidden" tabIndex={-1} autoComplete="username" readOnly />
          <input type="password" name="prevent_autofill_pwd" className="hidden" tabIndex={-1} autoComplete="current-password" readOnly />

          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
              <span>{formError}</span>
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
                onChange={(e) => {
                  setFormUsername(e.target.value);
                  clearFieldError('username');
                }}
                disabled={!!editingStaff}
                error={formFieldErrors.username}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Vai trò (Role) <span className="text-red-500">*</span>
              </label>
              <Select<number>
                value={formRoleId || 0}
                onChange={(val) => {
                  setFormRoleId(Number(val));
                  clearFieldError('roleId');
                }}
                options={[
                  { value: 0, label: 'Chọn vai trò', disabled: true },
                  ...roles.map((r) => ({
                    value: r.id,
                    label: `${r.name} (${r.code})`,
                  })),
                ]}
                size="lg"
                error={formFieldErrors.roleId}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Họ và tên đệm <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="Nguyễn Văn"
                value={formLastName}
                onChange={(e) => {
                  setFormLastName(e.target.value);
                  clearFieldError('lastName');
                }}
                error={formFieldErrors.lastName}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Tên <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="A"
                value={formFirstName}
                onChange={(e) => {
                  setFormFirstName(e.target.value);
                  clearFieldError('firstName');
                }}
                error={formFieldErrors.firstName}
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
                onChange={(e) => {
                  setFormEmail(e.target.value);
                  clearFieldError('email');
                }}
                error={formFieldErrors.email}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Số điện thoại <span className="text-red-500">*</span>
              </label>
              <Input
                type="tel"
                name="staff_contact_phone"
                id="staff_contact_phone"
                autoComplete="off"
                placeholder="0901234567"
                value={formPhone}
                onChange={(e) => {
                  setFormPhone(e.target.value);
                  clearFieldError('phone');
                }}
                error={formFieldErrors.phone}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Phạm vi dữ liệu (Scope Type) <span className="text-red-500">*</span>
              </label>
              <Select<StaffScopeType>
                value={formScopeType}
                onChange={(val) => {
                  setFormScopeType(val);
                  clearFieldError('scopeEntityId');
                  if (val === 'CHAIN') {
                    setFormScopeEntityId('');
                    setFormDeptId('');
                  } else if (val === 'REGION') {
                    setFormScopeEntityId(regions.length > 0 ? regions[0].id : '');
                    setFormDeptId('');
                  } else {
                    setFormScopeEntityId(hotels.length > 0 ? hotels[0].id : '');
                  }
                }}
                options={[
                  { value: 'PROPERTY', label: 'PROPERTY (Khách sạn cơ sở)' },
                  { value: 'REGION', label: 'REGION (Theo khu vực)' },
                  { value: 'CHAIN', label: 'CHAIN (Toàn bộ chuỗi)' },
                ]}
                size="lg"
              />
            </div>

            {formScopeType === 'PROPERTY' && (
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Khách sạn trực thuộc (Property) <span className="text-red-500">*</span>
                </label>
                <Select<number | ''>
                  value={formScopeEntityId === '' ? '' : Number(formScopeEntityId)}
                  onChange={(val) => {
                    setFormScopeEntityId(val ? Number(val) : '');
                    clearFieldError('scopeEntityId');
                  }}
                  options={[
                    { value: '', label: 'Chọn khách sạn', disabled: true },
                    ...hotels.map((h) => ({
                      value: h.id,
                      label: `${h.name} (${h.address})`,
                    })),
                  ]}
                  size="lg"
                  error={formFieldErrors.scopeEntityId}
                />
              </div>
            )}

            {formScopeType === 'REGION' && (
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Khu vực quản lý (Region) <span className="text-red-500">*</span>
                </label>
                <Select<number | ''>
                  value={formScopeEntityId === '' ? '' : Number(formScopeEntityId)}
                  onChange={(val) => {
                    setFormScopeEntityId(val ? Number(val) : '');
                    clearFieldError('scopeEntityId');
                  }}
                  options={[
                    { value: '', label: 'Chọn khu vực', disabled: true },
                    ...regions.map((r) => ({
                      value: r.id,
                      label: `${r.name} (${r.code})`,
                    })),
                  ]}
                  size="lg"
                  error={formFieldErrors.scopeEntityId}
                />
              </div>
            )}

            {formScopeType === 'PROPERTY' && (
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Phòng ban trực thuộc
                </label>
                <Select<number | ''>
                  value={formDeptId === '' ? '' : Number(formDeptId)}
                  onChange={(val) => setFormDeptId(val ? Number(val) : '')}
                  options={[
                    { value: '', label: 'Không phân phòng ban' },
                    ...departments.map((d) => ({
                      value: d.id,
                      label: d.name,
                    })),
                  ]}
                  size="lg"
                />
              </div>
            )}

            {formScopeType === 'CHAIN' && (
              <div className="flex items-center text-xs text-neutral-600 bg-neutral-50 p-3 rounded-xl border border-neutral-200">
                <span>Phạm vi toàn chuỗi (CHAIN) áp dụng cho cấp quản trị viên chuỗi, không gắn riêng với cơ sở hay phòng ban.</span>
              </div>
            )}

            {!editingStaff && (
              <div className="md:col-span-2 space-y-3 p-4 bg-neutral-50 rounded-2xl border border-neutral-200">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div>
                    <label className="block text-xs font-bold text-neutral-900">
                      Mật khẩu đăng nhập khởi tạo <span className="text-red-500">*</span>
                    </label>
                    <p className="text-[11px] text-neutral-500 mt-0.5">
                      Mật khẩu chính thức dùng để đăng nhập hệ thống ngay lập tức (tối thiểu 6 ký tự).
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setFormPassword('Hotel@123456');
                      setFormConfirmPassword('Hotel@123456');
                      clearFieldError('password');
                      clearFieldError('confirmPassword');
                    }}
                    className="text-[11px] text-red-600 hover:text-red-700 font-semibold bg-red-50 hover:bg-red-100 border border-red-200 px-2.5 py-1 rounded-lg transition-colors flex items-center gap-1 shrink-0 self-start sm:self-auto cursor-pointer"
                  >
                    <Sparkles className="w-3.5 h-3.5 text-red-600" />
                    Dùng mặc định: Hotel@123456
                  </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-neutral-700 mb-1">
                      Mật khẩu <span className="text-red-500">*</span>
                    </label>
                    <Input
                      type={showFormPassword ? 'text' : 'password'}
                      name="staff_creation_password"
                      id="staff_creation_password"
                      autoComplete="new-password"
                      placeholder="Nhập tối thiểu 6 ký tự..."
                      value={formPassword}
                      onChange={(e) => {
                        setFormPassword(e.target.value);
                        clearFieldError('password');
                      }}
                      error={formFieldErrors.password}
                      rightIcon={
                        <button
                          type="button"
                          onClick={() => setShowFormPassword(!showFormPassword)}
                          className="hover:text-neutral-700 cursor-pointer p-1"
                          title={showFormPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                        >
                          {showFormPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>
                      }
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-neutral-700 mb-1">
                      Xác nhận mật khẩu <span className="text-red-500">*</span>
                    </label>
                    <Input
                      type={showFormPassword ? 'text' : 'password'}
                      name="staff_creation_confirm_password"
                      id="staff_creation_confirm_password"
                      autoComplete="new-password"
                      placeholder="Nhập lại mật khẩu..."
                      value={formConfirmPassword}
                      onChange={(e) => {
                        setFormConfirmPassword(e.target.value);
                        clearFieldError('confirmPassword');
                      }}
                      error={formFieldErrors.confirmPassword}
                      rightIcon={
                        <button
                          type="button"
                          onClick={() => setShowFormPassword(!showFormPassword)}
                          className="hover:text-neutral-700 cursor-pointer p-1"
                          title={showFormPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                        >
                          {showFormPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>
                      }
                    />
                  </div>
                </div>
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
        description="Mật khẩu mới sẽ được mã hóa và đồng bộ trực tiếp vào máy chủ xác thực Keycloak (tài khoản sẽ có thể đăng nhập ngay)."
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

          <div className="flex justify-between items-center">
            <span className="text-xs text-neutral-500">Thiết lập mật khẩu mới:</span>
            <button
              type="button"
              onClick={() => {
                setNewPassword('Hotel@123456');
                setConfirmPassword('Hotel@123456');
              }}
              className="text-[11px] text-red-600 hover:text-red-700 font-semibold bg-red-50 hover:bg-red-100 border border-red-200 px-2 py-0.5 rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
            >
              <Sparkles className="w-3 h-3 text-red-600" />
              Dùng mặc định: Hotel@123456
            </button>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Mật khẩu mới <span className="text-red-500">*</span>
            </label>
            <Input
              type={showResetPassword ? 'text' : 'password'}
              placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)..."
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              rightIcon={
                <button
                  type="button"
                  onClick={() => setShowResetPassword(!showResetPassword)}
                  className="hover:text-neutral-700 cursor-pointer p-1"
                  title={showResetPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showResetPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              }
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Xác nhận mật khẩu mới <span className="text-red-500">*</span>
            </label>
            <Input
              type={showResetPassword ? 'text' : 'password'}
              placeholder="Nhập lại mật khẩu mới..."
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              rightIcon={
                <button
                  type="button"
                  onClick={() => setShowResetPassword(!showResetPassword)}
                  className="hover:text-neutral-700 cursor-pointer p-1"
                  title={showResetPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showResetPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              }
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

      {/* CREATED CREDENTIALS SUCCESS MODAL */}
      <Modal
        isOpen={!!createdCredentials}
        onClose={() => setCreatedCredentials(null)}
        title="Tạo tài khoản nhân viên thành công!"
        description="Thông tin đăng nhập đã được kích hoạt trực tiếp trên hệ thống Keycloak IAM. Vui lòng lưu lại và bàn giao cho nhân viên."
        maxWidth="md"
      >
        {createdCredentials && (
          <div className="space-y-4">
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl space-y-3">
              <div className="flex items-center justify-between pb-2 border-b border-emerald-200/60">
                <span className="text-xs font-semibold text-emerald-800">Họ và tên:</span>
                <span className="text-xs font-bold text-neutral-900">{createdCredentials.fullName}</span>
              </div>
              <div className="flex items-center justify-between pb-2 border-b border-emerald-200/60">
                <span className="text-xs font-semibold text-emerald-800">Vai trò / Phạm vi:</span>
                <span className="text-xs font-medium text-neutral-800 text-right">
                  {createdCredentials.roleName} • {createdCredentials.scope}
                </span>
              </div>
              <div className="flex items-center justify-between py-1 bg-white p-3 rounded-xl border border-emerald-200">
                <div>
                  <span className="block text-[10px] uppercase font-bold text-neutral-500">Tên đăng nhập (Username)</span>
                  <span className="text-sm font-bold text-neutral-900">{createdCredentials.username}</span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    void navigator.clipboard.writeText(createdCredentials.username);
                    setCopiedField('username');
                    setTimeout(() => setCopiedField(null), 2000);
                  }}
                  className="p-1.5 hover:bg-neutral-100 rounded-lg text-neutral-600 transition-colors cursor-pointer"
                  title="Sao chép tên đăng nhập"
                >
                  {copiedField === 'username' ? (
                    <Check className="w-4 h-4 text-emerald-600" />
                  ) : (
                    <Copy className="w-4 h-4" />
                  )}
                </button>
              </div>
              <div className="flex items-center justify-between py-1 bg-white p-3 rounded-xl border border-emerald-200">
                <div>
                  <span className="block text-[10px] uppercase font-bold text-neutral-500">Mật khẩu khởi tạo</span>
                  <span className="text-sm font-mono font-bold text-red-600 tracking-wider">
                    {createdCredentials.password}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    void navigator.clipboard.writeText(createdCredentials.password);
                    setCopiedField('password');
                    setTimeout(() => setCopiedField(null), 2000);
                  }}
                  className="p-1.5 hover:bg-neutral-100 rounded-lg text-neutral-600 transition-colors cursor-pointer"
                  title="Sao chép mật khẩu"
                >
                  {copiedField === 'password' ? (
                    <Check className="w-4 h-4 text-emerald-600" />
                  ) : (
                    <Copy className="w-4 h-4" />
                  )}
                </button>
              </div>
            </div>

            <div className="p-3 bg-neutral-50 border border-neutral-200 text-neutral-600 text-xs rounded-xl flex items-start gap-2">
              <Shield className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <span>
                Tài khoản đã được gán mật khẩu vĩnh viễn (không bị treo yêu cầu đổi mật khẩu lần đầu). Nhân viên có thể sử dụng thông tin trên để đăng nhập ngay tại màn hình Đăng nhập.
              </span>
            </div>

            <div className="flex justify-end pt-2">
              <Button
                type="button"
                variant="primary"
                onClick={() => setCreatedCredentials(null)}
                className="bg-red-600 hover:bg-red-700 text-white font-bold"
              >
                Hoàn tất & Đóng
              </Button>
            </div>
          </div>
        )}
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
            <Select<StaffStatus>
              value={newStatus}
              onChange={(val) => setNewStatus(val)}
              options={[
                { value: 'ACTIVE', label: 'ACTIVE - Đang làm việc' },
                { value: 'SUSPENDED', label: 'SUSPENDED - Tạm đình chỉ công tác' },
                { value: 'RESIGNED', label: 'RESIGNED - Đã chấm dứt hợp đồng / Nghỉ việc' },
              ]}
              size="lg"
            />
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
        description={`Bạn có chắc chắn muốn xóa mềm ${deletingIds.length} tài khoản nhân viên đã chọn? Tài khoản sẽ không thể đăng nhập hệ thống sau khi xóa.`}
        confirmText="Xóa nhân viên"
        cancelText="Hủy"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default StaffListPage;
