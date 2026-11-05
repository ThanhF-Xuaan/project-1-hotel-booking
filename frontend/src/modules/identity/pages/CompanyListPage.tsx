import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Trash2, RefreshCw, Search, Edit2, Building2 } from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import companyService from '../services/company.service';
import type { CompanyDto } from '../types/company.types';

export const CompanyListPage: React.FC = () => {
  const [companies, setCompanies] = useState<CompanyDto[]>([]);
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
  const [editingCompany, setEditingCompany] = useState<CompanyDto | null>(null);
  const [formName, setFormName] = useState('');
  const [formTaxCode, setFormTaxCode] = useState('');
  const [formAddress, setFormAddress] = useState('');
  const [formContactName, setFormContactName] = useState('');
  const [formContactPhone, setFormContactPhone] = useState('');
  const [formContactEmail, setFormContactEmail] = useState('');
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
      const response = await companyService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        status: status || undefined,
      });
      if (response?.result) {
        setCompanies(response.result.content || []);
        setTotalElements(response.result.totalElements || 0);
        setTotalPages(response.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách doanh nghiệp đối tác:', err);
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
      setSelectedIds(companies.map((c) => c.id));
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
    setEditingCompany(null);
    setFormName('');
    setFormTaxCode('');
    setFormAddress('');
    setFormContactName('');
    setFormContactPhone('');
    setFormContactEmail('');
    setFormStatus('ACTIVE');
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const openEditModal = (comp: CompanyDto) => {
    setEditingCompany(comp);
    setFormName(comp.name);
    setFormTaxCode(comp.taxCode || '');
    setFormAddress(comp.address || '');
    setFormContactName(comp.contactName || '');
    setFormContactPhone(comp.contactPhone || '');
    setFormContactEmail(comp.contactEmail || '');
    setFormStatus(comp.status);
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim()) {
      setFormError('Vui lòng nhập tên doanh nghiệp');
      return;
    }

    setIsSubmitting(true);
    setFormError(null);
    try {
      if (editingCompany) {
        await companyService.update(editingCompany.id, {
          name: formName.trim(),
          taxCode: formTaxCode.trim() || undefined,
          address: formAddress.trim() || undefined,
          contactName: formContactName.trim() || undefined,
          contactPhone: formContactPhone.trim() || undefined,
          contactEmail: formContactEmail.trim() || undefined,
          status: formStatus,
        });
      } else {
        await companyService.create({
          name: formName.trim(),
          taxCode: formTaxCode.trim() || undefined,
          address: formAddress.trim() || undefined,
          contactName: formContactName.trim() || undefined,
          contactPhone: formContactPhone.trim() || undefined,
          contactEmail: formContactEmail.trim() || undefined,
          status: formStatus,
        });
      }
      setIsFormModalOpen(false);
      void loadData();
    } catch {
      setFormError('Không thể lưu thông tin doanh nghiệp. Vui lòng kiểm tra mã số thuế trùng hoặc kết nối.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return;
    setIsDeleting(true);
    try {
      await companyService.deleteBatch(deletingIds);
      setDeleteConfirmOpen(false);
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)));
      setDeletingIds([]);
      void loadData();
    } catch {
      alert('Không thể xóa doanh nghiệp. Doanh nghiệp này có thể đang liên kết với các đơn đặt phòng Corporate.');
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
              Identity & CRM
            </span>
            <span className="text-xs text-neutral-500 font-medium">Khách hàng Doanh nghiệp</span>
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 mt-1 flex items-center gap-2">
            <Building2 className="w-6 h-6 text-red-600" />
            Doanh nghiệp Đối tác (B2B Corporate)
          </h1>
          <p className="text-xs text-neutral-500 mt-0.5">
            Quản lý danh sách đối tác doanh nghiệp, công ty lữ hành, mã số thuế và thông tin người liên hệ ký hợp đồng.
          </p>
        </div>
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-end">
          <div className="sm:col-span-6">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Tìm kiếm doanh nghiệp
            </label>
            <Input
              placeholder="Nhập tên doanh nghiệp, mã số thuế, người liên hệ..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>
          <div className="sm:col-span-3">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Trạng thái hợp tác
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hợp tác (ACTIVE)</option>
              <option value="INACTIVE">Tạm ngưng (INACTIVE)</option>
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
            <Plus className="w-4 h-4 mr-1.5" /> Thêm doanh nghiệp mới
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
          Tổng số: <strong className="text-neutral-900">{totalElements}</strong> đối tác
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
                    checked={companies.length > 0 && selectedIds.length === companies.length}
                    onChange={handleSelectAll}
                    className="rounded text-red-600 focus:ring-red-500"
                  />
                </th>
                <th className="p-3.5">Tên doanh nghiệp</th>
                <th className="p-3.5">Mã số thuế</th>
                <th className="p-3.5">Người đại diện</th>
                <th className="p-3.5">Số điện thoại</th>
                <th className="p-3.5">Trạng thái</th>
                <th className="p-3.5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 text-sm">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
                    Đang tải danh sách doanh nghiệp đối tác...
                  </td>
                </tr>
              ) : companies.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    Chưa có doanh nghiệp đối tác nào phù hợp với bộ lọc tìm kiếm.
                  </td>
                </tr>
              ) : (
                companies.map((comp) => {
                  const isSelected = selectedIds.includes(comp.id);
                  return (
                    <tr
                      key={comp.id}
                      className={`hover:bg-neutral-50/80 transition-colors ${
                        isSelected ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-3.5 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(comp.id)}
                          className="rounded text-red-600 focus:ring-red-500"
                        />
                      </td>
                      <td className="p-3.5">
                        <div className="font-bold text-neutral-900">{comp.name}</div>
                        {comp.address && (
                          <div className="text-xs text-neutral-400 truncate max-w-[240px]">
                            {comp.address}
                          </div>
                        )}
                      </td>
                      <td className="p-3.5 font-mono text-xs font-semibold text-neutral-700">
                        {comp.taxCode || '—'}
                      </td>
                      <td className="p-3.5 text-neutral-800 text-xs">
                        {comp.contactName || '—'}
                      </td>
                      <td className="p-3.5 text-neutral-600 text-xs font-mono">
                        {comp.contactPhone || '—'}
                      </td>
                      <td className="p-3.5">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                            comp.status === 'ACTIVE'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-neutral-100 text-neutral-600'
                          }`}
                        >
                          {comp.status === 'ACTIVE' ? 'Đang hợp tác' : 'Tạm ngưng'}
                        </span>
                      </td>
                      <td className="p-3.5 text-right space-x-1">
                        <button
                          type="button"
                          onClick={() => openEditModal(comp)}
                          className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors"
                          title="Chỉnh sửa thông tin đối tác"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setDeletingIds([comp.id]);
                            setDeleteConfirmOpen(true);
                          }}
                          className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                          title="Xóa đối tác"
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
        title={editingCompany ? `Cập nhật Doanh nghiệp: ${editingCompany.name}` : 'Thêm mới Doanh nghiệp Đối tác'}
        maxWidth="lg"
      >
        <form onSubmit={handleSave} className="space-y-4">
          {formError && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Tên doanh nghiệp / Đối tác <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="VD: Tập đoàn Viettel, Công ty Du lịch Saigontourist..."
                value={formName}
                onChange={(e) => setFormName(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Mã số thuế (MST)
              </label>
              <Input
                placeholder="VD: 0100109106"
                value={formTaxCode}
                onChange={(e) => setFormTaxCode(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Trạng thái hợp tác
              </label>
              <select
                value={formStatus}
                onChange={(e) => setFormStatus(e.target.value as 'ACTIVE' | 'INACTIVE')}
                className="w-full h-10 md:h-12 px-3 text-sm bg-white border border-neutral-300 rounded-xl focus:ring-2 focus:ring-red-600 focus:outline-hidden"
              >
                <option value="ACTIVE">Đang hợp tác (ACTIVE)</option>
                <option value="INACTIVE">Tạm ngưng (INACTIVE)</option>
              </select>
            </div>

            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Địa chỉ trụ sở
              </label>
              <Input
                placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành..."
                value={formAddress}
                onChange={(e) => setFormAddress(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Người đại diện liên hệ
              </label>
              <Input
                placeholder="Họ tên người liên hệ phụ trách"
                value={formContactName}
                onChange={(e) => setFormContactName(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Số điện thoại liên hệ
              </label>
              <Input
                placeholder="VD: 02462556789"
                value={formContactPhone}
                onChange={(e) => setFormContactPhone(e.target.value)}
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Email liên hệ đối tác
              </label>
              <Input
                type="email"
                placeholder="contact@doanhnghiep.com"
                value={formContactEmail}
                onChange={(e) => setFormContactEmail(e.target.value)}
              />
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
              {editingCompany ? 'Lưu thay đổi' : 'Tạo đối tác'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa doanh nghiệp đối tác"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} doanh nghiệp đã chọn? Thao tác này sẽ xóa mềm bản ghi khỏi hệ thống.`}
        confirmText="Xóa vĩnh viễn"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default CompanyListPage;

