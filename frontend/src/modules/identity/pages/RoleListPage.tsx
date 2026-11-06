import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Plus,
  Trash2,
  RefreshCw,
  Search,
  Edit2,
  ShieldCheck,
  Key,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import roleService from '../services/role.service';
import type { RoleDto, PermissionDto } from '../types/role.types';

export const RoleListPage: React.FC = () => {
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [availablePermissions, setAvailablePermissions] = useState<PermissionDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);

  // Selection
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editingRole, setEditingRole] = useState<RoleDto | null>(null);
  const [formName, setFormName] = useState('');
  const [formCode, setFormCode] = useState('');
  const [formStatus, setFormStatus] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE');
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<number[]>([]);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deletingIds, setDeletingIds] = useState<number[]>([]);
  const [isDeleting, setIsDeleting] = useState(false);

  // Tải danh mục quyền hạn toàn hệ thống
  useEffect(() => {
    roleService
      .getAllPermissions()
      .then((res) => {
        if (res.result) setAvailablePermissions(res.result);
      })
      .catch((err) => console.error('Lỗi khi tải danh mục quyền hạn:', err));
  }, []);

  // Gom nhóm quyền hạn theo tài nguyên (resource)
  const groupedPermissions = useMemo(() => {
    const groups: Record<string, PermissionDto[]> = {};
    availablePermissions.forEach((p) => {
      const resName = p.resource || 'CHUNG';
      if (!groups[resName]) groups[resName] = [];
      groups[resName].push(p);
    });
    return groups;
  }, [availablePermissions]);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await roleService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        status: status || undefined,
      });
      if (response?.result) {
        setRoles(response.result.content || []);
        setTotalElements(response.result.totalElements || 0);
        setTotalPages(response.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách vai trò:', err);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, keyword, status]);

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
    setStatus('');
    setPage(0);
  };

  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(roles.map((r) => r.id));
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
    setEditingRole(null);
    setFormName('');
    setFormCode('');
    setFormStatus('ACTIVE');
    setSelectedPermissionIds([]);
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const openEditModal = (role: RoleDto) => {
    setEditingRole(role);
    setFormName(role.name);
    setFormCode(role.code);
    setFormStatus(role.status);
    setSelectedPermissionIds(role.permissions?.map((p) => p.id) || []);
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const togglePermission = (id: number) => {
    setSelectedPermissionIds((prev) =>
      prev.includes(id) ? prev.filter((pId) => pId !== id) : [...prev, id]
    );
  };

  const toggleGroupPermissions = (perms: PermissionDto[]) => {
    const groupIds = perms.map((p) => p.id);
    const allSelected = groupIds.every((id) => selectedPermissionIds.includes(id));
    if (allSelected) {
      setSelectedPermissionIds((prev) => prev.filter((id) => !groupIds.includes(id)));
    } else {
      setSelectedPermissionIds((prev) => Array.from(new Set([...prev, ...groupIds])));
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim()) {
      setFormError('Vui lòng nhập tên vai trò');
      return;
    }
    if (!editingRole && !formCode.trim()) {
      setFormError('Vui lòng nhập mã vai trò (VD: PROPERTY_MANAGER)');
      return;
    }

    setIsSubmitting(true);
    setFormError(null);
    try {
      if (editingRole) {
        await roleService.update(editingRole.id, {
          name: formName.trim(),
          status: formStatus,
          permissionIds: selectedPermissionIds,
        });
      } else {
        await roleService.create({
          name: formName.trim(),
          code: formCode.trim().toUpperCase(),
          status: formStatus,
          permissionIds: selectedPermissionIds,
        });
      }
      setIsFormModalOpen(false);
      void loadData();
    } catch {
      setFormError('Không thể lưu thông tin vai trò. Vui lòng kiểm tra mã trùng lặp hoặc kết nối.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return;
    setIsDeleting(true);
    try {
      await roleService.deleteBatch(deletingIds);
      setDeleteConfirmOpen(false);
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)));
      setDeletingIds([]);
      void loadData();
    } catch {
      alert('Không thể xóa vai trò. Vai trò có thể đang được gán cho nhân viên trong hệ thống.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="space-y-5">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 bg-neutral-100 text-neutral-700 font-bold text-xs rounded-full uppercase">
              IAM Security
            </span>
            <span className="text-xs text-neutral-500 font-medium">Phân quyền Hệ thống</span>
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 mt-1 flex items-center gap-2">
            <ShieldCheck className="w-6 h-6 text-red-600" />
            Vai trò & Quyền hạn (Roles & Permissions)
          </h1>
          <p className="text-xs text-neutral-500 mt-0.5">
            Quản trị danh mục vai trò Keycloak RBAC, phân quyền chi tiết (granular permissions) theo từng module chức năng.
          </p>
        </div>
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-end">
          <div className="sm:col-span-6">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Tìm kiếm vai trò
            </label>
            <Input
              placeholder="Nhập tên vai trò hoặc mã vai trò (VD: ADMIN, RECEPTIONIST)..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>
          <div className="sm:col-span-3">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Trạng thái
            </label>
            <Select<string>
              value={status}
              onChange={(val) => setStatus(val)}
              options={[
                { value: '', label: 'Tất cả trạng thái' },
                { value: 'ACTIVE', label: 'Đang kích hoạt (ACTIVE)' },
                { value: 'INACTIVE', label: 'Tạm khóa (INACTIVE)' },
              ]}
              size="lg"
            />
          </div>
          <div className="sm:col-span-3 flex gap-2">
            <Button type="submit" variant="primary" className="flex-1 bg-red-600 hover:bg-red-700">
              <Search className="w-4 h-4 mr-1.5" /> Lọc
            </Button>
            <Button type="button" variant="outline" onClick={handleReset} title="Làm mới bộ lọc">
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>
      </div>

      {/* Row 2: Action Toolbar */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <div className="flex items-center gap-2">
          <Button
            variant="primary"
            onClick={openCreateModal}
            className="bg-red-600 hover:bg-red-700 text-white font-bold"
          >
            <Plus className="w-4 h-4 mr-1.5" /> Thêm vai trò mới
          </Button>
          {selectedIds.length > 0 && (
            <Button
              variant="danger"
              onClick={() => {
                setDeletingIds(selectedIds);
                setDeleteConfirmOpen(true);
              }}
              className="bg-red-50 text-red-600 hover:bg-red-100 border border-red-200"
            >
              <Trash2 className="w-4 h-4 mr-1.5" /> Xóa đã chọn ({selectedIds.length})
            </Button>
          )}
        </div>
        <div className="text-xs font-medium text-neutral-500">
          Tổng số: <strong className="text-neutral-900">{totalElements}</strong> vai trò
        </div>
      </div>

      {/* Row 3: Data Table & Pagination */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-neutral-50 text-neutral-700 text-xs font-bold uppercase tracking-wider border-b border-neutral-200">
                <th className="p-3.5 w-10 text-center">
                  <input
                    type="checkbox"
                    checked={roles.length > 0 && selectedIds.length === roles.length}
                    onChange={handleSelectAll}
                    className="rounded text-red-600 focus:ring-red-500"
                  />
                </th>
                <th className="p-3.5">Tên vai trò</th>
                <th className="p-3.5">Mã vai trò Keycloak</th>
                <th className="p-3.5">Quyền hạn đã gán</th>
                <th className="p-3.5">Trạng thái</th>
                <th className="p-3.5">Ngày cập nhật</th>
                <th className="p-3.5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 text-sm">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
                    Đang tải danh sách vai trò...
                  </td>
                </tr>
              ) : roles.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    Chưa có vai trò nào phù hợp với bộ lọc tìm kiếm.
                  </td>
                </tr>
              ) : (
                roles.map((role) => {
                  const isSelected = selectedIds.includes(role.id);
                  const permCount = role.permissions?.length || 0;
                  return (
                    <tr
                      key={role.id}
                      className={`hover:bg-neutral-50/80 transition-colors ${
                        isSelected ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-3.5 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(role.id)}
                          className="rounded text-red-600 focus:ring-red-500"
                        />
                      </td>
                      <td className="p-3.5 font-bold text-neutral-900">
                        {role.name}
                      </td>
                      <td className="p-3.5 font-mono text-xs font-semibold text-neutral-700">
                        <span className="px-2 py-0.5 bg-neutral-100 rounded-md border border-neutral-200">
                          {role.code}
                        </span>
                      </td>
                      <td className="p-3.5">
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-100">
                          <Key className="w-3 h-3" />
                          {permCount} quyền hạn
                        </span>
                      </td>
                      <td className="p-3.5">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                            role.status === 'ACTIVE'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-neutral-100 text-neutral-600'
                          }`}
                        >
                          {role.status === 'ACTIVE' ? 'Kích hoạt' : 'Tạm khóa'}
                        </span>
                      </td>
                      <td className="p-3.5 text-neutral-500 text-xs">
                        {role.updatedAt
                          ? new Date(role.updatedAt).toLocaleDateString('vi-VN')
                          : '—'}
                      </td>
                      <td className="p-3.5 text-right space-x-1">
                        <button
                          type="button"
                          onClick={() => openEditModal(role)}
                          className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                          title="Chỉnh sửa vai trò & quyền"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setDeletingIds([role.id]);
                            setDeleteConfirmOpen(true);
                          }}
                          className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                          title="Xóa vai trò"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <Pagination
          pageNumber={page}
          pageSize={pageSize}
          totalElements={totalElements}
          totalPages={totalPages}
          onPageChange={(newPage) => setPage(newPage)}
        />
      </div>

      {/* Create / Edit Modal with Permission Selector */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingRole ? `Cập nhật Vai trò & Quyền: ${editingRole.name}` : 'Thêm mới Vai trò Hệ thống'}
        maxWidth="4xl"
      >
        <form onSubmit={handleSave} className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Tên hiển thị vai trò <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="VD: Tổng Quản lý Khách sạn"
                value={formName}
                onChange={(e) => setFormName(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Mã vai trò Keycloak <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="VD: PROPERTY_MANAGER"
                value={formCode}
                onChange={(e) => setFormCode(e.target.value.toUpperCase())}
                disabled={!!editingRole}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Trạng thái
              </label>
              <Select<'ACTIVE' | 'INACTIVE'>
                value={formStatus}
                onChange={(val) => setFormStatus(val)}
                options={[
                  { value: 'ACTIVE', label: 'Kích hoạt (ACTIVE)' },
                  { value: 'INACTIVE', label: 'Tạm khóa (INACTIVE)' },
                ]}
                size="lg"
              />
            </div>
          </div>

          {/* Granular Permission Checklist */}
          <div className="space-y-3 pt-2 border-t border-neutral-100">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-neutral-800 uppercase tracking-wider flex items-center gap-1.5">
                <Key className="w-4 h-4 text-red-600" />
                Gán danh mục quyền hạn (Đã chọn: {selectedPermissionIds.length} quyền)
              </label>
              <button
                type="button"
                onClick={() => {
                  if (selectedPermissionIds.length === availablePermissions.length) {
                    setSelectedPermissionIds([]);
                  } else {
                    setSelectedPermissionIds(availablePermissions.map((p) => p.id));
                  }
                }}
                className="text-xs text-red-600 hover:text-red-700 font-semibold cursor-pointer underline"
              >
                {selectedPermissionIds.length === availablePermissions.length
                  ? 'Bỏ chọn tất cả'
                  : 'Chọn tất cả quyền'}
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {Object.entries(groupedPermissions).map(([resource, perms]) => {
                const groupIds = perms.map((p) => p.id);
                const allGroupSelected = groupIds.every((id) =>
                  selectedPermissionIds.includes(id)
                );
                return (
                  <div
                    key={resource}
                    className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 space-y-2"
                  >
                    <div className="flex items-center justify-between border-b border-neutral-200 pb-1.5">
                      <span className="text-xs font-bold text-neutral-800 uppercase">
                        {resource} ({perms.length})
                      </span>
                      <button
                        type="button"
                        onClick={() => toggleGroupPermissions(perms)}
                        className="text-[11px] text-blue-600 hover:underline font-medium cursor-pointer"
                      >
                        {allGroupSelected ? 'Bỏ chọn nhóm' : 'Chọn nhóm'}
                      </button>
                    </div>

                    <div className="grid grid-cols-2 gap-1.5 pt-1">
                      {perms.map((p) => {
                        const isChecked = selectedPermissionIds.includes(p.id);
                        return (
                          <label
                            key={p.id}
                            className={`flex items-center gap-2 p-1.5 rounded-lg text-xs cursor-pointer transition-colors ${
                              isChecked
                                ? 'bg-red-50 text-red-900 font-medium'
                                : 'hover:bg-white text-neutral-600'
                            }`}
                          >
                            <input
                              type="checkbox"
                              checked={isChecked}
                              onChange={() => togglePermission(p.id)}
                              className="rounded text-red-600 focus:ring-red-500"
                            />
                            <span className="truncate">{p.action}</span>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-neutral-100">
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
              isLoading={isSubmitting}
              className="bg-red-600 hover:bg-red-700 text-white font-bold"
            >
              {editingRole ? 'Lưu thay đổi' : 'Tạo vai trò'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa vai trò"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} vai trò đã chọn? Thao tác này sẽ xóa mềm bản ghi khỏi hệ thống.`}
        confirmText="Xóa vĩnh viễn"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default RoleListPage;

