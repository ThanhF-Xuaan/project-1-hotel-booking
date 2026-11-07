import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Search, RefreshCw, Plus, Trash2, Pen, ShieldCheck, AlertCircle } from 'lucide-react';
import Button from '../../../../core/components/ui/Button';
import Input from '../../../../core/components/ui/Input';
import Select from '../../../../core/components/ui/Select';
import Modal, { ConfirmModal } from '../../../../core/components/ui/Modal';
import Pagination from '../../../../core/components/ui/Pagination';
import permissionService from '../../services/permission.service';
import type { PermissionDto } from '../../types/role.types';

export const PermissionListTab: React.FC = () => {
  const [permissions, setPermissions] = useState<PermissionDto[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(15);
  const [keyword, setKeyword] = useState('');
  const [selectedResource, setSelectedResource] = useState<string>('');
  const [selectedAction, setSelectedAction] = useState<string>('');
  const [selectedStatus, setSelectedStatus] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);

  // Dynamic lists from backend
  const [availableResources, setAvailableResources] = useState<string[]>([]);
  const [availableActions, setAvailableActions] = useState<string[]>([]);

  // Selection state
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  // Create / Edit Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editingPermission, setEditingPermission] = useState<PermissionDto | null>(null);
  const [formResource, setFormResource] = useState('');
  const [formAction, setFormAction] = useState('');
  const [formStatus, setFormStatus] = useState('ACTIVE');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete Confirmation State
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deletingIds, setDeletingIds] = useState<number[]>([]);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // Load distinct filter options on mount
  const loadFilterOptions = useCallback(async () => {
    try {
      const [resResources, resActions] = await Promise.all([
        permissionService.getResources().catch(() => null),
        permissionService.getActions().catch(() => null),
      ]);
      if (resResources?.result) setAvailableResources(resResources.result);
      if (resActions?.result) setAvailableActions(resActions.result);
    } catch (err) {
      console.warn('Không thể tải danh sách tài nguyên/hành động động:', err);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadFilterOptions();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadFilterOptions]);

  // Dropdown options
  const resourceOptions = useMemo(() => {
    const list = availableResources.map((res) => ({ value: res, label: res }));
    return [{ value: '', label: 'Tất cả Module / Tài nguyên' }, ...list];
  }, [availableResources]);

  const actionOptions = useMemo(() => {
    const list = availableActions.map((act) => ({ value: act, label: act }));
    return [{ value: '', label: 'Tất cả Hành động (Actions)' }, ...list];
  }, [availableActions]);

  const statusOptions = useMemo(() => [
    { value: '', label: 'Tất cả trạng thái' },
    { value: 'ACTIVE', label: 'Kích hoạt (ACTIVE)' },
    { value: 'INACTIVE', label: 'Tạm khóa (INACTIVE)' },
  ], []);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setDeleteError(null);
    try {
      const response = await permissionService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        resource: selectedResource || undefined,
        action: selectedAction || undefined,
        status: selectedStatus || undefined,
      });

      if (response?.result) {
        setPermissions(response.result.content || []);
        setTotalElements(response.result.totalElements || 0);
        setTotalPages(response.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi khi tải danh mục quyền hạn:', err);
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, keyword, selectedResource, selectedAction, selectedStatus]);

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
    setSelectedResource('');
    setSelectedAction('');
    setSelectedStatus('');
    setPage(0);
  };

  // Selection handlers
  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      setSelectedIds(permissions.map((p) => p.id));
    } else {
      setSelectedIds([]);
    }
  };

  const handleSelectOne = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  // Modal open handlers
  const openCreateModal = () => {
    setEditingPermission(null);
    setFormResource('');
    setFormAction('');
    setFormStatus('ACTIVE');
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const openEditModal = (p: PermissionDto) => {
    setEditingPermission(p);
    setFormResource(p.resource);
    setFormAction(p.action);
    setFormStatus(p.status || 'ACTIVE');
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanResource = formResource.trim().toUpperCase();
    const cleanAction = formAction.trim().toUpperCase();

    if (!cleanResource) {
      setFormError('Vui lòng nhập tên tài nguyên (resource).');
      return;
    }
    if (!cleanAction) {
      setFormError('Vui lòng nhập tên hành động (action).');
      return;
    }

    setIsSubmitting(true);
    setFormError(null);
    try {
      if (editingPermission) {
        await permissionService.update(editingPermission.id, {
          resource: cleanResource,
          action: cleanAction,
          status: formStatus,
        });
      } else {
        await permissionService.create({
          resource: cleanResource,
          action: cleanAction,
          status: formStatus,
        });
      }

      setIsFormModalOpen(false);
      void loadData();
      void loadFilterOptions();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setFormError(
        errObj.response?.data?.message ||
          'Không thể lưu quyền hạn. Cặp (hành động, tài nguyên) có thể đã tồn tại.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  // Delete handlers
  const openDeleteOne = (id: number) => {
    setDeletingIds([id]);
    setDeleteError(null);
    setDeleteConfirmOpen(true);
  };

  const openDeleteBatch = () => {
    if (selectedIds.length === 0) return;
    setDeletingIds(selectedIds);
    setDeleteError(null);
    setDeleteConfirmOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return;
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await permissionService.deleteBatch(deletingIds);
      setDeleteConfirmOpen(false);
      setSelectedIds([]);
      void loadData();
      void loadFilterOptions();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { message?: string } } };
      setDeleteError(
        errObj.response?.data?.message ||
          'Không thể xóa quyền hạn. Quyền hạn này đang được gán cho vai trò hoạt động trong hệ thống.'
      );
    } finally {
      setIsDeleting(false);
    }
  };

  const getActionBadgeColor = (action: string) => {
    switch (action.toUpperCase()) {
      case 'VIEW':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'CREATE':
        return 'bg-green-50 text-green-700 border-green-200';
      case 'UPDATE':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'CANCEL':
      case 'DELETE':
        return 'bg-red-50 text-red-700 border-red-200';
      case 'EXECUTE':
      case 'MANAGE':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      default:
        return 'bg-neutral-100 text-neutral-700 border-neutral-200';
    }
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '---';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="space-y-4 animate-in fade-in duration-150">
      {/* Alert banner if delete failed due to integrity constraint */}
      {deleteError && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-800 rounded-xl text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
            <span>{deleteError}</span>
          </div>
          <button
            onClick={() => setDeleteError(null)}
            className="text-red-500 hover:text-red-700 font-bold ml-4"
          >
            Đóng
          </button>
        </div>
      )}

      {/* STANDARD 3-ROW LAYOUT */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        {/* ROW 1: SEARCH & FILTER BAR */}
        <form
          onSubmit={handleSearch}
          className="p-4 md:p-5 border-b border-neutral-200 bg-neutral-50/50 flex flex-col md:flex-row items-center gap-3"
        >
          <div className="flex-1 w-full">
            <Input
              placeholder="Tìm theo quyền, tài nguyên hoặc mô tả..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>

          <div className="w-full md:w-52">
            <Select<string>
              value={selectedResource}
              onChange={(val) => {
                setSelectedResource(val);
                setPage(0);
              }}
              options={resourceOptions}
            />
          </div>

          <div className="w-full md:w-44">
            <Select<string>
              value={selectedAction}
              onChange={(val) => {
                setSelectedAction(val);
                setPage(0);
              }}
              options={actionOptions}
            />
          </div>

          <div className="w-full md:w-40">
            <Select<string>
              value={selectedStatus}
              onChange={(val) => {
                setSelectedStatus(val);
                setPage(0);
              }}
              options={statusOptions}
            />
          </div>

          <div className="flex items-center gap-2 w-full md:w-auto">
            <Button
              type="submit"
              variant="primary"
              className="bg-red-600 hover:bg-red-700 text-white font-bold h-12 px-6 rounded-xl shadow-xs"
            >
              <Search className="w-4 h-4 mr-2" />
              Lọc
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={handleReset}
              className="h-12 px-4 rounded-xl text-neutral-600 hover:text-neutral-900 border-neutral-200"
              title="Đặt lại bộ lọc"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </form>

        {/* ROW 2: ACTION TOOLBAR */}
        <div className="px-5 py-3.5 border-b border-neutral-200 bg-white flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <Button
              type="button"
              variant="primary"
              onClick={openCreateModal}
              className="bg-red-600 hover:bg-red-700 text-white font-bold h-10 px-4 rounded-xl shadow-xs text-xs"
            >
              <Plus className="w-4 h-4 mr-1.5" />
              Thêm quyền hạn mới
            </Button>

            {selectedIds.length > 0 && (
              <Button
                type="button"
                variant="outline"
                onClick={openDeleteBatch}
                className="h-10 px-3.5 rounded-xl text-red-600 border-red-200 hover:bg-red-50 text-xs font-bold transition-colors"
              >
                <Trash2 className="w-4 h-4 mr-1.5" />
                Xóa đã chọn ({selectedIds.length})
              </Button>
            )}
          </div>

          <div className="text-xs font-semibold text-neutral-600 bg-neutral-100 px-3 py-1.5 rounded-full self-end sm:self-auto">
            Tổng số: <span className="font-bold text-neutral-900">{totalElements}</span> quyền hạn
          </div>
        </div>

        {/* ROW 3: DATA TABLE */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-neutral-50/80 border-b border-neutral-200 text-[11px] font-bold text-neutral-600 uppercase tracking-wider">
                <th className="p-3.5 pl-5 w-10">
                  <input
                    type="checkbox"
                    className="rounded text-red-600 focus:ring-red-500 w-4 h-4 cursor-pointer"
                    checked={
                      permissions.length > 0 &&
                      selectedIds.length === permissions.length
                    }
                    onChange={handleSelectAll}
                  />
                </th>
                <th className="p-3.5 w-16">ID</th>
                <th className="p-3.5">Tài nguyên (Resource)</th>
                <th className="p-3.5">Hành động</th>
                <th className="p-3.5">Mã quyền RBAC</th>
                <th className="p-3.5">Mô tả nghiệp vụ</th>
                <th className="p-3.5">Vai trò đang sở hữu</th>
                <th className="p-3.5 text-center">Trạng thái</th>
                <th className="p-3.5">Ngày cập nhật</th>
                <th className="p-3.5 text-center pr-5 w-24">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200 text-xs">
              {isLoading ? (
                <tr>
                  <td colSpan={10} className="text-center py-12 text-neutral-400">
                    <div className="inline-flex items-center gap-2">
                      <RefreshCw className="w-4 h-4 animate-spin text-red-600" />
                      <span>Đang tải danh mục quyền hạn...</span>
                    </div>
                  </td>
                </tr>
              ) : permissions.length === 0 ? (
                <tr>
                  <td colSpan={10} className="text-center py-12 text-neutral-400">
                    Không tìm thấy quyền hạn nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                permissions.map((p) => {
                  const isChecked = selectedIds.includes(p.id);
                  const isInactive = p.status?.toUpperCase() === 'INACTIVE';
                  return (
                    <tr
                      key={p.id}
                      className={`hover:bg-neutral-50/60 transition-colors ${
                        isChecked ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-3.5 pl-5">
                        <input
                          type="checkbox"
                          className="rounded text-red-600 focus:ring-red-500 w-4 h-4 cursor-pointer"
                          checked={isChecked}
                          onChange={() => handleSelectOne(p.id)}
                        />
                      </td>
                      <td className="p-3.5 font-mono text-neutral-500 text-[11px]">
                        #{p.id}
                      </td>
                      <td className="p-3.5 font-bold text-neutral-900">
                        <span className="inline-flex items-center gap-1.5">
                          <ShieldCheck className="w-4 h-4 text-neutral-400 shrink-0" />
                          {p.resource}
                        </span>
                      </td>
                      <td className="p-3.5">
                        <span
                          className={`px-2 py-0.5 text-[10px] font-extrabold uppercase rounded border tracking-wide ${getActionBadgeColor(
                            p.action
                          )}`}
                        >
                          {p.action}
                        </span>
                      </td>
                      <td className="p-3.5 font-mono text-[11px] text-neutral-700 font-semibold">
                        ROLE_{p.resource}_{p.action}
                      </td>
                      <td className="p-3.5 text-neutral-600 max-w-xs">
                        {p.description || `${p.action} ${p.resource}`}
                      </td>
                      <td className="p-3.5 max-w-sm">
                        {p.roleNames && p.roleNames.length > 0 ? (
                          <div className="flex flex-wrap gap-1">
                            {p.roleNames.map((roleName, idx) => (
                              <span
                                key={idx}
                                className="inline-block px-2 py-0.5 bg-neutral-100 text-neutral-700 text-[10px] font-medium rounded-md border border-neutral-200"
                              >
                                {roleName}
                              </span>
                            ))}
                          </div>
                        ) : (
                          <span className="text-neutral-400 italic text-[11px]">
                            Chưa gán vai trò nào
                          </span>
                        )}
                      </td>
                      <td className="p-3.5 text-center">
                        <span
                          className={`inline-block px-2.5 py-0.5 text-[11px] font-bold rounded-full border ${
                            isInactive
                              ? 'bg-neutral-100 text-neutral-600 border-neutral-300'
                              : 'bg-green-50 text-green-700 border-green-200'
                          }`}
                        >
                          {isInactive ? 'Tạm khóa' : 'Kích hoạt'}
                        </span>
                      </td>
                      <td className="p-3.5 text-neutral-500 text-[11px]">
                        {formatDate(p.updatedAt)}
                      </td>
                      <td className="p-3.5 text-center pr-5">
                        <div className="flex items-center justify-center gap-1">
                          <button
                            type="button"
                            onClick={() => openEditModal(p)}
                            className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors"
                            title="Chỉnh sửa quyền hạn"
                          >
                            <Pen className="w-3.5 h-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => openDeleteOne(p.id)}
                            className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Xóa quyền hạn"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
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

      {/* CREATE / EDIT MODAL */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingPermission ? 'Chỉnh sửa Quyền hạn Hệ thống' : 'Thêm mới Quyền hạn Hệ thống'}
      >
        <form onSubmit={handleFormSubmit} className="space-y-4">
          {formError && (
            <div className="p-3.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
              <span>{formError}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1.5">
              Tài nguyên (Resource) <span className="text-red-600">*</span>
            </label>
            <Input
              placeholder="VD: BOOKING, STAFF, GUEST..."
              value={formResource}
              onChange={(e) => setFormResource(e.target.value)}
              className="uppercase font-mono"
              required
            />
            <p className="text-[11px] text-neutral-500 mt-1">
              Nhập mã phân hệ hoặc module chức năng (viết hoa, không dấu, phân cách bằng dấu gạch dưới).
            </p>
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1.5">
              Hành động (Action) <span className="text-red-600">*</span>
            </label>
            <Input
              placeholder="VD: VIEW, CREATE, UPDATE, DELETE..."
              value={formAction}
              onChange={(e) => setFormAction(e.target.value)}
              className="uppercase font-mono"
              required
            />
            <p className="text-[11px] text-neutral-500 mt-1">
              Hành động thực thi: VIEW, CREATE, UPDATE, DELETE, EXECUTE, MANAGE, REFUND...
            </p>
          </div>

          <div>
            <label className="block text-xs font-bold text-neutral-700 uppercase tracking-wider mb-1.5">
              Trạng thái
            </label>
            <Select<string>
              value={formStatus}
              onChange={(val) => setFormStatus(val)}
              options={[
                { value: 'ACTIVE', label: 'Kích hoạt (ACTIVE)' },
                { value: 'INACTIVE', label: 'Tạm khóa (INACTIVE)' },
              ]}
            />
          </div>

          {formResource && formAction && (
            <div className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 text-xs">
              <span className="text-neutral-500">Mã quyền RBAC dự kiến: </span>
              <span className="font-mono font-bold text-neutral-900">
                ROLE_{formResource.trim().toUpperCase()}_{formAction.trim().toUpperCase()}
              </span>
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-neutral-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsFormModalOpen(false)}
              className="text-neutral-600"
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={isSubmitting}
              className="bg-red-600 hover:bg-red-700 text-white font-bold"
            >
              {editingPermission ? 'Lưu thay đổi' : 'Tạo quyền hạn'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* DELETE CONFIRMATION MODAL */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa quyền hạn"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} quyền hạn đã chọn? Lưu ý: Nếu quyền hạn đang được bất kỳ vai trò nào sử dụng, hệ thống sẽ từ chối xóa để đảm bảo an toàn.`}
        confirmText="Xác nhận xóa"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default PermissionListTab;
