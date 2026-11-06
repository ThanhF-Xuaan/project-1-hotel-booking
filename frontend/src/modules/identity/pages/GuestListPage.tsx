import React, { useState, useEffect, useCallback } from 'react';
import {
  Plus,
  Trash2,
  RefreshCw,
  Search,
  Edit2,
  UserSquare2,
  History,
  Phone,
  CreditCard,
  Building,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import Select from '../../../core/components/ui/Select';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import Pagination from '../../../core/components/ui/Pagination';
import guestService from '../services/guest.service';
import type { GuestDto, GuestStayHistoryResponse } from '../types/guest.types';

export const GuestListPage: React.FC = () => {
  const [guests, setGuests] = useState<GuestDto[]>([]);
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
  const [editingGuest, setEditingGuest] = useState<GuestDto | null>(null);
  const [formFullName, setFormFullName] = useState('');
  const [formPhone, setFormPhone] = useState('');
  const [formEmail, setFormEmail] = useState('');
  const [formIdentityType, setFormIdentityType] = useState('CCCD');
  const [formIdentityNumber, setFormIdentityNumber] = useState('');
  const [formBirthDate, setFormBirthDate] = useState('');
  const [formNationality, setFormNationality] = useState('Việt Nam');
  const [formStatus, setFormStatus] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // CRM Stay History Modal State
  const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false);
  const [stayHistory, setStayHistory] = useState<GuestStayHistoryResponse | null>(null);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  // Delete Modal
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [deletingIds, setDeletingIds] = useState<number[]>([]);
  const [isDeleting, setIsDeleting] = useState(false);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await guestService.filter({
        page,
        pageSize,
        keyword: keyword.trim() || undefined,
        status: status || undefined,
      });
      if (response?.result) {
        setGuests(response.result.content || []);
        setTotalElements(response.result.totalElements || 0);
        setTotalPages(response.result.totalPages || 0);
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách khách hàng:', err);
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
      setSelectedIds(guests.map((g) => g.id));
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
    setEditingGuest(null);
    setFormFullName('');
    setFormPhone('');
    setFormEmail('');
    setFormIdentityType('CCCD');
    setFormIdentityNumber('');
    setFormBirthDate('');
    setFormNationality('Việt Nam');
    setFormStatus('ACTIVE');
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const openEditModal = (guest: GuestDto) => {
    setEditingGuest(guest);
    setFormFullName(guest.fullName);
    setFormPhone(guest.phone || '');
    setFormEmail(guest.email || '');
    setFormIdentityType(guest.identityType || 'CCCD');
    setFormIdentityNumber(guest.identityNumber || '');
    setFormBirthDate(guest.birthDate || '');
    setFormNationality(guest.nationality || 'Việt Nam');
    setFormStatus(guest.status);
    setFormError(null);
    setIsFormModalOpen(true);
  };

  const openHistoryModal = async (guest: GuestDto) => {
    setIsLoadingHistory(true);
    setStayHistory(null);
    setIsHistoryModalOpen(true);
    try {
      const res = await guestService.getStayHistory(guest.id);
      if (res?.result) {
        setStayHistory(res.result);
      }
    } catch (err) {
      console.error('Lỗi khi tải lịch sử khách hàng:', err);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formFullName.trim()) {
      setFormError('Vui lòng nhập họ và tên khách hàng');
      return;
    }
    if (!formPhone.trim()) {
      setFormError('Vui lòng nhập số điện thoại');
      return;
    }

    setIsSubmitting(true);
    setFormError(null);
    try {
      if (editingGuest) {
        await guestService.update(editingGuest.id, {
          fullName: formFullName.trim().toUpperCase(),
          phone: formPhone.trim(),
          email: formEmail.trim() || undefined,
          identityType: formIdentityType,
          identityNumber: formIdentityNumber.trim() || undefined,
          birthDate: formBirthDate || undefined,
          nationality: formNationality.trim() || 'Việt Nam',
          status: formStatus,
        });
      } else {
        await guestService.create({
          fullName: formFullName.trim().toUpperCase(),
          phone: formPhone.trim(),
          email: formEmail.trim() || undefined,
          identityType: formIdentityType,
          identityNumber: formIdentityNumber.trim() || undefined,
          birthDate: formBirthDate || undefined,
          nationality: formNationality.trim() || 'Việt Nam',
        });
      }
      setIsFormModalOpen(false);
      void loadData();
    } catch {
      setFormError('Không thể lưu hồ sơ khách hàng. Vui lòng kiểm tra số điện thoại hoặc kết nối.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (deletingIds.length === 0) return;
    setIsDeleting(true);
    try {
      await guestService.deleteBatch(deletingIds);
      setDeleteConfirmOpen(false);
      setSelectedIds((prev) => prev.filter((id) => !deletingIds.includes(id)));
      setDeletingIds([]);
      void loadData();
    } catch {
      alert('Không thể xóa hồ sơ khách hàng. Khách có thể đang có đơn đặt phòng hoạt động.');
    } finally {
      setIsDeleting(false);
    }
  };

  const formatVnd = (amount?: number) => {
    if (!amount) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
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
            <span className="text-xs text-neutral-500 font-medium">Quản trị Khách hàng</span>
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 mt-1 flex items-center gap-2">
            <UserSquare2 className="w-6 h-6 text-red-600" />
            Hồ sơ Khách hàng (CRM Guest Profiles)
          </h1>
          <p className="text-xs text-neutral-500 mt-0.5">
            Quản lý tập trung hồ sơ khách đặt phòng, lịch sử lưu trú, tổng tích lũy chi tiêu và thông tin giấy tờ tùy thân.
          </p>
        </div>
      </div>

      {/* Row 1: Search & Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-end">
          <div className="sm:col-span-6">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Tìm kiếm khách hàng
            </label>
            <Input
              placeholder="Nhập họ tên, số điện thoại hoặc số CCCD/Hộ chiếu..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              leftIcon={<Search className="w-4 h-4 text-neutral-400" />}
            />
          </div>
          <div className="sm:col-span-3">
            <label className="block text-xs font-semibold text-neutral-700 mb-1">
              Trạng thái hồ sơ
            </label>
            <Select<string>
              value={status}
              onChange={(val) => setStatus(val)}
              options={[
                { value: '', label: 'Tất cả trạng thái' },
                { value: 'ACTIVE', label: 'Đang hoạt động (ACTIVE)' },
                { value: 'INACTIVE', label: 'Ngừng hoạt động (INACTIVE)' },
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
            <Plus className="w-4 h-4 mr-1.5" /> Thêm hồ sơ khách mới
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
          Tổng số: <strong className="text-neutral-900">{totalElements}</strong> hồ sơ khách hàng
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
                    checked={guests.length > 0 && selectedIds.length === guests.length}
                    onChange={handleSelectAll}
                    className="rounded text-red-600 focus:ring-red-500"
                  />
                </th>
                <th className="p-3.5">Họ và tên</th>
                <th className="p-3.5">Số điện thoại</th>
                <th className="p-3.5">Giấy tờ tùy thân</th>
                <th className="p-3.5">Email</th>
                <th className="p-3.5">Trạng thái</th>
                <th className="p-3.5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100 text-sm">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
                    Đang tải danh sách hồ sơ khách hàng...
                  </td>
                </tr>
              ) : guests.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-neutral-400">
                    Chưa có hồ sơ khách hàng nào phù hợp với bộ lọc tìm kiếm.
                  </td>
                </tr>
              ) : (
                guests.map((guest) => {
                  const isSelected = selectedIds.includes(guest.id);
                  return (
                    <tr
                      key={guest.id}
                      className={`hover:bg-neutral-50/80 transition-colors ${
                        isSelected ? 'bg-red-50/30' : ''
                      }`}
                    >
                      <td className="p-3.5 text-center">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(guest.id)}
                          className="rounded text-red-600 focus:ring-red-500"
                        />
                      </td>
                      <td className="p-3.5">
                        <div className="font-bold text-neutral-900">{guest.fullName}</div>
                        {guest.birthDate && (
                          <div className="text-xs text-neutral-400">
                            NS: {guest.birthDate} {guest.nationality && `(${guest.nationality})`}
                          </div>
                        )}
                      </td>
                      <td className="p-3.5 font-mono text-xs font-semibold text-neutral-800">
                        {guest.phone ? (
                          <span className="flex items-center gap-1">
                            <Phone className="w-3.5 h-3.5 text-neutral-400" />
                            {guest.phone}
                          </span>
                        ) : (
                          '—'
                        )}
                      </td>
                      <td className="p-3.5">
                        {guest.identityNumber ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-mono bg-blue-50 text-blue-700 border border-blue-100">
                            <CreditCard className="w-3 h-3" />
                            {guest.identityType || 'CCCD'}: {guest.identityNumber}
                          </span>
                        ) : (
                          <span className="text-neutral-400 text-xs">Chưa cập nhật</span>
                        )}
                      </td>
                      <td className="p-3.5 text-neutral-600 text-xs">
                        {guest.email || '—'}
                      </td>
                      <td className="p-3.5">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                            guest.status === 'ACTIVE'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-neutral-100 text-neutral-600'
                          }`}
                        >
                          {guest.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm khóa'}
                        </span>
                      </td>
                      <td className="p-3.5 text-right space-x-1">
                        <button
                          type="button"
                          onClick={() => openHistoryModal(guest)}
                          className="p-1.5 text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded-lg transition-colors cursor-pointer"
                          title="Xem lịch sử lưu trú CRM"
                        >
                          <History className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => openEditModal(guest)}
                          className="p-1.5 text-neutral-500 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition-colors cursor-pointer"
                          title="Chỉnh sửa hồ sơ khách"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setDeletingIds([guest.id]);
                            setDeleteConfirmOpen(true);
                          }}
                          className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                          title="Xóa hồ sơ khách"
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

      {/* CRM Stay History Modal */}
      <Modal
        isOpen={isHistoryModalOpen}
        onClose={() => setIsHistoryModalOpen(false)}
        title={`Lịch sử Lưu trú & Chi tiêu: ${stayHistory?.fullName || 'Khách hàng'}`}
        description="Thông tin thống kê tổng quan và lịch sử các đơn đặt phòng trong toàn hệ thống chuỗi."
        maxWidth="2xl"
      >
        <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
          {isLoadingHistory ? (
            <div className="py-12 text-center text-neutral-400">
              <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-red-600" />
              Đang tải lịch sử lưu trú...
            </div>
          ) : !stayHistory ? (
            <div className="py-8 text-center text-neutral-400">
              Không thể tải dữ liệu lịch sử khách hàng.
            </div>
          ) : (
            <div className="space-y-4">
              {/* Stat Cards */}
              <div className="grid grid-cols-2 gap-3">
                <div className="p-4 bg-red-50 border border-red-200 rounded-2xl">
                  <div className="text-xs font-bold text-red-700 uppercase">Tổng chi tiêu tích lũy</div>
                  <div className="text-xl font-extrabold text-red-900 mt-1 font-mono">
                    {formatVnd(stayHistory.totalSpent)}
                  </div>
                </div>
                <div className="p-4 bg-blue-50 border border-blue-200 rounded-2xl">
                  <div className="text-xs font-bold text-blue-700 uppercase">Tổng lượt lưu trú</div>
                  <div className="text-xl font-extrabold text-blue-900 mt-1 font-mono">
                    {stayHistory.totalStays} <span className="text-xs font-normal">lượt</span>
                  </div>
                </div>
              </div>

              {/* Booking History Table */}
              <div>
                <h4 className="text-xs font-bold text-neutral-700 uppercase tracking-wider mb-2">
                  Danh sách đơn đặt phòng gần nhất ({stayHistory.bookings?.length || 0})
                </h4>
                {(!stayHistory.bookings || stayHistory.bookings.length === 0) ? (
                  <div className="p-6 text-center border-2 border-dashed border-neutral-200 rounded-xl text-xs text-neutral-400">
                    Khách hàng chưa có lịch sử đơn đặt phòng nào hoàn tất.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {stayHistory.bookings.map((b) => (
                      <div
                        key={b.bookingId}
                        className="p-3 bg-neutral-50 border border-neutral-200 rounded-xl flex items-center justify-between gap-3 text-xs"
                      >
                        <div>
                          <div className="font-bold text-neutral-900 font-mono">{b.bookingNumber}</div>
                          <div className="text-neutral-500 flex items-center gap-1 mt-0.5">
                            <Building className="w-3 h-3 text-neutral-400" />
                            {b.hotelName} — Ngày đặt: {b.bookingDate}
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="font-bold text-neutral-900 font-mono">
                            {formatVnd(b.totalPrice)}
                          </div>
                          <span className="inline-block mt-0.5 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-neutral-200 text-neutral-800">
                            {b.status}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          <div className="flex justify-end pt-3 border-t border-neutral-100">
            <Button variant="outline" onClick={() => setIsHistoryModalOpen(false)}>
              Đóng
            </Button>
          </div>
        </div>
      </Modal>

      {/* Create / Edit Modal */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingGuest ? `Cập nhật Hồ sơ Khách: ${editingGuest.fullName}` : 'Thêm mới Hồ sơ Khách hàng'}
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
                Họ và tên khách hàng <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="NGUYỄN VĂN A"
                value={formFullName}
                onChange={(e) => setFormFullName(e.target.value.toUpperCase())}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Số điện thoại liên hệ <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="0912345678"
                value={formPhone}
                onChange={(e) => setFormPhone(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Email
              </label>
              <Input
                type="email"
                placeholder="khach@gmail.com"
                value={formEmail}
                onChange={(e) => setFormEmail(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Loại giấy tờ
              </label>
              <Select<string>
                value={formIdentityType}
                onChange={(val) => setFormIdentityType(val)}
                options={[
                  { value: 'CCCD', label: 'Căn cước công dân (CCCD)' },
                  { value: 'PASSPORT', label: 'Hộ chiếu (Passport)' },
                  { value: 'CMND', label: 'Chứng minh nhân dân (CMND)' },
                  { value: 'DRIVER_LICENSE', label: 'Giấy phép lái xe' },
                  { value: 'OTHER', label: 'Khác' },
                ]}
                size="lg"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Số giấy tờ (CCCD / Passport)
              </label>
              <Input
                placeholder="001200001234"
                value={formIdentityNumber}
                onChange={(e) => setFormIdentityNumber(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Ngày sinh
              </label>
              <Input
                type="date"
                value={formBirthDate}
                onChange={(e) => setFormBirthDate(e.target.value)}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">
                Quốc tịch
              </label>
              <Input
                placeholder="Việt Nam"
                value={formNationality}
                onChange={(e) => setFormNationality(e.target.value)}
              />
            </div>

            {editingGuest && (
              <div className="sm:col-span-2">
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  Trạng thái hồ sơ
                </label>
                <Select<'ACTIVE' | 'INACTIVE'>
                  value={formStatus}
                  onChange={(val) => setFormStatus(val)}
                  options={[
                    { value: 'ACTIVE', label: 'Đang hoạt động (ACTIVE)' },
                    { value: 'INACTIVE', label: 'Ngừng hoạt động (INACTIVE)' },
                  ]}
                  size="lg"
                />
              </div>
            )}
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
              {editingGuest ? 'Lưu thay đổi' : 'Tạo hồ sơ'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xác nhận xóa hồ sơ khách hàng"
        description={`Bạn có chắc chắn muốn xóa ${deletingIds.length} hồ sơ khách hàng đã chọn? Thao tác này sẽ xóa mềm bản ghi khỏi hệ thống.`}
        confirmText="Xóa vĩnh viễn"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default GuestListPage;

