import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Trash2, RefreshCw, Search, Edit2, Briefcase } from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import departmentService from '../services/department.service';
import type { DepartmentDto } from '../types/department.types';

export const DepartmentListPage: React.FC = () => {
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
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
  const [editingDept, setEditingDept] = useState<DepartmentDto | null>(null);
  const [formCode, setFormCode] = useState('');
  const [formName, setFormName] = useState('');
  const [formStatus, setFormStatus] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deletingIds, setDeletingIds] = useState<number[]>([]);
  const [isDeleting, setIsDeleting] = useState(false);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await departmentService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        status: status || undefined,
      });
      if (response?.result) {
        setDepartments(response.result.content || []);
        setTotalElements(response.result.totalElements || 0);
        setTotalPages(response.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách phòng ban:', err);
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
      setSelectedIds(departments.map((d) => d.id));
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
    setEditingDept(null);
    setFormCode('');
    setFormName('');
    setFormStatus('ACTIVE');
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const openEditModal = (dept: DepartmentDto) => {
    setEditingDept(dept);
    setFormCode(dept.code);
    setFormName(dept.name);
    setFormStatus(dept.status);
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingDept && !formCode.trim()) {
      setFormError('Vui lòng nhập mã phòng ban (VD: FRONT_OFFICE, F_AND_B)');
      return;
    }
    if (!formName.trim()) {
      setFormError('Vui lòng nhập tên phòng ban');
      return;
    }

    setIsSubmitting(true);
    setFormError(null);
    try {
      if (editingDept) {
        await departmentService.update(editingDept.id, {
          name: formName.trim(),
          status: formStatus,
        });
      } else {
        await departmentService.create({
          code: formCode.trim().toUpperCase(),
          name: formName.trim(),
          status: formStatus,
        });
      }
      setIsFormModalOpen(false);
      void loadData();
    } catch {
      setFormError('Không thể lưu thông tin phòng ban. Vui lòng kiểm tra mã trùng lặp hoặc kết nối.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return;
    setIsDeleting(true);
    try {
      await departmentService.deleteBatch(deletingIds);
      setDeleteConfirmOpen(false);
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)));
      setDeletingIds([]);
      void loadData();
    } catch {
      alert('Không thể xóa phòng ban. Có thể phòng ban đang có nhân viên trực thuộc.');
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
              Organization Module
            </span>
            <span className="text-xs text-neutral-500 font-medium">Cơ cấu Doanh nghiệp</span>
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 mt-1 flex items-center gap-2">
            <Briefcase className="w-6 h-6 text-red-600" />
            Phòng ban Doanh nghiệp
          </h1>
          <p className="text-xs text-neutral-500 mt-0.5">
            Quản lý danh mục phòng ban chuẩn hóa toàn chuỗi khách sạn (Lễ tân, Buồng phòng, F&B, Kỹ thuật...).
          </p>
        </div>
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-end">
          <div className="sm:col-span-6">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Tìm kiếm phòng ban
            </label>
            <Input
              placeholder="Nhập mã hoặc tên phòng ban..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>
          <div className="sm:col-span-3">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Trạng thái hoạt động
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
            </select>
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
            <Plus className="w-4 h-4 mr-1.5" /> Thêm phòng ban mới
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
          Tổng số: <strong className="text-neutral-900">{totalElements}</strong> phòng ban
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
                    checked={departments.length > 0 && selectedIds.length === departments.length}
                    onChange={handleSelectAll}
                    className="rounded text-red-600 focus:ring-red-500"
                  />
                </th>
                <th className="p-3.5">Mã phòng ban</th>
                <th className="p-3.5">Tên phòng ban</th>
                <th className="p-3.5">Trạng thái</th>
                <th className="p-3.5">Ngày tạo</th>
                <th className="p-3.5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 text-sm">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
                    Đang tải danh sách phòng ban...
                  </td>
                </tr>
              ) : departments.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-neutral-400">
                    Chưa có phòng ban nào phù hợp với bộ lọc tìm kiếm.
                  </td>
                </tr>
              ) : (
                departments.map((dept) => {
                  const isSelected = selectedIds.includes(dept.id);
                  return (
                    <tr
                      key={dept.id}
                      className={`hover:bg-neutral-50/80 transition-colors ${
                        isSelected ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-3.5 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(dept.id)}
                          className="rounded text-red-600 focus:ring-red-500"
                        />
                      </td>
                      <td className="p-3.5 font-mono font-bold text-neutral-900 text-xs">
                        {dept.code}
                      </td>
                      <td className="p-3.5 font-semibold text-neutral-800">
                        {dept.name}
                      </td>
                      <td className="p-3.5">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                            dept.status === 'ACTIVE'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-neutral-100 text-neutral-600'
                          }`}
                        >
                          {dept.status === 'ACTIVE' ? 'Hoạt động' : 'Ngừng hoạt động'}
                        </span>
                      </td>
                      <td className="p-3.5 text-neutral-500 text-xs">
                        {dept.createdAt ? new Date(dept.createdAt).toLocaleDateString('vi-VN') : '—'}
                      </td>
                      <td className="p-3.5 text-right space-x-1">
                        <button
                          type="button"
                          onClick={() => openEditModal(dept)}
                          className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors"
                          title="Chỉnh sửa phòng ban"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setDeletingIds([dept.id]);
                            setDeleteConfirmOpen(true);
                          }}
                          className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                          title="Xóa phòng ban"
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

      {/* Create / Edit Modal */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingDept ? `Cập nhật Phòng ban: ${editingDept.code}` : 'Thêm mới Phòng ban Doanh nghiệp'}
        maxWidth="md"
      >
        <form onSubmit={handleSave} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl">
              {formError}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Mã phòng ban <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: FRONT_OFFICE, HOUSEKEEPING..."
              value={formCode}
              onChange={(e) => setFormCode(e.target.value.toUpperCase())}
              disabled={!!editingDept}
              required
            />
            {editingDept && (
              <p className="text-[11px] text-neutral-400 mt-1">
                Mã phòng ban không thể thay đổi sau khi tạo để bảo toàn tính toàn vẹn khóa ngoại.
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Tên phòng ban <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="VD: Lễ tân Tiền sảnh, Buồng phòng..."
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Trạng thái
            </label>
            <select
              value={formStatus}
              onChange={(e) => setFormStatus(e.target.value as 'ACTIVE' | 'INACTIVE')}
              className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
            >
              <option value="ACTIVE">Đang hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
            </select>
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
              {editingDept ? 'Lưu thay đổi' : 'Tạo phòng ban'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa phòng ban"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} phòng ban đã chọn? Thao tác này sẽ xóa mềm bản ghi khỏi hệ thống.`}
        confirmText="Xóa vĩnh viễn"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default DepartmentListPage;

